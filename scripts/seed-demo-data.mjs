#!/usr/bin/env node
// Seed de dados de demonstração: cria um usuário demo com ~6 meses de histórico financeiro
// realista (contas, categorias, clientes, transações, transferências, orçamentos e estimativas
// de imposto) via chamadas HTTP normais à API — sem tocar no banco direto, então respeita todas
// as mesmas regras de negócio e validações que um usuário real passaria.
//
// Uso:
//   node scripts/seed-demo-data.mjs
//   API_BASE_URL=https://minha-api.exemplo.com/api node scripts/seed-demo-data.mjs
//
// Idempotente na prática: se o usuário demo já existir e já tiver contas, o script não duplica
// nada — só reautentica e sai. Requer Node 18+ (usa fetch nativo).

const API_BASE_URL = process.env.API_BASE_URL ?? 'http://localhost:8080/api'
const DEMO_EMAIL = 'demo@finpro.app'
const DEMO_PASSWORD = 'Demo@12345'

async function api(path, { method = 'GET', body, token } = {}) {
  const headers = { 'Content-Type': 'application/json' }
  if (token) headers.Authorization = `Bearer ${token}`
  const response = await fetch(`${API_BASE_URL}${path}`, {
    method,
    headers,
    body: body ? JSON.stringify(body) : undefined,
  })
  if (!response.ok) {
    const errorBody = await response.json().catch(() => null)
    const error = new Error(`${method} ${path} -> ${response.status}: ${errorBody?.message ?? response.statusText}`)
    error.status = response.status
    throw error
  }
  if (response.status === 204) return undefined
  return response.json()
}

// --- geradores de CPF/CNPJ válidos (mesmo algoritmo do backend/CpfValidator e CnpjValidator) ---

function cpfCheckDigit(base, startingWeight) {
  let weight = startingWeight
  let sum = 0
  for (const ch of base) sum += Number(ch) * weight--
  const remainder = sum % 11
  return remainder < 2 ? 0 : 11 - remainder
}

function randomCpf() {
  let base
  do {
    base = Array.from({ length: 9 }, () => Math.floor(Math.random() * 10)).join('')
  } while (new Set(base).size === 1)
  const d1 = cpfCheckDigit(base, 10)
  const d2 = cpfCheckDigit(base + d1, 11)
  return `${base}${d1}${d2}`
}

const CNPJ_FIRST_WEIGHTS = [5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2]
const CNPJ_SECOND_WEIGHTS = [6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2]

function cnpjCheckDigit(base, weights) {
  let sum = 0
  for (let i = 0; i < base.length; i++) sum += Number(base[i]) * weights[i]
  const remainder = sum % 11
  return remainder < 2 ? 0 : 11 - remainder
}

function randomCnpj() {
  let base
  do {
    base = Array.from({ length: 12 }, () => Math.floor(Math.random() * 10)).join('')
  } while (new Set(base).size === 1)
  const d1 = cnpjCheckDigit(base, CNPJ_FIRST_WEIGHTS)
  const d2 = cnpjCheckDigit(base + d1, CNPJ_SECOND_WEIGHTS)
  return `${base}${d1}${d2}`
}

// --- helpers de data/valor ---

function monthsBack(n) {
  const now = new Date()
  const months = []
  for (let i = n - 1; i >= 0; i--) {
    months.push(new Date(now.getFullYear(), now.getMonth() - i, 1))
  }
  return months
}

function monthKey(monthDate) {
  return `${monthDate.getFullYear()}-${String(monthDate.getMonth() + 1).padStart(2, '0')}`
}

function isCurrentMonth(monthDate) {
  const today = new Date()
  return monthDate.getFullYear() === today.getFullYear() && monthDate.getMonth() === today.getMonth()
}

/** Dia máximo utilizável no mês: o mês inteiro para meses passados, ou o dia de hoje se for o mês atual (evita datas futuras). */
function maxDayFor(monthDate) {
  if (!isCurrentMonth(monthDate)) return undefined
  return new Date().getDate()
}

function randomDayInMonth(monthDate, cap) {
  const daysInMonth = new Date(monthDate.getFullYear(), monthDate.getMonth() + 1, 0).getDate()
  const upperBound = cap ? Math.min(cap, daysInMonth) : daysInMonth
  const day = 1 + Math.floor(Math.random() * upperBound)
  return new Date(monthDate.getFullYear(), monthDate.getMonth(), day)
}

function toIsoDate(date) {
  return date.toISOString().slice(0, 10)
}

function randomAmount(min, max) {
  return Math.round((min + Math.random() * (max - min)) * 100) / 100
}

