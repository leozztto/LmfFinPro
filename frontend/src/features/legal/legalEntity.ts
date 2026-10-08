/**
 * Dados de quem opera o FinPro, usados nos Termos de Uso e na Política de Privacidade.
 *
 * PREENCHER ANTES DE PUBLICAR: os campos entre colchetes são lacunas do modelo. Os textos em
 * `TermsPage` e `PrivacyPage` são uma minuta e precisam de revisão de advogado. Ao alterar o texto
 * de qualquer um dos dois, mude a versão correspondente em `LegalDocuments.java` (backend): todos
 * os usuários voltam a ver a tela de aceite no próximo acesso.
 */
export const LEGAL_ENTITY = {
  /** Razão social ou nome do controlador dos dados. */
  name: 'LEZZOTTO TECH LTDA',
  cnpj: '67.608.540/0001-23',
  address: 'Rua Anchieta, 847, bairro São Vicente, Pato Branco/PR, CEP 85506-360',
  /** Canal do encarregado pelo tratamento de dados pessoais (DPO), exigido pela LGPD, art. 41. */
  privacyEmail: 'lezzottotech@gmail.com',
  dpoName: 'Leandro Menegazzo Franceschetto',
  /** Cidade/UF do foro eleito nos Termos. */
  forum: 'Pato Branco/PR',
} as const

/**
 * Enquanto for `true`, as páginas mostram um aviso de que o texto é uma minuta em revisão. Mude
 * para `false` só depois da revisão jurídica e do preenchimento de `LEGAL_ENTITY`.
 */
export const LEGAL_DRAFT = true
