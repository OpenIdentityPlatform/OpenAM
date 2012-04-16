/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: ISPolicy.java,v 1.6 2008/08/19 19:09:17 veiming Exp $
 *
 */
/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.policy.jaas;

import com.sun.identity.shared.debug.Debug;

import java.security.CodeSource;
import java.security.PermissionCollection;
import java.security.ProtectionDomain;
import java.util.Enumeration;

/**
 * This is an implementation of abstract class
 * <code>java.security.Policy</code> for representing the system security
 * policy for a Java application environment. It provides a custom policy
 * implementation based on JAAS and JDK 1.5 and above.It makes policy evaluation
 * against the OpenSSO Policy Service instead of the default file
 * based one.
 *<p>
 * It provides implementation of the abstract methods in Policy class.
 * <p>In general the source location for the policy information utilized by the
 * Policy object to make policy decisions is up to the Policy implementation.
 * In the case of ISPolicy the source comes from the OpenSSO's policy
 * store, which is consulted to make the policy evaluation.
 * <p>A <code>Policy</code> object can be queried for the set of Permissions 
 * granted to set of classes running as a <code>Principal</code> in the 
 * following manner: 
 * <pre>
 *   policy = Policy.getPolicy();
 *   PermissionCollection perms = policy.getPermissions(ProtectionDomain);
 * </pre>
 * The <code>Policy</code> object consults the local policy and returns the 
 * appropriate <code>PermissionCollection</code> object
 * {@link com.sun.identity.policy.jaas.ISPermissionCollection} containing
 * the Permissions granted to the  Principals and granted to the set of classes
 *  specified by the provided <code>protectionDomain</code>.
 *
 * <p>The currently-installed Policy object can be obtained by
 * calling the <code>getPolicy</code> method, and it can be
 * changed by a call to the <code>setPolicy</code> method (by
 * code with permission to reset the Policy).
 *
 * <p>The <code>refresh</code> method causes the policy
 * object to refresh/reload its current configuration.
 *
 * @see java.security.ProtectionDomain
 * @see java.security.PermissionCollection
 * @supported.all.api
 */
public class ISPolicy extends java.security.Policy {

    private static java.security.Policy defaultPolicy = null;

    static Debug debug = Debug.getInstance("amPolicy");

    /**
     * Constructs an <code>ISPolicy</code> instance.
     * Save the existing global policy , so that we can use that
     * for evaluating permissions we do not support through our custom policy 
     * implementation like <code>FilePermission</code>,
     * <code>SecurityPermission</code> etc.
     */
    public ISPolicy() {
        super();
        defaultPolicy = java.security.Policy.getPolicy();
        debug.message("ISPolicy:: ISPolicy() called");
    }
        
    /**
     * Evaluates the global policy and returns a
     * <code>PermissionCollection</code> object specifying the set of
     * permissions allowed for Principals associated with the enclosed
     * set of classes. Here we always return the 
     * <code>PermissionCollection</code> after
     * adding the<code>ISPermission</code> object into it, so that policy
     * determination is also based on OpenSSO's policies.
     *
     * @param protectionDomain  the protection domain which encapsulates the 
     *        characteristics of a domain, which encloses the set of classes 
     *        whose  instances are granted the permissions when being executed 
     *        on behalf of the given set of Principals.
     *
     * @return the Collection of permissions allowed for the protection
     *        domain according to the policy.
     *
     * @exception java.lang.SecurityException if the current thread does not
     * have permission to call <code>getPermissions</code> on the policy object.
     */
    public PermissionCollection getPermissions(ProtectionDomain 
        protectionDomain)  
    {
        debug.message("ISPolicy:: Calling getPermissions");
        if (debug.messageEnabled()) {
            debug.message("ISPolicy:: protectionDomain="
                +protectionDomain.toString());
        }
        PermissionCollection pc;
        pc = defaultPolicy.getPermissions(protectionDomain);
        
        // add the ISPermission into the PermissionCollection
        // returned by the default policy.
        pc.add(new ISPermission(protectionDomain));
        if (debug.messageEnabled()) {
            debug.message("ISPolicy:getPermissions::pc.elements()");
            for (Enumeration e = pc.elements(); e.hasMoreElements();) {
                debug.message(e.nextElement().toString()+"\n");
            }
        }
        return pc;
    }

    /**
     * Evaluates the global policy and returns a
     * <code>PermissionCollection</code> object specifying the set of
     * permissions allowed for Principals associated with the specified code
     * source. Here we always return the <code>PermissionCollection</code> 
     * after adding the<code>ISPermission</code> object into it, so that policy
     * determination is also based on OpenSSO's policies.
     *
     * @param codesource the <code>CodeSource</code> associated with the caller.
     * This encapsulates the original location of the code (where the code 
     * came from) and the public key(s) of its signer.This parameter may 
     * be null.

     * @return the Collection of permissions allowed for the code
     *         from <code>codesource</code> according to the policy.
     *
     * @exception java.lang.SecurityException if the current thread does not
     * have permission to call <code>getPermissions</code> on the policy object.
     */
    public PermissionCollection getPermissions(CodeSource codesource) { 
        debug.message("ISPolicy:: Calling getPermissions");
        if (debug.messageEnabled()) {
            debug.message("ISPolicy:: codesource's URL="
                +codesource.getLocation().toString());
        }
        PermissionCollection pc;
        pc = defaultPolicy.getPermissions(codesource);
        
        // add the ISPermission into the PermissionCollection
        // returned by the default policy.
        pc.add(new ISPermission(codesource));
        if (debug.messageEnabled()) {
            debug.message("ISPolicy:getPermissions::pc.elements()");
            for (Enumeration e = pc.elements(); e.hasMoreElements();) {
                debug.message(e.nextElement().toString()+"\n");
            }
        }
        return pc;
    }

    /**
     * Refreshes/reloads the policy configuration. The behavior of this method
     * depends on the implementation. In this implementation we will call 
     * refresh on the <code>defaultPolicy</code> we saved in the
     * <code>ISPolicy</code> constructor.
     *
     * @exception java.lang.SecurityException if the current thread does not
     *            have permission to refresh this Policy object.
     */
    public void refresh() {
        defaultPolicy.refresh();
        debug.message("ISPolicy:: Calling refresh");
    }
}
