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
 * $Id: Token.java,v 1.2 2008/06/25 05:52:01 qcheng Exp $
 *
 */

package com.sun.identity.agents.util;

/**
 * The representation of a token in pattern matching
 */ 
public abstract class Token {

    public abstract int getTokenType();

    public abstract int getConsumptionLength();

    public abstract boolean consume(String data);

    public abstract String getTokenString();

    public String toString() {

        StringBuffer buff = new StringBuffer();

        switch(getTokenType()) {

        case TYPE_CONSTANT_TOKEN :
            buff.append("ConstantToken{");
            break;

        case TYPE_WILDCARD_TOKEN :
            buff.append("WildCardToken{");
            break;

        default :
            buff.append("Token{");
            break;
        }

        buff.append(getTokenString());
        buff.append("}");

        return buff.toString();
    }

    public static final int TYPE_CONSTANT_TOKEN = 0;
    public static final int TYPE_WILDCARD_TOKEN = 1;
}

