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
 * $Id: PolicyManager.java,v 1.19 2010/01/25 23:48:15 veiming Exp $
 *
 */

/*
 * Portions Copyrighted 2011 ForgeRock AS
 */
package com.sun.identity.policy;

import com.sun.identity.entitlement.opensso.PrivilegeUtils;
import com.iplanet.am.util.SystemProperties;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.iplanet.sso.SSOException;
import com.sun.identity.entitlement.Application;
import com.sun.identity.entitlement.ApplicationManager;
import com.sun.identity.entitlement.EntitlementConfiguration;
import com.sun.identity.policy.interfaces.Subject;
import com.sun.identity.idm.IdUtils;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.IPrivilege;
import com.sun.identity.entitlement.PrivilegeIndexStore;
import com.sun.identity.entitlement.PrivilegeManager;
import com.sun.identity.entitlement.ReferralPrivilege;
import com.sun.identity.entitlement.ReferralPrivilegeManager;
import com.sun.identity.entitlement.opensso.SubjectUtils;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.ldap.util.DN;
import com.sun.identity.shared.xml.XMLUtils;
import com.sun.identity.sm.DNMapper;
import com.sun.identity.sm.OrganizationConfigManager;
import com.sun.identity.sm.PluginSchema;
import com.sun.identity.sm.SMSEntry;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceAlreadyExistsException;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;
import com.sun.identity.sm.ServiceListener;
import com.sun.identity.sm.ServiceManager;
import com.sun.identity.sm.ServiceNotFoundException;
import com.sun.identity.sm.ServiceSchemaManager;
import java.io.ByteArrayInputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.AccessController;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import org.w3c.dom.Node;
import org.w3c.dom.Document;

/**
 * The <code>PolicyManager</code> class manages policies
 * for a specific organization, sub organization or a container.
 * This class is the
 * starting point for policy management, and provides methods to
 * create/modify/delete policies.
 * <p>It is a final class
 * and hence cannot be further extended. The methods in this class
 * works directly with the backend datastore (usually a
 * directory server) to store and manage policies. Hence, user
 * of this class must have valid <code>SSOToken</code>
 * and privileges to the backend datastore.
 *
 * @supported.api
 */
public final class PolicyManager {

    /**
     * The service name for Policy component.
     * @supported.api
     */
    public static final String POLICY_SERVICE_NAME = "iPlanetAMPolicyService";
    public static final String POLICY_DEBUG_NAME = "amPolicy";
    
    /**
     * The key for the plugins to get the organization name.
     * @supported.api
     */
    public static final String ORGANIZATION_NAME = "OrganizationName";
    public static final String DELEGATION_REALM = 
                      "/sunamhiddenrealmdelegationservicepermissions";

    public static final String NAMED_POLICY = "Policies";
    static final String REALM_SUBJECTS = "RealmSubjects";
    static final String XML_REALM_SUBJECTS = "xmlRealmSubjects";
    private static final String NAMED_POLICY_ID = "NamedPolicy";
    static final String RESOURCES_POLICY = "Resources";
    static final String RESOURCES_POLICY_ID = "ServiceType";
    private static final String SUBJECTS_POLICY = "Subjects";
    static final String SUBJECT_POLICY = "Subject";
    static final String REALM_SUBJECT_POLICY = "RealmSubject";
    static final String CONDITION_POLICY = "Condition";
    static final String RESP_PROVIDER_POLICY = "ResponseProvider";
    static final String REFERRAL_POLICY = "Referral";
    static final String REFERRALS_POLICY = "Referrals";
    private static final String POLICY_XML = "xmlpolicy";
    static final String POLICY_VERSION = "1.0";

    public static final String POLICY_ROOT_NODE = "Policy";
    static final String POLICY_RULE_NODE = "Rule";
    static final String POLICY_SUBJECTS_NODE = "Subjects";
    static final String POLICY_CONDITIONS_NODE = "Conditions";
    static final String POLICY_RESP_PROVIDERS_NODE = "ResponseProviders";
    static final String POLICY_REFERRALS_NODE = "Referrals";
    static final String POLICY_RULE_SERVICE_NODE = "ServiceName";
    static final String POLICY_RULE_RESOURCE_NODE = "ResourceName";
    static final String POLICY_RULE_EXCLUDED_RESOURCE_NODE =
        "ExcludedResourceName";
    static final String POLICY_RULE_APPLICATION_NAME_NODE = "ApplicationName";
    static final String ATTR_VALUE_PAIR_NODE = "AttributeValuePair";
    static final String ATTR_NODE = "Attribute";
    static final String ATTR_VALUE_NODE = "Value";
    static final String NAME_ATTRIBUTE = "name";
    static final String TYPE_ATTRIBUTE = "type";
    static final String DESCRIPTION_ATTRIBUTE = "description";
    static final String CREATED_BY_ATTRIBUTE = "createdby";
    static final String CREATION_DATE_ATTRIBUTE = "creationdate";
    static final String LAST_MODIFIED_BY_ATTRIBUTE = "lastmodifiedby";
    static final String LAST_MODIFIED_DATE_ATTRIBUTE = "lastmodifieddate";
    static final String PRIORITY_ATTRIBUTE = "priority";
    static final String STATUS_ATTRIBUTE = "priority";
    static final String STATUS_ACTIVE = "active";
    static final String STATUS_INACTIVE = "inactive";
    static final String SERVICE_TYPE_NAME_ATTRIBUTE = "serviceName";

    static final String POLICY_INDEX_ROOT_NODE = "PolicyCrossReferences";
    static final String POLICY_INDEX_ROOT_NODE_NAME_ATTR = "name";
    static final String POLICY_INDEX_ROOT_NODE_TYPE_ATTR = "type";
    static final String
        POLICY_INDEX_ROOT_NODE_TYPE_ATTR_RESOURCES_VALUE = "Resources";
    static final String POLICY_INDEX_REFERENCE_NODE = "Reference";
    static final String POLICY_INDEX_REFERENCE_NODE_NAME_ATTR = "name";
    static final String POLICY_INDEX_POLICYNAME_NODE = "PolicyName";
    static final String POLICY_INDEX_POLICYNAME_NODE_NAME_ATTR = "name";
    static final long DEFAULT_SUBJECTS_RESULT_TTL = 10 * 60 * 1000;

    static final String WEB_AGENT_SERVICE = "iPlanetAMWebAgentService";
    public static final String ID_REPO_SERVICE = "sunIdentityRepositoryService";
    public static final String ORG_ALIAS = "sunOrganizationAliases";
    public static final String ORG_ALIAS_URL_HTTP_PREFIX = "http://";
    public static final String ORG_ALIAS_URL_HTTPS_PREFIX = "https://";
    public static final String ORG_ALIAS_URL_SUFFIX = ":*";

    private String org = "/";
    private String givenOrgName = "";
    private ServiceConfigManager scm;
    private OrganizationConfigManager ocm;
    private ResourceManager rm;
    private ServiceTypeManager svtm;
    private SubjectTypeManager stm;
    private ConditionTypeManager ctm;
    private ResponseProviderTypeManager rpm;
    private ReferralTypeManager rtm;
    private PolicyCache policyCache;
    private ResourceIndexManager rim;

    private static ServiceSchemaManager ssm;
    private static SSOToken superAdminToken =
        (SSOToken) AccessController.doPrivileged(
        AdminTokenAction.getInstance());
    private static javax.security.auth.Subject adminSubject =
        SubjectUtils.createSubject(superAdminToken);
    SSOToken token;

    // Can be shared by classes
    static Debug debug = Debug.getInstance(POLICY_DEBUG_NAME);
    static DN delegationRealm = new DN(DNMapper.orgNameToDN(DELEGATION_REALM));
    private static boolean migratedToEntitlementService = false;

    static {
        EntitlementConfiguration ec = EntitlementConfiguration.getInstance(
            adminSubject, "/");
        migratedToEntitlementService = ec.migratedToEntitlementService();
    }



