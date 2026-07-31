/**
 * RuralOS Progressive Web App (PWA) Service Worker
 * File: /app/src/main/assets/sw.js
 * 
 * Provides:
 * 1. Static & Dynamic caching of critical assets for complete offline capability.
 * 2. Background synchronization API listeners.
 * 3. IndexedDB state setup and helper scripts for offline persistence of:
 *    - Agricultural notices & government schemes
 *    - Labor marketplace job listings & bookings
 *    - Equipment rentals
 *    - IoT telemetry sensor caches
 */

const CACHE_NAME = 'ruralos-cache-v1';
const OFFLINE_URL = '/offline.html';

// Critical assets to cache during installation
const STATIC_ASSETS = [
    '/',
    '/index.html',
    '/offline.html',
    '/css/main.css',
    '/js/app.js',
    '/manifest.json',
    '/assets/icons/icon-192x192.png',
    '/assets/icons/icon-512x512.png',
    '/assets/logos/ruralos_logo.png'
];

// IndexedDB configuration & setup
const DB_NAME = 'RuralOSOfflineDB';
const DB_VERSION = 1;

/**
 * Initialize IndexedDB inside the Service Worker context.
 * This stores farmer profiles, marketplace inventory, offline messages, and sync queues.
 */
function initIndexedDB() {
    return new Promise((resolve, reject) => {
        const request = indexedDB.open(DB_NAME, DB_VERSION);

        request.onerror = (event) => {
            console.error('IndexedDB failed to open inside sw.js:', event.target.error);
            reject(event.target.error);
        };

        request.onsuccess = (event) => {
            resolve(event.target.result);
        };

        request.onupgradeneeded = (event) => {
            const db = event.target.result;
            
            // 1. User Profile store
            if (!db.objectStoreNames.contains('user_profile')) {
                db.createObjectStore('user_profile', { keyPath: 'id' });
            }

            // 2. Sync Queue (unsent offline messages, bookings, job applications)
            if (!db.objectStoreNames.contains('sync_queue')) {
                db.createObjectStore('sync_queue', { keyPath: 'id', autoIncrement: true });
            }

            // 3. Local Market Produce and Jobs cache
            if (!db.objectStoreNames.contains('cached_market')) {
                db.createObjectStore('cached_market', { keyPath: 'id' });
            }

            // 4. Offline AI Chat History
            if (!db.objectStoreNames.contains('offline_chat')) {
                db.createObjectStore('offline_chat', { keyPath: 'id', autoIncrement: true });
            }

            console.log('IndexedDB stores successfully established in Service Worker!');
        };
    });
}

// 1. INSTALL EVENT: Cache static shells & critical pages
self.addEventListener('install', (event) => {
    console.log('[Service Worker] Install Event triggered. Pre-caching static shells...');
    event.waitUntil(
        caches.open(CACHE_NAME)
            .then((cache) => {
                return cache.addAll(STATIC_ASSETS);
            })
            .then(() => {
                return initIndexedDB();
            })
            .then(() => {
                return self.skipWaiting();
            })
    );
});

// 2. ACTIVATE EVENT: Clean up older cache versions
self.addEventListener('activate', (event) => {
    console.log('[Service Worker] Activate Event triggered. Pruning legacy caches...');
    event.waitUntil(
        caches.keys().then((cacheNames) => {
            return Promise.all(
                cacheNames.map((cache) => {
                    if (cache !== CACHE_NAME) {
                        console.log('[Service Worker] Deleting obsolete cache:', cache);
                        return caches.delete(cache);
                    }
                })
            );
        }).then(() => {
            return self.clients.claim();
        })
    );
});

// 3. FETCH EVENT: Cache-First Strategy with Network Fallback
self.addEventListener('fetch', (event) => {
    // Avoid caching non-HTTP requests (like chrome-extension:// or file://)
    if (!event.request.url.startsWith(self.location.origin)) {
        return;
    }

    // Skip caching for POST API requests (handled via background sync queue in IndexedDB)
    if (event.request.method !== 'GET') {
        return;
    }

    event.respondWith(
        caches.match(event.request)
            .then((cachedResponse) => {
                if (cachedResponse) {
                    // Return cached asset immediately, fetch updated asset in background (Stale-While-Revalidate)
                    fetch(event.request).then((networkResponse) => {
                        if (networkResponse.status === 200) {
                            caches.open(CACHE_NAME).then((cache) => {
                                cache.put(event.request, networkResponse);
                            });
                        }
                    }).catch(() => {/* Ignore network update failure offline */});
                    return cachedResponse;
                }

                // If not in cache, fetch from network
                return fetch(event.request)
                    .then((response) => {
                        if (!response || response.status !== 200 || response.type !== 'basic') {
                            return response;
                        }
                        // Put new asset in cache dynamically
                        const responseToCache = response.clone();
                        caches.open(CACHE_NAME).then((cache) => {
                            cache.put(event.request, responseToCache);
                        });
                        return response;
                    })
                    .catch(() => {
                        // Return the custom offline page for navigations
                        if (event.request.mode === 'navigate') {
                            return caches.match(OFFLINE_URL);
                        }
                    });
            })
    );
});

// 4. SYNC EVENT: Automatic Synchronization when Connection Returns
self.addEventListener('sync', (event) => {
    console.log('[Service Worker] Network connection restored. Resolving sync tag:', event.tag);
    if (event.tag === 'sync-offline-bookings' || event.tag === 'sync-offline-jobs') {
        event.waitUntil(
            processOfflineSyncQueue()
        );
    }
});

/**
 * Process offline sync queue by reading unsent items from IndexedDB
 * and transmitting them back to the RuralOS Central Server.
 */
async function processOfflineSyncQueue() {
    try {
        const db = await initIndexedDB();
        const tx = db.transaction('sync_queue', 'readonly');
        const store = tx.objectStore('sync_queue');
        
        const unsentRequests = await new Promise((resolve) => {
            const req = store.getAll();
            req.onsuccess = () => resolve(req.result);
        });

        if (!unsentRequests || unsentRequests.length === 0) {
            console.log('[Service Worker Sync] Queue is empty. Nothing to transmit.');
            return;
        }

        console.log(`[Service Worker Sync] Found ${unsentRequests.length} offline operations to transmit.`);

        for (const item of unsentRequests) {
            try {
                const response = await fetch(item.url, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(item.payload)
                });

                if (response.ok) {
                    // Remove successfully synchronized action from IndexedDB
                    const deleteTx = db.transaction('sync_queue', 'readwrite');
                    deleteTx.objectStore('sync_queue').delete(item.id);
                    console.log('[Service Worker Sync] Successfully synchronized payload item:', item.id);
                }
            } catch (err) {
                console.error('[Service Worker Sync] Failed transmission, keeping in queue:', err);
                break; // Stop processing further to preserve order
            }
        }
    } catch (err) {
        console.error('[Service Worker Sync] Fatal sync error:', err);
    }
}
