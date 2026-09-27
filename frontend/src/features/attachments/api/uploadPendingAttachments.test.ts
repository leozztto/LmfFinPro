import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError } from '@/shared/api/httpClient'
import type { PendingAttachment } from '../types'
import { attachmentsApi } from './attachmentsApi'
import { uploadPendingAttachments } from './uploadPendingAttachments'

vi.mock('./attachmentsApi', () => ({ attachmentsApi: { upload: vi.fn() } }))

const upload = vi.mocked(attachmentsApi.upload)

function pending(key: number, name: string): PendingAttachment {
  return { key, file: new File(['x'], name, { type: 'application/pdf' }), documentType: 'INVOICE' }
}

describe('uploadPendingAttachments', () => {
  beforeEach(() => upload.mockReset())

  it('sends every file to the transaction with its document type', async () => {
    upload.mockResolvedValue({} as never)
    const items = [pending(1, 'nota.pdf'), pending(2, 'recibo.pdf')]

    const result = await uploadPendingAttachments(42, items)

    expect(result).toEqual({ uploadedCount: 2, failed: [] })
    expect(upload).toHaveBeenNthCalledWith(1, 42, items[0].file, 'INVOICE')
    expect(upload).toHaveBeenNthCalledWith(2, 42, items[1].file, 'INVOICE')
  })

  it('keeps going after a failure and returns the failed file with the backend message', async () => {
    upload
      .mockRejectedValueOnce(new ApiError(400, 'O conteúdo do arquivo não é um PDF válido.'))
      .mockResolvedValueOnce({} as never)

    const result = await uploadPendingAttachments(42, [pending(1, 'falso.pdf'), pending(2, 'ok.pdf')])

    expect(result.uploadedCount).toBe(1)
    expect(result.failed).toHaveLength(1)
    expect(result.failed[0].file.name).toBe('falso.pdf')
    expect(result.failed[0].error).toBe('O conteúdo do arquivo não é um PDF válido.')
  })
})
