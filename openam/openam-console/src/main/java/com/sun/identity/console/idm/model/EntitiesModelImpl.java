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
 * $Id: EntitiesModelImpl.java,v 1.17 2009/09/05 01:30:46 veiming Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.console.idm.model;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.authentication.config.AMAuthConfigUtils;
import com.sun.identity.console.base.model.AMAdminConstants;
import com.sun.identity.console.base.model.AMAdminUtils;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMFormatUtils;
import com.sun.identity.console.base.model.AMModelBase;
import com.sun.identity.console.delegation.model.DelegationConfig;
import com.sun.identity.console.property.PropertyTemplate;
import com.sun.identity.console.property.PropertyXMLBuilder;
import com.sun.identity.common.BackwardCompSupport;
import com.sun.identity.common.CaseInsensitiveHashMap;
import com.sun.identity.common.configuration.AgentConfiguration;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.AMIdentityRepository;
import com.sun.identity.idm.IdConstants;
import com.sun.identity.idm.IdOperation;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdRepoFatalException;
import com.sun.identity.idm.IdSearchResults;
import com.sun.identity.idm.IdSearchControl;
import com.sun.identity.idm.IdSearchOpModifier;
import com.sun.identity.idm.IdType;
import com.sun.identity.idm.IdUtils;
import com.sun.identity.shared.locale.Locale;
import com.sun.identity.sm.AttributeSchema;
import com.sun.identity.sm.RequiredValueValidator;
import com.sun.identity.sm.SchemaType;
import com.sun.identity.sm.ServiceSchema;
import com.sun.identity.sm.ServiceSchemaManager;
import com.sun.identity.sm.ServiceManager;
import com.sun.identity.sm.ServiceConfigManager;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.SMSException;
import java.text.MessageFormat;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.StringTokenizer;
import javax.servlet.http.HttpServletRequest;

/* - LOG COMPLETE - */

