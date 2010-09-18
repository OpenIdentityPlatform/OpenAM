/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2005 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: AuthSPrincipal.java,v 1.3 2008/06/25 05:41:54 qcheng Exp $
 *
 */

package com.sun.identity.authentication.internal.server;

import java.security.Principal;

import com.iplanet.services.util.I18n;
import com.sun.identity.authentication.internal.util.AuthI18n;

/**
 * <p>
 * This class implements the <code>Principal</code> interface that represents
 * a user.
 * 
 * <p>
 * <code>AuthPrincipal</code> may be associated with a particular
 * <code>Subject</code> to augment that <code>Subject</code> with an
 * additional identity. Refer to the <code>Subject</code> class for more
 * information on how to achieve this. Authorization decisions can then be based
 * upon the Principals associated with a <code>Subject</code>.
 * 
 * <p>
 * <code>AuthSPrincipal</code> contains the "set" methods, whereas
 * <code>AuthPrincipal</code> contains just the "get" methods.
 * 
 * @see java.security.Principal
 * @see javax.security.auth.Subject
 * @see com.sun.identity.authentication.internal.AuthPrincipal
 */
public class AuthSPrincipal implements Principal, java.io.Serializable {

    /**
     * @serial
     */

    protected String name;

    protected String authMethod = null;

    protected String authLevel = null;

    protected I18n myAuthI18n = AuthI18n.authI18n;

    // protected String distinguishedName = null;

    /**
     * Create an AuthSPrincipal with a username.
     * 
     * <p>
     * 
     * @param name
     *            the username for this user.
     * 
     * @exception NullPointerException
     *                if the <code>name</code> is <code>null</code>.
     */
    public AuthSPrincipal(String name) {
        if (name == null) {
            // throw new NullPointerException ("illegal null input");
            throw new NullPointerException(myAuthI18n
                    .getString("authError-nullInput"));
        }

        this.name = name;
    }

    /**
     * Return the username for this <code>AuthPrincipal</code>.
     * 
     * <p>
     * 
     * @return the username for this <code>AuthPrincipal</code>
     */
    public String getName() {
        return name;
    }

    /**
     * Return the AuthMethod for this <code>AuthPrincipal</code>.
     * 
     * <p>
     * 
     * @return the AuthMethod for this <code>AuthPrincipal</code>
     */
    public String getAuthMethod() {
        return authMethod;
    }

    /**
     * Return the AuthLevel for this <code>AuthPrincipal</code>.
     * 
     * <p>
     * 
     * @return the AuthLevel for this <code>AuthPrincipal</code>
     */
    public String getAuthLevel() {
        return authLevel;
    }

    /**
     * Set the AuthMethod for this <code>AuthPrincipal</code>.
     * 
     * <p>
     * 
     * @param auth_method
     *            AuthMethod for this <code>AuthPrincipal</code>
     */
    protected void setAuthMethod(String auth_method) {
        authMethod = auth_method;
    }

    /**
     * Set the AuthLevel for this <code>AuthPrincipal</code>.
     * 
     * <p>
     * 
     * @param auth_level
     *            AuthLevel for this <code>AuthPrincipal</code>
     */
    protected void setAuthLevel(String auth_level) {
        authLevel = auth_level;
    }
}
