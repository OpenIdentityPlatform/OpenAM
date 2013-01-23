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
 * $Id: LDAPRequestParser.java,v 1.1 2009/11/20 23:52:58 ww203982 Exp $
 */
/**
 * Portions Copyrighted 2013 ForgeRock Inc
 */
package com.sun.identity.shared.ldap;

import com.sun.identity.shared.ldap.ber.stream.BERElement;
import com.sun.identity.shared.ldap.client.JDAPFilter;
import com.sun.identity.shared.ldap.client.JDAPFilterOpers;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Enumeration;

/**
 * @deprecated As of ForgeRock OpenAM 10.
 */
public class LDAPRequestParser {

    public static final int DEFAULT_DEREFERENCE = 0;

    public static final int DEFAULT_SERVER_TIME_LIMIT = 0;

    public static final int DEFAULT_MAX_RESULT = 0;

    public static LDAPAbandonRequest parseAbandonRequest(int msgid) {
        LinkedList bytesList = new LinkedList();
        int Length = 0;
        byte[] tempTag;
        Length += addInt(bytesList, msgid);
        tempTag = new byte[1];
        tempTag[0] = (byte) (BERElement.APPLICATION | 16);
        bytesList.addFirst(tempTag);
        Length++;
        return new LDAPAbandonRequest(bytesList, Length);
    }

    public static LDAPAddRequest parseAddRequest(LDAPEntry entry) {
        LDAPAttributeSet attrSet = entry.getAttributeSet();
        LDAPAttribute[] attrs = new LDAPAttribute[attrSet.size()];
        for( int i = 0; i < attrSet.size(); i++ ) {
            attrs[i] = (LDAPAttribute)attrSet.elementAt( i );
        }
        return parseAddRequest(entry.getDN(), attrs);
    }

    public static LDAPAddRequest parseAddRequest(String dn,
        LDAPAttribute attrs[]) {
        LinkedList bytesList = new LinkedList();
        byte[] tempBytes;
        byte[] tempTag;
        int Length = 0;
        for (int i = attrs.length - 1; i >= 0; i--) {
            Length += attrs[i].addLDAPAttribute(bytesList);
        }
        // add sequence of attributes
        tempBytes = getLengthBytes(Length);
        bytesList.addFirst(tempBytes);
        Length += tempBytes.length;
        bytesList.addFirst(BERElement.SEQUENCE_BYTES);
        Length++;
        // add dn
        Length += addOctetString(bytesList, dn);
        bytesList.addFirst(BERElement.OCTETSTRING_BYTES);
        Length++;
        // add length of whole message
        tempBytes = getLengthBytes(Length);
        bytesList.addFirst(tempBytes);
        Length += tempBytes.length;
        tempTag = new byte[1];
        tempTag[0] = (byte) (BERElement.APPLICATION |
            BERElement.CONSTRUCTED | 8);
        bytesList.addFirst(tempTag);
        Length++;
        return new LDAPAddRequest(dn, attrs, bytesList, Length);
    }

    public static LDAPBindRequest parseBindRequest(String name,
        String password) {
        return parseBindRequest(LDAPConnection.LDAP_VERSION, name, password);
    }

    public static LDAPBindRequest parseBindRequest(int version, String name,
        String password) {
        LinkedList bytesList = new LinkedList();
        byte[] tempBytes;
        byte[] tempTag;
        int Length = 0;
        // add password
        Length += addOctetString(bytesList, password);
        tempTag = new byte[1];
        tempTag[0] = (byte) BERElement.CONTEXT;
        bytesList.addFirst(tempTag);
        Length++;
        // add user name
        Length += addOctetString(bytesList, name);
        bytesList.addFirst(BERElement.OCTETSTRING_BYTES);
        Length++;
        // add version
        Length += addInt(bytesList, version);
        bytesList.addFirst(BERElement.INTEGER_BYTES);
        Length++;
        // add whole message length
        tempBytes = getLengthBytes(Length);
        bytesList.addFirst(tempBytes);
        Length += tempBytes.length;
        tempTag = new byte[1];
        tempTag[0] = (byte) (BERElement.APPLICATION |
            BERElement.CONSTRUCTED | 0);
        bytesList.addFirst(tempTag);
        Length++;
        return new LDAPBindRequest(version, name, password, bytesList, Length);
    }

