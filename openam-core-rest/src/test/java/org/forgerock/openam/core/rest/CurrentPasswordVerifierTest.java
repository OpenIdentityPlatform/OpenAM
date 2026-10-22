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

package org.forgerock.openam.core.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;

import org.forgerock.json.resource.BadRequestException;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.AMIdentityRepository;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdType;

/**
 * The re-authentication the identity endpoints perform before a protected attribute is changed.
 * <p>
 * It never reaches the authentication service, so the repository call is the only thing between the caller and the
 * change. These tests pin what that call is asked for: the identity type of the endpoint rather than any
 * repository of the realm, and the status of the identity as well as its password.
 */
public class CurrentPasswordVerifierTest {

    private static final String REALM = "/";
    private static final String NAME = "demo";
    private static final String PASSWORD = "changeit";

    @Test
    public void shouldAcceptTheCurrentPasswordOfAnActiveIdentity() throws Exception {
        Verifier verifier = new Verifier(IdentityResourceV2.USER_TYPE);

        assertThat(verifier.isCurrentPasswordOf(NAME, PASSWORD.toCharArray(), REALM)).isTrue();
        assertThat(verifier.offeredName).isEqualTo(NAME);
        assertThat(verifier.offeredPassword).isEqualTo(PASSWORD);
    }

    @Test
    public void shouldRefuseTheCurrentPasswordOfAnInactiveIdentity() throws Exception {
        Verifier verifier = new Verifier(IdentityResourceV2.USER_TYPE);
        verifier.active = false;

        assertThat(verifier.isCurrentPasswordOf(NAME, PASSWORD.toCharArray(), REALM)).isFalse();
        assertThat(verifier.statusReads).isEqualTo(1);
    }

    @Test
    public void shouldNotReadTheStatusWhereThePasswordIsWrong() throws Exception {
        Verifier verifier = new Verifier(IdentityResourceV2.USER_TYPE);
        verifier.passwordAccepted = false;

        assertThat(verifier.isCurrentPasswordOf(NAME, PASSWORD.toCharArray(), REALM)).isFalse();
        assertThat(verifier.statusReads).isEqualTo(0);
    }

    @Test
    public void shouldOfferTheCredentialsOnlyToTheUserRepositories() throws Exception {
        Verifier verifier = new Verifier(IdentityResourceV2.USER_TYPE);

        assertThat(verifier.isCurrentPasswordOf(NAME, PASSWORD.toCharArray(), REALM)).isTrue();
        assertThat(verifier.authenticatedType).isEqualTo(IdType.USER);
        assertThat(verifier.statusType).isEqualTo(IdType.USER);
    }

    @Test
    public void shouldOfferTheCredentialsOnlyToTheAgentRepositoriesOnTheAgentsEndpoint() throws Exception {
        Verifier verifier = new Verifier(IdentityResourceV2.AGENT_TYPE);

        assertThat(verifier.isCurrentPasswordOf(NAME, PASSWORD.toCharArray(), REALM)).isTrue();
        assertThat(verifier.authenticatedType).isEqualTo(IdType.AGENT);
        assertThat(verifier.statusType).isEqualTo(IdType.AGENT);
    }

    @Test
    public void shouldRefuseWhereTheRepositoryCannotBeReached() throws Exception {
        Verifier verifier = new Verifier(IdentityResourceV2.USER_TYPE);
        verifier.repositoryFailure = new IdRepoException("no repository configured");

        assertThat(verifier.isCurrentPasswordOf(NAME, PASSWORD.toCharArray(), REALM)).isFalse();
    }

    @Test
    public void shouldRefuseWhereTheEndpointServesIdentitiesThatHoldNoPassword() throws Exception {
        Verifier verifier = new Verifier("role");

        assertThatThrownBy(() -> verifier.isCurrentPasswordOf(NAME, PASSWORD.toCharArray(), REALM))
                .isInstanceOf(BadRequestException.class);
        assertThat(verifier.offeredName).isNull();
        assertThat(verifier.statusReads).isEqualTo(0);
    }

    /**
     * A group holds no password: the repository would bind a user of the same name and then throw on the status
     * read, so the endpoint has to be refused outright rather than through a swallowed exception.
     */
    @Test
    public void shouldRefuseTheGroupsEndpointWithoutOfferingTheCredentials() throws Exception {
        Verifier verifier = new Verifier(IdentityResourceV2.GROUP_TYPE);

        assertThatThrownBy(() -> verifier.isCurrentPasswordOf(NAME, PASSWORD.toCharArray(), REALM))
                .isInstanceOf(BadRequestException.class);
        assertThat(verifier.offeredName).isNull();
        assertThat(verifier.statusReads).isEqualTo(0);
    }

    @Test(expectedExceptions = BadRequestException.class)
    public void shouldRejectAMissingUserName() throws Exception {
        new Verifier(IdentityResourceV2.USER_TYPE).isCurrentPasswordOf(null, PASSWORD.toCharArray(), REALM);
    }

    @Test(expectedExceptions = BadRequestException.class)
    public void shouldRejectAMissingPassword() throws Exception {
        new Verifier(IdentityResourceV2.USER_TYPE).isCurrentPasswordOf(NAME, null, REALM);
    }

