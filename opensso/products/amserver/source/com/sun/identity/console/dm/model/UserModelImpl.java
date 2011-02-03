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
 * $Id: UserModelImpl.java,v 1.7 2009/01/28 05:34:57 ww203982 Exp $
 *
 */

package com.sun.identity.console.dm.model;

import com.iplanet.sso.SSOException;
import com.iplanet.am.sdk.AMAssignableDynamicGroup;
import com.iplanet.am.sdk.AMConstants;
import com.iplanet.am.sdk.AMException;
import com.iplanet.am.sdk.AMGroup;
import com.iplanet.am.sdk.AMObject;
import com.iplanet.am.sdk.AMOrganization;
import com.iplanet.am.sdk.AMOrganizationalUnit;
import com.iplanet.am.sdk.AMPeopleContainer;
import com.iplanet.am.sdk.AMSearchControl;
import com.iplanet.am.sdk.AMSearchResults;
import com.iplanet.am.sdk.AMStaticGroup;
import com.iplanet.am.sdk.AMStoreConnection;
import com.iplanet.am.sdk.AMTemplate;
import com.iplanet.am.sdk.AMUser;

import com.sun.identity.common.admin.AdminInterfaceUtils;
import com.sun.identity.console.base.model.AMAdminConstants;
import com.sun.identity.console.base.model.AMAdminUtils;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMFormatUtils;
import com.sun.identity.console.base.model.AMSystemConfig;

import com.sun.identity.console.delegation.model.DelegationConfig;
import com.sun.identity.console.property.PropertyTemplate;
import com.sun.identity.console.property.PropertyXMLBuilder;
import com.sun.identity.console.property.PropertyXMLBuilderBase;
import com.sun.identity.sm.AttributeSchema;
import com.sun.identity.sm.SchemaType;
import com.sun.identity.sm.ServiceSchema;
import com.sun.identity.sm.ServiceSchemaManager;
import com.sun.identity.sm.SMSException;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;				
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import javax.servlet.http.HttpServletRequest;

/*
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.List;

        */

import com.sun.identity.shared.ldap.util.DN;
import com.sun.identity.shared.ldap.util.RDN;

/**
 * This model is used by organization related views.
 */
