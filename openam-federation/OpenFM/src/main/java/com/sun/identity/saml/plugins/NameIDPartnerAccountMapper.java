/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: NameIDPartnerAccountMapper.java,v 1.5 2010/01/09 19:41:52 qcheng Exp $
 *
 * Portions Copyright 2015 ForgeRock AS.
 */

package com.sun.identity.saml.plugins;

import com.sun.identity.saml.assertion.NameIdentifier;
import com.sun.identity.saml.assertion.Subject;
import com.sun.identity.saml.common.SAMLUtils;
import com.sun.identity.sm.SMSEntry;
import org.forgerock.openam.ldap.LDAPUtils;

import java.util.Map;

/**
 * The class <code>NameIDPartnerAccountMapper</code> provide an 
 * implementation of the <code>PartnerAccountMapper</code> interface,
 * the class maps user bases on the value of NameIdentifer only. If
 * the value is DN, the RND value will be returned. If value is email
 * address, the email id (without @domain) will be returned. Otherwise,
 * whole Name ID value will be returned.
 * <p>
 */
public class NameIDPartnerAccountMapper extends DefaultPartnerAccountMapper {

    protected void getUser(Subject subject, String sourceID, Map map) {
        // Get name id 
        NameIdentifier nameIdentifier = subject.getNameIdentifier();
        if (nameIdentifier != null) {
            String name = nameIdentifier.getName();
            if (name != null && (!name.equals(""))) {
                if (SAMLUtils.debug.messageEnabled()) {
                    SAMLUtils.debug.message("NameIDPartnerAccountMapper: name="
                         + name);
                }
                map.put(NAME, getUserName(name));
            } else {
                SAMLUtils.debug.warning("NameIDPAccountMapper: Name is null");
                map.put(NAME, ANONYMOUS_USER);
            }
            String rootSuffix = SMSEntry.getRootSuffix(); 
            map.put(ORG, "/");
        }
    }

    private String getUserName(String name) {
        if (LDAPUtils.isDN(name)) {
            return removeAt(LDAPUtils.rdnValueFromDn(name));
        } else {
            return removeAt(name);
        }
    }

    private String removeAt(String name) {
        int loc = name.indexOf("@");
        if (loc != -1) {
            return name.substring(0, loc);
        } else {
            return name;
        }
    }
}
