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
 * $Id: XACMLOpenSSOPrivilege.java,v 1.1 2009/08/19 05:40:36 veiming Exp $
 */

package com.sun.identity.entitlement.opensso;

import com.sun.identity.entitlement.PrivilegeType;

/**
 *
 * 
 */
public class XACMLOpenSSOPrivilege extends OpenSSOPrivilege {

    public XACMLOpenSSOPrivilege() {
       super();
    }

    @Override
    public PrivilegeType getType() {
        return PrivilegeType.XACML3_OPENSSO;
    }


    /*
     TODO: override to evaluate full blown xacml policy
    @Override
    public List<Entitlement> evaluate(
        Subject subject,
        String resourceName,
        Set<String> actionNames,
        Map<String, Set<String>> environment,
        boolean recursive
    ) throws EntitlementException {
        return null; 
    }
    */

}
