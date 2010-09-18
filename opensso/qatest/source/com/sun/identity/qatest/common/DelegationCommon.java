/* The contents of this file are subject to the terms
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
 * $Id: DelegationCommon.java,v 1.5 2008/06/26 20:10:38 rmisra Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.identity.qatest.common;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.iplanet.sso.SSOToken;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdType;
import com.sun.identity.qatest.delegation.DelegationConstants;
import com.sun.identity.sm.OrganizationConfigManager;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.logging.Level;

/**
 *
 *This class contains helper methods to executed delegation testcases
 */
public class DelegationCommon extends IDMCommon implements DelegationConstants {
    
    /**
     * famadm.jsp url
     */
    private String fmadmURL;
    
    /**
     * Federation Manager admin object
     */
    private FederationManager fmadm;
    
    /**
     * webClient object used to access html content.
     */
    private WebClient webClient;
    
    /**
     * Federation Manager login URL
     */
    protected String loginURL;
    
    /**
     * Federation Manager logout URL
     */
    protected String logoutURL;
    
    protected ResourceBundle famMsgBdl;
    
    /** Creates a new instance of DelegationCommon */
    public DelegationCommon(String componentName) {
        super(componentName);
        loginURL = protocol + ":" + "//" + host + ":" + port + uri +
                "/UI/Login";
        logoutURL = protocol + ":" + "//" + host + ":" + port + uri +
                "/UI/Logout";
        fmadmURL = protocol + ":" + "//" + host + ":" + port + uri;
        fmadm = new FederationManager(fmadmURL);
        famMsgBdl = ResourceBundle.getBundle("delegation" + fileseparator +
                DELEGATION_GLOBAL);
    }
    
