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
package org.forgerock.identity.openam.xacml.services;

import com.sun.identity.common.SystemConfigurationUtil;

import com.sun.identity.saml.xmlsig.KeyProvider;
import com.sun.identity.saml2.assertion.Assertion;
import com.sun.identity.saml2.assertion.AssertionFactory;
import com.sun.identity.saml2.assertion.EncryptedAssertion;
import com.sun.identity.saml2.assertion.Issuer;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2Utils;
import com.sun.identity.saml2.jaxb.metadata.XACMLAuthzDecisionQueryDescriptorElement;
import com.sun.identity.saml2.key.EncInfo;
import com.sun.identity.saml2.key.KeyUtil;
import com.sun.identity.saml2.logging.LogUtil;
import com.sun.identity.saml2.meta.SAML2MetaException;
import com.sun.identity.saml2.protocol.RequestAbstract;
import com.sun.identity.saml2.protocol.Response;
import com.sun.identity.saml2.soapbinding.RequestHandler;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.xacml.client.XACMLRequestProcessor;
import com.sun.identity.xacml.common.XACMLException;
import com.sun.identity.xacml.context.ContextFactory;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;
import org.forgerock.identity.openam.xacml.commons.AuthenticationDigest;
import org.forgerock.identity.openam.xacml.commons.CommonType;
import org.forgerock.identity.openam.xacml.commons.ContentType;
import org.forgerock.identity.openam.xacml.commons.XACML3Utils;
import org.forgerock.identity.openam.xacml.model.XACML3Constants;
import org.forgerock.identity.openam.xacml.model.XACMLRequestInformation;
import org.forgerock.identity.openam.xacml.resources.XacmlHomeResource;
import org.json.JSONException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.parsers.*;
import javax.xml.soap.MimeHeaders;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.xpath.*;
import java.io.*;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * XACML Resource Router
 * <p/>
 * Provides main end-point for all XACML requests, either XML or JSON based over HTTP/HTTPS REST based protocol flow.
 * This ForgeRock developed XACML Resource Router complies with the following OASIS Specifications:
 * <p/>
 * <p/>
 * <p/>
 * Some content of this code was originally used from the @see com.sun.identity.saml2.soapbinding.QueryHandlerServlet.
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


    // TODO Fix this constant.

    private static final String schemaFileName =
            "/Users/jaschenk/MyWorkspaces/OPENAM/branches/openam_10.1.0_xacml3_JAS/openam/openam-schema/openam-xacml3-schema/src/main/resources/xsd/xacml-core-v3-schema-wd-17.xsd";


    private static final String x = "<order xmlns=\042urn:oasis:names:tc:xacml:3.0:core:schema:wd-17\042" +
            " xmlns:xsi=\042http://www.w3.org/2001/XMLSchema-instance\042" +
            " xsi:schemaLocation=\042urn:oasis:names:tc:xacml:3.0:core:schema:wd-17 file:" + schemaFileName + "\042>";


    private static final String DEFAULT_RESPONSE = XML_HEADER + "<xacml-ctx:response xmlns:xacml-ctx=\"urn:oasis:names:tc:xacml:3.0:core:schema:wd-17\">" +
            "</xacml-ctx:response>";

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
     * XACML Schema for Validation.
     */
    private static Schema xacmlSchema;

    /**
     * Digest Authentication Global Constants.
     */
    private final String authenticationMethod = "auth";
    private final String USERNAME = "username";

    /**
     * Digest Authentication Objects.
     */
    private static String nonce;
    private static ScheduledExecutorService nonceRefreshExecutor;

    /**
     * Initialize our Servlet
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
            // TODO Make this a Static variable to pick up xsd as a resource from a JAR.
            Source xacmlSchemaSource = new StreamSource(
                    new File(schemaFileName));
            xacmlSchema = constraintFactory.newSchema(xacmlSchemaSource);
        } catch (SAXException se) {
            debug.error("SAX Exception obtaining XACML Schema for Validation,", se);
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

    /**
     * Generate the Authentication Header with our Digest.
     *
     * @return
     */
    private final String generateAuthenticateHeader(String realm) {
        StringBuilder header = new StringBuilder();
        header.append("Digest realm=\"").append(realm).append("\",");
        if (!StringUtils.isBlank(authenticationMethod)) {
            header.append("qop=").append(authenticationMethod).append(",");
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
    private final String calculateNonce() {
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
    private final String getOpaque(String domain, String nonce) {
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
    private AuthenticationDigest preAuthenticateUsingDigest(final String authenticationHeader,
                                                            final String requestBody,
                                                            final HttpServletRequest request, String realm)
            throws ServletException, IOException {
        // *********************************************
        // Parse the Authentication Header Information.
        System.out.println("authenticationHeader:[" + authenticationHeader + "]"); // TODO Change Me...

        String headerStringWithoutAuthScheme = authenticationHeader.substring(authenticationHeader.indexOf(" ") + 1)
                .trim();
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
        String ha1 = DigestUtils.md5Hex(USERNAME + ":" + realm + ":" + "password");    // TODO This needs to be fixed.

        String qop = headerValues.get("qop");
        String ha2;

        String requestURI = headerValues.get("uri");

        if (!StringUtils.isBlank(qop) && qop.equals("auth-int")) {
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
            String domain = headerValues.get("realm");   // TODO Fix me.....

            String nonceCount = headerValues.get("nc");
            String clientNonce = headerValues.get("cnonce");

            serverResponse = DigestUtils.md5Hex(ha1 + ":" + nonce + ":"
                    + nonceCount + ":" + clientNonce + ":" + qop + ":" + ha2);

        }
        // ******************************************************
        // Now Compare our Server Response with Client Response.
        String clientResponse = headerValues.get("response");
        if (!serverResponse.equals(clientResponse)) {
            return null;
        } else {
            // Authenticate Digest is Valid.
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
            if (request.getContentLength() < 0) {
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
        // Set up Method Logging Incoming Request.
        // TODO Make message level of info.
        debug.error(classMethod + " processing Incoming Request MediaType:[" + request.getContentType() + "], Content Length:[" + request.getContentLength() + "]");
        // ******************************************************************
        // Indicate preProcessing was completed with no Issues and Content
        // Type is valid with our derived ContentType.
        return requestContentType;
    }

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
        response.setCharacterEncoding("UTF-8");
        response.setContentLength(0);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);  // 401
        renderResponse(requestContentType, null, response);
        return;
    }

    /**
     * Simple Helper Method to provide common Not Authorized render Method.
     *
     * @param requestContentType
     * @param response
     */
    private void renderBadRequest(final ContentType requestContentType, HttpServletResponse response) {
        response.setCharacterEncoding("UTF-8");
        response.setContentLength(0);
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);  // 400
        renderResponse(requestContentType, null, response);
        return;
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "This ForgeRock OpenAM Servlet Implements the XACML 3.0 Standards per OASIS.";
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
        final String classMethod = "XacmlContentHandlerService:doGet";
        boolean digest_valid = false;
        boolean authenticated = false;
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
        // ******************************************************************
        // Check for any HTTP Digest Authorization Request
        if ((request.getContentLength() == 0) && (xacmlRequestInformation.getAuthenticationHeader() != null) &&
                (!xacmlRequestInformation.getAuthenticationHeader().isEmpty())) {
            // This Starts the Authorization via Digest Flow...
            this.renderUnAuthorized(xacmlRequestInformation.getRealm(), requestContentType, response);
            return;
        }
        // ******************************************************************
        // Obtain our Request Content Data.
        if ((xacmlRequestInformation.getAuthenticationHeader() != null) &&
                (!xacmlRequestInformation.getAuthenticationHeader().isEmpty()) &&
                (xacmlRequestInformation.getAuthenticationHeader().startsWith(DIGEST))) {
            AuthenticationDigest authenticationDigestResponse =
                    preAuthenticateUsingDigest(xacmlRequestInformation.getAuthenticationHeader(),
                            xacmlRequestInformation.getOriginalContent(), request, xacmlRequestInformation.getRealm());
            if (authenticationDigestResponse == null) {
                // Not Authenticated.
                // This Starts the Authorization via Digest Flow...
                this.renderUnAuthorized(xacmlRequestInformation.getRealm(), requestContentType, response);
                return;
            } else {
                digest_valid = true;
            }
        } else if (xacmlRequestInformation.getAuthenticationContent() == null) {
            // ***********************************************************
            // if no XACML saml Wrapper,
            // We only support WWW Authenticate with Schemes:
            // + Digest, Basic Authentication is not supported per OASIS  Specification.
            // Or Content whose contents contains a wrapper in either XML or JSON.
            // + XACMLAuthzDecisionQuery Wrapper Of Request.
            //
            // This Starts the Authorization Digest Flow...
            this.renderUnAuthorized(xacmlRequestInformation.getRealm(), requestContentType, response);
            return;
        }
        // ******************************************************************
        // Check for Valid Digest Request.
        if (digest_valid) {
            // TODO -- Continue Validation of UserId and Password.
        }

        // ******************************************************************
        // Determine our Content Type Language, XML or JSON for now.
        if (requestContentType.commonType() == CommonType.XML) {
            // If Content is XML,
        } else {
            // Else, our Content is assumed to be JSON.
        }


        // ************************************************************
        // Accept a pre-determined entry point for the Home Documents
        try {
            // *****************************************************************
            // Render our Response
            renderResponse(requestContentType, XacmlHomeResource.getHome(xacmlRequestInformation, request), response);
            return;

        } catch (JSONException je) {
            debug.error("JSON processing Exception: " + je.getMessage(), je);
        }


        /**
         * Id
         ￼
         urn:oasis:names:tc:xacml:3.0:profile:rest:assertion:home:status
         ￼
         Normative Source
         ￼
         GET on the home location MUST return status code 200
         ￼
         Target
         ￼
         Response to GET request on the home location
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
         urn:oasis:names:tc:xacml:3.0:profile:rest:assertion:home:body
         ￼
         Normative Source
         ￼
         GET on the home location MUST return a home document
         ￼
         Target
         ￼
         Response to GET request on the home location
         ￼
         Predicate
         ￼
         The HTTP body in the [response] follows the home document schema
         [HomeDocument]
         ￼
         Prescription Level
         ￼
         mandatory
         */


        /**
         * Id
         ￼
         urn:oasis:names:tc:xacml:3.0:profile:rest:assertion:home:pdp
         ￼
         Normative Source
         ￼
         The XACML entry point representation SHOULD contain a link to the PDP
         ￼
         Target
         ￼
         Response to GET request on the home location
         ￼
         Predicate
         ￼
         The home document in the [response] body contains a resource with link relation http://docs.oasis-open.org/ns/xacml/relation/pdp and a valid URL
         ￼
         Prescription Level
         ￼
         mandatory
         */


        // Check our query string.
        String queryParam = request.getQueryString();

        // Formulate Response.
        StringBuilder xacmlStringBuilderResponse = new StringBuilder();
        if ((request.getContentType() == ContentType.NONE.applicationType()) ||
                (request.getContentType().equalsIgnoreCase(ContentType.JSON_HOME.applicationType())) ||
                (request.getContentType().equalsIgnoreCase(ContentType.JSON.applicationType()))) {
            try {
                xacmlStringBuilderResponse.append(XacmlHomeResource.getHome(xacmlRequestInformation, request)); // TODO -- Cache the Default Home JSON Document Object.
            } catch (JSONException jsonException) {
                // TODO
            }
        } else {
            // Formulate the Home Document in XML.
            // TODO

            xacmlStringBuilderResponse.append("<resources xmlns=\042http://ietf.org/ns/home-documents\042\n");
            xacmlStringBuilderResponse.append("xmlns:atom=\042http://www.w3.org/2005/Atom\042>\n");
            xacmlStringBuilderResponse.append("<resource rel=\042http://docs.oasis-open.org/ns/xacml/relation/pdp\042>");
            xacmlStringBuilderResponse.append("<atom:link href=\042/authorization/pdp\042/>");  // TODO Static?
            xacmlStringBuilderResponse.append("</resource>");
            xacmlStringBuilderResponse.append(" </resources>");
        }
        // TODO Determine if there are any other Get Request Types we need to deal with,
        // TODO otherwise pass along.


        // *****************************************************************
        // Render our Response
        renderResponse(requestContentType, xacmlStringBuilderResponse.toString(), response);
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
        String classMethod = "XacmlContentHandlerService:doPost";
        boolean digest_valid = false;
        boolean authenticated = false;
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
        // ******************************************************************
        // Check for any HTTP Digest Authorization Request
        if ((request.getContentLength() == 0) && (xacmlRequestInformation.getAuthenticationHeader() != null) &&
                (!xacmlRequestInformation.getAuthenticationHeader().isEmpty())) {
            // This Starts the Authorization via Digest Flow...
            this.renderUnAuthorized(xacmlRequestInformation.getRealm(), requestContentType, response);
            return;
        }
        // ******************************************************************
        // Check for a existence of a XACML Request
        if (xacmlRequestInformation.isRequestNodePresent()) {
            // No Request Node found within the document, not valid request.
            this.renderBadRequest(requestContentType, response);
            return;
        }
        // ******************************************************************
        // Obtain our Request Content Data.
        if ((xacmlRequestInformation.getAuthenticationHeader() != null) &&
                (!xacmlRequestInformation.getAuthenticationHeader().isEmpty()) &&
                (xacmlRequestInformation.getAuthenticationHeader().startsWith(DIGEST))) {
            AuthenticationDigest authenticationDigestResponse =
                    preAuthenticateUsingDigest(xacmlRequestInformation.getAuthenticationHeader(),
                            xacmlRequestInformation.getOriginalContent(), request, xacmlRequestInformation.getRealm());
            if (authenticationDigestResponse == null) {
                // Not Authenticated.
                // This Starts the Authorization via Digest Flow...
                this.renderUnAuthorized(xacmlRequestInformation.getRealm(), requestContentType, response);
                return;
            } else {
                digest_valid = true;
            }
        } else if (xacmlRequestInformation.getAuthenticationContent() == null) {
            // if no XACML saml Wrapper,
            // We only support WWW Authenticate with Schemes:
            // + Digest, Basic Authentication is not supported per OASIS  Specification.
            // Or Content whose contents contains a wrapper in either XML or JSON.
            // + XACMLAuthzDecisionQuery Wrapper Of Request.
            //
            // This Starts the Authorization Digest Flow...
            this.renderUnAuthorized(xacmlRequestInformation.getRealm(), requestContentType, response);
            return;
        }
        // ******************************************************************
        // Check for Valid Digest Request.
        if (digest_valid) {
            // TODO -- Continue Validation of UserId and Password.
        }
        // ******************************************************************
        // Check for any XACMLAuthzDecisionQuery Request in either Flavor.
        if (xacmlRequestInformation.getAuthenticationContent() != null) {
            if (requestContentType.commonType() == CommonType.XML) {
                // **************************************************************
                // Content is XML and Nodes are Available to be consumed.

                // TODO

            } else {
                // **************************************************************
                // Else, our Content is assumed to be JSON, but we can still have
                // a xacml-samlp:XACMLAuthzDecisionQuery, but in JSON format.

                // TODO

            }
        } else if (!authenticated) {
            // ******************************************************************
            // Authenticate and Authorize
            response.setCharacterEncoding("UTF-8");
            response.setContentLength(0);
            response.setStatus(HttpServletResponse.SC_FORBIDDEN); // 403.
            renderResponse(requestContentType, null, response);
            return;
        }
        // ******************************************************************
        // Now this session has been Authenticated, either using Auth-Digest
        // or via xacml-samlp:XACMLAuthzDecisionQuery Wrapper.
        // Process the Authenticated Request...
        //


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
         Response to POST request on the PDP location with invalid XACML request wrapped in a xacml-samlp:XACMLAuthzDecisionQuery in the body
         ￼
         Predicate
         ￼
         The HTTP status code in the [response] is 400
         ￼
         Prescription Level
         ￼
         optional
         */

        // *****************************************************************
        // perform the PDP Request from the PEP.
        String xacmlStringResponse = processPDPRequest(xacmlRequestInformation, request, response);
        // *****************************************************************
        // Render our Response
        renderResponse(requestContentType, xacmlStringResponse, response);
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        super.doPut(req, resp);    // TODO
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        super.doDelete(req, resp);    // TODO
    }

    /**
     * Processes the POST Request for the PDP.
     *
     * @param request
     * @param response
     * @throws ServletException
     * @throws IOException
     */
    private String processPDPRequest(XACMLRequestInformation xacmlRequestInformation,
                                     HttpServletRequest request,
                                     HttpServletResponse response)
            throws ServletException, IOException {
        String classMethod = "XacmlContentHandlerService:processPDPRequest";
        // Get all the headers from the HTTP request
        MimeHeaders headers = SAML2Utils.getHeaders(request);

        // Ready our Response Object...
        com.sun.identity.entitlement.xacml3.core.Response xacmlResponse = new com.sun.identity.entitlement.xacml3.core.Response();
        com.sun.identity.entitlement.xacml3.core.Result xacmlResult = new com.sun.identity.entitlement.xacml3.core.Result();
        xacmlResponse.getResult().add(xacmlResult);

        String xacmlStringResponse = null;

        // Check Original Content
        if (xacmlRequestInformation.getOriginalContent() != null) {

            debug.error("XACML Incoming Request:[" + xacmlRequestInformation.getOriginalContent() + "], " +
                    "Length:[" + xacmlRequestInformation.getOriginalContent().length() + "]");

            // ********************************************
            // Process the PDP Request from the PEP.
            String pepEntityID = null;    // TODO Need to resolve this.

            try {
                if (xacmlRequestInformation.getContentType().commonType().equals(CommonType.XML)) {
                    Document requestDocument = (Document) xacmlRequestInformation.getContent();
                    if (requestDocument != null) {
                        Object xacmlRequestObject = null;
                        try {
                            // Formulate the XACML Request Object Graph.
                            xacmlRequestObject = xmlToRequestObject(requestDocument);
                            if (xacmlRequestObject != null) {
                                debug.error("Unmarshalled Request Object:[" + xacmlRequestObject.toString() + "]");
                            }
                        } catch (Exception e) {
                            debug.error("Exception performing xml to Object Graph.", e);

                            // TODO --- This should send a 401
                        }
                        Response samlResponse =
                                processSAMLRequest(xacmlRequestInformation.getRealm(),
                                        xacmlRequestInformation.getPdpEntityID(),
                                        ((Document) xacmlRequestInformation.getAuthenticationContent()).getDocumentElement(), request,
                                        requestDocument.getDocumentElement());
                        if (samlResponse != null) {
                            xacmlStringResponse = samlResponse.toXMLString(true, true);
                        }


                        // TODO Needs Work to process the request....

                        /*****

                         // Find the Authorization Element.
                         Element reqAbs = XACML3Utils.getSamlpElement(requestDocument.getDocumentElement(),
                         REQUEST_ABSTRACT);

                         *****/


                        // Set our Response Status per specification.
                        response.setStatus(HttpServletResponse.SC_OK);   // 200
                    } else if (xacmlRequestInformation.getContentType().commonType().equals(CommonType.JSON)) {


                        // Set our Response Status per specification.
                        response.setStatus(HttpServletResponse.SC_OK);   // 200
                    } else {
                        // Invalid content.
                        response.setStatus(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);   // 415
                    }

                } else {
                    // Bad or no content.
                    response.setStatus(HttpServletResponse.SC_NO_CONTENT);   // 204 // Fix this Return.
                }

            } catch (Exception se) {
                debug.error(classMethod + "XACML Processing Exception", se);
                // TODO Handle invalid.
                // Bad or no content.
                response.setStatus(HttpServletResponse.SC_NO_CONTENT);   // 204 // Fix this Return.
            }

        } else {
            // Bad or no content.
            response.setStatus(HttpServletResponse.SC_NO_CONTENT);   // 204 // Fix this Return.
        }
        // TODO Handle invalid.
        SAML2Utils.putHeaders(headers, response);

        // TODO Temporary return...
        if (xacmlStringResponse == null) {
            xacmlStringResponse = DEFAULT_RESPONSE;
        }


        return xacmlStringResponse;
    }


    /**
     * Signs an <code>Assertion</code>.
     *
     * @param realm       the realm name of the Policy Decision Point (PDP).
     * @param pdpEntityID the entity id of the policy decision provider.
     * @param assertion   the <code>Assertion</code> to be signed.
     * @throws <code>SAML2Exception</code> it there is an error signing
     *                                     the assertion.
     */
    static void signAssertion(String realm, String pdpEntityID,
                              Assertion assertion) throws SAML2Exception {
        String classMethod = "XacmlContentHandlerService.signAssertion: ";

        // Don't load the KeyProvider object in static block as it can
        // cause issues when doing a container shutdown/restart.
        KeyProvider keyProvider = KeyUtil.getKeyProviderInstance();
        if (keyProvider == null) {
            debug.error(classMethod +
                    "Unable to get a key provider instance.");
            throw new SAML2Exception("nullKeyProvider");
        }
        String pdpSignCertAlias = SAML2Utils.getAttributeValueFromXACMLConfig(
                realm, SAML2Constants.PDP_ROLE, pdpEntityID,
                SAML2Constants.SIGNING_CERT_ALIAS);
        if (pdpSignCertAlias == null) {
            debug.error(classMethod +
                    "Unable to get the hosted PDP signing certificate alias.");
            String[] data = {realm, pdpEntityID};
            LogUtil.error(Level.INFO, LogUtil.NULL_PDP_SIGN_CERT_ALIAS, data);
            throw new SAML2Exception("missingSigningCertAlias");
        }
        assertion.sign(keyProvider.getPrivateKey(pdpSignCertAlias),
                keyProvider.getX509Certificate(pdpSignCertAlias));
    }

    /**
     * Returns the SAMLv2 <code>Response</code> received in response to
     * the Request.
     *
     * @param realm              the realm of the entity.
     * @param pdpEntityID        entity identifier of the Policy Decision Point.
     * @param reqAbs             the Document Element object.
     * @param request            the <code>HttpServletRequest</code> object.
     * @param requestBodyElement the <code>Element</code> object
     * @return the <code>Response</code> object.
     * @throws <code>SAML2Exception</code> if there is an error processing
     *                                     the request.
     */
    private Response processSAMLRequest(String realm, String pdpEntityID, Element reqAbs,
                                        HttpServletRequest request, Element requestBodyElement)
            throws SAML2Exception {
        String classMethod = "XacmlContentHandlerService:processSAMLRequest";
        Response samlResponse = null;

        if (reqAbs != null) {
            String xsiType = reqAbs.getAttribute(XSI_TYPE_ATTR);
            if (debug.messageEnabled()) {
                debug.message(classMethod + "xsi type is : " + xsiType);
            }
            if (xsiType != null && xsiType.indexOf(XACML_AUTHZ_QUERY) != -1) {
                RequestAbstract samlRequest =
                        ContextFactory.getInstance()
                                .createXACMLAuthzDecisionQuery(reqAbs);
                String requestStr = samlRequest.toXMLString(true, true);
                String[] data = {requestStr, pdpEntityID};
                LogUtil.access(Level.FINE, LogUtil.REQUEST_MESSAGE, data);

                Issuer issuer = samlRequest.getIssuer();
                String pepEntityID = null;
                if (issuer != null) {
                    pepEntityID = issuer.getValue().trim();
                }
                if (debug.messageEnabled()) {
                    debug.message(classMethod + "Issuer is:" + pepEntityID);
                }
                boolean isTrusted = false;
                try {
                    isTrusted = SAML2Utils.getSAML2MetaManager().
                            isTrustedXACMLProvider(realm, pdpEntityID, pepEntityID,
                                    SAML2Constants.PDP_ROLE);
                } catch (SAML2MetaException sme) {
                    debug.error("Error retreiving meta", sme);
                }
                if (!isTrusted) {
                    if (debug.messageEnabled()) {
                        debug.message(classMethod +
                                "Issuer in Request is not valid." + pepEntityID);
                    }
                    String[] args = {realm, pepEntityID, pdpEntityID};
                    LogUtil.error(Level.INFO,
                            LogUtil.INVALID_ISSUER_IN_PEP_REQUEST,
                            args);
                    throw new SAML2Exception("invalidIssuerInRequest");
                }
                samlResponse =
                        processXACMLResponse(realm, pdpEntityID, samlRequest, request,
                                requestBodyElement);

            }
        }
        return samlResponse;
    }

    /**
     * Returns the received Response to the Requester.
     * Validates the message signature if signed and invokes the
     * Request Handler to pass the request for further processing.
     *
     * @param realm              realm of the entity.
     * @param pdpEntityID        entity identifier of Policy Decision Point (PDP).
     * @param samlRequest        the <code>RequestAbstract</code> object.
     * @param request            the <code>HttpServletRequest</code> object.
     * @param requestBodyElement the <code>Element</code> object.
     * @throws <code>SAML2Exception</code> if there is an error processing
     *                                     the request and returning a  response.
     */
    private Response processXACMLResponse(String realm, String pdpEntityID,
                                          RequestAbstract samlRequest, HttpServletRequest request,
                                          Element requestBodyElement) throws SAML2Exception {

        String classMethod = "XacmlContentHandlerService:processXACMLResponse";
        Response samlResponse = null;
        String path = request.getPathInfo();
        String key = path.substring(path.indexOf(METAALIAS_KEY) + 10);
        String pepEntityID = samlRequest.getIssuer().getValue();
        if (debug.messageEnabled()) {
            debug.message(classMethod + "SOAPMessage KEY . :" + key);
            debug.message(classMethod + "pepEntityID is :" + pepEntityID);
        }
        //Retreive metadata
        boolean pdpWantAuthzQuerySigned =
                SAML2Utils.getWantXACMLAuthzDecisionQuerySigned(realm,
                        pdpEntityID, SAML2Constants.PDP_ROLE);

        if (debug.messageEnabled()) {
            debug.message(classMethod + "PDP wantAuthzQuerySigned:" +
                    pdpWantAuthzQuerySigned);
        }
        if (pdpWantAuthzQuerySigned) {
            if (samlRequest.isSigned()) {
                XACMLAuthzDecisionQueryDescriptorElement pep =
                        SAML2Utils.getSAML2MetaManager().
                                getPolicyEnforcementPointDescriptor(
                                        realm, pepEntityID);
                X509Certificate cert =
                        KeyUtil.getPEPVerificationCert(pep, pepEntityID);
                if (cert == null ||
                        !samlRequest.isSignatureValid(cert)) {
                    // error
                    debug.error(classMethod + "Invalid signature in message");
                    throw new SAML2Exception("invalidQuerySignature");

                } else {
                    debug.message(classMethod + "Valid signature found");
                }
            } else {
                debug.error("Request not signed");
                throw new SAML2Exception("nullSig");
            }
        }

        //getRequestHandlerClass
        RequestHandler handler =
                (RequestHandler) XacmlContentHandlerService.handlers.get(key);  // TODO -- THis was referencing handlers in the SOAPBindingService class.
        if (handler != null) {
            if (debug.messageEnabled()) {
                debug.message(classMethod + "Found handler");
            }

            // TODO Figure out how to implement this.
            //samlResponse = handler.handleQuery(pdpEntityID, pepEntityID,
            //        samlRequest, requestBodyElement);

            // set response attributes
            samlResponse.setID(SAML2Utils.generateID());
            samlResponse.setVersion(SAML2Constants.VERSION_2_0);
            samlResponse.setIssueInstant(new Date());
            Issuer issuer = AssertionFactory.getInstance().createIssuer();
            issuer.setValue(pdpEntityID);
            samlResponse.setIssuer(issuer);
            // end set Response Attributes

            //set Assertion attributes
            List assertionList = samlResponse.getAssertion();
            Assertion assertion = (Assertion) assertionList.get(0);

            assertion.setID(SAML2Utils.generateID());
            assertion.setVersion(SAML2Constants.VERSION_2_0);
            assertion.setIssueInstant(new Date());
            assertion.setIssuer(issuer);
            // end assertion set attributes

            // check if assertion needs to be encrypted,signed.
            String wantAssertionEncrypted =
                    SAML2Utils.getAttributeValueFromXACMLConfig(
                            realm, SAML2Constants.PEP_ROLE,
                            pepEntityID,
                            SAML2Constants.WANT_ASSERTION_ENCRYPTED);


            XACMLAuthzDecisionQueryDescriptorElement
                    pepDescriptor = SAML2Utils.
                    getSAML2MetaManager().
                    getPolicyEnforcementPointDescriptor(realm,
                            pepEntityID);

            EncInfo encInfo = null;
            boolean wantAssertionSigned = pepDescriptor.isWantAssertionsSigned();

            if (debug.messageEnabled()) {
                debug.message(classMethod +
                        " wantAssertionSigned :" + wantAssertionSigned);
            }
            if (wantAssertionSigned) {
                signAssertion(realm, pdpEntityID, assertion);
            }

            if (wantAssertionEncrypted != null
                    && wantAssertionEncrypted.equalsIgnoreCase
                    (SAML2Constants.TRUE)) {
                encInfo = KeyUtil.getPEPEncInfo(pepDescriptor, pepEntityID);

                // encrypt the Assertion
                EncryptedAssertion encryptedAssertion =
                        assertion.encrypt(
                                encInfo.getWrappingKey(),
                                encInfo.getDataEncAlgorithm(),
                                encInfo.getDataEncStrength(),
                                pepEntityID);
                if (encryptedAssertion == null) {
                    debug.error(classMethod + "Assertion encryption failed.");
                    throw new SAML2Exception("FailedToEncryptAssertion");
                }
                assertionList = new ArrayList();
                assertionList.add(encryptedAssertion);
                samlResponse.setEncryptedAssertion(assertionList);
                //reset Assertion list
                samlResponse.setAssertion(new ArrayList());
                if (debug.messageEnabled()) {
                    debug.message(classMethod + "Assertion encrypted.");
                }
            } else {
                List assertionsList = new ArrayList();
                assertionsList.add(assertion);
                samlResponse.setAssertion(assertionsList);
            }
            signResponse(samlResponse, realm, pepEntityID, pdpEntityID);

        } else {
            // error -  missing request handler.
            debug.error(classMethod + "RequestHandler not found");
            throw new SAML2Exception("missingRequestHandler");
        }
        return samlResponse;
    }

    /**
     * Signs the <code>Response</code>.
     *
     * @param response    the <code>Response<code> object.
     * @param realm       the realm of the entity.
     * @param pepEntityID Policy Enforcement Point Entity Identitifer.
     * @param pdpEntityID Policy Decision Point Entity Identifier.
     * @throws <code>SAML2Exception</code> if there is an exception.
     */
    static void signResponse(Response response,
                             String realm, String pepEntityID,
                             String pdpEntityID)
            throws SAML2Exception {
        String classMethod = "signResponse : ";
        String attrName = "wantXACMLAuthzDecisionResponseSigned";
        String wantResponseSigned =
                SAML2Utils.getAttributeValueFromXACMLConfig(realm,
                        SAML2Constants.PEP_ROLE, pepEntityID, attrName);

        if (wantResponseSigned == null ||
                wantResponseSigned.equalsIgnoreCase("false")) {
            if (debug.messageEnabled()) {
                debug.message(classMethod +
                        "Response doesn't need to be signed.");
            }
        } else {
            String pdpSignCertAlias =
                    SAML2Utils.getAttributeValueFromXACMLConfig(
                            realm, SAML2Constants.PDP_ROLE, pdpEntityID,
                            SAML2Constants.SIGNING_CERT_ALIAS);
            if (pdpSignCertAlias == null) {
                debug.error(classMethod + "PDP certificate alias is null.");
                String[] data = {realm, pdpEntityID};
                LogUtil.error(Level.INFO, LogUtil.NULL_PDP_SIGN_CERT_ALIAS, data);
                throw new SAML2Exception("missingSigningCertAlias");
            }

            if (debug.messageEnabled()) {
                debug.message(classMethod + "realm is : " + realm);
                debug.message(classMethod + "pepEntityID is :" + pepEntityID);
                debug.message(classMethod + "pdpEntityID : " + pdpEntityID);
                debug.message(classMethod + "wantResponseSigned" +
                        wantResponseSigned);
                debug.message(classMethod + "Cert Alias:" + pdpSignCertAlias);
            }
            // Don't load the KeyProvider object in static block as it can
            // cause issues when doing a container shutdown/restart.
            KeyProvider keyProvider = KeyUtil.getKeyProviderInstance();
            if (keyProvider == null) {
                debug.error(classMethod +
                        "Unable to get a key provider instance.");
                throw new SAML2Exception("nullKeyProvider");
            }
            PrivateKey signingKey = keyProvider.getPrivateKey(pdpSignCertAlias);
            X509Certificate signingCert =
                    keyProvider.getX509Certificate(pdpSignCertAlias);

            if (signingKey != null) {
                response.sign(signingKey, signingCert);
            } else {
                debug.error("Incorrect configuration for Signing Certificate.");
                throw new SAML2Exception("metaDataError");
            }
        }
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
        try {
            // Get URI and MetaAlias Data.
            String requestURI = request.getRequestURI();
            String queryMetaAlias =
                    XACML3Utils.getMetaAliasByUri(requestURI);
            String realm = XACML3Utils.getRealmByMetaAlias(queryMetaAlias);
            // Get PDP entity ID
            String pdpEntityID =
                    XACML3Utils.getEntityByMetaAlias(queryMetaAlias);
            // Bootstrap our Request Information Object for this Request.
            XACMLRequestInformation xacmlRequestInformation = new XACMLRequestInformation(contentType, requestURI,
                    queryMetaAlias, pdpEntityID,
                    realm);
            // Consume the Request Content, by parsing
            // Get Raw Content.
            xacmlRequestInformation.setOriginalContent(getRequestBody(request));
            // the Content Depending upon the Content Type.
            if (contentType.commonType().equals(CommonType.XML)) {
                parseXMLRequest(xacmlRequestInformation);
            } else {
                // Assume JSON, as no other content can get to this point.
                parseJSONRequest(xacmlRequestInformation);
            }
            // **************************************
            // Return our Request Information for
            // processing request.
            return xacmlRequestInformation;
        } catch (SAML2Exception xe) {
            debug.error(classMethod + " XACML MetaException: " + xe.getMessage(), xe);
            // TODO
            return null;
        }
    }

    /**
     * Return the Request Body Content.
     *
     * @param request
     * @return String - Request Content Body.
     */
    private final String getRequestBody(final HttpServletRequest request) {
        // Get the body content of the HTTP request,
        // remember we have no normal WS* SOAP Body, just String
        // data either XML or JSON.
        try {
            InputStream inputStream = request.getInputStream();
            return new Scanner(inputStream).useDelimiter("\\A").next();
        } catch (IOException ioe) {
            // TODO
        } catch (NoSuchElementException nse) {   // runtime exception.
            // TODO
        }
        return null;
    }

    /**
     * Private Helper to Parse the XML Request Body.
     *
     * @param xacmlRequestInformation To indicate the XACMLRequestInformation's request Node
     *                                does not exist, the XACMLRequestInformation.isRequestNodePresent() will
     *                                return false, which indicates an Invalid XACML Request.
     */
    private void parseXMLRequest(XACMLRequestInformation xacmlRequestInformation) {
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
                    new ByteArrayInputStream((XML_HEADER + xacmlRequestInformation.getOriginalContent()).getBytes());
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
        // ********************************************************
        // Now dig using XPaths to perform a validation.
        // We need to obtain the Request Node and the
        // XACMLAuthzDecisionQuery wrapper if applicable.
        //
        Node requestNode;
        Node xacmlAuthzDecisionQueryNode;

        if (document != null) {
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
                        debug.error("Node: " + xacmlAuthzDecisionQueryNode.getNodeName() + " attribute: " + xacmlAuthzDecisionQueryNode.getAttributes().item(i));
                        // TODO Verify.....
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

            // Verify we have a PDP Request Request
            try {
                // Verify we have a Request.
                XPathExpression expr = xpath.compile("/" + REQUEST);
                requestNode = (Node) expr.evaluate(document, XPathConstants.NODE);
                if (requestNode != null) {
                    // Indicate our Request Node is in fact present, either wrapped or not.
                    xacmlRequestInformation.setRequestNodePresent(true);
                    for (int i = 0; i < requestNode.getAttributes().getLength(); i++) {
                        debug.error("Node: " + requestNode.getNodeName() + " attribute: " + requestNode.getAttributes().item(i));
                        // TODO Verify....
                        // TODO
                    }
                }
            } catch (XPathExpressionException xee) {
                // Our initial Expression for a Node was invalid, we have no Request Object.
                // This could be a maintenance which has no request, but not part of specification yet...
                // Document could be bad or suspect.
                debug.error("XPathExpressException: " + xee.getMessage() + ", returning null invalid content!");
            }

        } // end of check for null document.
    }

    /**
     * Private Helper to Parse the JSON Request Body.
     *
     * @param xacmlRequestInformation -- String JSON Data
     * @return
     */
    private void parseJSONRequest(XACMLRequestInformation xacmlRequestInformation) {
        // TODO
    }


    /**
     * Marshal information into an xml string represented document.
     */
    private static String responseToXML(com.sun.identity.entitlement.xacml3.core.Response xacmlResponse) {
        StringWriter stringWriter = new StringWriter();
        try {
            JAXBContext jc = JAXBContext.newInstance("com.sun.identity.entitlement.xacml3.core");
            Marshaller marshaller = jc.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            marshaller.marshal(xacmlResponse, stringWriter);
            if (stringWriter != null) {
                return stringWriter.toString();
            }
        } catch (JAXBException jbe) {
            debug.error("JAXBException Occurred Marshaling Response Object to XML!", jbe);
        }
        return "<xacml-ctx:response xmlns:xacml-ctx=\"urn:oasis:names:tc:xacml:3.0:core:schema:wd-17\">" +
                "</xacml-ctx:response>";
    }


    private static com.sun.identity.entitlement.xacml3.core.Request xmlToRequestObject(Document xmlDocument) throws Exception {
        if (xmlDocument == null) {
            return null;
        }
        com.sun.identity.entitlement.xacml3.core.Request request = null;

        /**

         // Provide the package name where our ObjectFactory can be found for unMarshaling.
         JAXBContext jaxbContext = JAXBContext.newInstance("com.sun.identity.entitlement.xacml3.core");
         Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
         //unmarshaller.setSchema(xacmlSchema);

         // TODO -- This below fails!
         com.sun.identity.entitlement.xacml3.core.Request request =
         (com.sun.identity.entitlement.xacml3.core.Request) unmarshaller.unmarshal(xmlDocument);
         debug.error("************ xacml Request Object: " + request);

         **/

        /**
         * javax.xml.bind.UnmarshalException
         - with linked exception:
         [org.xml.sax.SAXParseException; cvc-elt.1.a: Cannot find the declaration of element 'xacml-ctx:request'.]
         */

        return request;
    }


}

