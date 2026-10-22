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
 * Copyright 2026 3A Systems, LLC.
 */

package com.sun.identity.security.cert;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;

import org.forgerock.opendj.ldap.Connections;
import org.forgerock.opendj.ldap.DecodeOptions;
import org.forgerock.opendj.ldap.LDAPClientContext;
import org.forgerock.opendj.ldap.LDAPListener;
import org.forgerock.opendj.ldap.MemoryBackend;
import org.forgerock.opendj.ldap.requests.SearchRequest;

import com.forgerock.reactive.ServerConnectionFactoryAdapter;

/**
 * A loopback LDAP server over a {@link MemoryBackend}, standing in for an {@code ldap://}
 * distribution point, and recording every search it is sent so a test can assert on the shape of
 * the request and not only on what came back.
 *
 * <p>Anonymous only: the SDK's listener drops a connection whose bind request carries a control,
 * and {@code AMCertStore} always sends one. For the CRL directory, serve a
 * {@link RecordingLdapBackend} in-JVM instead.
 */
final class TestLdapServer implements Closeable {

    private final RecordingLdapBackend recorder;
    private final LDAPListener listener;

    @SuppressWarnings("deprecation") // the adapter is the SDK's own bridge for a RequestHandler
    TestLdapServer(MemoryBackend backend) throws IOException {
        recorder = new RecordingLdapBackend(backend);
        listener = new LDAPListener("127.0.0.1", 0, new ServerConnectionFactoryAdapter(
                new DecodeOptions(),
                Connections.<LDAPClientContext>newServerConnectionFactory(recorder)));
    }

    int port() {
        return listener.firstSocketAddress().getPort();
    }

    /** @return a snapshot of the searches received so far, oldest first. */
    List<SearchRequest> searches() {
        return recorder.searches();
    }

    @Override
    public void close() {
        listener.close();
    }
}
