package ko.dh.goot.security.principal;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import ko.dh.goot.user.domain.User;

public class SecurityUserDetails implements UserDetails {

    private final User user;

    public SecurityUserDetails(User user) {
        this.user = user;
    }

    public String getUserId() {
        return user.getUserId();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // 엔티티의 Role 정보를 시큐리티 권한으로 변환
        return Collections.singletonList(new SimpleGrantedAuthority(user.getRole()));
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        // status가 ACTIVE인 경우에만 잠기지 않은 것으로 간주
        return "ACTIVE".equals(user.getStatus());
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        // 탈퇴(WITHDRAWN) 상태가 아니면 활성화된 계정으로 간주
        return !"WITHDRAWN".equals(user.getStatus());
    }
}
