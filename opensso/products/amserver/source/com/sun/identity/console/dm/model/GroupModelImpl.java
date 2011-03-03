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
 * $Id: GroupModelImpl.java,v 1.5 2008/10/02 16:31:27 veiming Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.console.dm.model;

import com.sun.identity.common.DisplayUtils;
import com.sun.identity.common.admin.AdminInterfaceUtils;
import com.sun.identity.console.base.model.AMAdminUtils;
import com.sun.identity.console.base.model.AMAdminConstants;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMFormatUtils;
import com.sun.identity.console.delegation.model.DelegationConfig;
import com.sun.identity.console.property.PropertyTemplate;
import com.sun.identity.console.property.PropertyXMLBuilder;
import com.sun.identity.console.property.PropertyXMLBuilderBase;
import com.iplanet.am.sdk.AMAssignableDynamicGroup;
import com.iplanet.am.sdk.AMConstants;
import com.iplanet.am.sdk.AMException;
import com.iplanet.am.sdk.AMGroup;
import com.iplanet.am.sdk.AMDynamicGroup;
import com.iplanet.am.sdk.AMGroupContainer;
import com.iplanet.am.sdk.AMObject;
import com.iplanet.am.sdk.AMOrganization;
import com.iplanet.am.sdk.AMOrganizationalUnit;
import com.iplanet.am.sdk.AMSearchControl;
import com.iplanet.am.sdk.AMStaticGroup;
import com.iplanet.am.sdk.AMSearchResults;
import com.iplanet.am.sdk.AMStoreConnection;
import com.iplanet.sso.SSOException;
import com.sun.identity.shared.datastruct.OrderedSet;
import com.sun.identity.sm.AttributeSchema;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.SchemaType;
import com.sun.identity.sm.ServiceSchema;
import com.sun.identity.sm.ServiceSchemaManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;

/* - LOG COMPLETE - */

/**
 * This model is used by <code>GroupViewBean.java</code>
 */
