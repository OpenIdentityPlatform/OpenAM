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
 * $Id: TabControllerBase.java,v 1.2 2008/06/25 05:42:51 qcheng Exp $
 *
 */

package com.sun.identity.console.controller;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.console.base.model.AMAdminConstants;
import com.sun.identity.console.base.model.AMModelBase;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.sm.AttributeSchema;
import com.sun.identity.sm.SchemaType;
import com.sun.identity.sm.ServiceSchema;
import com.sun.identity.sm.ServiceSchemaManager;
import com.sun.identity.sm.ServiceListener;
import com.sun.identity.sm.SMSException;
import java.security.AccessController;
import java.util.Set;

/**
 * This is the base class of tab controllers. Tab Constroller
 * indicates to Console on whether to show/hide tabs.
 */
public abstract class TabControllerBase
    implements TabController, ServiceListener
{
    protected boolean visible;

    protected void updateStatus() {
	boolean status = false;
        SSOToken adminSSOToken = (SSOToken)AccessController.doPrivileged(
            AdminTokenAction.getInstance());
	try {
	    ServiceSchemaManager mgr = new ServiceSchemaManager(
		AMAdminConstants.ADMIN_CONSOLE_SERVICE, adminSSOToken);
	    ServiceSchema schema = mgr.getSchema(SchemaType.GLOBAL);
	    AttributeSchema as = schema.getAttributeSchema(
		getConfigAttribute());
	    Set defaultValue = as.getDefaultValues();
	    if ((defaultValue != null) && !defaultValue.isEmpty()) {
		String val = (String)defaultValue.iterator().next();
		status = (val != null) && val.equals("true");
	    }
	} catch (SMSException e) {
	    AMModelBase.debug.error("TabControllerBase.updateStatus", e);
	} catch (SSOException e) {
	    AMModelBase.debug.error("TabControllerBase.updateStatus", e);
	}
	visible = status;
    }

    protected void addListener() {
        SSOToken adminSSOToken = (SSOToken)AccessController.doPrivileged(
            AdminTokenAction.getInstance());
        try {
            ServiceSchemaManager mgr = new ServiceSchemaManager(
                AMAdminConstants.ADMIN_CONSOLE_SERVICE, adminSSOToken);
            mgr.addListener(this);
        } catch (SMSException e) {
            AMModelBase.debug.error("TabControllerBase.addListener", e);
        } catch (SSOException e) {
            AMModelBase.debug.error("TabControllerBase.addListener", e);
        }
    }

    /**
     * Update the controller classes if the administration console
     * service attributes is altered.
     */
    public void schemaChanged(String serviceName, String version) {
	updateStatus();
    }

    public void globalConfigChanged(
        String serviceName,
        String version,
        String groupName,
        String serviceComponent,
        int type)
    {
        //NO-OP
    }

    public void organizationConfigChanged(
        String serviceName,
        String version,
        String orgName,
        String groupName,
        String serviceComponent,
        int type)
    {
	//NO-OP
    }

    protected abstract String getConfigAttribute();
}
