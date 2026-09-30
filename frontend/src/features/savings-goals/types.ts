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
  /** Quando ligado, o sistema separa a sugestão automaticamente uma vez por dia. */
  autoContribute: boolean
  /** Conta reserva onde o dinheiro guardado fica de fato — fixa depois de criada. */
  accountId: number
  /** Conta de onde o aporte sai (e para onde o resgate volta) — fixa depois de criada. */
  fundingAccountId: number
}

/** Conta reserva e conta de origem só são escolhidas na criação — fixas depois disso. */
export interface SavingsGoalCreateInput {
  name: string
  type: SavingsGoalType
  targetAmount: number
  deadline: string | null
  incomeRate: number | null
  autoContribute: boolean
  accountId: number
  fundingAccountId: number
}

export interface SavingsGoalUpdateInput {
  name: string
  type: SavingsGoalType
  targetAmount: number
  deadline: string | null
  incomeRate: number | null
  autoContribute: boolean
}

export interface GoalContribution {
  id: number
  goalId: number
  type: ContributionType
  amount: number
  contributionDate: string
  note: string | null
  /** Id da transferência real por trás do aporte/resgate. */
  transferId: number
}

export interface GoalContributionInput {
  type: ContributionType
  amount: number
  contributionDate: string
  note: string | null
}
