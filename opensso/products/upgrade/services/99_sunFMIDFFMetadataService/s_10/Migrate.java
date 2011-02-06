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
 * $Id: Migrate.java,v 1.5 2008/08/19 19:14:59 veiming Exp $
 *
 */

import com.sun.identity.upgrade.MigrateTasks;
import com.sun.identity.upgrade.UpgradeException;
import com.sun.identity.upgrade.UpgradeUtils;
import com.sun.identity.liberty.ws.meta.jaxb.AffiliationDescriptorType;
import com.sun.identity.federation.jaxb.entityconfig.AffiliationDescriptorConfigElement;
import com.sun.identity.liberty.ws.meta.jaxb.SPDescriptorType.AssertionConsumerServiceURLType;
import com.sun.identity.federation.jaxb.entityconfig.AttributeType;
import com.sun.identity.federation.jaxb.entityconfig.IDPDescriptorConfigElement;
import com.sun.identity.federation.jaxb.entityconfig.SPDescriptorConfigElement;
import com.sun.identity.federation.meta.IDFFMetaManager;
import com.sun.identity.federation.meta.IDFFMetaSecurityUtils;
import com.sun.identity.liberty.ws.meta.jaxb.ProviderDescriptorType;
import com.sun.identity.liberty.ws.meta.jaxb.EntityDescriptorElement;
import com.sun.identity.liberty.ws.meta.jaxb.ExtensionType;
import com.sun.identity.liberty.ws.meta.jaxb.IDPDescriptorType;
import com.sun.identity.liberty.ws.meta.jaxb.OrganizationDisplayNameType;
import com.sun.identity.liberty.ws.meta.jaxb.OrganizationNameType;
import com.sun.identity.liberty.ws.meta.jaxb.SPDescriptorType;
import com.sun.identity.liberty.ws.common.jaxb.xmlsig.X509DataType.X509Certificate;
import com.sun.identity.liberty.ws.common.jaxb.xmlsig.X509DataType;
import com.sun.identity.liberty.ws.common.jaxb.xmlsig.X509DataElement;
import com.sun.identity.liberty.ws.common.jaxb.xmlsig.KeyInfoType;
import com.sun.identity.shared.DateUtils;
import com.sun.identity.sm.ServiceConfig;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Creates new service schema for <code>sunFMIDFFMetadataService</code>.
 * This service replaces the <code>iPlanetAMProviderConfigService</code>. 
 * Migration of data from old service to this service is required.
 * This class is invoked during migration from older versions
 * of Access Manager to the latest version.
 */
public class Migrate implements MigrateTasks {

