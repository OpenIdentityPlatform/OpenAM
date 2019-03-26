package ru.org.openam.cassandra;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.forgerock.guice.core.InjectorHolder;
import com.datastax.driver.core.querybuilder.Clause;
import com.datastax.driver.core.querybuilder.QueryBuilder;

public class Filter {
	final DataLayerConfiguration dataLayerConfiguration;

	@Inject
	public Filter(DataLayerConfiguration dataLayerConfiguration) {
		this.dataLayerConfiguration = dataLayerConfiguration;
	}

	
	@Override
	public String toString() {
		return "Filter [clauses=" + clauses + "]";
	}

	public List<Clause> clauses=new ArrayList<Clause>();
	public Map<String,Object> field2value=new TreeMap<String,Object>(String.CASE_INSENSITIVE_ORDER);

	public String getTable() {
		if (field2value.size()==1) {
			final String field=field2value.keySet().iterator().next();
			if (!StringUtils.equalsIgnoreCase("coretokenid",field) && (
					StringUtils.equalsIgnoreCase("coreTokenString03",field)
					||StringUtils.equalsIgnoreCase("coreTokenMultiString01",field)
					||StringUtils.equalsIgnoreCase("coreTokenUserId",field)
					))
				return field;
		}
		return dataLayerConfiguration.getTableName();
	}
	
	public static Filter and(List<Filter> filters) {
		final Filter res=InjectorHolder.getInstance(Filter.class);
		for (Filter filter : filters) {
			res.clauses.addAll(filter.clauses);
			res.field2value.putAll(filter.field2value);
		}
		return res;
	}
	
	public static Filter equality(String name, Object value) {
		final Filter res=InjectorHolder.getInstance(Filter.class);
		res.field2value.put(name, value);
		res.clauses.add(QueryBuilder.eq(name, value));
		return res;
	}
	
	public static Filter greaterThan(String name, Object value) {
		final Filter res=InjectorHolder.getInstance(Filter.class);
		res.field2value.put(name, value);
		res.clauses.add(QueryBuilder.gt(name, value));
		return res;
	}
	
	public static Filter greaterOrEqual(String name, Object value) {
		final Filter res=InjectorHolder.getInstance(Filter.class);
		res.field2value.put(name, value);
		res.clauses.add(QueryBuilder.gte(name, value));
		return res;
	}
	
	public static Filter lessThan(String name, Object value) {
		final Filter res=InjectorHolder.getInstance(Filter.class);
		res.field2value.put(name, value);
		res.clauses.add(QueryBuilder.lt(name, value));
		return res;
	}
	
	public static Filter lessOrEqual(String name, Object value) {
		final Filter res=InjectorHolder.getInstance(Filter.class);
		res.field2value.put(name, value);
		res.clauses.add(QueryBuilder.lte(name, value));
		return res;
	}
}
