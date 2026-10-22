/*
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
 *
 * Copyright 2026 3A Systems, LLC.
 */
package org.forgerock.openam.upgrade.steps;

import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;

import jakarta.inject.Inject;

import org.forgerock.openam.sm.datalayer.api.ConnectionFactory;
import org.forgerock.openam.sm.datalayer.api.ConnectionType;
import org.forgerock.openam.sm.datalayer.api.DataLayer;
import org.forgerock.openam.upgrade.UpgradeException;
import org.forgerock.openam.upgrade.UpgradeProgress;
import org.forgerock.openam.upgrade.UpgradeServices;
import org.forgerock.openam.upgrade.UpgradeStepInfo;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.AMIdentityRepository;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdType;

/**
 * Deactivates the identities that the configurator creates with a published default password, where that
 * password is still in place and the identity is still active.
 * <p>
 * Older installations were configured with an active {@code demo} user and a set of active WS-Security agents
 * whose password equals their own name. Both are documented in the installer source, so they amount to
 * publicly known credentials on any deployment where an administrator never changed them.
 * <p>
 * The password is never rotated here. Silently changing the password of an agent that is genuinely in use
 * breaks the integration in a way that is harder to diagnose than an explicit deactivation, and the upgrade
 * report tells the administrator exactly what was switched off and why.
 * <p>
 * The step carries no version guard, unlike most of its neighbours: an identity that still answers to a
 * password published in the installer source is a live credential whatever release it survived into, and the
 * check costs one bind per identity.
 */
@UpgradeStepInfo(dependsOn = "org.forgerock.openam.upgrade.steps.UpgradeServiceSchemaStep")
public class DeactivateDefaultCredentialsStep extends AbstractUpgradeStep {

    private static final String ROOT_REALM = "/";

    private static final String AUDIT_START = "upgrade.defaultcredentials.start";
    private static final String AUDIT_SHORT = "upgrade.defaultcredentials.short";
    private static final String AUDIT_REPORT = "upgrade.defaultcredentials.report";
    private static final String AUDIT_NOT_DONE = "upgrade.defaultcredentials.notdeactivated";
    private static final String AUDIT_SUCCESS = "upgrade.success";
    private static final String AUDIT_FAILED = "upgrade.failed";
    private static final String DATA_PLACEHOLDER = "%DATA_PLACEHOLDER%";

    /**
     * The identities {@code AMSetupServlet} creates, with the password it creates them with. Keep this list in
     * step with {@code AMSetupServlet.createDemoUser()} and
     * {@code AMSetupServlet.createIdentitiesForWSSecurity()}.
     */
    private static final List<InstallationDefault> INSTALLATION_DEFAULTS = Arrays.asList(
            new InstallationDefault(IdType.USER, IdType.USER, "demo", "changeit"),
            new InstallationDefault(IdType.AGENTONLY, IdType.AGENT, "wsc", "wsc"),
            new InstallationDefault(IdType.AGENTONLY, IdType.AGENT, "wsp", "wsp"),
            new InstallationDefault(IdType.AGENTONLY, IdType.AGENT, "SecurityTokenService", "SecurityTokenService"),
            new InstallationDefault(IdType.AGENTONLY, IdType.AGENT, "agentAuth", "changeit"));

    private final List<InstallationDefault> toDeactivate = new ArrayList<InstallationDefault>();
    /** The entries {@link #perform()} could not deactivate, with the reason, for the upgrade report. */
    private final Map<InstallationDefault, String> failures = new LinkedHashMap<>();

    @Inject
    public DeactivateDefaultCredentialsStep(final PrivilegedAction<SSOToken> adminTokenAction,
            @DataLayer(ConnectionType.DATA_LAYER) final ConnectionFactory connectionFactory) {
        super(adminTokenAction, connectionFactory);
    }

    @Override
    public void initialize() throws UpgradeException {
        final AMIdentityRepository repository = getIdentityRepository();

        for (InstallationDefault candidate : INSTALLATION_DEFAULTS) {
            if (stillUsesInstallationDefault(repository, candidate)) {
                toDeactivate.add(candidate);
            }
        }
    }

    @Override
    public boolean isApplicable() {
        return !toDeactivate.isEmpty();
    }

