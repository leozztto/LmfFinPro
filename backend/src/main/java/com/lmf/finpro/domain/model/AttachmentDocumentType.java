package com.lmf.finpro.domain.model;

/** O que o anexo comprova — ajuda a organizar a documentação para o IR. */
public enum AttachmentDocumentType {
    PAYMENT_PROOF("Comprovante de pagamento"),
    INVOICE("Nota fiscal"),
    RECEIPT("Recibo"),
    OTHER("Outro");

    private final String label;

    AttachmentDocumentType(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
