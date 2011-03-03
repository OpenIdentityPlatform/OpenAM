/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: OpenSSOJACCPolicyConfiguration.java,v 1.2 2009/02/04 06:06:18 kalpanakm Exp $
 *
 */

package com.sun.opensso.agents.jsr115;


import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Iterator;
import java.util.Collection;
import java.util.Vector;
import java.util.StringTokenizer;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.Properties;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.net.URLDecoder;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpUtils;

import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.security.ProtectionDomain;
import java.security.Principal;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginException;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;

import javax.security.jacc.PolicyConfiguration;
import javax.security.jacc.PolicyContext;
import javax.security.jacc.PolicyContextException;

import com.sun.enterprise.deployment.interfaces.SecurityRoleMapper;
import com.sun.enterprise.deployment.interfaces.SecurityRoleMapperFactory;
import com.sun.enterprise.deployment.interfaces.SecurityRoleMapperFactoryMgr;

import com.sun.identity.policy.PolicyManager;
import com.sun.identity.policy.client.PolicyEvaluator;
import com.sun.identity.policy.client.PolicyEvaluatorFactory;
import com.sun.identity.policy.SubjectTypeManager;
import com.sun.identity.policy.Policy;
import com.sun.identity.policy.Rule;
import com.sun.identity.policy.NameAlreadyExistsException;
import com.sun.identity.policy.InvalidNameException;
import com.sun.identity.policy.NameNotFoundException;
import com.sun.identity.shared.debug.Debug;

import com.sun.identity.policy.PolicyException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.iplanet.sso.SSOException;
import com.iplanet.services.naming.WebtopNaming;
import com.sun.identity.authentication.AuthContext;

import com.sun.identity.sm.DNMapper;
import com.sun.identity.policy.plugins.AuthenticatedUsers;

import com.sun.identity.agents.arch.ServiceFactory;
import com.sun.identity.agents.arch.AgentConfiguration;
import com.sun.identity.agents.arch.IBaseModuleConstants;
import com.sun.identity.policy.plugins.AMIdentitySubject;

import com.sun.opensso.agents.jsr196.OpenSSORequestHandler;

/**
 * Implements the PolicyConfiguration interface.
 * This class is responsible for the policy management and evaluation of the 
 * policies. 
 * 
 * @author kalpana
 */
public class OpenSSOJACCPolicyConfiguration implements PolicyConfiguration {
        
    public static final int OPEN_STATE = 0;
    public static final int INSERVICE_STATE = 2;
    public static final int DELETED_STATE = 3;
    private static final Permission setPolicyPermission = 
            new java.security.SecurityPermission("setPolicy");
    private int state = OPEN_STATE;
    private static OpenSSORequestHandler _handler = null;
        
    private static String PROVIDER_URL = "policy.url.";
    
    private static String isUserName = null;       
    private static String isUserPassword = null;
          
    private static AuthContext lc = null;
    private static PolicyManager policyManager = null;
    private static SubjectTypeManager subjectTypeManager = null;
        
    private static String isOrganizationDN = null;        
    private static String isHostName = null;        
    private static String isPort = null;    
    private static String isPolicyAdmLoc = null;
    
    private static Object refreshLock = new Object();        
    
    private static SecurityRoleMapperFactory factory =
    SecurityRoleMapperFactoryMgr.getFactory();
        
    private static Debug _debug = null;  
    
    private static boolean isInitialized  = initializeISPolicies();
        
    private HashMap userDataConstraints = new HashMap();    
    private String CONTEXT_ID = null;    
    // Excluded permissions
    private PermissionCollection excludedPermissions = null;
    // Unchecked permissions
    private PermissionCollection uncheckedPermissions = null;
    // permissions mapped to roles.
    private HashMap rolePermissionsTable = null;
    
    private boolean wasRefreshed = false;
    private java.security.Policy policy = null;
    
    private boolean writeOnCommit = true;
            
    // Lock on this PolicyConfiguration onject
    private ReentrantReadWriteLock pcLock = new ReentrantReadWriteLock(true);
    private Lock pcrLock = pcLock.readLock();
    private Lock pcwLock = pcLock.writeLock();
    
    
    protected OpenSSOJACCPolicyConfiguration(String contextID){
         //id = contextID;
         CONTEXT_ID=contextID;    
         initialize(false, false);
    }
    
    protected OpenSSOJACCPolicyConfiguration(String contextID, boolean remove){
         //id = contextID;
         CONTEXT_ID=contextID;         
         initialize(false, remove);
    }
    
    /**
     * 
     * @return context id 
     * @throws javax.security.jacc.PolicyContextException
     */
    
    public String getContextID()
            throws javax.security.jacc.PolicyContextException {
        return CONTEXT_ID;
        
    }
    
    /**
     * 
     * The container calls addToExcludedPolicy to add excluded permissions
     * 
     * @param permissions a collection of permission objects
     * @throws javax.security.jacc.PolicyContextException
     */
    
    public void addToExcludedPolicy(PermissionCollection permissions)
            throws PolicyContextException {
         checkSetPolicyPermission();
        pcwLock.lock();
        try {
            assertStateIsOpen();
            if (permissions != null) {
                for (Enumeration e = permissions.elements();
                        e.hasMoreElements();) {
                    this.getExcludedPermissions().add(
                            (Permission) e.nextElement());
                }

            }
        } finally {
            pcwLock.unlock();
        }
    }
    
