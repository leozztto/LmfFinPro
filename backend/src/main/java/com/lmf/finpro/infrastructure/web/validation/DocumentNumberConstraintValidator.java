package com.lmf.finpro.infrastructure.web.validation;

import com.lmf.finpro.domain.model.CnpjValidator;
import com.lmf.finpro.domain.model.CpfValidator;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class DocumentNumberConstraintValidator implements ConstraintValidator<ValidDocumentNumber, HasDocument> {

    @Override
    public boolean isValid(HasDocument value, ConstraintValidatorContext context) {
        if (value == null || value.documentType() == null || value.documentNumber() == null) {
            return true; // @NotNull/@NotBlank nos campos individuais cuidam da ausência
        }

        return switch (value.documentType()) {
            case CPF -> CpfValidator.isValid(value.documentNumber());
            case CNPJ -> CnpjValidator.isValid(value.documentNumber());
        };
    }
}
