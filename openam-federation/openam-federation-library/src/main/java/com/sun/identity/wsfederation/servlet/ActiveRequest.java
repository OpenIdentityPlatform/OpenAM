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
package com.sun.identity.wsfederation.servlet;

import static com.sun.identity.wsfederation.common.WSFederationConstants.*;
import static org.forgerock.openam.utils.Time.newDate;
import static org.forgerock.openam.wsfederation.common.ActiveRequestorException.newReceiverException;
import static org.forgerock.openam.wsfederation.common.ActiveRequestorException.newSenderException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.MimeHeaders;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPConstants;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPHeader;
import javax.xml.soap.SOAPMessage;

import org.forgerock.openam.saml2.plugins.WsFedAuthenticator;
import org.forgerock.openam.utils.IOUtils;
import org.forgerock.openam.utils.StringUtils;
import org.forgerock.openam.wsfederation.common.ActiveRequestorException;
import org.owasp.esapi.ESAPI;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.iplanet.sso.SSOToken;
import com.sun.identity.plugin.session.SessionException;
import com.sun.identity.plugin.session.SessionManager;
import com.sun.identity.saml.assertion.Assertion;
import com.sun.identity.saml.common.SAMLConstants;
import com.sun.identity.saml.common.SAMLUtils;
import com.sun.identity.saml2.common.SAML2Utils;
import com.sun.identity.saml2.common.SOAPCommunicator;
import com.sun.identity.shared.DateUtils;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.whitelist.URLPatternMatcher;
import com.sun.identity.wsfederation.common.WSFederationException;
import com.sun.identity.wsfederation.common.WSFederationUtils;
import com.sun.identity.wsfederation.jaxb.entityconfig.IDPSSOConfigElement;
import com.sun.identity.wsfederation.meta.WSFederationMetaManager;
import com.sun.identity.wsfederation.meta.WSFederationMetaUtils;
import com.sun.identity.wsfederation.profile.SAML11RequestedSecurityToken;

/**
 * A {@link WSFederationAction} implementation that processes WS-Federation Active Requestor Profile SOAP requests. It
 * does so, by processing RST/Issue requests based on the 2005/02 spec, to allow backwards compatibility with legacy
 * client applications.
 */
public class ActiveRequest extends WSFederationAction {

    private static final Debug DEBUG = Debug.getInstance("libWSFederation");
    public static final String NO_PROOF_KEY_KEY_TYPE = "http://schemas.xmlsoap.org/ws/2005/05/identity/NoProofKey";
    private static final String ACTION = "http://schemas.xmlsoap.org/ws/2005/02/trust/RST/Issue";
    private static final String REQUEST_TYPE = "http://schemas.xmlsoap.org/ws/2005/02/trust/Issue";
    private String realm = null;
    private String messageId = null;
    private String username = null;
    private char[] password = null;
    private String expires = null;
    private String address = null;

    public ActiveRequest(HttpServletRequest request, HttpServletResponse response) {
        super(request, response);
    }

