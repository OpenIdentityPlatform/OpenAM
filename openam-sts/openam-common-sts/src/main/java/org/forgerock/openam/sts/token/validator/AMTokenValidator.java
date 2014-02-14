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

package org.forgerock.openam.sts.token.validator;

import org.apache.cxf.sts.request.ReceivedToken;
import org.apache.cxf.sts.token.validator.TokenValidator;
import org.apache.cxf.sts.token.validator.TokenValidatorParameters;
import org.apache.cxf.sts.token.validator.TokenValidatorResponse;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.forgerock.openam.sts.AMSTSConstants;
import org.forgerock.openam.sts.STSPrincipal;
import org.forgerock.openam.sts.TokenCreationException;
import org.forgerock.openam.sts.token.ThreadLocalAMTokenCache;
import org.restlet.engine.header.Header;
import org.restlet.representation.Representation;
import org.restlet.resource.ClientResource;
import org.restlet.util.Series;
import org.w3c.dom.Element;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.Principal;
import java.util.Map;
import org.slf4j.Logger;

public class AMTokenValidator implements TokenValidator {
    private static final String TRUE = "true";
    private static final String ID = "id";
    private final String amDeploymentUri;
    private final String amJsonRestBase;
    private final String realm;
    private final String amRestIdFromSessionUriElement;
    private final String amSessionCookieName;
    private final ThreadLocalAMTokenCache threadLocalAMTokenCache;
    private final String sessionToUsernameUrl;
    private final Logger logger;

    /*
    The lifecycle for this class is controlled by the TokenOperationFactoryImpl, and thus needs no @Inject.
     */
    public AMTokenValidator(String amDeploymentUri, String amJsonRestBase, String realm, String amRestIdFromSessionUriElement, String amSessionCookieName,
                            ThreadLocalAMTokenCache threadLocalAMTokenCache, Logger logger) {
        this.amDeploymentUri = amDeploymentUri;
        this.amJsonRestBase = amJsonRestBase;
        this.realm = realm;
        this.amRestIdFromSessionUriElement = amRestIdFromSessionUriElement;
        sessionToUsernameUrl = amDeploymentUri + amRestIdFromSessionUriElement;
        this.amSessionCookieName = amSessionCookieName;
        this.threadLocalAMTokenCache = threadLocalAMTokenCache;
        this.logger = logger;
    }

    /*
    Because the ReceivedToken and SecurityToken objects ultimately represent a token object as a DOM Element, this
    class must process them as an Element.
     */
    @Override
    public boolean canHandleToken(ReceivedToken validateTarget) {
        Object token = validateTarget.getToken();
        if (token instanceof Element) {
            Element tokenElement = (Element)token;
            return AMSTSConstants.AM_SESSION_ID_ELEMENT_NAME.equals(tokenElement.getLocalName());
        }
        return false;
    }

    @Override
    public boolean canHandleToken(ReceivedToken validateTarget, String realm) {
        logger.debug("canHandleToken called with a realm of " + realm);
        return canHandleToken(validateTarget);
    }

    @Override
    public TokenValidatorResponse validateToken(TokenValidatorParameters tokenParameters) {
        TokenValidatorResponse response = new TokenValidatorResponse();
        ReceivedToken validateTarget = tokenParameters.getToken();
        validateTarget.setState(ReceivedToken.STATE.INVALID);
        response.setToken(validateTarget);
        try {
            String sessionId = parseSessionIdFromRequest(tokenParameters.getToken());
            threadLocalAMTokenCache.cacheAMToken(sessionId);
            Principal principal = obtainPrincipalFromSession(constitutePrincipalFromSessionUrl(), sessionId);
            response.setPrincipal(principal);
            validateTarget.setState(ReceivedToken.STATE.VALID);
        } catch (Exception e) {
            logger.info("Exception caught obtaining principal from session id: " + e, e);
        }
        return response;
    }

