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
package org.forgerock.identity.openam.xacml.v3.services;

import com.sun.identity.common.SystemConfigurationUtil;

import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.xacml.client.XACMLRequestProcessor;
import com.sun.identity.xacml.common.XACMLException;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;

import org.forgerock.identity.openam.xacml.v3.commons.*;
import org.forgerock.identity.openam.xacml.v3.model.AuthenticationDigest;
import org.forgerock.identity.openam.xacml.v3.model.XACML3Constants;
import org.forgerock.identity.openam.xacml.v3.model.XACMLRequestInformation;
import org.forgerock.identity.openam.xacml.v3.resources.XacmlHomeResource;
import org.forgerock.identity.openam.xacml.v3.resources.XacmlPDPResource;

import org.forgerock.identity.openam.xacml.v3.resources.XacmlPingResource;
import org.json.JSONException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.XMLConstants;
import javax.xml.parsers.*;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.xpath.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * XACML v3 Resource Router
 * <p/>
 * Provides main end-point for all XACML v3.0 requests,
 * either XML or JSON based over HTTP/HTTPS REST based protocol flow.
 * <p/>
 * This ForgeRock developed XACML Resource Router complies with the following OASIS Specifications:
 * <ul>
 * <li>xacml-3.0-core-spec-cs-01-en.pdf</li>
 * <li>xacml-rest-v1.0-csprd01.pdf</li>
 * <li>xacml-json-http-v1-1.0-wd09.doc</li>
 * <li>...</li>
 * </ul>
 * <p/>
 * <b>The following XACML v3 End Points are currently viable:</b>
 * <table>
 * <tr><th>Method</th><th>XACML Path</th><th>Description</th></tr>
 * <tr><td>GET</td><td><ul><li>&#47;openam&#47;xacml&#47;</li></ul></td><td><em>Default, Provides Home Document</em></td></tr>
 * <tr><td>GET</td><td><ul><li>&#47;openam&#47;xacml&#47;home&#47;</li></ul></td><td><em>Provides Home Document</em></td></tr>
 * <tr><td>GET</td><td><ul><li>&#47;openam&#47;xacml&#47;status&#47;</li></ul></td><td><em>Provides Status and Home
 * Document</em></td></tr>
 * <tr><td>GET</td><td><ul><li>&#47;openam&#47;xacml&#47;ping&#47;</li></ul></td><td><em>Provides
 * Roundtrip from Ping Request to Ping Response.  Ping Response will always be in the form of a Decision Type of
 * INDETERMINATE to conform to a standard XACML Decision Type. [ForgeRockOnly]
 * </em></td></tr>
 * <p/>
 * <tr><td>POST</td><td><ul><li>&#47;openam&#47;xacml&#47;</li></ul></td><td><em>Default, Request from PEP</em></td></tr>
 * <tr><td>POST</td><td><ul><li>&#47;openam&#47;xacml&#47;pdp&#47;</li></ul></td><td><em>Request from PEP</em></td></tr>
 * </table>
 * <b><i>Future intended EndPoints which are not Implemented yet:</i></b>
 * <table>
 * <tr><th>Method</th><th>XACML Path</th><th>Description</th></tr>
 * <p/>
 * <tr><td>GET
 * </td><td><ul><li>&#47;openam&#47;xacml&#47;pip&#47;&lt;query&gt;</li></ul></td><td><em>Policy Information Point
 * Query</em></td></tr>
 * <tr><td>GET &amp; POST
 * </td><td><ul><li>&#47;openam&#47;xacml&#47;pap&#47;*</li></ul></td><td><em>Policy Administration Point</em></td></tr>
 * <tr><td>POST
 * </td><td><ul><li>&#47;openam&#47;xacml&#47;pap&#47;import&#47</li></ul></td><td><em>PAP Import Policy</em></td></tr>
 * <tr><td>POST
 * </td><td><ul><li>&#47;openam&#47;xacml&#47;pap&#47;export&#47</li></ul></td><td><em>PAP Export Policy</em></td></tr>
 * </table>
 *
 * WWW-Authentication via Digest definition can be found here:
 * http://en.wikipedia.org/wiki/Digest_Access_Authentication#Alternative_authentication_protocols
 *
 * @author Jeff.Schenk@forgerock.com
 */
public class XacmlContentHandlerService extends HttpServlet implements XACML3Constants {
    /**
     * Initialize our Resource Bundle.
     */
    protected static final String RESOURCE_BUNDLE_NAME = "amXACML";
    protected static ResourceBundle resourceBundle =
            com.sun.identity.shared.locale.Locale.getInstallResourceBundle(RESOURCE_BUNDLE_NAME);
    /**
     * Attribute that specifies maximum content length for SAML request in
     * <code>AMConfig.properties</code> file.
     */
    public static final String HTTP_MAX_CONTENT_LENGTH =
            "com.sun.identity.xacml.request.maxContentLength";

    /**
     * Default maximum content length is set to 16k.
     */
    public static final int defaultMaxLength = 16384;

    /**
     * Default maximum content length in string format.
     */
    public static final String DEFAULT_CONTENT_LENGTH =
            String.valueOf(defaultMaxLength);

    private static int maxContentLength = 0;

    /**
     * Define our Static resource Bundle for our debugger.
     */
    private static Debug debug;

