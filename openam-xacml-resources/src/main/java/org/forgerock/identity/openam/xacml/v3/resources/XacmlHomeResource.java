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
 * XACML Resource for Home Documents
 * <p/>
 * Provides main end-point for all XACML Home requests.
 *
 * @author Jeff.Schenk@forgerock.com
 */
public class XacmlHomeResource implements XACML3Constants {
    /**
     * Define our Static resource Bundle for our debugger.
     */
    private static Debug debug = Debug.getInstance("amXACML");

    /**
     * Do not allow instantiation, only static methods.
     */
    private XacmlHomeResource() {
    }

    /**
     * Creates Home Document Content providing hints.
     *
     * @param xacmlRequestInformation
     * @param request
     * @return String -- Containing Response in requested ContentType.
     * @throws javax.servlet.ServletException
     * @throws java.io.IOException
     */
    public static String getHome(XACMLRequestInformation xacmlRequestInformation, HttpServletRequest request) throws ServletException,
            JSONException, IOException {
        String classMethod = "XacmlHomeResource:getHome";
        debug.error(classMethod + " processing URI:[" + request.getRequestURI() + "], Content Type:[" + request.getContentType() + "]");
        StringBuilder sb = new StringBuilder();
        // ************************************************************
        // Determine how to respond based upon Content Type.
        if ( (xacmlRequestInformation.getContentType().equals(ContentType.NONE.applicationType())) ||
             (xacmlRequestInformation.getContentType().commonType() == CommonType.JSON) ) {
                sb.append(getJSONHomeDocument(xacmlRequestInformation));
        } else {
                // Formulate the Home Document for XML Consumption, based upon Atom - RFC4287
                sb.append(getXMLHomeDocument(xacmlRequestInformation));
        } // End of Check for Content Type.

        // *******************************************************
        // Render with XML or JSON content.
        return sb.toString();
    }

    public static String getXMLHomeDocument(XACMLRequestInformation xacmlRequestInformation) {
        StringBuilder sb = new StringBuilder();
        // Formulate the Home Document for XML Consumption, based upon Atom - RFC4287
        sb.append(XML_HEADER);
        sb.append("<resources xmlns=\042http://ietf.org/ns/home-documents\042\n");
        sb.append("xmlns:atom=\042http://www.w3.org/2005/Atom\042>\n");
        sb.append("<resource rel=\042http://docs.oasis-open.org/ns/xacml/relation/pdp\042>");
        sb.append("<atom:link href=\042"+PDP_AUTHORIZATION_ENDPOINT+"\042/>");
        sb.append("</resource>");
        sb.append("</resources>");
        return sb.toString();
    }


    /**
     * Formulate our Home Document.
     *
     * @return JSONObject
     * @throws org.json.JSONException
     */
    public static JSONObject getJSONHomeDocument(XACMLRequestInformation xacmlRequestInformation) throws JSONException {
        JSONObject resources = new JSONObject();
        JSONArray resourceArray = new JSONArray();

        JSONObject resource_1 = new JSONObject();
        resource_1.append("href", PDP_AUTHORIZATION_ENDPOINT);
        JSONObject resource_1A = new JSONObject();
        resource_1A.append(xacmlRequestInformation.getXacmlHome(), resource_1);

        JSONObject resource_2 = new JSONObject();
        resource_2.append("href-template", "/xacml/");
        resource_2.append("hints", getHomeHints(xacmlRequestInformation));
        JSONObject resource_2A = new JSONObject();
        resource_2A.append(xacmlRequestInformation.getXacmlHome(), resource_2);

        resourceArray.put(resource_1A);
        resourceArray.put(resource_2A);


        resources.append("resources", resourceArray);
        return resources;
    }

    /**
     * Formulate our Hints for our REST EndPoint to allow Discovery.
     * Per Internet Draft: draft-nottingham-json-home-02
     *
     * @return JSONObject - Containing Hints for our Home Application.
     * @throws org.json.JSONException
     */
    private static JSONObject getHomeHints(XACMLRequestInformation xacmlRequestInformation) throws JSONException {
        JSONObject hints = new JSONObject();

        /**
         * Hints the HTTP methods that the current client will be able to use to
         * interact with the resource; equivalent to the Allow HTTP response
         * header.
         *
         * Content MUST be an array of strings, containing HTTP methods.
         */
        JSONArray allow = new JSONArray();
        allow.put("GET");
        allow.put("POST");
        hints.append("allow", allow);

        /**
         * Hints the representation types that the resource produces and
         * consumes, using the GET and PUT methods respectively, subject to the
         * ’allow’ hint.
         *
         * Content MUST be an array of strings, containing media types.
         */
        JSONArray representations = new JSONArray();
        representations.put(ContentType.JSON.applicationType());
        representations.put(ContentType.XML.applicationType());
        representations.put(ContentType.XACML_PLUS_JSON.applicationType());
        representations.put(ContentType.XACML_PLUS_XML.applicationType());
        hints.append("representations", representations);

        /**
         * Hints the POST request formats accepted by the resource for this
         * client.
         *
         * Content MUST be an array of strings, containing media types.
         *
         * When this hint is present, "POST" SHOULD be listed in the "allow"
         * hint.
         */
        JSONArray accept_post = new JSONArray();
        accept_post.put(ContentType.JSON.applicationType());
        accept_post.put(ContentType.XML.applicationType());
        accept_post.put(ContentType.XACML_PLUS_XML.applicationType());
        hints.append("accept-post", accept_post);

        /**
         * Return our Hints for consumption by requester.
         */
        return hints;
    }

}

