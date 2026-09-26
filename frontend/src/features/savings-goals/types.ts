export type SavingsGoalType = 'EMERGENCY_FUND' | 'TAX_RESERVE' | 'VACATION' | 'OTHER'

export type ContributionType = 'DEPOSIT' | 'WITHDRAWAL'

/** Valores calculados (guardado, quanto falta, valor mensal e sugestão) vêm prontos do backend. */
export interface SavingsGoal {
  id: number
  name: string
  type: SavingsGoalType
  targetAmount: number
  deadline: string | null
  /** Fração das receitas a separar (0.06 = 6%). */
  incomeRate: number | null
  savedAmount: number
  remainingAmount: number
  /** Quanto guardar por mês até o prazo; null sem prazo ou com a meta atingida. */
  monthlyNeeded: number | null
  /** Receitas já recebidas (pagas) no mês atual. */
  monthPaidIncome: number
  /** Quanto separar agora pelo percentual da meta; null quando a meta não tem percentual. */
  suggestedContribution: number | null
}

export interface SavingsGoalInput {
  name: string
  type: SavingsGoalType
  targetAmount: number
  deadline: string | null
  incomeRate: number | null
}

export interface GoalContribution {
  id: number
  goalId: number
  type: ContributionType
  amount: number
  contributionDate: string
  note: string | null
}

export interface GoalContributionInput {
  type: ContributionType
  amount: number
  contributionDate: string
  note: string | null
}