    /**
     * Creates the String representing the url at which the principal id from session token functionality can be
     * consumed.
     * @return A String representing the url of OpenAM's Restful principal from session id service
     * TODO: proper creation of the url - including insuring proper '/' values in the right places. Should be a first-class
     * concern - an interface/implimentation, bound by guice, which can also be used to validate user input when STS instances
     * are being configured.
     */
    private String constitutePrincipalFromSessionUrl() {
        StringBuilder sb = new StringBuilder(amDeploymentUri);
        sb.append(amJsonRestBase);
        if (!AMSTSConstants.ROOT_REALM.equals(realm)) {
            sb.append(realm);
        }
        sb.append(amRestIdFromSessionUriElement);
        return sb.toString();
    }

    private String parseSessionIdFromRequest(ReceivedToken receivedToken) throws TokenCreationException {
        Object token = receivedToken.getToken();
        if (token instanceof Element) {
            Element tokenElement = (Element)token;
            if (AMSTSConstants.AM_SESSION_ID_ELEMENT_NAME.equals(tokenElement.getLocalName())) {
                return ((Element)token).getFirstChild().getNodeValue();
            } else {
                try {
                    Transformer transformer = TransformerFactory.newInstance().newTransformer();
                    StreamResult res =  new StreamResult(new ByteArrayOutputStream());
                    transformer.transform(new DOMSource(tokenElement), res);
                    String message = "Unexpected state: should be dealing with a DOM Element defining an AM session, but " +
                            "not the following token element: " +
                            new String(((ByteArrayOutputStream)res.getOutputStream()).toByteArray());
                    logger.error(message);
                    throw new TokenCreationException(message);
                } catch (Exception e) {
                    throw new TokenCreationException("Unexpected state: should be dealing with a DOM Element defining an " +
                            "AM Session, but this is not the case.");
                }
            }
        } else {
            String message = "Unexpected state in AMTokenValidator: validated token of unexpected type: " +
                    (token != null ? token.getClass().getCanonicalName() : null);
            logger.error(message);
            throw new TokenCreationException(message);
        }
    }

    /*
    TODO: it may well be that the name of the Principal should not just correspond to the id of the subject whose authentication
    the session represents, but any given (user configured) attribute that the user wants to pull from this particular principal.
    And will there always be a id? Obviously if we are authenticating via LDAP, but what if the sessionId corresponds to some other
    type of authentication?
     */
    private Principal obtainPrincipalFromSession(String sessionToUsernameUrl, String sessionId) throws TokenCreationException {
        logger.debug("sessionToUsernameUrl: " + sessionToUsernameUrl);
        ClientResource resource = new ClientResource(sessionToUsernameUrl);
        resource.setFollowingRedirects(false);
        Series<Header> headers = (Series<Header>)resource.getRequestAttributes().get(AMSTSConstants.RESTLET_HEADER_KEY);
        if (headers == null) {
            headers = new Series<Header>(Header.class);
            resource.getRequestAttributes().put(AMSTSConstants.RESTLET_HEADER_KEY, headers);
        }
        headers.set(AMSTSConstants.COOKIE, amSessionCookieName + AMSTSConstants.EQUALS + sessionId);
        headers.set(AMSTSConstants.CONTENT_TYPE, AMSTSConstants.APPLICATION_JSON);
        headers.set(AMSTSConstants.ACCEPT, AMSTSConstants.APPLICATION_JSON);
        //TODO: this throws the unchecked ResourceException - catch and rethrow as checked exception, or ??
        Representation representation = resource.post(null);
        Map<String,Object> responseAsMap = null;
        try {
            //TODO: do I want to do some buffered reading on the representation, instead of pulling it all into a String via the getText call?
            //could run into memory issues if the return value is gigantic - but this is not the case for the given call, so...
            responseAsMap = new ObjectMapper().readValue(representation.getText(),
                    new TypeReference<Map<String,Object>>() {});
        } catch (IOException ioe) {
            String message = "Exception caught getting the text of idFromSession response: " + ioe;
            logger.error(message, ioe);
            throw new TokenCreationException(message, ioe);
        }
        String principalName = (String)responseAsMap.get(ID);
        if ((principalName != null) && !principalName.isEmpty()) {
            return new STSPrincipal(principalName);
        } else {
            throw new TokenCreationException("id returned from idFromSession is null or empty. The returned value: " + principalName);
        }
    }
}
