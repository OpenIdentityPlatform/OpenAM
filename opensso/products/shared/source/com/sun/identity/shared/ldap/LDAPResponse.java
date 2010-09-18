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

import com.sun.identity.shared.ldap.client.opers.JDAPProtocolOp;
import com.sun.identity.shared.ldap.client.opers.JDAPResult;
import java.util.BitSet;
import com.sun.identity.shared.ldap.ber.stream.BERElement;
import java.util.LinkedList;

/**
 * Represents the response to a particular LDAP operation.
 * 
 * @version 1.0
 */
public class LDAPResponse extends LDAPMessage {
    static final long serialVersionUID = 5822205242593427418L;

    public static final short RESULT_CODE = 2;
    public static final short MATCHED_DN = 3;
    public static final short ERROR_MESSAGE = 4;
    public static final short OPTIONAL = 5;
    protected int m_result_code;
    protected String m_matched_dn = null;
    protected String m_error_message = null;
    protected String[] m_referrals = null;
    protected short offsetIndex;

    /**
     * Constructor
     * 
     * @param msgid message identifier
     * @param rsp operation response
     * @param controls array of controls or null
     */
    LDAPResponse(int msgid, JDAPProtocolOp rsp, LDAPControl controls[]) {
        super(msgid, rsp, controls);
    }

    protected LDAPResponse(int msgid, int operType, int messageContentLength,
        byte[] content) {
        super(msgid, operType, messageContentLength, content);
        this.offsetIndex = RESULT_CODE;
    }

