import { useCallback, useEffect, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { pushApi } from '../api/pushApi'
import { isPushSupported, needsInstallForPush, urlBase64ToUint8Array } from '../pushSupport'

export type PushStatus =
  | 'loading'
  /** Servidor sem chaves VAPID: a opção nem aparece. */
  | 'disabled'
  | 'unsupported'
  | 'needs-install'
  | 'denied'
  | 'off'
  | 'on'

async function currentSubscription(): Promise<PushSubscription | null> {
  const registration = await navigator.serviceWorker.ready
  return registration.pushManager.getSubscription()
}

export function usePushNotifications() {
  const supported = isPushSupported()
  const config = useQuery({ queryKey: ['push', 'config'], queryFn: pushApi.getConfig, enabled: supported })
  const [subscribed, setSubscribed] = useState<boolean | null>(null)
  const [permission, setPermission] = useState<NotificationPermission>(supported ? Notification.permission : 'default')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!supported) return
    let cancelled = false
    currentSubscription()
      .then((subscription) => !cancelled && setSubscribed(subscription !== null))
      .catch(() => !cancelled && setSubscribed(false))
    return () => {
      cancelled = true
    }
  }, [supported])

  const enable = useCallback(async () => {
    const publicKey = config.data?.publicKey
    if (!publicKey) return
    setBusy(true)
    setError(null)
    try {
      const result = await Notification.requestPermission()
      setPermission(result)
      if (result !== 'granted') return
      const registration = await navigator.serviceWorker.ready
      const subscription = await registration.pushManager.subscribe({
        userVisibleOnly: true,
        applicationServerKey: urlBase64ToUint8Array(publicKey),
      })
      const { endpoint, keys } = subscription.toJSON()
      try {
        await pushApi.subscribe({ endpoint: endpoint ?? subscription.endpoint, p256dh: keys?.p256dh ?? '', auth: keys?.auth ?? '' })
      } catch (apiError) {
        // Sem registro no servidor a inscrição local não serve para nada: desfaz para não divergir.
        await subscription.unsubscribe()
        throw apiError
      }
      setSubscribed(true)
    } catch {
      setError('Não foi possível ativar as notificações neste aparelho.')
    } finally {
      setBusy(false)
    }
  }, [config.data?.publicKey])

  const disable = useCallback(async () => {
    setBusy(true)
    setError(null)
    try {
      const subscription = await currentSubscription()
      if (subscription) {
        await pushApi.unsubscribe(subscription.endpoint)
        await subscription.unsubscribe()
      }
      setSubscribed(false)
    } catch {
      setError('Não foi possível desativar as notificações neste aparelho.')
    } finally {
      setBusy(false)
    }
  }, [])

  let status: PushStatus
  if (!supported) status = needsInstallForPush() ? 'needs-install' : 'unsupported'
  else if (config.isLoading || subscribed === null) status = 'loading'
  else if (!config.data?.enabled) status = 'disabled'
  else if (permission === 'denied') status = 'denied'
  else status = subscribed ? 'on' : 'off'

  return { status, busy, error, enable, disable }
}
