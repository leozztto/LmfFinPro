package com.lmf.finpro.integration.support;

import java.util.concurrent.ThreadLocalRandom;

/** Gera CNPJs válidos (dígitos verificadores corretos) e únicos para os testes de integração. */
public class CnpjTestFactory {

    private static final int[] FIRST_WEIGHTS = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
    private static final int[] SECOND_WEIGHTS = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};

    public static String randomValidCnpj() {
        int[] base = new int[12];
        for (int i = 0; i < base.length; i++) {
            base[i] = ThreadLocalRandom.current().nextInt(10);
        }

        int firstCheckDigit = checkDigit(base, FIRST_WEIGHTS);
        int[] baseWithFirstDigit = appendDigit(base, firstCheckDigit);
        int secondCheckDigit = checkDigit(baseWithFirstDigit, SECOND_WEIGHTS);

        StringBuilder cnpj = new StringBuilder();
        for (int digit : base) {
            cnpj.append(digit);
        }
        cnpj.append(firstCheckDigit).append(secondCheckDigit);
        return cnpj.toString();
    }

    private static int[] appendDigit(int[] base, int digit) {
        int[] result = new int[base.length + 1];
        System.arraycopy(base, 0, result, 0, base.length);
        result[base.length] = digit;
        return result;
    }

    private static int checkDigit(int[] digits, int[] weights) {
        int sum = 0;
        for (int i = 0; i < digits.length; i++) {
            sum += digits[i] * weights[i];
        }
        int remainder = sum % 11;
        return remainder < 2 ? 0 : 11 - remainder;
    }
}