    final String SCHEMA_FILE = "fmIDFF.xml";
    final static String IDFF_SERVICE_NAME = "iPlanetAMProviderConfigService";
    final static String IDFF_SERVICE_VERSION = "1.1";
    // IDFF Attribute constants
    final static String IDFF_PROVIDER_TYPE = "iplanet-am-provider-type";
    final static String IDFF_SSO_URL = "iplanet-am-sso-service-url";
    final static String IDFF_ASSERTION_CONSUMER_SERVICE_URL =
            "iplanet-am-assertion-consumer-service-url";
    final static String IDFF_SSO_PROTOCOL_PROFILE =
            "sunIdentityServerSingleSignOnProtocolprofile";
    final static String IDFF_SOAP_END_POINT = "iplanet-am-soap-end-point";
    final static String IDFF_TERM_PROTOCOL_PROFILE =
            "iplanet-am-federation-termination-protocol-profile";
    final static String IDFF_NAME_REGIS_URL =
            "iplanet-am-name-registration-url";
    final static String IDFF_SLO_SERVICE_URL = "iplanet-am-slo-service-url";
    final static String IDFF_SLO_RETURN_URL =
            "iplanet-am-slo-service-return-url";
    final static String IDFF_PROTO_SUPPORT_ENUM =
            "sunIdentityServerProtocolSupportEnum";
    final static String IDFF_NAMEID_MAPPING_ENC_PROFILE =
            "sunIdentityServerNameIdMappingEncryptionProfile";
    final static String IDFF_ENC_METHOD = "sunIdentityServerEncryptionMethod";
    final static String IDFF_KEY_SIZE = "sunIdentityServerKeySize";
    final static String IDFF_KEY_USE = "sunIdentityServerKeyUse";
    final static String IDFF_CERT_ALIAS = "iplanet-am-certificate-alias";
    final static String IDFF_ENC_ALIAS = "sunIdentityServerEncryptionKeyalias";
    final static String IDFF_TERM_RETURN_URL =
            "iplanet-am-federation-termination-service-return-url";
    final static String IDFF_SLO_PROTO_PROFILE =
            "iplanet-am-slo-protocol-profile";
    final static String IDFF_NAMEID_REGIS_PROFILE =
            "iplanet-am-name-registration-profile";
    final static String IDFF_NAMEID_REGIS_RETURN_URL =
            "iplanet-am-name-registration-return-url";
    final static String IDFF_TERM_SERVICE_URL =
            "iplanet-am-federation-termination-service-url";
    final static String IDFF_META_ALIAS = "iplanet-am-provider-alias";
    final static String IDFF_PROVIDER_STATUS = "iplanet-am-provider-status";
    final static String IDFF_NAMEID_ENC =
            "sunIdentityServerNameIdentifierEncryption";
    final static String IDFF_BOOT_STRAP = "sunIdentityServerBootstrapping";
    final static String IDFF_AUTH_TYPE = "iplanet-am-provider-auth-type";
    final static String IDFF_ASSERTION_INTERVAL =
            "iplanet-am-assertion-interval";
    final static String IDFF_ARTIFACT_TIMEOUT = "iplanet-am-artifact-timeout";
    final static String IDFF_CLEANUP_INTERVAL = "iplanet-am-cleanup-interval";
    final static String IDFF_ASSERTION_LIMIT = "iplanet-am-assertion-limit";
    final static String IDFF_HOMEPAGE_URL = "iplanet-am-provider-homepage-url";
    final static String IDFF_PROVIDER_ROLE = "iplanet-am-provider-role";
    final static String IDFF_COMPANY = "sunIdentityServerProviderCompany";
    final static String IDFF_GIVEN_NAME = "sunIdentityServerProviderGivenName";
    final static String IDFF_SURNAME = "sunIdentityServerProviderSurName";
    final static String IDFF_EMAIL_ADDRESS =
            "sunIdentityServerProviderEmailAddress";
    final static String IDFF_TELEPHONE_NUM =
            "sunIdentityServerProviderTelephoneNum";
    final static String IDFF_EXTENSION =
            "sunIdentityServerProviderCPExtension";
    final static String IDFF_PRINCIPAL_ID =
            "sunIdentityServerProviderLibertyPrincipalId";
    final static String IDFF_CONTACT_TYPE =
            "sunIdentityServerProviderContactType";
    final static String IDFF_ORG_NAME = "sunIdentityServerProviderOrgName";
    final static String IDFF_ORG_DISPLAY_NAME =
            "sunIdentityServerProviderOrgDisplayName";
    final static String IDFF_ORG_URL =
            "sunIdentityServerProviderOrgURL";
    final static String IDFF_ORG_EXTENSION =
            "sunIdentityServerProviderOrgExtension";
    final static String IDFF_DEFAULT_AUTH_CTX =
            "iplanet-am-default-authncontext";
    final static String IDFF_AUTH_CTX =
            "iplanet-am-authentication-context";
    final static String IDFF_MODULE_KEY =
            "iplanet-am-authentication-context-moduleindicator-key";
    final static String IDFF_LEVEL = "iplanet-am-authentication-context-level";
    final static String IDFF_IDP_LEVEL =
            "iplanet-am-authentication-context-priority";
    final static String IDFF_MODULE_KEY_VALUE =
            "iplanet-am-authentication-context-moduleindicator-key";
    final static String IDFF_RESPONDS_WITH =
            "iplanet-am-respond-withs";
    final static String IDFF_NAMEID_IMPL =
            "iplanet-am-name-identifier-implementation";
    final static String IDFF_AUTHN_FED_PROFILE =
            "iplanet-am-authnfed-profile";
    final static String IDFF_COT_LIST = "iplanet-am-trusted-providers";
    final static String IDFF_NAMEID_REG_DONE_URL =
            "iplanet-am-name-registration-donepage-url";
    final static String IDFF_ASSERTION_ISSUER =
            "sunIdentityServerProviderAssertionIssuer";
    final static String IDFF_ATTR_PLUGIN =
            "sunIdentityServerProviderAttributePlugin";
    final static String IDFF_AUTO_FED_ENABLED =
            "sunIdentityServerProviderisAutoFedEnabled";
    final static String IDFF_AUTO_FED_ATTR =
            "sunIdentityServerProviderAutoFedAttr";
    final static String IDFF_ATTR_MAP =
            "sunIdentityServerProviderIDPAttributeMap";
    final static String IDFF_FORCE_AUTHN = "iplanet-am-force-authentication";
    final static String IDFF_IS_PASSIVE = "iplanetm-am-is-passive";
    final static String IDFF_SSO_FAIL_URL = "iplanet-am-sso-fail-redirect-url";
    final static String IDFF_ATTR_MAPPER_CLASS =
            "sunIdentityServerProviderAttributeMapperClass";
    final static String IDFF_SP_ATTR_MAP =
            "sunIdentityServerProviderSPAttributeMap";
    final static String IDFF_SP_ADAPTER = "sunIdentityServerProviderSPAdapter";
    final static String IDFF_AUTH_DOMAIN_LIST =
            "iplanet-am-list-of-authenticationdomains";
    final static String SP_ROLE = "SP";
    final static String IDP_ROLE = "IDP";
    final static String IDFF_VALID_UNTIL = "sunIdentityServerValidUntil";
    final static String IDFF_CACHE_DURATION = "sunIdentityServerCacheDuration";
    final static String IDFF_ADD_META_LOCATION =
            "sunIdentityServerAdditionalMetaLocation";
    final static String IDFF_ENABLE_PROXY =
            "sunIdentityServerProviderEnableProxy";
    final static String IDFF_ENABLE_AFFILIATION =
            "sunIdentityServerAffiliationFederation";
    final static String IDFF_PROXY_LIST =
            "sunIdentityServerProviderProxies";
    final static String IDFF_PROXY_COUNT =
            "sunIdentityServerProviderProxyCount";
    final static String IDFF_INTRODUCTION_FOR_IDPPROXY =
            "sunIdentityServerIntroductionForProxying";
    final static String IDFF_NAMEID_POLICY =
            "sunIdentityServerNameIDPolicy";
    final static String IDFF_AUTHN_SERVICE_URL =
            "sunIdentityServerAuthnServiceUrl";
    final static String IDFF_IS_AUTHNREQ_SIGNED =
            "iplanet-am-authnrequest-signed";
    final static String IDFF_IS_DEFAULT =
            "sunIdentityServerAssertionConsumerServiceUrlisDefault";
    final static String IDFF_ASSERTION_ID =
            "sunIdentityServerAssertionConsumerServiceUrlId";
    final static String IDFF_AFF_ID = "sunIdentityServerAffiliationID";
    final static String IDFF_AFF_MEMBER = "sunIdentityServerAffiliationMember";
    final static String IDFF_AFF_OWNER_ID =
            "sunIdentityServerAffiliationOwnerID";
    final static String IDFF_ENTITY_GIVEN_NAME = "sunIdentityServerGivenName";
    final static String IDFF_ENTITY_SURNAME = "sunIdentityServerSurName";
    final static String IDFF_ENTITY_EMAIL = "sunIdentityServerEmailAddress";
    final static String IDFF_ENTITY_TELE_NUM = "sunIdentityServerTelephoneNum";
    final static String IDFF_ENTITY_COMPANY = "sunIdentityServerCompany";
    final static String IDFF_ENTITY_EXTENSION = "sunIdentityServerCPExtension";
    final static String IDFF_ENTITY_LIBERTY_PRINCIPAL_ID =
            "sunIdentityServerLibertyPrincipalId";
    final static String IDFF_ENTITY_CONTACT_TYPE =
            "sunIdentityServerContactType";
    final static String IDFF_ENTITY_ORG_NAME = "sunIdentityServerOrgName";
    final static String IDFF_ENTITY_ORG_DISPLAY_NAME =
            "sunIdentityServerOrgDisplayName";
    final static String IDFF_ENTITY_ORG_URL = "sunIdentityServerOrgURL";
    final static String IDFF_ENTITY_ORG_EXTENSION =
            "sunIdentityServerOrgExtension";
    final static String IDFF_AFF_ENC_METHOD =
            "sunIdentityServerAffiliationEncryptionMethod";
    final static String IDFF_AFF_KEY_SIZE =
            "sunIdentityServerAffiliationKeySize";
    final static String IDFF_AFF_KEY_USE = "sunIdentityServerAffiliationKeyUse";
    final static String IDFF_AFF_ENC_ALIAS =
            "sunIdentityServerAffiliationEncryptionKeyalias";
    final static String IDFF_ENTITY_VALID_UNTIL =
            "sunIdentityServerEntityValidUntil";
    final static String IDFF_ENTITY_CACHE_DURATION =
            "sunIdentityServerEntityCacheDuration";
    final static String CONTACT_PERSON = "ContactPerson";
    final static String ORGANIZATION = "Organization";
    final static String ENTITY_ORGANIZATION = "EntityOrganization";
    final static String AFF_DESC = "AffiliationDescriptor";
    final static String PROVIDER_CONTACT_PERSON = "ProviderContactPerson";
    final static String PROVIDER_ORGANIZATION = "ProviderOrganization";
    final static String LOCAL_CONFIG = "LocalConfiguration";
    final static String IDP_AUTH_CONTEXT_INFO = "IDPAuthenticationContextInfo";
    final static String IDP_AUTH_CONTEXT_MAPPING =
            "AuthenticationContext-Priority-ModuleIndicator-Mapping";
    final static String SP_AUTH_CONTEXT_INFO = "SPAuthenticationContextInfo";
    final static String SP_AUTH_CONTEXT_MAPPING =
            "AuthenticationContext-Level-Mapping";
    final static String DEFAULT_ENC_METHOD =
            "http://www.w3.org/2001/04/xmlenc#aes128-cbc";
    final static String DEFAULT_KEY_SIZE = "128";
    final static String INVALID_KEY_SIZE = "-1";
    final static String META_ALIAS = "metaAlias";
    // OpenSSO IDFF META Attribute Constants.
    final static String ROOT_REALM = "/";
    final static String DEFAULT_PROVIDER_TYPE = "hosted";
    final static String DEFAULT_PROXY_COUNT = "-1";
    final static String DEFAULT_ASSERTION_INTERVAL = "60";
    final static String DEFAULT_ASSERTION_LIMIT = "0";
    final static String DEFAULT_ARTIFACT_TIMEOUT = "120";
    final static String DEFAULT_CLEANUP_INTERVAL = "180";
    final static String FALSE = "false";
    final static String TRUE = "true";
    final static String ACTIVE = "active";
    final static String FEDERATED = "federated";
    final static String DEFAULT_LEVEL = "0";
    final static String DEFAULT_KEY = "Module";
    final static String DEFAULT_KEY_VALUE = "DataStore";
    final static String DEFAULT_AUTH_CONTEXT =
            "http://www.projectliberty.org/schemas/authctx/classes/Password";
    final static String CONTEXT_PARAM = "context";
    final static String LEVEL_PARAM = "level";
    final static String KEY_PARAM = "key";
    final static String VALUE_PARAM = "value";
    final static String EQUAL = "=";
    final static String PIPE = "|";
    final static String EMPTY_VALUE = "";
    final static String LOCAL = "local";
    final static String KEY_ENC_USE = "encryption";
    final static String ARTIFACT =
            "http://projectliberty.org/profiles/brws-art";
    final static String POST = "http://projectliberty.org/profiles/brws-post";
    final static String WML_POST =
            "http://projectliberty.org/profiles/wml-post";
    final static String LECP = "http://projectliberty.org/profiles/lecp";
    final static String TERM_IDP_HTTP =
            "http://projectliberty.org/profiles/fedterm-idp-http";
    final static String TERM_IDP_SOAP =
            "http://projectliberty.org/profiles/fedterm-idp-soap";
    final static String SLO_IDP_HTTP =
            "http://projectliberty.org/profiles/slo-idp-http";
    final static String SLO_IDP_SOAP =
            "http://projectliberty.org/profiles/slo-idp-soap";
    final static String RNI_IDP_HTTP =
            "http://projectliberty.org/profiles/rni-idp-http";
    final static String RNI_IDP_SOAP =
            "http://projectliberty.org/profiles/rni-idp-soap";
    final static String TERM_SP_HTTP =
            "http://projectliberty.org/profiles/fedterm-sp-http";
    final static String TERM_SP_SOAP =
            "http://projectliberty.org/profiles/fedterm-sp-soap";
    final static String SLO_SP_HTTP =
            "http://projectliberty.org/profiles/slo-sp-http";
    final static String SLO_SP_SOAP =
            "http://projectliberty.org/profiles/slo-sp-soap";
    final static String RNI_SP_HTTP =
            "http://projectliberty.org/profiles/rni-sp-http";
    final static String RNI_SP_SOAP =
            "http://projectliberty.org/profiles/rni-sp-soap";
    final static String ENABLE_IDP_PROXY = "enableIDPProxy";
    final static String IDP_PROXY_LIST = "idpProxyList";
    final static String IDP_PROXY_COUNT = "idpProxyCount";
    final static String ENABLE_AFFILIATION = "enableAffiliation";
    final static String USE_INTRO_IDP_PROXY = "useIntroductionForIDPProxy";
    final static String NAMEID_POLICY = "nameIDPolicy";
    final static String SP_AUTHCONTEXT_MAPPING = "spAuthnContextMapping";
    final static String IDP_AUTHCONTEXT_MAPPING = "idpAuthnContextMapping";
    final static String SUPPORTED_SSO_PROFILE = "supportedSSOProfile";
    final static String FORCE_AUTHN = "forceAuthn";
    final static String IS_PASSIVE = "isPassive";
    final static String SSO_FAILURE_REDIRECT_URL = "ssoFailureRedirectURL";
    final static String ATTRIBUTE_MAPPER = "attributeMapperClass";
    final static String SP_ATTRIBUTE_MAP = "spAttributeMap";
    final static String DEFAULT_SP_ADAPTER =
            "com.sun.identity.federation.plugins.FSDefaultSPAdapter";
    final static String SP_ADAPTER = "federationSPAdapter";
    final static String IDP_BOOT_STRAP = "generateBootstrapping";
    final static String DEFAULT_NAMEID_IMPL_CLASS =
            "com.sun.identity.federation.services.util.FSNameIdentifierImpl";
    final static String PROVIDER_STATUS = "providerStatus";
    final static String SIGN_CERT_ALIAS = "signingCertAlias";
    final static String ENC_CERT_ALIAS = "encryptionCertAlias";
    final static String ENABLE_NAMEID_ENC = "enableNameIDEncryption";
    final static String NAMEID_IMPL_CLASS = "nameIDImplementationClass";
    final static String AUTH_TYPE = "authType";
    final static String RESPONDS_WITH = "respondsWith";
    final static String COT_LIST = "cotlist";
    final static String PROVIDER_HOMEPAGE_URL = "providerHomePageURL";
    final static String REGIS_DONE_URL = "registrationDoneURL";
    final static String TERM_DONE_URL = "terminationDoneURL";
    final static String LOGOUT_DONE_URL = "logoutDoneURL";
    final static String ENABLE_AUTO_FED = "enableAutoFederation";
    final static String AUTO_FED_ATTR = "autoFederationAttribute";
    final static String DEFAULT_AUTHN_CONTEXT = "defaultAuthnContext";
    final static String ASSERTION_INTERVAL = "assertionInterval";
    final static String ARTIFACT_TIMEOUT = "artifactTimeout";
    final static String CLEANUP_INTERVAL = "cleanupInterval";
    final static String ASSERTION_LIMIT = "assertionLimit";
    final static String ASSERTION_ISSUER = "assertionIssuer";
    final static String ATTRIBUTE_PLUGIN = "attributePlugin";
    final static String LOCAL_CONFIGURATION = "LocalConfiguration";
    final static String IDP_ATTRIBUTE_MAP = "idpAttributeMap";

    /**
     * Creates service schema for <code>sunFAMSTSService</code> service.
     *
     * @return true if service creation is successful otherwise false.
     */
    public boolean migrateService() {
        boolean isSuccess = true;
        try {
            String fileName = UpgradeUtils.getNewServiceNamePath(SCHEMA_FILE);
            UpgradeUtils.createService(fileName);
            isSuccess = true;
        } catch (UpgradeException e) {
            UpgradeUtils.debug.error("Error creating service schema", e);
        }
        return isSuccess;
    }

    /**
     * Post Migration operations.
     * TODO: load data from ProviderConfig to this service
     * @return true if successful else error.
     */
    public boolean postMigrateTask() {
        // migrate data from old idff schema to new one.
        migrateIDFFProviders();
        return true;
    }

    /**
     * Pre Migration operations.
     * TODO: read data from iPlanetAMProviderConfigService 
     *
     * @return true if successful else error.
     */
    public boolean preMigrateTask() {
        return true;
    }

