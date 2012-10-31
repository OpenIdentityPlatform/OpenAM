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
 * $Id: EntityModelImpl.java,v 1.20 2009/12/25 09:13:22 babysunil Exp $
 *
 */

package com.sun.identity.console.federation.model;

import com.sun.identity.console.base.model.AMModelBase;
import com.sun.identity.console.base.model.AMConsoleException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;

import com.sun.identity.federation.meta.IDFFMetaManager;
import com.sun.identity.federation.meta.IDFFMetaException;
import com.sun.identity.liberty.ws.meta.jaxb.AffiliationDescriptorType;
import com.sun.identity.liberty.ws.meta.jaxb.ObjectFactory;

import com.sun.identity.shared.datastruct.OrderedSet;
import com.sun.identity.saml2.meta.SAML2MetaUtils;
import com.sun.identity.saml2.meta.SAML2MetaManager;
import com.sun.identity.saml2.meta.SAML2MetaException;
import com.sun.identity.saml2.jaxb.metadata.EntityDescriptorElement;

import com.sun.identity.wsfederation.meta.WSFederationMetaManager;
import com.sun.identity.wsfederation.meta.WSFederationMetaException;
import com.sun.identity.wsfederation.jaxb.wsfederation.FederationElement;
import com.sun.identity.wsfederation.jaxb.wsfederation.UriNamedClaimTypesOfferedElement;
import com.sun.identity.wsfederation.jaxb.wsfederation.TokenIssuerEndpointElement;

public class EntityModelImpl extends AMModelBase implements EntityModel {
    
    private Set realms = null;
    
    public EntityModelImpl(HttpServletRequest req, Map map) {
        super(req, map);
        try {
            realms = getRealmNames(getStartDN(), "*");
        } catch (AMConsoleException a) {
            debug.warning("EntityModel problem getting realm names");
            realms = Collections.EMPTY_SET;
        }
    }
    
    /**
     * Returns a map with all entities including data about
     * what realm, the roles, and location of each entity.
     *
     * @throws AMConsoleException if unable to retrieve the entities.
     */
    public Map getEntities()
        throws AMConsoleException 
    {
        Map allEntities = getSAMLv2Entities();
        allEntities.putAll(getIDFFEntities());
        allEntities.putAll(getWSFedEntities());
        
        return allEntities;
    }
    
    /**
     * Returns a map of all the samlv2 entities including data about
     * what realm, the roles, and location of each entity.
     *
     * @throws AMConsoleException if unable to retrieve the Samlv2 entities.
     */
    public Map getSAMLv2Entities()
        throws AMConsoleException 
    {
        Map samlv2Map = new HashMap();
        
        try {
            SAML2MetaManager samlManager = new SAML2MetaManager();
            for (Iterator i = realms.iterator(); i.hasNext(); ) {
                String realmName = (String)i.next();
                
                Set samlEntities = samlManager.getAllEntities(realmName);
                List hostedEntities =
                    samlManager.getAllHostedEntities(realmName);
                for (Iterator j = samlEntities.iterator(); j.hasNext();) {
                    String entityName = (String)j.next();
                    
                    Map data = new HashMap(8);
                    data.put(REALM, realmName);
                    // get the roles this entity is acting in
                    data.put(ROLE,
                        listToString(getSAMLv2Roles(entityName, realmName)));
                    
                    data.put(PROTOCOL, SAMLV2);
                    
                    if (isAffiliate(SAMLV2, realmName, entityName)) {
                        data.put(LOCATION, "");                                        
                    } else if ((hostedEntities != null) &&
                        hostedEntities.contains(entityName)) 
                    {
                        data.put(LOCATION, HOSTED);
                    } else {
                        data.put(LOCATION, REMOTE);
                    }

                    String entityNamewithRealm = entityName+","+realmName;
                    
                    samlv2Map.put(entityNamewithRealm, (HashMap)data);
                }
            }
        } catch (SAML2MetaException e) {
            debug.error("EntityModel.getSAMLv2Entities", e);
            throw new AMConsoleException(e.getMessage());
        }
        
        return (samlv2Map != null) ? samlv2Map : Collections.EMPTY_MAP;
    }
    
