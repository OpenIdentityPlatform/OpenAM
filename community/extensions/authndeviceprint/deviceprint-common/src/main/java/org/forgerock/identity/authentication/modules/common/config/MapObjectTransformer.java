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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import com.sun.identity.shared.debug.Debug;

public class MapObjectTransformer extends AbstractObjectAttributesTransformer{

	public Map<String, Object> inputMap;
	
	/**
	 * Logger
	 */
	private final static Debug debug = Debug.getInstance(MapObjectTransformer.class.getName());
	
	public MapObjectTransformer(Map<String, Object> inputMap) {
		super();
		this.inputMap = inputMap;
	}

	public MapObjectTransformer() {
		super();
	}

	@Override
	protected Object getAttributeValue(String mappedAttributeName) {
		return inputMap.get(mappedAttributeName);
	}
	
	public static Map<String, Object> convertObjectToMap(Object inObject) {
		Class<? extends Object> inObjectClass = inObject.getClass();
		Field[] declaredFields = inObjectClass.getDeclaredFields();
		HashMap<String, Object> result = new HashMap<String, Object>();
		
		for (Field field : declaredFields) {
			String fieldName = null;
			try {
				fieldName = field.getName();
				String getterName = GETTER_PREFIX + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
				Method getterMethod = inObjectClass.getMethod(getterName);

				AttributeNameMapping attrMappingAnnotation = field.getAnnotation(AttributeNameMapping.class);

				if(attrMappingAnnotation != null) {
					String mappedAttributeName = attrMappingAnnotation.value();					
					result.put(mappedAttributeName, getterMethod.invoke(inObject));
				}
			} catch (Exception e) {
				debug.error("Unable to get field value: " + fieldName, e);
			}
		}
		if(debug.messageEnabled()) {
			debug.message("Configuration object created:" + result);
		}
		return result;
	}

	public Map<String, Object> getInputMap() {
		return inputMap;
	}

	public void setInputMap(Map<String, Object> inputMap) {
		this.inputMap = inputMap;
	}
	
}
