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
 * $Id: AMIdentityRepository.java,v 1.21 2010/01/06 01:58:26 veiming Exp $
 *
 * Portions Copyrighted 2011-2015 ForgeRock AS.
 */
package com.sun.identity.idm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.security.auth.callback.Callback;

import com.sun.identity.shared.ldap.LDAPDN;
import com.sun.identity.shared.ldap.util.DN;

import com.iplanet.am.sdk.AMHashMap;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.common.CaseInsensitiveHashMap;
import com.sun.identity.common.DNUtils;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.DNMapper;
import com.sun.identity.sm.OrganizationConfigManager;
import com.sun.identity.sm.SMSException;

/**
 * The class <code> AMIdentityRepository </code> represents an object to access
 * the repositories in which user/role/group and other identity data is
 * configured. This class provides access to methods which will search, create
 * and delete identities. An instance of this class can be obtained in the
 * following manner:
 * <p>
 * 
 * <PRE>
 * 
 * AMIdentityRepository idRepo = new AMIdentityRepository(ssoToken, realmName);
 * 
 * </PRE>
 * 
 * @supported.api
 */
public final class AMIdentityRepository {
    private SSOToken token;
    private String organizationDN;
    private String idRealmName;

    public static Debug debug = Debug.getInstance("amIdm");
    public static Map listeners = new CaseInsensitiveHashMap();

    /**
     * @supported.api
     * 
     * Constructor for the <code>AMIdentityRepository</code> object. If a null
     * is passed for the organization identifier <code>realmName</code>, then
     * the "root" realm is assumed.
     * 
     * @param ssotoken
     *            Single sign on token of the user
     * @param realmName
     *            Name of the realm (can be a Fully qualified DN)
     * @throws IdRepoException
     *             if there are repository related error conditions.
     * @throws SSOException
     *             if user's single sign on token is invalid.
     */
    public AMIdentityRepository(SSOToken ssotoken, String realmName)
            throws IdRepoException, SSOException {
        token = ssotoken;
        idRealmName = realmName;
        organizationDN = DNMapper.orgNameToDN(realmName);
    }

    /**
     * @supported.api
     * 
     * Returns the set of supported object types <code>IdType</code> for this
     * deployment. This is not realm specific.
     * 
     * @return Set of supported <code> IdType </code> objects.
     * @throws IdRepoException
     *             if there are repository related error conditions.
     * @throws SSOException
     *             if user's single sign on token is invalid.
     */
    public Set getSupportedIdTypes() throws IdRepoException, SSOException {
        IdServices idServices = IdServicesFactory.getDataStoreServices();
        Set res = idServices.getSupportedTypes(token, organizationDN);
        res.remove(IdType.REALM);
        return res;
    }

    /**
     * @supported.api
     * 
     * Returns the set of Operations for a given <code>IdType</code>,
     * <code>IdOperations</code> that can be performed on an Identity. This
     * varies for each organization (and each plugin?).
     * 
     * @param type
     *            Type of identity
     * @return Set of <code>IdOperation</code> objects.
     * @throws IdRepoException
     *             if there are repository related error conditions.
     * @throws SSOException
     *             if user's single sign on token is invalid.
     */
    public Set getAllowedIdOperations(IdType type) throws IdRepoException,
            SSOException {
        IdServices idServices = IdServicesFactory.getDataStoreServices();
        return idServices.getSupportedOperations(token, type, organizationDN);

    }

    /**
     * 
     * Return the special identities for this realm for a given type. These
     * identities cannot be deleted and hence have to be shown in the admin
     * console as non-deletable.
     * 
     * @param type
     *            Type of the identity
     * @return IdSearchResult
     * @throws IdRepoException
     *             if there is a datastore exception
     * @throws SSOException
     *             if the user's single sign on token is not valid.
     */
    public IdSearchResults getSpecialIdentities(IdType type)
            throws IdRepoException, SSOException {

        IdSearchResults results = getSpecialIdentities(token, type, 
                organizationDN);

        if (type.equals(IdType.USER)) {
            // Iterating through to get out the names and remove only amadmin
            // anonymous as per AM console requirement.

            IdSearchResults newResults = new IdSearchResults(type, 
                    organizationDN);
            Set identities = results.getSearchResults();
            if ((identities != null) && !identities.isEmpty()) {
                for (Iterator i = identities.iterator(); i.hasNext();) {
                    AMIdentity amid = ((AMIdentity) i.next());
                    String remUser = amid.getName().toLowerCase();
                    if (!remUser.equalsIgnoreCase(IdConstants.AMADMIN_USER)
                            && !remUser.equalsIgnoreCase(
                                    IdConstants.ANONYMOUS_USER)) 
                    {
                        newResults.addResult(amid, Collections.EMPTY_MAP);
                    }
                }
                results = newResults;
            }
        }
        return results;
    }