    public static LDAPBindRequest parseBindRequest(String name,
        String mechanism, byte credentials[]) {
        LinkedList bytesList = new LinkedList();
        byte[] tempBytes;
        byte[] tempTag;
        int Length = 0;
        // add credentials
        Length += addOctetBytes(bytesList, credentials);
        bytesList.addFirst(BERElement.OCTETSTRING_BYTES);
        Length++;
        // add mechanism
        Length += addOctetString(bytesList, mechanism);
        bytesList.addFirst(BERElement.OCTETSTRING_BYTES);
        Length++;
        // add credentials and mechanism Tag length
        tempBytes = getLengthBytes(Length);
        bytesList.addFirst(tempBytes);
        Length += tempBytes.length;
        tempTag = new byte[1];
        tempTag[0] = (byte) (BERElement.SASLCONTEXT | 3);
        bytesList.addFirst(tempTag);
        Length++;
        // add user name
        Length += addOctetString(bytesList, name);
        bytesList.addFirst(BERElement.OCTETSTRING_BYTES);
        Length++;
        // add version
        Length += addInt(bytesList, 3);
        bytesList.addFirst(BERElement.INTEGER_BYTES);
        Length++;
        tempBytes = getLengthBytes(Length);
        bytesList.addFirst(tempBytes);
        Length += tempBytes.length;
        tempTag = new byte[1];
        tempTag[0] = (byte) (BERElement.APPLICATION |
            BERElement.CONSTRUCTED | 0);
        bytesList.addFirst(tempTag);
        Length++;
        return new LDAPBindRequest(name, mechanism, credentials,
            bytesList, Length);
    }

    public static LDAPCompareRequest parseCompareRequest(String dn,
        LDAPAttribute attr) {
        Enumeration en = attr.getStringValues();
        String val = (String)en.nextElement();
        return parseCompareRequest(dn, attr.getName(), val);
    }

    public static LDAPCompareRequest parseCompareRequest(String dn,
        String type, String value) {
        LinkedList bytesList = new LinkedList();
        byte[] tempBytes;
        byte[] tempTag;
        int Length = 0;
        // add JDAPAVA
        Length += addOctetBytes(bytesList,
        JDAPFilterOpers.getOctetString(value).getValue());
        bytesList.addFirst(BERElement.OCTETSTRING_BYTES);
        Length++;
        Length += addOctetString(bytesList, type);
        bytesList.addFirst(BERElement.OCTETSTRING_BYTES);
        Length++;
        // add length for AVA sequence
        tempBytes = getLengthBytes(Length);
        bytesList.addFirst(tempBytes);
        Length += tempBytes.length;
        bytesList.addFirst(BERElement.SEQUENCE_BYTES);
        Length++;
        // add DN
        Length += addOctetString(bytesList, dn);
        bytesList.addFirst(BERElement.OCTETSTRING_BYTES);
        Length++;
        tempBytes = getLengthBytes(Length);
        bytesList.addFirst(tempBytes);
        Length += tempBytes.length;
        tempTag = new byte[1];
        tempTag[0] = (byte) (BERElement.APPLICATION |
            BERElement.CONSTRUCTED | 14);
        bytesList.addFirst(tempTag);
        Length++;
        return new LDAPCompareRequest(dn, type, value, bytesList, Length);
    }

    public static LDAPDeleteRequest parseDeleteRequest(String dn) {
        LinkedList bytesList = new LinkedList();
        byte[] tempBytes;
        byte[] tempTag;
        int Length = 0;
        // add dn
        Length += addOctetString(bytesList, dn);
        tempTag = new byte[1];
        tempTag[0] = (byte) (BERElement.APPLICATION | 10);
        bytesList.addFirst(tempTag);
        Length++;
        return new LDAPDeleteRequest(dn, bytesList, Length);
    }

    public static LDAPExtendedRequest parseExtendedRequest(String oid,
        byte[] value) {
        LinkedList bytesList = new LinkedList();
        byte[] tempTag;
        byte[] tempBytes;
        int Length = 0;
        // add value
        if (value != null) {
            Length += addOctetBytes(bytesList, value);
            tempTag = new byte[1];
            tempTag[0] = (byte) (BERElement.CONTEXT | 1);
            bytesList.addFirst(tempTag);
            Length++;
        }
        // add oid
        Length += addOctetString(bytesList, oid);
        tempTag = new byte[1];
        tempTag[0] = (byte) (BERElement.CONTEXT | 0);
        bytesList.addFirst(tempTag);
        Length++;
        // add length of whole message
        tempBytes = getLengthBytes(Length);
        bytesList.addFirst(tempBytes);
        Length += tempBytes.length;
        tempTag = new byte[1];
        tempTag[0] = (byte) (BERElement.APPLICATION |
            BERElement.CONSTRUCTED | 23);
        Length++;
        return new LDAPExtendedRequest(oid, value, bytesList, Length);
    }

