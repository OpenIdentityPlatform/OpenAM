/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: PolicyPrivilegeManager.java,v 1.9 2010/01/26 20:10:15 dillidorai Exp $
 *
 * Portions Copyrighted 2014-2016 ForgeRock AS.
 */
package com.sun.identity.entitlement.opensso;

import static org.forgerock.openam.utils.Time.*;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.entitlement.Application;
import com.sun.identity.entitlement.ApplicationManager;
import com.sun.identity.entitlement.ApplicationPrivilege;
import com.sun.identity.entitlement.ApplicationPrivilegeManager;
import com.sun.identity.entitlement.EntitlementConfiguration;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.IPrivilege;
import com.sun.identity.entitlement.PolicyDataStore;
import com.sun.identity.entitlement.Privilege;
import com.sun.identity.entitlement.PrivilegeChangeNotifier;
import com.sun.identity.entitlement.PrivilegeIndexStore;
import com.sun.identity.entitlement.PrivilegeManager;
import com.sun.identity.entitlement.xacml3.XACMLPrivilegeUtils;
import com.sun.identity.policy.Policy;
import com.sun.identity.policy.PolicyCache;
import com.sun.identity.policy.PolicyEvent;
import com.sun.identity.policy.PolicyException;
import com.sun.identity.policy.PolicyManager;
import com.sun.identity.security.AdminTokenAction;
import org.forgerock.openam.entitlement.constraints.ConstraintValidator;
import org.forgerock.openam.entitlement.service.ApplicationServiceFactory;
import org.forgerock.openam.entitlement.service.ResourceTypeService;

import java.security.AccessController;
import java.security.Principal;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;
import javax.security.auth.Subject;

/**
 * Implementation of <code>PrivilegeManager</code> that saves privileges as <code>com.sun.identity.policy</code> objects
 */
public class PolicyPrivilegeManager extends PrivilegeManager {

    private static boolean migratedToEntitlementSvc = false;
    private static boolean xacmlEnabled = false;
    private String realm = "/";
    private PolicyManager pm;
    private static PolicyCache policyCache;
    private static Subject dsameUserSubject;

    static {
        SSOToken adminToken = AccessController.doPrivileged(AdminTokenAction.getInstance());
        dsameUserSubject = SubjectUtils.createSubject(adminToken);
        EntitlementConfiguration ec = EntitlementConfiguration.getInstance(dsameUserSubject, "/");
        migratedToEntitlementSvc = ec.migratedToEntitlementService();
        xacmlEnabled = ec.xacmlPrivilegeEnabled();
        try {
            if (PrivilegeManager.debug.messageEnabled()) {
                PrivilegeManager.debug.message(
                        "PolicyPrivilegeManager.static initializer, getting instance of PolicyCache");
            }
            policyCache = PolicyCache.getInstance();
        } catch (Exception e) {
            PrivilegeManager.debug.error("PolicyPrivilegeManager.static initializer failed to create PolicyCache", e);
        }
    }

    /**
     * Creates instance of <code>PolicyPrivilegeManager</code>
     */
    @Inject
    public PolicyPrivilegeManager(ApplicationServiceFactory applicationServiceFactory,
                                  ResourceTypeService resourceTypeService,
                                  ConstraintValidator constraintValidator) {
        super(applicationServiceFactory, resourceTypeService, constraintValidator);
    }

    /**
     * Initializes the object
     * @param subject subject that would be used for privilege management
     * operations
     */
    @Override
    public void initialize(String realm, Subject subject) {
        super.initialize(realm, subject);
        this.realm = realm;
        SSOToken ssoToken = SubjectUtils.getSSOToken(subject);

        if (!migratedToEntitlementSvc) {
            try {
                pm = new PolicyManager(ssoToken, realm);
            } catch (SSOException e) {
                PrivilegeManager.debug.error("PolicyPrivilegeManager.initialize", e);
            } catch (PolicyException e) {
                PrivilegeManager.debug.error("PolicyPrivilegeManager.initialize", e);
            }
        }
    }

    /**
     * Finds a privilege by its unique name.
     *
     * @param name name of the privilege to be returned
     * @throws com.sun.identity.entitlement.EntitlementException if privilege is not found.
     */
    @Override
    public Privilege findByName(String name)
        throws EntitlementException {
        return findByName(name, getAdminSubject());
    }

