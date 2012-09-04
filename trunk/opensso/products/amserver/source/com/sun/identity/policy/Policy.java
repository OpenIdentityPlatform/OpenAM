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
 * $Id: Policy.java,v 1.9 2010/01/10 01:19:35 veiming Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.policy;

import com.sun.identity.policy.interfaces.Subject;
import com.sun.identity.policy.interfaces.Condition;
import com.sun.identity.policy.interfaces.ResponseProvider;
import com.sun.identity.policy.interfaces.Referral;

import java.util.*;
import com.sun.identity.shared.ldap.util.DN;

import org.w3c.dom.*;

import com.iplanet.sso.*;
import com.sun.identity.shared.debug.Debug;
import com.iplanet.am.util.Cache;
import com.sun.identity.shared.xml.XMLUtils;
import com.sun.identity.sm.AttributeSchema;
import com.sun.identity.policy.plugins.OrgReferral;

/**
 * The class <code>Policy</code> represents a policy definition.
 * A policy contains a set of rules associated with a collection of 
 * users and conditions. The policy object is saved in the data store
 * only when the <code>store</code> method of the <code>Policy</code> is 
 * called, or if the methods <code>addPolicy</code> or <code>replacePolicy
 * </code> of <code>PolicyManager</code> instance is invoked with this policy.
 * The <code>Policy</code> object is accessible to policy evaluation and 
 * enforcement points only after it is saved in data store. 
 *
 * @supported.api
 */
public class Policy implements Cloneable {

    static final String REFERRAL_POLICY = "referralPolicy";
    static final String ACTIVE_FLAG = "active";

    private static final int SUBJECTS_CONDITIONS_RULES = 1;
    private static final int CONDITIONS_SUBJECTS_RULES = 2;
    private static final int RULES_SUBJECTS_CONDITIONS = 3;
    private static final int RULES_CONDITIONS_SUBJECTS = 4;
    private static final int SUBJECTS_RULES_CONDITIONS = 5;
    private static final int CONDITIONS_RULES_SUBJECTS = 6;

    private static String EVALUATION_WEIGHTS = null;
    private static String DEFAULT_EVALUATION_WEIGHTS = "10:10:10";
    private final static String EVALUATION_WEIGHTS_KEY 
            = "com.sun.identity.policy.Policy.policy_evaluation_weights";
    private static final Debug DEBUG = PolicyManager.debug;

    private int evaluationOrder = RULES_SUBJECTS_CONDITIONS;

    private static int ruleWeight;
    private static int conditionWeight;
    private static int subjectWeight;

    private int prWeight;
    private int pcWeight;
    private int psWeight;

    static {
        initializeStaticEvaluationWeights();
    }

    private String origPolicyName;
    private String policyName;
    private String description = "";
    private String createdBy;
    private String lastModifiedBy;
    private long creationDate;
    private long lastModifiedDate;
    private boolean referralPolicy=false;
    private boolean active = true;

    private int priority;
    private Map rules = new HashMap();
    private Subjects users = new Subjects();
    private Conditions conditions = new Conditions();
    private ResponseProviders respProviders = new ResponseProviders();
    private Referrals referrals = new Referrals();
    private String organizationName;
    private final static int MATCHED_RULE_RESULTS_CACHE_SIZE = 1000;
    private final static int MATCHED_REFERRAL_RULES_CACHE_SIZE = 100;
    private Cache matchRulesResultsCache 
            = new Cache(MATCHED_RULE_RESULTS_CACHE_SIZE);
    private String subjectRealm;

    /**
     * No-arg constructor.
     */
    private Policy() {
        // do nothing
    }

    /**
     * Constructs a policy given the policy name.
     *
     * @param policyName name of the policy
     *
     * @exception InvalidNameException if policy name is not valid
     *
     * @supported.api
     *
     */
    public Policy(String policyName) throws InvalidNameException {
        this(policyName, null);
    }

    /**
     * Constructs a policy given the policy name and priority. 
     *
     * @param policyName name of the policy
     * @param priority priority assigned to the policy
     *
     * @exception InvalidNameException if policy name is not valid
     */
    private Policy(String policyName, int priority) throws InvalidNameException 
    {
        validateName(policyName);
        this.policyName = policyName;
        // Set the policy priority
        this.priority = priority;
    }

    /**
     * Constructs a policy given  the policy name and description.
     *
     * @param policyName name of the policy
     * @param description description for the policy
     *
     * @exception InvalidNameException if policy name is not valid
     *
     * @supported.api
     *
     */
    public Policy(String policyName, String description)
        throws InvalidNameException {

        this(policyName, description, false, true);
    }

    /**
     * Constructs a policy given  the policy name,description and a 
     * referralPolicy flag.
     *
     * @param policyName name of the policy
     * @param description description for the policy
     * @param referralPolicy indicates whether the policy is a 
     *        referral policy or a standard policy.
     * A referral policy is used only to delegate policy definitions to 
     * sub/peer organizations. A referral policy does not make use of any
     * action values
     *
     *
     * @exception InvalidNameException if policy name is not valid
     *
     * @supported.api
     *
     */
    public Policy(String policyName, String description, 
        boolean referralPolicy) throws InvalidNameException 
    {
        this(policyName, description, referralPolicy, true);
    }

    /**
     * Constructs a policy given  the policy name , description,
     * referralPolicy flag, and active flag
     *
     * @param policyName name of the policy
     * @param description description for the policy
     * @param referralPolicy indicates whether the policy is a 
     *        referral policy or a standard policy.
     * @param active indicates if the policy is active or not.
     * A referral policy is used only to delegate policy definitions to 
     * sub/peer organizations. A referral policy does not make use of any
     * action values
     *
     * @exception InvalidNameException if policy name is not valid
     *
     * @supported.api
     *
     */
    public Policy(String policyName, String description, 
        boolean referralPolicy, boolean active) throws InvalidNameException 
    {
        validateName(policyName);
        this.policyName = policyName;
        if (description != null) {
            this.description = description;
        }
        this.referralPolicy = referralPolicy;
        this.active = active;
    }

    /**
     * Constructs a policy given the Policy Node. 
     * This is used by PolicyManager
     * @param pm <code>PolicyManager</code> requesting the operation
     *
     * @param policyNode XML node in W3C DOM format representing 
     * the policy object which needs to be created.
     * @exception InvalidFormatException, InvalidNameException,
     * NameNotFoundException, PolicyException
     */
    public Policy(PolicyManager pm, Node policyNode)
        throws InvalidFormatException, InvalidNameException,
        NameNotFoundException, PolicyException {
        // Check if the node name is PolicyManager.POLICY_ROOT_NODE
        if (!policyNode.getNodeName().equalsIgnoreCase(
            PolicyManager.POLICY_ROOT_NODE)) {
            if (PolicyManager.debug.warningEnabled()) {
                PolicyManager.debug.warning(
                    "invalid policy xml blob given to construct policy");
            }
            throw (new InvalidFormatException(ResBundleUtils.rbName,
                "invalid_xml_policy_root_node", null, "", 
                PolicyException.POLICY));
        }

        // Get the policy name
        policyName = XMLUtils.getNodeAttributeValue(policyNode,
            PolicyManager.NAME_ATTRIBUTE);
        validateName(policyName);

        // Get descrition, can be null
        description = XMLUtils.getNodeAttributeValue(policyNode,
                PolicyManager.DESCRIPTION_ATTRIBUTE);

        getModificationInfo(policyNode);

        // Get referralPolicy flag
        String referralPolicy = XMLUtils.getNodeAttributeValue(policyNode,
                Policy.REFERRAL_POLICY);
        if ( (referralPolicy != null) &&
                (referralPolicy.equalsIgnoreCase("true")) ) {
            this.referralPolicy = true;
        }

        // Get active flag
        String active = XMLUtils.getNodeAttributeValue(policyNode,
                Policy.ACTIVE_FLAG);
        if ( (active != null) &&
                (active.equalsIgnoreCase("false")) ) {
            this.active = false;
        }

        // Get priority
        String pri = XMLUtils.getNodeAttributeValue(policyNode,
            PolicyManager.PRIORITY_ATTRIBUTE);
        if (pri != null) {
            try {
                priority = Integer.parseInt(pri);
            } catch (NumberFormatException nfe) {
                // write to debug and continue
                PolicyManager.debug.error("Number format exception in " +
                   "determining policy's priority: " + pri, nfe);
            }
        }


        // Get the rule nodes and instantiate them
        Set ruleNodes = XMLUtils.getChildNodes(policyNode,
            PolicyManager.POLICY_RULE_NODE);
        if ( ruleNodes != null ) {
            Iterator items = ruleNodes.iterator();
            while (items.hasNext()) {
                Node ruleNode = (Node) items.next();
                Rule rule = new Rule(ruleNode);
                addRule(rule);
            }
        }

        if (!this.referralPolicy) {
            // Get the users collection and instantiate Subjects
            Node subjectsNode = XMLUtils.getChildNode(policyNode,
                    PolicyManager.POLICY_SUBJECTS_NODE);
            if ( subjectsNode != null ) {
                users = new Subjects(pm, subjectsNode);
            }
        
            // Get the conditions collection and instantiate Conditions
            Node conditionsNode = XMLUtils.getChildNode(policyNode,
                    PolicyManager.POLICY_CONDITIONS_NODE);
            if ( conditionsNode != null ) {
                conditions = new Conditions(pm.getConditionTypeManager(), 
                    conditionsNode);
            }
            // Get the respProviders collection and instantiate 
            // ResponseProviders
            Node respProvidersNode = XMLUtils.getChildNode(policyNode,
                    PolicyManager.POLICY_RESP_PROVIDERS_NODE);
            if ( respProvidersNode != null ) {
                respProviders = new ResponseProviders(
                    pm.getResponseProviderTypeManager(), 
                    respProvidersNode);
            }
        } else {
            // Get the referrals collection and instantiate Referrals
            Node referralsNode = XMLUtils.getChildNode(policyNode,
                    PolicyManager.POLICY_REFERRALS_NODE);
            if ( referralsNode != null ) {
                referrals = new Referrals(pm, referralsNode);
            }
        }
    }

    private void getModificationInfo(Node policyNode) {
        String strCreationDate = XMLUtils.getNodeAttributeValue(policyNode,
                PolicyManager.CREATION_DATE_ATTRIBUTE);
        if ((strCreationDate != null) && (strCreationDate.length() > 0)) {
            try {
                creationDate = Long.parseLong(strCreationDate);
            } catch (NumberFormatException e) {
                //ignore
            }
        }
        String strLastModifiediDate = XMLUtils.getNodeAttributeValue(
            policyNode, PolicyManager.LAST_MODIFIED_DATE_ATTRIBUTE);
        if ((strLastModifiediDate != null) &&
            (strLastModifiediDate.length() > 0)
        ) {
            try {
                lastModifiedDate = Long.parseLong(strLastModifiediDate);
            } catch (NumberFormatException e) {
                //ignore
            }
        }

        createdBy = XMLUtils.getNodeAttributeValue(policyNode,
            PolicyManager.CREATED_BY_ATTRIBUTE);
        lastModifiedBy = XMLUtils.getNodeAttributeValue(policyNode,
            PolicyManager.LAST_MODIFIED_BY_ATTRIBUTE);
    }

