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
 * $Id: SOAPFaultException.java,v 1.2 2008/06/25 05:47:23 qcheng Exp $
 *
 */


package com.sun.identity.liberty.ws.soapbinding; 


/**
 * The <code>SOAPFaultException</code> class represents a SOAP Fault while
 * processing SOAP request.
 *
 * @supported.all.api
 */
public class SOAPFaultException extends Exception {

    private Message message = null;

    /**
     * Constructor.
     *
     * @param message a <code>Message</code> containing <code>SOAPFault</code>.
     */
    public SOAPFaultException(Message message) {
        this.message = message;
    }


    /**
     * Returns <code>Message</code> containing <code>SOAPFault</code>.
     *
     * @return the <code>Message</code> containing <code>SOAPFault</code>.
     */
    public Message getSOAPFaultMessage() {
        return message;
    }
}