    @Override
    public Privilege findByName(String privilegeName, Subject adminSubject) throws EntitlementException {
        if (privilegeName == null) {
            throw new EntitlementException(12);
        }

        Privilege privilege = null;
        try {
            if (!migratedToEntitlementSvc) {
                Policy policy = pm.getPolicy(privilegeName);
                Set<IPrivilege> privileges = PrivilegeUtils.policyToPrivileges(policy);
                Iterator<IPrivilege> it = privileges.iterator();

                if (it.hasNext()) {
                    IPrivilege searchResult = it.next();
                    privilege = (Privilege) searchResult;
                }
            } else {
                PrivilegeIndexStore pis = PrivilegeIndexStore.getInstance(adminSubject, getRealm());
                privilege = (Privilege) pis.getPrivilege(privilegeName);

                if (privilege == null) {
                    throw new EntitlementException(EntitlementException.NO_SUCH_POLICY, new Object[] { privilegeName });
                }
            }

            if (adminSubject != PrivilegeManager.superAdminSubject) {
                if (privilege != null) {
                    // Delegation to applications is currently not configurable, passing super admin (see AME-4959)
                    ApplicationPrivilegeManager applPrivilegeMgr =
                            ApplicationPrivilegeManager.getInstance(realm, PrivilegeManager.superAdminSubject);
                    if (applPrivilegeMgr == null) {
                        return null;
                    }
                    if (!applPrivilegeMgr.hasPrivilege(privilege, ApplicationPrivilege.Action.READ)) {
                        throw new EntitlementException(326);
                    }
                }
            }
        } catch (PolicyException pe) {
            throw new EntitlementException(102, pe);
        } catch (SSOException ssoe) {
            throw new EntitlementException(102, ssoe);
        }
        return privilege;
    }

    @Override
    public List<Privilege> findAllPolicies() throws EntitlementException {
        PrivilegeIndexStore indexStore = PrivilegeIndexStore.getInstance(getAdminSubject(), getRealm());

        if (indexStore == null) {
            throw new NullPointerException("Policy index store not initialised");
        }

        return indexStore.findAllPolicies();
    }

    @Override
    public List<Privilege> findAllPoliciesByApplication(String application) throws EntitlementException {
        PrivilegeIndexStore indexStore = PrivilegeIndexStore.getInstance(getAdminSubject(), getRealm());

        if (indexStore == null) {
            throw new NullPointerException("Policy index store not initialised");
        }

        return indexStore.findAllPoliciesByApplication(application);
    }

    /**
     * Add a privilege.
     *
     * @param privilege privilege to add.
     * @throws EntitlementException if privilege cannot be added.
     */
    @Override
    public void add(Privilege privilege) throws EntitlementException {
        super.add(privilege);
        String name = privilege.getName();

        try {
            if (!migratedToEntitlementSvc) {
                Object policyObject = PrivilegeUtils.privilegeToPolicyObject(realm, privilege);
                pm.addPolicy((Policy) policyObject);
            } else {
                PolicyDataStore pdb = PolicyDataStore.getInstance();
                String currentRealm = getRealm();
                pdb.addPolicy(getAdminSubject(), currentRealm, privilege);
                notifyPrivilegeChanged(currentRealm, null, privilege);
            }
        } catch (PolicyException e) {
            Object[] params = {name};
            throw new EntitlementException(202, params, e);
        } catch (SSOException e) {
            Object[] params = {name};
            throw new EntitlementException(202, params, e);
        }
    }

    /**
     * Remove a privilege.
     *
     * @param name name of the privilege to be removed.
     * @throws EntitlementException if privilege cannot be removed.
     */
    @Override
    public void remove(String name) throws EntitlementException {
        if (name == null) {
            throw new EntitlementException(12);
        }
        try {
            if (!migratedToEntitlementSvc) {
                pm.removePolicy(name);
            } else {
                Privilege privilege = findByName(name);

                if (privilege != null) {
                    String currentRealm = getRealm();
                    Subject adminSubject = getAdminSubject();
                    PolicyDataStore pdb = PolicyDataStore.getInstance();
                    pdb.removePrivilege(adminSubject, currentRealm, privilege);
                    notifyPrivilegeChanged(currentRealm, null, privilege);
                }
            }
        } catch (PolicyException e) {
            Object[] params = {name};
            throw new EntitlementException(205, params, e);
        } catch (SSOException e) {
            Object[] params = {name};
            throw new EntitlementException(205, params, e);
        }
    }

    private void updateMetaInfo(String existingName, Privilege privilege) throws EntitlementException {
        Privilege origPrivilege = findByName(existingName, PrivilegeManager.superAdminSubject);

        if (origPrivilege != null) {
            privilege.setCreatedBy(origPrivilege.getCreatedBy());
            privilege.setCreationDate(origPrivilege.getCreationDate());
        }

        Date date = newDate();
        privilege.setLastModifiedDate(date.getTime());

        Set<Principal> principals = getAdminSubject().getPrincipals();
        if ((principals != null) && !principals.isEmpty()) {
            privilege.setLastModifiedBy(principals.iterator().next().getName());
        }
    }