    public static void migrateIDFFProviders() {
        String classMethod = "Migrate:migrateIDFFProviders";
        try {
            ServiceConfig orgConfig = UpgradeUtils.getOrganizationServiceConfig(
                    IDFF_SERVICE_NAME, IDFF_SERVICE_VERSION);
            if (orgConfig != null) {
                Set subConfigs = orgConfig.getSubConfigNames();
                if (UpgradeUtils.debug.messageEnabled()) {
                    UpgradeUtils.debug.message("SubConfigs to migrate : "
                            + subConfigs);
                }
                if (subConfigs != null && !subConfigs.isEmpty()) {
                    Iterator iter = subConfigs.iterator();
                    while (iter.hasNext()) {
                        String subConfigName = (String) iter.next();
                        System.out.println("*********************************");
                        System.out.println("Migrate subConfig:" 
                                + subConfigName);
                        ServiceConfig providerSC =
                                orgConfig.getSubConfig(subConfigName);
                        Set providerids = providerSC.getSubConfigNames();
                        System.out.println("providerids :" + providerids);
                        getAllProviderConfig(providerids,
                                providerSC, subConfigName);
                    }
                }
            }
        } catch (Exception e) {
            UpgradeUtils.debug.error(classMethod +
                    "error migrating IDFF service ", e);
        }
    }

    /**
     * Retrieves provider config to migrate to new service.
     */
    private static void getAllProviderConfig(
            Set providerIDs, ServiceConfig providerSC, String entityID) {
        String classMethod = "Migrate:getAllProviderConfig";
        try {
            boolean createEntityConfig = false;
            com.sun.identity.liberty.ws.meta.jaxb.ObjectFactory objFactory =
                    new com.sun.identity.liberty.ws.meta.jaxb.ObjectFactory();
            com.sun.identity.federation.jaxb.entityconfig.ObjectFactory 
                    configFactory = getEntityConfigObjectFactory();
            com.sun.identity.federation.jaxb.entityconfig.EntityConfigElement 
                    entityConfig = configFactory.createEntityConfigElement();
            EntityDescriptorElement entityDescriptor =
                    objFactory.createEntityDescriptorElement();
            entityDescriptor.setProviderID(entityID);
            IDFFMetaManager idffMetaManager = UpgradeUtils.getIDFFMetaManager();
            Map entityAttrs = providerSC.getAttributes();
            if (UpgradeUtils.debug.messageEnabled()) {
                UpgradeUtils.debug.message("providerSC attributes are :" 
                        + entityAttrs);
            }
            updateEntityAttributes(entityDescriptor, entityAttrs);
            for (Iterator iter = providerIDs.iterator(); iter.hasNext();) {
                createEntityConfig = false;
                String providerid = (String) iter.next();
                ServiceConfig config = providerSC.getSubConfig(providerid);
                String configID = config.getSchemaID();
                Map attributes = config.getAttributes();
                if (configID.equals(CONTACT_PERSON)) {
                    com.sun.identity.liberty.ws.meta.jaxb.ContactType conType =
                            setEntityContactPersonAttributes(
                            objFactory, attributes);
                    entityDescriptor.setContactPerson(conType);
                } else if (configID.equals(ENTITY_ORGANIZATION)) {
                    com.sun.identity.liberty.ws.meta.jaxb.OrganizationType
                            orgType = setEntityOrganizationAttributes(
                            objFactory, attributes);
                    entityDescriptor.setOrganization(orgType);
                } else if (configID.equals(AFF_DESC)) {
                    AffiliationDescriptorType affDesc =
                            createAffiliation(objFactory, attributes);
                    entityDescriptor.setAffiliationDescriptor(affDesc);
                    AffiliationDescriptorConfigElement affConfig =
                            createAffiliationEntityConfig(configFactory);
                    if (affConfig != null) {
                        String affID = UpgradeUtils.getAttributeString(
                                IDFF_AFF_ID, attributes);
                        entityConfig.setEntityID(affID);
                        entityConfig.setAffiliationDescriptorConfig(affConfig);
                        createEntityConfig = true;
                    }
                } else {
                    createEntityConfig = true;
                    // get ProviderContactPerson
                    com.sun.identity.liberty.ws.meta.jaxb.ContactType 
                            contactType = 
                            setContactPersonAttributes(config, objFactory);
                    // get ProviderOrganization
                    com.sun.identity.liberty.ws.meta.jaxb.OrganizationType org =
                            setOrganizationAttributes(config, objFactory);

                    ServiceConfig localConfig =
                            config.getSubConfig(LOCAL_CONFIGURATION);
                    Map localAttributes = localConfig.getAttributes();

                    String providerRole =
                            UpgradeUtils.getAttributeString(
                            IDFF_PROVIDER_ROLE, attributes);
                    if (UpgradeUtils.debug.messageEnabled()) {
                        UpgradeUtils.debug.message(classMethod +
                                "Provider role :" + providerRole);
                        UpgradeUtils.debug.message(classMethod +
                                "Local Attributes" + localAttributes);
                    }
                    entityConfig.setEntityID(providerid);
                    String providerType = UpgradeUtils.getAttributeString(
                            IDFF_PROVIDER_TYPE,
                            attributes, DEFAULT_PROVIDER_TYPE);
                    if (providerType.equals("hosted")) {
                        entityConfig.setHosted(true);
                    } else {
                        entityConfig.setHosted(false);
                    }
                    if (providerRole.equalsIgnoreCase(IDP_ROLE)) {
                        System.out.println("Creating IDP Entity:" + providerid);
                        if (UpgradeUtils.debug.messageEnabled()) {
                            UpgradeUtils.debug.message(classMethod +
                                    "Creating IDP Entity :" + providerid);
                        }
                        IDPDescriptorType idpDesc =
                                objFactory.createIDPDescriptorType();
                        createIDPEntity(providerid, attributes, idpDesc);
                        // set certificate info
                        setKeyDescriptor(attributes, idpDesc, objFactory);
                        if (contactType != null) {
                            idpDesc.getContactPerson().add(contactType);
                        }
                        if (org != null) {
                            idpDesc.setOrganization(org);
                        }
                        entityDescriptor.getIDPDescriptor().add(idpDesc);
                        entityDescriptor.setProviderID(providerid);
                        // set authentication context info.
                        ServiceConfig authCtxConfig =
                                localConfig.getSubConfig(IDP_AUTH_CONTEXT_INFO);
                        Map authAttrs = new HashMap();
                        if (authCtxConfig != null) {
                            ServiceConfig idpAuthCtxConfig =
                                    authCtxConfig.getSubConfig(
                                    IDP_AUTH_CONTEXT_MAPPING);
                            if (idpAuthCtxConfig != null) {
                                authAttrs = idpAuthCtxConfig.getAttributes();
                            }
                        }
                        IDPDescriptorConfigElement idpEntityConfig =
                                createIDPConfig(providerid, attributes,
                                localAttributes, authAttrs);
                        entityConfig.getIDPDescriptorConfig().add(
                                idpEntityConfig);
                    } else if (providerRole.equalsIgnoreCase(SP_ROLE)) {
                        //Assume SP, there's no other role defined.
                        // add auth attrs
                        ServiceConfig authCtxConfig =
                                config.getSubConfig(SP_AUTH_CONTEXT_INFO);
                        Map authAttrs = new HashMap();
                        if (authCtxConfig != null) {
                            ServiceConfig spAuthCtxConfig =
                                    authCtxConfig.getSubConfig(
                                    SP_AUTH_CONTEXT_MAPPING);
                            if (spAuthCtxConfig != null) {
                                authAttrs = spAuthCtxConfig.getAttributes();
                            }
                        }
                        System.out.println("Creating SP Entity:" + providerid);
                        if (UpgradeUtils.debug.messageEnabled()) {
                            UpgradeUtils.debug.message(classMethod +
                                    "Creating SP EntityID : " + providerid);
                        }
                        SPDescriptorType spDesc =
                                objFactory.createSPDescriptorType();
                        createSPEntity(providerid, attributes, spDesc);
                        setKeyDescriptor(attributes, spDesc, objFactory);
                        if (contactType != null) {
                            spDesc.getContactPerson().add(contactType);
                        }
                        if (org != null) {
                            spDesc.setOrganization(org);
                        }
                        entityDescriptor.getSPDescriptor().add(spDesc);
                        entityDescriptor.setProviderID(providerid);
                        SPDescriptorConfigElement spEntityConfig =
                                createSPConfig(providerid, attributes,
                                localAttributes, authAttrs);
                        entityConfig.getSPDescriptorConfig().add(
                                spEntityConfig);
                    }
                }
                idffMetaManager.createEntityDescriptor(
                        ROOT_REALM, entityDescriptor);
                if (createEntityConfig) {
                    idffMetaManager.createEntityConfig(
                            ROOT_REALM, entityConfig);
                }
            }
        } catch (Exception smex) {
            UpgradeUtils.debug.error(classMethod + "Error :", smex);
        }
    }

    /**
     *  Creates Affiliation Descriptor
     */
    private static AffiliationDescriptorType createAffiliation(
            com.sun.identity.liberty.ws.meta.jaxb.ObjectFactory objFactory,
            Map attributes) {
        String classMethod = "Migrate:createAffiliation";
        AffiliationDescriptorType affDesc = null;
        try {
            affDesc = objFactory.createAffiliationDescriptorType();
            Set affiliationMember = (Set) attributes.get(IDFF_AFF_MEMBER);
            if (affiliationMember != null && !affiliationMember.isEmpty()) {
                Iterator a = affiliationMember.iterator();
                while (a.hasNext()) {
                    String affMember = (String) a.next();
                    affDesc.getAffiliateMember().add(affMember);
                }
            }
            String affID = UpgradeUtils.getAttributeString(
                    IDFF_AFF_ID, attributes);
            affDesc.setAffiliationID(affID);
            String affOwnerID = UpgradeUtils.getAttributeString(
                    IDFF_AFF_OWNER_ID, attributes);
            affDesc.setAffiliationOwnerID(affOwnerID);
            String validUntil = UpgradeUtils.getAttributeString(
                    IDFF_VALID_UNTIL, attributes);
            if (validUntil != null) {
                Date date = DateUtils.stringToDate(validUntil);
                Calendar calDate = Calendar.getInstance();
                calDate.setTime(date);
                affDesc.setValidUntil(calDate);
            }
            String cacheDuration = UpgradeUtils.getAttributeString(
                    IDFF_CACHE_DURATION, attributes);
            if (cacheDuration != null) {
                affDesc.setCacheDuration(cacheDuration);
            }
            setAffKeyDescriptor(attributes, affDesc, objFactory);
            String extension = UpgradeUtils.getAttributeString(
                    IDFF_EXTENSION, attributes);
            if (extension != null) {
                com.sun.identity.liberty.ws.meta.jaxb.ExtensionType extType =
                        objFactory.createExtensionType();
                extType.getAny().add(extension);
                affDesc.setExtension(extType);
            }
        } catch (Exception e) {
            UpgradeUtils.debug.error(classMethod +
                    "Error Creating Affiliation Descriptor", e);
        }
        return affDesc;
    }

