/**
 * Copyright 2005-2024 Qlik
 *
 * The contents of this file are subject to the terms of one of the following
 * open source licenses: Apache 2.0 or or EPL 1.0 (the "Licenses"). You can
 * select the license that you prefer but you may not use this file except in
 * compliance with one of these Licenses.
 *
 * You can obtain a copy of the Apache 2.0 license at
 * http://www.opensource.org/licenses/apache-2.0
 *
 * You can obtain a copy of the EPL 1.0 license at
 * http://www.opensource.org/licenses/eclipse-1.0
 *
 * See the Licenses for the specific language governing permissions and
 * limitations under the Licenses.
 *
 * Alternatively, you can obtain a royalty free commercial license with less
 * limitations, transferable or non-transferable, directly at
 * https://restlet.talend.com/
 *
 * Restlet is a registered trademark of QlikTech International AB.
 */

package org.forgerock.openam.rest.jakarta.servlet.internal;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import jakarta.servlet.ServletContext;

import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.engine.local.Entity;
import org.restlet.representation.InputRepresentation;
import org.restlet.representation.Representation;
import org.restlet.service.MetadataService;

/**
 * Local entity based on a Servlet context's resource file.
 *
 * @author Thierry Boileau
 */
public class ServletWarEntity extends Entity {
    /**
     * List of children files if it is a directory. We suppose that in a WAR
     * entity, this list does not change, and thus can be cached during the
     * request processing.
     */
    private List<Entity> children = null;

    /** Is this file a directory? */
    private final boolean directory;

    /** The full name of the file (without trailing "/"). */
    private final String fullName;

    /** The relative path of the file inside the context. */
    private final String path;

    /** The Servlet context to use. */
    private final ServletContext servletContext;

    /**
     * Constructor.
     *
     * @param servletContext
     *            The parent Servlet context.
     * @param path
     *            The entity path.
     * @param metadataService
     *            The metadata service to use.
     */
    public ServletWarEntity(ServletContext servletContext, String path,
                            MetadataService metadataService) {
        super(metadataService);
        this.children = null;
        this.servletContext = servletContext;
        this.path = path;

        if (path.endsWith("/")) {
            this.directory = true;
            this.fullName = path.substring(0, path.length() - 1);
            Set<?> childPaths = getServletContext().getResourcePaths(path);

            if (childPaths != null && !childPaths.isEmpty()) {
                this.children = new ArrayList<Entity>();

                for (Object childPath : childPaths) {
                    if (!childPath.equals(this.path)) {
                        this.children.add(new org.forgerock.openam.rest.jakarta.servlet.internal.ServletWarEntity(
                                this.servletContext, (String) childPath,
                                metadataService));
                    }
                }
            }
        } else {
            this.fullName = path;
            Set<?> childPaths = getServletContext().getResourcePaths(path);

            if (childPaths != null && !childPaths.isEmpty()) {
                this.directory = true;
                this.children = new ArrayList<Entity>();

                for (Object childPath : childPaths) {
                    if (!childPath.equals(this.path)) {
                        this.children.add(new org.forgerock.openam.rest.jakarta.servlet.internal.ServletWarEntity(
                                this.servletContext, (String) childPath,
                                metadataService));
                    }
                }
            } else {
                this.directory = false;
            }
        }
    }

    @Override
    public boolean exists() {
        boolean result = false;

        try {
            result = (isDirectory() && getChildren() != null)
                    || (isNormal() && getServletContext()
                    .getResource(this.path) != null);
        } catch (MalformedURLException e) {
            Context.getCurrentLogger().log(Level.WARNING,
                    "Unable to test the existence of the WAR resource", e);
        }

        return result;
    }

    @Override
    public List<Entity> getChildren() {
        return this.children;
    }

    @Override
    public String getName() {
        int index = this.fullName.lastIndexOf("/");

        if (index != -1) {
            return this.fullName.substring(index + 1);
        }

        return this.fullName;
    }

    @Override
    public Entity getParent() {
        Entity result = null;
        int index = this.fullName.lastIndexOf("/");

        if (index != -1) {
            result = new ServletWarEntity(getServletContext(),
                    this.fullName.substring(0, index + 1), getMetadataService());
        }

        return result;
    }

    @Override
    public Representation getRepresentation(MediaType defaultMediaType,
                                            int timeToLive) {
        Representation result = null;

        try {
            URL resource = getServletContext().getResource(path);
            if (resource != null) {
                URLConnection connection = resource.openConnection();
                result = new InputRepresentation(connection.getInputStream(),
                        defaultMediaType);

                // Sets the modification date
                result.setModificationDate(new Date(connection
                        .getLastModified()));

                // Sets the expiration date
                if (timeToLive == 0) {
                    result.setExpirationDate(null);
                } else if (timeToLive > 0) {
                    result.setExpirationDate(new Date(System
                            .currentTimeMillis() + (1000L * timeToLive)));
                }
            }
        } catch (IOException e) {
            Context.getCurrentLogger().log(Level.WARNING,
                    "Error getting the WAR resource.", e);
        }
        return result;
    }

    /**
     * Returns the Servlet context to use.
     *
     * @return The Servlet context to use.
     */
    public ServletContext getServletContext() {
        return this.servletContext;
    }

    @Override
    public boolean isDirectory() {
        return this.directory;
    }

    @Override
    public boolean isNormal() {
        return !isDirectory();
    }

}

