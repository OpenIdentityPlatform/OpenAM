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
 * $Id: ISLocaleContext.java,v 1.14 2008/08/19 19:09:00 veiming Exp $
 *
 */

package com.sun.identity.common;

import com.iplanet.am.util.AMClientDetector;
import com.iplanet.am.util.SystemProperties;
import com.iplanet.am.util.Misc;
import com.iplanet.services.cdm.Client;
import com.iplanet.services.cdm.ClientException;
import com.iplanet.services.cdm.ClientsManager;
import com.iplanet.services.cdm.G11NSettings;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.locale.Locale;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;
import com.sun.identity.sm.ServiceSchema;
import com.sun.identity.sm.ServiceSchemaManager;
import java.security.AccessController;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;

/**
 * Sets the locale suitable for the given situation. Each response to end-user
 * must be in a well defined locale. Even if the user has not logged in, OpenSSO
 * Enterprise should respond in a locale. Hence OpenSSO consults
 * various parameter to find out the locale for any given response. The order
 * and priorty of local setting is as follows:
 *
 * Priority 0 - OS_LOCALE - value returned by java.util.Locale.getDefault()
 * Priority 1 - PLATFORM_LOCALE - iplanet-am-platform-locale attribute value
 *              in iPlanetAMPlatform Service - Global value for entire 
 *              OpenSSO
 * Priority 2 - AUTH-LOCALE - iplanet-am-auth-locale attribute value in 
 *              iPlantAMAuth service - Org specific locale value
 * Priority 3 - USER_LOCALE - preferredlocale - iPlanetAMUser service and 
 *              it can be configured per org and user level
 * Priority 4 - HTTP_HEADER_LOCALE - Accept-Language header of HTTP Request
 * Priority 5 - URL_LOCALE - locale value passed as URL parameter
 *
 * Usage: There are three key parameters in the request that can decide the
 * locale of a given request. This class expects application to pass all these
 * parameters whenever they are available and use getLocale method to get the
 * current locale The three key parameters are HttpServletRequest - to process
 * Accept-langyage and URL parameter locale UserPreferredLocale - user attribute
 * if user has sucessful login OrgDN - DN of the org in which the user resides
 * it can take take core auth locale of this org <code>
 * ISLocaleContext is = new ISLocaleContext();
 * is.setOrgLocale ("o=isp,dc=iplanet,dc=com"); // sets the org locale 
 * is.setLocale (req) // get locale from accept-lang,locale param etc
 * // if user logs in
 * is.setUserLocale (loc);
 * </code>
 * For your response , get the locale using is.getLocale();
 * 
 * Locale with highest priority takes precentse over lower priority.
 */
public class ISLocaleContext {
    public static final int OS_LOCALE = 0;

    public static final int PLATFORM_LOCALE = 1;

    public static final int CORE_AUTH_LOCALE = 2;

    public static final int USER_PREFERRED_LOCALE = 3;
    
    public static final int HTTP_HEADER_LOCALE = 4;

    public static final int URL_LOCALE = 5;

    private static int initLocaleLevel; // Default locale level

    // Default locale for every request, it will be changed by HTTP_HEADER,
    // USER_PREFERRED_LOCALE or URL_LOCALE is defined for any request
    private static java.util.Locale initLocale;

    private static AMClientDetector clientDt;

    private static Client defaultClient;
    
    private Client client;

    private static G11NSettings g11nSettings;

    private static String initCharset;    

    private String charset;

    private java.util.Locale locale;

    private int localeLevel;

