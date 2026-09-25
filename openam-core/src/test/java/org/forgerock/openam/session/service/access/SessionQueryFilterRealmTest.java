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

package org.forgerock.openam.session.service.access;

import static org.assertj.core.api.Assertions.assertThat;

import org.forgerock.json.JsonPointer;
import org.forgerock.openam.utils.CrestQuery;
import org.forgerock.util.query.QueryFilter;
import org.testng.annotations.Test;

public class SessionQueryFilterRealmTest {

    private static final JsonPointer REALM = new JsonPointer("realm");
    private static final JsonPointer USERNAME = new JsonPointer("username");

    @Test
    public void shouldExtractTheRealmOfARealmOnlyFilter() {
        //given
        CrestQuery crestQuery = new CrestQuery(QueryFilter.equalTo(REALM, "/realmA"));

        //when
        String realm = SessionQueryFilterRealm.extract(crestQuery);

        //then
        assertThat(realm).isEqualTo("/realmA");
    }

    @Test
    public void shouldExtractTheRealmOfAUsernameAndRealmFilter() {
        //given
        CrestQuery crestQuery = new CrestQuery(QueryFilter.and(
                QueryFilter.equalTo(USERNAME, "demo"),
                QueryFilter.equalTo(REALM, "/realmA/sub")));

        //when
        String realm = SessionQueryFilterRealm.extract(crestQuery);

        //then
        assertThat(realm).isEqualTo("/realmA/sub");
    }

    @Test
    public void shouldExtractTheRealmOfANestedFilter() {
        //given
        CrestQuery crestQuery = new CrestQuery(QueryFilter.and(
                QueryFilter.and(QueryFilter.equalTo(REALM, "/")),
                QueryFilter.equalTo(USERNAME, "amadmin")));

        //when
        String realm = SessionQueryFilterRealm.extract(crestQuery);

        //then
        assertThat(realm).isEqualTo("/");
    }

    @Test
    public void shouldReturnNullWhenTheFilterDoesNotNameARealm() {
        //given
        CrestQuery crestQuery = new CrestQuery(QueryFilter.equalTo(USERNAME, "demo"));

        //when
        String realm = SessionQueryFilterRealm.extract(crestQuery);

        //then
        assertThat(realm).isNull();
    }

    @Test
    public void shouldReturnNullWhenTheRealmIsNotAString() {
        //given
        CrestQuery crestQuery = new CrestQuery(QueryFilter.equalTo(REALM, 42));

        //when
        String realm = SessionQueryFilterRealm.extract(crestQuery);

        //then
        assertThat(realm).isNull();
    }

    @Test
    public void shouldReturnNullWhenThereIsNoQueryFilter() {
        //given
        CrestQuery crestQuery = new CrestQuery("all");

        //when
        String realm = SessionQueryFilterRealm.extract(crestQuery);

        //then
        assertThat(realm).isNull();
    }

    @Test
    public void shouldReturnNullWhenThereIsNoQuery() {
        //when
        String realm = SessionQueryFilterRealm.extract(null);

        //then
        assertThat(realm).isNull();
    }

    @Test
    public void shouldAcceptTheSameRealmTwice() {
        //given
        CrestQuery crestQuery = new CrestQuery(QueryFilter.and(
                QueryFilter.equalTo(REALM, "/realmA"),
                QueryFilter.equalTo(REALM, "/realmA")));

        //when
        String realm = SessionQueryFilterRealm.extract(crestQuery);

        //then
        assertThat(realm).isEqualTo("/realmA");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void shouldRejectAFilterNamingMoreThanOneRealm() {
        //given
        CrestQuery crestQuery = new CrestQuery(QueryFilter.and(
                QueryFilter.equalTo(REALM, "/realmA"),
                QueryFilter.equalTo(REALM, "/")));

        //when
        SessionQueryFilterRealm.extract(crestQuery);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void shouldRejectARealmWithAParentPathSegment() {
        //given
        CrestQuery crestQuery = new CrestQuery(QueryFilter.equalTo(REALM, "/realmA/../realmB"));

        //when
        SessionQueryFilterRealm.extract(crestQuery);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void shouldRejectARealmThatIsOnlyAParentPathSegment() {
        //given
        CrestQuery crestQuery = new CrestQuery(QueryFilter.equalTo(REALM, ".."));

        //when
        SessionQueryFilterRealm.extract(crestQuery);
    }

    @Test
    public void shouldAcceptARealmNameContainingTwoDots() {
        //given
        CrestQuery crestQuery = new CrestQuery(QueryFilter.equalTo(REALM, "/realmA/a..b"));

        //when
        String realm = SessionQueryFilterRealm.extract(crestQuery);

        //then
        assertThat(realm).isEqualTo("/realmA/a..b");
    }

    @Test(expectedExceptions = UnsupportedOperationException.class)
    public void shouldRejectAFilterTypeTheSessionQueryDoesNotSupport() {
        //given
        CrestQuery crestQuery = new CrestQuery(QueryFilter.or(
                QueryFilter.equalTo(REALM, "/realmA"),
                QueryFilter.equalTo(REALM, "/realmB")));

        //when
        SessionQueryFilterRealm.extract(crestQuery);
    }
}
