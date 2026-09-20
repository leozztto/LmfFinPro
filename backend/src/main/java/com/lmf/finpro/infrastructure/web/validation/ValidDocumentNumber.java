package com.lmf.finpro.infrastructure.web.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Validação de nível de classe: o número do documento precisa ser um CPF ou CNPJ válido, dependendo
 * do {@code documentType} informado no mesmo request.
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = DocumentNumberConstraintValidator.class)
public @interface ValidDocumentNumber {
    String message() default "documento inválido para o tipo informado";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
