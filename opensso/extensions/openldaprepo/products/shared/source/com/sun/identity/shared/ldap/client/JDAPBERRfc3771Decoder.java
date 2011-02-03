/**
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
 * Portions Copyrighted 2009 Nortel
 *
 * $Id: JDAPBERRfc3771Decoder.java,v 1.1 2009/08/10 17:35:41 superpat7 Exp $
 *
 */

package com.sun.identity.shared.ldap.client;

import java.io.IOException;
import java.io.InputStream;

import com.sun.identity.shared.ldap.ber.stream.BERElement;
import com.sun.identity.shared.ldap.ber.stream.BEROctetString;
import com.sun.identity.shared.ldap.ber.stream.BERTagDecoder;

/**
 * This class decodes the IntermediateResponse message.
 * 
 * The IntermediateResponse message is defined as:
 * 
 * IntermediateResponse ::= [APPLICATION 25] SEQUENCE {
 *         responseName     [0] LDAPOID OPTIONAL
 *         responseValue    [1] OCTET STRING OPTIONAL }
 * 
 * @see RFC3771
 */
public class JDAPBERRfc3771Decoder extends BERTagDecoder {
    /**
     * Gets an element specific to an IntermediateResponse message.
     * {@inheritDoc}
     */
    public BERElement getElement(BERTagDecoder decoder, int tag,
        InputStream stream, int[] bytes_read, boolean[] implicit)
        throws IOException {
        final BERTagDecoder jdapBerTagDecoder = new JDAPBERTagDecoder(); 
        BERElement element = null;
        switch (tag) {
            // [0] LDAPOID OPTIONAL which is of BER class
            // Context-specific
            case 0x80:
            {
                element = new BEROctetString(
                      jdapBerTagDecoder, stream, bytes_read);
                implicit[0] = true;
                break;
            }
            // [1] OCTET STRING OPTIONAL which is of BER class
            // Context-specific 
            case 0x81:
            {
                element = new BEROctetString(
                      jdapBerTagDecoder, stream, bytes_read);
                implicit[0] = true;
            break;
            }
            default:
            {
                throw new IOException();
            }
        }
        return element;
    }
}
