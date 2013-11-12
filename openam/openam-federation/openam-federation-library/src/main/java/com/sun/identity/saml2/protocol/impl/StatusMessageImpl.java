/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: StatusMessageImpl.java,v 1.2 2008/06/25 05:48:01 qcheng Exp $
 *
 */


package com.sun.identity.saml2.protocol.impl;

import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.protocol.StatusMessage;

/**
 * This class defines methods for adding <code>StatusMessage</code> element.
 */

public class StatusMessageImpl implements StatusMessage {
    
    private String messageValue = null;
    
    /**
     * Constructs the <code>StatusMessage</code> Object.
     *
     * @param value A String <code>StatusMessage</code> value
     */
    public StatusMessageImpl(String value) {
        this.messageValue = value;
    }
    
    /**
     * Returns the <code>StatusMessage</code> value.
     *
     * @return A String value of the <code>StatusMessage</code>
     */
    public java.lang.String getValue() {
        return messageValue;
    }
    
    /**
     * Returns the <code>StatusMessage</code> in an XML document String format
     * based on the <code>StatusMessage</code> schema described above.
     *
     * @return An XML String representing the <code>StatusMessage</code>.
     * @throws SAML2Exception if some error occurs during conversion to
     *         <code>String</code>.
     */
    public String toXMLString() throws SAML2Exception {
        return toXMLString(true,false);
    }
    
    /**
     * Returns the <code>StatusMessage</code> in an XML document String format
     * based on the <code>StatusMessage</code> schema described above.
     *
     * @param includeNSPrefix Determines whether or not the namespace qualifier
     * is prepended to the Element when converted
     * @param declareNS Determines whether or not the namespace is declared
     *        within the Element.
     * @return A XML String representing the <code>StatusMessage</code>.
     * @throws SAML2Exception if some error occurs during conversion to
     *         <code>String</code>.
     */
    public String toXMLString(boolean includeNSPrefix,
    boolean declareNS) throws SAML2Exception {
        String xmlStr = null;
        if ((messageValue != null) && (messageValue.length() != 0)) {
            StringBuffer xmlString = new StringBuffer(500);
            xmlString.append(SAML2Constants.START_TAG);
            if (includeNSPrefix) {
                xmlString.append(SAML2Constants.PROTOCOL_PREFIX);
            }
            xmlString.append(SAML2Constants.STATUS_MESSAGE);
            if (declareNS) {
                xmlString.append(SAML2Constants.PROTOCOL_DECLARE_STR);
            }
            xmlString.append(SAML2Constants.END_TAG);
            
            xmlString.append(SAML2Constants.NEWLINE)
            .append(messageValue);
            
            xmlString.append(SAML2Constants.NEWLINE)
            .append(SAML2Constants.SAML2_END_TAG)
            .append(SAML2Constants.STATUS_MESSAGE)
            .append(SAML2Constants.END_TAG);
            
            xmlStr = xmlString.toString();
        }
        return xmlStr;
    }
}
