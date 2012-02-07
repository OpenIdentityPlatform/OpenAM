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
 * $Id: BoundedToken.java,v 1.2 2008/06/25 05:51:30 qcheng Exp $
 *
 */

package com.sun.identity.install.tools.util.xml;

/**
 * Represents a bounded string fragment of an XML document. A bounded string
 * framgement is defined as a set of characters that begins with a '<' and end
 * with '>' character.
 */
public class BoundedToken extends Token {

    BoundedToken(String tokenString) throws Exception {
        super(tokenString);

        String nameStr = tokenString.replace('<', ' ').replace('>', ' ')
                .replace('/', ' ').trim();
        if (nameStr.indexOf(' ') == -1) {
            setName(nameStr);
        } else {
            setName(nameStr.substring(0, nameStr.indexOf(' ')));
        }

        String startTypeStr = tokenString.substring(1).trim();
        if (startTypeStr.startsWith("/")) {
            setEndMarker();
        } else {
            setStartMarker();
        }

        String endTypeStr = tokenString.substring(0, tokenString.length() - 1)
                .trim();
        if (endTypeStr.endsWith("/")) {
            setEndMarker();
        }

        String attrString = tokenString.substring(1, tokenString.length() - 1)
                .trim();
        if (attrString.startsWith("/")) {
            attrString = attrString.substring(1).trim();
        }

        if (attrString.endsWith("/")) {
            attrString = attrString.substring(0, attrString.length() - 1)
                    .trim();
        }

        attrString = attrString.substring(getName().length()).trim();
        setAttributeString(attrString);

    }

    String getName() {
        return name;
    }

    void removeAttributeString() {
        updateAttributeString(null);
    }

    void updateAttributeString(String attributeString) {
        setAttributeString(attributeString);
        StringBuffer buff = new StringBuffer("<");
        if (!elementComplete() && elementEnd()) {
            buff.append("/");
        }
        buff.append(getName());
        if (attributeString != null && attributeString.trim().length() > 0) {
            buff.append(" ").append(attributeString.trim());
        }
        if (elementComplete()) {
            buff.append("/");
        }
        buff.append(">");

        setTokenString(buff.toString());

    }

    String getAttributeString() {
        return attributeString;
    }

    boolean elementStart() {
        return elementStart;
    }

    boolean elementEnd() {
        return elementEnd;
    }

    boolean elementComplete() {
        return elementStart() && elementEnd();
    }

    protected String getTokenTypeString() {
        return TOKEN_TYPE_BOUNDED + "(" + getName() + ": start="
                + elementStart() + ": end= " + elementEnd();
    }

    private void setName(String name) {
        this.name = name;
    }

    private void setStartMarker() {
        elementStart = true;
    }

    private void setEndMarker() {
        elementEnd = true;
    }

    private void setAttributeString(String attributeString) {
        this.attributeString = attributeString;
    }

    private String name;

    private boolean elementStart = false;

    private boolean elementEnd = false;

    private String attributeString;
}
