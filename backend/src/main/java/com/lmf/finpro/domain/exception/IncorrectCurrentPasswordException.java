package com.lmf.finpro.domain.exception;

/**
 * Senha atual errada numa operação feita já logado (trocar senha/e-mail). Separada de
 * InvalidCredentialsException de propósito: aquela vira 401, e um 401 aqui faria o frontend
 * encerrar a sessão de quem só digitou a senha errada.
 */
public class IncorrectCurrentPasswordException extends RuntimeException {
    public IncorrectCurrentPasswordException(String message) {
        super(message);
    }
}