    /**
     * Constructor for <code>PolicyManager</code> for the
     * top (or root) organization. It requires a <code>SSOToken
     * </code> which will be used to perform all data store
     * operations. If the user does not have sufficient
     * privileges <code>NoPermissionException</code> will be thrown.
     *
     * @param token <code>SSOToken</code> of the user managing policy
     *
     * @throws SSOException invalid or expired single-sign-on token
     * @throws PolicyException for any other abnormal condition
     *  
     * @supported.api
     */
    public PolicyManager(SSOToken token) throws SSOException, PolicyException {
        this(token, "");
        if (debug.messageEnabled()) {
            debug.message("Policy Manager constructed using SSO token");
        }
    }

    /**
     * Constructor for <code>PolicyManager</code> for the
     * specified organization, sub organization or a container object.
     * The names of the organization, sub organization or the
     * container object could be either "/" separated (as per SMS)
     * or could be the complete DN of the object.
     * For example: <code>/isp/coke<code>, <code>/isp/pepsi/tacobell<code>,
     * etc., or <code>"ou=tacobell, o=pepsi, o=isp"<code>,
     * <code>"o=coke, o=isp"</code>, etc.
     * The constructor also requires a single sign on token.
     * which will be used to perform all data store
     * operations. If the user does not have sufficient
     * privileges <code>NoPermissionException</code> will be thrown.
     *
     * @param token single-sign-on token of the user managing policies
     * @param name name of the organization, sub organization
     * or container for which to manage policies.
     * The name could be either slash (/) separated
     * or the complete DN.
     *
     * @throws SSOException invalid or expired single-sign-on token
     * @throws NameNotFoundException if the given organization,
     *          sub-organization or container name is not present
     * @throws PolicyException for any other abnormal condition
     *
     * @supported.api
     */
    public PolicyManager(SSOToken token, String name)
        throws SSOException, NameNotFoundException, PolicyException {
        SSOTokenManager.getInstance().validateToken(token);
        this.token = token;
        try {
            scm = new ServiceConfigManager(POLICY_SERVICE_NAME, token);

            // Check name i.e., org name
            org = verifyOrgName(name);
            givenOrgName = name;

            rm = new ResourceManager(org);
        } catch (SMSException se) {
            debug.error("In constructor for PolicyManager with orgName" +
                "Unable to get service config manager", se);
            throw (new PolicyException(se));
        }
            
        if (debug.messageEnabled()) {
            debug.message("Policy Manager constructed with SSO token " +
                " for organization: " + org);
        }
        if (SystemProperties.isServerMode()) {
            policyCache = PolicyCache.getInstance();
            svtm = ServiceTypeManager.getServiceTypeManager();
        } else {
            policyCache = PolicyCache.getInstance(token);
            svtm = new ServiceTypeManager(token);
        }
        rim = new ResourceIndexManager(rm);

    }

    /**
     * Gets the organization name for which the policy manager
     * was initialized with. The organization name could either be
     * slash ("/") separated or could be the distinguished name
     * depending on the manner in which it was initialized.
     *
     * @return organization name for which the policy manager was
     *           instantiated
     *
     * @supported.api
     */
    public String getOrganizationName() {
        return (givenOrgName);
    }

    /** 
     * Gets the policy config attribute values defined for this policy manager
     * @return policy config attributes defined for this policy manager. Also, 
     *         includes the organization distinguished name. 
     */
    public Map getPolicyConfig() {
        Map policyConfig = null;
        try {
            policyConfig = PolicyConfig.getPolicyConfig(org);
        } catch (PolicyException pe) {
                    debug.error("PolicyManager:can not get policy config "
                    + " for org : " + org, pe);
        }
        if (policyConfig != null) {
            Set set = new HashSet();
            set.add(org);
            policyConfig.put(ORGANIZATION_NAME, set);
        } else {
            debug.error("PolicyManager: policy config is null for org:" 
                  + org + ". Most likely it has been unregistered." 
                  + " It is not recommended to unregister the policy"
                  + " configuration serivce. If you do so, the result"
                  + " is undefined.");
        }
        return policyConfig;
    }

    /**
     * Gets the organization DN
     * @return DN of the organization for which this policy manager
     *          was created
     */
    String getOrganizationDN() {
        return (org);
    }

    /**
     * Gets a set of  names of polices defined in the
     * organization for which the policy manager was instantiated.
     * If there are no policies defined, this method returns
     * an empty set (not null).
     *
     * @return <code>Set</code> of names of policies defined in the organization
     * 
     * @throws SSOException invalid or expired single-sign-on token
     * @throws NoPermissionException user does not have sufficient
     *          privileges to get policy names
     * @throws PolicyException for any other abnormal condition
     *
     * @supported.api
     */
    public Set getPolicyNames() throws SSOException, NoPermissionException,
        PolicyException {
        return (getPolicyNames("*"));
    }

    /**
     * Gets a set of selected policy names matching the
     * pattern in the given organization. The pattern
     * accepts "*" as the wild card for searching policy names.
     * For example if the pattern is "co*", it returns policies
     * starting with "co". Similarly, if the pattern is "*net", it returns
     * policies ending with "net". The wildcard can be anywhere in the
     * the string. If there are no policies that match the provided filter,
     * this method returns an empty set (not null).
     *
     * @param pattern search pattern that will be used to select policy names
     *
     * @return <code>Set</code> of policy names that satisfy the pattern
     *
     * @throws SSOException invalid or expired single-sign-on token
     * @throws NoPermissionException user does not have sufficient
     *          privileges to get policy names
     * @throws PolicyException for any other abnormal condition
     *
     * @supported.api
     */
    public Set getPolicyNames(String pattern) throws SSOException,
        NoPermissionException, PolicyException {
        try {
            ServiceConfig oConfig = scm.getOrganizationConfig(org, null);
            ServiceConfig namedPolicy = (oConfig == null) ? null :
                oConfig.getSubConfig(NAMED_POLICY);
            if (namedPolicy == null) {
               return (Collections.EMPTY_SET);
            } else {
                if (pattern.equals("*")) {
                    return (namedPolicy.getSubConfigNames());
                } else {
                    return (namedPolicy.getSubConfigNames(pattern));
                }
            }
       } catch (SMSException se) {
            debug.error("Unable to get named policies for organization: " 
                    + org);
            // Check for permission exception
            String objs[] = { org };
            if (se.getExceptionCode() == SMSException.STATUS_NO_PERMISSION) {
                throw (new NoPermissionException(ResBundleUtils.rbName,
                    "insufficient_access_rights", null));
            } else {
                // Throw generic policy exception
                throw (new PolicyException(ResBundleUtils.rbName,
                    "unable_to_get_policies_for_organization", objs,
                    se));
            }
        }
    }

    /**
     * Gets the LDAP DN (distinguished name) for the named policy
     * @param policyName name of the policy
     * @return DN of the policy 
     * @throws SSOException if single sign on token associated with the policy
     *         manager is not valid.
     * @throws NoPermissionException if not enough permissions
     * @throws NameNotFoundException if the policy is not found
     * @throws PolicyException for any other abnormal condition
     *
     */
    public String getPolicyDN(String policyName) throws
        SSOException, NoPermissionException, NameNotFoundException,
        PolicyException {
        // Get the policy. If the policy does not exists it will
        // throw NameNotFoundException
        getPolicy(policyName);

        // Policy is present, construct the DN and return
        StringBuilder answer = new StringBuilder(100);
        answer.append("ou=");
        answer.append(policyName);
        answer.append(",ou=");
        answer.append(NAMED_POLICY);
        answer.append(",ou=default,ou=organizationConfig,ou=");
        answer.append(POLICY_VERSION);
        answer.append(",ou=");
        answer.append(POLICY_SERVICE_NAME);
        answer.append(",ou=services,");
        answer.append(org);
        return (answer.toString());
    }

