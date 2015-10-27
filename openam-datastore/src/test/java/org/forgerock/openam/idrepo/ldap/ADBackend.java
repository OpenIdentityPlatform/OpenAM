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
 * Copyright 2014-2015 ForgeRock AS.
 */
package org.forgerock.openam.idrepo.ldap;

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
import org.forgerock.opendj.ldap.requests.SearchRequest;
import org.forgerock.opendj.ldap.responses.BindResult;
import org.forgerock.opendj.ldap.responses.CompareResult;
import org.forgerock.opendj.ldap.responses.ExtendedResult;
import org.forgerock.opendj.ldap.responses.Result;
import org.forgerock.opendj.ldap.responses.SearchResultEntry;
import org.forgerock.opendj.ldap.responses.SearchResultReference;

/**
 * This backend implementation is here to provide support for references in the memorybackend, so we can test IdRepo
 * behavior when the directory contains references.
 */
public class ADBackend implements RequestHandler<RequestContext> {

    private final MemoryBackend backend;

    public ADBackend(MemoryBackend memoryBackend) {
        this.backend = memoryBackend;
    }

    public void handleAdd(RequestContext requestContext, AddRequest request,
            IntermediateResponseHandler intermediateResponseHandler, LdapResultHandler<Result> resultHandler) {
        backend.handleAdd(requestContext, request, intermediateResponseHandler, resultHandler);
    }

    public void handleBind(RequestContext requestContext, int version, BindRequest request,
            IntermediateResponseHandler intermediateResponseHandler, LdapResultHandler<BindResult> resultHandler) {
        backend.handleBind(requestContext, version, request, intermediateResponseHandler, resultHandler);
    }

    public void handleCompare(RequestContext requestContext, CompareRequest request,
            IntermediateResponseHandler intermediateResponseHandler, LdapResultHandler<CompareResult> resultHandler) {
        backend.handleCompare(requestContext, request, intermediateResponseHandler, resultHandler);
    }

    public void handleDelete(RequestContext requestContext, DeleteRequest request,
            IntermediateResponseHandler intermediateResponseHandler, LdapResultHandler<Result> resultHandler) {
        backend.handleDelete(requestContext, request, intermediateResponseHandler, resultHandler);
    }

    public <R extends ExtendedResult> void handleExtendedRequest(RequestContext requestContext,
            ExtendedRequest<R> request, IntermediateResponseHandler intermediateResponseHandler,
                                                                 LdapResultHandler<R> resultHandler) {
        backend.handleExtendedRequest(requestContext, request, intermediateResponseHandler, resultHandler);
    }

    public void handleModify(RequestContext requestContext, ModifyRequest request,
            IntermediateResponseHandler intermediateResponseHandler, LdapResultHandler<Result> resultHandler) {
        backend.handleModify(requestContext, request, intermediateResponseHandler, resultHandler);
    }

    public void handleModifyDN(RequestContext requestContext, ModifyDNRequest request,
            IntermediateResponseHandler intermediateResponseHandler, LdapResultHandler<Result> resultHandler) {
        backend.handleModifyDN(requestContext, request, intermediateResponseHandler, resultHandler);
    }

    public void handleSearch(RequestContext requestContext, SearchRequest request,
            IntermediateResponseHandler intermediateResponseHandler, final SearchResultHandler searchHandler,
                             final LdapResultHandler<Result> resultHandler) {
        SearchResultHandler fakeHandler = new SearchResultHandler() {

            public boolean handleEntry(SearchResultEntry entry) {
                return searchHandler.handleEntry(entry);
            }

            public boolean handleReference(SearchResultReference reference) {
                return searchHandler.handleReference(reference);
            }

        };
        backend.handleSearch(requestContext, request, intermediateResponseHandler, fakeHandler, resultHandler);
    }
}
