package ko.dh.goot.auth.dao;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import ko.dh.goot.auth.domain.RefreshToken;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String token);

    /**
     * 특정 user의 모든 리프레시 토큰 삭제 (로그아웃 시 사용)
     */
    void deleteByUserId(String userId);

    /**
     * userId로 토큰 조회(보통 최신 토큰 확인용)
     */
    Optional<RefreshToken> findByUserId(String userId);
}