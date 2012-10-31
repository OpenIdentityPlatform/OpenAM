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
 * $Id: IDFFModelImpl.java,v 1.9 2009/11/10 01:19:49 exu Exp $
 *
 */
package com.sun.identity.console.federation.model;

import com.sun.identity.console.base.model.AMAdminUtils;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.federation.IDFFAuthContexts;
import com.sun.identity.federation.meta.IDFFMetaException;
import com.sun.identity.federation.meta.IDFFMetaManager;
import com.sun.identity.federation.meta.IDFFMetaSecurityUtils;
import com.sun.identity.federation.meta.IDFFMetaUtils;
import com.sun.identity.federation.key.EncInfo;
import com.sun.identity.federation.key.KeyUtil;
import com.sun.identity.liberty.ws.meta.jaxb.AffiliationDescriptorType;
import com.sun.identity.liberty.ws.meta.jaxb.ProviderDescriptorType;
import com.sun.identity.federation.jaxb.entityconfig.AttributeElement;
import com.sun.identity.federation.jaxb.entityconfig.AttributeType;
import com.sun.identity.federation.jaxb.entityconfig.BaseConfigType;
import com.sun.identity.federation.jaxb.entityconfig.EntityConfigElement;
import com.sun.identity.federation.jaxb.entityconfig.IDPDescriptorConfigElement;
import com.sun.identity.federation.jaxb.entityconfig.ObjectFactory;
import com.sun.identity.federation.jaxb.entityconfig.SPDescriptorConfigElement;
import com.sun.identity.liberty.ws.meta.jaxb.EntityDescriptorElement;
import com.sun.identity.liberty.ws.meta.jaxb.IDPDescriptorType;
import com.sun.identity.liberty.ws.meta.jaxb.SPDescriptorType;
import com.sun.identity.federation.common.IFSConstants;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.xml.bind.JAXBException;

