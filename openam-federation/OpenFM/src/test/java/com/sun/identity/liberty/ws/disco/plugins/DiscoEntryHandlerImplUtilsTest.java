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
package com.sun.identity.liberty.ws.disco.plugins;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sun.identity.liberty.ws.disco.jaxb.ResourceOfferingType;
import com.sun.identity.liberty.ws.disco.plugins.jaxb.DiscoEntryElement;
import com.sun.identity.plugin.datastore.DataStoreProvider;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.mockito.ArgumentCaptor;
import org.testng.annotations.Test;

/**
 * The read path drops every stored entry it cannot unmarshal and asks the caller to store the rest,
 * so an unmarshalling regression would silently delete a user's discovery entries.
 */
@Test
public class DiscoEntryHandlerImplUtilsTest {

    private static final String USER_ID = "id=demo,ou=user,dc=openam,dc=example,dc=org";
    private static final String ATTR_NAME = "sunIdentityServerDiscoEntries";
    private static final String ENDPOINT = "http://openam.example.org:8080/openam/Liberty/disco";

    /** The fmDisco.xml bootstrapping DiscoEntry default with its server placeholders substituted. */
    private static final String BOOTSTRAP_ENTRY = discoEntry("");
    private static final String ENTRY_WITH_ID = discoEntry(" entryID=\"entry-1\"");

    private static String discoEntry(String entryIdAttribute) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<DiscoEntry xmlns=\"urn:com:sun:identityserver:liberty:ws:disco:discoentry\">"
                + "<ResourceOffering xmlns=\"urn:liberty:disco:2003-08\"" + entryIdAttribute + ">"
                + "<ResourceID></ResourceID>"
                + "<ServiceInstance><ServiceType>urn:liberty:disco:2003-08</ServiceType>"
                + "<ProviderID>" + ENDPOINT + "</ProviderID>"
                + "<Description><SecurityMechID>urn:liberty:security:2003-08:null:null</SecurityMechID>"
                + "<Endpoint>" + ENDPOINT + "</Endpoint></Description>"
                + "</ServiceInstance></ResourceOffering></DiscoEntry>";
    }

    public void storedEntryWithIdIsReadBackWithoutStoring() throws Exception {
        Map<String, DiscoEntryElement> entries = new HashMap<>();

        boolean needStore = DiscoEntryHandlerImplUtils.getUserDiscoEntries(
                storeReturning(ENTRY_WITH_ID), USER_ID, ATTR_NAME, entries);

        assertThat(needStore).isFalse();
        assertThat(entries).containsOnlyKeys("entry-1");
        ResourceOfferingType offering = entries.get("entry-1").getValue().getResourceOffering().getValue();
        assertThat(offering.getServiceInstance().getServiceType()).isEqualTo("urn:liberty:disco:2003-08");
        assertThat(offering.getServiceInstance().getProviderID()).isEqualTo(ENDPOINT);
    }

    public void bootstrapEntryWithoutIdGetsGeneratedIdAndIsStored() throws Exception {
        Map<String, DiscoEntryElement> entries = new HashMap<>();

        boolean needStore = DiscoEntryHandlerImplUtils.getUserDiscoEntries(
                storeReturning(BOOTSTRAP_ENTRY), USER_ID, ATTR_NAME, entries);

        assertThat(needStore).isTrue();
        assertThat(entries).hasSize(1);
        Map.Entry<String, DiscoEntryElement> entry = entries.entrySet().iterator().next();
        assertThat(entry.getKey()).isNotEmpty();
        assertThat(entry.getValue().getValue().getResourceOffering().getValue().getEntryID())
                .isEqualTo(entry.getKey());
    }

    public void malformedEntryIsDroppedAndOthersAreKept() throws Exception {
        Map<String, DiscoEntryElement> entries = new HashMap<>();

        boolean needStore = DiscoEntryHandlerImplUtils.getUserDiscoEntries(
                storeReturning("<DiscoEntry", ENTRY_WITH_ID), USER_ID, ATTR_NAME, entries);

        assertThat(needStore).isTrue();
        assertThat(entries).containsOnlyKeys("entry-1");
    }

    @SuppressWarnings("unchecked")
    public void writtenEntriesAreReadBack() throws Exception {
        Map<String, DiscoEntryElement> entries = new HashMap<>();
        DiscoEntryHandlerImplUtils.getUserDiscoEntries(
                storeReturning(ENTRY_WITH_ID), USER_ID, ATTR_NAME, entries);
        DataStoreProvider writeStore = mock(DataStoreProvider.class);

        assertThat(DiscoEntryHandlerImplUtils.setUserDiscoEntries(
                writeStore, USER_ID, ATTR_NAME, entries.values())).isTrue();

        ArgumentCaptor<Map> written = ArgumentCaptor.forClass(Map.class);
        verify(writeStore).setAttributes(eq(USER_ID), written.capture());
        Set<String> stored = (Set<String>) written.getValue().get(ATTR_NAME);
        Map<String, DiscoEntryElement> readBack = new HashMap<>();
        boolean needStore = DiscoEntryHandlerImplUtils.getUserDiscoEntries(
                storeReturning(stored.toArray(new String[0])), USER_ID, ATTR_NAME, readBack);
        assertThat(needStore).isFalse();
        assertThat(readBack).containsOnlyKeys("entry-1");
        assertThat(readBack.get("entry-1").getValue().getResourceOffering().getValue()
                .getServiceInstance().getProviderID()).isEqualTo(ENDPOINT);
    }

    private static DataStoreProvider storeReturning(String... values) throws Exception {
        DataStoreProvider store = mock(DataStoreProvider.class);
        Set<String> attribute = new HashSet<>(Arrays.asList(values));
        when(store.getAttribute(USER_ID, ATTR_NAME)).thenReturn(attribute);
        return store;
    }
}
