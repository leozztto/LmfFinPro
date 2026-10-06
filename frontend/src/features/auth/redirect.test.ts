import { describe, expect, it } from 'vitest'
import { safeInternalPath } from './redirect'

describe('safeInternalPath', () => {
  it('accepts internal paths, with query string', () => {
    expect(safeInternalPath('/')).toBe('/')
    expect(safeInternalPath('/convite?token=abc')).toBe('/convite?token=abc')
  })

  it('rejects absolute URLs and protocol-relative paths', () => {
    expect(safeInternalPath('https://evil.test')).toBeNull()
    expect(safeInternalPath('//evil.test')).toBeNull()
    expect(safeInternalPath('/\\evil.test')).toBeNull()
  })

  it('rejects anything that is not a non-empty path string', () => {
    expect(safeInternalPath(undefined)).toBeNull()
    expect(safeInternalPath(null)).toBeNull()
    expect(safeInternalPath(42)).toBeNull()
    expect(safeInternalPath('')).toBeNull()
    expect(safeInternalPath('convite')).toBeNull()
  })
})