    /**
     * Gets the policy object given the name of the policy. 
     *
     * @param policyName name of the policy
     * @return policy with the given policy name
     * @throws SSOException if single sign on token associated with the policy
     *         manager is not valid.
     * @throws NoPermissionException if not enough permissions.
     * @throws InvalidFormatException if <code>policyName</code> has 
     *                                invalid format.
     * @throws NameNotFoundException if the policy is not found.
     * @throws InvalidNameException if <code>policyName</code> is invalid.
     * @throws PolicyException for any other abnormal condition.
     *
     *
     * @supported.api
     */
    public Policy getPolicy(String policyName) throws SSOException,
            NoPermissionException, InvalidFormatException, 
            NameNotFoundException,
        InvalidNameException, PolicyException {
        if (policyName == null) {
            throw (new InvalidNameException(ResBundleUtils.rbName,
                "null_name", null, "null", PolicyException.POLICY));
        }
        if (debug.messageEnabled()) {
            debug.message("searching for named policy: " + policyName +
                " in organization: " + org);
           }

        // Check the cache %%% Need to have notification for policy changes
        Policy answer = null;

        try {
            ServiceConfig oConfig = scm.getOrganizationConfig(org, null);
            ServiceConfig namedPolicy = (oConfig == null) ? null :
                oConfig.getSubConfig(NAMED_POLICY);
            ServiceConfig policy = null;
            Map attrs = null;
            Set  res = null;
            if ((namedPolicy == null) ||
                ((policy = namedPolicy.getSubConfig(policyName)) == null) ||
                ((attrs = policy.getAttributes()) == null) ||
                ((res = (Set) attrs.get(POLICY_XML)) == null) ||
                (res.size() <= 0)) {
                // Named policy node or policy node does not exist
                if (debug.warningEnabled()) {
                    debug.warning("Unable to find named policy: " + policyName +
                        " in organization: " + org);
                }
                String objs[] = { policyName, org };
                throw (new NameNotFoundException(ResBundleUtils.rbName,
                    "policy_not_found_in_organization", objs, policyName,
                    PolicyException.POLICY));
            }

            // Now create a policy object out of the XML blob
            Iterator it = res.iterator();
            String policyXml = (String) it.next();
            Document doc = null;
            try {
                doc = XMLUtils.getXMLDocument(
                    new ByteArrayInputStream(policyXml.getBytes("UTF8")));
            } catch (Exception xmle) {
                debug.error("XML parsing error for policy: " + policyName +
                    " in organization: " + org);
                // throw generic policy exception
                throw (new PolicyException(xmle));
            }
            Node rootNode = XMLUtils.getRootNode(doc, POLICY_ROOT_NODE);
            if (rootNode == null) {
                debug.error("invalid xml policy blob for named policy: " +
                        policyName +    " in organization: " + org);
                throw (new InvalidFormatException(ResBundleUtils.rbName,
                    "invalid_xml_policy_root_node", null, policyName,
                    PolicyException.POLICY));
            }

            if (debug.messageEnabled()) {
                debug.message("returning named policy: " + policyName +
                    " for organization: " + org);
            }

            // Return the policy object
            answer = new Policy(this, rootNode);
            Map policyConfig = getPolicyConfig();
            if (policyConfig != null) {
                answer.setSubjectsResultTtl(
                        PolicyConfig.getSubjectsResultTtl(policyConfig));
            }

            return (answer);
        } catch (SMSException se) {
            debug.error("SMS error in finding named policy: " + policyName +
                    " in organization: " + org);
            // Check for permission exception
            String objs[] = { policyName, org };
            if (se.getExceptionCode() == SMSException.STATUS_NO_PERMISSION) {
                throw (new NoPermissionException(ResBundleUtils.rbName,
                    "insufficient_access_rights", null));
            } else {
                // Throw generic policy exception
                throw (new PolicyException(ResBundleUtils.rbName,
                    "unable_to_get_policy", objs, se));
            }
        }

    }

    /**
     * Adds a policy to the data store. 
     *
     * @param policy policy object to be added to the organization
     *
     * @throws SSOException invalid or expired single-sign-on token
     * @throws NoPermissionException user does not have sufficient
     * privileges to add policy
     * @throws InvalidFormatException the data in the policy object
     * has been corrupted or does not have a valid format
     * @throws NameAlreadyExistsException a policy with the same
     * name already exists
     * @throws PolicyException for any other abnormal condition
     *
     * @supported.api
     */
    public void addPolicy(Policy policy) throws SSOException,
            NameAlreadyExistsException, NoPermissionException,
            InvalidFormatException, PolicyException {

        String realmName = getOrganizationDN();

        //TODO: handle non DNs/
        realmName = new DN(realmName).toRFCString().toLowerCase();
        String subjectRealm = policy.getSubjectRealm();
        String[] realmNames = {realmName, subjectRealm};
        if ((subjectRealm != null) && !subjectRealm.equals(realmName)) {
            if (debug.messageEnabled()) {
                debug.message("Can not add policy in realm :"
                        + realmName + ", policy has realm subjects "
                        + " from realm : " + subjectRealm);
            }

            //TODO : add logging?

            throw (new InvalidFormatException(ResBundleUtils.rbName,
                "policy_realm_does_not_match", realmNames, null, realmName, 
                PolicyException.POLICY));
        }
        validateForResourcePrefix(policy);
        validateReferrals(policy);

        String testCreatedBy = policy.getCreatedBy();
        //testCreatedBy is set if we are doing policy replaced.
        if ((testCreatedBy == null) || (testCreatedBy.length() == 0)) {
            Date creationDate = new Date();
            policy.setCreatedBy(token.getPrincipal().getName());
            policy.setCreationDate(creationDate.getTime());
            policy.setLastModifiedBy(token.getPrincipal().getName());
            policy.setLastModifiedDate(creationDate.getTime());
        }

        // Construct the named policy
        String policyXml = policy.toXML();
        Map attrs = new HashMap();
        Set set = new HashSet();
        set.add(policyXml);
        attrs.put(POLICY_XML, set);

        // Get(create if necessary) ou=policies entry
        ServiceConfig namedPolicy = createOrGetPolicyConfig(
            NAMED_POLICY, NAMED_POLICY, scm, org);
        try {
            //create the policy entry
            namedPolicy.addSubConfig(policy.getName(),
                NAMED_POLICY_ID, 0, attrs);
            if (isMigratedToEntitlementService()) {
                PrivilegeIndexStore pis = PrivilegeIndexStore.getInstance(
                    adminSubject, realmName);
                Set<IPrivilege> privileges = PrivilegeUtils.policyToPrivileges(
                    policy);
                pis.add(privileges);
                if (policy.isReferralPolicy()) {
                    ReferralPrivilegeManager refpm =
                        new ReferralPrivilegeManager(realmName, adminSubject);
                    refpm.addApplicationToSubRealm(
                        (ReferralPrivilege)privileges.iterator().next());
                }
                policyCache.sendPolicyChangeNotification(null, policy,
                    ServiceListener.ADDED);
            } else {
                // do the addition in resources tree
                //rm.addPolicyToResourceTree(policy);
                rim.addPolicyToResourceTree(svtm, token, policy);
            }
        } catch (EntitlementException e) {
            String[] objs = { policy.getName(), org };
            throw (new PolicyException(ResBundleUtils.rbName, 
                "unable_to_add_policy", objs, e)); 
        } catch (ServiceAlreadyExistsException e) {
            String[] objs = { policy.getName(), org };
            if (PolicyUtils.logStatus) {
                 PolicyUtils.logErrorMessage(
                    "POLICY_ALREADY_EXISTS_IN_REALM", objs, token);
            }
            throw (new NameAlreadyExistsException(ResBundleUtils.rbName,
                "policy_already_exists_in_org", objs, policy.getName(), 
                PolicyException.POLICY));
        } catch (SMSException se) {
            String[] objs = { policy.getName(), org };
            if (PolicyUtils.logStatus) {
                 PolicyUtils.logErrorMessage("UNABLE_TO_ADD_POLICY", objs, 
                    token);
            }
            debug.error("SMS error in add policy: " +
                    policy.getName() + " for org: " + org, se);

            // Check for permission exception
            if (se.getExceptionCode() == SMSException.STATUS_NO_PERMISSION) {
                throw (new NoPermissionException(ResBundleUtils.rbName,
                    "insufficient_access_rights", null));
            } else {
                // Throw generic policy exception
                throw (new PolicyException(ResBundleUtils.rbName, 
                        "unable_to_add_policy",
                        objs, se));
            }
        }
        if (PolicyUtils.logStatus) {
            String[] objs = { policy.getName(), org };
            PolicyUtils.logAccessMessage("POLICY_CREATE_SUCCESS", objs, token);
        }
    }

