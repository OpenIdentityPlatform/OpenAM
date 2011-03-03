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
 * $Id: RoleModelImpl.java,v 1.4 2008/10/02 16:31:27 veiming Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.console.dm.model;

import com.iplanet.am.sdk.AMConstants;
import com.iplanet.am.sdk.AMException;
import com.iplanet.am.sdk.AMObject;
import com.iplanet.am.sdk.AMOrganization;
import com.iplanet.am.sdk.AMOrganizationalUnit;
import com.iplanet.am.sdk.AMFilteredRole;
import com.iplanet.am.sdk.AMRole;
import com.iplanet.am.sdk.AMSearchControl;
import com.iplanet.am.sdk.AMSearchResults;
import com.iplanet.sso.SSOException;

import com.sun.identity.common.admin.AdminInterfaceUtils;

import com.sun.identity.console.base.model.AMAdminUtils;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.property.PropertyTemplate;
import com.sun.identity.console.property.PropertyXMLBuilder;
import com.sun.identity.console.property.PropertyXMLBuilderBase;

import com.sun.identity.sm.AttributeSchema;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.SchemaType;
import com.sun.identity.sm.ServiceSchemaManager;
import com.sun.identity.sm.ServiceSchema;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;

/**
 * This model is used by <code>RoleViewBean.java</code>
 */