    /**
     * Returns a map of all the idff entities including data about
     * what realm, the roles, and location of each entity.
     *
     * @throws AMConsoleException if unable to retrieve the IDFF entities.
     */
    public Map getIDFFEntities()
        throws AMConsoleException 
    {
        Map idffMap = new HashMap();
        try {
            IDFFMetaManager idffManager = new IDFFMetaManager(null);
            
            for (Iterator j = realms.iterator(); j.hasNext(); ) {
                String realm = (String)j.next();
                
                Set entities = idffManager.getAllEntities(realm);
                List hostedEntities = idffManager.getAllHostedEntities(realm);
                
                for (Iterator i = entities.iterator(); i.hasNext();) {
                    String name = (String)i.next();
                    
                    Map data = new HashMap(8);
                    
                    data.put(REALM, realm);
                    
                    data.put(PROTOCOL, IDFF);
                    data.put(ROLE, listToString(getIDFFRoles(name, realm)));
                    if(isAffiliate(IDFF, realm, name)){
                        data.put(LOCATION, "");
                    } else if ((hostedEntities != null) && 
                        hostedEntities.contains(name)) {
                        data.put(LOCATION, HOSTED);
                    } else {
                        data.put(LOCATION, REMOTE);
                    }

                    String entityNamewithRealm = name+","+realm;
                    idffMap.put(entityNamewithRealm, (HashMap)data);
                }
            }
        } catch (IDFFMetaException e) {
            debug.warning("EntityModel.getIDFFEntities", e);
            throw new AMConsoleException(e.getMessage());
        }
        
        return (idffMap != null) ? idffMap : Collections.EMPTY_MAP;
    }
    
    /**
     * Returns a map of all the wsfed entities including data about
     * what realm, the roles, and location of each entity.
     *
     * @throws AMConsoleException if unable to retrieve the WSFED entities.
     */
    public Map getWSFedEntities()
        throws AMConsoleException 
    {
        Map wsfedMap = new HashMap();
        for (Iterator i = realms.iterator(); i.hasNext(); ) {
            String realm = (String)i.next();
            
            try {
                WSFederationMetaManager metaManager = 
                    new WSFederationMetaManager();
                Set wsfedEntities =
                    metaManager.getAllEntities(realm);
                List hosted =
                    metaManager.getAllHostedEntities(realm);
                for (Iterator j = wsfedEntities.iterator(); j.hasNext(); ) {
                    String entity = (String)j.next();
                    Map data = new HashMap(8);
                    data.put(REALM, realm);
                    data.put(PROTOCOL, WSFED);
                    data.put(ROLE, listToString(getWSFedRoles(entity, realm)));
                    if ((hosted != null) && (hosted.contains(entity))) {
                        data.put(LOCATION, HOSTED);
                    } else {
                        data.put(LOCATION, REMOTE);
                    }

                     String entityNamewithRealm = entity+","+realm;
                    wsfedMap.put(entityNamewithRealm, (HashMap)data);
                }
            } catch (WSFederationMetaException e) {
                debug.error("EntityModel.getWSFedEntities", e);
                throw new AMConsoleException(e.getMessage());
            }
        }
        
        return (wsfedMap != null) ?
            wsfedMap : Collections.EMPTY_MAP;
    }
    
    /**
     * This is a convenience routine that can be used
     * to convert a List of String objects to a single String in the format of
     *     "one; two; three"
     */
    private String listToString(List roleNames) {
        StringBuffer sb = new StringBuffer();
        for (Iterator i = roleNames.iterator(); i.hasNext(); ) {
            String role = (String)i.next();
            if (sb.length() > 0) {
                sb.append("; ");
            }
            sb.append(getLocalizedString(role + ".label"));
        }
        return sb.toString();
    }
    
    /**
     * Creates an entity.
     *
     * @param data which contains the attributes of the entity to be created.
     * @throws AMConsoleException if unable to create entity.
     */
    public void createEntity(Map data) throws AMConsoleException {
        String protocol = (String)data.remove(PROTOCOL);
        if (protocol.equals(SAMLV2)) {
            createSAMLv2Provider(data);
        } else if (protocol.equals(WSFED)) {
            createWSFedProvider(data);
        } else if (protocol.equals(IDFF)) {
            createIDFFProvider(data);
        }
    }
    
