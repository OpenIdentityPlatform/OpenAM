/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
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
 * $Id: UriInfoImpl.java,v 1.2 2009/06/11 05:29:46 superpat7 Exp $
 */

package org.opensso.c1demoserver.service;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

public class UriInfoImpl implements UriInfo {

    String absolutePath;

    public UriInfoImpl(String absolutePath)
    {
        this.absolutePath = absolutePath;
    }

    public String getPath() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public String getPath(boolean arg0) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public List<PathSegment> getPathSegments() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public List<PathSegment> getPathSegments(boolean arg0) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public URI getRequestUri() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public UriBuilder getRequestUriBuilder() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public URI getAbsolutePath() {
        try {
            return new URI(absolutePath);
        } catch (URISyntaxException ex) {
            
        }
        return null;
    }

    public UriBuilder getAbsolutePathBuilder() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public URI getBaseUri() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public UriBuilder getBaseUriBuilder() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public MultivaluedMap<String, String> getPathParameters() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public MultivaluedMap<String, String> getPathParameters(boolean arg0) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public MultivaluedMap<String, String> getQueryParameters() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public MultivaluedMap<String, String> getQueryParameters(boolean arg0) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public List<String> getMatchedURIs() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public List<String> getMatchedURIs(boolean arg0) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public List<Object> getMatchedResources() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