    /**
     * Creates Affiliation Config
     */
    private AffiliationDescriptorConfigElement createAffiliationConfig(
            com.sun.identity.federation.jaxb.entityconfig.ObjectFactory 
            configFactory, Map attributes) {
        String classMethod = "Migrate:createAffiliationConfig";
        AffiliationDescriptorConfigElement affConfig = null;
        try {
            affConfig =
                    configFactory.createAffiliationDescriptorConfigElement();
            String encAlias = UpgradeUtils.getAttributeString(
                    IDFF_ENC_ALIAS, attributes);
            if (encAlias != null) {
                AttributeType attr5 = UpgradeUtils.getAttribute(
                        configFactory.createAttributeElement(),
                        "encryptionCertAlias",
                        encAlias);
                affConfig.getAttribute().add(attr5);
            }
        } catch (Exception e) {
            UpgradeUtils.debug.error(classMethod +
                    "Error creating affiliation entity config", e);
        }
        return affConfig;
    }

    /**
     * Creates Provider Contact Person.
     */
    private static com.sun.identity.liberty.ws.meta.jaxb.ContactType 
            setContactPersonAttributes(ServiceConfig serviceConfig,
            com.sun.identity.liberty.ws.meta.jaxb.ObjectFactory objectFactory) {

        String classMethod = "Migrate:setContactPersonAttributes";

        com.sun.identity.liberty.ws.meta.jaxb.ContactType contactType = null;
        try {
            ServiceConfig contactPersonConfig =
                    serviceConfig.getSubConfig(PROVIDER_CONTACT_PERSON);
            if (contactPersonConfig != null) {
                Map contactPersonAttrs = contactPersonConfig.getAttributes();
                if (UpgradeUtils.debug.messageEnabled()) {
                    UpgradeUtils.debug.message("Contact Person Attrs :" 
                            + contactPersonAttrs);
                }
                contactType = objectFactory.createContactType();
                String company =
                        UpgradeUtils.getAttributeString(
                        IDFF_COMPANY, contactPersonAttrs, EMPTY_VALUE);
                contactType.setCompany(company);

                String givenName =
                        UpgradeUtils.getAttributeString(
                        IDFF_GIVEN_NAME, contactPersonAttrs, EMPTY_VALUE);
                contactType.setGivenName(givenName);
                String surName =
                        UpgradeUtils.getAttributeString(IDFF_SURNAME,
                        contactPersonAttrs, EMPTY_VALUE);
                contactType.setSurName(surName);
                Set emailAddress =
                        (Set) contactPersonAttrs.get(IDFF_EMAIL_ADDRESS);
                if (emailAddress != null && !emailAddress.isEmpty()) {
                    Iterator i = emailAddress.iterator();
                    while (i.hasNext()) {
                        contactType.getEmailAddress().add(i.next());
                    }
                }
                Set telephoneNumber =
                        (Set) contactPersonAttrs.get(IDFF_TELEPHONE_NUM);
                if (telephoneNumber != null && !telephoneNumber.isEmpty()) {
                    Iterator i = telephoneNumber.iterator();
                    while (i.hasNext()) {
                        contactType.getTelephoneNumber().add(i.next());
                    }
                }
                String extension =
                        UpgradeUtils.getAttributeString(
                        IDFF_EXTENSION, contactPersonAttrs);
                if (extension != null) {
                    com.sun.identity.liberty.ws.meta.jaxb.ExtensionType extType
                            = objectFactory.createExtensionType();
                    extType.getAny().add(extension);
                }
                String libertyPrincipalId =
                        UpgradeUtils.getAttributeString(
                        IDFF_PRINCIPAL_ID, contactPersonAttrs);
                if (libertyPrincipalId != null) {
                    contactType.setLibertyPrincipalIdentifier(
                            libertyPrincipalId);
                }
                String conType =
                        UpgradeUtils.getAttributeString(
                        IDFF_CONTACT_TYPE, contactPersonAttrs);
                if (conType != null) {
                    contactType.setContactType(conType);
                }

            }
        } catch (Exception e) {
            UpgradeUtils.debug.error(classMethod +
                    "Error setting attributes" + e.getMessage());
        }
        return contactType;
    }

    /** 
     * Creates Provider Organization 
     */
    private static com.sun.identity.liberty.ws.meta.jaxb.OrganizationType
            setOrganizationAttributes(ServiceConfig config,
            com.sun.identity.liberty.ws.meta.jaxb.ObjectFactory objectFactory) {
        String classMethod = "Migrate:setOrganizationAttributes";
        com.sun.identity.liberty.ws.meta.jaxb.OrganizationType orgType = null;
        try {
            ServiceConfig orgConfig =
                    config.getSubConfig(PROVIDER_ORGANIZATION);
            Map orgConfigAttrs = orgConfig.getAttributes();
            System.out.println("orgConfigAttrs :" + orgConfigAttrs);
            if (orgConfigAttrs != null && !orgConfigAttrs.isEmpty()) {
                orgType = objectFactory.createOrganizationType();
                Set orgName = (Set) orgConfigAttrs.get(IDFF_ORG_NAME);
                Set orgDisplayName =
                        (Set) orgConfigAttrs.get(IDFF_ORG_DISPLAY_NAME);
                Set orgURL = (Set) orgConfigAttrs.get(IDFF_ORG_URL);
                String extension =
                        UpgradeUtils.getAttributeString(
                        IDFF_ORG_EXTENSION, orgConfigAttrs);
                if ((orgName == null || orgName.isEmpty()) &&
                        (orgDisplayName == null || orgDisplayName.isEmpty()) &&
                        (orgURL == null || orgURL.isEmpty()) &&
                        extension == null) {
                    return null;
                }

                if (orgName != null && !orgName.isEmpty()) {
                    Iterator i = orgName.iterator();
                    while (i.hasNext()) {
                        String orgNameStr = (String) i.next();
                        int index = orgNameStr.indexOf(PIPE);
                        if (index != -1) {
                            String lang = orgNameStr.substring(0, index);
                            String orgN = orgNameStr.substring(index + 1, 
                                    orgNameStr.length());
                            com.sun.identity.liberty.ws.meta.jaxb.OrganizationNameType 
                                    orgNameType = 
                                    objectFactory.createOrganizationNameType();
                            orgNameType.setLang(lang);
                            orgNameType.setValue(orgN);
                            orgType.getOrganizationName().add(orgNameType);
                        }
                    }
                }
                if (orgDisplayName != null && !orgDisplayName.isEmpty()) {
                    Iterator i = orgDisplayName.iterator();
                    while (i.hasNext()) {
                        String orgDisplayStr = (String) i.next();
                        int index = orgDisplayStr.indexOf(PIPE);
                        if (index != -1) {
                            String lang = orgDisplayStr.substring(0, index);
                            String orgD = orgDisplayStr.substring(
                                    index + 1, orgDisplayStr.length());
                            com.sun.identity.liberty.ws.meta.jaxb.OrganizationDisplayNameType
                                    odType = 
                                    objectFactory.createOrganizationDisplayNameType();
                            odType.setLang(lang);
                            odType.setValue(orgD);
                            orgType.getOrganizationDisplayName().add(odType);
                        }
                    }
                }
                if (orgURL != null && !orgURL.isEmpty()) {
                    Iterator i = orgURL.iterator();
                    while (i.hasNext()) {
                        String localizedURI = (String) i.next();
                        int index = localizedURI.indexOf(PIPE);
                        if (index != -1) {
                            String lang = localizedURI.substring(0, index);
                            String uri = localizedURI.substring(
                                    index + 1, localizedURI.length());
                            com.sun.identity.liberty.ws.meta.jaxb.LocalizedURIType
                                    luriType = 
                                    objectFactory.createLocalizedURIType();
                            luriType.setLang(lang);
                            luriType.setValue(uri);
                            orgType.getOrganizationURL().add(luriType);
                        }
                    }
                }
                if (extension != null) {
                    com.sun.identity.liberty.ws.meta.jaxb.ExtensionType extType 
                            = objectFactory.createExtensionType();
                    extType.getAny().add(extension);
                    orgType.setExtension(extType);
                }
            }
        } catch (Exception e) {
            UpgradeUtils.debug.error(classMethod +
                    "Error setting attribtues", e);
        }
        return orgType;
    }

    /** 
     * Creates Contact Person Element for EntityDescriptor 
     */
    private static com.sun.identity.liberty.ws.meta.jaxb.ContactType 
            setEntityContactPersonAttributes(
            com.sun.identity.liberty.ws.meta.jaxb.ObjectFactory objectFactory,
            Map contactPersonAttrs) {
        String classMethod = "Migrate:setEntityContactPersonAttributes";
        com.sun.identity.liberty.ws.meta.jaxb.ContactType contactType = null;
        if (contactPersonAttrs == null || contactPersonAttrs.isEmpty()) {
            if (UpgradeUtils.debug.messageEnabled()) {
                UpgradeUtils.debug.message(classMethod +
                        "No Entity ContactPerson Attributes");
            }
        } else {
            try {
                contactType = objectFactory.createContactType();
                String company = UpgradeUtils.getAttributeString(
                        IDFF_ENTITY_COMPANY, contactPersonAttrs, EMPTY_VALUE);
                contactType.setCompany(company);
                String givenName = UpgradeUtils.getAttributeString(
                        IDFF_ENTITY_GIVEN_NAME, contactPersonAttrs,EMPTY_VALUE);
                contactType.setGivenName(givenName);
                String surName =
                        UpgradeUtils.getAttributeString(
                        IDFF_ENTITY_SURNAME, contactPersonAttrs, EMPTY_VALUE);
                contactType.setSurName(surName);
                Set emailAddress =
                        (Set) contactPersonAttrs.get(IDFF_ENTITY_EMAIL);
                if (emailAddress != null || !emailAddress.isEmpty()) {
                    Iterator i = emailAddress.iterator();
                    while (i.hasNext()) {
                        contactType.getEmailAddress().add(i.next());
                    }
                }
                Set telephoneNumber =
                        (Set) contactPersonAttrs.get(IDFF_ENTITY_TELE_NUM);
                if (telephoneNumber != null || !telephoneNumber.isEmpty()) {
                    Iterator i = telephoneNumber.iterator();
                    while (i.hasNext()) {
                        contactType.getTelephoneNumber().add(i.next());
                    }
                }
                String extension = UpgradeUtils.getAttributeString(
                        IDFF_ENTITY_EXTENSION, contactPersonAttrs, EMPTY_VALUE);
                if (extension != null) {
                    ExtensionType extType = objectFactory.createExtensionType();
                    extType.getAny().add(extension);
                }
                String libertyPrincipalId = UpgradeUtils.getAttributeString(
                        IDFF_ENTITY_LIBERTY_PRINCIPAL_ID, contactPersonAttrs,
                        EMPTY_VALUE);
                if (libertyPrincipalId != null) {
                    contactType.setLibertyPrincipalIdentifier(
                            libertyPrincipalId);
                }
                String conType = UpgradeUtils.getAttributeString(
                        IDFF_ENTITY_CONTACT_TYPE, contactPersonAttrs);
                if (conType != null) {
                    contactType.setContactType(conType);
                }
            } catch (Exception e) {
                UpgradeUtils.debug.error(classMethod +
                        "Error creating Entity Contact Person", e);
            }
        }
        return contactType;
    }

