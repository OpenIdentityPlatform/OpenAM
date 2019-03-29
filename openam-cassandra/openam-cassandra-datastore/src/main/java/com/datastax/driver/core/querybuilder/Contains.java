package com.datastax.driver.core.querybuilder;

import com.datastax.driver.core.querybuilder.Clause.SimpleClause;

public class Contains extends SimpleClause {
	public Contains(String name, Object value) {
		super(name, " CONTAINS ", value);
	}
}
