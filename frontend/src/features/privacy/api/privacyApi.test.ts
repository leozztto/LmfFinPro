import { beforeEach, describe, expect, it, vi } from 'vitest'
import { privacyApi } from './privacyApi'

const { httpClient } = vi.hoisted(() => ({
  httpClient: { get: vi.fn(), delete: vi.fn(), getBlob: vi.fn() },
}))

vi.mock('@/shared/api/httpClient', () => ({ httpClient }))

describe('privacyApi', () => {
  beforeEach(() => {
    httpClient.get.mockReset()
    httpClient.delete.mockReset()
    httpClient.getBlob.mockReset()
  })

  it('downloads the export as a blob from /privacy/export', () => {
    privacyApi.exportData()

    expect(httpClient.getBlob).toHaveBeenCalledWith('/privacy/export')
  })

  it('asks the server what the deletion would do', () => {
    privacyApi.getDeletionPreview()

    expect(httpClient.get).toHaveBeenCalledWith('/privacy/account-deletion-preview')
  })

  it('sends the password in the body of DELETE /privacy/account', () => {
    privacyApi.deleteAccount('senha12345')

    expect(httpClient.delete).toHaveBeenCalledWith('/privacy/account', { password: 'senha12345' })
  })
})
