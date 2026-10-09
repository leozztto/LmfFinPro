import type { OnboardingStepId } from './types'

/** Parâmetro de URL que abre o modal de primeiros passos por cima da tela atual. */
export const ONBOARDING_PARAM = 'primeiros-passos'
export const ONBOARDING_PATH = `/?${ONBOARDING_PARAM}=1`

interface TourStep {
  id: OnboardingStepId
  /** A tarefa a que o passo pertence. */
  group: string
  title: string
  description: string
  /** Onde a pessoa encontra isso no sistema. */
  where: string
}

/** Os passos do guia, na ordem em que aparecem (a mesma do enum do servidor). */
export const TOUR_STEPS: TourStep[] = [
  {
    id: 'ACCOUNT_OPEN',
    group: 'Criar uma conta',
    title: 'Abra o cadastro de conta',
    description: 'A conta é onde os lançamentos ficam: corrente, cartão ou carteira. Comece pelo botão de adicionar.',
    where: 'Menu Contas, botão +',
  },
  {
    id: 'ACCOUNT_FILL',
    group: 'Criar uma conta',
    title: 'Preencha os dados da conta',
    description:
      'Dê um nome, escolha o tipo e informe o saldo que a conta tem hoje. Os valores abaixo são só um exemplo.',
    where: 'Formulário Nova conta',
  },
  {
    id: 'ACCOUNT_SAVE',
    group: 'Criar uma conta',
    title: 'Salve a conta',
    description: 'Clique em adicionar conta. Ela já aparece na lista e fica disponível para receber lançamentos.',
    where: 'Botão no fim do formulário',
  },
  {
    id: 'TRANSACTION_FILL',
    group: 'Lançar uma transação',
    title: 'Preencha o lançamento',
    description:
      'Informe a descrição, o valor, a data, a conta e a categoria. Despesa entra como saída e receita como entrada.',
    where: 'Menu Transações, botão +',
  },
  {
    id: 'TRANSACTION_SAVE',
    group: 'Lançar uma transação',
    title: 'Salve a transação',
    description: 'Ao salvar, o lançamento entra na lista e o saldo da conta, o dashboard e os relatórios se atualizam.',
    where: 'Botão no fim do formulário',
  },
  {
    id: 'IMPORT_PICK',
    group: 'Importar o extrato',
    title: 'Envie o extrato do banco',
    description:
      'Baixe o extrato do último mês no app ou no site do seu banco (CSV ou OFX), escolha a conta e o arquivo e clique em importar.',
    where: 'Menu Importações',
  },
  {
    id: 'IMPORT_RESULT',
    group: 'Importar o extrato',
    title: 'Veja o resultado na hora',
    description:
      'Depois do envio você vê quanto entrou, quanto saiu, o saldo do período e as categorias em que mais gastou.',
    where: 'Logo abaixo do envio',
  },
  {
    id: 'BUDGET_FILL',
    group: 'Definir um orçamento',
    title: 'Escolha um teto de gasto',
    description: 'Selecione uma categoria, o mês e o valor limite. Você é avisado antes de estourar.',
    where: 'Menu Orçamentos, botão +',
  },
  {
    id: 'BUDGET_SAVE',
    group: 'Definir um orçamento',
    title: 'Salve o orçamento',
    description: 'Ao salvar, o acompanhamento do mês já começa. Agora é só fazer o mesmo no seu sistema.',
    where: 'Botão no fim do formulário',
  },
  {
    id: 'RECURRING_FILL',
    group: 'Automatizar com recorrências',
    title: 'Cadastre o que se repete',
    description:
      'Aluguel, assinatura, salário: informe a descrição, o valor, a frequência e a data da próxima ocorrência.',
    where: 'Menu Recorrências, botão +',
  },
  {
    id: 'RECURRING_SAVE',
    group: 'Automatizar com recorrências',
    title: 'Salve a recorrência',
    description: 'Daqui em diante o sistema lança sozinho a cada ciclo, sem você digitar todo mês.',
    where: 'Botão no fim do formulário',
  },
  {
    id: 'CALENDAR_VIEW',
    group: 'Acompanhar o mês',
    title: 'Veja o mês no calendário',
    description:
      'Cada dia mostra o que entrou, o que saiu e o que está para vencer. Clique em um dia para ver os lançamentos.',
    where: 'Menu Calendário',
  },
  {
    id: 'DASHBOARD_VIEW',
    group: 'Acompanhar o mês',
    title: 'Leia o dashboard',
    description: 'O resumo do mês: saldo, entradas, saídas e a evolução dos seus gastos por categoria.',
    where: 'Menu Dashboard',
  },
  {
    id: 'REPORTS_VIEW',
    group: 'Acompanhar o mês',
    title: 'Gere relatórios',
    description: 'Escolha o período e o tipo de relatório para analisar com mais detalhe e exportar os dados.',
    where: 'Menu Relatórios',
  },
]

/** Modelo de CSV para quem ainda não baixou o extrato do banco. */
export const SAMPLE_CSV = [
  'date,description,amount',
  '2026-01-05,Supermercado,-182.40',
  '2026-01-06,Pagamento cliente,3500.00',
  '2026-01-08,Uber,-24.90',
  '2026-01-10,Aluguel,-1200.00',
  '',
].join('\n')
