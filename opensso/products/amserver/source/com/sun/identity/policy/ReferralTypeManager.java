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
 * $Id: ReferralTypeManager.java,v 1.3 2008/06/25 05:43:44 qcheng Exp $
 *
 */




package com.sun.identity.policy;

import java.util.*;

import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOException;
import com.sun.identity.sm.*;
import com.sun.identity.policy.interfaces.Referral;

import com.sun.identity.shared.locale.AMResourceBundleCache;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.locale.Locale;


/**
 * The class <code>ReferralTypeManager</code> provides
 * methods to get a list of configured <code>Referral
 * </code> objects
 *
 * @supported.all.api
 */
public class ReferralTypeManager {

    private static String REFERRAL = "Referral";

    private SSOToken token;
    private PolicyManager pm;

    private ResourceBundle rb;
    private static AMResourceBundleCache amCache = 
            AMResourceBundleCache.getInstance();

    static Debug debug = PolicyManager.debug;

    /**
     * Creates a <code>ReferralTypeManager</code> object
     */
    protected ReferralTypeManager() throws SSOException {
        token = ServiceTypeManager.getSSOToken();
	String lstr = token.getProperty("Locale");
        java.util.Locale loc = com.sun.identity.shared.locale.Locale.getLocale(
            lstr);
        rb = amCache.getResBundle(ResBundleUtils.rbName, loc);
    }

    /**
     * Creates a <code>ReferralTypeManager</code> object
     * @param pm <code>PolicyManager</code> to initialize 
     * the <code>ReferralTypeManager</code> with
     */
    protected ReferralTypeManager(PolicyManager pm) {
        this.pm = pm;
        token = pm.token;
	java.util.Locale loc ;
	try {
	    String lstr = token.getProperty("Locale");
	    loc = com.sun.identity.shared.locale.Locale.getLocale(lstr);
	} catch (SSOException ex) {
	    debug.error(
                "ConditionTypeManager:Unable to retreive locale from SSOToken",
                ex);
	    loc = Locale.getDefaultLocale();
	}

         if (debug.messageEnabled()) {
            debug.message("SubjectManager locale="+loc+"\tI18nFileName = "+
                     ResBundleUtils.rbName);
        }
        rb = amCache.getResBundle(ResBundleUtils.rbName, loc);
    }

    /**
     * Returns a set of all valid referral type names defined by the policy
     * service.
     * Examples are <code>PeerOrgReferral</code>, <code>SubOrgReferral</code>
     *
     * @return a set of all valid referral type names defined by the policy
     *         service.
     * @throws SSOException if the <code>SSOToken</code> used to create 
     *                      the <code>PolicyManager</code> has become invalid
     * @throws PolicyException for any other abnormal condition
     */
    public Set getReferralTypeNames() throws SSOException,
            PolicyException {
        return (PolicyManager.getPluginSchemaNames(REFERRAL));
    }

    /**
     * Returns a set of valid referral type names configured for the
     * organization.
     * Examples are <code>PeerOrgReferral</code>, <code>SubOrgReferral</code>
     *
     * @return a set of valid referral type names configured for the
     *         organization.
     * @throws SSOException if the <code>SSOToken</code> used to create 
     *                      the <code>PolicyManager</code> has become invalid
     * @throws PolicyException for any other abnormal condition
     */
    public Set getSelectedReferralTypeNames() throws SSOException,
            PolicyException {
        Map policyConfig = pm.getPolicyConfig();
        Set selectedReferrals = null;
        if (policyConfig != null) {
            selectedReferrals = 
                    (Set)policyConfig.get(PolicyConfig.SELECTED_REFERRALS); 
        }
        if (selectedReferrals == null) {
            selectedReferrals = Collections.EMPTY_SET;
        }
        return selectedReferrals;
    }

    /**
     * Returns the type of the <code>Referral</code> implementation.
     * For example, <code>PeerOrgReferral</code>, <code>SubOrgReferral</code>
     *
     * @param referral referral object for which this method will
     * return its associated type
     *
     * @return type of the referral, e.g., <code>PeerOrgReferral</code>,
     * <code>SubOrgReferral</code> Returns <code>null</code> if not present.
     */
    public String getReferralTypeName(Referral referral) {
        return (referralTypeName(referral));
    }

    /**
     * Returns the I18N properties file name that should be
     * used to localize display names for the given
     * referral type.
     *
     * @param referralType referral type name
     *
     * @return i18n properties file name
     */
    protected String getI18NPropertiesFileName(String referralType) {
        // %%% Need to get the file name from plugin schema
        return (null);
    }

    /**
     * Returns the I18N key to be used to localize the
     * display name for the referral type name.
     *
     * @param referralType referral type name
     *
     * @return i18n key to obtain the display name
     */
    public String getI18NKey(String referralType) {
        PluginSchema ps = PolicyManager.getPluginSchema(REFERRAL, referralType);
        if (ps != null) {
            return (ps.getI18NKey());
        }
        return (null);
    }


    /**
     * Gets the display name for the referral type
     * @param referralType referral type
     * @return display name for the referral type
     */
    public String getDisplayName(String referralType) {
	String displayName = null;
	String i18nKey = getI18NKey(referralType);
	if (i18nKey == null || i18nKey.length()==0 ) {
	    displayName = referralType;
	} else {
	    displayName = Locale.getString(rb,i18nKey,debug);
	}
	return displayName;
    }

    /**
     * Returns an instance of the <code>Referral</code>
     * given the referral type name.
     *
     * @param referralType type of referral.
     * @return an instance of the <code>Referral</code> given the referral type
     *         name.
     * @throws NameNotFoundException if the <code>Referral</code> for the
     *            <code>referralType</code> name is not found
     * @throws PolicyException for any other abnormal condition
     */
    public Referral getReferral(String referralType)
        throws NameNotFoundException, PolicyException {
        PluginSchema ps = PolicyManager.getPluginSchema(REFERRAL, referralType);
        if (ps == null) {
            throw (new NameNotFoundException(ResBundleUtils.rbName,
		"invalid_referral", null,
                referralType, PolicyException.USER_COLLECTION));
        }

        // Construct the object
        Referral answer = null;
        try {
            String className = ps.getClassName();
            answer = (Referral) Class.forName(className).newInstance();
        } catch (Exception e) {
            throw (new PolicyException(e));
        }

        // Construct the Referral and return
        Map policyConfig = pm.getPolicyConfig();
        answer.initialize(policyConfig);
        return (answer);
    }


    /**
     * Returns the type name for the <code>Referral</code>
     * @param referral <code>Referral</code> for which to find the type
     * @return the type name for the <code>Referral</code>
     */
    static String referralTypeName(Referral referral) {
        if (referral == null) {
            return (null);
        }
        String answer = null;
        String className = referral.getClass().getName();
        Iterator items =
            PolicyManager.getPluginSchemaNames(REFERRAL).iterator();
        while (items.hasNext()) {
            String pluginName = (String) items.next();
            PluginSchema ps = PolicyManager.getPluginSchema(
                REFERRAL, pluginName);
            if (className.equals(ps.getClassName())) {
                answer = pluginName;
                break;
            }
        }
        return (answer);
    }

    /**
     * Gets the view bean URL given the Referral
     *
     * @param referral referral for which to get the view bean URL
     *
     * @return view bean URL defined for the referral plugin in the policy
     *         service <code>PluginSchema</code>
     */
    public String getViewBeanURL(Referral referral) {
	return PolicyManager.getViewBeanURL(
            REFERRAL, referral.getClass().getName());
    }

}
