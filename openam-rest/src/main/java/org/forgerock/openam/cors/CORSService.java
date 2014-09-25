/*
* The contents of this file are subject to the terms of the Common Development and
* Distribution License (the License). You may not use this file except in compliance with the
* License.
*
* You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
* specific language governing permission and limitations under the License.
*
* When distributing Covered Software, include this CDDL Header Notice in each file and include
* the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
* Header, with the fields enclosed by brackets [] replaced by your own identifying
* information: "Portions copyright [year] [name of copyright owner]".
*
* Copyright 2014 ForgeRock AS.
*/
package org.forgerock.openam.cors;

import com.sun.identity.shared.debug.Debug;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.forgerock.openam.cors.utils.CSVHelper;
import org.forgerock.util.Reject;

/**
 * See <a href="http://www.w3.org/TR/cors/">http://www.w3.org/TR/cors/</a> for further information.
 *
 * Once constructed, this service serves a number of purposes:
 *
 * <ul>
 * <li>Validates requests are CORS (by analysis of the Origin header).
 * <li>Determines if a request is to follow the preflight or actual request flow.
 * <li>Verifies the requests are valid in this configuration of CORS (through comparison against pre-
 * configured values.
 * <li>Alters the responses such that they are valid for CORS.
 * </ul>
 *
 * When comparing Strings HTTP methods and Origin headers are case-sensitive.
 *
 */
public class CORSService {

    private static final Debug DEBUG = Debug.getInstance("frRest");

    private final CSVHelper csvHelper = new CSVHelper();

    private final List<String> acceptedOrigins;
    private final List<String> acceptedMethods;
    private final List<String> acceptedHeaders;
    private final List<String> exposedHeaders;
    private final String expectedHostname;
    private final int maxAge;
    private final boolean allowCredentials;

    /**
     * Constructor setting all necessary values to create a usable CORSService.
     * If the maxAge parameter is less than zero, we will set it to zero here.
     * The acceptedHeaders and exposedHeaders lists must contain at least one
     * entry each.
     *
     * @param acceptedOrigins A list of origins from which we accept CORS requests
     * @param acceptedMethods A list of HTTP methods of whose type we accept CORS requests
     * @param acceptedHeaders (Optional) A list of HTTP headers which - if included - in the request don't make it abort
     * @param exposedHeaders (Optional) The list of headers which the user-agent can expose to its CORS-client
     * @param maxAge The maximum cache-length of the pre-flight response by the CORS-client in seconds
     * @param allowCredentials Whether we include the allow credentials header
     * @param expectedHostname (Optional) the name of the host the request should be headed to in its Host header
     * @throws IllegalArgumentException If the acceptedOrigins or acceptedMethods params are null or empty
     */
    public CORSService(final List<String> acceptedOrigins, final List<String> acceptedMethods,
                       List<String> acceptedHeaders, List<String> exposedHeaders,
                       int maxAge, final boolean allowCredentials, String expectedHostname) {

        Reject.ifTrue(acceptedOrigins == null || acceptedOrigins.size() < 1, "AcceptedOrigins must have at least one value.");
        Reject.ifTrue(acceptedMethods == null || acceptedMethods.size() < 1, "AcceptedOrigins must have at least one value.");

        if (maxAge < 0) {
            maxAge = 0;
        }

        if (acceptedHeaders == null) {
            acceptedHeaders = new ArrayList<String>();
        }

        if (exposedHeaders == null) {
            exposedHeaders = new ArrayList<String>();
        }

        this.acceptedOrigins = acceptedOrigins;
        this.acceptedMethods = acceptedMethods;
        this.acceptedHeaders = acceptedHeaders;
        this.exposedHeaders = exposedHeaders;
        this.allowCredentials = allowCredentials;
        this.maxAge = maxAge;
        this.expectedHostname = expectedHostname;
    }


