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
 * $Id: IdServices.java,v 1.10 2010/01/06 01:58:26 veiming Exp $
 *
 * Portions Copyrighted 2015-2016 ForgeRock AS.
 */

package com.sun.identity.idm;

import java.util.Map;
import java.util.Set;

import javax.security.auth.callback.Callback;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.sm.SchemaType;

public interface IdServices {
    
    /**
     * Returns <code>true</code> if the data store has successfully
     * authenticated the identity with the specific type and provided credentials. In case the
     * data store requires additional credentials, the list would be returned
     * via the <code>IdRepoException</code> exception.
     *
     * @param orgName
     *            realm name to which the identity would be authenticated
     * @param credentials
     *            Array of callback objects containing information such as
     *            username and password.
     *
     * @return <code>true</code> if data store authenticates the identity;
     *         else <code>false</code>
     */
    public boolean authenticate(String orgName, Callback[] credentials)
        throws IdRepoException, AuthLoginException;
    
    /**
     * Returns <code>true</code> if the data store has successfully
     * authenticated the identity with the specific type and provided credentials. In case the
     * data store requires additional credentials, the list would be returned
     * via the <code>IdRepoException</code> exception.
     *
     * @param orgName
     *            realm name to which the identity would be authenticated
     * @param credentials
     *            Array of callback objects containing information such as
     *            username and password.
     * @param idType
     *            The type of identity, or null for any.
     *
     * @return <code>true</code> if data store authenticates the identity;
     *         else <code>false</code>
     */
    boolean authenticate(String orgName, Callback[] credentials, IdType idType)
           throws IdRepoException, AuthLoginException;

    public AMIdentity create(SSOToken token, IdType type, String name,
            Map attrMap, String amOrgName) throws IdRepoException, SSOException;

    public void delete(SSOToken token, IdType type, String name,
            String orgName, String amsdkDN) throws IdRepoException,
            SSOException;

    public Map getAttributes(SSOToken token, IdType type, String name,
            Set attrNames, String amOrgName, String amsdkDN, boolean isString)
            throws IdRepoException, SSOException;

    public Map getAttributes(SSOToken token, IdType type, String name,
            String amOrgName, String amsdkDN) throws IdRepoException,
            SSOException;

    public Set getMembers(SSOToken token, IdType type, String name,
            String amOrgName, IdType membersType, String amsdkDN)
            throws IdRepoException, SSOException;

    public Set getMemberships(SSOToken token, IdType type, String name,
            IdType membershipType, String amOrgName, String amsdkDN)
            throws IdRepoException, SSOException;

    public boolean isExists(SSOToken token, IdType type, String name,
            String amOrgName) throws SSOException, IdRepoException;

    public boolean isActive(SSOToken token, IdType type, String name,
            String amOrgName, String amsdkDN) throws SSOException,
            IdRepoException;

    public void setActiveStatus (SSOToken token, IdType type, String name,
            String amOrgName, String amsdkDN, boolean active)
            throws SSOException, IdRepoException;

    public void modifyMemberShip(SSOToken token, IdType type, String name,
            Set members, IdType membersType, int operation, String amOrgName)
            throws IdRepoException, SSOException;

    public void removeAttributes(SSOToken token, IdType type, String name,
            Set attrNames, String amOrgName, String amsdkDN)
            throws IdRepoException, SSOException;

    public IdSearchResults search(SSOToken token, IdType type, String pattern,
            IdSearchControl ctrl, String amOrgName) throws IdRepoException,
            SSOException;

    public void setAttributes(SSOToken token, IdType type, String name,
            Map attributes, boolean isAdd, String amOrgName, String amsdkDN, 
            boolean isString) throws IdRepoException, SSOException;

    public void changePassword(SSOToken token, IdType type, String name,
            String oldPassword, String newPassword, String amOrgName,
            String amsdkDN) throws IdRepoException, SSOException;

    public Set getAssignedServices(SSOToken token, IdType type, String name,
            Map mapOfServiceNamesAndOCs, String amOrgName, String amsdkDN)
            throws IdRepoException, SSOException;

    public void assignService(SSOToken token, IdType type, String name,
            String serviceName, SchemaType stype, Map attrMap,
            String amOrgName, String amsdkDN) throws IdRepoException,
            SSOException;

    public void unassignService(SSOToken token, IdType type, String name,
            String serviceName, Map attrMap, String amOrgName, String amsdkDN)
            throws IdRepoException, SSOException;

    public Map getServiceAttributes(SSOToken token, IdType type, String name,
            String serviceName, Set attrNames, String amOrgName, String amsdkDN)
            throws IdRepoException, SSOException;

    public Map getBinaryServiceAttributes(SSOToken token, IdType type,
            String name, String serviceName, Set attrNames, String amOrgName,
            String amsdkDN) throws IdRepoException, SSOException;

    /**
     * Non-javadoc, non-public methods
     * Get the service attributes of the name identity. Traverse to the global
     * configuration if necessary until all attributes are found or reached
     * the global area whichever occurs first.
     *
     * @param token is the sso token of the person performing this operation.
     * @param type is the identity type of the name parameter.
     * @param name is the identity we are interested in.
     * @param serviceName is the service we are interested in
     * @param attrNames are the name of the attributes wer are interested in.
     * @param amOrgName is the orgname.
     * @param amsdkDN is the amsdkDN.
     * @throws IdRepoException if there are repository related error conditions.
     * @throws SSOException if user's single sign on token is invalid.
     */
    public Map getServiceAttributesAscending(SSOToken token, IdType type,
            String name, String serviceName, Set attrNames, String amOrgName,
            String amsdkDN) throws IdRepoException, SSOException;

    public void modifyService(SSOToken token, IdType type, String name,
            String serviceName, SchemaType stype, Map attrMap,
            String amOrgName, String amsdkDN) throws IdRepoException,
            SSOException;

    public Set getSupportedTypes(SSOToken token, String amOrgName)
            throws IdRepoException, SSOException;

    public Set getSupportedOperations(SSOToken token, IdType type,
            String amOrgName) throws IdRepoException, SSOException;
    
    public void clearIdRepoPlugins();
    
    public void clearIdRepoPlugins(String orgName, String serviceComponent,
            int type);
    
    public void reloadIdRepoServiceSchema();
    
    public void reinitialize();

    public Set getFullyQualifiedNames(SSOToken token, IdType type,
        String name, String orgName) throws IdRepoException, SSOException;

    public IdSearchResults getSpecialIdentities(SSOToken token, IdType type,
            String orgName) throws IdRepoException, SSOException;
}
