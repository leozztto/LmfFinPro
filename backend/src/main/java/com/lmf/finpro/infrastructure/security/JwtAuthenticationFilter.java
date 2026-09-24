package com.lmf.finpro.infrastructure.security;

import com.lmf.finpro.domain.exception.InvalidTokenException;
import com.lmf.finpro.domain.port.out.TokenClaims;
import com.lmf.finpro.domain.port.out.TokenPort;
import com.lmf.finpro.domain.port.out.UserRepositoryPort;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
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

    private final TokenPort tokenPort;
    private final UserRepositoryPort userRepositoryPort;

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
                AuthenticatedUser principal =
                        new AuthenticatedUser(claims.userId(), claims.email());
                var authentication =
                        new UsernamePasswordAuthenticationToken(principal, null, List.of());
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (InvalidTokenException ex) {
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
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
