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
 * $Id: EncryptedID.java,v 1.2 2008/06/25 05:47:41 qcheng Exp $
 *
 */


package com.sun.identity.saml2.assertion;

import java.security.Key;

import com.sun.identity.saml2.common.SAML2Exception;

/**
 * The <code>EncryptedID</code> carries the content of an unencrypted
 * identifier in encrypted fasion. It contains an <code>EncryptedData</code>
 * and zero or more <code>EncryptedKey</code>s.
 * 
 * @supported.all.api 
 */
public interface EncryptedID extends EncryptedElement {

    /**
     * Decrypts the encrypted ID.
     *
     * @param recipientPrivateKey Private key of the recipient used to
     *                            decrypt the secret key
     * @return a <code>NameID</code> that is decrypted from this object
     * @exception SAML2Exception if it could not decrypt the ID properly.
     */
    public NameID decrypt(Key recipientPrivateKey) throws SAML2Exception;

}
