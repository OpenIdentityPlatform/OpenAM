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
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright 2015 ForgeRock AS. All rights reserved.
 */

package org.forgerock.openam.sts;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * An instance of this interface will be passed to the HttpURLConnectionWrapperFactory. There will be two implementations:
 * one that calls HttpURLConnectionManager.getConnection(url), and one that simple obtains the connection from the url
 * directly. When the HttpURLConnectionWrapperFactory is consumed in the context of OpenAM, the HttpURLConnectionManager
 * will back the implementation of this interface. However, when the HttpURLConnectionWrapperFactory is bound in the
 * context of the soap-sts, a default implementation, which provides a connection by invoking the url, will be bound.
 * This because the HttpURLConnectionManager has a static initializer which pulls in a bunch of OpenAM cruft, triggering
 * other static initializers and other dependencies that the soap-sts should not have to satisfy.
 */
public interface HttpURLConnectionFactory {
    HttpURLConnection getHttpURLConnection(URL url) throws IOException;
}