    /**
     * Gets the name of the policy.
     *
     * @return name of the policy
     */
    public String getName() {
        return (policyName);
    }

    /**
     * Sets the name of the policy.
     *
     * @param policyName name of the policy.
     * @exception InvalidNameException if <code>policyName</code> is an invalid
     * name.
     *
     * @supported.api
     *
     */
    public void setName(String policyName) throws InvalidNameException {
        validateName(policyName);
        if (this.policyName.equals(policyName)) {
            return;
        }
        if (origPolicyName == null) {
            origPolicyName = this.policyName;
        }
        this.policyName = policyName;
    }

    /**
     * Gets the original policy name. 
     * This is used to track policies called via
     * <code>PolicyManager::replacePolicy()</code>
     * with the changed policy name.
     *
     * @return the policy name that was present when
     *          the object was instantiated
     */
    protected String getOriginalName() {
        return (origPolicyName);
    }

    /**
     * Sets the organization name under which the policy is created
     * This would be set only for policies that have been read from data store.
     * Otherwise this would be <code>null</code>
     *
     * @param  organizationName name of the organization name in which the 
     * policy is created.
     */
    void setOrganizationName(String organizationName) {
        this.organizationName = organizationName;
    }

    /**
     * Gets the organization name under which the policy is created
     * This would be set only for policies that have been read from data store.
     * Otherwise this would be <code>null</code>
     *
     * @return the organization name under which the policy is created
     *
     * @supported.api
     *
     */
    public String getOrganizationName() {
        return organizationName;
    }

    /**
     * Resets the original policy name
     */
    protected void resetOriginalName() {
        origPolicyName = null;
    }

    /**
     * Gets the description for the policy.
     * If the description for the policy has not been set
     * the method will return an empty string; not <code>
     * null</code>.
     *
     * @return description of the policy
     *
     * @supported.api
     *
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the description for the policy.
     *
     * @param description description for the policy
     * @exception InvalidNameException if the description is invalid
     *
     * @supported.api
     *
     */
    public void setDescription(String description)
        throws InvalidNameException {
        if (description != null) {
            this.description = description;
        }
    }

    /**
     * Checks whether the policy is a referral policy.
     * A referral policy is used only to delegate policy definitions to 
     * sub/peer organizations. A referral policy does not make use of any
     * action values
     *
     * @return <code>true</code> if this is a referral policy.
     *         Otherwise returns <code>false</code>
     *
     * @supported.api
     *
     */
    public boolean isReferralPolicy() {
        return referralPolicy;
    }

    /**
     * Checks whether the policy is active or inactive
     * An inactive policy is not used to make policy evaluations.
     *
     * @return <code>true</code> if this is an active policy.
     *         Otherwise returns <code>false</code>
     *
     * @supported.api
     *
     */
    public boolean isActive() {
        return active;
    }

    /**
     * Set the active flag for policy.
     * An inactive policy is not used to make policy evaluations.
     * @param active <code>boolean</code> representing active or inactive.
     *
     * @supported.api
     *
     */
    public void setActive(boolean active) {
        this.active = active;
    }

    /**
     * Gets the priority of the policy.
     *
     * @return priority of the policy
     */
    public int getPriority() {
        return (priority);
    }

    /**
     * Sets a priority of the policy.
     *
     * @param priority priority of the policy
     */
    public void setPriority(int priority) {
        this.priority = priority;
    }

    /**
     * Gets the set of rule names associated with the policy.
     *
     * @return <code>Set</code> of rule names
     *
     * @supported.api
     *
     */
    public Set getRuleNames() {
        return (new HashSet(rules.keySet()));
    }

    /**
     * Gets the rule object identified by name.
     *
     * @param ruleName name of rule.
     *
     * @return <code>Rule</code> object.
     *
     * @exception NameNotFoundException if a <code>Rule</code> with the given 
     *            name does not exist
     * @supported.api
     *
     */
    public Rule getRule(String ruleName) throws NameNotFoundException {
        Rule rule = (Rule) rules.get(ruleName);
        if (rule == null) {
            throw (new NameNotFoundException(ResBundleUtils.rbName,
                "rule_not_found", null, ruleName, PolicyException.RULE));
        }
        return (rule);
    }

    /**
     * Adds a new policy rule.
     *
     * @param rule rule object to be added to the policy
     * @exception NameAlreadyExistsException a rule with the given name
     *            already exists
     * @exception InvalidNameException if the rule name is invalid
     *            same service name as the policy
     * @supported.api
     *
     */
    public void addRule(Rule rule) throws NameAlreadyExistsException ,
            InvalidNameException {
        // Since 5.0 does not support rule name, it can be null
        if (rule.getName() == null) {
            // Assign a name dynamically
            rule.setName("rule" + ServiceTypeManager.generateRandomName());
        }

        // Check if the rule name or rule itself already exists
        if (rules.containsKey(rule.getName())) {
            throw (new NameAlreadyExistsException(ResBundleUtils.rbName,
                "rule_name_already_present", null, rule.getName(),
                PolicyException.RULE));
        } else if (rules.containsValue(rule)) {
            throw (new NameAlreadyExistsException(ResBundleUtils.rbName,
                "rule_already_present", null, rule.getName(), 
                PolicyException.RULE));
        }

        rules.put(rule.getName(), rule);
    }

    /**
     * Replaces an existing rule with the same name by the
     * current one. If a <code>Rule</code> with the same name does not exist,
     * it will be added.
     *
     * @param rule <code>Rule</code> that will replace an existing rule
     *        with the same name
     *
     * @exception InvalidNameException if <code>Rule</code> name is invalid
     *
     * @supported.api
     *
     */
    public void replaceRule(Rule rule) throws InvalidNameException {
        // Since 5.0 does not support rule name, it can be null
        if (rule.getName() == null) {
            // Assign a name dynamically
            rule.setName("rule" + ServiceTypeManager.generateRandomName());
        }

        rules.put(rule.getName(), rule);
    }

    /**
     * Removes the <code>Rule</code> with the given name. 
     *
     * @param ruleName name of the rule
     *
     * @return returns the <code>Rule</code> object being removed;
     *         if not present returns <code>null</code>
     *
     * @supported.api
     *
     */
    public Rule removeRule(String ruleName) {
        return ((Rule) rules.remove(ruleName));
    }

    /**
     * Returns a <code>Subjects</code> object that contains
     * a set of <code>Subject</code> instances for which the
     * policy is applied.
     *
     * @return Subjects object of the policy
     */
    Subjects getSubjects() {
        return (users);
    }

    /**
     * Get the <code>Set</code> of subject names associated with the policy.
     *
     * @return <code>Set</code> of String objects representing subject names
     *
     * @supported.api
     *
     */
    public Set getSubjectNames() {
        return users.getSubjectNames();
    }

    /**
     * Gets the Subject object identified by name.
     *
     * @param subjectName name of subject.
     *
     * @return <code>Subject</code> object
     *
     * @exception NameNotFoundException if a Subject with the given name
     * does not exist
     *
     * @supported.api
     *
     */
    public Subject getSubject(String subjectName) throws NameNotFoundException {
        return users.getSubject(subjectName);
    }

    /**
     * Adds a new policy subject.
     * The subject is added as a normal (non exclusive) subject.
     * So, policy will apply to members of the subject.
     * The policy will apply to a user if he is a member of 
     * any normal (non exclusive) subject in the policy
     * or not a member of any exclusive subject in the policy.
     *
     * @param name name of the Subject instance 
     * @param subject Subject object to be added to the policy
     *
     * @exception NameAlreadyExistsException if a Subject with the given name
     *          already exists
     * @exception InvalidNameException if the subject name is invalid
     *
     * @supported.api
     *
     */
    public void addSubject(String name, Subject subject) 
            throws NameAlreadyExistsException, InvalidNameException {
        users.addSubject(name, subject, false);
    }

    /**
     * Adds a reference in the policy to a Subject defined at the realm.
     *
     *
     * @param token SSOToken of the user adding the subject
     * @param subjectName name of the Subject as defined at the realm
     * @param realmName name of the realm in which the subject is defined
     *
     * @exception NameAlreadyExistsException if a Subject with the given name
     *          already exists in the policy
     * @exception InvalidNameException if the subject name is invalid 
     *         or the subject is not found at the realm
     * @exception SSOException if the SSO token is invalid
     * @exception PolicyException if the subject could not be added 
     *               for any other reason
     *
     * @supported.api
     *
     */
    public void addRealmSubject(SSOToken token, String subjectName, 
            String realmName, boolean exclusive) 
            throws NameAlreadyExistsException, InvalidNameException,
            PolicyException, SSOException {
        PolicyManager pm = new PolicyManager(token, realmName);
        SubjectTypeManager stm = pm.getSubjectTypeManager();
        addRealmSubject(subjectName, stm, exclusive);
    }

    /**
     * Adds a reference in the policy to a Subject defined at the realm.
     *
     *
     * @param subjectName name of the Subject as defined at the realm
     * @param stm <code>SubjectTypeManager<code> of the realm.
     *       You have to pass the SubjectTypeManager of realm in which 
     *       you would save the policy. Trying to save the policy at 
     *       a different realm would throw PolicyException.
     *
     * @exception NameAlreadyExistsException if a Subject with the given name
     *          already exists in the policy
     * @exception InvalidNameException if the subject name is invalid 
     *         or the subject is not found at the realm
     * @exception SSOException if the SSO token is invalid
     * @exception PolicyException if the subject could not be added 
     *               for any other reason
     *
     * @supported.api
     *
     */
    public void addRealmSubject(String subjectName, SubjectTypeManager stm,
            boolean exclusive) 
            throws NameAlreadyExistsException, InvalidNameException,
            PolicyException, SSOException {
        String realmName = stm.getPolicyManager().getOrganizationDN();
        realmName = new DN(realmName).toRFCString().toLowerCase();
        if ((subjectRealm != null) && !subjectRealm.equals(realmName)) {
            String[] objs = {realmName, subjectRealm};
            if (DEBUG.messageEnabled()) {
                DEBUG.message("Policy.addRealmSubject():can not add"
                        + " realm subject " + subjectName
                        + " , from realm : " + realmName
                        + " , policy already has subject from different realm:"
                        + subjectRealm);
            }

            throw (new InvalidNameException(ResBundleUtils.rbName,
                "policy_realms_do_not_match", objs, null, realmName, 
                PolicyException.POLICY));
        }
        if (subjectRealm == null) {
            subjectRealm = realmName;
        }
        /**
         *  would result in NameNotFoundException if the subject does not exist
         *  we would propogate the exception without catching
         */
        stm.getSubjectByName(subjectName); 

        users.addSubject(subjectName, stm.getSharedSubject(subjectName),
                exclusive);

        if (DEBUG.messageEnabled()) {
            DEBUG.message("Policy.addRealmSubject():added "
                    + " realm subject " + subjectName
                    + " , from realm : " + realmName);
        }
    }

