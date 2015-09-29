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

package org.forgerock.openam.forgerockrest.utils;

import static org.forgerock.json.JsonValue.*;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.xml.XMLUtils;
import org.forgerock.caf.authentication.api.AuthenticationException;
import org.forgerock.caf.authentication.api.MessageContext;
import org.forgerock.caf.authentication.framework.AuditTrail;
import org.forgerock.caf.authentication.framework.AuthenticationFailedException;
import org.forgerock.caf.authentication.framework.ResponseWriter;
import org.forgerock.guava.common.net.MediaType;
import org.forgerock.http.header.ContentTypeHeader;
import org.forgerock.http.protocol.Response;
import org.forgerock.http.protocol.Status;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.PermanentException;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.util.Reject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * An implementation of {@code ResourceExceptionHandler} that renders to XML.
 */
public class XMLResourceExceptionHandler implements ResponseWriter {
    private static final Debug LOGGER = Debug.getInstance("frRest");
    private static final List<MediaType> HANDLES = Arrays.asList(
            MediaType.XML_UTF_8.withoutParameters(), MediaType.APPLICATION_XML_UTF_8.withoutParameters());

    /**
     * {@inheritDoc}
     * @return {@code text/xml} and {@code application/xml}.
     */
    public Collection<MediaType> handles() {
        return HANDLES;
    }

    @Override
    public void write(MessageContext context, AuthenticationException exception) {
        Reject.ifNull(exception);
        try {
            ResourceException jre;
            if (exception instanceof AuthenticationFailedException) {
                jre = new PermanentException(Status.UNAUTHORIZED.getCode(), exception.getMessage(), null);
            } else if (exception.getCause() instanceof ResourceException) {
                jre = (ResourceException) exception.getCause();
            } else {
                LOGGER.error(exception.getMessage(), exception);
                jre = new InternalServerErrorException("Authentication Failed", exception);
            }
            AuditTrail auditTrail = context.getAuditTrail();
            List<Map<String, Object>> failureReasonList = auditTrail.getFailureReasons();
            if (failureReasonList != null && !failureReasonList.isEmpty()) {
                jre.setDetail(json(object(field("failureReasons", failureReasonList))));
            }
            Response response = context.getResponse();
            response.setStatus(Status.valueOf(jre.getCode()));
            context.<Response>getResponse().getHeaders().put(ContentTypeHeader.valueOf(MediaType.XML_UTF_8.toString()));
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            Transformer transformer = XMLUtils.getTransformerFactory().newTransformer();
            transformer.transform(new DOMSource(asXMLDOM(jre.includeCauseInJsonValue().toJsonValue().asMap())),
                    new StreamResult(outputStream));
            response.getEntity().setBytes(outputStream.toByteArray());
        } catch (TransformerException e1) {
            throw new IllegalStateException("Could not write XML to response", e1);
        }
    }

    /**
     * Convert a Map to XML DOM. The root node is an {@code error} element. Collections are
     * represented as child {@code entry} elements.
     * @param map The map to convert.
     * @return The DOM document.
     */
    public static Document asXMLDOM(Map<String, Object> map) {
        try {
            Document document = XMLUtils.newDocument();
            Element root = write(document, "error", map);
            document.appendChild(root);
            return document;
        } catch (ParserConfigurationException e1) {
            throw new IllegalStateException("Cannot construct DOM Document", e1);
        }
    }

    private static Element write(Document document, String name, Object content) {
        if (content instanceof JsonValue) {
            return write(document, name, ((JsonValue) content).getObject());
        }
        Element e = document.createElement(name);
        if (content instanceof Map) {
            Map<String, Object> map = (Map<String, Object>)content;
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                e.appendChild(write(document, entry.getKey(), entry.getValue()));
            }
        } else if (content instanceof Collection) {
            Collection<Object> collection = (Collection<Object>) content;
            for (Object o : collection) {
                e.appendChild(write(document, "entry", o));
            }
        } else {
            e.setTextContent(content.toString());
        }
        return e;
    }
}
