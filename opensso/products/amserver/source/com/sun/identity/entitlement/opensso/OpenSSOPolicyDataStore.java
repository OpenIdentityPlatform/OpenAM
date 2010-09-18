/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
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
 * If applicable, addReferral the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: OpenSSOPolicyDataStore.java,v 1.7 2010/01/08 22:20:47 veiming Exp $
 */

package com.sun.identity.entitlement.opensso;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.entitlement.ApplicationPrivilege;
import com.sun.identity.entitlement.ApplicationPrivilegeManager;
import com.sun.identity.entitlement.EntitlementConfiguration;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.IPrivilege;
import com.sun.identity.entitlement.PolicyDataStore;
import com.sun.identity.entitlement.Privilege;
import com.sun.identity.entitlement.PrivilegeIndexStore;
import com.sun.identity.entitlement.PrivilegeManager;
import com.sun.identity.entitlement.ReferralPrivilege;
import com.sun.identity.policy.Policy;
import com.sun.identity.policy.PolicyException;
import com.sun.identity.policy.PolicyManager;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.xml.XMLUtils;
import com.sun.identity.sm.DNMapper;
import com.sun.identity.sm.SMSEntry;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;
import java.io.ByteArrayInputStream;
import java.security.AccessController;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import javax.security.auth.Subject;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 */
public class OpenSSOPolicyDataStore extends PolicyDataStore {
    private static final String POLICY_XML = "xmlpolicy";
    private static final String REALM_DN_TEMPLATE =
         "ou=Policies,ou=default,ou=OrganizationConfig,ou=1.0,ou=" +
         PolicyManager.POLICY_SERVICE_NAME + ",ou=services,{0}";
    private static Subject dsameUserSubject;
    private static SSOToken dsameUserToken;

    static {
        dsameUserToken = (SSOToken) AccessController.doPrivileged(
            AdminTokenAction.getInstance());
        dsameUserSubject = SubjectUtils.createSubject(dsameUserToken);
    }

    public void addPolicy(Subject subject, String realm,
        Privilege privilege)
        throws EntitlementException {
        ApplicationPrivilegeManager applPrivilegeMgr =
            ApplicationPrivilegeManager.getInstance(realm, subject);

        if (!applPrivilegeMgr.hasPrivilege(privilege,
            ApplicationPrivilege.Action.MODIFY)) {
            throw new EntitlementException(326);
        }

        String name = "";
        try {
            Object policy = PrivilegeUtils.privilegeToPolicyObject(
                realm, privilege);
            name = PrivilegeUtils.getPolicyName(policy);

            if (policy instanceof Policy ||
                policy instanceof com.sun.identity.entitlement.xacml3.core.Policy
            ) {
                String dn = getPolicyDistinguishedName(realm, name);
                createParentNode(dsameUserToken, realm);
                SMSEntry s = new SMSEntry(dsameUserToken, dn);
                Map<String, Set<String>> map = new
                    HashMap<String, Set<String>>();

                Set<String> setServiceID = new HashSet<String>(2);
                map.put(SMSEntry.ATTR_SERVICE_ID, setServiceID);
                setServiceID.add("NamedPolicy");

                Set<String> setObjectClass = new HashSet<String>(4);
                map.put(SMSEntry.ATTR_OBJECTCLASS, setObjectClass);
                setObjectClass.add(SMSEntry.OC_TOP);
                setObjectClass.add(SMSEntry.OC_SERVICE_COMP);

                Set<String> setValue = new HashSet<String>(2);
                map.put(SMSEntry.ATTR_KEYVAL, setValue);
                setValue.add(POLICY_XML + "=" +
                    PrivilegeUtils.policyToXML(policy));
                s.setAttributes(map);

                String[] logParams = {DNMapper.orgNameToRealmName(realm),
                    name};
                OpenSSOLogger.log(OpenSSOLogger.LogLevel.MESSAGE, Level.INFO,
                    "ATTEMPT_ADD_PRIVILEGE", logParams, subject);

                s.save();

                OpenSSOLogger.log(OpenSSOLogger.LogLevel.MESSAGE, Level.INFO,
                    "SUCCEEDED_ADD_PRIVILEGE", logParams, subject);

                PrivilegeIndexStore pis = PrivilegeIndexStore.getInstance(
                    dsameUserSubject, realm);
                Set<IPrivilege> privileges = new HashSet<IPrivilege>();
                privileges.add(privilege);
                pis.add(privileges);
            } else {
                PrivilegeManager.debug.error(
                    "OpenSSOPolicyDataStore.addPolicy: unknown class " +
                    policy.getClass().getName(), null);
            }
        } catch (PolicyException e) {
            String[] logParams = {DNMapper.orgNameToRealmName(realm),
                name, e.getMessage()};
            OpenSSOLogger.log(OpenSSOLogger.LogLevel.ERROR, Level.INFO,
                "FAILED_ADD_PRIVILEGE", logParams, subject);
            Object[] params = {name};
            throw new EntitlementException(202, params, e);
        } catch (SSOException e) {
            String[] logParams = {DNMapper.orgNameToRealmName(realm),
                name, e.getMessage()};
            OpenSSOLogger.log(OpenSSOLogger.LogLevel.ERROR, Level.INFO,
                "FAILED_ADD_PRIVILEGE", logParams, subject);
            Object[] params = {name};
            throw new EntitlementException(202, params, e);
        } catch (SMSException e) {
            String[] logParams = {DNMapper.orgNameToRealmName(realm),
                name, e.getMessage()};
            OpenSSOLogger.log(OpenSSOLogger.LogLevel.ERROR, Level.INFO,
                "FAILED_ADD_PRIVILEGE", logParams, subject);
            Object[] params = {name};
            throw new EntitlementException(202, params, e);
        }
    }

