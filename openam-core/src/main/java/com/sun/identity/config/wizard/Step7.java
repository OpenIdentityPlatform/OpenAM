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
 * $Id: Step7.java,v 1.15 2009/10/27 05:31:45 hengming Exp $
 *
 */

/*
 * Portions Copyrighted 2010-2011 ForgeRock AS
 */
package com.sun.identity.config.wizard;

import com.sun.identity.config.SessionAttributeNames;
import com.sun.identity.config.util.AjaxPage;
import com.sun.identity.setup.SetupConstants;
import org.apache.click.Context;

/**
 * This is the summary page for the values entered during the configuration
 * process. No actual work is done here except setting the page elements.
 */
public class Step7 extends AjaxPage {

    public void onInit() {
        Context ctx = getContext();
        String tmp = getAttribute(
            SetupConstants.CONFIG_VAR_DATA_STORE, 
            SetupConstants.SMS_EMBED_DATASTORE);
        boolean isEmbedded = tmp.equals(SetupConstants.SMS_EMBED_DATASTORE);
            
        // Config Store Properties
        tmp =(String)ctx.getSessionAttribute(SessionAttributeNames.CONFIG_DIR);
        add("configDirectory", tmp);
        
        if (isEmbedded) {
            add("isEmbedded", "1");
            add("configStoreHost", "localhost");
        } else {
            tmp = getAttribute("configStoreHost", getHostName());
            add("configStoreHost", tmp);
        }
        tmp = getAttribute("configStoreSSL", "");
        add("displayConfigStoreSSL", tmp.equals("SSL") ? 
            getLocalizedString("yes.label") : 
            getLocalizedString("no.label"));
        tmp = getAttribute("rootSuffix", Wizard.defaultRootSuffix);
        add("rootSuffix", tmp);

        // Provide Port Settings
        tmp = getAttribute("configStorePort", getAvailablePort(50389));
        add("configStorePort", tmp);
        tmp = getAttribute("configStoreAdminPort", getAvailablePort(4444));
        add("configStoreAdminPort", tmp);
        tmp = getAttribute("configStoreJmxPort", getAvailablePort(1689));
        add("configStoreJmxPort", tmp);
        tmp = getAttribute("configStoreLoginId", Wizard.defaultUserName);
        add("configStoreLoginId", tmp);

        // Provide User Store Properties.
        tmp = getAttribute(SetupConstants.DS_EMB_REPL_FLAG, "");
        if (!tmp.equals(SetupConstants.DS_EMP_REPL_FLAG_VAL)) {
            // User Config Store Properties
            tmp = (String) ctx.getSessionAttribute(
                SessionAttributeNames.EXT_DATA_STORE);
            if (tmp.equals("true")) {
                tmp = (String) ctx.getSessionAttribute(
                    SessionAttributeNames.USER_STORE_HOST);
                add("displayUserHostName", tmp);

                tmp = (String) ctx.getSessionAttribute(
                    SessionAttributeNames.USER_STORE_SSL);
                add("xuserHostSSL", tmp.equals("SSL") ? 
                    getLocalizedString("yes.label") : 
                    getLocalizedString("no.label"));

                tmp = (String) ctx.getSessionAttribute(
                    SessionAttributeNames.USER_STORE_PORT);
                add("userHostPort", tmp);

                tmp = (String) ctx.getSessionAttribute(
                    SessionAttributeNames.USER_STORE_ROOT_SUFFIX);
                add("userRootSuffix", tmp);

                tmp = (String) ctx.getSessionAttribute(
                    SessionAttributeNames.USER_STORE_LOGIN_ID);
                add("userLoginID", tmp);

                tmp = (String) ctx.getSessionAttribute(
                    SessionAttributeNames.USER_STORE_TYPE);
                if (tmp.equals("LDAPv3ForODSEE")) {
                    add("userStoreType", 
                        getLocalizedString("odsee.ldap.schema"));
                } else if (tmp.equals("LDAPv3ForAD")) {
                    add("userStoreType", getLocalizedString(
                        "activedirectory.ldap.schema"));
                } else if (tmp.equals("LDAPv3ForADDC")) {
                    add("userStoreType", getLocalizedString(
                        "activedirectoryfordomainname.ldap.schema"));
                } else if (tmp.equals("LDAPv3ForADAM")) {
                    add("userStoreType", getLocalizedString(
                        "adam.ldap.schema"));
                } else if (tmp.equals("LDAPv3ForOpenDS")) {
                    add("userStoreType", 
                        getLocalizedString("opends.ldap.schema"));
                } else {
                    add("userStoreType", 
                        getLocalizedString("tivoli.ldap.schema"));
                }
            }
            add("firstInstance", "1");
        }
        
        // Load Balancer Properties
        add("loadBalancerHost", 
            (String)ctx.getSessionAttribute(
                SessionAttributeNames.LB_SITE_NAME));
        add("loadBalancerPort", 
            (String)ctx.getSessionAttribute(
                SessionAttributeNames.LB_PRIMARY_URL));

        super.onInit();
    }

    protected void add(String key, Object value) {
        if (value != null) {
            addModel(key, value);
        }
    }
}