    /**
     * Adds a new policy subject.
     * The policy will apply to a user if he is a member of 
     * any normal (non exclusive) subject  in the policy
     * or not a member of any exclusive subject in the policy.
     *
     * @param name name of the Subject instance 
     * @param subject Subject object to be added to the policy
     *
     * @param exclusive boolean flag indicating whether the subject 
     *        is to be exclusive subject. If subject is exclusive, 
     *        policy applies to users who are not members of the 
     *        subject. Otherwise, policy applies to members of the subject.
     *
     * @exception NameAlreadyExistsException if a Subject with the given name
     *          already exists
     * @exception InvalidNameException if the subject name is invalid
     *
     * @supported.api
     *
     */
    public void addSubject(String name, Subject subject, boolean exclusive) 
            throws NameAlreadyExistsException, InvalidNameException {
        users.addSubject(name, subject, exclusive);
    }

    /**
     * Replaces an existing subject with the same name by the
     * current one. If a subject with the same name does not exist,
     * it will be added.
     * The subject is replaced as a normal (non exclusive) subject.
     * So, policy will apply to members of the subject.
     * The policy will apply to a user if he is a member of 
     * any normal (non exclusive) subject subject in the policy 
     * or not a member of any exclusive subject subject in the policy.
     *
     * @param name name of the Subject instance 
     * @param subject Subject that will replace an existing Subject
     *        with the same name
     *
     * @exception NameNotFoundException if a Subject instance
     *        with the given name is not present
     *
     * @supported.api
     *
     */
    public void replaceSubject(String name, Subject subject) 
            throws NameNotFoundException {
        users.replaceSubject(name, subject, false);
    }

    /**
     * Replaces an existing subject with the same name by the
     * current one. If a subject with the same name does not exist,
     * it will be added.
     * The policy will apply to a user if he is a member of 
     * any normal (non exclusive) subject in the policy 
     * or not a member of any exclusive subject in the policy.
     *
     * @param name name of the Subject instance 
     * @param subject Subject that will replace an existing Subject
     * with the same name
     *
     * @param exclusive boolean flag indicating whether the subject 
     *        is to be exclusive subject. If subject is exclusive, 
     *        policy applies to users who are not members of the 
     *        subject. Otherwise, policy applies to members of the subject.
     *
     * @exception NameNotFoundException if a Subject instance
     *        with the given name is not present
     *
     * @supported.api
     *
     */
    public void replaceSubject(String name, Subject subject, boolean exclusive) 
            throws NameNotFoundException {
        users.replaceSubject(name, subject, exclusive);
    }

    /**
     * Removes the subject with the given name. 
     *
     * @param subjectName name of the Subject
     *
     * @return returns the Subject object being removed.
     *         if not present returns <code>null</code>
     *
     * @supported.api
     *
     */
    public Subject removeSubject(String subjectName) {
        return users.removeSubject(subjectName);
    }

    /**
     * Removes the <code>Subject</code> object identified by
     * object's <code>equals</code> method. If a Subject instance
     * does not exist, the method will return silently.
     *
     * @param subject Subject object that
     *        will be removed from the user collection
     *
     * @supported.api
     *
     */
    public void removeSubject(Subject subject) {
        String subjectName = users.getSubjectName(subject);
        if (subjectName != null) {
            removeSubject(subjectName);
        }
    }


    /**
     * Checks if the subject is exclusive. 
     * If subject is exclusive, policy applies to users who are not members of
     * the subject. Otherwise, policy applies to members of the subject.
     * The policy will apply to a user if he is a member of 
     * any normal (non exclusive) subject in the policy
     * or not a member of any exclusive subject in the policy.
     *
     * @param subjectName name of the subject 
     * @return <code>true</code> if the subject is exclusive, <code>false</code>
     *         otherwise.
     * @exception NameNotFoundException if the subject with the given
     *         <code>subjectName</code> does not exist in the policy.
     *
     * @supported.api
     *
     */
    public boolean isSubjectExclusive(String subjectName) 
            throws NameNotFoundException {
        return users.isSubjectExclusive(subjectName);
    }

    /**
     * Checks if the subjectName is a reference to a Subject 
     * defined at the realm
     *
     * @param subjectName name of the subject 
     * @return <code>true</code> if the subject is a reference to a 
     *         Subject defined at the realm, <code>false</code>
     *        otherwise.
     * @exception NameNotFoundException if the subject with the given
     *         <code>subjectName</code> does not exist in the policy.
     *
     * @supported.api
     *
     */
    public boolean isRealmSubject(String subjectName) 
            throws NameNotFoundException {
        return users.isRealmSubject(subjectName);
    }

    /**
     * Returns a <code>Referrals</code> object that contains
     * a set of <code>Referral</code> instances for whom the
     * policy is applied.
     *
     * @return Referrals object of the policy
     */
    Referrals getReferrals() {
        return (referrals);
    }

    /**
     * Get the <code>Set</code> of referral names associated with the policy.
     *
     * @return <code>Set</code> of referral names
     *
     * @supported.api
     *
     */
    public Set getReferralNames() {
        return referrals.getReferralNames();
    }

    /**
     * Gets the Referral object identified by name.
     *
     * @param referralName name of referral.
     *
     * @return <code>Referral</code> object
     *
     * @exception NameNotFoundException if a Referral with the given name
     * does not exist
     *
     * @supported.api
     *
     */
    public Referral getReferral(String referralName) throws 
        NameNotFoundException 
    {
        return referrals.getReferral(referralName);
    }

    /**
     * Adds a new policy referral.
     *
     * @param name name of the <code>Referral</code> instance 
     * @param referral <code>Referral</code> object to be added to the policy
     *
     * @exception NameAlreadyExistsException if a Referral with the given name
     *            already exists
     * @exception InvalidNameException if the referral name is invalid
     *
     * @supported.api
     *
     */
    public void addReferral(String name, Referral referral) 
            throws NameAlreadyExistsException, InvalidNameException {
        referrals.addReferral(name, referral);
    }

    /**
     * Replaces an existing referral with the same name by the
     * current one. If a referral with the same name does not exist,
     * it will be added.
     *
     * @param name name of the <code>Referral</code> instance 
     * @param     referral <code>Referral</code> that will replace an existing 
     *            Referral with the same name
     *
     * @exception NameNotFoundException if a Referral instance
     *            with the given name is not present
     *
     * @supported.api
     *
     */
    public void replaceReferral(String name, Referral referral) 
            throws NameNotFoundException {
        referrals.replaceReferral(name, referral);
    }

    /**
     * Removes the referral with the given name. 
     *
     * @param referralName name of the <code>Referral</code>
     *
     * @return returns the <code>Referral</code> object being removed;
     *         if not present returns <code>null</code>
     *
     * @supported.api
     *
     */
    public Referral removeReferral(String referralName) {
        return referrals.removeReferral(referralName);
    }
    /**
     * Removes the <code>Referral</code> object identified by
     * object's <code>equals</code> method. If a Referral instance
     * does not exist, the method will return silently.
     *
     * @param referral Referral object that will be removed 
     *
     * @supported.api
     *
     */
    public void removeReferral(Referral referral) {
        String referralName = referrals.getReferralName(referral);
        if (referralName != null) {
            removeReferral(referralName);
        }
    }


    /**
     * Returns a <code>Conditions</code> object that contains
     * a set of <code>Condition</code> objects that apply
     * to the policy
     *
     * @return <code>Conditions</code> object of the policy
     */
    Conditions getConditions() {
        return (conditions);
    }

    /**
     * Get the set of condition names associated with the policy.
     *
     * @return <code>Set</code> of condition names
     *
     * @supported.api
     *
     */
    public Set getConditionNames() {
        return conditions.getConditionNames();
    }

    /**
     * Gets the condition object identified by name.
     *
     * @param condition name of condition.
     *
     * @return <code>Condition</code> object.
     *
     * @exception NameNotFoundException if a Condition with the given name
     * does not exist.
     *
     * @supported.api
     *
     */
    public Condition getCondition(String condition) throws 
        NameNotFoundException 
    {
        return conditions.getCondition(condition);
    }

    /**
     * Adds a new policy condition.
     *
     * @param name name of the Condition instance 
     * @param condition Condition object to be added to the policy
     *
     * @exception NameAlreadyExistsException if a Condition with the given name
     *            already exists
     * @exception InvalidNameException if the condition name is invalid
     *
     * @supported.api
     *
     */
    public void addCondition(String name, Condition condition) 
            throws NameAlreadyExistsException, InvalidNameException {
        conditions.addCondition(name, condition);
    }

    /**
     * Replaces an existing condition with the same name by the
     * current one. If a condition with the same name does not exist,
     * it will be added.
     *
     * @param name name of the <code>Condition</code> instance 
     * @param     condition <code>Condition</code> that will replace an 
     *            existing Condition with the same name
     *
     * @exception NameNotFoundException if a Condition instance
     *            with the given name is not present
     *
     * @supported.api
     *
     */
    public void replaceCondition(String name, Condition condition) 
            throws NameNotFoundException {
        conditions.replaceCondition(name, condition);
    }

    /**
     * Removes the condition with the given name. 
     *
     * @param condition name of the <code>Condition</code>
     *
     * @return returns the Condition object being removed;
     *         if not present returns <code>null</code>
     *
     * @supported.api
     *
     */
    public Condition removeCondition(String condition) {
        return conditions.removeCondition(condition);
    }

    /**
     * Removes the <code>Condition</code> object identified by
     * object's <code>equals</code> method. If a condition instance
     * does not exist, the method will return silently.
     *
     * @param condition Condition object that will be removed 
     *
     * @supported.api
     *
     */
    public void removeCondition(Condition condition) {
        String conditionName = conditions.getConditionName(condition);
        if (conditionName != null) {
            removeCondition(conditionName);
        }
    }


    /**
     * Returns a <code>ResponseProviders</code> object that contains
     * a set of <code>ResponseProvider</code> objects that apply
     * to the policy
     *
     * @return <code>ResponseProviders</code> object found in the policy
     */
    ResponseProviders getResponseProviders() {
        return (respProviders);
    }

    /**
     * Get a <code>Set</code> of <code>String</code> objects representing
     * the responseProvider names associated with the policy.
     *
     * @return <code>Set</code> of responseProvider names
     *
     *
     */
    public Set getResponseProviderNames() {
        return respProviders.getResponseProviderNames();
    }

    /**
     * Gets the <code>ResponseProvider</code> object identified by name.
     *
     * @param respProvider name of <code>ResponseProvider</code>.
     *
     * @return <code>ResponseProvider</code> object.
     *
     * @exception NameNotFoundException if a ResponseProvider with the given 
     *            name does not exist.
     *
     *
     */
    public ResponseProvider getResponseProvider(String respProvider) 
        throws NameNotFoundException {
        return respProviders.getResponseProvider(respProvider);
    }

