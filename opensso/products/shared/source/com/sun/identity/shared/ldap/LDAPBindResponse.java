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
 * $Id: LDAPBindResponse.java,v 1.1 2009/11/20 23:52:58 ww203982 Exp $
 */
package com.sun.identity.shared.ldap;

import com.sun.identity.shared.ldap.client.opers.JDAPBindResponse;
import com.sun.identity.shared.ldap.ber.stream.BERElement;

/**
 * @deprecated As of ForgeRock OpenAM 10.
 */
public class LDAPBindResponse extends LDAPResponse {

    protected byte[] m_credentials = null;

    protected LDAPBindResponse(int msgid, int operType, int contentLength,
        byte[] content) {
        super(msgid, operType, contentLength, content);
    }

    public byte[] getCredentials() {
        while (offsetIndex != END) {
            parseComponent(OPTIONAL);
        }
        return m_credentials;
    }

    public boolean equals(Object obj) {
        LDAPBindResponse msg = null;
        if (obj instanceof LDAPBindResponse) {
            msg = (LDAPBindResponse) obj;
        } else {
            return false;
        }
        boolean e = super.equals(msg);
        if (!e) {
            return false;
        }
        byte[] thisCredentials = getCredentials();
        byte[] otherCredentials = msg.getCredentials();
        if (thisCredentials != null) {
            if (otherCredentials != null) {
                if (thisCredentials.length != otherCredentials.length) {
                    return false;
                }
                for (int i = 0; i < thisCredentials.length; i++) {
                    if (thisCredentials[i] != otherCredentials[i]) {
                        return false;
                    }
                }
            } else {
                return false;
            }
        } else {
            if (otherCredentials != null) {
                return false;
            }
        }
        return true;
    }

    protected synchronized void parseComponent(final short index) {
        if ((index == OPTIONAL) && ((content[offset[0]] & 0xff) == 0xa7)) {
            int[] bytesProcessed = new int[1];
            bytesProcessed[0] = 0;
            /* Context Specific <Construct> [7]:
             * Handle Microsoft v3 serverCred in
             * bind response. MS encodes it as SEQUENCE OF
             * while it should be CHOICE OF.
             */
            offset[0]++;
            bytesProcessed[0]++;
            int length = LDAPParameterParser.getLengthOctets(
                content, offset, bytesProcessed);
            if (content[offset[0]] == BERElement.OCTETSTRING) {
                offset[0]++;
                bytesProcessed[0]++;
                m_credentials = LDAPParameterParser.parseOctetBytes(content,
                    offset, bytesProcessed);
            }
            if (length == -1) {
                if (content[offset[0]] == BERElement.EOC) {
                    offset[0] += 2;
                    bytesProcessed[0] += 2;
                }
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

}
