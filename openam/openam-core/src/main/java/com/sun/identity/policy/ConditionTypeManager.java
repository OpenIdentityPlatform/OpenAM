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
 * $Id: ConditionTypeManager.java,v 1.3 2008/06/25 05:43:43 qcheng Exp $
 *
 */



package com.sun.identity.policy;

import java.util.*;

import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOException;
import com.sun.identity.sm.*;
import com.sun.identity.policy.interfaces.Condition;
import com.sun.identity.shared.locale.AMResourceBundleCache;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.locale.Locale;

/**
 * The class <code>ConditionTypeManager</code> provides
 * methods to get a list of configured <code>Condition
 * </code> objects, and to obtain a factory object for it.
 *
 * @supported.all.api
 */
public class ConditionTypeManager {

    private static String CONDITION = "Condition";

    private SSOToken token;
    private PolicyManager pm;

    private ResourceBundle rb;
    private static AMResourceBundleCache amCache = 
        AMResourceBundleCache.getInstance();

    static Debug debug = PolicyManager.debug;

    /**
     * Constructor with no argument, initializes
     * required data.
     * @exception SSOException if unable to retrieve locale from 
     *            <code>SSOToken</code> obtained from 
     *            <code>ServiceTypeManager</code>
     */
    ConditionTypeManager() throws SSOException {
        token = ServiceTypeManager.getSSOToken();
        String lstr = token.getProperty("Locale");
        java.util.Locale loc = 
            com.sun.identity.shared.locale.Locale.getLocale(lstr);
        rb = amCache.getResBundle(ResBundleUtils.rbName, loc);
    }

    /**
     * Constructor,  initializes required data.
     * @param pm <code>PolicyManager</code> to be used to get token from.
     *        If unable to retrieve locale from <code>PolicyManager</code>'s
     *        <code>SSOToken</code> defaults to default locale in <code>
     *        am.util.Locale</code>
     */
    ConditionTypeManager(PolicyManager pm) {
        this.pm = pm;
        token = pm.token;
        java.util.Locale loc ;
        try {
            String lstr = token.getProperty("Locale");
            loc = com.sun.identity.shared.locale.Locale.getLocale(lstr);
        } catch (SSOException ex) {
            debug.error ("ConditionTypeManager:Unable to retreive locale from"
                +"SSOToken", ex);
            loc = Locale.getDefaultLocale();
        }

         if (debug.messageEnabled()) {
            debug.message("SubjectManager locale="+loc+"\tI18nFileName = "+
                     ResBundleUtils.rbName);
        }
        rb = amCache.getResBundle(ResBundleUtils.rbName, loc);
    }

    /**
     * Returns a <code>Set</code> of all valid condition type names defined 
     * by the policy service. Examples are <code>AuthLevelCondition</code>,
     * <code>IPCondition</code>.
     *
     * @return a <code>Set</code> of all valid condition type names defined 
     *            by the policy service.
     * @exception SSOException if the <code>SSOToken</code> used to create 
     *            the <code>PolicyManager</code> has become invalid
     * @exception PolicyException for any other abnormal condition
     */
    public Set getConditionTypeNames() throws SSOException,
        PolicyException {
        return (PolicyManager.getPluginSchemaNames(CONDITION));
    }

    /**
     * Returns a <code>Set</code> of valid condition type names configured for 
     * the organization. Examples are <code>AuthLevelCondition</code>, 
     * <code>IPCondition</code>.
     *
     * @return a <code>Set</code> of valid condition type names configured for 
     *            the organization.
     * @exception SSOException if the <code>SSOToken</code> used to create 
     *            the <code>PolicyManager</code> has become invalid
     * @exception PolicyException for any other abnormal condition
     */
    public Set getSelectedConditionTypeNames() throws SSOException,
        PolicyException {
        String org = pm.getOrganizationDN();
        Map map = PolicyConfig.getPolicyConfig(org);
        if (map != null) {
            Object answer = map.get(PolicyConfig.SELECTED_CONDITIONS);
            if (answer != null) {
                return (Set) answer; 
            }
        }
        return Collections.EMPTY_SET;
    }