    /**
     * 
     * The container calls addToExcludedPolicy to add an excluded permission
     * 
     * @param permissions
     * @throws javax.security.jacc.PolicyContextException
     */
    
    
    public void addToExcludedPolicy(Permission permission)
            throws PolicyContextException {
         checkSetPolicyPermission();
        pcwLock.lock();
        try {
            assertStateIsOpen();
            if (permission != null) {
                getExcludedPermissions().add(permission);
            }

        } finally {
            pcwLock.unlock();
        }        
    }
    
    /**
     * 
     * The container calls addToUncheckedPolicy to add unchecked permissions
     * 
     * @param permissions - collection of permission objects
     * @throws javax.security.jacc.PolicyContextException
     */
    
    public void addToUncheckedPolicy(PermissionCollection permissions)
            throws PolicyContextException {
         checkSetPolicyPermission();
        pcwLock.lock();
        try {
            assertStateIsOpen();
            if (permissions != null) {
                for (Enumeration e = permissions.elements();
                        e.hasMoreElements();) {
                    this.getUncheckedPermissions().add(
                            (Permission) e.nextElement());
                }

            }
        } finally {
            pcwLock.unlock();
        }
    }
    
    /**
     * 
     * The container calls addToUncheckedPolicy to add unchecked permissions
     * 
     * @param permissions - collection of permission objects
     * @throws javax.security.jacc.PolicyContextException
     */
    
    public void addToUncheckedPolicy(Permission permission)
            throws PolicyContextException {
                    checkSetPolicyPermission();
        pcwLock.lock();
        try {
            assertStateIsOpen();
            if (permission != null) {
                getUncheckedPermissions().add(permission);
            }

        } finally {
            pcwLock.unlock();
        }        
    }  
    
    /**
     * 
     * The container calls the addToRole to add permission collection for a role
     * 
     * @param role to which the permission is applicable
     * @param permissions collection of permission objects
     * @throws javax.security.jacc.PolicyContextException
     */
    
    public void addToRole(String role, PermissionCollection permissions)
            throws PolicyContextException {
        pcwLock.lock();        
                        
        try {
            assertStateIsOpen();
            if (role != null && permissions != null) {
                checkSetPolicyPermission();
                for(Enumeration enum1 = permissions.elements(); enum1.hasMoreElements();) {
                    this.getRolePermissions(role).add((Permission)enum1.nextElement());                    
                }
             }
        }finally {
            pcwLock.unlock();
        }                
    }
    
    /**
     * 
     * The container calls the addToRole to add permission collection for a role
     * 
     * @param role to which the permission is applicable
     * @param permissions collection of permission objects
     * @throws javax.security.jacc.PolicyContextException
     */
    
    
    public void addToRole(String role, Permission permission)
            throws PolicyContextException {
        checkSetPolicyPermission();                
        pcwLock.lock();        
                        
        try {
            assertStateIsOpen();
            if (role != null && permission != null) {                
                this.getRolePermissions(role).add(permission);                                    
             }
        }finally {
            pcwLock.unlock();
        }
        
    }
    
    /**
     * 
     * @return whether the policyconfiguration is in service 
     * @throws javax.security.jacc.PolicyContextException
     */
    
    public boolean inService() 
            throws PolicyContextException {        
         pcrLock.lock();
        try {
            return stateIs(INSERVICE_STATE);
        } finally {
            pcrLock.unlock();
        }        
    }
    
    protected static boolean inService(String ctxid) 
            throws PolicyContextException {
        OpenSSOJACCPolicyConfiguration pc = SharedState.lookupConfig(ctxid);
        if (pc == null) {
            return false;
        }
        return pc.inService();
    }
    
    /**
     *  commit() is reponsible for writing the policies to the config store
     * 
     * @throws javax.security.jacc.PolicyContextException
     */
    
    public void commit()
            throws PolicyContextException {
        
        pcwLock.lock();
        try {
            if (_debug.messageEnabled()){
                _debug.message("JACCPC: Call to commit()");
            }
                
            if(state == DELETED_STATE){
                String defMsg="Cannot perform Operation on a deleted PolicyConfiguration";
                throw new UnsupportedOperationException(defMsg);
            } else {             
                checkSetPolicyPermission();
                if (state == OPEN_STATE) {
                    generatePermissions();
                    state = INSERVICE_STATE;
                 }               
            } 
            
        } catch (Exception e) {
            throw new PolicyContextException(e);
        }       
        finally {
            pcwLock.unlock();
        }
    }
       
    /**
     * The containers calls delete() when there is a need to remove the policy 
     * @throws javax.security.jacc.PolicyContextException
     */
    
    public void delete() 
             throws PolicyContextException {        
        checkSetPolicyPermission();
        synchronized(refreshLock) {
            try {
                removePolicy();
            } finally {
                state = DELETED_STATE;
            }
        }
    }
    
    /**
     * @see javax.security.jacc.PolicyConfiguration
     * 
     * @param link
     * @throws javax.security.jacc.PolicyContextException
     */
    
    public void linkConfiguration(PolicyConfiguration link)
             throws PolicyContextException {
        checkSetPolicyPermission();
        pcrLock.lock();
        try {
            assertStateIsOpen();
        } finally {
            pcrLock.unlock();
        }
        /* if at this point, a simultaneous attempt is made to delete or commit 
         * this pc, we could end up with a corrupted link table. Niether event
         * is likely, but we should try to properly serialize those events.
         */

        SharedState.link(CONTEXT_ID, link.getContextID());       
    }

    /**
     * 
     * @see javax.security.jacc.PolicyConfiguration
     * 
     * do nothing.
     * 
     * @throws javax.security.jacc.PolicyContextException
     */
    
    public void removeExcludedPolicy()
            throws PolicyContextException {
        
    }
    
    /**
     * 
     * @see javax.security.jacc.PolicyConfiguration
     * 
     * do nothing.
     * 
     * @throws javax.security.jacc.PolicyContextException
     */
    
