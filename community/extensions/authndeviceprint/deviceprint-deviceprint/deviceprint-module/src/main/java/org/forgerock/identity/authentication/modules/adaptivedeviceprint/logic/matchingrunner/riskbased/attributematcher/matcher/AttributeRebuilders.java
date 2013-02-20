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

package org.forgerock.identity.authentication.modules.adaptivedeviceprint.logic.matchingrunner.riskbased.attributematcher.matcher;

/**
 * Predefined Rebuilders
 * @author jdabrowski
 *
 */
public final class AttributeRebuilders {
	
	private AttributeRebuilders() {
		super();
	}

	/**
	 * Rebuilds string replacing every digit, dot and plus
	 */
	public static AttributeRebuilder IGNORE_VERSION_CHARS = new AttributeRebuilder() {
		@Override
		public String apply(String attribute) {
			return attribute.replaceAll("[0-9\\.\\+]", "").trim();
		}
	};
	
}