    /**
     * Defined and established Handlers.
     */
    private static HashMap handlers = new HashMap();

    /**
     * Preserve our Servlet Context PlaceHolder,
     * for referencing Artifacts.
     */
    private static ServletContext servletCtx;

    /**
     * Establish our Core Request Processor to handle
     * all assertions and responses.
     */
    private static XACMLRequestProcessor xacmlRequestProcessor;

    /**
     * XACML Schemata for Validation.
     */
    private static Schema xacmlSchema;

    /**
     * Digest Authentication Objects.
     */
    private static String nonce;
    private static ScheduledExecutorService nonceRefreshExecutor;

    /**
     * Initialize our Servlet/Restlet Request Handler for All
     * XACML v3 Requests.
     *
     * @param config
     * @throws ServletException
     */
    public void init(ServletConfig config) throws ServletException {
        // ******************************************************
        // Acquire our Logging Interface.
        debug = Debug.getInstance("amXACML");
        // ******************************************************
        // Acquire Servlet Context and XACML Request Processor.
        servletCtx = config.getServletContext();
        try {
            xacmlRequestProcessor = XACMLRequestProcessor.getInstance();
        } catch (XACMLException xacmlException) {
            debug.error("Unable to obtain Reference to XACMLRequestProcessor for Core Functionality, unable to process XACML Requests.");
            xacmlRequestProcessor = null;
        }
        // ***************************************************
        // Acquire MaxContent Length
        try {
            maxContentLength = Integer.parseInt(SystemConfigurationUtil.
                    getProperty(HTTP_MAX_CONTENT_LENGTH, DEFAULT_CONTENT_LENGTH));
        } catch (NumberFormatException ne) {
            debug.error("Wrong format of XACML request max content "
                    + "length. Using Default Value.");
            maxContentLength = defaultMaxLength;
        }
        // ***************************************************
        // Get Schema for Validation.
        try {
            SchemaFactory constraintFactory =
                    SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            // Create Streams for every applicable schema.
            InputStream xmlCoreSchemaResourceContentStream = XACML3Utils.getResourceContentStream
                    (xmlCoreSchemaResourceName);
            InputStream resourceContentStream = XACML3Utils.getResourceContentStream(xacmlCoreSchemaResourceName);
            // Create the schema object from our Input Source Streams.
            if ((xmlCoreSchemaResourceContentStream != null) && (resourceContentStream != null)) {
                xacmlSchema = constraintFactory.newSchema(new StreamSource[]{new StreamSource
                        (xmlCoreSchemaResourceContentStream), new StreamSource(resourceContentStream)});
                xmlCoreSchemaResourceContentStream.close();
                resourceContentStream.close();
            }
        } catch (SAXException se) {
            debug.error("SAX Exception obtaining XACML Schema for Validation,", se);
        } catch (IOException ioe) {
            debug.error("IO Exception obtaining XACML Schema for Validation,", ioe);
        }
        // ***************************************************
        // Ensure we are ok and have necessary assets to run.
        if (xacmlSchema != null) {
            debug.error("Initialization of XACML Content Resource Router, Server Information: " + servletCtx.getServerInfo());
        }
        // ***************************************************
        // Initialize our Authentication Digest Thread.
        nonce = calculateNonce();

        nonceRefreshExecutor = Executors.newScheduledThreadPool(1);

        nonceRefreshExecutor.scheduleAtFixedRate(new Runnable() {

            public void run() {
                nonce = calculateNonce();
            }
        }, 1, 1, TimeUnit.MINUTES);

        // *****************************************************
        // Allow Parent to initialize as well.
        super.init(config);
    }

