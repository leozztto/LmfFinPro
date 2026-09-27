import { describe, expect, it } from 'vitest'
import { MAX_ATTACHMENT_SIZE_BYTES, formatFileSize, validateAttachmentFile } from './utils'

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