    private void createParentNode(
        SSOToken adminToken,
        String realm
    ) throws SSOException, SMSException {
        ServiceConfig orgConf = getOrgConfig(adminToken, realm);
        Set<String> subConfigNames = orgConf.getSubConfigNames();
        if (!subConfigNames.contains(PolicyDataStore.POLICIES)) {
            orgConf.addSubConfig(PolicyDataStore.POLICIES,
                PolicyDataStore.POLICIES, 0, null);
        }
    }

    private ServiceConfig getOrgConfig(SSOToken adminToken, String realm)
        throws SMSException, SSOException {
        ServiceConfigManager mgr = new ServiceConfigManager(
            PolicyManager.POLICY_SERVICE_NAME, adminToken);
        ServiceConfig orgConf = mgr.getOrganizationConfig(realm, null);
        if (orgConf == null) {
            mgr.createOrganizationConfig(realm, null);
            orgConf = mgr.getOrganizationConfig(realm, null);
        }
        return orgConf;
    }

    public Object getPolicy(Subject adminSubject, String realm, String name)
        throws EntitlementException {
        SSOToken adminToken = SubjectUtils.getSSOToken(adminSubject);

        if (adminToken == null) {
            Object[] params = {name};
            throw new EntitlementException(209, params);
        }

        String dn = getPolicyDistinguishedName(realm, name);

        if (!SMSEntry.checkIfEntryExists(dn, adminToken)) {
            Object[] params = {name};
            throw new EntitlementException(203, params);
        }
        try {
            SMSEntry s = new SMSEntry(adminToken, dn);
            Map<String, Set<String>> map = s.getAttributes();
            Set<String> xml = map.get(SMSEntry.ATTR_KEYVAL);
            String strXML = xml.iterator().next();
            if (strXML.startsWith(POLICY_XML)) {
                strXML = strXML.substring(POLICY_XML.length() +1);
            }
            return createPolicy(adminToken, realm, strXML);
        } catch (SSOException ex) {
            Object[] params = {name};
            throw new EntitlementException(204, params, ex);
        } catch (SMSException ex) {
            Object[] params = {name};
            throw new EntitlementException(204, params, ex);
        } catch (Exception ex) {
            Object[] params = {name};
            throw new EntitlementException(204, params, ex);
        }
    }

    public ReferralPrivilege getReferral(
        Subject adminSubject,
        String realm,
        String name
    ) throws EntitlementException {
        SSOToken adminToken =
            (adminSubject == PrivilegeManager.superAdminSubject) ?
                dsameUserToken : SubjectUtils.getSSOToken(adminSubject);
        
        if (adminToken == null) {
            Object[] params = {name};
            throw new EntitlementException(262, params);
        }

        String dn = getPolicyDistinguishedName(realm, name);

        if (!SMSEntry.checkIfEntryExists(dn, adminToken)) {
            Object[] params = {name};
            throw new EntitlementException(263, params);
        }
        try {
            SMSEntry s = new SMSEntry(adminToken, dn);
            Map<String, Set<String>> map = s.getAttributes();
            Set<String> set = map.get(SMSEntry.ATTR_KEYVAL);
            String xml = set.iterator().next();
            if (xml.startsWith(POLICY_XML)) {
                xml = xml.substring(POLICY_XML.length() +1);
            }
            Set<IPrivilege> privileges = PrivilegeUtils.policyToPrivileges(
                createPolicy(adminToken, realm, xml));
            return (ReferralPrivilege)privileges.iterator().next();
        } catch (SSOException ex) {
            Object[] params = {name};
            throw new EntitlementException(204, params, ex);
        } catch (SMSException ex) {
            Object[] params = {name};
            throw new EntitlementException(204, params, ex);
        } catch (Exception ex) {
            Object[] params = {name};
            throw new EntitlementException(204, params, ex);
        }
    }