    @Override
    public void perform() throws UpgradeException {
        UpgradeProgress.reportStart(AUDIT_START);
        for (InstallationDefault entry : toDeactivate) {
            try {
                identityOf(entry.identityType, entry.name).setActiveStatus(false);
                DEBUG.message("Deactivated " + entry + ": still using its installation default password");
            } catch (IdRepoException | SSOException e) {
                // One identity that cannot be switched off - deleted between the two phases, or a repository
                // that has since gone away - must not fail the upgrade and leave the rest half applied. The
                // administrator is told which ones are still open, and why, in the upgrade report.
                DEBUG.error("Unable to deactivate " + entry, e);
                failures.put(entry, e.getMessage());
            }
        }
        UpgradeProgress.reportEnd(failures.isEmpty() ? AUDIT_SUCCESS : AUDIT_FAILED);
    }

    @Override
    public String getShortReport(String delimiter) {
        return BUNDLE.getString(AUDIT_SHORT) + delimiter;
    }

    @Override
    public String getDetailedReport(String delimiter) {
        final StringBuilder builder = new StringBuilder();
        for (InstallationDefault entry : toDeactivate) {
            if (failures.containsKey(entry)) {
                continue;
            }
            builder.append(BULLET).append(entry).append(delimiter);
        }
        for (Map.Entry<InstallationDefault, String> failure : failures.entrySet()) {
            builder.append(BULLET).append(failure.getKey()).append(" - ").append(BUNDLE.getString(AUDIT_NOT_DONE))
                    .append(": ").append(failure.getValue()).append(delimiter);
        }

        final Map<String, String> reportContents = new HashMap<String, String>();
        reportContents.put(DATA_PLACEHOLDER, builder.toString());
        reportContents.put(UpgradeServices.LF, delimiter);

        return UpgradeServices.tagSwapReport(reportContents, AUDIT_REPORT);
    }

    /**
     * Reports whether the identity exists, is still active, and still accepts the password the configurator
     * gave it. Anything that goes wrong along the way means the identity is left alone: an upgrade must not
     * fail, nor deactivate an account, because of an unrelated repository problem.
     */
    private boolean stillUsesInstallationDefault(AMIdentityRepository repository, InstallationDefault candidate) {
        try {
            final AMIdentity identity = identityOf(candidate.identityType, candidate.name);
            if (!identity.isExists() || !identity.isActive()) {
                return false;
            }
            return repository.authenticate(candidate.authenticationType, candidate.credentials());
        } catch (IdRepoException | SSOException e) {
            // IdRepoException is thrown for a wrong password as well as for a repository failure, so it
            // cannot be treated as evidence either way.
            DEBUG.message("Unable to check the password of " + candidate + ", leaving it alone", e);
            return false;
        } catch (AuthLoginException ale) {
            DEBUG.message(candidate + " does not use its installation default password", ale);
            return false;
        }
    }

    /**
     * The identity repository of the top level realm. Overridable so that the step can be exercised without a
     * configuration store behind it.
     */
    protected AMIdentityRepository getIdentityRepository() {
        return new AMIdentityRepository(ROOT_REALM, getAdminToken());
    }

    /**
     * The identity of the given type and name in the top level realm. Overridable for the same reason.
     */
    protected AMIdentity identityOf(IdType identityType, String name) {
        return new AMIdentity(getAdminToken(), name, identityType, ROOT_REALM, null);
    }

    /**
     * An identity the configurator creates together with the password it creates it with.
     */
    private static final class InstallationDefault {

        /** The type the identity is stored under, and which {@code setActiveStatus} has to be applied to. */
        private final IdType identityType;
        /** The type the identity repository authenticates the identity as. */
        private final IdType authenticationType;
        private final String name;
        private final String password;

        private InstallationDefault(IdType identityType, IdType authenticationType, String name, String password) {
            this.identityType = identityType;
            this.authenticationType = authenticationType;
            this.name = name;
            this.password = password;
        }

        private Callback[] credentials() {
            final NameCallback nameCallback = new NameCallback("name");
            nameCallback.setName(name);
            final PasswordCallback passwordCallback = new PasswordCallback("password", false);
            passwordCallback.setPassword(password.toCharArray());
            return new Callback[]{nameCallback, passwordCallback};
        }

        /**
         * By identity type and name, the pair that names the identity. The report looks entries up in the map of
         * the ones that could not be deactivated; every entry is one of the INSTALLATION_DEFAULTS constants, so
         * this only matters if an equal instance is ever built elsewhere.
         */
        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof InstallationDefault)) {
                return false;
            }
            final InstallationDefault that = (InstallationDefault) other;
            return identityType.equals(that.identityType) && name.equals(that.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(identityType, name);
        }

        @Override
        public String toString() {
            return identityType.getName() + " " + name;
        }
    }
}
