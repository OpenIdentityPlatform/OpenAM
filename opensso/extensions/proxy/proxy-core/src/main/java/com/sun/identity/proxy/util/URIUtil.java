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
 * $Id: URIUtil.java,v 1.1 2009/10/17 09:09:21 pbryan Exp $
 *
 * Copyright 2009 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.proxy.util;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Miscellaneous uniform resource identifier (URI) utility methods.
 *
 * @author Paul C. Bryan
 */
public class URIUtil {

    /**
     * Replaces the path in the supplied URI, and truncates query and
     * fragment.
     *
     * @param URI the URI to have its path replaced.
     * @param path the path to replace in the supplied URI.
     * @return the supplied URI with its path replaced.
     * @throws IllegalArgumentException if the URI violates RFC 2396.
     */
    public static URI newPath(URI uri, String path) {
        try {
            return new URI(uri.getScheme(), null, uri.getHost(), uri.getPort(), path, null, null);
        }
        catch (URISyntaxException use) {
            throw new IllegalArgumentException(use);
        }
    }
}