    /*
     * TBD what is the best approach for creating a new provider with
     * minimal input from the user
     *
     */
    
    private void createSAMLv2Provider(Map data) throws AMConsoleException {
        throw new AMConsoleException("create SAML not implemented yet");
    }
    
    private void createWSFedProvider(Map data) throws AMConsoleException {
        throw new AMConsoleException("create WSFed not implemented yet");
    }
    
    private void createIDFFProvider(Map data) throws AMConsoleException {
        throw new AMConsoleException("create IDFF not implemented yet");
    }
    
    /**
     * Deletes the entity specified.
     *
     * @param name Name of entity descriptor.
     * @param protocol Protocol to which entity belongs.
     * @param realm the realm in which the entity resides.
     *
     * @throws AMConsoleException if unable to delete entitiy.
     */
    public void deleteEntities(
        String name, 
        String protocol, 
        String realm
    ) throws AMConsoleException {       
        if (protocol.equals(IDFF)) {
            deleteIDFFEntity(name, realm);
        } else if (protocol.equals(WSFED)) {
            deleteWSFedEntity(name,realm);
        } else {
            deleteSAMLv2Entity(name,realm);
        }
    }
    
    private void deleteSAMLv2Entity(String entityID, String realm)
        throws AMConsoleException 
    {
        try {
            SAML2MetaManager metaManager = new SAML2MetaManager();
            metaManager.deleteEntityDescriptor(realm, entityID);
        } catch (SAML2MetaException e) {
            throw new AMConsoleException("delete.entity.exists.error");
        }
    }
    
    private void deleteIDFFEntity(String entityID, String realm)
        throws AMConsoleException 
    {
        try {
            IDFFMetaManager metaManager = new IDFFMetaManager(null);
            
            metaManager.deleteEntityDescriptor(realm, entityID);
            
        } catch (IDFFMetaException e) {
            throw new AMConsoleException(e.getMessage());
        }
    }
    
    private void deleteWSFedEntity(String entityID, String realm)
        throws AMConsoleException 
    {
        try {
            (new WSFederationMetaManager()).deleteFederation(
                realm, entityID); 
        } catch (WSFederationMetaException w) {
            debug.warning("EntityModel.deleteWSFedEntity", w);
            throw new AMConsoleException(w.getMessage());
        }
    }
    
    /*
     * This is used to determine what 'roles' a particular entity is
     * acting as. It will producs a list of role names which can then
     * be used by the calling routine for whatever purpose it needs.
     */
    public List getIDFFRoles(String entity, String realm) {
        List roles = new ArrayList(6);
        
        try {
            IDFFMetaManager idffManager = new IDFFMetaManager(null);
            
            // find out what role this dude is playing
            if (idffManager.getIDPDescriptor(realm, entity) != null) {
                roles.add(IDENTITY_PROVIDER);
            }
            if (idffManager.getSPDescriptor(realm, entity) != null) {
                roles.add(SERVICE_PROVIDER);
            }
            if(idffManager.getAffiliationDescriptor(realm, entity) != null) {
                roles.add(AFFILIATE);
            }
        } catch (IDFFMetaException s) {
            if (debug.warningEnabled()) {
                debug.warning("EntityModel.getIDFFRoles() - " +
                    "Couldn't get SAMLMetaManager");
            }
        }
        
        return roles;
    }
    
