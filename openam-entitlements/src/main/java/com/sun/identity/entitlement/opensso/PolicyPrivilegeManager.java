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
 * Portions Copyrighted 2025 3A Systems LLC.
 */
package com.sun.identity.entitlement.opensso;

import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.utils.Time.newDate;

import java.security.AccessController;
import java.security.Principal;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.inject.Inject;
import javax.security.auth.Subject;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.entitlement.constraints.ConstraintValidator;
import org.forgerock.openam.entitlement.service.ApplicationServiceFactory;
import org.forgerock.openam.entitlement.service.ResourceTypeService;
import org.forgerock.openam.notifications.NotificationBroker;
import org.forgerock.openam.notifications.NotificationsConfig;
import org.forgerock.openam.notifications.Topic;
import org.forgerock.util.Reject;

import com.iplanet.sso.SSOToken;
import com.sun.identity.entitlement.ApplicationPrivilege;
import com.sun.identity.entitlement.ApplicationPrivilegeManager;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.PolicyDataStore;
import com.sun.identity.entitlement.PolicyEventType;
import com.sun.identity.entitlement.Privilege;
import com.sun.identity.entitlement.PrivilegeChangeNotifier;
import com.sun.identity.entitlement.PrivilegeIndexStore;
import com.sun.identity.entitlement.PrivilegeManager;
import com.sun.identity.security.AdminTokenAction;

/**
 * Implementation of <code>PrivilegeManager</code> that saves privileges as <code>com.sun.identity.policy</code> objects
 */
public class PolicyPrivilegeManager extends PrivilegeManager {

    private String realm = "/";
    private static Subject dsameUserSubject;

    private final NotificationBroker broker;
    private final NotificationsConfig notificationsConfig;

    static {
        SSOToken adminToken = AccessController.doPrivileged(AdminTokenAction.getInstance());
        dsameUserSubject = SubjectUtils.createSubject(adminToken);
        try {
            if (PrivilegeManager.debug.messageEnabled()) {
                PrivilegeManager.debug.message(
                        "PolicyPrivilegeManager.static initializer, getting instance of PolicyCache");
            }
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
                                  ConstraintValidator constraintValidator,
                                  NotificationBroker broker,
                                  NotificationsConfig notificationsConfig) {
        super(applicationServiceFactory, resourceTypeService, constraintValidator);
        this.broker = broker;
        this.notificationsConfig = notificationsConfig;
    }

    /**
     * Initializes the object
     *
     * @param subject subject that would be used for privilege management
     * operations
     */
    @Override
    public void initialize(String realm, Subject subject) {
        super.initialize(realm, subject);
        this.realm = realm;
        SSOToken ssoToken = SubjectUtils.getSSOToken(subject);
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
        PrivilegeIndexStore pis = PrivilegeIndexStore.getInstance(adminSubject, getRealm());
        privilege = (Privilege) pis.getPrivilege(privilegeName);

        if (privilege == null) {
            throw new EntitlementException(EntitlementException.NO_SUCH_POLICY, new Object[] {privilegeName});
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

    @Override
    public List<Privilege> findAllPoliciesByIdentityUid(String uid) throws EntitlementException {
        Reject.ifNull(uid);
        PrivilegeIndexStore indexStore = PrivilegeIndexStore.getInstance(getAdminSubject(), getRealm());
        Reject.ifNull(indexStore, "Policy index store not initialised");
        return indexStore.findAllPoliciesByIdentityUid(uid);
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

        PolicyDataStore pdb = PolicyDataStore.getInstance();
        String currentRealm = getRealm();
        pdb.addPolicy(getAdminSubject(), currentRealm, privilege);
        notifyPrivilegeChanged(currentRealm, null, privilege, PolicyEventType.CREATE);
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
        Privilege privilege = findByName(name);

        if (privilege != null) {
            String currentRealm = getRealm();
            Subject adminSubject = getAdminSubject();
            PolicyDataStore pdb = PolicyDataStore.getInstance();
            pdb.removePrivilege(adminSubject, currentRealm, privilege);
            notifyPrivilegeChanged(currentRealm, null, privilege, PolicyEventType.DELETE);
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

        PolicyDataStore pdb = PolicyDataStore.getInstance();
        Privilege oldP = findByName(existingName, getAdminSubject());

        String currentRealm = getRealm();

        pdb.removePrivilege(getAdminSubject(), currentRealm, oldP);

        pdb.addPolicy(getAdminSubject(), currentRealm, privilege);
        notifyPrivilegeChanged(currentRealm, oldP, privilege, PolicyEventType.UPDATE);
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

    @Override
    protected void notifyPrivilegeChanged(String realm, Privilege previous, Privilege current,
            PolicyEventType eventType) throws EntitlementException {
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

        if (notificationsConfig.isAgentsEnabled()) {
            JsonValue json = json(object(
                    field("realm", realm),
                    field("policy", current.getName()),
                    field("policySet", applicationName),
                    field("eventType", eventType)
            ));
            broker.publish(Topic.of("/agent/policy"), json);
        }

        PrivilegeChangeNotifier.getInstance().notify(getAdminSubject(), realm,
                applicationName, current.getName(), resourceNames);
    }
}
