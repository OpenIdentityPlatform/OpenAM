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
 * $Id: InteractionRedirectException.java,v 1.2 2008/06/25 05:47:18 qcheng Exp $
 *
 */

package com.sun.identity.liberty.ws.interaction;

/**
 * Class for exception thrown by <code>InteractionManager</code>, on the 
 * <code>WSC</code> side, to indicate that the User Agent is redirected to
 * <code>WSP</code>.
 *
 * @supported.all.api
 */
public class InteractionRedirectException extends InteractionException {

    private String messageID;

    /**
     * Constructor
     * @param messageID <code>messageID</code> of SOAP request message that
     *        caused this exception
     *
     * @supported.api
     */
    public InteractionRedirectException(String messageID) {
        super(messageID);
        this.messageID = messageID;
    }

    /**
     *
     * Gets <code>messageID</code> of SOAP request message that caused this
     * exception.  <code>WSC</code> could use this as a key to save any
     * additional information that it may want to lookup when user agent
     * is redirected  back after resource owner interactions. This would
     * be provided as a values of query parameter
     * <code>InteractionManager.REQUEST_ID</code>
     * when the user agent is redirected back to <code>WSC</code>.
     *
     * @return <code>messageID</code> of SOAP request message that caused this
     *                  exception
     * @supported.api
     */
    public String getMessageID() {
        return messageID;
    }

}

