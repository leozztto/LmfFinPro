import { useState } from 'react'
import { FormField, Select } from '@/shared/ui'
import { REPORT_TYPE_LABELS, type ReportType } from '../types'
import { ClientReceiptForm } from './ClientReceiptForm'
import { AccountStatementForm } from './AccountStatementForm'
import { ClientAnnualStatementForm } from './ClientAnnualStatementForm'
import { CategoryExpenseReportForm } from './CategoryExpenseReportForm'
import { IncomeStatementForm } from './IncomeStatementForm'

export function ReportsPage() {
  const [reportType, setReportType] = useState<ReportType>('CLIENT_RECEIPT')

  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-lg font-semibold text-zinc-800 dark:text-zinc-100">Relatórios</h2>
        <p className="hidden text-sm text-zinc-500 dark:text-zinc-400 sm:block">
          Gere documentos e exportações a partir dos seus dados.
        </p>
      </div>

      <div className="max-w-xs">
        <FormField label="Tipo de relatório" htmlFor="report-type">
          <Select
            id="report-type"
            value={reportType}
            onChange={(e) => setReportType(e.target.value as ReportType)}
          >
            {Object.entries(REPORT_TYPE_LABELS).map(([value, label]) => (
              <option key={value} value={value}>
                {label}
              </option>
            ))}
          </Select>
        </FormField>
      </div>

      {reportType === 'CLIENT_RECEIPT' && <ClientReceiptForm />}
      {reportType === 'ACCOUNT_STATEMENT' && <AccountStatementForm />}
      {reportType === 'CLIENT_ANNUAL_STATEMENT' && <ClientAnnualStatementForm />}
      {reportType === 'CATEGORY_EXPENSE_REPORT' && <CategoryExpenseReportForm />}
      {reportType === 'INCOME_STATEMENT' && <IncomeStatementForm />}
    </div>
  )
}
