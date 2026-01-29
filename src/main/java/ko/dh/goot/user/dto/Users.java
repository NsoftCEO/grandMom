package ko.dh.goot.user.dto;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.ToString;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "password") // 로그에 비밀번호 노출 방지
public class Users {

    private String userId;          // PK (UUID 권장)
    private String name;            // 사용자 이름
    private String password;        // 암호화된 비밀번호
    private String email;           // 로그인 ID
    private String phone;           // 휴대폰 번호

    private String role;            // ROLE_USER, ROLE_ADMIN
    private String status;          // ACTIVE, BLOCKED, DELETED

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
