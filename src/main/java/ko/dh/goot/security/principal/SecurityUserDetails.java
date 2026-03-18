package ko.dh.goot.security.principal;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import ko.dh.goot.auth.domain.UserRole;
import ko.dh.goot.common.exception.BusinessException;
import ko.dh.goot.common.exception.ErrorCode;
import ko.dh.goot.user.domain.User;

/**
 * SecurityUserDetails: 경량 UserDetails 구현
 * - fromClaims(...) : 토큰(claims)에서 생성 (필터에서 사용)
 * - fromUser(...)   : DB에서 User를 조회한 뒤 생성 (Service에서 사용 가능)
 */
public class SecurityUserDetails implements UserDetails {
    private static final long serialVersionUID = 1L;

    private final String userId;
    private final String email;    // nullable
    private final String name;     // nullable
    private final List<String> roles; // "ROLE_USER" 형태
    private final String status;   // nullable
    // DB에서 가져온(인코딩된) 패스워드. JWT 기반 principal은 null 가능.
    private final String password;
    
    private SecurityUserDetails(String userId, String email, String name, List<String> roles, String status, String password) {
        this.userId = Objects.requireNonNull(userId, "userId required");
        this.email = email;
        this.name = name;
        this.roles = roles == null ? List.of() : List.copyOf(roles);
        this.status = status;
        this.password = password;
    }

    /** 필터에서 토큰 기반으로 경량 principal 생성 (비밀번호 없음) */
    public static SecurityUserDetails fromClaims(String userId, List<String> roles, String email, String name) {
        return new SecurityUserDetails(userId, email, name, roles, null, null);
    }

    /** DB에서 User 엔티티를 조회한 뒤 principal 생성 (로그인용) */
    public static SecurityUserDetails fromUser(User user) {
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND); // 404 처리
        }

        if (user.getRole() == null) {
            throw new BusinessException(ErrorCode.USER_ROLE_INVALID, "User role is null: " + user.getUserId());
        }

        List<String> roleStrings = List.of(user.getRole().name());

        String encodedPassword = user.getPassword();
        if (encodedPassword == null || encodedPassword.isBlank()) {
            throw new BadCredentialsException("Empty encoded password for user: " + user.getUserId());
        }

        return new SecurityUserDetails(
                user.getUserId(),
                user.getEmail(),
                user.getName(),
                roleStrings,
                user.getStatus(),
                encodedPassword
        );
    }

    /** 안전한 읽기 전용 DTO */
    public UserInfo toUserInfo() {
        return new UserInfo(userId, email, name, roles, status);
    }

    public String getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    public String getName() {
        return name;
    }

    public String getStatus() {
        return status;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (roles == null || roles.isEmpty()) return Collections.emptyList();
        return roles.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList());
    }

	// 로그인(DAO) 흐름에서는 non-null 인코딩된 패스워드가 필요, JWT 기반 인증에서는 패스워드가 없음
    @Override
    public String getPassword() { 
        return password;
    }

    @Override
    public String getUsername() {
        return email != null ? email : userId;
    }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return "ACTIVE".equals(status); }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return !"WITHDRAWN".equals(status); }

    public static record UserInfo(
            String userId,
            String email,
            String name,
            List<String> roles,
            String status
    ) {}
}