    /**
     * Processes the incoming SOAP request {@link #parseAndValidateRequest(SOAPMessage, IDPSSOConfigElement) parsing
     * and validating the request}, and then authenticating the end-user using a customizable {@link WsFedAuthenticator}
     * implementation. In case of a successful login, a SAML1.1 RequestedSecurityToken is returned in a SOAP message.
     *
     * @throws ServletException If there was a problem whilst rendering the response.
     * @throws IOException If there was an IO error whilst working with the request or response.
     * @throws WSFederationException If there was an unrecoverable error while processing the request.
     */
    @Override
    public void process() throws ServletException, IOException, WSFederationException {
        final String metaAlias = WSFederationMetaUtils.getMetaAliasByUri(request.getRequestURI());
        if (StringUtils.isEmpty(metaAlias)) {
            DEBUG.error("unable to get IDP meta alias from request.");
            throw new WSFederationException(BUNDLE_NAME, "IDPMetaAliasNotFound", null);
        }

        WSFederationMetaManager metaManager = WSFederationUtils.getMetaManager();
        realm = WSFederationMetaUtils.getRealmByMetaAlias(metaAlias);
        final String idpEntityId = metaManager.getEntityByMetaAlias(metaAlias);

        if (StringUtils.isEmpty(idpEntityId)) {
            DEBUG.error("Unable to get IDP Entity ID from metaAlias");
            throw new WSFederationException(BUNDLE_NAME, "nullIDPEntityID", null);
        }

        final IDPSSOConfigElement idpConfig = metaManager.getIDPSSOConfig(realm, idpEntityId);
        if (idpConfig == null) {
            DEBUG.error("Cannot find configuration for IdP " + idpEntityId);
            throw new WSFederationException(BUNDLE_NAME, "unableToFindIDPConfiguration", null);
        }

        final boolean activeRequestorEnabled = Boolean.parseBoolean(WSFederationMetaUtils.getAttribute(idpConfig,
                ACTIVE_REQUESTOR_PROFILE_ENABLED));

        if (!activeRequestorEnabled) {
            DEBUG.warning("Active Requestor Profile is not enabled for the hosted IdP {}", idpEntityId);
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        // We can process the SOAP request now.
        SAMLUtils.checkHTTPContentLength(request);

        MimeHeaders headers = SOAPCommunicator.getInstance().getHeaders(request);
        SOAPMessage soapFault;
        SSOToken ssoToken = null;
        try (InputStream is = request.getInputStream()) {
            SOAPMessage soapMessage = MessageFactory.newInstance(SOAPConstants.SOAP_1_2_PROTOCOL)
                    .createMessage(headers, is);
            if (DEBUG.messageEnabled()) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                soapMessage.writeTo(baos);
                DEBUG.message("SOAP message received: " + maskPassword(new String(baos.toByteArray(),
                        StandardCharsets.UTF_8)));
            }
            parseAndValidateRequest(soapMessage, idpConfig);
            ssoToken = authenticateEndUser(soapMessage, WSFederationMetaUtils.getAttribute(idpConfig,
                    AUTHENTICATOR_CLASS, "org.forgerock.openam.saml2.plugins.DefaultWsFedAuthenticator"));
            final SAML11RequestedSecurityToken requestedSecurityToken = WSFederationUtils.createSAML11Token(realm,
                    idpEntityId, address, ssoToken, address, SAMLConstants.AUTH_METHOD_PASSWORD_URI, true);
            final Assertion assertion = requestedSecurityToken.getAssertion();

            request.setAttribute("inResponseTo", ESAPI.encoder().encodeForXML(messageId));
            final Date responseCreated = newDate();
            request.setAttribute("responseCreated", ESAPI.encoder().encodeForXML(
                    DateUtils.dateToString(responseCreated)));
            request.setAttribute("responseExpires", ESAPI.encoder().encodeForXML(
                    DateUtils.dateToString(newDate(responseCreated.getTime() + 300 * 1000))));
            request.setAttribute("notBefore", ESAPI.encoder().encodeForXML(
                    DateUtils.dateToString(assertion.getConditions().getNotBefore())));
            request.setAttribute("notOnOrAfter", ESAPI.encoder().encodeForXML(
                    DateUtils.dateToString(assertion.getConditions().getNotOnorAfter())));
            request.setAttribute("targetAddress", ESAPI.encoder().encodeForXML(address));
            request.setAttribute("requestedSecurityToken", requestedSecurityToken.toString());
            request.setAttribute("assertionId", ESAPI.encoder().encodeForXML(assertion.getAssertionID()));
            request.getRequestDispatcher("/wsfederation/jsp/activeresponse.jsp").forward(request, response);
            return;
        } catch (ActiveRequestorException are) {
            DEBUG.message("An error occurred while processing the Active Request", are);
            soapFault = are.getSOAPFault();
        } catch (SOAPException | WSFederationException ex) {
            DEBUG.error("An unexpected error occurred while processing the SOAP message", ex);
            soapFault = newReceiverException(ex).getSOAPFault();
        } finally {
            try {
                SessionManager.getProvider().invalidateSession(ssoToken, request, response);
            } catch (SessionException se) {
                DEBUG.message("Unable to invalidate temporary session", se);
            }
        }

        if (soapFault != null) {
            OutputStream os = null;
            try {
                if (soapFault.saveRequired()) {
                    soapFault.saveChanges();
                }
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                SAML2Utils.putHeaders(soapFault.getMimeHeaders(), response);
                os = response.getOutputStream();
                soapFault.writeTo(os);
                os.flush();
            } catch (SOAPException se) {
                DEBUG.error("An error occurred while sending back SOAP fault:", se);
            } finally {
                IOUtils.closeIfNotNull(os);
            }
        } else {
            // This can happen if we failed to create the SOAP Fault for some reason.
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private void parseAndValidateRequest(SOAPMessage soapMessage, IDPSSOConfigElement idpConfig) throws SOAPException,
            WSFederationException {
        final SOAPHeader soapHeader = soapMessage.getSOAPHeader();
        final NodeList headerNodes = soapHeader.getChildNodes();

        String action = null;
        String to = null;
        for (int i = 0; i < headerNodes.getLength(); i++) {
            final Node node = headerNodes.item(i);
            if (node instanceof Element) {
                final Element element = (Element) node;
                if (WSA_NAMESPACE.equals(element.getNamespaceURI())) {
                    final String textContent = element.getTextContent();
                    if ("Action".equals(element.getLocalName())) {
                        action = textContent;
                    } else if ("To".equals(element.getLocalName())) {
                        to = textContent;
                    } else if ("MessageID".equals(element.getLocalName())) {
                        messageId = textContent;
                    }
                } else if (WSSE_NAMESPACE.equals(element.getNamespaceURI())) {
                    if ("Security".equals(element.getLocalName())) {
                        extractSecurityDetails(element);
                    }
                }
            }
        }

        final String stsEndpoint = WSFederationMetaUtils.getEndpointBaseUrl(idpConfig, request)
                + "/WSFederationServlet/sts/metaAlias" + idpConfig.getMetaAlias();
        final Date expiresDate;
        try {
            expiresDate = DateUtils.stringToDate(expires);
        } catch (ParseException pe) {
            throw newSenderException("invalidOrExpiredRequest");
        }
        if (!ACTION.equals(action)) {
            throw newSenderException("invalidValueForElement", "wsa:Action");
        } else if (StringUtils.isEmpty(username) || password.length == 0) {
            throw newSenderException("unableToAuthenticate");
        } else if (newDate().after(expiresDate)) {
            throw newSenderException("timeInvalid");
        } else {
            try {
                URLPatternMatcher patternMatcher = new URLPatternMatcher();
                if (!patternMatcher.match(stsEndpoint, Collections.singleton(to), false)) {
                    throw newSenderException("invalidValueForElement", "wsa:To");
                }
            } catch (MalformedURLException murle) {
                throw newSenderException("invalidValueForElement", "wsa:To");
            }
        }

        final SOAPBody soapBody = soapMessage.getSOAPBody();
        final String requestType = getSingleElement(soapBody, WST_NAMESPACE, "RequestType");
        if (!REQUEST_TYPE.equals(requestType)) {
            throw newReceiverException("unsupportedRequestType");
        }

        address = getSingleElement(soapBody, WSA_NAMESPACE, "Address");
        final List<String> trustedAddresses = WSFederationMetaUtils.getAttributes(idpConfig, TRUSTED_ADDRESSES);
        if (trustedAddresses == null || !trustedAddresses.contains(address)) {
            throw newReceiverException("invalidReceiver");
        }

        final String keyType = getSingleElement(soapBody, WST_NAMESPACE, "KeyType");
        if (!NO_PROOF_KEY_KEY_TYPE.equals(keyType)) {
            throw newReceiverException("unsupportedKeyType");
        }
    }

    private SSOToken authenticateEndUser(SOAPMessage soapMessage, String authenticator)
            throws ActiveRequestorException {
        try {
            final WsFedAuthenticator wsFedAuthenticator = Class.forName(authenticator)
                    .asSubclass(WsFedAuthenticator.class).newInstance();
            return wsFedAuthenticator.authenticate(request, response, soapMessage, realm, username, password);
        } catch (ReflectiveOperationException roe) {
            DEBUG.error("An error occurred while invoking WsFedAuthenticator", roe);
            throw newReceiverException(roe);
        }
    }

    private void extractSecurityDetails(Element element) throws WSFederationException {
        username = getSingleElement(element, WSSE_NAMESPACE, "Username");
        password = getSingleElement(element, WSSE_NAMESPACE, "Password").toCharArray();
        expires = getSingleElement(element, WSU_NAMESPACE, "Expires");
    }

    private String getSingleElement(Element element, String namespaceURI, String localName)
            throws WSFederationException {
        NodeList nodeList = element.getElementsByTagNameNS(namespaceURI, localName);
        if (nodeList.getLength() == 0) {
            throw newSenderException("missingElement", localName, namespaceURI);
        } else if (nodeList.getLength() > 1) {
            throw newSenderException("tooManyElements", localName, namespaceURI);
        } else {
            return nodeList.item(0).getTextContent();
        }
    }

    private String maskPassword(String text) {
        int start = text.indexOf("Password>");
        int end = text.lastIndexOf("<", text.lastIndexOf("Password>"));

        return text.substring(0, start + "Password>".length()) + "### MASKED PASSWORD ###" + text.substring(end);
    }
}
