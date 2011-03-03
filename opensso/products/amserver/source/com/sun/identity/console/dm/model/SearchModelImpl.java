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
 * $Id: SearchModelImpl.java,v 1.5 2009/01/28 05:34:57 ww203982 Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.console.dm.model;
 
import com.iplanet.am.sdk.AMAssignableDynamicGroup;
import com.iplanet.am.sdk.AMConstants;
import com.iplanet.am.sdk.AMDynamicGroup;
import com.iplanet.am.sdk.AMException;
import com.iplanet.am.sdk.AMFilteredRole;
import com.iplanet.am.sdk.AMGroup;
import com.iplanet.am.sdk.AMGroupContainer;
import com.iplanet.am.sdk.AMObject;
import com.iplanet.am.sdk.AMOrganization;
import com.iplanet.am.sdk.AMOrganizationalUnit;
import com.iplanet.am.sdk.AMPeopleContainer;
import com.iplanet.am.sdk.AMRole;
import com.iplanet.am.sdk.AMSearchControl;
import com.iplanet.am.sdk.AMSearchResults;
import com.iplanet.am.sdk.AMStaticGroup;
import com.iplanet.am.sdk.AMStoreConnection;
import com.iplanet.sso.SSOException;
import com.sun.identity.common.admin.AdminInterfaceUtils;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.property.PropertyTemplate;
import com.sun.identity.console.property.PropertyXMLBuilder;
import com.sun.identity.console.property.PropertyXMLBuilderBase;
import com.sun.identity.sm.AttributeSchema;
import com.sun.identity.sm.SchemaType;
import com.sun.identity.sm.ServiceSchema;
import com.sun.identity.sm.ServiceSchemaManager;
import com.sun.identity.sm.SMSException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import com.sun.identity.shared.ldap.util.DN;

/* - NEED NOT LOG - */
 
