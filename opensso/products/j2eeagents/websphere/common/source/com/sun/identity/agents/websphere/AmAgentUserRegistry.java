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
 * $Id: AmAgentUserRegistry.java,v 1.2 2008/11/21 22:21:45 leiming Exp $
 *
 */

package com.sun.identity.agents.websphere;

import java.rmi.RemoteException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.ibm.websphere.security.CertificateMapFailedException;
import com.ibm.websphere.security.CertificateMapNotSupportedException;
import com.ibm.websphere.security.CustomRegistryException;
import com.ibm.websphere.security.EntryNotFoundException;
import com.ibm.websphere.security.NotImplementedException;
import com.ibm.websphere.security.PasswordCheckFailedException;
import com.ibm.websphere.security.Result;
import com.ibm.websphere.security.UserRegistry;
import com.ibm.websphere.security.cred.WSCredential;
import com.sun.identity.agents.arch.AgentException;

/**
 * This class contains customized Websphere's User Registry.
 */
public class AmAgentUserRegistry implements UserRegistry {
    
    public void initialize(Properties arg0) throws CustomRegistryException,
            RemoteException {
        // No handling required
    }
    
    public String checkPassword(String userSecurityName, String passwd)
    throws PasswordCheckFailedException, CustomRegistryException,
            RemoteException {
        String result = null;
        try {
            result = getRealmUserRegistry().checkCredentials(
                    userSecurityName, passwd);
        } catch (AgentException ex) {
            throw new CustomRegistryException(ex);
        }
        return result;
    }
    
    public String mapCertificate(X509Certificate[] cert)
    throws CertificateMapNotSupportedException,
            CertificateMapFailedException, CustomRegistryException,
            RemoteException {
        String result = null;
        try {
            result = getRealmUserRegistry().getUserName(cert);
        } catch (AgentException ex) {
            throw new CustomRegistryException(ex);
        }
        return result;
    }
    
    public String getRealm() throws CustomRegistryException, RemoteException {
        return IAmRealmUserRegistry.REALM_NAME;
    }
    
    public Result getUsers(String pattern, int limit)
    throws CustomRegistryException, RemoteException {
        Result result = new Result();
        List users = new ArrayList();
        try {
            users = getRealmUserRegistry().getUsers(pattern, limit);
        } catch (AgentException ex) {
            throw new CustomRegistryException(ex);
        }
        result.setList(users);
        return result;
    }
    
    public String getUserDisplayName(String userSecurityName)
    throws EntryNotFoundException, CustomRegistryException,
            RemoteException {
        // Agent does not maintain a different display name
        return userSecurityName;
    }
    
    public String getUniqueUserId(String userSecurityName)
    throws EntryNotFoundException,
            CustomRegistryException, RemoteException {
        return userSecurityName;
    }
    
    public String getUserSecurityName(String uniqueUserId)
    throws EntryNotFoundException, CustomRegistryException,
            RemoteException {
        return uniqueUserId;
    }
    
    public boolean isValidUser(String userSecurityName)
    throws CustomRegistryException,
            RemoteException {
        boolean result = false;
        try {
            result = getRealmUserRegistry().isValidUser(userSecurityName);
        } catch (AgentException ex) {
            throw new CustomRegistryException(ex);
        }
        return result;
    }
    
    public Result getGroups(String pattern, int limit)
    throws CustomRegistryException, RemoteException {
        Result result = new Result();
        List groups = new ArrayList();
        try {
            groups = getRealmUserRegistry().getGroups(pattern, limit);
        } catch (AgentException ex) {
            throw new CustomRegistryException(ex);
        }
        result.setList(groups);
        return result;
    }
    
    public String getGroupDisplayName(String groupSecurityName)
    throws EntryNotFoundException, CustomRegistryException,
            RemoteException {
        // Group security name is the display name as well
        return groupSecurityName;
    }
    
    public String getUniqueGroupId(String groupSecurityName)
    throws EntryNotFoundException, CustomRegistryException, RemoteException {
        return groupSecurityName;
    }
    
    public List getUniqueGroupIds(String uniqueUserId)
    throws EntryNotFoundException, CustomRegistryException, RemoteException {
        List result = new ArrayList();
        
        try {
            List memberships =
                    getRealmUserRegistry().getMemberships(uniqueUserId);
            result.addAll(memberships);
        } catch (AgentException ex) {
            throw new CustomRegistryException(ex);
        }
        
        return result;
    }
    
    public String getGroupSecurityName(String uniqueGroupId)
    throws EntryNotFoundException, CustomRegistryException,
            RemoteException {
        return uniqueGroupId;
    }
    
    public boolean isValidGroup(String groupSecurityName)
    throws CustomRegistryException, RemoteException {
        boolean result = false;
        try {
            result = getRealmUserRegistry().isValidGroup(groupSecurityName);
        } catch (AgentException ex) {
            throw new CustomRegistryException(ex);
        }
        return result;	}
    
    public List getGroupsForUser(String userName) throws EntryNotFoundException,
            CustomRegistryException, RemoteException {
        return getUniqueGroupIds(userName);
    }
    
    public Result getUsersForGroup(String groupSecurityName, int limit)
    throws NotImplementedException, EntryNotFoundException,
            CustomRegistryException, RemoteException {
        throw new NotImplementedException();
    }
    
    public WSCredential createCredential(String userSecurityName)
    throws NotImplementedException, EntryNotFoundException,
            CustomRegistryException, RemoteException {
        // As suggested by WAS docs, return null for this method
        return null;
    }
    
    private IAmRealmUserRegistry getRealmUserRegistry() {
        return AmWebsphereManager.getAmRealmUserRegistryInstance();
    }
}