    /**
     * Entry point for classes using the service. Validates requests against the
     * CORS specification, and add headers required to the response.
     *
     * @param req CORS HTTP request
     * @param res HTTP response
     * @return true if the caller is to continue processing the request
     */
    public boolean handleRequest(final HttpServletRequest req, final HttpServletResponse res) {
        if (req.getHeader(CORSConstants.ORIGIN) == null) {
            return true;
        }

        if (!isValidCORSRequest(req)) {
            return false;
        }

        if (isPreflightFlow(req)) {
            handlePreflightFlow(req, res);
            return false;
        } else {
            handleActualRequestFlow(req, res);
            return true;
        }

    }

    /**
     * Handles the preflight flow.
     *
     * Validates the Origin header that the request is of type OPTIONS, and
     * sets the following headers:
     *
     * <ul>
     * <li><code>Access-Control-Allow-Methods</code>
     * <li><code>Access-Control-Allow-Headers</code>
     * <li><code>Access-Control-Max-Age</code>
     * </ul>
     *
     * @param req The request
     * @param res The response
     * @return true if the request was valid and successfully handled
     */
    private void handlePreflightFlow(final HttpServletRequest req, final HttpServletResponse res) {

        final String originHeader = req.getHeader(CORSConstants.ORIGIN);

        if (!isPreflightValid(req)) {
            return;
        }

        res.setHeader(CORSConstants.AC_ALLOW_METHODS, csvHelper.listToCSVString(acceptedMethods));

        if (acceptedHeaders.size() > 0) {
            res.setHeader(CORSConstants.AC_ALLOW_HEADERS, csvHelper.listToCSVString(acceptedHeaders));
        }

        if (maxAge > 0) {
            res.setIntHeader(CORSConstants.AC_MAX_AGE, maxAge);
        }

        addOriginAndCredsHeaders(res, originHeader);
    }

    /**
     * Handles the actual request flow.
     *
     * Validates the Origin header and sets the <code>Access-Control-Expose-Headers</code>
     * header on the response listing the headers to allow the receiver to expose.
     *
     * Then adds the <code>Access-Control-Allow-Origins</code>, and
     * <code>Access-Control-Allow-Credentials</code> headers if appropriate.
     *
     * @param req The request
     * @param res The response
     */
    private void handleActualRequestFlow(final HttpServletRequest req, final HttpServletResponse res) {

        final String originHeader = req.getHeader(CORSConstants.ORIGIN);

        if(exposedHeaders.size() > 0) {
            res.setHeader(CORSConstants.AC_EXPOSE_HEADERS, csvHelper.listToCSVString(exposedHeaders));
        }

        addOriginAndCredsHeaders(res, originHeader);
    }

    /**
     * Determines if this request should use the preflight flow or the normal flow.
     * Selects preflight if:
     *
     * <ul>
     * <li>The method is of type OPTIONS and
     * <li>A header exists with the name Access-Control-Request-Method</code> and a value
     * </ul>
     *
     * @param req The request
     * @return true if this request is to use the preflight flow
     */
    private boolean isPreflightFlow(final HttpServletRequest req) {

        final String reqMethodHeader = req.getHeader(CORSConstants.AC_REQUEST_METHOD);

        return CORSConstants.HTTP_OPTIONS.equals(req.getMethod())
                && reqMethodHeader != null
                && !reqMethodHeader.isEmpty();
    }