    /**
     * Adds a new <code>ResponseProvider</code> to the policy.
     *
     * @param name name of the <code>ResponseProvider</code> instance 
     * @param respProvider <code>ResponseProvider</code> object to be added to 
     *            the policy
     *
     * @exception NameAlreadyExistsException if a ResponseProvider with the 
     *            given name already exists
     * @exception InvalidNameException if the <code>respProvider</code>
     *             name is invalid
     *
     *
     */
    public void addResponseProvider(String name, ResponseProvider respProvider) 
            throws NameAlreadyExistsException {
        respProviders.addResponseProvider(name, respProvider);
    }

    /**
     * Replaces an existing <code>ResponseProvider</code> with the same name 
     * by the current one. If a respProvider with the same name does not exist,
     * it will be added.
     *
     * @param name name of the ResponseProvider instance 
     * @param respProvider ResponseProvider that will replace an existing 
     *            ResponseProvider with the same name
     *
     * @exception NameNotFoundException if a ResponseProvider instance
     *            with the given name is not present.
     *
     *
     */
    public void replaceResponseProvider(String name, 
        ResponseProvider respProvider) throws NameNotFoundException {
        respProviders.replaceResponseProvider(name, respProvider);
    }

    /**
     * Removes the <code>ResponseProvider</code> with the given name. 
     *
     * @param respProvider name of the ResponseProvider
     *
     * @return returns the ResponseProvider object being removed;
     * if not present returns null.
     *
     *
     */
    public ResponseProvider removeResponseProvider(String respProvider) {
        return respProviders.removeResponseProvider(respProvider);
    }

    /**
     * Removes the <code>ResponseProvider</code> object.
     * If a respProvider instance does not exist, the method will 
     * return silently.
     *
     * @param respProvider ResponseProvider object that
     * will be removed 
     *
     *
     */
    public void removeResponseProvider(ResponseProvider respProvider) {
        String respProviderName = respProviders.getResponseProviderName(
            respProvider);
        if (respProviderName != null) {
            removeResponseProvider(respProviderName);
        }
    }

    /**
     * Stores the policy object in a persistent data store
     * under the organization, sub-organization or a container
     * object, specified as a parameter. The organization,
     * sub-organization, or the container can be either
     * a LDAP distinguished name (<code>dn</code>) or slash "/" separated
     * as per SMS. This method
     * uses the <code>SSOToken</code> provided to perform the store
     * operation, and hence if the single sign token has expired
     * <code>SSOException</code> will be thrown, and if the
     * user does not have the required privileges
     * <code>NoPermissionException</code> exception will be thrown.
     * <p>
     * If a policy with the same name exists for the organization
     * the method will throw <code>NameAlreadyExistsException</code>.
     * And if the organization name does not exist, the method
     * will throw <code>NameNotFoundException</code>.
     *
     * @param token  SSO token of the user managing policy
     * @param name name of the organization, sub-organization or
     * a container in which the policy will be stored.
     *
     * @exception SSOException invalid or expired single-sign-on token
     * @exception NoPermissionException user does not have sufficient
     * privileges to add policy
     *
     * @exception NameAlreadyExistsException a policy with the same
     * name already exists
     *
     * @exception NameNotFoundException the given organization name
     * does not exist
     *
     * @exception PolicyException for any other abnormal condition
     *
     * @supported.api
     *
     */
    public void store(SSOToken token, String name) throws SSOException,
        NoPermissionException, NameAlreadyExistsException,
        NameNotFoundException, PolicyException {
        PolicyManager pm = new PolicyManager(token, name);
        pm.addPolicy(this);
    }

    /**
     * Checks if two policy objects are equal.
     * This method does not check the policy name and description 
     * for equality.
     *
     * @param obj object againt which the policy object
     * will be checked for equality
     *
     * @return <code>true</code> if policies are equal,
     * <code>false</code> otherwise.
     */
    public boolean equals(Object obj) {
        if (obj instanceof Policy) {
            Policy p = (Policy) obj;
            if (rules.equals(p.rules) && users.equals(p.users)
                        && referrals.equals(p.referrals)
                        && respProviders.equals(p.respProviders)
                        && conditions.equals(p.conditions) ) {
                return (true);
            }
        }
        return (false);
    }

    /**
     * Creates and returns a copy of this object. The returned
     * <code>Policy</code> object will have the same policy
     * name, rules, subjects, referrals and conditions
     * such that <code>x.clone().equals(x)</code> will be
     * <code>true</code>. However <code>x.clone()</code>
     * will not be the same as <code>x</code>, i.e.,
     * <code>x.clone() != x</code>.
     *
     * @return a copy of this object
     */
    public Object clone() {
        Policy answer = null;
        try {
            answer = (Policy) super.clone();
        } catch (CloneNotSupportedException se) {
            answer = new Policy();
        }
        // Copy state variables
        answer.origPolicyName = origPolicyName;
        answer.policyName = policyName;
        answer.description = description;
        answer.active = active;

        // Copy rules
        answer.rules = new HashMap();
        Iterator items = rules.keySet().iterator();
        while (items.hasNext()) {
            Object o = items.next();
            Rule rule = (Rule) rules.get(o);
            answer.rules.put(o, rule.clone());
        }

        // Copy subjects
        answer.users = (Subjects) users.clone();

        // Copy referrals
        answer.referrals = (Referrals) referrals.clone();

        // Copy responseProviders
        answer.respProviders = (ResponseProviders) respProviders.clone();

        // Copy conditions
        answer.conditions = (Conditions) conditions.clone();

        return (answer);
    }

    /**
     * Returns the serialized policy in XML 
     * @return serialized policy in XML
     *
     * @supported.api
     *
     */
    public String toXML() {
        return toXML(true);
    }

    public String toXML(boolean withHeader) {
        StringBuilder answer = new StringBuilder(200);

        if (withHeader) {
            answer.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        }

        answer.append("<Policy name=\"");
        answer.append(XMLUtils.escapeSpecialCharacters(policyName));
        if ((description != null) && (description.length() > 0)) {
            answer.append("\" description=\"");
            answer.append(XMLUtils.escapeSpecialCharacters(description));
        }

        if ((createdBy != null) && (createdBy.length() > 0)) {
            answer.append("\" ")
                .append(PolicyManager.CREATED_BY_ATTRIBUTE)
                .append("=\"")
                .append(XMLUtils.escapeSpecialCharacters(createdBy));
        }
        if ((lastModifiedBy != null) && (lastModifiedBy.length() > 0)) {
            answer.append("\" ")
                .append(PolicyManager.LAST_MODIFIED_BY_ATTRIBUTE)
                .append("=\"")
                .append(XMLUtils.escapeSpecialCharacters(lastModifiedBy));
        }
        if (creationDate > 0) {
            answer.append("\" ")
                .append(PolicyManager.CREATION_DATE_ATTRIBUTE)
                .append("=\"")
                .append(XMLUtils.escapeSpecialCharacters(
                    Long.toString(creationDate)));
        }
        if (lastModifiedDate > 0) {
            answer.append("\" ")
                .append(PolicyManager.LAST_MODIFIED_DATE_ATTRIBUTE)
                .append("=\"")
                .append(XMLUtils.escapeSpecialCharacters(
                    Long.toString(lastModifiedDate)));
        }

        answer.append("\" referralPolicy=\"").append(referralPolicy);
        answer.append("\" active=\"").append(active);
        answer.append("\" >");
        for (Iterator i = getRuleNames().iterator(); i.hasNext(); ) {
            String ruleName = (String)i.next();
            try {
                Rule rule = getRule(ruleName);
                answer.append(rule.toXML());
            } catch (Exception e) {
                // Ignore the exception
                DEBUG.error("Error in policy.toXML():" + e.getMessage());
            }
        }

        if (!this.referralPolicy) {
            // Add the users
            if ( !(users.getSubjectNames().isEmpty()) ) {
                answer.append(users.toXML());
            }

            // Add the conditions
            if ( !(conditions.getConditionNames().isEmpty()) ) {
                answer.append(conditions.toXML());
            }
            // Add the responseProviders
            if ( !(respProviders.getResponseProviderNames().isEmpty()) ) {
                answer.append(respProviders.toXML());
            }
        } else {
            // Add the referrals
            if ( !(referrals.getReferralNames().isEmpty()) ) {
                answer.append(referrals.toXML());
            }
        }

        answer.append("\n").append("</Policy>");
        return (answer.toString());
    }

    /**
     * Gets string representation of the policy object.
     *
     * @return XML string representation of the policy object
     *
     * @supported.api
     */
    public String toString() {
        return (toXML());
    }

    /**
     * Checks for the char <code>c</code> in the String
     * @param name String in which the character needs to be checked for.
     * @param c <code>char</code> which needs to be checked.
     * @exception InvalidNameException if <code>c</code> does not occur
     *            anywhere in <code>name</code>.
     */
    static void checkForCharacter(String name, char c)
        throws InvalidNameException {
        if (name.indexOf(c) != -1) {
            Character objs[] =  { new Character(c) };
            throw (new InvalidNameException(ResBundleUtils.rbName,
                "invalid_char_in_name", objs, name,
                PolicyException.POLICY));
        }
    }

