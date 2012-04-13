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
 * $Id: PropertiesFinder.java,v 1.3 2008/06/25 05:42:26 qcheng Exp $
 *
 */

package com.sun.identity.common;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.sm.AttributeSchema;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceSchema;
import com.sun.identity.sm.ServiceSchemaManager;
import java.security.AccessController;
import java.util.Set;

/**
 * This class tries to locate an attribute value in service.
 */
public class PropertiesFinder {
    private PropertiesFinder() {
    }

    public static String getProperty(
        String propertyName,
        AttributeStruct ast
    ) {
        String value = null;
        SSOToken adminToken = (SSOToken)AccessController.doPrivileged(
            AdminTokenAction.getInstance());
        try {
            ServiceSchemaManager ssm = new ServiceSchemaManager(ast.serviceName,
                adminToken);
            
            if (ssm.getRevisionNumber() >= ast.revisionNumber) {
                ServiceSchema ss = ssm.getGlobalSchema();
                if (ss != null) {
                    AttributeSchema as =
                        ss.getAttributeSchema(ast.attributeName);
                    if (as != null) {
                        Set values = as.getDefaultValues();
                        if ((values != null) && !values.isEmpty()) {
                            value = (String)values.iterator().next();
                        }
                    }
                }
            }
        } catch (SSOException ex) {
            // ignore: Service may not be present.
        } catch (SMSException ex) {
            // ignore: Service may not be present.
        }
        return value;
    }

}