// --- criação dos dados ---

async function ensureDemoUser() {
  try {
    const auth = await api('/auth/register', {
      method: 'POST',
      body: {
        name: 'Ana Freelancer',
        email: DEMO_EMAIL,
        password: DEMO_PASSWORD,
        documentType: 'CPF',
        documentNumber: randomCpf(),
        phone: '11987654321',
        taxRegime: 'AUTONOMO',
        address: {
          zipCode: '01310100',
          street: 'Avenida Paulista',
          number: '1000',
          complement: 'Sala 42',
          neighborhood: 'Bela Vista',
          city: 'São Paulo',
          state: 'SP',
        },
      },
    })
    console.log('Usuário demo criado.')
    return { token: auth.token, isNew: true }
  } catch (error) {
    if (error.status === 409) {
      console.log('Usuário demo já existe — autenticando...')
      const auth = await api('/auth/login', { method: 'POST', body: { email: DEMO_EMAIL, password: DEMO_PASSWORD } })
      return { token: auth.token, isNew: false }
    }
    throw error
  }
}

async function createAccounts(token) {
  const defs = [
    { name: 'Conta Corrente', type: 'CHECKING', initialBalance: 5000 },
    { name: 'Poupança', type: 'SAVINGS', initialBalance: 15000 },
    { name: 'Carteira', type: 'WALLET', initialBalance: 300 },
  ]
  const accounts = {}
  for (const def of defs) {
    const created = await api('/accounts', { method: 'POST', token, body: def })
    accounts[def.name] = created.id
  }
  console.log(`${defs.length} contas criadas.`)
  return accounts
}

async function createCategories(token) {
  const defs = [
    { name: 'Consultoria', type: 'INCOME', color: '#2AD6A5', icon: 'briefcase' },
    { name: 'Freelance', type: 'INCOME', color: '#4F8CFF', icon: 'laptop' },
    { name: 'Moradia', type: 'EXPENSE', color: '#F97316', icon: 'home' },
    { name: 'Alimentação', type: 'EXPENSE', color: '#EF4444', icon: 'utensils' },
    { name: 'Transporte', type: 'EXPENSE', color: '#EAB308', icon: 'car' },
    { name: 'Lazer', type: 'EXPENSE', color: '#EC4899', icon: 'film' },
    { name: 'Saúde', type: 'EXPENSE', color: '#14B8A6', icon: 'heart' },
    { name: 'Educação', type: 'EXPENSE', color: '#6366F1', icon: 'book' },
    { name: 'Assinaturas', type: 'EXPENSE', color: '#A855F7', icon: 'repeat' },
    { name: 'Impostos', type: 'EXPENSE', color: '#64748B', icon: 'file-text' },
  ]
  const categories = {}
  for (const def of defs) {
    const created = await api('/categories', { method: 'POST', token, body: def })
    categories[def.name] = { id: created.id, type: def.type }
  }
  console.log(`${defs.length} categorias criadas.`)
  return categories
}

async function createClients(token) {
  const defs = [
    {
      name: 'TechCorp Consultoria',
      email: 'financeiro@techcorp.example.com',
      phone: '11912345678',
      documentType: 'CNPJ',
      documentNumber: randomCnpj(),
      workType: 'PJ',
      notes: 'Contrato mensal de consultoria em TI.',
      color: '#2AD6A5',
      active: true,
    },
    {
      name: 'StartupXYZ',
      email: 'contato@startupxyz.example.com',
      phone: '11923456789',
      documentType: 'CNPJ',
      documentNumber: randomCnpj(),
      workType: 'PJ',
      notes: 'Projetos pontuais de desenvolvimento.',
      color: '#4F8CFF',
      active: true,
    },
    {
      name: 'Loja ABC',
      email: 'compras@lojaabc.example.com',
      phone: '11934567890',
      documentType: 'CPF',
      documentNumber: randomCpf(),
      workType: 'AUTONOMO',
      notes: 'Serviços eventuais.',
      color: '#EC4899',
      active: true,
    },
  ]
  const clients = []
  for (const def of defs) {
    const created = await api('/clients', { method: 'POST', token, body: def })
    clients.push({ id: created.id, name: def.name })
  }
  console.log(`${defs.length} clientes criados.`)
  return clients
}

async function createCategoryRules(token, categories) {
  const defs = [
    { pattern: 'UBER', categoryId: categories['Transporte'].id },
    { pattern: 'IFOOD', categoryId: categories['Alimentação'].id },
    { pattern: 'NETFLIX', categoryId: categories['Assinaturas'].id },
    { pattern: 'ALUGUEL', categoryId: categories['Moradia'].id },
  ]
  for (const def of defs) {
    await api('/category-rules', { method: 'POST', token, body: def })
  }
  console.log(`${defs.length} regras de categorização criadas.`)
}

