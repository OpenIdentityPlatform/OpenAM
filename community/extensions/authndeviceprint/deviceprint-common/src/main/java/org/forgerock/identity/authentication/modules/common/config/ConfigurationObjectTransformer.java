/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * https://opensso.dev.java.net/public/CDDLv1.0.html or
 * opensso/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at opensso/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 */

package org.forgerock.identity.authentication.modules.common.config;

import java.util.Map;

import com.sun.identity.shared.datastruct.CollectionHelper;

public class ConfigurationObjectTransformer extends AbstractObjectAttributesTransformer{

	/**
	 * 
	 */
	protected Map<Object, String> options;
	
	/**
     * Get configuration option
     * @param optionName
     * @return
     */
	@Override
	protected String getAttributeValue(String mappedAttributeName) {
		return CollectionHelper.getMapAttr(options,mappedAttributeName);
	}

	public ConfigurationObjectTransformer(Map<Object, String> options) {
		super();
		this.options = options;
	}

	public ConfigurationObjectTransformer() {
		super();
	}

	public Map<Object, String> getOptions() {
		return options;
	}

	public void setOptions(Map<Object, String> options) {
		this.options = options;
	}
	
	
}
