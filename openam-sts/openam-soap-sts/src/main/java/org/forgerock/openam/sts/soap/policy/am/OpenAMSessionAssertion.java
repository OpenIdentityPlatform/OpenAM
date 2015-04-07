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
 * Copyright 2015 ForgeRock AS.
 */

package org.forgerock.openam.sts.soap.policy.am;

import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.ws.security.policy.SP12Constants;
import org.apache.cxf.ws.security.policy.model.Token;
import org.apache.ws.security.message.token.BinarySecurity;
import org.apache.cxf.ws.security.policy.SPConstants;
import org.forgerock.openam.sts.AMSTSConstants;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

/**
 * Class which represents the Assertion corresponding to the custom OpenAMSessionToken SecurityPolicy binding. An assertion
 * of this sort needs to be passed to the ISSUE operation which takes an OpenAMSession id. This will occur via the
 * OpenAMSessionTokenClientAssertionBuilder registered with the org.apache.cxf.ws.policy.AssertionBuilderRegistry (obtained
 * via the cxf Bus) in the STSClient instance used to consume the OpenAM soap sts.
 */
public class OpenAMSessionAssertion extends Token {
    private final String sessionId;

    /**
     *
     * @param version Constant indicating what SecurityPolicy version is being used(1.1 vs. 1.2). I believe that it is
     *                used to distinguish different QNames for different constructs in different versions
     * @param includeTokenType The SecurityPolicy specification of when to include the token - e.g. always, never,
     *                         always-to-recipient, etc.
     * @param nestedPolicy The policy element nested within the OpenAM Session assertion. WS-SecurityPolicy nests policies
     *                     within policies, and the nested policies qualify the enclosing policy. For the OpenAM session assertion,
     *                     the nested policy is empty, but must be present in the SecurityPolicy binding, as the cxf runtime
     *                     expects this nested policy element. Could be used to specify the version of the assertion, as is
     *                     done for Username tokens.
     * @param sessionId The OpenAM session id. When the OpenAMSessionAssertion builder constructs the OpenAMSessionAssertion,
     *                  it will set the sessionId. This sessionId will be specified by the client and set by the
     *                  OpenAMSessionTokenClientAssertionBuilder, and pulled from the BinarySecurityToken element which
     *                  encapsulates this sessionId when it arrives at the targeted sts.
     */
    public OpenAMSessionAssertion(SPConstants version, SPConstants.IncludeTokenType includeTokenType, Element nestedPolicy, String sessionId) {
        super(version);
        setInclusion(includeTokenType);
        setIgnorable(false);
        setOptional(false);
        setPolicy(nestedPolicy);
        this.sessionId = sessionId;
    }

    @Override
    public QName getName() {
        return AMSTSConstants.AM_SESSION_TOKEN_ASSERTION_QNAME;
    }

    /**
     * @see org.apache.neethi.Assertion
     * This method is not called as part of the OpenAMSessionTokenClientInterceptor to include this class' state in the
     * xml infoset (getTokenElement below is called instead), but rather seems to be called by classes in the
     * org.apache.cxf.ws.security.policy.model packages, such as InitiatorToken, ProtectionToken,
     * SupportingToken(signed and/or encrypted incarnation), RecipientSignatureToken, etc. However, it does not appear
     * that this method is ever called. See
     * http://cxf.547215.n5.nabble.com/Custom-SecurityPolicy-Assertions-and-the-Symmetric-binding-td5754879.html#a5755303
     * for details. I will leave the current implementation as purpose/invocation seems to be somewhat unknown, even to
     * the author of the cxf sts, and the wss4j lead. Note that it is not invoked to actually include a token in an
     * XML infoset, but rather write the SecurityPolicy elements corresponding to this token type. It is (probably)
     * not called because the SecurityPolicy in the wsdl is consulted directly.
     */
    @Override
    public void serialize(XMLStreamWriter writer) throws XMLStreamException {
        String localname = AMSTSConstants.AM_SESSION_TOKEN_ASSERTION_QNAME.getLocalPart();
        String namespaceURI = AMSTSConstants.AM_SESSION_TOKEN_ASSERTION_QNAME.getNamespaceURI();
        String prefix = writer.getPrefix(namespaceURI);
        if (prefix == null) {
            prefix = AMSTSConstants.AM_SESSION_TOKEN_ASSERTION_QNAME.getPrefix();
            writer.setPrefix(prefix, namespaceURI);
        }
        //start element of the OpenAMSessionToken
        writer.writeStartElement(prefix, localname, namespaceURI);
        writer.writeNamespace(prefix, namespaceURI);
        writer.writeAttribute(prefix, namespaceURI, SPConstants.ATTR_INCLUDE_TOKEN, SP12Constants.INCLUDE_ALWAYS);

        String pPrefix = writer.getPrefix(SPConstants.POLICY.getNamespaceURI());
        if (pPrefix == null) {
            pPrefix = SPConstants.POLICY.getPrefix();
            writer.setPrefix(SPConstants.POLICY.getPrefix(), SPConstants.POLICY.getNamespaceURI());
        }
        // write start element of nested policy element
        writer.writeStartElement(pPrefix, SPConstants.POLICY.getLocalPart(), SPConstants.POLICY
                .getNamespaceURI());
        // write end element of nested policy element
        writer.writeEndElement();
        // write end element of OpenAMSessionToken
        writer.writeEndElement();
    }

    /**
     * Called by the OpenAMSessionTokenClientInterceptor to obtain the xml defining the BinarySecurityToken encapsulating
     * the OpenAMSessionToken to be included in the STS invocation. Note that the various AbstractTokenInterceptor implementations
     * in the cxf code-base define a builder or token class to obtain the Element representation of the token to-be-included
     * in the soap security header. @see org.apache.cxf.ws.security.wss4j.UsernameTokenInterceptor#addToken for details.
     * In other words, the org.apache.neethi.Assertion#serialize method which this class inherits via the Token superclass
     * is not used by token-specific-interceptors to obtain the xml representation of the token.
     * @return the Element defining the BinarySecurityToken with the encapsulated OpenAMSessionToken.
     */
    public Element getTokenElement() {
        OpenAMSessionToken token = new OpenAMSessionToken(DOMUtils.createDocument());
        token.setSessionId(sessionId);
        return token.getElement();
    }

    /**
     * A private subclass of the wss4j BinarySecurityToken class, as an aid to obtain the xml corresponding to a
     * BinarySecurityToken necessary for inclusion in the soap security header by the OpenAMSessionTokenClientInterceptor.
     */
    private static class OpenAMSessionToken extends BinarySecurity {
        OpenAMSessionToken(Document doc) {
            super(doc);
            setValueType(AMSTSConstants.AM_SESSION_TOKEN_ASSERTION_BST_VALUE_TYPE);
        }

        /**
         * The BinarySecurity#setData method is used to set the binary data, but it base64-encodes by default. Adding a method
         * to set the token data, without any encoding.
         * @param sessionId The OpenAM session id to-be-included in the BST.
         */
        void setSessionId(String sessionId) {
            getFirstNode().setData(sessionId);
        }
    }
}
