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
 * $Id: SecurityAttributePlugin.java,v 1.2 2008/06/25 05:47:21 qcheng Exp $
 *
 */


package com.sun.identity.liberty.ws.security;

import com.sun.identity.saml.assertion.NameIdentifier;

import java.util.List;
/** 
 * This interface <code>SecurityAttributePlugin</code> is used to
 * insert security attributes via the <code>AttributeStatement</code> into
 * the <code>SecurityAssertion</code> during the discovery  service 
 * credential generation.
 *
 * @supported.all.api
 */
public interface SecurityAttributePlugin {

    /**
     * Returns the list of SAML <code>Attribute</code> objects.
     * @param nameID <code>NameIdentifier</code> of the subject in the
     *        <code>Assertion</code>.
     * @param resourceID <code>ResourceID</code> or the 
     *        <code>EncryptedResourceID</code> object of the entry or the user
     *        that is being accessed by the web services client.
     * @param providerID Discovery service <code>ProviderID</code>.
     * @return list of <code>Attribute</code>s.
     */
    public List getAttributes(
        NameIdentifier nameID, Object resourceID, String providerID);
}