    /**
     * Replaces a policy object  in the data store with the same policy name
     *
     * @param policy policy object to be added to the organization
     *
     * @throws SSOException invalid or expired single-sign-on token
     * @throws NoPermissionException user does not have sufficient
     * privileges to replace policy
     * @throws NameNotFoundException policy with the same name does
     * not exist.
     * @throws InvalidFormatException the provide policy from the
     * data store has been corrupted or does not have a valid format
     * @throws PolicyException for any other abnormal condition.
     *
     * @supported.api
     */
    public void replacePolicy(Policy policy) throws SSOException,
        NameNotFoundException, NoPermissionException,
        InvalidFormatException, PolicyException {

        String realm = getOrganizationDN();
        String subjectRealm = policy.getSubjectRealm();
        String[] realmNames = {realm, subjectRealm};
        if ((subjectRealm != null) && !subjectRealm.equals(realm)) {

            if (debug.messageEnabled()) {
                debug.message("Can not replace policy in realm :"
                        + realm + ", policy has realm subjects "
                        + " from realm : " + subjectRealm);
            }

            throw (new InvalidFormatException(ResBundleUtils.rbName,
                "policy_realm_does_not_match", realmNames, null, realm,
                PolicyException.POLICY));
        }

        policy.setLastModifiedBy(token.getPrincipal().getName());
        Date lastModifiedDate = new Date();
        policy.setLastModifiedDate(lastModifiedDate.getTime());

        // Construct the named policy
        String policyXml = policy.toXML();
        Map attrs = new HashMap();
        Set set = new HashSet();
        set.add(policyXml);
        attrs.put(POLICY_XML, set);

        String name = null;
        // Get(create if necessary) ou=policies entry
        ServiceConfig namedPolicy = createOrGetPolicyConfig(
            NAMED_POLICY, NAMED_POLICY, scm, org);
        try {
            String policyName = policy.getName();
            String oldPolicyName = policy.getOriginalName();
            ServiceConfig policyEntry = namedPolicy.getSubConfig(policyName);
            ServiceConfig oldPolicyEntry = null;
            if ( oldPolicyName != null ) {
                oldPolicyEntry = namedPolicy.getSubConfig(oldPolicyName);
                name = oldPolicyName;
            } else {
                name = policy.getName();
            }
            if ( policyEntry == null ) {
                if ( oldPolicyEntry != null ) {
                    removePolicy(oldPolicyName);
                    addPolicy(policy);
                    // reset the policy name
                    // TODO: need to think this through
                    policy.resetOriginalName();
                } else {
                    // neither the new policy nor the old policy is present
                    String objs[] = { policy.getName(), org };
                    throw (new NameNotFoundException(ResBundleUtils.rbName,
                            "policy_not_found_in_organization", objs,
                         policy.getName(), PolicyException.POLICY));
                }
            } else { //newPolicy exisits
                String[] objs = { policy.getName(), org };
                if((oldPolicyName != null) && 
                            !policy.getName().equalsIgnoreCase(oldPolicyName)) {
                    if (PolicyUtils.logStatus) {
                        PolicyUtils.logErrorMessage(
                            "DID_NOT_REPLACE_POLICY", 
                            objs, token);
                    }
                    throw (new NameAlreadyExistsException(ResBundleUtils.rbName,
                        "policy_already_exists_in_org", objs, policy.getName(), 
                        PolicyException.POLICY));
                }

                Policy oldPolicy = getPolicy(policy.getName());
                validateForResourcePrefix(policy);
                validateReferrals(policy);
                policyEntry.setAttributes(attrs);
                if (oldPolicy != null) {
                    if (isMigratedToEntitlementService()) {
                        PrivilegeIndexStore pis = PrivilegeIndexStore.
                            getInstance(SubjectUtils.createSubject(token),
                            realm);
                        pis.delete(PrivilegeUtils.policyToPrivileges(
                            oldPolicy));
                        pis.add(PrivilegeUtils.policyToPrivileges(policy));
                    } else {
                        //rm.replacePolicyInResourceTree(oldPolicy, policy);
                        rim.replacePolicyInResourceTree(svtm, token, oldPolicy,
                            policy);
                    }

                    policyCache.sendPolicyChangeNotification(oldPolicy, policy, ServiceListener.MODIFIED);
                }
            }
        } catch (EntitlementException e) {
            String[] objs = { name, org };
            throw (new PolicyException(ResBundleUtils.rbName,
                "unable_to_replace_policy", objs, e));
        } catch (SMSException se) {
            String[] objs = { name, org };
            if (PolicyUtils.logStatus) {
                PolicyUtils.logErrorMessage("UNABLE_TO_REPLACE_POLICY", objs, 
                    token);
            }
            debug.error("SMS error in replacing policy: " +
                    policy.getOriginalName() + " for org: " + org, se);
            // Check for permission exception
            if (se.getExceptionCode() == SMSException.STATUS_NO_PERMISSION) {
                throw (new NoPermissionException(ResBundleUtils.rbName,
                    "insufficient_access_rights", null));
            } else {
                // Throw generic policy exception
                throw (new PolicyException(ResBundleUtils.rbName, 
                    "unable_to_replace_policy", objs, se));
            }
        }
        if (PolicyUtils.logStatus) {
            String[] objs = { name, org };
            PolicyUtils.logAccessMessage("POLICY_MODIFY_SUCCESS", objs, token);
        }
    }

    /**
     * Deletes a policy in the organization with the given name.
     *
     * @param policyName name of the policy to be deleted
     *
     * @throws SSOException invalid or expired single-sign-on token
     * @throws NoPermissionException user does not have sufficient
     * privileges to remove policies
     * @throws PolicyException for any other abnormal condition
     *
     * @supported.api
     */
    public void removePolicy(String policyName) throws
        SSOException, NoPermissionException, PolicyException {
        // Check if name is valid
        if (policyName == null) {
            if (debug.warningEnabled()) {
                debug.warning("In PolicyManager::removePolicy(), name is null");
            }
            throw (new InvalidNameException(ResBundleUtils.rbName,
                "null_name", null, "null", PolicyException.POLICY));
        }

        try {
            // Get service config for named policy node
            ServiceConfig oConfig = scm.getOrganizationConfig(org, null);
            ServiceConfig namedPolicy = (oConfig == null) ? null :
                oConfig.getSubConfig(NAMED_POLICY);
            if (namedPolicy != null) {
                /* Remove the named policy
                 * before removing the named policy
                 * prepare for changes in resources tree
                 */
                Policy policy = getPolicy(policyName);

                // do the removal of policy 
                namedPolicy.removeSubConfig(policyName);

                if (policy != null) {
                    if (isMigratedToEntitlementService()) {
                        // should use super admin token to remove the index store
                        // entry
                        PrivilegeIndexStore pis = PrivilegeIndexStore.
                            getInstance(
                            SubjectUtils.createSuperAdminSubject(),
                            getOrganizationDN());
                        if (policy.isReferralPolicy()) {
                            pis.deleteReferral((policyName));
                        } else {
                            pis.delete(
                                PrivilegeUtils.policyToPrivileges(policy));
                        }
                        policyCache.sendPolicyChangeNotification(null, policy,
                            ServiceListener.REMOVED);
                    } else {
                        // do the removal in resources tree
                        rim.removePolicyFromResourceTree(svtm, token, policy);

                    }
                }
            }
        } catch (EntitlementException e) {
            debug.error("Error while removing policy : " + e.getMessage());
        } catch (ServiceNotFoundException snfe) {
            debug.error("Error while removing policy : " +
                    snfe.getMessage() );
        } catch (SMSException smse) {
            String objs[] = { policyName, org };
            if (PolicyUtils.logStatus) {
                PolicyUtils.logErrorMessage("UNABLE_TO_REMOVE_POLICY", objs, 
                    token);
            }
            debug.error("SMS error in deleting policy: " +
                    policyName + " for org: " + org, smse);
            // Check for permission exception
            if (smse.getExceptionCode() == SMSException.STATUS_NO_PERMISSION) {
                throw (new NoPermissionException(ResBundleUtils.rbName,
                    "insufficient_access_rights", null));
            } else {
                // Throw generic policy exception
                throw (new PolicyException(ResBundleUtils.rbName, 
                    "unable_to_remove_policy", objs, smse));
            }
        }
        String objs[] = { policyName, org };
        if (PolicyUtils.logStatus) {
            PolicyUtils.logAccessMessage("POLICY_REMOVE_SUCCESS", objs, token);
        }
    }

