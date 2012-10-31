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
 * $Id: RealmResourceOfferingModelImpl.java,v 1.2 2008/06/25 05:49:43 qcheng Exp $
 *
 */

package com.sun.identity.console.realm.model;

import com.iplanet.sso.SSOException;
import com.sun.identity.console.base.model.AMAdminConstants;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMModelBase;
import com.sun.identity.console.service.model.SMDiscoveryServiceData;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.AMIdentityRepository;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.sm.OrganizationConfigManager;
import com.sun.identity.sm.SMSException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;

/* - LOG COMPLETE - */

public class RealmResourceOfferingModelImpl
    extends AMModelBase
    implements RealmResourceOfferingModel
{
    public RealmResourceOfferingModelImpl(HttpServletRequest req, Map map) {
	super(req, map);
    }

    /**
     * Returns resource offering entry stored for a realm name.
     *
     * @param realm Realm Name.
     * @return resource offering entry stored for a given user.
     * @throws AMConsoleException if entry cannot be determined.
     */
    public SMDiscoveryServiceData getRealmDiscoEntry(String realm)
	throws AMConsoleException {
	SMDiscoveryServiceData resourceOffering = null;
	String[] params = {realm, AMAdminConstants.DISCOVERY_SERVICE,
	    AMAdminConstants.DISCOVERY_SERVICE_NAME_DYNAMIC_DISCO_ENTRIES}; 
	logEvent("ATTEMPT_GET_ATTR_VALUE_OF_SERVICE_UNDER_REALM", params);

	try {
	    AMIdentityRepository repo = new AMIdentityRepository(
		getUserSSOToken(), realm);
	    AMIdentity realmIdentity = repo.getRealmIdentity();
	    Set servicesFromIdRepo = realmIdentity.getAssignedServices();
	    Map map = null;

	    if (servicesFromIdRepo.contains(AMAdminConstants.DISCOVERY_SERVICE)
	    ) {
		map = realmIdentity.getServiceAttributes(
		    AMAdminConstants.DISCOVERY_SERVICE);
	    } else {
		OrganizationConfigManager orgCfgMgr =
		    new OrganizationConfigManager(getUserSSOToken(), realm);
		map = orgCfgMgr.getServiceAttributes(
		    AMAdminConstants.DISCOVERY_SERVICE);
	    }
	    logEvent("SUCCEED_GET_ATTR_VALUE_OF_SERVICE_UNDER_REALM",
		params);
	    resourceOffering = SMDiscoveryServiceData.getEntries(
		(Set)map.get(
		AMAdminConstants.DISCOVERY_SERVICE_NAME_DYNAMIC_DISCO_ENTRIES));
        } catch (SSOException e) {
            String strError = getErrorString(e);
            String[] paramsEx = {realm, AMAdminConstants.DISCOVERY_SERVICE,
		strError};
            logEvent("SSO_EXCEPTION_GET_ATTR_VALUE_OF_SERVICE_UNDER_REALM",
                paramsEx);
            debug.error("RealmResourceOfferingModelImpl.getAttributeValues", e);
        } catch (IdRepoException e) {
            String strError = getErrorString(e);
            String[] paramsEx = {realm, AMAdminConstants.DISCOVERY_SERVICE,
		strError};
            logEvent(
                "IDREPO_EXCEPTION_GET_ATTR_VALUE_OF_SERVICE_UNDER_REALM",
                paramsEx);
            debug.error("RealmResourceOfferingModelImpl.getAttributeValues", e);
        } catch (SMSException e) {
            String strError = getErrorString(e);
            String[] paramsEx = {realm, AMAdminConstants.DISCOVERY_SERVICE,
		strError};
            logEvent("SMS_EXCEPTION_GET_ATTR_VALUE_OF_SERVICE_UNDER_REALM",
                paramsEx);
            debug.error("RealmResourceOfferingModelImpl.getAttributeValues", e);
        }

	return resourceOffering;
    }

    /**
     * Assigns service to a realm.
     *
     * @param realm Realm Name.
     * @throws AMConsoleException if values cannot be set.
     */
    public void assignService(String realm)
	throws AMConsoleException
    {
	String[] params = {realm, AMAdminConstants.DISCOVERY_SERVICE};
	try {
	    AMIdentityRepository repo = new AMIdentityRepository(
		getUserSSOToken(), realm);
	    AMIdentity realmIdentity = repo.getRealmIdentity();
	    Set servicesFromIdRepo = realmIdentity.getAssignableServices();

	    if (servicesFromIdRepo.contains(AMAdminConstants.DISCOVERY_SERVICE)
	    ) {
		realmIdentity.assignService(
		    AMAdminConstants.DISCOVERY_SERVICE, Collections.EMPTY_MAP);
	    } else {
		OrganizationConfigManager orgCfgMgr =
		    new OrganizationConfigManager(getUserSSOToken(), realm);
		orgCfgMgr.assignService(
		    AMAdminConstants.DISCOVERY_SERVICE, Collections.EMPTY_MAP);
	    }
	} catch (SSOException e) {
	    String strError = getErrorString(e);
	    String[] paramsEx = {realm, AMAdminConstants.DISCOVERY_SERVICE,
		strError};
	    logEvent("SSO_EXCEPTION_ASSIGN_SERVICE_TO_REALM", paramsEx);
	    throw new AMConsoleException(strError);
	} catch (IdRepoException e) {
	    String strError = getErrorString(e);
	    String[] paramsEx = {realm, AMAdminConstants.DISCOVERY_SERVICE,
		strError};
	    logEvent("IDREPO_EXCEPTION_ASSIGN_SERVICE_TO_REALM", paramsEx);
	    throw new AMConsoleException(strError);
	} catch (SMSException e) {
	    String strError = getErrorString(e);
	    String[] paramsEx = {realm, AMAdminConstants.DISCOVERY_SERVICE,
		strError};
	    logEvent("SMS_EXCEPTION_ASSIGN_SERVICE_TO_REALM", paramsEx);
	    throw new AMConsoleException(strError);
	}
    }

    /**
     * Set resource offering entry.
     *
     * @param realm Realm Name.
     * @param smData Resource offering entry.
     * @throws AMConsoleException if entry cannot be set.
     */
    public void setRealmDiscoEntry(String realm, SMDiscoveryServiceData smData)
	throws AMConsoleException {
	String[] params = {realm, AMAdminConstants.DISCOVERY_SERVICE};
	logEvent("ATTEMPT_MODIFY_SERVICE_UNDER_REALM", params);
	Map map = new HashMap(2);
	map.put(AMAdminConstants.DISCOVERY_SERVICE_NAME_DYNAMIC_DISCO_ENTRIES, 
	    smData.getDiscoveryEntries());

	try {
	    AMIdentityRepository repo = new AMIdentityRepository(
		getUserSSOToken(), realm);
	    AMIdentity realmIdentity = repo.getRealmIdentity();
	    Set servicesFromIdRepo = realmIdentity.getAssignedServices();

	    if (servicesFromIdRepo.contains(AMAdminConstants.DISCOVERY_SERVICE)
	    ){
		realmIdentity.modifyService(
		    AMAdminConstants.DISCOVERY_SERVICE, map);
	    } else {
		OrganizationConfigManager orgCfgMgr =
		    new OrganizationConfigManager(
			getUserSSOToken(), realm);
		orgCfgMgr.modifyService(
		    AMAdminConstants.DISCOVERY_SERVICE, map);
	    }
	    logEvent("SUCCEED_MODIFY_SERVICE_UNDER_REALM", params);
	} catch (SSOException e) {
	    String strError = getErrorString(e);
	    String[] paramsEx = {realm, AMAdminConstants.DISCOVERY_SERVICE,
		strError};
	    logEvent("SSO_EXCEPTION_MODIFY_SERVICE_UNDER_REALM", paramsEx);
	    throw new AMConsoleException(strError);
	} catch (IdRepoException e) {
	    String strError = getErrorString(e);
	    String[] paramsEx = {realm, AMAdminConstants.DISCOVERY_SERVICE,
		strError};
	    logEvent("IDREPO_EXCEPTION_MODIFY_SERVICE_UNDER_REALM", paramsEx);
	    throw new AMConsoleException(strError);
	} catch (SMSException e) {
	    String strError = getErrorString(e);
	    String[] paramsEx = {realm, AMAdminConstants.DISCOVERY_SERVICE,
		strError};
	    logEvent("SMS_EXCEPTION_MODIFY_SERVICE_UNDER_REALM", paramsEx);
	    throw new AMConsoleException(strError);
	}
    }
}