    /** 
     * Gets policy decision 
     * @param token sso token identifying the user for who the policy has to 
     *        be evaluated.
     * @param resourceTypeName resourceType name
     * @param resourceName resourceName
     * @param actionNames a set of action names for which policy results
     *        are to be evaluated. Each element of the set should be a
     *        String
     * @param envParameters a  <code>Map</code> of environment parameters
     *        Each key of the <code>Map</code> is a String valued parameter name
     *        Each value of the map is a <code>Set</code> of String values
     * @return a <code>PolicyDecision</code>
     * @exception NameNotFoundException if the action name or resource name
     *         is not found
     * @exception SSOException if token is invalid
     * @exception PolicyException for any other exception condition
     */
   public PolicyDecision getPolicyDecision(SSOToken token, 
            String resourceTypeName,String resourceName, Set actionNames, 
            Map envParameters) throws SSOException, NameNotFoundException, 
            PolicyException 
   {

        PolicyDecision policyDecision = new PolicyDecision();

        ServiceTypeManager stm = ServiceTypeManager.getServiceTypeManager();
        ServiceType resourceType 
                = stm.getServiceType(resourceTypeName);

        /**
         * get the evaluation order that is likely to be least expensive 
         * in terms of cpu.
         */
        if (token != null) {
            evaluationOrder = getEvaluationOrder(token);
        } else {
            evaluationOrder = SUBJECTS_RULES_CONDITIONS;
        }
        if (DEBUG.messageEnabled()) {
            DEBUG.message("Policy " + getName() 
                + " is Using Policy evaluation order :" + evaluationOrder);
        }
        if (isReferralPolicy() && !referrals.isEmpty()) {

            //process referrals irrespective subjects and conditions
            PolicyDecision referralDecision 
                    = referrals.getPolicyDecision(token, 
                    resourceTypeName, resourceName, actionNames, 
                    envParameters);
            if (referralDecision != null) {
                PolicyEvaluator.mergePolicyDecisions(
                        resourceType, referralDecision, policyDecision);
            } 
            if (DEBUG.messageEnabled()) {
                String tokenPrincipal = 
                        (token != null) ? token.getPrincipal().getName()
                        : PolicyUtils.EMPTY_STRING;
                DEBUG.message(
                    new StringBuffer("at Policy.getPolicyDecision()")
                    .append(" after processing referrals only:")
                    .append(" principal, resource name, action names,")
                    .append(" policyName, referralResults = ")
                    .append(tokenPrincipal) .append(",  ")
                    .append(resourceName) .append(",  ")
                    .append(actionNames) .append(",  ")
                    .append(this.getName()).append(",  ")
                    .append(referralDecision).toString());
            }
        } else if (evaluationOrder == SUBJECTS_CONDITIONS_RULES) {
            if (DEBUG.messageEnabled()) {
                DEBUG.message("Using policy evaluation order:" 
                        + "SUBJECTS_CONDITIONS_RULES");
            }
            getPolicyDecisionSCR(token, resourceType, resourceName, 
                    actionNames, envParameters, policyDecision);
        } else if (evaluationOrder == CONDITIONS_SUBJECTS_RULES) {
            if (DEBUG.messageEnabled()) {
                DEBUG.message("Using policy evaluation order:" 
                        + "CONDITIONS_SUBJECTS_RULES");
            }
            getPolicyDecisionCSR(token, resourceType, resourceName, 
                    actionNames, envParameters, policyDecision);
        }  else if (evaluationOrder == RULES_SUBJECTS_CONDITIONS) {
            if (DEBUG.messageEnabled()) {
                DEBUG.message("Using policy evaluation order:" 
                        + "RULES_SUBJECTS_CONDITIONS");
            }
            getPolicyDecisionRSC(token, resourceType, resourceName, 
                    actionNames, envParameters, policyDecision);
        }  else if (evaluationOrder == RULES_CONDITIONS_SUBJECTS) {
            if (DEBUG.messageEnabled()) {
                DEBUG.message("Using policy evaluation order:" 
                        + "RULES_CONDITIONS_SUBJECTS");
            }
            getPolicyDecisionRCS(token, resourceType, resourceName, 
                    actionNames, envParameters, policyDecision);
        }  else if (evaluationOrder == SUBJECTS_RULES_CONDITIONS) {
            if (DEBUG.messageEnabled()) {
                DEBUG.message("Using policy evaluation order:" 
                        + "SUBJECTS_RULES_CONDITIONS");
            }
            getPolicyDecisionSRC(token, resourceType, resourceName, 
                    actionNames, envParameters, policyDecision);
        }  else if (evaluationOrder == CONDITIONS_RULES_SUBJECTS) {
            if (DEBUG.messageEnabled()) {
                DEBUG.message("Using policy evaluation order:" 
                        + "CONDITIONS_RULES_SUBJECTS");
            }
            getPolicyDecisionCRS(token, resourceType, resourceName, 
                    actionNames, envParameters, policyDecision);
        }  else { //default:RULES_CONDITIONS_SUBJECTS
            if (DEBUG.messageEnabled()) {
                DEBUG.message("Using default policy evaluation order:" 
                        + "RULES_CONDITIONS_SUBJECTS");
            }
            getPolicyDecisionRCS(token, resourceType, resourceName, 
                    actionNames, envParameters, policyDecision);
        }

        if (DEBUG.messageEnabled()) {
            String tokenPrincipal = 
                    (token != null) ? token.getPrincipal().getName()
                    : PolicyUtils.EMPTY_STRING;
            DEBUG.message(
                new StringBuffer("at Policy.getPolicyDecision()")
                .append(" principal, resource name, action names,")
                .append(" policyName, policyDecision = ")
                .append(tokenPrincipal) .append(",  ")
                .append(resourceName) .append(",  ")
                .append(actionNames) .append(",  ")
                .append(this.getName()).append(",  ")
                .append(policyDecision).toString());
        }
        Map actionDecisionMap = policyDecision.getActionDecisions();
        if (actionDecisionMap != null && !actionDecisionMap.isEmpty())
        {
            Collection actionDecisions = null;
            if ((actionDecisions = actionDecisionMap.values()) != null &&
                !actionDecisions.isEmpty()) {
                Iterator it = actionDecisions.iterator();
                while (it.hasNext()) {
                    Set actionValues = ((ActionDecision)it.next()).getValues();
                    if (actionValues != null && !actionValues.isEmpty())
                        { // put the response Attrs in the PolicyDecision
                        Map responseAttributes = 
                           respProviders.getResponseProviderDecision(token, 
                                envParameters);
                            policyDecision.setResponseAttributes(
                                responseAttributes);
                        break;/**
                               * even if one action Value found, set the 
                               * resp attributes
                               */
                    }
                }
            }
        }
        return policyDecision;
    }

    /** Gets matched rule results given resource type, resource name and 
     *  action names
     *  @param resourceType resource type(<code>ServiceType</code> of resource
     *  @param resourceName resource name for which to get action values
     *  @param actionNames action names for which to get values
     *  @return <code>Map</code> of action values keyed by action names
     *  @exception NameNotFoundException
     */
    private Map getMatchedRuleResults(ServiceType resourceType,
            String resourceName, Set actionNames) throws NameNotFoundException {
        String resourceTypeName = resourceType.getName();
        Map answer = null;
        StringBuilder cacheKeyBuffer = new StringBuilder(100);
        String cacheKey = cacheKeyBuffer.append(resourceTypeName)
                .append(resourceName).append(actionNames).toString();
        answer = (Map) matchRulesResultsCache.get(cacheKey);
        if ( answer == null ) {
            answer = new HashMap();

            //Process rules
            Iterator ruleIterator = rules.values().iterator();
            while (ruleIterator.hasNext()) {
                Rule rule = (Rule) ruleIterator.next();
                Map actionResults = rule.getActionValues(resourceTypeName, 
                        resourceName, actionNames);
                PolicyUtils.appendMapToMap(actionResults, answer);
            }

            Iterator actions = answer.keySet().iterator();
            while ( actions.hasNext() ) {
                String action = (String) actions.next();
                Set actionValues = (Set) answer.get(action);
                if ( actionValues.size() == 2 ) {
                    ActionSchema actionSchema = null;
                    AttributeSchema.Syntax actionSyntax = null;
                    try {
                        actionSchema = resourceType.getActionSchema(action);
                        actionSyntax = actionSchema.getSyntax();
                    } catch(InvalidNameException e) {
                        PolicyManager.debug.error(
                            "can not find action schmea for action = " 
                            + action, e );
                    }
                    if (AttributeSchema.Syntax.BOOLEAN.equals(
                            actionSyntax)) {
                        String trueValue = actionSchema.getTrueValue();
                        actionValues.remove(trueValue);
                    }
                }
            }

            // Add to cache
            matchRulesResultsCache.put(cacheKey, answer);
        }
        return cloneRuleResults(answer);
    }

    /**Gets resource names that are exact matches, sub resources or 
     * wild card matches of argument resource name.
     * To determine whether to include a
     * resource name of a resource,  we compare argument resource name and 
     * policy resource name, treating wild characters in the policy 
     * resource name as wild. If the comparsion resulted in EXACT_MATCH,
     * WILD_CARD_MATCH or SUB_RESOURCE_MATCH, the resource result would be
     * included.
     *
     * @param token sso token
     * @param serviceTypeName service type name
     * @param resourceName resource name
     * @param followReferrals indicates whether to follow the referrals to
     *                        compute the resources
     * @return resource names that match to be exact match, sub 
     *         resource match or wild card match of the argument
     *         resourceName
     *
     * @exception PolicyException
     * @exception SSOException
     *
     * @see ResourceMatch#EXACT_MATCH
     * @see ResourceMatch#SUB_RESOURCE_MATCH
     * @see ResourceMatch#WILDCARD_MATCH
     *
     */
    Set getResourceNames(SSOToken token, String serviceTypeName,
            String resourceName, boolean followReferrals) 
            throws PolicyException, SSOException {
        Set resourceNames = new HashSet();
        ServiceType st = ServiceTypeManager.getServiceTypeManager()
                .getServiceType( serviceTypeName);
        Iterator ruleIterator = rules.values().iterator();
        while (ruleIterator.hasNext()) {
            Rule rule = (Rule) ruleIterator.next();
            if (rule.getServiceType().getName().equals(serviceTypeName)) {
                String ruleResource = rule.getResourceName();
                ResourceMatch resourceMatch = st.compare(resourceName,
                        ruleResource, true); // interpret wild char
                if (resourceMatch.equals(ResourceMatch.SUB_RESOURCE_MATCH)
                      || resourceMatch.equals(ResourceMatch.EXACT_MATCH)
                      || resourceMatch.equals(ResourceMatch.WILDCARD_MATCH)) {
                    resourceNames.add(ruleResource);
                } 
            if ( DEBUG.messageEnabled() ) {
                StringBuilder sb = new StringBuilder(200);
                sb.append("at Policy.getResourceNames : ");
                sb.append(" for policyName, serviceType, resourceName, ");
                sb.append(" ruleResource, resourceMatch :");
                sb.append(getName()).append( ",").append( serviceTypeName);
                sb.append(",").append(resourceName).append(",");
                sb.append(ruleResource).append(",").append(resourceMatch);
                DEBUG.message(sb.toString());
            }
            }
        }
        if (!resourceNames.isEmpty() && followReferrals) {
            Set rResourceNames = referrals.getResourceNames(token, 
                    serviceTypeName, resourceName);
            resourceNames.addAll(rResourceNames);
        }
        if ( DEBUG.messageEnabled() ) {
            StringBuilder sb = new StringBuilder(200);
            sb.append("at Policy.getResourceNames : ");
            sb.append(" for policyName, serviceType, resourceName, ");
            sb.append(" followReferral, resourceNames :");
            sb.append(getName()).append( ",").append( serviceTypeName);
            sb.append(",").append(resourceName).append(",");
            sb.append(followReferrals).append(",").append(resourceNames);
            DEBUG.message(sb.toString());
        }
        return resourceNames;
    }

    /** Gets the resource names of a given serviceType managed by this
     *  policy.
     *  @param  serviceTypeName name of service type for which to 
     *          find resource names
     *  @return a set of resource names of serviceTypeName managed
     *          by this policy
     *  @exception SSOException
     *  @exception NameNotFoundException
     */
    Set getResourceNames(String serviceTypeName) throws SSOException,
            NameNotFoundException {
        Set resourceNames = new HashSet();
        Iterator ruleIterator = rules.values().iterator();
        while (ruleIterator.hasNext()) {
            Rule rule = (Rule) ruleIterator.next();
            String rSvcTypeName = (rule.getServiceType() == null) ?
                rule.getServiceTypeName() : rule.getServiceType().getName();
            if (rSvcTypeName.equals(serviceTypeName)) {
                String ruleResource = rule.getResourceName();
                resourceNames.add(ruleResource);
            }
        }
        return resourceNames;
    }

//    public String getServiceTypeName() {
        /* com.iplanet.am.admin.cli uses this method. 
         * Need to clean up cli not to use this 
         * method. Without this method build breaks - 03/05/02 */
 //       return null;
  //  }

