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
 */

package com.sun.identity.authentication.spi;

/**
 * This exception throws form Authentication Module and process by {@link com.sun.identity.authentication.jaas.LoginContext#invoke()}
 *   if there's a need to go back in authentcation chain on 
 * @author maximthomas
 *
 */

public class SetNextModuleException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4512323171050725535L;
	
	private int moduleIndex;
	
	public SetNextModuleException(int moduleIndex) {
		this.moduleIndex = moduleIndex;
	}
	
	public int getModuleIndex() {
		return moduleIndex;
	}

}
