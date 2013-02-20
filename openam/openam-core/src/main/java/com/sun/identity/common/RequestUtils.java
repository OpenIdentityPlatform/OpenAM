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
 * $Id: RequestUtils.java,v 1.5 2008/08/19 19:09:00 veiming Exp $
 *
 */

package com.sun.identity.common;

import java.util.StringTokenizer;

import javax.servlet.http.HttpServletRequest;

import com.sun.identity.shared.Constants;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.locale.L10NMessageImpl;
import com.iplanet.am.util.SystemProperties;

/**
 * This class provides common utilities for requests.
 */
public class RequestUtils {

    // the debug file name
    private static final String debugName = "amRequestUtils";

    // the resource bundle name
    private static final String bundleName = "amCommonUtils";

    // get instance of debug instance
    private static Debug debug = Debug.getInstance(debugName);

    // http request max content length
    private static int maxContentLength = getMaxContentLength();

    private static String rdProtocol = null;

    private static String rdHost = null;

    static {
        String redirect = SystemProperties.get(Constants.AM_REDIRECT, null);
        if (redirect != null) {
            StringTokenizer tokenizer = new StringTokenizer(redirect, ",");
            if (tokenizer.countTokens() == 2) {
                rdProtocol = tokenizer.nextToken().trim();
                rdHost = tokenizer.nextToken().trim();
            }
        }
    }

    /*
     * Returns right protocol for redirection. This method picks up configured
     * protocol and host name and returned configured protocol whenever request
     * needs to be redirected to configured host.
     * 
     * @param scheme Protocol of request. @param host Host name of request.
     * @return the right protocol for redirection.
     */
    public static String getRedirectProtocol(String scheme, String host) {
        String protocol = scheme;
        if ((rdHost != null) && (rdProtocol != null)) {
            // Check appended port number
            int idx = host.indexOf(':');

            // Check appended uri
            if (idx == -1) {
                idx = host.indexOf('/');
            }

            // Get host name only
            if (idx != -1) {
                host = host.substring(0, idx);
            }

            if (rdHost.equalsIgnoreCase(host)) {
                protocol = rdProtocol;
            }
        }

        return protocol;
    }

    /**
     * Use this method to check an HTTP servlet request size against the
     * configured limit to insure that it is not too large, and possibly being
     * sent to an OpenSSO servlet to cause a Denial of Service (DOS).
     * 
     * @param servletRequest
     *            The HTTP servlet request.
     * @throws L10NMessageImpl
     *             If the request content length is too long.
     */
    public static void checkContentLength(HttpServletRequest servletRequest)
            throws L10NMessageImpl {

        // get content length of request
        int length = servletRequest.getContentLength();

        // Check content length against configured limit
        if (length > maxContentLength) {
            Object[] args = { new Integer(length),
                    new Integer(maxContentLength) };
            throw new L10NMessageImpl(
                    bundleName, "contentLengthTooLarge", args);
        } else {
            return;
        }

    }

    /*
     * Get the maxContentLength.
     */
    private static int getMaxContentLength() {

        // set the default content length to 16k
        int maxContentLength = 16384;

        // get the maxContentLength from properties file
        String maxContentLengthProp = SystemProperties.get(
                Constants.SERVICES_COMM_SERVER_PLLREQUEST_MAX_CONTENT_LENGTH);

        try {
            maxContentLength = Integer.parseInt(maxContentLengthProp);
        } catch (NumberFormatException ne) {
            if (debug.messageEnabled()) {
                debug.message("RequestUtils: invalid property for " +
                    "maxContentLength [" + maxContentLengthProp + "] property");
            }
        }

        return maxContentLength;

    }

}
