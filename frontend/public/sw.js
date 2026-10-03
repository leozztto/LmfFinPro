/* Service worker do FinPro: só notificações push.
 * Sem cache offline de propósito — são dados financeiros e uma tela velha seria pior do que nenhuma. */

self.addEventListener('install', () => self.skipWaiting())
self.addEventListener('activate', (event) => event.waitUntil(self.clients.claim()))

self.addEventListener('push', (event) => {
  let data = {}
  try {
    data = event.data ? event.data.json() : {}
  } catch (error) {
    data = {}
  }
  const title = data.title || 'FinPro'
  event.waitUntil(
    self.registration.showNotification(title, {
      body: data.body || '',
      icon: '/icons/icon-192.png',
      badge: '/icons/icon-192.png',
      // Mesma tag: um resumo novo substitui o anterior em vez de empilhar.
      tag: 'finpro-alertas',
      data: { url: data.url || '/' },
    }),
  )
})

self.addEventListener('notificationclick', (event) => {
  event.notification.close()
  const target = new URL((event.notification.data && event.notification.data.url) || '/', self.location.origin)
  event.waitUntil(
    self.clients.matchAll({ type: 'window', includeUncontrolled: true }).then((windows) => {
      const open = windows.find((client) => new URL(client.url).origin === target.origin)
      if (open) {
        return open.focus().then(() => ('navigate' in open ? open.navigate(target.href) : undefined))
      }
      return self.clients.openWindow(target.href)
    }),
  )
})