    /**
     * Creates Entity Organization object.
     */
    private static com.sun.identity.liberty.ws.meta.jaxb.OrganizationType
            setEntityOrganizationAttributes(
            com.sun.identity.liberty.ws.meta.jaxb.ObjectFactory objectFactory,
            Map orgAttrs) {

        String classMethod = "Migrate:setEntityOrganizationAttributes";
        com.sun.identity.liberty.ws.meta.jaxb.OrganizationType orgType = null;
        try {
            orgType = objectFactory.createOrganizationType();
            if (orgAttrs != null && !orgAttrs.isEmpty()) {
                Set orgName = (Set) orgAttrs.get(IDFF_ENTITY_ORG_NAME);

                if (orgName != null && !orgName.isEmpty()) {
                    Iterator i = orgName.iterator();

                    while (i.hasNext()) {
                        String orgNameStr = (String) i.next();
                        int index = orgNameStr.indexOf(PIPE);
                        if (index != -1) {
                            String lang = orgNameStr.substring(0, index);
                            String value = orgNameStr.substring(index + 1, 
                                    orgNameStr.length());
                            com.sun.identity.liberty.ws.meta.jaxb.OrganizationNameType 
                                    orgNameType = 
                                    objectFactory.createOrganizationNameType();
                            orgNameType.setLang(lang);
                            orgNameType.setValue(value);
                            orgType.getOrganizationName().add(orgNameType);
                        }
                    }
                }
                Set orgDisplayName =
                        (Set) orgAttrs.get(IDFF_ENTITY_ORG_DISPLAY_NAME);
                if (orgDisplayName != null && !orgDisplayName.isEmpty()) {
                    Iterator i = orgDisplayName.iterator();
                    while (i.hasNext()) {
                        String orgDispStr = (String) i.next();
                        int index = orgDispStr.indexOf(PIPE);
                        if (index != -1) {
                            String lang = orgDispStr.substring(0, index);
                            String value = orgDispStr.substring(index + 1, 
                                    orgDispStr.length());
                            OrganizationDisplayNameType orgDName =
                                    objectFactory.createOrganizationDisplayNameType();
                            orgDName.setLang(lang);
                            orgDName.setValue(value);
                            orgType.getOrganizationDisplayName().add(orgDName);
                        }
                    }
                }
                Set orgURL = (Set) orgAttrs.get(IDFF_ENTITY_ORG_URL);
                if (orgURL != null && !orgURL.isEmpty()) {
                    Iterator i = orgURL.iterator();
                    while (i.hasNext()) {
                        String localizedURI = (String) i.next();
                        int index = localizedURI.indexOf(PIPE);
                        if (index != -1) {
                            String lang = localizedURI.substring(0, index);
                            String uri = localizedURI.substring(index + 1, 
                                    localizedURI.length());
                            com.sun.identity.liberty.ws.meta.jaxb.LocalizedURIType
                                    luriType = 
                                    objectFactory.createLocalizedURIType();
                            luriType.setLang(lang);
                            luriType.setValue(uri);
                            orgType.getOrganizationURL().add(luriType);
                        }
                    }
                }
                String extension = UpgradeUtils.getAttributeString(
                        IDFF_ENTITY_ORG_EXTENSION, orgAttrs);
                if (extension != null) {
                    ExtensionType extType = objectFactory.createExtensionType();
                    extType.getAny().add(extension);
                    orgType.setExtension(extType);
                }
            }
        } catch (Exception e) {
            UpgradeUtils.debug.error(classMethod +
                    "Error Create Entity Organization", e);
        }
        return orgType;
    }

    /**
     * Adds attributes to AffiliationDescriptor
     */
    private static void setAffKeyDescriptor(Map attributes,
            AffiliationDescriptorType affDesc,
            com.sun.identity.liberty.ws.meta.jaxb.ObjectFactory objFactory) {

        String classMethod = "Migrate:setAffKeyDescriptor";
        String encryptAlias = UpgradeUtils.getAttributeString(
                IDFF_AFF_ENC_ALIAS, attributes);

        boolean isEncAlias = (encryptAlias != null);
        if (isEncAlias) {
            try {
                com.sun.identity.liberty.ws.meta.jaxb.KeyDescriptorType 
                        keyDescriptor = objFactory.createKeyDescriptorType();
                com.sun.identity.liberty.ws.common.jaxb.xmlsig.ObjectFactory 
                        cObjFactory =
                        new com.sun.identity.liberty.ws.common.jaxb.xmlsig.ObjectFactory();

                String affEncCert =
                        IDFFMetaSecurityUtils.buildX509Certificate(
                        encryptAlias);
                byte[] encBytes = affEncCert.getBytes();
                String encryptionMethod = UpgradeUtils.getAttributeString(
                        IDFF_AFF_ENC_METHOD, attributes, DEFAULT_ENC_METHOD);

                keyDescriptor.setEncryptionMethod(encryptionMethod);
                String keySize = UpgradeUtils.getAttributeString(
                        IDFF_AFF_KEY_SIZE, attributes, DEFAULT_KEY_SIZE);
                if (keySize == null || keySize.equals(INVALID_KEY_SIZE)) {
                    keySize = DEFAULT_KEY_SIZE;
                }
                keyDescriptor.setKeySize(new BigInteger(keySize));
                String keyUse = UpgradeUtils.getAttributeString(
                        IDFF_AFF_KEY_USE, attributes);
                keyDescriptor.setUse(keyUse);

                X509Certificate x509Cert =
                        cObjFactory.createX509DataTypeX509Certificate(encBytes);
                System.out.println("x509 Cert :" + x509Cert);
                X509DataType x509DataType = cObjFactory.createX509DataType();
                x509DataType.getX509IssuerSerialOrX509SKIOrX509SubjectName()
                        .add(x509Cert);
                KeyInfoType keyInfoType = cObjFactory.createKeyInfoType();
                keyInfoType.getContent().add(x509Cert);
                keyDescriptor.setKeyInfo(keyInfoType);
                affDesc.getKeyDescriptor().add(keyDescriptor);
            } catch (Exception e) {
                UpgradeUtils.debug.error(classMethod + "Error setting " + 
                        " attributes in Affiliation Descriptor", e);
            }
        }
    }

    /**
     *  Creates SPDescriptorConfig 
     */
    static SPDescriptorConfigElement createSPConfig(String providerID,
            Map attributes, Map localAttributes, Map authAttrs) {
        String classMethod = "Migrate:createSPConfig";
        com.sun.identity.federation.jaxb.entityconfig.SPDescriptorConfigElement 
                spConfig = null;
        try {
            // Extended meta data
            com.sun.identity.federation.jaxb.entityconfig.ObjectFactory 
                configFactory =
                new com.sun.identity.federation.jaxb.entityconfig.ObjectFactory();
            spConfig = configFactory.createSPDescriptorConfigElement();
            List attrs = new ArrayList();
            setExtendedAttrs(configFactory, spConfig,
                    attributes, localAttributes, attrs);

            String metaAlias = UpgradeUtils.getAttributeString(
                    IDFF_META_ALIAS, attributes);
            if (metaAlias == null) {
                String assertionURL = UpgradeUtils.getAttributeString(
                        IDFF_ASSERTION_CONSUMER_SERVICE_URL, attributes);
                // get it from the URL
                int index = assertionURL.indexOf(META_ALIAS);
                metaAlias = assertionURL.substring(index + 9,
                        assertionURL.length());
            }
            if (UpgradeUtils.debug.messageEnabled()) {
                UpgradeUtils.debug.message(classMethod + "SP MetaAlias : " 
                        + metaAlias);
            }
            spConfig.setMetaAlias(metaAlias);

            String enableIDPProxy = UpgradeUtils.getAttributeString(
                    IDFF_ENABLE_PROXY, attributes, FALSE);

            AttributeType attr1 = UpgradeUtils.getAttribute(
                    configFactory.createAttributeElement(),
                    ENABLE_IDP_PROXY,
                    enableIDPProxy);
            attrs.add(attr1);

            Set idpProxyList = (Set) attributes.get(IDFF_PROXY_LIST);
            AttributeType attr2 = configFactory.createAttributeElement();
            attr2.setName(IDP_PROXY_LIST);
            if (idpProxyList != null && !idpProxyList.isEmpty()) {
                Iterator i = idpProxyList.iterator();
                while (i.hasNext()) {
                    attr2.getValue().add(i.next());
                }
            } else {
                attr2.getValue().add(EMPTY_VALUE);
            }
            attrs.add(attr2);

            String idpProxyCount = UpgradeUtils.getAttributeString(
                    IDFF_PROXY_COUNT, attributes, DEFAULT_PROXY_COUNT);
            AttributeType attr3 = UpgradeUtils.getAttribute(
                    configFactory.createAttributeElement(),
                    IDP_PROXY_COUNT, idpProxyCount);

            attrs.add(attr3);
            String enableAffiliation = UpgradeUtils.getAttributeString(
                    IDFF_ENABLE_AFFILIATION, attributes, FALSE);
            AttributeType attr4 = UpgradeUtils.getAttribute(
                    configFactory.createAttributeElement(),
                    ENABLE_AFFILIATION, enableAffiliation);

            attrs.add(attr4);
            String useIntroductionForProxy =
                    UpgradeUtils.getAttributeString(
                    IDFF_INTRODUCTION_FOR_IDPPROXY,
                    attributes, FALSE);
            AttributeType attr5 = UpgradeUtils.getAttribute(
                    configFactory.createAttributeElement(),
                    USE_INTRO_IDP_PROXY, useIntroductionForProxy);

            attrs.add(attr5);
            String nameIDPolicy = UpgradeUtils.getAttributeString(
                    IDFF_NAMEID_POLICY, attributes, FEDERATED);

            AttributeType attr6 = UpgradeUtils.getAttribute(
                    configFactory.createAttributeElement(), NAMEID_POLICY,
                    nameIDPolicy);
            attrs.add(attr6);

            // set auth attrs 
            if (authAttrs != null && !authAttrs.isEmpty()) {
                String level = UpgradeUtils.getAttributeString(
                        IDFF_IDP_LEVEL, authAttrs, DEFAULT_LEVEL);

                String authCtx = UpgradeUtils.getAttributeString(
                        IDFF_AUTH_CTX, authAttrs, DEFAULT_AUTH_CONTEXT);

                String spAuthnMapping = new StringBuffer().append(CONTEXT_PARAM)
                        .append(EQUAL).append(authCtx).append(PIPE)
                        .append(LEVEL_PARAM).append(EQUAL)
                        .append(level).toString();

                AttributeType attr7 = UpgradeUtils.getAttribute(
                        configFactory.createAttributeElement(),
                        SP_AUTHCONTEXT_MAPPING, spAuthnMapping);
                attrs.add(attr7);
            }
            setSPLocalAttributes(configFactory, localAttributes,
                    attributes, attrs, spConfig);
            Iterator attIterator = attrs.iterator();
            while (attIterator.hasNext()) {
                AttributeType a = (AttributeType) attIterator.next();
                spConfig.getAttribute().add(a);
            }
        } catch (Exception je) {
            UpgradeUtils.debug.error(classMethod +
                    "Error creating SP Entity Config ", je);
        }
        return spConfig;
    }

