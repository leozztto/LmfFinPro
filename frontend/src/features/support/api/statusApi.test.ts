import { describe, expect, it, vi } from 'vitest'
import { statusApi } from './statusApi'

const { httpClient } = vi.hoisted(() => ({ httpClient: { get: vi.fn() } }))

vi.mock('@/shared/api/httpClient', () => ({ httpClient }))

describe('statusApi', () => {
  it('reads the platform status from the public endpoint', () => {
    statusApi.getStatus()

    expect(httpClient.get).toHaveBeenCalledWith('/status')
  })
})