    /**
     * Gets the <code>ResourceManager</code> object instance associated
     * with this <code>PolicyManager</code> object instance
     *
     * @return <code>ResourceManager</code> object
     *
     * @supported.api
     */    
    public ResourceManager getResourceManager() {
        return rm;
    }
    
    /**
     * Gets the <code>SubjectTypeManager</code> object instance associated
     * with this <code>PolicyManager</code> object instance
     *
     * @return <code>SubjectTypeManager</code> object
     *
     * @supported.api
     */    
    public SubjectTypeManager getSubjectTypeManager() {
        if (stm == null) {
            stm = new SubjectTypeManager(this);
        }
        return (stm);
    }
    
    /**
     * Gets the <code>ConditionTypeManager</code> object instance associated
     * with this <code>PolicyManager</code> object instance
     *
     * @return <code>ConditionTypeManager</code> object
     *
     * @supported.api
     */    
    public ConditionTypeManager getConditionTypeManager() {
        if (ctm == null) {
            ctm = new ConditionTypeManager(this);
        }
        return (ctm);
    }

     /**
      * Gets the <code>ResponseProviderTypeManager</code> object instance 
      * associated with this <code>PolicyManager</code> object instance
      *
      * @return <code>ResponseProviderTypeManager</code> object
      *
      */    
     public ResponseProviderTypeManager getResponseProviderTypeManager() {
         if (rpm == null) {
             rpm = new ResponseProviderTypeManager(this);
         }
         return (rpm);
     }
     
    /** Creates or gets a node (namedPolicy, resources, or userCollection)
     *  under ou=policies
     */
    static ServiceConfig createOrGetPolicyConfig(String configName,
        String configId, ServiceConfigManager scm, String org)
        throws NoPermissionException, PolicyException, SSOException {
        // Get service config that represents named policy node
        ServiceConfig sConfig = null;
        try {
            ServiceConfig oConfig = scm.getOrganizationConfig(org, null);
            sConfig = (oConfig == null) ? null :
                oConfig.getSubConfig(configName);
            if (sConfig == null) {
                if (debug.messageEnabled()) {
                    debug.message("Creating the " + configName +
                        " tree for org: " + org);
                }
                // create the named policy node
                createPolicyTree(configName, configId, scm, org);

                // Check if policy tree is created
                if (oConfig == null) {
                    oConfig = scm.getOrganizationConfig(org, null);
                }
                if ((oConfig == null) || ((sConfig = oConfig.getSubConfig(
                    configName)) == null)) {
                    // Throw generic policy exception
                    String objs[] = { configName };
                    throw (new PolicyException(ResBundleUtils.rbName,
                        "unable_to_get_policy_node", objs, null));
                }
            }
        } catch (SMSException smse) {
            // Debug messages
            debug.error("SMS error in creating " + configName +
                            " node for org: " + org, smse);

            // Check for permission exception
            if (smse.getExceptionCode() == SMSException.STATUS_NO_PERMISSION) {
                String objs[] = { configName };
                throw (new NoPermissionException(ResBundleUtils.rbName,
                    "insufficient_access_rights", objs));
            } else {
                // Throw generic policy exception
                String objs[] = { configName };
                throw (new PolicyException(ResBundleUtils.rbName,
                    "unable_to_get_policy_node", objs, smse));
            }
        }
        return (sConfig);
    }

    /**
     *  Creates the policy tree, ou=policy, ou=services, ... 
     */
    static void createPolicyTree(String configName, String configId,
        ServiceConfigManager scm, String org) throws NoPermissionException,
        PolicyException, SSOException {
        try {
            // Get the iPlanetPolicyService node            
            ServiceConfig pConfig = scm.getOrganizationConfig(org, null);
            if (pConfig == null) {
                // Since not present, create organization services node
                // i.e, ou=services, <org dn> and ou=iPlanetPolicyService,
                //  ou=services, ...
                scm.createOrganizationConfig(org, null);
                // Since it is created above, get it
                pConfig = scm.getOrganizationConfig(org, null);
            }
            // Create requested policy sub node
            // i.e., ou=<configName>, ou=iPlanetPolicyService, ou=services, ...
            pConfig.addSubConfig(configName, configId, 0, null);
        } catch (ServiceAlreadyExistsException se) {
            // do nothing
            if (debug.messageEnabled()) {
                debug.message("PolicyManager->createPolicyTree: Name: " +
                    configName + " ID: " + configId + 
                    " Policy service already exists under org->" + org);
            }
        } catch (SMSException e) {
            // Check for permission exception
            String[] objs = { org };
            if (e.getExceptionCode() == SMSException.STATUS_NO_PERMISSION) {
                throw (new NoPermissionException(ResBundleUtils.rbName,
                    "insufficient_access_rights", null));
            } else {
                throw (new PolicyException(ResBundleUtils.rbName,
                    "unable_to_create_policy_for_org", objs, e));
            }
        }
    }

    /**
     * Verifies if the name, which specifies a organization,
     * a suborganization or a container object, is in DN format; If not,
     * the name will be converted to a DN. Furthermore, whether the entry
     * corresponding to the DN exists is also checked.
     *
     * @param name name of the organization, suborganization
     * or container for which to manage policies.
     * The name could be either slash (/) separated
     * or the complete DN.
     *
     * @return the name in DN format (possibly converted from slash format)
     *
     * @throws SSOException invalid or expired single-sign-on token
     * @throws InvalidNameException the name is null
     * @throws NameNotFoundException if the given organization,
     * sub-organization or container name is not present
     */
    String verifyOrgName(String name)
        throws InvalidNameException, NameNotFoundException, SSOException {
        String orgName = null;

        // Check for null name
        if (name == null) {
            throw (new InvalidNameException(ResBundleUtils.rbName, "null_name",
                null, "null", PolicyException.ORGANIZATION));
        }

        /* this mapping call is required if name is not a DN
         * or if the name is not DN of a realm
         * This call is harmless if the DN is already DN of a realm
         */
        orgName = DNMapper.orgNameToDN(name);

        // Check to see if the organization exists
        // if not present throw NameNotFoundException
        if (!SMSEntry.checkIfEntryExists(orgName, token)) {
            if (debug.warningEnabled()) {
                debug.warning("Checking for organization name: " +
                    orgName + " failed since entry does not exist");
            }
            String[] objs = { name };
            throw (new NameNotFoundException(ResBundleUtils.rbName,
                "org_not_found", objs, orgName,
                PolicyException.ORGANIZATION));
        }
        return (orgName);
    }
    
