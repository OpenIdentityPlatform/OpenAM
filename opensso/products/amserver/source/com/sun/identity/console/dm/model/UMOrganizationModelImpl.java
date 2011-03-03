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
 * $Id: UMOrganizationModelImpl.java,v 1.4 2008/10/02 16:31:28 veiming Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.console.dm.model;

import com.sun.identity.console.base.AMViewConfig;
import com.sun.identity.console.base.model.AMAdminConstants;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMModelBase;
import com.sun.identity.console.base.model.AMSystemConfig;
import com.sun.identity.console.delegation.model.DelegationConfig;
import com.sun.identity.console.property.PropertyTemplate;
import com.sun.identity.console.property.PropertyXMLBuilder;
import com.sun.identity.console.property.PropertyXMLBuilderBase;
import com.sun.identity.sm.SMSException;
import com.iplanet.am.sdk.AMConstants;
import com.iplanet.am.sdk.AMException;
import com.iplanet.am.sdk.AMObject;
import com.iplanet.am.sdk.AMOrganization;
import com.iplanet.am.sdk.AMOrganizationalUnit;
import com.iplanet.am.sdk.AMSearchControl;
import com.iplanet.am.sdk.AMSearchResults;
import com.iplanet.sso.SSOException;
import com.sun.identity.common.admin.AdminInterfaceUtils;
import com.sun.identity.sm.AttributeSchema;
import com.sun.identity.sm.SchemaType;
import com.sun.identity.sm.ServiceSchemaManager;
import com.sun.identity.sm.ServiceSchema;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;                                                
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;

/* - LOG COMPLETE - */

/**
 * This model is used by organization related views.
 */
