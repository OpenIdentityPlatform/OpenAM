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

import com.sun.identity.shared.ldap.client.opers.JDAPSearchResultReference;
import com.sun.identity.shared.ldap.ber.stream.BERElement;
import java.util.BitSet;
import java.util.LinkedList;

/**
 * An LDAPSearchResultReference object encapsulates a continuation
 * reference from a search operation.
 * 
 * @version 1.0
 */
public class LDAPSearchResultReference extends LDAPMessage {

    static final long serialVersionUID = -7816778029315223117L;

    /**
     * A list of LDAP URLs that are referred to.
     */
    private String m_URLs[] = null;

    public static final short URL = 2;
    protected short offsetIndex;
    
    /**
     * Constructor
     * 
     * @param msgid message identifier
     * @param resRef search result reference response
     * @param controls array of controls or null
     * @see com.sun.identity.shared.ldap.LDAPEntry
     */
    LDAPSearchResultReference(int msgid, JDAPSearchResultReference resRef, LDAPControl[]controls) {
        super(msgid, resRef, controls);    
        m_URLs = resRef.getUrls();
    }

    LDAPSearchResultReference(int msgid, int operType,
        int messageContentLength, byte[] content) {
        super(msgid, operType, messageContentLength, content);
        this.offsetIndex = URL;
    }

    protected synchronized void parseComponent(final short index) {
        int[] bytesProcessed = new int[1];
        bytesProcessed[0] = 0;
        switch (index) {
            case URL:
                if (offsetIndex != URL) {
                    return;
                }
                LinkedList url = new LinkedList();
                while ((messageContentLength > bytesProcessed[0]) ||
                    (messageContentLength == -1)) {
                    if (messageContentLength == -1) {
                        if (content[offset[0]] == BERElement.EOC) {
                            offset[0] += 2;
                            bytesProcessed[0] += 2;
                            break;
                        }
                    }
                    if (((int) content[offset[0]]) == BERElement.OCTETSTRING) {
                        offset[0]++;
                        bytesProcessed[0]++;
                        url.add(LDAPParameterParser.parseOctetString(content,
                            offset, bytesProcessed));
                    } else {
                        if (((int) content[offset[0]]) == 
                            (BERElement.OCTETSTRING |
                            BERElement.CONSTRUCTED)) {
                            offset[0]++;
                            bytesProcessed[0]++;
                            url.add(LDAPParameterParser.parseOctetStringList(
                                content, offset, bytesProcessed));
                        }
                    }
                }
                if (!url.isEmpty()) {
                    m_URLs = (String[]) url.toArray(new String[0]);
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

    public boolean equals(Object obj) {
        LDAPSearchResultReference msg = null;
        if (obj instanceof LDAPSearchResultReference) {
            msg = (LDAPSearchResultReference) obj;
        } else {
            return false;
        }
        boolean e = super.equals(msg);
        if (!e) {
            return false;
        }
        String[] thisURL = getUrls();
        String[] otherURL = msg.getUrls();
        if (thisURL != null) {
            if (otherURL != null) {
                if (thisURL.length != otherURL.length) {
                    return false;
                } else {
                    for (int i = 0; i < thisURL.length; i++) {
                        if (!thisURL[i].equals(otherURL[i])) {
                            return false;
                        }
                    }
                }
            } else {
                return false;
            }
        } else {
            if (otherURL != null) {
                return false;
            }
        }
        return true;
    }

    public LDAPControl[] getControls() {
        if (getProtocolOp() == null) {
            if (!controlsParsed) {
                if (messageContentLength == -1) {
                    while (offsetIndex != END) {
                        parseComponent(URL);
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

    /**
     * Returns a list of LDAP URLs that are referred to.
     * @return a list of URLs.
     */
    public String[] getUrls() {
        if (getProtocolOp() == null) {
            if (offsetIndex == URL) {
                parseComponent(URL);
            }
        }
        return m_URLs;
    }
    
    public int getMessageType() {
        return LDAPMessage.LDAP_SEARCH_RESULT_REFERENCE_MESSAGE;
    }

    public String getString() {
        StringBuffer s = new StringBuffer();
        s.append("SearchResultReference ");
        String[] urls = getUrls();
        if (urls != null) {
            for (int i = 0; i < urls.length; i++) {
                if (i != 0) {
                    s.append(",");
                }
                s.append(urls[i]);
            }
        }
        return s.toString();
    }
}
