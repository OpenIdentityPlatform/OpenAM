/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems, Inc. All Rights Reserved.
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
 * $Id: AmTomcatRealm.java,v 1.4 2009/05/13 00:54:32 kamna Exp $
 */


package com.sun.identity.agents.tomcat.v6;

import com.sun.identity.agents.arch.IModuleAccess;
import com.sun.identity.agents.arch.ModuleAccess;
import com.sun.identity.agents.realm.AmRealmAuthenticationResult;
import com.sun.identity.agents.realm.AmRealmManager;
import com.sun.identity.agents.realm.IAmRealm;

import org.apache.catalina.realm.RealmBase;
import org.apache.catalina.Realm;
import org.apache.catalina.realm.GenericPrincipal;

import java.lang.UnsupportedOperationException;

import java.lang.reflect.Constructor;
import java.security.Principal;
import java.security.cert.X509Certificate;

import java.util.Iterator;
import java.util.Set;
import java.util.ArrayList;
import java.util.List;


/**
 * <b>AmTomcatRealm</b> is the facade for an underlying security realm used to
 * authenticate Identity server users to Tomcat. Realms can be attached at any
 * Container level, but will typically only be attached to a Context, or higher
 * level, Container.
 *
 */
public class AmTomcatRealm extends RealmBase {
    private static IAmRealm amRealm = null;
    private static IModuleAccess moduleAccess = null;

    static {
        try {
            
            amRealm = AmRealmManager.getAmRealmInstance();
            moduleAccess = AmRealmManager.getModuleAccess();

            if ((moduleAccess != null)
                    && moduleAccess.isLogMessageEnabled()) {
                moduleAccess.logMessage(
                    "AmTomcatRealm: Realm Initialized");
            }
        } catch (Exception ex) {
            if ((moduleAccess != null)
                    && moduleAccess.isLogWarningEnabled()) {
                moduleAccess.logError(
                    "AmTomcatRealm: Realm Instantiation Error: " + ex);
            }
        }
    }

    /** Descriptive information about this Realm implementation */
    private final String info = 
            "AmTomcatRealm - Realm implementation for Tomcat ";

    /**
     * The <code> AmTomcatRealm </code> returns the GenericPrincipal associated with
     * the specified username and credentials; otherwise returns
     * <code>null</code>.
     *
     * @param username
     *            Username of the Principal to look up
     * @param credentials
     *            Password or other credentials to use in authenticating this
     *            username
     */
    public Principal authenticate(
        String username,
        String credentials) {

        Principal tomcatUser = null;

        try {
            AmRealmAuthenticationResult result = amRealm.authenticate(
                    username,
                    credentials);

            if ((result == null) || (!result.isValid())) {
                if ((moduleAccess != null)
                        && moduleAccess.isLogMessageEnabled()) {
                    moduleAccess.logMessage(
                        "AmTomcatRealm: Authentication FAILED for "
                        + username);
                }
            } else {

                if ((moduleAccess != null)
                        && moduleAccess.isLogMessageEnabled()) {
                    moduleAccess.logMessage(
                        "AmTomcatRealm: Authentication SUCCESSFUL for "
                        + username);
                }

               
                Set roles = result.getAttributes();
                ArrayList rolesList = new ArrayList();

                if ((roles != null) && (roles.size() > 0)) {
                	Iterator it = roles.iterator();
                	StringBuffer bufRoles = new StringBuffer();

                	while (it.hasNext()) {
                		String role = (String) it.next();
                		bufRoles.append(role);
                		bufRoles.append(" ");
                		rolesList.add(role);
                	}
                	
                	tomcatUser = instantiateGenericPrincipal(this,
            				username,
            				credentials,
            				rolesList);
                	
                	if ((moduleAccess != null)
                			&& moduleAccess.isLogMessageEnabled()) {
                		moduleAccess.logMessage(
                				"AmTomcatRealm: User " + username
                				+ " has roles: " + bufRoles.toString());
                		
                	}
                }
            }
        } catch (Exception ex) {
            if (moduleAccess != null) {
                moduleAccess.logError(
                    "AmTomcatRealm: encountered exception "
                    + ex.getMessage() + " while authenticating user "
                    + username,
                    ex);
            }
        }

        return tomcatUser;
    }

