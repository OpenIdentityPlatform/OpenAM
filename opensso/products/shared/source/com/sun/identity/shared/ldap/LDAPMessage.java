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

import java.util.*;
import com.sun.identity.shared.ldap.client.opers.*;
import com.sun.identity.shared.ldap.client.JDAPBERTagDecoder;
import com.sun.identity.shared.ldap.ber.stream.*;
import java.io.*;
import java.net.*;


/**
 * Base class for LDAP request and response messages.
 * This class represents the LDAPMessage in RFC2251. The
 * message is the entity that got transferred back and
 * fro between the server and the client interface. Each
 * message has a protocol operation. The protocol operation
 * indicates if it is a request or response.
 * <pre>
 * LDAPMessage ::= SEQUENCE {
 *   messageID MessageID,
 *   protocolOp CHOICE {
 *     bindRequest BindRequest,
 *     ...
 *   }
 *   controls [0] Controls OPTIONAL
 * }
 * </pre>
 *
 * @version 1.0
 */
public class LDAPMessage implements java.io.Serializable {

    static final long serialVersionUID = -1364094245850026720L;

    public final static int BIND_REQUEST        = 0;
    public final static int BIND_RESPONSE       = 1;
    public final static int UNBIND_REQUEST      = 2;
    public final static int SEARCH_REQUEST      = 3;
    public final static int SEARCH_RESPONSE     = 4;
    public final static int SEARCH_RESULT       = 5;
    public final static int MODIFY_REQUEST      = 6;
    public final static int MODIFY_RESPONSE     = 7;
    public final static int ADD_REQUEST         = 8;
    public final static int ADD_RESPONSE        = 9;
    public final static int DEL_REQUEST         = 10;
    public final static int DEL_RESPONSE        = 11;
    public final static int MODIFY_RDN_REQUEST  = 12;
    public final static int MODIFY_RDN_RESPONSE = 13;
    public final static int COMPARE_REQUEST     = 14;
    public final static int COMPARE_RESPONSE    = 15;
    public final static int ABANDON_REQUEST     = 16;
    public final static int SEARCH_RESULT_REFERENCE = 19;
    public final static int EXTENDED_REQUEST    = 23;
    public final static int EXTENDED_RESPONSE   = 24;
    public final static int LDAP_SEND_REQUEST_MESSAGE = 25;
    public final static int LDAP_SEARCH_RESULT_MESSAGE = 26;
    public final static int LDAP_SEARCH_RESULT_REFERENCE_MESSAGE = 27;
    public final static int LDAP_EXTENDED_RESPONSE_MESSAGE = 28;
    public final static int LDAP_RESPONSE_MESSAGE = 29;

    private static JDAPBERTagDecoder decoder = new JDAPBERTagDecoder();
    public static final Integer SEQUENCE = new Integer(BERElement.SEQUENCE);
    public static final Integer SET = new Integer(BERElement.SET);
    public static final Integer[] EXPECTED_ROOT_TAG = {SEQUENCE, new
        Integer(BERElement.INTEGER)};
    public static final short END = 0;
    public static final short CONTROLS = 1;
        
    /**
     * Internal variables
     */
    protected JDAPProtocolOp m_protocolOp = null;
    protected LDAPControl m_controls[] = null;
    protected int[] offset;
    protected byte[] content = null;
    protected int messageContentLength;
    protected int messageBytesProcessed;
    protected int m_msgid;
    protected int m_type;
    protected boolean controlsParsed;
    
    /**
     * Constructs a ldap message.
     * @param msgid message identifier
     * @param op operation protocol
     */
    LDAPMessage(int msgid, JDAPProtocolOp op) {
        m_msgid = msgid;
        m_protocolOp = op;        
    }

    LDAPMessage(int msgid, JDAPProtocolOp op, LDAPControl controls[]) {
        m_msgid = msgid;
        m_protocolOp = op;
        m_controls = controls; /* LDAPv3 additions */        
    }

    protected LDAPMessage(int msgid, int type, int messageContentLength,
        byte[] content) {
        m_msgid = msgid;
        m_type = type;
        this.offset = new int[1];
        this.offset[0] = 0;
        this.messageContentLength = messageContentLength;
        this.content = content;
        this.messageBytesProcessed = 0;
        this.controlsParsed = false;
    }

    public int getMessageType() {
        return LDAP_SEND_REQUEST_MESSAGE;
    }