    public List getWSFedRoles(String entity, String realm) {
        List roles = new ArrayList(4);
        boolean isSP = true;
        int cnt = 0;
        try {
            WSFederationMetaManager metaManager = 
                new WSFederationMetaManager();
            if (metaManager.getIDPSSOConfig(realm,entity) != null) {
                roles.add(IDENTITY_PROVIDER);
            }
            if (metaManager.getSPSSOConfig(realm, entity) != null) {
                roles.add(SERVICE_PROVIDER);
            }
            
            //to handle dual roles specifically for WSFED
            if (roles.isEmpty()) {
                FederationElement fedElem =
                    metaManager.getEntityDescriptor(realm, entity);
                if (fedElem != null) {
                    for (Iterator iter = fedElem.getAny().iterator(); 
                        iter.hasNext(); ) 
                    {
                        Object o = iter.next();
                        if (o instanceof UriNamedClaimTypesOfferedElement) {
                            roles.add(IDENTITY_PROVIDER);
                            isSP = false; 
                        } else if (o instanceof TokenIssuerEndpointElement) {
                            cnt++;
                        }
                    }
                    if ((isSP) || (cnt >1)) {  
                        roles.add(SERVICE_PROVIDER);
                    } 
                }
            }
        } catch (WSFederationMetaException e) {
            debug.warning("EntityModelImpl.getWSFedRoles", e); 
        }
        return (roles != null) ? roles : Collections.EMPTY_LIST;
    }

    /*
     * This is used to determine what 'roles' a particular entity is
     * acting as. It will producs a list of role names which can then
     * be used by the calling routine for whatever purpose it needs.
     */
    public List getSAMLv2Roles(String entity, String realm) {
        List roles = new ArrayList();
        
        try {
            SAML2MetaManager samlManager = new SAML2MetaManager();
            EntityDescriptorElement d =
                samlManager.getEntityDescriptor(realm, entity);
            
            if (d != null) {
                // find out what role this dude is playing
                if (SAML2MetaUtils.getSPSSODescriptor(d) != null) {
                    roles.add(SERVICE_PROVIDER);
                }
                if (SAML2MetaUtils.getIDPSSODescriptor(d) != null) {
                    roles.add(IDENTITY_PROVIDER);
                }
                if (SAML2MetaUtils.getPolicyDecisionPointDescriptor(d) != null) {
                    roles.add(POLICY_DECISION_POINT_DESCRIPTOR);
                }
                if (SAML2MetaUtils.getPolicyEnforcementPointDescriptor(d) != null) {
                    roles.add(POLICY_ENFORCEMENT_POINT_DESCRIPTOR);
                }
                if (SAML2MetaUtils.
                        getAttributeAuthorityDescriptor(d) != null) {
                    roles.add(SAML_ATTRAUTHORITY);
                }
                if (SAML2MetaUtils.getAuthnAuthorityDescriptor(d) != null) {
                    roles.add(SAML_AUTHNAUTHORITY);
                }
                if (SAML2MetaUtils.getAttributeQueryDescriptor(d) != null) {
                    roles.add(SAML_ATTRQUERY);
                }
                if (samlManager.getAffiliationDescriptor(realm, entity) != null) {
                    roles.add(AFFILIATE);
                }
            }
        } catch (SAML2MetaException s) {
            if (debug.warningEnabled()) {
                debug.warning("EntityModel.getSAMLv2Roles() - " +
                    "Couldn't get SAMLMetaManager");
            }
        }
        
        return (roles != null) ? roles : Collections.EMPTY_LIST;
    }
    
    private Map createTabEntry(String type) {
        Map tab = new HashMap(12);
        tab.put("label", "federation." + type + ".label");
        tab.put("status", "federation." + type + ".status");
        tab.put("tooltip", "federation." + type + ".tooltip");
        tab.put("url", "../federation/" + type);
        tab.put("viewbean", 
            "com.sun.identity.console.federation." + type + "ViewBean");
        tab.put("permissions", "sunAMRealmService");
        
        return tab;
    }
    
    /*
     * Creates a list of tab entries dynamically based on the roles supported
     * for an entity.
     *
     *@param protocol the protocl which the entity belongs to.
     *@param entity Name of entity descriptor.
     *@param realm the realm in which the entity resides.
     */
    public List getTabMenu(String protocol, String entity, String realm) {
        List entries = new ArrayList();
        List roles = new ArrayList();
        
        // do not localize General. Its the name of a class file.
        if (protocol.equals(WSFED)) {
            roles.add("General");
        }
        
        if (protocol.equals(IDFF)) {
            roles.addAll(getIDFFRoles(entity, realm));
        } else {
            roles.addAll(getWSFedRoles(entity, realm));
        }
        
        // create a tab for each role type
        for (Iterator type = roles.iterator(); type.hasNext(); ) {
            String name = protocol + (String)type.next();
            entries.add(createTabEntry(name));
        }
        
        return entries;
    }
    
