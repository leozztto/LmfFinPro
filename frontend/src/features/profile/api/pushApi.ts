import { httpClient } from '@/shared/api/httpClient'
import type { PushConfig, PushSubscriptionInput } from '../types'

export const pushApi = {
  getConfig: () => httpClient.get<PushConfig>('/push/config'),
  subscribe: (input: PushSubscriptionInput) => httpClient.post<void, PushSubscriptionInput>('/push/subscriptions', input),
  unsubscribe: (endpoint: string) => httpClient.delete('/push/subscriptions', { endpoint }),
}
