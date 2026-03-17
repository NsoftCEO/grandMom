package ko.dh.goot.security.jwt;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;

@Component
public class JwtProvider {

    // 기본값: 30분
    @Value("${jwt.access-exp-ms}")
    private long accessExpireMs;

    // 기본값: 7일
    @Value("${jwt.refresh-exp-ms}")
    private long refreshExpireMs;

    @Value("${jwt.secret}")
    private String secret;

    private SecretKey key;

    @PostConstruct
    void init() {
        if (secret == null || secret.trim().isEmpty()) {
            throw new IllegalStateException("jwt.secret is not configured. Please set jwt.secret in properties.");
        }

        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);

        // 최소 길이 체크 (HS256의 안전한 키 길이를 보장하기 위해 권장 길이 체크)
        // Keys.hmacShaKeyFor 자체가 짧은 키에 대해 예외를 던지지만, 사용자에게 명확한 메시지를 주기 위함.
        if (keyBytes.length < 32) { // 256bit(32bytes) 권장
            throw new IllegalStateException("The configured jwt.secret is too short. Use a longer secret (recommended >= 32 bytes).");
        }
        
        if (accessExpireMs <= 0) {
            throw new IllegalStateException("jwt.access-exp-ms must be > 0 (ms). Current: " + accessExpireMs);
        }
        if (refreshExpireMs <= 0) {
            throw new IllegalStateException("jwt.refresh-exp-ms must be > 0 (ms). Current: " + refreshExpireMs);
        }

        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Access Token 생성
     * 기본 claim: subject(userId), role
     *
     * @param userId subject (여기서는 String userId)
     * @param role role claim
     * @return JWT access token
     */
    public String createAccessToken(String userId, String role) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + accessExpireMs);

        return Jwts.builder()
                .setSubject(userId)
                .setIssuedAt(now)
                .setExpiration(exp)
                .claim("role", role)
                .signWith(key)
                .compact();
    }

    /**
     * Access Token 생성(추가 claims 제공용)
     *
     * @param userId subject
     * @param role role
     * @param extraClaims 추가 클레임(예: name, email 등)
     * @return JWT access token
     */
    public String createAccessToken(String userId, String role, Map<String, Object> extraClaims) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + accessExpireMs);

        io.jsonwebtoken.JwtBuilder b = Jwts.builder()
                .setSubject(userId)
                .setIssuedAt(now)
                .setExpiration(exp)
                .claim("role", role);

        if (extraClaims != null && !extraClaims.isEmpty()) {
            b.addClaims(extraClaims);
        }

        return b.signWith(key).compact();
    }

    /**
     * Refresh Token은 UUID (JWT 아님).
     * 만료/저장은 외부(DB/Redis)에서 TTL로 관리.
     */
    public String createRefreshToken() {
        return UUID.randomUUID().toString();
    }

    /**
     * 토큰이 유효한지(파싱 가능한지) 확인.
     * 만료된 토큰도 false를 반환 (만료을 구분하려면 validateAndGetClaims 사용)
     */
    public boolean validateToken(String token) {
        if (token == null || token.isBlank()) return false;
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    /**
     * 토큰이 만료되었는지 여부 반환.
     * 만료된 경우 true 반환, 그 외(파싱 불가 등)도 false 또는 예외로 처리 가능.
     */
    public boolean isTokenExpired(String token) {
        try {
            Date exp = getExpiration(token);
            return exp != null && exp.before(new Date());
        } catch (ExpiredJwtException e) {
            // 이미 만료됐으면 예외가 발생하므로 만료로 처리
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            // 파싱 불가(유효하지 않은 토큰) — 만료 여부를 알 수 없음
            return false;
        }
    }

    /**
     * 토큰에서 subject (userId) 추출
     */
    public String getUserId(String token) {
        Claims claims = parseClaims(token);
        return claims.getSubject();
    }

    /**
     * 토큰에서 role 추출
     */
    public String getRole(String token) {
        Claims claims = parseClaims(token);
        return claims.get("role", String.class);
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
     * 토큰 파싱(Claims 반환). 만료된 토큰은 ExpiredJwtException 발생.
     * 내부적으로 parseClaimsJws 사용.
     */
    private Claims parseClaims(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("JWT token is null or blank");
        }
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * Access 토큰 만료(ms) 반환 (서비스에서 응답에 사용)
     */
    public long getAccessExpireMs() {
        return accessExpireMs;
    }

    /**
     * Refresh 토큰 만료(ms) 반환 (DB/Redis TTL 설정에 사용)
     */
    public long getRefreshExpireMs() {
        return refreshExpireMs;
    }
}