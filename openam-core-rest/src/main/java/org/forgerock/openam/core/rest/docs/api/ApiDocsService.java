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

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.forgerock.json.resource.Requests.newApiRequest;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;

import javax.inject.Inject;
import javax.inject.Named;

import org.asciidoctor.Asciidoctor;
import org.asciidoctor.AttributesBuilder;
import org.asciidoctor.OptionsBuilder;
import org.asciidoctor.Placement;
import org.asciidoctor.SafeMode;
import org.forgerock.api.markup.ApiDocGenerator;
import org.forgerock.api.models.ApiDescription;
import com.google.common.base.Joiner;
import com.google.common.io.Files;
import org.forgerock.http.header.ContentTypeHeader;
import org.forgerock.http.io.FileBranchingStream;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.http.protocol.Status;
import org.forgerock.json.resource.ResourcePath;
import org.forgerock.json.resource.Router;
import org.forgerock.openam.http.ApiDescriptorFilter;
import org.forgerock.openam.http.annotations.Contextual;
import org.forgerock.openam.http.annotations.Get;
import org.forgerock.services.context.RootContext;
import org.forgerock.services.descriptor.Describable;

import com.sun.identity.shared.debug.Debug;

/**
 * This service provides an HTML representation of the OpenAM REST API.
 *
 * @since 14.0.0
 */
public class ApiDocsService implements Describable.Listener {

    private static final ContentTypeHeader HTML_CONTENT_TYPE = ContentTypeHeader.valueOf("text/html; charset=UTF-8");
    private static final ContentTypeHeader TEXT_CONTENT_TYPE = ContentTypeHeader.valueOf("text/plain; charset=UTF-8");
    private static final int SECTION_NUMBERING_DEPTH = 5;
    private static final int TOC_LEVELS = 5;

    private final Asciidoctor asciidoctor;
    private final Router rootRouter;
    private final Debug debug;
    private volatile Future<ApiDescription> description;
    private volatile Future<File> docs;
    private volatile Future<File> asciidoc;

    /**
     * Create an instance of the {@link ApiDocsService}.
     *
     * @param rootRouter The {@link Router} that can be used to retrieve the CREST API.
     */
    @Inject
    public ApiDocsService(@Named("CrestRootRouter") Router rootRouter, @Named("frRest") Debug debug) {
        asciidoctor = initializeAsciidoctor();
        this.rootRouter = rootRouter;
        this.debug = debug;
        this.rootRouter.addDescriptorListener(this);
    }

    /**
     * Creates the {@link Asciidoctor} instance for later use.
     * Depending on the container used to host OpenAM, Asciidoctor may struggle to find the asciidoctor.rb source file
     * on the classpath.
     * Under normal circumstances Asciidoctor finds all JAR files on the classpath that has a {@literal specifications}
     * folder in it, then parses all the .gemspec files in that folder to figure out the exact locations of the (J)Ruby
     * gems. Example resource URL: {@code file:/path/to/asciidoctor.jar!specifications/}.
     * When using WildFly (or JBoss EAP) containers, this gets a little bit more tricky, because the resource URLs
     * returned will always have vfs protocol. As both Asciidoctor and JRuby are unaware of the internals of such
     * URLs, they will handle the returned URL just as if it was a file path. Example resource URL: {@code
     * vfs:/path/to/asciidoctor.jar/specifications}.
     * Since in the vfs case the URL points to a non-existent folder, JRuby will then fail to find the gemspec files,
     * and hence it will also fail to find the asciidoctor gem. To overcome this problem we are using a different
     * factory method that takes GEM_PATH as parameter. The GEM_PATH parameter needs to point to the parent directory of
     * the {@literal gems} and the {@literal specifications} folders, so OpenAM just looks for those folders on the
     * classpath and figures out the url to the parent directory (which will be the path to the JAR file essentially
     * since both folders are at the root of the JAR files).
     * To minimize the impact, in case VFS is not in use, we just fallback to the default factory method.
     *
     * @return An {@link Asciidoctor} instance.
     * @see <a href="http://schneems.com/2014/04/15/gem-path.html">GEM_PATH variable</a>
     */
    private Asciidoctor initializeAsciidoctor() {
        List<String> gemPaths = new ArrayList<>();
        try {
            for (URL url : Collections.list(Thread.currentThread().getContextClassLoader().getResources("gems"))) {
                if ("vfs".equals(url.getProtocol())) {
                    String path;
                    try {
                        URI uri = url.toURI();
                        uri = uri.getPath().endsWith("/") ? uri.resolve("..") : uri.resolve(".");
                        path = uri.getSchemeSpecificPart();
                    } catch (URISyntaxException urise) {
                        path = url.getPath();
                    }
                    gemPaths.add(path.endsWith("/") ? path.substring(0, path.length() - 1) : path);
                } else {
                    break;
                }
            }
        } catch (IOException ignored) {
        }
        if (gemPaths.isEmpty()) {
            return Asciidoctor.Factory.create();
        } else {
            return Asciidoctor.Factory.create(Joiner.on(":").join(gemPaths));
        }
    }

