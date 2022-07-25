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
