// @vitest-environment jsdom
import { cleanup, fireEvent, render, screen } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import type { AccountDeletionPreview } from '../types'
import { DeleteAccountModal } from './DeleteAccountModal'

const state = vi.hoisted(() => ({
  preview: { data: undefined, isLoading: false, isError: false } as {
    data?: unknown
    isLoading: boolean
    isError: boolean
  },
  remove: { mutate: vi.fn(), reset: vi.fn(), isPending: false, isError: false, error: null as unknown },
  logout: vi.fn(),
  navigate: vi.fn(),
  showToast: vi.fn(),
}))

vi.mock('../hooks/usePrivacy', () => ({
  useDeletionPreview: () => state.preview,
  useDeleteAccount: () => state.remove,
}))
vi.mock('@/shared/auth/AuthContext', () => ({ useAuth: () => ({ logout: state.logout }) }))
vi.mock('@/shared/toast/ToastContext', () => ({ useToast: () => ({ showToast: state.showToast }) }))
vi.mock('react-router-dom', () => ({ useNavigate: () => state.navigate }))

function preview(overrides: Partial<AccountDeletionPreview> = {}): AccountDeletionPreview {
  return {
    canDelete: true,
    blockers: [],
    deletedGroups: [{ id: 1, name: 'Ana', type: 'PERSONAL', accountsBroughtByYou: 0 }],
    leftGroups: [],
    attachmentCount: 0,
    ...overrides,
  }
}

function type(label: string, value: string) {
  fireEvent.change(screen.getByLabelText(label), { target: { value } })
}

function submitButton() {
  return screen.getByRole('button', { name: 'Excluir definitivamente' }) as HTMLButtonElement
}

describe('DeleteAccountModal', () => {
  beforeEach(() => {
    state.preview = { data: preview(), isLoading: false, isError: false }
    state.remove = { mutate: vi.fn(), reset: vi.fn(), isPending: false, isError: false, error: null }
    state.logout = vi.fn()
    state.navigate = vi.fn()
    state.showToast = vi.fn()
  })
  afterEach(cleanup)

  it('renders nothing while closed', () => {
    render(<DeleteAccountModal open={false} onClose={vi.fn()} />)

    expect(screen.queryByRole('dialog')).toBeNull()
  })

  it('keeps the delete button disabled until password and confirmation word are filled', () => {
    render(<DeleteAccountModal open onClose={vi.fn()} />)
    expect(submitButton().disabled).toBe(true)

    type('Sua senha', 'senha12345')
    expect(submitButton().disabled).toBe(true)

    type('Digite EXCLUIR para confirmar', 'EXCLUIR')
    expect(submitButton().disabled).toBe(false)
  })

  it('lists what will be deleted and the attachments', () => {
    state.preview = {
      data: preview({
        attachmentCount: 3,
        deletedGroups: [
          { id: 1, name: 'Ana', type: 'PERSONAL', accountsBroughtByYou: 0 },
          { id: 2, name: 'Só eu', type: 'SHARED', accountsBroughtByYou: 0 },
        ],
      }),
      isLoading: false,
      isError: false,
    }

    render(<DeleteAccountModal open onClose={vi.fn()} />)

    expect(screen.getByText(/Todo o seu espaço pessoal/)).toBeTruthy()
    expect(screen.getByText(/O grupo "Só eu"/)).toBeTruthy()
    expect(screen.getByText(/3 anexos de transações/)).toBeTruthy()
  })

  it('explains what stays behind in the groups the person leaves', () => {
    state.preview = {
      data: preview({ leftGroups: [{ id: 7, name: 'Casa', type: 'SHARED', accountsBroughtByYou: 2 }] }),
      isLoading: false,
      isError: false,
    }

    render(<DeleteAccountModal open onClose={vi.fn()} />)

    expect(screen.getByText(/"Casa": 2 contas que você trouxe ficam no grupo/)).toBeTruthy()
  })

  it('shows the blocker and never enables the button, even with everything filled', () => {
    state.preview = {
      data: preview({
        canDelete: false,
        blockers: ['Você é o dono do grupo "Casa", que tem outros membros.'],
      }),
      isLoading: false,
      isError: false,
    }
    render(<DeleteAccountModal open onClose={vi.fn()} />)

    type('Sua senha', 'senha12345')
    type('Digite EXCLUIR para confirmar', 'EXCLUIR')

    expect(screen.getByText(/dono do grupo "Casa"/)).toBeTruthy()
    expect(submitButton().disabled).toBe(true)
  })

  it('does not enable the button before the preview arrives', () => {
    state.preview = { data: undefined, isLoading: true, isError: false }
    render(<DeleteAccountModal open onClose={vi.fn()} />)

    type('Sua senha', 'senha12345')
    type('Digite EXCLUIR para confirmar', 'EXCLUIR')

    expect(submitButton().disabled).toBe(true)
  })

  it('sends the password and, once deleted, logs out and goes to the login screen', () => {
    state.remove.mutate = vi.fn((_password: string, options: { onSuccess: () => void }) => options.onSuccess())
    render(<DeleteAccountModal open onClose={vi.fn()} />)
    type('Sua senha', 'senha12345')
    type('Digite EXCLUIR para confirmar', 'excluir')

    fireEvent.click(submitButton())

    expect(state.remove.mutate).toHaveBeenCalledWith('senha12345', expect.any(Object))
    expect(state.logout).toHaveBeenCalledTimes(1)
    expect(state.navigate).toHaveBeenCalledWith('/login', { replace: true })
    expect(state.showToast).toHaveBeenCalledWith('Sua conta foi excluída.', 'success')
  })

  it('does not log out when the server refuses the deletion', () => {
    state.remove.mutate = vi.fn()
    state.remove.isError = true
    state.remove.error = new Error('Senha incorreta')
    render(<DeleteAccountModal open onClose={vi.fn()} />)

    expect(state.logout).not.toHaveBeenCalled()
    expect(screen.getByRole('alert').textContent).toContain('Não foi possível excluir a conta.')
  })
})
