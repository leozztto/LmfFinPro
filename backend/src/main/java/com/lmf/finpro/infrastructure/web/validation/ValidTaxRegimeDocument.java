package com.lmf.finpro.infrastructure.web.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Validação de nível de classe: o tipo de documento precisa ser coerente com o regime tributário
 * informado (MEI/Simples Nacional/Lucro Presumido são pessoa jurídica e exigem CNPJ; os demais, CPF).
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = TaxRegimeDocumentConstraintValidator.class)
public @interface ValidTaxRegimeDocument {
    String message() default "tipo de documento incompatível com o regime tributário informado";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