    public static LDAPModifyRDNRequest parseModifyRDNRequest(
        String old_dn, String new_rdn, boolean delete_old_dn) {
        return parseModifyRDNRequest(old_dn, new_rdn, delete_old_dn, null);
    }

    public static LDAPModifyRDNRequest parseModifyRDNRequest(String old_dn,
        String new_rdn, boolean delete_old_dn, String new_superior) {
        LinkedList bytesList = new LinkedList();
        byte[] tempTag;
        byte[] tempBytes;
        int Length = 0;
        // add superior
        if (new_superior != null) {
            Length += addOctetString(bytesList, new_superior);
            tempTag = new byte[1];
            tempTag[0] = (byte) (BERElement.CONTEXT | 0);
            bytesList.addFirst(tempTag);
            Length++;
        }
        // add delete old dn
        Length += addBoolean(bytesList, delete_old_dn);
        bytesList.addFirst(BERElement.BOOLEAN_BYTES);
        Length++;
        // add new rdn
        Length += addOctetString(bytesList, new_rdn);
        bytesList.addFirst(BERElement.OCTETSTRING_BYTES);
        Length++;
        // add old dn 
        Length += addOctetString(bytesList, old_dn);
        bytesList.addFirst(BERElement.OCTETSTRING_BYTES);
        Length++;
        // add the length of the whole message
        tempBytes = getLengthBytes(Length);
        bytesList.addFirst(tempBytes);
        Length += tempBytes.length;
        tempTag = new byte[1];
        tempTag[0] = (byte) (BERElement.APPLICATION |
            BERElement.CONSTRUCTED | 12);
        Length++;
        return new LDAPModifyRDNRequest(old_dn, new_rdn, delete_old_dn,
            new_superior, bytesList, Length);
    }

    public static LDAPModifyRequest parseModifyRequest(String dn,
        LDAPModification mod) {
        LDAPModification[] mods = {mod};
        return parseModifyRequest(dn, mods);
    }

    public static LDAPModifyRequest parseModifyRequest(String dn,
        LDAPModificationSet mods) {
        LDAPModification[] modList = new LDAPModification[mods.size()];
        for( int i = 0; i < mods.size(); i++) {
            modList[i] = mods.elementAt(i);
        }
        return parseModifyRequest(dn, modList);
    }

    public static LDAPModifyRequest parseModifyRequest(String dn,
        LDAPModification mod[]) {
        LinkedList bytesList = new LinkedList();
        byte[] tempBytes;
        byte[] tempTag;
        int Length = 0;
        // add modification
        if (mod != null) {
            for (int i = mod.length -1; i >= 0; i--) {
                Length += mod[i].addLDAPModification(bytesList);
            }
        }
        tempBytes = getLengthBytes(Length);
        bytesList.addFirst(tempBytes);
        Length += tempBytes.length;
        bytesList.addFirst(BERElement.SEQUENCE_BYTES);
        Length++;
        // add dn
        Length += addOctetString(bytesList, dn);
        bytesList.addFirst(BERElement.OCTETSTRING_BYTES);
        Length++;
        // add length of the whole message
        tempBytes = getLengthBytes(Length);
        bytesList.addFirst(tempBytes);
        Length += tempBytes.length;
        tempTag = new byte[1];
        tempTag[0] = (byte) (BERElement.APPLICATION |
            BERElement.CONSTRUCTED | 6);
        bytesList.addFirst(tempTag);
        Length++;
        return new LDAPModifyRequest(dn, mod, bytesList, Length);
    }

    public static LDAPUnbindRequest parseUnbindRequest() {
        LinkedList bytesList = new LinkedList();
        int Length = 0;
        byte[] tempBytes = getLengthBytes(0);
        bytesList.addFirst(tempBytes);
        Length += tempBytes.length;
        byte[] tempTag = new byte[1];
        tempTag[0] = (byte) (BERElement.APPLICATION | 2);
        bytesList.addFirst(tempTag);
        Length++;
        return new LDAPUnbindRequest(bytesList, Length);
    }