public class UMOrganizationModelImpl
    extends DMModelBase
    implements UMOrganizationModel
{
    private Map localizedAttrNames = null;
    private AMOrganization org = null;
    private ServiceSchemaManager entrySpecificSvcMgr = null;
    private static String ORGANIZATION = "Organization";

    /**
     * Creates a organization navigation model implementation object
     *
     * @param req  HTTP Servlet Request
     * @param map of user information
     */
    public UMOrganizationModelImpl(HttpServletRequest req, Map map) {
	super(req, map);
    }    

    /**
     * Returns assignable services of an organization.
     *
     * @param location Location DN of the organization.
     * @return assignable services of an organization.
     */
    public Map getAssignableServiceNames(String location)
        throws AMConsoleException
    {
        Map  available = null;

        try {
            AMOrganization org = getAMStoreConnection().getOrganization(
                location);
            Set currentServices = org.getRegisteredServiceNames();
            Set parentServices = null;
            if (location.equals(AMSystemConfig.defaultOrg)) {
                parentServices = getAMStoreConnection().getServiceNames();
            } else {
                String parentDN = org.getParentDN();
                AMOrganization parent = getAMStoreConnection().getOrganization(
                    parentDN);
                parentServices = parent.getRegisteredServiceNames();
            }
	    
            parentServices.removeAll(currentServices);
            if (parentServices != null &&  !parentServices.isEmpty()) {
                available = new HashMap(parentServices.size() * 2);
		AMViewConfig viewConfig = AMViewConfig.getInstance();

                for (Iterator i = parentServices.iterator(); i.hasNext(); ) {
		    String name = (String)i.next();
		    if (viewConfig.isServiceVisible(name)) {
			String displayName = getLocalizedServiceName(name);
			if (!name.equals(displayName)) {
			    available.put(name, displayName);
			}
                    }
		} 
            }
        } catch (AMException e) {
            debug.warning("`OrganizationModel.getAssignableServices", e);
	    throw new AMConsoleException(e.getMessage());
        } catch (SSOException e) {
            debug.warning("OrganizationModel.getAssignableServices", e);
        }

        return (available == null) ? Collections.EMPTY_MAP : available;
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
	    logEvent("ATTEMPT_DIR_MGR_GET_ASSIGNED_SERVICE_TO_ORG", param);
            AMOrganization org = getAMStoreConnection().getOrganization(
                location);
            Set tmp = org.getRegisteredServiceNames();
            if (tmp != null &&  !tmp.isEmpty()) {
                names = new HashMap(tmp.size() * 2);
		AMViewConfig viewConfig = AMViewConfig.getInstance();

                for (Iterator iter = tmp.iterator(); iter.hasNext(); ) {
		    String name = (String)iter.next();
		    if (viewConfig.isServiceVisible(name)) {
        		String displayName = getLocalizedServiceName(name);
        		if (!name.equals(displayName)) {
        		    names.put(name, displayName);
			}
		    }
                 } 
            }
	    logEvent("SUCCEED_DIR_MGR_GET_ASSIGNED_SERVICE_TO_ORG", param);
        } catch (SSOException e) {
	    String[] paramsEx = {location, getErrorString(e)};
	    logEvent("SSO_EXCEPTION_DIR_MGR_GET_ASSIGNED_SERVICE_TO_ORG",
		paramsEx);
            debug.warning("OrganizationModel.getAssignedServices", e);
        } catch (AMException e) {
	    String[] paramsEx = {location, getErrorString(e)};
	    logEvent("AM_EXCEPTION_DIR_MGR_GET_ASSIGNED_SERVICE_TO_ORG",
		paramsEx);
            debug.warning("OrganizationModel.getAssignedServices", e);
        }
        return (names == null) ? Collections.EMPTY_MAP : names;
    }
        
    /**
     * Removes the specified services from this organization.
     *
     * @param location name of current organization.
     * @param services of services to remove from the organization.
     */
    public void removeServices(String location, Set services) 
        throws AMConsoleException
    {
        if (services == null || services.isEmpty()) {
            throw new AMConsoleException("no.entries.selected");
        }
	if (location == null || location.length() == 0) {
            throw new AMConsoleException("system.error");
	}

        AMOrganization org = null;

        try {
            org = getAMStoreConnection().getOrganization(location);
        } catch (SSOException ssoe) {
            debug.warning("UMOrganizationModelImpl.removeServices", ssoe);
        }
        if (org == null) {
            throw new AMConsoleException("invalid.organization.entry");
        }

	String[] params = new String[2];
	params[0] = location;
	String currentName = "";

        for (Iterator i = services.iterator(); i.hasNext();) {
            try {
                String name = (String)i.next();
		currentName = name;
		params[1] = name;
		logEvent("ATTEMPT_DIR_MGR_REMOVE_SERVICES_FROM_ORG", params);
                org.unregisterService(name);
		logEvent("SUCCEED_DIR_MGR_REMOVE_SERVICES_FROM_ORG", params);
            } catch (SSOException e) {
		String strError = getErrorString(e);
		String[] paramsEx = {location, currentName, strError};
		logEvent("SSO_EXCEPTION_DIR_MGR_REMOVE_SERVICES_FROM_ORG",
		    paramsEx);
                debug.warning("UMOrganizationModel.removeServices", e);
		throw new AMConsoleException(strError);
            } catch (AMException e) {
		String strError = getErrorString(e);
		String[] paramsEx = {location, currentName, strError};
		logEvent("AM_EXCEPTION_DIR_MGR_REMOVE_SERVICES_FROM_ORG",
		    paramsEx);
                debug.warning("UMOrganizationModel.removeServices", e);
		throw new AMConsoleException(strError);
            }
        }
    }

    /**
     * Gets a set of organizations
     *
     * @param location name or current organization location
     * @param filter wildcards
     * @return a set of organizations
     */
    public Set getOrganizations(String location, String filter) {
        if (location == null) {
            location = getStartDSDN();
        }

        AMSearchResults results = null;
        AMSearchControl searchControl = new AMSearchControl();
        setSearchControlAttributes(location, SUB_SCHEMA_ORGANIZATION,
        AMObject.ORGANIZATION, searchControl, ORGANIZATIONS);
        setSearchControlLimits(searchControl);

        try {
            String[] params = {location, filter};
            logEvent("ATTEMPT_DIR_MGR_SEARCH_ORG_IN_ORG", params);
            AMOrganization org = getAMStoreConnection().getOrganization(
                location);
            results = getOrganizations(org, filter, searchControl);
            logEvent("SUCCEED_DIR_MGR_SEARCH_ORG_IN_ORG", params);
        } catch (SSOException e) {
            String strError = getErrorString(e);
            String[] paramsEx = {location, filter, strError};
            logEvent("SSO_EXCEPTION_DIR_MGR_SEARCH_ORG_IN_ORG", paramsEx);
            debug.warning("UMOrganizationModelImpl.getOrganizations", e);
        } catch (AMException e) {
            String strError = getErrorString(e);
            String[] paramsEx = {location, filter, strError};
            logEvent("AM_EXCEPTION_DIR_MGR_SEARCH_ORG_IN_ORG", paramsEx);
            debug.warning("UMOrganizationModelImpl.getOrganizations", e);
        }

        return setSearchResults(results);
    }

    // retrieves the organizations to be displayed
    private AMSearchResults getOrganizations(
        AMOrganization parent,
        String wildcard,
        AMSearchControl searchControl)
        throws AMException, SSOException
    {
        searchControl.setSearchScope(AMConstants.SCOPE_SUB);
        String [] sortKeys = new String[1];
        sortKeys[0] = AdminInterfaceUtils.getNamingAttribute(
            AMObject.ORGANIZATION, debug);
        searchControl.setSortKeys(sortKeys);
        return parent.searchSubOrganizations(wildcard, searchControl);
    }

    public void updateOrganization(String name, Map raw)
        throws AMConsoleException
    {
	Map data = removeEmptyValueInMap(raw);
	validateRequiredAttributes(data);
	String[] param = {name};
	logEvent("ATTEMPT_DIR_MGR_MODIFY_ORGANIZATION", param);

        try {
            AMOrganization ao = getAMStoreConnection().getOrganization(name);
            ao.setAttributes(data);
            ao.store();
	    logEvent("SUCCEED_DIR_MGR_MODIFY_ORGANIZATION", param);
        } catch (AMException e) {
	    String strError = getErrorString(e);
	    String[] paramsEx = {name, strError};
	    logEvent("AM_EXCEPTION_DIR_MGR_MODIFY_ORGANIZATION", paramsEx);
            throw new AMConsoleException(strError);
        } catch (SSOException e) {
	    String strError = getErrorString(e);
	    String[] paramsEx = {name, strError};
	    logEvent("SSO_EXCEPTION_DIR_MGR_MODIFY_ORGANIZATION", paramsEx);
            throw new AMConsoleException(strError);
        }
    }

    private Map removeEmptyValueInMap(Map raw) {
	Map data = new HashMap(raw.size() *2);

        for (Iterator i= raw.keySet().iterator(); i.hasNext();) {
            String key = (String)i.next();
            Set values = (Set)raw.get(key);

            if (values != null && !values.isEmpty()) {
		/* 20050623 Dennis/Jon
		 * HACK: avoid Attribute uniqueness violation.
		 */
		if (values.size() == 1) {
		    String val = (String)values.iterator().next();
		    if (val.trim().length() == 0) {
			data.put(key, Collections.EMPTY_SET);
		    } else {
			data.put(key, values);
		    }
		} else {
		    data.put(key, values);
		}
	    }
        }

	return data;
    }

    public void createOrganization(String location, String name, Map raw)
        throws AMConsoleException
    {
        if ( (raw == null) || raw.isEmpty()) {
            debug.warning("null or missing data values");
            throw new AMConsoleException(
                getLocalizedString("createFailure.message"));
        }
        if (name == null || name.length() == 0) {
            throw new AMConsoleException(
                getLocalizedString("createFailure.message"));
        }

        if (location == null) {
            location = getStartDSDN();
        }

	Map data = removeEmptyValueInMap(raw);
	validateRequiredAttributes(data);

        Map input = new HashMap(2);
        input.put(name,data);
	String[] params = {location, name};
	logEvent("ATTEMPT_DIR_MGR_CREATE_ORG_IN_ORG", params);

        String createdObj = AdminInterfaceUtils.getNamingAttribute(
                AMObject.ORGANIZATION, AMModelBase.debug) +
            "=" + name + "," + location;

        try {
	    int type = getObjectType(location);
	    if (type == AMObject.ORGANIZATION) {  
                AMOrganization parent = getAMStoreConnection().getOrganization(
                    location);
                parent.createSubOrganizations(input);
		logEvent("SUCCEED_DIR_MGR_CREATE_ORG_IN_ORG", params);
	    } else if (type == AMObject.ORGANIZATIONAL_UNIT) {
                AMOrganizationalUnit parent = 
		    getAMStoreConnection().getOrganizationalUnit(location);
                parent.createOrganizations(input);
		logEvent("SUCCEED_DIR_MGR_CREATE_ORG_IN_ORG", params);
            } else { 
                if (debug.warningEnabled()) {
		    debug.warning("UMOrganizationModel.createOrganization: " +
			"current location invalid for create, " + location);
		}
	    }
        } catch (AMException e) {

            if (debug.warningEnabled()) {
                debug.warning("UMCreateOrgModelImpl.createOrganization " +
                    e.getMessage());
            }
	    String strError = getErrorString(e);
	    String[] paramsEx = {location, name, strError};
	    logEvent("AM_EXCEPTION_DIR_MGR_CREATE_ORG_IN_ORG", paramsEx);
            throw new AMConsoleException(strError);
        } catch (SSOException e) {
	    String strError = getErrorString(e);
	    String[] paramsEx = {location, name, strError};
	    logEvent("SSO_EXCEPTION_DIR_MGR_CREATE_ORG_IN_ORG", paramsEx);
            debug.error("UMOrganizationModelImpl.createOrganization", e);
        }
    }

    public Map getValues(String name) 
        throws AMConsoleException
    {
        Map map = null;

        try {
            String[] param = {name};
            logEvent("ATTEMPT_DIR_MGR_GET_ORG_ATTR_VALUES", param);
            AMOrganization ao = getAMStoreConnection().getOrganization(name);
            map = correctAttributeNameCase(ao.getAttributes());
            logEvent("SUCCEED_DIR_MGR_GET_ORG_ATTR_VALUES", param);
        } catch (AMException e) {
	    String strError = getErrorString(e);
	    String[] paramsEx = {name, strError};
	    logEvent("AM_EXCEPTION_DIR_MGR_GET_ORG_ATTR_VALUES", paramsEx);
            throw new AMConsoleException(strError);
        } catch (SSOException e) {
	    String strError = getErrorString(e);
	    String[] paramsEx = {name, strError};
	    logEvent("SSO_EXCEPTION_DIR_MGR_GET_ORG_ATTR_VALUES", paramsEx);
            throw new AMConsoleException(strError);
        }

        return (map == null) ? Collections.EMPTY_MAP : map;
    }

    private Map correctAttributeNameCase(Map map) {
        Map corrected = null;
        if ((map != null) && !map.isEmpty()) {
            corrected = new HashMap(map.size() *2);
            Map dataMap = getDataMap();
            for (Iterator i = dataMap.keySet().iterator(); i.hasNext(); ) {
                String attrName = (String)i.next();
                Object values = map.get(attrName.toLowerCase());
                if (values == null) {
                    values = Collections.EMPTY_SET;
                }
                map.put(attrName, values);
                corrected.put(attrName, values);
            }
        }
        return corrected;
    }

    /**
     * Returns Map of attribute name to empty set of values.
     *
     * @throws AMConsoleException if map cannot be obtained.
     */
    public Map getDataMap() {
        Map map = new HashMap();

        try {
            ServiceSchema sub = getSubSchema(DMConstants.ENTRY_SPECIFIC_SERVICE,
                SchemaType.GLOBAL, ORGANIZATION);
            Set attrSchemas = sub.getAttributeSchemas();
            for (Iterator iter = attrSchemas.iterator(); iter.hasNext(); ) {
                AttributeSchema as = (AttributeSchema)iter.next();
                map.put(as.getName(), Collections.EMPTY_SET);
            }
        } catch (SMSException e) {
            debug.error("OrganizationModelImpl.getDataMap", e);
        } catch (SSOException e) {
            debug.error("OrganizationModelImpl.getDataMap", e);
	}

        return map;
    }

    /**
     * Returns sub realm creation property XML.
     *
     * @return sub realm creation property XML.
     */
    public String getCreateOrganizationPropertyXML()
	throws AMConsoleException {
        StringBuffer buff = new StringBuffer(2000);
        buff.append(PropertyXMLBuilderBase.getXMLDefinitionHeader())
            .append(PropertyTemplate.START_TAG)
            .append(PROPERTY_SECTION_CREATION_GENERAL);
        getPropertyXML(buff, false);
        buff.append(PropertyTemplate.END_TAG);
        return buff.toString();
    }

    /**
     * Returns organization profile property sheet XML.
     *
     * @param realmName Realm/Organization Name.
     * @param viewbeanClassName Class Name of view bean.
     * @return organization profile property sheet XML.
     */
    public String getOrganizationProfileXML(
	String realmName,
	String viewbeanClassName
    ) throws AMConsoleException {
	DelegationConfig dConfig = DelegationConfig.getInstance();
	boolean canModify = dConfig.hasPermission(realmName, null,
	    AMAdminConstants.PERMISSION_MODIFY, this, viewbeanClassName);
        StringBuffer buff = new StringBuffer(2000);
        buff.append(PropertyXMLBuilderBase.getXMLDefinitionHeader())
            .append(PropertyTemplate.START_TAG) ;
        getPropertyXML(buff, !canModify);
        buff.append(PropertyTemplate.END_TAG);
        return buff.toString();
    }

    private void getPropertyXML(StringBuffer buff, boolean readonly)
	throws AMConsoleException
    {
        try {
            ServiceSchema sub = getSubSchema(DMConstants.ENTRY_SPECIFIC_SERVICE,
                SchemaType.GLOBAL, "Organization");
            PropertyXMLBuilder xmlBuilder = 
                new PropertyXMLBuilder(sub, this);
	    xmlBuilder.setAllAttributeReadOnly(readonly);
            buff.append(xmlBuilder.getXML(false));

	    setMandatoryAttributes(xmlBuilder.getAttributeSchemas());
        } catch (SSOException e) {  
	    throw new AMConsoleException(getErrorString(e));
        } catch (SMSException e) {
	    throw new AMConsoleException(getErrorString(e));
        }

    }

    public void registerService(String organization, String service) 
        throws AMConsoleException 
    {
	String[] params = {organization, service};
	logEvent("ATTEMPT_DIR_MGR_ADD_SERVICE_TO_ORG", params);

        try {
            AMOrganization org = getAMStoreConnection().getOrganization(
                organization);
            org.registerService(service, true, true);
	    logEvent("SUCCEED_DIR_MGR_ADD_SERVICE_TO_ORG", params);
        } catch (AMException e) {
	    String strError = getErrorString(e);
	    String[] paramsEx = {organization, service, strError};
	    logEvent("AM_EXCEPTION_DIR_MGR_ADD_SERVICE_TO_ORG", paramsEx);
            throw new AMConsoleException(strError);
        } catch (SSOException e) {
	    String strError = getErrorString(e);
	    String[] paramsEx = {organization, service, strError};
	    logEvent("SSO_EXCEPTION_DIR_MGR_ADD_SERVICE_TO_ORG", paramsEx);
            throw new AMConsoleException(strError);
        }
    }

    private static final String PROPERTY_SECTION_CREATION_GENERAL = "<section name=\"general\" defaultValue=\"realm.sectionHeader.general\"><property required=\"true\"><label name=\"lblName\" defaultValue=\"authDomain.attribute.label.name\" labelFor=\"tfName\" /><cc name=\"tfName\" tagclass=\"com.sun.web.ui.taglib.html.CCTextFieldTag\" ><attribute name=\"autoSubmit\" value=\"false\" /></cc></property></section>";
}
