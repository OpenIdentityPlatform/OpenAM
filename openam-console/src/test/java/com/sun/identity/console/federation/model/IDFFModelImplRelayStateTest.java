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

package com.sun.identity.console.federation.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.util.Map;

import org.testng.annotations.Test;

/**
 * {@code IDFFModelImpl.updateAttrInConfig} clears the stored attributes and rebuilds them only from
 * the extended-meta maps. Unless {@code relayStateUrlList} is seeded into those maps the console
 * silently drops a configured allow-list on save, so this pins the key into both maps (mirroring
 * {@code SAMLv2ModelImpl}). Reflection is used because {@code IDFFModelImpl} needs a live SSO
 * session to instantiate; the accessors merely merge these static maps.
 */
public class IDFFModelImplRelayStateTest {

    private static final String RELAY_STATE_URL_LIST = "relayStateUrlList";

    @Test
    public void idpExtendedMetaMapKeepsRelayStateUrlList() throws Exception {
        assertThat(staticMap("extendedMetaIdpMap")).containsKey(RELAY_STATE_URL_LIST);
    }

    @Test
    public void spExtendedMetaMapKeepsRelayStateUrlList() throws Exception {
        assertThat(staticMap("extendedMetaSpMap")).containsKey(RELAY_STATE_URL_LIST);
    }

    @SuppressWarnings("unchecked")
    private static Map<Object, Object> staticMap(String fieldName) throws Exception {
        Field field = IDFFModelImpl.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return (Map<Object, Object>) field.get(null);
    }
}
