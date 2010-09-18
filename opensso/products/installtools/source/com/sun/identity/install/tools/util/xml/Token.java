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
 * $Id: Token.java,v 1.2 2008/06/25 05:51:31 qcheng Exp $
 *
 */

package com.sun.identity.install.tools.util.xml;

/**
 * A <code>Token</code> represents a string fragment from within the XML
 * document.
 */
public abstract class Token implements IXMLUtilsConstants {

    /**
     * Constructs a Token instance using the given string representation of the
     * token.
     * 
     * @param tokenString
     *            the string representation of the token.
     */
    protected Token(String tokenString) {
        setTokenString(tokenString);
    }

    /**
     * Returns the string representation of this token. If an XML document 
     * parse resulted in <code>n</code> tokens, then concatenating the
     * <code>toString()</code> of each of these tokens in the give order will
     * result in the creation of the exact same XML document. If the token was
     * deleted, an empty string is returned.
     * 
     * @return the string representation of this token, or an empty string if
     *         the token has been marked deleted.
     */
    public String toString() {
        return (isDeleted() ? "" : getTokenString());
    }

    /**
     * Returns the string representation of the token
     * 
     * @return
     */
    protected String getTokenString() {
        return tokenString;
    }

    /**
     * Allows the specialized tokens to manipulate the token string as needed.
     * 
     * @param tokenString
     *            the string representation of the token that will be set from
     *            this point onwards.
     */
    protected void setTokenString(String tokenString) {
        this.tokenString = tokenString;
    }

    /**
     * Returns a string representation of this token including any specific
     * token related details along with it.
     * 
     * @return a string representation of this token including debug 
     * information
     */
    public String toDebugString() {
        return "[" + getTokenTypeString() + ":" + getTokenIndex() + "]: "
                + toString();
    }

    int getTokenIndex() {
        return index;
    }

    void setTokenIndex(int index) {
        this.index = index;
    }

    void markDeleted() {
        deleted = true;
    }

    boolean isDeleted() {
        return deleted;
    }

    protected abstract String getTokenTypeString();

    private String tokenString;

    private int index;

    private boolean deleted = false;
}
