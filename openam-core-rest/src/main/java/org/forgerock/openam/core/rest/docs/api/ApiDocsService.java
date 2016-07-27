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
 * Copyright 2016 ForgeRock AS.
 */
package org.forgerock.openam.core.rest.docs.api;

import static org.forgerock.json.resource.Requests.newApiRequest;
import static org.forgerock.openam.utils.StringUtils.isNotEmpty;

import javax.inject.Inject;
import javax.inject.Named;

import org.asciidoctor.Asciidoctor;
import org.asciidoctor.AttributesBuilder;
import org.asciidoctor.OptionsBuilder;
import org.asciidoctor.Placement;
import org.asciidoctor.SafeMode;
import org.forgerock.api.markup.ApiDocGenerator;
import org.forgerock.api.models.ApiDescription;
import org.forgerock.http.header.ContentTypeHeader;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.http.protocol.Status;
import org.forgerock.json.resource.ResourcePath;
import org.forgerock.json.resource.Router;
import org.forgerock.openam.http.annotations.Contextual;
import org.forgerock.openam.http.annotations.Get;
import org.forgerock.openam.rest.ResourceRouter;
import org.forgerock.services.context.Context;

/**
 * This service provides an HTML representation of the OpenAM REST API.
 *
 * @since 14.0.0
 */
public class ApiDocsService {

    private static final ContentTypeHeader HTML_CONTENT_TYPE = ContentTypeHeader.valueOf("text/html; charset=UTF-8");
    private static final ContentTypeHeader TEXT_CONTENT_TYPE = ContentTypeHeader.valueOf("text/plain; charset=UTF-8");
    private static final int SECTION_NUMBERING_DEPTH = 5;
    private static final int TOC_LEVELS = 5;

    private final Asciidoctor asciidoctor = Asciidoctor.Factory.create();
    private final Router realmRouter;

    /**
     * Create an instance of the {@link ApiDocsService}.
     *
     * @param realmResourceRouter The {@link ResourceRouter} that can be used to retrieve the CREST API.
     */
    @Inject
    public ApiDocsService(@Named("RealmResourceRouter") ResourceRouter realmResourceRouter) {
        this.realmRouter = realmResourceRouter.getRouter();
    }

    /**
     * Handle the request for the OpenAM REST API docs. This will make an internal call to the CREST API endpoint to
     * retrieve the API description and use that to generate the docs in HTML.
     *
     * @param context The request context.
     * @param request The HTTP request.
     *
     * @return The {@link Response} containing the OpenAM REST API docs as HTML.
     */
    @Get
    public Response handle(@Contextual Context context, @Contextual Request request) {
        ApiDescription apiDescription = realmRouter.handleApiRequest(context, newApiRequest(ResourcePath.empty()));
        String asciiDocMarkup = ApiDocGenerator.execute("OpenAM API", apiDescription, null);

        String query = request.getUri().getQuery();
        if (isNotEmpty(query) && query.contains("format=asciidoc")) {
            Response response = new Response(Status.OK);
            response.getHeaders().add(TEXT_CONTENT_TYPE);
            response.setEntity(asciiDocMarkup);
            return response;
        }

        String html;
        synchronized (asciidoctor) {
            html = asciidoctor.render(asciiDocMarkup,
                    OptionsBuilder.options()
                            .attributes(AttributesBuilder.attributes()
                                    .tableOfContents(Placement.LEFT)
                                    .sectNumLevels(SECTION_NUMBERING_DEPTH)
                                    .attribute("toclevels", TOC_LEVELS)
                                    .get())
                            .safe(SafeMode.SAFE)
                            .headerFooter(true)
                            .get());
        }

        Response response = new Response(Status.OK);
        response.getHeaders().add(HTML_CONTENT_TYPE);
        response.setEntity(html);
        return response;
    }
}
