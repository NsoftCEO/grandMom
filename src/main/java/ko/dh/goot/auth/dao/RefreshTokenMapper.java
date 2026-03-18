package ko.dh.goot.auth.dao;


import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;
import ko.dh.goot.auth.domain.RefreshToken;

@Mapper
public interface RefreshTokenMapper {

    Optional<RefreshToken> findByToken(String token);

    void deleteByUserId(String userId);

    void insertToken(RefreshToken refreshToken);

    void revokeToken(String token);
}