    /**
     *  Gets organizations referred to in this policy by OrgReferral(s)
     *  defined in this policy.
     *  
     *  @return names of organization (DNs) of organizations referred
     *          to in this policy via <code>OrgReferral</code>(s) defined in 
     *          this policy.
     *          Please note that <code>PeerOrgReferral</code> and 
     *          <code>SubOrgReferral</code> extend <code>OrgReferral</code> 
     *          and hence qualify as OrgReferral.
     *  @exception PolicyException
     */
    Set getReferredToOrganizations() throws PolicyException {
        Set referredToOrgs = new HashSet();
        Iterator referralNames = referrals.getReferralNames().iterator();
        while ( referralNames.hasNext() ) {
            String referralName = (String) referralNames.next();
            Referral referral = (Referral) referrals.getReferral(referralName);
            if ( referral instanceof OrgReferral ) {
                Set values = referral.getValues();
                if ( (values != null) && (!values.isEmpty()) ) {
                    String orgName = (String) values.iterator().next();
                    referredToOrgs.add(orgName.toLowerCase());
                }
            }
        }
        return referredToOrgs;
    }

    /** Sets time to live for Subjects result. 
     *  @param ttl time to live for Subjects result
     */
    void setSubjectsResultTtl(long ttl) {
        users.setResultTtl(ttl);
    }


    /**
     * validates the String <code>name</code>.
     * @param name String to be validated.
     * @exception throws InvalidNameException is name is null or
     *            does contain invalid character "/".
     */

    private void validateName(String name) throws InvalidNameException {
        if ( (name == null) || (name.length() == 0) )  {
            DEBUG.message("Invalid policy name:" + name);
            throw (new InvalidNameException(ResBundleUtils.rbName,
                "null_name", null, "", PolicyException.POLICY));
        } else if ( name.indexOf('/') != -1 ) {
            DEBUG.message("Invalid policy name:" + name );
            DEBUG.message("Index Of /:" + name.indexOf('/'));
            throw (new InvalidNameException(ResBundleUtils.rbName,
                "illegal_character_/_in_name", null, "", 
                PolicyException.POLICY));
        }
    }

    /** Gets policy decision  computing Subjects, Conditions and Rules
     *  in this order. Referrals in the policy are ignored.
     *
     * @param token sso token identifying the user for who the policy has to 
     *        be evaluated.
     * @param resourceType service type
     * @param resourceName resource name
     * @param actionNames a set of action names for which policy results
     *        are to be evaluated. Each element of the set should be a
     *        String
     * @param envParameters a map of environment parameters
     *        Each key of the map is a String valued parameter name
     *        Each value of the map is a set of String values
     * @param policyDecision a collecting argument. Computed policy decisions
     *        in this method are merged to this policy decision
     * @return computed and merged policy decision
     * @exception NameNotFoundException if the action name or resource name
     *         is not found
     * @exception SSOException if token is invalid
     * @exception PolicyException for any other exception condition
     */
    private PolicyDecision getPolicyDecisionSCR(SSOToken token, 
            ServiceType resourceType,
            String resourceName, Set actionNames, Map envParameters,
            PolicyDecision policyDecision) 
            throws SSOException, NameNotFoundException, PolicyException {
        boolean resourceMatched = false;
        ConditionDecision conditionDecision = null;
        boolean allowedByConditions = false;
        boolean allowedBySubjects = false;
        Map advicesFromConditions = null;
        long conditionsTtl = Long.MIN_VALUE;
        long timeToLive = Long.MIN_VALUE;
        Map actionResults = null;

        if (token != null) {
            allowedBySubjects = users.isMember(token);
            timeToLive = users.getResultTtl(token);
        } else {
            allowedBySubjects = true;
            timeToLive = Long.MAX_VALUE;
        }

        if (allowedBySubjects) { //subjects+ 
            conditionDecision = conditions.getConditionDecision(
                    token, envParameters);
            allowedByConditions = conditionDecision.isAllowed();
            advicesFromConditions = conditionDecision.getAdvices();
            conditionsTtl = conditionDecision.getTimeToLive();
            if ( conditionsTtl < timeToLive ) {
                timeToLive = conditionsTtl;
            }
            if (allowedByConditions) { //subjects+, conditions+
                actionResults = getMatchedRuleResults(resourceType,
                        resourceName, actionNames);
                resourceMatched = !actionResults.isEmpty();
                if (resourceMatched) { //subjects+,conditions+,resourceMatch+
                    Iterator resultActionNames 
                            = actionResults.keySet().iterator();
                    while ( resultActionNames.hasNext() ) {
                        String resultActionName 
                                = (String) resultActionNames.next();
                        Set resultActionValues 
                            = (Set)actionResults.get(resultActionName);

                        /*  ActionDecision to include values, no advices
                         */
                        ActionDecision actionDecision 
                                = new ActionDecision(resultActionName,
                                resultActionValues, 
                                advicesFromConditions, timeToLive);
                        policyDecision.addActionDecision(
                                actionDecision, resourceType);
                    }
                } else { // subjects+,conditions+,resourceMatch-
                    policyDecision.setTimeToLive(Long.MAX_VALUE);
                }
            } else { //subjects+,conditions-
                //ActionDecision to include advices only

                if (!advicesFromConditions.isEmpty()) {
                    actionResults = getMatchedRuleResults(resourceType,
                            resourceName, actionNames);
                    Iterator resultActionNames 
                            = actionResults.keySet().iterator();
                    while ( resultActionNames.hasNext() ) {
                        String resultActionName 
                                = (String) resultActionNames.next();

                        /*  ActionDecision to include advices, no values
                         */
                        ActionDecision actionDecision 
                                = new ActionDecision(resultActionName,
                                Collections.EMPTY_SET, 
                                advicesFromConditions, timeToLive);
                        policyDecision.addActionDecision(
                                actionDecision, resourceType);
                    }
                } else {
                    policyDecision.setTimeToLive(timeToLive);
                }
            }
        } else { //subjects-
            policyDecision.setTimeToLive(timeToLive);
        }
        return policyDecision;
    }

    /** Gets policy decision  computing Subjects, Rules and Conditions
     *  in this order. Referrals in the policy are ignored.
     *
     * @param token sso token identifying the user for who the policy has to 
     *        be evaluated.
     * @param resourceType service type
     * @param resourceName resourceName
     * @param actionNames a set of action names for which policy results
     *        are to be evaluated. Each element of the set should be a
     *        String
     * @param envParameters a map of environment parameters
     *        Each key of the map is a String valued parameter name
     *        Each value of the map is a set of String values
     * @param policyDecision a collecting argument. Computed policy decisions
     *        in this method are merged to this policy decision
     * @return computed and merged policy decision
     * @exception NameNotFoundException if the action name or resource name
     *         is not found
     * @exception SSOException if token is invalid
     * @exception PolicyException for any other exception condition
     */
    private PolicyDecision getPolicyDecisionSRC(SSOToken token, 
            ServiceType resourceType,
            String resourceName, Set actionNames, Map envParameters,
            PolicyDecision policyDecision) 
            throws SSOException, NameNotFoundException, PolicyException {
        boolean resourceMatched = false;
        ConditionDecision conditionDecision = null;
        boolean allowedByConditions = false;
        boolean allowedBySubjects = false;
        Map advicesFromConditions = null;
        long conditionsTtl = Long.MIN_VALUE;
        long timeToLive = Long.MIN_VALUE;
        Map actionResults = null;

        if (token != null) {
            allowedBySubjects = users.isMember(token);
            timeToLive = users.getResultTtl(token);
        } else {
            allowedBySubjects = true;
            timeToLive = Long.MAX_VALUE;
        }
        if (allowedBySubjects) { //subjects+
            actionResults = getMatchedRuleResults(resourceType,
                    resourceName, actionNames);
            resourceMatched = !actionResults.isEmpty();
            if (resourceMatched) { //subjects+, resourceMatch+
                conditionDecision = conditions.getConditionDecision(
                        token, envParameters);
                allowedByConditions = conditionDecision.isAllowed();
                advicesFromConditions = conditionDecision.getAdvices();
                conditionsTtl = conditionDecision.getTimeToLive();
                if ( conditionsTtl < timeToLive ) {
                    timeToLive = conditionsTtl;
                }
                if (allowedByConditions) { 
                    //subjects+, resourceMatch+,conditions+
                    Iterator resultActionNames 
                            = actionResults.keySet().iterator();
                    while ( resultActionNames.hasNext() ) {
                        String resultActionName 
                                = (String) resultActionNames.next();
                        Set resultActionValues 
                            = (Set)actionResults.get(resultActionName);

                        /*  ActionDecision to include values, no advices
                         */
                        ActionDecision actionDecision 
                                = new ActionDecision(resultActionName,
                                resultActionValues, 
                                advicesFromConditions, timeToLive);
                        policyDecision.addActionDecision(
                                actionDecision, resourceType);
                    }
                } else { //subjects+, resourceMatch+,conditions-
                    if (!advicesFromConditions.isEmpty()) {
                        Iterator resultActionNames 
                                = actionResults.keySet().iterator();
                        while ( resultActionNames.hasNext() ) {
                            String resultActionName 
                                    = (String) resultActionNames.next();

                            /*  ActionDecision to include advices, no values
                             */
                            ActionDecision actionDecision 
                                    = new ActionDecision(resultActionName,
                                    Collections.EMPTY_SET, 
                                    advicesFromConditions, timeToLive);
                            policyDecision.addActionDecision(
                                    actionDecision, resourceType);
                        }
                    } else {
                        policyDecision.setTimeToLive(timeToLive);
                    }
                }
            } else { //subjects+,resourceMatch-
                policyDecision.setTimeToLive(Long.MAX_VALUE);
            }
        } else { //subjects-
            policyDecision.setTimeToLive(timeToLive);
        }
        return policyDecision;
    }

