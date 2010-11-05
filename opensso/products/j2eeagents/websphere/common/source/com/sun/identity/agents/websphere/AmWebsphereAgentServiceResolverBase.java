/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: AmWebsphereAgentServiceResolverBase.java,v 1.3 2009/02/07 01:31:57 leiming Exp $
 *
 */

package com.sun.identity.agents.websphere;

import com.sun.identity.agents.arch.ServiceFactory;
import com.sun.identity.agents.arch.ServiceResolver;
import com.sun.identity.agents.filter.GenericJ2EELogoutHandler;
import com.sun.identity.agents.filter.J2EEAuthenticationHandler;
import com.sun.identity.agents.realm.GenericExternalVerificationHandler;

/**
 * Abstract service resolver class for Websphere application 
 * server/portal.
 */
public abstract class AmWebsphereAgentServiceResolverBase 
extends ServiceResolver {
	
	public String[] getModuleList() {
		String[] coreModules = super.getModuleList();
		int count = coreModules.length;
		String[] result = new String[count + 1];
		System.arraycopy(coreModules, 0, result, 0, count);
		result[count] = AmWebsphereModule.class.getName();
		
		return result;
        }

	public String getGlobalJ2EEAuthHandlerImpl() {
		return J2EEAuthenticationHandler.class.getName();
	}

	public String getGlobalJ2EELogoutHandlerImpl() {
		return GenericJ2EELogoutHandler.class.getName();
	}

	public String getGlobalVerificationHandlerImpl() {
		return GenericExternalVerificationHandler.class.getName();
	}
	
	public abstract String getAmIdentityAsserterImpl();
	
	public String getAuditResultHandlerImpl() {
                return AmWebsphereTIAAuditResultHandler.class.getName();
        } 
	
	public String getNotificationTaskHandlerImpl() {
                return AmWebsphereTAINotificationTaskHandler.class.getName();
        }
	
	public String getAmRealmUserRegistryImpl() {
		return AmRealmUserRegistry.class.getName();
	}
}
