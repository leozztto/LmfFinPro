package com.lmf.finpro.infrastructure.web.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class TaxRegimeDocumentConstraintValidator
        implements ConstraintValidator<ValidTaxRegimeDocument, HasTaxRegimeDocument> {

    @Override
    public boolean isValid(HasTaxRegimeDocument value, ConstraintValidatorContext context) {
        if (value == null || value.taxRegime() == null || value.documentType() == null) {
            return true; // @NotNull nos campos individuais cuida da ausência
        }
        return value.documentType() == value.taxRegime().expectedDocumentType();
    }
}