    /** Gets policy decision  computing Conditions, Subject and Rules
     *  in this order. Referrals in the policy are ignored.
     *
     * @param token sso token identifying the user for who the policy has to 
     *        be evaluated.
     * @param resourceType service type
     * @param resourceName resourceName
     * @param actionNames a set of action names for which policy results
     *        are to be evaluated. Each element of the set should be a
     *        String
     * @param envParameters a map of environment parameters
     *        Each key of the map is a String valued parameter name
     *        Each value of the map is a set of String values
     * @param policyDecision a collecting arugment. Computed policy decisions
     *        in this method are merged to this policy decision
     * @return computed and merged policy decision
     * @exception NameNotFoundException if the action name or resource name
     *         is not found
     * @exception SSOException if token is invalid
     * @exception PolicyException for any other exception condition
     */
    private PolicyDecision getPolicyDecisionCSR(SSOToken token, 
            ServiceType resourceType,
            String resourceName, Set actionNames, Map envParameters,
            PolicyDecision policyDecision) 
            throws SSOException, NameNotFoundException, PolicyException {
        boolean resourceMatched = false;
        ConditionDecision conditionDecision = null;
        boolean allowedByConditions = false;
        boolean allowedBySubjects = false;
        Map advicesFromConditions = null;
        long timeToLive = Long.MIN_VALUE;
        long subjectsTtl = Long.MIN_VALUE;
        Map actionResults = null;

        conditionDecision = conditions.getConditionDecision(
                token, envParameters);
        allowedByConditions = conditionDecision.isAllowed();
        advicesFromConditions = conditionDecision.getAdvices();
        timeToLive = conditionDecision.getTimeToLive();
        if (allowedByConditions) { //conditions+
            allowedBySubjects = users.isMember(token);
            subjectsTtl = users.getResultTtl(token);
            if (subjectsTtl < timeToLive) {
                timeToLive = subjectsTtl;
            }
            if (allowedBySubjects) { //conditions+, subjects+
                actionResults = getMatchedRuleResults(resourceType,
                        resourceName, actionNames);
                resourceMatched = !actionResults.isEmpty();
                if (resourceMatched) { 
                    //conditions+, subjects+, resourceMatched+
                    Iterator resultActionNames 
                            = actionResults.keySet().iterator();
                    while ( resultActionNames.hasNext() ) {
                        String resultActionName 
                                = (String) resultActionNames.next();
                        Set resultActionValues 
                            = (Set)actionResults.get(resultActionName);

                        /*  ActionDecision to include values, no advices
                         */
                        ActionDecision actionDecision 
                                = new ActionDecision(resultActionName,
                                resultActionValues, 
                                advicesFromConditions, timeToLive);
                        policyDecision.addActionDecision(
                                actionDecision, resourceType);
                    }
                } else { //conditions+, subjects+, resourceMatched-
                    policyDecision.setTimeToLive(Long.MAX_VALUE);
                }
            } else { //conditions+,subjects-
                policyDecision.setTimeToLive(timeToLive);
            }
        } else { //conditions-
            boolean reportAdvices = false;
            if (!advicesFromConditions.isEmpty()) {
                reportAdvices = users.isMember(token);
                subjectsTtl = users.getResultTtl(token);
                if (subjectsTtl < timeToLive) {
                    timeToLive = subjectsTtl;
                }
            }
            if (reportAdvices) {
                actionResults = getMatchedRuleResults(resourceType,
                        resourceName, actionNames);
                Iterator resultActionNames 
                        = actionResults.keySet().iterator();
                while ( resultActionNames.hasNext() ) {
                    String resultActionName 
                            = (String) resultActionNames.next();

                    /*  ActionDecision to include advices, no values
                     */
                    ActionDecision actionDecision 
                            = new ActionDecision(resultActionName,
                            Collections.EMPTY_SET, 
                            advicesFromConditions, timeToLive);
                    policyDecision.addActionDecision(
                            actionDecision, resourceType);
                }
            } else { //no advices to report
                policyDecision.setTimeToLive(timeToLive);
            }
        }
        return policyDecision;
    }

    /** Gets policy decision  computing Conditions, Rules and Subjects
     *  in this order. Referrals in the policy are ignored.
     *
     * @param token sso token identifying the user for who the policy has to 
     *        be evaluated.
     * @param resourceType service type
     * @param resourceName resourceName
     * @param actionNames a set of action names for which policy results
     *        are to be evaluated. Each element of the set should be a
     *        String
     * @param envParameters a map of environment parameters
     *        Each key of the map is a String valued parameter name
     *        Each value of the map is a set of String values
     * @param policyDecision a collecting arugment. Computed policy decisions
     *        in this method are merged to this policy decision
     * @return computed and merged policy decision
     * @exception NameNotFoundException if the action name or resource name
     *         is not found
     * @exception SSOException if token is invalid
     * @exception PolicyException for any other exception condition
     */
    private PolicyDecision getPolicyDecisionCRS(SSOToken token, 
            ServiceType resourceType,
            String resourceName, Set actionNames, Map envParameters,
            PolicyDecision policyDecision) 
            throws SSOException, NameNotFoundException, PolicyException {
        boolean resourceMatched = false;
        ConditionDecision conditionDecision = null;
        boolean allowedByConditions = false;
        boolean allowedBySubjects = false;
        Map advicesFromConditions = null;
        long subjectsTtl = Long.MIN_VALUE;
        long timeToLive = Long.MIN_VALUE;
        Map actionResults = null;

        conditionDecision = conditions.getConditionDecision(
                token, envParameters);
        allowedByConditions = conditionDecision.isAllowed();
        advicesFromConditions = conditionDecision.getAdvices();
        timeToLive = conditionDecision.getTimeToLive();
        actionResults = getMatchedRuleResults(resourceType,
                resourceName, actionNames);
        if (allowedByConditions) { //conditions+
            resourceMatched = !actionResults.isEmpty();
            if (resourceMatched) { ///conditions+, resourceMatched+
                allowedBySubjects = users.isMember(token);
                subjectsTtl = users.getResultTtl(token);
                if (subjectsTtl < timeToLive) {
                    timeToLive = subjectsTtl;
                }
                if (allowedBySubjects) {

                    //conditions+, resourceMatched+, subjects+
                    Iterator resultActionNames 
                            = actionResults.keySet().iterator();
                    while ( resultActionNames.hasNext() ) {
                        String resultActionName 
                                = (String) resultActionNames.next();
                        Set resultActionValues 
                            = (Set)actionResults.get(resultActionName);

                        /*  ActionDecision to include values, no advices
                         */
                        ActionDecision actionDecision 
                                = new ActionDecision(resultActionName,
                                resultActionValues, 
                                advicesFromConditions, timeToLive);
                        policyDecision.addActionDecision(
                                actionDecision, resourceType);
                    }
                } else { //conditions+, resourceMatched+, subjects-
                    policyDecision.setTimeToLive(timeToLive);
                }
            } else { //conditions+, resourceMatched-
                policyDecision.setTimeToLive(Long.MAX_VALUE);
            }
        } else { //conditions-
            boolean reportAdvices = false;
            if (!advicesFromConditions.isEmpty()) {
                reportAdvices = users.isMember(token);
                subjectsTtl = users.getResultTtl(token);
                if (subjectsTtl < timeToLive) {
                    timeToLive = subjectsTtl;
                }
            }
            if (reportAdvices) {
                actionResults = getMatchedRuleResults(resourceType,
                        resourceName, actionNames);
                Iterator resultActionNames 
                        = actionResults.keySet().iterator();
                while ( resultActionNames.hasNext() ) {
                    String resultActionName 
                            = (String) resultActionNames.next();

                    /*  ActionDecision to include advices, no values
                     */
                    ActionDecision actionDecision 
                            = new ActionDecision(resultActionName,
                            Collections.EMPTY_SET, 
                            advicesFromConditions, timeToLive);
                    policyDecision.addActionDecision(
                            actionDecision, resourceType);
                }
            } else { //no advices to report
                policyDecision.setTimeToLive(timeToLive);
            }
        }
        return policyDecision;
    }

    /** Gets policy decision  computing Rules, Subjects and Conditions
     *  in this order. Referrals in the policy are ignored.
     *
     * @param token sso token identifying the user for who the policy has to 
     *        be evaluated.
     * @param resourceType service type
     * @param resourceName resourceName
     * @param actionNames a set of action names for which policy results
     *        are to be evaluated. Each element of the set should be a
     *        String
     * @param envParameters a map of environment parameters
     *        Each key of the map is a String valued parameter name
     *        Each value of the map is a set of String values
     * @param policyDecision a collecting arugment. Computed policy decisions
     *        in this method are merged to this policy decision
     * @return computed and merged policy decision
     * @exception NameNotFoundException if the action name or resource name
     *         is not found
     * @exception SSOException if token is invalid
     * @exception PolicyException for any other exception condition
     */
    private PolicyDecision getPolicyDecisionRSC(SSOToken token, 
            ServiceType resourceType,
            String resourceName, Set actionNames, Map envParameters,
            PolicyDecision policyDecision) 
            throws SSOException, NameNotFoundException, PolicyException {
        boolean resourceMatched = false;
        ConditionDecision conditionDecision = null;
        boolean allowedByConditions = false;
        boolean allowedBySubjects = false;
        Map advicesFromConditions = null;
        long conditionsTtl = Long.MIN_VALUE;
        long timeToLive = Long.MIN_VALUE;

        Map actionResults = getMatchedRuleResults(resourceType,
                resourceName, actionNames);
        resourceMatched = !actionResults.isEmpty();
        if (resourceMatched) { //resourceMatched+
            allowedBySubjects = users.isMember(token);
            timeToLive = users.getResultTtl(token);
            if (allowedBySubjects) { //resourceMatched+, subjects+
                conditionDecision = conditions.getConditionDecision(
                        token, envParameters);
                allowedByConditions = conditionDecision.isAllowed();
                advicesFromConditions = conditionDecision.getAdvices();
                conditionsTtl = conditionDecision.getTimeToLive();
                if (conditionsTtl < timeToLive) {
                    timeToLive = conditionsTtl;
                }
                if (allowedByConditions) {

                    //resourceMatched+, subjects+, conditions+
                    Iterator resultActionNames 
                            = actionResults.keySet().iterator();
                    while ( resultActionNames.hasNext() ) {
                        String resultActionName 
                                = (String) resultActionNames.next();
                        Set resultActionValues 
                            = (Set)actionResults.get(resultActionName);

                        /*  ActionDecision to include values, no advices
                         */
                        ActionDecision actionDecision 
                                = new ActionDecision(resultActionName,
                                resultActionValues, 
                                advicesFromConditions, timeToLive);
                        policyDecision.addActionDecision(
                                actionDecision, resourceType);
                    }
                } else { //resourceMatched+, subjects+, conditions-
                    Iterator resultActionNames  
                            = actionResults.keySet().iterator();
                    if (!advicesFromConditions.isEmpty()) {
                        while ( resultActionNames.hasNext() ) {
                            String resultActionName 
                                    = (String) resultActionNames.next();

                            /*  ActionDecision to include advices, no values
                             */
                            ActionDecision actionDecision 
                                    = new ActionDecision(resultActionName,
                                    Collections.EMPTY_SET, 
                                    advicesFromConditions, timeToLive);
                            policyDecision.addActionDecision(
                                    actionDecision, resourceType);
                        }
                    } else {
                        policyDecision.setTimeToLive(timeToLive);
                    }
                }
            } else { //resourceMatched+, subjects-
                policyDecision.setTimeToLive(timeToLive);
            }
        } else { //resourceMached-
            policyDecision.setTimeToLive(Long.MAX_VALUE);
        }
        return policyDecision;
    }