    public void removeUncheckedPolicy() 
            throws PolicyContextException {
        
    }
    
    /**
     * 
     * @see javax.security.jacc.PolicyConfiguration
     * 
     * do nothing.
     * 
     * @throws javax.security.jacc.PolicyContextException
     */
    
    public void removeRole(String role)
            throws PolicyContextException {
        
    }  
    
    /**
     * 
     * doImplies() is responsible for evaluation of policies against
     * OpenSSO Enterprise
     * 
     * 
     * @param pd
     * @param p
     * 
     * @return true if the request has permission to proceed, else
     * false to deny the request
     */
    
     boolean doImplies(ProtectionDomain pd, Permission p) {
         
        
         boolean isAllow = true;
         Principal[] prin = pd.getPrincipals();        
        
         try {
             HttpServletRequest pl = 
                     (HttpServletRequest) PolicyContext.getContext("javax.servlet.http.HttpServletRequest");        
        
             boolean cont = _handler.shouldContinue();        
             if (_debug.messageEnabled()){
                 _debug.message("JACCPC: Should continue with authorization " +
                         "evaluation :: " + !cont);
             }
             
            if (!cont) {
                SSOTokenManager ssoManager = SSOTokenManager.getInstance();
                SSOToken token = ssoManager.createSSOToken(pl);
                
                StringBuffer resourceName = HttpUtils.getRequestURL(pl);
                String action = pl.getMethod();                
                
                PolicyEvaluatorFactory pef = PolicyEvaluatorFactory.getInstance();
                PolicyEvaluator pe = pef.getPolicyEvaluator("iPlanetAMWebAgentService",
                        ServiceFactory.getAmWebPolicyAppSSOProvider(_handler.getFilter().getManager()));
                
                isAllow = pe.isAllowed(token, resourceName.toString(), action);
                if (_debug.messageEnabled()){
                    _debug.message("JACCPC: isAllow :: " + isAllow);
                    _debug.message("JACCPC: Resource Name:: " + resourceName.toString());
                }                
            }
        } catch (SSOException soe) {
            soe.printStackTrace();
        } catch (Exception e) {
            if (_debug.errorEnabled()) {
                _debug.error("JACCPC: Exception thrown :: " + e.getMessage());             
            }
            e.printStackTrace();
        }
        
        return isAllow;      
    }        
    
    protected static OpenSSOJACCPolicyConfiguration getPolicyConfig(
            String pcid, boolean remove) throws PolicyContextException {                       

        OpenSSOJACCPolicyConfiguration pc = SharedState.getConfig(pcid, remove);
        pc.pcwLock.lock();
        try {
            if (remove) {
                pc.removePolicy();
            }
            pc.setState(OPEN_STATE);
        } finally {
            pc.pcwLock.unlock();
        }

        return pc;
    }
    
    protected static Collection getPolicyConfigurationCollection() 
            throws PolicyContextException {
        return SharedState.getConfigCollection();        
    }
    
