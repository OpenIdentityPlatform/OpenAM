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
 * $Id: AMModelBase.java,v 1.18 2009/12/11 23:25:19 veiming Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.console.base.model;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.common.DisplayUtils;
import com.sun.identity.common.ISLocaleContext;
import com.sun.identity.common.configuration.AgentConfiguration;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.AMIdentityRepository;
import com.sun.identity.idm.IdConstants;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdType;
import com.sun.identity.idm.IdSearchResults;
import com.sun.identity.idm.IdUtils;
import com.sun.identity.log.messageid.LogMessageProvider;
import com.sun.identity.log.messageid.MessageProviderFactory;
import com.sun.identity.log.LogRecord;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.security.EncryptAction;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.encode.Base64;
import com.sun.identity.shared.ldap.util.DN;
import com.sun.identity.shared.locale.L10NMessage;
import com.sun.identity.shared.locale.Locale;
import com.sun.identity.sm.AttributeSchema;
import com.sun.identity.sm.DNMapper;
import com.sun.identity.sm.OrganizationConfigManager;
import com.sun.identity.sm.SMSEntry;
import com.sun.identity.sm.SchemaType;
import com.sun.identity.sm.ServiceSchema;
import com.sun.identity.sm.ServiceSchemaManager;
import com.sun.identity.sm.SMSException;
import java.io.IOException;
import java.security.AccessController;
import java.text.Collator;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Random;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;
import javax.servlet.http.HttpServletRequest;

/* - NEED NOT LOG - */

/**
 * This class implements all the basic and commonly used methods used by view
 * beans.
 * <p>
 * All the OpenSSO model implementation classes extends from this
 * class.
 */
