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
 * @see org.forgerock.openam.sts.HttpURLConnectionFactory
 */
public class DefaultHttpURLConnectionFactory implements HttpURLConnectionFactory {
    private static final int READ_TIMEOUT = 10000;
    private static final int CONNECT_TIMEOUT = 5000;
    @Override
    public HttpURLConnection getHttpURLConnection(URL url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection)url.openConnection();
        connection.setConnectTimeout(CONNECT_TIMEOUT);
        connection.setReadTimeout(READ_TIMEOUT);
        return connection;
    }
}
