package ko.dh.goot.auth.dao;

import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import ko.dh.goot.auth.domain.RefreshToken;

@Mapper
public interface RefreshTokenMapper {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    void insertToken(RefreshToken refreshToken);

    int deleteByUserId(String userId); 

    int revokeToken(@Param("tokenHash") String tokenHash, @Param("reason") String reason);

    int revokeFamily(@Param("familyId") String familyId, @Param("reason") String reason);

    int revokeByUserIdAndDeviceIdAndDeviceType(
            @Param("userId") String userId, 
            @Param("deviceId") String deviceId, 
            @Param("deviceType") String deviceType, 
            @Param("reason") String reason
    );

    int deleteExpiredAndOldRevokedTokens();
}