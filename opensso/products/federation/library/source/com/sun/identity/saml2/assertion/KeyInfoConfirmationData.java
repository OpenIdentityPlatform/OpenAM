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
 * $Id: KeyInfoConfirmationData.java,v 1.2 2008/06/25 05:47:41 qcheng Exp $
 *
 */


package com.sun.identity.saml2.assertion;

import java.util.List;
import com.sun.identity.saml2.common.SAML2Exception;

/**
 * The <code>KeyInfoConfirmationData</code> constrains a 
 * <code>SubjectConfirmationData</code> element to contain one or more
 * <code>ds:KeyInfo</code> elements that identify cryptographic keys that are
 * used in some way to authenticate an attesting entity. The particular 
 * confirmation method MUST define the exact mechanism by which the
 * confirmation data can be used. The optional attributes defined by
 * <code>SubjectConfirmationData</code> MAY also appear.
 * @supported.all.api 
 */
public interface KeyInfoConfirmationData extends SubjectConfirmationData {
    
    /**
     * Returns the key info
     *
     * @return the key info
     */
    public List getKeyInfo();

    /**
     * Sets the key info
     *
     * @param info the key info
     */
    public void setKeyInfo(List info) throws SAML2Exception;

}
