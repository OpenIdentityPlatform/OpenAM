/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: FSRealmAttributePlugin.java,v 1.2 2008/06/25 05:46:55 qcheng Exp $
 *
 */

package com.sun.identity.federation.services;

import com.sun.identity.federation.message.FSSubject;
import java.util.List;

/**
 * <p>
 * The interface <code>FSRealmAttributePlugin</code> is a plugin for adding the
 * AttributeStatements into the <code>Assertion</code> by the Identity Provider
 * during the Single Sign-on process. The implementation of this plugin
 * must return list of SAML <code>AttributeStatement</code>s.
 * </p>
 * @supported.all.api
 */ 
public interface FSRealmAttributePlugin {

    /**
     * Gets the list of AttributeStatements.
     * @param realm The realm under which the entity resides
     * @param hostProviderID Hosted ProviderID.
     * @param remoteProviderID Remote ProviderID.
     * @param subject <code>FSSubject</code> to use in the statements
     * @param token session object
     * @return List A list of SAML <code>AttributeStatement<code>s.
     */
    public List getAttributeStatements(
        String realm,
        String hostProviderID,
        String remoteProviderID,
        FSSubject subject,
        Object token);
}
