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
/*
 * Portions Copyrighted 2013 Syntegrity.
 * Portions Copyrighted 2013 ForgeRock Inc.
 */

package org.forgerock.openam.authentication.modules.deviceprint;

import java.util.Map;
import java.util.Set;

import com.iplanet.sso.SSOException;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;

/**
 * A wrapper class around AMIdentity to facility testing of code that needs to use AMIdentity.
 */
public class AMIdentityWrapper {

	private final AMIdentity amIdentity;

    /**
     * Constructs an instance of the AMIdentityWrapper.
     *
     * @param amIdentity An instance of AMIdentity.
     */
	public AMIdentityWrapper(AMIdentity amIdentity) {
        this.amIdentity = amIdentity;
    };

    /**
     * Delegates to the store method on the AMIdentity instance.
     */
	public void store() throws SSOException, IdRepoException {
		amIdentity.store();
	}

    /**
     * Delegates to the setAttributes method on the AMIdentity instance.
     */
	public void setAttributes(Map attrMap) throws SSOException, IdRepoException {
		amIdentity.setAttributes(attrMap);
	}

    /**
     * Delegates to the getAttribute method on the AMIdentity instance.
     */
	public Set<?> getAttribute(String attr) throws SSOException, IdRepoException {
		return amIdentity.getAttribute(attr);
	}
}
