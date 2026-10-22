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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;

import org.forgerock.openam.sm.datalayer.api.ConnectionFactory;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.iplanet.sso.SSOToken;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.AMIdentityRepository;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdType;

/**
 * The step deactivates an identity of the installation only where it still exists, is still active, and still
 * answers to the password the configurator gave it, and it reports what it could not switch off rather than
 * failing the upgrade over it.
 */
public class DeactivateDefaultCredentialsStepTest {

    private static final String DEMO = "demo";
    private static final String WSC = "wsc";
    private static final String WSP = "wsp";
    private static final String STS = "SecurityTokenService";
    private static final String AGENT_AUTH = "agentAuth";

    /** The identities the step knows about, in the order it checks them. */
    private static final List<String> INSTALLATION_DEFAULTS = Arrays.asList(DEMO, WSC, WSP, STS, AGENT_AUTH);

    /**
     * The password {@code AMSetupServlet} gives each identity. Keep in step with
     * {@code DeactivateDefaultCredentialsStep.INSTALLATION_DEFAULTS}: a table that drifts silently
     * would deactivate nothing on upgrade and the suite would stay green.
     */
    private static final Map<String, String> INSTALLATION_PASSWORDS = new HashMap<>();
    static {
        INSTALLATION_PASSWORDS.put(DEMO, "changeit");
        INSTALLATION_PASSWORDS.put(WSC, "wsc");
        INSTALLATION_PASSWORDS.put(WSP, "wsp");
        INSTALLATION_PASSWORDS.put(STS, "SecurityTokenService");
        INSTALLATION_PASSWORDS.put(AGENT_AUTH, "changeit");
    }

    @Mock
    private PrivilegedAction<SSOToken> adminTokenAction;
    @Mock
    private ConnectionFactory connectionFactory;
    @Mock
    private AMIdentityRepository repository;

    private final Map<String, AMIdentity> identities = new HashMap<>();
    /** The identities whose installation default password the repository still accepts. */
    private final Set<String> acceptsItsInstallationPassword = new HashSet<>();
    /** The identities the repository refuses with an exception rather than with a plain {@code false}. */
    private final Set<String> refusesTheLogin = new HashSet<>();

    private TestStep upgradeStep;

    @BeforeMethod
    public void setup() throws Exception {
        initMocks(this);
        identities.clear();
        acceptsItsInstallationPassword.clear();
        refusesTheLogin.clear();

        // Absent unless the test says otherwise, which is the state of a store the configurator never
        // created them in.
        for (String name : INSTALLATION_DEFAULTS) {
            AMIdentity identity = mock(AMIdentity.class, name);
            given(identity.isExists()).willReturn(false);
            identities.put(name, identity);
        }
        given(repository.authenticate(any(IdType.class), any(Callback[].class)))
                .willAnswer(this::answerAuthentication);

        upgradeStep = new TestStep(adminTokenAction, connectionFactory, repository, identities);
    }

    @Test
    public void shouldDeactivateAnIdentityThatStillAcceptsItsInstallationPassword() throws Exception {
        AMIdentity demo = givenIdentity(DEMO, true, true);
        acceptsItsInstallationPassword.add(DEMO);

        upgradeStep.initialize();

        assertThat(upgradeStep.isApplicable()).isTrue();

        upgradeStep.perform();

        verify(demo).setActiveStatus(false);
        assertThat(reportLineFor(DEMO)).isNotNull();
    }

    @Test
    public void shouldDeactivateEveryIdentityThatStillAcceptsItsInstallationPassword() throws Exception {
        for (String name : INSTALLATION_DEFAULTS) {
            givenIdentity(name, true, true);
        }
        acceptsItsInstallationPassword.addAll(INSTALLATION_DEFAULTS);

        upgradeStep.initialize();

        assertThat(upgradeStep.isApplicable()).isTrue();

        upgradeStep.perform();

        for (String name : INSTALLATION_DEFAULTS) {
            verify(identities.get(name)).setActiveStatus(false);
            assertThat(reportLineFor(name)).isNotNull();
        }
    }

    @Test
    public void shouldTouchAndReportOnlyTheIdentitiesItSelected() throws Exception {
        // All five exist and are active, but only demo still answers to its installation password: the
        // other four had theirs changed by an administrator and are in use.
        for (String name : INSTALLATION_DEFAULTS) {
            givenIdentity(name, true, true);
        }
        acceptsItsInstallationPassword.add(DEMO);

        upgradeStep.initialize();
        upgradeStep.perform();

        verify(identities.get(DEMO)).setActiveStatus(false);
        assertThat(reportLineFor(DEMO)).isNotNull();
        for (String name : INSTALLATION_DEFAULTS.subList(1, INSTALLATION_DEFAULTS.size())) {
            verify(identities.get(name), never()).setActiveStatus(anyBoolean());
            assertThat(reportLineFor(name)).isNull();
        }
    }

    @Test
    public void shouldLeaveAnIdentityThatNoLongerUsesThatPasswordAlone() throws Exception {
        AMIdentity wsc = givenIdentity(WSC, true, true);

        upgradeStep.initialize();

        assertThat(upgradeStep.isApplicable()).isFalse();
        verify(wsc, never()).setActiveStatus(anyBoolean());
    }

    @Test
    public void shouldLeaveAnIdentityThatIsAlreadyInactiveAlone() throws Exception {
        AMIdentity wsp = givenIdentity(WSP, true, false);
        acceptsItsInstallationPassword.add(WSP);

        upgradeStep.initialize();

        assertThat(upgradeStep.isApplicable()).isFalse();
        // Not even asked: an inactive identity cannot authenticate whatever its password is, and the
        // upgrade has no business binding as it.
        verify(repository, never()).authenticate(any(IdType.class), any(Callback[].class));
    }

