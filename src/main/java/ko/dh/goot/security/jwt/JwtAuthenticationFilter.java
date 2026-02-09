package ko.dh.goot.security.jwt;

import java.io.IOException;
import java.util.List;

import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import ko.dh.goot.auth.UserPrincipal;
import ko.dh.goot.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private final JwtProvider jwtProvider;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		try {
			String token = resolveToken(request);

			if (token != null && jwtProvider.validateToken(token)) {

				String userId = jwtProvider.getUserId(token);
				String role = jwtProvider.getRole(token);

				UserPrincipal principal = new UserPrincipal(userId, role);

				UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(principal,
						null, List.of(new SimpleGrantedAuthority(role)));

				SecurityContextHolder.getContext().setAuthentication(authentication);
			}

			filterChain.doFilter(request, response);
		} catch (ExpiredJwtException e) {
			request.setAttribute("authError", ErrorCode.TOKEN_EXPIRED);
			throw new AuthenticationCredentialsNotFoundException("Token expired", e);

		} catch (JwtException | IllegalArgumentException e) {
			request.setAttribute("authError", ErrorCode.INVALID_TOKEN);
			throw new AuthenticationCredentialsNotFoundException("Invalid token", e);
		}
	}

	private String resolveToken(HttpServletRequest request) {
		String bearer = request.getHeader("Authorization");
		if (bearer != null && bearer.startsWith("Bearer ")) {
			return bearer.substring(7);
		}
		return null;
	}
}
