import { beforeEach, describe, expect, it, vi } from 'vitest'
import { legalApi } from './legalApi'

const { httpClient } = vi.hoisted(() => ({ httpClient: { get: vi.fn(), post: vi.fn() } }))

vi.mock('@/shared/api/httpClient', () => ({ httpClient }))

describe('legalApi', () => {
  beforeEach(() => {
    httpClient.get.mockReset()
    httpClient.post.mockReset()
  })

  it('reads the current document versions from the public endpoint', () => {
    legalApi.getVersions()

    expect(httpClient.get).toHaveBeenCalledWith('/legal/versions')
  })

  it('reads the consent status of the logged user', () => {
    legalApi.getConsentStatus()

    expect(httpClient.get).toHaveBeenCalledWith('/consents')
  })

  it('posts back exactly the versions the person saw', () => {
    const versions = { termsVersion: '2026-10-07', privacyVersion: '2026-10-07' }

    legalApi.acceptConsent(versions)

    expect(httpClient.post).toHaveBeenCalledWith('/consents', versions)
  })
})