    /**
     * Set IDFF Service Provider Local Configuration attributes 
     * in SP Entity Config.
     */
    private static void setSPLocalAttributes(
            com.sun.identity.federation.jaxb.entityconfig.ObjectFactory
            configFactory, Map localAttributes, Map attributes, List attrs,
            SPDescriptorConfigElement spConfig) {
        String classMethod = "Migrate:setSPLocalAttributes";
        try {
            AttributeType attr3 = configFactory.createAttributeElement();
            attr3.setName(SUPPORTED_SSO_PROFILE);
            attr3.getValue().add(ARTIFACT);
            attr3.getValue().add(POST);
            attr3.getValue().add(WML_POST);
            attr3.getValue().add(LECP);
            attrs.add(attr3);

            String forceAuthn = UpgradeUtils.getAttributeString(
                    IDFF_FORCE_AUTHN, localAttributes, FALSE);

            AttributeType attr4 =
                    UpgradeUtils.getAttribute(
                    configFactory.createAttributeElement(),
                    FORCE_AUTHN, forceAuthn);
            attrs.add(attr4);

            String isPassive = UpgradeUtils.getAttributeString(
                    IDFF_IS_PASSIVE, localAttributes, FALSE);

            AttributeType attr5 =
                    UpgradeUtils.getAttribute(
                    configFactory.createAttributeElement(),
                    IS_PASSIVE, isPassive);
            attrs.add(attr5);

            String ssoFailureRedirectURL = UpgradeUtils.getAttributeString(
                    IDFF_SSO_FAIL_URL, localAttributes, EMPTY_VALUE);

            AttributeType attr6 =
                    UpgradeUtils.getAttribute(
                    configFactory.createAttributeElement(),
                    SSO_FAILURE_REDIRECT_URL, ssoFailureRedirectURL);
            attrs.add(attr6);

            String attrMapperClass = UpgradeUtils.getAttributeString(
                    IDFF_ATTR_MAPPER_CLASS, localAttributes, EMPTY_VALUE);

            AttributeType attr7 = UpgradeUtils.getAttribute(
                    configFactory.createAttributeElement(),
                    ATTRIBUTE_MAPPER, attrMapperClass);
            attrs.add(attr7);

            Set attributeMap = (Set) localAttributes.get(IDFF_SP_ATTR_MAP);
            if (attributeMap != null && !attributeMap.isEmpty()) {
                AttributeType attr8 = configFactory.createAttributeElement();
                Iterator i = attributeMap.iterator();
                attr8.setName(SP_ATTRIBUTE_MAP);
                while (i.hasNext()) {
                    attr8.getValue().add(i.next());
                }
                attrs.add(attr8);
            }

            String spAdapter = UpgradeUtils.getAttributeString(
                    IDFF_SP_ADAPTER, localAttributes, DEFAULT_SP_ADAPTER);

            AttributeType attr15 = UpgradeUtils.getAttribute(
                    configFactory.createAttributeElement(),
                    SP_ADAPTER, spAdapter);
            attrs.add(attr15);
        } catch (Exception e) {
            UpgradeUtils.debug.error(classMethod +
                    "Error setting atttributes in SP Entity Config", e);
        }
    }

    /**
     * Creates Service Provider Entity Descriptor
     */
    private static void createSPEntity(String providerID, Map attributes,
            SPDescriptorType spDesc) {
        String classMethod = "Migrate:createSPEntity";
        try {
            int defaultID = 1;
            String assertionConsumerServiceURL =
                    UpgradeUtils.getAttributeString(
                    IDFF_ASSERTION_CONSUMER_SERVICE_URL, attributes);
            String id = UpgradeUtils.getAttributeString(
                    IDFF_ASSERTION_ID, attributes);
            String isDefaultStr = UpgradeUtils.getAttributeString(
                    IDFF_IS_DEFAULT, attributes);
            boolean isDefault = new Boolean(isDefaultStr).booleanValue();

            com.sun.identity.liberty.ws.meta.jaxb.ObjectFactory objFactory =
                    new com.sun.identity.liberty.ws.meta.jaxb.ObjectFactory();
            AssertionConsumerServiceURLType assertionConsumerURL =
                    objFactory.createSPDescriptorTypeAssertionConsumerServiceURLType();

            assertionConsumerURL.setValue(assertionConsumerServiceURL);
            if (id == null || id.length() == 0) {
                id = new Integer(defaultID).toString();
                defaultID++;
            }
            assertionConsumerURL.setId(id);
            assertionConsumerURL.setIsDefault(isDefault);
            spDesc.getAssertionConsumerServiceURL().add(assertionConsumerURL);
            setProviderEntity(spDesc, attributes);

            String authnRequestSigned =
                    UpgradeUtils.getAttributeString(
                    IDFF_IS_AUTHNREQ_SIGNED, attributes);
            boolean isAuthnReqSigned =
                    new Boolean(authnRequestSigned).booleanValue();

            spDesc.setAuthnRequestsSigned(isAuthnReqSigned);

            Set terminationProfile =
                    (Set) attributes.get(IDFF_TERM_PROTOCOL_PROFILE);
            if (terminationProfile == null || terminationProfile.isEmpty()) {
                terminationProfile.add(TERM_IDP_HTTP);
                terminationProfile.add(TERM_IDP_SOAP);
            }
            Iterator i = terminationProfile.iterator();
            while (i.hasNext()) {
                spDesc.getFederationTerminationNotificationProtocolProfile()
                        .add(i.next());
            }

            Set sloProtocol = (Set) attributes.get(IDFF_SLO_PROTO_PROFILE);
            if (sloProtocol == null || sloProtocol.isEmpty()) {
                sloProtocol.add(SLO_IDP_HTTP);
                sloProtocol.add(SLO_IDP_SOAP);
            }
            i = sloProtocol.iterator();
            while (i.hasNext()) {
                spDesc.getSingleLogoutProtocolProfile().add(i.next());
            }

            Set rniProtocol = (Set) attributes.get(IDFF_NAMEID_REGIS_PROFILE);
            if (rniProtocol == null || rniProtocol.isEmpty()) {
                rniProtocol.add(RNI_IDP_HTTP);
                rniProtocol.add(RNI_IDP_SOAP);
            }
            i = rniProtocol.iterator();
            while (i.hasNext()) {
                spDesc.getRegisterNameIdentifierProtocolProfile().add(i.next());
            }
        } catch (Exception e) {
            UpgradeUtils.debug.error(classMethod + "Error creating SP Entity " 
                    + "Descriptor for : " + providerID, e);
        }
    }

    /**
     * Creates IDP entity configuration.
     */
    private static void createIDPEntity(String providerID,
            Map attributes, IDPDescriptorType idpDesc) {
        String classMethod = "Migrate:createIDPEntity";
        try {
            String ssoURL =
                    UpgradeUtils.getAttributeString(IDFF_SSO_URL, attributes);
            idpDesc.setSingleSignOnServiceURL(ssoURL);


            String ssoProfile = UpgradeUtils.getAttributeString(
                    IDFF_SSO_PROTOCOL_PROFILE, attributes);
            idpDesc.getSingleSignOnProtocolProfile().add(ssoProfile);

            String authnServiceURL = UpgradeUtils.getAttributeString(
                    IDFF_AUTHN_SERVICE_URL, attributes);
            if (authnServiceURL != null) {
                idpDesc.setAuthnServiceURL(authnServiceURL);
            }
            setProviderEntity(idpDesc, attributes);

            Set terminationProfile =
                    (Set) attributes.get(IDFF_TERM_PROTOCOL_PROFILE);
            if (terminationProfile == null || terminationProfile.isEmpty()) {
                terminationProfile.add(TERM_SP_HTTP);
                terminationProfile.add(TERM_SP_SOAP);
            }
            Iterator i = terminationProfile.iterator();
            while (i.hasNext()) {
                idpDesc.getFederationTerminationNotificationProtocolProfile().
                        add(i.next());
            }

            Set sloProtocol = (Set) attributes.get(IDFF_SLO_PROTO_PROFILE);
            if (sloProtocol == null || sloProtocol.isEmpty()) {
                sloProtocol.add(SLO_SP_HTTP);
                sloProtocol.add(SLO_SP_SOAP);
            }
            i = sloProtocol.iterator();
            while (i.hasNext()) {
                idpDesc.getSingleLogoutProtocolProfile().add(i.next());
            }

            Set rniProtocol = (Set) attributes.get(IDFF_NAMEID_REGIS_PROFILE);
            if (rniProtocol == null || rniProtocol.isEmpty()) {
                rniProtocol.add(RNI_SP_HTTP);
                rniProtocol.add(RNI_SP_SOAP);
            }
            i = rniProtocol.iterator();
            while (i.hasNext()) {
                idpDesc.getRegisterNameIdentifierProtocolProfile().add(i.next());
            }
        } catch (Exception e) {
            UpgradeUtils.debug.error(classMethod + "Error create IDP Entity " 
                    + "Descriptor for provider " + providerID, e);
        }
    }

