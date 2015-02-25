/**
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
 * $Id: PrivilegeXMLBuilder.java,v 1.3 2008/10/02 16:31:29 veiming Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.console.property;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.console.base.model.AMAdminUtils;
import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.delegation.DelegationException;
import com.sun.identity.delegation.DelegationManager;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;

public class PrivilegeXMLBuilder
    implements PropertyTemplate
{
    private static PrivilegeXMLBuilder instance = new PrivilegeXMLBuilder();
    private SSOToken adminSSOToken;

    private PrivilegeXMLBuilder() {
        adminSSOToken = AMAdminUtils.getSuperAdminSSOToken();
    }

    public static PrivilegeXMLBuilder getInstance() {
        return instance;
    }

    public Set getAllPrivileges(String realm, AMModel model) {
        Set privileges = null;
        if (realm == null) {
            realm = model.getStartDN();
        }
        try {
            DelegationManager mgr = new DelegationManager(adminSSOToken, realm);
            privileges = mgr.getConfiguredPrivilegeNames();
        } catch (SSOException e) {
            PropertyXMLBuilderBase.debug.error(
                "PrivilegeXMLBuilder.getAllPrivileges", e);
        } catch (DelegationException e) {
            PropertyXMLBuilderBase.debug.error(
                "PrivilegeXMLBuilder.getAllPrivileges", e);
        }

        return (privileges != null) ? privileges : Collections.EMPTY_SET;
    }

    public String getXML(String realm, AMModel model) {
        StringBuilder xml = new StringBuilder(1000);
        if (realm == null) {
            realm = model.getStartDN();
        }

        try {
            DelegationManager mgr = new DelegationManager(adminSSOToken, realm);
            Set privileges = mgr.getConfiguredPrivilegeNames();

            if ((privileges != null) && !privileges.isEmpty()) {
                xml.append(PropertyXMLBuilderBase.getXMLDefinitionHeader())
                   .append(START_TAG)
                   .append(PRIVILEGE_SECTION_TAG);
            
                for (Iterator iter = privileges.iterator(); iter.hasNext(); ) {
                    String name = (String)iter.next();
                    String[] params = {name, name};
                    xml.append(
                        MessageFormat.format(PRIVILEGE_PROPERTY_TAG, (Object[])params));
                }

                xml.append(SECTION_END_TAG)
                   .append(END_TAG);
            }
        } catch (SSOException e) {
            PropertyXMLBuilderBase.debug.error("PrivilegeXMLBuilder.getXML", e);
        } catch (DelegationException e) {
            PropertyXMLBuilderBase.debug.error("PrivilegeXMLBuilder.getXML", e);
        }

        return xml.toString();
    }

    private static final String PRIVILEGE_SECTION_TAG =
        "<section name=\"privileges\" defaultValue=\"delegation.section.privileges\">";
    private static final String PRIVILEGE_PROPERTY_TAG =
        "<property><cc name=\"{0}\" tagclass=\"com.sun.web.ui.taglib.html.CCCheckBoxTag\"><attribute name=\"label\" value=\"delegation.{1}\" /></cc></property>";
}