    @Test
    public void shouldLeaveAnIdentityThatDoesNotExistAlone() throws Exception {
        // The guard reads isExists AND isActive; pinning isActive=true stops a mutant that skips the
        // isExists check from being covered by Mockito's default false return.
        for (String name : INSTALLATION_DEFAULTS) {
            given(identities.get(name).isActive()).willReturn(true);
        }
        acceptsItsInstallationPassword.addAll(INSTALLATION_DEFAULTS);

        upgradeStep.initialize();

        assertThat(upgradeStep.isApplicable()).isFalse();
        verify(repository, never()).authenticate(any(IdType.class), any(Callback[].class));
    }

    @Test
    public void shouldLeaveAnIdentityTheRepositoryRefusesToAuthenticateAlone() throws Exception {
        AMIdentity sts = givenIdentity(STS, true, true);
        refusesTheLogin.add(STS);

        upgradeStep.initialize();

        assertThat(upgradeStep.isApplicable()).isFalse();
        verify(sts, never()).setActiveStatus(anyBoolean());
    }

    @Test
    public void shouldLeaveAnIdentityWhoseRepositoryFailsAlone() throws Exception {
        AMIdentity demo = givenIdentity(DEMO, true, true);
        acceptsItsInstallationPassword.add(DEMO);
        // A repository that cannot answer is not evidence that the credential is still in place: the
        // step must not deactivate the account on the strength of a failure.
        doThrow(new IdRepoException("the store is down"))
                .when(repository).authenticate(eq(IdType.USER), any(Callback[].class));

        upgradeStep.initialize();

        assertThat(upgradeStep.isApplicable()).isFalse();
        verify(demo, never()).setActiveStatus(anyBoolean());
    }

    @Test
    public void shouldReportAnIdentityItCouldNotDeactivateRatherThanFailingTheUpgrade() throws Exception {
        AMIdentity demo = givenIdentity(DEMO, true, true);
        AMIdentity wsc = givenIdentity(WSC, true, true);
        acceptsItsInstallationPassword.addAll(Arrays.asList(DEMO, WSC));
        // Deleted between the two phases, say.
        doThrow(new IdRepoException("no such object")).when(demo).setActiveStatus(false);

        upgradeStep.initialize();
        upgradeStep.perform();

        // The rest are still switched off, and the upgrade carries on.
        verify(wsc).setActiveStatus(false);

        String notDeactivated = ResourceBundle.getBundle("amUpgrade")
                .getString("upgrade.defaultcredentials.notdeactivated");
        assertThat(reportLineFor(DEMO)).contains(notDeactivated).contains("no such object");
        assertThat(reportLineFor(WSC)).doesNotContain(notDeactivated);
    }

    private Object answerAuthentication(InvocationOnMock invocation) throws AuthLoginException {
        final IdType type = invocation.getArgument(0);
        final Callback[] callbacks = invocation.getArgument(1);
        final String name = ((NameCallback) callbacks[0]).getName();
        final String password = new String(((PasswordCallback) callbacks[1]).getPassword());
        if (refusesTheLogin.contains(name)) {
            throw new AuthLoginException("the password is not the one the configurator set");
        }
        // Pin the table the step offers: the exact installation password, and the identity type the
        // step declares it authenticates as (USER for demo, AGENT for the four WS-Security agents).
        return acceptsItsInstallationPassword.contains(name)
                && password.equals(INSTALLATION_PASSWORDS.get(name))
                && type.equals(DEMO.equals(name) ? IdType.USER : IdType.AGENT);
    }

    private AMIdentity givenIdentity(String name, boolean exists, boolean active) throws Exception {
        final AMIdentity identity = identities.get(name);
        given(identity.isExists()).willReturn(exists);
        given(identity.isActive()).willReturn(active);
        return identity;
    }

    /**
     * The line the detailed report devotes to the identity, or {@code null} where it does not mention it.
     * The report names an identity as "&lt;type&gt; &lt;name&gt;", followed by the reason where the step
     * could not switch it off.
     */
    private String reportLineFor(String name) {
        for (String line : upgradeStep.getDetailedReport("\n").split("\n")) {
            if (line.startsWith("* ") && (line.endsWith(" " + name) || line.contains(" " + name + " - "))) {
                return line;
            }
        }
        return null;
    }

    /**
     * Replaces the two pieces of the step that reach the identity repository.
     */
    private static final class TestStep extends DeactivateDefaultCredentialsStep {

        private final AMIdentityRepository repository;
        private final Map<String, AMIdentity> identities;

        private TestStep(PrivilegedAction<SSOToken> adminTokenAction, ConnectionFactory connectionFactory,
                AMIdentityRepository repository, Map<String, AMIdentity> identities) {
            super(adminTokenAction, connectionFactory);
            this.repository = repository;
            this.identities = identities;
        }

        @Override
        protected AMIdentityRepository getIdentityRepository() {
            return repository;
        }

        @Override
        protected AMIdentity identityOf(IdType identityType, String name) {
            // Pin the lookup type the step asks for: USER for demo, AGENTONLY for the four
            // WS-Security agents. Ignoring the type would leave a swap between the two mutants green.
            assertThat(identityType).isEqualTo(DEMO.equals(name) ? IdType.USER : IdType.AGENTONLY);
            return identities.get(name);
        }
    }
}
