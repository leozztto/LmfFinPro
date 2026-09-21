package com.lmf.finpro.infrastructure.web.validation;

import static org.assertj.core.api.Assertions.assertThat;

import com.lmf.finpro.domain.model.DocumentType;
import com.lmf.finpro.domain.model.TaxRegime;
import org.junit.jupiter.api.Test;

class TaxRegimeDocumentConstraintValidatorTest {

    private final TaxRegimeDocumentConstraintValidator validator =
            new TaxRegimeDocumentConstraintValidator();

    private record Request(TaxRegime taxRegime, DocumentType documentType)
            implements HasTaxRegimeDocument {}

    @Test
    void acceptsMatchingRegimeAndDocumentType() {
        assertThat(validator.isValid(new Request(TaxRegime.MEI, DocumentType.CNPJ), null)).isTrue();
    }

    @Test
    void rejectsMismatchedRegimeAndDocumentType() {
        assertThat(validator.isValid(new Request(TaxRegime.MEI, DocumentType.CPF), null)).isFalse();
    }

    @Test
    void isValidWhenTaxRegimeIsNull() {
        // @NotNull no campo individual cuida da ausência — o validador de classe não deve falhar
        // sozinho.
        assertThat(validator.isValid(new Request(null, DocumentType.CPF), null)).isTrue();
    }

    @Test
    void isValidWhenDocumentTypeIsNull() {
        assertThat(validator.isValid(new Request(TaxRegime.AUTONOMO, null), null)).isTrue();
    }

    @Test
    void isValidWhenValueItselfIsNull() {
        assertThat(validator.isValid(null, null)).isTrue();
    }
}
