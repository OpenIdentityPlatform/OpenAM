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
 * $Id: PolicyModel.java,v 1.2 2008/06/25 05:43:07 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */

package com.sun.identity.console.policy.model;

import com.iplanet.jato.view.html.OptionList;
import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.QueryResults;
import com.sun.identity.policy.ActionSchema;
import com.sun.identity.policy.Policy;
import com.sun.identity.policy.Syntax;
import com.sun.identity.policy.ValidValues;
import com.sun.identity.policy.interfaces.Condition;
import com.sun.identity.policy.interfaces.Referral;
import com.sun.identity.policy.interfaces.ResponseProvider;
import com.sun.identity.policy.interfaces.Subject;
import java.util.List;
import java.util.Map;
import java.util.Set;

/* - NEED NOT LOG - */

public interface PolicyModel
    extends AMModel
{
    String TF_NAME = "tfName";

    /**
     * Returns cached policy object.
     *
     * @param cacheID Cache ID.
     * @return cached policy object.
     * @throws AMConsoleException if policy object cannot be located.
     */
    CachedPolicy getCachedPolicy(String cacheID)
        throws AMConsoleException;

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
    String cachePolicy(
        String policyName,
        String description,
        boolean isReferral,
        boolean isActive
    ) throws AMConsoleException;
    
    /**
     * Caches an existing policy. Returns the cache ID of the policy object.
     *
     * @param realmName Name of realm.
     * @param policyName Name of policy.
     * @return cache ID of the policy object.
     * @throws AMConsoleException if policy cannot be cached.
     */
    String cachePolicy(String realmName, String policyName) 
        throws AMConsoleException;

    /**
     * Returns policy names that are under a realm.
     *
     * @param realmName Name of realm.
     * @param filter Filter string.
     * @return policy names that are under a realm.
     * @throws AMConsoleException if policy names cannot be returned.
     */
    Set getPolicyNames(String realmName, String filter)
        throws AMConsoleException;

    /**
     * Creates a policy.
     *
     * @param realmName Name of realm.
     * @param policy Policy object.
     * @throws AMConsoleException if policy cannot be created.
     */
    void createPolicy(String realmName, Policy policy)
        throws AMConsoleException;

    /**
     * Modifies a policy.
     *
     * @param realmName Name of realm.
     * @param policy Policy object.
     * @throws AMConsoleException if policy cannot be created.
     */
    void replacePolicy(String realmName, Policy policy)
        throws AMConsoleException;

    /**
     * Deletes policies.
     *
     * @param realmName Name of realm that contains these policies.
     * @param names Set of policy names to be deleted.
     * @throws AMConsoleException if policies cannot be deleted.
     */
    void deletePolicies(String realmName, Set names)
        throws AMConsoleException;

    /**
     * Returns true if rule can be created in a policy.
     *
     * @param policy Policy object.
     * @param realmName Realm Name.
     * @return true if rule can be created in a policy.
     */
    boolean canCreateRule(Policy policy, String realmName);

    /**
     * Returns all registered service type names. Map of service name to its
     * localized name.
     *
     * @return all registered service type names.
     */
    Map getServiceTypeNames();

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
    Set getActionSchemas(
        Policy policy,
        String realmName,
        String name,
        boolean withResourceName);

    /**
     * Returns localized name of action schema.
     *
     * @param name Name of Service Type.
     * @param actionSchema Action Schema.
     * @return localized name of action schema.
     */
    String getActionSchemaLocalizedName(String name, ActionSchema actionSchema);

    /**
     * Returns a option list of possible choices.
     *
     * @param name Name of Service Type.
     * @param actionSchema Action Schema.
     * @return a option list of possible choices.
     */
    OptionList getChoiceValues(String name, ActionSchema actionSchema);

    /**
     * Returns true if service type requires resource name.
     *
     * @param policy Policy object.
     * @param realmName Realm Name.
     * @param name Name of Service Type.
     * @return true if service type requires resource name.
     */
    boolean requiredResourceName(Policy policy, String realmName, String name);

    /**
     * Returns true if service type does not require resource name.
     *
     * @param policy Policy object.
     * @param realmName Realm Name.
     * @param name Name of Service Type.
     * @return true if service typedoes not  require resource name.
     */
    boolean notRequiredResourceName(
        Policy policy,
        String realmName,
        String name);

    /**
     * Returns true of new resource can be created under a realm of a given
     * service type.
     *
     * @param realmName Name of Realm.
     * @param svcTypeName Name of Service Type.
     * @return true of new resource can be created under a realm of a given
     *         service type.
     */
    boolean canCreateNewResource(String realmName, String svcTypeName);

    /**
     * Returns a list of managed resource names.
     *
     * @param realmName Name of realm.
     * @param serviceTypeName Name of service type.
     * @return a list of managed resource names.
     */
    List getManagedResources(String realmName, String serviceTypeName);

    /**
     * Returns a map of active referral types for a realm to its display name.
     *
     * @param realmName Name of Realm.
     * @return a map of active referral types for a realm to its display name.
     */
    Map getActiveReferralTypes(String realmName);

    /**
     * Returns syntax for a referral.
     *
     * @param realmName Name of Realm.
     * @param referralType Name of referral type.
     * @return syntax for a referral.
     */
    Syntax getReferralSyntax(String realmName, String referralType);

    /**
     * Returns a referral object.
     *
     * @param realmName Name of Realm.
     * @param referralType Name of referral type.
     * @param values Values of the referral.
     * @return referral obejct.
     * @throws AMConsoleException if referral cannot be created.
     */
    Referral createReferral(String realmName, String referralType, Set values)
        throws AMConsoleException;

    /**
     * Returns a set of possible values for a referral type.
     *
     * @param realmName Name of Realm.
     * @param referralType Name of Referral Type.
     * @param filter wildcards for filtering the results.
     * @return a set of possible values for a referral type.
     */
    ValidValues getReferralPossibleValues(
        String realmName, String referralType, String filter);

    /**
     * Returns properties view bean URL of a referral.
     *
     * @param realmName Name of realm.
     * @param referralTypeName Name of Referral Type.
     * @return properties view bean URL of a referral.
     */
    String getReferralViewBeanURL(String realmName, String referralTypeName);

    /**
     * Returns a map of values to localized label.
     *
     * @param realmName Name of realm.
     * @param referralTypeName Name of referral Type.
     * @param values Valid values.
     * @return a map of values to localized label.
     */
    Map getDisplayNameForReferralValues(
        String realmName,
        String referralTypeName,
        Set values);

    /**
     * Returns a map of active subject types for a realm to its display name.
     *
     * @param realmName Name of Realm.
     * @return a map of active subject types for a realm to its display name.
     */
    QueryResults getActiveSubjectTypes(String realmName);

    /**
     * Returns syntax for a subject.
     *
     * @param realmName Name of Realm.
     * @param subjectType Name of Subject type.
     * @return syntax for a subject.
     */
    Syntax getSubjectSyntax(String realmName, String subjectType);

    /**
     * Returns a subject object.
     *
     * @param realmName Name of Realm.
     * @param subjectType Name of subject type.
     * @param values Values of the subject.
     * @return subject obejct.
     * @throws AMConsoleException if subject cannot be created.
     */
    Subject createSubject(String realmName, String subjectType, Set values)
        throws AMConsoleException;

    /**
     * Returns a set of possible values for a subject type.
     *
     * @param realmName Name of Realm.
     * @param subjectType Name of Subject Type.
     * @param filter wildcards for filtering the results.
     * @return a set of possible values for a subject type.
     * @throws AMConsoleException if values cannot be obtained.
     */
    ValidValues getSubjectPossibleValues(
        String realmName,
        String subjectType,
        String filter
    ) throws AMConsoleException;

    /**
     * Returns subject type name of a subject.
     *
     * @param realmName Name of realm.
     * @param subject Subject instance.
     * @return subject type name of a subject.
     */
    String getSubjectTypeName(String realmName, Subject subject);

    /**
     * Returns properties view bean URL of a subject.
     *
     * @param realmName Name of realm.
     * @param subject Subject instance.
     * @return properties view bean URL of a subject.
     */
    String getSubjectViewBeanURL(String realmName, Subject subject);

    /**
     * Returns properties view bean URL of a subject.
     *
     * @param realmName Name of realm.
     * @param subjectTypeName Name of Subject Type.
     * @return properties view bean URL of a subject.
     */
    String getSubjectViewBeanURL(String realmName, String subjectTypeName);

    /**
     * Returns a map of values to localized label.
     *
     * @param realmName Name of realm.
     * @param subjectTypeName Name of Subject Type.
     * @param values Valid values.
     * @return a map of values to localized label.
     */
    Map getDisplayNameForSubjectValues(
        String realmName,
        String subjectTypeName,
        Set values);
    /**
     * Returns property sheet XML for response provider.
     *
     * @param realmName Name of Realm.
     * @param providerType Name of response provider name.
     * @param bCreate true for create view bean.
     * @param readonly true if administrator can only read permission.
     * @return property sheet XML for response provider.
     */
    String getResponseProviderXML(
        String realmName,
        String providerType,
        boolean bCreate,
        boolean readonly);

    /**
     * Returns property names of a response provider.
     *
     * @param realmName Name of Realm.
     * @param providerType Name of response provider name.
     * @return property names of a response provider.
     */
    List getResponseProviderPropertyNames(
        String realmName,
        String providerType);

    /**
     * Returns a response provider object.
     *
     * @param realmName Name of Realm.
     * @param providerType Name of response provider type.
     * @param values Values of the response provider.
     * @return response provider object.
     * @throws AMConsoleException if response provider cannot be created.
     */
    ResponseProvider createResponseProvider(
        String realmName,
        String providerType,
        Map values
    ) throws AMConsoleException;

    /**
     * Returns a map of active response provider types for a realm to its
     * display name.
     *
     * @param realmName Name of Realm.
     * @return a map of active response provider types for a realm to its
     *         display name.
     */
    Map getActiveResponseProviderTypes(String realmName);

    /**
     * Returns response provider type name of a response provider.
     *
     * @param realmName Name of realm.
     * @param provider response provider instance.
     * @return response provider type name of a response provider.
     */
    String getResponseProviderTypeName(
        String realmName,
        ResponseProvider provider);

    /**
     * Returns properties view bean URL of a response provider.
     *
     * @param realmName Name of realm.
     * @param provider response provider Object.
     * @return properties view bean URL of a response provider.
     */
    String getResponseProviderViewBeanURL(
        String realmName,
        ResponseProvider provider);
                                                                                
    /**
     * Returns properties view bean URL of a response provider.
     *
     * @param realmName Name of realm.
     * @param typeName Name of response provider Type.
     * @return properties view bean URL of a response provider.
     */
    String getResponseProviderViewBeanURL(String realmName, String typeName);

    /**
     * Returns a map of active condition types for a realm to its display name.
     *
     * @param realmName Name of Realm.
     * @return a map of active condition types for a realm to its display name.
     */
    Map getActiveConditionTypes(String realmName);

    /**
     * Returns properties view bean URL of a condition.
     *
     * @param realmName Name of realm.
     * @param condition Condition Object.
     * @return properties view bean URL of a condition.
     */
    String getConditionViewBeanURL(String realmName, Condition condition);

    /**
     * Returns properties view bean URL of a condition.
     *
     * @param realmName Name of realm.
     * @param conditionTypeName Name of Condition Type.
     * @return properties view bean URL of a condition.
     */
    String getConditionViewBeanURL(String realmName, String conditionTypeName);

    /**
     * Returns a condition object.
     *
     * @param realmName Name of Realm.
     * @param conditionType Name of condition type.
     * @param values Values of the condition.
     * @return condition object.
     * @throws AMConsoleException if condition cannot be created.
     */
    Condition createCondition(
        String realmName,
        String conditionType,
        Map values
    ) throws AMConsoleException;

    /**
     * Returns property sheet XML for condition.
     *
     * @param realmName Name of Realm.
     * @param conditionType Name of condition name.
     * @param readonly true if the administrator has only read only permission.
     * @return property sheet XML for condition.
     */
    String getConditionXML(
        String realmName,
        String conditionType,
        boolean readonly);

    /**
     * Returns property names of a condition.
     *
     * @param realmName Name of Realm.
     * @param conditionType Name of condition name.
     * @return property names of a condition.
     */
    List getConditionPropertyNames(String realmName, String conditionType);

    /**
     * Returns condition type name of a condition.
     *
     * @param realmName Name of realm.
     * @param condition Condition instance.
     * @return Condition type name of a condition.
     */
    String getConditionTypeName(String realmName, Condition condition);

    /**
     * Returns a descriptive message if policy cannot be created under a realm.
     *
     * @param realmName Name of Realm.
     * @return a descriptive message if policy cannot be created under a realm.
     */
    String canCreatePolicy(String realmName);

    /**
     * Returns set of authentication instances.
     *
     * @param realmName Name of Realm.
     * @return set of authentication instances.
     * @throws AMConsoleException if authentication instances cannot be
     *         obtained.
     */
    Set getAuthenticationInstances(String realmName)
        throws AMConsoleException;
    
    /**
     * Returns true if the policy is active, false otherwise.
     *
     * @param realmName Realm name.
     * @param policyName Policy name.
     * @return true if the policy is active, false otherwise.
     */
    boolean isPolicyActive(String realmName, String policyName)
        throws AMConsoleException;

    /**
     * Returns names of resources protected by the policy.
     *
     * @param realmName Realm name.
     * @param policyName Policy name.
     * @return Names of resources protected by the policy.
     */
    Set getProtectedResourceNames(String realmName, String policyName)
        throws AMConsoleException;

    /**
     * Returns authentication instances configured for the realm.
     *
     * @param realmName Name of realm.
     * @return authentication instances configured for the realm.
     */
    Set getAuthInstances(String realmName);

    /**
     * Returns authentication level of an authentication instance.
     *
     * @param realmName Name of realm.
     * @param name Authentication Instance name.
     * @return authentication level of an authentication instance.
     */
    String getAuthenticationLevel(String realmName, String name);

    /**
     * Returns realms that have names matching with a filter.
     *
     * @param base Base realm name for this search. null indicates root
     *        suffix.
     * @param filter Filter string.
     * @return realms that have names matching with a filter.
     * @throws AMConsoleException if search fails.
     */
    Set getRealmNames(String base, String filter)
        throws AMConsoleException;
}
