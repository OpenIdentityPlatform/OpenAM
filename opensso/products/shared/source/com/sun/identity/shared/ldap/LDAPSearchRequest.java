/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: LDAPSearchRequest.java,v 1.1 2009/11/20 23:52:59 ww203982 Exp $
 */
package com.sun.identity.shared.ldap;

import com.sun.identity.shared.ldap.client.opers.JDAPProtocolOp;
import java.util.LinkedList;

/**
 * @deprecated As of ForgeRock OpenAM 10.
 */
public class LDAPSearchRequest extends LDAPRequest {

    String baseDN;
    int scope;
    int deref;
    int sizeLimit;
    int timeLimit;
    boolean attrsOnly;
    String filter;
    String[] attrs;

    protected LDAPSearchRequest(String baseDN, int scope, String filter, 
        String[] attrs, boolean attrsOnly, int timeLimit, int deref,
        int sizeLimit, LinkedList bytesList, int length) {
        super(bytesList, length);
        this.baseDN= baseDN;
        this.scope = scope;
        this.filter = filter;
        this.attrs = attrs;
        this.attrsOnly = attrsOnly;
        this.deref = deref;
        this.sizeLimit = sizeLimit;
        this.timeLimit = timeLimit;
    }

    public String getBaseDN() {
        return baseDN;
    }

    public int getScope() {
        return scope;
    }

    public int getDereference() {
        return deref;
    }

    public String getFilter() {
        return filter;
    }

    public int getType() {
        return JDAPProtocolOp.SEARCH_REQUEST;
    }

    public String[] getAttributes() {
        return attrs;
    }

    public boolean getAttributesOnly() {
        return attrsOnly;
    }
}