public class RoleModelImpl extends DMModelBase
    implements RoleModel {
    
    private static final String USERS = "users";
    private final String CONTAINER_DEFAULT_TEMPLATE_ROLE = 
	"cn=ContainerDefaultTemplateRole,";
    private Set roles = null;
    private AMRole role = null;

    private Map defaultACIMap = null;
    private String roleName = null;
    private ServiceSchemaManager userSvcMgr = null;
    private boolean filter = false;

    private static final int ACI_DESCRIPTION = 0;
    private static final int DEFAULT_PERMISSION = 1;
    private static final String NS_ROLE_FILTER = "nsRoleFilter";

    private Map mapAttributeValues = null;


    /**
     * Creates a role navigation model implementation object
     *
     * @param req  HTTP Servlet Request
     * @param map of user information
     */
    public RoleModelImpl(HttpServletRequest req, Map map) {
	super(req, map);
    }    
        
    /** 
     * Return the set of attributes used to populate the creation page.
     */
    public Map getDataMap(int type) {
        Map data = new HashMap(20);

        if (type == AMObject.FILTERED_ROLE) {
            for (Iterator it = getFilterAttributeNames().iterator(); 
                 it.hasNext(); ) 
            {
                data.put((String)it.next(), Collections.EMPTY_SET);
            }
        }
        data.put(NS_ROLE_FILTER, Collections.EMPTY_SET);
        data.put(ENTRY_NAME_ATTRIBUTE_NAME, Collections.EMPTY_SET);
        data.put(ROLE_DESCRIPTION_ATTR, Collections.EMPTY_SET);
        data.put(ROLE_TYPE_ATTR, Collections.EMPTY_SET);
        data.put(ROLE_ACI_LIST_ATTR, Collections.EMPTY_SET);

        return data;
    }    

    /**
     * Removes users from the group.
     *
     * @param location where to delete the given users
     * @param names set of distinguished names of users to be removed.
     * @throws AMConsoleException if users cannot be removed.
     */  
    public void removeUsers(String location, Set names)
        throws AMConsoleException
    {
	String userNames = AMAdminUtils.getString(names, ",", false);
        try {
            if (getObjectType(location) == AMObject.FILTERED_ROLE) {
                throw new AMConsoleException(
                    getLocalizedString("unsupported.operation"));
            }

	    String[] params = {location, userNames};
	    logEvent("ATTEMPT_DIR_MGR_REMOVE_USERS_FROM_ROLE", params);
            AMRole role =(AMRole)getAMObject(location);
            role.removeUsers(names);
	    logEvent("SUCCEED_DIR_MGR_REMOVE_USERS_FROM_ROLE", params);
        } catch (AMException ex) {
	    String strError = getErrorString(ex);
	    String[] paramsEx = {location, userNames, strError};
	    logEvent("AM_EXCEPTION_DIR_MGR_REMOVE_USERS_FROM_ROLE", paramsEx);
            debug.warning("RoleModel.removeUsers failed", ex);
	    throw new AMConsoleException(strError);
        } catch (SSOException ex) {
	    String strError = getErrorString(ex);
	    String[] paramsEx = {location, userNames, strError};
	    logEvent("SSO_EXCEPTION_DIR_MGR_REMOVE_USERS_FROM_ROLE", paramsEx);
            debug.warning("RoleModel.removeUsers failed", ex);
	    throw new AMConsoleException(strError);
        }
    }
    
    /**
     * Gets value for description field
     *   
     * @return value for description field
     */  
    private Set getDescriptionValue(AMRole role) {
        Set description = new HashSet(2);
        try {
            String value = 
                role.getStringAttribute(ROLE_DESCRIPTION_ATTR);

            if (value != null && value.length() > 0) {
                String tmp = value.trim().replace(' ','-');
                String localized = getLocalizedString(tmp);
                if (!localized.equals(tmp)) {
                    value = localized;
                }
                description.add(value);
            }    
        } catch (AMException ame) {
            debug.warning("couldn't get role description", ame);
        } catch (SSOException soe) {
            debug.warning("couldn't get role description", soe);
        }

        return (description == null) ? Collections.EMPTY_SET : description;
    }

    /**
     * Gets value for permission field
     *   
     * @return value for permission field
     */  
    private Set getPermissionValue(AMRole role) {
        Set permission = new HashSet(2);
        try {
            String value = 
                role.getStringAttribute(ROLE_ACI_DESCRIPTION_ATTR);

            if (value != null && value.length() > 0) {
                String tmp = value.trim().replace(' ','-');
                String localized = getLocalizedString(tmp);
                if (!localized.equals(tmp)) {
                    value = localized;
                }
                permission.add(value);
            }
        } catch (AMException dpe) {
            debug.warning("couldn't get role aci description");
        } catch (SSOException dpe) {
            debug.warning("couldn't get role aci description");
        }
        return (permission == null) ? Collections.EMPTY_SET : permission;
    }    
 
    public Map getValues(String name) throws AMConsoleException {
        Map values = new HashMap(6);
	Exception ex = null;

	String[] params = {name};
	logEvent("ATTEMPT_DIR_MGR_GET_ROLE_ATTR_VALUES", params);

        try {
            AMRole role =(AMRole)getAMObject(name);
            values.put(ROLE_DESCRIPTION_ATTR, getDescriptionValue(role));
            values.put(ROLE_ACI_DESCRIPTION_ATTR,getPermissionValue(role));
	    if (getObjectType(name) == AMObject.FILTERED_ROLE) {           
                Set hs = new HashSet(2);
                hs.add(((AMFilteredRole)role).getFilter());
                values.put(NS_ROLE_FILTER, hs);
	    }
	    logEvent("SUCCEED_DIR_MGR_GET_ROLE_ATTR_VALUES", params);
        } catch (AMException e) {
	    String strError = getErrorString(e);
	    String[] paramsEx = {name, strError};
	    logEvent("SSO_EXCEPTION_DIR_MGR_GET_ROLE_ATTR_VALUES", paramsEx);
	    debug.error("RoleModelImpl.getValues", e);
	    throw new AMConsoleException(strError);
	} catch (SSOException e) {
	    String strError = getErrorString(e);
	    String[] paramsEx = {name, strError};
	    logEvent("SSO_EXCEPTION_DIR_MGR_GET_ROLE_ATTR_VALUES", paramsEx);
	    debug.error("RoleModelImpl.getValues", e);
	    throw new AMConsoleException(strError);
	}
        return (values == null) ? Collections.EMPTY_MAP : values;
    }

    /** 
     * Use to modify the editable properties for a role. The current 
     * properties which can be set are the role description and the  
     * role permission description. 
     * 
     * @param name of the role to update. 
     * @param values to set for the description fields 
     */ 
    public void updateRole(String name, Map values) 
        throws AMConsoleException 
    {
        try {
	    String[] param = {name};
	    logEvent("ATTEMPT_DIR_MGR_MODIFY_ROLE", param);
            AMRole role =(AMRole)getAMObject(name);
            role.setAttributes(values);
            role.store();
	    logEvent("SUCCEED_DIR_MGR_MODIFY_ROLE", param);
        } catch (AMException e) {
	    String[] paramsEx = {name, getErrorString(e)};
	    logEvent("AM_EXCEPTION_DIR_MGR_MODIFY_ROLE", paramsEx);
            debug.warning("RoleModel.updateRole", e);
            throw new AMConsoleException(getErrorString(e));
        } catch (SSOException e) {
	    String[] paramsEx = {name, getErrorString(e)};
	    logEvent("SSO_EXCEPTION_DIR_MGR_MODIFY_ROLE", paramsEx);
            debug.warning("RoleModel.updateRole", e);
            throw new AMConsoleException(getErrorString(e));
        }
    }

    /** 
     * Returns a <code>Set</code> of user DN's which which match 
     * the given pattern.  SearchControls are based on the settings
     * in the authenticated organization administration service.
     *
     * @param location is the dn of the current role.
     * @param pattern to match the user entries.
     */
    public Set getMembers(String location, String pattern) 
        throws AMConsoleException
    {
        try {
	    String[] params = {location, pattern};
	    logEvent("ATTEMPT_DIR_MGR_GET_ROLE_MEMBERS", params);

            AMSearchControl searchControl = new AMSearchControl();
            setSearchControlLimits(searchControl);
            setSearchControlAttributes(searchControl, 
                getValidUserAttributes(getSearchReturnValue()));

            AMRole role =(AMRole)getAMObject(location);
            AMSearchResults searchResults = role.searchUsers(
                searchControl, createUserSearchFilter(pattern));
            setSearchResults(searchResults);

	    logEvent("SUCCEED_DIR_MGR_GET_ROLE_MEMBERS", params);
        } catch (AMException e) {
	    String strError = getErrorString(e);
	    String[] paramsEx = {location, pattern, strError};
	    logEvent("AM_EXCEPTION_DIR_MGR_GET_ROLE_MEMBERS", paramsEx);
            debug.warning("RoleModel.getMembers", e);
            throw new AMConsoleException(strError);
        } catch (SSOException e) {
	    String strError = getErrorString(e);
	    String[] paramsEx = {location, pattern, strError};
	    logEvent("SSO_EXCEPTION_DIR_MGR_GET_ROLE_MEMBERS", paramsEx);
            debug.warning("RoleModel.getMembers", e);
            throw new AMConsoleException(strError);
        }

        return getSearchResults();
    }

    public String getServiceXML(String service) {
        StringBuilder buff = new StringBuilder(2000);
        buff.append(PropertyXMLBuilderBase.getXMLDefinitionHeader())
            .append(PropertyTemplate.START_TAG);

        try {
	    ServiceSchema ss = getServiceSchema(service, SchemaType.DYNAMIC);
	    if (ss != null) {
                Set as = ss.getAttributeSchemas();
                for (Iterator iter = as.iterator(); iter.hasNext(); ) {
                    AttributeSchema tmp = (AttributeSchema)iter.next();
                    String i18n = tmp.getI18NKey();
         
                    if ((i18n == null) || (i18n.trim().length() == 0)) {
                        iter.remove();
                    }
                }

                PropertyXMLBuilder xmlBuilder = new PropertyXMLBuilder(
                    service, this, as);
                buff.append(xmlBuilder.getXML(false));
            }
	} catch (SMSException s) {
	    debug.error("RoleModelImpl.getServiceXML",s);
	} catch (SSOException s) {
	    debug.error("RoleModelImpl.getServiceXML",s);
	} catch (AMConsoleException s) {
	    debug.error("RoleModelImpl.getServiceXML",s);
	}

	buff.append(PropertyTemplate.END_TAG);

        return buff.toString(); 
    }

    public String getRoleProfileXML(int type) {
        StringBuilder buff = new StringBuilder(2000);
        buff.append(PropertyXMLBuilderBase.getXMLDefinitionHeader())
            .append(PropertyTemplate.START_TAG)
            .append(START_SECTION)
            .append(PROFILE_PROPERTIES);

        if (type == AMObject.FILTERED_ROLE) {
            buff.append(FILTER_ATTRIBUTE);
        }
        buff.append(PropertyTemplate.SECTION_END_TAG)
            .append(PropertyTemplate.END_TAG);
        return buff.toString();
    }

    public String getRoleCreateXML(int type) {
        StringBuilder buff = new StringBuilder(2000);
        buff.append(PropertyXMLBuilderBase.getXMLDefinitionHeader())
            .append(PropertyTemplate.START_TAG)
            .append(CREATE_PROPERTIES);

        if (type == AMObject.FILTERED_ROLE) {
            buff.append(getFilterAttributesXML());
        }
        buff.append(PropertyTemplate.SECTION_END_TAG)
            .append(PropertyTemplate.END_TAG);
        
        return buff.toString();
    }

    /**
     * Gets a set of roles.
     *
     * @param filter wildcards
     * @return a set of Roles
     */
    public Set getRoles(String location, String filter) {
	if (roles == null) {
	    AMSearchResults results = null;

            locationType = getObjectType(location);
            locationDN = location;

            AMSearchControl searchControl = new AMSearchControl();
            searchControl.setSearchScope(AMConstants.SCOPE_SUB);
            setSearchControlLimits(searchControl);
            setSearchControlAttributes(
                locationDN, 
                SUB_SCHEMA_FILTERED_ROLE,
                AMObject.ROLE, 
                searchControl, 
                ROLES);

	    boolean bOrganization = false;
	    String[] params = {location, filter};

            try {
                switch (locationType) {
		case AMObject.ORGANIZATION:
		    bOrganization = true;
		    logEvent("ATTEMPT_DIR_MGR_GET_ROLES_IN_ORG", params);
		    AMOrganization org = getAMStoreConnection().getOrganization(
			locationDN);
		    results = org.searchAllRoles(filter,searchControl);
		    logEvent("SUCCEED_DIR_MGR_GET_ROLES_IN_ORG", params);
		    break;
		case AMObject.ORGANIZATIONAL_UNIT:
		    logEvent("ATTEMPT_DIR_MGR_GET_ROLES_IN_CONTAINER", params);
		    AMOrganizationalUnit orgUnit = getAMStoreConnection().
                        getOrganizationalUnit(locationDN);
		    results = orgUnit.searchAllRoles(filter,searchControl);
		    logEvent("SUCCEED_DIR_MGR_GET_ROLES_IN_CONTAINER", params);
		    break;
		default:
		    if (debug.warningEnabled()) {
			debug.warning("RoleModelImpl.getRoles "
			    + "invalid location " + locationType);
		    }
		}
	    } catch (SSOException e) {
		String strError = getErrorString(e);
		String[] paramsEx = {location, filter, strError};
		String msgId = (bOrganization) ?
		    "SSO_EXCEPTION_DIR_MGR_GET_ROLES_IN_ORG" :
		    "SSO_EXCEPTION_DIR_MGR_GET_ROLES_IN_CONTAINER";
		logEvent(msgId, paramsEx);
		debug.warning("RoleModelImpl.getRoles", e);
	    } catch (AMException e) {
		searchErrorMsg = getErrorString(e);
		String[] paramsEx = {location, filter, searchErrorMsg};
		String msgId = (bOrganization) ?
		    "AM_EXCEPTION_DIR_MGR_GET_ROLES_IN_ORG" :
		    "AM_EXCEPTION_DIR_MGR_GET_ROLES_IN_CONTAINER";
		logEvent(msgId, paramsEx);
		debug.warning("RoleModelImpl.getRoles", e);
	    }
	    
	    roles = setSearchResults(results);
	}
	
	if (roles == null) {
	    roles = Collections.EMPTY_SET;
	} else {
	    roles.remove(CONTAINER_DEFAULT_TEMPLATE_ROLE + location);
	}	
	return roles;
    }
    
    /**
     * Gets attribute list for roles.
     *
     * @return set data for roles
     */
    public Set getAttrList() {
        return roles;
    }

    /**
     * Sets attribute list for roles.
     *
     * @param set  data for roles
     */
    public void setAttrList(Set set) {
        roles = set;
    }

    /**
     * Returns true if current location type is valid
     *
     * @return true if current location type is valid
     */
    protected boolean isCurrentLocationTypeValid() {
	boolean valid = false;

	switch (locationType) {
	case AMObject.ORGANIZATION:
	case AMObject.ORGANIZATIONAL_UNIT:
	    valid = true;
	    break;
	default:
	    if (debug.warningEnabled()) {
		debug.warning(
		    "RoleModelImpl.isCurrentLocationTypeValid: " +
		    "invalid location type, " + locationType);
	    }
	}

	return valid;
    }

    /**
     * Returns value of the return attribute in the administration service.
     *
     * @return the value of the return attribute in the administration
     *         service.
     */
    public List getSearchReturnAttributes() {
        return getSearchReturnAttributes(null, AMObject.ROLE, ROLES);
    }

    /**
     * Returns role's attribute names.
     *
     * @return a set of role's attribute names.
     */
    private Set getRoleAttributeNames() {
        Set set = Collections.EMPTY_SET;
        try {
            String namingAttr = AdminInterfaceUtils.getNamingAttribute(
                AMObject.PEOPLE_CONTAINER, debug);
            String temp = namingAttr + "=" +
                AdminInterfaceUtils.defaultPeopleContainerName() + "," + 
                getLocationDN(); 
            // warning : this is constructing the people admin role dn
            // which is not guaranteed to exist...
            String roleDN = 
                AdminInterfaceUtils.getNamingAttribute(AMObject.ROLE, debug) +
                 "=" + temp.replace(',', '_') + "," + getLocationDN();
            AMRole role = getAMStoreConnection().getRole(roleDN);
            Map map = role.getAttributes();
            if (map !=null && !map.isEmpty()) {
                set = map.keySet();
            }
        } catch (SSOException ssoe) {
            debug.error("RoleModelImpl.getRoleAttributeNames", ssoe);
        } catch (AMException ae) {
            debug.error("RoleModelImpl.getRoleAttributeNames", ae);
        }
        return set;
    }

    /**
     * Returns the validated attribute names from a return search string.
     *
     * @param returnAttr string to search attributes for.
     * @param schemaName name of schema.
     * @param objectType object type.
     * @param type navigation view type.
     * @return the validated attribute names from a return search string.
     */
    protected List getValidatedAttributes(
        String returnAttr, 
        String schemaName, 
        int objectType, 
        String type
    ) {
        List searchAttrs = Collections.EMPTY_LIST;
        if (returnAttr != null && returnAttr.length() > 0) {
            List list = getObjectDisplayList(returnAttr, type);
            if (list != null && !list.isEmpty()) {
                searchAttrs = new ArrayList(list.size());
                Set roleAttrs = getRoleAttributeNames();
                Set filteredRoleAttrs = getFilteredRoleAttributeNames();

                int validAttrSize = 0;
                if (roleAttrs != null && !roleAttrs.isEmpty()) {
                    validAttrSize = roleAttrs.size();
                }
                if (filteredRoleAttrs != null && !filteredRoleAttrs.isEmpty()) {
                    validAttrSize += filteredRoleAttrs.size();
                }
                if (validAttrSize > 0) {
                    Set set = new HashSet(validAttrSize);
                    set.addAll(roleAttrs);
                    set.addAll(filteredRoleAttrs);
                
                    Iterator iter = list.iterator();
                    while (iter.hasNext()) {
                        String str = (String)iter.next();
                        if (set.contains(str) && !searchAttrs.contains(str)) {
                            searchAttrs.add(str);
                        }
                    }
                }
            }
        }
        if (searchAttrs == null || searchAttrs.isEmpty()) {
            searchAttrs = new ArrayList(1);
            searchAttrs.add(AdminInterfaceUtils.getNamingAttribute(
                objectType, debug));
        }
        return searchAttrs;
    }

    /**
     * Returns localized name of attribute name.
     *
     * @param name of attribute.
     * @return localized name of attribute name.
     */
    public String getAttributeLocalizedName(String name) {
        return getAttributeLocalizedName(name, SUB_SCHEMA_FILTERED_ROLE);
    }

    /**
     * Returns a set of filtered role's attribute names.
     *
     * @return a set of filtered role's attribute names.
     */
    private Set getFilteredRoleAttributeNames() {
        ServiceSchemaManager mgr = null;
        try {
	    mgr = getServiceSchemaManager(ENTRY_SPECIFIC_SERVICE);
        } catch (SSOException e) {
            debug.error("RoleModelImpl.getFilteredRoleAttributeNames", e);
        } catch (SMSException e) {
            debug.error("RoleModelImpl.getFilteredRoleAttributeNames", e);
        }

        Set attrSchemaSet = getAttributesToDisplay(
            mgr, SchemaType.GLOBAL, SUB_SCHEMA_FILTERED_ROLE);

        Set set = Collections.EMPTY_SET;
        if (attrSchemaSet != null && !attrSchemaSet.isEmpty()) {
            Iterator iter = attrSchemaSet.iterator();
            set = new HashSet(attrSchemaSet.size());
            while (iter.hasNext()) {
                AttributeSchema attrSchema = (AttributeSchema)iter.next();
                String name = attrSchema.getName();
                set.add(name);
            }
        }
        return set;
    }


    /**
     * Returns the <code>AMFilteredRole</code> object for a given role dn.
     *
     * @param dn role dn.
     * @return the <code>AMFilteredRole</code> object for a given role dn.
     */
    private AMFilteredRole getFilteredRoleObject(String dn) {
        AMFilteredRole role = null;
        try {
            role = getAMStoreConnection().getFilteredRole(dn);
            if (role == null || !role.isExists()) {
                if (debug.warningEnabled()) {
                    debug.warning("RoleModelImpl.getFilteredRoleObject "
                    + "role does not exists " + dn);
                }
                role = null;
            }
        } catch (SSOException ssoe) {
            debug.warning(
                "RoleModelImpl.getFilteredRoleObject", ssoe);
        }

        return role;
    }

    /**
     * Returns the filtered role values for a given role dn.
     *
     * @param dn role dn.
     * @return the filtered role values for a given role dn.
     */
    private Set getFilteredRoleAttributeValues(String dn) {
        ServiceSchemaManager mgr = null;
        try {
	    getServiceSchemaManager(ENTRY_SPECIFIC_SERVICE);
        } catch (SSOException e) {
            debug.error("RoleModelImpl.getFilteredRoleAttributeValues", e);
        } catch (SMSException e) {
            debug.error("RoleModelImpl.getFilteredRoleAttributeValues", e);
        }
        AMFilteredRole role = getFilteredRoleObject(dn);

        Set values = Collections.EMPTY_SET;
        if (mgr != null && role != null) {
            Set attrSchemaSet = getAttributesToDisplay(
                mgr, SchemaType.GLOBAL, SUB_SCHEMA_FILTERED_ROLE);

            if ((attrSchemaSet != null) && !attrSchemaSet.isEmpty()) {
                for (Iterator iter = attrSchemaSet.iterator(); iter.hasNext();)
                {
                    AttributeSchema attrSchema = (AttributeSchema)iter.next();
                    String name = attrSchema.getName();

                    try {
                        if (name.equals(FILTERED_ROLE_FILTERINFO)) {
                            values = new HashSet(1);
                            values.add(role.getFilter());
                        } else {
                            values = role.getAttribute(name);
                        }
                    } catch (AMException ame) {
                        if (debug.warningEnabled()) {
                            debug.warning(
                                "Could not get value for " + name, ame);
                        }
                    } catch (SSOException sso) {
                        if (debug.warningEnabled()) {
                            debug.warning(
                                "Could not get value for " + name, sso);
                        }
                    }

                }
            }
        }
        return values;
    }


    /**
     * Creates a role below the current organization or organization unit. 
     *
     * @param location where to create the given role
     * @param data - map which contains the role name and the optional and
     *               required attributes.
     */  
    public void createRole(String location, Map data) 
        throws AMConsoleException
    {
        if ( (data == null) || data.isEmpty()) {
            debug.error("null or missing data values");
            throw new AMConsoleException(
                getLocalizedString("createFailure.message"));
        }

        /* 
         * get the 'type' of permission being set on the role.
         * this value is removed in the addACIDescription so 
         * we need to get it now to pass to the setDisplayOptions later.
         */
        Set pSet = (Set)data.get(ROLE_ACI_LIST_ATTR);
        String permission = (String)pSet.iterator().next();

        addACIDescription(data);

        int roleType = getRoleType(data);
        Map dataSet = getCreateRoleMap(data, location);
        String name = (String)(dataSet.keySet().iterator().next());
        Set roles = Collections.EMPTY_SET;
	String[] params = {location, name};
	boolean bOrganization = false;

        try {
            // get the appropriate parent object
            if (getObjectType(location) == AMObject.ORGANIZATION) {
		bOrganization = true;
		logEvent("ATTEMPT_DIR_MGR_CREATE_ROLES_IN_ORG", params);
                AMOrganization parent = getAMStoreConnection().getOrganization(
                    location);
                if (roleType == AMObject.FILTERED_ROLE) {
                    createFilter(data, dataSet);
                    roles = parent.createFilteredRoles(dataSet);
                } else {
                    roles = parent.createRoles(dataSet);
                }
            } else {      
		logEvent("ATTEMPT_DIR_MGR_CREATE_ROLES_IN_CONTAINER", params);
                AMOrganizationalUnit parent =
                    getAMStoreConnection().getOrganizationalUnit(location);
                if (roleType == AMObject.FILTERED_ROLE) {
                    createFilter(data, dataSet);
                    roles = parent.createFilteredRoles(dataSet);
                } else {
                    roles = parent.createRoles(dataSet);
                }
            }    
            
            /* JON TBD
            if (roles != null) {   
                setDisplayOptions(roles, permission);
                String msgId = (bOrganization) ?
                    "SUCCEED_DIR_MGR_CREATE_ROLES_IN_ORG" :
                    "SUCCEED_DIR_MGR_CREATE_ROLES_IN_CONTAINER";
                logEvent(msgId, params);
            }
             */
            
        } catch (AMException e) {
	    String strError = getErrorString(e);
	    String[] paramsEx = {location, name, strError};
	    String msgId = (bOrganization) ?
		"AM_EXCEPTION_DIR_MGR_CREATE_ROLES_IN_ORG" :
		"AM_EXCEPTION_DIR_MGR_CREATE_ROLES_IN_CONTAINER";
	    logEvent(msgId, paramsEx);
	    throw new AMConsoleException(strError);
        } catch (SSOException e) {
	    String strError = getErrorString(e);
	    String[] paramsEx = {location, name, strError};
	    String msgId = (bOrganization) ?
		"SSO_EXCEPTION_DIR_MGR_CREATE_ROLES_IN_ORG" :
		"SSO_EXCEPTION_DIR_MGR_CREATE_ROLES_IN_CONTAINER";
	    logEvent(msgId, paramsEx);
	    throw new AMConsoleException(strError);
        }
    }
/* JON CHECK ON THIS TBD
 *
    private void setDisplayOptions(Set roles, String permission) {
        if ((roles != null) && !roles.isEmpty()) {
            AMRole role = (AMRole)roles.iterator().next();

            try {
                Set displayOptions =
                    DisplayOptionsUtils.getDefaultDisplayOptions(
                        getUserSSOToken(), permission);

                if ((displayOptions != null) && !displayOptions.isEmpty()) {
                    Map map = new HashMap(2);
                    map.put(RoleSettings.ROLE_DISPLAY_OPTION_ATTRIBUTE_NAME,
                        displayOptions);
                    role.setAttributes(map);
                    role.store();
                }
            } catch (SMSException smse) {
                debug.error("UMCreateRoleImpl.setDisplayOptions", smse);
            } catch (AMException ame) {
                debug.error("UMCreateRoleImpl.setDisplayOptions", ame);
            } catch (SSOException ssoe) {
                debug.error("UMCreateRoleImpl.setDisplayOptions", ssoe);
            }
        }
    }
    
    */

    private void addACIDescription(Map dataIn) {
        // permission is an index into the defaultACIMap
        String permission = "";
        String aciDescription = null;

        /* 
         * permission is passed only when creating a role through the
         * basic role page. On the advanced page the aciDescription and
         * aciField are passed directly.
         */
	Set tmpSet = (Set)dataIn.get(ROLE_ACI_LIST_ATTR);
        String tmp = (String)tmpSet.iterator().next();
        if (tmp == null) {
            permission = (String)dataIn.remove("aciField");
            aciDescription = (String)dataIn.remove("aciDescription");
        } else {
            if (defaultACIMap == null) {
                createACIMap();
            }
            List ll = (List)defaultACIMap.get(tmp);
            permission = (String)ll.get(DEFAULT_PERMISSION);
            aciDescription = (String)ll.get(ACI_DESCRIPTION);
        }

        Set permSet = Collections.EMPTY_SET;
        if (permission != null) {
            permSet = new HashSet(2);
            permSet.add(permission);
        }
        dataIn.put(ROLE_ACI_LIST_ATTR, permSet);

        Set descSet = Collections.EMPTY_SET;
        if (aciDescription != null) {
            descSet = new HashSet(2);
            descSet.add(aciDescription);
        }
        dataIn.put(ROLE_ACI_DESCRIPTION_ATTR,descSet);
    }

    private Map getCreateRoleMap(Map roleMap, String loc) {
        // set the role managed container dn if its an admin type role
        Set type = (Set)roleMap.get(ROLE_TYPE_ATTR);
        String val = (String)type.iterator().next();
        if (val.equals(AMRole.GENERAL_ADMIN_ROLE+"")) {
            Set x = new HashSet(2);
            x.add(loc);
            roleMap.put(ROLE_CONTAINER_DN, x);
        }
        
        Set tmp = (Set)roleMap.remove(ENTRY_NAME_ATTRIBUTE_NAME);
        String roleName = (String)tmp.iterator().next();
        Map newRole = new HashMap(2);
        newRole.put(roleName, roleMap);
        
        return newRole;
    }
    
    /**
     * Gets the role type (static role or filtered role).
     *   
     * @param dataIn map which contains role data
     * @return role type
     */  
    private int getRoleType(Map dataIn) {
        int roleType = AMObject.ROLE;
        String str = (String)dataIn.remove(ROLE_TYPE);

        if (str != null && str.length() != 0) {
            try {
                roleType = Integer.parseInt(str);
            } catch (NumberFormatException e) {
                debug.warning("RoleModel.getRoleType", e);
            }
        }
        return roleType;
    }
    /**
     * Creates the filter for the filtered role
     *   
     * @param dataIn map of role data
     * @param dataSet map of the data to be added to the role
     */  
    private void createFilter(Map dataIn, Map dataSet) {
        Set los = (Set)dataIn.remove("logicalOp");
	String logicalOp = ((los != null) && !los.isEmpty()) ?
	    (String)los.iterator().next() : STRING_LOGICAL_AND;

        // filterset present when creating filter by hand (advanced view)
        Set filterSet = (Set)dataIn.remove("filterinfo");

        String filterInfo = null;
        if (filterSet != null && !filterSet.isEmpty()) {
            filterInfo  = (String)filterSet.iterator().next();
            if (!filterInfo.startsWith("(") &&
                !filterInfo.endsWith(")")) {
                filterInfo = "(" + filterInfo + ")";
            }
        } else {
            Map filterData = new HashMap(dataIn);
            StringBuffer avPairs = new StringBuffer(100);
            for (Iterator iter = getFilterAttributeNames().iterator(); 
                iter.hasNext(); ) 
            {
                String key = (String)iter.next();
                Set value = (Set)dataIn.remove(key);
                if (value == null || value.isEmpty()) {
                    if (key.equals(USER_SERVICE_UID)) {
                        avPairs.append("(").append(key).append("=*)");
                    }
                    continue;
                }
                Iterator valIter = value.iterator();
                String val = (String)valIter.next();
                if (key.equalsIgnoreCase(USER_SERVICE_ACTIVE_STATUS)) {
                    if (val.equalsIgnoreCase(STRING_ACTIVE)) {
                        avPairs.append("(|(")
                               .append(USER_SERVICE_ACTIVE_STATUS)
                               .append("=active)(!(")
                               .append(USER_SERVICE_ACTIVE_STATUS)
                               .append("=*)))");
                    } else {
                        avPairs.append("(")
                               .append(USER_SERVICE_ACTIVE_STATUS)
                               .append("=")
                               .append(val)
                               .append(")");
                    }
                } else {
                    if (val.length() > 0) {
                        avPairs.append("(")
                            .append(key)
                            .append("=")
                            .append(val)
                            .append(")");
                    } else if (key.equals(USER_SERVICE_UID)) {
                        avPairs.append("(" + USER_SERVICE_UID + "=*)");
                    }

                }
            }
 
            StringBuilder avFilter = new StringBuilder(100);
            
            // add inetorgperson to filter to return only users
            avFilter.append("(&(objectclass=inetorgperson)");
 
            if (avPairs.length() != 0) {
                // add the & or | only if there is more than one avpair
                if (filterData.size() > 1) {
                    if (logicalOp != null &&
                        logicalOp.equalsIgnoreCase(STRING_LOGICAL_AND))
                    {
                        avFilter.append("(&");
                    } else {
                        avFilter.append("(|");
                    }
                    avFilter.append(avPairs).append(")");
                } else {
                    avFilter.append(avPairs);
                }
            }
 
            // close off inetorgperson
            avFilter.append(")");
            filterInfo = avFilter.toString();
        }
 
        if (filterInfo != null && filterInfo.length() > 0) {
            Set attrValue = new HashSet(2);
            attrValue.add(filterInfo);
            String tmp = (String)(dataSet.keySet().iterator().next());
            Map roleMap = (Map)dataSet.get(tmp);
            roleMap.put(NS_ROLE_FILTER, attrValue);
        }
    }    
    
    /**
     * Gets the list of default permission names
     *
     * @return Set of the default permission names
     */
    public Set getDefaultPermissions() {
        if (defaultACIMap == null) {
            createACIMap();
        }
        return defaultACIMap.keySet();
    }

    /**
     * Gets the list of default role types. Currently available types
     * are Administrative and Service
     *
     * @return Set of the default role types
     */
    public Map getDefaultTypes() {
        Map dt = new HashMap(2);
        dt.put(
            getLocalizedString("roleType."+AMRole.GENERAL_ADMIN_ROLE),
            Integer.toString(AMRole.GENERAL_ADMIN_ROLE));

        dt.put(
            getLocalizedString("roleType."+AMRole.USER_ROLE),
            Integer.toString(AMRole.USER_ROLE));

        return dt;
    }

    private void createACIMap() {
        try {
            ServiceSchemaManager mgr = getServiceSchemaManager(
                ADMIN_CONSOLE_SERVICE);
            ServiceSchema schema = mgr.getSchema(SchemaType.GLOBAL);
            Map serviceAttrs = schema.getAttributeDefaults();
            Set ACISet = (Set)serviceAttrs.get(ROLE_DEFAULT_ACI_ATTR);
            Iterator it = ACISet.iterator();
            defaultACIMap = new HashMap(ACISet.size());

            // the list of permissions is of the form
            // permissionName|ACI description|DN:ACI#DN:ACI#...#DN:ACI
            // here we are going to construct a map with permisssion name
            // | { DESCRIPTION, ACI }
            while (it.hasNext()) {
                String entry = (String)it.next();

                int start = 0;
                int end = entry.indexOf("|");
                if (end == -1) {
                    if (debug.warningEnabled()) {
                        debug.warning("invalid default aci entry found: " +
                            entry);
                    }
                    continue;
                }
                String permissionName = entry.substring(start,end);

                start = end+1;
                end = entry.indexOf("|", start);
                if (end == -1) {
                    if (debug.warningEnabled()) {
                        debug.warning("invalid default aci entry found: " +
                            entry);
                    }
                    continue;
                }
                String description = entry.substring(start,end);

                start = end+1;
                end = entry.length();
                String aci = entry.substring(start,end);

                List valSet = new ArrayList(2);
                valSet.add(ACI_DESCRIPTION,description);
                valSet.add(DEFAULT_PERMISSION,aci);

                defaultACIMap.put(permissionName,valSet);
            }
        } catch (SMSException e) {
            debug.error("RoleModelImpl.createACIMap", e);
        } catch (SSOException e) {
            debug.warning("RoleModelImpl.createACIMap", e);
        }
    }

    /**
     * Gets the value for the empty permission
     *
     * @return empty permission value
     */
    public String getEmptyPermission() {
        return "No Permission";
    }

    /**
     * Gets the localized string for the default permissions list
     *
     * @param option value of the permission
     * @return localized string for permission
     */
    public String getOptionString(String option) {
        String original = option;

        // replace spaces with - and get localized string
        option = option.trim().replace(' ','-');
        String localized = getLocalizedString(option);

        if (option.equals(localized)) {
            // return the original value, couldn't find localized value
            localized = original;
            if (debug.warningEnabled()) {
                debug.warning("RoleModelImpl.getOptionString() " +
                    "no localized value for " + option);
            }
        }

        return localized;
    }

    private boolean hasDynamicAttributes(String serviceName) {
        ServiceSchema dynamic = null;
	
	try {
	    dynamic = getServiceSchema(serviceName, SchemaType.DYNAMIC);
        } catch (SMSException e) {
	    debug.warning("RoleModelImpl.hasDynamicAttributes", e);
        } catch (SSOException e) {
	    debug.warning("RoleModelImpl.hasDynamicAttributes", e);
	}

	boolean display = false;
        if (dynamic != null) {
	    Set as = dynamic.getAttributeSchemas();
	    if (as != null && !as.isEmpty()) {
                for (Iterator i = as.iterator(); i.hasNext() && !display; ) {
                    if (isDisplayed((AttributeSchema)i.next())) {
			display = true;
                    }
		}
	    }
        }
        return display; 
    }

    /**
     * Gets the services assigned to the current organization which have
     * dynmamic attributes.
     *   
     * @param location name or current organization location
     * @return a set of organizations
     */  
    public Map getAssignedServices(String location) {
        Map  names = null;
        try {
            AMOrganization org = getAMStoreConnection().getOrganization(
                location);
            Set tmp = org.getRegisteredServiceNames();
            if (tmp != null &&  !tmp.isEmpty()) {
                names = new HashMap(tmp.size() * 2);
                for (Iterator iter = tmp.iterator(); iter.hasNext(); ) {
                    String name = (String)iter.next();

		    // hide authentication configuration service
		    if (name.equals(AUTH_CONFIG_SERVICE)) {
			 continue;
                    }

                    String displayName = getLocalizedServiceName(name);
                    if (!name.equals(displayName)) {
		        if (hasDynamicAttributes(name)) {
                            names.put(name, displayName);
                         }
                     }
                 }
            }
        } catch (SSOException ssoe) {
            debug.warning("OrganizationModel.getOrganizations", ssoe);
        } catch (AMException ame) {
            debug.warning("OrganizationModel.getOrganizations", ame);
        }
        return (names == null) ? Collections.EMPTY_MAP : names;
    }

    /**
     * Returns true if the specified role is a filtered role. 
     *
     * @param roleName the name of the role to test.
     * @return true if the role is a filtered role.
     */
    public boolean isFilteredRole(String roleName) {
	return (getObjectType(roleName) == AMObject.FILTERED_ROLE);
    }

    private static final String CREATE_PROPERTIES = "<section name=\"general\" defaultValue=\"\" > <property required=\"true\"> <label name=\"roleNameLabel\" labelFor=\"entryName\" defaultValue=\"create.role.name.label\" /> <cc name=\"entryName\" tagclass=\"com.sun.web.ui.taglib.html.CCTextFieldTag\" > <attribute name=\"size\" value=\"75\" /> </cc> </property> <property> <label name=\"roleDescriptionLabel\" defaultValue=\"create.role.description.label\" labelFor=\"iplanet-am-role-description\" /> <cc name=\"iplanet-am-role-description\" tagclass=\"com.sun.web.ui.taglib.html.CCTextFieldTag\" > <attribute name=\"size\" value=\"75\" /> </cc> </property> <property required=\"true\"> <label name=\"roleTypeLabel\" defaultValue=\"create.role.type.label\" labelFor=\"iplanet-am-role-type\" /> <cc name=\"iplanet-am-role-type\" tagclass=\"com.sun.web.ui.taglib.html.CCDropDownMenuTag\"> </cc> </property> <property required=\"true\"> <label name=\"rolePermissionLabel\" defaultValue=\"create.role.permission.label\" labelFor=\"iplanet-am-role-aci-list\" /> <cc name=\"iplanet-am-role-aci-list\" tagclass=\"com.sun.web.ui.taglib.html.CCDropDownMenuTag\"> </cc> </property>";
    
    private static final String PROFILE_PROPERTIES = "<property><label name=\"roleDescriptionLabel\" defaultValue=\"create.role.description.label\" labelFor=\"iplanet-am-role-description\" /> <cc name=\"iplanet-am-role-description\" tagclass=\"com.sun.web.ui.taglib.html.CCTextFieldTag\" > <attribute name=\"size\" value=\"75\" /></cc></property>  <property><label name=\"rolePermissionLabel\" defaultValue=\"create.role.permission.label\" labelFor=\"iplanet-am-role-aci-description\" /> <cc name=\"iplanet-am-role-aci-description\" tagclass=\"com.sun.web.ui.taglib.html.CCTextFieldTag\" > <attribute name=\"size\" value=\"75\" /></cc></property>";
    
    private static final String FILTER_ATTRIBUTE = "<property> <label name=\"filterLabel\" labelFor=\"" + NS_ROLE_FILTER + "\" defaultValue=\"role.properties.name.filter\" /> <cc name=\"" + NS_ROLE_FILTER + "\" tagclass=\"com.sun.web.ui.taglib.html.CCTextFieldTag\" > <attribute name=\"size\" value=\"75\" /> </cc> </property>";
        
    private static final String START_SECTION = "<section name=\"general\" defaultValue=\"\" >";
        

}
