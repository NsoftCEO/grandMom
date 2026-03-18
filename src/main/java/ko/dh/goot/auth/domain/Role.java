package ko.dh.goot.auth.domain;

public enum Role {
    USER,
    ADMIN;

    public String toAuthority() {
        return "ROLE_" + name();
    }
}