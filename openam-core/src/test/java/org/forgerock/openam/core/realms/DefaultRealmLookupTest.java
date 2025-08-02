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
import static org.mockito.BDDMockito.given;

import jakarta.inject.Provider;
import java.util.HashSet;
import java.util.Set;

import com.iplanet.sso.SSOToken;
import com.sun.identity.idm.IdRepoErrorCode;
import com.sun.identity.sm.OrganizationConfigManagerFactory;
import com.sun.identity.sm.SMSException;
import org.forgerock.openam.core.CoreWrapper;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class DefaultRealmLookupTest {

    private DefaultRealmLookup defaultRealms;

    @Mock
    private SSOToken token;
    @Mock
    private CoreWrapper coreWrapper;
    @Mock
    private OrganizationConfigManagerFactory organizationConfigManagerFactory;
    private RealmTestHelper realmTestHelper;
    private Set<String> foundSubRealms = new HashSet<>();
    private Set<String> foundRealmAliases = new HashSet<>();

    @BeforeMethod
    public void setup() throws Exception {
        MockitoAnnotations.initMocks(this);
        realmTestHelper = new RealmTestHelper(coreWrapper);
        realmTestHelper.setupRealmClass();
        Provider<SSOToken> adminTokenProvider = new Provider<SSOToken>() {
            @Override
            public SSOToken get() {
                return token;
            }
        };
        defaultRealms = new DefaultRealmLookup(adminTokenProvider, organizationConfigManagerFactory) {
            @Override
            Set<String> getSubOrganisations(SSOToken token, String realm) {
                return foundSubRealms;
            }

            @Override
            Set<String> getRealmAliases(SSOToken token, String realmAlias) {
                return foundRealmAliases;
            }

            @Override
            boolean isCoexistenceMode() {
                return false;
            }

            @Override
            boolean isAMSDKEnabled() {
                return false;
            }
        };
        foundSubRealms.clear();
        foundRealmAliases.clear();
    }

    @AfterMethod
    public void tearDown() {
        realmTestHelper.tearDownRealmClass();
    }

    @DataProvider
    private Object[][] evaluatesToRootRealm() {
        return new Object[][]{
            {null}, {""}, {"/"}
        };
    }

    @Test(dataProvider = "evaluatesToRootRealm")
    public void itLooksUpRootRealmForEmptyStringOrSlash(String realmPath) throws Exception {

        //When
        Realm realm = defaultRealms.lookup(realmPath);

        //Then
        assertThat(realm).isEqualTo(Realm.root());
    }

    @Test
    public void itLooksUpRealmViaPathFormat() throws Exception {

        //Given
        Realm realm = realmTestHelper.mockRealm("realm");
        given(organizationConfigManagerFactory.create(token, realm.asPath())).willReturn(null);

        //When
        Realm returnedRealm = defaultRealms.lookup(realm.asPath());

        //Then
        assertThat(returnedRealm).isEqualTo(realm);
    }

    @Test
    public void itThrowsNoMappingFoundExceptionWhenRealmLookUpViaPathFormatFails() throws Exception {

        //Given
        Realm realm = realmTestHelper.mockRealm("realm");
        given(organizationConfigManagerFactory.create(token, realm.asPath())).willThrow(SMSException.class);

        try {
            //When
            defaultRealms.lookup(realm.asPath());
        } catch (RealmLookupException e) {
            //Then
            assertThat(e.getErrorCode()).isEqualTo(IdRepoErrorCode.NO_MAPPING_FOUND);
            assertThat(e.getMessageArgs()).containsOnly(realm.asPath());
        }
    }

    @Test
    public void itLooksUpRealmViaDNFormat() throws Exception {

        //Given
        Realm realm = realmTestHelper.mockRealm("realm");
        given(organizationConfigManagerFactory.create(token, realm.asDN())).willReturn(null);

        //When
        Realm returnedRealm = defaultRealms.lookup(realm.asDN());

        //Then
        assertThat(returnedRealm).isEqualTo(realm);
    }

    @Test
    public void itThrowsNoMappingFoundExceptionWhenRealmLookupViaDNFormatFails() throws Exception {

        //Given
        Realm realm = realmTestHelper.mockRealm("realm");
        given(organizationConfigManagerFactory.create(token, realm.asDN())).willThrow(SMSException.class);

        try {
            //When
            defaultRealms.lookup(realm.asDN());
        } catch (RealmLookupException e) {
            //Then
            assertThat(e.getErrorCode()).isEqualTo(IdRepoErrorCode.NO_MAPPING_FOUND);
            assertThat(e.getMessageArgs()).containsOnly(realm.asDN());
        }
    }

    @Test
    public void itThrowsNoMappingFoundExceptionWhenRealmIsNotFoundUsingLdap() throws Exception {

        try {
            //When
            defaultRealms.lookup("realm");
        } catch (RealmLookupException e) {
            //Then
            assertThat(e.getErrorCode()).isEqualTo(IdRepoErrorCode.NO_MAPPING_FOUND);
            assertThat(e.getMessageArgs()).containsOnly("realm");
        }
    }

    @Test
    public void itLooksUpRealmUsingLdap() throws Exception {

        //Given
        Realm realm = realmTestHelper.mockRealm("realm");
        foundSubRealms.add("realm");

        //When
        Realm returnedRealm = defaultRealms.lookup("realm");

        //Then
        assertThat(returnedRealm).isEqualTo(realm);
    }

    @Test
    public void itThrowsMultipleMappingsFoundExceptionWhenLdapReturnsMultipleRealmsWithSameName() throws Exception {

        //Given
        foundSubRealms.add("realm");
        foundSubRealms.add("other/realm");

        try {
            //When
            defaultRealms.lookup("realm");
        } catch (RealmLookupException e) {
            //Then
            assertThat(e.getErrorCode()).isEqualTo(IdRepoErrorCode.MULTIPLE_MAPPINGS_FOUND);
            assertThat(e.getMessageArgs()).containsOnly("realm");
        }
    }

    @Test
    public void itLooksUpRealmAliasUsingLdap() throws Exception {

        //Given
        Realm realm = realmTestHelper.mockRealm("realm");
        foundRealmAliases.add("realm");

        //When
        Realm returnedRealm = defaultRealms.lookup("alias");

        //Then
        assertThat(returnedRealm).isEqualTo(realm);
    }

    @Test
    public void itLooksUpRealmAndResolvesSingleRealmWithSameNameAndAlias() throws Exception {

        //Given
        Realm realm = realmTestHelper.mockRealm("realm");
        foundSubRealms.add("realm");
        foundRealmAliases.add("realm");

        //When
        Realm returnedRealm = defaultRealms.lookup("realm");

        //Then
        assertThat(returnedRealm).isEqualTo(realm);
    }

    @Test
    public void itThrowsMultipleMappingsFoundExceptionWhenRealmWithMatchingNameAndRealmWithMatchingAlias() throws Exception {

        //Given
        realmTestHelper.mockRealm("realm");
        realmTestHelper.mockRealm("otherRealm");
        foundSubRealms.add("realm");
        foundRealmAliases.add("otherRealm");

        try {
            //When
            defaultRealms.lookup("realm");
        } catch (RealmLookupException e) {
            //Then
            assertThat(e.getErrorCode()).isEqualTo(IdRepoErrorCode.MULTIPLE_MAPPINGS_FOUND);
            assertThat(e.getMessageArgs()).containsOnly("realm");
        }
    }

    @Test
    public void itThrowsMultipleMappingsFoundExceptionWhenMultipleRealmsFoundWithSameAlias() throws Exception {

        //Given
        foundRealmAliases.add("realm");
        foundRealmAliases.add("otherRealm");

        try {
            //When
            defaultRealms.lookup("realm");
        } catch (RealmLookupException e) {
            //Then
            assertThat(e.getErrorCode()).isEqualTo(IdRepoErrorCode.MULTIPLE_MAPPINGS_FOUND);
            assertThat(e.getMessageArgs()).containsOnly("realm");
        }
    }
}
