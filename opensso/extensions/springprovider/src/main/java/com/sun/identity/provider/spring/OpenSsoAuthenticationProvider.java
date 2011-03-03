/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * https://opensso.dev.java.net/public/CDDLv1.0.html or
 * opensso/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at opensso/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * 
 * "Portions Copyrighted 2008 Robert Dale <robdale@gmail.com>"
 *
 * $Id: OpenSsoAuthenticationProvider.java,v 1.1 2008/09/15 18:19:46 robdale Exp $
 *
 */
package com.sun.identity.provider.spring;

import org.acegisecurity.Authentication;
import org.acegisecurity.AuthenticationException;
import org.acegisecurity.providers.AuthenticationProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpenSsoAuthenticationProvider implements AuthenticationProvider {

	private final Logger log = LoggerFactory.getLogger(OpenSsoAuthenticationProvider.class);

	public Authentication authenticate(Authentication authentication) throws AuthenticationException {
		log.debug("Authentication: {}", authentication);
		return authentication;
	}

	public boolean supports(Class authentication) {
		log.debug("Class: {}", authentication);
		return true;
	}

}
