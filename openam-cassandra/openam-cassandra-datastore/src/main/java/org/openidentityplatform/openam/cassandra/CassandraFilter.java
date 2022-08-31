package org.openidentityplatform.openam.cassandra;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class CassandraFilter {
    private final Map<String, Set<String>> filter = new HashMap<>();
    private Integer filterOp = Repo.AND_MOD;

    public Map<String, Set<String>> getFilter() {
        return filter;
    }

    public int getFilterOp() {
        return filterOp;
    }

    public void setFilterOp(int filterOp) {
        this.filterOp = filterOp;
    }
}
