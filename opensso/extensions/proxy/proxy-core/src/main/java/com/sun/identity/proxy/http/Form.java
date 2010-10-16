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
 * $Id: Form.java,v 1.4 2009/10/17 09:10:23 pbryan Exp $
 *
 * Copyright 2009 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.proxy.http;

import com.sun.identity.proxy.http.Request;
import com.sun.identity.proxy.io.Streamer;
import com.sun.identity.proxy.util.StringListMap;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.List;

/**
 * Form parameters, a case-sensitive multiple-valued map. The form can be
 * read from and written to request objects as query parameters and form
 * entities.
 *
 * @author Paul C. Bryan
 */
public class Form extends StringListMap
{
    /**
     * Parses the query parameters of a request and returns them in a new
     * form object.
     *
     * @param request the request to be parsed.
     */
    public static Form getQueryParams(Request request) {
        Form form = new Form();
        form.parseQueryParams(request);
        return form;
    }

    /**
     * Parses the query parameters of a request and stores them in this object.
     *
     * @param request the request to be parsed.
     */
    public void parseQueryParams(Request request) {
        String query = request.uri.getRawQuery();
        if (query != null) {
            parse(query);
        }
    }

    /**
     * Parses the URL-encoded form entity of a request and returns them in a
     * new form object.
     *
     * @param request the request to be parsed.
     * @throws IOException if an I/O exception occurs.
     */
    public static Form getFormEntity(Request request) throws IOException {
        Form form = new Form();
        form.parseFormEntity(request);
        return form;
    }

    /**
     * Parses the URL-encoded form entity of a request and stores them in this
     * object.
     *
     * @param request the request to be parsed.
     * @throws IOException if an I/O exception occurs.
     */
    public void parseFormEntity(Request request) throws IOException {
        if (request != null & request.entity != null && request.headers != null
        && request.headers.first("Content-Type").equals("application/x-www-form-urlencoded")) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Streamer.stream(request.entity, baos);
            parse(baos.toString());
        }
    }

    /**
     * Parses a URL-encoded string containing form parameters and stores them
     * in this object.
     *
     * @param s the URL-encoded string to parse.
     */
    public void parse(String s) {
        for (String param : s.split("&")) {
            String[] nv = param.split("=", 2);
            if (nv.length == 2) {
                add(URLDecoder.decode(nv[0]), URLDecoder.decode(nv[1]));
            }
        }
    }

    /**
     * Sets a request URI with query parameters. This overwrites any query
     * parameters that exist in the request URI.
     *
     * @param request the request to set query parameters to.
     */
    public void toQueryParams(Request request) {
        String uri = (request.uri != null ? request.uri.toString() : "");
        int i = uri.indexOf('?');
        if (i >= 0) { // strip out existing query
            uri = uri.substring(0, i);
        }
        String query = toString();
        if (query.length() > 0) { // add query from form
            uri = uri + '?' + query;
        }
        request.uri = URI.create(uri);
    }

    /**
     * Adds query parameters to an existing request URI. This leaves any
     * existing query parameters intact.
     *
     * @param request the request to add query parameters to.
     */
    public void addQueryParams(Request request) {
        String query = toString();
        if (query.length() > 0) {
            String uri = (request.uri != null ? request.uri.toString() : "");
            uri = uri + (uri.indexOf('?') > 0 ? '&' : '?') + query;
            request.uri = URI.create(uri);
        }
    }

    /**
     * Populates a request with the necessary headers and entity for the form
     * to be submitted as a POST with application/x-www-form-urlencoded content
     * type. This overwrites any entity that may already be in the request.
     *
     * @param request the request to add the form entity to.
     */
    public void toFormEntity(Request request) {
        String form = toString();
        request.method = "POST";
        request.headers.put("Content-Type", "application/x-www-form-urlencoded");
        request.headers.put("Content-Length", Integer.toString(form.length()));
        request.entity = new ByteArrayInputStream(form.getBytes());
    }

    /**
     * Returns this form in a URL-encoded format {@link String}.
     *
     * @return the URL-encoded form.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (String name : keySet()) {
            List<String> values = get(name);
            for (String value: values) {
                if (sb.length() > 0) {
                    sb.append('&');
                }
                sb.append(URLEncoder.encode(name)).append('=').append(URLEncoder.encode(value));
            }
        }
        return sb.toString();
    }
}

