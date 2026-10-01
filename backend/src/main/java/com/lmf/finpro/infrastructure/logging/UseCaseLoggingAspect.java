package com.lmf.finpro.infrastructure.logging;

import static net.logstash.logback.argument.StructuredArguments.kv;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

/**
 * Registra qual fluxo de negócio foi executado (ex.: {@code Account.create}, {@code
 * Transaction.list}), quanto demorou e se falhou — é o que liga uma requisição HTTP ao que o
 * usuário estava fazendo na tela. Só o nome do fluxo vai para o log: argumentos e retorno nunca são
 * registrados (têm dados pessoais e financeiros), e a falha sai só com o tipo da exceção.
 *
 * <p>Só o fluxo de fora é registrado: serviços que chamam outros serviços não geram linhas
 * repetidas. Enquanto o fluxo roda, o MDC carrega {@code flow}, então qualquer log emitido dentro
 * dele sai com o nome do fluxo. Chamados por um scheduler, os fluxos bem-sucedidos saem em DEBUG
 * (um por item do lote encheria o log); as falhas saem sempre.
 *
 * <p>Para silenciar: {@code
 * logging.level.com.lmf.finpro.infrastructure.logging.UseCaseLoggingAspect=WARN}.
 */
@Slf4j
@Aspect
@Component
public class UseCaseLoggingAspect {

    public static final String MDC_FLOW = "flow";

    private static final String SERVICE_SUFFIX = "ApplicationService";

    @Around("execution(public * com.lmf.finpro.application..*ApplicationService.*(..))")
    public Object logFlow(ProceedingJoinPoint joinPoint) throws Throwable {
        if (MDC.get(MDC_FLOW) != null) {
            return joinPoint.proceed();
        }
        String flow = flowName(joinPoint);
        boolean inScheduler = MDC.get("scheduler") != null;
        MDC.put(MDC_FLOW, flow);
        long start = System.nanoTime();
        try {
            Object result = joinPoint.proceed();
            if (inScheduler) {
                log.debug("Fluxo {} concluído {}", flow, kv("durationMs", elapsedMs(start)));
            } else {
                log.info("Fluxo {} concluído {}", flow, kv("durationMs", elapsedMs(start)));
            }
            return result;
        } catch (Throwable ex) {
            log.warn(
                    "Fluxo {} falhou {} {}",
                    flow,
                    kv("error", ex.getClass().getSimpleName()),
                    kv("durationMs", elapsedMs(start)));
            throw ex;
        } finally {
            MDC.remove(MDC_FLOW);
        }
    }

    private static String flowName(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String service = signature.getDeclaringType().getSimpleName();
        if (service.endsWith(SERVICE_SUFFIX)) {
            service = service.substring(0, service.length() - SERVICE_SUFFIX.length());
        }
        return service + "." + signature.getName();
    }

    private static long elapsedMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }
}
