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
 * $Id: AD.java,v 1.3 2008/06/25 05:41:55 qcheng Exp $
 *
 */


package com.sun.identity.authentication.modules.ad;

import com.sun.identity.authentication.modules.ldap.LDAP;
import com.sun.identity.authentication.config.AMAuthConfigUtils;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.shared.datastruct.CollectionHelper;
import com.sun.identity.shared.debug.Debug;

/**
 * Auth module for Active Directory
 */
public class AD extends LDAP {
    private ADPrincipal userPrincipal;

    public AD() {
        amAuthLDAP = "amAuthAD";
        debug = Debug.getInstance(amAuthLDAP);
    }

    public java.security.Principal getPrincipal() {
        if (userPrincipal != null) {
            return userPrincipal;
        } else if (validatedUserID != null) {
            userPrincipal = new ADPrincipal(validatedUserID);
            return userPrincipal;
        } else {
            return null;
        }
    }

    // cleanup state fields
    public void destroyModuleState() {
        super.destroyModuleState();
        userPrincipal = null;
    }

    public boolean initializeLDAP() throws AuthLoginException{
        boolean returnValue = super.initializeLDAP();
        String authLevel = CollectionHelper.getMapAttr(currentConfig,
            AMAuthConfigUtils.getAuthLevelAttribute(currentConfig, "AD"));

        if (authLevel != null) {
            try {
                setAuthLevel(Integer.parseInt(authLevel));
            } catch (Exception e) {
                debug.error("Unable to set auth level " + authLevel);
            }
        }
        
        ldapUtil.setAD(true);
        return returnValue;
    }
}
