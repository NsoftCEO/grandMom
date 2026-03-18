package ko.dh.goot.auth.domain;

import java.util.Locale;

public enum UserRole {
    ROLE_USER,
    ROLE_ADMIN;

    /**
     * DB 문자열에서 안전하게 enum으로 변환
     * @param roleStr DB에 저장된 문자열 (예: "ROLE_USER")
     * @return UserRole enum
     */
    public static UserRole fromString(String roleStr) {
        if (roleStr == null || roleStr.isBlank()) {
            throw new IllegalArgumentException("Role string is null or empty");
        }
        try {
            return UserRole.valueOf(roleStr.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid role value from DB: " + roleStr, e);
        }
    }

    /**
     * Spring Security에서 사용할 권한 문자열 반환
     * @return "ROLE_USER" 형식
     */
    public String toAuthority() {
        return this.name();
    }
}