    /**
     * Creates IDP Entity Configuration
     */
    static IDPDescriptorConfigElement createIDPConfig(
            String providerID, Map attributes,
            Map localAttributes, Map authAttrs) {
        String classMethod = "Migrate:createIDPConfig";
        IDPDescriptorConfigElement idpConfig = null;
        try {
            // Extended meta data
            com.sun.identity.federation.jaxb.entityconfig.ObjectFactory 
                    configFactory =
                    new com.sun.identity.federation.jaxb.entityconfig.ObjectFactory();
            idpConfig = configFactory.createIDPDescriptorConfigElement();

            List attrs = new ArrayList();

            setExtendedAttrs(configFactory, idpConfig,
                    attributes, localAttributes, attrs);

            // attributes specific to IDP Config only
            String bootStrap = UpgradeUtils.getAttributeString(
                    IDFF_BOOT_STRAP, attributes, TRUE);

            AttributeType attr5 = UpgradeUtils.getAttribute(
                    configFactory.createAttributeElement(),
                    IDP_BOOT_STRAP, bootStrap);
            attrs.add(attr5);

            // update auth attributes
            if (authAttrs != null && !authAttrs.isEmpty()) {
                String key = UpgradeUtils.getAttributeString(
                        IDFF_MODULE_KEY, authAttrs, DEFAULT_KEY);
                String level = UpgradeUtils.getAttributeString(
                        IDFF_IDP_LEVEL, authAttrs, DEFAULT_LEVEL);

                String keyValue = UpgradeUtils.getAttributeString(
                        IDFF_MODULE_KEY_VALUE, authAttrs, DEFAULT_KEY_VALUE);


                String authCtx = UpgradeUtils.getAttributeString(
                        IDFF_AUTH_CTX, authAttrs, DEFAULT_AUTH_CONTEXT);

                String idpAuthnMapping =
                        new StringBuffer().append(CONTEXT_PARAM).append(EQUAL)
                        .append(authCtx).append(PIPE).append(KEY_PARAM)
                        .append(EQUAL).append(key).append(PIPE)
                        .append(VALUE_PARAM).append(EQUAL).append(keyValue)
                        .append(PIPE).append(LEVEL_PARAM).append(EQUAL)
                        .append(level).toString();

                AttributeType attr13 = UpgradeUtils.getAttribute(
                        configFactory.createAttributeElement(),
                        IDP_AUTHCONTEXT_MAPPING, idpAuthnMapping);
                attrs.add(attr13);
            }
            setIDPLocalAttributes(configFactory, localAttributes,
                    attributes, attrs, idpConfig);
            Iterator attIterator = attrs.iterator();
            while (attIterator.hasNext()) {
                AttributeType a = (AttributeType) attIterator.next();
                idpConfig.getAttribute().add(a);
            }
        } catch (Exception je) {
            UpgradeUtils.debug.error(classMethod + "Error create IDP Entity " +
                    "Config for provider " + providerID, je);
        }
        return idpConfig;
    }

    /**
     * Sets provider attributes in extended configuration.
     */
    private static void setExtendedAttrs(
            com.sun.identity.federation.jaxb.entityconfig.ObjectFactory 
            configFactory,
            com.sun.identity.federation.jaxb.entityconfig.BaseConfigType 
            configType, Map attributes, Map localAttributes, List attrs) {
        String classMethod = "Migrate:setExtendedAttrs";
        try {
            String providerStatus = UpgradeUtils.getAttributeString(
                    IDFF_PROVIDER_STATUS, attributes, ACTIVE);

            com.sun.identity.federation.jaxb.entityconfig.AttributeType attr1 =
                    UpgradeUtils.getAttribute(
                    configFactory.createAttributeElement(),
                    PROVIDER_STATUS, providerStatus);
            attrs.add(attr1);

            String signAlias = UpgradeUtils.getAttributeString(
                    IDFF_CERT_ALIAS, attributes, EMPTY_VALUE);

            AttributeType attr2 = UpgradeUtils.getAttribute(
                    configFactory.createAttributeElement(),
                    SIGN_CERT_ALIAS, signAlias);
            attrs.add(attr2);

            String encryptAlias = UpgradeUtils.getAttributeString(
                    IDFF_ENC_ALIAS, attributes, EMPTY_VALUE);
            AttributeType attr3 = UpgradeUtils.getAttribute(
                    configFactory.createAttributeElement(),
                    ENC_CERT_ALIAS, encryptAlias);
            attrs.add(attr3);

            String nameIDEncryption = UpgradeUtils.getAttributeString(
                    IDFF_NAMEID_ENC, attributes, FALSE);
            AttributeType attr4 = UpgradeUtils.getAttribute(
                    configFactory.createAttributeElement(),
                    ENABLE_NAMEID_ENC, nameIDEncryption);
            attrs.add(attr4);

            String nameIDImplClass = UpgradeUtils.getAttributeString(
                    IDFF_NAMEID_IMPL, localAttributes, 
                    DEFAULT_NAMEID_IMPL_CLASS);
            AttributeType attr5 = UpgradeUtils.getAttribute(
                    configFactory.createAttributeElement(),
                    NAMEID_IMPL_CLASS, nameIDImplClass);
            attrs.add(attr5);

            String authType = UpgradeUtils.getAttributeString(IDFF_AUTH_TYPE,
                    localAttributes, LOCAL);
            AttributeType attr6 = UpgradeUtils.getAttribute(
                    configFactory.createAttributeElement(), 
                    AUTH_TYPE, authType);
            attrs.add(attr6);

            String respondsWith = UpgradeUtils.getAttributeString(
                    IDFF_RESPONDS_WITH, localAttributes, EMPTY_VALUE);

            AttributeType attr8 = UpgradeUtils.getAttribute(
                    configFactory.createAttributeElement(),
                    RESPONDS_WITH, respondsWith);
            attrs.add(attr8);

            Set cotList = (Set) attributes.get(IDFF_AUTH_DOMAIN_LIST);
            AttributeType attr9 = configFactory.createAttributeElement();
            attr9.setName(COT_LIST);
            Iterator i = cotList.iterator();
            while (i.hasNext()) {
                attr9.getValue().add(i.next());
            }
            attrs.add(attr9);

            String regDoneURL = UpgradeUtils.getAttributeString(
                    IDFF_NAMEID_REG_DONE_URL, localAttributes, EMPTY_VALUE);
            AttributeType attr10 = UpgradeUtils.getAttribute(
                    configFactory.createAttributeElement(),
                    REGIS_DONE_URL, regDoneURL);
            attrs.add(attr10);

            String providerHomePageURL = UpgradeUtils.getAttributeString(
                    IDFF_HOMEPAGE_URL, localAttributes, EMPTY_VALUE);
            AttributeType attr11 = UpgradeUtils.getAttribute(
                    configFactory.createAttributeElement(),
                    PROVIDER_HOMEPAGE_URL, providerHomePageURL);
            attrs.add(attr11);


            String autoFedEnabled = UpgradeUtils.getAttributeString(
                    IDFF_AUTO_FED_ENABLED, localAttributes, FALSE);
            AttributeType attr12 = UpgradeUtils.getAttribute(
                    configFactory.createAttributeElement(),
                    ENABLE_AUTO_FED, autoFedEnabled);
            attrs.add(attr12);

            String autoFedAttr = UpgradeUtils.getAttributeString(
                    IDFF_AUTO_FED_ATTR, localAttributes, EMPTY_VALUE);
            AttributeType attr13 = UpgradeUtils.getAttribute(
                    configFactory.createAttributeElement(),
                    AUTO_FED_ATTR, autoFedAttr);
            attrs.add(attr13);


            // update auth attributes
            String defaultAuthCtx = UpgradeUtils.getAttributeString(
                    IDFF_DEFAULT_AUTH_CTX, localAttributes, 
                    DEFAULT_AUTH_CONTEXT);
            AttributeType attr14 = UpgradeUtils.getAttribute(
                    configFactory.createAttributeElement(),
                    DEFAULT_AUTHN_CONTEXT, defaultAuthCtx);
            attrs.add(attr14);

        } catch (Exception e) {
            UpgradeUtils.debug.error(classMethod +
                    "Error setting local attributes", e);
        }
    }

    /**
     *  Sets attributes in Provider Entity Descriptor.
     */
    private static void setProviderEntity(
            com.sun.identity.liberty.ws.meta.jaxb.ProviderDescriptorType
            providerDesc, Map attributes) {
        String classMethod = "Migrate:setProviderEntity";
        try {
            String soapEndpoint = UpgradeUtils.getAttributeString(
                    IDFF_SOAP_END_POINT, attributes);
            if (soapEndpoint != null) {
                providerDesc.setSoapEndpoint(soapEndpoint);
            }
            String sloURL = UpgradeUtils.getAttributeString(
                    IDFF_SLO_SERVICE_URL, attributes);
            if (sloURL != null) {
                providerDesc.setSingleLogoutServiceURL(sloURL);
            }

            String sloReturnURL = UpgradeUtils.getAttributeString(
                    IDFF_SLO_RETURN_URL, attributes);
            if (sloReturnURL != null) {
                providerDesc.setSingleLogoutServiceReturnURL(sloReturnURL);
            }

            String termURL = UpgradeUtils.getAttributeString(
                    IDFF_TERM_SERVICE_URL, attributes);
            if (termURL != null) {
                providerDesc.setFederationTerminationServiceURL(termURL);
            }
            String termReturnURL = UpgradeUtils.getAttributeString(
                    IDFF_TERM_RETURN_URL, attributes);
            if (termReturnURL != null) {
                providerDesc.setFederationTerminationServiceReturnURL(
                        termReturnURL);
            }
            String nameRegisURL = UpgradeUtils.getAttributeString(
                    IDFF_NAME_REGIS_URL, attributes);
            if (nameRegisURL != null) {
                providerDesc.setRegisterNameIdentifierServiceURL(nameRegisURL);
            }

            String rniReturnURL = UpgradeUtils.getAttributeString(
                    IDFF_NAMEID_REGIS_RETURN_URL, attributes);
            if (rniReturnURL != null) {
                providerDesc.setRegisterNameIdentifierServiceReturnURL(
                        rniReturnURL);
            }

            /**  TODO don't know format (not being used)
            Set additionalMetaLocation = 
            (Set) attributes.get(IDFF_ADD_META_LOCATION);
            if (additionalMetaLocation != null && 
            !additionalMetaLocation.isEmpty()) {
            Iterator i = additionalMetaLocation.iterator();
            while (i.hasNext()) {
            }
            } */
            com.sun.identity.liberty.ws.meta.jaxb.ObjectFactory objFactory =
                    new com.sun.identity.liberty.ws.meta.jaxb.ObjectFactory();
            com.sun.identity.liberty.ws.meta.jaxb.ExtensionType extensionType =
                    objFactory.createExtensionType();
            Set extensions = (Set) attributes.get(IDFF_EXTENSION);
            if (extensions != null && !extensions.isEmpty()) {
                Iterator i = extensions.iterator();
                extensionType.getAny().add(i.next());
                providerDesc.setExtension(extensionType);
            }

            String validUntil = UpgradeUtils.getAttributeString(
                    IDFF_VALID_UNTIL, attributes);
            if (validUntil != null) {
                Date date = DateUtils.stringToDate(validUntil);
                Calendar calDate = Calendar.getInstance();
                calDate.setTime(date);
                providerDesc.setValidUntil(calDate);
            }

            String cacheDuration = UpgradeUtils.getAttributeString(
                    IDFF_CACHE_DURATION, attributes);
            if (cacheDuration != null) {
                providerDesc.setCacheDuration(cacheDuration);
            }

            String protocol = UpgradeUtils.getAttributeString(
                    IDFF_PROTO_SUPPORT_ENUM, attributes);
            if (protocol != null) {
                providerDesc.getProtocolSupportEnumeration().add(protocol);
            }

            /*FORMAT NOT KNOWN :
            Set nameMappingProtocolProfile = 
            (Set) attributes.get(IDFF_NAMEID_MAPPING_BINDING);
            if ((nameMappingProtocolProfile != null) && 
            (!nameMappingProtocolProfile.isEmpty())) {
            com.sun.identity.liberty.ws.common.jaxb.assertion
            .AuthorityBindingType 
            authBinding = objectFactory.createAuthorityBindingType();
            }*/

            Set nameMappingEncryptionProfile =
                    (Set) attributes.get(IDFF_NAMEID_MAPPING_ENC_PROFILE);
            if ((nameMappingEncryptionProfile != null) &&
                    (!nameMappingEncryptionProfile.isEmpty())) {
                Iterator i = nameMappingEncryptionProfile.iterator();
                while (i.hasNext()) {
                    providerDesc.getNameIdentifierMappingEncryptionProfile()
                            .add(i.next());
                }
            }
        } catch (Exception e) {
            UpgradeUtils.debug.error(classMethod +
                    "Error setting provider attributes entity ", e);
        }
    }