    /**
     * Handle the request for the OpenAM REST API docs. This will make an internal call to the CREST API endpoint to
     * retrieve the API description and use that to generate the docs in HTML.
     *
     * @param request The HTTP request.
     *
     * @return The {@link Response} containing the OpenAM REST API docs as HTML.
     */
    @Get
    public Response handle(@Contextual Request request) {
        if (!ApiDescriptorFilter.State.INSTANCE.isEnabled()) {
            return new Response(Status.NOT_IMPLEMENTED);
        }
        Response response = new Response(Status.OK);
        Future<File> input;
        if ("asciidoc".equals(request.getForm().getFirst("format"))) {
            response.getHeaders().add(TEXT_CONTENT_TYPE);
            input = asciidoc;
        } else {
            response.getHeaders().add(HTML_CONTENT_TYPE);
            input = docs;
        }
        try {
            response.setEntity(new FileBranchingStream(input.get()));
        } catch (InterruptedException | ExecutionException | IOException e) {
            debug.error("ApiDocsService#handle :: Could not read API docs", e);
            return new Response(Status.INTERNAL_SERVER_ERROR).setCause(e);
        }
        return response;
    }

    private File getDocs(File asciidoc) throws IOException {
        File docs = File.createTempFile("openam-api.", ".html");
        docs.deleteOnExit();
        try (Reader reader = new FileReader(asciidoc); Writer writer = new FileWriter(docs)) {
            asciidoctor.render(
                    reader,
                    writer,
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
        return docs;
    }

    private File getAsciiDoc(ApiDescription description) throws IOException {
        String asciiDocMarkup = ApiDocGenerator.execute("OpenAM API", description, null);
        File asciidoc = File.createTempFile("openam-api.", ".asciidoc");
        asciidoc.deleteOnExit();
        Files.write(asciiDocMarkup, asciidoc, StandardCharsets.UTF_8);
        return asciidoc;
    }

    private ApiDescription getDescription() {
        return rootRouter.handleApiRequest(new RootContext(), newApiRequest(ResourcePath.empty()));
    }

    @Override
    public synchronized void notifyDescriptorChange() {
        debug.error("API Descriptor has changed - regenerating docs");
        ExecutorService executorService = new ThreadPoolExecutor(1, 1, 0, SECONDS, new LinkedBlockingQueue<Runnable>());
        final Future<ApiDescription> description = executorService.submit(new Callable<ApiDescription>() {
            @Override
            public ApiDescription call() throws Exception {
                debug.message("Initialising API Description");
                ApiDescription apiDescription = null;
                try {
                    apiDescription = getDescription();
                    return apiDescription;
                } catch (Exception e) {
                    debug.message("API Description failed", e);
                    throw e;
                } finally {
                    debug.message("API Description initialised: {}", apiDescription);
                }
            }
        });
        final Future<File> asciidoc = executorService.submit(new Callable<File>() {
            @Override
            public File call() throws Exception {
                debug.message("Initialising API Docs Asciidoc");
                try {
                    return getAsciiDoc(description.get());
                } catch (Exception e) {
                    debug.message("API Asciidoc failed", e);
                    throw e;
                } finally {
                    debug.message("Asciidoc initialised");
                }
            }
        });
        final Future<File> docs = executorService.submit(new Callable<File>() {
            @Override
            public File call() throws Exception {
                debug.message("Initialising API Docs HTML");
                try {
                    return getDocs(asciidoc.get());
                } catch (Exception e) {
                    debug.message("API HTML failed", e);
                    throw e;
                } finally {
                    debug.message("HTML initialised");
                }
            }
        });
        executorService.shutdown();
        this.description = description;
        this.asciidoc = asciidoc;
        this.docs = docs;
    }
}
