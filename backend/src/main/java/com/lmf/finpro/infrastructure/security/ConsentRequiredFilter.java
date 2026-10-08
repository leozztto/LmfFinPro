package com.lmf.finpro.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lmf.finpro.application.legal.ConsentGuard;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Recusa as chamadas de quem ainda não aceitou as versões vigentes dos Termos e da Política (LGPD).
 * A tela de aceite do frontend é só a parte visível: sem este filtro, quem chamasse a API direto
 * usaria o serviço sem ter aceitado.
 *
 * <p>Roda depois do {@link JwtAuthenticationFilter}; requisição sem autenticação passa (quem decide
 * é a autorização). Ficam liberados o que a pessoa precisa para aceitar, sair, baixar os dados e
 * excluir a conta, e as consultas de leitura do próprio cadastro e dos grupos de que o app precisa
 * para montar a tela. Responde {@code 428} com o código {@value #ERROR_CODE}, que o frontend usa
 * para abrir a tela de aceite mesmo que as versões mudem no meio da sessão.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ConsentRequiredFilter extends OncePerRequestFilter {

    public static final String ERROR_CODE = "CONSENT_REQUIRED";

    private static final List<String> ALWAYS_ALLOWED_PREFIXES =
            List.of("/api/auth/", "/api/legal/", "/api/consents", "/api/privacy/");
    private static final List<String> READ_ONLY_ALLOWED_PREFIXES =
            List.of("/api/profile", "/api/households");

    private final ConsentGuard consentGuard;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null
                && authentication.getPrincipal() instanceof AuthenticatedUser user
                && !isAllowedWithoutConsent(request)
                && !consentGuard.hasAcceptedCurrentVersions(user.userId())) {
            log.debug(
                    "Chamada recusada: aceite dos documentos legais pendente usuário={}",
                    user.userId());
            reject(request, response);
            return;
        }
        filterChain.doFilter(request, response);
    }

    private static boolean isAllowedWithoutConsent(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (ALWAYS_ALLOWED_PREFIXES.stream().anyMatch(path::startsWith)) {
            return true;
        }
        boolean readOnly =
                HttpMethod.GET.matches(request.getMethod())
                        || HttpMethod.HEAD.matches(request.getMethod());
        return readOnly && READ_ONLY_ALLOWED_PREFIXES.stream().anyMatch(path::startsWith);
    }

    private void reject(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.PRECONDITION_REQUIRED.value());
        body.put("error", HttpStatus.PRECONDITION_REQUIRED.getReasonPhrase());
        body.put("code", ERROR_CODE);
        body.put(
                "message",
                "Aceite os Termos de Uso e a Política de Privacidade vigentes para continuar.");
        body.put("path", request.getRequestURI());
        response.setStatus(HttpStatus.PRECONDITION_REQUIRED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
