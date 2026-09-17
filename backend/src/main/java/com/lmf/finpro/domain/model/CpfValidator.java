package com.lmf.finpro.domain.model;

/**
 * Validação de CPF (formato + dígitos verificadores). Regra de negócio pura do domínio —
 * um {@link User} precisa de um CPF válido, independentemente de como o dado chega (API, import futuro, etc).
 */
public final class CpfValidator {

    private CpfValidator() {
    }

    public static boolean isValid(String rawCpf) {
        if (rawCpf == null) {
            return false;
        }
        String cpf = rawCpf.replaceAll("\\D", "");
        if (cpf.length() != 11 || hasAllDigitsEqual(cpf)) {
            return false;
        }

        int firstCheckDigit = checkDigit(cpf.substring(0, 9), 10);
        int secondCheckDigit = checkDigit(cpf.substring(0, 9) + firstCheckDigit, 11);
        return cpf.equals(cpf.substring(0, 9) + firstCheckDigit + secondCheckDigit);
    }

    private static boolean hasAllDigitsEqual(String cpf) {
        return cpf.chars().distinct().count() == 1;
    }

    private static int checkDigit(String base, int startingWeight) {
        int weight = startingWeight;
        int sum = 0;
        for (char c : base.toCharArray()) {
            sum += (c - '0') * weight--;
        }
        int remainder = sum % 11;
        return remainder < 2 ? 0 : 11 - remainder;
    }
}
