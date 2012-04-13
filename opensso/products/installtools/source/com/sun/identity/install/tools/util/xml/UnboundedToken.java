/**
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
 * $Id: UnboundedToken.java,v 1.2 2008/06/25 05:51:31 qcheng Exp $
 *
 */

package com.sun.identity.install.tools.util.xml;

/**
 * Represents an unbounded string fragment of an XML document. An Unbounded
 * token string is defined as a string of characters that is surronded by a
 * bounded token on both sides or either side if this string of characters is 
 * at the begining or the end of the document.
 */
public class UnboundedToken extends Token {

    UnboundedToken(String tokenString) {
        super(tokenString);
    }

    String getValue() {
        return getTokenString().trim();
    }

    void setValue(String value) {
        String tokenString = getTokenString();
        String prefix = tokenString.substring(0, tokenString
                .indexOf(tokenString.trim()));
        String suffix = tokenString.substring(prefix.length()
                + tokenString.trim().length());

        setTokenString(prefix + value + suffix);
    }

    protected String getTokenTypeString() {
        return TOKEN_TYPE_UNBOUNDED + "(=> " + getValue() + ")";
    }

}
