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

package org.forgerock.identity.authentication.modules.adaptivedeviceprint.logic.requiredattributes;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.forgerock.identity.authentication.modules.adaptivedeviceprint.model.DevicePrint;

import com.sun.identity.shared.debug.Debug;

/**
 * This class allows to check if one or more attributes of DevicePrint is not empty.
 * It works only on String attributes.
 *
 * @mbilski
 */
public class AttributesChecker implements EssentialAttributeCheckerIface {	
	
	private static final Debug debug = Debug.getInstance(AttributesChecker.class.getName());
	
	private static final String GETTER_PREFIX = "get";
	
	private String[] attributes; 
	
	public AttributesChecker(String... attributes) {
		this.attributes = attributes;
	}
	
	/** {@inheritDoc} */
	@Override
	public boolean hasRequiredAttribute(DevicePrint dp) {
		Class<? extends DevicePrint> c = dp.getClass();
		Field[] fields = c.getDeclaredFields();
		
		try {
			for(String attr : attributes) {
				boolean hasField = false;
				
				for(Field f : fields) {
					String fieldName = f.getName();
					
					if(fieldName.equals(attr)) {
						String getterName = GETTER_PREFIX + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
						Method getterMethod = c.getMethod(getterName);
						
						Object ret = getterMethod.invoke(dp);
						
						if(f.getType().equals(String.class)) {							
							if( ret == null || ((String) ret).equals("") ) {
								debug.message("DevicePrint does not have required attribute (string): " + attr);
								return false;
							}
							
							hasField = true;
							continue;
						} else if (f.getType().equals(Double.class)) {
							if( ((Double) ret) == 0 ) {
								debug.message("DevicePrint does not have required attribute (Double): " + attr);
								return false;
							}
							
							hasField = true;
							continue;
						} else if (f.getType().equals(int.class)) {
							if( ((Integer) ret) == 0 ) {
								debug.message("DevicePrint does not have required attribute (Integer): " + attr);
								return false;
							}
							
							hasField = true;
							continue;
						}
					}
				}
				
				if(!hasField) {
					debug.error("DevicePrint does not have " + attr + " field");
					return false;
				}
			}
		} catch (Exception e) {
			debug.error("Exception during DevicePrint attributes checks");
			return false;
		}
		
		return true;
	}

}
