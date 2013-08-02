/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2005 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: Request.java,v 1.2 2008/06/25 05:41:35 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted 2013 ForgeRock AS
 */

package com.iplanet.services.comm.share;

import com.sun.identity.shared.xml.XMLUtils;

/**
 * This <code>Request</code> class represents a request. The most important
 * information in this Request object is the content of this request. The
 * content in this Request object can be an arbitrary String. This makes it
 * possible that high level services and applications can define their own
 * request XML DTDs and then embed the corresponding XML document into this
 * Request object as its content.
 * 
 * @see com.iplanet.services.comm.share.RequestSet
 */

public class Request {

    private String dtdID = null;

    private String sessionID = null;

    private String requestContent = "";

    /**
     * Contructs an instance of Request class with the content of the Request.
     * The session ID and DTD ID need to be set explicitly using corresponding
     * setters as those are optional for the request.
     * 
     * @param content
     *            The content of this Request.
     */
    public Request(String content) {
    	requestContent =  XMLUtils.removeInvalidXMLChars(content);
    }

    /*
     * This constructor is used by RequestSetParser to reconstruct a Request
     * object.
     */
    Request() {
    }

    /**
     * Sets the ID of the DTD for the content of the Request
     * 
     * @param id
     *            The ID of the DTD for the content of the Request.
     */
    public void setDtdID(String id) {
        dtdID = id;
    }

    /**
     * Gets the ID of the DTD for the content of the Request
     * 
     * @return The ID of the DTD for the content of the Request.
     */
    public String getDtdID() {
        return dtdID;
    }

    /**
     * Sets the session ID of the request.
     * 
     * @param id
     *            A string representing the session ID of the request.
     */
    public void setSessionID(String id) {
        sessionID = id;
    }

    /**
     * Gets the session ID of the request.
     * 
     * @return The session ID of the request.
     */
    public String getSessionID() {
        return sessionID;
    }

    /**
     * Gets the content of the Request.
     * 
     * @return The content of the Request.
     */
    public String getContent() {
        return requestContent;
    }

    /**
     * Sets the content of the Request.
     * 
     * @param content
     *            The content of the Request in String format.
     */
    public void setContent(String content) {
        requestContent = content;
    }
}