    protected static LDAPMessage getLDAPMessage(InputStream stream,
        int[] bytesRead, int[] bytesProcessed) throws IOException,
        LDAPException {
        int msgid = -1;
        int tag;
        bytesRead[0] = 0;
        int messageLength = 0;
        int tagIndex = 0;
        LDAPMessage msg = null;
        boolean invalid = false;
        do {
            bytesProcessed[0] = 0;
            tag = stream.read();
            if (tag == -1) {
                throw new IOException();
            }
            bytesRead[0]++;
            bytesProcessed[0]++;
            if ((tagIndex < EXPECTED_ROOT_TAG.length) &&
                (EXPECTED_ROOT_TAG[tagIndex].intValue() != tag)) {
                invalid = true;
            } else {
                tagIndex++;
            }
            switch (tag) {
                case BERElement.SEQUENCE:
                    if (invalid) {
                        throw new IOException("invalid tag " + tag);
                    } else {
                        messageLength = LDAPParameterParser.getLengthOctets(
                            stream, bytesRead, bytesProcessed);
                    }
                    break;
                case BERElement.INTEGER:
                    if (invalid) {
                        throw new IOException("invalid tag " + tag);
                    } else {
                        msgid = LDAPParameterParser.parseInt(stream, bytesRead,
                            bytesProcessed);
                    }
                    if (messageLength >= bytesProcessed[0]) {
                        messageLength -= bytesProcessed[0];
                    }
                    break;
                default:
                    int contentLength = LDAPParameterParser.getLengthOctets(
                        stream, bytesRead, bytesProcessed);
                    if (messageLength >= bytesProcessed[0]) {
                        messageLength -= bytesProcessed[0];
                    }
                    if (messageLength > 0) {
                        byte[] messageContent = new byte[messageLength];
                        int length = 0;
                        int messageOffset = 0;
                        while ((length = stream.read(messageContent,
                            messageOffset, messageLength)) != -1) {
                            messageLength -= length;
                            messageOffset += length;
                            if (messageLength == 0) {
                                break;
                            }
                        }
                        msg = LDAPMessageTagDecoder.decodeResponseMessageTag(
                            tag, msgid, contentLength, messageContent);
                    } else {
                        BERSequence seq = new BERSequence();
                        seq.addElement(new BERInteger(msgid));
                        seq.addElement(-1, decoder, stream, bytesRead); 
                        msg = LDAPMessage.parseMessage(seq);
                    }
            }
        } while(tagIndex <= EXPECTED_ROOT_TAG.length);
        return (LDAPMessage) msg;
    }

    public boolean equals(Object obj) {
        LDAPMessage msg = null;
        if (obj instanceof LDAPMessage) {
            msg = (LDAPMessage) obj;
        } else {
            return false;
        }

        if (getMessageType() != msg.getMessageType()) {
            return false;
        }
        if (getType() != msg.getType()) {
            return false;
        }
        if (getMessageID() != msg.getMessageID()) {
            return false;
        }
        LDAPControl[] thisControl = getControls();
        LDAPControl[] otherControl = msg.getControls();
        if (thisControl != null) {
            if (otherControl != null) {
                if (thisControl.length != otherControl.length) {
                    return false;
                } else {
                    for (int i = 0; i < thisControl.length; i++) {
                        if (!thisControl[i].toString().equals(
                            otherControl[i].toString())) {
                            return false;
                        }
                    }
                }
            } else {
                return false;
            }
        } else {
            if (otherControl != null) {
                return false;
            }
        }
        return true;
    }

