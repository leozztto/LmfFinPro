import { useRef } from 'react'
import { Input } from './Input'

const HEX_COLOR_PATTERN = /^#[0-9a-fA-F]{6}$/

interface ColorInputProps {
  id: string
  value: string
  onChange: (value: string) => void
  onBlur?: () => void
  placeholder?: string
}

/** Input de texto para hex color, com um botão-amostra embutido que abre o seletor de cor nativo. */
export function ColorInput({ id, value, onChange, onBlur, placeholder }: ColorInputProps) {
  const colorPickerRef = useRef<HTMLInputElement>(null)
  const swatchColor = HEX_COLOR_PATTERN.test(value) ? value : '#9ca3af'

  return (
    <div className="relative">
      <button
        type="button"
        aria-label="Escolher cor"
        onClick={() => colorPickerRef.current?.click()}
        className="absolute left-2.5 top-1/2 h-4 w-4 -translate-y-1/2 rounded-full border border-zinc-300 shadow-sm dark:border-zinc-600"
        style={{ backgroundColor: swatchColor }}
      />
      <Input
        id={id}
        type="text"
        className="pl-9"
        placeholder={placeholder}
        value={value}
        onChange={(event) => onChange(event.target.value)}
        onBlur={onBlur}
      />
      <input
        ref={colorPickerRef}
        type="color"
        aria-hidden="true"
        tabIndex={-1}
        value={swatchColor}
        onChange={(event) => onChange(event.target.value)}
        className="absolute h-0 w-0 opacity-0"
      />
    </div>
  )
}
