function StatCard({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-xl border border-slate-200 bg-white p-4 shadow-sm dark:border-slate-700 dark:bg-slate-800">
      <p className="text-sm text-slate-500 dark:text-slate-400">{label}</p>
      <p className="mt-1 text-2xl font-semibold text-slate-900 dark:text-slate-50">{value}</p>
    </div>
  )
}

export default function App() {
  return (
    <div className="min-h-screen bg-slate-50 dark:bg-slate-900">
      <header className="border-b border-slate-200 bg-white px-6 py-4 dark:border-slate-700 dark:bg-slate-800">
        <h1 className="text-xl font-bold text-slate-900 dark:text-slate-50">FinPro</h1>
        <p className="text-sm text-slate-500 dark:text-slate-400">
          Controle financeiro para freelancers e autônomos
        </p>
      </header>

      <main className="mx-auto max-w-5xl px-6 py-8">
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
          <StatCard label="Saldo atual" value="R$ 0,00" />
          <StatCard label="Receita do mês" value="R$ 0,00" />
          <StatCard label="Despesa do mês" value="R$ 0,00" />
        </div>

        <div className="mt-8 rounded-xl border border-dashed border-slate-300 p-8 text-center text-slate-400 dark:border-slate-600">
          Dashboard em construção — próximo passo: ligar isso na API do backend.
        </div>
      </main>
    </div>
  )
}
