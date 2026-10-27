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

package org.forgerock.openam.core.realms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.openam.utils.CollectionUtils.asSet;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import com.iplanet.sso.SSOToken;
import com.sun.identity.idm.IdConstants;
import com.sun.identity.security.AMSecurityPropertiesException;
import com.sun.identity.sm.OrganizationConfigManagerFactory;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceConfigManager;
import com.sun.identity.sm.ServiceListener;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class RealmDnsAliasesTest {

    @Mock
    private SSOToken adminToken;
    @Mock
    private OrganizationConfigManagerFactory organizationConfigManagerFactory;
    @Mock
    private ServiceConfigManager idRepoService;
    @Captor
    private ArgumentCaptor<ServiceListener> serviceListenerCaptor;

    private final Map<String, Set<String>> aliasesByRealm = new HashMap<>();
    private int realmReads;
    private boolean failNextRealmRead;
    private boolean failNextAdminTokenLookup;
    private boolean failNextIdRepoServiceLookup;
    private String realmReadDuringChange;
    private RealmDnsAliases realmDnsAliases;

    @BeforeMethod
    public void setup() throws Exception {
        MockitoAnnotations.initMocks(this);
        given(idRepoService.addListener(any(ServiceListener.class))).willReturn("LISTENER_ID");
        aliasesByRealm.clear();
        aliasesByRealm.put("/", asSet("openam.example.com"));
        aliasesByRealm.put("/acme", asSet("acme.example.com"));
        aliasesByRealm.put("/acme/sub", asSet("sub.acme.example.com"));
        realmReads = 0;
        failNextRealmRead = false;
        failNextAdminTokenLookup = false;
        failNextIdRepoServiceLookup = false;
        realmReadDuringChange = null;

        realmDnsAliases = new RealmDnsAliases(this::getAdminToken, organizationConfigManagerFactory,
                this::getIdRepoService) {
            @Override
            Set<String> getSubRealms(SSOToken token) throws SMSException {
                realmReads++;
                if (failNextRealmRead) {
                    failNextRealmRead = false;
                    throw new SMSException("Configuration store is unavailable");
                }
                Set<String> subRealms = new HashSet<>();
                for (String realm : aliasesByRealm.keySet()) {
                    if (!realm.equals("/")) {
                        subRealms.add(realm.substring(1));
                    }
                }
                return subRealms;
            }

            @Override
            Set<String> getRealmAliases(SSOToken token, String realm) {
                Set<String> aliases = aliasesByRealm.get(realm);
                if (realm.equals(realmReadDuringChange)) {
                    // the change is notified after this realm has already been read
                    realmReadDuringChange = null;
                    aliasesByRealm.put(realm, asSet("new.example.com"));
                    realmConfigurationChanged();
                }
                return aliases;
            }
        };
    }

    @Test
    public void shouldAcceptDnsAliasOfAnyRealm() {
        assertThat(realmDnsAliases.contains("openam.example.com")).isTrue();
        assertThat(realmDnsAliases.contains("acme.example.com")).isTrue();
        assertThat(realmDnsAliases.contains("sub.acme.example.com")).isTrue();
    }

    @Test
    public void shouldAcceptDnsAliasInAnyCase() {
        assertThat(realmDnsAliases.contains("ACME.Example.COM")).isTrue();
    }

    @Test
    public void shouldAcceptDnsAliasConfiguredInUpperCase() {
        aliasesByRealm.put("/acme", asSet("ACME.Example.COM"));

        assertThat(realmDnsAliases.contains("acme.example.com")).isTrue();
    }

    @Test
    public void shouldRejectHostThatIsNotADnsAliasOfAnyRealm() {
        assertThat(realmDnsAliases.contains("attacker.example.org")).isFalse();
    }

    @Test
    public void shouldRejectNullHost() {
        assertThat(realmDnsAliases.contains(null)).isFalse();
    }

    @Test
    public void shouldReadRealmWithoutDnsAliases() {
        aliasesByRealm.put("/acme/sub", null);

        assertThat(realmDnsAliases.contains("acme.example.com")).isTrue();
    }

    @Test
    public void shouldReadRealmsOnlyOnceWhileRealmConfigurationIsUnchanged() {
        realmDnsAliases.contains("acme.example.com");
        realmDnsAliases.contains("attacker.example.org");
        realmDnsAliases.contains("another-attacker.example.org");

        assertThat(realmReads).isEqualTo(1);
    }

    @Test
    public void shouldRegisterServiceListenerOnlyOnce() {
        realmDnsAliases.contains("acme.example.com");
        realmDnsAliases.contains("attacker.example.org");

        verify(idRepoService, times(1)).addListener(any(ServiceListener.class));
    }

    @Test
    public void shouldAcceptDnsAliasAddedAfterRealmConfigurationChange() {
        assertThat(realmDnsAliases.contains("new.example.com")).isFalse();
        aliasesByRealm.put("/acme", asSet("acme.example.com", "new.example.com"));

        realmConfigurationChanged();

        assertThat(realmDnsAliases.contains("new.example.com")).isTrue();
    }

    @Test
    public void shouldRejectDnsAliasRemovedAfterRealmConfigurationChange() {
        assertThat(realmDnsAliases.contains("acme.example.com")).isTrue();
        aliasesByRealm.put("/acme", asSet());

        realmConfigurationChanged();

        assertThat(realmDnsAliases.contains("acme.example.com")).isFalse();
    }

    @Test
    public void shouldAcceptDnsAliasOfRealmCreatedAfterRealmConfigurationChange() {
        assertThat(realmDnsAliases.contains("other.example.com")).isFalse();
        aliasesByRealm.put("/other", asSet("other.example.com"));

        realmConfigurationChanged();

        assertThat(realmDnsAliases.contains("other.example.com")).isTrue();
    }

    @Test
    public void shouldReadRealmsAgainAfterGlobalConfigurationChange() {
        realmDnsAliases.contains("acme.example.com");
        verify(idRepoService).addListener(serviceListenerCaptor.capture());

        serviceListenerCaptor.getValue().globalConfigChanged(IdConstants.REPO_SERVICE, "1.0", "", "",
                ServiceListener.MODIFIED);
        realmDnsAliases.contains("acme.example.com");

        assertThat(realmReads).isEqualTo(2);
    }

    @Test
    public void shouldReadRealmsAgainWhenRealmConfigurationChangesWhileTheyAreRead() {
        realmReadDuringChange = "/acme";

        assertThat(realmDnsAliases.contains("acme.example.com")).isTrue();
        assertThat(realmDnsAliases.contains("new.example.com")).isTrue();
    }

    @Test
    public void shouldRejectHostWhenRealmsCannotBeRead() {
        failNextRealmRead = true;

        assertThat(realmDnsAliases.contains("acme.example.com")).isFalse();
    }

    @Test
    public void shouldReadRealmsAgainAfterTheyCouldNotBeRead() {
        failNextRealmRead = true;
        realmDnsAliases.contains("acme.example.com");

        assertThat(realmDnsAliases.contains("acme.example.com")).isTrue();
    }

    @Test
    public void shouldRejectHostWhenAdminTokenCannotBeObtained() {
        failNextAdminTokenLookup = true;

        assertThat(realmDnsAliases.contains("acme.example.com")).isFalse();
    }

    @Test
    public void shouldRejectHostWhenServiceListenerCannotBeRegistered() {
        failNextIdRepoServiceLookup = true;

        assertThat(realmDnsAliases.contains("acme.example.com")).isFalse();
    }

    @Test
    public void shouldRegisterServiceListenerAfterIdRepoServiceCouldNotBeObtained() {
        failNextIdRepoServiceLookup = true;
        realmDnsAliases.contains("acme.example.com");

        realmDnsAliases.contains("acme.example.com");

        verify(idRepoService, times(1)).addListener(any(ServiceListener.class));
    }

    @Test
    public void shouldReadRealmsAgainOnceServiceListenerIsRegisteredAfterFailedRegistration() {
        given(idRepoService.addListener(any(ServiceListener.class))).willReturn(null, "LISTENER_ID");
        assertThat(realmDnsAliases.contains("acme.example.com")).isTrue();
        // no listener is registered yet, so the change is not notified
        aliasesByRealm.put("/acme", asSet());

        assertThat(realmDnsAliases.contains("acme.example.com")).isFalse();
    }

    @Test
    public void shouldNotReadRealmsWhileServiceListenerIsBeingRegistered() throws Exception {
        AtomicBoolean firstRegistration = new AtomicBoolean(true);
        AtomicInteger realmReadsDuringRegistration = new AtomicInteger();
        Thread concurrentRequest = new Thread(() -> realmDnsAliases.contains("acme.example.com"));
        given(idRepoService.addListener(any(ServiceListener.class))).willAnswer(invocation -> {
            if (firstRegistration.compareAndSet(true, false)) {
                // the concurrent request either reads the realms or waits for the registration to complete
                concurrentRequest.start();
                while (concurrentRequest.isAlive() && concurrentRequest.getState() != Thread.State.BLOCKED
                        && concurrentRequest.getState() != Thread.State.WAITING) {
                    concurrentRequest.join(10);
                }
                realmReadsDuringRegistration.set(realmReads);
            }
            return "LISTENER_ID";
        });

        realmDnsAliases.contains("acme.example.com");
        concurrentRequest.join();

        assertThat(realmReadsDuringRegistration.get()).isEqualTo(0);
    }

    private SSOToken getAdminToken() {
        if (failNextAdminTokenLookup) {
            failNextAdminTokenLookup = false;
            throw new AMSecurityPropertiesException("The admin token cannot be obtained");
        }
        return adminToken;
    }

    private ServiceConfigManager getIdRepoService() {
        if (failNextIdRepoServiceLookup) {
            failNextIdRepoServiceLookup = false;
            // as RealmGuiceModule.provideIdRepoService reports an SMSException
            throw new RuntimeException(new SMSException("Configuration store is unavailable"));
        }
        return idRepoService;
    }

    private void realmConfigurationChanged() {
        verify(idRepoService).addListener(serviceListenerCaptor.capture());
        serviceListenerCaptor.getValue().organizationConfigChanged(IdConstants.REPO_SERVICE, "1.0",
                "o=acme,ou=services,dc=openam,dc=openidentityplatform,dc=org", "", "", ServiceListener.MODIFIED);
    }
}