    /** 
     * Sets attributes in Identity Provider extended entity configuration.
     */
    private static void setIDPLocalAttributes(
            com.sun.identity.federation.jaxb.entityconfig.ObjectFactory
            configFactory, Map localAttributes, Map attributes, List attrs,
            IDPDescriptorConfigElement idpConfig) {
        String classMethod = "Migrate:setIDPLocalAttributes";
        try {
            String metaAlias = UpgradeUtils.getAttributeString(
                    IDFF_META_ALIAS, localAttributes);
            if (metaAlias == null) {
                String ssoURL = UpgradeUtils.getAttributeString(
                        IDFF_SSO_URL, attributes);
                // get it from the URL
                int index = ssoURL.indexOf(META_ALIAS);
                metaAlias = ssoURL.substring(index + 9, ssoURL.length());
            }
            idpConfig.setMetaAlias(metaAlias);

            String assertionInterval = UpgradeUtils.getAttributeString(
                    IDFF_ASSERTION_INTERVAL, localAttributes,
                    DEFAULT_ASSERTION_INTERVAL);
            AttributeType attr7 = UpgradeUtils.getAttribute(
                    configFactory.createAttributeElement(),
                    ASSERTION_INTERVAL, assertionInterval);
            attrs.add(attr7);

            String artifactTimeout = UpgradeUtils.getAttributeString(
                    IDFF_ARTIFACT_TIMEOUT, localAttributes,
                    DEFAULT_ARTIFACT_TIMEOUT);
            AttributeType attr8 = UpgradeUtils.getAttribute(
                    configFactory.createAttributeElement(),
                    ARTIFACT_TIMEOUT, artifactTimeout);
            attrs.add(attr8);

            String cleanupInterval = UpgradeUtils.getAttributeString(
                    IDFF_CLEANUP_INTERVAL, localAttributes,
                    DEFAULT_CLEANUP_INTERVAL);
            AttributeType attr9 = UpgradeUtils.getAttribute(
                    configFactory.createAttributeElement(), CLEANUP_INTERVAL,
                    cleanupInterval);
            attrs.add(attr9);

            String assertionLimit = UpgradeUtils.getAttributeString(
                    IDFF_ASSERTION_LIMIT,
                    localAttributes, DEFAULT_ASSERTION_LIMIT);
            AttributeType attr10 = UpgradeUtils.getAttribute(
                    configFactory.createAttributeElement(),
                    ASSERTION_LIMIT, assertionLimit);
            attrs.add(attr10);

            String assertionIssuer = UpgradeUtils.getAttributeString(
                    IDFF_ASSERTION_ISSUER, localAttributes, EMPTY_VALUE);
            AttributeType attr11 = UpgradeUtils.getAttribute(
                    configFactory.createAttributeElement(),
                    ASSERTION_ISSUER, assertionIssuer);
            attrs.add(attr11);

            String attrPlugin = UpgradeUtils.getAttributeString(
                    IDFF_ATTR_PLUGIN,
                    localAttributes, EMPTY_VALUE);

            AttributeType attr12 = UpgradeUtils.getAttribute(
                    configFactory.createAttributeElement(),
                    ATTRIBUTE_PLUGIN, attrPlugin);
            attrs.add(attr12);

            Set attrSet = (Set) attributes.get(IDFF_ATTR_MAP);
            if (attrSet != null && !attrSet.isEmpty()) {
                Iterator i = attrSet.iterator();
                AttributeType attr15 = configFactory.createAttributeElement();
                attr15.setName(IDP_ATTRIBUTE_MAP);
                while (i.hasNext()) {
                    attr15.getValue().add(i.next());
                }
            }
        } catch (Exception e) {
            UpgradeUtils.debug.error(classMethod +
                    "Error setting provider config attribtues", e);
        }
    }

    /**
     * Returns the Entity Config ObjectFactory object.
     * 
     */
    private static com.sun.identity.federation.jaxb.entityconfig.ObjectFactory 
            getEntityConfigObjectFactory() {
        com.sun.identity.federation.jaxb.entityconfig.ObjectFactory cof =
            new com.sun.identity.federation.jaxb.entityconfig.ObjectFactory();
        return cof;
    }

    /**
     * Adds attributes to Entity 
     */
    private static void updateEntityAttributes(
            EntityDescriptorElement entityDescriptor, Map entityAttrs) {
        String classMethod = "UpgradeUtils:updateEntityAttributes";

        String validUntil = UpgradeUtils.getAttributeString(
                IDFF_ENTITY_VALID_UNTIL, entityAttrs);
        if (validUntil != null) {
            entityDescriptor.setValidUntil(validUntil);
        }

        String cacheDuration = UpgradeUtils.getAttributeString(
                IDFF_ENTITY_CACHE_DURATION, entityAttrs);
        if (cacheDuration != null) {
            entityDescriptor.setCacheDuration(cacheDuration);
        }
    }

    /**
     * Create KeyDescriptor object for provider.
     */
    private static void setKeyDescriptor(Map attributes,
            ProviderDescriptorType providerDesc,
            com.sun.identity.liberty.ws.meta.jaxb.ObjectFactory objFactory) {
        String classMethod = "Migrate:setKeyDescriptor";
        String signAlias = UpgradeUtils.getAttributeString(
                IDFF_CERT_ALIAS, attributes);
        String encryptAlias = UpgradeUtils.getAttributeString(
                IDFF_ENC_ALIAS, attributes);

        boolean isSignAlias = (signAlias != null && signAlias.length() > 0);
        boolean isEncAlias = 
                (encryptAlias != null && encryptAlias.length() > 0);
        if (isSignAlias || isEncAlias) {
            try {
                com.sun.identity.liberty.ws.meta.jaxb.KeyDescriptorType
                        keyDescriptor = objFactory.createKeyDescriptorType();
                com.sun.identity.liberty.ws.common.jaxb.xmlsig.ObjectFactory 
                    cObjFactory = 
                    new com.sun.identity.liberty.ws.common.jaxb.xmlsig.ObjectFactory();
                if (isSignAlias) {
                    String idpSignCert =
                            IDFFMetaSecurityUtils.buildX509Certificate(
                            signAlias);
                    byte[] certBytes = idpSignCert.getBytes();
                    X509DataType.X509Certificate x509Cert =
                            cObjFactory.createX509DataTypeX509Certificate(
                            certBytes);
                    System.out.println("sign X509 Cert is : " + x509Cert);
                    X509DataElement x509DataElement =
                            cObjFactory.createX509DataElement();
                    x509DataElement.getX509IssuerSerialOrX509SKIOrX509SubjectName()
                            .add(x509Cert);
                    com.sun.identity.liberty.ws.common.jaxb.xmlsig.KeyInfoType 
                            keyInfoType = cObjFactory.createKeyInfoType();
                    //keyInfoType.getContent().add(x509Cert);
                    keyInfoType.getContent().add(x509DataElement);
                    keyDescriptor.setKeyInfo(keyInfoType);
                    keyDescriptor.setUse("signing");
                    providerDesc.getKeyDescriptor().add(keyDescriptor);
                } // isSign Alias

                if (isEncAlias) {
                    String idpEncCert =
                            IDFFMetaSecurityUtils.buildX509Certificate(
                            encryptAlias);
                    byte[] encBytes = idpEncCert.getBytes();
                    String encryptionMethod = UpgradeUtils.getAttributeString(
                            IDFF_ENC_METHOD, attributes, DEFAULT_ENC_METHOD);
                    keyDescriptor.setEncryptionMethod(encryptionMethod);
                    String keySize = UpgradeUtils.getAttributeString(
                            IDFF_KEY_SIZE, attributes, DEFAULT_KEY_SIZE);
                    keyDescriptor.setKeySize(new BigInteger(keySize));
                    String keyUse = UpgradeUtils.getAttributeString(
                            IDFF_KEY_USE, attributes, KEY_ENC_USE);
                    keyDescriptor.setUse(keyUse);
                    X509DataType.X509Certificate x509Cert =
                            cObjFactory.createX509DataTypeX509Certificate(
                            encBytes);
                    X509DataType x509DataType =
                            cObjFactory.createX509DataType();
                    x509DataType.getX509IssuerSerialOrX509SKIOrX509SubjectName()
                            .add(x509Cert);
                    com.sun.identity.liberty.ws.common.jaxb.xmlsig.KeyInfoType
                            keyInfoType = cObjFactory.createKeyInfoType();
                    keyInfoType.getContent().add(x509Cert);
                    keyDescriptor.setKeyInfo(keyInfoType);
                    providerDesc.getKeyDescriptor().add(keyDescriptor);
                }

            } catch (Exception e) {
                UpgradeUtils.debug.error(classMethod +
                        "Error creating key descriptor", e);
            }
        }
    }

    static AffiliationDescriptorConfigElement createAffiliationEntityConfig(
            com.sun.identity.federation.jaxb.entityconfig.ObjectFactory cfObj)
            throws javax.xml.bind.JAXBException {
        return cfObj.createAffiliationDescriptorConfigElement();
    }
}