async function createTransaction(token, { account, category, client, description, amount, date, type }) {
  await api('/transactions', {
    method: 'POST',
    token,
    body: {
      accountId: account,
      categoryId: category ?? null,
      clientId: client ?? null,
      description,
      amount,
      transactionDate: toIsoDate(date),
      type,
    },
  })
}

async function createTransactions(token, accounts, categories, clients) {
  const months = monthsBack(6)
  const incomeByMonth = {}
  let count = 0

  for (const monthDate of months) {
    const cap = maxDayFor(monthDate)
    incomeByMonth[monthKey(monthDate)] = 0
    const addIncome = (value) => {
      incomeByMonth[monthKey(monthDate)] += value
    }

    // Receita fixa: consultoria mensal com o cliente principal.
    const consultingAmount = randomAmount(3500, 6000)
    await createTransaction(token, {
      account: accounts['Conta Corrente'],
      category: categories['Consultoria'].id,
      client: clients[0].id,
      description: 'Consultoria mensal - TechCorp',
      amount: consultingAmount,
      date: randomDayInMonth(monthDate, Math.min(10, cap ?? 10)),
      type: 'INCOME',
    })
    addIncome(consultingAmount)
    count++

    // Receita variável: 1 a 2 projetos com o segundo cliente.
    const startupInvoices = 1 + Math.round(Math.random())
    for (let i = 0; i < startupInvoices; i++) {
      const amount = randomAmount(1500, 3200)
      await createTransaction(token, {
        account: accounts['Conta Corrente'],
        category: categories['Freelance'].id,
        client: clients[1].id,
        description: `Projeto StartupXYZ #${i + 1}`,
        amount,
        date: randomDayInMonth(monthDate, cap),
        type: 'INCOME',
      })
      addIncome(amount)
      count++
    }

    // Receita eventual: nem todo mês tem serviço avulso.
    if (Math.random() < 0.6) {
      const amount = randomAmount(800, 2000)
      await createTransaction(token, {
        account: accounts['Conta Corrente'],
        category: categories['Freelance'].id,
        client: clients[2].id,
        description: 'Serviço avulso - Loja ABC',
        amount,
        date: randomDayInMonth(monthDate, cap),
        type: 'INCOME',
      })
      addIncome(amount)
      count++
    }

    // Despesas fixas.
    await createTransaction(token, {
      account: accounts['Conta Corrente'],
      category: categories['Moradia'].id,
      description: 'Aluguel',
      amount: 1800,
      date: randomDayInMonth(monthDate, Math.min(5, cap ?? 5)),
      type: 'EXPENSE',
    })
    await createTransaction(token, {
      account: accounts['Conta Corrente'],
      category: categories['Assinaturas'].id,
      description: 'Assinaturas (streaming, software)',
      amount: 89.9,
      date: randomDayInMonth(monthDate, Math.min(10, cap ?? 10)),
      type: 'EXPENSE',
    })
    await createTransaction(token, {
      account: accounts['Conta Corrente'],
      category: categories['Impostos'].id,
      description: 'Guia de recolhimento mensal',
      amount: randomAmount(250, 450),
      date: randomDayInMonth(monthDate, Math.min(20, cap ?? 20)),
      type: 'EXPENSE',
    })
    count += 3

    // Despesas variáveis, com quantidade e valor oscilando mês a mês.
    const variableExpenses = [
      { category: 'Alimentação', countRange: [4, 8], amountRange: [25, 180], account: 'Conta Corrente' },
      { category: 'Transporte', countRange: [3, 6], amountRange: [15, 70], account: 'Conta Corrente' },
      { category: 'Lazer', countRange: [1, 3], amountRange: [50, 280], account: 'Carteira' },
    ]
    for (const spec of variableExpenses) {
      const [minCount, maxCount] = spec.countRange
      const n = minCount + Math.floor(Math.random() * (maxCount - minCount + 1))
      for (let i = 0; i < n; i++) {
        await createTransaction(token, {
          account: accounts[spec.account],
          category: categories[spec.category].id,
          description: `${spec.category} #${i + 1}`,
          amount: randomAmount(spec.amountRange[0], spec.amountRange[1]),
          date: randomDayInMonth(monthDate, cap),
          type: 'EXPENSE',
        })
        count++
      }
    }

    // Despesas ocasionais.
    if (Math.random() < 0.4) {
      await createTransaction(token, {
        account: accounts['Conta Corrente'],
        category: categories['Saúde'].id,
        description: 'Consulta / exame',
        amount: randomAmount(100, 400),
        date: randomDayInMonth(monthDate, cap),
        type: 'EXPENSE',
      })
      count++
    }
    if (Math.random() < 0.25) {
      await createTransaction(token, {
        account: accounts['Conta Corrente'],
        category: categories['Educação'].id,
        description: 'Curso online',
        amount: randomAmount(150, 600),
        date: randomDayInMonth(monthDate, cap),
        type: 'EXPENSE',
      })
      count++
    }
  }

  console.log(`${count} transações criadas ao longo de 6 meses.`)
  return incomeByMonth
}