    /**
     * Gets a list of subordinate organizations or containers. If
     * there are no subordinate organizations or containers, returns
     * an empty set (not null).
     *
     * @return set of valid subordinate organizations or containers
     *
     * @throws SSOException invalid or expired single-sign-on token
     * @throws NoPermissionException user does not have sufficient
     * privileges to replace policy
     */
    Set getSubOrganizationNames() throws SSOException,
        NoPermissionException, PolicyException {
        throw (new UnsupportedOperationException());
    }

    /**
     * Gets sub-organizations, given a filter. The filter
     * accepts "*" as the wild card for searching sub-organization names.
     * For example if the filter is "co*", it returns sub-organizations
     * starting with "co". Similarly, if the filter is "*net", it returns
     * sub-organizations ending with "net".
     *
     * @param filter the search filter that be used to 
     *               identify sub-organizations
     *
     * @return a set of sub-organizations
     */
    Set getSubOrganizationNames(String filter)
        throws SSOException, PolicyException {
        throw (new UnsupportedOperationException());
    }

    /**
     * Gets sub-organization's policy manager.
     *
     * @param subOrgName name of the sub-organization organization. This
     * should be relative to the current organization.
     *
     * @return returns the policy manager for the organization;
     * else returns <code>null</code>.
     */
    PolicyManager getSubOrganizationPolicyManager(String subOrgName)
        throws SSOException, PolicyException {
        // the assumption is that orgnames are / separated
        return (new PolicyManager(token, org + "/" + subOrgName));
    }

    /**
     * Gets <code>ServiceTypeManager</code> associated with this 
     * policy manager
     */
    ServiceTypeManager getServiceTypeManager() {
        return svtm;
    }
    
    /**
     * Returns <code>ReferralTypeManager</code> associated with this 
     * policy manager.
     *
     * @return <code>ReferralTypeManager</code> associated with this 
     * policy manager.
     * @supported.api
     */
    public ReferralTypeManager getReferralTypeManager() {
        if ( rtm == null ) {
            rtm = new ReferralTypeManager(this);
        }
        return rtm;
    }


    /**
     * Gets plugins schemas for a given interface name
     */
    static Set getPluginSchemaNames(String interfaceName) {
        if (ssm == null) {
            try {
                ssm = new ServiceSchemaManager(
                    PolicyManager.POLICY_SERVICE_NAME,
                    ServiceTypeManager.getSSOToken());
            } catch (Exception se) {
                PolicyManager.debug.error(
                        "Cannot create service schema " +
                        "manager for policy", se);
                return (Collections.EMPTY_SET);
            }
        }

        Set answer = null;
        try {
           /* Get the plugin schema names for the root
            * In the future might want to customize it for org
            */
            answer = ssm.getPluginSchemaNames(interfaceName, null);
        } catch (Exception e) {
            PolicyManager.debug.error(
                    "Cannot get plugin schemas: " +
                    interfaceName + " for policy", e);
            return (Collections.EMPTY_SET);
        }
        return ((answer == null) ? Collections.EMPTY_SET : answer);
    }


    /**
     * Gets PluginSchema object for the given plugin interface name
     * and plugin name. Returns <code>null</code> if not present.
     */
    static PluginSchema getPluginSchema(String interfaceName,
        String pluginName) {
        Set plugins = getPluginSchemaNames(interfaceName);
        if (plugins.contains(pluginName)) {
            try {
                return (ssm.getPluginSchema(pluginName, interfaceName, null));
            } catch (Exception e) {
                PolicyManager.debug.error(
                        "Cannot get plugin schemas: " +
                        interfaceName + " for policy", e);
            }
        }
        return (null);
    }

    /**
     * Gets the view bean URL given the plugin type 
     * and the plugin java class name
     *
     * @param pluginType  type of plugin such as Subject, Referral, Condition
     * @param pluginClassName name of java class name implementing the plugin
     *                        type
     *
     * @return view bean URL defined for pluginType with name pluginName
     */
    static String getViewBeanURL(String pluginType, String pluginClassName) {
        String viewBeanURL = null;
        if (pluginType != null) {
            Iterator items = PolicyManager.getPluginSchemaNames(
                    pluginType).iterator();
            while (items.hasNext()) {
                String pluginName = (String) items.next();
                PluginSchema ps = PolicyManager.getPluginSchema(pluginType, 
                        pluginName);
                if (pluginClassName.equals(ps.getClassName())) {
                    viewBeanURL = ps.getPropertiesViewBeanURL();
                    break;
                }
            }
        }
        return viewBeanURL;
    }

    /** Gets a policy using policy cache. 
     * @param policyName policy name
     * @param useCache flag to indicate whether to use cache or not
     * @return the policy with the given policy name
     * @throws SSOException
     * @throws NoPermissionException
     * @throws InvalidFormatException
     * @throws PolicyException
     */
    Policy getPolicy(String policyName, boolean useCache) throws SSOException,
            NoPermissionException, InvalidFormatException, 
            NameNotFoundException,
            InvalidFormatException, PolicyException {
        Policy policy = null;
        if ( useCache ) {
            policy = policyCache.getPolicy(org, policyName);
        } else {
            policy = getPolicy(policyName);
        }
        return policy;
    }

    ResourceIndexManager getResourceIndexManager() {
        return rim;
    }

    private boolean validateResourceForPrefix(ServiceType resourceType, 
        String resourceName) throws PolicyException {
        Set<String> resourcePrefixes = getManagedResourceNames(
                resourceType.getName());
        return validateResourceForPrefix(resourceType,
            resourcePrefixes, resourceName);
    }

    private boolean validateResourceForPrefixE(
        String realm,
        String serviceName,
        Set<String> resourcePrefixes,
        String resourceName) throws PolicyException, EntitlementException {

        String realmName = (DN.isDN(realm)) ?
            DNMapper.orgNameToRealmName(realm) :realm;

        Application appl = ApplicationManager.getApplication(
            PrivilegeManager.superAdminSubject, realmName, serviceName);
        com.sun.identity.entitlement.interfaces.ResourceName resComp = appl.
            getResourceComparator();
        resourceName = resComp.canonicalize(resourceName);

        for (String prefix : resourcePrefixes) {
            boolean interpretWildCard = true;
            com.sun.identity.entitlement.ResourceMatch resMatch =
                resComp.compare(resourceName,
                resComp.canonicalize(prefix), interpretWildCard);
            if ( resMatch.equals(
                com.sun.identity.entitlement.ResourceMatch.SUPER_RESOURCE_MATCH)
                || resMatch.equals(
                    com.sun.identity.entitlement.ResourceMatch.WILDCARD_MATCH)
                || resMatch.equals(
                    com.sun.identity.entitlement.ResourceMatch.EXACT_MATCH) ) {
                return true;
            }

        }
        return false;
    }

    private boolean validateResourceForPrefix(
        ServiceType resourceType,
        Set<String> resourcePrefixes,
        String resourceName) throws PolicyException {

        for (String prefix : resourcePrefixes) {
            boolean interpretWildCard = true;
            ResourceMatch resMatch = resourceType.compare(resourceName, prefix,
                    interpretWildCard);
            if ( resMatch.equals(ResourceMatch.SUPER_RESOURCE_MATCH)
                        || resMatch.equals(ResourceMatch.WILDCARD_MATCH)
                        || resMatch.equals(ResourceMatch.EXACT_MATCH) ) {
                return true;
            }

        }
        return false;
    }

    private void validateForResourcePrefix(Policy policy)
                throws SSOException, PolicyException {
        if (isMigratedToEntitlementService()) {
            validateForResourcePrefixE(policy);
        } else {
            validateForResourcePrefixO(policy);
        }
    }

