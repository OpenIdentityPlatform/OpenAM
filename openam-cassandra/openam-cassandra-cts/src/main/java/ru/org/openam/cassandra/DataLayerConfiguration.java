package ru.org.openam.cassandra;

import javax.inject.Inject;
import javax.inject.Named;

import org.forgerock.openam.cts.impl.CTSDataLayerConfiguration;
import org.forgerock.openam.sm.datalayer.api.DataLayerConstants;

import com.iplanet.am.util.SystemProperties;

public class DataLayerConfiguration extends CTSDataLayerConfiguration {
	@Inject
	public DataLayerConfiguration(@Named(DataLayerConstants.ROOT_DN_SUFFIX) String rootDnSuffix) {
		super(rootDnSuffix);
	}
	
	public String getTable(){
	    return SystemProperties.get(getCustomTokenRootSuffixProperty());
	}
	
	public String getKeySpace(){
	    return getTable().split("\\.")[0];
	}
	
	public String getTableName(){
	    return getTable().split("\\.")[1];
	}
}
