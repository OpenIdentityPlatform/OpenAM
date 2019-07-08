package org.openidentityplatform.openam.authentication.modules.webauthn;

import java.security.Principal;

public class WebAuthnPrincipal implements Principal {

	private String username;
	
	public WebAuthnPrincipal(String username) {
		this.username = username;
	}
	
	@Override
	public String getName() {
		return username;
	}

}