    /**
     * This function adds privileges to identity using famadm.jsp
     * @param ssoToken ssotoken of the identity
     * @param idName identity Name
     * @param idType identity Type
     * @param realmName Realm name in which the identity exists
     * @param privileges List of privileges to add
     * @return true if privileges added successfully
     */
    public boolean addPrivileges(SSOToken ssotoken, String idName,
            IdType idType,
            String realmName,
            List privileges)
    throws Exception {
        boolean status = false;
        try {
            webClient = new WebClient();
            consoleLogin(webClient, loginURL, adminUser, adminPassword);
            if (privileges != null) {
                HtmlPage addPrivilegesPage ;
                addPrivilegesPage = fmadm.addPrivileges(webClient, realmName,
                        idName, idType.getName(), privileges);
                if (FederationManager.getExitCode(addPrivilegesPage) != 0) {
                    log(Level.SEVERE, "addPrivileges",
                            "addPrivilages famadm command failed");
                    assert false;
                }
                if (getHtmlPageStringIndex(addPrivilegesPage,
                        getMsg(PRIV_ADD_SUCCESS_MSG)) != -1) {
                    status = true;
                    log(Level.FINEST, "addPrivileges", "Privilege " +
                            privileges.toString() + " are added successfully");
                } else if (getHtmlPageStringIndex(addPrivilegesPage,
                        getMsg(ALREADY_HAVE_PRIV_MSG)) != -1) {
                    status = true;
                    log(Level.FINEST, "addPrivileges", "Privilege " +
                            privileges.toString() + " already exists");
                } else {
                    log(Level.SEVERE, "addPrivileges",
                            "Failed to add privilege " + privileges.toString());
                }
            } else {
                log(Level.FINE, "addPrivileges", "No privilege is specified");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            throw ex;
        } finally {
            consoleLogout(webClient, logoutURL);
        }
        return status;
    }
    
    /**
     * This function removes privileges to identity using famadm.jsp
     * @param ssoToken ssotoken of the identity
     * @param idName identity Name
     * @param idType identity Type
     * @param realmName Realm name in which the identity exists
     * @param privileges List of privileges to remove
     * @return true if the privileges are removed.
     */
    public boolean removePrivileges(SSOToken ssotoken, String idName,
            IdType idType,
            String realmName,
            List privileges)
    throws Exception {
        boolean status = false;
        try {
            webClient = new WebClient();
            consoleLogin(webClient, loginURL, adminUser, adminPassword);
            if (privileges != null){
                HtmlPage removePrivilegesPage ;
                removePrivilegesPage = fmadm.removePrivileges(webClient,
                        realmName, idName, idType.getName(), privileges);
                if (FederationManager.getExitCode(removePrivilegesPage) != 0) {
                    log(Level.SEVERE, "removePrivileges",
                            "removePrivilages famadm command failed");
                    assert false;
                }
                if (getHtmlPageStringIndex(removePrivilegesPage,
                        getMsg(PRIV_REMOVE_SUCCESS_MSG)) != -1) {
                    status = true;
                    log(Level.FINEST, "removePrivileges", "Privilege " +
                            privileges.toString() +
                            " are removed successfully");
                } else {
                    log(Level.SEVERE, "removePrivileges",
                            "Failed to remove privilege " +
                            privileges.toString());
                }
            } else {
                log(Level.FINE, "removePrivileges",
                        "No privilege is specified");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            throw ex;
        } finally {
            consoleLogout(webClient, logoutURL);
        }
        return status;
    }
    
    /**
     * This function shows privileges for identity using famadm.jsp
     * @param ssoToken ssotoken of the identity
     * @param idName identity Name
     * @param idType identity Type
     * @param realmName Realm name in which the identity exists
     * @return true if privileges shown.
     */
    public boolean showPrivileges(SSOToken ssotoken, String idName,
            IdType idType,
            String realmName,
            String testPrivileges)
    throws Exception {
        boolean status = false;
        try {
            webClient = new WebClient();
            consoleLogin(webClient, loginURL, adminUser, adminPassword);
            HtmlPage showPrivilegesPage ;
            showPrivilegesPage = fmadm.showPrivileges(webClient, realmName,
                    idName, idType.getName());
            if (FederationManager.getExitCode(showPrivilegesPage) != 0) {
                log(Level.SEVERE, "showPrivileges",
                        "showPrivilages famadm command failed");
                assert false;
            }
            if(getHtmlPageStringIndex(showPrivilegesPage,
                    testPrivileges) != -1) {
                status = true;
            } else {
                log(Level.SEVERE, "showPrivileges",
                        "Couldn't find privilege " + testPrivileges);
                status = false;
            }
            
        } catch (Exception ex) {
            ex.printStackTrace();
            throw ex;
        } finally {
            consoleLogout(webClient, logoutURL);
        }
        return status;
    }
    
    /**
     * This method adds privileges to identity by converting the prvileges to
     * a List.
     * @param idName identity Name
     * @param idType identity Type
     * @param privileges privileges to be added
     * @param ssoToken admin SSOToken
     * @param realmName Realm name in which identity exists.
     * @return true if privileges added successfully
     */
    public boolean addPrivilegesToId(String idName, String idType,
            String privileges,
            SSOToken ssoToken,
            String realmName)
    throws Exception {
        List privList = getAttributeList(privileges,
                DelegationConstants.IDM_KEY_SEPARATE_CHARACTER);
        return addPrivileges(ssoToken, idName, getIdType(idType),
                realmName, privList);
    }
    
    /**
     * This method removes privileges to identity
     * @param idName identity Name
     * @param idType identity Type
     * @param privileges privileges to be remvoed
     * @param SSOToken admin SSOToken
     * @param realmName realm in which identityExists.
     * @return true if privileges removed successfully
     */
    public boolean removePrivilegesFromId(String idName, String idType,
            String privileges,
            SSOToken ssoToken,
            String realmName)
    throws Exception {
        List privList = getAttributeList(privileges,
                DelegationConstants.IDM_KEY_SEPARATE_CHARACTER);
        return removePrivileges(ssoToken, idName, getIdType(idType),
                realmName, privList);
    }
    
    /**
     * This method will delete the realms recursively
     * @param adminSSOToken SSO token for
     * @return true if the deletion is success.
     */
    public boolean deleteRealmsRecursively(SSOToken adminSSOToken)
    throws Exception {
        boolean status = false;
        try {
            boolean recursive = true ;
            OrganizationConfigManager ocm = new OrganizationConfigManager(
                    adminSSOToken, realm);
            Set results = ocm.getSubOrganizationNames("*", recursive);
            log(Level.FINEST, "deleteRealmsRecursively", "Found realms: "
                    + results);
            Object[] realms = results.toArray();
            webClient = new WebClient();
            consoleLogin(webClient, loginURL, adminUser, adminPassword);
            HtmlPage deleteRealmsPage ;
            for (int i = realms.length-1; i >= 0; i--) {
                String delRealmName = realms[i].toString();
                deleteRealmsPage =
                        fmadm.deleteRealm(webClient, delRealmName, recursive);
                if (FederationManager.getExitCode(deleteRealmsPage) != 0) {
                    log(Level.SEVERE, "deleteRealms",
                            "deleteRealms famadm command failed ");
                    status = false;
                }
                log(Level.FINE, "deleteRealmsRecursively", "Realm: " +
                        delRealmName);
                if (getHtmlPageStringIndex(deleteRealmsPage,
                        getMsg(REALM_DEL_MSG)) != -1) {
                    status = true;
                } else {
                    status = false;
                }
            }
        } catch (Exception e) {
            log(Level.SEVERE, "deleteRealmsRecursively",
                    "Error deleting realm.");
            e.printStackTrace();
            throw e;
        } finally {
            consoleLogout(webClient, logoutURL);
        }
        return status;
    }
    
    /**
     * This method assigns service to the User
     * @param adminSSOToken Admin user ssotoken
     * @param userName User Name
     * @param serviceName Service Name
     * @param attrMap Attribute value pair
     * @param delegationReal User realm name
     * @return true if service is assigned else return false.
     */
    public boolean assignServiceToUser(SSOToken adminSSOToken,
            String userName,
            String serviceName,
            Map attrMap,
            String delegationRealm) 
    throws Exception {
        boolean status = false;
        try {
            AMIdentity amId = new AMIdentity(adminSSOToken, userName,
                    IdType.USER, delegationRealm, null);
            log(Level.FINE, "assignServiceToUser", "Assign " + serviceName +
                    " to user " + userName);
            amId.assignService(serviceName, attrMap);
            status = isServiceAssigned(amId, serviceName);
            if (status) {
                log(Level.FINE, "assignServiceToUser", "Service assigned " +
                        "successfully ");
            } else {
                log(Level.SEVERE, "assignServiceToUser", "Assigning service " +
                        " failed.");
            }
        } catch (Exception ex) {
            log(Level.SEVERE, "assignServiceToUser",
                    "Error assigning services to Id " + ex.getMessage());
            throw ex;
        }
        return status;
    }
    
    /**
     * This method finds if the service is assigned to user or not
     * @param amId AMIdentity
     * @param serviceName Service Name
     * @return true if service is assigned else false.
     */
    public boolean isServiceAssigned(AMIdentity amId, String serviceName) 
    throws Exception {
        Set assignedServices = amId.getAssignedServices();
        log(Level.FINEST, "isServiceAssigned", "List of services for " +
                amId.getName() + " : " + assignedServices.toString());
        Iterator serviceItr = assignedServices.iterator();
        String avServiceName = null;
        boolean status = false;
        while (serviceItr.hasNext()) {
            avServiceName = (String)serviceItr.next();
            if (avServiceName.equals(serviceName)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * This method removes service from the given user
     * @param adminSSOToken Admin user ssotoken
     * @param userName User Name
     * @param serviceName Service Name
     * @param delegationRealm User realm name
     * @return true if service is removed else return false.
     */
    public boolean unAssignServiceFromUser(SSOToken adminSSOToken,
            String userName,
            String serviceName,
            String delegationRealm) 
    throws Exception {
        boolean status = true;    
        try {
            AMIdentity amId = new AMIdentity(adminSSOToken, userName, 
                    IdType.USER, delegationRealm, null);
            log(Level.FINE, "unAssignServiceFromUser", "Unassign " + 
                    serviceName + " from user " + userName);
            amId.unassignService(serviceName);
            status = isServiceAssigned(amId, serviceName);
            if (!status) {
                log(Level.FINE, "unAssignServiceFromUser", 
                        "Service unassigned successfully ");
            } else {
                log(Level.SEVERE, "unAssignServiceFromUser", 
                        "Unassigning service failed.");
            }
        } catch (Exception ex) {
            log(Level.SEVERE, "unAssignServiceFromUser",
                    "Error unassigning services from Id " + ex.getMessage());
            throw ex;
        }
        return (!status);
    }
    
    /**
     * This method modifys service attributes for the given user
     * @param adminSSOToken Admin user ssotoken
     * @param userName User Name
     * @param serviceName Service Name
     * @param attrMap Service attribute value map.
     * @param delegationRealm Parent Realm
     * @return true if service is removed else return false.
     */
    public boolean modifyUsersAssignedService(SSOToken adminSSOToken,
            String userName,
            String serviceName,
            Map attrMap,
            String delegationRealm) 
    throws Exception {
        boolean status = false;
        try {
            AMIdentity amId = new AMIdentity(adminSSOToken, userName,
                    IdType.USER, delegationRealm, null);
            log(Level.FINEST, "modifyUsersAssignedService", "Modify " +
                    serviceName + " for user " + userName + " with values " +
                    attrMap);
            amId.modifyService(serviceName, attrMap);
            status = isServiceValuesEqual(amId, serviceName, attrMap);
            if (status) {
                log(Level.FINE, "modifyUsersAssignedService", "Service " +
                        "modified successfully ");
            } else {
                log(Level.SEVERE, "modifyUsersAssignedService",
                        "Service Modification failed.");
            }
        } catch (Exception ex) {
            log(Level.SEVERE, "modifyUsersAssignedService",
                    "Error modfifying service. " + ex.getMessage());
            throw ex;
        }
        return status;
    }

    /**
     * This method gets service attributes for the given user
     * @param token Admin user ssotoken
     * @param userName User Name
     * @param serviceName Service Name
     * @param attrMap Service attribute value map.
     * @param delegationRealm Parent Realm
     * @return true if service is removed else return false.
     */
    public Map getServiceAttrsOfUser(SSOToken token,
            String userName,
            String serviceName,
            String delegationRealm)
    throws Exception {
        AMIdentity amId = new AMIdentity(token, userName,
                    IdType.USER, delegationRealm, null);
        if(isServiceAssigned(amId, serviceName)) {
            return amId.getServiceAttributes(serviceName);
        } else {
            log(Level.SEVERE, "getServiceAttrsOfUser", "Service " +
                        "not assigned to user " + userName);
            return null;
        } 
    }
    
    /**
     * This method finds if the service have the same values as attrMap
     * @param amId AMIdentity
     * @param serviceName Service Name
     * @param attrMAp Service attribute value map
     * @return true if the service have the same values as attrMap
     */
    public boolean isServiceValuesEqual(AMIdentity amId, String serviceName,
            Map attrMap)
    throws Exception {
        try {
            Map attValMap = amId.getServiceAttributes(serviceName);
            log(Level.FINEST, "isServiceValuesEqual",
                    "Service attributes after update " + attValMap);
            boolean equal;
            if ((attValMap != null) && (attrMap != null)) {
                Set updatedKeys = attrMap.keySet();
                Iterator itr1 = updatedKeys.iterator();
                while (itr1.hasNext()) {
                    String key = (String)itr1.next();
                    Set val1Set = (Set)attrMap.get(key);
                    Set val2Set = (Set)attValMap.get(key);
                    equal = val1Set.equals(val2Set);
                    if (!equal) {
                        return false;
                    }
                }
            } else {
                return false;
            }
        } catch (Exception ex) {
            log(Level.SEVERE, "isServiceValuesEqual",
                    "Error validating attribute values " + ex.getMessage());
            throw ex;
        }
        return true;
    }
    
    /**
     * Returns message from the DelegationGlobal properties file.
     *
     * @return Message string.
     */
    private String getMsg(String key) {
        String paramKey = DELEGATION_GLOBAL + "." + key;
        return famMsgBdl.getString(paramKey);
    }
}
