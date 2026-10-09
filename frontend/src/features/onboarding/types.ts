export type OnboardingStepId =
  | 'ACCOUNT_OPEN'
  | 'ACCOUNT_FILL'
  | 'ACCOUNT_SAVE'
  | 'TRANSACTION_FILL'
  | 'TRANSACTION_SAVE'
  | 'IMPORT_PICK'
  | 'IMPORT_RESULT'
  | 'BUDGET_FILL'
  | 'BUDGET_SAVE'
  | 'RECURRING_FILL'
  | 'RECURRING_SAVE'
  | 'CALENDAR_VIEW'
  | 'DASHBOARD_VIEW'
  | 'REPORTS_VIEW'

export interface OnboardingStep {
  id: OnboardingStepId
  /** A pessoa já passou por este passo do guia. */
  done: boolean
}

export interface OnboardingProgress {
  steps: OnboardingStep[]
  completedCount: number
  totalCount: number
  /** Todos os passos vistos. */
  completed: boolean
  /** A pessoa dispensou o guia. */
  dismissed: boolean
  activationEmailsEnabled: boolean
}
