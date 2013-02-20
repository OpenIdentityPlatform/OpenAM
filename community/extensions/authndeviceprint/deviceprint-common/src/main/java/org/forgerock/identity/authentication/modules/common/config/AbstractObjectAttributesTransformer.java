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
import java.util.regex.Pattern;

import com.sun.identity.shared.debug.Debug;


public abstract class AbstractObjectAttributesTransformer {
	
	protected static final String SETTER_PREFIX = "set";
	protected static final String GETTER_PREFIX = "get";
	
	/**
	 * Logger
	 */
	private final static Debug debug = Debug.getInstance(AbstractObjectAttributesTransformer.class.getName());
	
	public <T> T createObjectUsingAttributes(Class<T> targetObjectClass) {
		Field[] declaredFields = targetObjectClass.getDeclaredFields();
		T result;
		try {
			result = targetObjectClass.newInstance();
		} catch (Exception e1) {
			throw new RuntimeException(e1);
		}
		for (Field field : declaredFields) {
			String fieldName = null;
			try {
				fieldName = field.getName();
				
				//skip constans
				if(Pattern.matches("[A-Z_]+", fieldName)) {
					debug.message("Skipping constans field");
					continue;
				}
				
				String setterName = SETTER_PREFIX + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
				Method setterMethod = targetObjectClass.getMethod(setterName, field.getType());

				AttributeNameMapping attrMappingAnnotation = field.getAnnotation(AttributeNameMapping.class);

				if(attrMappingAnnotation != null) {
					String mappedAttributeName = attrMappingAnnotation.value();					
					Object readValue = getAttributeValue(mappedAttributeName);
					Object newValue = getNewValue(field, readValue);
					
					if( newValue != null ) {
						setterMethod.invoke(result, newValue);
					}
				}
			} catch (Exception e) {
				debug.error("Unable to set field in the object: " + fieldName, e);
			}
		}
		if(debug.messageEnabled()) {
			debug.message("Object created from attributes: " + result);
		}
		return result;
	}

	private Object getNewValue(Field field, Object readValue) {
		Object newValue = null;
		
		if(readValue == null) {
			newValue = null;
		} else if(readValue instanceof String) {
			newValue = getNewValueFromString(field, readValue, newValue);	
		} else {
			if(readValue instanceof Integer && field.getType().equals(Long.class)) {
				newValue = ((Integer) readValue).longValue();	
			} else {
				newValue = readValue;
			}
		}
		return newValue;
	}

	private Object getNewValueFromString(Field field, Object readValue,
			Object newValue) {
		String stringReadValue = (String)readValue;
		if(field.getType().equals(String.class)) {
			newValue = readValue;
		} else if(field.getType().equals(Long.class)) {
			newValue = Long.parseLong(stringReadValue);
		} else if(field.getType().equals(Integer.class)) {
			newValue = Integer.parseInt(stringReadValue);
		} else if(field.getType().equals(Boolean.class)) {
			newValue = Boolean.parseBoolean(stringReadValue);
		}
		return newValue;
	}
	
	protected abstract Object getAttributeValue(String mappedAttributeName);
}