public class IDFFModelImpl
        extends EntityModelImpl
        implements IDFFModel {

    private IDFFMetaManager metaManager;
    private static Map extendedMetaMap = new HashMap(24);
    private static Map extendedMetaIdpMap = new HashMap(9);
    private static Map extendedMetaSpMap = new HashMap(13);
    private static List federationTerminationProfileList = new ArrayList(2);

    static {
        federationTerminationProfileList.add("http://projectliberty.org/profiles/fedterm-sp-http");
        federationTerminationProfileList.add("http://projectliberty.org/profiles/fedterm-sp-soap");
    }
    private static List singleLogoutProfileList = new ArrayList(3);

    static {
        singleLogoutProfileList.add("http://projectliberty.org/profiles/slo-sp-http");
        singleLogoutProfileList.add("http://projectliberty.org/profiles/slo-idp-http-get");
        singleLogoutProfileList.add("http://projectliberty.org/profiles/slo-sp-soap");
    }
    private static List nameRegistrationProfileList = new ArrayList(2);

    static {
        nameRegistrationProfileList.add("http://projectliberty.org/profiles/rni-sp-http");
        nameRegistrationProfileList.add("http://projectliberty.org/profiles/rni-sp-soap");
    }
    private static List federationProfileList = new ArrayList(3);

    static {
        federationProfileList.add("http://projectliberty.org/profiles/brws-post");
        federationProfileList.add("http://projectliberty.org/profiles/brws-art");
        federationProfileList.add("http://projectliberty.org/profiles/lecp");
    }
    private static List supportedSSOProfileList = new ArrayList(4);

    static {
        supportedSSOProfileList.add("http://projectliberty.org/profiles/brws-post");
        supportedSSOProfileList.add("http://projectliberty.org/profiles/brws-art");
        supportedSSOProfileList.add("http://projectliberty.org/profiles/wml-post");
        supportedSSOProfileList.add("http://projectliberty.org/profiles/lecp");
    }

    // BOTH idp AND SP extended metadata
    static {
        extendedMetaMap.put(ATTR_DO_FEDERATION_PAGE_URL,
                Collections.EMPTY_SET);
        extendedMetaMap.put(ATTR_ATTRIBUTE_MAPPER_CLASS,
                Collections.EMPTY_SET);
        extendedMetaMap.put(ATTR_ENABLE_AUTO_FEDERATION,
                Collections.EMPTY_SET);
        extendedMetaMap.put(ATTR_REGISTERATION_DONE_URL,
                Collections.EMPTY_SET);
        extendedMetaMap.put(ATTR_COT_LIST,
                Collections.EMPTY_SET);
        extendedMetaMap.put(ATTR_RESPONSD_WITH,
                Collections.EMPTY_SET);
        extendedMetaMap.put(ATTR_ENABLE_NAME_ID_ENCRYPTION,
                Collections.EMPTY_SET);
        extendedMetaMap.put(ATTR_SSO_FAILURE_REDIRECT_URL,
                Collections.EMPTY_SET);
        extendedMetaMap.put(ATTR_LIST_OF_COTS_PAGE_URL,
                Collections.EMPTY_SET);
        extendedMetaMap.put(ATTR_DEFAULT_AUTHN_CONTEXT,
                Collections.EMPTY_SET);
        extendedMetaMap.put(ATTR_SIGNING_CERT_ALIAS,
                Collections.EMPTY_SET);
        extendedMetaMap.put(ATTR_REALM_NAME,
                Collections.EMPTY_SET);
        extendedMetaMap.put(ATTR_USER_PROVIDER_CLASS,
                Collections.EMPTY_SET);
        extendedMetaMap.put(ATTR_NAME_ID_IMPLEMENETATION_CLASS,
                Collections.EMPTY_SET);
        extendedMetaMap.put(ATTR_FEDERATION_DONE_URL,
                Collections.EMPTY_SET);
        extendedMetaMap.put(ATTR_AUTH_TYPE,
                Collections.EMPTY_SET);
        extendedMetaMap.put(ATTR_ENCRYPTION_CERT_ALIAS,
                Collections.EMPTY_SET);
        extendedMetaMap.put(ATTR_TERMINATION_DONE_URL,
                Collections.EMPTY_SET);
        extendedMetaMap.put(ATTR_AUTO_FEDERATION_ATTRIBUTE,
                Collections.EMPTY_SET);
        extendedMetaMap.put(ATTR_ERROR_PAGE_URL,
                Collections.EMPTY_SET);
        extendedMetaMap.put(ATTR_PROVIDER_STATUS,
                Collections.EMPTY_SET);
        extendedMetaMap.put(ATTR_PROVIDER_DESCRIPTION,
                Collections.EMPTY_SET);
        extendedMetaMap.put(ATTR_LOGOUT_DONE_URL,
                Collections.EMPTY_SET);
        extendedMetaMap.put(ATTR_PROVIDER_HOME_PAGE_URL,
                Collections.EMPTY_SET);
    }

    // IDP extend meta attribute ONLY IDP
    static {
        extendedMetaIdpMap.put(ATTR_ASSERTION_LIMIT,
                Collections.EMPTY_SET);
        extendedMetaIdpMap.put(ATTR_ATTRIBUTE_PLUG_IN,
                Collections.EMPTY_SET);
        extendedMetaIdpMap.put(ATTR_IDP_ATTRIBUTE_MAP,
                Collections.EMPTY_SET);
        extendedMetaIdpMap.put(ATTR_ASSERTION_ISSUER,
                Collections.EMPTY_SET);
        extendedMetaIdpMap.put(ATTR_CLEANUP_INTERVAL,
                Collections.EMPTY_SET);
        extendedMetaIdpMap.put(ATTR_IDP_AUTHN_CONTEXT_MAPPING,
                Collections.EMPTY_SET);
        extendedMetaIdpMap.put(ATTR_GERNERATE_BOOT_STRAPPING,
                Collections.EMPTY_SET);
        extendedMetaIdpMap.put(ATTR_ARTIFACT_TIMEOUT,
                Collections.EMPTY_SET);
        extendedMetaIdpMap.put(ATTR_ASSERTION_INTERVAL,
                Collections.EMPTY_SET);
    }

    // SP extend meta attribute.. ONLY SP
    static {
        extendedMetaSpMap.put(ATTR_IS_PASSIVE,
                Collections.EMPTY_SET);
        extendedMetaSpMap.put(ATTR_SP_ATTRIBUTE_MAP,
                Collections.EMPTY_SET);
        extendedMetaSpMap.put(ATTR_SP_AUTHN_CONTEXT_MAPPING,
                Collections.EMPTY_SET);
        extendedMetaSpMap.put(ATTR_IDP_PROXY_LIST,
                Collections.EMPTY_SET);
        extendedMetaSpMap.put(ATTR_ENABLE_IDP_PROXY,
                Collections.EMPTY_SET);
        extendedMetaSpMap.put(ATTR_NAME_ID_POLICY,
                Collections.EMPTY_SET);
        extendedMetaSpMap.put(ATTR_FEDERATION_SP_ADAPTER_ENV,
                Collections.EMPTY_SET);
        extendedMetaSpMap.put(ATTR_ENABLE_AFFILIATION,
                Collections.EMPTY_SET);
        extendedMetaSpMap.put(ATTR_FORCE_AUTHN,
                Collections.EMPTY_SET);
        extendedMetaSpMap.put(ATTR_IDP_PROXY_COUNT,
                Collections.EMPTY_SET);
        extendedMetaSpMap.put(ATTR_FEDERATION_SP_ADAPTER,
                Collections.EMPTY_SET);
        extendedMetaSpMap.put(ATTR_USE_INTRODUCTION_FOR_IDP_PROXY,
                Collections.EMPTY_SET);
        extendedMetaSpMap.put(ATTR_SUPPORTED_SSO_PROFILE,
                Collections.EMPTY_SET);
    }

    /**
     * Creates a simple model using default resource bundle.
     *
     * @param req HTTP Servlet Request
     * @param map of user information
     */
    public IDFFModelImpl(HttpServletRequest req, Map map) {
        super(req, map);
    }

    /**
     * Returns provider-affiliate common attribute values.
     * @param realm the realm in which the entity resides.
     * @param entityName Name of Entity Descriptor.
     * @return provider-affiliate common attribute values.
     * @throws IDFFMetaException if attribute values cannot be obtained.
     */
    public Map getCommonAttributeValues(String realm, String entityName)
            throws AMConsoleException {
        Map values = new HashMap(26);
        String[] param = {realm, entityName, "IDFF", "General"};
        logEvent("ATTEMPT_GET_ENTITY_DESCRIPTOR_ATTR_VALUES", param);

        try {
            IDFFMetaManager manager = getIDFFMetaManager();
            EntityDescriptorElement desc = manager.getEntityDescriptor(
                    realm, entityName);
            values.put(ATTR_VALID_UNTIL, returnEmptySetIfValueIsNull(
                    desc.getValidUntil()));
            values.put(ATTR_CACHE_DURATION, returnEmptySetIfValueIsNull(
                    desc.getCacheDuration()));
            logEvent("SUCCEED_GET_ENTITY_DESCRIPTOR_ATTR_VALUES", param);
        } catch (IDFFMetaException e) {
            String[] paramsEx = {realm, entityName, "IDFF", "General",
                getErrorString(e)
            };
            logEvent("FEDERATION_EXCEPTION_GET_ENTITY_DESCRIPTOR_ATTR_VALUES",
                    paramsEx);
            throw new AMConsoleException(getErrorString(e));
        }
        return values;
    }

    /**
     * Modifies entity descriptor profile.
     *
     * @param realm the realm in which the entity resides.
     * @param entityName Name of entity descriptor.
     * @param map Map of attribute type to a Map of attribute name to values.
     * @throws AMConsoleException if profile cannot be modified.
     */
    public void modifyEntityProfile(String realm, String entityName, Map map)
            throws AMConsoleException {
        String[] param = {entityName};
        logEvent("ATTEMPT_MODIFY_ENTITY_DESCRIPTOR", param);

        try {
            IDFFMetaManager manager = getIDFFMetaManager();
            EntityDescriptorElement desc = manager.getEntityDescriptor(
                    realm, entityName);

            desc.setValidUntil((String) AMAdminUtils.getValue(
                    (Set) map.get(ATTR_VALID_UNTIL)));
            desc.setCacheDuration((String) AMAdminUtils.getValue(
                    (Set) map.get(ATTR_CACHE_DURATION)));

            manager.setEntityDescriptor(realm, desc);
            logEvent("SUCCEED_MODIFY_ENTITY_DESCRIPTOR", param);
        } catch (IDFFMetaException e) {
            String[] paramsEx = {entityName, getErrorString(e)};
            logEvent("FEDERATION_EXCEPTION_MODIFY_ENTITY_DESCRIPTOR", paramsEx);
            throw new AMConsoleException(getErrorString(e));
        }
    }

    /**
     * Returns a map of IDP key/value pairs.
     *
     * @param realm where the entity exists.
     * @param entityName of entity descriptor.
     * @return map of IDP key/value pairs
     */
    public Map getEntityIDPDescriptor(String realm, String entityName)
            throws AMConsoleException {
        String[] params = {realm, entityName, "IDFF", "IDP-Standard Metadata"};
        logEvent("ATTEMPT_GET_ENTITY_DESCRIPTOR_ATTR_VALUES", params);
        Map map = new HashMap();
        try {
            IDFFMetaManager manager = getIDFFMetaManager();
            IDPDescriptorType pDesc = manager.getIDPDescriptor(realm, entityName);

            // common attributes
            map.put(ATTR_PROTOCOL_SUPPORT_ENUMERATION,
                    convertListToSet(pDesc.getProtocolSupportEnumeration()));

            //communication URLs
            map.put(ATTR_SOAP_END_POINT,
                    returnEmptySetIfValueIsNull(pDesc.getSoapEndpoint()));
            map.put(ATTR_SINGLE_SIGN_ON_SERVICE_URL,
                    returnEmptySetIfValueIsNull(pDesc.getSingleSignOnServiceURL()));
            map.put(ATTR_SINGLE_LOGOUT_SERVICE_URL,
                    returnEmptySetIfValueIsNull(pDesc.getSingleLogoutServiceURL()));
            map.put(ATTR_SINGLE_LOGOUT_SERVICE_RETURN_URL,
                    returnEmptySetIfValueIsNull(
                    pDesc.getSingleLogoutServiceReturnURL()));
            map.put(ATTR_FEDERATION_TERMINATION_SERVICES_URL,
                    returnEmptySetIfValueIsNull(
                    pDesc.getFederationTerminationServiceURL()));
            map.put(ATTR_FEDERATION_TERMINATION_SERVICE_RETURN_URL,
                    returnEmptySetIfValueIsNull(
                    pDesc.getFederationTerminationServiceReturnURL()));
            map.put(ATTR_REGISTRATION_NAME_IDENTIFIER_SERVICE_URL,
                    returnEmptySetIfValueIsNull(
                    pDesc.getRegisterNameIdentifierServiceURL()));
            map.put(ATTR_REGISTRATION_NAME_IDENTIFIER_SERVICE_RETURN_URL,
                    returnEmptySetIfValueIsNull(
                    pDesc.getRegisterNameIdentifierServiceReturnURL()));

            // communication profiles
            map.put(ATTR_FEDERATION_TERMINATION_NOTIFICATION_PROTOCOL_PROFILE,
                    returnEmptySetIfValueIsNull(
                    (String) pDesc.getFederationTerminationNotificationProtocolProfile().get(0)));
            map.put(ATTR_SINGLE_LOGOUT_PROTOCOL_PROFILE,
                    returnEmptySetIfValueIsNull((String) pDesc.getSingleLogoutProtocolProfile().get(0)));
            map.put(ATTR_REGISTRATION_NAME_IDENTIFIER_PROFILE_PROFILE,
                    returnEmptySetIfValueIsNull((String) pDesc.getRegisterNameIdentifierProtocolProfile().get(0)));
            map.put(ATTR_SINGLE_SIGN_ON_PROTOCOL_PROFILE,
                    returnEmptySetIfValueIsNull((String) pDesc.getSingleSignOnProtocolProfile().get(0)));

            // get signing key size and algorithm               
            EncInfo encinfo = KeyUtil.getEncInfo(
                    (ProviderDescriptorType) pDesc,
                    entityName,
                    true); //isIDP
            if (encinfo == null) {
                map.put(ATTR_ENCRYPTION_KEY_SIZE, Collections.EMPTY_SET);
                map.put(ATTR_ENCRYPTION_ALGORITHM, Collections.EMPTY_SET);
            } else {
                int size = encinfo.getDataEncStrength();
                String alg = encinfo.getDataEncAlgorithm();
                map.put(ATTR_ENCRYPTION_KEY_SIZE,
                        returnEmptySetIfValueIsNull(Integer.toString(size)));
                map.put(ATTR_ENCRYPTION_ALGORITHM,
                        returnEmptySetIfValueIsNull(alg));
            }
            logEvent("SUCCEED_GET_ENTITY_DESCRIPTOR_ATTR_VALUES", params);
        } catch (IDFFMetaException e) {
            String strError = getErrorString(e);
            String[] paramsEx =
                    {realm, entityName, "IDFF", "IDP-Standard Metadata", strError};
            logEvent("FEDERATION_EXCEPTION_GET_ENTITY_DESCRIPTOR_ATTR_VALUES",
                    paramsEx);
            throw new AMConsoleException(strError);
        }
        return map;
    }

    /**
     * Returns a map of an SP entity descriptors key/value pairs.
     *
     * @param realm where the entity exists.
     * @param entityName name of entity descriptor.
     * @return map of SP key/value pairs
     */
    public Map getEntitySPDescriptor(String realm, String entityName)
            throws AMConsoleException {
        String[] params = {realm, entityName, "IDFF", "SP-Standard Metadata"};
        logEvent("ATTEMPT_GET_ENTITY_DESCRIPTOR_ATTR_VALUES", params);

        Map map = new HashMap();
        SPDescriptorType pDesc = null;

        try {
            IDFFMetaManager manager = getIDFFMetaManager();
            pDesc = manager.getSPDescriptor(realm, entityName);

            // common attributes
            map.put(ATTR_PROTOCOL_SUPPORT_ENUMERATION,
                    convertListToSet(pDesc.getProtocolSupportEnumeration()));

            //communication URLs
            map.put(ATTR_SOAP_END_POINT,
                    returnEmptySetIfValueIsNull(pDesc.getSoapEndpoint()));
            map.put(ATTR_SINGLE_LOGOUT_SERVICE_URL,
                    returnEmptySetIfValueIsNull(pDesc.getSingleLogoutServiceURL()));
            map.put(ATTR_SINGLE_LOGOUT_SERVICE_RETURN_URL,
                    returnEmptySetIfValueIsNull(
                    pDesc.getSingleLogoutServiceReturnURL()));
            map.put(ATTR_FEDERATION_TERMINATION_SERVICES_URL,
                    returnEmptySetIfValueIsNull(
                    pDesc.getFederationTerminationServiceURL()));
            map.put(ATTR_FEDERATION_TERMINATION_SERVICE_RETURN_URL,
                    returnEmptySetIfValueIsNull(
                    pDesc.getFederationTerminationServiceReturnURL()));
            map.put(ATTR_REGISTRATION_NAME_IDENTIFIER_SERVICE_URL,
                    returnEmptySetIfValueIsNull(
                    pDesc.getRegisterNameIdentifierServiceURL()));
            map.put(ATTR_REGISTRATION_NAME_IDENTIFIER_SERVICE_RETURN_URL,
                    returnEmptySetIfValueIsNull(
                    pDesc.getRegisterNameIdentifierServiceReturnURL()));

            // communication profiles
            map.put(ATTR_FEDERATION_TERMINATION_NOTIFICATION_PROTOCOL_PROFILE,
                    returnEmptySetIfValueIsNull(
                    (String) pDesc.getFederationTerminationNotificationProtocolProfile().get(0)));
            map.put(ATTR_SINGLE_LOGOUT_PROTOCOL_PROFILE,
                    returnEmptySetIfValueIsNull((String) pDesc.getSingleLogoutProtocolProfile().get(0)));
            map.put(ATTR_REGISTRATION_NAME_IDENTIFIER_PROFILE_PROFILE,
                    returnEmptySetIfValueIsNull((String) pDesc.getRegisterNameIdentifierProtocolProfile().get(0)));

            // only for Service Provider
            com.sun.identity.liberty.ws.meta.jaxb.SPDescriptorType.AssertionConsumerServiceURLType assertionType =
                    (com.sun.identity.liberty.ws.meta.jaxb.SPDescriptorType.AssertionConsumerServiceURLType) ((List) pDesc.getAssertionConsumerServiceURL()).get(0);
            if (assertionType != null) {
                map.put(ATTR_ASSERTION_CUSTOMER_SERVICE_URIID,
                        returnEmptySetIfValueIsNull(assertionType.getId()));
                map.put(ATTR_ASSERTION_CUSTOMER_SERVICE_URL,
                        returnEmptySetIfValueIsNull(assertionType.getValue()));
                map.put(ATTR_ASSERTION_CUSTOMER_SERVICE_URL_AS_DEFAULT,
                        returnEmptySetIfValueIsNull(assertionType.isIsDefault()));
            } else {
                map.put(ATTR_ASSERTION_CUSTOMER_SERVICE_URIID,
                        Collections.EMPTY_SET);
                map.put(ATTR_ASSERTION_CUSTOMER_SERVICE_URL,
                        Collections.EMPTY_SET);
                map.put(ATTR_ASSERTION_CUSTOMER_SERVICE_URL_AS_DEFAULT,
                        Collections.EMPTY_SET);
            }

            map.put(ATTR_AUTHN_REQUESTS_SIGNED,
                    returnEmptySetIfValueIsNull(pDesc.isAuthnRequestsSigned()));

            // get signing key size and algorithm                           
            EncInfo encinfo = KeyUtil.getEncInfo(
                    (ProviderDescriptorType) pDesc,
                    entityName,
                    false); //isIDP
            if (encinfo == null) {
                map.put(ATTR_ENCRYPTION_KEY_SIZE, Collections.EMPTY_SET);
                map.put(ATTR_ENCRYPTION_ALGORITHM, Collections.EMPTY_SET);
            } else {
                int size = encinfo.getDataEncStrength();
                String alg = encinfo.getDataEncAlgorithm();
                map.put(ATTR_ENCRYPTION_KEY_SIZE,
                        returnEmptySetIfValueIsNull(Integer.toString(size)));
                map.put(ATTR_ENCRYPTION_ALGORITHM,
                        returnEmptySetIfValueIsNull(alg));
            }
            logEvent("SUCCEED_GET_ENTITY_DESCRIPTOR_ATTR_VALUES", params);
        } catch (IDFFMetaException e) {
            String strError = getErrorString(e);
            String[] paramsEx =
                    {realm, entityName, "IDFF", "SP-Standard Metadata", strError};
            logEvent("FEDERATION_EXCEPTION_GET_ENTITY_DESCRIPTOR_ATTR_VALUES",
                    paramsEx);
            throw new AMConsoleException(strError);
        }

        return map;
    }

    /**
     * Returns attributes values in extended metadata.
     *
     * @param realm where the entity exists.
     * @param entityName Name of Entity Descriptor.
     * @param location Location of provider such as Hosted or Remote.
     * @return attributes values of provider.
     */
    public Map getIDPEntityConfig(
            String realm,
            String entityName,
            String location) throws AMConsoleException {
        String[] params = {realm, entityName, "IDFF", "IDP-Extended Metadata"};
        logEvent("ATTEMPT_GET_ENTITY_DESCRIPTOR_ATTR_VALUES", params);

        IDFFMetaManager manager;
        Map map = new HashMap();
        Map tmpMap = new HashMap();
        try {
            manager = getIDFFMetaManager();
            String metaAlias = null;

            BaseConfigType idpConfig =
                    manager.getIDPDescriptorConfig(realm, entityName);
            if (idpConfig != null) {
                map = IDFFMetaUtils.getAttributes(idpConfig);
                metaAlias = idpConfig.getMetaAlias();
            } else {
                createEntityConfig(realm, entityName, IFSConstants.IDP, location);
            }

            Set entries = map.entrySet();
            Iterator iterator = entries.iterator();
            while (iterator.hasNext()) {
                Map.Entry entry = (Map.Entry) iterator.next();
                tmpMap.put((String) entry.getKey(),
                        returnEmptySetIfValueIsNull(
                        convertListToSet((List) entry.getValue())));
            }
            tmpMap.put(ATTR_PROVIDER_ALIAS,
                    returnEmptySetIfValueIsNull(metaAlias));
            if (!tmpMap.containsKey(ATTR_SIGNING_CERT_ALIAS)) {
                tmpMap.put(ATTR_SIGNING_CERT_ALIAS,
                        Collections.EMPTY_SET);
            }
            if (!tmpMap.containsKey(ATTR_ENCRYPTION_CERT_ALIAS)) {
                tmpMap.put(ATTR_ENCRYPTION_CERT_ALIAS,
                        Collections.EMPTY_SET);
            }

            logEvent("SUCCEED_GET_ENTITY_DESCRIPTOR_ATTR_VALUES", params);
        } catch (IDFFMetaException e) {
            String strError = getErrorString(e);
            String[] paramsEx =
                    {realm, entityName, "IDFF", "IDP-Extended Metadata", strError};
            logEvent("FEDERATION_EXCEPTION_GET_ENTITY_DESCRIPTOR_ATTR_VALUES",
                    paramsEx);
            throw new AMConsoleException(getErrorString(e));
        } catch (AMConsoleException e) {
            String strError = getErrorString(e);
            String[] paramsEx =
                    {realm, entityName, "IDFF", "IDP-Extended Metadata", strError};
            logEvent("FEDERATION_EXCEPTION_GET_ENTITY_DESCRIPTOR_ATTR_VALUES",
                    paramsEx);
            throw new AMConsoleException(getErrorString(e));
        }
        return tmpMap;
    }

    /**
     * Returns attributes values in extended metadata.
     *
     * @param realm where the entity exists.
     * @param entityName Name of Entity Descriptor.
     * @param location Location of provider such as Hosted or Remote.
     * @return attributes values of provider.
     */
    public Map getSPEntityConfig(
            String realm,
            String entityName,
            String location) throws AMConsoleException {
        String[] params = {realm, entityName, "IDFF", "SP-Extended Metadata"};
        logEvent("ATTEMPT_GET_ENTITY_DESCRIPTOR_ATTR_VALUES", params);
        IDFFMetaManager manager;
        Map map = new HashMap();
        Map tmpMap = new HashMap();
        try {
            manager = getIDFFMetaManager();
            String metaAlias = null;

            BaseConfigType spConfig =
                    manager.getSPDescriptorConfig(realm, entityName);
            if (spConfig != null) {
                map = IDFFMetaUtils.getAttributes(spConfig);
                metaAlias = spConfig.getMetaAlias();
            } else {
                createEntityConfig(realm, entityName, IFSConstants.SP, location);
            }

            Set entries = map.entrySet();
            Iterator iterator = entries.iterator();
            while (iterator.hasNext()) {
                Map.Entry entry = (Map.Entry) iterator.next();
                if (((String) entry.getKey()).equals(ATTR_SUPPORTED_SSO_PROFILE)) {
                    List supportedSSOProfileList = (List) entry.getValue();
                    if (!supportedSSOProfileList.isEmpty()) {
                        tmpMap.put((String) entry.getKey(),
                                returnEmptySetIfValueIsNull(
                                (String) supportedSSOProfileList.get(0)));
                    }
                } else {
                    tmpMap.put((String) entry.getKey(),
                            returnEmptySetIfValueIsNull(
                            convertListToSet((List) entry.getValue())));
                }
            }
            tmpMap.put(ATTR_PROVIDER_ALIAS,
                    returnEmptySetIfValueIsNull(metaAlias));
            if (!tmpMap.containsKey(ATTR_SIGNING_CERT_ALIAS)) {
                tmpMap.put(ATTR_SIGNING_CERT_ALIAS,
                        Collections.EMPTY_SET);
            }
            if (!tmpMap.containsKey(ATTR_ENCRYPTION_CERT_ALIAS)) {
                tmpMap.put(ATTR_ENCRYPTION_CERT_ALIAS,
                        Collections.EMPTY_SET);
            }
            logEvent("SUCCEED_GET_ENTITY_DESCRIPTOR_ATTR_VALUES", params);
        } catch (IDFFMetaException e) {
            String strError = getErrorString(e);
            String[] paramsEx =
                    {realm, entityName, "IDFF", "SP-Extended Metadata", strError};
            logEvent("FEDERATION_EXCEPTION_GET_ENTITY_DESCRIPTOR_ATTR_VALUES",
                    paramsEx);
            throw new AMConsoleException(getErrorString(e));
        } catch (AMConsoleException e) {
            String strError = getErrorString(e);
            String[] paramsEx =
                    {realm, entityName, "IDFF", "SP-Extended Metadata", strError};
            logEvent("FEDERATION_EXCEPTION_GET_ENTITY_DESCRIPTOR_ATTR_VALUES",
                    paramsEx);
            throw new AMConsoleException(getErrorString(e));
        }
        return tmpMap;
    }

    public void updateEntitySPDescriptor(
            String realm,
            String entityName,
            Map attrValues,
            Map extendedValues,
            boolean ishosted) throws AMConsoleException {
        String[] params = {realm, entityName, "IDFF", "SP-Standard Metadata"};
        logEvent("ATTEMPT_MODIFY_ENTITY_DESCRIPTOR", params);
        try {

            //save key and encryption details if present for hosted
            if (ishosted == true) {
                String keysize = getValueByKey(attrValues,
                        ATTR_ENCRYPTION_KEY_SIZE);
                String algorithm = getValueByKey(attrValues,
                        ATTR_ENCRYPTION_ALGORITHM);
                String e_certAlias = getValueByKey(extendedValues,
                        ATTR_ENCRYPTION_CERT_ALIAS);
                String s_certAlias = getValueByKey(extendedValues,
                        ATTR_SIGNING_CERT_ALIAS);
                int keysi = (keysize != null && keysize.length() > 0) ? Integer.parseInt(keysize) : 128;
                String alg = (algorithm == null || algorithm.length() == 0) ? "http://www.w3.org/2001/04/xmlenc#aes128-cbc" : algorithm;
                IDFFMetaSecurityUtils.updateProviderKeyInfo(realm,
                        entityName, e_certAlias, false, false, alg, keysi);
                IDFFMetaSecurityUtils.updateProviderKeyInfo(realm,
                        entityName, s_certAlias, true, false, alg, keysi);
            }

            IDFFMetaManager idffManager = getIDFFMetaManager();
            EntityDescriptorElement entityDescriptor =
                    idffManager.getEntityDescriptor(realm, entityName);
            SPDescriptorType pDesc = idffManager.getSPDescriptor(
                    realm, entityName);

            //Protocol Support Enumeration
            pDesc.getProtocolSupportEnumeration().clear();
            pDesc.getProtocolSupportEnumeration().add(
                    (String) AMAdminUtils.getValue(
                    (Set) attrValues.get(ATTR_PROTOCOL_SUPPORT_ENUMERATION)));

            //communication URLs
            pDesc.setSoapEndpoint(
                    (String) AMAdminUtils.getValue((Set) attrValues.get(
                    ATTR_SOAP_END_POINT)));
            pDesc.setSingleLogoutServiceURL(
                    (String) AMAdminUtils.getValue((Set) attrValues.get(
                    ATTR_SINGLE_LOGOUT_SERVICE_URL)));
            pDesc.setSingleLogoutServiceReturnURL(
                    (String) AMAdminUtils.getValue((Set) attrValues.get(
                    ATTR_SINGLE_LOGOUT_SERVICE_RETURN_URL)));
            pDesc.setFederationTerminationServiceURL(
                    (String) AMAdminUtils.getValue((Set) attrValues.get(
                    ATTR_FEDERATION_TERMINATION_SERVICES_URL)));
            pDesc.setFederationTerminationServiceReturnURL(
                    (String) AMAdminUtils.getValue((Set) attrValues.get(
                    ATTR_FEDERATION_TERMINATION_SERVICE_RETURN_URL)));
            pDesc.setRegisterNameIdentifierServiceURL(
                    (String) AMAdminUtils.getValue((Set) attrValues.get(
                    ATTR_REGISTRATION_NAME_IDENTIFIER_SERVICE_URL)));
            pDesc.setRegisterNameIdentifierServiceReturnURL(
                    (String) AMAdminUtils.getValue((Set) attrValues.get(
                    ATTR_REGISTRATION_NAME_IDENTIFIER_SERVICE_RETURN_URL)));

            // communication profiles
            pDesc.getFederationTerminationNotificationProtocolProfile().clear();
            pDesc.getFederationTerminationNotificationProtocolProfile().add(
                    (String) AMAdminUtils.getValue((Set) attrValues.get(
                    ATTR_FEDERATION_TERMINATION_NOTIFICATION_PROTOCOL_PROFILE)));
            int size = federationTerminationProfileList.size();
            for (int i = 0; i < size; i++) {
                if (!federationTerminationProfileList.get(i).equals(
                        (String) AMAdminUtils.getValue((Set) attrValues.get(
                        ATTR_FEDERATION_TERMINATION_NOTIFICATION_PROTOCOL_PROFILE)))) {
                    pDesc.getFederationTerminationNotificationProtocolProfile().add(
                            federationTerminationProfileList.get(i));
                }
            }

            pDesc.getSingleLogoutProtocolProfile().clear();
            pDesc.getSingleLogoutProtocolProfile().add(
                    (String) AMAdminUtils.getValue((Set) attrValues.get(
                    ATTR_SINGLE_LOGOUT_PROTOCOL_PROFILE)));
            size = singleLogoutProfileList.size();
            for (int i = 0; i < size; i++) {
                if (!singleLogoutProfileList.get(i).equals(
                        (String) AMAdminUtils.getValue((Set) attrValues.get(
                        ATTR_SINGLE_LOGOUT_PROTOCOL_PROFILE)))) {
                    pDesc.getSingleLogoutProtocolProfile().add(
                            singleLogoutProfileList.get(i));
                }
            }

            pDesc.getRegisterNameIdentifierProtocolProfile().clear();
            pDesc.getRegisterNameIdentifierProtocolProfile().add(
                    (String) AMAdminUtils.getValue((Set) attrValues.get(
                    ATTR_REGISTRATION_NAME_IDENTIFIER_PROFILE_PROFILE)));
            size = nameRegistrationProfileList.size();
            for (int i = 0; i < size; i++) {
                if (!nameRegistrationProfileList.get(i).equals(
                        (String) AMAdminUtils.getValue((Set) attrValues.get(
                        ATTR_REGISTRATION_NAME_IDENTIFIER_PROFILE_PROFILE)))) {
                    pDesc.getRegisterNameIdentifierProtocolProfile().add(
                            nameRegistrationProfileList.get(i));
                }
            }

            // only for sp
            String id = (String) AMAdminUtils.getValue(
                    (Set) attrValues.get(ATTR_ASSERTION_CUSTOMER_SERVICE_URIID));
            String value = (String) AMAdminUtils.getValue(
                    (Set) attrValues.get(ATTR_ASSERTION_CUSTOMER_SERVICE_URL));
            String isDefault = (String) AMAdminUtils.getValue(
                    (Set) attrValues.get(
                    ATTR_ASSERTION_CUSTOMER_SERVICE_URL_AS_DEFAULT));
            String authnRequestsSigned = (String) AMAdminUtils.getValue(
                    (Set) attrValues.get(ATTR_AUTHN_REQUESTS_SIGNED));
            com.sun.identity.liberty.ws.meta.jaxb.ObjectFactory objFactory =
                    new com.sun.identity.liberty.ws.meta.jaxb.ObjectFactory();
            com.sun.identity.liberty.ws.meta.jaxb.SPDescriptorType.AssertionConsumerServiceURLType assertionType =
                    objFactory.createSPDescriptorTypeAssertionConsumerServiceURLType();
            assertionType.setId(id);
            assertionType.setValue(value);
            if (isDefault.equals("true")) {
                assertionType.setIsDefault(true);
            } else {
                assertionType.setIsDefault(false);
            }
            pDesc.getAssertionConsumerServiceURL().clear();
            pDesc.getAssertionConsumerServiceURL().add(assertionType);
            if (authnRequestsSigned.equals("true")) {
                pDesc.setAuthnRequestsSigned(true);
            } else {
                pDesc.setAuthnRequestsSigned(false);
            }

            entityDescriptor.getSPDescriptor().clear();
            entityDescriptor.getSPDescriptor().add(pDesc);
            idffManager.setEntityDescriptor(realm, entityDescriptor);
            logEvent("SUCCEED_MODIFY_ENTITY_DESCRIPTOR", params);
        } catch (IDFFMetaException e) {
            debug.error("IDFFMetaException, updateEntitySPDescriptor");
            String strError = getErrorString(e);
            String[] paramsEx =
                    {realm, entityName, "IDFF", "SP-Standard Metadata", strError};
            logEvent("FEDERATION_EXCEPTION_MODIFY_ENTITY_DESCRIPTOR", paramsEx);
            throw new AMConsoleException(strError);
        } catch (JAXBException e) {
            debug.error("JAXBException, updateEntitySPDescriptor");
            String strError = getErrorString(e);
            String[] paramsEx =
                    {realm, entityName, "IDFF", "SP-Standard Metadata", strError};
            logEvent("FEDERATION_EXCEPTION_MODIFY_ENTITY_DESCRIPTOR", paramsEx);
            throw new AMConsoleException(strError);

        }
    }

    public void updateEntityIDPDescriptor(
            String realm,
            String entityName,
            Map attrValues,
            Map extendedValues,
            boolean ishosted) throws AMConsoleException {
        String[] params = {realm, entityName, "IDFF", "IDP-Standard Metadata"};
        logEvent("ATTEMPT_MODIFY_ENTITY_DESCRIPTOR", params);
        try {

            //save key and encryption details if present for hosted
            if (ishosted == true) {
                String keysize = getValueByKey(attrValues,
                        ATTR_ENCRYPTION_KEY_SIZE);
                String algorithm = getValueByKey(attrValues,
                        ATTR_ENCRYPTION_ALGORITHM);
                String e_certAlias = getValueByKey(extendedValues,
                        ATTR_ENCRYPTION_CERT_ALIAS);
                String s_certAlias = getValueByKey(extendedValues,
                        ATTR_SIGNING_CERT_ALIAS);
                int keysi = (keysize != null && keysize.length() > 0) ? Integer.parseInt(keysize) : 128;
                String alg = (algorithm == null || algorithm.length() == 0) ? "http://www.w3.org/2001/04/xmlenc#aes128-cbc" : algorithm;
                IDFFMetaSecurityUtils.updateProviderKeyInfo(realm,
                        entityName, e_certAlias, false, true, alg, keysi);
                IDFFMetaSecurityUtils.updateProviderKeyInfo(realm,
                        entityName, s_certAlias, true, true, alg, keysi);
            }

            IDFFMetaManager idffManager = getIDFFMetaManager();
            EntityDescriptorElement entityDescriptor =
                    idffManager.getEntityDescriptor(realm, entityName);
            IDPDescriptorType pDesc = idffManager.getIDPDescriptor(
                    realm, entityName);

            //Protocol Support Enumeration
            pDesc.getProtocolSupportEnumeration().clear();
            pDesc.getProtocolSupportEnumeration().addAll(
                    (Collection) attrValues.get(ATTR_PROTOCOL_SUPPORT_ENUMERATION));

            //communication URLs
            pDesc.setSoapEndpoint(
                    (String) AMAdminUtils.getValue((Set) attrValues.get(
                    ATTR_SOAP_END_POINT)));
            pDesc.setSingleSignOnServiceURL(
                    (String) AMAdminUtils.getValue((Set) attrValues.get(
                    ATTR_SINGLE_SIGN_ON_SERVICE_URL)));
            pDesc.setSingleLogoutServiceURL(
                    (String) AMAdminUtils.getValue((Set) attrValues.get(
                    ATTR_SINGLE_LOGOUT_SERVICE_URL)));
            pDesc.setSingleLogoutServiceReturnURL(
                    (String) AMAdminUtils.getValue((Set) attrValues.get(
                    ATTR_SINGLE_LOGOUT_SERVICE_RETURN_URL)));
            pDesc.setFederationTerminationServiceURL(
                    (String) AMAdminUtils.getValue((Set) attrValues.get(
                    ATTR_FEDERATION_TERMINATION_SERVICES_URL)));
            pDesc.setFederationTerminationServiceReturnURL(
                    (String) AMAdminUtils.getValue((Set) attrValues.get(
                    ATTR_FEDERATION_TERMINATION_SERVICE_RETURN_URL)));
            pDesc.setRegisterNameIdentifierServiceURL(
                    (String) AMAdminUtils.getValue((Set) attrValues.get(
                    ATTR_REGISTRATION_NAME_IDENTIFIER_SERVICE_URL)));
            pDesc.setRegisterNameIdentifierServiceReturnURL(
                    (String) AMAdminUtils.getValue((Set) attrValues.get(
                    ATTR_REGISTRATION_NAME_IDENTIFIER_SERVICE_RETURN_URL)));

            // communication profiles
            pDesc.getFederationTerminationNotificationProtocolProfile().clear();
            pDesc.getFederationTerminationNotificationProtocolProfile().add(
                    (String) AMAdminUtils.getValue((Set) attrValues.get(
                    ATTR_FEDERATION_TERMINATION_NOTIFICATION_PROTOCOL_PROFILE)));
            int size = federationTerminationProfileList.size();
            for (int i = 0; i < size; i++) {
                if (!federationTerminationProfileList.get(i).equals(
                        (String) AMAdminUtils.getValue((Set) attrValues.get(
                        ATTR_FEDERATION_TERMINATION_NOTIFICATION_PROTOCOL_PROFILE)))) {
                    pDesc.getFederationTerminationNotificationProtocolProfile().add(
                            federationTerminationProfileList.get(i));
                }
            }

            pDesc.getSingleLogoutProtocolProfile().clear();
            pDesc.getSingleLogoutProtocolProfile().add(
                    (String) AMAdminUtils.getValue((Set) attrValues.get(
                    ATTR_SINGLE_LOGOUT_PROTOCOL_PROFILE)));
            size = singleLogoutProfileList.size();
            for (int i = 0; i < size; i++) {
                if (!singleLogoutProfileList.get(i).equals(
                        (String) AMAdminUtils.getValue((Set) attrValues.get(
                        ATTR_SINGLE_LOGOUT_PROTOCOL_PROFILE)))) {
                    pDesc.getSingleLogoutProtocolProfile().add(
                            singleLogoutProfileList.get(i));
                }
            }

            pDesc.getRegisterNameIdentifierProtocolProfile().clear();
            pDesc.getRegisterNameIdentifierProtocolProfile().add(
                    (String) AMAdminUtils.getValue((Set) attrValues.get(
                    ATTR_REGISTRATION_NAME_IDENTIFIER_PROFILE_PROFILE)));
            size = nameRegistrationProfileList.size();
            for (int i = 0; i < size; i++) {
                if (!nameRegistrationProfileList.get(i).equals(
                        (String) AMAdminUtils.getValue((Set) attrValues.get(
                        ATTR_REGISTRATION_NAME_IDENTIFIER_PROFILE_PROFILE)))) {
                    pDesc.getRegisterNameIdentifierProtocolProfile().add(
                            nameRegistrationProfileList.get(i));
                }
            }

            pDesc.getSingleSignOnProtocolProfile().clear();
            pDesc.getSingleSignOnProtocolProfile().add(
                    (String) AMAdminUtils.getValue((Set) attrValues.get(
                    ATTR_SINGLE_SIGN_ON_PROTOCOL_PROFILE)));
            size = federationProfileList.size();
            for (int i = 0; i < size; i++) {
                if (!federationProfileList.get(i).equals(
                        (String) AMAdminUtils.getValue((Set) attrValues.get(
                        ATTR_SINGLE_SIGN_ON_PROTOCOL_PROFILE)))) {
                    pDesc.getSingleSignOnProtocolProfile().add(
                            federationProfileList.get(i));
                }

            }

            entityDescriptor.getIDPDescriptor().clear();
            entityDescriptor.getIDPDescriptor().add(pDesc);
            idffManager.setEntityDescriptor(realm, entityDescriptor);
            logEvent("SUCCEED_MODIFY_ENTITY_DESCRIPTOR", params);
        } catch (IDFFMetaException e) {
            debug.error("IDFFMetaException , updateEntityIDPDescriptor", e);
            String strError = getErrorString(e);
            String[] paramsEx =
                    {realm, entityName, "IDFF", "SP-Standard Metadata", strError};
            logEvent("FEDERATION_EXCEPTION_MODIFY_ENTITY_DESCRIPTOR", paramsEx);
            throw new AMConsoleException(strError);
        }
    }

    private void updateAttrInConfig(
            BaseConfigType baseConfig,
            Map values) throws AMConsoleException {
        List attrList = baseConfig.getAttribute();
        for (Iterator i = attrList.iterator(); i.hasNext();) {
            AttributeElement avpnew = (AttributeElement) i.next();
            String name = avpnew.getName();
            Set set = (Set) values.get(name);
            if (set != null) {
                avpnew.getValue().clear();
                avpnew.getValue().addAll(set);
            }
        }
    }

    /**
     * Modifies a identity provider's extended metadata.
     *
     * @param entityName name of Entity Descriptor.
     * @param realm where entity exists.
     * @param attrValues Map of attribute name to set of values.
     * @throws AMConsoleException if provider cannot be modified.
     * @throws JAXBException if provider cannot be retrieved.
     */
    public void updateIDPEntityConfig(
            String realm,
            String entityName,
            Map attrValues) throws AMConsoleException {
        String[] params = {realm, entityName, "IDFF", "IDP-Extended Metadata"};
        logEvent("ATTEMPT_MODIFY_ENTITY_DESCRIPTOR", params);

        try {
            IDFFMetaManager idffMetaMgr = getIDFFMetaManager();
            EntityConfigElement entityConfig =
                    idffMetaMgr.getEntityConfig(realm, entityName);
            if (entityConfig == null) {
                throw new AMConsoleException("invalid.entity.name");
            }

            IDPDescriptorConfigElement idpDecConfigElement =
                    idffMetaMgr.getIDPDescriptorConfig(realm, entityName);
            if (idpDecConfigElement == null) {
                throw new AMConsoleException("invalid.config.element");
            } else {
                updateAttrInConfig(
                        idpDecConfigElement,
                        attrValues,
                        EntityModel.IDENTITY_PROVIDER);
            }

            //saves the attributes by passing the new entityConfig object
            idffMetaMgr.setEntityConfig(realm, entityConfig);
            logEvent("SUCCEED_MODIFY_ENTITY_DESCRIPTOR", params);
        } catch (IDFFMetaException e) {
            String strError = getErrorString(e);
            String[] paramsEx =
                    {realm, entityName, "IDFF", "IDP-Extended Metadata", strError};
            logEvent("FEDERATION_EXCEPTION_MODIFY_ENTITY_DESCRIPTOR", paramsEx);
            throw new AMConsoleException(strError);
        } catch (JAXBException e) {
            String strError = getErrorString(e);
            String[] paramsEx =
                    {realm, entityName, "IDFF", "IDP-Extended Metadata", strError};
            logEvent("FEDERATION_EXCEPTION_MODIFY_ENTITY_DESCRIPTOR", paramsEx);
            throw new AMConsoleException(strError);
        }
    }

    /**
     * Modifies a service provider's extended metadata.
     *
     * @param realm where entity exists.
     * @param entityName name of Entity Descriptor.
     * @param attrValues Map of attribute name to set of values.
     * @throws AMConsoleException if provider cannot be modified.
     * @throws JAXBException if provider cannot be retrieved.
     */
    public void updateSPEntityConfig(
            String realm,
            String entityName,
            Map attrValues) throws AMConsoleException {
        String[] params = {realm, entityName, "IDFF", "SP-Extended Metadata"};
        logEvent("ATTEMPT_MODIFY_ENTITY_DESCRIPTOR", params);

        try {
            IDFFMetaManager idffMetaMgr = getIDFFMetaManager();
            EntityConfigElement entityConfig =
                    idffMetaMgr.getEntityConfig(realm, entityName);
            if (entityConfig == null) {
                throw new AMConsoleException("invalid.entity.name");
            }

            SPDescriptorConfigElement spDecConfigElement =
                    idffMetaMgr.getSPDescriptorConfig(realm, entityName);
            if (spDecConfigElement == null) {
                throw new AMConsoleException("invalid.config.element");
            } else {
                // update sp entity config
                updateAttrInConfig(
                        spDecConfigElement,
                        attrValues,
                        EntityModel.SERVICE_PROVIDER);
                //handle supported sso profile
                List supportedSSOProfileList = new ArrayList();
                supportedSSOProfileList.add((String) AMAdminUtils.getValue(
                        (Set) attrValues.get(ATTR_SUPPORTED_SSO_PROFILE)));
                int size = supportedSSOProfileList.size();
                for (int i = 0; i < size; i++) {
                    if (!supportedSSOProfileList.get(i).equals(
                            (String) AMAdminUtils.getValue((Set) attrValues.get(
                            ATTR_SUPPORTED_SSO_PROFILE)))) {
                        supportedSSOProfileList.add(
                                supportedSSOProfileList.get(i));
                    }
                }
                updateAttrInConfig(
                        spDecConfigElement,
                        ATTR_SUPPORTED_SSO_PROFILE,
                        supportedSSOProfileList);
            }

            //saves the attributes by passing the new entityConfig object
            idffMetaMgr.setEntityConfig(realm, entityConfig);
            logEvent("SUCCEED_MODIFY_ENTITY_DESCRIPTOR", params);
        } catch (IDFFMetaException e) {
            String strError = getErrorString(e);
            String[] paramsEx =
                    {realm, entityName, "IDFF", "SP-Extended Metadata", strError};
            logEvent("FEDERATION_EXCEPTION_MODIFY_ENTITY_DESCRIPTOR", paramsEx);
            throw new AMConsoleException(strError);
        } catch (JAXBException e) {
            String strError = getErrorString(e);
            String[] paramsEx =
                    {realm, entityName, "IDFF", "IDP-Extended Metadata", strError};
            logEvent("FEDERATION_EXCEPTION_MODIFY_ENTITY_DESCRIPTOR", paramsEx);
            throw new AMConsoleException(strError);
        }
    }

    /**
     * update IDP Authentication Contexts
     *
     * @param realm Realm of Entity
     * @param entityName Name of Entity Descriptor.     
     * @param cxt IDFFAuthContexts object contains IDP
     *        Authentication Contexts values
     */
    public void updateIDPAuthenticationContexts(
            String realm,
            String entityName,
            IDFFAuthContexts cxt) throws AMConsoleException {

        List list = cxt.toIDPAuthContextInfo();
        String[] params = {realm, entityName, "IDFF",
            "IDP-updateIDPAuthenticationContexts"
        };
        logEvent("ATTEMPT_MODIFY_ENTITY_DESCRIPTOR", params);

        try {
            IDFFMetaManager idffMetaMgr = getIDFFMetaManager();
            EntityConfigElement entityConfig =
                    idffMetaMgr.getEntityConfig(realm, entityName);
            if (entityConfig == null) {
                throw new AMConsoleException("invalid.entity.name");
            }

            IDPDescriptorConfigElement idpDecConfigElement =
                    idffMetaMgr.getIDPDescriptorConfig(realm, entityName);
            if (idpDecConfigElement == null) {
                throw new AMConsoleException("invalid.config.element");
            } else {
                updateAttrInConfig(
                        idpDecConfigElement,
                        ATTR_IDP_AUTHN_CONTEXT_MAPPING,
                        list);
            }

            //saves the attributes by passing the new entityConfig object
            idffMetaMgr.setEntityConfig(realm, entityConfig);
            logEvent("SUCCEED_MODIFY_ENTITY_DESCRIPTOR", params);
        } catch (IDFFMetaException e) {
            String strError = getErrorString(e);
            String[] paramsEx =
                    {realm,
                entityName,
                "IDFF",
                "IDP-updateIDPAuthenticationContexts",
                strError
            };
            logEvent("FEDERATION_EXCEPTION_MODIFY_ENTITY_DESCRIPTOR", paramsEx);
            throw new AMConsoleException(strError);
        }

        return;
    }

    /**
     * update SP Authentication Contexts
     *
     * @param realm Realm of Entity
     * @param entityName Name of Entity Descriptor.     
     * @param cxt IDFFAuthContexts object contains SP
     *        Authentication Contexts values
     */
    public void updateSPAuthenticationContexts(
            String realm,
            String entityName,
            IDFFAuthContexts cxt) throws AMConsoleException {
        List list = cxt.toSPAuthContextInfo();
        String[] params = {
            realm,
            entityName,
            "IDFF",
            "SP-updateSPAuthenticationContexts"
        };
        logEvent("ATTEMPT_MODIFY_ENTITY_DESCRIPTOR", params);

        try {
            IDFFMetaManager idffMetaMgr = getIDFFMetaManager();
            EntityConfigElement entityConfig =
                    idffMetaMgr.getEntityConfig(realm, entityName);
            if (entityConfig == null) {
                throw new AMConsoleException("invalid.entity.name");
            }

            SPDescriptorConfigElement spDecConfigElement =
                    idffMetaMgr.getSPDescriptorConfig(realm, entityName);
            if (spDecConfigElement == null) {
                throw new AMConsoleException("invalid.config.element");
            } else {
                // update sp entity config
                updateAttrInConfig(
                        spDecConfigElement,
                        ATTR_SP_AUTHN_CONTEXT_MAPPING,
                        list);
            }

            //saves the attributes by passing the new entityConfig object
            idffMetaMgr.setEntityConfig(realm, entityConfig);
            logEvent("SUCCEED_MODIFY_ENTITY_DESCRIPTOR", params);
        } catch (IDFFMetaException e) {
            String strError = getErrorString(e);
            String[] paramsEx =
                    {realm,
                entityName,
                "IDFF",
                "SP-updateSPAuthenticationContexts",
                strError
            };
            logEvent("FEDERATION_EXCEPTION_MODIFY_ENTITY_DESCRIPTOR", paramsEx);
            throw new AMConsoleException(strError);
        }
        return;
    }

    private void updateAttrInConfig(
            BaseConfigType baseConfig,
            String attributeName,
            List list) throws AMConsoleException {
        List attrList = baseConfig.getAttribute();
        for (Iterator i = attrList.iterator(); i.hasNext();) {
            AttributeElement avpnew = (AttributeElement) i.next();
            String name = avpnew.getName();
            if (name.equals(attributeName)) {
                avpnew.getValue().clear();
                avpnew.getValue().addAll(list);
            }
        }
    }

    private BaseConfigType addAttributeType(Map values, BaseConfigType bctype)
            throws JAXBException {
        ObjectFactory objFactory = new ObjectFactory();
        for (Iterator iter = values.keySet().iterator();
                iter.hasNext();) {
            AttributeType avp = objFactory.createAttributeElement();
            String key = (String) iter.next();
            avp.setName(key);
            avp.getValue().addAll(Collections.EMPTY_LIST);
            bctype.getAttribute().add(avp);
        }
        return bctype;
    }

    /**
     * Updates the BaseConfigElement.
     *
     * @param baseConfig is the BaseConfigType passed.
     * @param values the Map which contains the new attribute/value pairs.
     * @param role the role of entity.
     * @throws AMConsoleException if update of baseConfig object fails.
     */
    private void updateAttrInConfig(
            BaseConfigType baseConfig,
            Map values,
            String role) throws JAXBException, AMConsoleException {
        List attrList = baseConfig.getAttribute();
        if (role.equals(EntityModel.IDENTITY_PROVIDER)) {
            attrList.clear();
            baseConfig = addAttributeType(
                    getAllIDPExtendedMetaMap(),
                    baseConfig);
            attrList = baseConfig.getAttribute();
        } else if (role.equals(EntityModel.SERVICE_PROVIDER)) {
            attrList.clear();
            baseConfig = addAttributeType(
                    getAllSPExtendedMetaMap(),
                    baseConfig);
            attrList = baseConfig.getAttribute();
        }
        for (Iterator it = attrList.iterator(); it.hasNext();) {
            AttributeElement avpnew = (AttributeElement) it.next();
            String name = avpnew.getName();
            if (values.keySet().contains(name)) {
                Set set = (Set) values.get(name);
                if (set != null) {
                    avpnew.getValue().clear();
                    avpnew.getValue().addAll(set);
                }
            }
        }
    }

    /**
     * Returns the object of Auththentication Contexts in IDP.
     *
     * @param realm Realm of Entity
     * @param entityName Name of Entity Descriptor.       
     * @return attributes values of provider.
     */
    public IDFFAuthContexts getIDPAuthenticationContexts(
            String realm,
            String entityName) throws AMConsoleException {
        String str = null;
        IDFFAuthContexts cxt = new IDFFAuthContexts();

        try {
            List tmpList = new ArrayList();
            IDFFMetaManager manager = getIDFFMetaManager();
            Map map = new HashMap();

            BaseConfigType idpConfig =
                    manager.getIDPDescriptorConfig(realm, entityName);
            if (idpConfig != null) {
                map = IDFFMetaUtils.getAttributes(idpConfig);
            } else {
                throw new AMConsoleException("invalid.entity.name");
            }
            List list = (List) map.get(ATTR_IDP_AUTHN_CONTEXT_MAPPING);

            for (int i = 0; i < list.size(); i++) {
                String tmp = (String) list.get(i);
                int index = tmp.lastIndexOf("|");
                String level = removeKey(tmp.substring(index + 1));

                tmp = tmp.substring(0, index);
                index = tmp.lastIndexOf("|");

                String value = removeKey(tmp.substring(index + 1));
                tmp = tmp.substring(0, index);

                index = tmp.indexOf("|");
                String key = removeKey(tmp.substring(index + 1));
                String name = removeKey(tmp.substring(0, index));

                cxt.put(name, "true", key, value, level);
            }

        } catch (IDFFMetaException e) {
            throw new AMConsoleException(getErrorString(e));
        } catch (AMConsoleException e) {
            throw new AMConsoleException(getErrorString(e));
        }
        return (cxt != null) ? cxt : new IDFFAuthContexts();
    }

    private String removeKey(String input) {
        int idx = input.lastIndexOf("=");
        String str = input.substring(idx + 1);
        return str;
    }

    /**
     * Returns  the object of Auththentication Contexts in SP.
     *
     * @param realm Realm of Entity
     * @param entityName Name of Entity Descriptor.     
     * @return attributes values of provider.
     */
    public IDFFAuthContexts getSPAuthenticationContexts(
            String realm,
            String entityName) throws AMConsoleException {
        IDFFAuthContexts cxt = new IDFFAuthContexts();
        String str = null;

        try {
            List tmpList = new ArrayList();
            IDFFMetaManager manager = getIDFFMetaManager();
            Map map = new HashMap();

            BaseConfigType spConfig =
                    manager.getSPDescriptorConfig(realm, entityName);
            if (spConfig != null) {
                map = IDFFMetaUtils.getAttributes(spConfig);
            } else {
                throw new AMConsoleException("invalid.entity.name");
            }

            List list = (List) map.get(ATTR_SP_AUTHN_CONTEXT_MAPPING);
            for (int i = 0; i < list.size(); i++) {
                String tmp = (String) list.get(i);
                int index = tmp.lastIndexOf("|");
                String level = removeKey(tmp.substring(index + 1));
                String name = removeKey(tmp.substring(0, index));
                cxt.put(name, "true", level);
            }
        } catch (IDFFMetaException e) {
            throw new AMConsoleException(getErrorString(e));
        } catch (AMConsoleException e) {
            throw new AMConsoleException(getErrorString(e));
        }

        return (cxt != null) ? cxt : new IDFFAuthContexts();

    }

    public void createEntityConfig(
            String realm,
            String entityName,
            String role,
            String location) throws AMConsoleException {
        try {
            IDFFMetaManager idffMetaMgr = getIDFFMetaManager();
            ObjectFactory objFactory = new ObjectFactory();
            // Check whether the entity id existed in the DS
            EntityDescriptorElement entityDesc =
                    idffMetaMgr.getEntityDescriptor(realm, entityName);

            if (entityDesc == null) {
                throw new AMConsoleException("invalid.entity.name");
            }
            EntityConfigElement entityConfig =
                    idffMetaMgr.getEntityConfig(realm, entityName);
            if (entityConfig == null) {
                entityConfig =
                        objFactory.createEntityConfigElement();
                // add to entityConfig
                entityConfig.setEntityID(entityName);
                if (location.equals("remote")) {
                    entityConfig.setHosted(false);
                } else {
                    entityConfig.setHosted(true);
                }
            }

            // create entity config and add the attribute
            BaseConfigType baseCfgType = null;

            // Decide which role EntityDescriptorElement includes
            // It could have one sp and one idp.
            if ((role.equals(IFSConstants.SP)) &&
                    (IDFFMetaUtils.getSPDescriptor(entityDesc) != null)) {
                baseCfgType = objFactory.createSPDescriptorConfigElement();

                for (Iterator iter = extendedMetaMap.keySet().iterator();
                        iter.hasNext();) {
                    AttributeType atype = objFactory.createAttributeType();
                    String key = (String) iter.next();
                    atype.setName(key);
                    atype.getValue().addAll(Collections.EMPTY_LIST);
                    baseCfgType.getAttribute().add(atype);
                }

                for (Iterator iter = extendedMetaSpMap.keySet().iterator();
                        iter.hasNext();) {
                    AttributeType atype = objFactory.createAttributeType();
                    String key = (String) iter.next();
                    atype.setName(key);
                    atype.getValue().addAll(Collections.EMPTY_LIST);
                    baseCfgType.getAttribute().add(atype);
                }
                entityConfig.getSPDescriptorConfig().add(baseCfgType);
            } else if ((role.equals(IFSConstants.IDP)) &&
                    (IDFFMetaUtils.getIDPDescriptor(entityDesc) != null)) {
                baseCfgType = objFactory.createIDPDescriptorConfigElement();

                for (Iterator iter = extendedMetaMap.keySet().iterator();
                        iter.hasNext();) {
                    AttributeType atype = objFactory.createAttributeType();
                    String key = (String) iter.next();
                    atype.setName(key);
                    atype.getValue().addAll(Collections.EMPTY_LIST);
                    baseCfgType.getAttribute().add(atype);
                }

                for (Iterator iter = extendedMetaIdpMap.keySet().iterator();
                        iter.hasNext();) {
                    AttributeType atype = objFactory.createAttributeType();
                    String key = (String) iter.next();
                    atype.setName(key);
                    atype.getValue().addAll(Collections.EMPTY_LIST);
                    baseCfgType.getAttribute().add(atype);
                }
                entityConfig.getIDPDescriptorConfig().add(baseCfgType);
            }
            idffMetaMgr.setEntityConfig(realm, entityConfig);
        } catch (IDFFMetaException e) {
            throw new AMConsoleException(getErrorString(e));
        } catch (JAXBException e) {
            throw new AMConsoleException(getErrorString(e));
        }
    }

    public Map getAllSPExtendedMetaMap() {
        Map map = new HashMap();
        map.putAll(extendedMetaMap);
        map.putAll(extendedMetaSpMap);
        return map;
    }

    public Map getAllIDPExtendedMetaMap() {
        Map map = new HashMap();
        map.putAll(extendedMetaMap);
        map.putAll(extendedMetaIdpMap);
        return map;
    }

    protected IDFFMetaManager getIDFFMetaManager()
            throws IDFFMetaException {
        if (metaManager == null) {
            metaManager = new IDFFMetaManager(null);
        }
        return metaManager;
    }

    /**
     * Returns true if entity descriptor is an affiliate.
     *
     * @param entityName of entity descriptor.
     * @return true if entity descriptor is an affiliate.
     * @throws AMConsoleException if entity cannot be retrieved.
     */
    public boolean isAffiliate(String realm, String entityName)
            throws AMConsoleException {
        boolean isAffiliate = false;
        try {
            IDFFMetaManager idffManager = getIDFFMetaManager();
            AffiliationDescriptorType ad =
                    (AffiliationDescriptorType) idffManager.getAffiliationDescriptor(
                    realm,
                    entityName);
            if (ad != null) {
                isAffiliate = true;
            }
        } catch (IDFFMetaException e) {
            debug.warning("IDFFModel.isAffiliate", e);
            throw new AMConsoleException(getErrorString(e));
        }

        return isAffiliate;
    }

    /**
     * Returns affiliate profile attribute values.
     *
     * @param realm the realm in which the entity resides.
     * @param entityName name of Entity Descriptor.
     * @return affiliate profile attribute values.
     * @throws AMConsoleException if attribute values cannot be obtained.
     */
    public Map getAffiliateProfileAttributeValues(
            String realm,
            String entityName) throws AMConsoleException {
        String[] params = {realm, entityName, "IDFF", "IDP"};
        logEvent("ATTEMPT_GET_AFFILIATE_ENTITY_DESCRIPTOR_ATTR_VALUES", params);
        Map values = new HashMap();
        try {
            IDFFMetaManager idffManager = getIDFFMetaManager();
            AffiliationDescriptorType aDesc =
                    (AffiliationDescriptorType) idffManager.getAffiliationDescriptor(
                    realm,
                    entityName);

            if (aDesc != null) {
                values.put(ATTR_AFFILIATE_ID,
                        returnEmptySetIfValueIsNull(aDesc.getAffiliationID()));

                values.put(ATTR_AFFILIATE_OWNER_ID,
                        returnEmptySetIfValueIsNull(aDesc.getAffiliationOwnerID()));                               

                BaseConfigType affiliationConfig =
                        idffManager.getAffiliationDescriptorConfig(
                        realm, 
                        entityName);

                if (affiliationConfig != null) {
                    Map map = IDFFMetaUtils.getAttributes(affiliationConfig);                                       
                    if (map.containsKey(ATTR_AFFILIATE_SIGNING_CERT_ALIAS)) {                      
                        values.put(ATTR_AFFILIATE_SIGNING_CERT_ALIAS,                                              
                            returnEmptySetIfValueIsNull(
                            convertListToSet((List) map.get(
                            ATTR_AFFILIATE_SIGNING_CERT_ALIAS))));
                    } else {    
                        values.put(ATTR_AFFILIATE_SIGNING_CERT_ALIAS,
                                Collections.EMPTY_SET);
                    }
                    if (map.containsKey(ATTR_AFFILIATE_ENCRYPTION_CERT_ALIAS)) {                       
                        values.put(ATTR_AFFILIATE_ENCRYPTION_CERT_ALIAS, 
                            returnEmptySetIfValueIsNull(
                            convertListToSet((List) map.get(
                            ATTR_AFFILIATE_ENCRYPTION_CERT_ALIAS))));
                    } else {
                        values.put(ATTR_AFFILIATE_ENCRYPTION_CERT_ALIAS,
                                Collections.EMPTY_SET);
                    }
                }
            } else {
                values.put(ATTR_AFFILIATE_ID, Collections.EMPTY_SET);
                values.put(ATTR_AFFILIATE_OWNER_ID, Collections.EMPTY_SET);
                values.put(ATTR_AFFILIATE_VALID_UNTIL, Collections.EMPTY_SET);
                values.put(ATTR_AFFILIATE_CACHE_DURATION,
                        Collections.EMPTY_SET);
                values.put(ATTR_AFFILIATE_SIGNING_CERT_ALIAS,
                        Collections.EMPTY_SET);
                values.put(ATTR_AFFILIATE_ENCRYPTION_CERT_ALIAS,
                        Collections.EMPTY_SET);
                values.put(ATTR_AFFILIATE_ENCRYPTION_KEY_SIZE,
                        Collections.EMPTY_SET);
                values.put(ATTR_AFFILIATE_ENCRYPTION_KEY_ALGORITHM,
                        Collections.EMPTY_SET);
            }
            logEvent("SUCCEED_GET_AFFILIATE_ENTITY_DESCRIPTOR_ATTR_VALUES",
                    params);
        } catch (IDFFMetaException e) {
            String strError = getErrorString(e);
            String[] paramsEx =
                    {realm, entityName, "IDFF", "SP", strError};
            logEvent("FEDERATION_EXCEPTION_GET_AFFILIATE_ENTITY_DESCRIPTOR_ATTR_VALUES",
                    paramsEx);
            throw new AMConsoleException(strError);
        }
        return (values != null) ? values : Collections.EMPTY_MAP;
    }

    /**
     * Modifies affiliate profile.
     *
     * @param realm the realm in which the entity resides.
     * @param entityName Name of entity descriptor.
     * @param values Map of attribute name/value pairs.
     * @param members Set of affiliate members
     * @throws AMConsoleException if profile cannot be modified.
     */
    public void updateAffiliateProfile(
            String realm,
            String entityName,
            Map values,
            Set members) throws AMConsoleException {
        String[] params = {realm, entityName, "IDFF", "Affiliate"};
        logEvent("ATTEMPT_MODIFY_AFFILIATE_ENTITY_DESCRIPTOR", params);
        try {
            IDFFMetaManager idffManager = getIDFFMetaManager();
            EntityDescriptorElement entityDescriptor =
                    idffManager.getEntityDescriptor(realm, entityName);
            AffiliationDescriptorType aDesc =
                    entityDescriptor.getAffiliationDescriptor();
           
            aDesc.setAffiliationOwnerID(
                    (String) AMAdminUtils.getValue((Set) values.get(
                    ATTR_AFFILIATE_OWNER_ID)));

            //TBD : common attributes which may be added here later
            /* ATTR_AFFILIATE_VALID_UNTIL,
             * ATTR_AFFILIATE_CACHE_DURATION 
             * ATTR_ENCRYPTION_KEY_SIZE 
             * ATTR_AFFILIATE_ENCRYPTION_KEY_ALGORITHM
             * ATTR_AFFILIATE_ENCRYPTION_CERT_ALIAS
             * ATTR_AFFILIATE_SIGNING_CERT_ALIAS
             */
                
            // add affilliate members
            aDesc.getAffiliateMember().clear();
            Iterator it = members.iterator();
            while (it.hasNext()) {
                String newMember = (String) it.next();
                aDesc.getAffiliateMember().add(newMember);
            }

            entityDescriptor.setAffiliationDescriptor(aDesc);
            idffManager.setEntityDescriptor(realm, entityDescriptor);
            logEvent("SUCCEED_MODIFY_AFFILIATE_ENTITY_DESCRIPTOR", params);
        } catch (IDFFMetaException e) {
            String strError = getErrorString(e);
            String[] paramsEx =
                    {realm, entityName, "IDFF", "Affiliate", strError};
            logEvent("FEDERATION_EXCEPTION_MODIFY_AFFILIATE_ENTITY_DESCRIPTOR",
                    paramsEx);
            throw new AMConsoleException(strError);
        }
    }

    /**
     * Returns a <code>Set</code> of entity descriptor names.
     *
     * @param realm the realm in which the entity resides.
     * @return the IDFF entity descriptor
     * @throws AMConsoleException
     */
    public Set getAllEntityDescriptorNames(String realm)
            throws AMConsoleException {
        Set entitySet = null;
        try {
            IDFFMetaManager idffManager = getIDFFMetaManager();
            entitySet = idffManager.getAllEntities(realm);
        } catch (IDFFMetaException e) {
            throw new AMConsoleException(e.getMessage());
        }
        return (entitySet != null) ? entitySet : Collections.EMPTY_SET;
    }

    /**
     * @return a Set of all the idff Affiliate entities.
     */
    public Set getAllAffiliateEntityDescriptorNames(String realm)
            throws AMConsoleException {
        Set entitySet = new HashSet();
        try {
            IDFFMetaManager idffManager = getIDFFMetaManager();
            Set allEntities = idffManager.getAllEntities(realm);
            Iterator it = allEntities.iterator();
            while (it.hasNext()) {
                String name = (String) it.next();
                if (isAffiliate(realm, name)) {
                    entitySet.add(name);
                }
            }
        } catch (IDFFMetaException e) {
            throw new AMConsoleException(e.getMessage());
        }
        return (entitySet != null) ? entitySet : Collections.EMPTY_SET;
    }

    /**
     * Returns a Set of all the affiliate members
     *
     * @param realm the realm in which the entity resides.
     * @param entityName name of the Entity Descriptor.
     * @throws AMConsoleException if values cannot be obtained.
     */
    public Set getAllAffiliateMembers(String realm, String entityName)
            throws AMConsoleException {
        Set memberSet = null;
        try {
            IDFFMetaManager idffManager = getIDFFMetaManager();
            AffiliationDescriptorType aDesc =
                    (AffiliationDescriptorType) idffManager.getAffiliationDescriptor(
                    realm,
                    entityName);
            memberSet = convertListToSet(aDesc.getAffiliateMember());
        } catch (IDFFMetaException e) {
            throw new AMConsoleException(e.getMessage());
        }

        return (memberSet != null) ? memberSet : Collections.EMPTY_SET;
    }

    private String getValueByKey(Map map, String key) {
        Set set = (Set) map.get(key);
        String val = null;
        if (set != null && !set.isEmpty()) {
            Iterator i = set.iterator();
            while ((i != null) && (i.hasNext())) {
                val = (String) i.next();
            }
        }
        return val;
    }
}