public class AMModelBase
    implements AMModel
{
    /** debug object */
    public static Debug debug = Debug.getInstance(
        AMAdminConstants.CONSOLE_DEBUG_FILENAME);

    private static SSOToken adminSSOToken =
        AMAdminUtils.getSuperAdminSSOToken();

    private static String LOG_PROVIDER = "Console";

    protected String locationDN = null;
    
    private ResourceBundle resBundle = null;
    private SSOToken ssoToken = null;
    private String userDN = null;
    protected java.util.Locale locale = null;
    private static Random random = new Random();
    private Map mapUserInfo;
    private Map consoleAttributes = null;
    private Map mapSvcSchemaMgrs = new HashMap(10);
    private String consoleJSPDirectory = null;
    private String rbName = AMAdminConstants.DEFAULT_RB;

    private ISLocaleContext localeContext = new ISLocaleContext();

    private static int svcRevisionNumber;

    /**
     * Creates a simple model using default resource bundle. 
     *
     * @param req HTTP Servlet Request
     */
    public AMModelBase(HttpServletRequest req) {
        initialize(req, null);
    }

    public AMModelBase() {
        // do nothing
    }

    /**
     * Creates a simple model.  The LDAP location distinguished name (DN) if
     * default to start DN of the currently logged in user.
     *
     * @param req HTTP Servlet Request
     * @param rbName resource bundle name
     */
    public AMModelBase(HttpServletRequest req, String rbName) {
        initialize(req, rbName);
    }

    /**
     * Creates a model with user information retrieved from the user
     * information map.
     *
     * @param req HTTP Servlet Request
     * @param rbName resource bundle name
     * @param map of user information
     */
    public AMModelBase(HttpServletRequest req, String rbName, Map map) {
        mapUserInfo = map;
        initialize(req, rbName);
    }

    /**
     * Creates a model with user information retrieved from the user
     * information map. (using default resource bundle)
     *
     * @param req HTTP Servlet Request
     * @param map of user information
     */
    public AMModelBase(HttpServletRequest req, Map map) {
        mapUserInfo = map;
        initialize(req, null);
    }


    /**
     * Set location distinguished name
     *
     * @param DN distinguished name
     */
    public void setLocationDN(String DN) {
        try {
            locationDN = (DN != null) ? DN : getStartDSDN();
            ssoToken.setProperty(CONSOLE_LOCATION_DN, locationDN);
        } catch (SSOException e) {
            debug.warning("AMModelBase.setLocationDN", e);
        }
    }

    /**
     * Returns location DN.
     *
     * @return location DN.
     */
    public String getLocationDN() {
        return (locationDN != null) ? locationDN : getStartDSDN();
    }

    /**
     * Returns user information from SSO token or map of serialized information.
     */
    protected void getUserInfo(HttpServletRequest req) {
        userDN = getUniversalID();
        setUserLocale(req);
        resBundle = AMResBundleCacher.getBundle(rbName, locale);
        locale = resBundle.getLocale();
    }

    /**
     * Returns universal ID of user.
     *
     * @return Universal ID of user.
     */
    public String getUniversalID() {
        String univId = null;
        try {
            univId = ssoToken.getProperty(Constants.UNIVERSAL_IDENTIFIER);
        } catch (SSOException e) {
            debug.warning("AMModelBase.getUniversalID", e);
        }
        return (univId != null) ? univId : null;
    }

    /**
     * Returns the preferred locale of currently logged in user.
     *
     * @return <code>java.util.Locale</code> of currently logged in user.
     */
    public java.util.Locale getUserLocale() {
        return locale;
    }

    /**
     * Returns current user's Single Sign On Token.
     *
     * @return current user's Single Sign On Token.
     */
    public SSOToken getUserSSOToken() {
        return ssoToken;
    }

    private void setUserLocale(HttpServletRequest req) {
        boolean bSet = false;
        try {
            String ssoPropLocale = ssoToken.getProperty("Locale");
            if ((ssoPropLocale != null) && (ssoPropLocale.length() > 0)) {
                locale = Locale.getLocale(ssoPropLocale);
                bSet = true;
            }
        } catch (SSOException e) {
            debug.warning("AMModelBase.setUserLocale", e);
        }
        if (!bSet) {
            localeContext.setLocale(req);
            locale = localeContext.getLocale();
        }
    }

    /**
     * Initializes object. It does the followings
     * <ol>
     * <li> check validity of SSO token
     * <li> get user information from user map and SSO token
     * <li> read console profile information from SSO token
     * </ol>
     *
     * @param req HTTP Servlet Request
     * @param rbName resource bundle name
     */
    protected void initialize(HttpServletRequest req, String rbName) {
        try {
            if (rbName != null) {
                this.rbName = rbName;
            }
            ssoToken = AMAuthUtils.getSSOToken(req);
            getUserInfo(req);

            ServiceSchemaManager idRepoServiceSchemaManager = 
                new ServiceSchemaManager(ssoToken,
                    IdConstants.REPO_SERVICE, "1.0");
            svcRevisionNumber = 
                idRepoServiceSchemaManager.getRevisionNumber();

        } catch (SMSException smse) {
            debug.warning("AMModelBase.initialize", smse);
        } catch (SSOException e) {
            debug.warning("AMModelBase.initialize", e);
        }
    }

    /**
     * Returns client type from SSO token
     *
     * @return client type
     */
    public String getClientType() {
        return AMI18NUtils.getClientType(ssoToken);
    }

    /**
     * Returns random string
     *
     * @return random string
     */
    public String getRandomString() {
        StringBuilder sb = new StringBuilder(30);
        byte[] keyRandom = new byte[5];
        random.nextBytes(keyRandom);
        sb.append(System.currentTimeMillis());
        sb.append(Base64.encode(keyRandom));
        return(sb.toString().replace('/', '.'));
    }

    /**
     * Returns the attribute name that is used for the main user display
     * on user entries. This attribute is defined in the administration
     * service in <code>Search Return Attribute/code>. The FIRST entry in
     * this attribute is the value returned. If the attribute contains the
     * following values <code>cn uid</code>, then <code>cn</code> will be
     * the return value.
     *   
     * @return attribute name used when displaying user entries.
     */  
    public String getUserDisplayAttribute() {
        String searchAttribute = null;
        Map attributes = getConsoleAttributes();
        Set values = (Set)attributes.get(CONSOLE_USER_SEARCH_RETURN_KEY);

        if ((values != null) && !values.isEmpty()) {
            String tmp = (String)values.iterator().next();
            StringTokenizer tokenizer = new StringTokenizer(tmp);
            searchAttribute = tokenizer.nextToken();
        }

        return searchAttribute;
    }

    /**
     * Returns the attribute name that is used when performing searches
     * on user entries. This attribute is defined in the administration
     * service in <code>User Search Key</code>.
     *   
     * @return attribute name for user searches.
     */  
    public String getUserSearchAttribute() {
        String searchAttribute = null;
        Map attributes = getConsoleAttributes();
        
        Set values = (Set)attributes.get(CONSOLE_USER_SEARCH_KEY);

        if ((values != null) && !values.isEmpty()) {
            searchAttribute = (String)values.iterator().next();
        }
 
        return searchAttribute;
    }

    /**
     * Returns DN of currently logged in user.
     *
     * @return DN of currently logged in user.
     */
    public String getUserDN() {
        String dn = "";
        if (userDN != null) {
            try {
                dn = IdUtils.getDN(IdUtils.getIdentity(ssoToken));
            } catch (SSOException e) {
                debug.error("AMModelBase.getUserDN", e);
            } catch (IdRepoException e) {
                debug.error("AMModelBase.getUserDN", e);
            }
        }
        return dn;
    }

    /**
     * Returns currently logged in user.
     *
     * @return currently logged in user.
     */
    public String getUserName() {
        return userDN;
    }

    /**
     * Returns the <code>DN</code> of the users organization.
     *   
     * @return <code>DN</code> of the users organization.
     */  
    public String getUserOrganization() {
        String orgDN = "";

        if (userDN != null) {
            try {
                AMIdentity amid = IdUtils.getIdentity(ssoToken);
                orgDN = amid.getRealm();
            } catch (SSOException e) {
                debug.error("AMModelBase.getUserOrganization", e);
            } catch (IdRepoException e) {
                debug.error("AMModelBase.getUserOrganization", e);
            }
        }

        return orgDN;
    }

    /**
     * Returns a localized error message from an exception. If the exception
     * is of type <code>AMException</code> the error code and any possible
     * arguments will be extracted from the exception and the message will be
     * generated from the code and arguments. All other exception types will
     * return the message from <code>Exception.getMessage</code>.
     *
     * @param ex exception
     * @return String error message localized to users locale
     */
    public String getErrorString(Throwable ex) {
        String message = null;

        if (ex instanceof L10NMessage) {
            message = ((L10NMessage)ex).getL10NMessage(locale);
        } else {
            message = ex.getMessage();
        }

        return message;
    }

    /**
     * Writes log event.
     *
     * @param id Log Message ID.
     * @param data Log Data.
     */
    public void logEvent(String id, String[] data) {
        try {
            LogMessageProvider provider = MessageProviderFactory.getProvider(
                LOG_PROVIDER);
            LogRecord rec = provider.createLogRecord(id, data, ssoToken);
            if (rec != null) {
                AMAdminLog.getInstance().doLog(rec);
            } else {
                debug.error("AMModelBase.logEvent: missing log entry, " + id);
            }
        } catch (IOException e) {
            debug.error("AMModelBase.logEvent", e);
        }
    }

    /**
     * Returns resource bundle.
     *
     * @return resource bundle.
     */
    protected ResourceBundle getResourceBundle() {
        return resBundle;
    }

    /**
     * Returns localized string.
     *
     * @param key Key of resource string.
     * @return localized string.
     */
    public String getLocalizedString(String key) {
        return Locale.getString(resBundle, key, debug);
    }

    /**
     * Returns paging page size from template if one exists otherwise page size
     * that is defined globally.
     *
     * @return paging page size
     */
    public int getPageSize() {
        int pageSize = getLimitAttributeValue(CONSOLE_PAGING_SIZE_ATTR);
        return (pageSize != -1) ? pageSize : getGlobalPageSize();
    }
  
    public String getConsoleDirectory() {
        if (consoleJSPDirectory == null) {
            Map m = getConsoleAttributes();
            if (m != null) {
                Set jspDirectory = (Set)m.get(CONSOLE_ORG_CUSTOM_JSP_DIRECTORY);
                if ((jspDirectory != null) && (!jspDirectory.isEmpty())) {
                    consoleJSPDirectory = 
                        (String)jspDirectory.iterator().next();
                }
            }
        }
        return consoleJSPDirectory;
    }

    /**
     * Returns a map of the cosole service attributes configured at the realm
     * where the user started (typically where they logged in at.) If the
     * admin service is not configured in that realm, the defaults are taken
     * from global configuration.
     */
    protected Map getConsoleAttributes() {
        if (consoleAttributes == null) {
            try {
                AMIdentityRepository repo = new AMIdentityRepository(
                    adminSSOToken, getStartDN());
                AMIdentity realmIdentity = repo.getRealmIdentity();
                Set servicesFromIdRepo = realmIdentity.getAssignedServices();
                if (servicesFromIdRepo.contains(ADMIN_CONSOLE_SERVICE)) {
                    consoleAttributes = realmIdentity.getServiceAttributes(
                        ADMIN_CONSOLE_SERVICE);
                } else {
                    OrganizationConfigManager orgCfgMgr =
                        new OrganizationConfigManager(
                            adminSSOToken, getStartDN());
                    consoleAttributes = orgCfgMgr.getServiceAttributes(
                        ADMIN_CONSOLE_SERVICE);
                }
            } catch (SSOException e) {
                debug.error("AMModelBase.getConsoleAttributes", e);
            } catch (SMSException e) {
                debug.error("AMModelBase.getConsoleAttributes", e);
            } catch (IdRepoException e) {
                debug.error("AMModelBase.getConsoleAttributes", e);
            }
        }
        return consoleAttributes;
    }

    private int getLimitAttributeValue(String attributeName) {
        int limit = -1;
        Map map = getConsoleAttributes();

        if ((map != null) && !map.isEmpty()) {
            Set values = (Set)map.get(attributeName);

            if ((values != null) && !values.isEmpty()) {
                String val = (String)values.iterator().next();
                try {
                    limit = Integer.parseInt(val);
                } catch (NumberFormatException e) {
                    debug.error("AMModelBase.getLimitAttributeValue, " +
                        "attributeName=" + attributeName, e);
                }
            }
        }

        return limit;
    }
                                                                                
    /**
     * Returns globally defined page size
     *
     * @return globally defined page size.
     */
    public int getGlobalPageSize() {
        return getGlobalIntegerConsoleAttribute(
            CONSOLE_PAGING_SIZE_ATTR, SchemaType.ORGANIZATION, 1,
                DEFAULT_PAGE_SIZE);
    }

    /**
     * Returns search result limit from template if one exists otherwise page
     * size that is defined globally.
     *
     * @return search result limit.
     */
    public int getSearchResultLimit() {
        int limit = getLimitAttributeValue(CONSOLE_SEARCH_RESULT_LIMIT_ATTR);
        return (limit != -1) ? limit : getGlobalSearchResultLimit();
    }

    /**
     * Returns globally defined search result limit.
     *
     * @return globally defined search result limit.
     */
    public int getGlobalSearchResultLimit() {
        return getGlobalIntegerConsoleAttribute(
            CONSOLE_SEARCH_RESULT_LIMIT_ATTR, SchemaType.ORGANIZATION, 1,
            DEFAULT_SEARCH_TIME_LIMIT);
    }

    /**
     * Returns search time limit from template if one exists otherwise page
     * size that is defined globally.
     *
     * @return search time limit.
     */
    public int getSearchTimeOutLimit() {
        int limit = getLimitAttributeValue(CONSOLE_SEARCH_TIME_LIMIT_ATTR);
        return (limit != -1) ? limit : getGlobalSearchTimeOutLimit();
    }

    /**
     * Returns globally defined search timeout limit.
     *
     * @return globally defined search timeout limit.
     */
    public int getGlobalSearchTimeOutLimit() {
        return getGlobalIntegerConsoleAttribute(
            CONSOLE_SEARCH_TIME_LIMIT_ATTR, SchemaType.ORGANIZATION, 1,
            DEFAULT_SEARCH_TIME_LIMIT);
    }

    private int getGlobalIntegerConsoleAttribute(
        String attrName,
        SchemaType schemaType,
        int lowerLimit,
        int defaultValue
    ) {
        int value = 0;

        try {
            ServiceSchemaManager mgr = new ServiceSchemaManager(
                ADMIN_CONSOLE_SERVICE, ssoToken);
            value =AMAdminUtils.getIntegerAttribute(
                mgr, schemaType, attrName);
        } catch (SSOException e) {
            debug.warning("AMModelBase.getGlobalIntegerConsoleAttribute", e);
        } catch (SMSException e) {
            debug.warning("AMModelBase.getGlobalIntegerConsoleAttribute", e);
        } catch (NumberFormatException e) {
            debug.warning("AMModelBase.getGlobalIntegerConsoleAttribute", e);
        }

        return (value < lowerLimit) ? defaultValue : value;
    }

    /**
     * Returns the localized service name.
     *
     * @param service Name of service.
     * @return the localized service name.
     */
    public String getLocalizedServiceName(String service) {
        return getLocalizedServiceName(service, service);
    }

    /**
     * Returns the localized service name.
     *
     * @param service Name of service.
     * @param defaultValue Default value of service name if localized service
     *        name cannot be determine.
     * @return the localized service name.
     */
    public String getLocalizedServiceName(String service, String defaultValue) {
        String i18nName = defaultValue;

        try {
            ServiceSchemaManager mgr = new ServiceSchemaManager(
                service, ssoToken);
            String rbName = mgr.getI18NFileName();

            if ((rbName != null) && (rbName.trim().length() > 0)) {
                ResourceBundle rb = AMResBundleCacher.getBundle(rbName, locale);

                String i18nKey = null;
                Set types = mgr.getSchemaTypes();
                if (!types.isEmpty()) {
                    SchemaType type = (SchemaType)types.iterator().next();
                    ServiceSchema schema = mgr.getSchema(type);
                    if (schema != null) {
                        i18nKey = schema.getI18NKey();
                    }
                }

                if ((i18nKey != null) && (i18nKey.length() > 0)) {
                    i18nName = Locale.getString(rb, i18nKey, debug);
                }
            }
        } catch (SSOException e) {
            debug.warning("AMModelBase.getLocalizedServiceName", e);
        } catch (SMSException e) {
            debug.warning("AMModelBase.getLocalizedServiceName", e);
        } catch (MissingResourceException e) {
            debug.warning("AMModelBase.getLocalizedServiceName", e);
        }
                                                                                
        return i18nName;
    }

    protected ResourceBundle getServiceResourceBundle(String serviceName) {
        ResourceBundle rb = null;

        try {
            ServiceSchemaManager mgr = new ServiceSchemaManager(
                serviceName, ssoToken);
            String rbName = mgr.getI18NFileName();

            if ((rbName != null) && (rbName.trim().length() > 0)) {
                rb = AMResBundleCacher.getBundle(rbName, locale);
            }
        } catch (SSOException e) {
            debug.warning("AMModelBase.getServiceResourceBundle", e);
        } catch (SMSException e) {
            debug.warning("AMModelBase.getServiceResourceBundle", e);
        }

        return rb;
    }

    /**
     * Returns properties view bean URL of a service.
     *
     * @param serviceName Name of service.
     * @return properties view bean URL of a service. Returns null if
     *         this URL is not defined in the schema.
     */
    public String getServicePropertiesViewBeanURL(String serviceName) {
        String url = null;

        try {
            ServiceSchemaManager mgr = new ServiceSchemaManager(
                serviceName, ssoToken);
            url = mgr.getPropertiesViewBeanURL();
        } catch (SSOException e) {
            debug.warning("AMModelBase.getServicePropertiesViewBeanURL", e);
        } catch (SMSException e) {
            debug.warning("AMModelBase.getServicePropertiesViewBeanURL", e);
        }

        return url;
    }

    /**
     * Gets URL of hyperlink to logout
     *
     * @return URL of hyperlink to logout page
     */
    public static String getLogoutURL() {
        StringBuilder url = new StringBuilder(30);
        url.append(AMSystemConfig.serverDeploymentURI)
           .append(AMAdminConstants.URL_LOGOUT);

        if (AMSystemConfig.isConsoleRemote) {
            url.insert(0, AMSystemConfig.serverURL);
        }

        return url.toString();
    }

    /**
     * Returns a map of suppported entity type to its localized name.
     *
     * @param realmName Name of Realm.
     * @return a map of suppported entity type to its localized name.
     */
    public Map getSupportedEntityTypes(String realmName) {
        Map map = null;
        if (realmName == null) {
            realmName = "/";
        }

        try {
             SSOToken adminToken = (SSOToken)AccessController.doPrivileged(
                 AdminTokenAction.getInstance());
            AMIdentityRepository repo = new AMIdentityRepository(
                adminToken, realmName);
            Set supportedTypes = repo.getSupportedIdTypes();
            map = new HashMap(supportedTypes.size() *2);

            for (Iterator iter = supportedTypes.iterator(); iter.hasNext(); ) {
                IdType type = (IdType)iter.next();
                if ( (!type.equals(IdType.AGENTONLY) &&
                    !type.equals(IdType.AGENTGROUP) &&
                    !type.equals(IdType.AGENT) ) ||
                    (type.equals(IdType.AGENT) && (svcRevisionNumber < 30))
                ) {
                    // add the "Agent" tab only if revision number of
                    // sunIdentityRepository service is less than 30.
                    // This is for backward compatibility to support 
                    // this scenerio : OpenSSO 8.0 server against
                    // AM 7.x existing DIT (Coexistence).

                    map.put(type.getName(), 
                        getLocalizedString(type.getName()));
                }
            }
        } catch (IdRepoException e) {
            debug.warning("AMModelBase.getSupportedTypes", e);
        } catch (SSOException e) {
            debug.warning("AMModelBase.getSupportedTypes", e);
        }

        return (map != null) ? map : Collections.EMPTY_MAP;
    }
    
    /**
     * Returns a map of supported agent type to its localized name.
     *
     * @return a map of supported agent type to its localized name.
     */
    public Map getSupportedAgentTypes() {
        Map map = null;
        try {
            Set types = AgentConfiguration.getAgentTypes();
            map = new HashMap(types.size() *2);

            for (Iterator iter = types.iterator(); iter.hasNext(); ) {
                String name = (String)iter.next();
                map.put(name, getLocalizedString("agenttype." + name));
            }
        } catch (SMSException e) {
            debug.warning("AMModelBase.getSupportedTypes", e);
        } catch (SSOException e) {
            debug.warning("AMModelBase.getSupportedTypes", e);
        }

        return (map != null) ? map : Collections.EMPTY_MAP;
    }

    /**
     * Gets start DN
     *
     * @return start DN
     */
    public String getStartDN() {
        String startDN = "/";
        try {
            startDN = DNMapper.orgNameToRealmName(
                ssoToken.getProperty(Constants.ORGANIZATION));
        } catch (SSOException e) {
            debug.warning("AMModelBase.getStartDN", e);
        }
        return startDN;
    }

    /**
     * Returns directory management start DN.
     *
     * @return directory management start DN.
     */
    public String getStartDSDN(){
        String startDN = "/";
        try {
            startDN = ssoToken.getProperty(Constants.ORGANIZATION);
        } catch (SSOException e) {
            debug.warning("AMModelBase.getStartDSDN", e);
        }
        return startDN;
    }

    protected Set getAttributesToDisplay(
        ServiceSchemaManager mgr,
        SchemaType schemaType,
        String schemaName
    ) {
        ServiceSchema schema = null;
        try {
            schema = mgr.getSchema(schemaType);
        } catch (SMSException smse) {
            debug.warning("error getting schema", smse);
        }

        if (schema == null) {
            return Collections.EMPTY_SET;
        }

        ServiceSchema subSchema = null;
        try {
            subSchema = schema.getSubSchema(schemaName);
        } catch (SMSException smse) {
            debug.warning("error getting subschema", smse);
        }
        if (subSchema == null) {
            return Collections.EMPTY_SET;
        }

        Set attrSchemaSet = Collections.EMPTY_SET;
        Set attrSchemaNames = subSchema.getAttributeSchemaNames();
        if (attrSchemaNames != null) {
            Collator collator = Collator.getInstance(getUserLocale());
            attrSchemaSet = new TreeSet(new AMAttrSchemaComparator(collator));
            Iterator asnIterator = attrSchemaNames.iterator();
            while (asnIterator.hasNext()) {
                String asn = (String)asnIterator.next();
                AttributeSchema attrSchema = subSchema.getAttributeSchema(asn);
                if (isDisplayed(attrSchema)) {
                    attrSchemaSet.add(attrSchema);
                }
            }
        }
        return attrSchemaSet; 
    }

    /**
     * Determines whether attribute is to be displayed or not
     *
     * @param attrSchema - attribute schema
     * @return true if
     */
    protected boolean isDisplayed(AttributeSchema attrSchema) {
        boolean displayed = true;
        if ((attrSchema == null) ||
            (attrSchema.getI18NKey() == null) ||
            (attrSchema.getI18NKey().length() == 0)) {
            displayed = false;
        }
        return displayed;
    }

    /**
     * Gets service schema manager, we cache instance of this manager per
     * HTTP request
     *
     * @param name of service
     * @return service schema manager
     * @throws SSOException when SSO token is invalid
     * @throws SMSException when we are unable to retrieve the service
     *         schema manager
     */
    protected ServiceSchemaManager getServiceSchemaManager(String name)
        throws SSOException, SMSException
    {
        ServiceSchemaManager mgr =
            (ServiceSchemaManager)mapSvcSchemaMgrs.get(name);

        if (mgr == null) {
            mgr = new ServiceSchemaManager(name, getUserSSOToken());
            mapSvcSchemaMgrs.put(name, mgr);
        }

        return mgr;
    }

    /**
     * Converts a set of string values to encrypted values. 
     * <code>AMPasswordUtil.encrypt</code> will be called on each value in
     * the set to convert the current value to an encrypted value. 
     *
     * @param values containing string values
     * @return Set of encrypted data.
     */
    public Set encryptString(Set values) {
        String tmp = "";
        if (!values.isEmpty()) {
            tmp = (String)values.iterator().next();
        }
        Set rs = new HashSet(2);
        rs.add((String)AccessController.doPrivileged(new EncryptAction(tmp)));
        return rs;
    }

    public static String getStartDN(HttpServletRequest req) {
        String startDN = "/";
        try {
            SSOToken token = AMAuthUtils.getSSOToken(req);
            startDN = DNMapper.orgNameToRealmName(
                token.getProperty(Constants.ORGANIZATION));
        } catch (SSOException e) {
            debug.warning("AMModelBase.getStartDN", e);
        }
        return startDN;
    }

    /**
     * Gets the organization where user authenticated to. This value is found
     * in single-sign on token.
     *   
     * return organization where user authenticated.
     */  
    public String getAuthenticatedOrgDN() {
        return AMAuthUtils.getAuthenticatedOrgDN(ssoToken);
    }

    /**
     * Check the value of the user status attribute. It needs
     * to be either 'Active' or 'Inactive' for the display to be set
     * correctly.
     */
    protected void validateUserStatusEntry(Map data) {
        Set h = (Set)data.get(ATTR_USER_STATUS);
        if ((h != null) && (!h.isEmpty())) {
            String tmp = (String)h.iterator().next();
            if (tmp.equalsIgnoreCase(STRING_ACTIVE)) {
                tmp = STRING_ACTIVE;
            } else {
                tmp = STRING_INACTIVE;
            }  
            h.clear();
            h.add(tmp);
            data.put(ATTR_USER_STATUS, h);
        }
    }  
 
    /**
     * Returns true if the user profile is to be ignored by the console.
     * The authentication service sets a property in the users session
     * based on the properties in the core authentication service.
     *
     * @return true if the user profile should be ignored.
     */
    public boolean ignoreUserProfile() {
        String profile = null;
        try {
            profile = ssoToken.getProperty(ISAuthConstants.USER_PROFILE);
        } catch (SSOException e) {
            debug.warning("AMModelBase.ignoreUserProfile", e);
        }

        return ((profile != null) && profile.equals(ISAuthConstants.IGNORE));
    }

    /**
     * Returns the formated display name for user identities. This is needed
     * to handle the situation where the attribute displayed for the user is
     * not the same as the naming attribute. It is also possible that the
     * attribute being displayed has multiple values. In the multiple value
     * situation each of the values will be separated by a semicolon ";".
     * 
     * @param id identity of the user being displayed.
     * @return String value of the attribute to be displayed.
     */
    public String getUserDisplayName(AMIdentity id) {
        String name = "";
        try {
            // get the values for the search attribute
            Set a = id.getAttribute(getUserSearchAttribute());
            if ((a != null) && (a.size() > 0)) {
                StringBuilder tmp = new StringBuilder(56);

                // we know there is at least one entry, so add it
                Iterator i = a.iterator();
                tmp.append((String)i.next());

                // iterate through any of the other values
                for (; i.hasNext(); ) {
                    tmp.append("; ").append((String)i.next());
                }
                name = tmp.toString();
            }  
        } catch (IdRepoException idr) {
            debug.warning("AMModelBase.getUserDisplayName ", idr);
        } catch (SSOException sso) {
            debug.warning("AMModelBase.getUserDisplayName ", sso);
        }
        
        // default to the identity name if display cant be constructed.
        return (name.length() > 0) ? name : id.getName();
    }  
    
    /**
     * Returns a set of special user identities. This set of identities 
     * typically should not be displayed in the console. 
     *
     * @param realmName Name of Realm.
     * @return a set of <code>AMIdentity</code> entries that should not be 
     *     displayed in the console.
     */
    public Set getSpecialUsers(String realmName) {
        Set identities = null;
        try {
            AMIdentityRepository repo = new AMIdentityRepository(
                getUserSSOToken(), realmName);
            IdSearchResults results = repo.getSpecialIdentities(IdType.USER);
            identities = results.getSearchResults();
        } catch (IdRepoException e) {
            debug.warning("AMModelBase.getSpecialUsers", e);
        } catch (SSOException e) {
            debug.warning("AMModelBase.getSpecialUsers", e);
        }
        
        return (identities == null) ? Collections.EMPTY_SET : identities;
    }

    /*
     * Returns the realm names that match the specified filter value.
     *
     * @param base Base realm name for this search. null indicates root
     *        suffix.
     * @param filter Filter string.
     * @return realms names that match the filter.
     * @throws AMConsoleException if search fails.
     */
    public Set getRealmNames(String base, String filter)
        throws AMConsoleException
    {
        if ((base == null) || (base.length() == 0)) {
            base = getStartDN();
        }
        String[] param = {base};
        logEvent("ATTEMPT_GET_REALM_NAMES", param);
        try {
            OrganizationConfigManager orgMgr =
                    new OrganizationConfigManager(getUserSSOToken(), base);
            logEvent("SUCCEED_GET_REALM_NAMES", param);
            return appendBaseDN(base,
                orgMgr.getSubOrganizationNames(filter, true), filter);
        } catch (SMSException e) {
            if (e.getExceptionCode() == SMSException.STATUS_NO_PERMISSION) {
                Set result = new HashSet(2);
                result.add(base);
                return result;
            } else {
                String strError = getErrorString(e);
                String[] paramsEx = {base, strError};
                logEvent("SMS_EXCEPTION_GET_REALM_NAMES", paramsEx);
                throw new AMConsoleException(strError);
            }
        }
    }
     
    /*
     * Search results are relative to the base (where the search was
     * performed. Use this to add the base back to the search result,
     * ending up with a fully qualified name.
     */
    private Set appendBaseDN(String base, Set results, String filter) {
        Set altered = new HashSet();
        String displayName = null;
        if (base.equals("/")) {
            displayName = AMFormatUtils.DNToName(this, getStartDSDN());
        } else {
            int idx = base.lastIndexOf("/");
            displayName = (idx != -1) ? base.substring(idx+1) : base;
        }
        if (DisplayUtils.wildcardMatch(displayName, filter)) {
            altered.add(base);
        }
        if ((results != null) && (!results.isEmpty())) {
            for (Iterator i = results.iterator(); i.hasNext(); ) {
                String name = (String)i.next();
                if (name.charAt(0) != '/') {
                    if (base.charAt(base.length() -1) == '/') {
                        altered.add(base + name);
                    } else {
                        altered.add(base + "/" + name);
                    }
                } else {
                    if (base.charAt(base.length() -1) == '/') {
                        altered.add(base.substring(0, base.length()-1) + name);
                    } else {
                        altered.add(base + name);
                    }
                }
            }
        }
        return altered;
    }
    
    /**
     * Returns <code>true</code> if server is running with <code>AMSDK</code>
     * repo enabled.
     * 
     * @return <code>true</code> if server is running with <code>AMSDK</code>
     * repo enabled.
     */
    public boolean isAMSDKEnabled() {
        try {
            ServiceSchemaManager schemaMgr = new ServiceSchemaManager(
                IdConstants.REPO_SERVICE, AMAdminUtils.getSuperAdminSSOToken());
            ServiceSchema orgSchema = schemaMgr.getOrganizationSchema();
            Set names = orgSchema.getSubSchemaNames();
            return (names != null) && names.contains("amSDK");
        } catch (SMSException e) {
            debug.error("AMModelBase.isAMSDKEnabled", e);
            return false;
        } catch (SSOException e) {
            debug.error("AMModelBase.isAMSDKEnabled", e);
            return false;
        }
    }
    
    protected String[] getServerInstanceForLogMsg() {
        String[] array = new String[1];
        array[0] = SystemProperties.getServerInstanceName();
        return array;
    }

    public boolean isAmadminUser(AMIdentity amid) {
        if (amid.getType().equals(IdType.USER)) {
            String amadminUUID = "id=amadmin,ou=user," +
                SMSEntry.getRootSuffix();
            DN dn = new DN(amadminUUID);
            DN amidDN = new DN(amid.getUniversalId());
            return dn.equals(amidDN);
        }

        return false;
    }
}
