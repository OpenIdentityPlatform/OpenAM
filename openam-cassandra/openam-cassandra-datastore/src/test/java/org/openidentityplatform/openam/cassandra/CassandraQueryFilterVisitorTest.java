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
 * Copyright 2022 Open Identity Platform Community.
 */

package org.openidentityplatform.openam.cassandra;

import org.forgerock.json.JsonPointer;
import org.forgerock.json.resource.QueryFilters;
import org.forgerock.util.query.QueryFilter;
import org.junit.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertTrue;

public class CassandraQueryFilterVisitorTest {

    @Test
    public void test() {
        final QueryFilter<JsonPointer> filter = QueryFilters.parse("username eq \"John\" and sn eq \"Doe\"");
        Map<String, Set<String>> map = filter.accept(new CassandraQueryFilterVisitor(), null);
        assertTrue(map.containsKey("username"));
        assertTrue(map.containsKey("sn"));
    }
}