    /** Gets policy decision  computing Rules, Conditions and Subjects
     *  in this order. Referrals in the policy are ignored.
     *
     * @param token sso token identifying the user for who the policy has to 
     *        be evaluated.
     * @param resourceType service type
     * @param resourceName resourceName
     * @param actionNames a set of action names for which policy results
     *        are to be evaluated. Each element of the set should be a
     *        String
     * @param envParameters a map of environment parameters
     *        Each key of the map is a String valued parameter name
     *        Each value of the map is a set of String values
     * @param policyDecision a collecting argument. Computed policy decisions
     *        in this method are merged to this policy decision
     * @return computed and merged policy decision
     * @exception NameNotFoundException if the action name or resource name
     *         is not found
     * @exception SSOException if token is invalid
     * @exception PolicyException for any other exception condition
     */
    private PolicyDecision getPolicyDecisionRCS(SSOToken token, 
            ServiceType resourceType,
            String resourceName, Set actionNames, Map envParameters,
            PolicyDecision policyDecision) 
            throws SSOException, NameNotFoundException, PolicyException {
        boolean resourceMatched = false;
        ConditionDecision conditionDecision = null;
        boolean allowedByConditions = false;
        boolean allowedBySubjects = false;
        Map advicesFromConditions = null;
        long conditionsTtl = Long.MIN_VALUE;
        long subjectsTtl = Long.MIN_VALUE;
        long timeToLive = Long.MIN_VALUE;

        Map actionResults = getMatchedRuleResults(resourceType,
                resourceName, actionNames);
        resourceMatched = !actionResults.isEmpty();
        if (resourceMatched) { //resourceMatch+
            conditionDecision = conditions.getConditionDecision(
                    token, envParameters);
            allowedByConditions = conditionDecision.isAllowed();
            advicesFromConditions = conditionDecision.getAdvices();
            conditionsTtl = conditionDecision.getTimeToLive();
            timeToLive = conditionsTtl;
            if (allowedByConditions) { //resourceMatch+, conditions+
                allowedBySubjects = users.isMember(token);
                subjectsTtl = users.getResultTtl(token);
                if (subjectsTtl < timeToLive) {
                    timeToLive = subjectsTtl;
                }
                if (allowedBySubjects) {
                    //resourceMatch+, conditions+, subjects+
                    Iterator resultActionNames 
                            = actionResults.keySet().iterator();
                    while ( resultActionNames.hasNext() ) {
                        String resultActionName 
                                = (String) resultActionNames.next();
                        Set resultActionValues 
                            = (Set)actionResults.get(resultActionName);

                        /*  ActionDecision to include values, no advices
                         */
                        ActionDecision actionDecision 
                                = new ActionDecision(resultActionName,
                                resultActionValues, 
                                advicesFromConditions, timeToLive);
                        policyDecision.addActionDecision(
                                actionDecision, resourceType);
                    }
                } else { //resourceMatch+, conditions+, subjects-
                    policyDecision.setTimeToLive(timeToLive);
                }
            } else { //resourceMatch+, conditions-
                boolean reportAdvices = false;
                if (!advicesFromConditions.isEmpty()) {
                    reportAdvices = users.isMember(token);
                    subjectsTtl = users.getResultTtl(token);
                    if (subjectsTtl < timeToLive) {
                        timeToLive = subjectsTtl;
                    }
                }
                if (reportAdvices) {
                    Iterator resultActionNames 
                            = actionResults.keySet().iterator();
                    while ( resultActionNames.hasNext() ) {
                        String resultActionName 
                                = (String) resultActionNames.next();

                        /*  ActionDecision to include advices, no values
                         */
                        ActionDecision actionDecision 
                                = new ActionDecision(resultActionName,
                                Collections.EMPTY_SET, 
                                advicesFromConditions, timeToLive);
                        policyDecision.addActionDecision(
                                actionDecision, resourceType);
                    }
                } else { //no advices to report
                    policyDecision.setTimeToLive(timeToLive);
                }
            }
        } else { //resourceMatch-
            policyDecision.setTimeToLive(Long.MAX_VALUE);
        }
        return policyDecision;
    }


    /** Gets evaluation order of Subjects, Rules and Conditions for this policy
     *  that is likely to be least expensive in terms of cpu.
     *
     *  @return int representing preferred evaluation order for this policy
     */
    private int getEvaluationOrder(SSOToken token) throws SSOException {

        int evaluationOrder = RULES_CONDITIONS_SUBJECTS;

        //treat subject weight as 0, if sub result is in cache
        int mpsWeight = users.isSubjectResultCached(token) ? 0 : psWeight;
        if (( mpsWeight <= pcWeight) && (pcWeight <= prWeight)) {
            evaluationOrder = SUBJECTS_CONDITIONS_RULES;
        }  else if (( pcWeight <= mpsWeight) && (mpsWeight <= prWeight)) {
            evaluationOrder = CONDITIONS_SUBJECTS_RULES;
        }  else if (( prWeight <= pcWeight) && (pcWeight <= mpsWeight)) {
            evaluationOrder = RULES_CONDITIONS_SUBJECTS;
        }  else if (( prWeight <= mpsWeight) && (mpsWeight <= pcWeight)) {
            evaluationOrder = RULES_SUBJECTS_CONDITIONS;
        }  else if (( mpsWeight <= prWeight) && (prWeight <= pcWeight)) {
            evaluationOrder = SUBJECTS_RULES_CONDITIONS;
        }  else if (( pcWeight <= prWeight) && (prWeight <= mpsWeight)) {
            evaluationOrder = CONDITIONS_RULES_SUBJECTS;
        } 
        return evaluationOrder;
    }

    /** Initializes global values of evaluation weight 
     *  per Subject, per Condition and per Rule element
     *  of the policies by reading value of property
     * <code>EVALUATION_WEIGHTS_KEY</code> from AMConfig.properties.
     * If the value is not defined in AMConfig.properties, the value defaults
     * to <code>DEFAULT_EVALUATION_WEIGHTS</code>.
     * @see #DEFAULT_EVALUATION_WEIGHTS
     */
    private static void initializeStaticEvaluationWeights() {
        EVALUATION_WEIGHTS = com.iplanet.am.util.SystemProperties.get(
                EVALUATION_WEIGHTS_KEY, DEFAULT_EVALUATION_WEIGHTS);
        StringTokenizer st = new StringTokenizer(EVALUATION_WEIGHTS, ":");
        int tokenCount = st.countTokens();
        if ( tokenCount != 3) {
            if (PolicyManager.debug.warningEnabled()) {
                PolicyManager.debug.warning(
                    "Policy.initializeStaticEvaluationWeights:"
                    + " invalid evaulationWeights defined, "
                    + " defaulting to " + DEFAULT_EVALUATION_WEIGHTS);
            }
            EVALUATION_WEIGHTS = DEFAULT_EVALUATION_WEIGHTS;
        } else {
            String weight = st.nextToken();
            try {
                subjectWeight = Integer.parseInt(weight);
            } catch (NumberFormatException nfe) {
                if (PolicyManager.debug.warningEnabled()) {
                    PolicyManager.debug.warning(
                        "Policy.initializeStaticEvaluationWeights:"
                        + " invalid subjectWeight defined, defaulting to 0");
                }
                subjectWeight = 0;
            }
            weight = st.nextToken();
            try {
                ruleWeight = Integer.parseInt(weight);
            } catch (NumberFormatException nfe) {
                if (PolicyManager.debug.warningEnabled()) {
                    PolicyManager.debug.warning(
                        "Policy.initializeStaticEvaluationWeights:"
                        + " invalid ruleWeight defined, defaulting to 0");
                }
                ruleWeight = 0;
            }
            weight = st.nextToken();
            try {
                conditionWeight = Integer.parseInt(weight);
            } catch (NumberFormatException nfe) {
                if (PolicyManager.debug.warningEnabled()) {
                    PolicyManager.debug.warning(
                        "Policy.initializeStaticEvaluationWeights:"
                        + " invalid conditionWeight defined, defaulting to 0");
                }
                conditionWeight = 0;
            }
        }
    }


    /** Initializes  evaluation weights for 
     *  Subjects, Conditions and rules of this policy object.
     */
    void initializeEvaluationWeights() {
        psWeight = users.size() * subjectWeight;
        prWeight = rules.size() * ruleWeight;
        pcWeight = conditions.size() * conditionWeight;
    }

    /**
     * Checks whether the policy is applicable to user identified by sso token
     * @return <code>true</code> if the policy is applicable to the  user 
     *          identified by sso token, else <code>false</code>
     */
    boolean isApplicableToUser(SSOToken token)
        throws PolicyException, SSOException 
    {
        return  users.isMember(token);
    }

    static  Map cloneRuleResults(Map ruleResults) {
        Map clonedResults = new HashMap();
        if ( (ruleResults != null) && !ruleResults.isEmpty()) {
            Iterator keys = ruleResults.keySet().iterator();
            while (keys.hasNext()) {
                String key = (String)keys.next();
                Set values = (Set)ruleResults.get(key);
                Set clonedValues = new HashSet();
                clonedValues.addAll(values);
                clonedResults.put(key, clonedValues);
            }
        }
        return clonedResults;
    }

    /*
     * We track the subject realm when a realm subject is added to the policy.
     * We use this information to enforce that a policy has 
     * realm subjects only from one realm. We also use this information
     * to enforce that policy is not saved into a different realm.
     */
    String getSubjectRealm() {
        return subjectRealm;
    }

    /**
     * Clears the cached membership evaluation results corresponding
     * to the <code>tokenIdString</code>. This is triggered through
     * <code>PolicySSOTokenListener</code> and <code>PolicyCache</code>
     * when session property
     * of a logged in user is changed
     *
     * @param tokenIdString sessionId of the user whose session property changed
     */
    void clearSubjectResultCache(String tokenIdString) throws PolicyException {
        if (DEBUG.messageEnabled()) {
            DEBUG.message("Policy.clearSubjectResultCache(tokenIdString): "
                    + " clearing cached subject evaluation result for "
                    + " tokenId XXXXX");
        }
        users.clearSubjectResultCache(tokenIdString);
    }

    /**
     * Returns creation date.
     *
     * @return creation date.
     */
    public long getCreationDate() {
        return creationDate;
    }
    
    /**
     * Sets the creation date.
     * 
     * @param creationDate creation date.
     */
    public void setCreationDate(long creationDate) {
        this.creationDate = creationDate;
    }
    
    /**
     * Returns last modified date.
     * 
     * @return last modified date.
     */
    public long getLastModifiedDate() {
        return lastModifiedDate;
    }

    /**
     * Sets the last modified date.
     *
     * @param lastModifiedDate last modified date.
     */
    public void setLastModifiedDate(long lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }

    /**
     * Returns the user ID who last modified the policy.
     *
     * @return user ID who last modified the policy.
     */
    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    /**
     * Sets the user ID who last modified the policy.
     *
     * @param lastModifiedBy user ID who last modified the policy.
     */
    public void setLastModifiedBy(String lastModifiedBy) {
        this.lastModifiedBy = lastModifiedBy;
    }

    /**
     * Returns the user ID who created the policy.
     *
     * @return user ID who created the policy.
     */
    public String getCreatedBy() {
        return createdBy;
    }

    /**
     * Sets the user ID who created the policy.
     *
     * @param createdBy user ID who created the policy.
     */
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
}