    public static LDAPSearchRequest parseReadRequest(String dn,
        String[] attrs) {
        return parseReadRequest(dn, attrs, DEFAULT_SERVER_TIME_LIMIT,
            DEFAULT_DEREFERENCE, DEFAULT_MAX_RESULT);
    }

    public static LDAPSearchRequest parseReadRequest(String dn) {
        return parseReadRequest(dn, null, DEFAULT_SERVER_TIME_LIMIT, 
            DEFAULT_DEREFERENCE, DEFAULT_MAX_RESULT);
    }

    public static LDAPSearchRequest parseReadRequest(String dn, String[] attrs,
        LDAPSearchConstraints cons) {
        return parseReadRequest(dn, attrs, cons.getServerTimeLimit(),
            cons.getDereference(), cons.getMaxResults());
    }

    public static LDAPSearchRequest parseReadRequest(String dn,
        LDAPSearchConstraints cons) {
        return parseReadRequest(dn, null, cons.getServerTimeLimit(),
            cons.getDereference(), cons.getMaxResults());
    }

    public static LDAPSearchRequest parseReadRequest(String dn,
        int timeLimit, int deref, int sizeLimit) {
        return parseReadRequest(dn, null, timeLimit, deref, sizeLimit);
    }

    public static LDAPSearchRequest parseReadRequest(String dn, String[] attrs,
        int timeLimit, int deref, int sizeLimit) {
        return parseSearchRequest(dn, LDAPConnection.SCOPE_BASE,
            "(objectclass=*)", attrs, false,
            timeLimit, deref, sizeLimit);
    }

    public static LDAPSearchRequest parseSearchRequest(String baseDN, int scope,
        String filter, String[] attrs, boolean attrsOnly,
        LDAPSearchConstraints cons) {
        return parseSearchRequest(baseDN, scope, filter, attrs, attrsOnly, cons.getServerTimeLimit(), 
        cons.getDereference(), cons.getMaxResults());
    }

    public static LDAPSearchRequest parseSearchRequest(String baseDN, int scope,
        String filter, String[] attrs, boolean attrsOnly) {
        return parseSearchRequest(baseDN, scope, filter, attrs, attrsOnly,
            DEFAULT_SERVER_TIME_LIMIT, DEFAULT_DEREFERENCE, DEFAULT_MAX_RESULT);
    }

    public static LDAPSearchRequest parseSearchRequest(String baseDN, int scope,
        String filter, String[] attrs, boolean attrsOnly, int timeLimit,
        int deref, int sizeLimit) {
        LinkedList bytesList = new LinkedList();
        byte[] tempBytes = null;
        int Length = 0;
        if (attrs != null) {
            for (int i = attrs.length - 1; i >= 0; i--) {
                Length += addOctetString(bytesList, attrs[i]);
                bytesList.addFirst(BERElement.OCTETSTRING_BYTES);
                Length++;
            }
        }
        tempBytes = getLengthBytes(Length);
        bytesList.addFirst(tempBytes);
        Length += tempBytes.length;
        bytesList.addFirst(BERElement.SEQUENCE_BYTES);
        Length++;
        JDAPFilter parsedFilter = JDAPFilter.getFilter(
            JDAPFilterOpers.convertLDAPv2Escape(filter));
        if (parsedFilter == null){
            throw new IllegalArgumentException("Bad search filter");
        }
        Length += parsedFilter.addLDAPFilter(bytesList);
        Length += addBoolean(bytesList, attrsOnly);
        bytesList.addFirst(BERElement.BOOLEAN_BYTES);
        Length++;
        Length += addInt(bytesList, timeLimit);
        bytesList.addFirst(BERElement.INTEGER_BYTES);
        Length++;
        Length += addInt(bytesList, sizeLimit);
        bytesList.addFirst(BERElement.INTEGER_BYTES);
        Length++;
        Length += addInt(bytesList, deref);
        bytesList.addFirst(BERElement.ENUMERATED_BYTES);
        Length++;
        Length += addInt(bytesList, scope);
        bytesList.addFirst(BERElement.ENUMERATED_BYTES);
        Length++;
        Length += addOctetString(bytesList, baseDN);
        bytesList.addFirst(BERElement.OCTETSTRING_BYTES);
        Length++;
        tempBytes = getLengthBytes(Length);        
        bytesList.addFirst(tempBytes);
        Length += tempBytes.length;
        byte[] tempTag = new byte[1];
        tempTag[0] =(byte) (BERElement.APPLICATION | BERElement.CONSTRUCTED
            | 3);
        bytesList.addFirst(tempTag);        
        Length++;
        return new LDAPSearchRequest(baseDN, scope, filter, attrs, attrsOnly,
            timeLimit, deref, sizeLimit, bytesList, Length);
    }

