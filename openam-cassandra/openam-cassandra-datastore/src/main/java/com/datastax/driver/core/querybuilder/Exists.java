package com.datastax.driver.core.querybuilder;

import java.util.List;

import com.datastax.driver.core.CodecRegistry;
import com.datastax.driver.core.querybuilder.Clause;

public class Exists extends Clause {
	@Override
	String name() {
		return "EXISTS";
	}
	
	@Override
	Object firstValue() {
		return "EXISTS";
	}

	@Override
	boolean containsBindMarker() {
		return false;
	}

	@Override
	void appendTo(StringBuilder sb, List<Object> values, CodecRegistry codecRegistry) {
		sb.append("EXISTS");
	}

}
