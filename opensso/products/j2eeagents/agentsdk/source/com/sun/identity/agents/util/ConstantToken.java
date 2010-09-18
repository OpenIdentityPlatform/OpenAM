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
 * $Id: ConstantToken.java,v 1.2 2008/06/25 05:51:59 qcheng Exp $
 *
 */

package com.sun.identity.agents.util;


public class ConstantToken extends Token {

    public ConstantToken(String tokenString) {
        setTokenString(tokenString);
    }

    public int getTokenType() {
        return TYPE_CONSTANT_TOKEN;
    }

    public int getConsumptionLength() {
        return getTokenString().length();
    }

    public boolean consume(String data) {
        return getTokenString().equals(data);
    }

    public String getTokenString() {
        return _tokenString;
    }

    private void setTokenString(String tokenString) {
        _tokenString = tokenString;
    }

    private String _tokenString;
}
