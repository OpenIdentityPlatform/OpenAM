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

import com.sun.identity.shared.ldap.client.opers.JDAPSearchResponse;
import com.sun.identity.shared.ldap.ber.stream.BERElement;
import java.util.BitSet;
import java.util.LinkedList;
import java.util.Enumeration;

/**
 * A LDAPSearchResult object encapsulates a single search result.
 *
 * @version 1.0
 */
public class LDAPSearchResult extends LDAPMessage {

    static final long serialVersionUID = 36890821518462301L;

    /**
     * LDAPEntry 
     */
    private LDAPEntry m_entry;
    
    /**
     * Constructor
     * 
     * @param msgid message identifier
     * @param rsp search operation response
     * @param controls array of controls or null
     * @see com.sun.identity.shared.ldap.LDAPEntry
     */
    LDAPSearchResult(int msgid, JDAPSearchResponse rsp, LDAPControl[]controls) {
        super(msgid, rsp, controls);
    }
    
    LDAPSearchResult(int msgid, int operType, int messageContentLength,
        byte[] content) {
        super(msgid, operType, messageContentLength, content);
    }

    public boolean equals(Object obj) {
        LDAPSearchResult msg = null;
        if (obj instanceof LDAPSearchResult) {
            msg = (LDAPSearchResult) obj;
        } else {
            return false;
        }
        boolean e = super.equals(msg);
        if (!e) {
            return false;
        }
        LDAPEntry thisEntry = getEntry();
        LDAPEntry otherEntry = msg.getEntry();
        if (thisEntry != null) {
            if (otherEntry != null) {
                String thisString = thisEntry.toString();
                String otherString = otherEntry.toString();
                if (!thisString.equals(otherString)) {
                    return false; 
                }
            } else {
                return false;
            }
        } else {
            if (otherEntry != null) {
                return false;
            }
        }
        return true;
    }

    public LDAPControl[] getControls() {
        if (getProtocolOp() == null) {
            if (!controlsParsed) {
                if (m_entry == null) {
                    m_entry = new LDAPEntry(content, offset);
                }
                if (messageContentLength == -1) {
                    m_entry.parseComponent(LDAPEntry.ATTR_SET);
                }
                parseControls();
                content = null;
            }
        }
        return m_controls;
    }

    /**
     * Returns the entry of a server search response.
     * @return an entry returned by the server in response to a search
     * request.
     * @see com.sun.identity.shared.ldap.LDAPEntry
     */
    public LDAPEntry getEntry() {
        if (m_entry == null) {
            if (getProtocolOp() == null) {
                m_entry = new LDAPEntry(content, offset);
                if (messageContentLength >= content.length) {
                    controlsParsed = true;
                    content = null;
                }
            } else {
                JDAPSearchResponse rsp = (JDAPSearchResponse)getProtocolOp();
                LDAPAttribute[] lattrs = rsp.getAttributes();
                LDAPAttributeSet attrs;
                if ( lattrs != null ) {
                    attrs = new LDAPAttributeSet( lattrs );
                } else {
                    attrs = new LDAPAttributeSet();
                }
                String dn = rsp.getObjectName();
                m_entry = new LDAPEntry( dn, attrs);
            }
        }
        return m_entry;
    }

    public int getMessageType() {
        return LDAPMessage.LDAP_SEARCH_RESULT_MESSAGE;
    }

    protected String getString() {
        StringBuffer s = new StringBuffer();        
        s.append("SearchResponse {entry='");
        LDAPEntry entry = getEntry();
        if (entry != null) {
            s.append(entry.getDN());
        }
        s.append("', attributes='");
        if (entry != null) {
            LDAPAttributeSet set = entry.getAttributeSet();
            if (set != null) {
                for (Enumeration attrs = set.getAttributes();
                    attrs.hasMoreElements();) {
                    LDAPAttribute attr = (LDAPAttribute) attrs.nextElement();
                    s.append(attr.toString());
                    if (attrs.hasMoreElements()) {
                        s.append(",");
                    }
                }
            }
        }
        s.append("'}");
        return s.toString();
    }
}