    private Object createPolicy(SSOToken adminToken, String realm, String xml)
        throws Exception, 
        SSOException, PolicyException {
        Object policy = null;

        if (xml.startsWith("xmlpolicy=")) {
            xml = xml.substring(10);
        }

        Document doc = XMLUtils.getXMLDocument(
            new ByteArrayInputStream(xml.getBytes("UTF8")));
        if (EntitlementConfiguration.getInstance(
                SubjectUtils.createSubject(adminToken),
                "/").xacmlPrivilegeEnabled()) {
            //TODO: create xacml policy from xml document
        } else {
            PolicyManager pm = new PolicyManager(adminToken, realm);
            Node rootNode = XMLUtils.getRootNode(doc, 
                PolicyManager.POLICY_ROOT_NODE);
            policy = new Policy(pm, rootNode);
        }
        return policy;
    }

    public void removePrivilege(Subject subject, String realm,
        Privilege privilege)
        throws EntitlementException {
        SSOToken adminToken = SubjectUtils.getSSOToken(subject);
        String name = privilege.getName();

        if (adminToken == null) {
            Object[] params = {name};
            throw new EntitlementException(211, params);
        }

        ApplicationPrivilegeManager applPrivilegeMgr =
            ApplicationPrivilegeManager.getInstance(realm, subject);

        if (!applPrivilegeMgr.hasPrivilege(privilege,
            ApplicationPrivilege.Action.MODIFY)) {
            throw new EntitlementException(326);
        }
        
        String dn = getPolicyDistinguishedName(realm, name);

        if (!SMSEntry.checkIfEntryExists(dn, dsameUserToken)) {
            Object[] params = {name};
            throw new EntitlementException(203, params);
        }
        try {
            String[] logParams = {DNMapper.orgNameToRealmName(realm),
                name};
            OpenSSOLogger.log(OpenSSOLogger.LogLevel.MESSAGE, Level.INFO,
                "ATTEMPT_REMOVE_PRIVILEGE", logParams, subject);
            SMSEntry s = new SMSEntry(dsameUserToken, dn);
            s.delete();
            OpenSSOLogger.log(OpenSSOLogger.LogLevel.MESSAGE, Level.INFO,
                "SUCCEEDED_REMOVE_PRIVILEGE", logParams, subject);

            PrivilegeIndexStore pis = PrivilegeIndexStore.getInstance(
                dsameUserSubject, realm);
            pis.delete(name);
        } catch (SSOException ex) {
            String[] logParams = {DNMapper.orgNameToRealmName(realm),
                name, ex.getMessage()};
            OpenSSOLogger.log(OpenSSOLogger.LogLevel.ERROR, Level.INFO,
                "FAILED_REMOVE_PRIVILEGE", logParams, subject);
            Object[] params = {name};
            throw new EntitlementException(205, params, ex);
        } catch (SMSException ex) {
            String[] logParams = {DNMapper.orgNameToRealmName(realm),
                name, ex.getMessage()};
            OpenSSOLogger.log(OpenSSOLogger.LogLevel.ERROR, Level.INFO,
                "FAILED_REMOVE_PRIVILEGE", logParams, subject);
            Object[] params = {name};
            throw new EntitlementException(205, params, ex);
        }
    }

    private static String getPolicyDistinguishedName(String realm, String name)
    {
        return "ou=" + name + "," + getStoreBaseDN(realm);
    }

    private static String getStoreBaseDN(String realm) {
        Object[] args = {DNMapper.orgNameToDN(realm)};
        return MessageFormat.format(REALM_DN_TEMPLATE, args);
    }

