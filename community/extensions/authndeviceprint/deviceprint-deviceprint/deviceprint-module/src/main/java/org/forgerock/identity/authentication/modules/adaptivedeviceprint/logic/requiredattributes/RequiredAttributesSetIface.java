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

import org.forgerock.identity.authentication.modules.adaptivedeviceprint.model.DevicePrint;

/**
 * A set of EssentialAttributeCheckerIfaces to check if DevicePrint has required attributes.
 * 
 * @author mbilski
 * 
 */
public interface RequiredAttributesSetIface {
	
	/**
	 * Creates EssentialAttributeCheckerIfaces based on RequiredAttributesConfig
	 */
	void init(RequiredAttributesConfig config);
	
	/** Add checker to the list */
	void addChecker(EssentialAttributeCheckerIface checker);
	
	/**
	 * It returns true if all EssentialAttributeCheckerIfaces passes.
	 */
	boolean hasRequiredAttributes(DevicePrint dp);
	
}
