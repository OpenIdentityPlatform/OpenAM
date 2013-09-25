/**
 * Copyright 2013 ForgeRock, Inc.
 *
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 */
package org.forgerock.openam.cts.api.tokens;

/**
 * This wrapper class wraps up the various fields of the SAML tokens and contains them in a single class.
 * This allows the SAML use-case of the Core Token Service to be modelled via the TokenAdapter interface.
 *
 * The SAML tokens do not follow any hierarchy and so are of type Object. This doesn't cause a problem
 * for the Core Token Service as the class information is persisted alongside the object.
 *
 * @author robert.wapshott@forgerock.com
 */
public class SAMLToken {
    private String primaryKey;
    private String secondaryKey;
    private long expiryTime;
    private Object token;

    /**
     * Create a new instance of the SAMLToken.
     *
     * @param primaryKey Primary key used as the Token ID.
     * @param secondaryKey Secondary key, used for searching over SAML Tokens.
     * @param expiryTime Expiry time mapped to Token Expiry Time.
     * @param token Object to be stored.
     */
    public SAMLToken(String primaryKey, String secondaryKey, long expiryTime, Object token) {
        this.primaryKey = primaryKey;
        this.secondaryKey = secondaryKey;
        this.expiryTime = expiryTime;
        this.token = token;
    }

    public String getPrimaryKey() {
        return primaryKey;
    }

    public String getSecondaryKey() {
        return secondaryKey;
    }

    /**
     * @return The timestamp in 'epoched seconds'.
     */
    public long getExpiryTime() {
        return expiryTime;
    }

    public Object getToken() {
        return token;
    }
}
