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
 * $Id: SCUtils.java,v 1.3 2008/07/10 23:27:24 veiming Exp $
 *
 */

package com.sun.identity.console.service.model;

import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.console.base.model.AMModelBase;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceSchemaManager;
import com.sun.identity.sm.ServiceSchema;
import com.sun.identity.sm.SchemaType;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import java.security.AccessController;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;
import java.util.MissingResourceException;
import java.util.ResourceBundle;


/* - NEED NOT LOG - */

public class SCUtils {

    private static SSOToken adminSSOToken =
        (SSOToken)AccessController.doPrivileged(AdminTokenAction.getInstance());

    private ServiceSchemaManager manager = null;
    private String serviceName = null;
    private SSOToken ssoToken = null;

    public SCUtils(String name, AMModel model) {
        initialize(name, model.getUserSSOToken());
    }

    /*
    * Establishes the ServiceSchemaManager connection for future operations.
    */
    private void initialize(String service, SSOToken token) {
        serviceName = service;
        ssoToken = token;
        Exception exception = null;

        try {
            manager =  new ServiceSchemaManager(serviceName, ssoToken);
        } catch (SSOException e) {
            exception = e;
        } catch (SMSException e) {
            exception = e;
        }

        if (exception != null && AMModelBase.debug.warningEnabled()) {
            AMModelBase.debug.warning("SCUtils.initialize: " + serviceName, exception);
        }
    }

    /**
     * The display URL for a service is the class  that controls the 
     * presentation of the service information.  To get the URL 
     * requires a <code>ServiceSchema</code> object. It can be
     * any of the <code>ServiceSchema</code> defined for the 
     * service. 
     */
    public String getServiceDisplayURL() {
        String displayURL = null;

        try {
            Set schemaTypes = manager.getSchemaTypes();

            //we need to use any schema, so get the first one
            if (schemaTypes != null && !schemaTypes.isEmpty()) {
                Iterator iter = schemaTypes.iterator();
                SchemaType type = (SchemaType) iter.next();
                ServiceSchema schema = manager.getSchema(type);
                if (schema != null) {
                    displayURL = schema.getPropertiesViewBeanURL();
                }
            }
        } catch (SMSException smse) {
            AMModelBase.debug.error("SCUtils.getServiceDisplayURL", smse);
        }

        return displayURL;
    }

    public static String getLocalizedServiceName(
        String serviceName, Locale locale)
        throws SMSException, SSOException, MissingResourceException
    {
        String localizedName = null;
        ServiceSchemaManager mgr = new ServiceSchemaManager(
            serviceName, adminSSOToken);
        ResourceBundle rb = ResourceBundle.getBundle(
            mgr.getI18NFileName(), locale);
        Set types = mgr.getSchemaTypes();

        if (!types.isEmpty()) {
            SchemaType type = (SchemaType)types.iterator().next();
            ServiceSchema schema = mgr.getSchema(type);

            if (schema != null) {
                String i18nKey = schema.getI18NKey();

                if ((i18nKey != null) && (i18nKey.trim().length() > 0)) {
                    localizedName = rb.getString(i18nKey);
                }
            }
        }

        return localizedName;
    }

}

