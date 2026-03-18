package ko.dh.goot.security.jwt;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import ko.dh.goot.auth.domain.Role;


@Component
public class JwtProvider {

    // Claim key constants
    private static final String CLAIM_ROLES = "roles";

    // 기본값: properties를 통해 주입
    private final long accessExpireMs;
    private final long refreshExpireMs;
    private final SecretKey key;
    private final JwtParser parser;
    private final Clock clock;
    private final String issuer;

    /**
     * 토큰 상태 반환용 enum
     */
    public enum TokenStatus {
        VALID,
        EXPIRED,
        INVALID
    }


    public JwtProvider(
            @Value("${jwt.access-exp-ms}") long accessExpireMs,
            @Value("${jwt.refresh-exp-ms}") long refreshExpireMs,
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.issuer:goot-api}") String issuer,
            Clock clock
    ) {
        if (secret == null || secret.trim().isEmpty()) {
            throw new IllegalStateException("jwt.secret is not configured. Please set jwt.secret in properties.");
        }
        if (accessExpireMs <= 0) {
            throw new IllegalStateException("jwt.access-exp-ms must be > 0 (ms). Current: " + accessExpireMs);
        }
        if (refreshExpireMs <= 0) {
            throw new IllegalStateException("jwt.refresh-exp-ms must be > 0 (ms). Current: " + refreshExpireMs);
        }

        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) { // 권장: 256bit / 32 bytes 이상
            throw new IllegalStateException("The configured jwt.secret is too short. Use a longer secret (recommended >= 32 bytes).");
        }

        this.accessExpireMs = accessExpireMs;
        this.refreshExpireMs = refreshExpireMs;
        this.issuer = (issuer == null || issuer.isBlank()) ? "goot-api" : issuer;
        this.clock = (clock == null) ? Clock.systemUTC() : clock;
        this.key = Keys.hmacShaKeyFor(keyBytes);

        // JwtParser는 재사용하도록 생성자에서 한 번만 초기화
        this.parser = Jwts.parserBuilder()
                .setSigningKey(this.key)
                .build();
    }

    /**
     * Access Token 생성 (단일 role 버전)
     */
    public String createAccessToken(String userId, Role role) {
        return createAccessToken(userId, role, null);
    }

    public String createAccessToken(String userId, Role role, Map<String, Object> extraClaims) {

        Instant nowInstant = clock.instant();
        Date now = Date.from(nowInstant);
        Date exp = Date.from(nowInstant.plusMillis(accessExpireMs));

        Map<String, Object> baseClaims = (extraClaims == null)
                ? new HashMap<>()
                : new HashMap<>(extraClaims);

        JwtBuilder b = Jwts.builder();

        if (!baseClaims.isEmpty()) {
            b.setClaims(baseClaims);
        }

        // 🔥 핵심: ROLE_ prefix 붙여서 저장
        List<String> rolesList = (role == null)
                ? Collections.emptyList()
                : List.of(role.toAuthority());

        return b
                .setSubject(userId)
                .setIssuer(issuer)
                .setId(UUID.randomUUID().toString())
                .setIssuedAt(now)
                .setExpiration(exp)
                .claim(CLAIM_ROLES, rolesList)
                .signWith(key)
                .compact();
    }

    /**
     * Refresh Token (JWT 아님) - UUID 사용
     * 실제 만료/저장은 DB/Redis에서 TTL로 관리
     */
    public String createRefreshToken() {
        return UUID.randomUUID().toString();
    }

    /**
     * Bearer 토큰에서 "Bearer " 접두어 제거 유틸
     */
    public String resolveToken(String bearerToken) {
        if (bearerToken == null || bearerToken.isBlank()) return bearerToken;
        if (bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7).trim();
        }
        return bearerToken;
    }

    /**
     * 토큰 상태 반환: VALID / EXPIRED / INVALID
     */
    public TokenStatus validateTokenStatus(String token) {
        if (token == null || token.isBlank()) return TokenStatus.INVALID;
        try {
            parser.parseClaimsJws(token);
            return TokenStatus.VALID;
        } catch (ExpiredJwtException e) {
            return TokenStatus.EXPIRED;
        } catch (JwtException | IllegalArgumentException e) {
            return TokenStatus.INVALID;
        }
    }

    /**
     * boolean 검증 (단순 유효성 체크: VALID -> true, 나머지 false)
     */
    public boolean validateToken(String token) {
        return validateTokenStatus(token) == TokenStatus.VALID;
    }

    /**
     * 만료 여부 체크
     */
    public boolean isTokenExpired(String token) {
        if (token == null || token.isBlank()) return true;
        try {
            Claims claims = parser.parseClaimsJws(token).getBody();
            Date exp = claims.getExpiration();
            return exp == null || exp.before(Date.from(clock.instant()));
        } catch (ExpiredJwtException e) {
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            // 파싱 불가한 토큰은 만료 여부를 판정할 수 없으므로 만료로 처리 (안전하게)
            return true;
        }
    }

    /**
     * 토큰에서 Claims 반환 (만료된 토큰은 ExpiredJwtException 발생)
     */
    private Claims parseClaims(String token) throws JwtException {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("JWT token is null or blank");
        }
        return parser.parseClaimsJws(token).getBody();
    }

    /**
     * 토큰에서 subject (userId) 추출
     */
    public String getUserId(String token) {
        Claims claims = parseClaims(token);
        return claims.getSubject();
    }

    /**
     * 토큰에서 roles 추출 (List<String>)
     */
    @SuppressWarnings("unchecked")
    public List<String> getRoles(String token) {
        Claims claims = parseClaims(token);
        Object v = claims.get(CLAIM_ROLES);
        if (v == null) return Collections.emptyList();
        if (v instanceof List) {
            return (List<String>) v;
        }
        // 예외적 형식(단일 문자열) 처리
        return List.of(String.valueOf(v));
    }

    /**
     * 토큰의 발급시간
     */
    public Date getIssuedAt(String token) {
        Claims claims = parseClaims(token);
        return claims.getIssuedAt();
    }

    /**
     * 토큰의 만료시간
     */
    public Date getExpiration(String token) {
        Claims claims = parseClaims(token);
        return claims.getExpiration();
    }

    /**
     * 특정 claim 조회 유틸
     */
    public <T> T getClaim(String token, String claimKey, Class<T> requiredType) {
        Claims claims = parseClaims(token);
        return claims.get(claimKey, requiredType);
    }

    /**
     * Access 토큰 만료(ms) 반환
     */
    public long getAccessExpireMs() {
        return accessExpireMs;
    }

    /**
     * Refresh 토큰 만료(ms) 반환
     */
    public long getRefreshExpireMs() {
        return refreshExpireMs;
    }
}