/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.identityplatform.openam.click;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.identityplatform.openam.click.service.FileUploadService;
import org.identityplatform.openam.click.util.ClickUtils;

/**
 * Provides a custom HttpServletRequest class for shielding users from
 * multipart request parameters. Thus calling request.getParameter(String)
 * will still work properly.
 */
class ClickRequestWrapper extends HttpServletRequestWrapper {

    /**
     * The <tt>FileItem</tt> objects for <tt>"multipart"</tt> POST requests.
     */
    private final Map<String, FileItem[]> fileItemMap;

    /** The request is a multi-part file upload POST request. */
    private final boolean isMultipartRequest;

    /** The map of <tt>"multipart"</tt> request parameter values. */
    private final Map<String, String[]> multipartParameterMap;

    /** The wrapped servlet request. */
    private final HttpServletRequest request;

    // Constructors -----------------------------------------------------------

    /**
     * @see HttpServletRequestWrapper(HttpServletRequest)
     */
    ClickRequestWrapper(final HttpServletRequest request,
                        final FileUploadService fileUploadService) {
        super(request);

        this.isMultipartRequest = ClickUtils.isMultipartRequest(request);
        this.request = request;

        if (isMultipartRequest) {

            Map<String, String[]> requestParams = new HashMap<String, String[]>();
            Map<String, FileItem[]> fileItems = new HashMap<String, FileItem[]>();

            try {
                List<FileItem> itemsList = new ArrayList<FileItem>();

                try {

                    itemsList = fileUploadService.parseRequest(request);

                } catch (FileUploadException fue) {
                    request.setAttribute(FileUploadService.UPLOAD_EXCEPTION, fue);
                }

                for (FileItem fileItem : itemsList) {
                    String name = fileItem.getFieldName();
                    String value = null;

                    // Form fields are placed in the request parameter map,
                    // while file uploads are placed in the file item map.
                    if (fileItem.isFormField()) {

                        if (request.getCharacterEncoding() == null) {
                            value = fileItem.getString();

                        } else {
                            try {
                                value = fileItem.getString(request.getCharacterEncoding());

                            } catch (UnsupportedEncodingException ex) {
                                throw new RuntimeException(ex);
                            }
                        }

                        // Add the form field value to the parameters.
                        addToMapAsString(requestParams, name, value);

                    } else {
                        // Add the file item to the list of file items.
                        addToMapAsFileItem(fileItems, name, fileItem);
                    }
                }

            } catch (Throwable t) {

                // Don't throw error here as it will break Context creation.
                // Instead add the error as a request attribute.
                request.setAttribute(Context.CONTEXT_FATAL_ERROR, t);

            } finally {
                fileItemMap = Collections.unmodifiableMap(fileItems);
                multipartParameterMap = Collections.unmodifiableMap(requestParams);
            }

        } else {
            fileItemMap = Collections.emptyMap();
            multipartParameterMap = Collections.emptyMap();
        }
    }

    // Public Methods ---------------------------------------------------------

    /**
     * Returns a map of <tt>FileItem arrays</tt> keyed on request parameter
     * name for "multipart" POST requests (file uploads). Thus each map entry
     * will consist of one or more <tt>FileItem</tt> objects.
     *
     * @return map of <tt>FileItem arrays</tt> keyed on request parameter name
     * for "multipart" POST requests
     */
    public Map<String, FileItem[]> getFileItemMap() {
        return fileItemMap;
    }

    /**
     * @see jakarta.servlet.ServletRequest#getParameter(String)
     */
    @Override
    public String getParameter(String name) {
        if (isMultipartRequest) {
            Object value = getMultipartParameterMap().get(name);

            if (value instanceof String) {
                return (String) value;
            }

            if (value instanceof String[]) {
                String[] array = (String[]) value;
                if (array.length >= 1) {
                    return array[0];
                } else {
                    return null;
                }
            }

            return (value == null ? null : value.toString());

        } else {
            return request.getParameter(name);
        }
    }

    /**
     * @see jakarta.servlet.ServletRequest#getParameterNames()
     */
    @Override
    @SuppressWarnings("unchecked")
    public Enumeration getParameterNames() {
        if (isMultipartRequest) {
            return Collections.enumeration(getMultipartParameterMap().keySet());

        } else {
            return request.getParameterNames();
        }
    }

    /**
     * @see jakarta.servlet.ServletRequest#getParameterValues(String)
     */
    @Override
    public String[] getParameterValues(String name) {
        if (isMultipartRequest) {
            Object values = getMultipartParameterMap().get(name);
            if (values instanceof String) {
                return new String[] { values.toString() };
            }
            if (values instanceof String[]) {
                return (String[]) values;
            } else {
                return null;
            }

        } else {
            return request.getParameterValues(name);
        }
    }

    /**
     * @see jakarta.servlet.ServletRequest#getParameterMap()
     */
    @Override
    @SuppressWarnings("unchecked")
    public Map getParameterMap() {
        if (isMultipartRequest) {
            return getMultipartParameterMap();
        } else {
            return request.getParameterMap();
        }
    }

    // Package Private Methods ------------------------------------------------

    /**
     * Return the map of <tt>"multipart"</tt> request parameter map.
     *
     * @return the <tt>"multipart"</tt> request parameter map
     */
    @SuppressWarnings("unchecked")
    Map getMultipartParameterMap() {
        if (request.getAttribute(ClickServlet.MOCK_MODE_ENABLED) == null) {
            return multipartParameterMap;
        } else {
            // In mock mode return the request parameter map. This ensures
            // calling request.setParameter(x,y) works for both normal and
            // multipart requests.
            return request.getParameterMap();
        }
    }

    // Private Methods --------------------------------------------------------

    /**
     * Stores the specified value in a FileItem array in the map, under the
     * specified name. Thus two values stored under the same name will be
     * stored in the same array.
     *
     * @param map the map to add the specified name and value to
     * @param name the name of the map key
     * @param value the value to add to the FileItem array
     */
    private void addToMapAsFileItem(Map<String, FileItem[]> map, String name, FileItem value) {
        FileItem[] oldValues = map.get(name);
        FileItem[] newValues = null;
        if (oldValues == null) {
            newValues = new FileItem[] {value};
        } else {
            newValues = new FileItem[oldValues.length + 1];
            System.arraycopy(oldValues, 0, newValues, 0, oldValues.length);
            newValues[oldValues.length] = value;
        }
        map.put(name, newValues);
    }

    /**
     * Stores the specified value in an String array in the map, under the
     * specified name. Thus two values stored under the same name will be
     * stored in the same array.
     *
     * @param map the map to add the specified name and value to
     * @param name the name of the map key
     * @param value the value to add to the string array
     */
    private void addToMapAsString(Map<String, String[]> map, String name, String value) {
        String[] oldValues = map.get(name);
        String[] newValues = null;
        if (oldValues == null) {
            newValues = new String[] {value};
        } else {
            newValues = new String[oldValues.length + 1];
            System.arraycopy(oldValues, 0, newValues, 0, oldValues.length);
            newValues[oldValues.length] = value;
        }
        map.put(name, newValues);
    }

}
