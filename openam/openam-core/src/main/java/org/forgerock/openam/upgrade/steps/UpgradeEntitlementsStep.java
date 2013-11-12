/*
 * Copyright 2013 ForgeRock AS.
 *
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 */
package org.forgerock.openam.upgrade.steps;

import com.iplanet.sso.SSOException;
import com.sun.identity.entitlement.Privilege;
import com.sun.identity.entitlement.interfaces.ISaveIndex;
import com.sun.identity.entitlement.opensso.EntitlementService;
import com.sun.identity.policy.Policy;
import com.sun.identity.policy.PolicyManager;
import com.sun.identity.policy.Rule;
import com.sun.identity.shared.datastruct.CollectionHelper;
import com.sun.identity.sm.SMSEntry;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.forgerock.openam.upgrade.UpgradeException;
import org.forgerock.openam.upgrade.UpgradeProgress;
import static org.forgerock.openam.upgrade.UpgradeServices.LF;
import static org.forgerock.openam.upgrade.UpgradeServices.tagSwapReport;
import org.forgerock.openam.upgrade.UpgradeStepInfo;
import static org.forgerock.openam.utils.CollectionUtils.*;
import org.forgerock.openam.utils.IOUtils;
import org.forgerock.opendj.ldap.Connection;
import org.forgerock.opendj.ldap.ModificationType;
import org.forgerock.opendj.ldap.SearchScope;
import org.forgerock.opendj.ldap.requests.ModifyRequest;
import org.forgerock.opendj.ldap.requests.Requests;
import org.forgerock.opendj.ldap.requests.SearchRequest;
import org.forgerock.opendj.ldap.responses.SearchResultEntry;
import org.forgerock.opendj.ldif.ConnectionEntryReader;
import org.json.JSONObject;

/**
 * Upgrades the entitlements stored in the configuration. The following steps are taken:
 * <ul>
 *  <li>Upgrades the iPlanetAMWebAgentService applicationType to ensure that it uses the new SearchIndex/SaveIndex
 *      implementations.</li>
 *  <li>Traverses through the realms and finds upgradable referrals. As part of other improvements on the entitlements
 *      framework, the referrals are now handling wildcards similarly to policies (earlier using * or *?* in a referral
 *      policy wasn't a requirement, but now it is), hence the existing referrals will be modified to include the * and
 *      *?* resourceNames as well, however if the referrals are already using the wildcards, then the referral won't be
 *      modified.</li>
 *  <li>Traverses through the realms and resaves the policies, so the entitlement indexes for all the resources are
 *      regenerated.</li>
 * </ul>
 *
 * The referrals are upgraded using the {@link PolicyManager} API, while the policies are upgraded directly via DJ SDK.
 * The reason behind this is that recreating lots of entitlement indexes has proven to be very slow considering the
 * actual difference between the old and the new formats, hence instead of doing DELETE/ADD iterations it's all MODIFY
 * operations now. The referral related upgrade requirements on the other hand are more complex (creating new referral
 * rules that is), so that is using the Policy API. The performance impact is not expected to be as big as with
 * policies, since in general the number of referral rules normally is much much lower than the number of policy rules.
 *
 * @author Peter Major
 */
@UpgradeStepInfo(dependsOn = "org.forgerock.openam.upgrade.steps.UpgradeServiceSchemaStep")
public class UpgradeEntitlementsStep extends AbstractUpgradeStep {

    private static final String ENTITLEMENT_INDEX_FILTER = "(&(sunserviceID=indexes)(sunxmlKeyValue=pathindex=*)"
            + "(!(o:dn:=sunamhiddenrealmdelegationservicepermissions))(!(ou:dn:=referrals)))";
    private static final String ENTITLEMENT_DATA = "%ENTITLEMENT_DATA%";
    private static final String DEFAULT_APP_TYPE = "iPlanetAMWebAgentService";
    private static final String SEARCH_INDEX_IMPL = "searchIndexImpl";
    private static final String SAVE_INDEX_IMPL = "saveIndexImpl";
    private static final String NEW_SEARCH_IMPL = "org.forgerock.openam.entitlement.indextree.TreeSearchIndex";
    private static final String NEW_SAVE_IMPL = "org.forgerock.openam.entitlement.indextree.TreeSaveIndex";
    public static final String SERIALIZABLE_PREFIX = "serializable=";
    public static final String SUN_KEY_VALUE = "sunkeyvalue";
    public static final String PATH_INDEX_PREFIX = "pathindex=";
    public static final String SUN_XML_KEY_VALUE = "sunxmlKeyValue";
    private final Map<String, Map<PolicyType, Set<String>>> upgradableConfigs =
            new LinkedHashMap<String, Map<PolicyType, Set<String>>>();
    private int policyRuleCount = 0;

