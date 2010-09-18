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
 * $Id: GroupContainerModelImpl.java,v 1.4 2008/10/02 16:31:27 veiming Exp $
 *
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
import com.iplanet.am.sdk.AMGroupContainer;
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
 * This model is used by <code>RoleViewBean.java</code>
 */
public class GroupContainerModelImpl extends DMModelBase
    implements GroupContainerModel {
    
    private Set containers = null;
    
    /**
     * Creates a role navigation model implementation object
     *
     * @param req  HTTP Servlet Request
     * @param map of user information
     */
    public GroupContainerModelImpl(HttpServletRequest req, Map map) {
	super(req, map);
    }    
        
    /**
     * Gets a set of Group Containers
     *
     * @param filter wildcards
     * @return a set of Group Containers
     */
    public Set getGroupContainers(String location, String filter) {
        if (containers == null) {
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
	    boolean bContainer = false;
            AMStoreConnection sc = getAMStoreConnection();

	    try {
		String[] params = {location, filter};
		switch (getObjectType(location)) {
		    case AMObject.ORGANIZATION:
			bOrganization = true;
			logEvent(
			    "ATTEMPT_DIR_MGR_SEARCH_GROUP_CONTAINERS_UNDER_ORG",
			    params);
			AMOrganization org = sc.getOrganization(location);
			results = org.searchGroupContainers(filter, null,
			    searchControl);
			logEvent(
			    "SUCCEED_DIR_MGR_SEARCH_GROUP_CONTAINERS_UNDER_ORG",
			    params);
			break;
		    case AMObject.ORGANIZATIONAL_UNIT:
			bContainer = true;
			logEvent(
		"ATTEMPT_DIR_MGR_SEARCH_GROUP_CONTAINERS_UNDER_CONTAINER",
			    params);
			AMOrganizationalUnit orgUnit =
			    sc.getOrganizationalUnit(location);
			results = orgUnit.searchGroupContainers(filter, null,
			    searchControl);
			logEvent(
		"SUCCEED_DIR_MGR_SEARCH_GROUP_CONTAINERS_UNDER_CONTAINER",
			    params);
			break;
		    case AMObject.GROUP_CONTAINER:
			logEvent(
	         "ATTEMPT_DIR_MGR_SEARCH_GROUP_CONTAINERS_UNDER_GRP_CONTAINER",
			    params);
			AMGroupContainer groupContainer =
			    sc.getGroupContainer(location);
			results =  groupContainer.searchSubGroupContainers(
			    filter, null, searchControl);
			logEvent(
		  "SUCCEED_DIR_MGR_SEARCH_GROUP_CONTAINERS_UNDER_GRP_CONTAINER",
			    params);
			break;			
		    default:
			if (debug.warningEnabled()) {
			    debug.warning(
				"GroupContainerModelImpl.getGroupContainers"
				+ "invalid location " + locationType);
			}
		}
	    } catch (SSOException ssoe) {
		String[] paramsEx = {location, filter, getErrorString(ssoe)};
		String msgId = null;
		if (bOrganization) {
		    msgId =
		    "SSO_EXCEPTION_DIR_MGR_SEARCH_GROUP_CONTAINERS_UNDER_ORG";
		} else {
		    msgId = (bContainer) ?
		"SSO_EXCEPTION_DIR_MGR_SEARCH_GROUP_CONTAINERS_UNDER_CONTAINER":
	    "SSO_EXCEPTION_DIR_MGR_SEARCH_GROUP_CONTAINERS_UNDER_GRP_CONTAINER";
		}
		logEvent(msgId, paramsEx);
		debug.warning("GroupContainerModelImpl.getGroupContainers",
		    ssoe);
	    } catch (AMException ame) {
		searchErrorMsg = getErrorString(ame);
		String[] paramsEx = {location, filter, searchErrorMsg};
		String msgId = null;
		if (bOrganization) {
		    msgId =
		    "AM_EXCEPTION_DIR_MGR_SEARCH_GROUP_CONTAINERS_UNDER_ORG";
		} else {
		    msgId = (bContainer) ?
		"AM_EXCEPTION_DIR_MGR_SEARCH_GROUP_CONTAINERS_UNDER_CONTAINER":
	     "AM_EXCEPTION_DIR_MGR_SEARCH_GROUP_CONTAINERS_UNDER_GRP_CONTAINER";
		}
		logEvent(msgId, paramsEx);
		debug.warning("UMGCNavModelImpl.GroupContainerModelImpl",
		    ame);
	    }
	    
	    containers = setSearchResults(results);
	}
	return containers;
    }

    public void createGroupContainer(String location, Map data) 
        throws AMConsoleException {
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

	validateRequiredAttributes(data);

	Map input = new HashMap(2);
	input.put(name, data);
	String[] params = {location, name};
	boolean bOrganization = false;
	boolean bContainer = false;
        AMStoreConnection sc = getAMStoreConnection();

        try {
            int locType = getObjectType(location);
            if (locType == AMObject.ORGANIZATION) {
		bOrganization = true;
		logEvent("ATTEMPT_DIR_MGR_CREATE_GROUP_CONTAINER_IN_ORG",
		    params);
                AMOrganization parent = sc.getOrganization(location);
		parent.createGroupContainers(input);
		logEvent("SUCCEED_DIR_MGR_CREATE_GROUP_CONTAINER_IN_ORG",
		    params);
            } else if (locType == AMObject.ORGANIZATIONAL_UNIT) {
		bContainer = true;
		logEvent("ATTEMPT_DIR_MGR_CREATE_GROUP_CONTAINER_IN_CONTAINER",
		    params);
                AMOrganizationalUnit parent =
                    sc.getOrganizationalUnit(location);
                parent.createGroupContainers(input);
		logEvent("SUCCEED_DIR_MGR_CREATE_GROUP_CONTAINER_IN_CONTAINER",
		    params);
            } else {
		logEvent(
		    "ATTEMPT_DIR_MGR_CREATE_GROUP_CONTAINER_IN_GRP_CONTAINER",
		    params);
                AMGroupContainer parent = sc.getGroupContainer(location);
                parent.createSubGroupContainers(input);
		logEvent(
		    "SUCCEED_DIR_MGR_CREATE_GROUP_CONTAINER_IN_GRP_CONTAINER",
		    params);
            }
        } catch (AMException e) {
	    String msgId = null;
	    if (bOrganization) {
		msgId = "AM_EXCEPTION_DIR_MGR_CREATE_GROUP_CONTAINER_IN_ORG";
	    } else {
		msgId = (bContainer) ?
		    "AM_EXCEPTION_DIR_MGR_CREATE_GROUP_CONTAINER_IN_CONTAINER":
		 "AM_EXCEPTION_DIR_MGR_CREATE_GROUP_CONTAINER_IN_GRP_CONTAINER";
	    }
	    String strError = getErrorString(e);
	    String[] paramsEx = {location, name, strError};
	    logEvent(msgId, paramsEx);

            throw new AMConsoleException(strError);
        } catch (SSOException e) {
	    String msgId = null;
	    if (bOrganization) {
		msgId = "SSO_EXCEPTION_DIR_MGR_CREATE_GROUP_CONTAINER_IN_ORG";
	    } else {
		msgId = (bContainer) ?
		    "SSO_EXCEPTION_DIR_MGR_CREATE_GROUP_CONTAINER_IN_CONTAINER":
		"SSO_EXCEPTION_DIR_MGR_CREATE_GROUP_CONTAINER_IN_GRP_CONTAINER";
	    }
	    String strError = getErrorString(e);
	    String[] paramsEx = {location, name, strError};
	    logEvent(msgId, paramsEx);

            debug.error("GroupContainerModelImpl.createGroupContainer", e);
        }
    }    
    
    /**
     * Returns people container creation property XML.
     *
     * @return people container creation property XML.
     */
    public String getCreateGroupContainerXML() {
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
            debug.error("GroupContainerModel.getDataMap", e);
        } catch (SSOException e) {
	    debug.error("GroupContainerModel.getDataMap", e);
	}

        return map;
    }

    public boolean hasDisplayProperties() {
	Map tmp = getDataMap();
	tmp.remove(NAME);
	return !tmp.isEmpty();
    }

    private static final String CREATE_PROPERTIES = "<section name=\"attributes\" defaultValue=\"\" ><property><label name=\"lblPcName\" defaultValue=\"label.name\" labelFor=\""+NAME+"\" /><cc name=\""+NAME+"\" tagclass=\"com.sun.web.ui.taglib.html.CCTextFieldTag\" /></property></section>";
}
