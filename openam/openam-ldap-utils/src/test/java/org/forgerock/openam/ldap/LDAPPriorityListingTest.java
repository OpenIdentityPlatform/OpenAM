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
 * Copyright 2013 ForgeRock Inc.
 */
package org.forgerock.openam.ldap;

import java.util.LinkedHashSet;
import java.util.Set;
import static org.fest.assertions.Assertions.*;
import static org.forgerock.openam.utils.CollectionUtils.*;
import org.testng.annotations.Test;

@Test
public class LDAPPriorityListingTest {

    public void duplicatedEntriesAreRemoved() {
        Set<LDAPURL> list = LDAPUtils.prioritizeServers(asOrderedSet("test1.com:389", "test1.com"), "01", "02");
        assertThat(list).hasSize(1).containsOnly(new LDAPURL("test1.com:389"));
    }

    public void matchingAndNonMatchingServersAreUnique() {
        Set<LDAPURL> list = LDAPUtils.prioritizeServers(asOrderedSet("test1.com:389", "test1.com|03"), "01", "02");
        assertThat(list).hasSize(1).containsOnly(new LDAPURL("test1.com:389"));
    }
    
    public void nonMatchingServerIsAtTheEndOfTheList(){
        Set<LDAPURL> list = LDAPUtils.prioritizeServers(asOrderedSet("test1.com:389|03", "test2.com|02", "test3.com|01"), "01", "02");
        assertThat(list.toArray()).isEqualTo(urls("test3.com", "test1.com", "test2.com").toArray());
    }

    public void matchingServersAreFirst() {
        Set<LDAPURL> list = LDAPUtils.prioritizeServers(asOrderedSet("test1.com:1389|03", "test3.com:2389|01", "test2.com:50389|01"), "01", "02");
        assertThat(list.toArray()).isEqualTo(urls("test3.com:2389", "test2.com:50389", "test1.com:1389").toArray());
        list = LDAPUtils.prioritizeServers(asOrderedSet("test1.com:1389|03", "test3.com:2389|02", "test2.com:50389|01"), "03", "01");
        assertThat(list.toArray()).isEqualTo(urls("test1.com:1389", "test3.com:2389", "test2.com:50389").toArray());
        list = LDAPUtils.prioritizeServers(asOrderedSet("test1.com:1389|03", "test3.com:2389|02", "test2.com:50389|01"), "02", "04");
        assertThat(list.toArray()).isEqualTo(urls("test3.com:2389", "test1.com:1389", "test2.com:50389").toArray());
        list = LDAPUtils.prioritizeServers(asOrderedSet("test1.com:1389|03", "test3.com:2389|04", "test2.com:50389|01|02"), "05", "02");
        assertThat(list.toArray()).isEqualTo(urls("test2.com:50389", "test1.com:1389", "test3.com:2389").toArray());
    }

    public void matchingSitesFirstIfNoServerMatching() {
        Set<LDAPURL> list = LDAPUtils.prioritizeServers(asOrderedSet("test1.com:1389|03", "test3.com:2389|01", "test2.com:50389|01|02"), "04", "02");
        assertThat(list.toArray()).isEqualTo(urls("test2.com:50389", "test1.com:1389", "test3.com:2389").toArray());
        list = LDAPUtils.prioritizeServers(asOrderedSet("test1.com:1389|03|02", "test2.com:50389|01|05"), "04", "02");
        assertThat(list.toArray()).isEqualTo(urls("test1.com:1389", "test2.com:50389").toArray());
        list = LDAPUtils.prioritizeServers(asOrderedSet("test1.com:1389|03|02", "test2.com:50389|01|05", "test4.com:389"), "01", "05");
        assertThat(list.toArray()).isEqualTo(urls("test2.com:50389", "test1.com:1389", "test4.com:389").toArray());
    }

    private Set<LDAPURL> urls(String... urls) {
        LinkedHashSet<LDAPURL> ret = new LinkedHashSet<LDAPURL>();
        for (String url : urls) {
            ret.add(new LDAPURL(url));
        }
        return ret;
    }
}
