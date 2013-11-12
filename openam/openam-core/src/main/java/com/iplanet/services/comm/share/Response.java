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
 * $Id: Response.java,v 1.2 2008/06/25 05:41:35 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted 2013 ForgeRock AS
 */

package com.iplanet.services.comm.share;

import com.sun.identity.shared.xml.XMLUtils;

/**
 * This <code>Response</code> class represents a response. The most important
 * information in this Response object is the content of this response. The
 * content in this Response object can be an arbitrary String. This makes it
 * possible that high level services and applications can define their own
 * response XML DTDs and then embed the corresponding XML document into this
 * Response object as its content.
 * 
 * @see com.iplanet.services.comm.share.ResponseSet
 */

public class Response {

    private String dtdID = null;

    private String responseContent = "";

    /*
     * Constructors
     */

    /**
     * Constructs an instance of Response class with the content of the Response.
     * The DTD ID needs to be set explicitly using the corresponding setter as
     * it is optional for the response.
     * 
     * @param content
     *            The content of this Response.
     */
    public Response(String content) {
        responseContent =  XMLUtils.removeInvalidXMLChars(content);
    }

    /*
     * This constructor is used by ResponseSetParser to reconstruct a Response
     * object.
     */
    Response() {
    }

    /**
     * Gets the ID of the DTD for the content of the Response
     * 
     * @return The ID of the DTD for the content of the Response.
     */
    public String getDtdID() {
        return dtdID;
    }

    /**
     * Gets the content of the Response.
     * 
     * @return The content of the Response.
     */
    public String getContent() {
        return responseContent;
    }

    /**
     * Sets the ID of the DTD for the content of the Response
     * 
     * @param id The ID of the DTD for the content of the Response.
     */
    public void setDtdID(String id) {
        dtdID = id;
    }

    /**
     * Sets the content of the Response.
     * 
     * @param content The content of the Response in String format.
     */
    public void setContent(String content) {
        responseContent = content;
    }
}
