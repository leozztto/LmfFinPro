import { beforeEach, describe, expect, it, vi } from 'vitest'
import { importBatchesApi } from './importBatchesApi'

const { httpClient } = vi.hoisted(() => ({
  httpClient: { get: vi.fn(), post: vi.fn(), put: vi.fn(), delete: vi.fn(), postForm: vi.fn() },
}))

vi.mock('@/shared/api/httpClient', () => ({ httpClient }))

describe('importBatchesApi.upload', () => {
  beforeEach(() => {
    httpClient.postForm.mockReset()
  })

  it('builds a FormData with the account id and file, and posts it to /import-batches', () => {
    const file = new File(['a,b,c'], 'extrato.csv', { type: 'text/csv' })

    importBatchesApi.upload({ accountId: 42, file })

    expect(httpClient.postForm).toHaveBeenCalledTimes(1)
    const [path, formData] = httpClient.postForm.mock.calls[0]
    expect(path).toBe('/import-batches')
    expect(formData).toBeInstanceOf(FormData)
    expect((formData as FormData).get('accountId')).toBe('42')
    expect((formData as FormData).get('file')).toBe(file)
  })
})
