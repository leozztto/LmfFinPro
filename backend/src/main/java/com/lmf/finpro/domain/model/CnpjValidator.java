package com.lmf.finpro.domain.model;

/**
 * Validação de CNPJ (formato + dígitos verificadores) — mesma ideia do {@link CpfValidator}, para
 * pessoa jurídica.
 */
public final class CnpjValidator {

    private static final int[] FIRST_WEIGHTS = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
    private static final int[] SECOND_WEIGHTS = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};

    private CnpjValidator() {}

    public static boolean isValid(String rawCnpj) {
        if (rawCnpj == null) {
            return false;
        }
        String cnpj = rawCnpj.replaceAll("\\D", "");
        if (cnpj.length() != 14 || hasAllDigitsEqual(cnpj)) {
            return false;
        }

        int firstCheckDigit = checkDigit(cnpj.substring(0, 12), FIRST_WEIGHTS);
        int secondCheckDigit = checkDigit(cnpj.substring(0, 12) + firstCheckDigit, SECOND_WEIGHTS);
        return cnpj.equals(cnpj.substring(0, 12) + firstCheckDigit + secondCheckDigit);
    }

    private static boolean hasAllDigitsEqual(String cnpj) {
        return cnpj.chars().distinct().count() == 1;
    }

    private static int checkDigit(String base, int[] weights) {
        int sum = 0;
        for (int i = 0; i < base.length(); i++) {
            sum += (base.charAt(i) - '0') * weights[i];
        }
        int remainder = sum % 11;
        return remainder < 2 ? 0 : 11 - remainder;
    }
}
