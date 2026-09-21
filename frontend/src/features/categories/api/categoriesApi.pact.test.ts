import { MatchersV3, PactV3 } from '@pact-foundation/pact'
import { describe, expect, it, vi } from 'vitest'
import { __setApiBaseUrlForTests } from '@/shared/api/httpClient'
import { categoriesApi } from './categoriesApi'

// O contrato precisa de um header Authorization, mas o valor exato não importa aqui — quem
// garante que é um JWT de verdade é o provider verification (backend), via request filter.
vi.mock('@/shared/auth/authStorage', () => ({
  getStoredToken: () => 'contract-test-token',
  clearSession: vi.fn(),
  authEvents: new EventTarget(),
  SESSION_EXPIRED_EVENT: 'finpro:session-expired',
}))

const { eachLike, integer, string, boolean, regex } = MatchersV3

const provider = new PactV3({
  consumer: 'LmfFinPro-frontend',
  provider: 'LmfFinPro-backend',
  // Caminho relativo ao diretório de onde o Vitest roda (frontend/) — evita node:path/process,
  // que exigiriam @types/node só por causa deste arquivo.
  dir: 'pacts',
})

describe('categoriesApi Pact contract', () => {
  it('lists categories for the authenticated user', () => {
    provider
      .given('o usuário autenticado tem ao menos uma categoria')
      .uponReceiving('uma requisição para listar categorias')
      .withRequest({
        method: 'GET',
        path: '/api/categories',
        headers: { Authorization: string('Bearer contract-test-token') },
      })
      .willRespondWith({
        status: 200,
        headers: { 'Content-Type': regex('application/json.*', 'application/json') },
        body: eachLike({
          id: integer(1),
          name: string('Alimentação'),
          type: string('EXPENSE'),
          color: string('#2E6E4E'),
          icon: string('utensils'),
          global: boolean(false),
        }),
      })

    return provider.executeTest(async (mockServer) => {
      // O httpClient normalmente aponta pra VITE_API_BASE_URL (que em produção já inclui o
      // prefixo /api — ver default em httpClient.ts); replicamos isso aqui pra bater exatamente
      // com o path real do backend (/api/categories), que é o que o provider vai verificar.
      __setApiBaseUrlForTests(`${mockServer.url}/api`)

      const categories = await categoriesApi.list()

      expect(categories.length).toBeGreaterThan(0)
      expect(categories[0]).toMatchObject({ name: 'Alimentação', type: 'EXPENSE', global: false })
    })
  })
})
