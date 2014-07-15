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
 * Copyright 2014 ForgeRock AS.
 */
package org.forgerock.openam.cts.api.filter;

import junit.framework.TestCase;
import org.forgerock.openam.cts.api.fields.CoreTokenField;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.fest.assertions.Assertions.assertThat;

public class TokenFilterTest extends TestCase {

    private TokenFilter filter;

    @BeforeMethod
    public void setup() {
        filter = new TokenFilter();
    }

    @Test
    public void shouldBeAndByDefault() {
        assertThat(filter.getType()).isEqualTo(TokenFilter.Type.AND);
    }

    @Test (expectedExceptions =  UnsupportedOperationException.class)
    public void shouldPreventModificationToFilters() {
        filter.getFilters().put(CoreTokenField.BLOB, "badger");
    }

    @Test (expectedExceptions = NullPointerException.class)
    public void shouldPreventNullType() {
        filter.setType(null);
    }

    @Test (expectedExceptions = NullPointerException.class)
    public void shouldPreventNullFieldOnAdd() {
        filter.addFilter(null, "");
    }

    @Test (expectedExceptions = NullPointerException.class)
    public void shouldPreventNullValueOnAdd() {
        filter.addFilter(CoreTokenField.BLOB, null);
    }

    @Test (expectedExceptions = UnsupportedOperationException.class)
    public void shouldPreventModificationToReturnAttributes() {
        filter.getReturnFields().clear();
    }

    @Test (expectedExceptions = NullPointerException.class)
    public void shouldPreventNullReturnAttribute() {
        filter.addReturnAttribute(null);
    }
}