    public static int addOctetBytes(LinkedList bytesList, byte[] value) {
        int addedLength = 0;
        if (value != null) {
            bytesList.addFirst(value);
            addedLength += value.length;
        }
        byte[] tempBytes = getLengthBytes(addedLength);
        bytesList.addFirst(tempBytes);
        addedLength += tempBytes.length;
        return addedLength;
    }

    public static int addOctetString(LinkedList bytesList, String value) {
        int addedLength = 0;
        byte[] tempBytes = null;
        if (value != null) {
            try {
                tempBytes = value.getBytes("UTF8");
            } catch (Exception wontHappen) {}
            bytesList.addFirst(tempBytes);
            addedLength = tempBytes.length;
        }
        tempBytes = getLengthBytes(addedLength);
        bytesList.addFirst(tempBytes);
        addedLength += tempBytes.length;
        return addedLength;
    }

    public static int addInt(LinkedList bytesList, int value) {
        int addedLength = 0;
        byte[] tempBytes;
        tempBytes = getIntBytes(value);
        bytesList.addFirst(tempBytes);
        addedLength += tempBytes.length;
        tempBytes = getLengthBytes(tempBytes.length);
        bytesList.addFirst(tempBytes);
        addedLength += tempBytes.length;
        return addedLength;
    }

    public static int addBoolean(LinkedList bytesList, boolean value) {
        byte[] tempBytes;
        tempBytes = new byte[1];
        if (value) {            
            tempBytes[0] = (byte) 0xFF;
        } else {
            tempBytes[0] = (byte) 0x00;
        }       
        bytesList.addFirst(tempBytes);
        tempBytes = new byte[1];
        tempBytes[0] = (byte) 0x01;       
        bytesList.addFirst(tempBytes);
        return 2;        
    }

    public static byte[] getBoolean(boolean value) {
        byte[] buffer = new byte[1];
        if (value) {
            buffer[0] = (byte) 0xFF;      
        } else {
            buffer[0] = (byte) 0x00;
        }
        return buffer;
    }

    public static byte[] getIntBytes(int value) {
        if (value == 0) {
            byte[] buffer = new byte[1];
            buffer[0] = 0;
            return buffer;
        } else {
            int num_content_octets = 0;
            int binary_value = (value < 0 ? ((value * -1) -1) : value);
            int write_value = 0;
            do {
                write_value = write_value << 8;
                if (value < 0) {
                    write_value = write_value | ((binary_value ^ 0xFF) & 0xFF);
                } else {
                    write_value = write_value | (binary_value & 0xFF);
                }
                binary_value = binary_value >> 8;
                num_content_octets++;
            } while (binary_value > 0);
            byte[] buffer;
            int offset = 0;
            if ((value > 0) && ((write_value & 0x80) > 0)) {
                buffer = new byte[num_content_octets + 1];
                buffer[offset] = 0;
                offset++;
            } else {
                buffer = new byte[num_content_octets];
            }
            for (int i = 0; i < num_content_octets; i++) {
                buffer[offset] = (byte) (write_value & 0xFF);
                offset++;
                write_value = write_value >> 8;
            }
            return buffer;
       }
    }

    public static byte[] getLengthBytes(int num_content_octets) {
        if (num_content_octets <= 127) {
            /* Use short form */
            byte[] buffer = new byte[1];
            buffer[0] = (byte) (num_content_octets & 0xFF);
            return buffer;
        } else {
            /* Using long form:
             * Need to determine how many octets are required to
             * encode the length.
             */
            int num_length_octets = 0;
            int num = num_content_octets;
            while (num >= 0) {
                num_length_octets++;
                num = (num>>8);
                if (num <= 0)
                    break;
            }

            byte[] buffer = new byte[num_length_octets + 1];
            buffer[0] = (byte)(0x80 | num_length_octets);

            num = num_content_octets;
            for (int i = num_length_octets; i > 0; i--) {
                buffer[i] = (byte)(num & 0xFF);
                num = (num>>8);
            }
            return buffer;
        }
    }

}
