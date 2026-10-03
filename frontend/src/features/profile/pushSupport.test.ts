import { describe, expect, it } from 'vitest'
import { urlBase64ToUint8Array } from './pushSupport'

describe('urlBase64ToUint8Array', () => {
  it('converte base64url (com - e _, sem padding) nos bytes originais', () => {
    // bytes 0xfb 0xff 0xfe => "+//+" em base64 => "-__-" em base64url
    expect(Array.from(urlBase64ToUint8Array('-__-'))).toEqual([0xfb, 0xff, 0xfe])
  })

  it('completa o padding que o base64url omite', () => {
    // "hi" => "aGk=" em base64 => "aGk" em base64url
    expect(Array.from(urlBase64ToUint8Array('aGk'))).toEqual([0x68, 0x69])
  })
})