    // ******************************************************************************************************
    // Servlet/Restlet Processing Methods
    // ******************************************************************************************************

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "This ForgeRock OpenAM XACML v3 Servlet/Restlet Implementation, Standards per OASIS, 2013.";
    }

    /**
     * Handles the HTTP <code>GET</code> method XACML REST Request.
     *
     * @param request
     * @param response
     * @throws ServletException
     * @throws java.io.IOException
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        /**
         * Process our GET Method.
         */
        processRequest(request, response);

        /**
         * Id: urn:oasis:names:tc:xacml:3.0:profile:rest:assertion:home:status
         ￼
         Normative Source
         ￼GET on the home location MUST return status code 200
         ￼
         Target
          Response to GET request on the home location
         ￼
         Predicate
         ￼ The HTTP status code in the [response] is 200
         ￼
         Prescription Level
         ￼mandatory
         */


        /**
         * Id ￼
         urn:oasis:names:tc:xacml:3.0:profile:rest:assertion:home:body

         Normative Source￼
         GET on the home location MUST return a home document
         ￼
         Target￼
         Response to GET request on the home location
         ￼
         Predicate
         The HTTP body in the [response] follows the home document schema
         [HomeDocument]
         ￼
         Prescription Level
         mandatory
         */


        /**
         * Id
           urn:oasis:names:tc:xacml:3.0:profile:rest:assertion:home:pdp
         ￼
         Normative Source
         The XACML entry point representation SHOULD contain a link to the PDP
         ￼
         Target
         Response to GET request on the home location
         ￼
         Predicate
         The home document in the [response] body contains a resource with link relation
         http://docs.oasis-open.org/ns/xacml/relation/pdp and a valid URL
         ￼
         Prescription Level
         mandatory
         */


    }

    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request  the <code>HttpServletRequest</code> object.
     * @param response the <code>HttpServletResponse</code> object.
     * @throws ServletException    if the request could not be
     *                             handled.
     * @throws java.io.IOException if an input or output error occurs.
     */
    @Override
    public void doPost(
            HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {
        /**
         * Process our POST Method.
         */
        processRequest(request, response);

        // POST operations to PDP.

        /**
         * ￼
         Id
         ￼
         urn:oasis:names:tc:xacml:3.0:profile:rest:assertion:pdp:xacml:status
         ￼
         Normative Source
         ￼
         POST on the PDP with a valid XACML request MUST return status code 200
         ￼
         Target
         ￼
         Response to POST request on the PDP location with valid XACML request in the body
         ￼
         Predicate
         ￼
         The HTTP status code in the [response] is 200
         ￼
         Prescription Level
         ￼
         mandatory
         */


        /**
         * Id
         ￼
         urn:oasis:names:tc:xacml:3.0:profile:rest:assertion:pdp:xacml:body
         ￼
         Normative Source
         ￼
         POST on the PDP with a valid XACML request MUST return a valid XACML response in the body
         ￼
         Target
         ￼
         Response to POST request on the PDP location with valid XACML request in the body
         ￼
         Predicate
         ￼
         The HTTP body in the [response] is a valid XACML response
         ￼
         Prescription Level
         ￼
         mandatory
         */


        /**
         * ￼
         Id
         ￼
         urn:oasis:names:tc:xacml:3.0:profile:rest:assertion:pdp:xacml:invalid
         ￼
         Normative Source
         ￼
         POST on the PDP with an invalid XACML request MUST return status code 400 (Bad Request)
         ￼
         Target
         ￼
         Response to POST request on the PDP location with invalid XACML request in the body
         ￼
         Predicate
         ￼
         The HTTP status code in the [response] is 400
         ￼
         Prescription Level
         ￼
         mandatory
         */


        /**
         * Id
         ￼
         urn:oasis:names:tc:xacml:3.0:profile:rest:assertion:pdp:saml:status
         ￼
         Normative Source
         ￼
         POST on the PDP with a valid XACML request MUST return status code 200
         ￼
         Target
         ￼
         Response to POST request on the PDP location with valid XACML request wrapped in a
         xacml-samlp:XACMLAuthzDecisionQuery in the body
         ￼
         Predicate
         ￼
         The HTTP status code in the [response] is 200
         ￼
         Prescription Level
         ￼
         optional
         */


        /**
         * Id
         ￼
         urn:oasis:names:tc:xacml:3.0:profile:rest:assertion:pdp:saml:body
         ￼
         Normative Source
         ￼
         POST on the PDP with a valid XACML request MUST return a valid XACML response in the body
         ￼
         Target
         ￼
         Response to POST request on the PDP location with valid XACML request wrapped in a
         xacml-samlp:XACMLAuthzDecisionQuery in the body
         ￼
         Predicate
         ￼
         The HTTP body in the [response] is a valid XACML response wrapped in a
         samlp:Response
         ￼
         Prescription Level
         ￼
         optional
         */


        /**
         * ￼
         Id
         ￼
         urn:oasis:names:tc:xacml:3.0:profile:rest:assertion:pdp:saml:invalid
         ￼
         Normative Source
         ￼
         POST on the PDP with an invalid XACML request MUST return status code 400 (Bad Request)
         ￼
         Target
         ￼
         Response to POST request on the PDP location with invalid XACML request
         wrapped in a xacml-samlp:XACMLAuthzDecisionQuery in the body
         ￼
         Predicate
         ￼
         The HTTP status code in the [response] is 400
         ￼
         Prescription Level
         ￼
         optional
         */


    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        super.doPut(req, resp);    // TODO Future
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        super.doDelete(req, resp);    // TODO Future
    }

    // ******************************************************************************************************
    // Common Private Methods
    // ******************************************************************************************************

    /**
     * Generate the Authentication Header with our Digest.
     *
     * @return String of Generated Authentication Header.
     */
    private String generateAuthenticateHeader(String realm) {
        StringBuilder header = new StringBuilder();
        header.append("Digest realm=\"").append(realm).append("\",");
        if (!StringUtils.isBlank(authenticationMethods)) {
            header.append("qop=\"").append(authenticationMethods).append("\",");
        }
        header.append("algorithm=\"").append("md5").append("\",");
        header.append("nonce=\"").append(nonce).append("\",");
        header.append("opaque=\"").append(this.getOpaque(realm, nonce)).append("\"");
        // Return generated Authentication Header value.
        return header.toString();
    }

    /**
     * Private Helper Method to Generate a random Seed for Authentication Digest.
     *
     * @return String of Calculated Nonce
     */
    private String calculateNonce() {
        Date d = new Date();
        SimpleDateFormat f = new SimpleDateFormat("yyyy:MM:dd:hh:mm:ss");
        String fmtDate = f.format(d);
        Random rand = new Random(100000);
        Long randomLong = rand.nextLong();
        return DigestUtils.md5Hex(fmtDate + randomLong.toString());
    }

    /**
     * Private Helper Method to Generate the Opaque Generated String
     *
     * @param domain
     * @param nonce
     * @return String of Calculated Opaque
     */
    private String getOpaque(String domain, String nonce) {
        return DigestUtils.md5Hex(domain + nonce);
    }

    /**
     * Authenticate using a Digest.
     * <p/>
     * Per RFC2617: @see http://tools.ietf.org/html/rfc2617
     *
     * @param authenticationHeader -Example of Data for WWW-Authenticate.
     *                             Digest realm="example.org",qop=auth,nonce="9fc422776b40c52a8a107742f9a08d5c",
     *                             opaque="aba7d38a079f1a7d2e0ba2d4b84f3aa2"
     * @param requestBody
     * @param request
     * @return AuthenticationDigest valid Object or Null is UnAuthorized.
     * @throws ServletException
     * @throws IOException
     */
    private AuthenticationDigest authenticateUsingDigest(final String authenticationHeader,
                                                         final String requestBody,
                                                         final HttpServletRequest request, String realm)
            throws ServletException, IOException {
        final String classMethod = "XacmlContentHandlerService:authenticateUsingDigest";
        // *********************************************
        // Parse the Authentication Header Information.
        if (debug.messageEnabled()) {
            debug.message(classMethod+" authenticationHeader:[" + authenticationHeader + "]");
        }
        String headerStringWithoutAuthScheme = authenticationHeader.substring(authenticationHeader.indexOf(" ") + 1)
                .trim();
        // Obtain a Map of our Authentication Header.
        HashMap<String, String> headerValues = new HashMap<String, String>();
        String keyValueArray[] = headerStringWithoutAuthScheme.split(",");
        for (String keyval : keyValueArray) {
            if (keyval.contains("=")) {
                String key = keyval.substring(0, keyval.indexOf("="));
                String value = keyval.substring(keyval.indexOf("=") + 1);
                headerValues.put(key.trim(), value.replaceAll("\"", "").trim());
            }
        }

        // *****************************************
        // Obtain Each Value for Authentication
        // of the Digest.
        String method = request.getMethod();
        // TODO This needs to be fixed.
        String ha1 = DigestUtils.md5Hex(USERNAME + ":" + realm + ":" + "password");
        // TODO, Get from a PIP Implementation.

        // Obtain values to compute.
        String qop = headerValues.get("qop");
        String ha2;
        String requestURI = headerValues.get("uri");
        // determine AUTH Digest Method Details...
        if ( (StringUtils.isNotBlank(qop)) && (qop.equalsIgnoreCase("auth-int")) &&
             (StringUtils.isNotBlank(requestBody)) ) {
            String entityBodyMd5 = DigestUtils.md5Hex(requestBody);
            ha2 = DigestUtils.md5Hex(method + ":" + requestURI + ":" + entityBodyMd5);
        } else {
            ha2 = DigestUtils.md5Hex(method + ":" + requestURI);
        }
        AuthenticationDigest authenticationDigest = new AuthenticationDigest(method, ha1, qop, ha2, requestURI, realm);
        // ******************************************
        // Now consume the Server Response.
        String serverResponse;
        if (StringUtils.isBlank(qop)) {
            serverResponse = DigestUtils.md5Hex(ha1 + ":" + nonce + ":" + ha2);
        } else {
            String nonceCount = headerValues.get("nc");
            String clientNonce = headerValues.get("cnonce");

            serverResponse = DigestUtils.md5Hex(ha1 + ":" + nonce + ":"
                    + nonceCount + ":" + clientNonce + ":" + qop + ":" + ha2);
        }
        // ******************************************************
        // Now Compare our Server Response with Client Response.
        String clientResponse = headerValues.get("response");
        if ( (clientResponse == null) || (clientResponse.isEmpty()) ) {
            clientResponse = headerValues.get("solution");
        }
        // ******************************************************
        // Show both calculated and received value from client.
        if (debug.messageEnabled()) {
            debug.message("*** Server Response: "+serverResponse);
            debug.message("*** Client Response: "+clientResponse);
        }
        // ******************************************************
        // Check for any Nulls on either side.
        if ( (clientResponse == null) || (clientResponse.isEmpty()) ||
             (serverResponse == null) || (serverResponse.isEmpty()) ||
             (!serverResponse.equals(clientResponse)) ) {
            return null;
        } else {
            // Authenticated Digest is Valid, Allow Access.
            return authenticationDigest;
        }
    }

    /**
     * Private helper method to be performed for each incoming Request.
     * This helper method simply performs some basic checks and validates
     * our content type.  If not valid content type, then preProcessing
     * method has set the appropriate HTTP Return Status in the response
     * and will return null.
     *
     * @param request
     * @param response
     * @return
     * @throws ServletException
     * @throws IOException
     */
    private ContentType preProcessingRequest(HttpServletRequest request, HttpServletResponse response) throws
            ServletException, IOException {
        final String classMethod = "XacmlContentHandlerService:preProcessingRequest";
        // ******************************************************************
        // Handle any DoS Attacks and Threats to the integrity of the PDP.
        if (maxContentLength != 0) {
            if ((!request.getMethod().equalsIgnoreCase("GET")) && (request.getContentLength() < 0)) {
                // We do not have any valid Content Length set.
                response.setStatus(HttpServletResponse.SC_LENGTH_REQUIRED);  // 411
                response.setCharacterEncoding("UTF-8");
                return ContentType.NONE;
            }
            if (request.getContentLength() > maxContentLength) {
                if (debug.messageEnabled()) {
                    debug.message(
                            "Content length too large: " + request.getContentLength());
                }
                // We do not have any valid Content Length set.
                response.setStatus(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE); // 413
                response.setCharacterEncoding("UTF-8");
                return ContentType.NONE;
            }
        }
        // ******************************************************************
        // Validate Request Media Type
        ContentType requestContentType = ((request.getContentType() == null) ? null :
                ContentType.getNormalizedContentType(request.getContentType()));
        if (requestContentType == null) {
            // We do not have a valid Application Content Type!
            response.setStatus(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);  // 415
            response.setCharacterEncoding("UTF-8");
            response.setContentLength(0);
            return ContentType.NONE;
        }
        // ******************************************************************
        // Indicate preProcessing was completed with no Issues and Content
        // Type is valid with our derived ContentType.
        return requestContentType;
    }

    /**
     * Common Process Request Method.
     * Provides processing for both GET and POST methods.
     *
     * @param request
     * @param response
     * @throws ServletException
     * @throws IOException
     */
    private void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String classMethod = "XacmlContentHandlerService:processRequest";
        // ******************************************************************
        // Validate Request and Obtain Media Type
        ContentType requestContentType = this.preProcessingRequest(request, response);
        if ((requestContentType == null) || (requestContentType.equals(ContentType.NONE))) {
            // We do not have a valid Application Content Type or other issue.
            // Response Status set in preProcessingRequest method.
            response.setCharacterEncoding("UTF-8");
            response.setContentLength(0);
            return;
        }
        // ******************************************************************
        // Parse our Request...
        XACMLRequestInformation xacmlRequestInformation = this.parseRequestInformation(requestContentType, request);
        if (xacmlRequestInformation == null) {
            // This Starts the Authorization via Digest Flow...
            this.renderUnAuthorized(XACML3_PDP_DEFAULT_REALM, requestContentType, response);
            return;
        }
        // ******************************************************************
        // Check for any HTTP Digest Authorization Request or content.
        if ((request.getContentLength() <= 0) &&
            (xacmlRequestInformation.getRequestMethod().equalsIgnoreCase("GET")) &&
                ((xacmlRequestInformation.getAuthenticationHeader() == null) ||
                 (xacmlRequestInformation.getAuthenticationHeader().isEmpty()))) {
            // ***********************************************************************************************
            // Knowing we have no Authentication at this point check the Request Path Information, provide the
            // Home Document to the Requester by Rendering our Response.
            if ((xacmlRequestInformation.getRequestURI() == null) ||
                    (xacmlRequestInformation.getRequestURI().isEmpty()) ||
                    (xacmlRequestInformation.getRequestURI().trim().equalsIgnoreCase("/openam/xacml")) ||
                    (xacmlRequestInformation.getRequestURI().trim().equalsIgnoreCase("/openam/xacml/pdp")) ||
                    (xacmlRequestInformation.getRequestURI().trim().equalsIgnoreCase("/openam/xacml/home")) ||
                    (xacmlRequestInformation.getRequestURI().trim().equalsIgnoreCase("/openam/xacml/status"))) {
                // All other EndPoints Require Authentication to obtain access.
                try {
                    renderServerOKResponse(requestContentType, XacmlHomeResource.getHome(xacmlRequestInformation,
                            request), response);
                } catch (JSONException jsonException) {
                    // If any Exceptions, Force Unauthorized and show exception for debugging.
                    debug.error(classMethod + " JSON Exception Occurred: " + jsonException.getMessage(), jsonException);
                    // This Starts the Authorization via Digest Flow...
                    this.renderUnAuthorized(xacmlRequestInformation.getRealm(), requestContentType, response);
                }
            } else if (xacmlRequestInformation.getRequestURI().trim().equalsIgnoreCase("/openam/xacml/ping")) {
                try{
                    renderServerOKResponse(requestContentType, XacmlPingResource.getPing(xacmlRequestInformation,
                            request), response);
                } catch (JSONException jsonException) {
                    // If any Exceptions, Force Unauthorized and show exception for debugging.
                    debug.error(classMethod + " JSON Exception Occurred: " + jsonException.getMessage(), jsonException);
                    // This Starts the Authorization via Digest Flow...
                    this.renderUnAuthorized(xacmlRequestInformation.getRealm(), requestContentType, response);
                }
            } else {
                // Not the Correct Path, then indicated Unauthorized.
                this.renderUnAuthorized(xacmlRequestInformation.getRealm(), requestContentType, response);
            }
            // Return.
            return;
        } // End of outer if Check for content
        // ******************************************************************
        // Obtain our Request Content Data.
        if ((xacmlRequestInformation.getAuthenticationHeader() != null) &&
                (xacmlRequestInformation.getAuthenticationHeader().startsWith(DIGEST))) {
            AuthenticationDigest authenticationDigestResponse =
                    authenticateUsingDigest(xacmlRequestInformation.getAuthenticationHeader(),
                            xacmlRequestInformation.getOriginalContent(), request, xacmlRequestInformation.getRealm());
            // If we receive a valid authenticationDigestResponse Object, we have successfully Authenticated the
            // Client.
            if (authenticationDigestResponse == null) {
                // Not Authenticated.
                // This Starts the Authorization via Digest Flow...
                this.renderUnAuthorized(xacmlRequestInformation.getRealm(), requestContentType, response);
                return;
            } else {
                // Authentication is valid, set our POJO indicators, we had a valid digest and authenticated.
                xacmlRequestInformation.setAuthenticated(true);
            }
        } else if (xacmlRequestInformation.getAuthenticationContent() == null) {
            // **************************************************************
            // if no XACML saml Wrapper, then reject request as UnAuthorized.
            //
            // We only support currently WWW Authenticate capabilities
            // + Digest, Basic Authentication is not supported per OASIS  Specification.
            // Or Content whose contents contains a wrapper in either XML or JSON.
            // + XACMLAuthzDecisionQuery Wrapper Of Request.
            //
            // This will begin the Authorization Digest Flow...
            this.renderUnAuthorized(xacmlRequestInformation.getRealm(), requestContentType, response);
            return;
        }
        // ******************************************************************
        // Check for any XACMLAuthzDecisionQuery Request in either Flavor.
        if ( (!xacmlRequestInformation.isAuthenticated()) &&
             (xacmlRequestInformation.getAuthenticationContent() != null) ) {
            // If the Content is XML Based, we have a DOM.
            if (requestContentType.commonType() == CommonType.XML) {
                // **************************************************************
                // Content is XML and Nodes are Available to be consumed.
                // So perform the PDP Request from the PEP, Authentication will
                // be performed naturally since the request is wrapped in a
                // PEP Authentication outer request.
                // Response is located within the XacmlRequestInformation Object.
                XacmlPDPResource.processPDP_XMLRequest(xacmlRequestInformation, request, response);
                // TODO -- Analyze Response.

            } else {
                // **************************************************************
                // Else, our Content is assumed to be JSON, but we can still have
                // a xacml-samlp:XACMLAuthzDecisionQuery, but in JSON format.
                XacmlPDPResource.processPDP_JSONRequest(xacmlRequestInformation, request, response);
                // TODO -- Analyze Response.

            }
            // **************************************************
            // Determine if the Authentication Wrapper provided
            // a Request which was already satisfied.
            if ( (xacmlRequestInformation.isAuthenticated()) && (xacmlRequestInformation.isRequestProcessed())) {
                // *****************************************************************
                // Render our Response
                renderResponse(requestContentType,
                     xacmlRequestInformation.getXacmlStringResponseBasedOnContent(requestContentType), response);
            }
        } // End of Check for XACMLAuthzDecisionQuery Object and possible request Resolution for a [SAML4XACML] request.

        // **********************************************************************
        // Do Not Continue if we have authenticated or Trust Requester.
        if (!xacmlRequestInformation.isAuthenticated()) {
            // ******************************************************************
            // Not Authenticated nor Authorized, Forbidden.
            response.setCharacterEncoding("UTF-8");
            response.setContentLength(0);
            response.setStatus(HttpServletResponse.SC_FORBIDDEN); // 403.
            renderResponse(requestContentType, null, response);
            return;
        }
        // ******************************************************************
        // Check for a existence of a XACML Request, for a POST, we must have
        // Correct Request Content and if no Request Object in XML or JSON
        // form.
        if ( (xacmlRequestInformation.getRequestMethod().equalsIgnoreCase("POST")) &&
                (xacmlRequestInformation.isRequestNodePresent()) ) {
            // No Request Node found within the document, not valid request.
            this.renderBadRequest(requestContentType, response);
            return;
        }
        // ******************************************************************
        // Now this session has been Authenticated, using Auth-Digest or
        // other Authentication Algorithm, process the Authenticated Request...
        //
        // Check the Request Path Information.
        //
        if ((xacmlRequestInformation.getRequestURI() == null) ||
            (xacmlRequestInformation.getRequestURI().trim().isEmpty()) ||
            (xacmlRequestInformation.getRequestURI().trim().equalsIgnoreCase("/openam/xacml")) ||
            (xacmlRequestInformation.getRequestURI().trim().equalsIgnoreCase("/openam/xacml/pdp")) ||
            (xacmlRequestInformation.getRequestURI().trim().contains("/openam/xacml/pdp/"))) {

            // TODO

            // TODO Process PDP Request, Hook into existing code base here......
            //xacmlRequestProcessor.processRequest()

        } else {

            // TODO Additional Request Processing.


        }
        // *****************************************************************
        // Render our Response, response Setting should be set prior, if not
        // it will set a default Indeterminate Result Response.
        String responseContent = xacmlRequestInformation.getXacmlStringResponseBasedOnContent(requestContentType);
        renderServerOKResponse(requestContentType, responseContent, response);
    }

    // ******************************************************************************************************
    // Common Rendering Response Methods
    // ******************************************************************************************************

    /**
     * Private Helper Method to Render Response Content.
     *
     * @param contentType
     * @param xacmlStringResponse
     * @param response
     */
    private void renderResponse(final ContentType contentType, final String xacmlStringResponse, HttpServletResponse response) {
        try {
            response.setContentType(contentType.applicationType());
            response.setCharacterEncoding("UTF-8");
            if ((xacmlStringResponse != null) && (!xacmlStringResponse.trim().isEmpty())) {
                response.setContentLength(xacmlStringResponse.length());
                response.getOutputStream().write(xacmlStringResponse.getBytes());
                response.getOutputStream().close();
            } else {
                response.setContentLength(0);
            }
        } catch (IOException ioe) {
            // Debug
        }
    }

    /**
     * Simple Helper Method to provide common Not Authorized render Method.
     *
     * @param requestContentType
     * @param response
     */
    private void renderUnAuthorized(final String realm, final ContentType requestContentType,
                                    HttpServletResponse response) {
        response.addHeader(WWW_AUTHENTICATE_HEADER, this.generateAuthenticateHeader(realm));
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);  // 401
        renderResponse(requestContentType, null, response);
    }

    /**
     * Simple Helper Method to provide common Not Authorized render Method.
     *
     * @param requestContentType
     * @param response
     */
    private void renderBadRequest(final ContentType requestContentType, HttpServletResponse response) {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);  // 400
        renderResponse(requestContentType, null, response);
    }

    /**
     * Simple Helper Method to provide common OK for Server PDP Status.
     *
     * @param requestContentType
     * @param responseContent
     * @param response
     */
    private void renderServerOKResponse(final ContentType requestContentType,
                                        String responseContent, HttpServletResponse response) {
        response.setCharacterEncoding("UTF-8");
        response.setStatus(HttpServletResponse.SC_OK);  // 200
        renderResponse(requestContentType, responseContent, response);
    }

    /**
     * Provide common Entry point Method for Parsing Initial Requests
     * to obtain information on how to process and route the request.
     *
     * @param request
     * @return XACMLRequestInformation - Object returned with Parsed Request Information.
     * @throws ServletException
     */
    private XACMLRequestInformation parseRequestInformation(ContentType contentType, HttpServletRequest request)
            throws ServletException {
        final String classMethod = "XacmlContentHandlerService:parseRequestInformation: ";

        // Get URI and MetaAlias Data.
        String requestURI = request.getRequestURI();
        String queryMetaAlias =
                XACML3Utils.getMetaAliasByUri(requestURI);
        String realm = XACML3Utils.getRealmByMetaAlias(queryMetaAlias);
        if ( (realm == null) || (realm.isEmpty()) ) {
            realm = XACML3_PDP_DEFAULT_REALM;
        }

        // Attempt to get the PDP Entity ID...
        String pdpEntityID = null;
        try {
            // Get PDP entity ID
            pdpEntityID =
                    XACML3Utils.getEntityByMetaAlias(queryMetaAlias);
        } catch (SAML2Exception xe) {
            debug.error(classMethod + " SAML2 Exception obtaining PDP Entity ID: " + xe.getMessage(), xe);
            // Absorb this issue and continue without our PDP Entity ID.
        }

        // Bootstrap our Request Information Object for this Request.
        XACMLRequestInformation xacmlRequestInformation = new XACMLRequestInformation(contentType,
                queryMetaAlias, pdpEntityID, realm, request);
        // Consume the Request Content, by parsing
        // the Content Depending upon the Content Type.
        if (contentType.commonType().equals(CommonType.XML)) {
            parseXMLRequest(xacmlRequestInformation);
        } else {
            // Only can be JSON at this point in Data FLow.
            parseJSONRequest(xacmlRequestInformation);
        }
        // **************************************
        // Return our Request Information for
        // processing request.
        return xacmlRequestInformation;
    }

    /**
     * Private Helper to Parse the XML Request Body.
     *
     * @param xacmlRequestInformation To indicate the XACMLRequestInformation's request Node
     *                                does not exist, the XACMLRequestInformation.isRequestNodePresent() will
     *                                return false, which indicates an Invalid XACML Request.
     */
    private void parseXMLRequest(XACMLRequestInformation xacmlRequestInformation) {
        if ((xacmlRequestInformation.getOriginalContent() == null) ||
                (xacmlRequestInformation.getOriginalContent().isEmpty())) {
            return;
        }
        // Get Document Builder Factory
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        // Leave off validation, namespaces and Schema, otherwise we will have false-positive validation
        // failures.  Validation will be performed using XPath Navigation.
        factory.setValidating(false);
        factory.setNamespaceAware(false);
        factory.setExpandEntityReferences(true);
        factory.setIgnoringComments(true);
        // unMarshal the XML String into a Document Object.
        Document document = null;
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            ByteArrayInputStream inputStream =
                    new ByteArrayInputStream(
                            (xacmlRequestInformation.getOriginalContent().startsWith("<?") ?
                                    xacmlRequestInformation.getOriginalContent().getBytes() :
                                    (XML_HEADER + xacmlRequestInformation.getOriginalContent()).getBytes()));
            document = builder.parse(inputStream);
        } catch (SAXException se) {
            debug.error("SAXException: " + se.getMessage());
        } catch (ParserConfigurationException pce) {
            debug.error("The underlying parser does not support the requested features: " + pce.getMessage());
        } catch (FactoryConfigurationError fce) {
            debug.error("Error occurred obtaining Document Builder Factory: " + fce.getMessage());
        } catch (IOException ioe) {
            debug.error("IO Exception occurred obtaining Document Builder Factory: " + ioe.getMessage());
        }
        // Save a Reference to our XML Document for later use.
        xacmlRequestInformation.setContent(document);
        if (document == null) {
            return;
        }
        // ********************************************************
        // Now dig using XPaths to perform a validation.
        // We need to obtain the Request Node and the
        // XACMLAuthzDecisionQuery wrapper if applicable.
        //
        Node requestNode;
        Node xacmlAuthzDecisionQueryNode;

        XPathFactory xPathfactory = XPathFactory.newInstance();
        XPath xpath = xPathfactory.newXPath();

        // Now check to see if we have an optional XACMLAuthzDecisionQueryNode wrapper for the request...
        try {
            // Check for we have a XACMLAuthzDecisionQueryNode.
            XPathExpression expr = xpath.compile("/" + XACML_AUTHZ_QUERY);
            xacmlAuthzDecisionQueryNode = (Node) expr.evaluate(document, XPathConstants.NODE);
            if (xacmlAuthzDecisionQueryNode != null) {
                // Save a Reference to our XML Node for later use.
                xacmlRequestInformation.setAuthenticationContent(xacmlAuthzDecisionQueryNode);
                for (int i = 0; i < xacmlAuthzDecisionQueryNode.getAttributes().getLength(); i++) {
                    xacmlRequestInformation.getXacmlAuthzDecisionQuery().setByName(
                            xacmlAuthzDecisionQueryNode.getAttributes().item(i).getNodeName(),
                            xacmlAuthzDecisionQueryNode.getAttributes().item(i).getNodeValue());
                    // Make me Message level....
                    debug.error("Node: " + xacmlAuthzDecisionQueryNode.getAttributes().item(i).getNodeName() +
                            " attribute: " + xacmlAuthzDecisionQueryNode.getAttributes().item(i).getNodeValue());
                }
                //
                // Verify the Node attributes.
                // They Must contain an ID, IssueInstant and Version fields are Required.
                // Destination and Consent Fields are Optional.
                //
                if ((!isRequiredFieldPresent(xacmlRequestInformation.getXacmlAuthzDecisionQuery().getId())) ||
                        (!isRequiredFieldPresent(xacmlRequestInformation.getXacmlAuthzDecisionQuery().getIssueInstant())) ||
                        (!isRequiredFieldPresent(xacmlRequestInformation.getXacmlAuthzDecisionQuery().getVersion()))) {
                    // Indicate Error in Request.
                    debug.error(XACML_AUTHZ_QUERY + " Required Field Missing, must contain ID, " +
                            "IssueInstant and Version.");
                    return;
                }

            } // End of Check for XACMLAuthzDecisionQuery Node Element.
        } catch (XPathExpressionException xee) {
            // Our initial Expression for a Node was invalid, we have no Request Object.
            // This could be a maintenance which has no request, but not part of specification yet...
            // Document could be bad or suspect.
            debug.error("XPathExpressException: " + xee.getMessage() + ", returning null invalid content!");
            return;
        }

        // Verify we have a PEP Request
        try {
            // Verify we have a Request.
            XPathExpression expr = xpath.compile("/" + REQUEST);
            requestNode = (Node) expr.evaluate(document, XPathConstants.NODE);
            if (requestNode != null) {
                // Indicate our Request Node is in fact present, either wrapped or not.
                xacmlRequestInformation.setRequestNodePresent(true);
                for (int i = 0; i < requestNode.getAttributes().getLength(); i++) {
                    debug.error("Node: " + requestNode.getAttributes().item(i).getNodeName() + " attribute: " + requestNode
                            .getAttributes().item(i));

                    // TODO Verify....
                    // TODO

                }
            }
        } catch (XPathExpressionException xee) {
            // Our initial Expression for a Node was invalid, we have no Request Object.
            // This could be a maintenance which has no request, but not part of specification yet...
            // Document could be bad or suspect.
            debug.error("XPathExpressException: " + xee.getMessage() + ", returning null invalid content!");
            return;
        }
        // Valid Request to Process.
        xacmlRequestInformation.setParsedCorrectly(true);
    }

    /**
     * Private Helper to Parse the JSON Request Body.
     *
     * @param xacmlRequestInformation
     */
    private void parseJSONRequest(XACMLRequestInformation xacmlRequestInformation) {
        if ((xacmlRequestInformation.getOriginalContent() == null) ||
                (xacmlRequestInformation.getOriginalContent().isEmpty())) {
            return;
        }
        try {
            // The Original Content will be UnMarshaled into a Map Object stored in Content.
            xacmlRequestInformation.setContent(JsonToMapUtility.fromString(xacmlRequestInformation.getOriginalContent()));
            xacmlRequestInformation.setParsedCorrectly(true);
        } catch (IOException ioe) {
            debug.error("parseJSONRequest Exception: " + ioe.getMessage() + "], content will be ignored!");
        }
    }

    /**
     * Simple helper for checking required field is present or not.
     *
     * @param value
     * @return boolean - indicator True if Field Present and False if not.
     */
    private static boolean isRequiredFieldPresent(String value) {
        return ((value != null) && (!value.isEmpty()));
    }

    /**
     * Public Helper Method for accessing handlers.
     * <p/>
     * TODO Remove!!!!
     *
     * @return
     */
    public static HashMap getHandlers() {
        return handlers;
    }

}
