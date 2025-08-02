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
 * Copyright 2016 ForgeRock AS.
 * Portions copyright 2025 3A Systems LLC.
 */

package org.forgerock.openam.core.realms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;
import static org.testng.Assert.fail;

import jakarta.inject.Provider;

import com.sun.identity.idm.IdRepoErrorCode;
import com.sun.identity.sm.ServiceConfigManager;
import com.sun.identity.sm.ServiceListener;
import org.forgerock.openam.core.CoreWrapper;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class CachingRealmLookupTest {

    private CachingRealmLookup realmsCache;

    @Mock
    private CoreWrapper coreWrapper;
    @Mock
    private RealmLookup realmLookupDelegate;
    @Mock
    private ServiceConfigManager idRepoService;
    @Captor
    private ArgumentCaptor<ServiceListener> serviceListenerCaptor;
    private RealmTestHelper realmTestHelper;

    @BeforeMethod
    public void setup() throws Exception {
        MockitoAnnotations.initMocks(this);
        realmTestHelper = new RealmTestHelper(coreWrapper);
        realmTestHelper.setupRealmClass();
        realmsCache = new CachingRealmLookup(realmLookupDelegate, new Provider<ServiceConfigManager>() {
            @Override
            public ServiceConfigManager get() {
                return idRepoService;
            }
        });
    }

    @AfterMethod
    public void tearDown() {
        realmTestHelper.tearDownRealmClass();
    }

    @Test
    public void shouldOnlyInitOnce() throws Exception {

        when(idRepoService.addListener(any(ServiceListener.class))).thenAnswer(new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocationOnMock) throws Throwable {
                Thread.sleep(1000L);
                return "LISTENER_ID";
            }
        });

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    realmsCache.isActive(Realm.root());
                } catch (RealmLookupException e) {
                    fail(Thread.currentThread().getName() + " failed", e);
                }
            }
        };

        Thread t1 = new Thread(runnable, "Thread 1");
        Thread t2 = new Thread(runnable, "Thread 2");

        t1.start();
        t2.start();

        t1.join();
        t2.join();

        verify(idRepoService).addListener(any(ServiceListener.class));
    }

    @Test
    public void shouldCacheSuccessfulRealmLookupResult() throws Exception {

        //Given
        Realm realm = mockRealm();
        when(realmLookupDelegate.lookup("/realm")).thenReturn(realm);

        realmsCache.lookup("/realm");

        //When
        Realm returnedRealm = realmsCache.lookup("/realm");

        //Then
        assertThat(returnedRealm).isEqualTo(realm);
        verify(realmLookupDelegate).lookup("/realm");
        verifyNoMoreInteractions(realmLookupDelegate);
    }

    @Test
    public void shouldCacheUnsuccessfulRealmLookupResult() throws Exception {

        //Given
        when(realmLookupDelegate.lookup("/realm"))
                .thenThrow(new NoRealmFoundException("/realm"));

        try {
            realmsCache.lookup("/realm");
        } catch (NoRealmFoundException ignored) {
            // Expected IdRepoException
        }

        try {
            //When
            realmsCache.lookup("/realm");
            fail("Expected IdRepoException");
        } catch (RealmLookupException e) {
            //Then
            assertThat(e.getErrorCode()).isEqualTo(IdRepoErrorCode.NO_MAPPING_FOUND);
            assertThat(e.getMessageArgs()).containsOnly("/realm");
            verify(realmLookupDelegate).lookup("/realm");
            verifyNoMoreInteractions(realmLookupDelegate);
        }
    }

    @Test
    public void shouldNotCacheFailedRealmLookup() throws Exception {

        //Given
        when(realmLookupDelegate.lookup("/realm"))
                .thenThrow(new MultipleRealmsFoundException("/realm"));

        try {
            realmsCache.lookup("/realm");
        } catch (MultipleRealmsFoundException ignored) {
            // Expected IdRepoException
        }

        try {
            //When
            realmsCache.lookup("/realm");
            fail("Expected IdRepoException");
        } catch (RealmLookupException e) {
            //Then
            verify(realmLookupDelegate, times(2)).lookup("/realm");
            verifyNoMoreInteractions(realmLookupDelegate);
        }
    }

    @Test
    public void shouldClearSuccessfulRealmLookupCacheOnGlobalConfigChange() throws Exception {

        //Given
        Realm realm = mockRealm();
        when(realmLookupDelegate.lookup("/realm"))
                .thenReturn(realm)
                .thenThrow(NoRealmFoundException.class);

        registerServiceListener();

        realmsCache.lookup("/realm");

        //When
        invokeServiceListenerGlobalConfigChange();
        try {
            realmsCache.lookup("/realm");
            fail("Expected IdRepoException");
        } catch (RealmLookupException e) {
            //Then
            verify(realmLookupDelegate, times(2)).lookup("/realm");
            verifyNoMoreInteractions(realmLookupDelegate);
        }
    }

    @Test
    public void shouldClearSuccessfulRealmLookupCacheOnOrganisationConfigChange() throws Exception {

        //Given
        Realm realm = mockRealm();
        when(realmLookupDelegate.lookup("/realm"))
                .thenReturn(realm)
                .thenThrow(NoRealmFoundException.class);

        registerServiceListener();

        realmsCache.lookup("/realm");

        //When
        invokeServiceListenerOrganisationConfigChange();
        try {
            realmsCache.lookup("/realm");
            fail("Expected IdRepoException");
        } catch (RealmLookupException e) {
            //Then
            verify(realmLookupDelegate, times(2)).lookup("/realm");
            verifyNoMoreInteractions(realmLookupDelegate);
        }
    }

    @Test
    public void shouldNotClearSuccessfulRealmLookupCacheOnSchemaChange() throws Exception {

        //Given
        Realm realm = mockRealm();
        when(realmLookupDelegate.lookup("/realm"))
                .thenReturn(realm)
                .thenThrow(NoRealmFoundException.class);

        registerServiceListener();

        realmsCache.lookup("/realm");

        //When
        invokeServiceListenerSchemaChange();
        Realm returnedRealm = realmsCache.lookup("/realm");

        //Then
        assertThat(returnedRealm).isEqualTo(realm);
        verify(realmLookupDelegate).lookup("/realm");
        verifyNoMoreInteractions(realmLookupDelegate);
    }

    @Test
    public void shouldClearUnsuccessfulRealmLookupCacheOnGlobalConfigChange() throws Exception {

        //Given
        Realm realm = mockRealm();
        when(realmLookupDelegate.lookup("/realm"))
                .thenThrow(new NoRealmFoundException("/realm"))
                .thenReturn(realm);

        registerServiceListener();

        try {
            realmsCache.lookup("/realm");
        } catch (NoRealmFoundException ignored) {
            // Expected IdRepoException
        }

        //When
        invokeServiceListenerGlobalConfigChange();
        Realm returnedRealm = realmsCache.lookup("/realm");

        //Then
        assertThat(returnedRealm).isEqualTo(realm);
        verify(realmLookupDelegate, times(2)).lookup("/realm");
        verifyNoMoreInteractions(realmLookupDelegate);
    }

    @Test
    public void shouldClearUnsuccessfulRealmLookupCacheOnOrganisationConfigChange() throws Exception {

        //Given
        Realm realm = mockRealm();
        when(realmLookupDelegate.lookup("/realm"))
                .thenThrow(new NoRealmFoundException("/realm"))
                .thenReturn(realm);

        registerServiceListener();

        try {
            realmsCache.lookup("/realm");
        } catch (NoRealmFoundException ignored) {
            // Expected IdRepoException
        }

        //When
        invokeServiceListenerOrganisationConfigChange();
        Realm returnedRealm = realmsCache.lookup("/realm");

        //Then
        assertThat(returnedRealm).isEqualTo(realm);
        verify(realmLookupDelegate, times(2)).lookup("/realm");
        verifyNoMoreInteractions(realmLookupDelegate);
    }

    @Test
    public void shouldNotClearUnsuccessfulRealmLookupCacheOnSchemaChange() throws Exception {

        //Given
        Realm realm = mockRealm();
        when(realmLookupDelegate.lookup("/realm"))
                .thenThrow(new NoRealmFoundException("/realm"))
                .thenReturn(realm);

        registerServiceListener();

        try {
            realmsCache.lookup("/realm");
        } catch (NoRealmFoundException ignored) {
            // Expected IdRepoException
        }

        //When
        invokeServiceListenerSchemaChange();
        try {
            realmsCache.lookup("/realm");
            fail("Expected IdRepoException");
        } catch (RealmLookupException e) {
            //Then
            assertThat(e.getErrorCode()).isEqualTo(IdRepoErrorCode.NO_MAPPING_FOUND);
            assertThat(e.getMessageArgs()).containsOnly("/realm");
            verify(realmLookupDelegate).lookup("/realm");
            verifyNoMoreInteractions(realmLookupDelegate);
        }
    }

    @Test
    public void shouldCacheIsActiveResult() throws Exception {

        //Given
        Realm realm = mockRealm();
        when(realmLookupDelegate.lookup("/realm")).thenReturn(realm);
        when(realmLookupDelegate.isActive(realm)).thenReturn(true);

        realmsCache.isActive(realm);

        //When
        boolean active = realmsCache.isActive(realm);

        //Then
        assertThat(active).isTrue();
        verify(realmLookupDelegate).isActive(realm);
        verifyNoMoreInteractions(realmLookupDelegate);
    }

    @Test
    public void shouldClearIsActiveCacheOnGlobalConfigChange() throws Exception {

        //Given
        Realm realm = mockRealm();
        when(realmLookupDelegate.lookup("/realm")).thenReturn(realm);
        registerServiceListener();

        when(realmLookupDelegate.isActive(realm)).thenReturn(true).thenReturn(false);

        realmsCache.isActive(realm);

        //When
        invokeServiceListenerGlobalConfigChange();
        boolean active = realmsCache.isActive(realm);

        //Then
        assertThat(active).isFalse();
        verify(realmLookupDelegate, times(2)).isActive(realm);
        verifyNoMoreInteractions(realmLookupDelegate);
    }

    @Test
    public void shouldClearIsActiveCacheOnOrganisationConfigChange() throws Exception {

        //Given
        Realm realm = mockRealm();
        when(realmLookupDelegate.lookup("/realm")).thenReturn(realm);
        registerServiceListener();

        when(realmLookupDelegate.isActive(realm)).thenReturn(true).thenReturn(false);

        realmsCache.isActive(realm);

        //When
        invokeServiceListenerOrganisationConfigChange();
        boolean active = realmsCache.isActive(realm);

        //Then
        assertThat(active).isFalse();
        verify(realmLookupDelegate, times(2)).isActive(realm);
        verifyNoMoreInteractions(realmLookupDelegate);
    }

    @Test
    public void shouldNotClearIsActiveCacheOnSchemaChange() throws Exception {

        //Given
        Realm realm = mockRealm();
        when(realmLookupDelegate.lookup("/realm")).thenReturn(realm);
        registerServiceListener();

        when(realmLookupDelegate.isActive(realm)).thenReturn(true).thenReturn(false);

        realmsCache.isActive(realm);

        //When
        invokeServiceListenerSchemaChange();
        boolean active = realmsCache.isActive(realm);

        //Then
        assertThat(active).isTrue();
        verify(realmLookupDelegate).isActive(realm);
        verifyNoMoreInteractions(realmLookupDelegate);
    }

    private Realm mockRealm() {
        when(coreWrapper.convertOrgNameToRealmName("o=realm,ou=services,dc=openam,dc=example,dc=com"))
                .thenReturn("/realm");
        return new Realm("o=realm,ou=services,dc=openam,dc=example,dc=com");
    }

    private void registerServiceListener() {
        when(idRepoService.addListener(serviceListenerCaptor.capture())).thenReturn("LISTENER_ID");
    }

    private void invokeServiceListenerGlobalConfigChange() {
        serviceListenerCaptor.getValue().globalConfigChanged(null, null, null, null, 0);
    }

    private void invokeServiceListenerOrganisationConfigChange() {
        serviceListenerCaptor.getValue().organizationConfigChanged(null, null, null, null, null, 0);
    }

    private void invokeServiceListenerSchemaChange() {
        serviceListenerCaptor.getValue().schemaChanged(null, null);
    }
}
