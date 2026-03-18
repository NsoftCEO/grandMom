package ko.dh.goot.user.domain;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import ko.dh.goot.auth.domain.UserRole;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString
public class User {

    @Id
    @Column(name = "user_id", length = 36, nullable = false)
    private String userId;

    @Column(length = 100, nullable = false)
    private String name;

    @Column(length = 255, nullable = false, unique = true)
    private String email;

    @Column(length = 255)
    private String password;

    @Column(length = 30)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role; // ROLE_USER, ROLE_ADMIN

    @Column(length = 20, nullable = false)
    private String status; // ACTIVE, INACTIVE, WITHDRAWN

    @Column(name = "login_type", length = 20, nullable = false)
    private String loginType; // LOCAL, SOCIAL

    @Column(length = 20)
    private String provider; // GOOGLE, KAKAO, NAVER 등

    @Column(name = "provider_id", length = 100)
    private String providerId;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * JPA 생명주기 콜백: 저장되기 전에 실행 (UUID 및 기본값 세팅)
     */
    @PrePersist
    protected void onCreate() {
        if (this.userId == null) {
            this.userId = UUID.randomUUID().toString();
        }
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    /**
     * JPA 생명주기 콜백: 업데이트되기 전에 실행
     */
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    @Builder
    public User(String name, String email, String password, String phone,
                UserRole role, String status, String loginType,
                String provider, String providerId) {

        if (name == null || name.isBlank()) throw new IllegalArgumentException("이름은 필수입니다.");
        if (email == null || email.isBlank()) throw new IllegalArgumentException("이메일은 필수입니다.");

        this.name = name;
        this.email = email;
        this.password = password;
        this.phone = phone;
        this.role = role != null ? role : UserRole.ROLE_USER;
        this.status = status != null ? status : "ACTIVE";
        this.loginType = loginType != null ? loginType : "LOCAL";
        this.provider = provider;
        this.providerId = providerId;
    }

    // =========================================================================
    // [DDD] 도메인 비즈니스 로직
    // =========================================================================

    /**
     * 내 정보 수정 (이름, 전화번호)
     */
    public void updateProfile(String name, String phone) {
        if (name != null && !name.isBlank()) {
            this.name = name;
        }
        if (phone != null && !phone.isBlank()) {
            this.phone = phone;
        }
    }

    /**
     * 비밀번호 변경
     */
    public void changePassword(String encodedPassword) {
        if (encodedPassword == null || encodedPassword.isBlank()) {
            throw new IllegalArgumentException("변경할 비밀번호가 없습니다.");
        }
        this.password = encodedPassword;
    }

    /**
     * 마지막 로그인 시간 갱신
     */
    public void updateLastLogin() {
        this.lastLoginAt = LocalDateTime.now();
    }

    /**
     * 회원 탈퇴 (소프트 딜리트)
     */
    public void withdraw() {
        this.status = "WITHDRAWN";
    }

    /**
     * 권한 변경
     */
    public void changeRole(UserRole newRole) {
        this.role = newRole;
    }
}