    @Override
    public boolean isApplicable() {
        return !upgradableConfigs.isEmpty();
    }

    @Override
    public void initialize() throws UpgradeException {
        try {
            DEBUG.message("Initializing UpgradeEntitlementsStep");
            ServiceConfig appType = getDefaultApplicationType();
            Map<String, Set<String>> attrs = appType.getAttributes();
            String searchImpl = CollectionHelper.getMapAttr(attrs, SEARCH_INDEX_IMPL);
            String saveImpl = CollectionHelper.getMapAttr(attrs, SAVE_INDEX_IMPL);
            if (NEW_SEARCH_IMPL.equals(searchImpl) && NEW_SAVE_IMPL.equals(saveImpl)) {
                DEBUG.message("The entitlements framework is already using the new TreeSearchIndex/TreeSaveIndex"
                        + " implementations");
            } else {
                for (String realm : getRealmNames()) {
                    Map<PolicyType, Set<String>> map = new EnumMap<PolicyType, Set<String>>(PolicyType.class);
                    PolicyManager pm = new PolicyManager(getAdminToken(), realm);
                    Set<String> policyNames = pm.getPolicyNames();
                    for (String policyName : policyNames) {
                        Policy policy = pm.getPolicy(policyName);
                        PolicyType type;
                        if (policy.isReferralPolicy()) {
                            type = PolicyType.REFERRAL;
                        } else {
                            //There is a small edgecase here in case a rule contains multiple resourcenames, but that
                            //isn't quite a supported case anyways
                            policyRuleCount += policy.getRuleNames().size();
                            type = PolicyType.POLICY;
                        }
                        Set<String> values = map.get(type);
                        if (values == null) {
                            values = new HashSet<String>();
                        }
                        values.add(policyName);
                        map.put(type, values);
                        upgradableConfigs.put(realm, map);
                    }
                }
                if (DEBUG.messageEnabled()) {
                    DEBUG.message("Discovered following policies/referrals:\n" + upgradableConfigs);
                }
            }
        } catch (Exception ex) {
            DEBUG.error("Error while trying to detect changes in entitlements", ex);
            throw new UpgradeException(ex);
        }
    }

    @Override
    public void perform() throws UpgradeException {
        try {
            ServiceConfig appType = getDefaultApplicationType();
            Map<String, Set<String>> attrs = new HashMap<String, Set<String>>();
            UpgradeProgress.reportStart("upgrade.apptype.start");
            attrs.put(SEARCH_INDEX_IMPL, asSet(NEW_SEARCH_IMPL));
            attrs.put(SAVE_INDEX_IMPL, asSet(NEW_SAVE_IMPL));
            appType.setAttributes(attrs);
            UpgradeProgress.reportEnd("upgrade.success");
            DEBUG.message("Entitlement service is now using the new TreeSearchIndex/TreeSaveIndex implementations");
            for (Map.Entry<String, Map<PolicyType, Set<String>>> entry : upgradableConfigs.entrySet()) {
                String realm = entry.getKey();
                Map<PolicyType, Set<String>> changes = entry.getValue();

                PolicyManager pm = new PolicyManager(getAdminToken(), realm);
                Set<String> referrals = changes.get(PolicyType.REFERRAL);
                //we should handle referrals first to ensure the policies have their corresponding policies all set up
                if (referrals != null) {
                    upgradeReferrals(pm, referrals);
                }
            }
            //the entitlements are upgraded regardless of the realms
            upgradeEntitlementIndexes();
        } catch (Exception ex) {
            UpgradeProgress.reportEnd("upgrade.failed");
            DEBUG.error("An error occurred while upgrading entitlements data", ex);
            throw new UpgradeException(ex);
        }
    }

    @Override
    public String getShortReport(String delimiter) {
        int referralCount = 0;
        for (Map<PolicyType, Set<String>> map : upgradableConfigs.values()) {
            Set<String> tmp = map.get(PolicyType.REFERRAL);
            if (tmp != null) {
                referralCount += tmp.size();
            }
        }
        StringBuilder sb = new StringBuilder();
        if (referralCount != 0) {
            sb.append(BUNDLE.getString("upgrade.entitlement.referrals")).append(" (").append(referralCount).append(')')
                    .append(delimiter);
        }
        if (policyRuleCount != 0) {
            sb.append(BUNDLE.getString("upgrade.entitlement.policies")).append(" (").append(policyRuleCount).append(')')
                    .append(delimiter);
        }
        return sb.toString();
    }

