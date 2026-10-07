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
  name: 'Lezzotto Tech',
  cnpj: '[CNPJ a preencher]',
  address: '[Endereço completo a preencher]',
  /** Canal do encarregado pelo tratamento de dados pessoais (DPO), exigido pela LGPD, art. 41. */
  privacyEmail: '[e-mail do encarregado a preencher]',
  dpoName: '[Nome do encarregado a preencher]',
  /** Cidade/UF do foro eleito nos Termos. */
  forum: '[Cidade/UF do foro a preencher]',
} as const

/**
 * Enquanto for `true`, as páginas mostram um aviso de que o texto é uma minuta em revisão. Mude
 * para `false` só depois da revisão jurídica e do preenchimento de `LEGAL_ENTITY`.
 */
export const LEGAL_DRAFT = true
