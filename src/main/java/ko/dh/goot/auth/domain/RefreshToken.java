package ko.dh.goot.auth.domain;

import java.time.Instant;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "refresh_token")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "refresh_token_id")
    private Long id;

    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    /**
     * DB 스키마는 token 길이 255로 설정되어 있음 (해시 저장)
     */
    @Column(name = "token", nullable = false, length = 255, unique = true)
    private String token;

    /**
     * 토큰 발급 시각 (DB default CURRENT_TIMESTAMP 또는 @PrePersist에서 설정)
     */
    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt;

    /**
     * 토큰 만료 시각
     */
    @Column(name = "expired_at", nullable = false)
    private Instant expiredAt;

    /**
     * 폐기 여부
     */
    @Builder.Default
    @Column(name = "revoked", nullable = false)
    private boolean revoked = false;

    /**
     * 폐기된 시각 (로그용)
     */
    @Column(name = "revoked_at")
    private Instant revokedAt;

    // =========================
    // device / client metadata
    // =========================

    @Column(name = "device_id", length = 128)
    private String deviceId;

    @Column(name = "device_type", length = 20)
    private String deviceType; // e.g. WEB, IOS, ANDROID

    @Column(name = "device_name", length = 255)
    private String deviceName;

    @Column(name = "user_agent", length = 512)
    private String userAgent;

    /**
     * 레코드 생성시점 (DB의 DEFAULT CURRENT_TIMESTAMP와 동일한 역할)
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // -------------------------
    // 콜백: 발급시간 기본값 보장
    // -------------------------
    @PrePersist
    protected void prePersist() {
        if (this.issuedAt == null) {
            this.issuedAt = Instant.now();
        }
    }

    // ============================
    // 도메인 로직
    // ============================

    /**
     * 토큰 만료 여부
     */
    public boolean isExpired() {
        return expiredAt == null || expiredAt.isBefore(Instant.now());
    }

    /**
     * 페기: revoked=true, revokedAt 기록
     */
    public void revoke() {
        this.revoked = true;
        this.revokedAt = Instant.now();
    }

    /**
     * 토큰 교체(회전)용 유틸: 새 토큰 값을 엔티티에 적용할 때 사용 가능
     * (선택적 편의 메서드)
     */
    public void rotateTo(String newHashedToken, Instant newExpiredAt) {
        this.token = newHashedToken;
        this.expiredAt = newExpiredAt;
        this.revoked = false;
        this.revokedAt = null;
        this.issuedAt = Instant.now();
    }
}