    /**
     * Searches for identities of a certain type. The iterator returns
     * AMIdentity objects for use by the application.
     * 
     * @deprecated This method is deprecated. Use
     *             {@link #searchIdentities(IdType type,String pattern,
     *             IdSearchControl ctrl)}
     * @param type
     *            Type of identity being searched for.
     * @param pattern
     *            Search pattern, like "a*" or "*".
     * @param avPairs
     *            Map of attribute-values which can further help qualify the
     *            search pattern.
     * @param recursive
     *            If true, then the search is performed on the entire subtree
     *            (if applicable)
     * @param maxResults
     *            Maximum number of results to be returned. A -1 means no limit
     *            on the result set.
     * @param maxTime
     *            Maximum amount of time after which the search should return
     *            with partial results.
     * @param returnAttributes
     *            Set of attributes to be read when performing the search.
     * @param returnAllAttributes
     *            If true, then read all the attributes of the entries.
     * @return results containing <code>AMIdentity</code> objects.
     * @throws IdRepoException
     *             if there are repository related error conditions.
     * @throws SSOException
     *             if user's single sign on token is invalid.
     */
    public IdSearchResults searchIdentities(IdType type, String pattern,
            Map avPairs, boolean recursive, int maxResults, int maxTime,
            Set returnAttributes, boolean returnAllAttributes)
            throws IdRepoException, SSOException {
        IdSearchControl crtl = new IdSearchControl();
        crtl.setSearchModifiers(IdSearchOpModifier.OR, avPairs);
        crtl.setRecursive(recursive);
        crtl.setMaxResults(maxResults);
        crtl.setTimeOut(maxTime);
        crtl.setReturnAttributes(returnAttributes);
        crtl.setAllReturnAttributes(returnAllAttributes);
        
        // Call search method that takes IdSearchControl
        return searchIdentities(type, pattern, crtl);
    }

    /**
     * @supported.api
     * 
     * Searches for identities of certain types from each plugin and returns a
     * combined result
     * 
     * <b>Note:</b> The AMIdentity objects representing IdType.REALM can be
     * used for services related operations only. The realm <code>AMIdentity
     * </code> object can be used to assign and unassign services containing
     * dynamic attributes to this realm.
     *
     * @param type
     *            Type of identity being searched for.
     * @param pattern
     *            Pattern to be used when searching.
     * @param ctrl
     *            IdSearchControl which can be used to set up various search
     *            controls on the search to be performed.
     * @return Returns the combined results in an object IdSearchResults.
     * @see com.sun.identity.idm.IdSearchControl
     * @see com.sun.identity.idm.IdSearchResults
     * @throws IdRepoException
     *             if there are repository related error conditions.
     * @throws SSOException
     *             if user's single sign on token is invalid.
     */
    public IdSearchResults searchIdentities(IdType type, String pattern,
            IdSearchControl ctrl) throws IdRepoException, SSOException {
        IdSearchResults idSearchResults = null;

        if (type.equals(IdType.REALM)) {
            try {
                idSearchResults = new IdSearchResults(type, idRealmName);
                OrganizationConfigManager orgMgr =
                    new OrganizationConfigManager(token, idRealmName);
                Set realmNames = orgMgr.getSubOrganizationNames(pattern, false);
                if (realmNames != null) {
                    Iterator iter = realmNames.iterator();
                    while (iter.hasNext()) {
                        String realmName = (String) iter.next();

                        AMIdentity realmIdentity = getSubRealmIdentity(
                                realmName);
                        Map attributes = new HashMap();
                        // TODO: To add attribute support to realms.
                        // Un comment this part once the support is added.
                        idSearchResults.addResult(realmIdentity, attributes);
                        idSearchResults.setErrorCode(IdSearchResults.SUCCESS);
                    }
                }
            } catch (SMSException sme) {
                debug.error("AMIdentityRepository.searchIdentities() - "
                        + "Error occurred while searching " + type.getName()
                        + ":", sme);
                throw new IdRepoException(sme.getMessage());
            }
        } else {
            IdServices idServices =
                IdServicesFactory.getDataStoreServices();

            idSearchResults = idServices.search(token, type, pattern, ctrl,
                    organizationDN);
        }

        return idSearchResults;

    }