    /**
     * Runs the real {@code authenticateIdentity} against a mock repository, so a mutant that restored the untyped
     * {@code AMIdentityRepository.authenticate(callbacks)} - the pre-fix bug - would leave this test red.
     * The other tests replace both seams entirely, which pins the type reaching the seam but not the type reaching
     * the repository itself.
     */
    @Test
    public void shouldOfferTheCredentialsToTheRepositoriesOfTheEndpointTypeOnly() throws Exception {
        final AMIdentityRepository repository = mock(AMIdentityRepository.class);
        given(repository.authenticate(any(IdType.class), any(Callback[].class))).willReturn(true);
        CurrentPasswordVerifier verifier = new CurrentPasswordVerifier(IdentityResourceV2.USER_TYPE) {
            @Override
            protected AMIdentityRepository identityRepository(String realm) {
                return repository;
            }

            @Override
            protected boolean isIdentityActive(String realm, IdType identityType, String name) {
                return true;
            }
        };

        assertThat(verifier.isCurrentPasswordOf(NAME, PASSWORD.toCharArray(), REALM)).isTrue();

        verify(repository).authenticate(eq(IdType.USER), any(Callback[].class));
        verify(repository, never()).authenticate(any(Callback[].class));
    }

    @Test
    public void shouldOfferTheCredentialsToTheAgentRepositoriesOnlyOnTheAgentsEndpoint() throws Exception {
        final AMIdentityRepository repository = mock(AMIdentityRepository.class);
        given(repository.authenticate(any(IdType.class), any(Callback[].class))).willReturn(true);
        CurrentPasswordVerifier verifier = new CurrentPasswordVerifier(IdentityResourceV2.AGENT_TYPE) {
            @Override
            protected AMIdentityRepository identityRepository(String realm) {
                return repository;
            }

            @Override
            protected boolean isIdentityActive(String realm, IdType identityType, String name) {
                return true;
            }
        };

        assertThat(verifier.isCurrentPasswordOf(NAME, PASSWORD.toCharArray(), REALM)).isTrue();

        verify(repository).authenticate(eq(IdType.AGENT), any(Callback[].class));
        verify(repository, never()).authenticate(any(Callback[].class));
    }

    /**
     * Runs the real {@code isIdentityActive} against a mock identity, so a mutant that read the status as
     * {@code IdType.USER} - the status of a same-named user rather than of the agent - would leave this test red.
     */
    @Test
    public void shouldReadTheStatusOfTheAgentOnTheAgentsEndpoint() throws Exception {
        final AMIdentityRepository repository = mock(AMIdentityRepository.class);
        given(repository.authenticate(any(IdType.class), any(Callback[].class))).willReturn(true);
        final AMIdentity agent = mock(AMIdentity.class);
        given(agent.isActive()).willReturn(false);
        final List<IdType> statusTypes = new ArrayList<>();
        CurrentPasswordVerifier verifier = new CurrentPasswordVerifier(IdentityResourceV2.AGENT_TYPE) {
            @Override
            protected AMIdentityRepository identityRepository(String realm) {
                return repository;
            }

            @Override
            protected AMIdentity identity(String realm, IdType identityType, String name) {
                statusTypes.add(identityType);
                return agent;
            }
        };

        assertThat(verifier.isCurrentPasswordOf(NAME, PASSWORD.toCharArray(), REALM)).isFalse();
        assertThat(statusTypes).containsExactly(IdType.AGENT);
    }

    @DataProvider
    public Object[][] endpoints() {
        return new Object[][] {
                {IdentityResourceV2.USER_TYPE, IdType.USER},
                {IdentityResourceV2.AGENT_TYPE, IdType.AGENT},
        };
    }

    /**
     * The endpoint hands the verifier its own identity type: a resource that wired every verifier up as
     * {@code USER_TYPE} would check an agent's current password against the user repositories.
     */
    @Test(dataProvider = "endpoints")
    public void shouldBeWiredUpWithTheIdentityTypeOfTheEndpoint(String objectType, IdType expected)
            throws Exception {
        // The constructor only stores its collaborators. They are left null rather than mocked: mocking
        // IdentityServicesImpl runs its static initialiser, which needs an injector that only exists here when
        // another test class of this module happens to have built one first.
        IdentityResourceV2 resource = new IdentityResourceV2(objectType, null, null, null, null, null, null, null,
                Collections.<UiRolePredicate>emptySet());

        assertThat(resource.currentPasswordVerifier().identityType()).isEqualTo(expected);
    }

    /**
     * The verifier with its two repository calls replaced by what the test wants them to answer, and a record of
     * what they were asked for.
     */
    private static final class Verifier extends CurrentPasswordVerifier {

        private boolean passwordAccepted = true;
        private boolean active = true;
        private IdRepoException repositoryFailure;

        private IdType authenticatedType;
        private String offeredName;
        private String offeredPassword;
        private IdType statusType;
        private int statusReads;

        private Verifier(String objectType) {
            super(objectType);
        }

        @Override
        protected boolean authenticateIdentity(String realm, IdType identityType, Callback[] credentials)
                throws IdRepoException {
            if (repositoryFailure != null) {
                throw repositoryFailure;
            }
            authenticatedType = identityType;
            for (Callback credential : credentials) {
                if (credential instanceof NameCallback) {
                    offeredName = ((NameCallback) credential).getName();
                } else if (credential instanceof PasswordCallback) {
                    offeredPassword = new String(((PasswordCallback) credential).getPassword());
                }
            }
            return passwordAccepted;
        }

        @Override
        protected boolean isIdentityActive(String realm, IdType identityType, String name) {
            statusReads++;
            statusType = identityType;
            return active;
        }
    }
}
