package com.lmf.finpro.infrastructure.security;

import com.lmf.finpro.domain.exception.InvalidTokenException;
import com.lmf.finpro.domain.model.HouseholdMembership;
import com.lmf.finpro.domain.port.out.HouseholdRepositoryPort;
import com.lmf.finpro.domain.port.out.TokenClaims;
import com.lmf.finpro.domain.port.out.TokenPort;
import com.lmf.finpro.domain.port.out.UserRepositoryPort;
import com.lmf.finpro.infrastructure.logging.RequestIdFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Extrai e valida o JWT do header Authorization e popula o SecurityContext. Não usa
 * UserDetailsService: login/registro são casos de uso de aplicação que já conhecem o usuário, então
 * o principal aqui é só o {@link AuthenticatedUser} do token.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    /** Escolhe qual grupo o usuário está vendo; sem ele vale o espaço pessoal. */
    public static final String HOUSEHOLD_HEADER = "X-Household-Id";

    private final TokenPort tokenPort;
    private final UserRepositoryPort userRepositoryPort;
    private final HouseholdRepositoryPort householdRepositoryPort;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith(BEARER_PREFIX)) {
            String token = header.substring(BEARER_PREFIX.length());
            try {
                TokenClaims claims = tokenPort.parse(token);
                if (!isCurrentSession(claims)) {
                    throw new InvalidTokenException("Sessão encerrada");
                }
                HouseholdMembership membership = resolveActiveMembership(request, claims.userId());
                if (membership == null) {
                    rejectUnknownHousehold(response);
                    return;
                }
                AuthenticatedUser principal =
                        new AuthenticatedUser(
                                claims.userId(),
                                claims.email(),
                                membership.householdId(),
                                membership.role());
                var authentication =
                        new UsernamePasswordAuthenticationToken(principal, null, List.of());
                SecurityContextHolder.getContext().setAuthentication(authentication);
                // Limpo pelo RequestIdFilter ao fim da requisição.
                MDC.put(RequestIdFilter.USER_MDC_KEY, String.valueOf(claims.userId()));
                MDC.put(
                        RequestIdFilter.HOUSEHOLD_MDC_KEY,
                        String.valueOf(membership.householdId()));
            } catch (InvalidTokenException ex) {
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * O grupo ativo: o do header {@value #HOUSEHOLD_HEADER}, se o usuário participa dele, senão
     * (sem header) o espaço pessoal. Devolve nulo quando o header aponta para um grupo de que o
     * usuário não faz parte. Usuário sem espaço pessoal é um estado inconsistente: o token é
     * recusado.
     */
    private HouseholdMembership resolveActiveMembership(HttpServletRequest request, Long userId) {
        String requested = request.getHeader(HOUSEHOLD_HEADER);
        if (requested == null || requested.isBlank()) {
            return householdRepositoryPort
                    .findPersonalMembership(userId)
                    .orElseThrow(() -> new InvalidTokenException("Usuário sem espaço pessoal"));
        }
        try {
            return householdRepositoryPort
                    .findMembership(Long.parseLong(requested.trim()), userId)
                    .orElse(null);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static void rejectUnknownHousehold(HttpServletResponse response) throws IOException {
        SecurityContextHolder.clearContext();
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter()
                .write(
                        "{\"status\":403,\"error\":\"Forbidden\",\"message\":\"Você não participa"
                                + " deste grupo\"}");
    }

    /**
     * Token assinado e dentro da validade ainda pode ser de uma sessão encerrada: a troca de senha
     * incrementa a versão de sessão do usuário (ver User#withPasswordHash). Usuário excluído também
     * derruba o token.
     */
    private boolean isCurrentSession(TokenClaims claims) {
        return userRepositoryPort
                .findSessionVersion(claims.userId())
                .map(current -> current == claims.sessionVersion())
                .orElse(false);
    }
}