async function createTransfers(token, accounts) {
  const months = monthsBack(6)
  let count = 0
  for (const monthDate of months) {
    const cap = maxDayFor(monthDate)

    // Reserva mensal pra poupança, perto do fim do mês.
    const savingsDay = cap ? Math.min(25, cap) : 25
    await api('/transfers', {
      method: 'POST',
      token,
      body: {
        fromAccountId: accounts['Conta Corrente'],
        toAccountId: accounts['Poupança'],
        amount: randomAmount(500, 1200),
        transferDate: toIsoDate(new Date(monthDate.getFullYear(), monthDate.getMonth(), savingsDay)),
        description: 'Reserva mensal',
      },
    })
    count++

    // Saque pra carteira, no início do mês — sem isso a Carteira só recebe despesas de Lazer e
    // fica com saldo negativo em poucos meses (nunca tem receita entrando nela).
    const withdrawalDay = cap ? Math.min(3, cap) : 3
    await api('/transfers', {
      method: 'POST',
      token,
      body: {
        fromAccountId: accounts['Conta Corrente'],
        toAccountId: accounts['Carteira'],
        amount: randomAmount(300, 500),
        transferDate: toIsoDate(new Date(monthDate.getFullYear(), monthDate.getMonth(), withdrawalDay)),
        description: 'Saque em espécie',
      },
    })
    count++
  }
  console.log(`${count} transferências criadas.`)
}

async function createBudgets(token, categories) {
  const referenceMonth = monthKey(new Date())
  const defs = [
    { category: 'Alimentação', limitValue: 900 },
    { category: 'Transporte', limitValue: 400 },
    { category: 'Lazer', limitValue: 350 },
  ]
  for (const def of defs) {
    await api('/budgets', {
      method: 'POST',
      token,
      body: { categoryId: categories[def.category].id, referenceMonth, limitValue: def.limitValue },
    })
  }
  console.log(`${defs.length} orçamentos criados para ${referenceMonth}.`)
}

async function createTaxEstimates(token, incomeByMonth) {
  const currentKey = monthKey(new Date())
  const entries = Object.entries(incomeByMonth)
    .filter(([key]) => key !== currentKey)
    .slice(-2)

  for (const [referenceMonth, rawGrossRevenue] of entries) {
    const grossRevenue = Math.round(rawGrossRevenue * 100) / 100
    const { rate } = await api(`/tax-estimates/suggested-rate?regime=AUTONOMO&grossRevenue=${grossRevenue}`, { token })
    await api('/tax-estimates', {
      method: 'POST',
      token,
      body: { referenceMonth, regime: 'AUTONOMO', grossRevenue, appliedRate: rate },
    })
  }
  console.log(`${entries.length} estimativas de imposto criadas.`)
}

function printCredentials() {
  console.log('\n--- Credenciais de demonstração ---')
  console.log(`E-mail: ${DEMO_EMAIL}`)
  console.log(`Senha:  ${DEMO_PASSWORD}`)
}

async function main() {
  console.log(`Seedando dados de demonstração em ${API_BASE_URL}...\n`)
  const { token, isNew } = await ensureDemoUser()

  if (!isNew) {
    const existingAccounts = await api('/accounts', { token })
    if (existingAccounts.length > 0) {
      console.log(`Usuário demo já tem ${existingAccounts.length} conta(s) cadastrada(s) — seed já rodou antes, nada a fazer.`)
      printCredentials()
      return
    }
  }

  const accounts = await createAccounts(token)
  const categories = await createCategories(token)
  const clients = await createClients(token)
  await createCategoryRules(token, categories)
  const incomeByMonth = await createTransactions(token, accounts, categories, clients)
  await createTransfers(token, accounts)
  await createBudgets(token, categories)
  await createTaxEstimates(token, incomeByMonth)

  console.log('\nSeed concluído com sucesso!')
  printCredentials()
}

main().catch((error) => {
  console.error('\nFalha ao rodar o seed:', error.message)
  process.exit(1)
})
