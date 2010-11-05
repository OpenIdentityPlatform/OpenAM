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
 * $Id: JDAPIntermediateResponse.java,v 1.1 2009/08/10 17:35:38 superpat7 Exp $
 *
 */

package com.sun.identity.shared.ldap.client.opers;

import java.io.IOException;

import com.sun.identity.shared.ldap.ber.stream.BERElement;
import com.sun.identity.shared.ldap.ber.stream.BEROctetString;
import com.sun.identity.shared.ldap.ber.stream.BERSequence;
import com.sun.identity.shared.ldap.ber.stream.BERTag;

/**
 * This class implements the intermediate response.
 * This object is sent from the LDAP server to the interface.
 * <pre>
 * IntermediateResponse :: [APPLICATION 25] SEQUENCE {
 *         responseName    [0] LDAPOID OPTIONAL,
 *         response        [1] OCTET STRING OPTIONAL
 * }
 * </pre>
 *
 * @see RFC3771
 */
public class JDAPIntermediateResponse implements JDAPProtocolOp {

    /**
     * Internal variables
     */
    protected String m_oid = null;
    protected byte[] m_value = null;
    protected BERElement m_element = null;

    /**
     * Constructs a JDAPIntermediateResponse object.
     * @param element BER element of the response
     * @throws IOException if the object cannot be constructed
     */
    public JDAPIntermediateResponse(BERElement element) throws IOException {
        this.m_element = ((BERTag)element).getValue();
        BERSequence sequence = (BERSequence)this.m_element;
        for (int i = 0; i < sequence.size(); i++) {
            BERElement subElement = sequence.elementAt(i);
            if (subElement.getType() != BERElement.TAG) {
                throw new IOException("The intermediate response " +
                      "message contains an illegal element.");
            }
            BERTag tag = (BERTag)subElement;
            switch (tag.getTag()) {
                // [0] LDAPOID OPTIONAL which is of BER class
                // Context-specific
                case 0x80:
                {
                    BEROctetString oid = (BEROctetString)tag.getValue();
                    this.m_oid = new String(oid.getValue(), "UTF8");
                    break;
                }
                // [1] OCTET STRING OPTIONAL which is of BER class
                // Context-specific 
                case 0x81:
                {
                    BEROctetString value = (BEROctetString)tag.getValue();
                    this.m_value = value.getValue();
                    break;
                }
            }
        }
    }

    /**
     * Retrieves the BER representation of the result.
     * @return BER representation of the result
     */
    public BERElement getBERElement()
    {
       return this.m_element;
    }
    
    /**
     * Retrieves the protocol operation type.
     * @return protocol type
     */
    public int getType() {
        return JDAPProtocolOp.INTERMEDIATE_RESPONSE;
    }

    /**
     * Retrieves the results of the intermediate operation.
     * @return intermediate operation results as byte array
     */
    public byte[] getValue() {
        return this.m_value;
    }

    /**
     * {@inheritDoc}
     */
    public String getID() {
        return this.m_oid;
    }
}
