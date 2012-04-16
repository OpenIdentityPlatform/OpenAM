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
 * $Id: OpenSSOJACCPolicy.java,v 1.1 2009/01/30 12:09:40 kalpanakm Exp $
 *
 */


package com.sun.opensso.agents.jsr115;

import java.security.Permission;
import java.security.Policy;
import java.security.PermissionCollection;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import javax.security.jacc.PolicyContext;
import javax.security.jacc.PolicyContextException;
import java.util.Collection;
import java.util.Iterator;

import com.sun.identity.shared.debug.Debug;
import com.sun.identity.agents.arch.IBaseModuleConstants;

import com.sun.enterprise.security.provider.PolicyWrapper;


/**
 * This class is a wrapper around the default jdk policy file
 * implementation. OpenSSOJACCPolicy is installed as the JRE policy object
 * It multiples policy decisions to the context specific instance of
 * sun.security.provider.PolicyFile.
 * 
 * Although this Policy provider is implemented using another Policy class,
 * this class is not a "delegating Policy provider" as defined by JACC, and
 * as such it SHOULD not be configured using the JACC system property
 * javax.security.jacc.policy.provider.
 * 
 * @author kalpana
 *
 */
public final class OpenSSOJACCPolicy extends java.security.Policy {
    
    // this is the jdk policy file instance
    private java.security.Policy policy = null;
    private static Debug _debug = Debug.getInstance(IBaseModuleConstants.AM_LOG_RESOURCE);
    private PolicyWrapper defaultPolicy;            
    
    /** Creates a new instance of DSamePolicyWrapper */
    public OpenSSOJACCPolicy() {
        // the jdk policy file implementation
        policy = (java.security.Policy) new sun.security.provider.PolicyFile();
        defaultPolicy = new PolicyWrapper();
    }
    
    /**
     * Evaluates the global policy and returns a
     * PermissionCollection object specifying the set of
     * permissions allowed for code from the specified
     * code source.
     *
     * @param codesource the CodeSource associated with the caller.
     * This encapsulates the original location of the code (where the code
     * came from) and the public key(s) of its signer.
     *
     * @return the set of permissions allowed for code from <i>codesource</i>
     * according to the policy.The returned set of permissions must be
     * a new mutable instance and it must support heterogeneous
     * Permission types.
     *
     */
    
    @Override
    public PermissionCollection getPermissions(CodeSource codesource) {
        String contextId = PolicyContext.getContextID();               
        
        if (SharedState.isAdminApp(contextId))
            return defaultPolicy.getPermissions(codesource);
        
        OpenSSOJACCPolicyConfiguration pci = getPolicyConfigForContext(contextId);
        Policy appPolicy = getPolicy(pci);
        PermissionCollection perms = appPolicy.getPermissions(codesource);
        if (_debug.messageEnabled()){
            _debug.message("JACC Policy Provider: DSamePolicyWrapper.getPermissions(cs), context ("+contextId+") codesource ("+codesource+") permissions: "+perms);
        }
        return perms;
    }
    
    /**
     * Evaluates the global policy and returns a
     * PermissionCollection object specifying the set of
     * permissions allowed given the characteristics of the
     * protection domain.
     *
     * @param domain the ProtectionDomain associated with the caller.
     *
     * @return the set of permissions allowed for the <i>domain</i>
     * according to the policy.The returned set of permissions must be
     * a new mutable instance and it must support heterogeneous
     * Permission types.
     *
     * @see java.security.ProtectionDomain
     * @see java.security.SecureClassLoader
     * @since 1.4
     */
    @Override
    public PermissionCollection getPermissions(ProtectionDomain domain) {
        String contextId = PolicyContext.getContextID();
        
        if (SharedState.isAdminApp(contextId))
            return defaultPolicy.getPermissions(domain);
        
        OpenSSOJACCPolicyConfiguration pci = getPolicyConfigForContext(contextId);
        Policy appPolicy = getPolicy(pci);
        PermissionCollection perms = appPolicy.getPermissions(domain);
      
        if (_debug.messageEnabled()){
            _debug.message("JACC Policy Provider: DSamePolicyWrapper.getPermissions(d), context ("+contextId+") permissions: "+perms);
        }
        return perms;
    }
    
    /**
     * Evaluates the global policy for the permissions granted to
     * the ProtectionDomain and tests whether the permission is
     * granted.
     *
     * @param domain the ProtectionDomain to test
     * @param permission the Permission object to be tested for implication.
     *
     * @return true if "permission" is a proper subset of a permission
     * granted to this ProtectionDomain.
     *
     * @see java.security.ProtectionDomain
     * @since 1.4
     */
    @Override
    public boolean implies(ProtectionDomain domain, Permission permission) {
        String contextId = PolicyContext.getContextID();
        
        if (SharedState.isAdminApp(contextId))
            return defaultPolicy.implies(domain, permission);
        
        
        OpenSSOJACCPolicyConfiguration pci = getPolicyConfigForContext(contextId);
        boolean result = pci.doImplies(domain,permission);                
        
        // log only the denied permissions
        if (!result) {
            if (_debug.messageEnabled()){
                _debug.message("JACC Policy Provider: OpenSSOJACCPolicy.implies, context ("+
                contextId+")- result was("+result+") permission ("
                +permission+")");
            }
        }
        return result;
    }
    
    /**
     * Refreshes/reloads the policy configuration. The behavior of this method
     * depends on the implementation. For example, calling <code>refresh</code>
     * on a file-based policy will cause the file to be re-read.
     *
     */
    @Override
    public void refresh() {        
        if (_debug.messageEnabled()){
            _debug.message("JACC Policy Provider: Refreshing Policy files!");
        }
        // always refreshes default policy context, but refresh
        // of application context depends on PolicyConfigurationImpl
        // this could result in an inconsistency since default context is
        // included in application contexts.
        policy.refresh();
        defaultPolicy.refresh();
        
        try {
        Collection c = OpenSSOJACCPolicyConfiguration.getPolicyConfigurationCollection();
        if (c != null) {
            Iterator it = c.iterator();
            while (it.hasNext()) {
                OpenSSOJACCPolicyConfiguration pci = (OpenSSOJACCPolicyConfiguration)it.next();
                if (pci != null) {
                    // false means don't force refresh if no update since last refresh.
                    // Should be able to set force switch based on whether the
                    // default context chnaged as a result of a refresh.
                    pci.refresh(false);
                }
            }
        }
        } catch (PolicyContextException e) {
        }
    }
    
    private static OpenSSOJACCPolicyConfiguration getPolicyConfigForContext(String contextId) {        
        OpenSSOJACCPolicyConfiguration pci = null;
        if (contextId != null) {
            pci = SharedState.getConfig(contextId, false);                    
        }
        return pci;
    }
    
    private Policy getPolicy(OpenSSOJACCPolicyConfiguration pci) {
        try {
        if (SharedState.isAdminApp(pci.getContextID()))
            return PolicyWrapper.getPolicy();
        }catch (Exception e) {
        }                
        
        Policy result = null;
        if (pci == null) {
            result = policy;
        } else {
            result = pci.getPolicy();
            if (result == null) {
                // the pc is not in service so use the default context
                result = policy;
            }
        }
        return result;
    }              
}
