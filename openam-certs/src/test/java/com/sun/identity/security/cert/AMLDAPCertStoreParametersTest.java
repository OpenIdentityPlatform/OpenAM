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

package com.sun.identity.security.cert;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import org.testng.annotations.Test;

public class AMLDAPCertStoreParametersTest {

    @Test
    public void aCopyCarriesEveryField() throws Exception {
        // The CRL store works on a copy, so a field the copy drops is a setting the store quietly
        // ignores — secureLdap, say, which would move the CRL directory connection and its bind
        // password onto plain LDAP. Walking the declared fields makes a field added later fail
        // here until it is both copied and set below.
        AMLDAPCertStoreParameters original = new AMLDAPCertStoreParameters("ldap.example.com", 1636);
        original.setUser("cn=admin");
        original.setPassword("secret");
        original.setSearchFilter("(cn=TestCA)");
        original.setStartLoc("dc=example,dc=com");
        original.setSecure(true);
        original.setURIParams("user=svc");
        original.setDoCRLCaching(false);
        original.setDoCRLUpdate(false);
        AMLDAPCertStoreParameters defaults = new AMLDAPCertStoreParameters(null, 0);

        AMLDAPCertStoreParameters copy = new AMLDAPCertStoreParameters(original);

        for (Field field : AMLDAPCertStoreParameters.class.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            field.setAccessible(true);
            assertThat(field.get(original))
                    .as("%s must be set away from its default, or this proves nothing about it",
                            field.getName())
                    .isNotEqualTo(field.get(defaults));
            assertThat(field.get(copy)).as(field.getName()).isEqualTo(field.get(original));
        }
    }
}
