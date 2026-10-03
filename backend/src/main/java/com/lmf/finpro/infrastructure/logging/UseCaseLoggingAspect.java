package com.lmf.finpro.infrastructure.logging;

import static net.logstash.logback.argument.StructuredArguments.kv;

import com.lmf.finpro.application.FlowLog;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

/**
 * Registra qual fluxo de negócio foi executado (ex.: {@code Account.create}, {@code
 * Transaction.list}), quanto demorou, o que ele tocou e se falhou — é o que liga uma requisição
 * HTTP ao que o usuário estava fazendo na tela.
 *
 * <p>Do contexto, só entram identificadores e contagens, nunca valores (têm dados pessoais e
 * financeiros): os parâmetros {@code Long} chamados {@code *Id} (menos {@code currentUserId}, que
 * já vai no MDC como {@code userId}), o {@code id} do retorno quando ele tem um e o tamanho de
 * listas retornadas. O serviço pode acrescentar desfechos que o aspecto não deduz via {@link
 * FlowLog}. A falha sai só com o tipo da exceção.
 *
 * <p>Só o fluxo de fora é registrado: serviços que chamam outros serviços não geram linhas
 * repetidas. Enquanto o fluxo roda, o MDC carrega {@code flow}, então qualquer log emitido dentro
 * dele sai com o nome do fluxo. Chamados por um scheduler, os fluxos bem-sucedidos saem em DEBUG
 * (um por item do lote encheria o log; o resumo fica na linha "Scheduler … finalizado"); as falhas
 * saem sempre.
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
    private static final String CURRENT_USER_PARAM = "currentUserId";

    @Around("execution(public * com.lmf.finpro.application..*ApplicationService.*(..))")
    public Object logFlow(ProceedingJoinPoint joinPoint) throws Throwable {
        if (MDC.get(MDC_FLOW) != null) {
            return joinPoint.proceed();
        }
        String flow = flowName(joinPoint);
        boolean inScheduler = MDC.get("scheduler") != null;
        MDC.put(MDC_FLOW, flow);
        FlowLog.begin();
        long start = System.nanoTime();
        try {
            Object result = joinPoint.proceed();
            Object[] args = arguments(flow, start, null, joinPoint, result);
            String message = "Fluxo {} concluído {}" + " {}".repeat(args.length - 2);
            if (inScheduler) {
                log.debug(message, args);
            } else {
                log.info(message, args);
            }
            return result;
        } catch (Throwable ex) {
            Object[] args = arguments(flow, start, ex, joinPoint, null);
            log.warn("Fluxo {} falhou {} {}" + " {}".repeat(args.length - 3), args);
            throw ex;
        } finally {
            FlowLog.end();
            MDC.remove(MDC_FLOW);
        }
    }

    /**
     * Fluxo, (tipo do erro, se falhou), duração e o contexto: o deduzido de parâmetros/retorno e,
     * por cima, o que o serviço registrou via {@link FlowLog}.
     */
    private static Object[] arguments(
            String flow,
            long start,
            Throwable error,
            ProceedingJoinPoint joinPoint,
            Object result) {
        List<Object> args = new ArrayList<>();
        args.add(flow);
        if (error != null) {
            args.add(kv("error", error.getClass().getSimpleName()));
        }
        args.add(kv("durationMs", elapsedMs(start)));
        Map<String, Object> context = new LinkedHashMap<>();
        collectIds(joinPoint, context);
        collectResult(result, context);
        context.putAll(FlowLog.snapshot());
        context.forEach((key, value) -> args.add(kv(key, value)));
        return args.toArray();
    }

    private static void collectIds(ProceedingJoinPoint joinPoint, Map<String, Object> context) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] names = signature.getParameterNames();
        Object[] values = joinPoint.getArgs();
        if (names == null) {
            return;
        }
        for (int i = 0; i < names.length && i < values.length; i++) {
            if (values[i] instanceof Long
                    && names[i].endsWith("Id")
                    && !CURRENT_USER_PARAM.equals(names[i])) {
                context.put(names[i], values[i]);
            }
        }
    }

    /** O {@code id} do que foi criado/devolvido e, para listas, quantos itens vieram. */
    private static void collectResult(Object result, Map<String, Object> context) {
        if (result == null) {
            return;
        }
        if (result instanceof Collection<?> collection) {
            context.put("resultCount", collection.size());
            return;
        }
        try {
            Method idAccessor = result.getClass().getMethod("id");
            if (idAccessor.getParameterCount() == 0
                    && Long.class.equals(idAccessor.getReturnType())) {
                Object id = idAccessor.invoke(result);
                if (id != null) {
                    context.put("resultId", id);
                }
            }
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // Retorno sem id() público (DTO, resumo, Optional…): sem contexto extra, sem erro.
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