    public void addReferral(
        Subject subject,
        String realm,
        ReferralPrivilege referral)
        throws EntitlementException {
        String name = referral.getName();
        String dn = getPolicyDistinguishedName(realm, name);

        SSOToken adminToken = SubjectUtils.getSSOToken(subject);
        if (adminToken == null) {
            Object[] params = {name};
            throw new EntitlementException(260, params);
        }
        
        ApplicationPrivilegeManager applPrivilegeMgr =
            ApplicationPrivilegeManager.getInstance(realm, subject);

        if (!applPrivilegeMgr.hasPrivilege(referral,
            ApplicationPrivilege.Action.MODIFY)) {
            throw new EntitlementException(326);
        }

        try {
            createParentNode(dsameUserToken, realm);

            SMSEntry s = new SMSEntry(dsameUserToken, dn);
            Map<String, Set<String>> map = new HashMap<String, Set<String>>();

            Set<String> setServiceID = new HashSet<String>(2);
            map.put(SMSEntry.ATTR_SERVICE_ID, setServiceID);
            setServiceID.add("NamedPolicy");

            Set<String> setObjectClass = new HashSet<String>(4);
            map.put(SMSEntry.ATTR_OBJECTCLASS, setObjectClass);
            setObjectClass.add(SMSEntry.OC_TOP);
            setObjectClass.add(SMSEntry.OC_SERVICE_COMP);

            Set<String> setValue = new HashSet<String>(2);
            map.put(SMSEntry.ATTR_KEYVAL, setValue);
            Policy p = PrivilegeUtils.referralPrivilegeToPolicy(
                realm, referral);
            setValue.add(POLICY_XML + "=" +p.toXML());
            s.setAttributes(map);

            String[] logParams = {DNMapper.orgNameToRealmName(realm), name};
            OpenSSOLogger.log(OpenSSOLogger.LogLevel.MESSAGE, Level.INFO,
                "ATTEMPT_ADD_REFERRAL", logParams, subject);
            s.save();
            OpenSSOLogger.log(OpenSSOLogger.LogLevel.MESSAGE, Level.INFO,
                "SUCCEEDED_ADD_REFERRAL", logParams, subject);

            PrivilegeIndexStore pis = PrivilegeIndexStore.getInstance(
                dsameUserSubject, realm);
            Set<IPrivilege> tmp = new HashSet<IPrivilege>();
            tmp.add(referral);
            pis.add(tmp);
        } catch (PolicyException e) {
            String[] logParams = {DNMapper.orgNameToRealmName(realm), name,
                e.getMessage()};
            OpenSSOLogger.log(OpenSSOLogger.LogLevel.ERROR, Level.INFO,
                "FAILED_ADD_REFERRAL", logParams, subject);
            Object[] params = {name};
            throw new EntitlementException(261, params, e);
        } catch (SSOException e) {
            String[] logParams = {DNMapper.orgNameToRealmName(realm), name,
                e.getMessage()};
            OpenSSOLogger.log(OpenSSOLogger.LogLevel.ERROR, Level.INFO,
                "FAILED_ADD_REFERRAL", logParams, subject);
            Object[] params = {name};
            throw new EntitlementException(261, params, e);
        } catch (SMSException e) {
            String[] logParams = {DNMapper.orgNameToRealmName(realm), name,
                e.getMessage()};
            OpenSSOLogger.log(OpenSSOLogger.LogLevel.ERROR, Level.INFO,
                "FAILED_ADD_REFERRAL", logParams, subject);
            Object[] params = {name};
            throw new EntitlementException(261, params, e);
        }
    }

    public void removeReferral(
        Subject subject,
        String realm,
        ReferralPrivilege referral
    ) throws EntitlementException {
        SSOToken adminToken = SubjectUtils.getSSOToken(subject);
        String name = referral.getName();

        if (adminToken == null) {
            Object[] params = {name};
            throw new EntitlementException(266, params);
        }
        ApplicationPrivilegeManager applPrivilegeMgr =
            ApplicationPrivilegeManager.getInstance(realm, subject);

        if (!applPrivilegeMgr.hasPrivilege(referral,
            ApplicationPrivilege.Action.MODIFY)) {
            throw new EntitlementException(326);
        }

        String dn = getPolicyDistinguishedName(realm, name);

        if (!SMSEntry.checkIfEntryExists(dn, dsameUserToken)) {
            Object[] params = {name};
            throw new EntitlementException(263, params);
        }
        try {
            String[] logParams = {DNMapper.orgNameToRealmName(realm),
                name};
            OpenSSOLogger.log(OpenSSOLogger.LogLevel.MESSAGE, Level.INFO,
                "ATTEMPT_REMOVE_REFERRAL", logParams, subject);

            SMSEntry s = new SMSEntry(dsameUserToken, dn);
            s.delete();

            OpenSSOLogger.log(OpenSSOLogger.LogLevel.MESSAGE, Level.INFO,
                "SUCCEEDED_REMOVE_REFERRAL", logParams, subject);

            PrivilegeIndexStore pis = PrivilegeIndexStore.getInstance(
                dsameUserSubject, realm);
            pis.deleteReferral(name);
        } catch (SSOException ex) {
            String[] logParams = {DNMapper.orgNameToRealmName(realm),
                name, ex.getMessage()};
            OpenSSOLogger.log(OpenSSOLogger.LogLevel.ERROR, Level.INFO,
                "FAILED_REMOVE_REFERRAL", logParams, subject);
            Object[] params = {name};
            throw new EntitlementException(205, params, ex);
        } catch (SMSException ex) {
            String[] logParams = {DNMapper.orgNameToRealmName(realm),
                name, ex.getMessage()};
            OpenSSOLogger.log(OpenSSOLogger.LogLevel.ERROR, Level.INFO,
                "FAILED_REMOVE_REFERRAL", logParams, subject);
            Object[] params = {name};
            throw new EntitlementException(205, params, ex);
        }
    }
}