    /**
     * Returns true if entity descriptor is an affiliate.
     *
     * @param protocol the Protocol to which entity belongs.
     * @param realm the realm in which the entity resides.
     * @param name Name of entity descriptor.
     * @return true if entity descriptor is an affiliate.
     */
    public boolean isAffiliate(String protocol, String realm, String name)
    throws AMConsoleException {
        boolean isAffiliate = false;
        com.sun.identity.liberty.ws.meta.jaxb.AffiliationDescriptorType
                idff_ad = null;
        com.sun.identity.saml2.jaxb.metadata.AffiliationDescriptorType
                samlv2_sd = null;        
        try {
            if (protocol.equals(IDFF)) {
                IDFFMetaManager idffManager = new IDFFMetaManager(null);
                idff_ad = (
                com.sun.identity.liberty.ws.meta.jaxb.AffiliationDescriptorType)
                    idffManager.getAffiliationDescriptor(realm, name);                
            } else if (protocol.equals(SAMLV2)) {
                SAML2MetaManager samlManager = new SAML2MetaManager();
                samlv2_sd = (
                com.sun.identity.saml2.jaxb.metadata.AffiliationDescriptorType)
                    samlManager.getAffiliationDescriptor(realm, name);
            }      
            if (idff_ad != null || samlv2_sd != null ) {
                isAffiliate = true;               
            }
        } catch (IDFFMetaException  e) {
            if (debug.warningEnabled()) {
                debug.warning("EntityModelImpl.isAffiliate", e);
            }
            throw new AMConsoleException(getErrorString(e));
        } catch (SAML2MetaException s) {
            if (debug.warningEnabled()) {
                debug.warning("EntityModel.isAffiliate() - " +
                        "Couldn't get SAMLMetaManager");
            }
            throw new AMConsoleException(getErrorString(s));
        }
        return isAffiliate;
    }


    protected Set returnEmptySetIfValueIsNull(boolean b) {
        Set set = new HashSet(2);
        set.add(Boolean.toString(b));
        return set;
    }
    
    protected Set returnEmptySetIfValueIsNull(String str) {
        Set set = Collections.EMPTY_SET;
        if (str != null) {
            set = new HashSet(2);
            set.add(str);
        }
        return set;
    }
    
    protected Set returnEmptySetIfValueIsNull(Set set) {
        return (set != null) ? set : Collections.EMPTY_SET;
    }
    
    protected Set returnEmptySetIfValueIsNull(List l) {
        Set set = new HashSet();
        int size = l.size();
        for (int i=0;i<size;i++){
            set.add(l.get(i));
        }
        return set;
    }

    protected List returnEmptyListIfValueIsNull(String str) {
        List list = Collections.EMPTY_LIST;
        if (str != null) {
            list = new ArrayList(2);
            list.add(str);
        }
        return list;
    }
    
    protected List returnEmptyListIfValueIsNull(List list) {
        return (list != null) ? list : Collections.EMPTY_LIST;
    }
    
    protected OrderedSet convertListToSet(List list) {
        OrderedSet s = new OrderedSet();
        for (int i=0; i<list.size();i++){
            s.add(list.get(i));
        }
        return s;
    }
    
    protected List convertSetToList(Set set) {
        List list = new ArrayList();
        Iterator it = set.iterator();
        while (it.hasNext()) {
            list.add(it.next());
        }
        return list;
    }

    protected Map convertSetToListInMap(Map map) {
        Map tmpMap = new HashMap();
        Set entries = map.entrySet();
        Iterator iterator = entries.iterator();
        while (iterator.hasNext()) {
            Map.Entry entry = (Map.Entry)iterator.next();
            tmpMap.put((String)entry.getKey(),
                convertSetToList((Set)entry.getValue()));
        }
        return tmpMap;
    }
}
