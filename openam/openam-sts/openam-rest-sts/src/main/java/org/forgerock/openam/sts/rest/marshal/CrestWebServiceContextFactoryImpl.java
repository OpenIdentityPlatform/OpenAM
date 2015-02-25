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
 * Copyright 2014 ForgeRock AS. All rights reserved.
 */

package org.forgerock.openam.sts.rest.marshal;

import org.forgerock.json.resource.servlet.HttpContext;
import org.forgerock.openam.sts.rest.service.RestSTSServiceHttpServletContext;
import org.w3c.dom.Element;

import javax.xml.ws.EndpointReference;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.handler.MessageContext;
import java.security.Principal;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * This class will create an instance of the WebServiceContext based upon input from the json-resource-servlet HttpContext
 * and json-resource SecurityContext. The CXF-STS engine passes a WebServiceContext instance in its TokenProviderParameter
 * and TokenValidatorParameter instances. These don't seem to be used by the CXF-STS engine itself, but rather by extensions
 * that want to use the HttpSession as a map to cache state related to invocation processing. This class will create an
 * implementation of the WebServiceContext and associated MessageContext based upon the input which Crest can provide, which
 * is the HttpContext and the SecurityContext. The HttpContext provides access to http headers, parameters, method, and path
 * information, which should be sufficient to satisfy any informational queries. The map caching of the HttpSession can be
 * satisfied with a map delegate.
 *
 * Note that in CXF, it is expected that the MessageContext actually implements the org.apache.cxf.message.Message interface,
 * as is done by the org.apache.cxf.message.MessageImpl class. It is tempting to translate between the MessageContext
 * key types defined in the MessageContext interface, and the state encapsulated by the HttpContext. Or it is tempting
 * to do this translation for the types defined in the org.apache.cxf.message.Message interface, which defines similar
 * lookup types, but with different values. The bottom line is that the CXF-STS engine does not seem to consume this
 * context (the transform set {unt->saml2, unt->openam, openam->saml2} works fine with a null WebServiceContext), but
 * exposes it only for implementations to squirrel away state. Thus, at this point, rather than spending lots of time
 * mapping between the constants defined in orga.apche.cxf.message.Message, or those defined in javax.xml.ws.handler.MessageContext,
 * I will simply throw an exception when they occur, so I can know that they occur, and under which specific
 * circumstances. This will guide me in any mapping which might need to occur.
 *
 * TODO: remove throw blocks prior to release?
 */
public class CrestWebServiceContextFactoryImpl implements WebServiceContextFactory {
    static final class MessageContextImpl implements MessageContext {
        private final Map<String, Object> mapDelegate;
        private final HttpContext httpContext;

        MessageContextImpl(HttpContext httpContext) {
            mapDelegate = new HashMap<String, Object>();
            this.httpContext = httpContext;
        }

        @Override
        public void setScope(String name, Scope scope) {
            throw new IllegalArgumentException("setScope called on faux MessageContextImpl with name param: " + name
                    + " and scope param: " + scope);
        }

        @Override
        public Scope getScope(String name) {
            throw new IllegalArgumentException("setScope called on faux MessageContextImpl with name param: " + name);
        }

        @Override
        public int size() {
            throw new IllegalArgumentException("size called on faux MessageContextImpl.");
        }

        @Override
        public boolean isEmpty() {
            throw new IllegalArgumentException("isEmpty called on faux MessageContextImpl.");
        }

        @Override
        public boolean containsKey(Object key) {
            throw new IllegalArgumentException("containsKey called on faux MessageContextImpl with key:" + key);
        }

        @Override
        public boolean containsValue(Object value) {
            throw new IllegalArgumentException("containsValue called on faux MessageContextImpl with value:" + value);
        }

        @Override
        public Object get(Object key) {
            throw new IllegalArgumentException("get called on faux MessageContextImpl with key:" + key);
        }

        @Override
        public Object put(String key, Object value) {
            throw new IllegalArgumentException("put called on faux MessageContextImpl with key:" + key + " and value " + value);
        }

        @Override
        public Object remove(Object key) {
            throw new IllegalArgumentException("put called on faux MessageContextImpl with key:" + key);
        }

        @Override
        public void putAll(Map<? extends String, ?> m) {
            throw new IllegalArgumentException("putAll called on faux MessageContextImpl with map:" + m);
        }

        @Override
        public void clear() {
            throw new IllegalArgumentException("clear called on faux MessageContextImpl with map:");
        }

        @Override
        public Set<String> keySet() {
            throw new IllegalArgumentException("keySet called on faux MessageContextImpl with map:");
        }

        @Override
        public Collection<Object> values() {
            throw new IllegalArgumentException("values called on faux MessageContextImpl with map:");
        }

        @Override
        public Set<Entry<String, Object>> entrySet() {
            throw new IllegalArgumentException("entrySet called on faux MessageContextImpl with map:");
        }
    }

    static final class WebServiceContextImpl implements WebServiceContext {
        private final HttpContext httpContext;
        private final RestSTSServiceHttpServletContext restSTSServiceHttpServletContext;
        private final MessageContext messageContext;

        WebServiceContextImpl(HttpContext httpContext, RestSTSServiceHttpServletContext restSTSServiceHttpServletContext) {
            this.httpContext = httpContext;
            this.restSTSServiceHttpServletContext = restSTSServiceHttpServletContext;
            messageContext = new MessageContextImpl(httpContext);
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
            throw new IllegalStateException("getUserPrincipal called on the faux WebServiceContext.");
        }

        @Override
        public boolean isUserInRole(String role) {
            throw new IllegalStateException("isUserInRole called on the faux WebServiceContext.");
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
    public WebServiceContext getWebServiceContext(HttpContext httpContext, RestSTSServiceHttpServletContext
            restSTSServiceHttpServletContext) {
        return new WebServiceContextImpl(httpContext, restSTSServiceHttpServletContext);
    }
}
