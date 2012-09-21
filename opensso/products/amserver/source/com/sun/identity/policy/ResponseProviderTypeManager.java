/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: ResponseProviderTypeManager.java,v 1.3 2008/06/25 05:43:45 qcheng Exp $
 *
 */



package com.sun.identity.policy;

import java.util.*;

import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOException;
import com.sun.identity.sm.*;
import com.sun.identity.policy.interfaces.ResponseProvider;
import com.sun.identity.shared.locale.AMResourceBundleCache;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.locale.Locale;

/**
 * The class <code>ResponseProviderTypeManager</code> provides
 * methods to get a list of configured <code>ResponseProvider
 * </code> objects, and to obtain a factory object for it.
 *
 * @supported.all.api
 */
public class ResponseProviderTypeManager {

    private static String RESPONSE_PROVIDER = "ResponseProvider";

    private SSOToken token;
    private PolicyManager pm;

    private ResourceBundle rb;
    private static AMResourceBundleCache amCache = 
            AMResourceBundleCache.getInstance();

    private static Debug debug = PolicyManager.debug;

    /**
     * Constructs a <code>ResponseProviderTypeManager</code> object
     */
    ResponseProviderTypeManager() throws SSOException {
	token = ServiceTypeManager.getSSOToken();
	String lstr;
	lstr = token.getProperty("Locale");
        java.util.Locale loc = 
            com.sun.identity.shared.locale.Locale.getLocale(lstr);
        rb = amCache.getResBundle(ResBundleUtils.rbName, loc);
    }

    /**
     * Constructs a <code>ResponseProviderTypeManager</code> object
     * @param pm <code>PolicyManager</code> to initialize the 
     * <code>ResponseProviderTypeManager</code> with
     */
    public ResponseProviderTypeManager(PolicyManager pm) {
	this.pm = pm;
	token = pm.token;
	java.util.Locale loc ;
	try {
	    String lstr = token.getProperty("Locale");
	    loc = com.sun.identity.shared.locale.Locale.getLocale(lstr);
	} catch (SSOException ex) {
	    debug.error ("ResponseProviderTypeManager:Unable to retreive "
		+"locale from SSOToken", ex);
	    loc = Locale.getDefaultLocale();
	}

         if (debug.messageEnabled()) {
            debug.message("ResponseProviderTypeManager locale="+loc+
		"\tI18nFileName = "+ ResBundleUtils.rbName);
        }
        rb = amCache.getResBundle(ResBundleUtils.rbName, loc);
    }

    /**
     * Returns a set of all valid <code>ResponseProvider</code> type names 
     * defined in the  <code>PolicyConfig</code> service. 
     * Out of the box will have only
     * <code>IDRepoResponseProvider</code>
     *
     * @return a set of all valid <code>ResponseProvider</code> type 
     * names defined in the  <code>PolicyConfig</code> service.
     * @throws SSOException if the <code>SSOToken</code> used to create 
     *                      the <code>PolicyManager</code> has become invalid
     * @throws PolicyException for any other abnormal condition.
     */
    public Set getResponseProviderTypeNames() throws SSOException, 
            PolicyException  {
	return (PolicyManager.getPluginSchemaNames(RESPONSE_PROVIDER));
    }

    /**
     * Returns a set of valid <code>ResponseProvider</code> type names 
     * configured.
     * Examples are <code>IDRepoResponseProvider</code> and any other
     * configured providers.
     *
     * @return a set of all valid <code>ResponseProvider</code> type names
     * defined in the  <code>PolicyConfig</code>  service.
     * @throws SSOException if the <code>SSOToken</code> used to create 
     *                      the <code>PolicyManager</code> has become invalid
     * @throws PolicyException for any other abnormal condition
     */
    public Set getSelectedResponseProviderTypeNames() throws SSOException,
            PolicyException  {
        String org = pm.getOrganizationDN();
        Map map = PolicyConfig.getPolicyConfig(org);
        if (map != null) {
            Object answer = map.get(PolicyConfig.SELECTED_RESPONSE_PROVIDERS);
            if (answer != null) {
                return (Set) answer; 
            }
        }
        return Collections.EMPTY_SET;
    }

    /**
     * Returns the type of the <code>ResponseProvider</code> object.
     * For example <code>IDRepoResponseProvider</code> or any other
     * configured providers.
     *
     * @param respProvider <code>ResponseProvider</code> object for which this
     *        method will return its associated type
     * @return type of the responseprovider, e.g. <code>IDRepoResponseProvider
     *         </code> . Returns <code>null</code> if not present.
     */
    public String getResponseProviderTypeName(ResponseProvider respProvider) {
	return responseProviderTypeName(respProvider);
    }

