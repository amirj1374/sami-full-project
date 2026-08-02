/* SAMI PWA service worker: application shell only; business APIs are network-only. */
const VERSION = 'sami-shell-v2'
const STATIC_CACHE = `${VERSION}-static`
const SHELL_CACHE = `${VERSION}-shell`
const SHELL = ['/', '/index.html', '/manifest.webmanifest', '/icons/sami-app-icon.svg']

self.addEventListener('install', (event) => {
  event.waitUntil(caches.open(SHELL_CACHE).then((cache) => cache.addAll(SHELL)))
})

self.addEventListener('activate', (event) => {
  event.waitUntil(
    caches.keys()
      .then((keys) => Promise.all(keys.filter((key) => !key.startsWith(VERSION)).map((key) => caches.delete(key))))
      .then(() => self.clients.claim()),
  )
})

self.addEventListener('message', (event) => {
  if (event.data?.type === 'SKIP_WAITING') self.skipWaiting()
})

function sameOriginPath(value, fallback = '/') {
  if (typeof value !== 'string' || !value.startsWith('/') || value.startsWith('//')) return fallback
  try {
    const url = new URL(value, self.location.origin)
    return url.origin === self.location.origin ? `${url.pathname}${url.search}${url.hash}` : fallback
  } catch {
    return fallback
  }
}

function sameOriginRoute(value) {
  const route = sameOriginPath(value)
  return route === '/api' || route.startsWith('/api/') ? '/' : route
}

function sameOriginAsset(value, fallback) {
  if (typeof value !== 'string') return fallback
  const asset = sameOriginPath(value, fallback)
  return asset.startsWith('/icons/') || asset.startsWith('/assets/') ? asset : fallback
}

function notificationActions(value) {
  if (!Array.isArray(value)) return undefined
  const actions = value.slice(0, 2).flatMap((item) => {
    if (
      !item
      || typeof item.action !== 'string'
      || !/^[a-zA-Z0-9._-]{1,64}$/.test(item.action)
      || typeof item.title !== 'string'
      || !item.title.trim()
    ) {
      return []
    }
    return [{
      action: item.action,
      title: item.title.trim().slice(0, 80),
      icon: sameOriginAsset(item.icon, undefined),
    }]
  })
  return actions.length ? actions : undefined
}

self.addEventListener('push', (event) => {
  if (!event.data) return
  let payload
  try {
    payload = event.data.json()
  } catch {
    return
  }
  if (!payload || typeof payload.title !== 'string' || !payload.title.trim()) return

  const url = sameOriginRoute(payload.url)
  const options = {
    body: typeof payload.body === 'string' ? payload.body : undefined,
    icon: sameOriginAsset(payload.icon, '/icons/sami-192.png'),
    badge: sameOriginAsset(payload.badge, '/icons/sami-32.png'),
    actions: notificationActions(payload.actions),
    data: {
      url,
      type: typeof payload.type === 'string' ? payload.type : undefined,
      data: payload.data && typeof payload.data === 'object' ? payload.data : undefined,
    },
  }
  event.waitUntil(self.registration.showNotification(payload.title.trim(), options))
})

self.addEventListener('notificationclick', (event) => {
  event.notification.close()
  const url = sameOriginRoute(event.notification.data?.url)
  event.waitUntil(
    self.clients.matchAll({ type: 'window', includeUncontrolled: true }).then(async (windows) => {
      const client = windows.find((candidate) => new URL(candidate.url).origin === self.location.origin)
      if (client) {
        client.postMessage({
          type: 'SAMI_PUSH_NOTIFICATION_CLICK',
          url,
          action: event.action || undefined,
        })
        return client.focus()
      }
      return self.clients.openWindow ? self.clients.openWindow(url) : undefined
    }),
  )
})

self.addEventListener('pushsubscriptionchange', (event) => {
  event.waitUntil(
    self.clients.matchAll({ type: 'window', includeUncontrolled: true }).then((windows) => {
      for (const client of windows) {
        client.postMessage({ type: 'SAMI_PUSH_SUBSCRIPTION_CHANGED' })
      }
    }),
  )
})

self.addEventListener('fetch', (event) => {
  const request = event.request
  if (request.method !== 'GET') return
  const url = new URL(request.url)
  if (url.origin !== self.location.origin || url.pathname === '/api' || url.pathname.startsWith('/api/')) return

  if (request.mode === 'navigate') {
    event.respondWith(
      fetch(request)
        .then((response) => {
          if (response.ok) {
            const copy = response.clone()
            caches.open(SHELL_CACHE).then((cache) => cache.put('/index.html', copy))
          }
          return response
        })
        .catch(() => caches.match('/index.html').then((response) => response || Response.error())),
    )
    return
  }

  if (
    url.pathname.startsWith('/assets/') ||
    url.pathname.startsWith('/icons/') ||
    /\.(?:css|js|woff2?|ttf|svg|png|webp|ico)$/.test(url.pathname)
  ) {
    event.respondWith(
      caches.match(request).then((cached) => cached || fetch(request).then((response) => {
        if (!response.ok || response.type !== 'basic') return response
        const copy = response.clone()
        caches.open(STATIC_CACHE).then((cache) => cache.put(request, copy))
        return response
      })),
    )
  }
})
