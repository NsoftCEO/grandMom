package ko.dh.goot.security.jwt;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import ko.dh.goot.common.exception.ErrorCode;
import ko.dh.goot.security.principal.SecurityUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * JwtAuthenticationFilter
 * - 토큰만 검증하고 경량 principal(SecurityUserDetails)을 생성하여 SecurityContext에 넣음
 * - DB 조회는 하지 않음 (Service 레이어에서 필요시 수행)
 */
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String rawAuth = request.getHeader("Authorization");
        String token = jwtProvider.resolveToken(rawAuth);

        try {
            if (token == null || token.isBlank()) {
                filterChain.doFilter(request, response);
                return;
            }

            JwtProvider.TokenStatus status = jwtProvider.validateTokenStatus(token);

            if (status == JwtProvider.TokenStatus.VALID) {
                String userId = jwtProvider.getUserId(token);
                List<String> roles = jwtProvider.getRoles(token);

                // optional convenience claims (may be null)
                String email = jwtProvider.getClaim(token, "email", String.class);
                String name  = jwtProvider.getClaim(token, "name", String.class);

                // 경량 principal 생성 (DB 조회 없음)
                SecurityUserDetails principal = SecurityUserDetails.fromClaims(userId, roles, email, name);

                var authorities = roles.stream()
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());

                var authentication = new UsernamePasswordAuthenticationToken(principal, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(authentication);

                filterChain.doFilter(request, response);
                return;

            } else if (status == JwtProvider.TokenStatus.EXPIRED) {
                request.setAttribute("authError", ErrorCode.TOKEN_EXPIRED);
                SecurityContextHolder.clearContext();
                throw new AuthenticationCredentialsNotFoundException("Token expired");
            } else {
                request.setAttribute("authError", ErrorCode.INVALID_TOKEN);
                SecurityContextHolder.clearContext();
                throw new AuthenticationCredentialsNotFoundException("Invalid token");
            }

        } catch (AuthenticationCredentialsNotFoundException ex) {
            // AuthenticationEntryPoint will handle response (401)
            throw ex;
        } catch (Exception ex) {
            log.error("Unexpected JWT processing error", ex);
            request.setAttribute("authError", ErrorCode.INVALID_TOKEN);
            SecurityContextHolder.clearContext();
            throw new AuthenticationCredentialsNotFoundException("Invalid token", ex);
        }
    }
}