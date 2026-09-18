import { useState } from 'react'

interface ExpandableTextProps {
  text: string
  className?: string
  maxLength?: number
}

export function ExpandableText({ text, className = '', maxLength = 60 }: ExpandableTextProps) {
  const [expanded, setExpanded] = useState(false)
  const isLong = text.length > maxLength

  return (
    <span className="inline-flex min-w-0 flex-wrap items-baseline gap-1">
      <span className={`break-words ${isLong && !expanded ? 'line-clamp-1' : ''} ${className}`}>{text}</span>
      {isLong && (
        <button
          type="button"
          onClick={() => setExpanded((value) => !value)}
          className="shrink-0 text-xs font-medium text-primary-600 hover:underline dark:text-primary-400"
        >
          {expanded ? 'mostrar menos' : 'mostrar mais'}
        </button>
      )}
    </span>
  )
}
