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
 * $Id: EntityResourceOfferingModelImpl.java,v 1.2 2008/06/25 05:49:41 qcheng Exp $
 *
 */

package com.sun.identity.console.idm.model;

import com.iplanet.sso.SSOException;
import com.sun.identity.common.CaseInsensitiveHashMap;
import com.sun.identity.console.base.model.AMAdminConstants;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMModelBase;
import com.sun.identity.console.service.model.SMDiscoveryServiceData;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdUtils;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;

/* - LOG COMPLETE - */

public class EntityResourceOfferingModelImpl
    extends AMModelBase
    implements EntityResourceOfferingModel
{
    public EntityResourceOfferingModelImpl(HttpServletRequest req, Map map) {
	super(req, map);
    }

    /**
     * Returns resource offering entry stored for an entity.
     *
     * @param universalId Universal ID of the entity.
     * @return resource offering entry stored.
     * @throws AMConsoleException if entry cannot be determined.
     */
    public SMDiscoveryServiceData getEntityDiscoEntry(String universalId)
	throws AMConsoleException {
	SMDiscoveryServiceData resourceOffering = null;
	String[] params = {universalId,
	    AMAdminConstants.DISCOVERY_SERVICE};
	logEvent("ATTEMPT_IDENTITY_READ_SERVICE_ATTRIBUTE_VALUES", params);

	try {
	    AMIdentity amid = IdUtils.getIdentity(
		getUserSSOToken(), universalId);
	    Map map = new CaseInsensitiveHashMap();
	    map.putAll(amid.getServiceAttributes(
		AMAdminConstants.DISCOVERY_SERVICE));
	    logEvent("SUCCEED_IDENTITY_READ_SERVICE_ATTRIBUTE_VALUES",
		params);
	    resourceOffering = SMDiscoveryServiceData.getEntries(
		(Set)map.get(
		AMAdminConstants.DISCOVERY_SERVICE_NAME_DYNAMIC_DISCO_ENTRIES));
        } catch (SSOException e) {
            String strError = getErrorString(e);
            String[] paramsEx = {universalId,
		AMAdminConstants.DISCOVERY_SERVICE, strError};
            logEvent("SSO_EXCEPTION_IDENTITY_READ_SERVICE_ATTRIBUTE_VALUES",
                paramsEx);
            debug.error(
		"EntityResourceOfferingModelImpl.getAttributeValues", e);
        } catch (IdRepoException e) {
            String strError = getErrorString(e);
            String[] paramsEx = {universalId,
		AMAdminConstants.DISCOVERY_SERVICE, strError};
            logEvent(
                "IDM_EXCEPTION_IDENTITY_READ_SERVICE_ATTRIBUTE_VALUES",
                paramsEx);
            debug.error(
		"EntityResourceOfferingModelImpl.getAttributeValues", e);
        }

	return resourceOffering;
    }

    /**
     * Assigns service to an entity.
     *
     * @param universalId Universal ID of the entity.
     * @throws AMConsoleException if service cannot be assigned.
     */
    public void assignService(String universalId)
	throws AMConsoleException
    {
	String[] params = {universalId, AMAdminConstants.DISCOVERY_SERVICE};
	logEvent("ATTEMPT_IDENTITY_ASSIGN_SERVICE", params);
	try {
	    AMIdentity amid = IdUtils.getIdentity(
		getUserSSOToken(), universalId);
	    amid.assignService(
		AMAdminConstants.DISCOVERY_SERVICE, Collections.EMPTY_MAP);
	} catch (SSOException e) {
	    String strError = getErrorString(e);
	    String[] paramsEx = {universalId,
		AMAdminConstants.DISCOVERY_SERVICE, strError};
	    logEvent("SSO_EXCEPTION_IDENTITY_ASSIGN_SERVICE", paramsEx);
	    throw new AMConsoleException(strError);
	} catch (IdRepoException e) {
	    String strError = getErrorString(e);
	    String[] paramsEx = {universalId,
		AMAdminConstants.DISCOVERY_SERVICE, strError};
	    logEvent("IDM_EXCEPTION_IDENTITY_ASSIGN_SERVICE", paramsEx);
	    throw new AMConsoleException(strError);
	}
    }

    /**
     * Set resource offering entry.
     *
     * @param universalId Universal ID of the entity.
     * @param smData Resource offering entry.
     * @throws AMConsoleException if entry cannot be set.
     */
    public void setEntityDiscoEntry(
	String universalId,
	SMDiscoveryServiceData smData
    ) throws AMConsoleException {
	String[] params = {universalId, AMAdminConstants.DISCOVERY_SERVICE};
	logEvent("ATTEMPT_IDENTITY_WRITE_SERVICE_ATTRIBUTE_VALUES", params);
	Map map = new HashMap(2);
	map.put(AMAdminConstants.DISCOVERY_SERVICE_NAME_DYNAMIC_DISCO_ENTRIES, 
	    smData.getDiscoveryEntries());

	try {
	    AMIdentity amid = IdUtils.getIdentity(
		getUserSSOToken(), universalId);
	    amid.modifyService(AMAdminConstants.DISCOVERY_SERVICE, map);
	    logEvent("SUCCEED_IDENTITY_WRITE_SERVICE_ATTRIBUTE_VALUES", params);
	} catch (SSOException e) {
	    String strError = getErrorString(e);
	    String[] paramsEx = {universalId,
		AMAdminConstants.DISCOVERY_SERVICE, strError};
	    logEvent("SSO_EXCEPTION_IDENTITY_WRITE_SERVICE_ATTRIBUTE_VALUES",
		paramsEx);
	    throw new AMConsoleException(strError);
	} catch (IdRepoException e) {
	    String strError = getErrorString(e);
	    String[] paramsEx = {universalId,
		AMAdminConstants.DISCOVERY_SERVICE, strError};
	    logEvent("IDM_EXCEPTION_IDENTITY_WRITE_SERVICE_ATTRIBUTE_VALUES",
		paramsEx);
	    throw new AMConsoleException(strError);
	}
    }
}
