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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.forgerock.opendj.ldap.IntermediateResponseHandler;
import org.forgerock.opendj.ldap.LdapResultHandler;
import org.forgerock.opendj.ldap.MemoryBackend;
import org.forgerock.opendj.ldap.RequestContext;
import org.forgerock.opendj.ldap.RequestHandler;
import org.forgerock.opendj.ldap.SearchResultHandler;
import org.forgerock.opendj.ldap.requests.AddRequest;
import org.forgerock.opendj.ldap.requests.BindRequest;
import org.forgerock.opendj.ldap.requests.CompareRequest;
import org.forgerock.opendj.ldap.requests.DeleteRequest;
import org.forgerock.opendj.ldap.requests.ExtendedRequest;
import org.forgerock.opendj.ldap.requests.ModifyDNRequest;
import org.forgerock.opendj.ldap.requests.ModifyRequest;
import org.forgerock.opendj.ldap.requests.Requests;
import org.forgerock.opendj.ldap.requests.SearchRequest;
import org.forgerock.opendj.ldap.responses.BindResult;
import org.forgerock.opendj.ldap.responses.CompareResult;
import org.forgerock.opendj.ldap.responses.ExtendedResult;
import org.forgerock.opendj.ldap.responses.Result;

/**
 * Hands every request to a {@link MemoryBackend}, keeping a copy of each search on the way, so a
 * test can assert on the shape of the request and not only on what came back. Serve it over
 * {@link TestLdapServer}, or in-JVM through {@code Connections.newInternalConnectionFactory}.
 */
final class RecordingLdapBackend implements RequestHandler<RequestContext> {

    private final MemoryBackend backend;
    private final List<SearchRequest> searches = Collections.synchronizedList(new ArrayList<>());

    RecordingLdapBackend(MemoryBackend backend) {
        this.backend = backend;
    }

    /** @return a snapshot of the searches received so far, oldest first. */
    List<SearchRequest> searches() {
        synchronized (searches) {
            return new ArrayList<>(searches);
        }
    }

    @Override
    public void handleSearch(RequestContext context, SearchRequest request,
            IntermediateResponseHandler intermediate, SearchResultHandler entries,
            LdapResultHandler<Result> result) {
        searches.add(Requests.copyOfSearchRequest(request));
        backend.handleSearch(context, request, intermediate, entries, result);
    }

    @Override
    public void handleAdd(RequestContext context, AddRequest request,
            IntermediateResponseHandler intermediate, LdapResultHandler<Result> result) {
        backend.handleAdd(context, request, intermediate, result);
    }

    @Override
    public void handleBind(RequestContext context, int version, BindRequest request,
            IntermediateResponseHandler intermediate, LdapResultHandler<BindResult> result) {
        backend.handleBind(context, version, request, intermediate, result);
    }

    @Override
    public void handleCompare(RequestContext context, CompareRequest request,
            IntermediateResponseHandler intermediate, LdapResultHandler<CompareResult> result) {
        backend.handleCompare(context, request, intermediate, result);
    }

    @Override
    public void handleDelete(RequestContext context, DeleteRequest request,
            IntermediateResponseHandler intermediate, LdapResultHandler<Result> result) {
        backend.handleDelete(context, request, intermediate, result);
    }

    @Override
    public <R extends ExtendedResult> void handleExtendedRequest(RequestContext context,
            ExtendedRequest<R> request, IntermediateResponseHandler intermediate,
            LdapResultHandler<R> result) {
        backend.handleExtendedRequest(context, request, intermediate, result);
    }

    @Override
    public void handleModify(RequestContext context, ModifyRequest request,
            IntermediateResponseHandler intermediate, LdapResultHandler<Result> result) {
        backend.handleModify(context, request, intermediate, result);
    }

    @Override
    public void handleModifyDN(RequestContext context, ModifyDNRequest request,
            IntermediateResponseHandler intermediate, LdapResultHandler<Result> result) {
        backend.handleModifyDN(context, request, intermediate, result);
    }
}
