package org.forgerock.openam.xacml.v3.resources;

import com.sun.identity.shared.debug.Debug;

import org.forgerock.openam.xacml.v3.model.ContentType;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

/**
 * XACML Resource for Home Documents
 * <p/>
 * Provides main end-point for all XACML Home requests.
 *
 * @author Jeff.Schenk@forgerock.com
 */
public class XACML3HomeResource extends XACML3Resource {
    /**
     * Define our Static resource Bundle for our debugger.
     */
    private static Debug DEBUG = Debug.getInstance("amXACML");

    /**
     * Do not allow instantiation, only static methods.
     */
    private XACML3HomeResource() {
    }

    /**
     * Creates Home Document Content providing hints.
     *
     * @return String -- Containing Response in requested ContentType.
     * @throws javax.servlet.ServletException
     * @throws java.io.IOException
     */
    public static String getHome() throws JSONException, IOException {
        String classMethod = "XacmlHomeResource:getHome";
        StringBuilder sb = new StringBuilder();
        // ************************************************************
        // Determine how to respond based upon Content Type.

            sb.append(getJSONHomeDocument());

            // Formulate the Home Document for XML Consumption, based upon Atom - RFC4287
            sb.append(getXMLHomeDocument());

        // *******************************************************
        // Render with XML or JSON content.
        return sb.toString();
    }

    /**
     * Home Document
     * XML Home Document using ATOM RFC4287
     * @return
     */
    public static String getXMLHomeDocument() {
        StringBuilder sb = new StringBuilder();
        // Formulate the Home Document for XML Consumption, based upon Atom - RFC4287
        sb.append(XML_HEADER);
        sb.append("<resources xmlns=\042http://ietf.org/ns/home-documents\042\n");
        sb.append("xmlns:atom=\042http://www.w3.org/2005/Atom\042>\n");
        sb.append("<resource rel=\042http://docs.oasis-open.org/ns/xacml/relation/pdp\042>");
        sb.append("<atom:link href=\042"+PDP_ENDPOINT+"\042/>");
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
    public static JSONObject getJSONHomeDocument() throws JSONException {
        JSONObject resources = new JSONObject();
        JSONArray resourceArray = new JSONArray();

        JSONObject resource_1 = new JSONObject();
        resource_1.append("href", PDP_ENDPOINT);
        JSONObject resource_1A = new JSONObject();

        // TODO :: Fix
        //resource_1A.append(xacmlRequestInformation.getXacmlHome(), resource_1);

        JSONObject resource_2 = new JSONObject();
        resource_2.append("href-template", "/xacml/");
        resource_2.append("hints", getHomeHints());
        JSONObject resource_2A = new JSONObject();

        // TODO :: Fix
        //resource_2A.append(xacmlRequestInformation.getXacmlHome(), resource_2);

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
    private static JSONObject getHomeHints() throws JSONException {
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
        representations.put(ContentType.JSON.getApplicationType());
        representations.put(ContentType.XML.getApplicationType());
        representations.put(ContentType.XACML_PLUS_JSON.getApplicationType());
        representations.put(ContentType.XACML_PLUS_XML.getApplicationType());
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
        accept_post.put(ContentType.JSON.getApplicationType());
        accept_post.put(ContentType.XML.getApplicationType());
        accept_post.put(ContentType.XACML_PLUS_XML.getApplicationType());
        hints.append("accept-post", accept_post);

        /**
         * Return our Hints for consumption by requester.
         */
        return hints;
    }

}