    /**
     * Returns <code>ResponseProvider</code> type name
     * @param respProvider <code>ResponseProvider</code> for which 
     * to get the type name
     * @return <code>ResponseProvider</code> type name for the given
     * <code>ResponseProvider</code>
     */
    public static String responseProviderTypeName(
	ResponseProvider respProvider)
    {
	if (respProvider == null) {
	    return (null);
	}
	String name = null;
	String className = respProvider.getClass().getName();
	Iterator items = PolicyManager.getPluginSchemaNames(RESPONSE_PROVIDER).
	    iterator();
	while (items.hasNext()) {
	    String pluginName = (String) items.next();
	    PluginSchema ps = PolicyManager.getPluginSchema(RESPONSE_PROVIDER, 
		pluginName);
	    if (className.equals(ps.getClassName())) {
		name = pluginName;
		break;
	    }
	}
	return (name);
    }

    /**
     * Returns the I18N properties file name that should be
     * used to localize display names for the given
     * responseprovider name.
     *
     * @param responseProviderTypeName response provider type name
     *
     * @return i18n properties file name
     */
    protected String getI18NPropertiesFileName(
	String responseProviderTypeName) 
    {
	PluginSchema ps = PolicyManager.getPluginSchema(RESPONSE_PROVIDER, 
	    responseProviderTypeName);
	if (ps != null) {
	    return ps.getI18NFileName();
	}
	return null;
    }

    /**
     * Returns the I18N key to be used to localize the
     * display name for the responseprovider name.
     *
     * @param responseProviderName Response provider type name.
     *
     * @return i18n key to obtain the display name.
     */
    public String getI18NKey(String responseProviderName) {
	PluginSchema ps = PolicyManager.getPluginSchema(RESPONSE_PROVIDER, 
	    responseProviderName);
	if (ps != null) {
	    return (ps.getI18NKey());
	}
	return null;
    }

    /**
     * Returns the display name for the response provider
     * @param responseProviderTypeName responseprovider type name
     * @return display name for the response provider
     */
    public String getDisplayName(String responseProviderTypeName) {
	String displayName = null;
	String i18nKey = getI18NKey(responseProviderTypeName);
	if (i18nKey == null || i18nKey.length()==0 ) {
	    displayName = responseProviderTypeName;
	} else {
	    displayName = Locale.getString(rb,i18nKey,debug);
	}
	return displayName;
    }

    /**
     * Returns an instance of the <code>ResponseProvider</code>
     * given the response provider type name.
     *
     * @param responseProviderTypeName response provider type name.
     * @return an instance of the <code>ResponseProvider</code>
     * given the response provider type name.
     * @throws NameNotFoundException if the <code>ResponseProvider</code> 
     * not found
     * @throws PolicyException for any other abnormal condition
     */
    public ResponseProvider getResponseProvider(String responseProviderTypeName)
	throws NameNotFoundException, PolicyException 
    {
	PluginSchema ps = PolicyManager.getPluginSchema(RESPONSE_PROVIDER, 
	    responseProviderTypeName);
	if (ps == null) {
	    throw (new NameNotFoundException(ResBundleUtils.rbName, 
		"invalid_response_provider", null, responseProviderTypeName, 
		PolicyException.RESPONSE_PROVIDER_COLLECTION));
	}

	// Construct the object
	ResponseProvider respProvider = null;
	try {
	    String className = ps.getClassName();
	    respProvider = (ResponseProvider) Class.forName(className).
		newInstance();
	} catch (Exception e) {
	    throw (new PolicyException(e));
	}
	respProvider.initialize(pm.getPolicyConfig());
	return respProvider;
    }

    /**
     * Returns the view bean URL given the <code>ResponseProvider</code>
     *
     * @param respProvider <code>ResponseProvider</code> for which 
     * to get the  view bean URL
     *
     * @return view bean URL defined for the <code>ResponseProvider</code> 
     * plugin in the  policy  service <code>PluginSchema</code>.
     */
    public String getViewBeanURL(ResponseProvider respProvider)  {
	return PolicyManager.getViewBeanURL(RESPONSE_PROVIDER, 
	    respProvider.getClass().getName());
    }
}
