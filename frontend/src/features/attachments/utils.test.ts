import { describe, expect, it } from 'vitest'
import {
  MAX_ATTACHMENT_SIZE_BYTES,
  MAX_ATTACHMENTS_PER_TRANSACTION,
  formatFileSize,
  splitValidFiles,
  validateAttachmentFile,
} from './utils'

describe('validateAttachmentFile', () => {
  it('accepts PDFs and images within the limits', () => {
    expect(validateAttachmentFile({ size: 1000, type: 'application/pdf' }, 0)).toBeNull()
    expect(validateAttachmentFile({ size: 1000, type: 'image/webp' }, 9)).toBeNull()
  })

  it('leaves files without a browser type for the backend to check', () => {
    expect(validateAttachmentFile({ size: 1000, type: '' }, 0)).toBeNull()
  })

  it('rejects empty, too large, unsupported and over-the-limit files', () => {
    expect(validateAttachmentFile({ size: 0, type: 'application/pdf' }, 0)).toContain('vazio')
    expect(validateAttachmentFile({ size: MAX_ATTACHMENT_SIZE_BYTES + 1, type: 'application/pdf' }, 0)).toContain(
      '10 MB',
    )
    expect(validateAttachmentFile({ size: 1000, type: 'application/zip' }, 0)).toContain('Formato')
    expect(validateAttachmentFile({ size: 1000, type: 'application/pdf' }, 10)).toContain('máximo')
  })
})

describe('formatFileSize', () => {
  it('uses B, KB or MB', () => {
    expect(formatFileSize(500)).toBe('500 B')
    expect(formatFileSize(2048)).toBe('2 KB')
    expect(formatFileSize(3.5 * 1024 * 1024)).toBe('3,5 MB')
  })
})

describe('splitValidFiles', () => {
  const pdf = (name: string) => new File(['conteudo'], name, { type: 'application/pdf' })

  it('queues the valid files and explains why the others were refused', () => {
    const zip = new File(['x'], 'fotos.zip', { type: 'application/zip' })

    const { accepted, rejected } = splitValidFiles([pdf('nota.pdf'), zip], 0)

    expect(accepted.map((file) => file.name)).toEqual(['nota.pdf'])
    expect(rejected).toEqual([expect.stringMatching(/^fotos\.zip: Formato/)])
  })

  it('stops accepting once the transaction reaches the attachment limit', () => {
    const { accepted, rejected } = splitValidFiles(
      [pdf('a.pdf'), pdf('b.pdf'), pdf('c.pdf')],
      MAX_ATTACHMENTS_PER_TRANSACTION - 2,
    )

    expect(accepted.map((file) => file.name)).toEqual(['a.pdf', 'b.pdf'])
    expect(rejected).toEqual([expect.stringContaining('c.pdf')])
  })
})