public class GroupModelImpl extends DMModelBase
    implements GroupModel 
{
    private Set groups = null;
    private ServiceSchemaManager entrySpecificSvcMgr = null;    
    private String errorMessage = ""; 
    
    private AMGroup amGroup = null;
    /**
     * Creates a managed group navigation model implementation object.
     *
     * @param req  HTTP Servlet Request
     * @param map of user information
     */
    public GroupModelImpl(HttpServletRequest req, Map map) {
	super(req, map);
    }    
        
    /**
     * Set the managed groups in model
     * This method is called when the Search for the groups is completed in the 
     * <code>dataframe</code>. The search viewbean in the <code>dataframe</code>
     * sets the results into the group navigation viewbeans model in order to
     * display the results in the Navigation frame.
     *
     * @param groupDNs Set of group DNs
     */       
    public void setManagedGroups(Set groupDNs) {
	if (groupDNs == null || groupDNs.isEmpty()) {
	    groups = Collections.EMPTY_SET;
        } else {
	    groups = groupDNs;
	}
    }

    /**
     * Gets a set of Managed Groups
     *
     * @param filter wildcards
     * @return a set of Managed Groups
     */
    public Set getGroups(String location, String filter) {
	if (groups != null) {
	    return groups;
	}
	locationType = getObjectType(location);
	locationDN = location;

	AMSearchResults results = null;
	AMGroup group = null;
	Set groupContainers = null;
	Iterator gcIter = null;
	    
	AMSearchControl searchControl = new AMSearchControl();
	searchControl.setSearchScope(AMConstants.SCOPE_SUB);
        setSearchControlLimits(searchControl);
        setSearchControlAttributes(
            locationDN, 
            SUB_SCHEMA_GROUP,
            AMObject.GROUP, 
            searchControl, 
            GROUPS);
	boolean bOrganization = false;
	boolean bContainer = false;
	boolean bDynGroup = false;
	boolean bAssDynGroup = false;
        AMStoreConnection sc = getAMStoreConnection();

	try {
	    String[] params = {locationDN, filter};

	    switch (locationType) {
	    case AMObject.ORGANIZATION:
		bOrganization = true;
		logEvent("ATTEMPT_DIR_MGR_SEARCH_GROUPS_UNDER_ORG", params);
		AMOrganization org = sc.getOrganization(locationDN);
		results = org.searchGroups(filter, null, searchControl);
		logEvent("SUCCEED_DIR_MGR_SEARCH_GROUPS_UNDER_ORG", params);
		break;
	    case AMObject.ORGANIZATIONAL_UNIT:
		bContainer = true;
		logEvent("ATTEMPT_DIR_MGR_SEARCH_GROUPS_UNDER_CONTAINER",
		    params);
		AMOrganizationalUnit orgUnit =
		    sc.getOrganizationalUnit(locationDN);
		results = orgUnit.searchGroups(filter, null, searchControl);
		logEvent("SUCCEED_DIR_MGR_SEARCH_GROUPS_UNDER_CONTAINER",
		    params);
		break;
	    case AMObject.GROUP_CONTAINER:
		AMGroupContainer gc = sc.getGroupContainer(locationDN);
		results = gc.searchGroups(filter, null, searchControl);
		break;
	    case AMObject.GROUP:
	    case AMObject.STATIC_GROUP:
		logEvent("ATTEMPT_DIR_MGR_SEARCH_GROUPS_UNDER_STATIC_GRP",
		    params);
		group = sc.getStaticGroup(locationDN);
		results = group.searchGroups(filter, null, searchControl);
		logEvent("SUCCEED_DIR_MGR_SEARCH_GROUPS_UNDER_STATIC_GRP",
		    params);
		break;
	    case AMObject.DYNAMIC_GROUP:
		bDynGroup = true;
		logEvent("ATTEMPT_DIR_MGR_SEARCH_GROUPS_UNDER_DYNAMIC_GRP",
		    params);
		group = sc.getDynamicGroup(locationDN);
		results = group.searchGroups(filter, null, searchControl);
		logEvent("SUCCEED_DIR_MGR_SEARCH_GROUPS_UNDER_DYNAMIC_GRP",
		    params);
		break;
	    case AMObject.ASSIGNABLE_DYNAMIC_GROUP:
		bAssDynGroup = true;
		logEvent(
		"ATTEMPT_DIR_MGR_SEARCH_GROUPS_UNDER_ASSIGNABLE_DYNAMIC_GRP",
		    params);
		group = sc.getAssignableDynamicGroup(locationDN);
		results = group.searchGroups(filter, null, searchControl);
		logEvent(
		"SUCCEED_DIR_MGR_SEARCH_GROUPS_UNDER_ASSIGNABLE_DYNAMIC_GRP",
		    params);
		break;
	    default:
		if (debug.warningEnabled()) {
		    debug.warning("GroupModelImpl.getManagedGroup"
			+ " invalid location " + locationType);
		}
	    }
	} catch (SSOException e) {
	    String strError = getErrorString(e);
	    String[] paramsEx = {locationDN, filter, strError};
	    String msgId = null;
	    if (bOrganization) {
		msgId = "SSO_EXCEPTION_DIR_MGR_SEARCH_GROUPS_UNDER_ORG";
	    } else if (bContainer) {
		msgId = "SSO_EXCEPTION_DIR_MGR_SEARCH_GROUPS_UNDER_CONTAINER";
	    } else if (bDynGroup) {
		msgId = "SSO_EXCEPTION_DIR_MGR_SEARCH_GROUPS_UNDER_DYNAMIC_GRP";
	    } else if (bAssDynGroup) {
    msgId = "SSO_EXCEPTION_DIR_MGR_SEARCH_GROUPS_UNDER_ASSIGNABLE_DYNAMIC_GRP";
	    } else {
		msgId = "SSO_EXCEPTION_DIR_MGR_SEARCH_GROUPS_UNDER_STATIC_GRP";
	    }

	    logEvent(msgId, paramsEx);
	    debug.warning("GroupModelImpl.getManagedGroup", e);
	} catch (AMException e) {
	    searchErrorMsg = getErrorString(e);
	    String[] paramsEx = {locationDN, filter, searchErrorMsg};
	    String msgId = null;
	    if (bOrganization) {
		msgId = "AM_EXCEPTION_DIR_MGR_SEARCH_GROUPS_UNDER_ORG";
	    } else if (bContainer) {
		msgId = "AM_EXCEPTION_DIR_MGR_SEARCH_GROUPS_UNDER_CONTAINER";
	    } else if (bDynGroup) {
		msgId = "AM_EXCEPTION_DIR_MGR_SEARCH_GROUPS_UNDER_DYNAMIC_GRP";
	    } else if (bAssDynGroup) {
     msgId = "AM_EXCEPTION_DIR_MGR_SEARCH_GROUPS_UNDER_ASSIGNABLE_DYNAMIC_GRP";
	    } else {
		msgId = "AM_EXCEPTION_DIR_MGR_SEARCH_GROUPS_UNDER_STATIC_GRP";
	    }

	    logEvent(msgId, paramsEx);
	    debug.warning("GroupModelImpl.getManagedGroup", e);
	}

	if (results != null) {
	    groups = removeHiddenGroups(setSearchResults(results));
	} else {
	    groups = Collections.EMPTY_SET;
	}
	return groups;
    }    
    
    private Set removeHiddenGroups(Set compliantGroupDNs) {
	if (isAdminGroupsEnabled() ||
	    (locationType != AMObject.ORGANIZATION &&
	     locationType != AMObject.ORGANIZATIONAL_UNIT &&
	     locationType != AMObject.GROUP_CONTAINER)) {
	    return compliantGroupDNs;
	}
	    
        Set groupDNs = new OrderedSet();
	String groupDN   = null;
	String groupName = null;
	
	for (Iterator iter = compliantGroupDNs.iterator(); iter.hasNext(); ) {
	    groupDN = (String)iter.next();
	    groupName = AMFormatUtils.DNToName(this, groupDN);
	    if (groupName.equalsIgnoreCase(DOMAIN_ADMINS) ||
		groupName.equalsIgnoreCase(DOMAIN_HELP_DESK_ADMINS) ||
		groupName.equalsIgnoreCase(SERVICE_ADMINS) ||
		groupName.equalsIgnoreCase(SERVICE_HELP_DESK_ADMINS)) {
		continue;
	    }
	    groupDNs.add(groupDN);
	}
	return groupDNs;
    }    
  
    /**
     * Gets the group list stored in the model
     *
     * @return the group list stored in the model
     */
    public Set getAttrList() {
        return groups;
    }

    /**
     * Sets attribute list for group
     *
     * @param set  data for group
     */
    public void setAttrList(Set set) {
        setManagedGroups(set);
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
	case AMObject.GROUP_CONTAINER:
	case AMObject.GROUP:
	case AMObject.STATIC_GROUP:
	case AMObject.DYNAMIC_GROUP:
	case AMObject.ASSIGNABLE_DYNAMIC_GROUP:
	    valid = true;
	    break;
	default:
	    if (debug.warningEnabled()) {
		debug.warning(
		    "GroupModelImpl.isCurrentLocationTypeValid: " +
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
        return getSearchReturnAttributes("Group", AMObject.GROUP, GROUPS);
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
        String type)
    {
        List searchAttrs = Collections.EMPTY_LIST;
        if (returnAttr != null && returnAttr.length() > 0) {
            List list = getObjectDisplayList(returnAttr, type);
            if (list != null && !list.isEmpty()) {
                searchAttrs = new ArrayList(list.size());
                Set groupAttrs = 
                    getObjectAttributeNames(schemaName, objectType);
                Set filteredGroupAttrs = getFilteredGroupAttributeNames();

                int validAttrSize = 0;
                if (groupAttrs != null && !groupAttrs.isEmpty()) {
                    validAttrSize = groupAttrs.size();
                }
                if (filteredGroupAttrs != null && 
                    !filteredGroupAttrs.isEmpty()) 
                {
                    validAttrSize += filteredGroupAttrs.size();
                }
                if (validAttrSize > 0) {
                    Set set = new HashSet(validAttrSize);
                    set.addAll(groupAttrs);
                    set.addAll(filteredGroupAttrs);

                    
                    for (Iterator iter = list.iterator(); iter.hasNext(); ) {
                        String str = (String)iter.next();
                        if (set.contains(str) && !searchAttrs.contains(str)) {
                            searchAttrs.add(str);
                        }
                    }                }
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
     * Returns value of the specified attribute for the given group.
     *
     * @param groupDN given DN of the group.
     * @param attribute name.
     * @return value of the of the attribute from the search results.
     */
    public String getAttributeValue(String groupDN, String attribute) {
        String value = "";

        if (resultsMap != null && !resultsMap.isEmpty()) {
            Map values = (Map)resultsMap.get(groupDN);
            if (values != null && !values.isEmpty()) {
                Set attrValues = (Set)values.get(attribute);
                if (attrValues == null || attrValues.isEmpty()) {
                    if (getObjectType(groupDN) == AMObject.DYNAMIC_GROUP) {
                        if (getFilteredGroupAttributeNames().contains(
			    attribute))
		        {
                            attrValues = 
			        getFilteredGroupAttributeValues(groupDN);
                            value = getMultiValue(attrValues);
                        }
                    }
                } else {
                    value = getMultiValue(attrValues);
                }
            }
        }
        return value;
    }

    /**
     * Returns localized name of attribute name.
     *
     * @param name of attribute.
     * @return localized name of attribute name.
     */
    public String getAttributeLocalizedName(String name) {
        return getAttributeLocalizedName(name, SUB_SCHEMA_GROUP);
    }


    /**
     * Returns a set of filtered group's attribute names.
     *
     * @return a set of filtered group's attribute names.
     */
    private Set getFilteredGroupAttributeNames() {
        ServiceSchemaManager mgr = null;
        try {
	    mgr = getServiceSchemaManager(ENTRY_SPECIFIC_SERVICE);
        } catch (SSOException e) {
            debug.error("GroupModelImpl.getFilteredGroupAttributeNames", e);
        } catch (SMSException e) {
            debug.error("GroupModelImpl.getFilteredGroupAttributeNames", e);
        }

        Set attrSchemaSet = getAttributesToDisplay(
            mgr, SchemaType.GLOBAL, SUB_SCHEMA_FILTERED_GROUP);

        Set set = Collections.EMPTY_SET;
        if (attrSchemaSet != null && !attrSchemaSet.isEmpty()) {
            
            set = new HashSet(attrSchemaSet.size());
            for (Iterator iter = attrSchemaSet.iterator(); iter.hasNext(); ) {
                AttributeSchema attrSchema = (AttributeSchema)iter.next();
                String name = attrSchema.getName();
                set.add(name);
            }
        }
        return set;
    }

    /**
     * Returns the <code>AMDynamicGroup</code> object for a given group dn.
     *
     * @param dn group dn.
     * @return the <code>AMDynamicGroup</code> object for a given group dn.
     */
    private AMDynamicGroup getFilteredGroupObject(String dn) {
        AMDynamicGroup group = null;
        try {
            group = getAMStoreConnection().getDynamicGroup(dn);
            if (group == null || !group.isExists()) {
                if (debug.warningEnabled()) {
                    debug.warning("UMRoleNavModelImpl.getFilteredGroupObject "
                        + "group does not exists " + dn);
                }
                group = null;
            }
        } catch (SSOException ssoe) {
            debug.warning(
                "UMRoleNavModelImpl.getFilteredGroupObject", ssoe);
        }

        return group;
    }

    /**
     * Returns the filtered group values for a given group dn.
     *
     * @param dn group dn.
     * @return the filtered group values for a given group dn.
     */
    private Set getFilteredGroupAttributeValues(String dn) {
        ServiceSchemaManager mgr = null;
	try {
	    mgr = getServiceSchemaManager(ENTRY_SPECIFIC_SERVICE);
        } catch (SSOException e) {
	    debug.error("GroupModelImpl.getFilteredGroupAttributeValues " + dn,
		e);
        } catch (SMSException e) {
	    debug.error("GroupModelImpl.getFilteredGroupAttributeValues " + dn,
		e);
	}

        AMDynamicGroup group = getFilteredGroupObject(dn);


        Set values = Collections.EMPTY_SET;
        if (mgr != null && group != null) {
            Set attrSchemaSet = getAttributesToDisplay(
                mgr, SchemaType.GLOBAL, SUB_SCHEMA_FILTERED_GROUP);

            if ((attrSchemaSet != null) && !attrSchemaSet.isEmpty()) {
                for (Iterator iter = attrSchemaSet.iterator(); iter.hasNext();)
                {
                    AttributeSchema attrSchema = (AttributeSchema)iter.next();
                    String name = attrSchema.getName();

                    try {
                        if (name.equals(FILTERED_GROUP_FILTERINFO)) {
                            values = new HashSet(1);
                            values.add(group.getFilter());
                        } else {
                            values = group.getAttribute(name);
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
     * Update attribute values. 
     *
     * @param name Name of the group.
     * @param values Map of attribute values to set.
     * @throws AMConsoleException if values cannot be set.
     */
    public void updateGroup(String name, Map values) 
	throws AMConsoleException 
    {
	AMGroup group = getAMGroup(name);
        try {
            modify(group, values);
        } catch (AMException a) {
	    debug.warning("GroupModelImpl.updateGroups ", a);
        } catch (SSOException a) {
	    debug.error("GroupModelImpl.updateGroups ", a);
        }
    }

    /**
     * Returns attribute values. Map of attribute name to set of values.
     *
     * @param name Name of group.
     * @throws AMConsoleException if values cannot be retrieved.
     * @return attribute values.
     */
    public Map getValues(String name) throws AMConsoleException {
        Map values = null;
	Exception ex = null;
        AMGroup group = getAMGroup(name);
        try {
	    values = group.getAttributes();
	    if (getObjectType(name) == AMObject.DYNAMIC_GROUP) {
                Set hs = new HashSet(2);
                hs.add(((AMDynamicGroup)group).getFilter());
	        values.put("filterinfo", hs);
	    }
        } catch (AMException s) {
	    debug.warning("GroupModel.getValues ", s);
	    ex = s;
	} catch (SSOException s) {
	    debug.error("getting values for group", s);
	    ex = s;
	}
        if (ex != null) {
	    throw new AMConsoleException(getErrorString(ex));
	}
        return (values == null) ? Collections.EMPTY_MAP : values;
    }

    /**
     * Returns XML for creating a static group.
     *
     * @return XML for creating a static group.
     */
    public String getCreateStaticGroupXML() {
        // get static group properties from the group schema
        String attributes = getPropertyXML(DMConstants.ENTRY_SPECIFIC_SERVICE,
            SUB_SCHEMA_GROUP, SchemaType.GLOBAL, false);

        StringBuilder buff = new StringBuilder(2000);
        buff.append(PropertyXMLBuilderBase.getXMLDefinitionHeader())
            .append(PropertyTemplate.START_TAG)
            .append(CREATE_PROPERTIES)
            .append(removeSectionTags(attributes))
            .append(PropertyTemplate.SECTION_END_TAG)
            .append(PropertyTemplate.END_TAG);

        return buff.toString();
    }

    /**  
     * Returns XML for creating a dynamic group.
     *   
     * @return XML for creating a dynamic group.
     */  
    public String getCreateDynamicGroupXML() {

        StringBuilder buff = new StringBuilder(2000);
        buff.append(PropertyXMLBuilderBase.getXMLDefinitionHeader())
            .append(PropertyTemplate.START_TAG)
            .append(CREATE_PROPERTIES)
            .append(getFilterAttributesXML())
            .append(PropertyTemplate.SECTION_END_TAG)
            .append(PropertyTemplate.END_TAG);

        return buff.toString();
    }                           
 
    public String getGroupProfileXML(String viewbeanClassName, int type) {
	boolean canModify = false;

	try {
	    AMOrganization org = getAMStoreConnection().getOrganization(
                locationDN);
	    DelegationConfig dConfig = DelegationConfig.getInstance();
	    canModify = dConfig.hasPermission(org.getDN(), null,
		AMAdminConstants.PERMISSION_MODIFY, this, viewbeanClassName);
	} catch (SSOException e) {
	    debug.warning("GroupModelImpl.getGroupProfileXML", e);
	}

	String schema = SUB_SCHEMA_GROUP;
	if (type == AMObject.DYNAMIC_GROUP) {
	    schema = SUB_SCHEMA_FILTERED_GROUP;
	}
        StringBuilder buff = new StringBuilder(2000);
        buff.append(PropertyXMLBuilderBase.getXMLDefinitionHeader())
            .append(PropertyTemplate.START_TAG);
        buff.append(getPropertyXML(DMConstants.ENTRY_SPECIFIC_SERVICE,
            schema, SchemaType.GLOBAL, !canModify));
        buff.append(PropertyTemplate.END_TAG);
	return buff.toString();
    }

    /**
     * Returns Map of attribute name to empty set of values.
     *
     * @throws AMConsoleException if map cannot be obtained.
     */
    public Map getDataMap(String name) {
        Map map = new HashMap();
        try {
            int type = getObjectType(name);
            String schema = SUB_SCHEMA_GROUP;
            if (type == AMObject.DYNAMIC_GROUP) {
                schema = SUB_SCHEMA_FILTERED_GROUP;
                map.put("filterinfo", Collections.EMPTY_SET);
            }

            ServiceSchema sub = getSubSchema(
                DMConstants.ENTRY_SPECIFIC_SERVICE,
                SchemaType.GLOBAL, 
                schema);

            // add the attributes defined in amEntrySpecific
            Set attrSchemas = sub.getAttributeSchemas();
            for (Iterator iter = attrSchemas.iterator(); iter.hasNext(); ) {
                AttributeSchema as = (AttributeSchema)iter.next();
                map.put(as.getName().toLowerCase(), Collections.EMPTY_SET);
            }
            for (Iterator i = getFilterAttributeNames().iterator(); 
		i.hasNext(); ) 
	    {
                map.put((String)i.next(), Collections.EMPTY_SET);
            }
 
	    map.put(ENTRY_NAME_ATTRIBUTE_NAME, Collections.EMPTY_SET);

        } catch (SMSException e) {
            debug.warning("GroupModelImpl.getDataMap", e);
        } catch (SSOException e) {
            debug.error("GroupModelImpl.getDataMap", e);
        }

        return map;
    }

    /**
     * This will prepend the default group container to the location dn.
     * It only does this for orgs or orgunits and only if the group container
     * display is not enabled. If the default group container doesn't exist
     * it will then search for any other containers and select the first one
     * if any are found. If after all this there are no group containers for 
     * the object, the original location will be returned.
     */ 
    private String appendGroupContainer(String loc) {
        int type = getObjectType(loc);
        if (type == AMObject.ORGANIZATION || 
            type == AMObject.ORGANIZATIONAL_UNIT) 
        { 
            if (showGroupContainers() == false) {
                StringBuilder tmp = new StringBuilder(128);
                tmp.append(AdminInterfaceUtils.getNamingAttribute(
                        AMObject.GROUP_CONTAINER, debug))
                    .append("=")
                    .append(AdminInterfaceUtils.defaultGroupContainerName()) 
                    .append(",") 
                    .append(loc);  
                loc = tmp.toString();
            }
        }
	return loc;
    } 

    /**
     * Gets set of group containers where the group object should be created.
     *   
     * @return set of group containers where the group object should be created.
     */  
    private String getGroupContainers(String loc) {
        Set gcDNs = Collections.EMPTY_SET;
	boolean bOrganization = false;
        try {  
	    String[] params = {loc, "*"};
            switch (getObjectType(loc)) {
            case AMObject.ORGANIZATION:
		bOrganization = true;
		logEvent("ATTEMPT_DIR_MGR_SEARCH_GROUP_CONTAINERS_UNDER_ORG",
		    params);
                AMOrganization org =getAMStoreConnection().getOrganization(loc);
                gcDNs = org.getGroupContainers(AMConstants.SCOPE_ONE);
		logEvent("SUCCEED_DIR_MGR_SEARCH_GROUP_CONTAINERS_UNDER_ORG",
		    params);
                break;
            case AMObject.ORGANIZATIONAL_UNIT:
		logEvent(
		    "ATTEMPT_DIR_MGR_SEARCH_GROUP_CONTAINERS_UNDER_CONTAINER",
		    params);
                AMOrganizationalUnit orgUnit =
                    getAMStoreConnection().getOrganizationalUnit(loc);
                gcDNs = orgUnit.getGroupContainers(AMConstants.SCOPE_ONE);
		logEvent(
		    "SUCCEED_DIR_MGR_SEARCH_GROUP_CONTAINERS_UNDER_CONTAINER",
		    params);
                break;
            }
        } catch (AMException e) {
	    String strError = getErrorString(e);
	    String[] paramsEx = {loc, "*", strError};
	    String msgId = (bOrganization) ?
		"AM_EXCEPTION_DIR_MGR_SEARCH_GROUP_CONTAINERS_UNDER_ORG":
		"AM_EXCEPTION_DIR_MGR_SEARCH_GROUP_CONTAINERS_UNDER_CONTAINER";
	    logEvent(msgId, paramsEx);
            debug.warning("failed to get group containers", e);
        } catch (SSOException e) {
	    String strError = getErrorString(e);
	    String[] paramsEx = {loc, "*", strError};
	    String msgId = (bOrganization) ?
		"SSO_EXCEPTION_DIR_MGR_SEARCH_GROUP_CONTAINERS_UNDER_ORG":
		"SSO_EXCEPTION_DIR_MGR_SEARCH_GROUP_CONTAINERS_UNDER_CONTAINER";
	    logEvent(msgId, paramsEx);
            debug.warning("failed to get group containers", e);
        }      

        return (gcDNs == null) ? "" : (String)gcDNs.iterator().next();
    }

    /**
     * Creates a group in the container below the current organization or
     * organization unit. A reload of the navigation frame will be performed
     * if the group is successfully created.
     *
     * @param location where to create the given group
     * @param dataIn map which contains the group name and the optional and
     *               required attributes.
     * @return true if the group was created, false otherwise
     */
    public boolean createGroup(String location, Map dataIn) 
	throws AMConsoleException
    {
        boolean created = false;
        Exception e = null;
	Set tmp = (Set)dataIn.get(ENTRY_NAME_ATTRIBUTE_NAME);
	String groupName = (String)tmp.iterator().next();

        // add the default groupContainer to the current location IF its
        // an organization or orgunit
        location = appendGroupContainer(location);
	boolean bOrganization = false;
	boolean bGrpContainer = false;
	boolean bContainer = false;
	boolean bDynamic = false;
	boolean bAssDynamic = false;

        // If there is no group container, create the 
	// group under the parent.
        try {
            switch (getObjectType(location)) {
            case AMObject.GROUP_CONTAINER:
		bGrpContainer = true;
                AMGroupContainer gc = getAMStoreConnection().getGroupContainer(
                    location);
                created = createGroup(gc, dataIn);
                break;
            case AMObject.ORGANIZATION:
		bOrganization = true;
                AMOrganization org = (AMOrganization)getAMObject(location);
                created = createGroup(org, dataIn);
                break;
            case AMObject.ORGANIZATIONAL_UNIT :
		bContainer = true;
                AMOrganizationalUnit unit = (AMOrganizationalUnit)getAMObject(
                    location);
                created = createGroup(unit, dataIn);
                break;
            case AMObject.GROUP:
            case AMObject.STATIC_GROUP:
                AMStaticGroup sgroup = (AMStaticGroup)getAMObject(location);
                created = createSubGroup(sgroup, dataIn);
                break;
            case AMObject.DYNAMIC_GROUP:
		bDynamic = true;
                AMDynamicGroup dgroup = (AMDynamicGroup)getAMObject(location);
		created = createSubGroup(dgroup, dataIn);
                break;
            case AMObject.ASSIGNABLE_DYNAMIC_GROUP:
		bAssDynamic = true;
                AMAssignableDynamicGroup agroup = (AMAssignableDynamicGroup)
                    getAMObject(location);
                created = createSubGroup(agroup, dataIn);
                break;
            }
        } catch (AMException ame) {
            debug.warning("create of group failed", ame);
            e = ame;
	    String[] paramsEx = {location, groupName, getErrorString(e)};
	    String msgId = null;
	    if (bOrganization) {
		msgId = "AM_EXCEPTION_DIR_MGR_CREATE_GROUP_UNDER_ORG";
	    } else if (bContainer) {
		msgId = "AM_EXCEPTION_DIR_MGR_CREATE_GROUP_UNDER_CONTAINER";
	    } else if (bGrpContainer) {
		msgId = "AM_EXCEPTION_DIR_MGR_CREATE_GROUP_UNDER_GRP_CONTAINER";
	    } else if (bDynamic) {
		msgId = "AM_EXCEPTION_DIR_MGR_CREATE_GROUP_UNDER_DYNAMIC_GRP";
	    } else if (bAssDynamic) {
		msgId =
		    "AM_EXCEPTION_DIR_MGR_CREATE_GROUP_UNDER_ASSIGN_DYN_GRP";
	    } else {
		msgId = "AM_EXCEPTION_DIR_MGR_CREATE_GROUP_UNDER_STATIC_GRP";
	    }
	    logEvent(msgId, paramsEx);
        } catch (SSOException soe) {
            debug.warning("create of group failed", soe);
            e = soe;
	    String[] paramsEx = {location, groupName, getErrorString(e)};
	    String msgId = null;
	    if (bOrganization) {
		msgId = "SSO_EXCEPTION_DIR_MGR_CREATE_GROUP_UNDER_ORG";
	    } else if (bContainer) {
		msgId = "SSO_EXCEPTION_DIR_MGR_CREATE_GROUP_UNDER_CONTAINER";
	    } else if (bGrpContainer) {
		msgId = 
		    "SSO_EXCEPTION_DIR_MGR_CREATE_GROUP_UNDER_GRP_CONTAINER";
	    } else if (bDynamic) {
		msgId = "SSO_EXCEPTION_DIR_MGR_CREATE_GROUP_UNDER_DYNAMIC_GRP";
	    } else if (bAssDynamic) {
		msgId =
		    "SSO_EXCEPTION_DIR_MGR_CREATE_GROUP_UNDER_ASSIGN_DYN_GRP";
	    } else {
		msgId = "SSO_EXCEPTION_DIR_MGR_CREATE_GROUP_UNDER_STATIC_GRP";
	    }
	    logEvent(msgId, paramsEx);
        } catch (AMConsoleException ace) {
            debug.warning("create of group failed", ace);
            e = ace;
	    String[] paramsEx = {location, groupName, getErrorString(e)};
	    String msgId = null;
	    if (bOrganization) {
		msgId = "AM_EXCEPTION_DIR_MGR_CREATE_GROUP_UNDER_ORG";
	    } else if (bContainer) {
		msgId = "AM_EXCEPTION_DIR_MGR_CREATE_GROUP_UNDER_CONTAINER";
	    } else if (bGrpContainer) {
		msgId = "AM_EXCEPTION_DIR_MGR_CREATE_GROUP_UNDER_GRP_CONTAINER";
	    } else if (bDynamic) {
		msgId = "AM_EXCEPTION_DIR_MGR_CREATE_GROUP_UNDER_DYNAMIC_GRP";
	    } else if (bAssDynamic) {
		msgId =
		    "AM_EXCEPTION_DIR_MGR_CREATE_GROUP_UNDER_ASSIGN_DYN_GRP";
	    } else {
		msgId = "AM_EXCEPTION_DIR_MGR_CREATE_GROUP_UNDER_STATIC_GRP";
	    }
	    logEvent(msgId, paramsEx);
        }

	if (e != null) {
	    throw new AMConsoleException(getErrorString(e));
        }

        return created;
    }

    private void modify(AMGroup group, Map dataIn)
        throws AMException, SSOException, AMConsoleException
    {
	validateRequiredAttributes(dataIn);

	String[] param = {group.getDN()};
	logEvent("ATTEMPT_DIR_MGR_MODIFY_GROUP", param);

        try {
            // if this is a dynamic group first try to set the filter
            Set filter = (Set)dataIn.remove("filterinfo");
            if (filter != null && !filter.isEmpty()) {
                String t = (String)filter.iterator().next();
                ((AMDynamicGroup)group).setFilter(t);
            }
            // now set the remaining attributes
            group.setAttributes(dataIn);
            group.store();
	    logEvent("SUCCEED_DIR_MGR_MODIFY_GROUP", param);
        } catch (AMException amc) {
	    String strError = getErrorString(amc);
	    String[] paramsEx = {group.getDN(), strError};
	    logEvent("AM_EXCEPTION_DIR_MGR_MODIFY_GROUP", paramsEx);
            debug.warning("GroupModelImpl.modify",amc);
        }
    }

    private boolean createGroup(AMGroupContainer parent, Map dataIn) 
	throws AMException, SSOException, AMConsoleException
    {
	boolean created = false;
	Set ns = null;
        int groupType = getGroupType(dataIn);

        Set tmp = (Set)dataIn.remove(ENTRY_NAME_ATTRIBUTE_NAME);
        String groupName = (String)tmp.iterator().next();
        Set sGroupNames = new HashSet(2);
        sGroupNames.add(groupName);
	String[] params = {parent.getDN(), groupName};
	logEvent("ATTEMPT_DIR_MGR_CREATE_GROUP_UNDER_GRP_CONTAINER", params);

        switch (groupType) {
        case AMObject.GROUP:
        case AMObject.STATIC_GROUP:
	    try {
		ns = parent.createStaticGroups(sGroupNames);

		if ((ns != null) && !ns.isEmpty()) {
		    AMStaticGroup group = (AMStaticGroup)ns.iterator().next();
		    modify(group, dataIn);
		    created = true;
		} else {
		    errorMessage = getLocalizedString(
			"createGroupFailed.message");
		}
	    } catch (IllegalArgumentException iae) {
		errorMessage = getErrorString(iae);
	    }

	    break;
        case AMObject.DYNAMIC_GROUP:
	    Map dynamicGroup = new HashMap(1);
	    dataIn.putAll(createMemberURL(parent.getDN(),dataIn));
	    dynamicGroup.put(groupName, dataIn);

	    try {
		ns = parent.createDynamicGroups(dynamicGroup);

		if ((ns != null) && !ns.isEmpty()) {
		    created = true;
		} else {
		    errorMessage = getLocalizedString(
			"createGroupFailed.message");
		}
	    } catch (IllegalArgumentException iae) {
		errorMessage = getErrorString(iae);
	    }

	    break;
        case AMObject.ASSIGNABLE_DYNAMIC_GROUP:
	    try {
		ns = parent.createAssignableDynamicGroups(sGroupNames);

		if ((ns != null) && !ns.isEmpty()) {
		    AMAssignableDynamicGroup adGroup =
			(AMAssignableDynamicGroup)ns.iterator().next();
		    modify(adGroup, dataIn);
		    created = true;
		} else {
		    errorMessage = getLocalizedString(
			"createGroupFailed.message");
		}
	    } catch (IllegalArgumentException iae) {
		errorMessage = getErrorString(iae);
	    }
	    break;
        }

	if (created) {
	    logEvent("SUCCEED_DIR_MGR_CREATE_GROUP_UNDER_GRP_CONTAINER",
		params);
	} else {
	    String[] paramsEx = {parent.getDN(), groupName, ""};
	    logEvent("AM_EXCEPTION_DIR_MGR_CREATE_GROUP_UNDER_GRP_CONTAINER",
		paramsEx);
	}

	return created;
    }


    private boolean createSubGroup(AMGroup parent, Map dataIn)
        throws AMException, SSOException, AMConsoleException
    {
        Set ns = null;
        int groupType = getGroupType(dataIn);
	Set tmp = (Set)dataIn.remove(ENTRY_NAME_ATTRIBUTE_NAME);
        String groupName = (String)tmp.iterator().next();
        Map groupMap = new HashMap(2);

	boolean bDynamic = false;
	boolean bAssDynamic = false;
	String[] params = {parent.getDN(), groupName};

        switch (groupType) {
        case AMObject.GROUP:
        case AMObject.STATIC_GROUP:
	    logEvent("ATTEMPT_DIR_MGR_CREATE_GROUP_UNDER_STATIC_GRP", params);
	    // remove unique members and add after the group is created
            Set sgMemberDNs =
                (Set)dataIn.remove(AMConstants.UNIQUE_MEMBER_ATTRIBUTE);
            groupMap.put(groupName, dataIn);
            ns = parent.createStaticGroups(groupMap);
            if (sgMemberDNs != null && !sgMemberDNs.isEmpty()) {
                Iterator iter = ns.iterator();
                if (iter.hasNext()) {
                    AMStaticGroup sgroup = (AMStaticGroup)iter.next();
		    // TBD LOG ADD USERS TO GROUP
                    sgroup.addUsers(sgMemberDNs);
                }
            }
            break;
        case AMObject.DYNAMIC_GROUP:
	    bDynamic = true;
	    logEvent("ATTEMPT_DIR_MGR_CREATE_GROUP_UNDER_DYNAMIC_GRP",
		params);
            dataIn.putAll(createMemberURL(parent.getDN(), dataIn));
            groupMap.put(groupName,dataIn);
            ns = parent.createDynamicGroups(groupMap);
            break;
        case AMObject.ASSIGNABLE_DYNAMIC_GROUP:
	    bAssDynamic = true;
	    logEvent("ATTEMPT_DIR_MGR_CREATE_GROUP_UNDER_ASSIGN_DYN_GRP",
		params);
            Set agMemberDNs =
                (Set)dataIn.remove(AMConstants.UNIQUE_MEMBER_ATTRIBUTE);
            groupMap.put(groupName, dataIn);
            ns = parent.createAssignableDynamicGroups(groupMap);
            if (agMemberDNs != null && !agMemberDNs.isEmpty()) {
                Iterator iter = ns.iterator();
                if (iter.hasNext()) {
                    AMAssignableDynamicGroup agroup =
                        (AMAssignableDynamicGroup)iter.next();
                    agroup.addUsers(agMemberDNs);
                }
            }
            break;
        default:
            debug.warning("GroupModelImpl.createSubGroup: invalid group " +
                groupType);
        }
        if (ns == null || ns.isEmpty()) {
	    String[] paramsEx = {parent.getDN(), groupName, ""};
	    String msgId = null;
	    if (bDynamic) {
		msgId = "AM_EXCEPTION_DIR_MGR_CREATE_GROUP_UNDER_DYNAMIC_GRP";
	    } else if (bAssDynamic) {
		msgId =
		    "AM_EXCEPTION_DIR_MGR_CREATE_GROUP_UNDER_ASSIGN_DYN_GRP";
	    } else {
		msgId = "SUCCEED_DIR_MGR_CREATE_GROUP_UNDER_STATIC_GRP";
	    }
	    logEvent(msgId, paramsEx);
            return false;
        }

	String msgId = null;
	if (bDynamic) {
	    msgId = "SUCCEED_DIR_MGR_CREATE_GROUP_UNDER_DYNAMIC_GRP";
	} else if (bAssDynamic) {
	    msgId = "SUCCEED_DIR_MGR_CREATE_GROUP_UNDER_ASSIGN_DYN_GRP";
	} else {
	    msgId = "SUCCEED_DIR_MGR_CREATE_GROUP_UNDER_STATIC_GRP";
	}
	logEvent(msgId, params);

        return true;
    }
 
    // create a group in an organization
    private boolean createGroup(AMOrganization parent, Map dataIn)
        throws AMException, SSOException, AMConsoleException
    {
        Set ns = null;
        int groupType = getGroupType(dataIn);
        Set tmp = (Set)dataIn.remove(ENTRY_NAME_ATTRIBUTE_NAME);
        String groupName = (String)tmp.iterator().next(); 
        
        Set sGroupNames = new HashSet(1);
        sGroupNames.add(groupName);

        // for static groups, modify the group after creation since the SDK
        // doesn't allow a static group to be initialized during creation.

	boolean bDynamic = false;
	boolean bAssDynamic = false;
	String[] params = {parent.getDN(), groupName};
	logEvent("ATTEMPT_DIR_MGR_CREATE_GROUP_UNDER_ORG", params);

        switch (groupType) {
        case AMObject.GROUP:
        case AMObject.STATIC_GROUP:
            ns = parent.createStaticGroups(sGroupNames);
            Iterator sgiter = ns.iterator();
            if (sgiter.hasNext()) {
                AMStaticGroup group = (AMStaticGroup)sgiter.next();
                modify(group, dataIn);
            }
            break;
        case AMObject.DYNAMIC_GROUP:
            //add the filter(memberurl)
            dataIn.putAll(createMemberURL(parent.getDN(),dataIn));

            Map dynamicGroup = new HashMap(1);
            dynamicGroup.put(groupName, dataIn);

            ns = parent.createDynamicGroups(dynamicGroup);
            // TBD LOG GROUP CREATE
            break;
        case AMObject.ASSIGNABLE_DYNAMIC_GROUP:
            ns = parent.createAssignableDynamicGroups(sGroupNames);
            // TBD LOG GROUP CREATE
            Iterator adIter = ns.iterator();
            if (adIter.hasNext()) {
                AMAssignableDynamicGroup adGroup =
                   (AMAssignableDynamicGroup)adIter.next();
                modify(adGroup, dataIn);
            }
            break;
        default:
            debug.warning(
                "GroupModelImpl.createGroup: unknown group type specified");
        }

        if (ns == null || ns.isEmpty()) {
	    String[] paramsEx = {parent.getDN(), groupName, ""};
            logEvent("AM_EXCEPTION_DIR_MGR_CREATE_GROUP_UNDER_ORG",
		paramsEx);
            return false;
        }

	logEvent("SUCCEED_DIR_MGR_CREATE_GROUP_UNDER_ORG", params);
        return true;
    }

    // create a group in an organizational unit
    private boolean createGroup(AMOrganizationalUnit parent, Map dataIn)
        throws AMException, SSOException, AMConsoleException
    {
        Set ns = null;
	Set tmp = (Set)dataIn.remove(ENTRY_NAME_ATTRIBUTE_NAME);
        String groupName = (String)tmp.iterator().next();
        Set sGroupNames = new HashSet(2);
        sGroupNames.add(groupName);

	String[] params = {parent.getDN(), groupName};
	logEvent("ATTEMPT_DIR_MGR_CREATE_GROUP_UNDER_CONTAINER", params);

        int groupType = getGroupType(dataIn);
        switch (groupType) {
        case AMObject.GROUP:
        case AMObject.STATIC_GROUP:
            ns = parent.createStaticGroups(sGroupNames);
            // Modify the group after creation since the SDK
            // doesn't allow a static group to be initialized
            // during creation.
            Iterator sgiter = ns.iterator();
            if (sgiter.hasNext()) {
                AMStaticGroup group = (AMStaticGroup)sgiter.next();
                modify(group, dataIn);
            }
            break;
        case AMObject.DYNAMIC_GROUP:

            //add the filter(memberurl)
            dataIn.putAll(createMemberURL(parent.getDN(),dataIn));

            Map dynamicGroup = new HashMap(1);
            dynamicGroup.put(groupName, dataIn);

            ns = parent.createDynamicGroups(dynamicGroup);
            break;
        case AMObject.ASSIGNABLE_DYNAMIC_GROUP:
            ns = parent.createAssignableDynamicGroups(sGroupNames);
            Iterator adIter = ns.iterator();

            // Modify the group after creation since the SDK doesn't allow an
            // assignable dynamic group to be initialized during creation

            if (adIter.hasNext()) {
                AMAssignableDynamicGroup adGroup =
                    (AMAssignableDynamicGroup)adIter.next();
                modify(adGroup, dataIn);
            }
            break;
        }

        if (ns == null || ns.isEmpty()) {
	    String[] paramsEx = {parent.getDN(), groupName, ""};
	    logEvent("AM_EXCEPTION_DIR_MGR_CREATE_GROUP_UNDER_CONTAINER",
		paramsEx);
            return false;
        }

	logEvent("SUCCEED_DIR_MGR_CREATE_GROUP_UNDER_CONTAINER", params);
        return true;
    }

    private int getGroupType(Map dataIn) {
        int groupType = getGroupConfiguration();
        String str = (String)dataIn.remove(GROUP_TYPE);

        if (str != null && str.length() != 0) {
            try {
                groupType = Integer.parseInt(str);
            } catch (NumberFormatException e) {
                if (debug.warningEnabled()) {
                    debug.warning("getGroupType defaulting to " + groupType, e);
                }
            }
        }
        return groupType;
    }

    private Map createMemberURL(String parentDN, Map dataIn) {
        String filterInfo = null;
        /*
        * check for the logical operator, if its present then the user is 
        * using the basic page so no filterinfo field
        */
        if (dataIn.containsKey(ATTR_NAME_LOGICAL_OPERATOR)) {
            filterInfo = createFilter(dataIn);
        } else {
            Set tmp = (Set)dataIn.remove("filterinfo");
            if (tmp == null || tmp.isEmpty()) {
                /*
                * user pressed create without specifying a filter.
                * create a default filter for all users
                */
                filterInfo = AdminInterfaceUtils.getNamingAttribute(
		    AMObject.GROUP, debug) + "=*";
            } else {
                filterInfo = (String)tmp.iterator().next();
            }
        }
        
        Set attrValue = new HashSet(1);
        attrValue.add("ldap:///" + parentDN +"??sub?" + filterInfo);
        Map attrMap = new HashMap(1);
        attrMap.put("memberurl", attrValue);
        return attrMap;
    }

    private String createFilter(Map dataIn) {
	Set set = (Set)dataIn.remove(ATTR_NAME_LOGICAL_OPERATOR);
        String logicalOp = (String)set.iterator().next();

        Map filterData = new HashMap(dataIn);
        
        StringBuffer avPairs = new StringBuffer(100);
        for (Iterator iter = filterData.keySet().iterator(); iter.hasNext();) {
            String key = (String)iter.next();
            Set value = (Set)dataIn.remove(key);
            if (value == null || value.isEmpty()) {
                if (key.equals(USER_SERVICE_UID)) {
                    avPairs.append("(" + key + "=*)");
                }
                continue;
            }
            Iterator valIter = value.iterator();
            String val = (String)valIter.next();
            if (key.equalsIgnoreCase(USER_SERVICE_ACTIVE_STATUS)) {
                if (val.equalsIgnoreCase(STRING_ACTIVE)) {
		    // Absence of inetuserstatus attributeimplies the user is
		    // Active. Create a filter with the Presence operator
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
 
        StringBuffer avFilter = new StringBuffer(100);

        // add inetorgperson to filter to return only users
        avFilter.append("(&(objectclass=inetorgperson)");

        if (avPairs.length() != 0) {
            // add the & or | only if there is more than one avpair
            if (filterData.size() > 1) {
                if ((logicalOp != null)
		    && logicalOp.equalsIgnoreCase(STRING_LOGICAL_AND))
		{
                    avFilter.append("(&");
                } else {
                    avFilter.append("(|");
                }
                avFilter.append(avPairs + ")");
            } else {
                avFilter.append(avPairs);
            }
        }

        // close off inetorgperson 
        avFilter.append(")");
        
        return avFilter.toString();
    }

    public String getManagedGroupType() {
        return Integer.toString(getGroupConfiguration());
    }

    /**
     * Returns a <code>Set</code> user entries that belong to this group.
     * Only entries that match the pattern will be returned.
     *
     * @return Set of user entries from the group.
     */
    private Set searchUsersInGroup(String pattern)
        throws AMException, SSOException
    {
        AMSearchControl searchControl = new AMSearchControl();
        searchControl.setSearchScope(AMConstants.SCOPE_ONE);
        setSearchControlLimits(searchControl);
        setSearchControlAttributes(searchControl, 
             getValidUserAttributes(getSearchReturnValue()));
        
        String[] params = {amGroup.getDN(), pattern};
        logEvent("ATTEMPT_DIR_MGR_SEARCH_USERS_IN_GROUP", params);
        
        AMSearchResults searchResults = amGroup.searchUsers(
            searchControl, createUserSearchFilter(pattern));
        
        logEvent("SUCCEED_DIR_MGR_SEARCH_USERS_IN_GROUP", params);
        return setSearchResults(searchResults);
    } 
    
    /**
     * Returns <code>Set</code> of DN's (entries) which are the members of a 
     * group. The members can be a mixture of groups and user entries.
     *
     * @param pattern Pattern used to limit the number of users returned.
     * @param groupDN DN of group to get the members.
     * @throws AMConsoleException
     */
    public Set getMembers(String pattern, String groupDN) 
        throws AMConsoleException 
    {
        Set results = Collections.EMPTY_SET;
	amGroup = getAMGroup(groupDN);
    
        if (amGroup != null) {
	    boolean usersSearched = false;
            results = new HashSet();

            try {
                results = searchUsersInGroup(pattern);
		usersSearched = true;

		String[] params = {groupDN};
		logEvent("ATTEMPT_DIR_MGR_GET_NESTED_GROUPS", params);
                Set set = amGroup.getNestedGroupDNs();
		logEvent("SUCCEED_DIR_MGR_GET_NESTED_GROUPS", params);

                if ((set != null) && !set.isEmpty()) {
                    for (Iterator iter = set.iterator(); iter.hasNext();) {
                        String dn = (String)iter.next();
                        // use our own wildcard match as sdk doesn't
                        // provide pattern matching on group names
                        String name = AMFormatUtils.DNToName(this, dn);
                        if (DisplayUtils.wildcardMatch(name, pattern)) {
                            results.add(dn);
                        }
                    }    
                }
            } catch (SSOException ssoe) {
		String msgId = (!usersSearched) ?
		    "SSO_EXCEPTION_DIR_MGR_SEARCH_USERS_IN_GROUP" :
		    "SSO_EXCEPTION_DIR_MGR_GET_NESTED_GROUPS";
		String strError = getErrorString(ssoe);
		String[] paramsEx = {groupDN, pattern, strError};
		logEvent(msgId, paramsEx);
                debug.warning("GroupModelImpl.getMembers", ssoe);
            } catch (AMException ame) {
		String msgId = (!usersSearched) ?
		    "AM_EXCEPTION_DIR_MGR_SEARCH_USERS_IN_GROUP" :
		    "AM_EXCEPTION_DIR_MGR_GET_NESTED_GROUPS";
		String strError = getErrorString(ame);
		String[] paramsEx = {groupDN, pattern, strError};
		logEvent(msgId, paramsEx);
                debug.warning("GroupModelImpl.getMembers", ame);
                throw new AMConsoleException(strError);
            }
        }
 
        return results;
    }
                 
    protected String getPropertyXML(
        String serviceName,
        String subSchemaName,
        SchemaType type,
	boolean readonly
    ) {
        String xml = "";
        try {
            ServiceSchema sub = getSubSchema(serviceName, type, subSchemaName);
            if (sub != null) {
                Set attributes = sub.getAttributeSchemas();

	        if (subSchemaName.equals(SUB_SCHEMA_GROUP)) {
	            for (Iterator it = attributes.iterator(); it.hasNext(); ) {
                        AttributeSchema as = (AttributeSchema)it.next();
		        if (as.getName().equalsIgnoreCase("uniquemember")) {
		            it.remove();
		            break;
		        }
	            }
	        }
                PropertyXMLBuilder xmlBuilder = new PropertyXMLBuilder(
                    sub, this, attributes);

		if (readonly) {
		    xmlBuilder.setAllAttributeReadOnly(true);
		}

                xml = xmlBuilder.getXML(false);
	    }
        } catch (SSOException e) {
            debug.error("GroupModelImpl.getPropertyXML", e);
        } catch (SMSException e) {
            debug.error("GroupModelImpl.getPropertyXML", e);
        } catch (AMConsoleException e) {
            debug.error("GroupModelImpl.getPropertyXML", e);
        }

        return xml;
    }

    /**
     * Removes member entries from the group. Entries can be either groups
     * or users.
     *
     * @param location Name of the group.
     * @param setDNs Set of names to remove from the group.
     * @throws AMConsoleException if members cannot be removed.
     */
    public void removeMembers(String location, Set setDNs)
        throws AMConsoleException
    {
        if ((setDNs != null) && !setDNs.isEmpty()) {
	    String DNs = AMAdminUtils.getString(setDNs, ",", false);
	    String[] params = {location, DNs};
	    logEvent("ATTEMPT_DIR_MGR_REMOVE_USERS_FROM_GROUP", params);

	    // separate out the group and user entries
            Set userDNs = new HashSet(setDNs.size()*2);
	    Set groupDNs = new HashSet(setDNs.size()*2);
	    for (Iterator i=setDNs.iterator(); i.hasNext();) {
		String dn = (String)i.next();
		if (isUser(dn)) {
		    userDNs.add(dn);
		} else {
		    groupDNs.add(dn);
		}
	    }

            try {
		// remove the groups
	        AMGroup group = getAMGroup(location);
		if (!groupDNs.isEmpty()) {
                    group.removeNestedGroups(groupDNs);
		}

                // remove the users
		if (!userDNs.isEmpty()) {
                    switch (getObjectType(location)) {
                    //switch (locationType) {
                    case AMObject.GROUP:
                    case AMObject.STATIC_GROUP:
                        ((AMStaticGroup)group).removeUsers(userDNs);
                        break;
                    case AMObject.ASSIGNABLE_DYNAMIC_GROUP:
                        ((AMAssignableDynamicGroup)group).removeUsers(userDNs);
                        break;
                    }
                }
		logEvent("SUCCEED_DIR_MGR_REMOVE_USERS_FROM_GROUP", params);
            } catch (SSOException e) {
		String strError = getErrorString(e);
		String[] paramsEx = {location, DNs, strError};
		logEvent("SSO_EXCEPTION_DIR_MGR_REMOVE_USERS_FROM_GROUP",
		    paramsEx);
                debug.warning("GroupModel.removeMembers", e);
                throw new AMConsoleException(strError);
            } catch (AMException e) {
		String strError = getErrorString(e);
		String[] paramsEx = {location, DNs, strError};
		logEvent("AM_EXCEPTION_DIR_MGR_REMOVE_USERS_FROM_GROUP",
		    paramsEx);
                debug.warning("GroupModel.removeMembers", e);
                throw new AMConsoleException(strError);
            }
        }
    }

    /**
     * Returns true if the specified dn is a dynamic group.
     *
     * @param dn of object to test.
     * @return true is dn is a dynamic group.
     */
    public boolean isDynamicGroup(String dn) {
	return (getObjectType(dn) == AMObject.DYNAMIC_GROUP);
    }

    public boolean isUser(String dn) {
	return (getObjectType(dn) == AMObject.USER);
    }

    private static final String CREATE_PROPERTIES = "<section name=\"general\" defaultValue=\"\"><property required=\"true\"><label name=\"lblPcName\" defaultValue=\"label.name\" labelFor=\""+ENTRY_NAME_ATTRIBUTE_NAME+"\" /><cc name=\""+ENTRY_NAME_ATTRIBUTE_NAME+"\" tagclass=\"com.sun.web.ui.taglib.html.CCTextFieldTag\" /></property>";
}
