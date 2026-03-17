package ko.dh.goot.security.jwt;

public interface TokenHasher {
    String hash(String token);
}