public class SearchModelImpl extends DMModelBase
    implements SearchModel
{
    private int searchType = SEARCH;
    private Map resultsMap = null;
    private Map attrValues = null;
    private ServiceSchemaManager userSvcMgr = null;
    private String locationDN = null;
    private String searchLocationDN = null;

    protected String errorMessage = null;
    protected int errorCode = AMSearchResults.SUCCESS;
    
    private String STRING_FILTER = "filter";

    private String NAME_VALUE = "groupName";
    private String SCOPE_VALUE = "searchScope";

    /**
     * Creates a role navigation model implementation object
     * 
     * @param req  HTTP Servlet Request
     * @param map of user information
     */
    public SearchModelImpl(HttpServletRequest req, Map map) {
        super(req, map);
    }

    /**
     * Convert the map from String-Set to a String-String 
     */
    private Map getSearchAttributeValues(Map avMap) {
        Map avPairs = new HashMap(avMap.size()*2);

        for (Iterator iter = avMap.entrySet().iterator(); iter.hasNext();) {
            Map.Entry mapEntry = (Map.Entry)iter.next();
            String key = (String)mapEntry.getKey();
            Set values = (Set)mapEntry.getValue();

            if (values != null && !values.isEmpty()) {
		if (values.size() == 1) {
		    String val  = (String)values.iterator().next();

		    /*
		     * 20050623 Dennis 
		     * Discard empty string and "false" value
		     * (unchecked checkbox is remaining false)
		     */
		    if ((val.length() > 0) && !val.equals("false")) {
			avPairs.put(key, values);
		    }
		} else {
		    avPairs.put(key, values);
		}
	    }
        }

        return avPairs;
    }

    private AMGroupContainer getDefaultGroupContainer(String dn) {
        AMGroupContainer defaultGC = null;
        Set containers = Collections.EMPTY_SET;
        AMStoreConnection sc = getAMStoreConnection();
        String groupContainerDN =
            AdminInterfaceUtils.getNamingAttribute(
                AMObject.GROUP_CONTAINER, debug) +
            "=" + AdminInterfaceUtils.defaultGroupContainerName() + "," + dn;

        try {
            switch (getObjectType(dn)) {
            case AMObject.ORGANIZATION:
                AMOrganization org = sc.getOrganization(dn);
                defaultGC = sc.getGroupContainer(groupContainerDN);
                if ((defaultGC == null) || !defaultGC.isExists()) {
                    containers = org.getGroupContainers(AMConstants.SCOPE_ONE);
                }
                break;
            case AMObject.ORGANIZATIONAL_UNIT:
                AMOrganizationalUnit orgUnit = sc.getOrganizationalUnit(dn);
                defaultGC = sc.getGroupContainer(groupContainerDN);
                if ((defaultGC == null) || !defaultGC.isExists()) {
                    containers =
                        orgUnit.getGroupContainers(AMConstants.SCOPE_ONE);
                }
                break;
            default:
                debug.warning("Unsupported object type for group containers");
            }

            if (!containers.isEmpty()) {
                Iterator iter = containers.iterator();
                if (iter.hasNext()) {
                    defaultGC = sc.getGroupContainer((String)iter.next());
                }
            }
        } catch (AMException ame) {
            debug.warning("UMGroupSearchModel.getDefaultGroupContainer", ame);
        } catch (SSOException ssoe) {
            debug.warning("UMGroupSearchModel.getDefaultGroupContainer", ssoe);
        }

        return defaultGC;
    }

    private AMSearchResults searchGroups(
        AMGroup parent,
        String name,
        Map avMap,
        AMSearchControl searchControl)
        throws AMException, SSOException
    {
        AMSearchResults results = parent.searchGroups(
	    name, avMap, searchControl);

        if (results != null) {
            // Remove the base group DN
            Set groupDNs = results.getSearchResults();
            groupDNs.remove(parent.getDN());            
        }
        return results;
    }

    /**
     * Performs a search for groups which is either of scope
     * <code>AMConstants.SCOPE_SUB</code> or <code>AMConstants.SCOPE_ONE</code>
     *
     * @param location where to search
     * @param avMap map of attribute name to a set of values. These map
                    entries are search criteria.
     * @return The users matching the search criteria
     */
    public Set searchGroups(String location, Map avMap) {
        if (location == null) {
            location = getStartDSDN();
        }

	DN dn = new DN(location);
	locationDN = dn.getParent().toString();

	// pull out the scope of the search 
        int scopeValue = AMConstants.SCOPE_ONE;
        Set tmp = (Set)avMap.remove(SCOPE_VALUE);
        if (tmp != null && !tmp.isEmpty()) {
            try {
                String scope = (String)tmp.iterator().next();
                scopeValue = Integer.parseInt(scope);
            } catch (NumberFormatException nfe) {
                debug.warning("using default value of one for scope level");
            }
        }


	// remove extra parameters that have blank values
        Map avPairs = getSearchAttributeValues(avMap);

	// get the name of the groups to return, the 'wildcard' value 
	String groupName = "*";
	Set name = (Set)avPairs.remove(NAME_VALUE);
	if (name != null && !name.isEmpty()) {
	    groupName = (String)name.iterator().next();
	}

        AMStoreConnection sc = getAMStoreConnection();
        AMSearchResults searchResults = null;
        AMSearchControl searchControl = new AMSearchControl();
        searchControl.setSearchScope(scopeValue);
	setSearchControlAttributes(searchControl, 
            AdminInterfaceUtils.getNamingAttribute(AMObject.GROUP, debug));

        /*
        * when searching for groups in either an organization or an
        * orgnization unit, we need to check the scope first. If the scope
        * is one level, then we need to start the search in the default
        * group container. If the scope is sub tree, then the search
        * needs to start in the organization or organization unit.
        */
        AMGroupContainer gc = null;
        try {
            switch (getObjectType(locationDN)) {
            case AMObject.ORGANIZATION:
                AMOrganization org = sc.getOrganization(locationDN);

                if (scopeValue == AMConstants.SCOPE_ONE) {
                    gc = getDefaultGroupContainer(locationDN);
                }
                if (gc != null) {
                    searchResults =
                        gc.searchGroups(groupName, avPairs, searchControl);
                } else {
                    searchResults =
                        org.searchGroups(groupName, avPairs, searchControl);
                }

                break;
            case AMObject.ORGANIZATIONAL_UNIT:
                AMOrganizationalUnit orgUnit = sc.getOrganizationalUnit(
                    locationDN);
                if (scopeValue == AMConstants.SCOPE_ONE) {
                    gc = getDefaultGroupContainer(locationDN);
                }
                if (gc != null) {
                    searchResults =
                        gc.searchGroups(groupName, avPairs, searchControl);
                } else {
                    searchResults =
                        orgUnit.searchGroups(groupName, avPairs, searchControl);
                }
                break;
            case AMObject.GROUP_CONTAINER:
                gc = sc.getGroupContainer(locationDN);

		if (scopeValue == AMConstants.SCOPE_ONE) {
		    searchResults = gc.searchGroups(
			groupName, avPairs, searchControl);
		} else {
		    AMOrganization o = sc.getOrganization(
			gc.getOrganizationDN());
                    searchResults =
                        o.searchGroups(groupName, avPairs, searchControl);
		}
                break;
            case AMObject.GROUP:
            case AMObject.STATIC_GROUP:
                AMStaticGroup sgroup = sc.getStaticGroup(locationDN);
                searchResults = searchGroups(
		    sgroup, groupName, avPairs, searchControl);
                break;
            case AMObject.DYNAMIC_GROUP:
                AMDynamicGroup dgroup = sc.getDynamicGroup(locationDN);
                searchResults = searchGroups(
		    dgroup, groupName, avPairs, searchControl);
                break;
            case AMObject.ASSIGNABLE_DYNAMIC_GROUP:
                AMAssignableDynamicGroup agroup =
                    sc.getAssignableDynamicGroup(locationDN);
                searchResults = searchGroups(
		    agroup, groupName, avPairs, searchControl);
                break;
            default:
                debug.warning("SearchModel.searchGroups() invalid location "
                    + locationDN);
            }
        } catch (AMException ame) {
            debug.warning("SearchModel.searchGroups()", ame);
            errorMessage = getErrorString(ame);
        } catch (SSOException ssoe) {
            debug.warning("SearchModel.searchGroups()", ssoe);
            errorMessage = getErrorString(ssoe);
        }

        Set groupDNs = Collections.EMPTY_SET;
        if (searchResults != null) {
            groupDNs = searchResults.getSearchResults();
	    purgeResults(groupDNs, location);
            resultsMap = searchResults.getResultAttributes();
            errorMessage = getSearchResultWarningMessage(searchResults);
        }
        return groupDNs;
    }
    
    private void purgeResults(Set entries, String loc) {
	try {
	    AMGroup ag = getAMGroup(loc);
            Set tmp = ag.getUserAndGroupDNs();
	    entries.removeAll(tmp);
	    entries.remove(loc);
	} catch (AMException a) {
	    debug.warning("SearchModel.purgeResults", a);    
	} catch (SSOException s) {
	    debug.warning("SearchModel.purgeResults", s);    
	}
    }

    public String getError() {
        return errorMessage;
    }

    public Map getDataMap() {
        Set attributeNames = getFilterAttributeNames();
        Map data = new HashMap(attributeNames.size() * 2);
        for (Iterator it = attributeNames.iterator(); it.hasNext(); ) {
            data.put((String)it.next(), Collections.EMPTY_SET);
        }
        data.put(ATTR_NAME_LOGICAL_OPERATOR, Collections.EMPTY_SET);

        return data;
    }

    /**
     * Returns a map of attribute names to empty set. The attributes 
     * returned are those that make up the group search page
     */
    public Map getGroupDataMap() {
        Map data = new HashMap();
	try {
	    ServiceSchema groupSchema = getSubSchema(
	        ENTRY_SPECIFIC_SERVICE, SchemaType.GLOBAL, SUB_SCHEMA_GROUP);
            Set attributeNames = groupSchema.getAttributeSchemaNames();

            for (Iterator it = attributeNames.iterator(); it.hasNext(); ) {
		String tmp = (String)it.next();
		if (!tmp.equals("uniquemember")) {
                    data.put(tmp, Collections.EMPTY_SET);
		}
            }
	} catch (SMSException sms) {
	    debug.warning("SearchModel.getGroupDataMap", sms);
	} catch (SSOException sso) {
	    debug.warning("SearchModel.getGroupDataMap", sso);
	}

	data.put(SCOPE_VALUE, Collections.EMPTY_SET);
	data.put(NAME_VALUE, Collections.EMPTY_SET);
	return data;
    }

    /**
     * Returns group search filter XML string.
     *   
     * @return group search filter XML string.
     */  
    public String getGroupSearchXML() {
        String attributes = getPropertyXML(DMConstants.ENTRY_SPECIFIC_SERVICE,
            SUB_SCHEMA_GROUP, SchemaType.GLOBAL, false);

        StringBuilder buff = new StringBuilder(2000);
        buff.append(PropertyXMLBuilderBase.getXMLDefinitionHeader())
            .append(PropertyTemplate.START_TAG)
            .append(START_SECTION)
            .append(SEARCH_SCOPE)
            .append(GROUP_NAME)
            .append(removeSectionTags(attributes))
            .append(PropertyTemplate.SECTION_END_TAG)
            .append(PropertyTemplate.END_TAG);

        return buff.toString();
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
            debug.error("SearchModel.getPropertyXML", e);
        } catch (SMSException e) {
            debug.error("SearchModel.getPropertyXML", e);
        } catch (AMConsoleException e) {
            debug.error("SearchModel.getPropertyXML", e);
        }

        return xml;
    }
    /**
     * Returns sub realm creation property XML.
     *   
     * @return sub realm creation property XML.
     */  
    public String getSearchXML() {
        StringBuilder buff = new StringBuilder(2000);
        buff.append(PropertyXMLBuilderBase.getXMLDefinitionHeader())
            .append(PropertyTemplate.START_TAG)
            .append(START_SECTION)
            .append(getFilterAttributesXML())
            .append(PropertyTemplate.SECTION_END_TAG)
            .append(PropertyTemplate.END_TAG);

        return buff.toString();
    }

    /**
     * Adds entries, either groups or users, to the specified group. 
     * The parentType should be set to SearchModel.GROUP_SEARCH to add
     * groups as members. 
     *   
     * @param parent dn of entry to add the entries.
     * @param members Set of user DNs to be added to the group.
     * @param parentType type of entry being updated.
     */  
    public void addMembers(String parent, Set members, String parentType) 
        throws AMConsoleException
    {
        if (parentType != null && parentType.equals(GROUP_SEARCH)) {
            addGroups(parent, members);
        } else {
            addUsers(parent, members);
        }
    }

    /**
     * Adds groups to the selected group
     *   
     * @param userDNs Set of user DNs to be added to the role or group
     * @return true if the users were successfully added.
     */  
    private void addGroups(String parentDN, Set entries) 
        throws AMConsoleException 
    {
        AMGroup group = getAMGroup(parentDN);

        if (group != null) {
            try {
                group.addNestedGroups(entries);
            } catch (AMException ame) {
                throw new AMConsoleException(getErrorString(ame));
            } catch (SSOException ssoe) {
                throw new AMConsoleException(getErrorString(ssoe));
            }
        }
    }

    /**
     * Adds users to the selected role or group
     *   
     * @param userDNs Set of user DNs to be added to the role or group
     * @return true if the users were successfully added.
     */  
    private void addUsers(String containerDN, Set userDNs) 
        throws AMConsoleException
    {
        if (containerDN == null || containerDN.length() == 0 ||
            userDNs == null || userDNs.isEmpty()) {
        }

        AMStoreConnection sc = getAMStoreConnection();

        try {
            int containerType = getObjectType(containerDN);
            switch (containerType) {
            case AMObject.GROUP:
            case AMObject.STATIC_GROUP:
                AMStaticGroup sGroup = sc.getStaticGroup(containerDN);
                sGroup.addUsers(userDNs);
                break;
            case AMObject.ASSIGNABLE_DYNAMIC_GROUP:
                AMAssignableDynamicGroup aGroup =
                    sc.getAssignableDynamicGroup(containerDN);
                aGroup.addUsers(userDNs);
                break;
            case AMObject.ROLE:
            case AMObject.MANAGED_ROLE:
                AMRole role = sc.getRole(containerDN);
                role.addUsers(userDNs);
                break;
            default:
                debug.warning("SearchModel:addUsers invalid container "
                    + containerDN);
            }
        } catch (AMException ame) {
            debug.warning("SearchModel.addUsers", ame);
            throw new AMConsoleException(getErrorString(ame));
        }  catch (SSOException ssoe) {
            debug.error("SearchModel.addUsers Error in SSO Token", ssoe);
            throw new AMConsoleException(getErrorString(ssoe));
        }
    }

    /**
     * Searches for users with a default search scope of subtree
     *
     * @param logicalOp logical operator
     * @param avPairs attribute value pairs
     * @param location where to search for users
     * @return a set of users which match the search criterias
     */
    public Set searchUsers(String logicalOp, Map avPairs, String location) {
        locationDN = location;
        searchLocationDN = location;
        return searchUsers(logicalOp, AMConstants.SCOPE_SUB + "", avPairs);
    }
 
    /**
     * Searches for users
     *
     * @param logicalOp logical operator
     * @param scope of the search
     * @param avPairs attribute value pairs
     * @return a set of users which match the search criterias
     */
    public Set searchUsers(String logicalOp, String scope, Map avPairs) {
        Set userDNs = Collections.EMPTY_SET;
        
        int scopeValue = AMConstants.SCOPE_ONE;
        try {
            scopeValue = Integer.parseInt(scope);
        } catch (NumberFormatException nfe) {
            if (debug.warningEnabled()) {
                debug.warning("SearchModel.searchUsers " + 
                    "using default value of one for scope level");
            }
        }
        
        AMSearchControl searchControl = new AMSearchControl();
        searchControl.setSearchScope(scopeValue);
        setSearchControlAttributes(
            searchControl, getValidUserAttributes(getSearchReturnValue()));
        setSearchControlLimits(searchControl);
        
        AMSearchResults searchResults = null;
        try {
            if (searchType == MEMBERSHIP) {
                searchResults = searchUsersForMembership(
                    logicalOp, avPairs, searchControl);
            } else { 
                // (searchType == SEARCH) 
                searchResults = searchUsers(
                    logicalOp, avPairs, searchControl);
            }
        } catch (AMException ame) {
            debug.warning("SearchModel.searchUsers", ame);
            errorMessage = getErrorString(ame);
        } catch (SSOException ssoe) {
            debug.warning("SearchModel.searchUsers", ssoe);
            errorMessage = getErrorString(ssoe);
        }
        
        if (searchResults != null) {
            userDNs = searchResults.getSearchResults();
            resultsMap = searchResults.getResultAttributes();
            errorCode = searchResults.getErrorCode();
            errorMessage = getSearchResultWarningMessage(searchResults);
        }
        return userDNs;
    }

    /**
     * @return Number of attributes that will be displayed. For an attribute
     * to be displayed an i18n key is required and any must contain filter.
     * 
     * @deprecated use getNumberOfAttributes
     */
    public int getSize(){
        return getFilterAttributeNames().size();
    }

    /**
     * @return number of attributes that will be displayed. For an attribute
     * to be displayed an i18n key is required and any must contain filter.
     * 
     */
    public int getNumberOfAttributes(){
        return getFilterAttributeNames().size();
    }

    private ServiceSchemaManager getUserServiceSchemaManager() {
	if (userSvcMgr == null) {
	    try {
		userSvcMgr = getServiceSchemaManager(USER_SERVICE);
	    } catch (SSOException ssoe) {
		debug.warning(
		   "SearchModel.getUserServiceSchemaManager", ssoe);
	    } catch (SMSException smse) {
		debug.error(
		   "SearchModel.getUserServiceSchemaManager", smse);
	    }
	}
	return userSvcMgr;
    }

    /**
     * Gets the search results map. Key is the user DN, value is the user 
     * return attribute.
     *
     * @return Map of search results
     */    
    public Map getResultsMap() {
	if (resultsMap == null) {
	    resultsMap = new HashMap(0);
	}
	return resultsMap;
    }

    /**
     * Sets search type.
     *
     * @param type of search
     */
    public void setSearchType(int type) {
        if (type == SEARCH || type == MEMBERSHIP) { 
            searchType = type;
        }
    }

    /**
     * Gets search type.
     *
     * @return search type.
     */
    public int getSearchType() {
        return searchType;
    }

    /** 
     * Determines whether the User service is denied to the user
     * accessing the console.
     *
     * @return true if the User service is denied.
     */
    public boolean isUserServiceDenied() {
	return false;
        //return (isServiceDenied(USER_SERVICE));
    }

    /**
     * Gets the localized message of the service denied dialog.
     *
     * @return the message
     */
    public String getNoServiceAccessMessage() {
        return getLocalizedString("userServiceDenied.message");
    }

    /**
     * Gets the localized message for no search attributes.
     *
     * @return the message
     */
    public String getNoAttributeAccessMessage() {
        return getLocalizedString("noSearchAttributes.message");
    }

    /**
     * Gets no search result message
     *
     * @return no search result message
     */
    public String getNoMatchMsg() {
	return getLocalizedString("noMatchingEntries.message");
    }

  
    /**
     * Checks if time or size limit error occurred.
     *
     * @return true if time or size limit reach
     */
    public boolean isTimeSizeLimit() {
        return errorCode == AMSearchResults.SIZE_LIMIT_EXCEEDED ||
               errorCode == AMSearchResults.TIME_LIMIT_EXCEEDED;
    }


    /**
     * Gets warning title
     *
     * @return warning title
     */
    public String getWarningTitle() {
        String tmp = null;
        if (errorCode == AMSearchResults.SIZE_LIMIT_EXCEEDED) {
            tmp = getLocalizedString("sizeLimitExceeded.title");
        } else if (errorCode == AMSearchResults.TIME_LIMIT_EXCEEDED) {
            tmp = getLocalizedString("timeLimitExceeded.title");
        } else {
            tmp = getLocalizedString("warningMessage.title");
        }

        return tmp;
    }

    /**
     * Search for users to add as members to a role, static group or 
     * assignable dynamic group.  This search is invoked by clicking on
     * the Add button in the User Navigation View Bean. The search begins 
     * at the parent organization of the role or group. Any users who are 
     * currently in the role or group are excluded from the search results 
     * by an LDAP filter.
     */    
    private AMSearchResults searchUsers(
        String logicalOp,
        Map avPairs,
        AMSearchControl searchControl
    ) throws AMException, SSOException {
        AMSearchResults searchResults = null;        
        AMPeopleContainer pc = null;
        int scope = searchControl.getSearchScope();
        AMStoreConnection sc = getAMStoreConnection();

        String filter = createFilter(logicalOp, avPairs);
        setSearchControlLimits(searchControl);

        switch (getObjectType(searchLocationDN)) {
        case AMObject.ORGANIZATION:
            /*
            * check the scope of the search. if its a one level search 
            * try and get the default people container for this org and 
            * start the search from there.
            */
            if (scope == AMConstants.SCOPE_ONE) {
                pc = getDefaultPeopleContainer();
            }
            if (pc != null) {
                searchResults = pc.searchUsers(searchControl, filter);
            } else {
                AMOrganization org = sc.getOrganization(searchLocationDN);
                searchResults = org.searchUsers(searchControl, filter);
            }	  

            break;
        case AMObject.ORGANIZATIONAL_UNIT:
            /*
            * check the scope of the search. if its a one level search 
            * try and get the default people container for this org and 
            * start the search from there.
            */
            if (scope == AMConstants.SCOPE_ONE) {
                pc = getDefaultPeopleContainer();
            }
            if (pc != null) {
                searchResults = pc.searchUsers(searchControl, filter);
            } else {
                AMOrganizationalUnit orgUnit =
                    sc.getOrganizationalUnit(searchLocationDN);
                searchResults = orgUnit.searchUsers(searchControl, filter);
            }
            break;
        case AMObject.PEOPLE_CONTAINER:
	    pc = sc.getPeopleContainer(searchLocationDN);
            searchResults = pc.searchUsers(searchControl, filter);
            break;
        case AMObject.ROLE:
        case AMObject.MANAGED_ROLE:
            AMRole role = sc.getRole(searchLocationDN);
            searchResults = role.searchUsers(searchControl, filter);
            break;
        case AMObject.FILTERED_ROLE:
            AMFilteredRole filteredRole = sc.getFilteredRole(searchLocationDN);
            searchResults = filteredRole.searchUsers(searchControl, filter);
            break;
        case AMObject.GROUP:
        case AMObject.STATIC_GROUP:
            AMStaticGroup sgroup = sc.getStaticGroup(searchLocationDN);
            searchResults = sgroup.searchUsers(searchControl, filter);
            break;
        case AMObject.ASSIGNABLE_DYNAMIC_GROUP:
            AMAssignableDynamicGroup agroup =
                sc.getAssignableDynamicGroup(searchLocationDN);
            searchResults = agroup.searchUsers(searchControl, filter);
            break;
        case AMObject.DYNAMIC_GROUP:
            AMDynamicGroup dgroup = sc.getDynamicGroup(searchLocationDN);
            searchResults = dgroup.searchUsers(searchControl, filter);
            break;
        default:
            debug.warning("SearchModel.searchUsers invalid location " +
                searchLocationDN);
        }
        return searchResults;
    }

    /**
     * Searches for users to add as members to a role, static group or
     * assignable dynamic group.  The search begins at the parent
     * organization of the role or group. Any users who are currently in the
     * role or group are excluded from the search results by an LDAP filter.
     */
    private AMSearchResults searchUsersForMembership(
        String logicalOp,
        Map avPairs,
        AMSearchControl searchControl
    ) throws AMException, SSOException {
        String avFilter = createFilter(logicalOp, avPairs);
        AMStoreConnection sc = getAMStoreConnection();

        AMObject parent = null;
        String filter = null;
 
        switch (getObjectType(searchLocationDN)) {
        case AMObject.ROLE:
        case AMObject.MANAGED_ROLE:
            // search for users and exclude the role members
            filter = "(&" + avFilter +
                "(!(nsroledn=" + searchLocationDN + ")))";
            AMRole role = sc.getRole(searchLocationDN);
            parent = getParentOrganization(role);
            break;
        case AMObject.GROUP:
        case AMObject.STATIC_GROUP:
            // search for users and exclude the static group members
            filter = "(&" + avFilter +
                "(!(iplanet-am-static-group-dn=" + searchLocationDN + ")))";
            AMStaticGroup sgroup = sc.getStaticGroup(searchLocationDN);
            parent = getParentOrganization(sgroup);
            break;
        case AMObject.ASSIGNABLE_DYNAMIC_GROUP:
            // search for users and exclude the assignable group members
            filter = "(&" + avFilter +
                "(!(memberof=" + searchLocationDN + ")))";
            AMAssignableDynamicGroup agroup =
                sc.getAssignableDynamicGroup(searchLocationDN);
            parent = getParentOrganization(agroup);
            break;
        default:
            debug.warning(
                "SearchModel.searchUsersForMembership invalid location " +
                searchLocationDN);
        }

        AMSearchResults searchResults = null;        
        if (parent != null) {
            if (parent instanceof AMOrganization) {
                searchResults = ((AMOrganization)parent).searchUsers(
                    searchControl, filter);
            } else {
                searchResults = ((AMOrganizationalUnit)parent).searchUsers(
                    searchControl, filter);
            }
        }
        return searchResults;
    }

    private String createFilter(String logicalOp, Map avPairs) {
        Iterator iter = avPairs.entrySet().iterator();

        StringBuffer avBuf = new StringBuffer(100);
        while (iter.hasNext()) {
            Map.Entry mapEntry = (Map.Entry)iter.next();
            String key = (String)mapEntry.getKey();
            Set value = (Set)mapEntry.getValue();
            if (value == null || value.isEmpty()) {
                if (key.equals(USER_SERVICE_UID)) {
                    avBuf.append("(").append(key).append("=*)");
                }
                continue;
            }

            boolean multipleValues = (value.size() > 1);
	    // open section for multi-valued attribute 
            if (multipleValues) {
		avBuf.append("(|");
            }
            for (Iterator valIter = value.iterator(); valIter.hasNext();) {
                String val = (String)valIter.next();
                if (key.equalsIgnoreCase(USER_SERVICE_ACTIVE_STATUS)) {
                    if (val.equalsIgnoreCase(STRING_ACTIVE)) {
                        /*
                        * Absence of inetuserstatus attribute
                        * implies the user is Active. Create a filter
                        * with the Presence operator
                        */
                        avBuf.append("(|(")
			     .append(USER_SERVICE_ACTIVE_STATUS)
			     .append("=active)(!(")
			     .append(USER_SERVICE_ACTIVE_STATUS)
			     .append("=*)))");
                    } else {
                        avBuf.append("(")
			     .append(USER_SERVICE_ACTIVE_STATUS)
			     .append("=")
			     .append(val)
			     .append(")");
                    }   
                } else {
                    if (val.length() > 0) {
                        avBuf.append("(")
                            .append(key)
                            .append("=")
                            .append(val)
                            .append(")");
                    } else if (key.equals(USER_SERVICE_UID)) {
                        avBuf.append('(').append(USER_SERVICE_UID).append("=*)");
                    }
                }
	    }
	    // close off multi-valued attribute section
            if (multipleValues) {
		avBuf.append(")");
            }
        }

        StringBuilder avFilter = new StringBuilder(100);
        if (avBuf.length() != 0) {
            // add the & or | only if there is more than one av pair
            if (avPairs.size() > 1) {
                if ((logicalOp != null) &&
                    logicalOp.equalsIgnoreCase(STRING_LOGICAL_AND)) {
                    avFilter.append("(&");
                } else {
                    avFilter.append("(|");
                }
                avFilter.append(avBuf).append(")");
            } else {
                avFilter.append(avBuf);
            }
        }

        return avFilter.toString();
    }

    private AMObject getParentOrganization(AMObject obj)
        throws AMException, SSOException {
        String dn = obj.getOrganizationDN();
        int parentType = getObjectType(dn);
        if (parentType == AMObject.ORGANIZATION) {
            return getAMStoreConnection().getOrganization(dn);
        } else {
            return getAMStoreConnection().getOrganizationalUnit(dn);
        }
    }
                                        
    private AMPeopleContainer getDefaultPeopleContainer() {
	AMPeopleContainer defaultPC = null;
	Set containers = Collections.EMPTY_SET;
        AMStoreConnection sc = getAMStoreConnection();
	
	try {
            String peopleContainerAttribute = 
                AdminInterfaceUtils.getNamingAttribute(
		    AMObject.PEOPLE_CONTAINER, debug);

	    switch (locationType) {
	    case AMObject.ORGANIZATION:
		AMOrganization org = sc.getOrganization(locationDN);
		defaultPC = sc.getPeopleContainer(
		    peopleContainerAttribute + "=" +
		    AdminInterfaceUtils.defaultPeopleContainerName() + "," +
		    org.getDN());

		if (defaultPC == null || !defaultPC.isExists()) {
		    containers = org.getPeopleContainers(AMConstants.SCOPE_ONE);
		}
		break;
	    case AMObject.ORGANIZATIONAL_UNIT:
		AMOrganizationalUnit orgUnit =
		    sc.getOrganizationalUnit(locationDN);
		defaultPC = sc.getPeopleContainer(
		    peopleContainerAttribute + "=" +
		    AdminInterfaceUtils.defaultPeopleContainerName() + "," +
		    orgUnit.getDN());

		if (defaultPC == null || !defaultPC.isExists()) {
		    containers = orgUnit.getPeopleContainers(
			AMConstants.SCOPE_ONE);
		}
		break;
	    default:
		debug.warning("Unsupported object type for people containers");
	    }
	    
	    if (!containers.isEmpty()) {
		Iterator iter = containers.iterator();
		if (iter.hasNext()) {
		    defaultPC = sc.getPeopleContainer((String)iter.next());
		}
	    }
	} catch (AMException ame) {
	    debug.warning("SearchModel.getDefaultPeopleContainer", ame);
	} catch (SSOException ssoe) {
	    debug.warning("SearchModel.getDefaultPeopleContainer", ssoe);
	}

	return defaultPC;
      }

    /**
     * Returns the value for the specified attribute.
     *
     * @param key attribute name.
     * @return values for the attribute.
     */
    public Set getAttrValue(String key) {
	return (attrValues != null) ? (Set)attrValues.get(key) : null;
    }

    /**
     * Store the map of attribute name and values in the model.
     *
     * @param valueMap of attribute name to values.
     */
    public void setAttrValues(Map valueMap) {
        if (attrValues == null) {
            attrValues = new HashMap(valueMap);
        } else {
            attrValues.putAll(valueMap);
        }
    }

    private static final String START_SECTION = "<section name=\"general\" defaultValue=\"\" >";

    private static final String SEARCH_SCOPE = "<property><label name=\"searchScopeLabel\" defaultValue=\"search.scope.label\" labelFor=\"searchScope\" /> <cc name=\"searchScope\" tagclass=\"com.sun.web.ui.taglib.html.CCRadioButtonTag\" > <attribute name=\"layout\" value=\"vertical\" /> <option label=\"search.scope.current\" value=\""+AMConstants.SCOPE_ONE+"\" /> <option label=\"search.scope.sub\" value=\""+AMConstants.SCOPE_SUB+"\" /> </cc></property>";

    private static final String GROUP_NAME = "<property><label name=\"groupNameLabel\" defaultValue=\"label.name\" labelFor=\"groupName\" /> <cc name=\"groupName\" tagclass=\"com.sun.web.ui.taglib.html.CCTextFieldTag\" /> </property>";
}