    /**
     * @supported.api
     * 
     * Returns a handle of the Identity object representing this
     * realm for services related operations only. This <code> AMIdentity
     * </code> object can be used to assign and unassign services containing
     * dynamic attributes to this realm
     * 
     * @return a handle of the Identity object.
     * @throws IdRepoException
     *             if there are repository related error conditions.
     * @throws SSOException
     *             if user's single sign on token is invalid.
     */
    public AMIdentity getRealmIdentity() throws IdRepoException, SSOException {
        return getRealmIdentity(token, organizationDN);
    }

    private AMIdentity getRealmIdentity(SSOToken token, String orgDN)
        throws IdRepoException {
        String universalId = "id=ContainerDefaultTemplateRole,ou=realm," +
            orgDN;
        return IdUtils.getIdentity(token, universalId);
    }

    private AMIdentity getSubRealmIdentity(String subRealmName) throws
        IdRepoException, SSOException {
        String realmName = idRealmName;
        if (DN.isDN(idRealmName)) {  // Wouldn't be a DN if it starts with "/"
            realmName = DNMapper.orgNameToRealmName(idRealmName);
        }

        String fullRealmName = realmName + IdConstants.SLASH_SEPARATOR +
            subRealmName;
        String subOrganizationDN = DNMapper.orgNameToDN(fullRealmName);

        return getRealmIdentity(token, subOrganizationDN);
    }

    /**
     * @supported.api
     *
     * Creates a single object of a type. The object is
     * created in all the plugins that support creation of this type of object.
     * 
     * This method is only valid for:
     *
     * <ol>
     * <li> {@link IdType#AGENT IdType.AGENT} </li>
     * <li> {@link IdType#USER  IdType.USER}  </li>
     * <li> {@link IdType#REALM  IdType.REALM}  </li>
     * </ol>
     *
     * <br>
     * <b>Note:</b> For creating {@link IdType#REALM  IdType.REALM} identities,
     * a map of <code>sunIdentityRepositoryService</code> attributes need to
     * be passed. Also, AMIdentity object representing this realm can be
     * used for services related operations only. This <code> AMIdentity
     * </code> object can be used to assign and unassign services containing
     * dynamic attributes to this realm
     *
     * 
     * @param type
     *            <code>IdType</code> of object to be created.
     * @param idName
     *            Name of object. If the type is <code>IdType.REALM</code>
     *            then enter a valid realm name.
     * @param attrMap
     *            Map of attribute-values to be set when creating the entry.
     * @return Identity object representing the newly created entry.
     * @throws IdRepoException
     *             if there are repository related error conditions.
     * @throws SSOException
     *             if user's single sign on token is invalid.
     */
    public AMIdentity createIdentity(IdType type, String idName, Map attrMap)
            throws IdRepoException, SSOException {
        IdServices idServices = IdServicesFactory.getDataStoreServices();
        return idServices.create(token, type, idName, attrMap, organizationDN);
    }

    /**
     * @supported.api
     * 
     * Creates multiple objects of the same type. The objects are created in all
     * the <code>IdRepo</code> plugins that support creation of these objects.
     * 
     * This method is only valid for:
     *
     * <ol>
     * <li> {@link IdType#AGENT IdType.AGENT} </li>
     * <li> (@link IdType#USER  IdType.USER}  </li>
     * <li> {@link IdType#REALM  IdType.REALM}  </li>
     * </ol>
     *
     * <br>
     * <b>Note:</b> For creating {@link IdType#REALM  IdType.REALM} identities,
     * a map of <code>sunIdentityRepositoryService</code> attributes need to
     * be passed. Also, AMIdentity object representing this realm can be
     * used for services related operations only. This <code> AMIdentity
     * </code> object can be used to assign and unassign services containing
     * dynamic attributes to this realm.
     * 
     * @param type
     *            Type of object to be created
     * @param identityNamesAndAttrs
     *            Names of the identities and their
     * @return Set of created Identities.
     * @throws IdRepoException
     *             if there are repository related error conditions.
     * @throws SSOException
     *             if user's single sign on token is invalid.
     */
    public Set createIdentities(IdType type, Map identityNamesAndAttrs)
            throws IdRepoException, SSOException {
        Set results = new HashSet();

        if (identityNamesAndAttrs == null || identityNamesAndAttrs.isEmpty()) {
            throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, "201", null);
        }