    static {
        String installTime = 
            SystemProperties.get(AdminTokenAction.AMADMIN_MODE, "false");
        if (installTime.equalsIgnoreCase("false")) {
            clientDt = new AMClientDetector();
            if ((clientDt != null ) && clientDt.isDetectionEnabled()) {
                defaultClient = ClientsManager.getDefaultInstance();
            }
        }
        try {
            String initLocaleStr;
            SSOToken token = (SSOToken) AccessController
                    .doPrivileged(AdminTokenAction.getInstance());
            String platformLocale = null;
            String authLocale = null;
            try {
                ServiceSchemaManager scm = new ServiceSchemaManager(
                        "iPlanetAMPlatformService", token);
                ServiceSchema psc = scm.getGlobalSchema();
                Map attrs = psc.getAttributeDefaults();
                platformLocale = Misc.getMapAttr(attrs,
                        "iplanet-am-platform-locale");
            } catch (SMSException ex) {
                // Ignore the exception and leave platformLocale = null;
            }
            try {
                ServiceSchemaManager scm = new ServiceSchemaManager(
                        "iPlanetAMAuthService", token);
                ServiceSchema psc = scm.getOrganizationSchema();
                Map attrs = psc.getAttributeDefaults();

                authLocale = Misc.getMapAttr(attrs, "iplanet-am-auth-locale");
            } catch (SMSException ex) {
                // Ignore the exception and leave authLocale = null;
            }

            String tLocale = authLocale;
            initLocaleLevel = CORE_AUTH_LOCALE;
            initLocaleStr = tLocale;
            if (tLocale == null || tLocale.length() == 0) {
                tLocale = platformLocale;
                initLocaleLevel = PLATFORM_LOCALE;
                initLocaleStr = tLocale;
                if (tLocale == null || tLocale.length() == 0) {
                    tLocale = java.util.Locale.getDefault().toString();
                    initLocaleLevel = OS_LOCALE;
                    initLocaleStr = tLocale;
                }
            }
            initLocale = Locale.getLocale(initLocaleStr);
        } catch (SSOException ex) {
            // unable to get SSOToken hence fallback to OS_LOCALE
            initLocale = java.util.Locale.getDefault();
            initLocaleLevel = OS_LOCALE;
        }

        initCharset = (defaultClient != null) ?
            defaultClient.getCharset(initLocale) :
            Constants.CONSOLE_UI_DEFAULT_CHARSET;
    }

    /**
     * Initializes <code>ISLocaleContext</code> to default level It examines
     * OS_LOCALE, PLATFORM_LOCALE, AUTH_LOCALE and initialize them based on
     * their priority
     */
    public ISLocaleContext() {
        locale = initLocale;
        localeLevel = initLocaleLevel;
        charset = initCharset;
        client = defaultClient;
    }

    /**
     * Initialize <code>ISLocaleContext</code> for a given Org It can look
     * into orgs core auth locale value and set the value if it is available
     */
    public ISLocaleContext(String orgDN) {
        this();
        setOrgLocale(orgDN);
    }

    /**
     * Set locale to given level.
     * 
     * @param level Possible values are <code>OS_LOCALE,PLATFORM_LOCALE</code>
     *        <code>AUTH_LOCALE</code>, <code>ACCEPT_LOCALE</code>,
     *        <code>USER_PREFERRED_LOCALE</code>, <code>URL_LOCALE</code>.
     * @param loc Locale value in string example <code>en</code>,
     *        <code>ja_JP</ocde>. Warning: This method overrides priority
     *        lookup mechanism.
     * 
     */
    public void setLocale(int level, String loc) {
        if (loc != null && loc.length() > 0) {
            setLocale(level, Locale.getLocale(loc));
        }
    }

    /**
     * Set locale to given level.
     * 
     * @param level Possible values are <code>OS_LOCALE,PLATFORM_LOCALE</code>
     *        <code>AUTH_LOCALE</code>, <code>ACCEPT_LOCALE</code>,
     *        <code>USER_PREFERRED_LOCALE</code>, <code>URL_LOCALE</code>.
     * @param loc Locale value.
     */
    public void setLocale(int level, java.util.Locale loc) {
        if (level < 0 || level > URL_LOCALE) {
            throw new IllegalArgumentException("Invalid locale level=" + level);
        }
        if (level >= localeLevel) {
            localeLevel = level;
            charset = (client != null) ? charset = client.getCharset(loc) :
                Constants.CONSOLE_UI_DEFAULT_CHARSET;
            locale = loc;
        }
    }

