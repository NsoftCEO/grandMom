package ko.dh.goot.auth.domain;

public enum UserRole {
    ROLE_USER,
    ROLE_ADMIN;

    /**
     * Spring Security에서 사용할 권한 문자열 반환
     * @return "ROLE_USER" 형식
     */
    public String toAuthority() {
        return this.name();
    }
}