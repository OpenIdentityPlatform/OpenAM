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
 * Copyright 2013 ForgeRock Inc.
 */

package org.forgerock.openam.forgerockrest.authn.core;

import org.apache.commons.collections.iterators.IteratorEnumeration;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * A HttpServletRequest Wrapper which allows package local classes to add additional parameters to the Http Request.
 * <p>
 * This is purely required for converting logic and parameters from the new REST Authentication endpoint to parameters
 * the underlying legacy authentication framework expects for composite advice functions and no session issuing.
 *
 * @author Phill Cunnington
 */
public class RestAuthHttpRequestWrapper extends HttpServletRequestWrapper {

    private Map<String, String> parameterMap = new HashMap<String, String>();

    /**
     * Constructs a RestAuthHttpRequestWrapper.
     *
     * @param request The wrapped HttpServletRequest.
     */
    RestAuthHttpRequestWrapper(HttpServletRequest request) {
        super(request);
    }

    /**
     * Gets the parameter with the given name from the underlying HttpServletRequest. If that is null will attempt
     * the same with the local parameter map.
     *
     * @param name {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public String getParameter(String name) {

        String value = super.getParameter(name);
        if (value == null) {
            value = parameterMap.get(name);
        }

        return value;
    }

    /**
     * Creates a combined parameter map from the underlying HttpServletRequest and the local parameter map.
     * <p>
     * The local parameter map will override any duplicate entries in the underlying HttpServletRequest map.
     *
     * @return {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public Map<String, String> getParameterMap() {
        Map<String, String> combined = new HashMap<String, String>(super.getParameterMap());
        combined.putAll(parameterMap);
        return combined;
    }

    /**
     * Gets the parameter names from the combined parameter maps.
     *
     * @see #getParameterMap()
     *
     * @return {@inheritDoc}
     */
    @Override
    public Enumeration getParameterNames() {
        return new IteratorEnumeration(getParameterMap().keySet().iterator());
    }

    /**
     * Gets the parameter values from the combined parameter maps.
     *
     * @see #getParameterMap()
     *
     * @param name {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public String[] getParameterValues(String name) {
        return getParameterMap().values().toArray(new String[0]);
    }

    /**
     * Adds a parameter to the local parameter map.
     *
     * @param key The parameter key.
     * @param value The parameter value.
     */
    void addParameter(String key, String value) {
        parameterMap.put(key, value);
    }
}
