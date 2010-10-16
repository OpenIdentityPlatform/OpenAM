/* -*- Mode: C++; tab-width: 4; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * The contents of this file are subject to the Netscape Public
 * License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.mozilla.org/NPL/
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * The Original Code is mozilla.org code.
 *
 * The Initial Developer of the Original Code is Netscape
 * Communications Corporation.  Portions created by Netscape are
 * Copyright (C) 1999 Netscape Communications Corporation. All
 * Rights Reserved.
 *
 * Contributor(s): 
 */
package com.sun.identity.shared.ldap;

import com.sun.identity.shared.ldap.client.opers.JDAPExtendedResponse;
import java.util.ArrayList;
import com.sun.identity.shared.ldap.ber.stream.BERElement;

/**
 * Represents a server response to an extended operation request.
 * 
 * @version 1.0
 */
public class LDAPExtendedResponse extends LDAPResponse
                                  implements java.io.Serializable {

    static final long serialVersionUID = -3813049515964705320L;

    protected String m_oid = null;

    protected byte[] m_value = null;

    /**
     * Constructor
     * 
     * @param msgid message identifier
     * @param rsp extended operation response
     * @paarm controls array of controls or null
     */
    LDAPExtendedResponse(int msgid, JDAPExtendedResponse rsp, LDAPControl controls[]) {
        super(msgid, rsp, controls);
    }

    LDAPExtendedResponse(int msgid, int operType, int contentLength,
        byte[] content) {
        super(msgid, operType, contentLength, content);
    }

    public boolean equals(Object obj) {
        LDAPExtendedResponse msg = null;
        if (obj instanceof LDAPExtendedResponse) {
            msg = (LDAPExtendedResponse) obj;
        } else {
            return false;
        }
        boolean e = super.equals(msg);
        if (!e) {
            return false;
        }
        String thisID = getID();
        String otherID = msg.getID();
        if (thisID != null) {
            if (otherID != null) {
                if (!thisID.equals(otherID)) {
                    return false;
                } 
            } else {
                return false;
            }
        } else {
            if (otherID != null) {
                return false;
            }
        }
        byte[] thisValue = getValue();
        byte[] otherValue = msg.getValue();
        if (thisValue != null) {
            if (otherValue != null) {
                if (thisValue.length != otherValue.length) {
                    return false;
                } else {
                    for (int i = 0; i < thisValue.length; i++) {
                        if (thisValue[i] != otherValue[i]) {
                            return false;
                        }
                    }
                }
            } else {
                return false;
            }
        } else {
            if (otherValue != null) {
                return false;
            }
        }
        return true;
    }

    protected synchronized void parseComponent(final short index) {
        if ((index == OPTIONAL) && (((content[offset[0]] & 0xff) == 0x8a) ||
            ((content[offset[0]] & 0xff) == 0x8b))) {
            int[] bytesProcessed = new int[1];
            bytesProcessed[0] = 0;
            switch (content[offset[0]] & 0xff) {
                case 0x8a: /* Context Specific [10]:
                            * Handle extended response
                            */
                    offset[0]++;
                    bytesProcessed[0]++;
                    m_oid = LDAPParameterParser.parseOctetString(
                        content, offset, bytesProcessed);
                    break;
                case 0x8b: /* Context Specific [11]:
                            * Handle extended response
                            */
                    offset[0]++;
                    bytesProcessed[0]++;
                    m_value = LDAPParameterParser.parseOctetBytes(
                        content, offset, bytesProcessed);
            }
            messageBytesProcessed += bytesProcessed[0];
            if ((messageBytesProcessed >= messageContentLength) &&
                (messageContentLength != -1)) {
                offsetIndex = END;
            } else {
                if (offset[0] < content.length) {
                    if (((int) content[offset[0]]) == BERElement.EOC) {
                        offset[0] += 2;
                        bytesProcessed[0] += 2;
                        if (messageContentLength == -1) {
                            if (offset[0] < content.length) {
                                if (((int) content[offset[0]]) ==
                                    BERElement.EOC) {
                                    offset[0] += 2;
                                    bytesProcessed[0] += 2;
                                }
                            }
                        }
                        messageBytesProcessed += bytesProcessed[0];
                        offsetIndex = END;
                    }
                }
            }
            if (offset[0] >= content.length) {
                controlsParsed = true;
                content = null;
            }
        } else {
            super.parseComponent(index);
        }
    }
    
    /**
     * Returns the OID of the response.
     *
     * @return the response OID.
     */
    public String  getID() {
        if (getProtocolOp() == null) {
            while (offsetIndex != END) {
                parseComponent(OPTIONAL);
            }
            return m_oid;
        } else {
            return ((JDAPExtendedResponse) getProtocolOp()).getID();            
        }
    }

    /**
     * Returns the OID of the response.
     *
     * @return the response OID.
     * @deprecated Use <CODE>LDAPExtendedResponse.getID()</CODE>
     */
    public String  getOID() {
        return getID();
    }

    /**
     * Returns the raw bytes of the value part of the response.
     *
     * @return response as a raw array of bytes.
     */
    public byte[] getValue() {
        if (getProtocolOp() == null) {
            while (offsetIndex != END) {
                parseComponent(OPTIONAL);
            }
            return m_value;
        } else {
            return ((JDAPExtendedResponse)getProtocolOp()).getValue();
        }
    }

    public int getMessageType() {
        return LDAPMessage.LDAP_EXTENDED_RESPONSE_MESSAGE;
    }

    protected String getString() {
        return "ExtendedResponse " + super.getString();
    }
}
