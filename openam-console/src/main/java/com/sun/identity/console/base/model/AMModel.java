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
 * $Id: AMModel.java,v 1.9 2009/12/11 23:25:19 veiming Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.console.base.model;

import com.iplanet.sso.SSOToken;
import com.sun.identity.idm.AMIdentity;
import java.util.Set;
import java.util.Map;

/* - NEED NOT LOG - */

/**
 * This defines a set of methods that are exposed to view beans.
 */
public interface AMModel
    extends AMAdminConstants
{
    /**
     * Name of paging page size limit.
     */
    String CONSOLE_PAGING_SIZE_ATTR = "iplanet-am-admin-console-paging-size";

    /**
     * Name of maximum search result.
     */
    String CONSOLE_SEARCH_RESULT_LIMIT_ATTR =
        "iplanet-am-admin-console-search-limit";

    /**
     * Name of maximum search time.
     */
    String CONSOLE_SEARCH_TIME_LIMIT_ATTR =
        "iplanet-am-admin-console-search-timeout";

    /**
     * Default paging size.
     */
    int DEFAULT_PAGE_SIZE = 25;

    /**
     * Default search results limit.
     */
    int DEFAULT_SEARCH_RESULT_LIMIT = 100;

    /**
     * Default search time out limit.
     */
    int DEFAULT_SEARCH_TIME_LIMIT = 5;

    /**
     * Returns client type from SSO token
     *
     * @return client type
     */
    String getClientType();

    /**
     * Returns random string
     *
     * @return random string
     */
    String getRandomString();

    /**
     * Returns the preferred locale of currently logged in user.
     *
     * @return <code>java.util.Locale</code> of currently logged in user.
     */
    java.util.Locale getUserLocale();

    /**
     * Returns user's single sign on token.
     *
     * @return user's single sign on token.
     */
    SSOToken getUserSSOToken();

    /**
     * Returns DN of currently logged in user.
     *
     * @return DN of currently logged in user.
     */
    String getUserDN();

    /**
     * Returns currently logged in user.
     *
     * @return currently logged in user.
     */
    String getUserName();

    /**
     * Returns universal ID of user.
     *
     * @return Universal ID of user.
     */
    String getUniversalID();

    /**
     * Returns localized string.
     *
     * @param key Key of resource string.
     * @return localized string.
     */
    String getLocalizedString(String key);

    /**
     * Returns paging page size from template if one exists otherwise page size
     * that is defined globally.
     *
     * @return paging page size
     */
    int getPageSize();

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
    String getErrorString(Throwable ex);

    /**
     * Returns the localized service name.
     *
     * @param service Name of service.
     * @return  the localized service name.
     */
    String getLocalizedServiceName(String service);

    /**
     * Returns properties view bean URL of a service.
     *
     * @param serviceName Name of service.
     * @return properties view bean URL of a service. Returns null if
     *         this URL is not defined in the schema.
     */
    String getServicePropertiesViewBeanURL(String serviceName);

    /**
     * Gets start DN
     *
     * @return start DN
     */
    String getStartDN();

    /**
     * Returns directory managemenet start DN
     *
     * @return start DN
     */
    String getStartDSDN();

    /**
     * Returns a map of suppported entity type to its localized name.
     *
     * @param realmName Name of Realm.
     * @return a map of suppported entity type to its localized name.
     */
    Map getSupportedEntityTypes(String realmName);
    
    /**
     * Returns a map of supported agent type to its localized name.
     *
     * @return a map of supported agent type to its localized name.
     */
    Map getSupportedAgentTypes();

    /**
     * Writes log event.
     *
     * @param id Log Message ID.
     * @param data Log Data.
     */
    void logEvent(String id, String[] data);

    public String getLocationDN();

    public void setLocationDN(String location);

    public String getConsoleDirectory();

    /**
     * Returns true if the user profile is to be ignored by the console.
     * The authentication service sets a property in the users session
     * based on the properties in the core authentication service.
     *
     * @return true if the user profile should be ignored.
     */
    public boolean ignoreUserProfile();

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
    public String getUserDisplayName(AMIdentity id);
    
       /**
     * Returns a set of special user identities. This set of identities 
     * typically should not be displayed in the console. 
     *
     * @param realmName Name of Realm.
     * @return a list of special user identities that cannot be displayed.
     */
    public Set getSpecialUsers(String realmName);
    
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
        throws AMConsoleException;

    /**
     * Returns <code>true</code> if server is running with <code>AMSDK</code>
     * repo enabled.
     * 
     * @return <code>true</code> if server is running with <code>AMSDK</code>
     * repo enabled.
     */
    boolean isAMSDKEnabled();

    /**
     * Returns <code>true</code> if identity is amadmin user.
     *
     * @param amid user object.
     * @return <code>true</code> if identity is amadmin user.
     */
    boolean isAmadminUser(AMIdentity amid);
}
