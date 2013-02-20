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

package org.forgerock.identity.authentication.modules.adaptivedeviceprint.logic;

import java.util.Map;
import java.util.Set;

import com.iplanet.sso.SSOException;
import com.sun.identity.idm.IdRepoException;

/**
 * AMIdentity is final class and cannot be mocked. This wrapper resolves this issue.
 * @author mbilski
 *
 */
public interface AMIdentityWrapperIface {

	/**
	 * @see com.sun.identity.idm.AMIdentity#store()
	 */
	void store() throws SSOException, IdRepoException;
	
	/**
	 * @see com.sun.identity.idm.AMIdentity#setAttributes(Map)
	 */
	@SuppressWarnings("rawtypes")
	void setAttributes(Map attrMap) throws SSOException, IdRepoException;
	
	/**
	 * @see com.sun.identity.idm.AMIdentity#getAttribute(String)
	 */
	@SuppressWarnings("rawtypes")
	Set getAttribute(String attr) throws SSOException, IdRepoException;

}