    /**
     * Modify a privilege.
     *
     * @param existingName the name with which the privilege is currently stored
     * @param privilege the privilege to be modified
     * @throws com.sun.identity.entitlement.EntitlementException if privilege cannot be modified.
     */
    @Override
    public void modify(String existingName, Privilege privilege) throws EntitlementException {
        validate(privilege);
        updateMetaInfo(existingName, privilege);

        try {
            if (!migratedToEntitlementSvc) {
                pm.removePolicy(privilege.getName());
                pm.addPolicy(PrivilegeUtils.privilegeToPolicy(realm, privilege));
            } else {
                PolicyDataStore pdb = PolicyDataStore.getInstance();
                Privilege oldP = findByName(existingName, getAdminSubject());

                String currentRealm = getRealm();

                pdb.removePrivilege(getAdminSubject(), currentRealm, oldP);

                pdb.addPolicy(getAdminSubject(), currentRealm, privilege);
                notifyPrivilegeChanged(currentRealm, oldP, privilege);
            }
        } catch (PolicyException e) {
            Object[] params = {existingName};
            throw new EntitlementException(206, params, e);
        } catch (SSOException e) {
            Object[] params = {existingName};
            throw new EntitlementException(206, params, e);
        }
    }

    /**
     * Modify a privilege.
     *
     * @param privilege the privilege to be modified
     * @throws com.sun.identity.entitlement.EntitlementException if privilege cannot be modified.
     */
    @Override
    public void modify(Privilege privilege) throws EntitlementException {
        modify(privilege.getName(), privilege);
    }

    /**
     * Returns the XML representation of this privilege.
     *
     * @param name Privilege name.
     * @return XML representation of this privilege.
     * @throws EntitlementException if privilege is not found, or cannot
     * be obtained.
     */
    @Override
    public String getPrivilegeXML(String name) throws EntitlementException {
        String xmlString = "";
        /* TODO: remove comment
        try {
            Object policy = null;
            if (!migratedToEntitlementSvc) {
                policy = pm.getPolicy(name);
            } else {
                PolicyDataStore pdb = PolicyDataStore.getInstance();
                policy = (Policy)pdb.getPolicy(getAdminSubject(),
                        getRealm(), name);
            }
            xmlString = PrivilegeUtils.policyToXML(policy);
        } catch (PolicyException pe) {
            throw new EntitlementException(102, pe);
        } catch (SSOException ssoe) {
            throw new EntitlementException(102, ssoe);
        }
        */
        //TODO: remove the tempoarary work around 29may09
        xmlString = XACMLPrivilegeUtils.toXACML(findByName(name));
        return xmlString;
    }

    /**
     * Returns the XML representation of this privilege.
     *
     * @param names Name of Privileges to export as XML.
     * @return XML representation of the specified privileges
     * @throws EntitlementException if a specified privilege is not found, or cannot
     * be obtained.
     */
    @Override
    public String getPrivilegesXML(Set<String> names) throws EntitlementException {
        String xmlString = "";
        if (names == null || names.isEmpty()) {
            return xmlString;
        }
        Set<Privilege> privileges = new HashSet<Privilege>();
        for (String name : names) {
            privileges.add(findByName(name));
        }
        xmlString = XACMLPrivilegeUtils.toXML(XACMLPrivilegeUtils.privilegesToPolicySet(realm, privileges));
        return xmlString;
    }

    /**
     * Returns <code>true</code> if the system stores privileges in
     * XACML format and supports exporting privileges in XACML format
     *
     *
     * @return <code>true</code> if the system stores privileges in
     * XACML format and supports exporting privileges in XACML format
     */
    public static boolean xacmlPrivilegeEnabled() {
        return xacmlEnabled;
    }

    @Override
    protected void notifyPrivilegeChanged(String realm, Privilege previous, Privilege current) throws EntitlementException {
        Set<String> resourceNames = new HashSet<String>();
        if (previous != null) {
            Set<String> r = previous.getEntitlement().getResourceNames();
            if (r != null) {
                resourceNames.addAll(r);
            }
        }

        Set<String> r = current.getEntitlement().getResourceNames();
        if (r != null) {
            resourceNames.addAll(r);
        }

        String applicationName = current.getEntitlement().getApplicationName();

        if (PrivilegeManager.debug.messageEnabled()) {
            PrivilegeManager.debug.message("PolicyPrivilegeManager.notifyPrivilegeChanged():"
                    + "applicationName=" + applicationName + ", resources=" + resourceNames);
        }
        PrivilegeChangeNotifier.getInstance().notify(getAdminSubject(), realm,
            applicationName, current.getName(), resourceNames);

        if (policyCache != null) {
            // Retrieve the underlying application type to map to the legacy service type model.
            final Application application = ApplicationManager
                    .getApplication(PrivilegeManager.superAdminSubject, realm, applicationName);

            if (application == null) {
                throw new EntitlementException(EntitlementException.APP_RETRIEVAL_ERROR, new Object[] {realm});
            }

            final String serviceTypeName = application.getApplicationType().getName();
            policyCache.firePrivilegeChanged(serviceTypeName, resourceNames, PolicyEvent.POLICY_MODIFIED);
        }
    }
}
