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
 * Copyright 2019 Open Identity Platform Community.
 * Portions copyright 2025 3A Systems LLC.
 */

package org.openidentityplatform.openam.cassandra;

import jakarta.inject.Inject;
import jakarta.inject.Named;

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
