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
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright 2013-2014 ForgeRock AS. All rights reserved.
 */

package org.forgerock.openam.sts.rest.marshal;

import org.w3c.dom.Element;

import javax.servlet.http.HttpServletRequest;
import javax.xml.ws.EndpointReference;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.handler.MessageContext;
import java.security.Principal;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * One fundamental issue in speaking REST to the CXF-STS back-end is that the CXF-STS was built for a WebService context.
 * Thus the fundamental token operations expect a valid instance of the WebServiceContext class, which is injected by the
 * jax-ws implementation. I need to provide this instance myself, using the HttpServletRequest as the input state. Verifying
 * the fact that a HttpServletRequest can provide the state necessary to create a valid WebServiceContext instance is
 * fundamental to deploying the cxf-sts behind a jax-rs resource.
 * In the CXF context, the org.apache.cxf.jaxws.context.WebServiceContextImpl implements the WebServiceContext interface.
 * This instance, and the associated MessageContext, are mocked-up in the CXF STS unit-tests with regularity. It seems like
 * a faux, but valid, WebServiceContext instance can be made with the HttpServletRequest available to invocations against
 * the REST-STS. The more elaborate EndpointReference getEndpointReference calls don't seem to be made by the cxf-sts engine.
 */
public class WebServiceContextFactoryImpl implements WebServiceContextFactory {
    private static final String HTTP_REQUEST_KEY = "HTTP.REQUEST"; //TODO: is this defined in some jdk class?

    static final class MessageContextImpl implements MessageContext {
        private final Map<String, Object> mapDelegate;

        MessageContextImpl(HttpServletRequest servletRequest) {
            mapDelegate = new HashMap<String, Object>();
            mapDelegate.put(HTTP_REQUEST_KEY, servletRequest);
        }

        @Override
        public void setScope(String name, Scope scope) {
            mapDelegate.put(name, scope);
        }

        @Override
        public Scope getScope(String name) {
            Object scopeObject = mapDelegate.get(name);
            if (scopeObject instanceof Scope) {
                return (Scope)scopeObject;
            }
            return null;
        }

        @Override
        public int size() {
            return mapDelegate.size();
        }

        @Override
        public boolean isEmpty() {
            return mapDelegate.isEmpty();
        }

        @Override
        public boolean containsKey(Object key) {
            return mapDelegate.containsKey(key);
        }

        @Override
        public boolean containsValue(Object value) {
            return mapDelegate.containsValue(value);
        }

        @Override
        public Object get(Object key) {
            return mapDelegate.get(key);
        }

        @Override
        public Object put(String key, Object value) {
            return mapDelegate.put(key, value);
        }

        @Override
        public Object remove(Object key) {
            return mapDelegate.remove(key);
        }

        @Override
        public void putAll(Map<? extends String, ?> m) {
            mapDelegate.putAll(m);
        }

        @Override
        public void clear() {
            mapDelegate.clear();
        }

        @Override
        public Set<String> keySet() {
            return mapDelegate.keySet();
        }

        @Override
        public Collection<Object> values() {
            return mapDelegate.values();
        }

        @Override
        public Set<Entry<String, Object>> entrySet() {
            return mapDelegate.entrySet();
        }
    }
    static final class WebServiceContextImpl implements WebServiceContext {
        private final HttpServletRequest servletRequest;
        private final MessageContext messageContext;
        WebServiceContextImpl(HttpServletRequest servletRequest) {
            this.servletRequest = servletRequest;
            messageContext = new MessageContextImpl(servletRequest);
        }

        @Override
        public MessageContext getMessageContext() {
            /*
            TODO: this is just a hack to provide an instance of the necessary type - but the state is not correct.
            I need to return to this to insure that I can provide a viable MessageContext implementation with the state
            provided in the HttpServletRequest. In theory, this should be possible, as it would appear that all state to inform
            a MessageContext instance will ultimately come from a HttpServletRequest anyway.
             */
            return messageContext;
        }

        @Override
        public Principal getUserPrincipal() {
            return servletRequest.getUserPrincipal();
        }

        @Override
        public boolean isUserInRole(String role) {
            return servletRequest.isUserInRole(role);
        }

        @Override
        public EndpointReference getEndpointReference(Element... referenceParameters) {
            throw new IllegalStateException("getEndpointReference called on the faux WebServiceContext!");
        }

        @Override
        public <T extends EndpointReference> T getEndpointReference(Class<T> clazz, Element... referenceParameters) {
            throw new IllegalStateException("getEndpointReference called on the faux WebServiceContext for class " + clazz);
        }
    }
    @Override
    public WebServiceContext getWebServiceContext(HttpServletRequest request) {
        return new WebServiceContextImpl(request);
    }
}
