/**
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2013 ForgeRock AS. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 */
package org.forgerock.identity.openam.xacml.v3.resources;

import com.sun.identity.shared.debug.Debug;
import org.forgerock.identity.openam.xacml.v3.commons.CommonType;
import org.forgerock.identity.openam.xacml.v3.commons.ContentType;
import org.forgerock.identity.openam.xacml.v3.model.XACML3Constants;
import org.forgerock.identity.openam.xacml.v3.model.XACMLRequestInformation;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * XACML Ping Resource
 * <p/>
 * Ping Resource, intent to measure and ensure Handler layer
 * active and available for testing during InterOp as well
 * to verify connectivity between PEP and PDP.
 *
 * @author Jeff.Schenk@forgerock.com
 */
public class XacmlPingResource implements XACML3Constants {
    /**
     * Define our Static resource Bundle for our debugger.
     */
    private static Debug debug = Debug.getInstance("amXACML");

    /**
     * Creates Ping Document Content.
     *
     * @param xacmlRequestInformation
     * @param request
     * @return String -- Containing Response in requested ContentType.
     * @throws javax.servlet.ServletException
     * @throws java.io.IOException
     */
    public static String getPing(XACMLRequestInformation xacmlRequestInformation, HttpServletRequest request) throws
            ServletException,
            JSONException, IOException {
        String classMethod = "XacmlPingResource:getPing";
        debug.error(classMethod + " processing URI:[" + request.getRequestURI() + "], Content Type:[" + request.getContentType() + "]");
        StringBuilder sb = new StringBuilder();
        // ************************************************************
        // Determine how to respond based upon Content Type.
        if ( (xacmlRequestInformation.getContentType().equals(ContentType.NONE.applicationType())) ||
                (xacmlRequestInformation.getContentType().commonType() == CommonType.JSON) ) {
            sb.append(getJSONPingDocument(xacmlRequestInformation));
        } else {
            // Formulate the Home Document for XML Consumption, based upon Atom - RFC4287
            sb.append(getXMLPingDocument(xacmlRequestInformation));
        } // End of Check for Content Type.

        // *******************************************************
        // Render with XML or JSON content.
        return sb.toString();
    }

    /**
     * Provide an XML Rendered PING Document.
     *
     * @param xacmlRequestInformation
     * @return
     */
    public static String getXMLPingDocument(XACMLRequestInformation xacmlRequestInformation) {
        StringBuilder sb = new StringBuilder();
        // Formulate the Home Document for XML Consumption, based upon Atom - RFC4287
        sb.append(XML_HEADER);
        // TODO...
        return sb.toString();
    }


    /**
     * Provide an JSON Rendered PING Document.
     *
     * @return JSONObject
     * @throws org.json.JSONException
     */
    public static JSONObject getJSONPingDocument(XACMLRequestInformation xacmlRequestInformation) throws
            JSONException {
        JSONObject ping = new JSONObject();
        JSONArray pingArray = new JSONArray();

        // TODO

        ping.append("ping", pingArray);
        return ping;
    }

}
