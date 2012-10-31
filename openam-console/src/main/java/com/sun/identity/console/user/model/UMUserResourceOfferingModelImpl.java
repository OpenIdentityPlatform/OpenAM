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
 * $Id: UMUserResourceOfferingModelImpl.java,v 1.2 2008/06/25 05:49:49 qcheng Exp $
 *
 */

package com.sun.identity.console.user.model;

import com.iplanet.sso.SSOException;
import com.sun.identity.console.base.model.AMAdminConstants;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMModelBase;
import com.sun.identity.console.service.model.SMDiscoveryServiceData;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdUtils;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;

/* - LOG COMPLETE - */

public class UMUserResourceOfferingModelImpl
    extends AMModelBase
    implements UMUserResourceOfferingModel
{
    public UMUserResourceOfferingModelImpl(HttpServletRequest req, Map map) {
	super(req, map);
    }

    /**
     * Returns user name.
     *
     * @param userId Universal ID of user.
     * @return user name.
     */
    public String getUserName(String userId) {
	String userName = "";
	try {	
	    AMIdentity amid = IdUtils.getIdentity(getUserSSOToken(), userId);
	    userName = amid.getName();
	} catch (IdRepoException e) {
	    debug.warning("UMUserResourceOfferingModelImpl.getUserName", e);
	}
	return userName;
    }

    /**
     * Returns resource offering entry stored for a given Universal ID of user.
     *
     * @param userId Universal ID of user.
     * @return resource offering entry stored for a given user.
     * @throws AMConsoleException if entry cannot be determined.
     */
    public SMDiscoveryServiceData getUserDiscoEntry(String userId)
	throws AMConsoleException {
	SMDiscoveryServiceData resourceOffering = null;
	String[] params = {
	    userId, AMAdminConstants.ATTR_USER_RESOURCE_OFFERING};

	try {
	    logEvent("ATTEMPT_READ_IDENTITY_ATTRIBUTE_VALUE", params);
	    AMIdentity amid = IdUtils.getIdentity(getUserSSOToken(), userId);
	    resourceOffering = SMDiscoveryServiceData.getEntries(
		amid.getAttribute(AMAdminConstants.ATTR_USER_RESOURCE_OFFERING),
		SMDiscoveryServiceData.USER_RESOURCE_OFFERING_ENTRY);
	    resourceOffering.setEntryType(
		SMDiscoveryServiceData.USER_RESOURCE_OFFERING_ENTRY);
	    logEvent("SUCCEED_READ_IDENTITY_ATTRIBUTE_VALUE", params);
	} catch (SSOException e) {
	    String strError = getErrorString(e);
	    String[] paramsEx = {userId,
		AMAdminConstants.ATTR_USER_RESOURCE_OFFERING, strError};
	    logEvent("SSO_EXCEPTION_READ_IDENTITY_ATTRIBUTE_VALUE", paramsEx);
	    throw new AMConsoleException(strError);
	} catch (IdRepoException e) {
	    String strError = getErrorString(e);
	    String[] paramsEx = {userId,
		AMAdminConstants.ATTR_USER_RESOURCE_OFFERING, strError};
	    logEvent("IDM_EXCEPTION_READ_IDENTITY_ATTRIBUTE_VALUE", paramsEx);
	    throw new AMConsoleException(strError);
	}
										
	return resourceOffering;
    }

    /**
     * Set resource offering entry.
     *
     * @param userId Universal ID of user.
     * @param smData Resource offering entry.
     * @throws AMConsoleException if entry cannot be set.
     */
    public void setUserDiscoEntry(String userId, SMDiscoveryServiceData smData)
	throws AMConsoleException {
	String[] params = {
	    userId, AMAdminConstants.ATTR_USER_RESOURCE_OFFERING};
	logEvent("ATTEMPT_MODIFY_IDENTITY_ATTRIBUTE_VALUE", params);

	try {
	    AMIdentity amid = IdUtils.getIdentity(getUserSSOToken(), userId);
	    Map map = new HashMap(2);
	    map.put(AMAdminConstants.ATTR_USER_RESOURCE_OFFERING, 
		smData.getDiscoveryEntries());
	    amid.setAttributes(map);
	    amid.store();
	    logEvent("SUCCEED_MODIFY_IDENTITY_ATTRIBUTE_VALUE", params);
	} catch (SSOException e) {
	    String strError = getErrorString(e);
	    String[] paramsEx = {userId, AMAdminConstants.ATTR_USER_PASSWORD,
		strError};
	    logEvent("SSO_EXCEPTION_MODIFY_IDENTITY_ATTRIBUTE_VALUE",
		paramsEx);
	    throw new AMConsoleException(strError);
	} catch (IdRepoException e) {
	    String strError = getErrorString(e);
	    String[] paramsEx = {userId, AMAdminConstants.ATTR_USER_PASSWORD,
		strError};
	    logEvent("IDM_EXCEPTION_MODIFY_IDENTITY_ATTRIBUTE_VALUE",
		paramsEx);
	    throw new AMConsoleException(strError);
	}
    }
}
