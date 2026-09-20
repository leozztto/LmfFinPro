import { useState, type ReactNode } from 'react'

export interface TabItem {
  id: string
  label: string
  content: ReactNode
}

interface TabsProps {
  tabs: TabItem[]
  defaultTabId?: string
}

export function Tabs({ tabs, defaultTabId }: TabsProps) {
  const [activeTabId, setActiveTabId] = useState(defaultTabId ?? tabs[0]?.id)
  const activeTab = tabs.find((tab) => tab.id === activeTabId) ?? tabs[0]

  return (
    <div>
      <div role="tablist" className="flex gap-1 border-b border-zinc-200 dark:border-zinc-700">
        {tabs.map((tab) => (
          <button
            key={tab.id}
            type="button"
            role="tab"
            aria-selected={tab.id === activeTab?.id}
            onClick={() => setActiveTabId(tab.id)}
            className={`-mb-px border-b-2 px-4 py-2 text-sm font-medium transition-colors ${
              tab.id === activeTab?.id
                ? 'border-primary-600 text-primary-700 dark:border-primary-400 dark:text-primary-200'
                : 'border-transparent text-zinc-500 hover:text-zinc-700 dark:text-zinc-400 dark:hover:text-zinc-200'
            }`}
          >
            {tab.label}
          </button>
        ))}
      </div>

      <div className="mt-6">{activeTab?.content}</div>
    </div>
  )
}
