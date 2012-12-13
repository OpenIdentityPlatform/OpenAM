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
 * $Id: SMSObjectIF.java,v 1.5 2009/10/28 04:24:27 hengming Exp $
 *
 */

/*
 * Portions Copyrighted 2011 ForgeRock AS
 */

package com.sun.identity.sm.jaxrpc;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Map;
import java.util.Set;

import com.iplanet.sso.SSOException;
import com.sun.identity.sm.SMSException;

/**
 * JAX-RPC interface for SMSObject and Services
 */
public interface SMSObjectIF extends Remote {

    public void checkForLocal() throws RemoteException;

    public Map read(String t, String name) throws SMSException, SSOException,
            RemoteException;

    public void create(String token, String objName, Map attributes)
            throws SMSException, SSOException, RemoteException;

    public void modify(String token, String objName, String mods)
            throws SMSException, SSOException, RemoteException;

    public void delete(String token, String objName) throws SMSException,
            SSOException, RemoteException;

    public Set subEntries(String token, String dn, String filter,
            int numOfEntries, boolean sortResults, boolean ascendingOrder)
            throws SMSException, SSOException, RemoteException;

    public Set schemaSubEntries(String token, String dn, String filter,
            String sidFilter, int numOfEntries, boolean sortResults, boolean ao)
            throws SMSException, SSOException, RemoteException;

    public Set search(String token, String startDN, String filter)
            throws SMSException, SSOException, RemoteException;

    public Set search2(String token, String startDN, String filter,
        int numOfEntries, int timeLimit, boolean sortResults,
        boolean ascendingOrder)
            throws SMSException, SSOException, RemoteException;
    
    public Set search3(String tokenID, String startDN, String filter,
        int numOfEntries, int timeLimit, boolean sortResults,
        boolean ascendingOrder, Set excludes)
            throws SMSException, SSOException, RemoteException;
    
    public Set searchSubOrgNames(String token, String dn, String filter,
            int numOfEntries, boolean sortResults, boolean ascendingOrder,
            boolean recursive) throws SMSException, SSOException,
            RemoteException;

    public Set searchOrganizationNames(String token, String dn,
            int numOfEntries, boolean sortResults, boolean ascendingOrder,
            String serviceName, String attrName, Set values)
            throws SMSException, SSOException, RemoteException;

    public boolean entryExists(String token, String objName)
            throws SSOException, RemoteException;

    public String getRootSuffix() throws RemoteException;

    public String getAMSdkBaseDN() throws RemoteException;

    // Objects changed within <i>time</i> minutes
    public Set objectsChanged(int time) throws RemoteException;

    public String registerNotificationURL(String url) throws RemoteException;

    public void deRegisterNotificationURL(String notificationID)
            throws RemoteException;

    // Interface to receive object changed notifications
    public void notifyObjectChanged(String name, int type)
            throws RemoteException;

    /**
     * Validates service configuration attributes.
     *
     * @param token Single Sign On token.
     * @param validatorClass validator class name.
     * @param values Values to be validated.
     * @return <code>true</code> of values are valid.
     * @throws SMSException if value is not valid.
     * @throws SSOException if single sign on token is in valid.
     * @throws RemoteException if remote method cannot be invoked.
     */
    public boolean validateServiceAttributes(
        String token,
        String validatorClass,
        Set values
    ) throws SMSException, SSOException, RemoteException;
}
