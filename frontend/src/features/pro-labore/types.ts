export type ProLaboreCalculationBase = 'MONTH_INCOME' | 'CURRENT_BALANCE'

export type ProLaboreTaxMode = 'AUTOMATIC' | 'MANUAL'

export type ProLaboreWithholdingMode = 'AUTOMATIC' | 'ENABLED' | 'DISABLED'

/** Frações (0.10 = 10%), como o backend guarda. */
export interface ProLaboreSettings {
  calculationBase: ProLaboreCalculationBase
  cashCushionMonths: number
  reserveRate: number
  taxMode: ProLaboreTaxMode
  manualTaxRate: number | null
  /** Pró-labore fixo mensal (bruto). */
  fixedAmount: number | null
  withholdingMode: ProLaboreWithholdingMode
  /** INSS patronal; null = automático pelo regime. */
  employerInssRate: number | null
}

/** Bruto, encargos e líquido do pró-labore do mês. */
export interface ProLaborePayroll {
  gross: number
  employeeInss: number
  irrf: number
  employerInss: number
  net: number
  /** Guias a pagar pela empresa: INSS do sócio + IRRF + INSS patronal. */
  taxesToCollect: number
}

export interface ProLaboreWithdrawal {
  transferId: number
  date: string
  amount: number
  fromAccountName: string
  toAccountName: string
}

/** Despesa de conta PJ que entrou em `monthBusinessExpenses` (o total da lista bate com ele). */
export interface ProLaboreBusinessExpense {
  transactionId: number
  date: string
  description: string
  amount: number
  paid: boolean
  /** Pendente com data anterior a hoje. */
  overdue: boolean
  accountName: string
  categoryName: string | null
}

/** Todas as parcelas vêm calculadas do backend; a tela só explica o número. */
export interface ProLaboreSummary {
  hasBusinessAccounts: boolean
  hasPersonalAccounts: boolean
  settings: ProLaboreSettings
  /** Líquido máximo do mês menos o já retirado: quanto ainda dá para transferir para a conta PF. */
  availableToWithdraw: number
  /** Valor pré-preenchido no "Pagar pró-labore": o que falta do fixo (limitado ao disponível) ou o disponível. */
  suggestedPayment: number
  /** Resultado da conta antes do limite de saldo (pode ser negativo). */
  calculatedAmount: number
  /** Teto pelo saldo na base de receitas; null na base de saldo. */
  balanceCap: number | null
  cappedByBalance: boolean
  /** Quanto a empresa pode gastar com o pró-labore no mês (bruto + INSS patronal). */
  monthBudget: number
  businessBalance: number
  monthBusinessIncome: number
  monthBusinessExpenses: number
  pendingBusinessExpenses: number
  /** Fração (0.06 = 6%). */
  taxRate: number
  taxOnMonthIncome: number
  taxReserveSaved: number
  taxReserve: number
  reserve: number
  averageMonthlyBusinessExpense: number
  cashCushion: number
  withholdingApplied: boolean
  employerInssRate: number
  payroll: ProLaborePayroll
  /** Líquido do fixo menos o já retirado; null sem valor fixo. */
  fixedRemaining: number | null
  fixedCovered: boolean
  withdrawnThisMonth: number
  withdrawals: ProLaboreWithdrawal[]
  businessExpenses: ProLaboreBusinessExpense[]
  suggestedFromAccountId: number | null
  suggestedToAccountId: number | null
}

export const CALCULATION_BASE_LABELS: Record<ProLaboreCalculationBase, string> = {
  MONTH_INCOME: 'Receitas do mês',
  CURRENT_BALANCE: 'Saldo atual das contas PJ',
}

export const TAX_MODE_LABELS: Record<ProLaboreTaxMode, string> = {
  AUTOMATIC: 'Automático',
  MANUAL: 'Definir alíquota',
}

export const WITHHOLDING_MODE_LABELS: Record<ProLaboreWithholdingMode, string> = {
  AUTOMATIC: 'Automático pelo regime',
  ENABLED: 'Sempre calcular',
  DISABLED: 'Não calcular',
}
