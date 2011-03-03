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
 * $Id: PeopleContainerModelImpl.java,v 1.3 2008/07/10 23:27:23 veiming Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.console.dm.model;

import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.property.PropertyXMLBuilder;
import com.sun.identity.console.property.PropertyTemplate;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.AttributeSchema;
import com.sun.identity.sm.SchemaType;
import com.sun.identity.sm.ServiceSchemaManager;
import com.sun.identity.sm.ServiceSchema;
import com.iplanet.am.sdk.AMConstants;
import com.iplanet.am.sdk.AMException;
import com.iplanet.am.sdk.AMObject;
import com.iplanet.am.sdk.AMOrganization;
import com.iplanet.am.sdk.AMOrganizationalUnit;
import com.iplanet.am.sdk.AMPeopleContainer;
import com.iplanet.am.sdk.AMSearchControl;
import com.iplanet.am.sdk.AMSearchResults;
import com.iplanet.am.sdk.AMStoreConnection;
import com.iplanet.sso.SSOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;

/* - LOG COMPLETE - */

/**
 * This model is used by <code>PeopleContainerViewBeans</code>
 */
public class PeopleContainerModelImpl extends DMModelBase
    implements PeopleContainerModel {

    private Set containers = null;
    
    /**
     * Creates a role navigation model implementation object
     *
     * @param req  HTTP Servlet Request
     * @param map of user information
     */
    public PeopleContainerModelImpl(HttpServletRequest req, Map map) {
	super(req, map);
    }    
        
    /**
     * Gets a set of People Containers
     *
     * @param filter wildcards
     * @return a set of People Containers
     */
    public Set getPeopleContainers(String location, String filter) {
	if (containers == null) {
            AMStoreConnection sc = getAMStoreConnection();
	    AMSearchResults results = null;
	    AMSearchControl searchControl = new AMSearchControl();
	    searchControl.setSearchScope(AMConstants.SCOPE_SUB);
            setSearchControlLimits(searchControl);
            setSearchControlAttributes(
                location, 
                DMConstants.SUB_SCHEMA_PEOPLE_CONTAINER,
                AMObject.PEOPLE_CONTAINER, 
                searchControl, 
                DMConstants.PEOPLE_CONTAINERS);

	    String[] params = {location, filter};
	    boolean bOrganization = false;
	    boolean bContainer = false;

	    try {
		switch (getObjectType(location)) {
		case AMObject.ORGANIZATION:
		    bOrganization = true;
		    logEvent("ATTEMPT_DIR_MGR_SEARCH_PEOPLE_CONTAINERS_IN_ORG",
			params);
		    AMOrganization org = sc.getOrganization(location);
		    results = org.searchPeopleContainers(filter, searchControl);
		    logEvent("SUCCEED_DIR_MGR_SEARCH_PEOPLE_CONTAINERS_IN_ORG",
			params);
		    break;
		case AMObject.ORGANIZATIONAL_UNIT:
		    bContainer = true;
		    logEvent(
			"ATTEMPT_DIR_MGR_SEARCH_PEOPLE_CONTAINERS_IN_CONTAINER",
			params);
		    AMOrganizationalUnit orgUnit =
			sc.getOrganizationalUnit(location);
		    results = orgUnit.searchPeopleContainers(
			filter, searchControl);
		    logEvent(
			"SUCCEED_DIR_MGR_SEARCH_PEOPLE_CONTAINERS_IN_CONTAINER",
			params);
		    break;
		case AMObject.PEOPLE_CONTAINER:
		    logEvent(
			"ATTEMPT_DIR_MGR_SEARCH_PEOPLE_PPLE_CONTS_IN_PPLE_CONT",
			params);
		    AMPeopleContainer peopleContainer =
			sc.getPeopleContainer(location);
		    containers = peopleContainer.searchSubPeopleContainers(
			filter, AMConstants.SCOPE_ONE);
		    createResultsMap(containers);
		    logEvent(
			"SUCCEED_DIR_MGR_SEARCH_PEOPLE_PPLE_CONTS_IN_PPLE_CONT",
			params);
		    break;			
		default:
		    if (debug.warningEnabled()) {
			debug.warning(
			    "PeopleContainerModel.getPeopleContainers"
			    + "invalid location " + locationType);
		    }
		}
	    } catch (SSOException e) {
		String strError = getErrorString(e);
		String[] paramsEx = {location, filter, strError};
		String msgId = null;
		if (bOrganization) {
		    msgId =
			"SSO_EXCEPTION_DIR_MGR_SEARCH_PEOPLE_CONTAINERS_IN_ORG";
		} else if (bContainer) {
		    msgId =
		  "SSO_EXCEPTION_DIR_MGR_SEARCH_PEOPLE_CONTAINERS_IN_CONTAINER";
		} else {
		    msgId =
		  "SSO_EXCEPTION_DIR_MGR_SEARCH_PEOPLE_PPLE_CONTS_IN_PPLE_CONT";
		}
		logEvent(msgId, paramsEx);
		debug.warning("PeopleContainerModel.getPeopleContainers", e);
	    } catch (AMException e) {
		searchErrorMsg = getErrorString(e);
		String[] paramsEx = {location, filter, searchErrorMsg};
		String msgId = null;
		if (bOrganization) {
		    msgId =
			"AM_EXCEPTION_DIR_MGR_SEARCH_PEOPLE_CONTAINERS_IN_ORG";
		} else if (bContainer) {
		    msgId =
		   "AM_EXCEPTION_DIR_MGR_SEARCH_PEOPLE_CONTAINERS_IN_CONTAINER";
		} else {
		    msgId =
		   "AM_EXCEPTION_DIR_MGR_SEARCH_PEOPLE_PPLE_CONTS_IN_PPLE_CONT";
		}
		logEvent(msgId, paramsEx);
		debug.warning("PeopleContainerModel.getPeopleContainers", e);
	    }
	
	    if (results != null) {
		containers = results.getSearchResults();
                resultsMap = results.getResultAttributes();
		searchErrorMsg = getSearchResultWarningMessage(results);
	    }
	}
	return containers;
    }

    private void createResultsMap(Set pcSet) {
	resultsMap = new HashMap();

	for (Iterator iter = pcSet.iterator(); iter.hasNext(); ) {
	    String dn = (String)iter.next();
	    Map map = new HashMap(2);
	    Set set = new HashSet(2);
	    set.add(DNToName(dn, false));
	    map.put(dn.substring(0, dn.indexOf('=')), set);
	    resultsMap.put(dn, map);
	}
    }

    /**
     * Deletes the People Containers that were selected
     *
     * @param pcDNSet of people container DNs that should be deleted
     * @return true if the People Containers delete operation is successful
     */
    public boolean deletePeopleContainers(Set pcDNSet) {
	boolean delete = false;
	if (pcDNSet != null) {
	    try {
		Map failedPCMap = deleteObject(pcDNSet);

		if (!failedPCMap.isEmpty()) {
		    Iterator it = failedPCMap.keySet().iterator();
		    setErrorMessage(getLocalizedString(
			"deletePCFailed.message"));
		
		    while (it.hasNext()) {
			String failedPCDN = (String)it.next();
			String failedPCName = DNToName(failedPCDN, false);
			String errMessage = (String)failedPCMap.get(failedPCDN);
			setErrorMessage(failedPCName + " - " + errMessage);
		    }
		} else {
		    delete = true;
		}
	    } catch (AMConsoleException ace) {
		setErrorMessage(getErrorString(ace));
	    }
	} else {
	    setErrorMessage(getLocalizedString("deleteFailed.message"));
	}
	return delete;
    }

    /**
     * Returns people container creation property XML.
     *
     * @return people container creation property XML.
     */
    public String getCreatePeopleContainerXML() {
        StringBuffer buff = new StringBuffer(2000);
        buff.append(PropertyTemplate.DEFINITION)
            .append(PropertyTemplate.START_TAG)
            .append(CREATE_PROPERTIES);
        getPropertyXML(DMConstants.ENTRY_SPECIFIC_SERVICE, 
            DMConstants.SUB_SCHEMA_PEOPLE_CONTAINER, SchemaType.GLOBAL,buff);

        buff.append(PropertyTemplate.END_TAG);
        return buff.toString();
    }


    public Map getDataMap() {
        Map map = new HashMap();

        try {
            ServiceSchema sub = getSubSchema(
                DMConstants.ENTRY_SPECIFIC_SERVICE, SchemaType.GLOBAL, 
                DMConstants.SUB_SCHEMA_PEOPLE_CONTAINER);

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
            debug.error("PeopleContainerModel.getDataMap", e);
        } catch (SSOException e) {
            debug.error("PeopleContainerModel.getDataMap", e);
	}

        return map;
    }

    public void createPeopleContainer(String location, Map data) 
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
		logEvent("ATTEMPT_DIR_MGR_CREATE_PEOPLE_CONTAINER_IN_ORG",
		    params);
                AMOrganization parent = sc.getOrganization(location);
		parent.createPeopleContainers(input);
		logEvent("SUCCEED_DIR_MGR_CREATE_PEOPLE_CONTAINER_IN_ORG",
		    params);
            } else if (locType == AMObject.ORGANIZATIONAL_UNIT) {
		bContainer = true;
		logEvent("ATTEMPT_DIR_MGR_CREATE_PEOPLE_CONTAINER_IN_CONTAINER",
		    params);
                AMOrganizationalUnit parent =sc.getOrganizationalUnit(location);
                parent.createPeopleContainers(input);
		logEvent("SUCCEED_DIR_MGR_CREATE_PEOPLE_CONTAINER_IN_CONTAINER",
		    params);
            } else {
		logEvent("ATTEMPT_DIR_MGR_CREATE_PEOPLE_CONTAINER_IN_PPLE_CONT",
		    params);
                AMPeopleContainer parent = sc.getPeopleContainer(location);
                parent.createSubPeopleContainers(input);
		logEvent("SUCCEED_DIR_MGR_CREATE_PEOPLE_CONTAINER_IN_PPLE_CONT",
		    params);
            }
        } catch (AMException e) {
	    String strError = getErrorString(e);
	    String[] paramsEx = {location, name, strError};
	    String msgId = null;
	    if (bOrganization) {
		msgId = "AM_EXCEPTION_DIR_MGR_CREATE_PEOPLE_CONTAINER_IN_ORG";
	    } else if (bContainer) {
		msgId =
		    "AM_EXCEPTION_DIR_MGR_CREATE_PEOPLE_CONTAINER_IN_CONTAINER";
	    } else {
		msgId =
		    "AM_EXCEPTION_DIR_MGR_CREATE_PEOPLE_CONTAINER_IN_PPLE_CONT";
	    }
	    logEvent(msgId, paramsEx);
            throw new AMConsoleException(strError);
        } catch (SSOException e) {
	    String strError = getErrorString(e);
	    String[] paramsEx = {location, name, strError};
	    String msgId = null;
	    if (bOrganization) {
		msgId = "SSO_EXCEPTION_DIR_MGR_CREATE_PEOPLE_CONTAINER_IN_ORG";
	    } else if (bContainer) {
		msgId =
		   "SSO_EXCEPTION_DIR_MGR_CREATE_PEOPLE_CONTAINER_IN_CONTAINER";
	    } else {
		msgId =
		"SSO_EXCEPTION_DIR_MGR_CREATE_PEOPLE_CONTAINER_IN_PPLE_CONT";
	    }
	    logEvent(msgId, paramsEx);
        }
    }

    protected void getPropertyXML(
        String serviceName,
        String subSchemaName,
        SchemaType type,
        StringBuffer buff
    ) {
        try {                                                      
            ServiceSchema sub = getSubSchema(serviceName, type, subSchemaName);
	    if (sub != null) {
                PropertyXMLBuilder xmlBuilder =
                    new PropertyXMLBuilder(sub, this);
                buff.append(xmlBuilder.getXML(false));
		setMandatoryAttributes(xmlBuilder.getAttributeSchemas());
	    }
        } catch (SSOException e) {
            debug.error("PeopleContainerModelImpl.getPropertyXML", e);
        } catch (SMSException e) {
            debug.error("PeopleContainerModelImpl.getPropertyXML", e);
        } catch (AMConsoleException e) {
            debug.error("PeopleContainerModelImpl.getPropertyXML", e);
        }

    }

    protected ServiceSchema getServiceSchema(
	String serviceName,
	SchemaType type
    ) throws SMSException, SSOException 
    {
        ServiceSchemaManager manager = getServiceSchemaManager(serviceName);
        ServiceSchema ss = manager.getSchema(type);
        return ss;
    }

    protected ServiceSchema getSubSchema(
        String serviceName, 
        SchemaType type, 
        String subschema
    ) throws SMSException, SSOException 
    {
        ServiceSchema service = getServiceSchema(serviceName, type);
        ServiceSchema tmp = service.getSubSchema(subschema);
        return tmp;
    }

    public boolean hasDisplayProperties() {
	Map tmp = getDataMap();
	tmp.remove(NAME);
	return !tmp.isEmpty();
    }

    private static final String CREATE_PROPERTIES = "<section name=\"attributes\" defaultValue=\"\" ><property><label name=\"lblPcName\" defaultValue=\"label.name\" labelFor=\""+NAME+"\" /><cc name=\""+NAME+"\" tagclass=\"com.sun.web.ui.taglib.html.CCTextFieldTag\" /></property></section>";
}
