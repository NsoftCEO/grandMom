package ko.dh.goot.auth;

import java.io.Serializable;

public record UserPrincipal(
	    String userId,
	    String role
	) implements Serializable {}