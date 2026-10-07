export const ACTIVE_HOUSEHOLD_KEY = 'finpro.household.active'

/** Header que diz ao backend qual grupo a requisição enxerga; sem ele vale o espaço pessoal. */
export const HOUSEHOLD_HEADER = 'X-Household-Id'

/** Disparado pelo httpClient quando o backend responde 403 a uma chamada feita com um grupo
 *  escolhido — pode ser que a pessoa tenha sido removida do grupo. O HouseholdProvider escuta para
 *  conferir a lista de grupos e, se for o caso, voltar ao espaço pessoal. */
export const HOUSEHOLD_FORBIDDEN_EVENT = 'finpro:household-forbidden'
export const householdEvents = new EventTarget()

/** Undefined = ainda não lido do localStorage. Null = espaço pessoal (o padrão). */
let activeHouseholdId: number | null | undefined

function readStored(): number | null {
  try {
    const raw = localStorage.getItem(ACTIVE_HOUSEHOLD_KEY)
    if (!raw) return null
    const id = Number(raw)
    return Number.isInteger(id) && id > 0 ? id : null
  } catch {
    return null
  }
}

/** Relê o grupo ativo do localStorage (usado na inicialização e nos testes). */
export function restoreActiveHousehold(): number | null {
  activeHouseholdId = readStored()
  return activeHouseholdId
}

/** Id do grupo compartilhado que a pessoa está vendo, ou null para o espaço pessoal. Só os grupos
 *  compartilhados são guardados: o pessoal é sempre o padrão e não precisa do header. */
export function getActiveHouseholdId(): number | null {
  if (activeHouseholdId === undefined) return restoreActiveHousehold()
  return activeHouseholdId
}

export function setActiveHouseholdId(id: number | null): void {
  activeHouseholdId = id
  try {
    if (id === null) {
      localStorage.removeItem(ACTIVE_HOUSEHOLD_KEY)
    } else {
      localStorage.setItem(ACTIVE_HOUSEHOLD_KEY, String(id))
    }
  } catch {
    // localStorage indisponível (modo privado, etc.) — a escolha só não é lembrada entre reloads.
  }
}

/** Ao encerrar a sessão o grupo escolhido não deve vazar para o próximo login neste navegador. */
export function clearActiveHousehold(): void {
  setActiveHouseholdId(null)
}