    protected static void checkSetPolicyPermission() {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(setPolicyPermission);
        }

    }
    
    private void setState(int stateValue) {
        this.state = stateValue;
    }

    private boolean stateIs(int stateValue) {
        return this.state == stateValue;
    }

    private void assertStateIsOpen() throws UnsupportedOperationException {
        if (!stateIs(OPEN_STATE)) {
            String msg = "Operation invoked on closed or deleted PolicyConfiguration.";
            throw new UnsupportedOperationException(msg);
        }
    }

    private void assertStateIsInService() throws UnsupportedOperationException {
        if (!stateIs(INSERVICE_STATE)) {
            String msg = "Operation invoked on open or deleted PolicyConfiguration.";
            throw new UnsupportedOperationException(msg);
        }
    }
    
    /**
     * Removes the policy from the config store.      
     * 
     */
    
    private void removePolicy() {
        excludedPermissions = null;
        uncheckedPermissions = null;
        rolePermissionsTable = null;
        SSOToken ssoToken1 = null;
        
        // retrieve all policyNames with the pattern "contextId*"
        // this includes contextId.unchecked, contextId.excluded and
        // contextId.RoleName(if they exists)
        try{
            String pattern = AgentConfiguration.getApplicationUser()+ CONTEXT_ID + ".*";              
            pattern = URLEncoder.encode(pattern);
            // authenticate and get single sign-on token from idenity server
            ssoToken1 = getSSOToken();
            // create policy manager
            PolicyManager policyManger1 =
            new PolicyManager(ssoToken1, isOrganizationDN);
            
            Set policyNameSet = policyManger1.getPolicyNames(pattern);
            String policyName=null;
            if (!policyNameSet.isEmpty()){
                Iterator policyNameIterator = policyNameSet.iterator();
                
                //iterate through all policyNames
                while ( policyNameIterator.hasNext()){
                    policyName = (String) policyNameIterator.next();
                    //remove policy
                    if (_debug.messageEnabled()){
                        _debug.message("JACCPC: Removing Policy :: " + policyName);
                    }
                    policyManger1.removePolicy(policyName);
                }
            }
        }catch (PolicyException pe){
            pe.printStackTrace();
            if(_debug.errorEnabled()){
                _debug.error("JACCPC: Got PolicyException ..." + pe);
            }
        }catch (SSOException ssoe){
            ssoe.printStackTrace();
            if (_debug.errorEnabled()) {
                _debug.error("JACCPC: SSOException in removePolicy ..." + ssoe.getMessage());
            }
        } finally {
            if (ssoToken1 != null) {
                destroySSOToken(ssoToken1);
            }
        }
       // SharedState.initLinkTable();
        policy = null;
        writeOnCommit = true;
    }
    
    private PermissionCollection getUncheckedPermissions() {
        if (uncheckedPermissions == null) {
            uncheckedPermissions = new Permissions();
        }
        return uncheckedPermissions;
    }

    private PermissionCollection getExcludedPermissions() {
        if (excludedPermissions == null) {
            excludedPermissions = new Permissions();
        }
        return excludedPermissions;
    }                  
    
    private Permissions getRolePermissions(String roleName) {
        if (rolePermissionsTable == null) rolePermissionsTable = new HashMap();
        Permissions rolePermissions = (Permissions) rolePermissionsTable.get(roleName);
        if (rolePermissions == null) {
            rolePermissions = new Permissions();
            rolePermissionsTable.put(roleName,rolePermissions);
        }
        return rolePermissions;
    }
    
    /**
     * generatePermissions() is responsible for converting j2EE policies into 
     * OpenSSO format and storing into the config store via Policy Manager
     * 
     * 
     * @throws java.io.FileNotFoundException
     * @throws java.io.IOException
     */
    
        
    private void generatePermissions()
    throws java.io.FileNotFoundException, java.io.IOException {
        SSOToken ssoTok = null;
                    
        // optimization - return if the rules have not changed
        
        if (_debug.messageEnabled()){
            _debug.message("JACCPC: In generatePermissions() ...");
        }
      
        if (!writeOnCommit) return;        
        
        if (_debug.messageEnabled()){
            _debug.message("JACCPC: In generatePermissions() .. ContextID::" + CONTEXT_ID);
        }
        
        
        // otherwise proceed to write policy file
       Map roleToSubjectMap = null;
        if (rolePermissionsTable != null) {
            // Make sure a role to subject map has been defined for the Policy Context
            if ( factory == null ) {
                factory = SecurityRoleMapperFactoryMgr.getFactory();
            }
            if (factory != null) {
                SecurityRoleMapper srm = factory.getRoleMapper(CONTEXT_ID);
                if (srm != null) {
                    roleToSubjectMap = srm.getRoleToSubjectMapping();
                }
                if (roleToSubjectMap != null) {
                    // make sure all liked PC's have the same roleToSubjectMap
                    Set linkSet = (Set) SharedState.getLink(CONTEXT_ID);
                    if (linkSet != null) {
                        Iterator it = linkSet.iterator();
                        while (it.hasNext()) {
                            String contextId = (String)it.next();
                            if (!CONTEXT_ID.equals(contextId)) {
                                SecurityRoleMapper otherSrm = factory.getRoleMapper(contextId);
                                Map otherRoleToSubjectMap = null;
                                
                                if (otherSrm != null) {
                                    otherRoleToSubjectMap = otherSrm.getRoleToSubjectMapping();
                                }
                                
                                if (otherRoleToSubjectMap != roleToSubjectMap) {
                                    if (_debug.errorEnabled()){
                                        _debug.error("There are more than one mappers " +
                                                "available for the same CONTEXT_ID");
                                    }
                                    throw new RuntimeException(
                                    "Linked policy contexts have different roleToSubjectMaps ("+CONTEXT_ID+")<->("+contextId+")");
                                }
                            }
                        }
                    }
                }
            }
        }
        
        
        try{
            String policyName =null;
            String[] resourceName =null;
            Policy isPolicy =null;
            Rule rule =null;
            int ruleSuffix=0;
            
            // authenticate and get single sign-on token from idenity server
            
            ssoTok = getSSOToken();
            
            // create policy manager
            PolicyManager polMan =
            new PolicyManager(ssoTok, isOrganizationDN);
            
            SubjectTypeManager stm = polMan.getSubjectTypeManager();
                       
            // write  unchecked policy into identity server
            try{
            if (uncheckedPermissions != null) {
                
                policyName = AgentConfiguration.getApplicationUser()+ CONTEXT_ID+".unchecked";                        
                policyName = URLEncoder.encode(policyName);
                
                isPolicy = new Policy(policyName, policyName);
                Enumeration pEnum = uncheckedPermissions.elements();
                
                while (pEnum.hasMoreElements()) {
                    Permission p = (Permission) pEnum.nextElement();
                    
                    if (p instanceof javax.security.jacc.WebUserDataPermission) {
                        // do not create thr rule now, just extract and keep the data
                        // in a map
                        // While creating the role rules, make sure there is no
                        // access to the resource ... remove if its there ...
                        storeUserDataRule(p, "UC"+ ruleSuffix++);                        
                        continue;
                    } else {       
                        if (p.getActions() != null && p.getActions().startsWith("!"))
                            continue;
                        rule = createRule(p, ruleSuffix++);
                    }
                    
                    try {
                    if (rule != null)
                        isPolicy.addRule(rule);
                    } catch (com.sun.identity.policy.NameAlreadyExistsException ex){
                        // Just log. Do nothing.
                        if (_debug.warningEnabled()){
                            _debug.warning("JACCPC: Rule already present in the policy");
                        }
                    }
                }        
                                                
                // The unchecked policies are stored with subject as AuthenticatedUsers.
                AuthenticatedUsers sub = new AuthenticatedUsers();                                                                             
                
                isPolicy.addSubject(policyName, sub);
                         
                if (_debug.messageEnabled()) {
                    _debug.message("JACCPC: Adding Policy : " + policyName);
                }
                //add policy to policyManager                
                polMan.addPolicy(isPolicy);
            } 
            }catch(Exception unex) {
              if (_debug.errorEnabled()){
                  _debug.error("JACCPC: Error in handling unchecked permissions ...");
              }
            //Cannot write unchecked policy
              unex.printStackTrace();
            }
                
            
            // write  role based policy into identity server
            try {
            if (rolePermissionsTable != null) {
                Iterator roleIt = rolePermissionsTable.keySet().iterator();
                //PolicyManager pm = new PolicyManager(getAdminToken(), isOrganizationDN);
           
                while (roleIt.hasNext()) {
                    boolean withPrincipals = false;
                    String roleName = (String) roleIt.next();
                    Permissions rolePerms = getRolePermissions(roleName);
                    javax.security.auth.Subject rolePrincipals =
                    (javax.security.auth.Subject) roleToSubjectMap.get(roleName);
                    if (rolePrincipals != null) {
                        Iterator pit = rolePrincipals.getPrincipals().iterator();
                        while (pit.hasNext()){
                            Principal prin = (Principal) pit.next();
                            assert prin instanceof java.security.Principal;
                            policyName = AgentConfiguration.getApplicationUser()+CONTEXT_ID+"."+prin.getName();
                            /*policyName = policyName.replace("/", "KK" );                        
                            int idx = -1;
                            if ((idx = policyName.indexOf(",")) != -1) {                            
                                policyName = policyName.substring(0, idx);
                                policyName = policyName.replace("=", "LL");                            
                            }*/
                            int idx = -1;
                            if ((idx = policyName.indexOf(",")) != -1) {                            
                                policyName = policyName.substring(0, idx);
                            }
                            policyName = URLEncoder.encode(policyName);
                
                            isPolicy = new Policy(policyName, policyName);
                            if (prin instanceof java.security.Principal) {
                                withPrincipals = true;
                                Enumeration pEnum = rolePerms.elements();
                                ruleSuffix=0;                                
                                while (pEnum.hasMoreElements()) {
                                    rule = null;
                                    Permission perm = (Permission) pEnum.nextElement();
                                    if (perm instanceof javax.security.jacc.WebUserDataPermission) {
                                            storeUserDataRule(perm, prin.getName() + ruleSuffix++);                        
                                            continue;
                                    } else {                                                                          
                                            String resourceName1 = perm.getName();                                            
                                            if (userDataConstraints.containsKey(resourceName1)) {
                                                rule = (Rule) userDataConstraints.get(resourceName1);
                                                //userDataConstraints.remove(refreshLock)
                                            }
                                            else
                                                rule = createRule(perm, ruleSuffix++);
                                    }
                                    try {
                                        if (rule != null)
                                            isPolicy.addRule(rule);
                                     } catch (com.sun.identity.policy.NameAlreadyExistsException ex){
                                         if (_debug.errorEnabled()){
                                               _debug.error("JACCPC: Error in handling unchecked permissions ...");
                                         }                                                    
                                     }
                                  }
                                }
                                                                
                                AMIdentitySubject sub = new AMIdentitySubject();                                
                                Set validValues = new HashSet();                                
                                validValues.add(prin.getName());
                                sub.setValues(validValues);                                
                                
                             //   String type = stm.getSubjectTypeName(sub);
                                
                                isPolicy.addSubject(policyName, sub);
                                                                
                                if(_debug.messageEnabled()){
                                    _debug.message("JACCPC: Adding policy ::" + policyName);
                                }
                            polMan.addPolicy(isPolicy);
                        }
                    }                                                                      
                    }
                 writeOnCommit = false;
            }                        
            }catch(Exception roleex){
                if (_debug.errorEnabled()){
                  _debug.error("JACCPC: Error in handling role permissions ...");
                }
                roleex.printStackTrace();                
            }
                      
            // write excluded policies into Identity server
            try {
            if (excludedPermissions != null) {
                Enumeration pEnum = excludedPermissions.elements();
                policyName = AgentConfiguration.getApplicationUser()+CONTEXT_ID+".excluded";                
                policyName = URLEncoder.encode(policyName);
            
                isPolicy = new Policy(policyName, policyName);
                
                ruleSuffix=0;
                while (pEnum.hasMoreElements()) {
                    rule = null;
                    Permission p = (Permission) pEnum.nextElement();
                    if (p instanceof javax.security.jacc.WebUserDataPermission) {
                        storeUserDataRule(p, "EX" + ruleSuffix++);                        
                    } else {                                                                          
                        Map actions = new HashMap(1);                        
                        Set deny = new HashSet(1);
                        deny.add("deny");
         
                        String pAction = p.getActions();
                        String[] tokens = null;
                        if (pAction != null) {                        
                            tokens = pAction.split(",");
            
                            for(int i =0; i < tokens.length; i++) {
                            if (tokens[i].equals("GET") || tokens[i].equals("POST"))                        
                                actions.put(tokens[i], deny);                       
                            }
                        } else {
                            // When no actions are defined, then it means all the HTTP Methods are allowed.         
                            actions.put("GET", deny);
                            actions.put("POST", deny);
                         }
                        
                        if (!actions.isEmpty())
                        {         
                        resourceName = getResourceName(p);
                        for (int i=0;i<1; i++){
                                System.out.println("Resource Name ->" + resourceName[i]);
                                try{
                                    rule = new Rule("Rule" + ruleSuffix ,
                                    "iPlanetAMWebAgentService", resourceName[i], actions);
                                    } catch (Exception e) {
                                            throw e;
                                    }
                        }
                        }                       
                    try {
                        if (rule != null)
                            isPolicy.addRule(rule);
                    } catch (com.sun.identity.policy.NameAlreadyExistsException ex){
                           if (_debug.warningEnabled()){
                                 _debug.warning("JACCPC: Same rule exists in excluded set ...");
                           }                                                                                       
                    }                                
                    }
                }
                 //add policy to policyManager
                polMan.addPolicy(isPolicy);
               
                }
                // set writeOnCommit to "false"
                writeOnCommit = false;                
            }catch(Exception excex) {                
                if (_debug.errorEnabled()){
                    _debug.error("JACCPC: Error in handling excluded permissions ...");
                    }             
                excex.printStackTrace();
            }         
        }catch(SSOException ssoe){
            if (_debug.errorEnabled()){
                _debug.error("JACCPC: got SSOException in generatePermissions() : " + ssoe.getMessage());
            }             
            ssoe.printStackTrace();
            writeOnCommit = true;
        }catch(InvalidNameException ine){
             if (_debug.errorEnabled()){
                _debug.error("JACCPC: got InvalidNameException in generatePermissions() : " + ine.getMessage());
             } 
             ine.printStackTrace();
             writeOnCommit = true;
        }catch(NameNotFoundException nnfe){
            if (_debug.errorEnabled()){
                _debug.error("JACCPC: got NameNotFoundException in generatePermissions() : " + nnfe.getMessage());
             }
            nnfe.printStackTrace();
            writeOnCommit = true;
        }catch(NameAlreadyExistsException naee){
             if (_debug.errorEnabled()){
                _debug.error("JACCPC: got NameAlreadyExistsException in generatePermissions() : " + naee.getMessage());
             }           
            naee.printStackTrace();
            writeOnCommit = true;
        }catch(PolicyException pe){
            if (_debug.errorEnabled()){
                _debug.error("JACCPC: got PolicyException in generatePermissions() : " + pe.getMessage());
             }
            pe.printStackTrace();
            writeOnCommit = true;
        } finally {
            if (ssoTok != null) {
                destroySSOToken(ssoTok);
            }
        }
       
        if (!writeOnCommit) wasRefreshed = false;
    }
    
     static SSOToken getSSOToken() {                  
        
        authenticate();
        SSOToken token = null;
        try {
            token = lc.getSSOToken();
        } catch (Exception e) {
            e.printStackTrace();
        }        
        return token;
        
    }    
     
    static void destroySSOToken(SSOToken token) {
        try {
            if (token != null){   
                SSOTokenManager manager = SSOTokenManager.getInstance();
                manager.destroyToken(token);            
            }
        } catch (SSOException soe){
            soe.printStackTrace();
        }
    }
    
    private static void authenticate() {
        try {
            URL isURL = new URL(WebtopNaming.getLocalServer());
            String url = WebtopNaming.getLocalServer();
            lc = null;
            lc = new AuthContext(isOrganizationDN,isURL);
            lc.login();
            Callback[] callbacks = null;
            // Get the information requested by the plug-ins
            while (lc.hasMoreRequirements()) {
        
                callbacks = lc.getRequirements();
                if (callbacks != null) {
                    addLoginCallbackMessage(callbacks);
                    lc.submitRequirements(callbacks);
                    
                    if (lc.getStatus() == AuthContext.Status.SUCCESS) {
                        Subject authSubject = lc.getSubject();
                        if ( authSubject != null) {
                            Iterator principals =
                            (authSubject.getPrincipals()).iterator();
                            Principal principal;
                            while (principals.hasNext()) {
                                principal = (Principal) principals.next();
                            }
                        }
                    } else if (lc.getStatus() == AuthContext.Status.FAILED) {
                        if (_debug.errorEnabled()){
                            _debug.error("JACCPC: authenticate():: Authentication failed");
                        }
                    }else {
                        if (_debug.errorEnabled()){
                            _debug.error("JACCPC: authenticate():: Unknown status");
                        }                        
                    }
                }else{
                    if (_debug.messageEnabled()){
                            _debug.message("JACCPC: authenticate():: more Callback requirements");
                        }                    
                }
            }
        } catch (LoginException le) {
            _debug.error("Exception thrown:" + le.getMessage());
            return;
        } catch (Exception e) {
            _debug.error("JACC: Authenticate():: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void addLoginCallbackMessage(Callback[] callbacks)
    throws UnsupportedCallbackException {
        String user = null, password = null;
        FileInputStream fin = null;
        try {
            fin = new FileInputStream(isPolicyAdmLoc);
            Properties prop = new Properties();
            prop.load(fin);
            user = prop.getProperty("com.sun.identity.agent.policyadmin.username");
            password = prop.getProperty("com.sun.identity.agent.policyadmin.pwd");
        } catch (FileNotFoundException fe) {
            if (_debug.errorEnabled()){                
                _debug.error("JACCPC:Policy Admin Details not found .... Policy Management Fails");
            }
        } catch (IOException ioe) {
            if (_debug.errorEnabled()){
                _debug.error("JACCPC:IOException ...Loading the policyAdmin details failed");
            }
        }finally {
            try {
                if (fin != null)
                    fin.close();
            } catch (Exception e){
                e.printStackTrace();
            }
        }              
        
        
        int i = 0;
        try {
            for (i = 0; i < callbacks.length; i++) {
                if (callbacks[i] instanceof NameCallback) {
                    // prompt the user for a username
                    NameCallback nc = (NameCallback) callbacks[i];
                    nc.setName(user);
                    
                } else if (callbacks[i] instanceof PasswordCallback) {
                    // prompt the user for sensitive information
                    PasswordCallback pc = (PasswordCallback) callbacks[i];
                    pc.setPassword(password.toCharArray());
                    
                } else {
                    if(_debug.errorEnabled()){
                        _debug.error("JACCPC: identityServer: adding unknown callback["+i+"]="+ callbacks[i]);
                        _debug.error("identityServer: received unsupported callback");
                    }
                }
            }
        } catch (Exception e) {
            if (_debug.errorEnabled()){
                _debug.error("JACCPC: Exception thrown in adding data to callback..");
            }
            e.printStackTrace();
        }
    }
        
   
    private String[] getResourceName(Permission perm)    {
        String url = perm.getName();        
        String agentUrl = AgentConfiguration.getClientNotificationURL();
        String[] resources = url.split(":");
        if (agentUrl != null) {
            int idx = agentUrl.indexOf("://");
            idx = agentUrl.indexOf("/", idx+3);
            agentUrl = agentUrl.substring(0, idx);
        }   
        int idx = CONTEXT_ID.indexOf('/');
        String appName = CONTEXT_ID.substring(0,idx);
        
        for (int i =0 ; i<resources.length; i++) {
            if (resources[i].startsWith("/"))
                resources[i] = agentUrl + "/" + appName + resources[i];
            else 
                resources[i] = agentUrl + "/" + appName + "/" + resources[i];      
        }
                                    
        return resources;
    }
    
    protected java.security.Policy getPolicy(){
        synchronized(refreshLock) {
            if (state == INSERVICE_STATE) {
                return this.policy;
            }
           if(_debug.messageEnabled()){
                _debug.message("JACC Policy Provider: getPolicy ("+CONTEXT_ID+") is NOT in service");
            }
            return null;
        }
    }
        
    protected void refresh(boolean force){
        synchronized(refreshLock){
            String contextId=CONTEXT_ID;
            if (state == INSERVICE_STATE && (wasRefreshed == false || force)) {
                // find open policy.url
                int i = 0;
                String value = null;
                String urlKey = null;
                while (true) {
                    urlKey = PROVIDER_URL+(++i);
                    value = java.security.Security.getProperty(urlKey);
                    if (value == null || value.equals("")) {
                        break;
                    }
                }
                try {
                    //java.security.Security.setProperty(urlKey,policyUrlValue);
                    //excludedPermissions = loadExcludedPolicy();
                    if (policy == null) {
                        policy = (java.security.Policy)
                        new sun.security.provider.PolicyFile();                        
                    } else {
                        policy.refresh();                                                
                    }
                    wasRefreshed = true;
                } finally {
                    // can't setProperty back to null, workaround is to
                    // use empty string
                    java.security.Security.setProperty(urlKey,"");
                }
            }
        }
    }      
    
     protected void initialize(boolean open, boolean remove) {                  
               
       _debug = Debug.getInstance(IBaseModuleConstants.AM_LOG_RESOURCE);
       
       synchronized(refreshLock) {            
	 
	    if (open || remove) {
		setState(OPEN_STATE);
	    } else {
		setState(INSERVICE_STATE);
	    }
	  
            if (remove) {
                removePolicy();
            }

	    wasRefreshed = false;	    
	}                     
    }
    
    private static boolean initializeISProperties(){
        
        _debug = Debug.getInstance(IBaseModuleConstants.AM_LOG_RESOURCE);
        
        isOrganizationDN = AgentConfiguration.getOrganizationName();        
                           
        isUserName = AgentConfiguration.getApplicationUser();
        
        isUserPassword = AgentConfiguration.getApplicationPassword();
        
        isHostName = AgentConfiguration.getServerHost();
        
        isPort = AgentConfiguration.getServerPort();                
        
        isPolicyAdmLoc = AgentConfiguration.getPolicyAdmLocation();
        
        File f = new File(isPolicyAdmLoc);
        if (!f.exists()){                        
            _debug.error("JACCPC: FATAL: PolicyAdmin Properties not found");
            throw new RuntimeException("PolicyAdmin Properties not found");
        }                    
                     
        if (( isOrganizationDN == null)  ||
        ( isUserName == null) ||
        ( isUserPassword == null )){
            
            _debug.error("JACCPC: FATAL : Identity server properties not set");
            throw new RuntimeException("Identity server properties not set");
        }
        
        isOrganizationDN = DNMapper.orgNameToDN(isOrganizationDN);                
        
        return true;
        
    }
        
                
     private static String getToken(StringTokenizer strToken){
        if(strToken.hasMoreTokens()){
            return strToken.nextToken();
        }else
            return null;
     }          
     
     private static boolean initializeISPolicies() {
        StringTokenizer strToken = null;
        String policyNameWithOutSuffix = null;
        Vector policyNameTable = new Vector();
        String policyName = null;
        SSOToken ssoToken = null;
        
        try{
            _handler = OpenSSORequestHandler.getInstance();                                 
            // initialize identity server properties
            initializeISProperties();
            // authenticate and get single sign-on token from idenity server
            ssoToken = getSSOToken();
            
                       
            policyManager = new PolicyManager(ssoToken, isOrganizationDN);            
            subjectTypeManager = policyManager.getSubjectTypeManager();
            // retrieve all policyNames with the pattern "AgentName*"
            String key = AgentConfiguration.getApplicationUser() + "*";
            Set policyNameSet = policyManager.getPolicyNames(key);
            // now create a policyNameTable with all distinct policyNames
            if (!policyNameSet.isEmpty()){
                Iterator policyNameIterator = policyNameSet.iterator();
                
                //iterate through all policyNames
                while (policyNameIterator.hasNext()){
                    policyName = (String) policyNameIterator.next();
                    policyName = URLDecoder.decode(policyName);
                    strToken = new StringTokenizer(policyName, ".");
                    policyNameWithOutSuffix = getToken(strToken);
                    policyNameWithOutSuffix = policyNameWithOutSuffix.substring(
                     AgentConfiguration.getApplicationUser().length());
                    if(!policyNameTable.contains(policyNameWithOutSuffix)){
                        policyNameTable.add(policyNameWithOutSuffix);
                    }
                }
            }
            
            // now create OpenSSOJACCPolicyConfiguration for each policyName
            if(policyNameTable!= null){
                Iterator policyNameTableIterator= policyNameTable.iterator();
                while(policyNameTableIterator.hasNext()){
                    String contextId = (String) policyNameTableIterator.next();
                                        
                    OpenSSOJACCPolicyConfiguration pc =
                            SharedState.getConfig(contextId, false);                                                        
                }                            
        }
            /*
             * Removing this facility, since deletion of temp file is not possible
             * during redeployment. Since we need to provide a consistent user 
             * experience, deletion of temp file is not included.
             * /
          
         // This will take care of the temp file during startup
         /*File fd = new File(isPolicyAdmLoc);         
         fd.delete();
         */
        }catch(SSOException ssoe){
            ssoe.printStackTrace();
            return false;
        }catch(InvalidNameException ine){
            ine.printStackTrace();
            return false;
        }catch(NameNotFoundException nnfe){
            nnfe.printStackTrace();
            return false;
        }catch(NameAlreadyExistsException naee){
            naee.printStackTrace();
            return false;
        }catch(PolicyException pe){
            pe.printStackTrace();
            return false;       
        }finally {
            if (ssoToken != null) {
                destroySSOToken(ssoToken);
            }            
        }
        return true;
    }             
         
    private Rule createRule(Permission p, int ruleSuffix) throws Exception{         
         Map actions = new HashMap(1);
         Rule rule = null;
         Set allow = new HashSet(1);
         allow.add("allow");
         Set deny = new HashSet(1);
         deny.add("deny");         
         
         String pAction = p.getActions();
         if (pAction != null) {
            boolean isDeny = pAction.startsWith("!");     
            String[] tokens = null;
            if (isDeny) {
                tokens = pAction.substring(1).split(",");                    
            }else{
                tokens = pAction.split(",");
            }
            for(int i =0; i < tokens.length; i++) {
                   if (tokens[i].equals("GET") || tokens[i].equals("POST")) {                       
                       if (isDeny)
                           actions.put(tokens[i], deny);
                       else                                                        
                           actions.put(tokens[i], allow);
                   }
            }                                                           
         } else {
             // When no actions are defined, then it means all the HTTP Methods are allowed.         
             actions.put("GET", allow);
             actions.put("POST", allow);
         }
         
         if (actions.isEmpty())
             return null;           
         
         String[] resourceName = getResourceName(p);
         for (int i=0;i<1; i++){     
              try{
                rule = new Rule("Rule" + ruleSuffix ,
                            "iPlanetAMWebAgentService", resourceName[i], actions);
              } catch (Exception e) {
                  throw e;
              }
         }        
         return rule;        
    }
    
    private void storeUserDataRule(Permission p, String ruleSuffix) throws Exception{                  
         Map actions = new HashMap(1);
         Rule rule = null;
         Set allow = new HashSet(1);
         allow.add("allow");
         Set deny = new HashSet(1);
         deny.add("deny");         
         int ruleCount = 0;
         
         String name = p.getName();
         String[] tokens = name.split(":");
         
         String key = tokens[0];
         
         String[] resourceNames = getResourceName(p);
         int idx = resourceNames[0].indexOf("://");
         int portIdx = resourceNames[0].indexOf(":",idx+1);
         String ruleHttpsResource = null;
         if (portIdx == -1){
             //no port             
             portIdx = resourceNames[0].indexOf("/", idx +  3);
             ruleHttpsResource = "https" + resourceNames[0].substring(idx,portIdx);
             ruleHttpsResource = ruleHttpsResource+ ":*" + resourceNames[0].substring(portIdx);                           
         } else {
             ruleHttpsResource = "https" + resourceNames[0].substring(idx, portIdx);
             portIdx = resourceNames[0].indexOf("/", portIdx);
             ruleHttpsResource = ruleHttpsResource + ":*" + resourceNames[0].substring(portIdx);
         }                  
                     
         String pAction = p.getActions();
         if (pAction != null) {
             int idx1 = pAction.indexOf(":");
             
             if (idx1 != -1){                 
                 pAction = pAction.substring(0,idx1);
                 boolean isDeny = pAction.startsWith("!");     
                 tokens = null;
                 if (isDeny) {
                      tokens = pAction.substring(1).split(",");                    
                 }else{
                     tokens = pAction.split(",");
                 }
                 for(int i =0; i < tokens.length; i++) {
                   if (tokens[i].equals("GET") || tokens[i].equals("POST")) {                       
                       if (isDeny)
                           actions.put(tokens[i], deny);
                       else                                                        
                           actions.put(tokens[i], allow);
                   }
                } 
                
                if (actions.isEmpty())
                    return;
                 
                rule = new Rule("RuleUD" + ruleSuffix,
                            "iPlanetAMWebAgentService", ruleHttpsResource, actions);                            
             }
         }
         
         if (rule!= null)
            userDataConstraints.put(key, rule);
    }
    
    static boolean isPolicyPresent(String pcid) {
        
        boolean isPresent = false;
        SSOToken token = null;
        
        try {
        token = getSSOToken();
        PolicyManager pm = new PolicyManager(token, isOrganizationDN);
        String policyName = AgentConfiguration.getApplicationUser() + pcid + ".*";
     
        policyName = URLEncoder.encode(policyName);
        Set pSet = pm.getPolicyNames(policyName);
        if (pSet != null && pSet.size() != 0)
            isPresent = true;
        } catch (Exception e) {        
            e.printStackTrace();
        } finally {
            if (token != null) {
                destroySSOToken(token);
            }
        }        
        return isPresent;
    }               
}