    /**
     * Returns the type of the <code>Condition</code> implementation.
     * For example <code>TimeCondition</code>, <code>DayTimeCondition</code>,
     * <code>IPCondition</code>.
     *
     * @param condition condition object for which this method will
     *         return its associated type
     *
     * @return type of the condition, e.g. <code>AuthLevelConditon</code>,
     *         <code>IPCondition</code>.  Returns <code>null</code> if not
     *         present.
     */
    public String getConditionTypeName(Condition condition) {
        return (conditionTypeName(condition));
    }

    /**
     * Returns the I18N properties file name that should be
     * used to localize display names for the given
     * condition type.
     *
     * @param conditionType condition type name
     *
     * @return <code>String</code> representing i18n properties file name
     */
    protected String getI18NPropertiesFileName(String conditionType) {
        PolicyManager.getPluginSchema(CONDITION, conditionType);
        // %%% Need to get the file name from plugin schema
        return (null);
    }

    /**
     * Returns the I18N key to be used to localize the
     * display name for the condition type name.
     *
     * @param conditionType condition type name
     *
     * @return <code>String</code> representing i18n key to obtain the display 
     * name
     */
    public String getI18NKey(String conditionType) {
        PluginSchema ps = PolicyManager.getPluginSchema(CONDITION, 
            conditionType);
        if (ps != null) {
            return (ps.getI18NKey());
        }
        return (null);
    }

    /**
     * Gets the display name for the condition type
     * @param conditionType condition type
     * @return display name for the condition type
     */
    public String getDisplayName(String conditionType) {
        String displayName = null;
        String i18nKey = getI18NKey(conditionType);
        if (i18nKey == null || i18nKey.length()==0 ) {
            displayName = conditionType;
        } else {
            displayName = Locale.getString(rb,i18nKey,debug);
        }
        return displayName;
    }

    /**
     * Returns an instance of the <code>Condition</code>
     * given the condition type name.
     *
     * @param conditionType condition type name.
     * @return an instance of the <code>Condition</code>
     *            given the condition type name.
     * @throws NameNotFoundException if the <code>Condition</code> for the
     *            <code>conditionType</code> name is not found
     * @throws PolicyException for any other abnormal condition
     */
    public Condition getCondition(String conditionType)
        throws NameNotFoundException, PolicyException {
        PluginSchema ps = PolicyManager.getPluginSchema(CONDITION, 
            conditionType);
        if (ps == null) {
            throw (new NameNotFoundException(ResBundleUtils.rbName, 
                "invalid_condition", null, conditionType, 
                PolicyException.USER_COLLECTION));
        }

        // Construct the object
        Condition condition = null;
        try {
            String className = ps.getClassName();
            condition = (Condition) Class.forName(className).newInstance();
        } catch (Exception e) {
            throw (new PolicyException(e));
        }
        return (condition);
    }


    /**
     * Returns the type of the <code>Condition</code> implementation.
     * For example <code>TimeCondition</code>, <code>DayTimeCondition</code>,
     * <code>IPCondition</code>.
     *
     * @param condition condition object for which this method will
     *         return its associated type
     *
     * @return type of the condition, e.g. <code>AuthLevelConditon</code>,
     *         <code>IPCondition</code>.  Returns <code>null</code> if not
     *         present.
     */
    static String conditionTypeName(Condition condition) {
        if (condition == null) {
            return (null);
        }
        String name = null;
        String className = condition.getClass().getName();
        Iterator items = PolicyManager.getPluginSchemaNames(CONDITION).
            iterator();
        while (items.hasNext()) {
            String pluginName = (String) items.next();
            PluginSchema ps = PolicyManager.getPluginSchema(CONDITION, 
                pluginName);
            if (className.equals(ps.getClassName())) {
                name = pluginName;
                break;
            }
        }
        return (name);
    }

    /**
     * Gets the view bean URL given the Condition
     *
     * @param condition condition for which to get the view bean URL
     *
     * @return view bean URL defined for the condition plugin in the policy
     *         service <code>PluginSchema</code>.
     */
    public String getViewBeanURL(Condition condition) {
        return PolicyManager.getViewBeanURL(CONDITION, 
            condition.getClass().getName());
    }
}
