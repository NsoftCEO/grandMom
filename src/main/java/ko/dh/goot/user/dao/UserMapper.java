package ko.dh.goot.user.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import ko.dh.goot.user.dto.User;

@Mapper
public interface UserMapper {

    User findByEmail(@Param("email") String email);

    User findByProvider(
        @Param("provider") String provider,
        @Param("providerId") String providerId
    );

    void insert(User user);

    void updateLastLogin(@Param("userId") String userId);
}
