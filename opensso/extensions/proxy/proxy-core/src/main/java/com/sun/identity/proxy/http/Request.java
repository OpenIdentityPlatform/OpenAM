/* The contents of this file are subject to the terms
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
 * $Id: Request.java,v 1.8 2009/10/22 01:18:22 pbryan Exp $
 *
 * Copyright 2009 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.proxy.http;

import com.sun.identity.proxy.util.IntegerUtil;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.Principal;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * An HTTP request message.
 *
 * @author Paul C. Bryan
 */
public class Request extends Message
{
    /** Splits string using colon delimiter. */
    private static final Pattern DELIM_COLON = Pattern.compile(":");

    /** The method to be performed on the resource. */
    public String method = null;

    /** The fully-qualified URI of the resource being accessed. */
    public URI uri = null;

    /** The user principal that the container associated with the request. */
    public Principal principal = null;

    /** A local context object associated with the request client. */
    public Session session = null;

    /** Allows information to be attached to the request for downstream handlers. */
    public Map<String, Object> attributes = new HashMap<String, Object>();

    /**
     * Resolves the request URI based on the request URI variable and optional
     * Host header. This allows the request URI to contain a raw IP address,
     * while the Host header resolves the hostname and port that the remote
     * client used to access it.
     * <p>
     * Note: This method returns a normalized URI, as though returned by the
     * {@link URI#normalize} method.
     *
     * @return the resolved URI value.
     */
    public URI resolveHostURI() {
        URI uri = this.uri;
        String header = (headers != null ? headers.first("Host") : null);
        if (uri != null && header != null) {
            String[] hostport = DELIM_COLON.split(header, 2);
            int port = (hostport.length == 2 ? IntegerUtil.parseInt(hostport[1], -1) : -1);
            try {
                uri = new URI(uri.getScheme(), null, hostport[0], port, "/", null, null).resolve(
                new URI(uri.getScheme(), null, uri.getHost(), uri.getPort(), null, null, null).relativize(uri));
            }
            catch (URISyntaxException use) {
            }
        }
        return uri;
    }
}
