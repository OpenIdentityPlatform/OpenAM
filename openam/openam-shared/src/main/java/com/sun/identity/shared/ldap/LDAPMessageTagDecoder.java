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
 * $Id: LDAPMessageTagDecoder.java,v 1.1 2009/11/20 23:52:58 ww203982 Exp $
 */
package com.sun.identity.shared.ldap;

import com.sun.identity.shared.ldap.client.opers.JDAPProtocolOp;
import java.io.IOException;
import java.io.InputStream;

/**
 * @deprecated As of ForgeRock OpenAM 10.
 */
public class LDAPMessageTagDecoder {

    public static LDAPMessage decodeResponseMessageTag(int tag, int msgid,
        int messageContentLength, byte[] content) throws LDAPException {
        LDAPMessage message = null;
        switch (tag & 0x1f) {
            case JDAPProtocolOp.BIND_RESPONSE:
                /* 0x61 [APPLICATION 1] Bind Response */
                message = new LDAPBindResponse(msgid,
                    JDAPProtocolOp.BIND_RESPONSE, messageContentLength,
                    content);
                break;
                /* If doing search without bind,
                 * x500.arc.nasa.gov returns tag SEARCH_REQUEST tag
                 * in SEARCH_RESULT.
                 */
            case JDAPProtocolOp.SEARCH_REQUEST:
            case JDAPProtocolOp.SEARCH_RESULT:  
                /* 0x65 [APPLICATION 5] Search Result */
                message = new LDAPResponse(msgid,
                    JDAPProtocolOp.SEARCH_RESULT, messageContentLength,
                    content);
                break;
            case JDAPProtocolOp.MODIFY_RESPONSE:  
                /* 0x67 [APPLICATION 7] Modify Response */
                message = new LDAPResponse(msgid,
                    JDAPProtocolOp.MODIFY_RESPONSE, messageContentLength,
                    content);
                break;
            case JDAPProtocolOp.ADD_RESPONSE:  
                /* 0x69 [APPLICATION 9] Add Response */
                message = new LDAPResponse(msgid,
                    JDAPProtocolOp.ADD_RESPONSE, messageContentLength,
                    content);
                break;
            case JDAPProtocolOp.DEL_RESPONSE:  
                /* 0x6b [APPLICATION 11] Del Response */
                message = new LDAPResponse(msgid,
                    JDAPProtocolOp.DEL_RESPONSE, messageContentLength,
                    content);
                break;
            case JDAPProtocolOp.MODIFY_RDN_RESPONSE:  
                /* 0x6d [APPLICATION 13] ModifyRDN Response */
                message = new LDAPResponse(msgid,
                    JDAPProtocolOp.MODIFY_RDN_RESPONSE, messageContentLength,
                    content);
                break;
            case JDAPProtocolOp.COMPARE_RESPONSE:
                /* 0x6f [APPLICATION 15] Compare Response */
                message = new LDAPResponse(msgid,
                    JDAPProtocolOp.COMPARE_RESPONSE, messageContentLength,
                    content);
                break;
            case JDAPProtocolOp.EXTENDED_RESPONSE:  
                /* 0x78 [APPLICATION 23] Extended Response */
                message = new LDAPExtendedResponse(msgid,
                    JDAPProtocolOp.EXTENDED_RESPONSE, messageContentLength,
                    content);
                break;
            case JDAPProtocolOp.SEARCH_RESPONSE:  
                /* 0x64 [APPLICATION 4] Search Response */
                message = new LDAPSearchResult(msgid,
                    JDAPProtocolOp.SEARCH_RESPONSE, messageContentLength,
                    content);
                break;
            case JDAPProtocolOp.SEARCH_RESULT_REFERENCE:  
                /* 0x73 [APPLICATION 19] SearchResultReference */
                message = new LDAPSearchResultReference(msgid,
                    JDAPProtocolOp.SEARCH_RESULT_REFERENCE,
                     messageContentLength, content);
                break;
            default:
                throw new LDAPException("invalid tag " + tag);
        }
        return message;
    }

}
