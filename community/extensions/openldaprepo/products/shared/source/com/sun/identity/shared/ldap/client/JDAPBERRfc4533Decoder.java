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
 * $Id: JDAPBERRfc4533Decoder.java,v 1.1 2009/08/10 17:35:41 superpat7 Exp $
 *
 */

package com.sun.identity.shared.ldap.client;

import java.io.IOException;
import java.io.InputStream;

import com.sun.identity.shared.ldap.ber.stream.BERElement;
import com.sun.identity.shared.ldap.ber.stream.BEROctetString;
import com.sun.identity.shared.ldap.ber.stream.BERSequence;
import com.sun.identity.shared.ldap.ber.stream.BERTagDecoder;

/**
 * Decodes a Sync Info Message.
 * The Sync Info Message is described in RFC 4533 (LDAP Content
 * Synchronization Operation). This message is received from the LDAP server
 * when the initial synchronization is complete and moves to the persist
 * state.
 * <p>
 * The value of the Sync Info Message contains a BER-encoded syncInfoValue as
 * follows:
 * <p>
 * <pre>
 * syncInfoValue ::= CHOICE {
 *     newcookie      [0] syncCookie,
 *     refreshDelete  [1] SEQUENCE {
 *         cookie         syncCookie OPTIONAL,
 *         refreshDone    BOOLEAN DEFAULT TRUE
 *     },
 *     refreshPresent [2] SEQUENCE {
 *         cookie         syncCookie OPTIONAL,
 *         refreshDone    BOOLEAN DEFAULT TRUE
 *     },
 *     syncIdSet      [3] SEQUENCE {
 *         cookie         syncCookie OPTIONAL,
 *         refreshDeletes BOOLEAN DEFAULT FALSE,
 *         syncUUIDs      SET OF syncUUID
 *     }
 * }
 * 
 * where the contents are defined as follows:
 * 
 * cookie - typically consists of the replica id number and additional
 * information that represents an existing content synchronization request
 * along with the content of the LDAP server at a particular time 
 *
 * refreshDone - indicates that the initial synchronization phase is complete
 * and is now moving to the persist phase
 *
 * refreshDeletes - used to indicate how the LDAP server is to address the
 * situation where content synchronization cannot be achieved - to reload the
 * entire content or send an e-syncRefreshRequired result code
 * </pre>
 * 
 * @see RFC4533
 */
public class JDAPBERRfc4533Decoder extends BERTagDecoder {
    /**
     * Gets an element specific to a SyncInfo message.
     * {@inheritDoc}
     */
    public BERElement getElement(BERTagDecoder decoder, int tag,
        InputStream stream, int[] bytes_read, boolean[] implicit)
        throws IOException {
        final BERTagDecoder jdapBerTagDecoder = new JDAPBERTagDecoder(); 
        BERElement element = null;
        switch (tag) {
            // CHOICE is: newcookie [0] syncCookie
            case 0xa0:
            {
               element = new BEROctetString(
                     jdapBerTagDecoder, stream, bytes_read);
               implicit[0] = true;
               break;
            }
            // CHOICE is: refreshDelete [1] SEQUENCE ...
            case 0xa1:
            // CHOICE is: refreshPresent [2] SEQUENCE ...
            case 0xa2:
            // CHOICE is: syncIdSet [3] SEQUENCE ...
            case 0xa3:
            {
               element = new BERSequence(
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