    /**
     * Return the Principal associated with the specified username and
     * credentials, if there is one; otherwise return <code>null</code>.
     *
     * @param username
     *            Username of the Principal to look up
     * @param credentials
     *            Password or other credentials to use in authenticating this
     *            username
     */
    public Principal authenticate(
        String username,
        byte[] credentials) {
        String password = new String(credentials);

        return authenticate(
            username,
            password);
    }
    
    /**
     * Return the GenericPrincipal associated with the specified username and
     * credentials, if there is one; otherwise return <code>null</code>.
     * 
     * This is here since Tomcat GenericPrincipal changed the method signature
     * between v6 and v7.
     *
     * @param realm
     *            Realm for the user
     * @param username
     *            Username of the Principal to look up
     * @param credentials
     *            Password or other credentials to use in authenticating this
     *            username
     * @param rolesList
     *            list of Roles for the User
     */
	private Principal instantiateGenericPrincipal(Realm realm,
			String username,
			String credentials,
			List<String>rolesList) {
		
		Constructor constructor;
		Principal	retVal = null;
		boolean     isV6 = true;
		
		try {
		Constructor[] constructors = GenericPrincipal.class.getConstructors();
		Class[] parameterTypes = constructors[0].getParameterTypes();
		
		if (parameterTypes[0] == Realm.class) {
			isV6 = true;
			constructor = GenericPrincipal.class.getConstructor(Realm.class, String.class, String.class, List.class);
			if (constructor != null) {
				retVal = (Principal)constructor.newInstance(realm,username,credentials,rolesList);
			}
		} else {
			isV6 = false;
			constructor = GenericPrincipal.class.getConstructor(String.class, String.class, List.class);
			if (constructor != null) {
				retVal = (Principal)constructor.newInstance(username,credentials,rolesList);
			}
		}
		} catch (Exception e) {
			retVal = null;
		} 
		return retVal;
	}


    /**
     * Return the Principal associated with the specified username, which
     * matches the digest calculated using the given parameters using the method
     * described in RFC 2069; otherwise return <code>null</code>.
     *
     * @param username
     *            Username of the Principal to look up
     * @param digest
     *            Digest which has been submitted by the client
     * @param nonce
     *            Unique (or supposedly unique) token which has been used for
     *            this request
     * @param realm
     *            Realm name
     * @param md5a2
     *            Second MD5 digest used to calculate the digest : MD5(Method +
     *            ":" + uri)
     */
    public Principal authenticate(
        String username,
        String digest,
        String nonce,
        String nc,
        String cnonce,
        String qop,
        String realm,
        String md5a2) {
        throw new UnsupportedOperationException();
    }

    /**
     * Return the Principal associated with the specified chain of X509 client
     * certificates. If there is none, return <code>null</code>.
     *
     * @param certs
     *            Array of client certificates, with the first one in the array
     *            being the certificate of the client itself.
     */
    public Principal authenticate(X509Certificate[] certs) {
        throw new UnsupportedOperationException();
    }

    /**
     * Return the Principal associated with the given user name.
     */
    protected Principal getPrincipal(String username) {
        return instantiateGenericPrincipal(this, username, null, null);
    }

    /**
     * Return descriptive information about this Realm implementation and the
     * corresponding version number, in the format
     * <code>&lt;description&gt;/&lt;version&gt;</code>.
     */
    public String getName() {
        return info;
    }

    /**
     * Return the password associated with the given principal's user name.
     */
    protected String getPassword(String username) {
        return (null);
    }

    private IAmRealm getAMRealm() {
        return amRealm;
    }

    private void setAMRealm(IAmRealm amRealm) {
        amRealm = amRealm;
    }

    private IModuleAccess getModuleAccess() {
        return moduleAccess;
    }

    private void setModuleAccess(IModuleAccess moduleAccess) {
        moduleAccess = moduleAccess;
    }
}
