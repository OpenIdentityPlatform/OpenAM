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
 * $Id: WildCardToken.java,v 1.2 2008/06/25 05:52:01 qcheng Exp $
 *
 */

package com.sun.identity.agents.util;


/**
 * A token contains wildcard
 */
public class WildCardToken extends Token {

    public int getTokenType() {
        return TYPE_WILDCARD_TOKEN;
    }

    public int getConsumptionLength() {
        return -1;
    }

    public boolean consume(String data) {
        return true;
    }

    public String getTokenString() {
        return "*";
    }
}


/*--- Formatted in Sun ONE Identity Server Coding Convention Style on Mon, Jun 16, '03 ---*/
