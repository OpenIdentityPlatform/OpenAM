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
 */

package org.forgerock.openam.core.realms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.forgerock.openam.core.CoreWrapper;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class RealmTest {

    @Mock
    private RealmLookup realmLookup;
    @Mock
    private CoreWrapper coreWrapper;
    private RealmTestHelper realmTestHelper;

    @BeforeMethod
    public void setup() throws Exception {
        MockitoAnnotations.initMocks(this);
        realmTestHelper = new RealmTestHelper(realmLookup, coreWrapper);
        realmTestHelper.setupRealmClass();
    }

    @AfterMethod
    public void tearDown() {
        realmTestHelper.tearDownRealmClass();
    }

    @Test
    public void shouldGetRootRealm() {

        //When
        Realm rootRealm = Realm.root();

        //Then
        assertThat(rootRealm.asPath()).isEqualTo("/");
        assertThat(rootRealm.asDN()).isEqualTo("dc=openam,dc=example,dc=com");
        assertThat(rootRealm.toString()).isEqualTo("/");
    }

    @Test
    public void shouldAlwaysGetTheSameRootRealmInstance() {

        //Given
        Realm rootRealm = Realm.root();

        //When
        Realm rootRealmAgain = Realm.root();

        //Then
        assertThat(rootRealmAgain == rootRealm).isTrue();
        assertThat(rootRealmAgain).isEqualTo(rootRealm);
        assertThat(rootRealmAgain.hashCode()).isEqualTo(rootRealm.hashCode());
        assertThat(rootRealmAgain.toString()).isEqualTo(rootRealm.toString());
    }

    @Test
    public void shouldLookupRealm() throws Exception {

        //When
        Realm.of("/realm");

        //Then
        verify(realmLookup).lookup("/realm");
    }

    @Test(expectedExceptions = RealmLookupException.class)
    public void shouldThrowRealmLookupExceptionOnFailedRealmLookup()
            throws Exception {

        //Given
        when(realmLookup.lookup("/realm")).thenThrow(RealmLookupException.class);

        //When
        Realm.of("/realm");

        //Then
        //Expected RealmLookupException
    }
}
