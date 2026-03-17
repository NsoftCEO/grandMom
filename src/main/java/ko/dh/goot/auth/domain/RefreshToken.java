package ko.dh.goot.auth.domain;

import java.time.Instant;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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

    @Column(name = "token", nullable = false, length = 500, unique = true)
    private String token;

    @Column(name = "expired_at", nullable = false)
    private Instant expiredAt;

    @Builder.Default
    @Column(name = "is_revoked", nullable = false)
    private boolean revoked = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // ============================
    // 도메인 로직
    // ============================

    public boolean isExpired() {
        return expiredAt.isBefore(Instant.now());
    }

    public void revoke() {
        this.revoked = true;
    }
}