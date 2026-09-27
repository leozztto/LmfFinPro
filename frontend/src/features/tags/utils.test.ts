import { describe, expect, it } from 'vitest'
import { MAX_TAG_NAME_LENGTH, normalizeTagName, tagLabel, validateTagName } from './utils'

describe('normalizeTagName', () => {
  it('treats different spellings as the same tag, like the backend', () => {
    expect(normalizeTagName('#Site Acme')).toBe('site-acme')
    expect(normalizeTagName('  SITE-ACME ')).toBe('site-acme')
    expect(normalizeTagName('## site   acme')).toBe('site-acme')
    expect(normalizeTagName('#Dedutível')).toBe('dedutível')
  })
})

describe('validateTagName', () => {
  it('accepts letters with accents, digits, hyphen and underscore', () => {
    expect(validateTagName('dedutível')).toBeNull()
    expect(validateTagName('ir_2026')).toBeNull()
  })

  it('rejects empty, too long and invalid names', () => {
    expect(validateTagName('')).toContain('Informe')
    expect(validateTagName('a'.repeat(MAX_TAG_NAME_LENGTH + 1))).toContain(String(MAX_TAG_NAME_LENGTH))
    expect(validateTagName('site/acme')).toContain('letras')
  })
})

describe('tagLabel', () => {
  it('shows the tag with the hash sign', () => {
    expect(tagLabel('site-acme')).toBe('#site-acme')
  })
})
