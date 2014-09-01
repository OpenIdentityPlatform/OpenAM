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
 * $Id: AmASRealm.java,v 1.6 2008/08/19 19:14:14 veiming Exp $
 */

/*
 *  Portions Copyrighted 2013 ForgeRock Inc.
 */

package com.sun.identity.agents.appserver.v81;


import java.util.Enumeration;
import java.util.Properties;

import com.sun.enterprise.security.auth.realm.IASRealm;
import com.sun.identity.agents.arch.IModuleAccess;
import com.sun.identity.agents.realm.AmRealmManager;
import com.sun.identity.agents.realm.IAmRealm;
import com.sun.identity.agents.util.EnumerationAdapter;

import com.sun.enterprise.security.auth.realm.BadRealmException;
import com.sun.enterprise.security.auth.realm.NoSuchUserException;
import com.sun.enterprise.security.auth.realm.NoSuchRealmException;
import com.sun.enterprise.security.auth.realm.InvalidOperationException;


/**
 * This class is the agent implementation of Sun Appserver Realm. 
 * It is used by the agent JAAS Login module to provide J2EE security. 
 */
public class AmASRealm extends IASRealm {

    /**
     * Returns names of all the groups in this particular realm.
     *
     * @return enumeration of group names (strings)
     * @exception BadRealmException if realm data structures are bad
     *
     */
    public Enumeration getGroupNames() throws BadRealmException {
        return new EnumerationAdapter();
    }

    /**
     * Returns names of all the groups of a user in this particular realm.
     *
     * @param userName The name of the user
     * @return enumeration of group names (strings)
     *
     * @throws InvalidOperationException 
     * @throws NoSuchUserException if the user doesn't exist in the realm
     *
     */
    public Enumeration getGroupNames(String userName)
            throws InvalidOperationException, NoSuchUserException {
        return new EnumerationAdapter();
    }

    protected String getAnyoneRole() {
        return _anyoneRole;
    }

    private void setAnyoneRole() {
        _anyoneRole = ANYONE_ROLE;
    }


    /*
     * This method is invoked during server startup when the realm is initially loaded. 
     * The props argument contains the properties defined for this realm in domain.xml. 
     * The realm can do any initialization it needs in this method. 
     * If the method returns without throwing an exception, the Application Server 
     * assumes the realm is ready to service authentication requests. 
     * If an exception is thrown, the realm is disabled.
     */
    protected void init(Properties props)
            throws BadRealmException, NoSuchRealmException {

        setRealmProperties(props);
        String jaasCtx = props.getProperty(IASRealm.JAAS_CONTEXT_PARAM);
        
        if(jaasCtx == null) {
            throw new BadRealmException(
                "Incomplete configuration in "
                + "Agent Realm: login module not specified.");
        }

        setProperty(IASRealm.JAAS_CONTEXT_PARAM, jaasCtx);
        setAnyoneRole();
    }

     
    /**
     * Return a short description of the kind of authentication which is 
     * supported by this realm.
     *
     * @return Description of the kind of authentication that is directly
     *     supported by this realm.
     */
    public String getAuthType() {
        return AUTH_TYPE;
    }

    /**
     * Returns the realm property based on a property name
     * @param name The property name
     *
     * @return the realm property based on a property name
     */
    public String getRealmProperty(String name) {
        return _realmProperties.getProperty(name);
    }

    private void setRealmProperties(Properties props) {
        _realmProperties = props;
    }

    
    public static final String AUTH_TYPE = 
        "OpenAM Policy Agent Realm";
    public static final String ANYONE_ROLE = "ANYONE";

    private Properties _realmProperties = null;
    private String _anyoneRole;
}

