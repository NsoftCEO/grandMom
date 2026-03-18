package ko.dh.goot.security.jwt;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
import ko.dh.goot.security.principal.UserPrincipal;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private final JwtProvider jwtProvider;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		try {
			// JwtProvider의 resolveToken 사용
			String token = jwtProvider.resolveToken(request.getHeader("Authorization"));

			if (token != null && !token.isBlank()) {
				JwtProvider.TokenStatus status = jwtProvider.validateTokenStatus(token);

				if (status == JwtProvider.TokenStatus.VALID) {
					// 토큰이 유효한 경우에만 Claims에서 값 추출
					String userId = jwtProvider.getUserId(token);
					List<String> roles = jwtProvider.getRoles(token);

					// 기존 UserPrincipal 생성자와의 호환성 유지: primary role 사용
					String primaryRole = roles.isEmpty() ? "ROLE_USER" : roles.get(0);
					UserPrincipal principal = new UserPrincipal(userId, primaryRole);

					// authorities 생성 (roles 전체 반영)
					List<SimpleGrantedAuthority> authorities = new ArrayList<>();
					for (String r : roles) {
						authorities.add(new SimpleGrantedAuthority(r));
					}

					UsernamePasswordAuthenticationToken authentication =
							new UsernamePasswordAuthenticationToken(principal, null, authorities);

					SecurityContextHolder.getContext().setAuthentication(authentication);
				} else if (status == JwtProvider.TokenStatus.EXPIRED) {
					request.setAttribute("authError", ErrorCode.TOKEN_EXPIRED);
					throw new AuthenticationCredentialsNotFoundException("Token expired");
				} else { // INVALID
					request.setAttribute("authError", ErrorCode.INVALID_TOKEN);
					throw new AuthenticationCredentialsNotFoundException("Invalid token");
				}
			}

			filterChain.doFilter(request, response);

		} catch (AuthenticationCredentialsNotFoundException e) {
			// 이미 authError 속성 설정된 상태에서 예외를 던짐
			throw e;
		} catch (Exception e) {
			// 파싱 도중의 예기치 않은 오류 처리: 토큰 무효로 간주
			request.setAttribute("authError", ErrorCode.INVALID_TOKEN);
			throw new AuthenticationCredentialsNotFoundException("Invalid token", e);
		}
	}
}