/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: FSAuthDomainsModelImpl.java,v 1.12 2009/11/10 01:19:49 exu Exp $
 *
 */

package com.sun.identity.console.federation.model;

import com.sun.identity.console.base.model.AMAdminUtils;
import com.sun.identity.console.base.model.AMModelBase;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.federation.meta.IDFFMetaException;
import com.sun.identity.federation.meta.IDFFMetaManager;
import com.sun.identity.wsfederation.meta.WSFederationMetaException;
import com.sun.identity.wsfederation.meta.WSFederationMetaManager;
import com.sun.identity.cot.CircleOfTrustManager;
import com.sun.identity.cot.CircleOfTrustDescriptor;
import com.sun.identity.cot.COTException;
import com.sun.identity.cot.COTConstants;
import com.sun.identity.saml2.meta.SAML2MetaException;
import com.sun.identity.saml2.meta.SAML2MetaManager;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import javax.servlet.http.HttpServletRequest;

public class FSAuthDomainsModelImpl extends AMModelBase
    implements FSAuthDomainsModel 
{
    private CircleOfTrustManager cotManager;
    private static Map DATA_MAP = new HashMap(10);
    
    static {
        DATA_MAP.put(TF_NAME, Collections.EMPTY_SET);
        DATA_MAP.put(TF_DESCRIPTION, Collections.EMPTY_SET);
        DATA_MAP.put(TF_IDFF_WRITER_SERVICE_URL, Collections.EMPTY_SET);
        DATA_MAP.put(TF_IDFF_READER_SERVICE_URL, Collections.EMPTY_SET);
        DATA_MAP.put(TF_SAML2_WRITER_SERVICE_URL, Collections.EMPTY_SET);
        DATA_MAP.put(TF_SAML2_READER_SERVICE_URL, Collections.EMPTY_SET);
        DATA_MAP.put(SINGLE_CHOICE_STATUS, Collections.EMPTY_SET);
        DATA_MAP.put(SINGLE_CHOICE_REALM, Collections.EMPTY_SET);
    }
    
    /**
     * Creates a simple model using default resource bundle.
     *
     * @param req HTTP Servlet Request
     * @param map of user information
     */
    public FSAuthDomainsModelImpl(HttpServletRequest req,  Map map) {
        super(req, map);
    }
    
    /**
     * Returns a &lt;code>Set&lt;/code> of all the authentication domains 
     * starting from the root realm.
     *
     * @return a Set of authentication domain names.
     */
    public Set getAuthenticationDomains() {
        Set results = null;
        String realm = "";
        try {
            CircleOfTrustManager manager = getCircleOfTrustManager();
            Set realms = getRealmNames("/", "*");

            for (Iterator i = realms.iterator(); i.hasNext(); ) {
                realm = (String)i.next();
                results.addAll(manager.getAllCirclesOfTrust(realm));                
            }
        } catch (COTException e) {
            String[] paramsEx = {realm, getErrorString(e)};
            logEvent("FEDERATION_EXCEPTION_GET_AUTH_DOMAINS", paramsEx);
            debug.warning(
                "FSAuthDomainsModelImpl.getAuthenticationDomains", e);
        } catch (AMConsoleException e){
            debug.warning(
                "FSAuthDomainsModelImpl.getAuthenticationDomains", e);
        }
        return (results != null) ? results : Collections.EMPTY_SET;
    }
    
    public Set getCircleOfTrustDescriptors() {
        Set descSet = new HashSet();
        String realm = COTConstants.ROOT_REALM;
        try {
            CircleOfTrustManager manager = getCircleOfTrustManager();
            Set realmSet = getRealmNames("/", "*");    
            for (Iterator i = realmSet.iterator(); i.hasNext(); ) {
                realm = (String)i.next();
                Set cotSet = manager.getAllCirclesOfTrust(realm);
                for (Iterator j = cotSet.iterator(); j.hasNext(); ) {              
                    String cotName = (String)j.next();
                    CircleOfTrustDescriptor descriptor = 
                        manager.getCircleOfTrust(realm, cotName);
                    descSet.add(descriptor);
                }
            }
        } catch (COTException e) {            
            String[] paramsEx = {realm, getErrorString(e)};
            logEvent("FEDERATION_EXCEPTION_GET_AUTH_DOMAINS", paramsEx);
            debug.warning(
                    "FSAuthDomainsModelImpl.getAuthenticationDomains", e);
        } catch (AMConsoleException e){
            debug.warning(
                    "FSAuthDomainsModelImpl.getAuthenticationDomains", e);
        }
        return descSet;
    }
    
    /**
     * Creates authentication domain.
     *
     * @param attrValues Map of attribute name to set of attribute values.
     * @throws AMConsoleException if authentication domain created.
     */
    public void createAuthenticationDomain(Map attrValues, Set providers)
        throws AMConsoleException 
    {
        String realm = (String)AMAdminUtils.getValue(
            (Set)attrValues.get(SINGLE_CHOICE_REALM));
        String status = (String)AMAdminUtils.getValue(
            (Set)attrValues.get(SINGLE_CHOICE_STATUS));
        String name = (String)AMAdminUtils.getValue(
            (Set)attrValues.get(TF_NAME));
        if (name.trim().length() == 0) {
            throw new AMConsoleException(
                "authdomain.authentication.domain.name.missing.message");
        }
        String[] param = {name};
        logEvent("ATTEMPT_CREATE_AUTH_DOMAIN", param);
        try {
            CircleOfTrustDescriptor descriptor =
                new CircleOfTrustDescriptor(name, realm, status);
            descriptor.setTrustedProviders(providers);
            descriptor.setCircleOfTrustDescription(
                (String)AMAdminUtils.getValue(
                (Set)attrValues.get(TF_DESCRIPTION)));
            descriptor.setIDFFReaderServiceURL(
                (String)AMAdminUtils.getValue(
                (Set)attrValues.get(TF_IDFF_READER_SERVICE_URL)));
            descriptor.setIDFFWriterServiceURL(
                (String)AMAdminUtils.getValue(
                (Set)attrValues.get(TF_IDFF_WRITER_SERVICE_URL)));
            descriptor.setSAML2ReaderServiceURL(
                (String)AMAdminUtils.getValue(
                (Set)attrValues.get(TF_SAML2_READER_SERVICE_URL)));
            descriptor.setSAML2WriterServiceURL(
                (String)AMAdminUtils.getValue(
                (Set)attrValues.get(TF_SAML2_WRITER_SERVICE_URL)));
            CircleOfTrustManager manager = getCircleOfTrustManager();
            manager.createCircleOfTrust(realm,descriptor);
            logEvent("SUCCEED_CREATE_AUTH_DOMAIN", param);
        } catch (COTException e) {
            String strError = getErrorString(e);
            String[] paramsEx = {name, strError};
            logEvent("FEDERATION_EXCEPTION_CREATE_AUTH_DOMAIN", paramsEx);
            throw new AMConsoleException(strError);
        }
    }
    
    /**
     * Deletes an authentication domain (circle of trust) within a given realm.
     *
     * @param realm name of realm where authentication domain exists.
     * @param cotName name of the authentication domain.
     * @throws AMConsoleException if authentication domain cannot be deleted.
     */
    public void deleteAuthenticationDomain(String realm, String cotName)
        throws AMConsoleException 
    {    
        String[] param = {realm, cotName};
        logEvent("ATTEMPT_DELETE_AUTH_DOMAINS", param);
        try {
            CircleOfTrustManager manager = getCircleOfTrustManager();
            manager.deleteCircleOfTrust(realm, cotName);
            logEvent("SUCCEED_DELETE_AUTH_DOMAIN", param);
        } catch (COTException e) {
            String strError = getErrorString(e);
            String[] paramsEx = {realm, cotName, strError};
            logEvent("FEDERATION_EXCEPTION_DELETE_AUTH_DOMAIN", paramsEx);
            throw new AMConsoleException(strError);
        }
    }
    
    private CircleOfTrustManager getCircleOfTrustManager()
        throws COTException 
    {
        if (cotManager == null) {
            cotManager = new  CircleOfTrustManager();
        }
        return cotManager;
    }
    
    /**
     * Returns attribute values.
     *
     * @param name Name of authentication domain.
     * @return Map of attribute name to values.
     * @throws AMConsoleException if attribute values cannot be retrieved.
     */
    public Map getAttributeValues(String realm, String name)
        throws AMConsoleException 
    {
        Map values = new HashMap(16);
        String[] param = {realm, name};
        logEvent("ATTEMPT_GET_AUTH_DOMAIN_ATTR_VALUES", param);
        try {
            CircleOfTrustManager manager = getCircleOfTrustManager();
            CircleOfTrustDescriptor desc =
                    manager.getCircleOfTrust(realm, name);
            values.put(TF_DESCRIPTION, AMAdminUtils.wrapInSet(
                    desc.getCircleOfTrustDescription()));
            values.put(TF_IDFF_WRITER_SERVICE_URL, AMAdminUtils.wrapInSet(
                    desc.getIDFFWriterServiceURL()));
            values.put(TF_IDFF_READER_SERVICE_URL, AMAdminUtils.wrapInSet(
                    desc.getIDFFReaderServiceURL()));
            values.put(TF_SAML2_WRITER_SERVICE_URL, AMAdminUtils.wrapInSet(
                    desc.getSAML2WriterServiceURL()));
            values.put(TF_SAML2_READER_SERVICE_URL, AMAdminUtils.wrapInSet(
                    desc.getSAML2ReaderServiceURL()));
            values.put(SINGLE_CHOICE_REALM, AMAdminUtils.wrapInSet(
                    desc.getCircleOfTrustRealm()));
            values.put(SINGLE_CHOICE_STATUS, AMAdminUtils.wrapInSet(
                    desc.getCircleOfTrustStatus()));
            logEvent("SUCCEED_GET_AUTH_DOMAIN_ATTR_VALUES", param);
        } catch (COTException e) {
            String strError = getErrorString(e);
            String[] paramsEx = {realm, name, strError};
            logEvent("FEDERATION_EXCEPTION_GET_AUTH_DOMAIN_ATTR_VALUES",
                paramsEx);
            throw new AMConsoleException(strError);
        }
        
        return values;
    }
    
    /**
     * Set attribute values.
     *
     * @param name Name of authentication domain.
     * @param values Map of attribute name to value.
     * @throws IDFFMetaException if attribute values cannot be set.
     */
    public void setAttributeValues(String realm, String name, Map values)
        throws AMConsoleException 
    {
        String[] param = {realm, name};
        logEvent("ATTEMPT_MODIFY_AUTH_DOMAIN", param);
        try {
            CircleOfTrustManager manager =
                    getCircleOfTrustManager();
            CircleOfTrustDescriptor desc =
                    manager.getCircleOfTrust(realm, name);
            desc.setCircleOfTrustDescription((String)AMAdminUtils.getValue(
                    (Set)values.get(TF_DESCRIPTION)));
            desc.setIDFFWriterServiceURL((String)AMAdminUtils.getValue(
                    (Set)values.get(TF_IDFF_WRITER_SERVICE_URL)));
            desc.setIDFFReaderServiceURL((String)AMAdminUtils.getValue(
                    (Set)values.get(TF_IDFF_READER_SERVICE_URL)));
            desc.setSAML2WriterServiceURL((String)AMAdminUtils.getValue(
                    (Set)values.get(TF_SAML2_WRITER_SERVICE_URL)));
            desc.setSAML2ReaderServiceURL((String)AMAdminUtils.getValue(
                    (Set)values.get(TF_SAML2_READER_SERVICE_URL)));
            desc.setCircleOfTrustStatus((String)AMAdminUtils.getValue(
                    (Set)values.get(SINGLE_CHOICE_STATUS)));
            manager.modifyCircleOfTrust(realm,desc);
            logEvent("SUCCEED_MODIFY_AUTH_DOMAIN", param);
        } catch (COTException e) {
            String strError = getErrorString(e);
            String[] paramsEx = {realm, name, strError};
            logEvent("FEDERATION_EXCEPTION_MODIFY_AUTH_DOMAIN", paramsEx);
            throw new AMConsoleException(strError);
        }
    }
    
    /**
     * Returns a map of authentication domain attributes.
     *
     * @return Map of authentication domain attributes.
     */
    public Map getDataMap() {
        return DATA_MAP;
    }
    
    /**
     * Returns a &lt;code>Set&lt;/code> of provider names that exist in the
     * specified realm.
     *
     * @param realm name of the realm to search.
     * @return a set of provider names.
     * @throws AMConsoleException if provider names cannot be obtained.
     */
    public Set getAllProviderNames(String realm)
        throws AMConsoleException 
    {
        String[] params = { realm };
        logEvent("ATTEMPT_GET_ALL_PROVIDER_NAMES", params);
        
        Set availableEntities = new HashSet();
        try {
            SAML2MetaManager saml2Mgr = new SAML2MetaManager();
            Set saml2Entities = saml2Mgr.getAllEntities(realm);
            Iterator it = saml2Entities.iterator();
            while (it.hasNext()){
                String entityId = (String)it.next();
                availableEntities.add(entityId + "|saml2");
            }
        } catch (SAML2MetaException e) {
            String strError = getErrorString(e);
            throw new AMConsoleException(strError);
        }
        
        try {
            Set wsfedEntities =
                (new WSFederationMetaManager()).
                    getAllEntities(realm);
            for (Iterator i = wsfedEntities.iterator(); i.hasNext(); ) {
                String tmp = (String)i.next();
                availableEntities.add(tmp + "|wsfed");
            }
        } catch (WSFederationMetaException e) {
            debug.warning("EntityModel.getWSFedEntities", e);
            throw new AMConsoleException(e.getMessage());
        }
        
        try {
            IDFFMetaManager idffManager = new IDFFMetaManager(null);

            Set entities = idffManager.getAllEntities(realm);
            for (Iterator i = entities.iterator(); i.hasNext(); ) {
                String tmp = (String)i.next();
                availableEntities.add(tmp + "|idff");
            }
        } catch (IDFFMetaException e) {
            debug.warning("FSAuthDomainModel.getAllProviderNames", e);
            throw new AMConsoleException(e.getMessage());
        }
        
        logEvent("SUCCEED_GET_ALL_PROVIDER_NAMES", params);
        return (availableEntities != null) ?
            availableEntities : Collections.EMPTY_SET;
    }
    
    /**
     * Returns a set of provider names under a authentication domain.
     *
     * @param name Name of authentication domain.
     * @return a set of provider names under a authentication domain.
     * @throws AMConsoleException if provider names cannot be obtained.
     */
    public Set getTrustedProviderNames(String realm, String name)
        throws AMConsoleException 
    {
        Set providers = null;
        try {
            String[] param = {realm, name};
            logEvent("ATTEMPT_GET_PROVIDER_NAMES_UNDER_AUTH_DOMAIN", param);
            CircleOfTrustManager manager = getCircleOfTrustManager();
            CircleOfTrustDescriptor desc = manager.getCircleOfTrust(realm, name);
            providers = desc.getTrustedProviders();
            logEvent("SUCCEED_GET_PROVIDER_NAMES_UNDER_AUTH_DOMAIN", param);            
        } catch (COTException e) {
            String strError = getErrorString(e);
            String[] paramsEx = {realm, name, strError};
            logEvent(
                    "FEDERATION_EXCEPTION_GET_PROVIDER_NAMES_UNDER_AUTH_DOMAIN",
                    paramsEx);
            throw new AMConsoleException(strError);
        }
        
        return (providers != null) ? providers : Collections.EMPTY_SET;
    }
    
    /**
     * Adds providers.
     * @param realm realm of circle of trust
     * @param cotName Name of circle of trust
     * @param names Names provider to be added.
     * @throws AMConsoleException if provider cannot be added.
     */
    public void addProviders(String realm, String cotName, Collection names)
        throws AMConsoleException 
    {
        String cotType = COTConstants.SAML2;
        String entityId = null;
        String providerNames = AMAdminUtils.getString(names, ",", false);
        String[] params = {realm, cotName, providerNames};
        logEvent("ATTEMPT_ADD_PROVIDERS_TO_AUTH_DOMAIN", params);
        try {
            CircleOfTrustManager manager = getCircleOfTrustManager();
            CircleOfTrustDescriptor cotDescriptor =
                    manager.getCircleOfTrust(realm,cotName);
            Set existingEntity = cotDescriptor.getTrustedProviders();
            if (existingEntity != null) {
                Iterator it = existingEntity.iterator();
                while(it.hasNext()) {
                    String entityString = (String)it.next();
                    String delims = "|";
                    StringTokenizer tokens =
                            new StringTokenizer(entityString, delims);
                    if (tokens.countTokens() == 2) {
                        entityId=tokens.nextToken();
                        cotType=tokens.nextToken();
                        manager.removeCircleOfTrustMember(
                                realm,cotName,cotType, entityId);
                    }
                }
            }
            
            if (names != null) {
                int sz=names.size();
                for (int i=0; i<sz; i++) {
                    String entityString = (String)((ArrayList)names).get(i);
                    String delims = "|";
                    StringTokenizer tokens =
                        new StringTokenizer(entityString, delims);
                    
                    if (tokens.countTokens() == 2) {
                        entityId=tokens.nextToken();
                        cotType=tokens.nextToken();
                        manager.addCircleOfTrustMember(
                            realm, cotName, cotType, entityId);
                    }
                }
            }
            logEvent("SUCCEED_ADD_PROVIDERS_TO_AUTH_DOMAIN", params);
        } catch (COTException e) {
            String strError = getErrorString(e);
            String[] paramsEx = {realm, cotName, providerNames, strError};
            logEvent("FEDERATION_EXCEPTION_ADD_PROVIDERS_TO_AUTH_DOMAIN",
                    paramsEx);
            throw new AMConsoleException(strError);
        }
    }
    
    /**
     * Returns realm that have name matching
     *
     * @param name Base realm name for this search. null indicates root
     *        suffix.
     * @return realm that have name matching
     * @throws AMConsoleException if search fails.
     */
    public String getRealm(String name)
        throws AMConsoleException 
    {
        String realm = null;
        Set s = getCircleOfTrustDescriptors();
        for (Iterator iter = s.iterator(); iter.hasNext() && realm == null; ) {
            CircleOfTrustDescriptor desc = (CircleOfTrustDescriptor)iter.next();
            String cotName = desc.getCircleOfTrustName();
            if (cotName.equals(name)) {
                realm = desc.getCircleOfTrustRealm();
            }
        }
        return realm;
    }
}
