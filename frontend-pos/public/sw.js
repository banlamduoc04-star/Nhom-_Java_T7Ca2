// Service worker POS (FR-POS-01: cai dat nhu ung dung, chay khi mat mang).
// Trang HTML: lay tu mang truoc, mat mang moi dung ban da luu -> moi lan deploy la thay giao dien moi ngay.
// File trong /assets/ co hash trong ten (Vite) nen khong bao gio doi noi dung -> lay tu cache truoc.
// Doi CACHE_NAME khi doi cach cache de xoa cache cu cua ban truoc.
const CACHE_NAME = 'oism-pos-v2'
const SHELL = ['/', '/manifest.webmanifest', '/icon.svg', '/favicon.svg']

self.addEventListener('install', (event) => {
  event.waitUntil(caches.open(CACHE_NAME).then((cache) => cache.addAll(SHELL)))
  self.skipWaiting()
})

self.addEventListener('activate', (event) => {
  event.waitUntil(
    caches.keys()
      .then((keys) => Promise.all(keys.filter((k) => k !== CACHE_NAME).map((k) => caches.delete(k))))
      .then(() => self.clients.claim()),
  )
})

self.addEventListener('fetch', (event) => {
  const request = event.request
  const url = new URL(request.url)
  // Chi xu ly GET cung origin; khong bao gio cache API
  if (request.method !== 'GET' || url.origin !== self.location.origin || url.pathname.startsWith('/api/')) {
    return
  }

  if (request.mode === 'navigate') {
    event.respondWith(
      fetch(request)
        .then((response) => {
          const copy = response.clone()
          caches.open(CACHE_NAME).then((cache) => cache.put('/', copy))
          return response
        })
        .catch(() => caches.match('/')),
    )
    return
  }

  if (url.pathname.startsWith('/assets/')) {
    event.respondWith(
      caches.match(request).then((cached) => cached || fetch(request).then((response) => {
        if (response.ok) {
          const copy = response.clone()
          caches.open(CACHE_NAME).then((cache) => cache.put(request, copy))
        }
        return response
      })),
    )
    return
  }

  // Icon, manifest...: mang truoc, mat mang thi dung cache
  event.respondWith(fetch(request).catch(() => caches.match(request)))
})
