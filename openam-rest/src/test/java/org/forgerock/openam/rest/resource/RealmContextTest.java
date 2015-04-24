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
 * Copyright 2014-2015 ForgeRock AS.
 */

package org.forgerock.openam.rest.resource;

import org.forgerock.json.resource.RootContext;
import org.forgerock.util.Pair;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.fest.assertions.Assertions.assertThat;

public class RealmContextTest {

    @DataProvider(name = "realmData")
    private Object[][] getRealmData() {
        return new Object[][]{
            // @formatter:off

            //DNS                               URI                                 URI                                 QUERY               EXPECTATION
            {null,                              null,                               null,                               null,               "/"},
            {Pair.of("DNS_ALIAS", "/REALM"),    null,                               null,                               null,               "/REALM"},
            {null,                              Pair.of("SUB_REALM_1", "/REALM_1"), null,                               null,               "/REALM_1"},
            {null,                              Pair.of("SUB_REALM_1", "/REALM_1"), Pair.of("SUB_REALM_2", "/REALM_2"), null,               "/REALM_1/REALM_2"},
            {null,                              null,                               null,                               "/OVERRIDE_REALM",  "/OVERRIDE_REALM"},
            {Pair.of("DNS_ALIAS", "/REALM"),    Pair.of("SUB_REALM_1", "/REALM_1"), null,                               null,               "/REALM/REALM_1"},
            {Pair.of("DNS_ALIAS", "/REALM"),    Pair.of("SUB_REALM_1", "/REALM_1"), Pair.of("SUB_REALM_2", "/REALM_2"), null,               "/REALM/REALM_1/REALM_2"},
            {Pair.of("DNS_ALIAS", "/REALM"),    Pair.of("SUB_REALM_1", "/REALM_1"), Pair.of("SUB_REALM_2", "/REALM_2"), "/OVERRIDE_REALM",  "/OVERRIDE_REALM"},

            // @formatter:on
        };
    }

    @Test(dataProvider = "realmData")
    public void getResolvedRealmShouldReturnCorrectRealm(Pair<String, String> dnsRealm, Pair<String, String> uriRealm1,
            Pair<String, String> uriRealm2, String queryParameterRealm, String expectedRealm) {

        //Given
        RealmContext context = new RealmContext(new RootContext());
        if (dnsRealm != null) {
            context.addDnsAlias(dnsRealm.getFirst(), dnsRealm.getSecond());
        }
        if (uriRealm1 != null) {
            context.addSubRealm(uriRealm1.getFirst(), uriRealm1.getSecond());
        }
        if (uriRealm2 != null) {
            context.addSubRealm(uriRealm2.getFirst(), uriRealm2.getSecond());
        }
        if (queryParameterRealm != null) {
            context.setOverrideRealm(queryParameterRealm);
        }

        //When
        String resolvedRealm = context.getResolvedRealm();

        //Then
        assertThat(resolvedRealm).isEqualTo(expectedRealm);
    }

    @DataProvider(name = "baseRealmData")
    private Object[][] getBaseRealmData() {
        return new Object[][]{
            // @formatter:off

            //DNS                               URI                                 URI                                 EXPECTATION
            {null,                              null,                               null,                               "/"},
            {null,                              Pair.of("SUB_REALM_1", "/REALM_1"), Pair.of("SUB_REALM_2", "/REALM_2"), "/REALM_1/REALM_2"},
            {Pair.of("DNS_ALIAS", "/"),         Pair.of("SUB_REALM_1", "/REALM_1"), Pair.of("SUB_REALM_2", "/REALM_2"), "/REALM_1/REALM_2"},
            {Pair.of("DNS_ALIAS", "/REALM"),    Pair.of("SUB_REALM_1", "/REALM_1"), Pair.of("SUB_REALM_2", "/REALM_2"), "/REALM"},

            // @formatter:on
        };
    }

