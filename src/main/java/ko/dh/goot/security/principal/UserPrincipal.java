package ko.dh.goot.security.principal;

import java.io.Serializable;

public record UserPrincipal(
	    String userId,
	    String role
	) implements Serializable {}