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
 * $Id: ContainerModelImpl.java,v 1.4 2008/10/02 16:31:27 veiming Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.console.dm.model;

import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.property.PropertyTemplate;
import com.sun.identity.console.property.PropertyXMLBuilderBase;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.AttributeSchema;
import com.sun.identity.sm.SchemaType;
import com.sun.identity.sm.ServiceSchema;
import com.iplanet.am.sdk.AMConstants;
import com.iplanet.am.sdk.AMException;
import com.iplanet.am.sdk.AMObject;
import com.iplanet.am.sdk.AMOrganization;
import com.iplanet.am.sdk.AMOrganizationalUnit;
import com.iplanet.am.sdk.AMSearchControl;
import com.iplanet.am.sdk.AMSearchResults;
import com.iplanet.am.sdk.AMStoreConnection;
import com.iplanet.sso.SSOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;

/* - LOG COMPLETE - */

/**
 * This model is used by <code>ContainerViewBean.java</code>
 */
public class ContainerModelImpl extends DMModelBase
    implements ContainerModel {

    /**
     * Creates a role navigation model implementation object
     *
     * @param req  HTTP Servlet Request
     * @param map of user information
     */
    public ContainerModelImpl(HttpServletRequest req, Map map) {
	super(req, map);
    }    
        
    /**
     * Gets a set of Containers.
     *
     * @param location where to start the search. 
     * @param filter to use in the search.
     * @return a set of Containers
     */
    public Set getContainers(String location, String filter) {
        AMStoreConnection sc = getAMStoreConnection();
	AMSearchResults results = null;
	AMSearchControl searchControl = new AMSearchControl();
	searchControl.setSearchScope(AMConstants.SCOPE_SUB);
        setSearchControlLimits(searchControl);
        setSearchControlAttributes(
            location, 
            DMConstants.SUB_SCHEMA_GROUP_CONTAINER,
            AMObject.GROUP_CONTAINER, 
            searchControl, 
            DMConstants.GROUP_CONTAINERS);

	boolean bOrganization = false;
	try {
            String[] params = {location, filter};
	    switch (getObjectType(location)) {
	    case AMObject.ORGANIZATION:
		bOrganization = true;
		logEvent(
		    "ATTEMPT_DIR_MGR_GET_CONTAINERS_FROM_ORGANIZATION",
		    params);
		AMOrganization org = sc.getOrganization(location);
		results = org.searchOrganizationalUnits(filter, null,
		    searchControl);
		logEvent(
		    "SUCCEED_DIR_MGR_GET_CONTAINERS_FROM_ORGANIZATION",
		    params);
		break;
	    case AMObject.ORGANIZATIONAL_UNIT:
		logEvent(
		    "ATTEMPT_DIR_MGR_GET_CONTAINERS_FROM_CONTAINER",
		    params);
		AMOrganizationalUnit orgUnit =
		    sc.getOrganizationalUnit(location);
		results = orgUnit.searchSubOrganizationalUnits(
		    filter, null, searchControl);
		logEvent(
		    "SUCCEED_DIR_MGR_GET_CONTAINERS_FROM_CONTAINER",
		    params);
		break;
	    default:
		if (debug.warningEnabled()) {
		    debug.warning("ContainerModel.getContainers"
			+ "invalid location " + locationType);
		}
            }
        } catch (SSOException e) {
	       searchErrorMsg = getErrorString(e);
		String msgId = (bOrganization) ?
		    "SSO_EXCEPTION_DIR_MGR_GET_CONTAINERS_FROM_ORGANIZATION" :
		    "SSO_EXCEPTION_DIR_MGR_GET_CONTAINERS_FROM_CONTAINER";
		String[] paramsEx = {location, filter, searchErrorMsg};
		logEvent(msgId, paramsEx);
		debug.warning("ContainerModel.getContainers", e);
	} catch (AMException e) {
	    searchErrorMsg = getErrorString(e);
	    String msgId = (bOrganization) ?
	        "AM_EXCEPTION_DIR_MGR_GET_CONTAINERS_FROM_ORGANIZATION" :
	        "AM_EXCEPTION_DIR_MGR_GET_CONTAINERS_FROM_CONTAINER";
	    String[] paramsEx = {location, filter, searchErrorMsg};
	    logEvent(msgId, paramsEx);
	    debug.warning("ContainerModel.getContainers", e);
	}
	    
        return setSearchResults(results);
    }

    public void createContainer(String location, Map data) 
        throws AMConsoleException
    {
        if (data == null || data.isEmpty()) {
            throw new AMConsoleException(
                getLocalizedString("createFailure.message"));
        }

        Set tmp = (Set)data.remove(NAME);
        if (tmp == null || tmp.isEmpty()) { 
            throw new AMConsoleException(
                getLocalizedString("message.missing.name"));
        } 

        String name = (String)tmp.iterator().next();
        if (name == null || name.length() == 0) { 
            throw new AMConsoleException(
                getLocalizedString("message.missing.name"));
        } 
	Map input = new HashMap(2);
	input.put(name, data);
	boolean bOrganization = false;

        try {
	    String[] params = {location, name};
            int locType = getObjectType(location);

            if (locType == AMObject.ORGANIZATION) {
		bOrganization = true;
		logEvent("ATTEMPT_DIR_MGR_CREATE_CONTAINER_UNDER_ORGANIZATION",
		    params);
                AMOrganization parent =
                    getAMStoreConnection().getOrganization(location);
		parent.createOrganizationalUnits(input);
		logEvent("SUCCEED_DIR_MGR_CREATE_CONTAINER_UNDER_ORGANIZATION",
		    params);
            } else if (locType == AMObject.ORGANIZATIONAL_UNIT) {
		logEvent("ATTEMPT_DIR_MGR_CREATE_CONTAINER_UNDER_CONTAINER",
		    params);
                AMOrganizationalUnit parent =
                    getAMStoreConnection().getOrganizationalUnit(location);
                parent.createSubOrganizationalUnits(input);
		logEvent("SUCCEED_DIR_MGR_CREATE_CONTAINER_UNDER_CONTAINER",
		    params);
            }
        } catch (AMException e) {
	    String msgId = (bOrganization) ?
		"AM_EXCEPTION_DIR_MGR_CREATE_CONTAINER_UNDER_ORGANIZATION":
		"AM_EXCEPTION_DIR_MGR_CREATE_CONTAINER_UNDER_CONTAINER";
	    String strError = getErrorString(e);
	    String[] paramsEx = {location, name, strError};
	    logEvent(msgId, paramsEx);
            throw new AMConsoleException(strError);
        } catch (SSOException e) {
	    String msgId = (bOrganization) ?
		"SSO_EXCEPTION_DIR_MGR_CREATE_CONTAINER_UNDER_ORGANIZATION":
		"SSO_EXCEPTION_DIR_MGR_CREATE_CONTAINER_UNDER_CONTAINER";
	    String strError = getErrorString(e);
	    String[] paramsEx = {location, name, strError};
	    logEvent(msgId, paramsEx);
            debug.error("error in sso", e);
        }
    }    
    
    /**
     * Returns people container creation property XML.
     *
     * @return people container creation property XML.
     */
    public String getCreateContainerXML() {
        StringBuffer buff = new StringBuffer(2000);
        buff.append(PropertyXMLBuilderBase.getXMLDefinitionHeader())
            .append(PropertyTemplate.START_TAG)
            .append(CREATE_PROPERTIES);    
        
        getPropertyXML(DMConstants.ENTRY_SPECIFIC_SERVICE, 
            DMConstants.SUB_SCHEMA_GROUP_CONTAINER, SchemaType.GLOBAL, buff);
        
        buff.append(PropertyTemplate.END_TAG);
        return buff.toString();
    }

    public Map getDataMap() {
        Map map = new HashMap();

        try {
            ServiceSchema sub = getSubSchema(
                DMConstants.ENTRY_SPECIFIC_SERVICE, SchemaType.GLOBAL, 
                DMConstants.SUB_SCHEMA_GROUP_CONTAINER);
            if (sub != null) {
                Set attrSchemas = sub.getAttributeSchemas();
                if (attrSchemas != null) {
                    for (Iterator i = attrSchemas.iterator(); i.hasNext(); ) {
                        AttributeSchema as = (AttributeSchema)i.next();
                        map.put(as.getName().toLowerCase(),
			    Collections.EMPTY_SET);
                    }
                }
            }

            // add the name field to the set of data returned.
            map.put(NAME, Collections.EMPTY_SET);
        } catch (SMSException e) {
            debug.error("ContainerModel.getDataMap", e);
        } catch (SSOException e) {
	    debug.error("ContainerModel.getDataMap error in sso", e);
	}

        return map;
    }

    public boolean hasDisplayProperties() {
	Map tmp = getDataMap();
	tmp.remove(NAME);
	return !tmp.isEmpty();
    }

    /**
     * Gets the services which are assigned to this organization.
     *
     * @param location name or current organization location
     * @return a set of organizations
     */
    public Map getAssignedServices(String location) {
        Map  names = null;
        try {
	    String[] param = {location};
	    logEvent("ATTEMPT_DIR_MGR_GET_ASSIGNED_SERVICE_TO_CONTAINER",
		param);
            AMOrganizationalUnit ou = 
                getAMStoreConnection().getOrganizationalUnit(location);
            Set tmp = ou.getRegisteredServiceNames();
            if (tmp != null &&  !tmp.isEmpty()) {
                names = new HashMap(tmp.size() * 2);
                for (Iterator iter = tmp.iterator(); iter.hasNext(); ) {
                    String name = (String)iter.next();
                    String displayName = getLocalizedServiceName(name);
                    if (!name.equals(displayName)) {
                        names.put(name, displayName);
                    }
                }
            }
	    logEvent("SUCCEED_DIR_MGR_GET_ASSIGNED_SERVICE_TO_CONTAINER",
		param);
        } catch (SSOException e) {
	    String[] paramsEx = {location, getErrorString(e)};
	    logEvent("SSO_EXCEPTION_DIR_MGR_GET_ASSIGNED_SERVICE_TO_CONTAINER",
		paramsEx);
            debug.warning("ContainerModelImpl.getOrganizations", e);
        } catch (AMException e) {
	    String[] paramsEx = {location, getErrorString(e)};
	    logEvent("AM_EXCEPTION_DIR_MGR_GET_ASSIGNED_SERVICE_TO_CONTAINER",
		paramsEx);
            debug.warning("ContainerModelImpl.getOrganizations", e);
        }
        return (names == null) ? Collections.EMPTY_MAP : names;
    }

    private static final String CREATE_PROPERTIES = "<section name=\"attributes\" defaultValue=\"\" ><property><label name=\"lblPcName\" defaultValue=\"label.name\" labelFor=\""+NAME+"\" /><cc name=\""+NAME+"\" tagclass=\"com.sun.web.ui.taglib.html.CCTextFieldTag\" /></property></section>";
}