    @Test(dataProvider = "baseRealmData")
    public void shouldGetBaseRealm(Pair<String, String> dnsRealm, Pair<String, String> uriRealm1,
            Pair<String, String> uriRealm2, String expectedRealm) {

        //Given
        RealmContext context = new RealmContext(new RootContext());
        if (dnsRealm != null) {
            context.addDnsAlias(dnsRealm.getFirst(), dnsRealm.getSecond());
        }
        if (uriRealm1 != null) {
            context.addSubRealm(uriRealm1.getFirst(), uriRealm1.getSecond());
        }
        if (uriRealm2 != null) {
            context.addSubRealm(uriRealm2.getFirst(), uriRealm2.getSecond());
        }

        //When
        String baseRealm = context.getBaseRealm();

        //Then
        assertThat(baseRealm).isEqualTo(expectedRealm);
    }

    @Test
    public void getRelativeRealmShouldReturnRootRealmWhenNotSet() {

        //Given
        RealmContext context = new RealmContext(new RootContext());

        //When
        String relativeRealm = context.getRelativeRealm();

        //Then
        assertThat(relativeRealm).isEqualTo("/");
    }

    @Test
    public void shouldGetRelativeRealm() {

        //Given
        RealmContext context = new RealmContext(new RootContext());
        context.addSubRealm("SUB_REALM_1", "/REALM_1");

        //When
        String relativeRealm = context.getRelativeRealm();

        //Then
        assertThat(relativeRealm).isEqualTo("/REALM_1");
    }

    @DataProvider(name = "rebasedRealmData")
    private Object[][] getRebasedRealmData() {
        return new Object[][]{
            // @formatter:off

            //DNS                               URI                                 URI                                 EXPECTATION
            {null,                              null,                               null,                               "/"},
            {null,                              Pair.of("SUB_REALM_1", "/REALM_1"), Pair.of("SUB_REALM_2", "/REALM_2"), "/REALM_1/REALM_2"},
            {Pair.of("DNS_ALIAS", "/"),         Pair.of("SUB_REALM_1", "/REALM_1"), Pair.of("SUB_REALM_2", "/REALM_2"), "/REALM_1/REALM_2"},
            {Pair.of("DNS_ALIAS", "/REALM"),    Pair.of("SUB_REALM_1", "/REALM_1"), Pair.of("SUB_REALM_2", "/REALM_2"), "/REALM/REALM_1/REALM_2"},

            // @formatter:on
        };
    }

    @Test(dataProvider = "rebasedRealmData")
    public void shouldGetRebasedRealm(Pair<String, String> dnsRealm, Pair<String, String> uriRealm1,
            Pair<String, String> uriRealm2, String expectedRealm) {

        //Given
        RealmContext context = new RealmContext(new RootContext());
        if (dnsRealm != null) {
            context.addDnsAlias(dnsRealm.getFirst(), dnsRealm.getSecond());
        }
        if (uriRealm1 != null) {
            context.addSubRealm(uriRealm1.getFirst(), uriRealm1.getSecond());
        }
        if (uriRealm2 != null) {
            context.addSubRealm(uriRealm2.getFirst(), uriRealm2.getSecond());
        }

        //When
        String rebasedRealm = context.getRebasedRealm();

        //Then
        assertThat(rebasedRealm).isEqualTo(expectedRealm);
    }

    @Test
    public void getOverrideRealmShouldReturnNullWhenNotSet() {

        //Given
        RealmContext context = new RealmContext(new RootContext());

        //When
        String overrideRealm = context.getOverrideRealm();

        //Then
        assertThat(overrideRealm).isNull();
    }

    @Test
    public void shouldGetOverrideRealm() {

        //Given
        RealmContext context = new RealmContext(new RootContext());
        context.setOverrideRealm("/OVERRIDE_REALM");

        //When
        String overrideRealm = context.getOverrideRealm();

        //Then
        assertThat(overrideRealm).isEqualTo("/OVERRIDE_REALM");
    }
}