    protected synchronized void parseControls() {
        int[] controlsOffset = new int[1];
        if (messageContentLength == -1) {
            controlsOffset[0] = offset[0];
        } else {
            controlsOffset[0] = messageContentLength;
        }
        if (controlsOffset[0] < content.length) {
            if ((content[controlsOffset[0]] & 0xff) == 0xa0) {
                int[] controlsBytesProcessed = new int[1];
                controlsBytesProcessed[0] = 0;
                controlsOffset[0]++;
                int length = LDAPParameterParser.getLengthOctets(content,
                    controlsOffset, controlsBytesProcessed);
                LinkedList controls = new LinkedList();
                controlsBytesProcessed[0] = 0;
                while ((length > controlsBytesProcessed[0]) ||
                    (length == -1)) {
                    if (length == -1) {
                        if (content[controlsOffset[0]] == BERElement.EOC) {
                            controlsOffset[0] += 2;
                            controlsBytesProcessed[0] += 2;
                            break;
                        }
                    }
                    if (content[controlsOffset[0]] == BERElement.SEQUENCE) {
                        controlsOffset[0]++;
                        controlsBytesProcessed[0]++;
                        int controlLength =
                            LDAPParameterParser.getLengthOctets(content,
                            controlsOffset, controlsBytesProcessed);
                        String oid = null;
                        if (content[controlsOffset[0]] ==
                            BERElement.OCTETSTRING) {
                            controlsOffset[0]++;
                            controlsBytesProcessed[0]++;
                            oid = LDAPParameterParser.parseOctetString(content,
                                controlsOffset, controlsBytesProcessed);
                        } else {
                            if (content[controlsOffset[0]] ==
                                (BERElement.OCTETSTRING |
                                BERElement.CONSTRUCTED)) {
                                controlsOffset[0]++;
                                controlsBytesProcessed[0]++;
                                oid = LDAPParameterParser.parseOctetStringList(
                                    content, controlsOffset,
                                    controlsBytesProcessed);
                            }
                        }
                        boolean critical = false;
                        if (content[controlsOffset[0]] == BERElement.BOOLEAN) {
                            controlsOffset[0]++;
                            controlsBytesProcessed[0]++;
                            critical = LDAPParameterParser.parseBoolean(
                                content, controlsOffset,
                                controlsBytesProcessed);
                        }
                        byte[] value = null;
                        if (content[controlsOffset[0]] ==
                            BERElement.OCTETSTRING) {
                            controlsOffset[0]++;
                            controlsBytesProcessed[0]++;
                            value = LDAPParameterParser.parseOctetBytes(
                                content, controlsOffset,
                                controlsBytesProcessed);
                        } else {
                            if (content[controlsOffset[0]] ==
                                (BERElement.OCTETSTRING |
                                BERElement.CONSTRUCTED)) {
                                controlsOffset[0]++;
                                controlsBytesProcessed[0]++;
                                value = LDAPParameterParser
                                    .parseOctetBytesList(
                                    content, controlsOffset,
                                    controlsBytesProcessed);
                            }
                        }
                        controls.add(LDAPControl.createControl(oid, critical,
                            value));
                        if (controlLength == -1) {
                            if (content[controlsOffset[0]] == BERElement.EOC) {
                                controlsOffset[0] += 2;
                                controlsBytesProcessed[0] += 2;
                            }
                        }
                    }
                }
                if (!controls.isEmpty()) {
                    m_controls = (LDAPControl[]) controls.toArray(
                        new LDAPControl[0]);
                }
                controls = null;
            }
        }
        controlsParsed = true;
    }

    /**
     * Creates a ldap message from a BERElement. This method is used
     * to parse LDAP response messages
     *
     * @param element ber element constructed from incoming byte stream
     */
    static LDAPMessage parseMessage(BERElement element) throws IOException {
        int l_msgid;
        JDAPProtocolOp l_protocolOp = null;
        LDAPControl l_controls[] = null;
        
        if (element.getType() != BERElement.SEQUENCE)
            throw new IOException("SEQUENCE in jdap message expected");
        BERSequence seq = (BERSequence)element;
        BERInteger msgid = (BERInteger)seq.elementAt(0);
        l_msgid = msgid.getValue();
        BERElement protocolOp = (BERElement)seq.elementAt(1);
        if (protocolOp.getType() != BERElement.TAG) {
            throw new IOException("TAG in protocol operation is expected");
        }
        BERTag tag = (BERTag)protocolOp;
        switch (tag.getTag()&0x1f) {
            case JDAPProtocolOp.BIND_RESPONSE:
                l_protocolOp = new JDAPBindResponse(protocolOp);
  	        break;
            case JDAPProtocolOp.SEARCH_RESPONSE:
                l_protocolOp = new JDAPSearchResponse(protocolOp);
  	        break;
            /*
             * If doing search without bind,
             * x500.arc.nasa.gov returns tag SEARCH_REQUEST tag
             * in SEARCH_RESULT.
             */
            case JDAPProtocolOp.SEARCH_REQUEST:
            case JDAPProtocolOp.SEARCH_RESULT:
                l_protocolOp = new JDAPSearchResult(protocolOp);
  	        break;
            case JDAPProtocolOp.MODIFY_RESPONSE:
                l_protocolOp = new JDAPModifyResponse(protocolOp);
  	        break;
            case JDAPProtocolOp.ADD_RESPONSE:
                l_protocolOp = new JDAPAddResponse(protocolOp);
      	    break;
            case JDAPProtocolOp.DEL_RESPONSE:
                l_protocolOp = new JDAPDeleteResponse(protocolOp);
          	break;
            case JDAPProtocolOp.MODIFY_RDN_RESPONSE:
                l_protocolOp = new JDAPModifyRDNResponse(protocolOp);
          	break;
            case JDAPProtocolOp.COMPARE_RESPONSE:
                l_protocolOp = new JDAPCompareResponse(protocolOp);
          	break;
            case JDAPProtocolOp.SEARCH_RESULT_REFERENCE:
                l_protocolOp = new JDAPSearchResultReference(protocolOp);
          	break;
            case JDAPProtocolOp.EXTENDED_RESPONSE:
                l_protocolOp = new JDAPExtendedResponse(protocolOp);
          	break;
            default:
                throw new IOException("Unknown protocol operation");
        }

        /* parse control */
        if (seq.size() >= 3) {
            tag = (BERTag)seq.elementAt(2);
            if ( tag.getTag() == (BERTag.CONSTRUCTED|BERTag.CONTEXT|0) ) {
                BERSequence controls = (BERSequence)tag.getValue();
                l_controls = new LDAPControl[controls.size()];
                for (int i = 0; i < controls.size(); i++) {
                    l_controls[i] = LDAPControl.parseControl(controls.elementAt(i));
    		    }
            }
        }
        
        switch (l_protocolOp.getType()) {
            case JDAPProtocolOp.SEARCH_RESPONSE:
                return new LDAPSearchResult(l_msgid,
                    (JDAPSearchResponse) l_protocolOp, l_controls);                
            case JDAPProtocolOp.SEARCH_RESULT_REFERENCE:
                return new LDAPSearchResultReference(l_msgid,
                    (JDAPSearchResultReference) l_protocolOp, l_controls);                
            case JDAPProtocolOp.EXTENDED_RESPONSE:
                return new LDAPExtendedResponse(l_msgid,
                    (JDAPExtendedResponse) l_protocolOp, l_controls);                
            default:
                return new LDAPResponse(l_msgid, l_protocolOp, l_controls);
        }
    }

