package com.lmf.finpro.integration.support;

import java.util.concurrent.ThreadLocalRandom;

/** Gera CPFs válidos (dígitos verificadores corretos) e únicos para os testes de integração. */
public class CpfTestFactory {

    public static String randomValidCpf() {
        int[] base = new int[9];
        for (int i = 0; i < base.length; i++) {
            base[i] = ThreadLocalRandom.current().nextInt(10);
        }

        int firstCheckDigit = checkDigit(base, 10);
        int[] baseWithFirstDigit = appendDigit(base, firstCheckDigit);
        int secondCheckDigit = checkDigit(baseWithFirstDigit, 11);

        StringBuilder cpf = new StringBuilder();
        for (int digit : base) {
            cpf.append(digit);
        }
        cpf.append(firstCheckDigit).append(secondCheckDigit);
        return cpf.toString();
    }

    private static int[] appendDigit(int[] base, int digit) {
        int[] result = new int[base.length + 1];
        System.arraycopy(base, 0, result, 0, base.length);
        result[base.length] = digit;
        return result;
    }

    private static int checkDigit(int[] digits, int startingWeight) {
        int weight = startingWeight;
        int sum = 0;
        for (int digit : digits) {
            sum += digit * weight--;
        }
        int remainder = sum % 11;
        return remainder < 2 ? 0 : 11 - remainder;
    }
}