    /**
     * Checks whether this preflight request should be handled or if it's invalid.
     * Logs the reason for failure if it's invalid, and returns a boolean to the
     * calling function indicating success/failure. Validation comprises checking
     * that:
     *
     * <ul>
     * <li>The method of the request is OPTIONS
     * <li>The method contained in the <code>Access-Control-Request-Method</code> header is
     *    contained within the list of allowed methods
     * <li>If the <code>Access-Control-Request-Headers</code> header is set, check that each
     *    entry is contained within the list of allowed headers
     * </ul>
     *
     * @param req The preflight request
     * @return true if the preflight request contains all necessary information
     */
    private boolean isPreflightValid(final HttpServletRequest req) {

        if (!CORSConstants.HTTP_OPTIONS.equals(req.getMethod())) {
            DEBUG.warning("CORS Fail - Preflight request method is not HTTP OPTIONS.");
            return false;
        }

        if (!acceptedMethods.contains(req.getHeader(CORSConstants.AC_REQUEST_METHOD))) {
            DEBUG.warning("CORS Fail - Preflight request did not contain the "
                    + CORSConstants.AC_REQUEST_METHOD + " header.");
            return false;
        }

        //only apply the rule if we have a header
        if (req.getHeader(CORSConstants.AC_REQUEST_HEADERS) != null) {

            String headerCSVList = req.getHeader(CORSConstants.AC_REQUEST_HEADERS);
            List<String> headerList = csvHelper.csvStringToList(headerCSVList, true);

            if (!acceptedHeaders.containsAll(headerList)) {
                DEBUG.warning("CORS Fail - Preflight request contained the "
                        + CORSConstants.AC_REQUEST_HEADERS + " headers with an invalid value.");
                return false;
            }
        }

        return true;
    }

    /**
     * Detrermines if the request contains a whitelisted Origin, is a method type which
     * this instantiation of the CORSService supports, and - if supplied with a hostname
     * at configtime - whether or not the hostname in the request matches the
     * expected hostname.
     *
     * @param req The request
     * @return true if we determine the request to be valid
     */
    private boolean isValidCORSRequest(final HttpServletRequest req) {
        final String hostHeader = req.getHeader(CORSConstants.HOST);
        final String originHeader = req.getHeader(CORSConstants.ORIGIN);

        //first filter on whether there's an ORIGIN header.
        if (originHeader == null || originHeader.isEmpty()) {
            DEBUG.warning("CORS Fail - Request did not contain an origin header.");
            return false;
        }

        if (!acceptedOrigins.contains(CORSConstants.ALL)) {
            //if we're from a valid origin or not
            if (!acceptedOrigins.contains(originHeader)) {
                DEBUG.warning("CORS Fail - Requested origin comes from a location not whitelisted.");
                return false;
            }
        }

        //check we are the host we're supposed to be
        if (expectedHostname != null && !expectedHostname.isEmpty() && !expectedHostname.equals(hostHeader)) {
            DEBUG.warning("CORS Fail - Expected hostname does not equal actual hostname.");
            return false;
        }

        //then look to see if one of the method types allowed for CORS from the configuration
        if (!acceptedMethods.contains(req.getMethod())) {
            DEBUG.warning("CORS Fail - Requested HTTP method has not been whitelisted.");
            return false;
        }

        return true;
    }

    /**
     * Finalises the response, by including headers which are required to be
     * added to both preflight and normal flow responses:
     *
     * <ul>
     * <li><code>Access-Control-Allow-Credentials</code> if enabled and
     * <li><code>Access-Control-Allow-Origin</code>
     * </ul>
     *
     * If the <code>Access-Control-Allow-Credentials</code> header is set, it must be set to the
     * case-sensitive value of <code>true</code>.
     *
     * @param res The HTTP response we're about to send out and to which we append more details
     * @param originHeader The origin location of the request which generated this response,
     *                     used as the <code>Access-Control-Allow-Origin</code> if we are not open
     *                     to everyone.
     */
    private void addOriginAndCredsHeaders(final HttpServletResponse res, final String originHeader) {

        if (allowCredentials) {
            res.setHeader(CORSConstants.VARY, CORSConstants.ORIGIN);
            res.setHeader(CORSConstants.AC_ALLOW_ORIGIN, originHeader);
            res.setHeader(CORSConstants.AC_ALLOW_CREDS, CORSConstants.AC_CREDENTIALS_TRUE);
        } else {
            if (acceptedOrigins.contains(CORSConstants.ALL)) {
                res.setHeader(CORSConstants.AC_ALLOW_ORIGIN, CORSConstants.ALL);
            } else {
                res.setHeader(CORSConstants.AC_ALLOW_ORIGIN, originHeader);
            }
        }

    }

}