    /**
     * Returns the message identifer.
     * @return message identifer.
     */
    public int getMessageID(){
        return m_msgid;
    }

    /**
     * Returns the LDAP operation type of the message
     * @return message type.
     */
    public int getType(){
        if (m_protocolOp == null) {
            return m_type;
        } else {
            return m_protocolOp.getType();
        }
    }

    /**
     * Retrieves the protocol operation.
     * @return protocol operation.
     */
    protected JDAPProtocolOp getProtocolOp() {
        return m_protocolOp;
    }

    /**
     * Retrieves list of controls.
     * @return controls.
     */
    public LDAPControl[] getControls() {
        return m_controls;
    }

    /**
     * Writes the ber encoding to stream.
     * @param s output stream
     */
    void write(OutputStream s) throws IOException {
        BERSequence seq = new BERSequence();
        BERInteger i = new BERInteger(m_msgid);
        seq.addElement(i);
        BERElement e = null;
        if (m_protocolOp != null) {
            e = m_protocolOp.getBERElement();
        }
        if (e == null) {
            throw new IOException("Bad BER element");
        }
        seq.addElement(e);
        if (m_controls != null) { /* LDAPv3 additions */
            BERSequence c = new BERSequence();
            for (int j = 0; j < m_controls.length; j++) {
                c.addElement(m_controls[j].getBERElement());
            }
            BERTag t = new BERTag(BERTag.CONTEXT|BERTag.CONSTRUCTED|0, c, true);
            seq.addElement(t);
        }
        seq.write(s);
    }

    protected String getString() {
        return "";
    }

    /**
     * Returns string representation of an LDAP message.
     * @return LDAP message.
     */
    public String toString() {
        StringBuffer sb = new StringBuffer("[LDAPMessage] ");
        sb.append(m_msgid);
        sb.append(" ");
        if (m_protocolOp != null) {
            sb.append(m_protocolOp.toString());
        } else {
            sb.append(getString());
        }
        for (int i =0; m_controls != null && i < m_controls.length; i++) {
            sb.append(" ");
            sb.append(m_controls[i].toString());
        }
        return sb.toString();
    }
    
    /**
     * Returns string representation of a ldap message with
     * the time stamp. Used for message trace
     * @return ldap message with the time stamp
     */
    StringBuffer toTraceString() {
        StringBuffer sb = new StringBuffer(" op=");
        sb.append(m_msgid);
        sb.append(" ");
        if (m_protocolOp != null) {
            sb.append(m_protocolOp.toString());
        } else {
            sb.append(getString());
        }
        for (int i =0; m_controls != null && i < m_controls.length; i++) {
            sb.append(" ");
            sb.append(m_controls[i].toString());
        }
        return sb;
    }

}