    public boolean equals(Object obj) {
        LDAPResponse msg = null;
        if (obj instanceof LDAPResponse) {
            msg = (LDAPResponse) obj;
        } else {
            return false;
        }
        boolean e = super.equals(msg);
        if (!e) {
            return false;
        }
        if (getResultCode() != msg.getResultCode()) {
            return false;
        }
        String thisDN = getMatchedDN();
        String otherDN = msg.getMatchedDN();
        if (thisDN != null) {
            if (otherDN != null) {
                if (!thisDN.equals(otherDN)) {
                    return false;
                }
            } else {
                return false;
            }
        } else {
            if (otherDN != null) {
                return false;
            }
        }
        String thisError = getErrorMessage();
        String otherError = msg.getErrorMessage();
        if (thisError != null) {
            if (otherError != null) {
                if (!thisError.equals(otherError)) {
                    return false;
                }
            } else {
                return false;
            }
        } else {
            if (otherError != null) {
                return false;
            }
        }
        String[] thisRef = getReferrals();
        String[] otherRef = msg.getReferrals();
        if (thisRef != null) {
            if (otherRef != null) {
                if (thisRef.length != otherRef.length) {
                    return false;
                } else {
                    for (int i = 0; i < thisRef.length; i++) {
                        if (!thisRef[i].equals(otherRef[i])) {
                            return false;
                        }
                    }
                }
            } else {
                return false;
            }
        } else {
            if (otherRef != null) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns any error message in the response.
     *
     * @return the error message of the last error (or <CODE>null</CODE>
     * if no message was set).
     */
    public String  getErrorMessage() {
        if (getProtocolOp() == null) {
            if ((offsetIndex <= ERROR_MESSAGE) &&
                (offsetIndex >= RESULT_CODE)) {
                parseComponent(ERROR_MESSAGE);
            }
        } else {
            return ((JDAPResult) getProtocolOp()).getErrorMessage();
        }
        return m_error_message;
    }

    /**
     * Returns the partially matched DN field, if any, in a server response.
     *
     * @return the maximal subset of a DN to match,
     * or <CODE>null</CODE>.
     */
    public String getMatchedDN() {
        if (getProtocolOp() == null) {
            if ((offsetIndex <= MATCHED_DN) && (offsetIndex >= RESULT_CODE)) {
                parseComponent(MATCHED_DN);
            }
        } else {
            return ((JDAPResult) getProtocolOp()).getMatchedDN();
        }
        return m_matched_dn;
    }
    
    /**
     * Returns all referrals, if any, in a server response.
     *
     * @return a list of referrals or <CODE>null</CODE>.
     */
    public String[] getReferrals() {
        if (getProtocolOp() == null) {
            while (offsetIndex != END) {
                parseComponent(OPTIONAL);
            }
        } else {
            return ((JDAPResult) getProtocolOp()).getReferrals();
        }
        return m_referrals;
    }    

    /**
     * Returns the result code in a server response.
     *
     * @return the result code.
     */
    public int getResultCode() {
        if (getProtocolOp() == null) {
            if (offsetIndex == RESULT_CODE) {
                parseComponent(RESULT_CODE);
            }
        } else {
            return ((JDAPResult) getProtocolOp()).getResultCode();
        }
        return m_result_code;
    }
    
    public int getMessageType() {
        return LDAPMessage.LDAP_RESPONSE_MESSAGE;
    }

    public LDAPControl[] getControls() {
        if (getProtocolOp() == null) {
            if (!controlsParsed) {
                if (messageContentLength == -1) {
                    while (offsetIndex != END) {
                        parseComponent(OPTIONAL);
                    }
                }
                parseControls();
                if (offsetIndex == END) {
                    content = null;
                }
            }
        }
        return m_controls;
    }

    protected synchronized void parseComponent(final short index) {
        int[] bytesProcessed = new int[1];
        bytesProcessed[0] = 0;
        int length;
        if (offset[0] == 0) {
            if (((int) content[offset[0]]) == BERElement.SEQUENCE) {
                offset[0]++;
                bytesProcessed[0]++;
                length = LDAPParameterParser.getLengthOctets(content,
                    offset, bytesProcessed);
                if (messageContentLength == -1) {
                    if (length != -1) {
                        messageContentLength = length + bytesProcessed[0];
                    }
                    messageBytesProcessed += bytesProcessed[0];
                    bytesProcessed[0] = 0;
                }
            }
        }
        switch (index) {
            case RESULT_CODE:
                if (offsetIndex != RESULT_CODE) {
                    return;
                }
                if (((int) content[offset[0]]) == BERElement.ENUMERATED) {
                    offset[0]++;
                    bytesProcessed[0]++;
                    m_result_code = LDAPParameterParser.parseInt(content,
                        offset, bytesProcessed);
                    offsetIndex = MATCHED_DN;
                }
                messageBytesProcessed += bytesProcessed[0];
                return;
            case MATCHED_DN:
                 if (offsetIndex == RESULT_CODE) {
                    parseComponent(RESULT_CODE);
                }
                if (offsetIndex != MATCHED_DN) {
                    return;
                }
                if (((int) content[offset[0]]) == BERElement.OCTETSTRING) {
                    offset[0]++;
                    bytesProcessed[0]++;
                    m_matched_dn = LDAPParameterParser.parseOctetString(content,
                        offset, bytesProcessed);
                    offsetIndex = ERROR_MESSAGE;
                    messageBytesProcessed += bytesProcessed[0];
                } else {
                    if (((int) content[offset[0]]) == (BERElement.OCTETSTRING |
                        BERElement.CONSTRUCTED)) {
                        offset[0]++;
                        bytesProcessed[0]++;
                        m_matched_dn =
                            LDAPParameterParser.parseOctetStringList(content,
                            offset, bytesProcessed);
                        offsetIndex = ERROR_MESSAGE;
                        messageBytesProcessed += bytesProcessed[0];
                    }
                }
                return;
            case ERROR_MESSAGE:
                switch (offsetIndex) {
                    case RESULT_CODE:
                    case MATCHED_DN:
                        parseComponent(MATCHED_DN);
                }
                if (offsetIndex != ERROR_MESSAGE) {
                    return;
                }
                if (((int) content[offset[0]]) == BERElement.OCTETSTRING) {
                    offset[0]++;
                    bytesProcessed[0]++;
                    m_error_message = LDAPParameterParser.parseOctetString(content,
                        offset, bytesProcessed);
                    offsetIndex = OPTIONAL;
                    messageBytesProcessed += bytesProcessed[0];
                } else {
                    if (((int) content[offset[0]]) == (BERElement.OCTETSTRING | 
                        BERElement.CONSTRUCTED)) {
                        offset[0]++;
                        bytesProcessed[0]++;
                        m_error_message =
                            LDAPParameterParser.parseOctetStringList(content,
                            offset, bytesProcessed);
                        offsetIndex = OPTIONAL;
                        messageBytesProcessed += bytesProcessed[0];
                    }
                }
                break;
            case OPTIONAL:
                switch (offsetIndex) {
                    case RESULT_CODE:
                    case MATCHED_DN:
                    case ERROR_MESSAGE:
                        parseComponent(ERROR_MESSAGE);
                }
                if ((offsetIndex != OPTIONAL) || (offsetIndex == END)) {
                    return;
                }
                switch (content[offset[0]] & 0xff) {
                    case 0x85: /* Context Specific [5]:
                      * (a) Handle Microsoft v3 referral bugs! (Response)
                      * (b) Handle Microsoft v3 supportedVersion in Bind
                      *     response
                      */
                        offset[0]++;
                        bytesProcessed[0]++;
                        LDAPParameterParser.parseInt(content, offset,
                            bytesProcessed);
                        break;
                    case 0xa3:
                        offset[0]++;
                        bytesProcessed[0]++;                       
                        LinkedList tempReferrals = new LinkedList();
                        length = LDAPParameterParser.getLengthOctets(
                            content, offset, bytesProcessed);
                        int[] tempBytesProcessed = new int[1];
                        tempBytesProcessed[0] = 0;
                        while ((length > tempBytesProcessed[0]) ||
                            (length == -1)) {
                            if ((length == -1) && (content[offset[0]] ==
                                BERElement.EOC)) {
                                offset[0] += 2;
                                tempBytesProcessed[0] += 2;
                                break;
                            }
                            if (content[offset[0]] == BERElement.OCTETSTRING) {
                                offset[0]++;
                                tempBytesProcessed[0]++;
                                tempReferrals.add(
                                    LDAPParameterParser.parseOctetString(
                                    content, offset, tempBytesProcessed));
                            } else {
                                if (((int) content[offset[0]]) ==
                                    (BERElement.OCTETSTRING |
                                    BERElement.CONSTRUCTED)) {
                                    offset[0]++;
                                    tempBytesProcessed[0]++;
                                    tempReferrals.add(LDAPParameterParser
                                        .parseOctetStringList(
                                        content, offset, tempBytesProcessed));
                                }
                            }
                        }
                        if (!tempReferrals.isEmpty()){
                            m_referrals = (String[]) tempReferrals.toArray(
                                new String[0]);
                        }
                        bytesProcessed[0] += tempBytesProcessed[0];
                }
                messageBytesProcessed += bytesProcessed[0];
        }
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
                            if (((int) content[offset[0]]) == BERElement.EOC) {
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
    }

    protected String getString() {
        StringBuffer sb = new StringBuffer("Result {resultCode=");
        sb.append(getResultCode());
        if (m_matched_dn != null) {
            sb.append(", matchedDN=");
            sb.append(getMatchedDN());
        }
        if (m_error_message != null) {
            sb.append(", errorMessage=");
            sb.append(getErrorMessage());
        }
        String[] referrals = getReferrals();
        if (referrals != null && referrals.length > 0) {
            sb.append(", referrals=");
            for (int i = 0; i < referrals.length; i++) {
                sb.append((i == 0 ? "" : " "));
                sb.append(referrals[i]);
            }
        }
        sb.append("}");
        return sb.toString();
    }
}
