package com.lmf.finpro.infrastructure.web.validation;

import static org.assertj.core.api.Assertions.assertThat;

import com.lmf.finpro.domain.model.DocumentType;
import com.lmf.finpro.integration.support.CnpjTestFactory;
import com.lmf.finpro.integration.support.CpfTestFactory;
import org.junit.jupiter.api.Test;

class DocumentNumberConstraintValidatorTest {

    private final DocumentNumberConstraintValidator validator =
            new DocumentNumberConstraintValidator();

    private record Request(DocumentType documentType, String documentNumber)
            implements HasDocument {}

    @Test
    void acceptsAValidCpf() {
        Request request = new Request(DocumentType.CPF, CpfTestFactory.randomValidCpf());

        assertThat(validator.isValid(request, null)).isTrue();
    }

    @Test
    void acceptsAValidCnpj() {
        Request request = new Request(DocumentType.CNPJ, CnpjTestFactory.randomValidCnpj());

        assertThat(validator.isValid(request, null)).isTrue();
    }

    @Test
    void rejectsAnInvalidCpf() {
        Request request = new Request(DocumentType.CPF, "11111111111");

        assertThat(validator.isValid(request, null)).isFalse();
    }

    @Test
    void isValidWhenDocumentTypeIsNull() {
        assertThat(validator.isValid(new Request(null, "12345678900"), null)).isTrue();
    }

    @Test
    void isValidWhenDocumentNumberIsNull() {
        assertThat(validator.isValid(new Request(DocumentType.CPF, null), null)).isTrue();
    }

    @Test
    void isValidWhenValueItselfIsNull() {
        assertThat(validator.isValid(null, null)).isTrue();
    }
}