    private void validateForResourcePrefixE(Policy policy)
        throws SSOException, PolicyException {
        DN orgDN = new DN(org);
        DN baseDN = new DN(ServiceManager.getBaseDN());

        if (!orgDN.equals(baseDN) && !orgDN.equals(delegationRealm)) {
            String realm = DNMapper.orgNameToRealmName(getOrganizationDN());
            Iterator ruleNames = policy.getRuleNames().iterator();
            while (ruleNames.hasNext()) {
                try {
                    String ruleName = (String) ruleNames.next();
                    Rule rule = (Rule) policy.getRule(ruleName);
                    String serviceTypeName = rule.getServiceTypeName();
                    String ruleResource = rule.getResourceName();

                    Set<String> referredResources = ApplicationManager.
                        getReferredResources(adminSubject,
                        realm, serviceTypeName);
                    if ((referredResources == null) || referredResources.
                        isEmpty()) {
                        String[] objs = {org};
                        throw new PolicyException(ResBundleUtils.rbName,
                            "no_referral_can_not_create_policy", objs, null);
                    }
                    ServiceType resourceType = getServiceTypeManager().
                        getServiceType(serviceTypeName);

                    if (!validateResourceForPrefixE(realm, serviceTypeName,
                        referredResources, ruleResource)) {
                        String[] objs = {ruleResource, resourceType.getName()};
                        throw new PolicyException(ResBundleUtils.rbName,
                            "resource_name_not_permitted_by_prefix_names", objs,
                            null);
                    }
                } catch (EntitlementException ex) {
                    String[] objs = {org};
                    throw new PolicyException(ResBundleUtils.rbName,
                        "no_referral_can_not_create_policy", objs, null);
                }
            }
        }
    }

    private void validateForResourcePrefixO(Policy policy)
            throws SSOException, PolicyException {
        DN orgDN = new DN(org);
        DN baseDN = new DN(ServiceManager.getBaseDN());
        Set prefixes = getManagedResourceNames();
        if (!orgDN.equals(baseDN) && !orgDN.equals(delegationRealm)
             && ((prefixes == null ) || prefixes.isEmpty()) ) {
                String[] objs = {org};
                throw new PolicyException(
                        ResBundleUtils.rbName,
                        "no_referral_can_not_create_policy", objs, null);
        }
        Iterator ruleNames = policy.getRuleNames().iterator();
        while ( ruleNames.hasNext() ) {
            String ruleName = (String) ruleNames.next();
            Rule rule = (Rule) policy.getRule(ruleName);
            String serviceTypeName = rule.getServiceTypeName();
            ServiceType resourceType = getServiceTypeManager()
                    .getServiceType(serviceTypeName);
            String ruleResource = rule.getResourceName();
            boolean validResource = true;
            if (!orgDN.equals(baseDN) && !orgDN.equals(delegationRealm)) {
                validResource = validateResourceForPrefix(resourceType, 
                        ruleResource);
            }
            if (!validResource) {
                String[] objs = { ruleResource, resourceType.getName() };
                throw new PolicyException(
                        ResBundleUtils.rbName,
                        "resource_name_not_permitted_by_prefix_names",
                        objs, null);
            }
        }

    }

    private void validateReferrals(Policy policy) 
            throws SSOException, PolicyException {
        Set candidateOrgs = policy.getReferredToOrganizations();
        if ( candidateOrgs.contains(org.toLowerCase()) ) {
            String[] objs = { org };
            throw new PolicyException(
                    ResBundleUtils.rbName,
                    "invalid_referral_pointing_to_self",
                    objs, null);
        }
        Iterator iter = candidateOrgs.iterator();
        while ( iter.hasNext() ) {
            String candidateOrg = (String) iter.next();

            /* 
             * check org orgName exisits - would result in
             * PolicyException if the orgName does not exist 
             */
            verifyOrgName(candidateOrg);
        }

    }

    void saveRealmSubjects(Subjects subjects) 
            throws PolicyException, SSOException {
        ServiceConfig realmSubjects = createOrGetPolicyConfig(
            REALM_SUBJECTS, REALM_SUBJECTS, scm, org);
        Map attributes = new HashMap(1);
        Set values = new HashSet(1);
        String subjectsXML = subjects.toXML();
        values.add(subjectsXML);
        attributes.put(XML_REALM_SUBJECTS, values);
        try {
            realmSubjects.setAttributes(attributes);
        } catch (SMSException se) {
            debug.error("SMS error in saving realm subjects " 
                    + " in organization: " + org);
            // Check for permission exception
            String objs[] = { org };
            if (se.getExceptionCode() == SMSException.STATUS_NO_PERMISSION) {
                throw new PolicyException(ResBundleUtils.rbName,
                    "insufficient_access_rights", null, se);
            } else {
                // Throw generic policy exception
                throw new PolicyException(ResBundleUtils.rbName,
                    "unable_to_save_realm_subjects", objs, se);
            }
        }
        if (debug.messageEnabled()) {
            debug.message("saved realm subjects:" + subjectsXML);
        }
    }

    Subjects readRealmSubjects() throws PolicyException, SSOException {
        Subjects subjects = null;
        ServiceConfig realmSubjects = createOrGetPolicyConfig(
                REALM_SUBJECTS, REALM_SUBJECTS, scm, org);
        Set values = null;
        values = (Set)realmSubjects.getAttributes().get(XML_REALM_SUBJECTS);
        if ((values != null) && !values.isEmpty()) {
            String xmlSubjects = (String)values.iterator().next();
            Document doc = null;
            try {
                doc = XMLUtils.getXMLDocument(
                    new ByteArrayInputStream(xmlSubjects.getBytes("UTF8")));
            } catch (Exception xmle) {
                debug.error("XML parsing error for realmSubjects: " 
                        + " in organization: " + org);
                // throw generic policy exception
                throw (new PolicyException(xmle));
            }
            Node subjectsNode = XMLUtils.getRootNode(doc, SUBJECTS_POLICY);
            if (subjectsNode == null) {
                debug.error("invalid xmlRealmSubjects blob " 
                        +    " in organization: " + org);
                throw (new InvalidFormatException(ResBundleUtils.rbName,
                    "invalid_xml_realmsubjects_root_node", null, 
                    org, PolicyException.POLICY));
            }
            subjects = new Subjects(this, subjectsNode);
        } else {
            subjects = new Subjects();
        }
        if (debug.messageEnabled()) {
            debug.message("read realm subjects:" + subjects.toXML());
        }
        subjects.setPolicyConfig(getPolicyConfig());
        return subjects;
    }

    /**
     * Gets the set of policies that use the realm subject
     * @param subjectName name of the realm subject to check for
     * @return a <code>Set</code> of <code>Policy</code> objects 
     *        that use the  realm subject
     */
    public Set getPoliciesUsingRealmSubject(String subjectName) 
            throws PolicyException, SSOException {
        Set policies = new HashSet();
        Set policyNames = getPolicyNames();
        for (Iterator policyIter = policyNames.iterator(); 
                policyIter.hasNext();) {
            String policyName = (String)policyIter.next();
            Policy policy = getPolicy(policyName);
            Set subjectNames = policy.getSubjectNames();
            if (subjectNames.contains(subjectName)) {
                Subject subject = policy.getSubject(subjectName);
                if (subject instanceof SharedSubject) {
                    policies.add(policy);
                }
            }
        }
        return policies;
    }

    Policy getPolicyUsingRealmSubject(String subjectName) 
            throws PolicyException, SSOException {
        Policy policy = null;
        Set policyNames = getPolicyNames();
        for (Iterator policyIter = policyNames.iterator(); 
                policyIter.hasNext();) {
            String policyName = (String)policyIter.next();
            Policy p = getPolicy(policyName);
            Set subjectNames = p.getSubjectNames();
            if (subjectNames.contains(subjectName)) {
                Subject subject = p.getSubject(subjectName);
                if (subject instanceof SharedSubject) {
                    policy = p;
                    break;
                }
            }
        }
        return policy;
    }