        Iterator it = identityNamesAndAttrs.keySet().iterator();

        while (it.hasNext()) {
            String name = (String) it.next();
            Map attrMap = (Map) identityNamesAndAttrs.get(name);
            AMIdentity id = createIdentity(type, name, attrMap);
            results.add(id);
        }

        return results;
    }

    /**
     * @supported.api
     * 
     * Deletes identities. The Set passed is a set of <code>AMIdentity</code>
     * objects.
     * 
     * This method is only valid for:
     * <ol>
     * <li> {@link IdType#AGENT IdType.AGENT} </li>
     * <li> {@link IdType#REALM IdType.REALM} </li>
     * <li> (@link IdType#USER IdType.USER} </li>
     * </ol>
     * 
     * @param type Type of Identity to be deleted.
     * @param identities Set of <code>AMIdentity</code> objects to be deleted.
     * @throws IdRepoException if there are repository related error conditions.
     * @throws SSOException if user's single sign on token is invalid.
     * @deprecated As of release AM 7.1, replaced by
     *             {@link #deleteIdentities(Set)}
     */
    public void deleteIdentities(IdType type, Set identities)
            throws IdRepoException, SSOException {
        deleteIdentities(identities);
    }

    /**
     * @supported.api
     * 
     * Deletes identities. The Set passed is a set of <code>AMIdentity</code>
     * objects.
     * 
     * This method is only valid for:
     * <ol>
     * <li> {@link IdType#AGENT IdType.AGENT} </li>
     * <li> {@link IdType#REALM IdType.REALM} </li>
     * <li> (@link IdType#USER  IdType.USER}  </li>
     * </ol>
     * 
     * @param identities Set of <code>AMIdentity</code> objects to be deleted
     * @throws IdRepoException if there are repository related error conditions.
     * @throws SSOException if user's single sign on token is invalid.
     */
    public void deleteIdentities(Set identities) throws IdRepoException,
            SSOException {
        if (identities == null || identities.isEmpty()) {
            throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, "201", null);
        }

        Iterator it = identities.iterator();
        while (it.hasNext()) {
            AMIdentity id = (AMIdentity) it.next();
            IdServices idServices = IdServicesFactory.getDataStoreServices();
            idServices.delete(token, id.getType(), id.getName(), organizationDN, 
                    id.getDN());
        }
    }

    /**
     * Non-javadoc, non-public methods Returns <code>true</code> if the data
     * store has successfully authenticated the identity with the provided
     * credentials. In case the data store requires additional credentials, the
     * list would be returned via the <code>IdRepoException</code> exception.
     * 
     * @param credentials
     *            Array of callback objects containing information such as
     *            username and password.
     * 
     * @return <code>true</code> if data store authenticates the identity;
     *         else <code>false</code>
     */
    public boolean authenticate(Callback[] credentials) throws IdRepoException,
            com.sun.identity.authentication.spi.AuthLoginException {
        IdServices idServices = IdServicesFactory.getDataStoreServices();
        return (idServices.authenticate(organizationDN, credentials));
    }

    /**
     * @supported.api
     * 
     * Adds a listener, which should receive notifications for all changes that
     * occurred in this organization.
     * 
     * This method is only valid for IdType User and Agent.
     * 
     * @param listener
     *            The callback which implements <code>AMEventListener</code>.
     * @return Integer identifier for this listener.
     */
    public int addEventListener(IdEventListener listener) {
        ArrayList listOfListeners = (ArrayList) listeners.get(organizationDN);
        if (listOfListeners == null) {
            listOfListeners = new ArrayList();
        }
        synchronized (listeners) {
            listOfListeners.add(listener);
            listeners.put(organizationDN, listOfListeners);
        }
        return (listOfListeners.size() - 1);
    }

    /**
     * @supported.api
     * 
     * Removes listener as the application is no longer interested in receiving
     * notifications.
     * 
     * @param identifier
     *            Integer identifying the listener.
     */
    public void removeEventListener(int identifier) {
        ArrayList listOfListeners = (ArrayList) listeners.get(organizationDN);
        if (listOfListeners != null) {
            synchronized (listeners) {
                listOfListeners.remove(identifier);
            }
        }
    }

    /**
     * Non-javadoc, non-public methods Returns <code>true</code> if the data
     * store has successfully authenticated the identity with the provided
     * credentials. In case the data store requires additional credentials, the
     * list would be returned via the <code>IdRepoException</code> exception.
     *
     * @param credentials
     *            Array of callback objects containing information such as
     *            username and password.
     * @param idType
     *            The type of identity to authenticate as, or null for any.
     *
     * @return <code>true</code> if data store authenticates the identity;
     *         else <code>false</code>
     */
    public boolean authenticate(IdType idType, Callback[] credentials) throws IdRepoException,
            com.sun.identity.authentication.spi.AuthLoginException {
        IdServices idServices = IdServicesFactory.getDataStoreServices();
        return idServices.authenticate(organizationDN, credentials, idType);
    }

    /**
     * @supported.api
     * 
     * Clears the cache.
     */
    public static void clearCache() {
        IdServices idServices = IdServicesFactory.getDataStoreServices();
        idServices.reinitialize();
        IdUtils.initialize();
    }


    public IdSearchResults getSpecialIdentities(SSOToken token, IdType type,
            String orgName) throws IdRepoException, SSOException {
        IdServices idServices = IdServicesFactory.getDataStoreServices();
        return idServices.getSpecialIdentities(token, type, orgName);
    }
    
    /**
     * Return String representation of the <code>AMIdentityRepository
     * </code> object. It returns realm name.
     *
     * @return String representation of <code>AMIdentityRepository</code>
     * object.
     */
    public String toString() {
        StringBuilder sb = new StringBuilder(100);
        sb.append("AMIdentityRepository object: ")
            .append(organizationDN);
        return (sb.toString());
    }

    // TODO:
    // FIXME: Move these utilities to a util class
    private Map reverseMapAttributeNames(Map attrMap, Map configMap) {
        if (attrMap == null || attrMap.isEmpty()) {
            return attrMap;
        }
        Map resultMap;
        Map[] mapArray = getAttributeNameMap(configMap);
        if (mapArray == null) {
            resultMap = attrMap;
        } else {
            resultMap = new CaseInsensitiveHashMap();
            Map reverseMap = mapArray[1];
            Iterator it = attrMap.keySet().iterator();
            while (it.hasNext()) {
                String curr = (String) it.next();
                if (reverseMap.containsKey(curr)) {
                    resultMap.put((String) reverseMap.get(curr), (Set) attrMap
                            .get(curr));
                } else {
                    resultMap.put(curr, (Set) attrMap.get(curr));
                }
            }
        }
        return resultMap;
    }

    private IdSearchResults combineSearchResults(SSOToken token,
            Object[][] arrayOfResult, int sizeOfArray, IdType type,
            String orgName, boolean amsdkIncluded, Object[][] amsdkResults) {
        Map amsdkDNs = new CaseInsensitiveHashMap();
        Map resultsMap = new CaseInsensitiveHashMap();
        int errorCode = IdSearchResults.SUCCESS;
        if (amsdkIncluded) {
            RepoSearchResults amsdkRepoRes = (RepoSearchResults) 
                amsdkResults[0][0];
            Set results = amsdkRepoRes.getSearchResults();
            Map attrResults = amsdkRepoRes.getResultAttributes();
            Iterator it = results.iterator();
            while (it.hasNext()) {
                String dn = (String) it.next();
                String name = LDAPDN.explodeDN(dn, true)[0];
                amsdkDNs.put(name, dn);
                Set attrMaps = new HashSet();
                attrMaps.add((Map) attrResults.get(dn));
                resultsMap.put(name, attrMaps);
            }
            errorCode = amsdkRepoRes.getErrorCode();
        }
        for (int i = 0; i < sizeOfArray; i++) {
            RepoSearchResults current = (RepoSearchResults) arrayOfResult[i][0];
            Map configMap = (Map) arrayOfResult[i][1];
            Iterator it = current.getSearchResults().iterator();
            Map allAttrMaps = current.getResultAttributes();
            while (it.hasNext()) {
                String m = (String) it.next();
                String mname = DNUtils.DNtoName(m);
                Map attrMap = (Map) allAttrMaps.get(m);
                attrMap = reverseMapAttributeNames(attrMap, configMap);
                Set attrMaps = (Set) resultsMap.get(mname);
                if (attrMaps == null) {
                    attrMaps = new HashSet();
                }
                attrMaps.add(attrMap);
                resultsMap.put(mname, attrMaps);
            }
        }
        IdSearchResults results = new IdSearchResults(type, orgName);
        Iterator it = resultsMap.keySet().iterator();
        while (it.hasNext()) {
            String mname = (String) it.next();
            Map combinedMap = combineAttrMaps((Set) resultsMap.get(mname), 
                    true);
            AMIdentity id = new AMIdentity(token, mname, type, orgName,
                    (String) amsdkDNs.get(mname));
            results.addResult(id, combinedMap);
        }
        results.setErrorCode(errorCode);
        return results;
    }

    private Map[] getAttributeNameMap(Map configMap) {
        Set attributeMap = (Set) configMap.get(IdConstants.ATTR_MAP);

        if (attributeMap == null || attributeMap.isEmpty()) {
            return null;
        } else {
            Map returnArray[] = new Map[2];
            int size = attributeMap.size();
            returnArray[0] = new CaseInsensitiveHashMap(size);
            returnArray[1] = new CaseInsensitiveHashMap(size);
            Iterator it = attributeMap.iterator();
            while (it.hasNext()) {
                String mapString = (String) it.next();
                int eqIndex = mapString.indexOf('=');
                if (eqIndex > -1) {
                    String first = mapString.substring(0, eqIndex);
                    String second = mapString.substring(eqIndex + 1);
                    returnArray[0].put(first, second);
                    returnArray[1].put(second, first);
                } else {
                    returnArray[0].put(mapString, mapString);
                    returnArray[1].put(mapString, mapString);
                }
            }
            return returnArray;
        }
    }

    private Map combineAttrMaps(Set setOfMaps, boolean isString) {
        // Map resultMap = new CaseInsensitiveHashMap();
        Map resultMap = new AMHashMap();
        Iterator it = setOfMaps.iterator();
        while (it.hasNext()) {
            Map currMap = (Map) it.next();
            if (currMap != null) {
                Iterator keyset = currMap.keySet().iterator();
                while (keyset.hasNext()) {
                    String thisAttr = (String) keyset.next();
                    if (isString) {
                        Set resultSet = (Set) resultMap.get(thisAttr);
                        Set thisSet = (Set) currMap.get(thisAttr);
                        if (resultSet != null) {
                            resultSet.addAll(thisSet);
                        } else {
                            /*
                             * create a new Set so that we do not alter the set
                             * that is referenced in setOfMaps
                             */
                            resultSet = new HashSet((Set) 
                                    currMap.get(thisAttr));
                            resultMap.put(thisAttr, resultSet);
                        }
                    } else { // binary attributes

                        byte[][] resultSet = (byte[][]) resultMap.get(thisAttr);
                        byte[][] thisSet = (byte[][]) currMap.get(thisAttr);
                        int combinedSize = thisSet.length;
                        if (resultSet != null) {
                            combinedSize = resultSet.length + thisSet.length;
                            byte[][] tmpSet = new byte[combinedSize][];
                            for (int i = 0; i < resultSet.length; i++) {
                                tmpSet[i] = (byte[]) resultSet[i];
                            }
                            for (int i = 0; i < thisSet.length; i++) {
                                tmpSet[i] = (byte[]) thisSet[i];
                            }
                            resultSet = tmpSet;
                        } else {
                            resultSet = (byte[][]) thisSet.clone();
                        }
                        resultMap.put(thisAttr, resultSet);

                    }

                }
            }
        }
        return resultMap;
    }
}
