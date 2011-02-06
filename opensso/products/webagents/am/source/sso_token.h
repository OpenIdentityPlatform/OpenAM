/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
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
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: sso_token.h,v 1.3 2008/06/25 08:14:37 qcheng Exp $
 *
 * Abstract:
 * 
 * Class representing a AM SSOToken.
 */ 

#ifndef SSO_TOKEN_H
#define SSO_TOKEN_H

#include <string>
#include "internal_macros.h"
#include "http.h"

BEGIN_PRIVATE_NAMESPACE

class SSOToken {
public:
    static const SSOToken INVALID_SSOTOKEN;

    SSOToken()
	: token(), encodedToken()
    {
    }

    SSOToken(const std::string &tok) {
	if(tok.find('%') != std::string::npos) {
	    token = Http::decode(tok);
	    encodedToken = tok;
	} else {
	    token = tok;
	    encodedToken = Http::encode(tok);
	}
    }

    SSOToken(const std::string& tok, const std::string& encodedTok)
	: token(tok), encodedToken(encodedTok)
    {
    }

    bool isValid() const {
	return *this != INVALID_SSOTOKEN;
    }

    bool operator==(const SSOToken& rhs) const {
	return token == rhs.token;
    }

    bool operator!=(const SSOToken& rhs) const {
	return token != rhs.token;
    }

    const std::string &getString() const {
	return token;
    }

    const std::string &getString() {
	return token;
    }

    const std::string &getEncodedString() const {
	return encodedToken;
    }

private:
    friend class AuthService;
    friend class SessionInfo;

    std::string token;
    std::string encodedToken;
};

END_PRIVATE_NAMESPACE

#endif	// not SSO_TOKEN_H