public class EntitiesModelImpl
    extends AMModelBase
    implements EntitiesModel
{
    private static final String AGENT_ATTRIBUTE_LIST =
        "sunIdentityServerDeviceKeyValue";
    private static final String RADIO_AGENT_TYPE = "rbAgentType";
    private static final String RADIO_AGENT_TYPE_GENERIC = "generic";
    private static final String RADIO_AGENT_TYPE_WSC = "wsc";
    private static final String RADIO_AGENT_TYPE_WSP = "wsp";
    private static boolean isWSSEnabled = false;

    private boolean endUser = false;
    private static SSOToken adminSSOToken =
        AMAdminUtils.getSuperAdminSSOToken();
    private static RequiredValueValidator reqValidator =
        new RequiredValueValidator();
    private Map requiredAttributeNames = new HashMap();
    private Set readOnlyAttributeNames = new HashSet();

    private String type = null;
    private boolean isServicesSupported = true;

    static {
        try {
            Class clazz = Class.forName(
                "com.sun.identity.wss.security.SecurityMechanism");
            isWSSEnabled = (clazz != null);
        } catch (ClassNotFoundException e) {
            //ignored
        }
    }
    
    public EntitiesModelImpl(HttpServletRequest req, Map map) {
        super(req, map);
        type = (String)map.get("entityTypeName");
    }

    /**
     * Returns the type of <code>entity</code> object for which the model
     * was constructed. 
     *
     * @return type of <code>entity</code> object being used.
     */
    public String getEntityType() {
        return type;
    }

    /**
     * Set end user flag.
     *
     * @param endUser end user flag.
     */
    public void setEndUser(boolean endUser) {
        this.endUser = endUser;
    }

    /**
     * Returns entity names.
     *
     * @param realmName Name of Realm.
     * @param pattern Search Pattern.
     * @param strType Entity Type.
     */
    public IdSearchResults getEntityNames(
        String realmName,
        String strType,
        String pattern
    ) throws AMConsoleException {
        if (realmName == null) {
            realmName = "/";
        }
        int sizeLimit = getSearchResultLimit();
        int timeLimit = getSearchTimeOutLimit();
        String[] params = {realmName, strType, pattern,
            Integer.toString(sizeLimit), Integer.toString(timeLimit)};
        
        try {
            IdSearchControl idsc = new IdSearchControl();
            idsc.setMaxResults(sizeLimit);
            idsc.setTimeOut(timeLimit);
            idsc.setAllReturnAttributes(false);

            /*
            * For user identities we will modify the search filter so that
            * we can search on a non naming attribute. 
            */
            IdType ltype = IdUtils.getType(strType);
            if (ltype.equals(IdType.USER) && !pattern.equals("*")) {
                Map searchMap = new HashMap(2);
                Set patternSet = new HashSet(2);
                patternSet.add(pattern);
                searchMap.put(getUserSearchAttribute(), patternSet);
                
                idsc.setSearchModifiers(IdSearchOpModifier.OR, searchMap);
                
                /*
                * change the pattern to * since we are passing a searchMap.
                * pattern will be used in the default filter and given to
                * the naming attribute (uid in this case). Here we are passing
                * cn=John Doe in the searchMap, but the naming attribute is
                * set to *.
                * "(&(&(uid=*)(objectClass=inetOrgPerson))(|(cn=John Doe)))"
                */
                pattern = "*";
            }

            logEvent("ATTEMPT_SEARCH_IDENTITY", params);

            AMIdentityRepository repo = new AMIdentityRepository(
                getUserSSOToken(), realmName);
            IdSearchResults results = repo.searchIdentities(
                ltype, pattern, idsc);

            logEvent("SUCCEED_SEARCH_IDENTITY", params);
            return results;

        } catch (IdRepoException e) {
            String[] paramsEx = {realmName, strType, pattern,
                Integer.toString(sizeLimit), Integer.toString(timeLimit),
                getErrorString(e)};
            logEvent("IDM_EXCEPTION_SEARCH_IDENTITY", paramsEx);
            if (debug.warningEnabled()) {
                debug.warning("EntitiesModelImpl.getEntityNames " + 
                    getErrorString(e));
            }
            throw new AMConsoleException("no.properties");
        } catch (SSOException e) {
            String[] paramsEx = {realmName, strType, pattern,
                Integer.toString(sizeLimit), Integer.toString(timeLimit),
                getErrorString(e)};
            logEvent("SSO_EXCEPTION_SEARCH_IDENTITY", paramsEx);
            debug.warning("EntitiesModelImpl.getEntityNames ", e);
            throw new AMConsoleException(getErrorString(e));
        }
    }

    /**
     * Returns attribute values of an entity object.
     *
     * @param universalId Universal ID of the entity.
     * @param bCreate true for creation page
     * @return attribute values of an entity object.
     * @throws AMConsoleException if object cannot located.
     */
    public Map getAttributeValues(String universalId, boolean bCreate)
        throws AMConsoleException {
        String[] param = {universalId, "*"};
        logEvent("ATTEMPT_READ_IDENTITY_ATTRIBUTE_VALUE", param);
        
        try {
            AMIdentity amid = IdUtils.getIdentity(
                getUserSSOToken(), universalId);
            Map tempMap = new CaseInsensitiveHashMap();
            tempMap.putAll(amid.getAttributes());
            validateUserStatusEntry(tempMap);
            Map values = new HashMap();
      
            String agentType = null;
            boolean webJ2EEagent = false;
            Set agentTypes = amid.getAttribute("AgentType");
            if ((agentTypes != null) && !agentTypes.isEmpty()) {
                agentType = (String)agentTypes.iterator().next();
                
                webJ2EEagent = 
                    agentType.equals(AgentConfiguration.AGENT_TYPE_J2EE) ||
                    agentType.equals(AgentConfiguration.AGENT_TYPE_WEB) ||
                    agentType.equals(
                        AgentConfiguration.AGENT_TYPE_AGENT_AUTHENTICATOR);
            }
      
          
            Set attributeSchemas = getAttributeSchemas(
                amid.getType().getName(), agentType, bCreate);
            Set attributeNames = new HashSet();

            for (Iterator iter = attributeSchemas.iterator(); iter.hasNext(); ){
                AttributeSchema as = (AttributeSchema)iter.next();
                String name = as.getName();
                if (!tempMap.containsKey(name)) {
                    values.put(name, Collections.EMPTY_SET);
                } else {
                    if (webJ2EEagent && name.equals(AGENT_ATTRIBUTE_LIST)) {
                        Set newValues = new HashSet();
                        Set temp = (Set)tempMap.get(name);
                        for (Iterator i = temp.iterator(); i.hasNext();) {
                            String val = (String) i.next();
                            if (val.startsWith(AGENT_ROOT_URL)) {
                                val = val.substring(AGENT_ROOT_URL.length());
                            }
                            newValues.add(val);
                        }
                        values.put(name, newValues);
                    } else {
                        values.put(name, tempMap.get(name));
                    }
                }
                attributeNames.add(name);
            }

            for (Iterator iter = values.keySet().iterator(); iter.hasNext(); ) {
                String name = (String)iter.next();
                if (!attributeNames.contains(name)) {
                    iter.remove();
                }
            }

            logEvent("SUCCEED_READ_IDENTITY_ATTRIBUTE_VALUE", param);
            return values;
        } catch (IdRepoException e) {
            String[] paramsEx = {universalId, "*", getErrorString(e)};
            logEvent("IDM_EXCEPTION_READ_IDENTITY_ATTRIBUTE_VALUE", paramsEx);
            debug.warning("EntitiesModelImpl.getAttributeValues", e);
            throw new AMConsoleException(getErrorString(e));
        } catch (SMSException e) {
            String[] paramsEx = {universalId, "*", getErrorString(e)};
            logEvent("SMS_EXCEPTION_READ_IDENTITY_ATTRIBUTE_VALUE", paramsEx);
            debug.warning("EntitiesModelImpl.getAttributeValues", e);
            throw new AMConsoleException(getErrorString(e));
        } catch (SSOException e) {
            String[] paramsEx = {universalId, "*", getErrorString(e)};
            logEvent("SSO_EXCEPTION_READ_IDENTITY_ATTRIBUTE_VALUE", paramsEx);
            debug.warning("EntitiesModelImpl.getAttributeValues", e);
            throw new AMConsoleException(getErrorString(e));
        }
    }

    /**
     * Returns property sheet XML for Entity Profile.
     *
     * @param realmName Name of Realm.
     * @param idType Type of Entity.
     * @param agentType agent type.
     * @param bCreate <code>true</code> for creation operation.
     * @param viewbeanClassName Class Name of View Bean.
     * @return property sheet XML for Entity Profile.
     */
    public String getPropertyXMLString(
        String realmName,
        String idType,
        String agentType,
        boolean bCreate,
        String viewbeanClassName
    ) throws AMConsoleException {
        setLocationDN(realmName);
        String xml = null;
        try {
            Set attributeSchemas = getAttributeSchemas(idType, agentType, 
                bCreate);
            String serviceName = getSvcNameForIdType(idType, agentType);
            if (serviceName != null) {
                PropertyXMLBuilder builder = new PropertyXMLBuilder(
                    serviceName, this, attributeSchemas);
                cacheAttributeValidators(attributeSchemas);
                if (!bCreate) {
                    DelegationConfig dConfig = DelegationConfig.getInstance();
                    if (!dConfig.hasPermission(realmName, null, 
                        AMAdminConstants.PERMISSION_MODIFY, this, 
                        viewbeanClassName)
                    ) {
                        builder.setAllAttributeReadOnly(true);
                    }
                }
                xml = builder.getXML(readOnlyAttributeNames,true);
            }
        } catch (AMConsoleException e) {
            debug.warning("EntitiesModelImpl.getPropertyXMLString", e); 
        } catch (IdRepoException e) {
            debug.warning("EntitiesModelImpl.getPropertyXMLString", e);
        } catch (SMSException e) {
            debug.warning("EntitiesModelImpl.getPropertyXMLString", e);
        } catch (SSOException e) {
            debug.warning("EntitiesModelImpl.getPropertyXMLString", e);
        }

        if (bCreate) {
            String xmlFile = (isWSSEnabled && idType.equalsIgnoreCase("agent"))?
                "com/sun/identity/console/propertyEntitiesAddAgentType.xml" :
                "com/sun/identity/console/propertyEntitiesAdd.xml";
            String header = AMAdminUtils.getStringFromInputStream(
                getClass().getClassLoader().getResourceAsStream(xmlFile));
            if (xml != null) {
                xml = PropertyXMLBuilder.prependXMLProperty(xml, header);
            } else {
                xml = PropertyXMLBuilder.formPropertySheetXML(header);
            }
        } else {
            String xmlFile =
                "com/sun/identity/console/propertyEntitiesEdit.xml";
            String extra = AMAdminUtils.getStringFromInputStream(
                getClass().getClassLoader().getResourceAsStream(xmlFile));
            if (xml != null) {
                xml = PropertyXMLBuilder.appendXMLProperty(xml, extra);
            } else {
                xml = PropertyXMLBuilder.formPropertySheetXML(extra, true);
            }
        }
        return xml;
    }

    private void cacheAttributeValidators(Set attributeSchemas) {
        if ((attributeSchemas != null) && !attributeSchemas.isEmpty()) {
            for (Iterator iter = attributeSchemas.iterator(); iter.hasNext();) {
                AttributeSchema as = (AttributeSchema)iter.next();
                if (isRequiredAttribute(as)) {
                    requiredAttributeNames.put(as.getName(), as);
                }
            }
        }
    }

    private boolean isRequiredAttribute(AttributeSchema as) {
        boolean isReqd = false;
        String any = as.getAny();
        if ((any != null) && (any.trim().length() > 0)) {
            StringTokenizer st = new StringTokenizer(any, "|");
            while (st.hasMoreTokens() && !isReqd) {
                String token = st.nextToken();
                isReqd = token.equals(PropertyTemplate.ANY_REQUIRED);
            }
        }
        return isReqd;
    }

    /**
     * Returns defauls values for an Entity Type.
     *
     * @param idType Type of Entity.
     * @param agentType mainly for agent type
     * @param bCreate true for creation page.
     * @throws AMConsoleException if default values cannot be obtained.
     */
    public Map getDefaultAttributeValues(
        String idType, 
        String agentType,
        boolean bCreate
    ) throws AMConsoleException {
        try {
            Set attributeSchemas =getAttributeSchemas(
                idType, agentType, bCreate);
            Map values = new HashMap(attributeSchemas.size() *2);

            for (Iterator i = attributeSchemas.iterator(); i.hasNext(); ) {
                AttributeSchema as = (AttributeSchema)i.next();
                values.put(as.getName(), as.getDefaultValues());
            }

            if (isWSSEnabled && bCreate && idType.equalsIgnoreCase("agent")) {
                Set set = new HashSet(2);
                set.add(RADIO_AGENT_TYPE_GENERIC);
                values.put(RADIO_AGENT_TYPE, set);
            }

            return values;
        } catch (IdRepoException e) {
            debug.warning("EntitiesModelImpl.getDefaultAttributeValues", e);
            throw new AMConsoleException(getErrorString(e));
        } catch (SMSException e) {
            debug.warning("EntitiesModelImpl.getDefaultAttributeValues", e);
            throw new AMConsoleException(getErrorString(e));
        } catch (SSOException e) {
            debug.warning("EntitiesModelImpl.getDefaultAttributeValues", e);
            throw new AMConsoleException(getErrorString(e));
        }
    }

    private Set getAttributeSchemas(
        String idType, 
        String agentType, 
        boolean bCreate
    ) throws SMSException, SSOException, IdRepoException {
        Set attributeSchemas = null;
        String serviceName = getSvcNameForIdType(idType, agentType);
        if (serviceName != null) {
            boolean bAgentType = (agentType != null) && 
                (agentType.length() > 0);
            ServiceSchemaManager svcSchemaMgr = new ServiceSchemaManager(
                serviceName, getUserSSOToken());

            ServiceSchema svcSchema = (bAgentType) ?
                svcSchemaMgr.getOrganizationSchema().getSubSchema(agentType) :
                svcSchemaMgr.getSchema(idType);
            if (svcSchema != null) {
                attributeSchemas = svcSchema.getAttributeSchemas();
            } else {
                attributeSchemas = Collections.EMPTY_SET;
            }

            // Clean up the Attribute Schema

            for (Iterator i = attributeSchemas.iterator(); i.hasNext();) {
                AttributeSchema as = (AttributeSchema)i.next();
                Set any = AMAdminUtils.getDelimitedValues(as.getAny(), "|");

                if (bCreate &&
                      ((!any.contains(AMAdminConstants.REQUIRED_ATTRIBUTE)) &&
                       (!any.contains(AMAdminConstants.OPTIONAL_ATTRIBUTE)))) {
                    i.remove();
                    continue;
                }
                if ((endUser && any.contains(AMAdminConstants.ADMIN_DISPLAY_ATTRIBUTE)) ||
                    (endUser && any.contains(AMAdminConstants.ADMIN_DISPLAY_READONLY_ATTRIBUTE))) {
                    i.remove();
                    continue;
                }
                if (endUser && any.contains(AMAdminConstants.DISPLAY_READONLY_ATTRIBUTE)) {
                    readOnlyAttributeNames.add(as.getName());
                }
                if (any.contains(AMAdminConstants.ADMIN_DISPLAY_READONLY_ATTRIBUTE)) {
                    readOnlyAttributeNames.add(as.getName());
                }

            }

            // get the attributes to display in create and profile pages
            if (bCreate) {
                String[] show = {"required", "optional"};
                PropertyXMLBuilder.filterAttributes(attributeSchemas, show);

                // beforeDisplay called to remove naming attr in create page
                beforeDisplay(idType, attributeSchemas);
            } else {
                String[] show = {
                        AMAdminConstants.ADMIN_DISPLAY_ATTRIBUTE,
                        AMAdminConstants.ADMIN_DISPLAY_READONLY_ATTRIBUTE,
                        AMAdminConstants.DISPLAY_ATTRIBUTE,
                        AMAdminConstants.DISPLAY_READONLY_ATTRIBUTE };
                if (!bAgentType) {
                    PropertyXMLBuilder.filterAttributes(attributeSchemas, show);
                } else {
                    for (Iterator i = attributeSchemas.iterator(); i.hasNext();) {
                        AttributeSchema as = (AttributeSchema) i.next();
                        AttributeSchema.Type type = as.getType();
                        if (type.equals(AttributeSchema.Type.VALIDATOR)) {
                            i.remove();
                        }
                    }
                }
            }
        }

        return (attributeSchemas != null)
            ? attributeSchemas : Collections.EMPTY_SET;
    }


    /**
     * Creates an entity.
     *
     * @param realmName Name of Realm.
     * @param entityName Name of Entity.
     * @param idType Type of Entity.
     * @param values Map of attribute name to Set of attribute values.
     * @throws AMConsoleException if entity cannot be created.
     */
    public void createEntity(
        String realmName,
        String entityName,
        String idType,
        Map values
    ) throws AMConsoleException {
        if (entityName.trim().length() == 0) {
            String msg = getLocalizedString("entities.missing.entityName");
            String[] param = {getLocalizedString(idType)};
            throw new AMConsoleException(MessageFormat.format(msg, (Object[])param));
        }

        if (realmName == null) {
            realmName = "/"; 
        }

        validateAttributes(values);
        setAgentDefaultValues(values);

        try {
            String[] params = {entityName, idType, realmName};
            logEvent("ATTEMPT_IDENTITY_CREATION", params);

            AMIdentityRepository repo = new AMIdentityRepository(
                getUserSSOToken(), realmName);
            beforeCreate(idType, entityName, values);
            repo.createIdentity(IdUtils.getType(idType), entityName, values);

            logEvent("IDENTITY_CREATED", params);
        } catch (IdRepoException e) {
            String strError = getErrorString(e);
            String[] params =
                {entityName, idType, realmName, strError};
            logEvent("IDM_EXCEPTION_IDENTITY_CREATION", params);
            throw new AMConsoleException(strError);
        } catch (SSOException e) {
            String strError = getErrorString(e);
            String[] params =
                {entityName, idType, realmName, strError};
            logEvent("SSO_EXCEPTION_IDENTITY_CREATION", params);
            throw new AMConsoleException(strError);
        }
    }

    private void setAgentDefaultValues(Map values)
        throws AMConsoleException {
        Set setAgentType = (Set)values.get(RADIO_AGENT_TYPE);
        if ((setAgentType != null) && !setAgentType.isEmpty()) {
            String agentType = (String)setAgentType.iterator().next();
            if (agentType.equals(RADIO_AGENT_TYPE_WSC)) {
                Set agentValues = new HashSet(6);
                agentValues.add(
                    "SecurityMech=urn:sun:wss:security:null:Anonymous");
                agentValues.add("useDefaultStore=true");
                agentValues.add("Type=wsc");
                values.put(AGENT_ATTRIBUTE_LIST, agentValues);
            } else if (agentType.equals(RADIO_AGENT_TYPE_WSP)) {
                try {
                    Class clazz = Class.forName(
                        "com.sun.identity.wss.security.SecurityMechanism");
                    Method mtd = clazz.getDeclaredMethod(
                        "getAllWSPSecurityMechanisms", (Class)null);
                    Method mtdGetURI = clazz.getDeclaredMethod("getURI", (Class)null);
                    List securityMech = (List)mtd.invoke(null, (Class)null);
                    StringBuffer securityMechStr = new StringBuffer();
                    boolean first = true;

                    for (Iterator i = securityMech.iterator(); i.hasNext(); ) {
                        Object mech = i.next();
                        if (first) {
                            first = false;
                        } else {
                            securityMechStr.append(",");
                        }
                        securityMechStr.append(
                            (String)mtdGetURI.invoke(mech, (Class)null));
                    }
                    Set agentValues = new HashSet(6);
                    agentValues.add("SecurityMech=" + securityMechStr);
                    agentValues.add("useDefaultStore=true");
                    agentValues.add("Type=wsp");
                    values.put(AGENT_ATTRIBUTE_LIST, agentValues);
                } catch (ClassNotFoundException e) {
                    throw new AMConsoleException(e);
                } catch (NoSuchMethodException e) {
                    throw new AMConsoleException(e);
                } catch (IllegalAccessException e) {
                    throw new AMConsoleException(e);
                } catch (InvocationTargetException e) {
                    throw new AMConsoleException(e);
                }
            }

            values.remove(RADIO_AGENT_TYPE);
        }
    }

    /**
     * Modifies profile of entity.
     *
     * @param realmName Name of Realm.
     * @param universalId Universal ID of the entity.
     * @param values Map of attribute name to set of attribute values.
     * @throws AMConsoleException if entity cannot be located or modified.
     */
    public void modifyEntity(String realmName, String universalId, Map values) 
        throws AMConsoleException {
        if ((values != null) && !values.isEmpty()) {
            String attrNames = AMAdminUtils.getString(
                values.keySet(), ",", false);

            try {
                AMIdentity amid = IdUtils.getIdentity(
                    getUserSSOToken(), universalId);

                validateAttributes(amid, values);
                String[] param = {universalId, attrNames};
                logEvent("ATTEMPT_MODIFY_IDENTITY_ATTRIBUTE_VALUE", param);
                String entityName = amid.getName();
                String idType = amid.getType().getName();

                // In the case of Agents, the attribute sun device key
                // values must be merged
                if (amid.getType().equals(IdType.AGENT) &&
                    values.containsKey(AGENT_ATTRIBUTE_LIST) &&
                    (amid.getAttribute(AGENT_ATTRIBUTE_LIST) != null)
                ) {
                    Set newDeviceKeyValue = (Set) values.get(
                        AGENT_ATTRIBUTE_LIST);
                    Set origDeviceKeyValue = amid.getAttribute(
                        AGENT_ATTRIBUTE_LIST);
                    for (Iterator items = origDeviceKeyValue.iterator();
                        items.hasNext();) {
                        String olValue = (String) items.next();
                        String[] olValues = olValue.split("=");
                        // Check if this attribute exists in new values
                        boolean found = false;
                        for (Iterator nt = newDeviceKeyValue.iterator();
                            nt.hasNext();) {
                            String ntValue = (String) nt.next();
                            String[] ntValues = ntValue.split("=");
                            if (ntValues[0].equalsIgnoreCase(olValues[0])) {
                                if ((ntValues.length > 1) &&
                                    (ntValues[1].trim().length() == 0)
                                ) {
                                    // Remove the entry
                                    nt.remove();
                                }
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            newDeviceKeyValue.add(olValue);
                        }
                    }
                }
                beforeModify(idType, entityName, values);
                amid.setAttributes(values);
                amid.store();

                logEvent("SUCCEED_MODIFY_IDENTITY_ATTRIBUTE_VALUE", param);
            } catch (IdRepoException e) {
                String[] paramsEx = {universalId, attrNames, getErrorString(e)};
                logEvent("IDM_EXCEPTION_MODIFY_IDENTITY_ATTRIBUTE_VALUE",
                    paramsEx);
                throw new AMConsoleException(getErrorString(e));
            } catch (SSOException e) {
                String[] paramsEx = {universalId, attrNames, getErrorString(e)};
                logEvent("SSO_EXCEPTION_MODIFY_IDENTITY_ATTRIBUTE_VALUE",
                    paramsEx);
                throw new AMConsoleException(getErrorString(e));
            }
        }
    }

    private void validateAttributes(AMIdentity amid, Map values)
        throws AMConsoleException {
        String agentType = null;
        boolean webJ2EEagent = false;
        try {
            Set agentTypes = amid.getAttribute("AgentType");
            if ((agentTypes != null) && !agentTypes.isEmpty()) {
                agentType = (String) agentTypes.iterator().next();
                webJ2EEagent =
                    agentType.equals(AgentConfiguration.AGENT_TYPE_J2EE) ||
                    agentType.equals(AgentConfiguration.AGENT_TYPE_WEB) ||
                    agentType.equals(
                    AgentConfiguration.AGENT_TYPE_AGENT_AUTHENTICATOR);
            }
            if (webJ2EEagent) {
                for (Iterator iter = values.keySet().iterator(); iter.hasNext();) {
                    String attrName = (String) iter.next();
                    if (attrName.equals(AGENT_ATTRIBUTE_LIST)) {
                        Set newValues = new HashSet();
                        Set temp = (Set) values.get(AGENT_ATTRIBUTE_LIST);
                        for (Iterator i = temp.iterator(); i.hasNext();) {
                            String val = AGENT_ROOT_URL + (String) i.next();
                            newValues.add(val);
                        }
                        values.put(AGENT_ATTRIBUTE_LIST, newValues);
                    }
                }
            }
        } catch (IdRepoException e) {
            throw new AMConsoleException(e);
        } catch (SSOException e) {
            throw new AMConsoleException(e);
        }
        validateAttributes(values);
    }
    
    private void validateAttributes(Map values)
        throws AMConsoleException {
        for (Iterator iter = values.keySet().iterator(); iter.hasNext(); ) {
            String attrName = (String)iter.next();
            if (requiredAttributeNames.keySet().contains(attrName)) {
                if (!reqValidator.validate((Set)values.get(attrName))) {
                    AttributeSchema as =
                        (AttributeSchema)requiredAttributeNames.get(attrName);
                    String serviceName = as.getServiceSchema().getServiceName();
                    String expMsg = getLocalizedString("entity-values-missing");
                    ResourceBundle rb = getServiceResourceBundle(serviceName);
                    String[] arg = {
                        Locale.getString(rb, as.getI18NKey(), debug)};
                    throw new AMConsoleException(MessageFormat.format(
                        expMsg, (Object[])arg));
                }
            }
        }
    }

    /**
     * Deletes entities.
     *
     * @param realmName Name of Realm.
     * @param names Name of Entities to be deleted.
     * @throws AMConsoleException if entity cannot be deleted.
     */
    public void deleteEntities(String realmName, Set names) 
        throws AMConsoleException {
        if ((names != null) && !names.isEmpty()) {
            String idNames = AMFormatUtils.toCommaSeparatedFormat(names);
            String[] params = {realmName, idNames};
            logEvent("ATTEMPT_DELETE_IDENTITY", params);

            try {
                AMIdentityRepository repo = new AMIdentityRepository(
                    getUserSSOToken(), realmName);
                repo.deleteIdentities(getAMIdentity(names));
                logEvent("SUCCEED_DELETE_IDENTITY", params);
            } catch (IdRepoException e) {
                String[] paramsEx = {realmName, idNames, getErrorString(e)};
                logEvent("IDM_EXCEPTION_DELETE_IDENTITY", paramsEx);
                throw new AMConsoleException(getErrorString(e));
            } catch (SSOException e) {
                String[] paramsEx = {realmName, idNames, getErrorString(e)};
                logEvent("SSO_EXCEPTION_DELETE_IDENTITY", paramsEx);
                throw new AMConsoleException(getErrorString(e));
            }
        }
    }

    /**
     * Returns true if services can be assigned to this entity type.
     *
     * @param realmName Name of Realm.
     * @param idType Type of Entity.
     * @return true if services can be assigned to this entity type.
     */
    public boolean canAssignService(String realmName, String idType) {
        boolean can = false;

        try {
            AMIdentityRepository repo = new AMIdentityRepository(
                adminSSOToken, realmName);
            Set allowedOperations = repo.getAllowedIdOperations(
                IdUtils.getType(idType));
            can = allowedOperations.contains(IdOperation.SERVICE);
        } catch (IdRepoException e) {
            debug.warning("EntitiesModelImpl.canAssignService", e);
        } catch (SSOException e) {
            debug.warning("EntitiesModelImpl.canAssignService", e);
        }

        return can;
    }

    /**
     * Returns a set of entity types of which a given type can have member of.
     *
     * @param realmName Name of Realm.
     * @param idType Type of Entity.
     * @return a set of entity types of which a given type can have member of.
     * @throws AMConsoleException if <code>idType</code> is not supported.
     */
    public Set getIdTypeMemberOf(String realmName, String idType)
        throws AMConsoleException {
        try {
            IdType ltype = IdUtils.getType(idType);
            Set memberOfs = new HashSet();
            memberOfs.addAll(ltype.canBeMemberOf());
            discardUnsupportedIdType(realmName, memberOfs);

            for (Iterator i = memberOfs.iterator(); i.hasNext(); ) {
                IdType t = (IdType)i.next();
                Set canAdd = t.canAddMembers();

                if (!canAdd.contains(ltype)) {
                    i.remove();
                }
            }
            return memberOfs;
        } catch (IdRepoException e) {
            debug.warning("EntitiesModelImpl.getIdTypeMemberOf", e);
            throw new AMConsoleException(getErrorString(e));
        }
    }

    /**
     * Returns a set of entity types that can be member of a given type.
     *
     * @param realmName Name of Realm.
     * @param idType Type of Entity.
     * @return a set of entity types that can be member of a given type.
     * @throws AMConsoleException if <code>idType</code> is not supported.
     */
    public Set getIdTypeBeMemberOf(String realmName, String idType)
        throws AMConsoleException {
        try {
            IdType type = IdUtils.getType(idType);
            Set beMemberOfs = new HashSet();
            beMemberOfs.addAll(type.canHaveMembers());
            discardUnsupportedIdType(realmName, beMemberOfs);

            return beMemberOfs;
        } catch (IdRepoException e) {
            debug.warning("EntitiesModelImpl.getIdTypeBeMemberOf", e);
            throw new AMConsoleException(getErrorString(e));
        }
    }

    /**
     * Returns true of members can be added to a type.
     *
     * @param realmName Name of Realm.
     * @param idType Type of Entity.
     * @param containerIDType Type of Entity of Container.
     * @return true of members can be added to a type.
     */
    public boolean canAddMember(
        String realmName,
        String idType,
        String containerIDType
    ) throws AMConsoleException {
        boolean can = false;
        try {
            IdType type = IdUtils.getType(idType);
            Set canAdd = type.canAddMembers();
            IdType ctype = IdUtils.getType(containerIDType);
            can = canAdd.contains(ctype);
        } catch (IdRepoException e) {
            debug.warning("EntitiesModelImpl.canAddMember", e);
            throw new AMConsoleException(getErrorString(e));
        }
        return can;
    }

    private void discardUnsupportedIdType(String realmName, Set set) {
        if ((set != null) && !set.isEmpty()) {
            Set supported = getSupportedEntityTypes(realmName).keySet();
            for (Iterator iter = set.iterator(); iter.hasNext(); ) {
                IdType type = (IdType)iter.next();
                if (!supported.contains(type.getName())) {
                    iter.remove();
                }
            }
        }
    }

    private Set getAMIdentity(Set names)
        throws IdRepoException
    {
        Set identities = new HashSet(names.size() *2);
        SSOToken ssoToken = getUserSSOToken();

        for (Iterator iter = names.iterator(); iter.hasNext(); ) {
            identities.add(IdUtils.getIdentity(ssoToken, (String)iter.next()));
        }

        return identities;
    }

    /**
     * Returns service name of a given ID type.
     *
     * @param idType ID Type.
     * @param agentType Agent Type.
     * @return service name of a given ID type.
     */
    public String getServiceNameForIdType(String idType, String agentType) {
        String serviceName = null;

        try {
            serviceName = getSvcNameForIdType(idType, agentType);
        } catch (IdRepoException e) {
            debug.warning("EntitiesModelImpl.getServiceNameForIdType", e);
        }
        return serviceName;
    }

    private String getSvcNameForIdType(String idType, String agentType)
        throws IdRepoException {
        String serviceName = ((agentType != null) && (agentType.length() > 0)) ?
            IdConstants.AGENT_SERVICE :
            IdUtils.getServiceName(IdUtils.getType(idType));

        if ((serviceName == null) || (serviceName.trim().length() == 0)) {
            if (ServiceManager.isCoexistenceMode()) {
                BackwardCompSupport support = BackwardCompSupport.getInstance();
                serviceName = support.getServiceName(idType);
            }
        }
        return serviceName;
    }

    private void beforeDisplay(String idType, Set attributeSchemas)
        throws IdRepoException {
        /*
        * This is required to hide the naming attribute in profile and 
        * creation view for users.
        */
        BackwardCompSupport support = BackwardCompSupport.getInstance();
        support.beforeDisplay(idType, attributeSchemas);
    }

    private void beforeModify(
        String idType,
        String entityName,
        Map values
    ) throws IdRepoException {
        // NO-OP 
    }

    private void beforeCreate(String idType, String entityName, Map values)
        throws IdRepoException 
    {
        /*
         * This is required to set entity name to naming attribute field 
         * in the creation view for user.
         */
        BackwardCompSupport support = BackwardCompSupport.getInstance();
        support.beforeCreate(idType, entityName, values);
    }

    /**
     * Returns membership of an entity.
     *
     * @param realmName Name of Realm.
     * @param universalId Universal ID of the entity.
     * @param type Type of membership.
     * @return membership of an entity.
     * @throws AMConsoleException if members cannot be returned.
     */
    public Set getMembership(String realmName, String universalId, String type) 
        throws AMConsoleException {
        String[] params = {universalId, type};
        logEvent("ATTEMPT_READ_IDENTITY_MEMBERSHIP", params);
        try {
            AMIdentity amid = IdUtils.getIdentity(
                getUserSSOToken(), universalId);
            Set results = amid.getMemberships(IdUtils.getType(type));
            logEvent("SUCCEED_READ_IDENTITY_MEMBERSHIP", params);
            return results;
        } catch (SSOException e) {
            String[] paramsEx = {universalId, type, getErrorString(e)};
            logEvent("SSO_EXCEPTION_READ_IDENTITY_MEMBERSHIP", paramsEx);
            debug.warning("EntitiesModelImpl.getMembership", e);
            throw new AMConsoleException(getErrorString(e));
        } catch (IdRepoException e) {
            String[] paramsEx = {universalId, type, getErrorString(e)};
            logEvent("IDM_EXCEPTION_READ_IDENTITY_MEMBERSHIP", paramsEx);
            debug.warning("EntitiesModelImpl.getMembership", e);
            throw new AMConsoleException(getErrorString(e));
        }
    }

    /**
     * Returns members of an entity.
     *
     * @param realmName Name of Realm.
     * @param universalId Universal ID of the entity.
     * @param type Type of membership.
     * @return members of an entity.
     * @throws AMConsoleException if members cannot be returned.
     */
    public Set getMembers(String realmName, String universalId, String type) 
        throws AMConsoleException {
        String[] params = {universalId, type};
        logEvent("ATTEMPT_READ_IDENTITY_MEMBER", params);

        try {
            AMIdentity amid = IdUtils.getIdentity(
                getUserSSOToken(), universalId);
            Set results = amid.getMembers(IdUtils.getType(type));
            logEvent("SUCCEED_READ_IDENTITY_MEMBER", params);
            return results;
        } catch (SSOException e) {
            String[] paramsEx = {universalId, type, getErrorString(e)};
            logEvent("SSO_EXCEPTION_READ_IDENTITY_MEMBER", paramsEx);
            debug.warning("EntitiesModelImpl.getMembers", e);
            throw new AMConsoleException(getErrorString(e));
        } catch (IdRepoException e) {
            String[] paramsEx = {universalId, type, getErrorString(e)};
            logEvent("IDM_EXCEPTION_READ_IDENTITY_MEMBER", paramsEx);
            debug.warning("EntitiesModelImpl.getMembers", e);
            throw new AMConsoleException(getErrorString(e));
        }
    }

    /**
     * Adds an entity to a set of membership.
     *
     * @param universalId Universal ID of the entity.
     * @param membership Set of Universal ID of membership.
     * @throws AMConsoleException if membership addition fails.
     */
    public void addMemberships(String universalId, Set membership)
        throws AMConsoleException {
        if ((membership == null) || membership.isEmpty()) {
            throw new AMConsoleException(
                "entities.membership.add.no.selection.message");
        }

        SSOToken ssoToken = getUserSSOToken();
        String currentId = "";

        try {
            AMIdentity amid = IdUtils.getIdentity(ssoToken, universalId);
            String[] params = new String[2];
            params[1] = universalId;

            for (Iterator iter = membership.iterator(); iter.hasNext(); ) {
                String id = (String)iter.next();
                AMIdentity amidentity = IdUtils.getIdentity(ssoToken, id);
                currentId = id;
                params[0] = id;

                logEvent("ATTEMPT_ADD_IDENTITY_MEMBER", params);
                amidentity.addMember(amid);
                logEvent("SUCCEED_ADD_IDENTITY_MEMBER", params);
            }
        } catch (SSOException e) {
            String[] paramsEx = {currentId, universalId, getErrorString(e)};
            logEvent("SSO_EXCEPTION_ADD_IDENTITY_MEMBER", paramsEx);
            debug.warning("EntitiesModelImpl.addMemberships", e);
            throw new AMConsoleException(getErrorString(e));
        } catch (IdRepoException e) {
            String[] paramsEx = {currentId, universalId, getErrorString(e)};
            logEvent("IDM_EXCEPTION_ADD_IDENTITY_MEMBER", paramsEx);
            debug.warning("EntitiesModelImpl.addMemberships", e);
            throw new AMConsoleException(getErrorString(e));
        }
    }

    /**
     * Adds an entities to a membership.
     *
     * @param universalId Universal ID of the membership.
     * @param names Set of Universal ID of entities.
     * @throws AMConsoleException if membership addition fails.
     */
    public void addMembers(String universalId, Set names)
        throws AMConsoleException {
        if ((names == null) || names.isEmpty()) {
            throw new AMConsoleException(
                "entities.members.add.no.selection.message");
        }

        SSOToken ssoToken = getUserSSOToken();
        String currentId = "";

        try {
            AMIdentity amid = IdUtils.getIdentity(ssoToken, universalId);
            String[] params = new String[2];
            params[0] = universalId;

            for (Iterator iter = names.iterator(); iter.hasNext(); ) {
                String id = (String)iter.next();
                AMIdentity amidentity = IdUtils.getIdentity(ssoToken, id);
                currentId = id;
                params[1] = id;

                logEvent("ATTEMPT_ADD_IDENTITY_MEMBER", params);
                amid.addMember(amidentity);
                logEvent("SUCCEED_ADD_IDENTITY_MEMBER", params);
            }
        } catch (SSOException e) {
            String[] paramsEx = {universalId, currentId, getErrorString(e)};
            logEvent("SSO_EXCEPTION_ADD_IDENTITY_MEMBER", paramsEx);
            debug.warning("EntitiesModelImpl.addMembers", e);
            throw new AMConsoleException(getErrorString(e));
        } catch (IdRepoException e) {
            String[] paramsEx = {universalId, currentId, getErrorString(e)};
            logEvent("IDM_EXCEPTION_ADD_IDENTITY_MEMBER", paramsEx);
            debug.warning("EntitiesModelImpl.addMembers", e);
            throw new AMConsoleException(getErrorString(e));
        }
    }

    /**
     * Removes an entity from a set of memberships.
     *
     * @param universalId Universal ID of the entity.
     * @param membership Set of Universal ID of membership.
     * @throws AMConsoleException if membership removal fails.
     */
    public void removeMemberships(String universalId, Set membership)
        throws AMConsoleException {
        if ((membership == null) || membership.isEmpty()) {
            throw new AMConsoleException(
                "entities.membership.remove.no.selection.message");
        }

        SSOToken ssoToken = getUserSSOToken();
        String currentId = "";

        try {
            AMIdentity amid = IdUtils.getIdentity(ssoToken, universalId);
            String[] params = new String[2];
            params[1] = universalId;

            for (Iterator iter = membership.iterator(); iter.hasNext(); ) {
                String id = (String)iter.next();
                AMIdentity amidentity = IdUtils.getIdentity(ssoToken, id);
                currentId = id;
                params[0] = id;

                logEvent("ATTEMPT_REMOVE_IDENTITY_MEMBER", params);
                amidentity.removeMember(amid);
                logEvent("SUCCEED_REMOVE_IDENTITY_MEMBER", params);
            }
        } catch (SSOException e) {
            String[] paramsEx = {currentId, universalId, getErrorString(e)};
            logEvent("SSO_EXCEPTION_REMOVE_IDENTITY_MEMBER", paramsEx);
            debug.warning("EntitiesModelImpl.removeMemberships", e);
            throw new AMConsoleException(getErrorString(e));
        } catch (IdRepoException e) {
            String[] paramsEx = {currentId, universalId, getErrorString(e)};
            logEvent("IDM_EXCEPTION_REMOVE_IDENTITY_MEMBER", paramsEx);
            debug.warning("EntitiesModelImpl.removeMemberships", e);
            throw new AMConsoleException(getErrorString(e));
        }
    }

    /**
     * Removes a set of entities from a membership.
     *
     * @param universalId Universal ID of the membership.
     * @param names Set of Universal ID of entities.
     * @throws AMConsoleException if membership removal fails.
     */
    public void removeMembers(String universalId, Set names)
        throws AMConsoleException {
        if ((names == null) || names.isEmpty()) {
            throw new AMConsoleException(
                "entities.members.remove.no.selection.message");
        }

        SSOToken ssoToken = getUserSSOToken();
        String currentId = "";

        try {
            AMIdentity amid = IdUtils.getIdentity(ssoToken, universalId);
            String[] params = new String[2];
            params[0] = universalId;

            for (Iterator iter = names.iterator(); iter.hasNext(); ) {
                String id = (String)iter.next();
                AMIdentity amidentity = IdUtils.getIdentity(ssoToken, id);
                currentId = id;
                params[1] = id;

                logEvent("ATTEMPT_REMOVE_IDENTITY_MEMBER", params);
                amid.removeMember(amidentity);
                logEvent("SUCCEED_REMOVE_IDENTITY_MEMBER", params);
            }
        } catch (SSOException e) {
            String[] paramsEx = {universalId, currentId, getErrorString(e)};
            logEvent("SSO_EXCEPTION_REMOVE_IDENTITY_MEMBER", paramsEx);
            debug.warning("EntitiesModelImpl.removeMembers", e);
            throw new AMConsoleException(getErrorString(e));
        } catch (IdRepoException e) {
            String[] paramsEx = {universalId, currentId, getErrorString(e)};
            logEvent("IDM_EXCEPTION_REMOVE_IDENTITY_MEMBER", paramsEx);
            debug.warning("EntitiesModelImpl.removeMembers", e);
            throw new AMConsoleException(getErrorString(e));
        }
    }

    /**
     * Returns assigned memberships.
     *
     * @param universalId Universal ID of the entity.
     * @param memberships Set of assignable memberships.
     * @throws AMConsoleException if memberships information cannot be
     * determined.
     */
    public Set getAssignedMemberships(String universalId, Set memberships)
        throws AMConsoleException {
        Set assigned = new HashSet(memberships.size() *2);
        try {
            AMIdentity amid = IdUtils.getIdentity(
                getUserSSOToken(), universalId);

            for (Iterator iter = memberships.iterator(); iter.hasNext(); ) {
                AMIdentity m = (AMIdentity)iter.next();
                if (amid.isMember(m)) {
                    assigned.add(m);
                }
            }
        } catch (SSOException e) {
            debug.warning("EntitiesModelImpl.getAssignedMemberships", e);
            throw new AMConsoleException(getErrorString(e));
        } catch (IdRepoException e) {
            debug.warning("EntitiesModelImpl.getAssignedMemberships", e);
            throw new AMConsoleException(getErrorString(e));
        }

        return assigned;
    }

    /**
     * Returns assigned members.
     *
     * @param universalId Universal ID of the entity.
     * @param members Set of assignable members.
     * @throws AMConsoleException if members information cannot be
     * determined.
     */
    public Set getAssignedMembers(String universalId, Set members)
        throws AMConsoleException {
        Set assigned = new HashSet(members.size() *2);
        try {
            AMIdentity amid = IdUtils.getIdentity(
                getUserSSOToken(), universalId);

            for (Iterator iter = members.iterator(); iter.hasNext(); ) {
                AMIdentity m = (AMIdentity)iter.next();
                if (m.isMember(amid)) {
                    assigned.add(m);
                }
            }
        } catch (SSOException e) {
            debug.warning("EntitiesModelImpl.getAssignedMembers", e);
            throw new AMConsoleException(getErrorString(e));
        } catch (IdRepoException e) {
            debug.warning("EntitiesModelImpl.getAssignedMembers", e);
            throw new AMConsoleException(getErrorString(e));
        }

        return assigned;
    }

    /**
     * Returns assigned services. Map of service name to its display name.
     *
     * @param universalId Universal ID of the entity.
     * @return assigned services.
     * @throws AMConsoleException if service information cannot be determined.
     */
    public Map getAssignedServiceNames(String universalId)
        throws AMConsoleException {
        Map assigned = null;
        String[] param = {universalId};
        logEvent("ATTEMPT_READ_IDENTITY_ASSIGNED_SERVICE", param);
        try {
            AMIdentity amid = IdUtils.getIdentity(
                getUserSSOToken(), universalId);
            Set serviceNames = amid.getAssignedServices();

            // don't show auth config or user services in the user profile.
            IdType type = amid.getType();
             if (type.equals(IdType.USER)) {
                serviceNames.remove(AMAdminConstants.USER_SERVICE);
                serviceNames.remove(AMAdminConstants.AUTH_CONFIG_SERVICE);
            }

            assigned = getLocalizedServiceNames(serviceNames);
            logEvent("SUCCEED_READ_IDENTITY_ASSIGNED_SERVICE", param);
        } catch (SSOException e) {
            String[] paramsEx = {universalId, getErrorString(e)};
            logEvent("SSO_EXCEPTION_READ_IDENTITY_ASSIGNED_SERVICE", paramsEx);
            debug.warning("EntitiesModelImpl.getAssignedServiceNames", e);
            throw new AMConsoleException(getErrorString(e));
        } catch (IdRepoFatalException e) {
            String[] paramsEx = {universalId, getErrorString(e)};
            logEvent("IDM_EXCEPTION_READ_IDENTITY_ASSIGNED_SERVICE", paramsEx);
            debug.warning("EntitiesModelImpl.getAssignedServiceNames", e);
            // special casing this, because exception message from this 
            // exception is too cryptic
            if (e.getErrorCode().equals("305")) {
                isServicesSupported = false;
                throw new AMConsoleException(
                    getLocalizedString("idrepo.sevices.not.supported"));
            } else {
                throw new AMConsoleException(getErrorString(e));
            }
        } catch (IdRepoException e) {
            String[] paramsEx = {universalId, getErrorString(e)};
            logEvent("IDM_EXCEPTION_READ_IDENTITY_ASSIGNED_SERVICE", paramsEx);
            debug.warning("EntitiesModelImpl.getAssignedServiceNames", e);
            throw new AMConsoleException(getErrorString(e));
        }

        return (assigned != null) ? assigned : Collections.EMPTY_MAP;
    }

    /**
     * Returns assignable services. Map of service name to its display name.
     *
     * @param universalId Universal ID of the entity.
     * @return assignable services.
     * @throws AMConsoleException if service information cannot be determined.
     */
    public Map getAssignableServiceNames(String universalId)
        throws AMConsoleException {
        Map assignable = null;
        String[] param = {universalId};
        logEvent("ATTEMPT_READ_IDENTITY_ASSIGNABLE_SERVICE", param);

        try {
            AMIdentity amid = IdUtils.getIdentity(
                getUserSSOToken(), universalId);
            Set serviceNames = amid.getAssignableServices();

            /*
             * don't show the auth config, user, or saml service.
             */
            IdType type = amid.getType();
             if (type.equals(IdType.USER)) {
                serviceNames.remove(AMAdminConstants.USER_SERVICE);
                serviceNames.remove(AMAdminConstants.AUTH_CONFIG_SERVICE);
                serviceNames.remove(AMAdminConstants.SAML_SERVICE);
            }
            discardServicesWithoutAttributeSchema(serviceNames, amid);
            assignable = getLocalizedServiceNames(serviceNames);
            logEvent("SUCCEED_READ_IDENTITY_ASSIGNABLE_SERVICE", param);
        } catch (SSOException e) {
            String[] paramsEx = {universalId, getErrorString(e)};
            logEvent("SSO_EXCEPTION_READ_IDENTITY_ASSIGNABLE_SERVICE",
                paramsEx);
            debug.warning("EntitiesModelImpl.getAssignableServiceNames", e);
            throw new AMConsoleException(getErrorString(e));
        } catch (IdRepoException e) {
            String[] paramsEx = {universalId, getErrorString(e)};
            logEvent("IDM_EXCEPTION_READ_IDENTITY_ASSIGNABLE_SERVICE",
                paramsEx);
            debug.warning("EntitiesModelImpl.getAssignableServiceNames", e);
            throw new AMConsoleException(getErrorString(e));
        }

        return (assignable != null) ? assignable : Collections.EMPTY_MAP;
    }

    private void discardServicesWithoutAttributeSchema(
        Set serviceNames,
        AMIdentity amid
    ) {
        for (Iterator iter = serviceNames.iterator(); iter.hasNext(); ) {
            String serviceName = (String)iter.next();
            String url = getServicePropertiesViewBeanURL(serviceName);
            if (url == null) {
                ServiceSchema serviceSchema = AMAdminUtils.getSchemaSchema(
                    serviceName, amid.getType());
                Set attributes = serviceSchema.getAttributeSchemas();

                if ((attributes == null) || attributes.isEmpty()) {
                    iter.remove();
                } else if (!hasI18nKeys(attributes)) {
                    iter.remove();
                }
            }
        }
    }

    private boolean hasI18nKeys(Set attributeSchemes) {
        boolean has = false;
        for (Iterator i = attributeSchemes.iterator(); (i.hasNext() && !has);) {
            AttributeSchema as = (AttributeSchema)i.next();
            String i18nKey = as.getI18NKey();
            has = (i18nKey != null) && (i18nKey.length() > 0);
        }
        return has;
    }

    private Map getLocalizedServiceNames(Set serviceNames) {
        Map localized = null;
        if ((serviceNames != null) && !serviceNames.isEmpty()) {
            localized = new HashMap(serviceNames.size() *2);

            for (Iterator i = serviceNames.iterator(); i.hasNext(); ) {
                String name = (String)i.next();
                String lname = getLocalizedServiceName(name);
                if (!lname.equals(name)) {
                    localized.put(name, lname);
                }
            }
        }
        return (localized == null) ? Collections.EMPTY_MAP : localized;
    }

    /**
     * Returns the XML for property sheet view component.
     *
     * @param realmName Name of Realm.
     * @param serviceName Name of service.
     * @param idType type of Identity.
     * @param bCreate true if the property sheet is for identity creation.
     * @param viewbeanClassName Class Name of View Bean.
     * @return the XML for property sheet view component.
     * @throws AMConsoleException if XML cannot be created.
     */
    public String getServicePropertySheetXML(
        String realmName,
        String serviceName,
        IdType idType,
        boolean bCreate,
        String viewbeanClassName
    ) throws AMConsoleException {
        setLocationDN(realmName);
        DelegationConfig dConfig = DelegationConfig.getInstance();

        try {
            ServiceSchema serviceSchema = AMAdminUtils.getSchemaSchema(
                serviceName, idType);
            Set set = new HashSet(2);
            set.add(serviceSchema.getServiceType());
            PropertyXMLBuilder xmlBuilder = new PropertyXMLBuilder(
                serviceName, set, this);
            if (!bCreate) {
                boolean canModify = dConfig.hasPermission(realmName,
                    serviceName, AMAdminConstants.PERMISSION_MODIFY, this,
                    viewbeanClassName);
                if (!canModify) {
                    xmlBuilder.setAllAttributeReadOnly(true);
                }
            }
            String xml = xmlBuilder.getXML();
            if (idType.equals(IdType.ROLE)) {
                String cosPriority = AMAdminUtils.getStringFromInputStream(
                    getClass().getClassLoader().getResourceAsStream(
                    "com/sun/identity/console/propertyEntitiesCOSPriority.xml")
                    );
                if (xml != null) {
                    xml = PropertyXMLBuilder.appendXMLProperty(xml, 
                        cosPriority);
                } else {
                    xml = PropertyXMLBuilder.formPropertySheetXML(
                        cosPriority, true);
                }
            }
            return xml;
        } catch (SMSException e) {
            throw new AMConsoleException(getErrorString(e));
        } catch (SSOException e) {
            throw new AMConsoleException(getErrorString(e));
        }
    }

    /**
     * Assigns service to an entity.
     *
     * @param universalId Universal ID of the entity.
     * @param serviceName Name of service names.
     * @param values Attribute Values of the service.
     * @throws AMConsoleException if service cannot be assigned.
     */
    public void assignService(
        String universalId,
        String serviceName,
        Map values
    ) throws AMConsoleException {
        try {
            String[] params = {universalId, serviceName};
            logEvent("ATTEMPT_IDENTITY_ASSIGN_SERVICE", params);
            AMIdentity amid = IdUtils.getIdentity(
                getUserSSOToken(), universalId);
            amid.assignService(serviceName, values);

            logEvent("SUCCEED_IDENTITY_ASSIGN_SERVICE", params);
        } catch (SSOException e) {
            String[] paramsEx = {universalId, serviceName, getErrorString(e)};
            logEvent("SSO_EXCEPTION_IDENTITY_ASSIGN_SERVICE", paramsEx);
            debug.warning("EntitiesModelImpl.assignService", e);
            throw new AMConsoleException(getErrorString(e));
        } catch (IdRepoException e) {
            debug.error("EntitiesModelImpl.assignService", e);
            String[] paramsEx = {universalId, serviceName, getErrorString(e)};
            logEvent("IDM_EXCEPTION_IDENTITY_ASSIGN_SERVICE", paramsEx);
            debug.warning("EntitiesModelImpl.assignService", e);
            throw new AMConsoleException(getErrorString(e));
        }
    }

    /**
     * Returns defaults values for an Entity Type.
     *
     * @param idType ID Type.
     * @param serviceName Name of service name.
     * @throws AMConsoleException if default values cannot be obtained.
     */
    public Map getDefaultValues(String idType, String serviceName)
        throws AMConsoleException {

        Map map = null;
        try {
            ServiceSchemaManager mgr = new ServiceSchemaManager(
                serviceName, getUserSSOToken());
            ServiceSchema svcSchema = mgr.getSchema(idType);
            ServiceSchema schema = mgr.getSchema(svcSchema.getServiceType());
            Set attributeSchemas = schema.getAttributeSchemas();
            map = new HashMap(attributeSchemas.size() *2);
            for (Iterator i = attributeSchemas.iterator(); i.hasNext(); ) {
                AttributeSchema as = (AttributeSchema)i.next();
                String i18nKey = as.getI18NKey();

                if ((i18nKey != null) && (i18nKey.trim().length() > 0)) {
                    Set values = as.getDefaultValues();
                    if ((values != null) && !values.isEmpty()) {
                        map.put(as.getName(), values);
                    } else {
                        map.put(as.getName(), Collections.EMPTY_SET);
                    }
                }
            }
        } catch (SMSException e) {
            throw new AMConsoleException(getErrorString(e));
        } catch (SSOException e) {
            throw new AMConsoleException(getErrorString(e));
        }

        return (map != null) ? map : Collections.EMPTY_MAP;
    }

    /**
     * Unassigns services from an entity.
     *
     * @param universalId Universal ID of the entity.
     * @param serviceNames Set of service names to be unassigned.
     * @throws AMConsoleException if services cannot be unassigned.
     */
    public void unassignServices(String universalId, Set serviceNames)
        throws AMConsoleException {
        if ((serviceNames != null) && !serviceNames.isEmpty()) {
            String[] params = new String[2];
            params[0] = universalId;
            String currentSvc = "";

            try {
                AMIdentity amid = IdUtils.getIdentity(
                    getUserSSOToken(), universalId);
                for (Iterator iter = serviceNames.iterator(); iter.hasNext();) {
                    currentSvc = (String)iter.next();
                    params[1] = currentSvc;

                    logEvent("ATTEMPT_IDENTITY_UNASSIGN_SERVICE", params);
                    amid.unassignService(currentSvc);
                    logEvent("SUCCEED_IDENTITY_UNASSIGN_SERVICE", params);
                }
            } catch (SSOException e) {
                String[] paramsEx = {universalId, currentSvc,
                    getErrorString(e)};
                logEvent("SSO_EXCEPTION_IDENTITY_UNASSIGN_SERVICE", paramsEx);
                debug.warning("EntitiesModelImpl.unassignServices", e);
                throw new AMConsoleException(getErrorString(e));
            } catch (IdRepoException e) {
                String[] paramsEx = {universalId, currentSvc,
                    getErrorString(e)};
                logEvent("IDM_EXCEPTION_IDENTITY_UNASSIGN_SERVICE", paramsEx);
                debug.warning("EntitiesModelImpl.unassignServices", e);
                throw new AMConsoleException(getErrorString(e));
            }
        }
    }

    /**
     * Returns properties view bean URL for an attribute schema.
     *
     * @param name Name of attribute schema.
     * @return properties view bean URL for an attribute schema.
     */
    public String getPropertiesViewBean(String name) {
        String url = null;
        try {
            ServiceSchemaManager mgr = new ServiceSchemaManager(
                name, adminSSOToken);
            ServiceSchema schema = mgr.getSchema(SchemaType.USER);
            Set attributeSchemas = schema.getAttributeSchemas();
            for (Iterator i= attributeSchemas.iterator();
                i.hasNext() && (url == null);
            ) {
                AttributeSchema as = (AttributeSchema)i.next();
                if (as.getName().equals(name)) {
                    url = as.getPropertiesViewBeanURL();
                }
            }
        } catch (SMSException e) {
            debug.warning("EntitiesModelImpl.getDefaultValues", e);
        } catch (SSOException e) {
            debug.warning("EntitiesModelImpl.getDefaultValues", e);
        }

        return url;
    }

    /**
     * Returns service attribute values of an entity.
     *
     * @param universalId Universal ID of the entity.
     * @param serviceName Name of service name.
     * @return service attribute values of entity.
     * @throws AMConsoleException if values cannot be returned.
     */
    public Map getServiceAttributeValues(String universalId, String serviceName)
        throws AMConsoleException {
        Map values = null;
        try {
            String[] params = {universalId, serviceName};
            logEvent("ATTEMPT_IDENTITY_READ_SERVICE_ATTRIBUTE_VALUES", params);
            AMIdentity amid = IdUtils.getIdentity(
                getUserSSOToken(), universalId);
            values = amid.getServiceAttributes(serviceName);
            values = correctAttributeNames(amid, serviceName, values);
            logEvent("SUCCEED_IDENTITY_READ_SERVICE_ATTRIBUTE_VALUES", params);
        } catch (SSOException e) {
            String[] paramsEx = {universalId, serviceName, getErrorString(e)};
            logEvent("SSO_EXCEPTION_IDENTITY_READ_SERVICE_ATTRIBUTE_VALUES",
                paramsEx);
            debug.warning("EntitiesModelImpl.getServiceAttributeValues", e);
            throw new AMConsoleException(getErrorString(e));
        } catch (IdRepoException e) {
            String[] paramsEx = {universalId, serviceName, getErrorString(e)};
            logEvent("IDM_EXCEPTION_IDENTITY_READ_SERVICE_ATTRIBUTE_VALUES",
                paramsEx);
            debug.warning("EntitiesModelImpl.getServiceAttributeValues", e);
            throw new AMConsoleException(getErrorString(e));
        }
        return (values != null) ? values : Collections.EMPTY_MAP;
    }

    /*
     * For whatever reason, AMIdentity.getServiceAttributes method
     * is returning attribute name is lowercase. Now, we have to
     * correct the case accordingly.
     */
    private Map correctAttributeNames(
        AMIdentity amid,
        String serviceName,
        Map values
    ) {
        Map correctedValues = new HashMap(values.size());
        ServiceSchema serviceSchema = AMAdminUtils.getSchemaSchema(
            serviceName, amid.getType());
        Set attributes = serviceSchema.getAttributeSchemas();
        Set emptySet = new HashSet();
        emptySet.add("");

        if ((attributes != null) && !attributes.isEmpty()) {
            for (Iterator iter = attributes.iterator(); iter.hasNext(); ) {
                AttributeSchema as = (AttributeSchema)iter.next();
                String attrName = as.getName();
                Object val = values.get(attrName.toLowerCase());
                if (val != null) {
                    correctedValues.put(attrName, val);
                } else {
                    correctedValues.put(attrName, emptySet);
                }
            }
        }

        if (amid.getType().equals(IdType.ROLE)) {
            Object val = values.get(AMIdentity.COS_PRIORITY);
            if (val != null) {
                correctedValues.put(AMIdentity.COS_PRIORITY, val);
            } else {
                correctedValues.put(AMIdentity.COS_PRIORITY, emptySet);
            }
        }

        return correctedValues;
    }

    /**
     * Set service attribute values to an entity.
     *
     * @param universalId Universal ID of the entity.
     * @param serviceName Name of service name.
     * @param values Attribute values.
     * @throws AMConsoleException if values cannot be set.
     */
    public void setServiceAttributeValues(
        String universalId,
        String serviceName,
        Map values
    ) throws AMConsoleException {
        if ((values != null) && !values.isEmpty()) {
            try {
                String[] params = {universalId, serviceName};
                logEvent("ATTEMPT_IDENTITY_WRITE_SERVICE_ATTRIBUTE_VALUES",
                    params);
                AMIdentity amid = IdUtils.getIdentity(
                    getUserSSOToken(), universalId);
                amid.modifyService(serviceName, values);
                logEvent("SUCCEED_IDENTITY_WRITE_SERVICE_ATTRIBUTE_VALUES",
                    params);
            } catch (SSOException e) {
                String[] paramsEx = {universalId, serviceName,
                    getErrorString(e)};
                logEvent(
                    "SSO_EXCEPTION_IDENTITY_WRITE_SERVICE_ATTRIBUTE_VALUES",
                    paramsEx);
                debug.warning("EntitiesModelImpl.setServiceAttributeValues", e);
                throw new AMConsoleException(getErrorString(e));
            } catch (IdRepoException e) {
                String[] paramsEx = {universalId, serviceName,
                    getErrorString(e)};
                logEvent(
                    "IDM_EXCEPTION_IDENTITY_WRITE_SERVICE_ATTRIBUTE_VALUES",
                    paramsEx);
                debug.warning("EntitiesModelImpl.setServiceAttributeValues", e);
                throw new AMConsoleException(getErrorString(e));
            }
        }
    }

    /**
     * Returns true if service has displayable user attributes.
     *
     * @param serviceName Name of service.
     * @return true if service has user attribute schema.
     */
    public boolean hasUserAttributeSchema(String serviceName) {
        return hasAttributeSchema(serviceName, SchemaType.USER);
    }

    /**
     * This is a convenience method to check if there is  displayable 
     * attributes for a given service.
     *
     * @param serviceName name of service being displayed.
     * @return true if the service schema has at least one attribute to display.
     */
    public boolean hasDisplayableAttributes(String serviceName) {
        SchemaType st = SchemaType.USER;
        if ((getEntityType() != null) && (!getEntityType().equals("user"))) {
            st = SchemaType.DYNAMIC;
        }
        return hasAttributeSchema(serviceName, st);
    }

    private boolean hasAttributeSchema(String serviceName, SchemaType type) {
        boolean hasAttributes = false;
        try {
            ServiceSchemaManager mgr = new ServiceSchemaManager(
                serviceName, getUserSSOToken());
            ServiceSchema schema = mgr.getSchema(type);
            if (schema != null) {
                Set attributeSchemas = schema.getAttributeSchemas();
                if ((attributeSchemas != null) && !attributeSchemas.isEmpty()) {
                    hasAttributes = hasI18nKeys(attributeSchemas);
                }
            }
        } catch (SMSException e) {
            debug.warning("EntitiesModelImpl.hasAttributeSchema", e);
        } catch (SSOException e) {
            debug.warning("EntitiesModelImpl.hasAttributeSchema", e);
        }
        return hasAttributes;
    }

    /**
     * Returns all the authentication chains in a realm.
     *
     * @param realm Name of realm.
     * @return all the authentication chains in a realm.
     * @throws AMConsoleException if authentication chains cannot be returned.+      */
    public Set getAuthenticationChains(String realm)
        throws AMConsoleException {
        if ((realm == null) || (realm.trim().length() == 0)) {
            realm = "/";
        }
        try {
            return AMAuthConfigUtils.getAllNamedConfig(realm,
                getUserSSOToken());
        } catch (SSOException e) {
            throw new AMConsoleException(getErrorString(e));
        } catch (SMSException e) {
            throw new AMConsoleException(getErrorString(e));
        }
    }
    
    /**
     * Returns <code>true</code> if services is supported for the identity.
     * 
     * @return <code>true</code> if services is supported for the identity.
     */
    public boolean isServicesSupported() {
        return isServicesSupported;
    }

    public boolean repoExists(String realmName) {
        try {
            ServiceConfigManager svcCfgMgr = new ServiceConfigManager(IdConstants.REPO_SERVICE, getUserSSOToken());
            ServiceConfig cfg = svcCfgMgr.getOrganizationConfig(realmName, null);
            if (cfg == null) {
                return false;
            }
            Set names = cfg.getSubConfigNames();
            if (names == null || names.isEmpty()) {
                return false;
            }
            return true;
        } catch (SMSException e) {
            String strError = getErrorString(e);
            String[] paramsEx = {realmName, strError};
            logEvent("SMS_EXCEPTION_GET_ID_REPO_NAMES", paramsEx);
            return false;
        } catch (SSOException e) {
            String strError = getErrorString(e);
            String[] paramsEx = {realmName, strError};
            logEvent("SSO_EXCEPTION_GET_ID_REPO_NAMES", paramsEx);
            return false;
        }
    }
}