    @Override
    public String getDetailedReport(String delimiter) {
        Map<String, String> tags = new HashMap<String, String>();
        tags.put(LF, delimiter);
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Map<PolicyType, Set<String>>> entry : upgradableConfigs.entrySet()) {
            sb.append(BUNDLE.getString("upgrade.realm")).append(": ").append(entry.getKey()).append(delimiter);
            for (Map.Entry<PolicyType, Set<String>> changes : entry.getValue().entrySet()) {
                sb.append(INDENT).append(changes.getKey()).append(delimiter);
                for (String subConfig : changes.getValue()) {
                    sb.append(INDENT).append(INDENT).append(subConfig).append(delimiter);
                }
            }
        }
        tags.put(ENTITLEMENT_DATA, sb.toString());
        return tagSwapReport(tags, "upgrade.entitlementreport");
    }

    /**
     * Returns the default application type's (iPlanetAMWebAgentService) configuration.
     *
     * @return The ServiceConfig for the default application type.
     * @throws SMSException If there was an error while retrieving the configuration.
     * @throws SSOException If there was an error while retrieving the configuration.
     */
    private ServiceConfig getDefaultApplicationType() throws SMSException, SSOException {
        ServiceConfigManager scm = new ServiceConfigManager(EntitlementService.SERVICE_NAME, getAdminToken());
        ServiceConfig globalConfig = scm.getGlobalConfig(null);
        ServiceConfig appTypes = globalConfig.getSubConfig("applicationTypes");
        return appTypes.getSubConfig(DEFAULT_APP_TYPE);
    }

    /**
     * Collects the resourcenames defined within the policy and returns all of them in a Set.
     *
     * @param policy The policy we need to collect the resourcenames from.
     * @return All the resourcenames defined within the passed in policy (it also handles the case when a rule contains
     * more than one resourcenames).
     * @throws Exception If there was an error while retrieving the policy rule.
     */
    private Set<String> getResourceNames(Policy policy) throws Exception {
        Set<String> ruleNames = policy.getRuleNames();
        Set<String> ret = new HashSet<String>(ruleNames.size());
        for (String ruleName : ruleNames) {
            ret.addAll(policy.getRule(ruleName).getResourceNames());
        }
        return ret;
    }

    /**
     * Creates a new rule in the policy that will be similar to the template rule provided. The main difference will be
     * that the new rule with have a different rulename, than the original, and will include the new resourcename only.
     *
     * @param policy The policy that needs to be modified.
     * @param template A rule that can be used as a template when creating the new policy rule.
     * @param existingResourceNames The names of resources that are already defined in the policy.
     * @param newResourceName The name of the new resource that needs to be added to the policy.
     * @throws Exception If there was an error while adding the new rule to the policy.
     */
    private void addSimilarPolicyRule(Policy policy, Rule template, Set<String> existingResourceNames,
            String newResourceName) throws Exception {
        if (!existingResourceNames.contains(newResourceName)) {
            Rule newRule = (Rule) template.clone();
            newRule.setResourceNames(asSet(newResourceName));
            int i = 1;
            String oldName = newRule.getName();
            String newName = oldName;
            while (policy.getRuleNames().contains(newName)) {
                newName = oldName + "_" + i++;
            }
            newRule.setName(newName);
            policy.addRule(newRule);
        }
    }

    private void upgradeReferrals(PolicyManager pm, Set<String> referrals) throws Exception {
        for (String referralName : referrals) {
            if (DEBUG.messageEnabled()) {
                DEBUG.message("Upgrading referral: " + referralName);
            }
            Policy referral = pm.getPolicy(referralName);
            Set<String> resourceNames = getResourceNames(referral);
            Set<String> currentRuleNames = new HashSet<String>(referral.getRuleNames());
            for (String ruleName : currentRuleNames) {
                Rule rule = referral.getRule(ruleName);
                for (String definedResourceName : rule.getResourceNames()) {
                    if (definedResourceName.endsWith("*?*")) {
                        //This is a special case we don't want to handle for referrals
                    } else if (definedResourceName.endsWith("*")) {
                        //define a new *?* resource within this referral
                        addSimilarPolicyRule(referral, rule, resourceNames, definedResourceName + "?*");
                    } else {
                        //no wildcard at the end of the resource name, we should create both * and *?*
                        addSimilarPolicyRule(referral, rule, resourceNames, definedResourceName + "*");
                        addSimilarPolicyRule(referral, rule, resourceNames, definedResourceName + "*?*");
                    }
                }
            }
            UpgradeProgress.reportStart("upgrade.entitlement.referral", referral.getName());
            //in either case we need to replace this referral to ensure the indexes are updated
            pm.replacePolicy(referral);
            UpgradeProgress.reportEnd("upgrade.success");
        }
    }

    private void upgradeEntitlementIndexes() throws UpgradeException {
        Connection conn = null;
        Connection modConn = null;
        try {
            conn = getConnection();
            //obtaining a second connection to perform the modifications.
            modConn = getConnection();
            SearchRequest sr = Requests.newSearchRequest(SMSEntry.getRootSuffix(), SearchScope.WHOLE_SUBTREE,
                    ENTITLEMENT_INDEX_FILTER, SUN_KEY_VALUE, SUN_XML_KEY_VALUE);
            ConnectionEntryReader reader = conn.search(sr);
            int counter = 0;
            long lastReport = System.currentTimeMillis();
            while (reader.hasNext()) {
                if (reader.isEntry()) {
                    if (System.currentTimeMillis() - lastReport > 3000) {
                        UpgradeProgress.reportEnd("upgrade.entitlement.privilege", counter, policyRuleCount);
                        lastReport = System.currentTimeMillis();
                    }
                    SearchResultEntry entry = reader.readEntry();
                    Set<String> newValues = processEntry(entry);
                    ModifyRequest modifyRequest = Requests.newModifyRequest(entry.getName());
                    modifyRequest.addModification(ModificationType.REPLACE, SUN_XML_KEY_VALUE, newValues.toArray());
                    if (DEBUG.messageEnabled()) {
                        DEBUG.message("Upgrading entitlements index for: " + entry.getName());
                    }
                    modConn.modify(modifyRequest);
                    counter++;
                } else {
                    reader.readReference();
                }
            }
            UpgradeProgress.reportEnd("upgrade.entitlement.privilege", policyRuleCount, policyRuleCount);
        } catch (Exception ex) {
            DEBUG.error("An error occured while upgrading the entitlement indexes", ex);
            throw new UpgradeException(ex);
        } finally {
            IOUtils.closeIfNotNull(conn);
            IOUtils.closeIfNotNull(modConn);
        }
    }

    private Set<String> processEntry(SearchResultEntry entry) throws Exception {
        Set<String> keyValues = entry.parseAttribute(SUN_KEY_VALUE).asSetOfString();
        Set<String> newPathIndexes = new HashSet<String>(1);
        ISaveIndex saveImpl = Class.forName(NEW_SAVE_IMPL).asSubclass(ISaveIndex.class).newInstance();
        for (String value : keyValues) {
            if (value.startsWith(SERIALIZABLE_PREFIX)) {
                String jsonData = value.substring(SERIALIZABLE_PREFIX.length());
                Privilege privilege = Privilege.getInstance(new JSONObject(jsonData));
                Set<String> pathIndexes = generatePathIndexes(saveImpl, privilege.getEntitlement().getResourceNames());
                for (String pathIndex : pathIndexes) {
                    newPathIndexes.add(PATH_INDEX_PREFIX + pathIndex);
                }
            }
        }
        Set<String> values = entry.parseAttribute(SUN_XML_KEY_VALUE).asSetOfString();
        Iterator<String> it = values.iterator();
        while (it.hasNext()) {
            if (it.next().startsWith(PATH_INDEX_PREFIX)) {
                it.remove();
            }
        }
        values.addAll(newPathIndexes);
        return values;
    }

    private Set<String> generatePathIndexes(ISaveIndex saveImpl, Set<String> resourceNames) throws Exception {
        Set<String> ret = new HashSet<String>(1);
        if (resourceNames != null) {
            for (String resourceName : resourceNames) {
                ret.addAll(saveImpl.getIndexes(resourceName).getPathIndexes());
            }
        }
        return ret;
    }

    private enum PolicyType {

        REFERRAL("upgrade.referral"),
        POLICY("upgrade.policy");
        private String i18nKey;

        private PolicyType(String i18nKey) {
            this.i18nKey = i18nKey;
        }

        @Override
        public String toString() {
            return BUNDLE.getString(i18nKey);
        }
    }
}