public class UserModelImpl
    extends DMModelBase
    implements UserModel
{
    AMUser user = null;
    private Set peopleContainers = null;
    private static final String DYNAMIC = "dynamic-";

    public static final String SELECTED_SERVICE_NAMES = "amSelectedServices";

    /**
     * Default roles attribute name. This attribute is found in Administration
     * Console service.
     */  
    public static final String CONSOLE_DEFAULT_ROLES_ATTR =
        "iplanet-am-admin-console-default-roles";

    /**
     * Creates a organization navigation model implementation object
     *
     * @param req  HTTP Servlet Request
     * @param map of user information
     */
    public UserModelImpl(HttpServletRequest req, Map map) {
	super(req, map);
    }    

    public Map getAssignableServiceNames(String uName)
	throws AMConsoleException
    {
        Map available = null;

        user = getUser(uName);
        try {
            String parentDN = user.getOrganizationDN();
            AMOrganization org = getAMStoreConnection().getOrganization(
                parentDN);
            Set availableServices = org.getRegisteredServiceNames();
            availableServices.removeAll(user.getAssignedServices());
            if (availableServices != null && !availableServices.isEmpty()) {
                available = new HashMap(availableServices.size() * 2);
                for (Iterator i = availableServices.iterator(); i.hasNext(); ) {
                    String name = (String)i.next();
                    if (hasDisplayedAttributes(name, SchemaType.USER) ||
                        hasDisplayedAttributes(name, SchemaType.DYNAMIC)) 
                    {
                        String displayName = getLocalizedServiceName(name);
                        if (!name.equals(displayName)) {
                            available.put(name, displayName);
                        }
                    }
                }
            }
        } catch (AMException e) {
            debug.warning("`UserModel.getAssignableServices", e);
            throw new AMConsoleException(getErrorString(e));
        } catch (SSOException e) {
            debug.warning("UserModel.getAssignableServices", e);
        }
        
        return (available == null) ? Collections.EMPTY_MAP : available;
    }

    public void assignService(String userName, String service) 
        throws AMConsoleException 
    {
        String event = null;
        String error = null;
        
        user = getUser(userName);
        Set s = new HashSet(2);
        s.add(service);
        try {
            String[] params = {userName, service};
            logEvent("ATTEMPT_DIR_MGR_ASSIGN_SERVICE_USER", params);
            user.assignServices(s);
            logEvent("SUCCEED_DIR_MGR_ASSIGN_SERVICE_USER", params);
        } catch (SSOException e) {
            error = getErrorString(e);
            event = "SSO_EXCEPTION_DIR_MGR_ASSIGN_SERVICE_USER";
        } catch (AMException e) {
            error = getErrorString(e);
            event = "AM_EXCEPTION_DIR_MGR_ASSIGN_SERVICE_USER";
        }
        
        if (error != null) {
            if (debug.warningEnabled()) {
                debug.warning("UserModel.assignServices " + error);
            }
            String[] paramsEx = {userName, error};
            logEvent(event, paramsEx);
            throw new AMConsoleException(error);
        }
    }

    /**
     * Gets the roles assigned to the user.
     *
     * @return the set of role DNs
     */
    public Set getAssignedRoles(String userName) {
        Set roleDNs = Collections.EMPTY_SET;
        String[] params = {userName};
        logEvent("ATTEMPT_DIR_MGR_GET_ASSIGNED_ROLE_OF_USER", params);

        try {
            user = getUser(userName);
        if (user != null) {
            roleDNs = user.getRoleDNs();
        }
    logEvent("SUCCEED_DIR_MGR_GET_ASSIGNED_ROLE_OF_USER", params);
    } catch (AMException e) {
    String strError = getErrorString(e);
    String[] paramsEx = {userName, strError};
    logEvent("AM_EXCEPTION_DIR_MGR_GET_ASSIGNED_ROLE_OF_USER",
    paramsEx);
    debug.error("UserModelImpl.getAssignedRoles", e);
    } catch (SSOException e) {
    String strError = getErrorString(e);
    String[] paramsEx = {userName, strError};
    logEvent("SSO_EXCEPTION_DIR_MGR_GET_ASSIGNED_ROLE_OF_USER",
    paramsEx);
    debug.error("UserModelImpl.getAssignedRoles", e);
    }

	if (roleDNs == null) {
	    roleDNs = Collections.EMPTY_SET;
	} else {
	    for (Iterator it=roleDNs.iterator(); it.hasNext(); ) {
		String tmp = (String)it.next();
		int type = getObjectType(tmp);
		if ((type != AMObject.ROLE) &&
		    (type != AMObject.MANAGED_ROLE) )
		{
		    if (debug.warningEnabled()) {
			debug.warning("removing " + tmp + " from the " +
			     "users role list. It is not an IS role.");
		    }
		    it.remove();
		}
	    }
	}
	return roleDNs;
    }

    /**
     * Updates the specified roles from the user entry.
     *
     * @param roleDNs  Set of role DNs to be assigned to the user
     * @throws AMConsoleException if cannot update role to the user entry.
     */
    public void updateRoles(String name, Set roleDNs) 
	throws AMConsoleException 
    {
	user = getUser(name);
	Set roleAssigned = getUserRoleDNs();
	Set roleToRemove = excludedObjFromSet(roleAssigned, roleDNs);
	Set roleToAdd = excludedObjFromSet(roleDNs, roleAssigned);
	removeRoles(roleToRemove);
	addRoles(roleToAdd);
    }

    /**
     * Gets the roles assigned to the user.
     *
     * @return the set of role DNs
     */
    private Set getUserRoleDNs() {
	Set roleDNs = Collections.EMPTY_SET;

	try {
	    if (user != null) {
		String[] params = {user.getDN()};
		logEvent("ATTEMPT_DIR_MGR_GET_ASSIGNED_ROLE_OF_USER", params);
		roleDNs = user.getRoleDNs();
		logEvent("SUCCEED_DIR_MGR_GET_ASSIGNED_ROLE_OF_USER", params);
	    }
	} catch (SSOException e) {
	    String strError = getErrorString(e);
	    String[] paramsEx = {user.getDN(), strError};
	    logEvent("SSO_EXCEPTION_DIR_MGR_GET_ASSIGNED_ROLE_OF_USER",
		paramsEx);
	    debug.error("UserModelImpl.getUserRoleDNs", e);
	} catch (AMException e) {
	    String strError = getErrorString(e);
	    String[] paramsEx = {user.getDN(), strError};
	    logEvent("SSO_EXCEPTION_DIR_MGR_GET_ASSIGNED_ROLE_OF_USER",
		paramsEx);
	    debug.warning("UserModelImpl.getUserRoleDNs", e);
	}

	if (roleDNs == null) {
	    roleDNs = Collections.EMPTY_SET;
	} else {
	    for (Iterator it=roleDNs.iterator(); it.hasNext(); ) {
		String tmp = (String)it.next();
		if (getObjectType(tmp) != AMObject.ROLE) {
		    if (debug.warningEnabled()) {
			debug.warning("removing " + tmp + " from the " +
			     "users role list. It is not an IS role.");
		    }
		    it.remove();
		}
	    }
	}
	return roleDNs;
    }

    /**
     * Removes the specified roles from the user entry.
     *
     * @param roleDNs  Set of role DNs to be removed.
     */
    private void removeRoles(Set roleDNs) throws AMConsoleException {
        String strError = null;

        if ((roleDNs != null) && (!roleDNs.isEmpty())) {
            String[] params = new String[2];
            params[0] = user.getDN();
        
            for (Iterator iter = roleDNs.iterator(); 
                iter.hasNext() && (strError == null); ) 
            {
                String roleDN = (String)iter.next();
                params[1] = roleDN;
                logEvent("ATTEMPT_DIR_MGR_REMOVE_ROLE_FROM_USER", params);
        
                try {
                    user.removeRole(roleDN);
                    logEvent("SUCCEED_DIR_MGR_REMOVE_ROLE_FROM_USER", params);
                } catch (AMException e) {
                    strError = getErrorString(e);
                    String[] paramsEx = {user.getDN(), roleDN, strError};
                    logEvent("AM_EXCEPTION_DIR_MGR_REMOVE_ROLE_FROM_USER",
                        paramsEx);
        
                    debug.warning("failed to remove role from user", e);
                } catch (SSOException e) {
                    strError = getErrorString(e);
                    String[] paramsEx = {user.getDN(), roleDN, strError};
                    logEvent("SSO_EXCEPTION_DIR_MGR_REMOVE_ROLE_FROM_USER",
                        paramsEx);

                    debug.warning("failed to remove role from user", e);
                }
            }
        }

        if (strError != null) {
            throw new AMConsoleException(strError);
        }
    }

    /**
     * Adds the specified roles to the user entry.
     *
     * @param addSet Set of role DNs to be added.
     * @throws AMConsoleException if cannot add role to the user entry.
     */
    private void addRoles(Set addSet) throws AMConsoleException {
        String strError = null;
        if ((addSet != null) && (!addSet.isEmpty())) {
            String[] params = new String[2];
            params[0] = user.getDN();

            for (Iterator iter = addSet.iterator(); 
                iter.hasNext() && (strError == null); ) 
            {
                String roleDN = (String)iter.next();
                params[1] = roleDN;
                logEvent("ATTEMPT_DIR_MGR_ADD_ROLE_TO_USER", params);

                try {
                    user.assignRole(roleDN);
                    logEvent("SUCCEED_DIR_MGR_ADD_ROLE_TO_USER", params);
                } catch (AMException e) {
                    strError = getErrorString(e);
                    String[] paramsEx = {user.getDN(), roleDN, strError};
                    logEvent("AM_EXCEPTION_DIR_MGR_ADD_ROLE_TO_USER",
                        paramsEx);
                } catch (SSOException e) {
                    strError = getErrorString(e);
                    String[] paramsEx = {user.getDN(), roleDN, strError};
                    logEvent("SSO_EXCEPTION_DIR_MGR_ADD_ROLE_TO_USER",
                        paramsEx);
                }
            }
        }

        if (strError != null) {
            throw new AMConsoleException(strError);
	}
    }

    /**
     * Gets excluded objects of a set
     *
     * @param set to compare
     * @param base set
     * @return set of excluded objects
     */
    private Set excludedObjFromSet(Set set, Set base) {
	Set excluded = new HashSet(set.size());
	for (Iterator iter = set.iterator(); iter.hasNext(); ) {
	    Object obj = iter.next();
	    if (!base.contains(obj)) {
		excluded.add(obj);
	    }
	}
	return excluded;
    }

    /**
     * Gets the services which are assigned to this user.
     *
     * @param userName name of a user
     * @return a set of services
     */
    public Map getAssignedServices(String userName) {
        //TBD LOG GETTING USER ASSIGNED SERVICES
	Map  names = null;
	try {
	    user = getUser(userName);
	    Set tmp = user.getAssignedServices();
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
	} catch (SSOException ssoe) {
	    debug.warning("UserModel.getAssignedServices", ssoe);
	} catch (AMException ame) {
	    debug.warning("UserModel.getAssignedServices", ame);
	}
	return (names == null) ? Collections.EMPTY_MAP : names;
    }
	
    /**
     * Removes the specified services from this user.
     *
     * @param userName name of a user
     * @param services set of services to remove from the user.
     */
    public void removeServices(String userName, Set services) 
        throws AMConsoleException
    {
        String error = null;
        String event = null;
        try {
            user = getUser(userName);
            String[] params = { userName };
            logEvent("ATTEMPT_DIR_MGR_REMOVE_SERVICE_USER", params);
            user.unassignServices(services);
            logEvent("SUCCEED_DIR_MGR_REMOVE_SERVICE_USER", params);
        } catch (SSOException e) {
            error = getErrorString(e);
            event = "SSO_EXCEPTION_DIR_MGR_REMOVE_SERVICE_USER";
        } catch (AMException e) {
            error = getErrorString(e);
            event = "AM_EXCEPTION_DIR_MGR_REMOVE_SERVICE_USER";
        }
        
        if (error != null) {
            if (debug.warningEnabled()) {
                debug.warning("UserModel.removeServices " + error);
            }
            String[] paramsEx = {userName, error};
            logEvent(event, paramsEx);
            throw new AMConsoleException(error);
        }
    }

    /**
     * Gets a set of users.
     *
     * @param location name or current organization location
     * @param pattern Pattern used to limit the number of users returned.
     * @return a set of organizations
     */
    public Set getUsers(String location, String pattern) {
        if (location == null) {
            location = getStartDSDN();
        }

        AMStoreConnection sc = getAMStoreConnection();
        AMSearchResults results = null;
        AMSearchControl searchControl = new AMSearchControl();
        searchControl.setSearchScope(AMConstants.SCOPE_SUB);
        setSearchControlAttributes(
            searchControl, getValidUserAttributes(getSearchReturnValue()));
        setSearchControlLimits(searchControl);

        try {
            String filter = createUserSearchFilter(pattern);
            String[] params = {location, pattern};
            logEvent("ATTEMPT_DIR_MGR_SEARCH_FOR_USERS_IN_ORG", params);
            int type = getObjectType(location);
            if (type == AMObject.ORGANIZATION) {
                AMOrganization org = sc.getOrganization(location);
                results = org.searchUsers(searchControl, filter);
                logEvent("SUCCEED_DIR_MGR_SEARCH_FOR_USERS_IN_ORG", params);
            } else if (type == AMObject.ORGANIZATIONAL_UNIT) {
                AMOrganizationalUnit orgUnit = sc.getOrganizationalUnit(
                    location);
                results =  orgUnit.searchUsers(searchControl, filter);
                logEvent("SUCCEED_DIR_MGR_SEARCH_FOR_USERS_IN_ORG", params);
            } else if (type == AMObject.PEOPLE_CONTAINER) {
                AMPeopleContainer peopleContainer = sc.getPeopleContainer(
                    location);
                results = peopleContainer.searchUsers(searchControl,filter);
                logEvent("SUCCEED_DIR_MGR_SEARCH_FOR_USERS_IN_ORG", params);
            } else {
                debug.warning(
                    "UserModel.getUsers() : unsupported type");
            }
        } catch (SSOException e) {
            String strError = getErrorString(e);
            String[] paramsEx = {location, pattern, strError};
            logEvent("SSO_EXCEPTION_DIR_MGR_SEARCH_FOR_USERS_IN_ORG",
                paramsEx);
            debug.warning("UserModelImpl.getUsers", e);
        } catch (AMException e) {
            String strError = getErrorString(e);
            String[] paramsEx = {location, pattern, strError};
            logEvent("AM_EXCEPTION_DIR_MGR_SEARCH_FOR_USERS_IN_ORG",
                paramsEx);
            debug.warning("UserModelImpl.getUsers", e);
        }

	return setSearchResults(results);
    }

    public void updateUser(String name, Map data) 
	throws AMConsoleException {
	for (Iterator i=data.keySet().iterator(); i.hasNext();) {
	    String key = (String)i.next();
	    Set value = (Set)data.get(key);
	    if (value == null || value.isEmpty()) {
		i.remove();
	    }
	}

	validateRequiredAttributes(data);

	try {
	    String[] params = {name};
	    logEvent("ATTEMPT_DIR_MGR_MODIFY_USER", params);
	    user = getUser(name);
	    user.setAttributes(data);
	    user.store();
	    logEvent("SUCCEED_DIR_MGR_MODIFY_USER", params);
	} catch (AMException e) {
	    String strError = getErrorString(e);
	    String[] paramsEx = {name, strError};
	    logEvent("AM_EXCEPTION_DIR_MGR_MODIFY_USER", paramsEx);
	    throw new AMConsoleException(strError);
	} catch (SSOException e) {
	    String strError = getErrorString(e);
	    String[] paramsEx = {name, strError};
	    logEvent("SSO_EXCEPTION_DIR_MGR_MODIFY_USER", paramsEx);
	    throw new AMConsoleException(strError);
	}
    }

    /**
     * Creates a user based on the specified data. The input data has four
     * key elements: user naming attribute; list of services to  assign to
     * the user; a set of required attribute values; and the people container
     * to create the user.
     * 
     * @param location where to create the given user
     * @param data map which contains the user name and the optional
     * and required attributes.
     */  
    public void createUser(String location, Map data) 
	throws AMConsoleException 
    {
	if (location == null) {
	    location = "ou=People," + getStartDSDN();
	}

	if ( (data == null) || data.isEmpty()) {
	    debug.warning("null or missing data values");
	    throw new AMConsoleException(
		getLocalizedString("createFailure.message"));
	}
        
        // check if the people container was passed in the request
        String pcDN = "";
        Set tmp = (Set)data.remove(PEOPLE_CONTAINER);
        if ((tmp != null) && !tmp.isEmpty()) {
            pcDN = (String)tmp.iterator().next();
        }
        
        // check that all the data we need to create a user is available
        validateRequiredAttributes(data);
        
        // extract the name used for this user
        String namingAttr = AdminInterfaceUtils.getNamingAttribute(
            AMObject.USER, debug);
        tmp = (Set)data.get(namingAttr);
        String name = (String)tmp.iterator().next();
        
        // create a map of username and the data to create the user
        Set orgs = Collections.EMPTY_SET;
        Map attrMap = new HashMap(2);
        attrMap.put(name, data);
        String[] params = {location, name};
        logEvent("ATTEMPT_DIR_MGR_CREATE_USER", params);

	try {
            // if pc name was not passed in request try creating it
	    int type = getObjectType(location);
	    if (pcDN.length() == 0) {
                pcDN = getDefaultPeopleContainer(
                    location, getObjectType(location));
	    } 

            Set serviceNames = (Set)data.remove(SELECTED_SERVICE_NAMES);
            if (serviceNames == null) {
                serviceNames = Collections.EMPTY_SET;
            }

	    AMPeopleContainer pc = getAMStoreConnection().getPeopleContainer(
                pcDN);
            Set users = pc.createUsers(attrMap, serviceNames);

            if ((users != null) && !users.isEmpty()) {
                AMUser amuser = (AMUser)users.iterator().next();
                String createdUserDN = amuser.getDN();
                Set userDN = new HashSet(2);
                userDN.add(createdUserDN);
                String errorStr = assignUserToGroup(
                    name, userDN, location);
                /*
                 * user may be created by group admin but cannot be assigned
                 * to nested group.
                 */
                if (errorStr == null) {
                    assignDefaultRolesToUser(amuser);
		    logEvent("ATTEMPT_DIR_MGR_CREATE_USER", params);
                } else {
		    String[] paramsEx = {location, name, ""};
		    logEvent("AM_EXCEPTION_DIR_MGR_CREATE_USER", paramsEx);
                    pc.deleteUsers(userDN);
                }
            }
	} catch (AMException e) {
	    if (debug.warningEnabled()) {
		debug.warning("UserModel.createUser " + e.getMessage());
	    }

	    String strError = getErrorString(e);
	    String[] paramsEx = {location, name, strError};
	    logEvent("AM_EXCEPTION_DIR_MGR_CREATE_USER", paramsEx);
	    throw new AMConsoleException(strError);
	} catch (SSOException e) {
	    String strError = getErrorString(e);
	    String[] paramsEx = {location, name, strError};
	    logEvent("SSO_EXCEPTION_DIR_MGR_CREATE_USER", paramsEx);
	    debug.error("error in sso ", e);
	}
    }
    
    /**
     * Returns error message if user cannot be assigned to a group.
     * If the user is created in a group then add the user to the group
     * after creation.
     *   
     * @return Error message if user cannot be assigned to a group.
     */  
    private String assignUserToGroup(String userName, Set userDN, String loc) {
        String errMsg = null;

        int type = getObjectType(loc);

        if ((type == AMObject.GROUP) || (type == AMObject.STATIC_GROUP)) {
            try {
                AMStaticGroup sgroup = getAMStoreConnection().getStaticGroup(
                    loc);
                sgroup.addUsers(userDN);
            } catch (AMException e) {
                debug.warning("UserModel.assignUserToGroup", e);
                errMsg = getErrorString(e);
            } catch (SSOException e) {
                debug.warning("UserModel.assignUserToGroup", e);
                errMsg = getErrorString(e);
            }
        } else if (type == AMObject.ASSIGNABLE_DYNAMIC_GROUP) {
            try {
                AMAssignableDynamicGroup agroup =
                    getAMStoreConnection().getAssignableDynamicGroup(loc);
                agroup.addUsers(userDN);
            } catch (AMException e) {
                debug.warning("UserModel.assignUserToGroup", e);
                errMsg = getErrorString(e);
            } catch (SSOException e) {
                debug.warning("UserModel.assignUserToGroup", e);
                errMsg = getErrorString(e);
            }
        } else {
            debug.warning("location is not a group type");
        }

        if (errMsg != null) {
            String[] param = {userName, errMsg};
            errMsg = MessageFormat.format(
                getLocalizedString("cannotAssignUserToGroup"), (Object[])param);
            
            //TBD LOG USER ASSIGNED TO GROUP
            //logEvent("SUCCEED_ASSIGN_GROUP_TO_USER", params);
        }
 
        return errMsg;
    }

    private Set getDefaultRoles(AMUser user) {
        Set roleDNs = Collections.EMPTY_SET;
        String orgDN = null;

        try {
            orgDN = user.getOrganizationDN();
        } catch (SSOException ssoe) {
            if (debug.warningEnabled()) {
                debug.warning("UserModel.getDefaultRoles: " +
                    "cannot get user's organization DN", ssoe);
            }
        } catch (AMException ame) {
            debug.error("UserModel.getDefaultRoles: " +
                "cannot get user's organization DN", ame);
        }

        if (orgDN != null) {
            roleDNs = getAttrValues(CONSOLE_DEFAULT_ROLES_ATTR,
                ADMIN_CONSOLE_SERVICE, orgDN);

            for (Iterator iter = roleDNs.iterator(); iter.hasNext(); ) {
                String roleDN = (String)iter.next();

                if (!orgDN.equalsIgnoreCase(AMAdminUtils.getParent(roleDN))) {
                    iter.remove();
                }
            }    
        }

        return roleDNs;
    }

    private void assignDefaultRolesToUser(AMUser user) {
        Set defaultRoleDNs = getDefaultRoles(user);
        for (Iterator iter = defaultRoleDNs.iterator(); iter.hasNext(); ) {
            //logEvent("ATTEMPT_ASSIGN_ROLE_TO_USER", params);
            String roleDN = (String)iter.next();
            try {
                debug.warning("assignin " + roleDN + " to user entry");
                user.assignRole(roleDN);
                //logEvent("SUCCEED_ASSIGN_ROLE_TO_USER", params)
            } catch (AMException ame) {
                debug.warning(
                    "UserModel.assignDefaultRolesToUser", ame);
                //logEvent("FAILED_ASSIGN_ROLE_TO_USER", params);
            } catch (SSOException ssoe) {
                debug.warning(
                    "UserModel.assignDefaultRolesToUser", ssoe);
                //logEvent("FAILED_ASSIGN_ROLE_TO_USER", params);
            }
        }
    }    

    public Map getValues(String name) 
	throws AMConsoleException
    {
	Map map = null;
        try {
            String[] params = {name};
            logEvent("ATTEMPT_DIR_MGR_GET_USER_ATTR_VALUES", params);
            AMUser amuser = getAMStoreConnection().getUser(name);
            map = correctAttributeNames(amuser.getAttributes());
            validateUserStatusEntry(map);
            logEvent("SUCCEED_DIR_MGR_GET_USER_ATTR_VALUES", params);
        } catch (AMException e) {
            String strError = getErrorString(e);
            String[] paramsEx = {name, strError};
            logEvent("AM_EXCEPTION_DIR_MGR_GET_USER_ATTR_VALUES", paramsEx);
            throw new AMConsoleException(strError);
        } catch (SSOException e) {
            String strError = getErrorString(e);
            String[] paramsEx = {name, strError};
            logEvent("SSO_EXCEPTION_DIR_MGR_GET_USER_ATTR_VALUES", paramsEx);
            throw new AMConsoleException(strError);
        }
        
	return (map == null) ? Collections.EMPTY_MAP : map;
    }

    private Map correctAttributeNames(Map values) {
	Map map = getDataMap();
	Map corrected = new HashMap(map.size() *2);

	for (Iterator i = map.keySet().iterator(); i.hasNext(); ) {
	    String attrName = (String)i.next();
	    Object val = values.get(attrName.toLowerCase());
	    /*
	     * need to pad with collection empty set otherwise we only get
	     * back a map of the attributes which have a value assigned to it. 
	     */
	    corrected.put(attrName,
		(val != null) ? val : Collections.EMPTY_SET);
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
	    ServiceSchemaManager manager =
		getServiceSchemaManager(AMAdminConstants.USER_SERVICE);
	    ServiceSchema schema = manager.getSchema(SchemaType.USER);
	    Set attrSchemas = schema.getAttributeSchemas();

	    for (Iterator iter = attrSchemas.iterator(); iter.hasNext(); ) {
		AttributeSchema as = (AttributeSchema)iter.next();
		map.put(as.getName(), Collections.EMPTY_SET);
	    }
	    map.put(PEOPLE_CONTAINER, Collections.EMPTY_SET);
	} catch (SMSException e) {
	    debug.error("UserModelImpl.getDataMap", e);
	} catch (SSOException e) {
	    debug.error("UserModelImpl.getDataMap", e);
	}

	return map;
    }

    /**
     * Returns sub realm creation property XML.
     *
     * @return sub realm creation property XML.
     */
    public String getCreateUserPropertyXML(String location) {
	StringBuffer buff = new StringBuffer(2000);
	try {
	    Set attributes = getUserAttributes(SchemaType.USER);
	    String[] show = {"required", "optional"};
	    PropertyXMLBuilder.filterAttributes(attributes, show);
	    buff.append(PropertyXMLBuilderBase.getXMLDefinitionHeader())
		.append(PropertyTemplate.START_TAG);
	    getPropertyXML(attributes, buff, false);
	    buff.append(PropertyTemplate.END_TAG);

	    /*
	    * insert people container dropdown if their is more than one 
	    * available and people containers are not being displayed.
	    */
	    Set pcs = getPeopleContainers(location);
	    if ((pcs != null) && (pcs.size() > 1) && !showPeopleContainers()) {
		String tmp = buff.toString();
		int start = tmp.indexOf("<section name=");
		start = tmp.indexOf('>', start);
                buff.insert(start+1, SHOW_PC_TAG);
	    }

	} catch (SSOException e) {
	    debug.error("UserModelImpl.getCreateUserPropertyXML", e);
	} catch (SMSException e) {
	    debug.error("UserModelImpl.getCreateUserPropertyXML", e);
	}

	return buff.toString();
    }

    private boolean isDisplayCombined() {
        String combined = "";

        try {
            AMTemplate template = getOrgTemplate(getAuthenticatedOrgDN());
            if (template == null) {
                ServiceSchemaManager mgr = getServiceSchemaManager(
                    ADMIN_CONSOLE_SERVICE);
                combined = getStringAttribute(mgr,
                    SchemaType.ORGANIZATION, CONSOLE_USER_SERVICE_DISPLAY_ATTR);
            } else {
                combined = getStringAttribute(
                    template, CONSOLE_USER_SERVICE_DISPLAY_ATTR);
            }
        } catch (SSOException sso) {
            debug.error("UserModelImpl.isDisplayCombined", sso);
        } catch (AMException ame) {
            debug.error("UserModelImpl.isDisplayCombined", ame);
        } catch (SMSException e) {
            debug.error("UserModelImpl.isDisplayCombined", e);
        }
         
        return combined.equalsIgnoreCase("Combined");
    }


    /**
     * Returns user profile property XML.
     *
     * @param userDN DN of users.
     * @param viewbeanClassName Class Name of View Bean.
     * @return user profile creation property XML.
     */
    public String getUserProfileXML(String userDN, String viewbeanClassName) {
	boolean canModify = canModify(userDN, viewbeanClassName);
	StringBuffer buff = new StringBuffer(2000);
	try {
	    Set attributes = getUserAttributes(SchemaType.USER);
            Set userSet = new HashSet(attributes.size()*2);
            for (Iterator i = attributes.iterator(); i.hasNext();) {
                AttributeSchema x = (AttributeSchema)i.next();
                userSet.add(x.getName());
            }

	    String[] show = {"display", "adminDisplay"};
	    PropertyXMLBuilder.filterAttributes(attributes, show);
	    buff.append(PropertyXMLBuilderBase.getXMLDefinitionHeader())
		.append(PropertyTemplate.START_TAG);
	    getPropertyXML(attributes, buff, !canModify);

            if (isDisplayCombined()) {
                Set dynamicAttributes = getUserAttributes(SchemaType.DYNAMIC);
                if (dynamicAttributes != null) {
                    
                    // remove those which are also user attributes
                    for (Iterator i = dynamicAttributes.iterator(); 
                         i.hasNext(); ) 
                    {
                        AttributeSchema as = (AttributeSchema)i.next();
                        String name = as.getName();
                        if (userSet.contains(name)) {
                            i.remove();
                        }
                    }
                    // true here means cannot modify
                    getPropertyXML(dynamicAttributes, buff, true);
                }
            }
            buff.append(PropertyTemplate.END_TAG);
	} catch (SSOException e) {
	    debug.error("UserModelImpl.getUserProfileXML", e);
	} catch (SMSException e) {
	    debug.error("UserModelImpl.getUserProfileXML", e);
	}

	return buff.toString();
    }

    public boolean canModify(String userDN, String viewbeanClassName) {
        boolean canModify = false;
        try {
            user = getUser(userDN);
            String orgDN = user.getOrganizationDN();
            DelegationConfig dConfig = DelegationConfig.getInstance();
            canModify = dConfig.hasPermission(orgDN, null,
                 AMAdminConstants.PERMISSION_MODIFY, this, viewbeanClassName);
        } catch (AMException e) {
            debug.error("UserModelImpl.canModify", e);
        } catch (SSOException e) {
            debug.error("UserModelImpl.canModify", e);
        }
        return canModify;
    }

    private void getPropertyXML(
	Set attributes,
	StringBuffer buff,
	boolean readonly
    ) {
	try {
	    PropertyXMLBuilder xmlBuilder =  new PropertyXMLBuilder(
		AMAdminConstants.USER_SERVICE, this, attributes);
	    if (readonly) {
		xmlBuilder.setAllAttributeReadOnly(true);
	    }
	    buff.append(xmlBuilder.getXML(false));
	} catch (SSOException e) {
	    debug.error("UserModelImpl.getPropertyXML", e);
	} catch (SMSException e) {
	    debug.error("UserModelImpl.getPropertyXML", e);
	} catch (AMConsoleException e) {
	    debug.error("UserModelImpl.getPropertyXML", e);
	}

    }

    public void registerService(String organization, String service) 
	throws AMConsoleException 
    {
	try {
	    AMOrganization org = getAMStoreConnection().getOrganization(
                organization);
	    org.registerService(service, true, true);
	} catch (AMException e) {
	    throw new AMConsoleException(getErrorString(e));
	} catch (SSOException soe) {
	    throw new AMConsoleException(getErrorString(soe));
	}
    }

    public String getRoleType(String role) {
	String type = "static.role";
	if (getObjectType(role) == AMObject.FILTERED_ROLE)  {
	    type = "filtered.role";
	}
	return getLocalizedString(type);
    }

    private Set getUserAttributes(SchemaType type)
	throws SSOException, SMSException
    {
	Set attributes = null;
	try {
	    ServiceSchemaManager manager =
		getServiceSchemaManager(AMAdminConstants.USER_SERVICE);
	    ServiceSchema sub = manager.getSchema(type);
	    attributes = sub.getAttributeSchemas();
	    setMandatoryAttributes(attributes);
	} catch (SSOException e) {
	    debug.error("UserModelImpl.getUserAttributes", e);
	} catch (SMSException e) {
	    debug.error("UserModelImpl.getUserAttributes", e);
	}
	return (attributes != null) ? attributes : Collections.EMPTY_SET;
    }

    /**
     * Returns set of roles that are available for assignment to a user.
     *
     * @param userName Name of user.
     * @param assigned Collection of assigned role.
     */
    public Set getAvailableRoles(String userName, Collection assigned) {
	Set roles = null;
	user = getUser(userName);
	if (user == null) {
	    return Collections.EMPTY_SET;
	}

	try {
	    String orgDN = user.getOrganizationDN();
	    AMOrganization org = getAMStoreConnection().getOrganization(orgDN);
	    if (org != null) {
		roles = org.getRoles(AMConstants.SCOPE_ONE);
	    }
	    if (roles != null) {
		AMAdminUtils.removeAllByDN(roles, assigned);
	    }
	} catch (AMException e) {
	    debug.error(getErrorString(e));
	} catch (SSOException soe) {
	    debug.error(getErrorString(soe));
	}
	
	return (roles == null) ? Collections.EMPTY_SET : roles;
    }

    /**
     * Returns set of groups that are available for assignment to a user.
     *
     * @param userName Name of user.
     * @param assigned Collection of assigned groups.
     */
    public Set getAvailableGroups(String userName, Collection assigned) {
	Set groups = null;
	user = getUser(userName);
	if (user == null) {
	    return Collections.EMPTY_SET;
	}
	// get all the available groups and then remove those already
	// assigned to the user.
	try {
	    String orgDN = user.getOrganizationDN();
	    AMOrganization org = getAMStoreConnection().getOrganization(orgDN);
	    if (org != null) {
		groups = org.getStaticGroups(AMConstants.SCOPE_SUB);
		groups.addAll(
		    org.getAssignableDynamicGroups(AMConstants.SCOPE_SUB));
	    }
	    if (groups != null) {
		AMAdminUtils.removeAllByDN(groups, assigned);
	    }
	} catch (AMException e) {
	    debug.error(getErrorString(e));
	} catch (SSOException soe) {
	    debug.error(getErrorString(soe));
	}
	
	return (groups == null) ? Collections.EMPTY_SET : groups;
    }

    /**
     * Updates the specified groups from the user entry.
     *
     * @param name of user entry.
     * @param groupDNs set of group to be assigned to the user.
     * @throws AMConsoleException if updating fails.
     */
    public void updateGroups(String name, Set groupDNs) 
	throws AMConsoleException 
    {
	user = getUser(name);
        Set groupAssigned = getAssignedGroups(name);
	Set groupToRemove = excludedObjFromSet(groupAssigned, groupDNs);
	Set groupToAdd = excludedObjFromSet(groupDNs, groupAssigned);
	removeGroups(groupToRemove);
	addGroups(groupToAdd);
    }

    /**
     * Returns the static groups currently assigned to the user. This
     * will not return the dynamic groups.
     *
     * @param userName of the user entry.
     * @return Set of group dns.
     */
    public Set getAssignedGroups(String userName) {   
	Set userGroups = null;
	user = getUser(userName);
	if (user == null) {
	    return Collections.EMPTY_SET;
	}
	try {
	    String[] params = {userName};
	    logEvent("ATTEMPT_DIR_MGR_GET_ASSIGNED_GROUPS_OF_USER", params);
	    userGroups = user.getStaticGroupDNs();
	    userGroups.addAll(user.getAssignableDynamicGroupDNs());
	    logEvent("SUCCEED_DIR_MGR_GET_ASSIGNED_GROUPS_OF_USER", params);
	} catch (AMException e) {
	    String strError = getErrorString(e);
	    String[] paramsEx = {userName, strError};
	    logEvent("AM_EXCEPTION_DIR_MGR_GET_ASSIGNED_GROUPS_OF_USER",    
		paramsEx);
	    debug.error(strError);
	} catch (SSOException e) {
	    String strError = getErrorString(e);
	    String[] paramsEx = {userName, strError};
	    logEvent("SSO_EXCEPTION_DIR_MGR_GET_ASSIGNED_GROUPS_OF_USER",
		paramsEx);
	    debug.error(strError);
	}

	if (!isAdminGroupsEnabled()) {
	    userGroups = removeHiddenGroups(userGroups);
	}
	if (userGroups == null) {
	    userGroups = Collections.EMPTY_SET;
	}

	return (userGroups == null) ? Collections.EMPTY_SET : userGroups;
    }

    /**
     * Removes the specified groups from the user entry.
     *
     * @param groupDNs Set of group DNs to be removed.
     */
    public void removeGroups(Set groupDNs) throws AMConsoleException {
	if (user == null || groupDNs == null || groupDNs.isEmpty()) {
	    return;
	}

	// groups to be removed
	String strError = null;
	String[] params = new String[2];
	params[0] = user.getDN();

	for (Iterator iter = groupDNs.iterator(); 
            iter.hasNext() && (strError == null); ) 
        {
	    String dn = (String) iter.next();
	    params[1] = dn;
	    logEvent("ATTEMPT_DIR_MGR_REMOVE_GROUP_FROM_USER", params);

	    try {
		int groupType = getObjectType(dn);
		if (groupType == AMObject.GROUP ||
		    groupType == AMObject.STATIC_GROUP) {
                    user.removeStaticGroup(dn);
		} else {
		    user.removeAssignableDynamicGroup(dn);
		}

		logEvent("SUCCEED_DIR_MGR_REMOVE_GROUP_FROM_USER", params);
	    } catch (AMException e) {
		strError = getErrorString(e);
		String[] paramsEx = {user.getDN(), dn, strError};
		logEvent("AM_EXCEPTION_DIR_MGR_REMOVE_GROUP_FROM_USER",
		    paramsEx);
		debug.warning("removing groups from user", e);
	    } catch (SSOException e) {
		strError = getErrorString(e);
		String[] paramsEx = {user.getDN(), dn, strError};
		logEvent("SSO_EXCEPTION_DIR_MGR_REMOVE_GROUP_FROM_USER",
		    paramsEx);
		debug.warning("removing groups from user", e);
	    }
	}
 
	if (strError != null) {
	    throw new AMConsoleException(strError);
	}
    }   
     
    /**
     * Removes hidden groups from iPlanet compliant group DNs
     *
     * @param groupDNs   iPlanet compliant group DNs
     * @return a set of unhidden groups
     */
    private Set removeHiddenGroups(Set groupDNs) {
	Set visibleGroups = Collections.EMPTY_SET;

	if ((groupDNs != null) && (!groupDNs.isEmpty())) {
	    visibleGroups = new HashSet(groupDNs.size());
	    for (Iterator iter = groupDNs.iterator(); iter.hasNext(); ) {
		String dn = (String) iter.next();
		String name = AMFormatUtils.DNToName(this, dn);

		if (!name.equalsIgnoreCase(DOMAIN_ADMINS) &&
		    !name.equalsIgnoreCase(DOMAIN_HELP_DESK_ADMINS) &&
		    !name.equalsIgnoreCase(SERVICE_ADMINS) &&
		    !name.equalsIgnoreCase(SERVICE_HELP_DESK_ADMINS)) {
		    visibleGroups.add(dn);
		}
	    }
	}

	return visibleGroups;
    }

    /**
     * Adds the specified groups to the user entry.
     *
     * @param addSet Set of group DNs to be added.
     * @throws AMConsoleException if cannot add group to the user entry.
     */
    private void addGroups(Set groupDNs) throws AMConsoleException {
        if (user == null || groupDNs == null || groupDNs.isEmpty()) {
            return;
        }

        // groups to be added
	String[] params = new String[2];
	params[0] = user.getDN();
	
        String strError = null;
        for (Iterator iter = groupDNs.iterator(); 
            iter.hasNext() && (strError == null); ) 
        {
            String dn = (String) iter.next();
	    params[1] = dn;
	    logEvent("ATTEMPT_DIR_MGR_ADD_GROUP_TO_USER", params);

            try {
                int groupType = getObjectType(dn);
                if (groupType == AMObject.GROUP ||
                    groupType == AMObject.STATIC_GROUP) {
                    user.assignStaticGroup(dn);
                } else {
                    user.assignAssignableDynamicGroup(dn);
                }
                logEvent("SUCCEED_DIR_MGR_ADD_GROUP_TO_USER", params);
            } catch (AMException e) {
                strError = getErrorString(e);
                String[] paramsEx = {user.getDN(), dn, strError};
                logEvent("AM_EXCEPTION_DIR_MGR_ADD_GROUP_TO_USER", paramsEx);
                debug.warning("assigning groups to user", e);
            } catch (SSOException e) {
                strError = getErrorString(e);
                String[] paramsEx = {user.getDN(), dn, strError};
                logEvent("SSO_EXCEPTION_DIR_MGR_ADD_GROUP_TO_USER", paramsEx);
                debug.warning("assigning groups to user", e);
            }
        }

        if (strError != null) {
            throw new AMConsoleException(strError);
        }
    }

    private AMUser getUser(String userName) {
	if (user == null) {
	    try {
		user = getAMStoreConnection().getUser(userName);
	    } catch (SSOException soe) {
		debug.error(getErrorString(soe));
	    }
	}	       
	return user;
    }

    /**
     * Returns default people container DN.
     *   
     * @return default people container DN.
     */  
    public String getDefaultPeopleContainer(String location, int type) {
        String pcDN = null;

        try {
            switch (type) {
            case AMObject.ORGANIZATION:
            case AMObject.ORGANIZATIONAL_UNIT:
                pcDN = getDefaultPeopleContainer(location);
                break;
            case AMObject.PEOPLE_CONTAINER:
                pcDN = location;
                break;
            case AMObject.GROUP:
            case AMObject.STATIC_GROUP:
            case AMObject.ASSIGNABLE_DYNAMIC_GROUP:
                pcDN = getDefaultPeopleContainer(getParentDN(location));
                break;
            default:
                pcDN = "";
            }
        } catch (SSOException ssoe) {
            debug.error("UserModelImpl.getDefaultPeopleContainer",
                ssoe);
        } catch (AMException ame) {
            debug.error("UserModelImpl.getDefaultPeopleContainer",
                ame);
        }
 
        return (pcDN != null) ? pcDN : "";
    }

    private String getDefaultPeopleContainer(String orgDN) {
        DN defaultPCDN = null;

        if (orgDN.equalsIgnoreCase(AMSystemConfig.defaultOrg)) {
            defaultPCDN = new DN(AMSystemConfig.defaultOrg);
        } else {
            defaultPCDN = new DN(orgDN);
        }

        defaultPCDN.addRDN(new RDN(AdminInterfaceUtils.getNamingAttribute(
	    AMObject.PEOPLE_CONTAINER, debug) +
            "=" + AdminInterfaceUtils.defaultPeopleContainerName()));

        return defaultPCDN.toString();
    }

    private Set getGroupPCList(AMGroup group) {
        Set peopleContainers = Collections.EMPTY_SET;
        DN groupDN = new DN(group.getDN());  
        String orgDN = "";
        try {
            orgDN = getParentDN(group.getDN());
        } catch (AMException a) {
            debug.error("getGroupPCList", a);
        } catch (SSOException a) {
            debug.error("getGroupPCList", a);
        }

        if (orgDN != null) {
            // get values from Groups People Container List
            Set values = getAttrValues(CONSOLE_GROUP_PC_LIST_ATTR,
                ADMIN_CONSOLE_SERVICE, orgDN);

            if ((values != null) && !values.isEmpty()) {
                peopleContainers = new HashSet(values.size());

                for (Iterator iter = values.iterator(); iter.hasNext(); ) {
                    StringTokenizer st =
                        new StringTokenizer((String)iter.next(), "|");

                    if (st.countTokens() == 2) {
                        DN dn = new DN(st.nextToken().trim());

                        if (dn.equals(groupDN)) {
                            peopleContainers.add(st.nextToken().trim());
                        }
                    }    
                }
            }
            if (peopleContainers.isEmpty()) {
                peopleContainers = new HashSet(1);
 
                // get value from Groups Default People Container
                Set defaultValues = getAttrValues(CONSOLE_GROUP_DEFAULT_PC_ATTR,
		    ADMIN_CONSOLE_SERVICE, orgDN);
 
                if ((defaultValues != null) && !defaultValues.isEmpty()) {
                    String dn =
                        ((String)defaultValues.iterator().next()).trim();
 
                    if (dn.length() > 0) {
                        peopleContainers.add(dn);
                    }
                }      
 
                // finally default to ou=People container
                if (peopleContainers.isEmpty()) {
                    peopleContainers.add(getDefaultPeopleContainer(orgDN));
                }
            }
        }
 
        return peopleContainers;
    }

    public Set getPeopleContainers(String locationDN) {
	if (peopleContainers == null) {
            peopleContainers = Collections.EMPTY_SET;
	    int locationType = getObjectType(locationDN);
            AMStoreConnection sc = getAMStoreConnection();
            try {
                switch (locationType) {
                case AMObject.ORGANIZATION:
                AMOrganization org = sc.getOrganization(locationDN);
                    peopleContainers = org.getPeopleContainers(
			AMConstants.SCOPE_ONE);
                    break;
                case AMObject.ORGANIZATIONAL_UNIT:
                    AMOrganizationalUnit orgUnit =
                        sc.getOrganizationalUnit(locationDN);
                    peopleContainers = orgUnit.getPeopleContainers(
			AMConstants.SCOPE_ONE);
                    break;
                case AMObject.PEOPLE_CONTAINER:
                    peopleContainers = new HashSet(1);
                    peopleContainers.add(locationDN);
                    break;
                case AMObject.GROUP:
                case AMObject.STATIC_GROUP:
                    AMStaticGroup sgroup = sc.getStaticGroup(locationDN);
                    peopleContainers = getGroupPCList(sgroup);
                    validatePCList(peopleContainers);
                    break;
                case AMObject.ASSIGNABLE_DYNAMIC_GROUP:
                    AMAssignableDynamicGroup agroup =
                        sc.getAssignableDynamicGroup(locationDN);
                    peopleContainers = getGroupPCList(agroup);
                    validatePCList(peopleContainers);
                    break;
                }
            } catch (AMException e) {
                debug.warning("failed getting people containers", e);
            } catch (SSOException e) {
                debug.error("failed getting people containers", e);
            }
	}
        return peopleContainers;
    }

    /**
     * Validates a set of people containers. The valid people containers will
     * be returned. Invalid people containers will be removed from the set.
     * Validation done as OpenSSO's user since the group
     * administrator does not have read and search access to people containers.
     */
    private void validatePCList(Set pcDNs) {
        if (pcDNs == null || pcDNs.isEmpty()) {
            return;
        }

        /*
        * tmp is used to store the pc's that are invalid. They will be
        * removed from the set of pc's.
        */
        Set tmp = new HashSet(pcDNs.size() *2);
        AMStoreConnection amsc = getAdminStoreConnection();

        if (amsc != null) {
            for (Iterator iter = pcDNs.iterator(); iter.hasNext(); ) {
                String pcDN = (String)iter.next();
                try {
                    AMPeopleContainer pc = amsc.getPeopleContainer(pcDN);
                    if ((pc == null) || !pc.isExists()) {
                        iter.remove();
                    }
                } catch (SSOException soe) {
                    debug.error(
                        "UserModelImpl.validateGroupPCList", soe);
                }
            }

            // remove any invalid pc's stored in tmp
            pcDNs.removeAll(tmp);
        } else {
            pcDNs.clear();
        }
    } 

    private static final String SHOW_PC_TAG = "\n<property required=\"true\"><label name=\"lblPeopleContainer\" defaultValue=\"table.dm.path.column.name\" labelFor=\"peopleContainer\" /><cc name=\""+PEOPLE_CONTAINER+"\" tagclass=\"com.sun.web.ui.taglib.html.CCDropDownMenuTag\" ></cc><fieldhelp name=\"lblHelp\" defaultValue=\"new.user.container.help\" /></property>";
}
