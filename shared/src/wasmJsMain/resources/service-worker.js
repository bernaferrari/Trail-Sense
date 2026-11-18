importScripts("offline-assets.js");

const cacheName = "trail-sense-" + self.TRAIL_SENSE_BUILD;
const scope = self.registration.scope;

self.addEventListener("install", event => {
    event.waitUntil((async () => {
        const cache = await caches.open(cacheName);
        await cache.addAll(self.TRAIL_SENSE_ASSETS.map(path => new URL(path, scope)));
        await self.skipWaiting();
    })());
});

self.addEventListener("activate", event => {
    event.waitUntil((async () => {
        for (const name of await caches.keys()) {
            if (name.startsWith("trail-sense-") && name !== cacheName) await caches.delete(name);
        }
        await self.clients.claim();
    })());
});

self.addEventListener("fetch", event => {
    if (event.request.method !== "GET" || !event.request.url.startsWith(scope)) return;
    event.respondWith((async () => {
        const cache = await caches.open(cacheName);
        const request = event.request.mode === "navigate" ? new URL("index.html", scope) : event.request;
        return await cache.match(request) ?? fetch(event.request);
    })());
});
