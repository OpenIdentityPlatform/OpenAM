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
 * $Id: PolicyModelImpl.java,v 1.6 2009/09/18 00:08:22 veiming Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */

package com.sun.identity.console.policy.model;

import com.iplanet.jato.view.html.OptionList;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.authentication.config.AMAuthenticationInstance;
import com.sun.identity.authentication.config.AMAuthenticationManager;
import com.sun.identity.authentication.config.AMConfigurationException;
import com.sun.identity.authentication.config.AMAuthConfigUtils;
import com.sun.identity.common.DisplayUtils;
import com.sun.identity.console.base.model.AMAdminConstants;
import com.sun.identity.console.base.model.AMAdminUtils;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMDisplayType;
import com.sun.identity.console.base.model.AMFormatUtils;
import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.console.base.model.AMModelBase;
import com.sun.identity.console.base.model.AMResBundleCacher;
import com.sun.identity.console.base.model.QueryResults;
import com.sun.identity.console.property.PolicyPropertyXMLBuilder;
import com.sun.identity.console.property.ResponseProviderXMLBuilder;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.policy.ActionSchema;
import com.sun.identity.policy.ConditionTypeManager;
import com.sun.identity.policy.InvalidFormatException;
import com.sun.identity.policy.InvalidNameException;
import com.sun.identity.policy.NameNotFoundException;
import com.sun.identity.policy.NoPermissionException;
import com.sun.identity.policy.Policy;
import com.sun.identity.policy.PolicyException;
import com.sun.identity.policy.PolicyManager;
import com.sun.identity.policy.ReferralTypeManager;
import com.sun.identity.policy.ResponseProviderTypeManager;
import com.sun.identity.policy.Rule;
import com.sun.identity.policy.ServiceType;
import com.sun.identity.policy.ServiceTypeManager;
import com.sun.identity.policy.SubjectTypeManager;
import com.sun.identity.policy.Syntax;
import com.sun.identity.policy.ValidValues;
import com.sun.identity.policy.interfaces.Condition;
import com.sun.identity.policy.interfaces.Referral;
import com.sun.identity.policy.interfaces.ResponseProvider;
import com.sun.identity.policy.interfaces.Subject;
import com.sun.identity.sm.AttributeSchema;
import com.sun.identity.sm.OrganizationConfigManager;
import com.sun.identity.sm.SMSException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;

/* - LOG COMPLETE - */

