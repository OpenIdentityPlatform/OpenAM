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
 * Portions Copyrighted 2025 3A Systems, LLC.
 */

package org.forgerock.openam.core.realms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.util.Arrays;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.iplanet.sso.SSOToken;
import org.apache.commons.lang3.StringUtils;
import org.assertj.core.description.Description;
import org.forgerock.openam.core.CoreWrapper;
import org.forgerock.util.Reject;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Test helper class for setting up the {@link Realm} class so that other tests can use
 * {@code Realm}.
 *
 * @since 14.0.0
 */
public class RealmTestHelper {

    private static final String ROOT_REALM_PATH = "/";
    private static final String BASE_DN = "dc=openam,dc=example,dc=com";
    private static final String SERVICES_DN = "ou=services";
    private static final String REALM_DN_PREFIX = "o=";

    private static StackTraceElement[] lastSetup;

    @Mock
    private RealmLookup realmLookup;
    @Mock
    private CoreWrapper coreWrapper;
    @Mock
    private SSOToken token;

    /**
     * Constructs a new RealmTestHelper using its own mocks.
     */
    public RealmTestHelper() {
        init();
        MockitoAnnotations.initMocks(this);
    }

    /**
     * Constructs a new RealmTestHelper using the provided {@literal coreWrapper} instead of its
     * own mocks.
     *
     * @param coreWrapper A {@link CoreWrapper} Mockito mock.
     */
    public RealmTestHelper(CoreWrapper coreWrapper) {
        this();
        this.coreWrapper = coreWrapper;
    }

    /**
     * Constructs a new RealmTestHelper using the provided {@literal realms} and
     * {@literal coreWrapper} instead of its own mocks.
     *
     * @param realmLookup A {@link RealmLookup} Mockito mock.
     * @param coreWrapper A {@link CoreWrapper} Mockito mock.
     */
    RealmTestHelper(RealmLookup realmLookup, CoreWrapper coreWrapper) {
        init();
        this.realmLookup = realmLookup;
        this.coreWrapper = coreWrapper;
    }

    private void init() {
        assertThat(lastSetup).describedAs(new Description() {
            @Override
            public String value() {
                StringBuilder s = new StringBuilder("Previously setup Realm class - forgot to call teardown");
                for (StackTraceElement e : Arrays.copyOfRange(lastSetup, 1, 6)) {
                    s.append("\n  at ").append(e);
                }
                return s.toString();
            }
        }).isNull();
    }

    /**
     * Sets up the {@link Realm} class so that it is initialised for use within tests.
     *
     * <p>MUST ensure {@link #tearDownRealmClass()} is called when tearing down test class.</p>
     */
    public void setupRealmClass() throws Exception {
        lastSetup = Thread.currentThread().getStackTrace();
        Guice.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                bind(RealmLookup.class).toInstance(realmLookup);
                bind(CoreWrapper.class).toInstance(coreWrapper);
                requestStaticInjection(Realm.class);
            }
        });
        mockRootRealm();
    }

    /**
     * Tears down setup of the {@link Realm} class.
     */
    public void tearDownRealmClass() {
        lastSetup = null;
    }

    private void mockRootRealm() throws Exception {
        when(coreWrapper.convertRealmNameToOrgName(ROOT_REALM_PATH)).thenReturn(BASE_DN);
        when(coreWrapper.convertOrgNameToRealmName(BASE_DN)).thenReturn(ROOT_REALM_PATH);
        Realm rootRealm = new Realm(BASE_DN);
        when(realmLookup.lookup(ROOT_REALM_PATH)).thenReturn(rootRealm);
    }

    /**
     * Mocks a {@link Realm} that is mapped to a hostname.
     *
     * @param dns The name of the host that maps to the realm.
     * @param realmParts An array of path elements of the realm to create. Elements cannot contain '/'.
     * @return A {@code Realm} instance for the provided realm path.
     */
    public Realm mockDnsAlias(String dns, String... realmParts) {
        Realm realm = mockRealm(realmParts);
        try {
            when(realmLookup.lookup(dns)).thenReturn(realm);
            return realm;
        } catch (RealmLookupException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Mocks a {@link Realm} that with an alias.
     *
     * @param alias The alias of the realm.
     * @param realmParts An array of path elements of the realm to create. Elements cannot contain '/'.
     * @return A {@code Realm} instance for the provided realm path.
     */
    public Realm mockRealmAlias(String alias, String... realmParts) {
        Realm realm = mockRealm(realmParts);
        try {
            when(realmLookup.lookup(alias)).thenReturn(realm);
            return realm;
        } catch (RealmLookupException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Mocks a {@link Realm} for the provided realm path.
     *
     * @param realmParts An array of path elements of the realm to create. Elements cannot contain '/'.
     * @return A {@code Realm} instance for the provided realm path.
     */
    public Realm mockRealm(String... realmParts) {
        Reject.ifNull(realmParts);
        for (String realm : realmParts) {
            Reject.ifTrue(realm.contains("/"), "realm part cannot contain '/'");
        }
        if (realmParts.length == 0) {
            return Realm.root();
        }
        StringBuilder sb = new StringBuilder();
        for (String realm : realmParts) {
            sb.append(REALM_DN_PREFIX).append(realm).append(",");
        }
        sb.append(SERVICES_DN).append(",").append(BASE_DN);
        String realmDN = sb.toString();
        String realmPath = "/" + StringUtils.join(realmParts, "/");
        when(coreWrapper.convertOrgNameToRealmName(realmDN)).thenReturn(realmPath);
        when(coreWrapper.convertRealmNameToOrgName(realmPath)).thenReturn(realmDN);
        when(coreWrapper.convertRealmNameToOrgName(realmPath.substring(1))).thenReturn(realmDN);
        Realm realm = new Realm(realmDN);
        try {
            when(realmLookup.lookup(realmPath)).thenReturn(realm);
            return realm;
        } catch (RealmLookupException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Mocks an invalid realm that will cause a {@code RealmLookupException} to be thrown when the
     * realm is used.
     *
     * @param realmParts An array of path elements of the realm to create. Elements cannot contain '/'.
     */
    public void mockInvalidRealm(String... realmParts) {
        Reject.ifNull(realmParts);
        for (String realm : realmParts) {
            Reject.ifTrue(realm.contains("/"), "realm part cannot contain '/'");
        }
        StringBuilder sb = new StringBuilder();
        for (String realm : realmParts) {
            sb.append(REALM_DN_PREFIX).append(realm).append(",");
        }
        sb.append(SERVICES_DN).append(",").append(BASE_DN);
        String realmPath = "/" + StringUtils.join(realmParts, "/");
        try {
            RealmLookupException exception = mock(RealmLookupException.class);
            when(exception.getRealm()).thenReturn(realmPath);
            doThrow(exception).when(realmLookup).lookup(realmPath);
        } catch (RealmLookupException e) {
            throw new RuntimeException(e);
        }
    }
}
