/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2005 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: Token.java,v 1.2 2008/06/25 05:41:28 qcheng Exp $
 *
 */

package com.iplanet.am.util;

/**
 * 
 * The Token class is used by RequestToken class to define valid tokens. It is
 * just a holder of name value pairs
 * 
 */

public class Token {

    /**
     * Value of token - An int
     */
    public int value;

    /**
     * Name of Token - A String
     */
    public String name;

    public Token() {
    }

    /**
     * Constructor
     * 
     * @param name
     *            A String representing the name of the token
     * @param value
     *            An int representing value of the token
     */
    public Token(String name, int value) {
        this.name = name;
        this.value = value;
    }
}