    /**
     * Set locale based on HTTP Servlet Request.
     * 
     * @param request Analyze HttpHeader and look for URL parameter called
     *        locale . If it is set, it takes high precedence. Else look for
     *        <code>accept-language</code> header and set the locale if it
     *        is present.
     */
    public void setLocale(HttpServletRequest request) {
        if (request != null) {
            String superLocale = request.getParameter("locale");
            String agentType = Client.CDM_DEFAULT_CLIENT_TYPE;
            if ((clientDt != null) && clientDt.isDetectionEnabled()) {
                agentType = clientDt.getClientType(request);

                try {
                    client = ClientsManager.getInstance(agentType);
                } catch (ClientException ex) {
                    // Unable to determine the client hence we fall back
                    // to default client . It is performed at initalization
                } catch (Exception e) {
                    // Unable to determine the client hence we fall back
                    // to default client . It is performed at initalization
                }
            }

            if (superLocale != null && superLocale.length() > 0) {
                setLocale(URL_LOCALE, superLocale);
            } else {
                String acceptLangHeader = request.getHeader("Accept-Language");
                if ((acceptLangHeader != null)
                        && (acceptLangHeader.length() > 0)) {
                    String acclocale = 
                        Locale.getLocaleStringFromAcceptLangHeader(
                                acceptLangHeader);
                    setLocale(HTTP_HEADER_LOCALE, acclocale);
                }
            }
        }
    }

    /**
     * Set the current locale level to <code>USER_LOCALE</code> and sets the
     * value if current locale level is greater than <code>USER_LOCALE</code>,
     * this setting will be ignored.
     * 
     * @param loc Locale.
     */
    public void setUserLocale(java.util.Locale loc) {
        setLocale(USER_PREFERRED_LOCALE, loc);
    }

    /**
     * Set the current locale level to <code>USER_LOCALE</code> and sets the
     * value the locale value is separated by underscore character
     * <code>ex:en_US</code>.
     * 
     * @param loc Locale.
     */
    public void setUserLocale(String loc) {
        if (loc == null || loc.length() == 0) {
            return;
        }
        setLocale(USER_PREFERRED_LOCALE, Locale.getLocale(loc));
    }

    /**
     * Returns Locale value that has got highest prioirty
     * 
     * @return locale.
     */
    public java.util.Locale getLocale() {
        return locale;
    }

    /**
     * get current priority level of locale
     * 
     * @return localeLevel
     */
    public int getLocaleLevel() {
        return localeLevel;
    }

    /**
     * Update locale context based on org locale user locale takes precedence
     * over this locale
     * 
     * @param orgDN -
     *            Distinguished Name of Organization
     */
    public void setOrgLocale(String orgDN) {
        if (localeLevel > CORE_AUTH_LOCALE) {
            return;
        }
        
        try {
            SSOToken token = (SSOToken) AccessController
                .doPrivileged(AdminTokenAction.getInstance());
            ServiceConfigManager scm = new ServiceConfigManager(
                "iPlanetAMAuthService", token);
            ServiceConfig sc = scm.getOrganizationConfig(orgDN, null);
            Map attrs = sc.getAttributes();
            String locale = Misc.getMapAttr(attrs, "iplanet-am-auth-locale");
            if (locale != null && locale.length() > 0) {
                setLocale(CORE_AUTH_LOCALE, Locale.getLocale(locale));
            }

        } catch (SSOException ssoe) {
            // Problems in getting SSOToken which can be safely
            // ignored as we have a fallback locale
        } catch (SMSException amex) {
            // Problems in getting attribute from org
            // ignored as we have fallback locale
        }
    }

    /**
     * get mime charset to be used for current request This class detectes
     * clientType using Http-Accept-Lang header You should have used
     * setLocale(HttpServletRequest req) before calling this method. Otherwise
     * it will use default clientType configured by client detection service All
     * http headers should have mime charset. For example use this API whenever
     * you set http header using setContentType()
     * 
     * @return mime charset to be used for current request
     */
    public String getMIMECharset() {
        return charset;
    }

    /**
     * get java charset to be used for current request This class detectes
     * clientType using Http-Accept-Lang header You should have used
     * setLocale(HttpServletRequest req) before calling this method. Otherwise
     * it will use default clientType configured by client detection service.
     * Most of the cases Javas codeset converter understands MIME charset names
     * withsome exceptions. It is recommended to use this API to set form hidden
     * field such as gx_charset
     * 
     * @return java charset to be used for current request
     */

    public String getJavaCharset() {
        String jCharset = G11NSettings.JAVA_CHARSET_NAME;
        if (g11nSettings == null) {
            g11nSettings = G11NSettings.getInstance();
        }
        if (g11nSettings != null) {
            jCharset = g11nSettings.getJavaCharset(charset);
        }
        return jCharset;
    }

}