    private Set getOrgAliasMappedResourceNames() 
            throws PolicyException {
        if (debug.messageEnabled()) {
            debug.message("PolicyManager.getOrgAliasMappedResourceNames(): "
                    + " entering:orgName = " + org);
        }
        Set managedResourceNames = new HashSet(3);
        if (ocm == null) {
            try {
                ocm = new OrganizationConfigManager(token, givenOrgName);
            } catch (SMSException sme) {
                String[] objs = { org };
                throw (new PolicyException(ResBundleUtils.rbName,
                        "unable_to_get_org_config_manager_for_org", 
                        objs, sme));
            }
        }

        Set orgAliases = null;
        try {
            Map orgAttributes = ocm.getAttributes(ID_REPO_SERVICE);
            orgAliases 
                    = (Set)orgAttributes.get(ORG_ALIAS);
            if (debug.messageEnabled()) {
                debug.message("PolicyManager.getOrgAliasMappedResourceNames(): "
                        + " orgName = " + org
                        + ":orgAliases=" + orgAliases);
            }
        } catch (SMSException sme) {
            String[] objs = { org };
            throw (new PolicyException(ResBundleUtils.rbName,
                    "unable_to_get_org_alias_for_org", objs, sme));
        }
        if (orgAliases != null) {
            Iterator iter = orgAliases.iterator();
            while (iter.hasNext()) {
                String orgAlias = (String)iter.next();
                managedResourceNames.add(ORG_ALIAS_URL_HTTP_PREFIX 
                        + orgAlias.trim() 
                        + ORG_ALIAS_URL_SUFFIX);
                managedResourceNames.add(ORG_ALIAS_URL_HTTPS_PREFIX 
                        + orgAlias.trim() 
                        + ORG_ALIAS_URL_SUFFIX);
            }
        }
        if (debug.messageEnabled()) {
            debug.message("PolicyManager.getOrgAliasMappedResourceNames(): "
                    + " returning: orgName = " + org
                    + ":orgAliases=" + orgAliases
                    + ":managedResourceNames=" + managedResourceNames);
        }
        return managedResourceNames;
    }

    public Set getManagedResourceNames(String serviceName)
            throws PolicyException {
        return (migratedToEntitlementService) ?
            getManagedResourceNamesE(serviceName) :
            getManagedResourceNamesO(serviceName);
    }

    public Set getManagedResourceNamesE(String serviceName) {
        try {
            Application appl = ApplicationManager.getApplication(
                PrivilegeManager.superAdminSubject, org, serviceName);
            return (appl == null) ? Collections.EMPTY_SET : appl.getResources();
        } catch (EntitlementException ex) {
            debug.error("PolicyManager.getManagedResourceNamesE", ex);
            return Collections.EMPTY_SET;
        }
    }


    public Set getManagedResourceNamesO(String serviceName)
            throws PolicyException {
        Set managedResourceNames = new HashSet();
        Set delegatedResourceNames = rm.getManagedResourceNames(serviceName);
        if (delegatedResourceNames != null) {
            managedResourceNames.addAll(delegatedResourceNames);
        }
        if (WEB_AGENT_SERVICE.equalsIgnoreCase(serviceName) 
                && PolicyConfig.orgAliasMappedResourcesEnabled() ) {
            managedResourceNames.addAll(getOrgAliasMappedResourceNames());
        }
        if (debug.messageEnabled()) {
            debug.message("PolicyManager.getManagedResourceNames(): "
                    + " returning: orgName = " + org
                    + ":serviceName=" + serviceName
                    + ":managedResourceNames=" + managedResourceNames);
        }
        return managedResourceNames;
    }

    public Set getManagedResourceNames() 
            throws PolicyException {
        Set managedResourceNames = rm.getManagedResourceNames();
        if ((managedResourceNames == null) 
                || (managedResourceNames == Collections.EMPTY_SET)) {
                    managedResourceNames = new HashSet();
        }
        managedResourceNames.addAll(getOrgAliasMappedResourceNames());
        if (debug.messageEnabled()) {
            debug.message("PolicyManager.getManagedResourceNames(): "
                    + " returning: orgName = " + org
                    + ":managedResourceNames=" + managedResourceNames);
        }
        return managedResourceNames;
    }

    String getOrgAliasWithResource(String resourceName) 
            throws PolicyException, SSOException {
        if (debug.messageEnabled()) {
            debug.message("PolicyManager.getOrgAliasWithResource(): "
                    + " orgName = " + org
                    + ", resourceName = " + resourceName);
        }
        if (resourceName == null) {
            return null;
        }
        String orgAlias = null;
        try {
            URL url = new URL(resourceName);
            orgAlias = url.getHost();
        } catch (MalformedURLException mfe) {
            String[] objs = { resourceName };
            if (debug.messageEnabled()) {
                debug.message("PolicyManager.getOrgAliasWithResource(): "
                        + " orgName = " + org
                        + ", resourceName = " + resourceName
                        + " is invalid URL, no org alias mapping can be found");
            }
        }
        return orgAlias;
    }

    String getOrgNameWithAlias(String orgAlias) 
            throws PolicyException, SSOException {
        String aliasMappedOrg = null;
        try {
            aliasMappedOrg = IdUtils.getOrganization(token, orgAlias);
        } catch (IdRepoException re) {
            //idRepo throws exception if there is no mapping
            if (debug.messageEnabled()) {
                debug.message("PolicyManager.getOrgNameWithAlias(): "
                        + " can not get orgName for orgAlias = " + orgAlias);
            }
        }
        if (debug.messageEnabled()) {
            debug.message("PolicyManager.getOrgNameWithAlias(): "
                    + " orgAlias = " + orgAlias
                    + ", mapped org = " + aliasMappedOrg);
        }
        return aliasMappedOrg;
    }

    public boolean canCreatePolicies(Set<String> services) 
        throws EntitlementException {
        String realm = DNMapper.orgNameToRealmName(getOrganizationDN());
        if (realm.equals("/")) {
            return true;
        }

        if (isMigratedToEntitlementService()) {
            for (String s : services) {
                Set<String> res = ApplicationManager.getReferredResources(
                    adminSubject, realm, s);
                if ((res != null) && !res.isEmpty()) {
                    return true;
                }
            }
            return false;
        } else {
            return canCreateNewResource(services) ||
                hasReferredResources();
        }
    }
    
    private boolean canCreateNewResource(Set<String> services) {
        boolean can = false;
        ResourceManager resMgr = getResourceManager();

        if (resMgr != null) {
            if ((services != null) && !services.isEmpty()) {
                for (Iterator i = services.iterator(); (i.hasNext() && !can);) {
                    String svcName = (String)i.next();
                    try {
                        can = resMgr.canCreateNewResource(svcName);
                    } catch (PolicyException  e) {
                        debug.warning("PolicyManager.canCreateNewResource",e);
                    }
                }
            }
        }

        return can;
    }

    private boolean hasReferredResources() {
        boolean hasPrefixes = false;
        try {
            Set prefixes = getManagedResourceNames();
            hasPrefixes = (prefixes != null) && !prefixes.isEmpty();
        } catch (PolicyException e) {
            debug.warning("PolicyManager.hasReferredResources", e);
        }
        return hasPrefixes;
    }

    static boolean isMigratedToEntitlementService() {
        return migratedToEntitlementService;
    }

    public boolean canCreateNewResource(String svcTypeName) {
        boolean can = false;
        if (migratedToEntitlementService) {
            ResourceManager resMgr = getResourceManager();
            if (resMgr != null) {
                try {
                    can = resMgr.canCreateNewResource(svcTypeName);
                } catch (PolicyException e) {
                    debug.warning("PolicyManager.canCreateNewResource",e);
                }
            }
        } else {
            String realm = DNMapper.orgNameToRealmName(getOrganizationDN());
            can = realm.equals("/");
        }
        return can;
    }
}
