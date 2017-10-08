<%--
    The contents of this file are subject to the terms of the Common Development and
    Distribution License (the License). You may not use this file except in compliance with the
    License.

    You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
    specific language governing permission and limitations under the License.

    When distributing Covered Software, include this CDDL Header Notice in each file and include
    the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
    Header, with the fields enclosed by brackets [] replaced by your own identifying
    information: "Portions copyright [year] [name of copyright owner]".

    Copyright 2016 ForgeRock AS.
--%><%@ page contentType="application/soap+xml;charset=UTF-8" language="java" session="false"
%><s:Envelope xmlns:s="http://www.w3.org/2003/05/soap-envelope" xmlns:a="http://www.w3.org/2005/08/addressing" xmlns:u="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd">
    <s:Header>
        <a:Action s:mustUnderstand="1">http://schemas.xmlsoap.org/ws/2005/02/trust/RSTR/Issue</a:Action>
        <a:RelatesTo>${inResponseTo}</a:RelatesTo>
        <o:Security s:mustUnderstand="1" xmlns:o="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd">
            <u:Timestamp u:Id="_0">
                <u:Created>${responseCreated}</u:Created>
                <u:Expires>${responseExpires}</u:Expires>
            </u:Timestamp>
        </o:Security>
    </s:Header>
    <s:Body>
        <wst:RequestSecurityTokenResponse xmlns:wst="http://schemas.xmlsoap.org/ws/2005/02/trust">
            <wst:Lifetime>
                <wsu:Created xmlns:wsu="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd">${notBefore}</wsu:Created>
                <wsu:Expires xmlns:wsu="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd">${notOnOrAfter}</wsu:Expires>
            </wst:Lifetime>
            <wsp:AppliesTo xmlns:wsp="http://schemas.xmlsoap.org/ws/2004/09/policy">
                <wsa:EndpointReference xmlns:wsa="http://www.w3.org/2005/08/addressing">
                    <wsa:Address>${targetAddress}</wsa:Address>
                </wsa:EndpointReference>
            </wsp:AppliesTo>
            ${requestedSecurityToken}
            <wst:RequestedAttachedReference>
                <o:SecurityTokenReference k:TokenType="http://docs.oasis-open.org/wss/oasis-wss-saml-token-profile-1.1#SAMLV1.1" xmlns:o="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd" xmlns:k="http://docs.oasis-open.org/wss/oasis-wss-wssecurity-secext-1.1.xsd">
                    <o:KeyIdentifier ValueType="http://docs.oasis-open.org/wss/oasis-wss-saml-token-profile-1.0#SAMLAssertionID">${assertionId}</o:KeyIdentifier>
                </o:SecurityTokenReference>
            </wst:RequestedAttachedReference>
            <wst:RequestedUnattachedReference>
                <o:SecurityTokenReference k:TokenType="http://docs.oasis-open.org/wss/oasis-wss-saml-token-profile-1.1#SAMLV1.1" xmlns:o="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd" xmlns:k="http://docs.oasis-open.org/wss/oasis-wss-wssecurity-secext-1.1.xsd">
                    <o:KeyIdentifier ValueType="http://docs.oasis-open.org/wss/oasis-wss-saml-token-profile-1.0#SAMLAssertionID">${assertionId}</o:KeyIdentifier>
                </o:SecurityTokenReference>
            </wst:RequestedUnattachedReference>
            <wst:TokenType>urn:oasis:names:tc:SAML:1.0:assertion</wst:TokenType>
            <wst:RequestType>http://schemas.xmlsoap.org/ws/2005/02/trust/Issue</wst:RequestType>
            <wst:KeyType>http://schemas.xmlsoap.org/ws/2005/05/identity/NoProofKey</wst:KeyType>
        </wst:RequestSecurityTokenResponse>
    </s:Body>
</s:Envelope>