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
 * $Id: IAmRealmUserRegistry.java,v 1.2 2008/11/21 22:21:46 leiming Exp $
 *
 */

package com.sun.identity.agents.websphere;

import java.security.cert.X509Certificate;
import java.util.List;

import com.sun.identity.agents.arch.AgentException;

/** 
 * User Registry interface.
 */
public interface IAmRealmUserRegistry {
    
    public void initialize() throws AgentException;
    
    public String checkCredentials(String userName, String password)
    throws AgentException;
    
    public String getUserName(X509Certificate[] certs) throws AgentException;
    
    public List getUsers(String pattern, int limit) throws AgentException;
    
    public List getGroups(String pattern, int limit) throws AgentException;
    
    public boolean isValidUser(String userName) throws AgentException;
    
    public boolean isValidGroup(String groupName) throws AgentException;
    
    public List getMemberships(String userName) throws AgentException;
    
    public static final String REALM_NAME = "AmRealm";
    
}
