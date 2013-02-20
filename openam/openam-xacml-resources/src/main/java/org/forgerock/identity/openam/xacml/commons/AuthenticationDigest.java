/**
 *
 ~ DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 ~
 ~ Copyright (c) 2011-2013 ForgeRock AS. All Rights Reserved
 ~
 ~ The contents of this file are subject to the terms
 ~ of the Common Development and Distribution License
 ~ (the License). You may not use this file except in
 ~ compliance with the License.
 ~
 ~ You can obtain a copy of the License at
 ~ http://forgerock.org/license/CDDLv1.0.html
 ~ See the License for the specific language governing
 ~ permission and limitations under the License.
 ~
 ~ When distributing Covered Code, include this CDDL
 ~ Header Notice in each file and include the License file
 ~ at http://forgerock.org/license/CDDLv1.0.html
 ~ If applicable, add the following below the CDDL Header,
 ~ with the fields enclosed by brackets [] replaced by
 ~ your own identifying information:
 ~ "Portions Copyrighted [year] [name of copyright owner]"
 *
 */
package org.forgerock.identity.openam.xacml.commons;

import org.apache.commons.codec.digest.DigestUtils;

import java.io.Serializable;

/**
 * Provides simple POJO for handling Incoming Digest Response for Authentication.
 *
 * @author jeff.schenk@forgerock.com
 *
 */
public class AuthenticationDigest implements Serializable {

    /**
     * Request Method, Get, Post, Put, Delete
     */
    private final String method;
    private final String ha1;

    private final String qop;
    private final String ha2;

    private final String requestURI;

    private final String realm;


    public AuthenticationDigest(String method, String ha1, String qop, String ha2, String requestURI,
                                   String realm) {
        this.method = method;
        this.ha1 = ha1;
        this.qop = qop;
        this.ha2 = ha2;
        this.requestURI = requestURI;
        this.realm = realm;
    }

    public String getMethod() {
        return method;
    }

    public String getHa1() {
        return ha1;
    }

    public String getQop() {
        return qop;
    }

    public String getHa2() {
        return ha2;
    }

    public String getRequestURI() {
        return requestURI;
    }

    public String getRealm() {
        return realm;
    }
}
