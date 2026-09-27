import { useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { Button, Modal, Tabs } from '@/shared/ui'
import { PlusIcon } from '@/shared/ui/icons'
import { TagsManager } from '@/features/tags/components/TagsManager'
import { CategoryForm } from './CategoryForm'
import { CategoryList } from './CategoryList'

const TAB_PARAM = 'aba'
const TAGS_TAB = 'tags'

/** Categorias e tags: as duas formas de classificar transações, em abas (`/categorias?aba=tags`). */
export function CategoriesPage() {
  const [isModalOpen, setIsModalOpen] = useState(false)
  const [searchParams, setSearchParams] = useSearchParams()
  const activeTab = searchParams.get(TAB_PARAM) === TAGS_TAB ? TAGS_TAB : 'categorias'

  function handleTabChange(tabId: string) {
    setSearchParams(tabId === TAGS_TAB ? { [TAB_PARAM]: TAGS_TAB } : {}, { replace: true })
  }

  return (
    <div className="space-y-6">
      <div className="flex items-start justify-between gap-3">
        <div>
          <h2 className="text-lg font-semibold text-zinc-800 dark:text-zinc-100">Categorias e tags</h2>
          <p className="hidden text-sm text-zinc-500 dark:text-zinc-400 sm:block">
            Categorias padrão do sistema e as suas próprias, e as tags que você usa para classificar transações.
          </p>
        </div>
        {activeTab !== TAGS_TAB && (
          <Button
            onClick={() => setIsModalOpen(true)}
            aria-label="Nova categoria"
            title="Nova categoria"
            className="px-3"
          >
            <PlusIcon />
          </Button>
        )}
      </div>

      <Modal open={isModalOpen} onClose={() => setIsModalOpen(false)} title="Nova categoria">
        <CategoryForm onSuccess={() => setIsModalOpen(false)} />
      </Modal>

      <Tabs
        activeTabId={activeTab}
        onTabChange={handleTabChange}
        tabs={[
          { id: 'categorias', label: 'Categorias', content: <CategoryList /> },
          { id: TAGS_TAB, label: 'Tags', content: <TagsManager /> },
        ]}
      />
    </div>
  )
}
