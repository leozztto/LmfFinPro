/** Versões vigentes dos documentos legais (as mesmas que o cadastro e o aceite exigem). */
export interface LegalVersions {
  termsVersion: string
  privacyVersion: string
}

export interface DocumentConsent {
  currentVersion: string
  /** Última versão que a pessoa aceitou; nulo se nunca aceitou. */
  acceptedVersion: string | null
  /** Quando aceitou essa última versão (ISO, sem fuso). */
  acceptedAt: string | null
  /** Verdadeiro se a versão vigente já foi aceita. */
  accepted: boolean
}

export interface ConsentStatus {
  terms: DocumentConsent
  privacy: DocumentConsent
  /** Verdadeiro enquanto houver documento com versão vigente ainda não aceita. */
  pending: boolean
}