public class PolicyModelImpl
    extends AMModelBase
    implements PolicyModel
{
    private ServiceTypeManager svcTypeMgr = null;
    private Map mapSvcTypeNameToActions = null;
    private Map mapSvcTypeNameToResBundle = null;
    private Map mapSvcNameToManagedResource = new HashMap();
    private Set requiredResourceNameService = new HashSet();
    private Set notRequiredResourceNameService = new HashSet();
    private static SSOToken adminSSOToken =
        AMAdminUtils.getSuperAdminSSOToken();

    /**
     * Creates a simple model using default resource bundle. 
     *
     * @param req HTTP Servlet Request
     * @param map of user information
     */
    public PolicyModelImpl(HttpServletRequest req, Map map) {
        super(req, map);
    }

    private PolicyManager getPolicyManager(String realmName)
        throws AMConsoleException {
        if ((realmName == null) || realmName.length() == 0) {
            realmName = getStartDN();
        }

        try {
            return new PolicyManager(getUserSSOToken(), realmName);
        } catch (SSOException e) {
            debug.warning("PolicyModelImpl.getPolicyManager", e);
            throw new AMConsoleException(e);
        } catch (PolicyException e) {
            debug.warning("PolicyModelImpl.getPolicyManager", e);
            throw new AMConsoleException(e);
        }
    }

    private ServiceTypeManager getServiceTypeManager()
        throws AMConsoleException {
        if (svcTypeMgr == null) {
            try {
                svcTypeMgr = new ServiceTypeManager(getUserSSOToken());
            } catch (SSOException ssoe) {
                throw new AMConsoleException(getErrorString(ssoe));
            }
        }
        return svcTypeMgr;
    }

    /**
     * Returns cached policy object.
     *
     * @param cacheID Cache ID.
     * @return cached policy object.
     * @throws AMConsoleException if policy object cannot be located.
     */
    public CachedPolicy getCachedPolicy(String cacheID)
        throws AMConsoleException
    {
        PolicyCache cache = PolicyCache.getInstance();
        return cache.getPolicy(getUserSSOToken(), cacheID);
    }

    /**
     * Caches a policy. Returns the cache ID of the policy object.
     *
     * @param policyName Name of policy.
     * @param description Description of policy.
     * @param isReferral <code>true</code> if policy is referral typed.
     * @param isActive <code>true</code> if policy is active.
     * @return cache ID of the policy object.
     * @throws AMConsoleException if policy cannot be cached.
     */
    public String cachePolicy(
        String policyName,
        String description,
        boolean isReferral,
        boolean isActive
    ) throws AMConsoleException {
        try {
            Policy policy = new Policy(
                policyName, description, isReferral, isActive);
            PolicyCache cache = PolicyCache.getInstance();
            return cache.cachePolicy(
                getUserSSOToken(), new CachedPolicy(policy));
        } catch (InvalidNameException e) {
            throw new AMConsoleException(getErrorString(e));
        }
    }

    /**
     * Caches an existing policy. Returns the cache ID of the policy object.
     *
     * @param realmName Name of realm.
     * @param policyName Name of policy.
     * @return cache ID of the policy object.
     * @throws AMConsoleException if policy cannot be cached.
     */
    public String cachePolicy(String realmName, String policyName) 
        throws AMConsoleException {
        try {
            PolicyManager policyManager = getPolicyManager(realmName);
            Policy policy = policyManager.getPolicy(policyName);
            PolicyCache cache = PolicyCache.getInstance();
            return cache.cachePolicy(
                getUserSSOToken(), new CachedPolicy(policy));
        } catch (InvalidFormatException e) {
            throw new AMConsoleException(getErrorString(e));
        } catch (InvalidNameException e) {
            throw new AMConsoleException(getErrorString(e));
        } catch (NoPermissionException e) {
            throw new AMConsoleException(getErrorString(e));
        } catch (NameNotFoundException e) {
            throw new AMConsoleException(getErrorString(e));
        } catch (PolicyException e) {
            throw new AMConsoleException(getErrorString(e));
        } catch (SSOException e) {
            throw new AMConsoleException(getErrorString(e));
        }
    }


    /**
     * Returns policy names that are under a realm.
     *
     * @param realmName Name of realm.
     * @param filter Filter string.
     * @return policy names that are under a realm.
     * @throws AMConsoleException if policy names cannot be returned.
     */
    public Set getPolicyNames(String realmName, String filter)
        throws AMConsoleException {
        Set names = null;

        try {
            String[] param = {realmName};
            logEvent("ATTEMPT_GET_POLICY_NAMES", param);
            PolicyManager policyManager = getPolicyManager(realmName);
            names = policyManager.getPolicyNames(filter);
            logEvent("SUCCEED_GET_POLICY_NAMES", param);
        } catch (SSOException e) {
            String strError = getErrorString(e);
            String[] params = {realmName, strError};
            logEvent("SSO_EXCEPTION_GET_POLICY_NAMES", params);
            throw new AMConsoleException(strError);
        } catch (PolicyException e) {
            String strError = getErrorString(e);
            String[] params = {realmName, strError};
            logEvent("POLICY_EXCEPTION_GET_POLICY_NAMES", params);
            throw new AMConsoleException(strError);
        }

        return (names != null) ? names : Collections.EMPTY_SET;
    }

    /**
     * Creates a policy.
     *
     * @param realmName Name of realm.
     * @param policy Policy object.
     * @throws AMConsoleException if policy cannot be created.
     */
    public void createPolicy(String realmName, Policy policy)
        throws AMConsoleException {
        try {
            String[] params = {realmName, policy.getName()};
            logEvent("ATTEMPT_CREATE_POLICY", params);
            PolicyManager policyManager = getPolicyManager(realmName);
            policyManager.addPolicy(policy);
            logEvent("SUCCEED_CREATE_POLICY", params);
        } catch (PolicyException e) {
            String strError = getErrorString(e);
            String[] params = {realmName, policy.getName(), strError};
            logEvent("POLICY_EXCEPTION_CREATE_POLICY", params);
            throw new AMConsoleException(getErrorString(e));
        } catch (SSOException e) {
            String strError = getErrorString(e);
            String[] params = {realmName, policy.getName(), strError};
            logEvent("SSO_EXCEPTION_CREATE_POLICY", params);
            throw new AMConsoleException(getErrorString(e));
        }
    }

    /**
     * Modifies a policy.
     *
     * @param realmName Name of realm.
     * @param policy Policy object.
     * @throws AMConsoleException if policy cannot be created.
     */
    public void replacePolicy(String realmName, Policy policy)
        throws AMConsoleException {
        try {
            String[] params = {realmName, policy.getName()};
            logEvent("ATTEMPT_MODIFY_POLICY", params);
            PolicyManager policyManager = getPolicyManager(realmName);
            policyManager.replacePolicy(policy);
            logEvent("SUCCEED_MODIFY_POLICY", params);
        } catch (PolicyException e) {
            String strError = getErrorString(e);
            String[] params = {realmName, policy.getName(), strError};
            logEvent("POLICY_EXCEPTION_MODIFY_POLICY", params);
            throw new AMConsoleException(getErrorString(e));
        } catch (SSOException e) {
            String strError = getErrorString(e);
            String[] params = {realmName, policy.getName(), strError};
            logEvent("SSO_EXCEPTION_MODIFY_POLICY", params);
            throw new AMConsoleException(getErrorString(e));
        }
    }

    /**
     * Deletes policies.
     *
     * @param realmName Name of realm that contains these policies.
     * @param names Set of policy names to be deleted.
     * @throws AMConsoleException if policies cannot be deleted.
     */
    public void deletePolicies(String realmName, Set names)
        throws AMConsoleException {
        PolicyManager policyManager = getPolicyManager(realmName);
        List unableToDelete = new ArrayList(names.size());
        String[] params = new String[2];
        params[0] = realmName;
        String[] paramsEx = new String[3];
        paramsEx[0] = realmName;

        for (Iterator iter = names.iterator(); iter.hasNext(); ) {
            String name = (String)iter.next();
            params[1] = name;

            try {
                logEvent("ATTEMPT_DELETE_POLICY", params);
                policyManager.removePolicy(name);
                logEvent("SUCCEED_DELETE_POLICY", params);
            } catch (PolicyException e) {
                paramsEx[1] = name;
                paramsEx[2] = getErrorString(e);
                logEvent("POLICY_EXCEPTION_DELETE_POLICY", params);
                debug.warning("PolicyModelImpl.deletePolicies", e);
                unableToDelete.add(name);
            } catch (SSOException e) {
                paramsEx[1] = name;
                paramsEx[2] = getErrorString(e);
                logEvent("SSO_EXCEPTION_DELETE_POLICY", params);
                debug.warning("PolicyModelImpl.deletePolicies", e);
                unableToDelete.add(name);
            }
        }

        if (!unableToDelete.isEmpty()) {
            Object[] p = (Object[])unableToDelete.toArray();
            String msg = MessageFormat.format(
                "policy.message.unableToDeletePolicies", p);
            throw new AMConsoleException(msg);
        }
    }

    /**
     * Returns true if rule can be created in a policy.
     *
     * @param policy Policy object.
     * @param realmName Realm Name.
     * @return true if rule can be created in a policy.
     */
    public boolean canCreateRule(Policy policy, String realmName) {
        getSvcTypeNameToActionsMap(policy, realmName);
        return !requiredResourceNameService.isEmpty() ||
            !notRequiredResourceNameService.isEmpty();
    }

    /**
     * Returns all registered service type names. Map of service name to its
     * localized name.
     *
     * @return all registered service type names.
     */
    public Map getServiceTypeNames() {
        Map map = null;

        try {
            Set types = getServiceTypeManager().getServiceTypeNames();

            if ((types != null) && !types.isEmpty()) {
                map = new HashMap(types.size() *2);

                for (Iterator iter = types.iterator(); iter.hasNext(); ) {
                    String name = (String)iter.next();
                    String lname = getLocalizedServiceName(name, null);
                    if (lname != null) {
                        map.put(name, lname);
                    }
                }
            }
        } catch (SSOException e) {
            debug.warning("PolicyModelImpl.getServiceTypeNames", e);
        } catch (NoPermissionException e) {
            debug.warning("PolicyModelImpl.getServiceTypeNames", e);
        } catch (AMConsoleException e) {
            debug.warning("PolicyModelImpl.getServiceTypeNames", e);
        }

        return (map == null) ? Collections.EMPTY_MAP : map;
    }

    /**
     * Returns action schemas of service type.
     * name.
     *
     * @param policy Policy object.
     * @param realmName Realm Name.
     * @param name Name of Service Type.
     * @param withResourceName <code>true</code> for action names for resource
     *        name.
     * @return action schemas of service type.
     */
    public Set getActionSchemas(
        Policy policy,
        String realmName,
        String name,
        boolean withResourceName
    ) {
        Set actions = null;
        Map map = getSvcTypeNameToActionsMap(policy, realmName);
        Set actionSchemas = (Set)map.get(name);

        if ((actionSchemas != null) && !actionSchemas.isEmpty()) {
            actions = new HashSet(actionSchemas.size() *2);

            for (Iterator iter = actionSchemas.iterator(); iter.hasNext(); ) {
                ActionSchema as = (ActionSchema)iter.next();

                if (withResourceName) {
                    if (as.requiresResourceName()) {
                        actions.add(as);
                    }
                } else if (!as.requiresResourceName()) {
                    actions.add(as);
                }
            }
        }

        return (actions != null) ? actions : Collections.EMPTY_SET;
    }

    /**
     * Returns localized name of action schema.
     *
     * @param name Name of Service Type.
     * @param actionSchema Action Schema.
     * @return localized name of action schema.
     */
    public String getActionSchemaLocalizedName(
        String name,
        ActionSchema actionSchema
    ) {
        ResourceBundle rb = (ResourceBundle)mapSvcTypeNameToResBundle.get(
            name);
        //i18nKey should not be null or empty because we have pre-scan them.
        return com.sun.identity.shared.locale.Locale.getString(
            rb, actionSchema.getI18NKey(), debug);
    }

    /**
     * Returns a option list of possible choices.
     *
     * @param name Name of Service Type.
     * @param actionSchema Action Schema.
     * @return a option list of possible choices.
     */
    public OptionList getChoiceValues(String name, ActionSchema actionSchema) {
        ResourceBundle rb = (ResourceBundle)mapSvcTypeNameToResBundle.get(
            name);
        OptionList optList = null;
        AttributeSchema.Syntax syntax = actionSchema.getSyntax();
        AttributeSchema.UIType uiType = actionSchema.getUIType();

        if ((uiType != null) && (uiType == AttributeSchema.UIType.RADIO)) {
            if (syntax == AttributeSchema.Syntax.BOOLEAN) {
                optList = getAttrRadioBooleanChoiceValue(actionSchema, rb);
            } else {
                optList = getAttrChoiceValue(actionSchema, rb);
            }
        } else {
            optList = getAttrChoiceValue(actionSchema, rb);
        }

        return optList;
    }

    /**
     * Returns true if service type requires resource name.
     *
     * @param policy Policy object.
     * @param realmName Realm Name.
     * @param name Name of Service Type.
     * @return true if service type requires resource name.
     */
    public boolean requiredResourceName(
        Policy policy,
        String realmName,
        String name
    ) {
        getSvcTypeNameToActionsMap(policy, realmName);
        return requiredResourceNameService.contains(name);
    }

    /**
     * Returns true if service type does not require resource name.
     *
     * @param policy Policy object.
     * @param realmName Realm Name.
     * @param name Name of Service Type.
     * @return true if service typedoes not  require resource name.
     */
    public boolean notRequiredResourceName(
        Policy policy,
        String realmName,
        String name
    ) {
        getSvcTypeNameToActionsMap(policy, realmName);
        return notRequiredResourceNameService.contains(name);
    }

    private OptionList getAttrRadioBooleanChoiceValue(
        ActionSchema actionSchema,
        ResourceBundle rb
    ) {
        OptionList optionList = new OptionList();
        String trueValue = actionSchema.getTrueValue();
        String falseValue = actionSchema.getFalseValue();
        if (trueValue == null) {
            trueValue = "true";
        }
        if (falseValue == null) {
            falseValue = "false";
        }

        String trueI18nKey = actionSchema.getTrueValueI18NKey();
        String falseI18nKey = actionSchema.getFalseValueI18NKey();
                                                                                
        if (trueI18nKey != null) {
            String label = com.sun.identity.shared.locale.Locale.getString(
                rb, trueI18nKey, debug);
            if ((label == null) || (label.length() == 0)) {
                optionList.add(trueValue, trueValue);
            } else {
                optionList.add(label, trueValue);
            }
        } else {
            optionList.add(trueValue, trueValue);
        }

        if (falseI18nKey != null) {
            String label = com.sun.identity.shared.locale.Locale.getString(
                rb, falseI18nKey, debug);
            if ((label == null) || (label.length() == 0)) {
                optionList.add(falseValue, falseValue);
            } else {
                optionList.add(label, falseValue);
            }
        } else {
            optionList.add(falseValue, falseValue);
        }

        return optionList;
    }

    private OptionList getAttrChoiceValue(
        ActionSchema actionSchema,
        ResourceBundle rb
    ) {
        OptionList optionList = new OptionList();
        String[] choices = actionSchema.getChoiceValues();

        for (int i = 0; i < choices.length; i++) {
            String choice = choices[i];
            String i18nKey = actionSchema.getChoiceValueI18NKey(choice);
            String lname = com.sun.identity.shared.locale.Locale.getString(
                rb, i18nKey, debug);

            if ((lname == null) || (lname.length() == 0)) {
                lname = i18nKey;
            }

            optionList.add(lname, choice);
        }

        return optionList;
    }

    private Map getSvcTypeNameToActionsMap(Policy policy, String realmName) {
        if (mapSvcTypeNameToActions == null) {
            try {
                Set serviceTypeNames = getServiceTypeNames().keySet();
                int sz = serviceTypeNames.size();
                mapSvcTypeNameToActions = new HashMap(sz *2);
                mapSvcTypeNameToResBundle = new HashMap(sz *2);
                ServiceTypeManager mgr = getServiceTypeManager();

                for (Iterator i = serviceTypeNames.iterator(); i.hasNext(); ) {
                    String serviceTypeName = (String)i.next();
                    ServiceType serviceType = mgr.getServiceType(
                        serviceTypeName);

                    if (serviceType != null) {
                        ResourceBundle rb = getResourceBundle(
                            serviceType, getUserLocale());

                        if (rb != null) {
                            mapSvcTypeNameToResBundle.put(serviceTypeName, rb);
                            Set as = getActionSchemas(serviceType);
                            filterActionSchemaWithI18nKey(as);

                            if ((as != null) && !as.isEmpty()) {
                                mapSvcTypeNameToActions.put(
                                    serviceTypeName, as);

                                if (requiresResourceName(policy, realmName,
                                    serviceTypeName, as, true)
                                ) {
                                    requiredResourceNameService.add(
                                        serviceTypeName);
                                }

                                if (requiresResourceName(policy, realmName,
                                    serviceTypeName, as, false)
                                ) {
                                    notRequiredResourceNameService.add(
                                        serviceTypeName);
                                }
                            }
                        }
                    }
                }
            } catch (AMConsoleException e) {
                debug.warning("PolicyModelImppl.getSvcTypeNameToActionsMap", e);
            } catch (SSOException e) {
                debug.warning("PolicyModelImppl.getSvcTypeNameToActionsMap", e);
            } catch (NameNotFoundException e) {
                debug.warning("PolicyModelImppl.getSvcTypeNameToActionsMap", e);
            }
        }

        return mapSvcTypeNameToActions;
    }

    private boolean requiresResourceName(
        Policy policy,
        String realmName,
        String serviceTypeName,
        Set actionSchemas,
        boolean required
    ) {
        if ((realmName == null) || (realmName.trim().length() == 0)) {
            realmName = getStartDN();
        }

        boolean yes = false;
        for (Iterator iter = actionSchemas.iterator(); iter.hasNext() && !yes;){
            ActionSchema as = (ActionSchema)iter.next();
            yes = (as.requiresResourceName() == required);
        }

        if (required) {
            if (yes) {
                yes = canCreateNewResource(realmName, serviceTypeName) ||
                    !getManagedResources(realmName, serviceTypeName).isEmpty();
            }
        } else {
            if (yes) {
                Set ruleWithoutRes = getRuleNamesWithoutRes(
                    policy, serviceTypeName);

                /* cannot have more than one rule for service without resource
                   name */
                if (ruleWithoutRes.isEmpty()) {
                    yes = realmName.equals("/") || !getManagedResources(
                        realmName, serviceTypeName).isEmpty();
                } else {
                    yes = false;
                }
            }
        }

        return yes;
    }

    private Set getRuleNamesWithoutRes(Policy policy, String serviceTypeName) {
        Set rules = getRules(policy);
        Set selected = new HashSet(rules.size() *2);

        for (Iterator iter = rules.iterator(); iter.hasNext(); ) {
            Rule rule = (Rule)iter.next();
            if (rule.getServiceTypeName().equals(serviceTypeName)){
                String res = rule.getResourceName();
                if (res == null) {
                    selected.add(rule.getName());
                }
            }
        }

        return selected;
    }

    private Set getRules(Policy policy) {
        Set rules = null;
        Set ruleNames = policy.getRuleNames();
        if ((ruleNames != null) && !ruleNames.isEmpty()) {
            rules = new HashSet(ruleNames.size() *2);

            for (Iterator iter = ruleNames.iterator(); iter.hasNext(); ) {
                String name = (String) iter.next();

                try {
                    rules.add(policy.getRule(name));
                 } catch (NameNotFoundException e) {
                    debug.warning("PolicyModelImpl.getRules", e);
                }
            }
        }
        return (rules != null) ? rules : Collections.EMPTY_SET;
    }

    /**
     * Returns true of new resource can be created under a realm of a given
     * service type.
     *
     * @param realmName Name of Realm.
     * @param svcTypeName Name of Service Type.
     * @return true of new resource can be created under a realm of a given
     *         service type.
     */
    public boolean canCreateNewResource(String realmName, String svcTypeName) {
        boolean can = false;
        try {
            PolicyManager mgr = getPolicyManager(realmName);
            can = mgr.canCreateNewResource(svcTypeName);
        } catch (AMConsoleException e) {
            debug.warning("PolicyModelImpl.canCreateNewResource", e);
        }

        return can;
    }

    /**
     * Returns a list of managed resource names.
     *
     * @param realmName Name of realm.
     * @param serviceTypeName Name of service type.
     * @return a list of managed resource names.
     */
    public List getManagedResources(String realmName, String serviceTypeName) {
        List managedResources = (List)mapSvcNameToManagedResource.get(
            serviceTypeName);

        if (managedResources == null) {
            managedResources = Collections.EMPTY_LIST;
            try {
                PolicyManager mgr = getPolicyManager(realmName);
                if (mgr != null) {
                    Set resources = mgr.getManagedResourceNames(
                        serviceTypeName);
                    if ((resources != null) && !resources.isEmpty()) {
                        managedResources = AMFormatUtils.sortItems(
                            resources, getUserLocale());
                    }
                }
            } catch (PolicyException e) {
                debug.warning("PolicyModelImpl.getManagedResources", e);
            } catch (AMConsoleException e) {
                debug.warning("PolicyModelImpl.getManagedResources", e);
            } 
            mapSvcNameToManagedResource.put(serviceTypeName, managedResources);
        }

        return managedResources;
    }

    private void filterActionSchemaWithI18nKey(Set actionSchemas) {
        if ((actionSchemas != null) && !actionSchemas.isEmpty()) {
            for (Iterator iter = actionSchemas.iterator(); iter.hasNext(); ) {
                ActionSchema as = (ActionSchema)iter.next();
                String i18nKey = as.getI18NKey();

                if ((i18nKey == null) || (i18nKey.trim().length() == 0)) {
                    iter.remove();
                }
            }
        }
    }

    private ResourceBundle getResourceBundle(ServiceType type, Locale locale) {
        String fileName = type.getI18NPropertiesFileName();
        return ((fileName != null) && (fileName.length() > 0)) ?
            AMResBundleCacher.getBundle(fileName, locale) : null;
    }

    private Set getActionSchemas(ServiceType svcType) {
        Set actionSchemas = null;
        Set actionNames = svcType.getActionNames();
                                                                                
        if ((actionNames != null) && !actionNames.isEmpty()) {
            actionSchemas = new HashSet(actionNames.size() *2);

            for (Iterator iter = actionNames.iterator(); iter.hasNext(); ) {
                String name = (String)iter.next();
                ActionSchema as = getActionSchema(svcType, name);
                if ((as != null) && isActionSchemaSupported(as)) {
                    actionSchemas.add(as);
                }
            }
        }

        return actionSchemas;
    }

    private ActionSchema getActionSchema(ServiceType svcType, String name) {
        ActionSchema schema = null;
        try {
            schema = svcType.getActionSchema(name);
        } catch (InvalidNameException e) {
            debug.warning("PolicyModelImpl.getActionSchema", e);
        }
        return schema;
    }

    private boolean isActionSchemaSupported(ActionSchema actionSchema) {
        int attrSyntax = AMDisplayType.getDisplaySyntax(actionSchema);
        return ((attrSyntax != AMDisplayType.SYNTAX_LINK) &&
            (attrSyntax != AMDisplayType.SYNTAX_BUTTON));
    }

    /**
     * Returns a map of active referral types for a realm to its display name.
     *
     * @param realmName Name of Realm.
     * @return a map of active referral types for a realm to its display name.
     */
    public Map getActiveReferralTypes(String realmName) {
        Map referralTypes = null;

        try {
            PolicyManager policyMgr = getPolicyManager(realmName);

            if (policyMgr != null) {
                ReferralTypeManager referralTypeMgr =
                    policyMgr.getReferralTypeManager();

                if (referralTypeMgr != null) {
                    Set types = referralTypeMgr.getSelectedReferralTypeNames();
                    referralTypes = new HashMap(types.size() *2);

                    for (Iterator iter = types.iterator(); iter.hasNext(); ){
                        String rName = (String)iter.next();
                        Referral referral = referralTypeMgr.getReferral(rName);

                        if (referral != null) {
                            Syntax syntax = referral.getValueSyntax(
                                getUserSSOToken());
                            if (!syntax.equals(Syntax.NONE)) {
                                referralTypes.put(rName,
                                    referralTypeMgr.getDisplayName(rName));
                            }
                        }
                    }
                }
            }
        } catch (AMConsoleException e) {
            debug.warning("PolicyModelImpl.getActiveReferralTypes", e);
        } catch (SSOException e) {
            debug.warning("PolicyModelImpl.getActiveReferralTypes", e);
        } catch (NameNotFoundException e) {
            debug.warning("PolicyModelImpl.getActiveReferralTypes", e);
        } catch (PolicyException e) {
            debug.warning("PolicyModelImpl.getActiveReferralTypes", e);
        }

        return (referralTypes == null) ? Collections.EMPTY_MAP : referralTypes;
    }

    /**
     * Returns syntax for a referral.
     *
     * @param realmName Name of Realm.
     * @param referralType Name of referral type.
     * @return syntax for a referral.
     */
    public Syntax getReferralSyntax(String realmName, String referralType) {
        Syntax syntax = Syntax.NONE;

        try {
            PolicyManager policyMgr = getPolicyManager(realmName);
            if (policyMgr != null) {
                ReferralTypeManager referralTypeMgr =
                    policyMgr.getReferralTypeManager();
                Referral referral = referralTypeMgr.getReferral(referralType);
                syntax = referral.getValueSyntax(getUserSSOToken());
            }
        } catch (SSOException e) {
            debug.warning("PolicyModelImpl.getActiveReferralTypes", e);
        } catch (NameNotFoundException e) {
            debug.warning("PolicyModelImpl.getActiveReferralTypes", e);
        } catch (PolicyException e) {
            debug.warning("PolicyModelImpl.getActiveReferralTypes", e);
        } catch (AMConsoleException e) {
            debug.warning("PolicyModelImpl.getReferralActionSchema", e);
        }

        return syntax;
    }

    /**
     * Returns a referral object.
     *
     * @param realmName Name of Realm.
     * @param referralType Name of referral type.
     * @param values Values of the referral.
     * @return referral obejct.
     * @throws AMConsoleException if referral cannot be created.
     */
    public Referral createReferral(
        String realmName,
        String referralType,
        Set values
    ) throws AMConsoleException {
        Referral referral = null;

        try {
            PolicyManager policyMgr = getPolicyManager(realmName);
            if (policyMgr != null) {
                ReferralTypeManager referralTypeMgr =
                    policyMgr.getReferralTypeManager();
                referral = referralTypeMgr.getReferral(referralType);
                referral.setValues(values);
            }
        } catch (NameNotFoundException e) {
            throw new AMConsoleException(getErrorString(e));
        } catch (PolicyException e) {
            throw new AMConsoleException(getErrorString(e));
        }
        return referral;
    }

    /**
     * Returns a set of possible values for a referral type.
     *
     * @param realmName Name of Realm.
     * @param referralType Name of Referral Type.
     * @param filter wildcards for filtering the results.
     * @return a set of possible values for a referral type.
     */
    public ValidValues getReferralPossibleValues(
        String realmName,
        String referralType,
        String filter
    ) {
        ValidValues values = null;
        try {
            PolicyManager policyMgr = getPolicyManager(realmName);
            if (policyMgr != null) {
                ReferralTypeManager referralTypeMgr =
                    policyMgr.getReferralTypeManager();
                Referral referral = referralTypeMgr.getReferral(referralType);
                values = referral.getValidValues(getUserSSOToken(), filter);
            }
        } catch (AMConsoleException e) {
            debug.warning("PolicyModelImpl.getReferralPossibleValues", e);
        } catch (NameNotFoundException e) {
            debug.warning("PolicyModelImpl.getReferralPossibleValues", e);
        } catch (SSOException e) {
            debug.warning("PolicyModelImpl.getReferralPossibleValues", e);
        } catch (PolicyException e) {
            debug.warning("PolicyModelImpl.getReferralPossibleValues", e);
        }

        return values;
    }

    /**
     * Returns properties view bean URL of a referral.
     *
     * @param realmName Name of realm.
     * @param referralTypeName Name of Referral Type.
     * @return properties view bean URL of a referral.
     */
    public String getReferralViewBeanURL(
        String realmName,
        String referralTypeName
    ) {
        String url = null;

        try {
            PolicyManager policyMgr = getPolicyManager(realmName);
            if (policyMgr != null) {
                ReferralTypeManager referralTypeMgr =
                    policyMgr.getReferralTypeManager();
                Referral referral = referralTypeMgr.getReferral(
                    referralTypeName);
                url = referralTypeMgr.getViewBeanURL(referral);
            }
        } catch (AMConsoleException e) {
            debug.warning("PolicyModelImpl.getReferralViewBeanURL", e);
        } catch (NameNotFoundException e) {
            debug.warning("PolicyModelImpl.getReferralViewBeanURL", e);
        } catch (PolicyException e) {
            debug.warning("PolicyModelImpl.getReferralViewBeanURL", e);
        }

        return url;
    }

    /**
     * Returns a map of values to localized label.
     *
     * @param realmName Name of realm.
     * @param referralTypeName Name of referral Type.
     * @param values Valid values.
     * @return a map of values to localized label.
     */
    public Map getDisplayNameForReferralValues(
        String realmName,
        String referralTypeName,
        Set values
    ) {
        Map map = null;

        if ((values != null) && !values.isEmpty()) {
            map = new HashMap(values.size() *2);
            Locale locale = getUserLocale();

            try {
                PolicyManager policyMgr = getPolicyManager(realmName);
                if (policyMgr != null) {
                    ReferralTypeManager mgr =
                        policyMgr.getReferralTypeManager();
                    Referral referral = mgr.getReferral(referralTypeName);

                    for (Iterator i = values.iterator(); i.hasNext(); ) {
                        String v = (String)i.next();
                        map.put(v, referral.getDisplayNameForValue(v, locale));
                    }
                }
            } catch (AMConsoleException e) {
                debug.warning(
                    "PolicyModelImpl.getDisplayNameForReferralValues", e);
            } catch (NameNotFoundException e) {
                debug.warning(
                    "PolicyModelImpl.getDisplayNameForReferralValues", e);
            } catch (PolicyException e) {
                debug.warning(
                    "PolicyModelImpl.getDisplayNameForReferralValues", e);
            }
        }

        return (map == null) ? Collections.EMPTY_MAP : map;
    }

    /**
     * Returns a map of active subject types for a realm to its display name.
     *
     * @param realmName Name of Realm.
     * @return a map of active subject types for a realm to its display name.
     */
    public QueryResults getActiveSubjectTypes(String realmName) {
        Map subjectTypes = Collections.EMPTY_MAP;
        String strError = null;

        try {
            PolicyManager policyMgr = getPolicyManager(realmName);

            if (policyMgr != null) {
                SubjectTypeManager subjectTypeMgr =
                    policyMgr.getSubjectTypeManager();

                if (subjectTypeMgr != null) {
                    Set types = subjectTypeMgr.getSelectedSubjectTypeNames();
                    subjectTypes = new HashMap(types.size() *2);

                    for (Iterator iter = types.iterator(); iter.hasNext(); ){
                        String rName = (String)iter.next();
                        
                        try {
                            Subject subject = subjectTypeMgr.getSubject(rName);

                            if (subject != null) {
                                Syntax syntax = subject.getValueSyntax(
                                    getUserSSOToken());
                                if (!syntax.equals(Syntax.NONE)) {
                                    subjectTypes.put(rName,
                                        subjectTypeMgr.getDisplayName(rName));
                                }
                            }
                        } catch (SSOException e) {
                            strError = getErrorString(e);
                        } catch (NameNotFoundException e) {
                            strError = getErrorString(e);
                        } catch (PolicyException e) {
                            strError = getErrorString(e);
                        }
                    }
                }
            }
        } catch (AMConsoleException e) {
            debug.error("PolicyModelImpl.getActiveSubjectTypes", e);
        } catch (SSOException e) {
            debug.error("PolicyModelImpl.getActiveSubjectTypes", e);
        } catch (NameNotFoundException e) {
            debug.error("PolicyModelImpl.getActiveSubjectTypes", e);
        } catch (PolicyException e) {
            debug.error("PolicyModelImpl.getActiveSubjectTypes", e);
        }

        return new QueryResults(subjectTypes, strError);
    }

    /**
     * Returns syntax for a subject.
     *
     * @param realmName Name of Realm.
     * @param subjectType Name of Subject type.
     * @return syntax for a subject.
     */
    public Syntax getSubjectSyntax(String realmName, String subjectType) {
        Syntax syntax = Syntax.NONE;

        try {
            PolicyManager policyMgr = getPolicyManager(realmName);
            if (policyMgr != null) {
                SubjectTypeManager subjectTypeMgr =
                    policyMgr.getSubjectTypeManager();
                Subject subject = subjectTypeMgr.getSubject(subjectType);
                syntax = subject.getValueSyntax(getUserSSOToken());
            }
        } catch (SSOException e) {
            debug.warning("PolicyModelImpl.getActiveSubjectTypes", e);
        } catch (NameNotFoundException e) {
            debug.warning("PolicyModelImpl.getActiveSubjectTypes", e);
        } catch (PolicyException e) {
            debug.warning("PolicyModelImpl.getActiveSubjectTypes", e);
        } catch (AMConsoleException e) {
            debug.warning("PolicyModelImpl.getActiveSubjectTypes", e);
        }

        return syntax;
    }

    /**
     * Returns a subject object.
     *
     * @param realmName Name of Realm.
     * @param subjectType Name of subject type.
     * @param values Values of the subject.
     * @return subject object.
     * @throws AMConsoleException if subject cannot be created.
     */
    public Subject createSubject(
        String realmName,
        String subjectType,
        Set values
    ) throws AMConsoleException {
        Subject subject = null;

        try {
            PolicyManager policyMgr = getPolicyManager(realmName);
            if (policyMgr != null) {
                SubjectTypeManager subjectTypeMgr =
                    policyMgr.getSubjectTypeManager();
                subject = subjectTypeMgr.getSubject(subjectType);
                subject.setValues(values);
            }
        } catch (NameNotFoundException e) {
            throw new AMConsoleException(getErrorString(e));
        } catch (PolicyException e) {
            throw new AMConsoleException(getErrorString(e));
        }

        return subject;
    }

    /**
     * Returns a set of possible values for a subject type.
     *
     * @param realmName Name of Realm.
     * @param subjectType Name of Subject Type.
     * @param filter wildcards for filtering the results.
     * @return a set of possible values for a subject type.
     * @throws AMConsoleException if values cannot be obtained.
     */
    public ValidValues getSubjectPossibleValues(
        String realmName,
        String subjectType,
        String filter
    ) throws AMConsoleException
    {
        debug.error("PolicyModelImpl.getSubjectPossibleValues()");
        ValidValues values = null;

        if ((filter == null) || (filter.trim().length() == 0)) {
            filter = "*";
        }

        try {
            PolicyManager policyMgr = getPolicyManager(realmName);
            if (policyMgr != null) {
                SubjectTypeManager subjectTypeMgr =
                    policyMgr.getSubjectTypeManager();
                Subject subject = subjectTypeMgr.getSubject(subjectType);
                values = subject.getValidValues(getUserSSOToken(), filter);
            }
        } catch (AMConsoleException e) {
            debug.warning("PolicyModelImpl.getSubjectPossibleValues", e);
        } catch (NameNotFoundException e) {
            debug.warning("PolicyModelImpl.getSubjectPossibleValues", e);
            throw new AMConsoleException(getErrorString(e));
        } catch (SSOException e) {
            debug.warning("PolicyModelImpl.getSubjectPossibleValues", e);
            throw new AMConsoleException(getErrorString(e));
        } catch (PolicyException e) {
            debug.warning("PolicyModelImpl.getSubjectPossibleValues", e);
            throw new AMConsoleException(getErrorString(e));
        }

        return values;
    }

    /**
     * Returns subject type name of a subject.
     *
     * @param realmName Name of realm.
     * @param subject Subject instance.
     * @return subject type name of a subject.
     */
    public String getSubjectTypeName(String realmName, Subject subject) {
        String typeName = null;

        try {
            PolicyManager policyMgr = getPolicyManager(realmName);
            if (policyMgr != null) {
                SubjectTypeManager subjectTypeMgr =
                    policyMgr.getSubjectTypeManager();
                typeName = subjectTypeMgr.getSubjectTypeName(subject);
            }
        } catch (AMConsoleException e) {
            debug.warning("PolicyModelImpl.getSubjectTypeName", e);
        }

        return typeName;
    }

    /**
     * Returns properties view bean URL of a subject.
     *
     * @param realmName Name of realm.
     * @param subjectTypeName Name of Subject Type.
     * @return properties view bean URL of a subject.
     */
    public String getSubjectViewBeanURL(
        String realmName,
        String subjectTypeName
    ) {
        String url = null;

        try {
            PolicyManager policyMgr = getPolicyManager(realmName);
            if (policyMgr != null) {
                SubjectTypeManager subjectTypeMgr =
                    policyMgr.getSubjectTypeManager();
                Subject subject = subjectTypeMgr.getSubject(subjectTypeName);
                url = subjectTypeMgr.getViewBeanURL(subject);
            }
        } catch (AMConsoleException e) {
            debug.warning("PolicyModelImpl.getSubjectViewBeanURL", e);
        } catch (NameNotFoundException e) {
            debug.warning("PolicyModelImpl.getSubjectViewBeanURL", e);
        } catch (PolicyException e) {
            debug.warning("PolicyModelImpl.getSubjectViewBeanURL", e);
        }

        return url;
    }

    /**
     * Returns properties view bean URL of a subject.
     *
     * @param realmName Name of realm.
     * @param subject Subject instance.
     * @return properties view bean URL of a subject.
     */
    public String getSubjectViewBeanURL(String realmName, Subject subject) {
        String url = null;

        try {
            PolicyManager policyMgr = getPolicyManager(realmName);
            if (policyMgr != null) {
                SubjectTypeManager subjectTypeMgr =
                    policyMgr.getSubjectTypeManager();
                url = subjectTypeMgr.getViewBeanURL(subject);
            }
        } catch (AMConsoleException e) {
            debug.warning("PolicyModelImpl.getSubjectViewBeanURL", e);
        }

        return url;
    }

    /**
     * Returns a map of values to localized label.
     *
     * @param realmName Name of realm.
     * @param subjectTypeName Name of Subject Type.
     * @param values Valid values.
     * @return a map of values to localized label.
     */
    public Map getDisplayNameForSubjectValues(
        String realmName,
        String subjectTypeName,
        Set values
    ) {
        Map map = null;

        if ((values != null) && !values.isEmpty()) {
            map = new HashMap(values.size() *2);
            Locale locale = getUserLocale();

            try {
                PolicyManager policyMgr = getPolicyManager(realmName);
                if (policyMgr != null) {
                    SubjectTypeManager subjectTypeMgr =
                        policyMgr.getSubjectTypeManager();
                    Subject subject = subjectTypeMgr.getSubject(
                        subjectTypeName);

                    for (Iterator i = values.iterator(); i.hasNext(); ) {
                        String v = (String)i.next();
                        map.put(v, subject.getDisplayNameForValue(v, locale));
                    }
                }
            } catch (AMConsoleException e) {
                debug.warning(
                    "PolicyModelImpl.getDisplayNameForSubjectValues", e);
            } catch (NameNotFoundException e) {
                debug.warning(
                    "PolicyModelImpl.getDisplayNameForSubjectValues", e);
            } catch (PolicyException e) {
                debug.warning(
                    "PolicyModelImpl.getDisplayNameForSubjectValues", e);
            }
        }

        return (map == null) ? Collections.EMPTY_MAP : map;
    }

    /**
     * Returns property sheet XML for response provider.
     *
     * @param realmName Name of Realm.
     * @param providerType Name of response provider name.
     * @param bCreate true for create view bean.
     * @param readonly true if administrator can only read permission.
     * @return property sheet XML for response provider.
     */
    public String getResponseProviderXML(
        String realmName,
        String providerType,
        boolean bCreate,
        boolean readonly
    ) {
        String xml = null;

        if (bCreate) {
            xml = "com/sun/identity/console/propertyPMResponseProviderAdd.xml";
        } else {
            xml = (readonly) ?
        "com/sun/identity/console/propertyPMResponseProviderEdit_Readonly.xml" :
                "com/sun/identity/console/propertyPMResponseProviderEdit.xml";
        }

        String prefix = AMAdminUtils.getStringFromInputStream(
            getClass().getClassLoader().getResourceAsStream(xml));
        ResponseProviderXMLBuilder builder = new ResponseProviderXMLBuilder(
            getResponseProviderInstance(realmName, providerType), this);
        if (!bCreate && readonly) {
            builder.setAllAttributeReadOnly(true);
        }
        return builder.getXML(prefix);
    }
 
    /**
     * Returns property names of a response provider.
     *
     * @param realmName Name of Realm.
     * @param providerType Name of response provider name.
     * @return property names of a response provider.
     */
    public List getResponseProviderPropertyNames(
        String realmName,
        String providerType
    ) {
        ResponseProvider provider = getResponseProviderInstance(
            realmName, providerType);
        return (provider != null) ? provider.getPropertyNames() :
            Collections.EMPTY_LIST;
    }
 
    /**
     * Returns a response provider object.
     *
     * @param realmName Name of Realm.
     * @param providerType Name of response provider type.
     * @param values Values of the response provider.
     * @return response provider object.
     * @throws AMConsoleException if response provider cannot be created.
     */
    public ResponseProvider createResponseProvider(
        String realmName,
        String providerType,
        Map values
    ) throws AMConsoleException {
        ResponseProvider provider = null;

        try {
            PolicyManager policyMgr = getPolicyManager(realmName);
            if (policyMgr != null) {
                ResponseProviderTypeManager mgr =
                    policyMgr.getResponseProviderTypeManager();
                provider = mgr.getResponseProvider(providerType);
                provider.setProperties(values);
            }
        } catch (NameNotFoundException e) {
            throw new AMConsoleException(getErrorString(e));
        } catch (PolicyException e) {
            throw new AMConsoleException(getErrorString(e));
        }

        return provider;
    }

    private ResponseProvider getResponseProviderInstance(
        String realmName,
        String typeName
    ) {
        ResponseProvider provider = null;

        try {
            PolicyManager policyMgr = getPolicyManager(realmName);
            if (policyMgr != null) {
                ResponseProviderTypeManager mgr =
                    policyMgr.getResponseProviderTypeManager();
                provider = mgr.getResponseProvider(typeName);
            }
        } catch (AMConsoleException e) {
            debug.warning("PolicyModelImpl.getResponseProviderInstance", e);
        } catch (NameNotFoundException e) {
            debug.warning("PolicyModelImpl.getResponseProviderInstance", e);
        } catch (PolicyException e) {
            debug.warning("PolicyModelImpl.getResponseProviderInstance", e);
        }

        return provider;
    }
 
    /**
     * Returns a map of active response provider types for a realm to its
     * display name.
     *
     * @param realmName Name of Realm.
     * @return a map of active response provider types for a realm to its
     *         display name.
     */
    public Map getActiveResponseProviderTypes(String realmName) {
        Map providerTypes = null;
                                                                                
        try {
            PolicyManager policyMgr = getPolicyManager(realmName);

            if (policyMgr != null) {
                ResponseProviderTypeManager providerTypeMgr =
                    policyMgr.getResponseProviderTypeManager();
                                                                                
                if (providerTypeMgr != null) {
                    Set types =
                        providerTypeMgr.getSelectedResponseProviderTypeNames();
                    providerTypes = new HashMap(types.size() *2);
                                                                                
                    for (Iterator iter = types.iterator(); iter.hasNext(); ){
                        String rName = (String)iter.next();
                        providerTypes.put(rName,
                            providerTypeMgr.getDisplayName(rName));
                    }
                }
            }
        } catch (AMConsoleException e) {
            debug.warning("PolicyModelImpl.getActiveResponseProviderTypes", e);
        } catch (SSOException e) {
            debug.warning("PolicyModelImpl.getActiveResponseProviderTypes", e);
        } catch (NameNotFoundException e) {
            debug.warning("PolicyModelImpl.getActiveResponseProviderTypes", e);
        } catch (PolicyException e) {
            debug.warning("PolicyModelImpl.getActiveResponseProviderTypes", e);
        }

        return (providerTypes == null) ? Collections.EMPTY_MAP : providerTypes;
    }
 
    /**
     * Returns response provider type name of a response provider.
     *
     * @param realmName Name of realm.
     * @param provider response provider instance.
     * @return response provider type name of a response provider.
     */
    public String getResponseProviderTypeName(
        String realmName,
        ResponseProvider provider
    ) {
        String typeName = null;

        try {
            PolicyManager policyMgr = getPolicyManager(realmName);
            if (policyMgr != null) {
                ResponseProviderTypeManager mgr =
                    policyMgr.getResponseProviderTypeManager();
                typeName = mgr.getResponseProviderTypeName(provider);
            }
        } catch (AMConsoleException e) {
            debug.warning("PolicyModelImpl.getResponseProviderTypeName", e);
        }

        return typeName;
    }
 
    /**
     * Returns properties view bean URL of a response provider.
     *
     * @param realmName Name of realm.
     * @param provider response provider Object.
     * @return properties view bean URL of a response provider.
     */
    public String getResponseProviderViewBeanURL(
        String realmName,
        ResponseProvider provider
    ) {
        String url = null;
        try {
            PolicyManager policyMgr = getPolicyManager(realmName);
            if (policyMgr != null) {
                ResponseProviderTypeManager ResponseProviderTypeMgr =
                    policyMgr.getResponseProviderTypeManager();
                url = ResponseProviderTypeMgr.getViewBeanURL(provider);
            }
        } catch (AMConsoleException e) {
            debug.warning("PolicyModelImpl.getResponseProviderViewBeanURL", e);
        }
        return url;
    }
                                                                                
    /**
     * Returns properties view bean URL of a response provider.
     *
     * @param realmName Name of realm.
     * @param typeName Name of response provider Type.
     * @return properties view bean URL of a response provider.
     */
    public String getResponseProviderViewBeanURL(
        String realmName,
        String typeName
    ) {
        String url = null;

        try {
            PolicyManager policyMgr = getPolicyManager(realmName);
            if (policyMgr != null) {
                ResponseProviderTypeManager mgr =
                    policyMgr.getResponseProviderTypeManager();
                ResponseProvider provider = mgr.getResponseProvider(typeName);
                url = mgr.getViewBeanURL(provider);
            }
        } catch (AMConsoleException e) {
            debug.warning("PolicyModelImpl.getResponseProviderViewBeanURL", e);
        } catch (NameNotFoundException e) {
            debug.warning("PolicyModelImpl.getResponseProviderViewBeanURL", e);
        } catch (PolicyException e) {
            debug.warning("PolicyModelImpl.getResponseProviderViewBeanURL", e);
        }

        return url;
    }


    /**
     * Returns a map of active condition types for a realm to its display name.
     *
     * @param realmName Name of Realm.
     * @return a map of active condition types for a realm to its display name.
     */
    public Map getActiveConditionTypes(String realmName) {
        Map condTypes = null;

        try {
            PolicyManager policyMgr = getPolicyManager(realmName);

            if (policyMgr != null) {
                ConditionTypeManager condTypeMgr =
                    policyMgr.getConditionTypeManager();

                if (condTypeMgr != null) {
                    Set types = condTypeMgr.getSelectedConditionTypeNames();
                    condTypes = new HashMap(types.size() *2);

                    for (Iterator iter = types.iterator(); iter.hasNext(); ){
                        String rName = (String)iter.next();
                        condTypes.put(rName, condTypeMgr.getDisplayName(rName));
                    }
                }
            }
        } catch (AMConsoleException e) {
            debug.warning("PolicyModelImpl.getActiveConditionTypes", e);
        } catch (SSOException e) {
            debug.warning("PolicyModelImpl.getActiveConditionTypes", e);
        } catch (NameNotFoundException e) {
            debug.warning("PolicyModelImpl.getActiveConditionTypes", e);
        } catch (PolicyException e) {
            debug.warning("PolicyModelImpl.getActiveConditionTypes", e);
        }

        return (condTypes == null) ? Collections.EMPTY_MAP : condTypes;
    }

    /**
     * Returns properties view bean URL of a condition.
     *
     * @param realmName Name of realm.
     * @param condition Condition Object.
     * @return properties view bean URL of a condition.
     */
    public String getConditionViewBeanURL(
        String realmName,
        Condition condition
    ) {
        String url = null;
        try {
            PolicyManager policyMgr = getPolicyManager(realmName);
            if (policyMgr != null) {
                ConditionTypeManager conditionTypeMgr =
                    policyMgr.getConditionTypeManager();
                url = conditionTypeMgr.getViewBeanURL(condition);
            }
        } catch (AMConsoleException e) {
            debug.warning("PolicyModelImpl.getConditionViewBeanURL", e);
        }
        return url;
    }

    /**
     * Returns properties view bean URL of a condition.
     *
     * @param realmName Name of realm.
     * @param conditionTypeName Name of Condition Type.
     * @return properties view bean URL of a condition.
     */
    public String getConditionViewBeanURL(
        String realmName,
        String conditionTypeName
    ) {
        String url = null;

        try {
            PolicyManager policyMgr = getPolicyManager(realmName);
            if (policyMgr != null) {
                ConditionTypeManager condTypeMgr =
                    policyMgr.getConditionTypeManager();
                Condition condition = condTypeMgr.getCondition(
                    conditionTypeName);
                url = condTypeMgr.getViewBeanURL(condition);
            }
        } catch (AMConsoleException e) {
            debug.warning("PolicyModelImpl.getConditionViewBeanURL", e);
        } catch (NameNotFoundException e) {
            debug.warning("PolicyModelImpl.getConditionViewBeanURL", e);
        } catch (PolicyException e) {
            debug.warning("PolicyModelImpl.getConditionViewBeanURL", e);
        }

        return url;
    }

    private Condition getConditionInstance(
        String realmName,
        String conditionTypeName
    ) {
        Condition condition = null;

        try {
            PolicyManager policyMgr = getPolicyManager(realmName);
            if (policyMgr != null) {
                ConditionTypeManager condTypeMgr =
                    policyMgr.getConditionTypeManager();
                condition = condTypeMgr.getCondition(conditionTypeName);
            }
        } catch (AMConsoleException e) {
            debug.warning("PolicyModelImpl.getConditionInstance", e);
        } catch (NameNotFoundException e) {
            debug.warning("PolicyModelImpl.getConditionInstance", e);
        } catch (PolicyException e) {
            debug.warning("PolicyModelImpl.getConditionInstance", e);
        }

        return condition;
    }

    /**
     * Returns a condition object.
     *
     * @param realmName Name of Realm.
     * @param conditionType Name of condition type.
     * @param values Values of the condition.
     * @return condition object.
     * @throws AMConsoleException if condition cannot be created.
     */
    public Condition createCondition(
        String realmName,
        String conditionType,
        Map values
    ) throws AMConsoleException {
        Condition condition = null;

        try {
            PolicyManager policyMgr = getPolicyManager(realmName);
            if (policyMgr != null) {
                ConditionTypeManager conditionTypeMgr =
                    policyMgr.getConditionTypeManager();
                condition = conditionTypeMgr.getCondition(conditionType);
                condition.setProperties(values);
            }
        } catch (NameNotFoundException e) {
            throw new AMConsoleException(getErrorString(e));
        } catch (PolicyException e) {
            throw new AMConsoleException(getErrorString(e));
        }

        return condition;
    }

    /**
     * Returns property sheet XML for condition.
     *
     * @param realmName Name of Realm.
     * @param conditionType Name of condition name.
     * @param readonly true if the administrator has only read only permission.
     * @return property sheet XML for condition.
     */
    public String getConditionXML(
        String realmName,
        String conditionType,
        boolean readonly
    ) {
        String prefix = AMAdminUtils.getStringFromInputStream(
            getClass().getClassLoader().getResourceAsStream(
                "com/sun/identity/console/propertyPMConditionAdd.xml"));
        PolicyPropertyXMLBuilder builder = new PolicyPropertyXMLBuilder(
            getConditionInstance(realmName, conditionType), this);
        builder.setAllAttributeReadOnly(readonly);
        return builder.getXML(prefix);
    }

    /**
     * Returns property names of a condition.
     *
     * @param realmName Name of Realm.
     * @param conditionType Name of condition name.
     * @return property names of a condition.
     */
    public List getConditionPropertyNames(
        String realmName,
        String conditionType
    ) {
        Condition cond = getConditionInstance(realmName, conditionType);
        return (cond != null) ? cond.getPropertyNames() :
            Collections.EMPTY_LIST;
    }

    /**
     * Returns condition type name of a condition.
     *
     * @param realmName Name of realm.
     * @param condition Condition instance.
     * @return Condition type name of a condition.
     */
    public String getConditionTypeName(String realmName, Condition condition) {
        String typeName = null;

        try {
            PolicyManager policyMgr = getPolicyManager(realmName);
            if (policyMgr != null) {
                ConditionTypeManager conditionTypeMgr =
                    policyMgr.getConditionTypeManager();
                typeName = conditionTypeMgr.getConditionTypeName(condition);
            }
        } catch (AMConsoleException e) {
            debug.warning("PolicyModelImpl.getConditionTypeName", e);
        }

        return typeName;
    }

    /**
     * Returns a descriptive message if policy cannot be created under a realm.
     *
     * @param realmName Name of Realm.
     * @return a descriptive message if policy cannot be created under a realm.
     */
    public String canCreatePolicy(String realmName) {
        String message = null;
        try {
            PolicyManager policyMgr = getPolicyManager(realmName);
            if (policyMgr != null) {
                Set<String> services = getServiceTypeNames().keySet();

                if (!policyMgr.canCreatePolicies(services)) {
                    message = "noReferralForOrg.message";
                } else if (!hasPolicyConfigSvcRegistered(realmName)) {
                    message = "noPolicyConfigSvc.message";
                }
            }
        } catch (EntitlementException e) {
            message = e.getMessage();
        } catch (AMConsoleException e) {
            message = e.getMessage();
        }

        return message;
    }

    private boolean hasPolicyConfigSvcRegistered(String realmName) {
        try {
            OrganizationConfigManager orgCfgMgr = new OrganizationConfigManager(
                adminSSOToken, realmName);
            Set names = orgCfgMgr.getAssignedServices();
            return (names != null) &&
                names.contains(AMAdminConstants.POLICY_SERVICE);
        } catch (SMSException e) {
            debug.warning("PolicyModelImpl.hasPolicyConfigSvcRegistered", e);
            return false;
        }
    }



    /**
     * Returns set of authentication instances.
     *
     * @param realmName Name of Realm.
     * @return set of authentication instances.
     * @throws AMConsoleException if authentication instances cannot be
     *         obtained.
     */
    public Set getAuthenticationInstances(String realmName)
        throws AMConsoleException {
        Set names = Collections.EMPTY_SET;
        try {
            AMAuthenticationManager mgr = new AMAuthenticationManager(
                getUserSSOToken(), realmName);
            Set instances = mgr.getAuthenticationInstances();
            if ((instances != null) && !instances.isEmpty()) {
                names = new HashSet(instances.size());
                for (Iterator i = instances.iterator(); i.hasNext(); ) {
                    names.add(((AMAuthenticationInstance)i.next()).getName());
                }
            }
        } catch (AMConfigurationException e) {
            throw new AMConsoleException(getErrorString(e));
        }

        return names;
    }

    public boolean isPolicyActive(String realmName, String policyName)
    throws AMConsoleException {
        String policyID = cachePolicy(realmName, policyName);
        CachedPolicy cachedPolicy = getCachedPolicy(policyID);
        Policy policy = cachedPolicy.getPolicy();

        return policy.isActive();
    }
    
    public Set getProtectedResourceNames(String realmName, String policyName)
        throws AMConsoleException {
        Set resourceNames = new HashSet();
        String policyID = cachePolicy(realmName, policyName);
        CachedPolicy cachedPolicy = getCachedPolicy(policyID);
        Policy policy = cachedPolicy.getPolicy();
        Set ruleNames = policy.getRuleNames();

        if ((ruleNames != null) && !ruleNames.isEmpty()) {
            for(Iterator iter = ruleNames.iterator(); iter.hasNext();) {
                String ruleName = (String)iter.next();
                try {
                    Rule rule = policy.getRule(ruleName);
                    if (rule != null) {
                        String resName = rule.getResourceName();
                        if ((resName != null) && (resName.trim().length() > 0)){
                            resourceNames.add(resName);
                        }
                    }
                } catch(NameNotFoundException nnfe) {
                    if (debug.warningEnabled()) {
                        debug.warning("Cannot find the rule with name '" +
                            ruleName + " in policy " + policy.getName(), nnfe);
                    }
                }
            }
        }
        return resourceNames;
    }

    /**
     * Returns authentication instances configured for the realm.
     *
     * @param realmName Name of realm.
     * @return authentication instances configured for the realm.
     */
    public Set getAuthInstances(String realmName) {
        Set instances = null;

        try {
            AMAuthenticationManager mgr = new AMAuthenticationManager(
                adminSSOToken, realmName);
            Set inst = mgr.getAuthenticationInstances();
            if ((inst != null) && !inst.isEmpty()) {
                instances = new HashSet(inst.size() *2);
                for (Iterator iter = inst.iterator(); iter.hasNext(); ) {
                    AMAuthenticationInstance i = (AMAuthenticationInstance)
                        iter.next();
                    instances.add(i.getName());
                }
            }
        } catch (AMConfigurationException e) {
            debug.warning("PolicyModelImpl.getAuthInstances", e);
        }

        return (instances == null) ? Collections.EMPTY_SET : instances;
    }

    /**
     * Returns authentication level of an authentication instance.
     *
     * @param realmName Name of realm.
     * @param name Authentication Instance name.
     * @return authentication level of an authentication instance.
     */
    public String getAuthenticationLevel(String realmName, String name) {
        String level = "0";

        try {
            AMAuthenticationManager mgr = new AMAuthenticationManager(
                adminSSOToken, realmName);
            AMAuthenticationInstance ai = mgr.getAuthenticationInstance(name);
            Map map = ai.getAttributeValues();
            String authType = ai.getType();
            Set set = (Set)map.get(
                AMAuthConfigUtils.getAuthLevelAttribute(map, authType));
            if ((set != null) && !set.isEmpty()) {
                level = (String)set.iterator().next();
            }
        } catch (AMConfigurationException e) {
            debug.warning("PolicyModelImpl.getInstanceValues", e);
        }

        return level;
    }

    /**
     * Returns realms that have names matching with a filter.
     *
     * @param base Base realm name for this search. null indicates root
     *        suffix.
     * @param filter Filter string.
     * @return realms that have names matching with a filter.
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
            OrganizationConfigManager orgMgr = new OrganizationConfigManager(
                adminSSOToken, base);
            logEvent("SUCCEED_GET_REALM_NAMES", param);
            return appendBaseDN(base,
                orgMgr.getSubOrganizationNames(filter, true), filter, this);
        } catch (SMSException e) {
            String strError = getErrorString(e);
            String[] paramsEx = {base, strError};
            logEvent("SMS_EXCEPTION_GET_REALM_NAMES", paramsEx);
            throw new AMConsoleException(strError);
        }
    }

    /*
     * the issue is that the search results are relative to the base.
     */
    static Set appendBaseDN(
        String base,
        Set results,
        String filter,
        AMModel model
    ) {
        Set altered = new HashSet();
        String displayName = null;
        if (base.equals("/")) {
            displayName = AMFormatUtils.DNToName(model, model.getStartDSDN());
        } else {
            int idx = base.lastIndexOf("/");
            displayName = (idx != -1) ? base.substring(idx+1) : base;
        }
        if (DisplayUtils.wildcardMatch(displayName, filter)) {
            altered.add(base);
        }

        if ((results != null) && !results.isEmpty()) {
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

}
