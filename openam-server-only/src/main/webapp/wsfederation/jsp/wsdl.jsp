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
--%><%@ page contentType="text/xml;charset=UTF-8" language="java" session="false"
%><wsdl:definitions name="SecurityTokenService"
                    targetNamespace="http://forgerock.org/ws-fed/securitytokenservice"
                    xmlns:xsd="http://www.w3.org/2001/XMLSchema"
                    xmlns:tns="http://forgerock.org/ws-fed/securitytokenservice"
                    xmlns:wsp="http://schemas.xmlsoap.org/ws/2004/09/policy"
                    xmlns:wsa10="http://www.w3.org/2005/08/addressing"
                    xmlns:soap12="http://schemas.xmlsoap.org/wsdl/soap12/"
                    xmlns:wsaw="http://www.w3.org/2006/05/addressing/wsdl"
                    xmlns:wsu="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd"
                    xmlns:wsdl="http://schemas.xmlsoap.org/wsdl/"
                    xmlns:wst="http://schemas.xmlsoap.org/ws/2005/02/trust">
    <wsp:Policy wsu:Id="usernameAuth">
        <wsp:ExactlyOne>
            <wsp:All>
                <sp:TransportBinding xmlns:sp="http://schemas.xmlsoap.org/ws/2005/07/securitypolicy">
                    <wsp:Policy>
                        <sp:TransportToken>
                            <wsp:Policy>
                                <sp:HttpsToken RequireClientCertificate="false"/>
                            </wsp:Policy>
                        </sp:TransportToken>
                        <sp:AlgorithmSuite>
                            <wsp:Policy>
                                <sp:Basic256/>
                            </wsp:Policy>
                        </sp:AlgorithmSuite>
                        <sp:Layout>
                            <wsp:Policy>
                                <sp:Strict/>
                            </wsp:Policy>
                        </sp:Layout>
                        <sp:IncludeTimestamp/>
                    </wsp:Policy>
                </sp:TransportBinding>
                <sp:SignedSupportingTokens xmlns:sp="http://schemas.xmlsoap.org/ws/2005/07/securitypolicy">
                    <wsp:Policy>
                        <sp:UsernameToken
                                sp:IncludeToken="http://schemas.xmlsoap.org/ws/2005/07/securitypolicy/IncludeToken/AlwaysToRecipient">
                            <wsp:Policy>
                                <sp:WssUsernameToken10/>
                            </wsp:Policy>
                        </sp:UsernameToken>
                    </wsp:Policy>
                </sp:SignedSupportingTokens>
                <sp:EndorsingSupportingTokens xmlns:sp="http://schemas.xmlsoap.org/ws/2005/07/securitypolicy">
                    <wsp:Policy>
                        <mssp:RsaToken
                                sp:IncludeToken="http://schemas.xmlsoap.org/ws/2005/07/securitypolicy/IncludeToken/Never"
                                wsp:Optional="true"
                                xmlns:mssp="http://schemas.microsoft.com/ws/2005/07/securitypolicy"/>
                        <sp:SignedParts>
                            <sp:Header Name="To" Namespace="http://www.w3.org/2005/08/addressing"/>
                        </sp:SignedParts>
                    </wsp:Policy>
                </sp:EndorsingSupportingTokens>
                <sp:Wss11 xmlns:sp="http://schemas.xmlsoap.org/ws/2005/07/securitypolicy">
                    <wsp:Policy/>
                </sp:Wss11>
                <sp:Trust10 xmlns:sp="http://schemas.xmlsoap.org/ws/2005/07/securitypolicy">
                    <wsp:Policy>
                        <sp:MustSupportIssuedTokens/>
                        <sp:RequireClientEntropy/>
                        <sp:RequireServerEntropy/>
                    </wsp:Policy>
                </sp:Trust10>
                <wsaw:UsingAddressing/>
            </wsp:All>
        </wsp:ExactlyOne>
    </wsp:Policy>
    <wsdl:types>
        <xsd:schema targetNamespace="http://forgerock.org/ws-fed/securitytokenservice/schema">
            <xsd:import namespace="http://schemas.xmlsoap.org/ws/2005/02/trust"
                        schemaLocation="${baseUrl}/wsfederation/xsd/ws-trust-1.0.xsd"/>
        </xsd:schema>
    </wsdl:types>
    <wsdl:message name="RST_InputMessage">
        <wsdl:part name="request" element="wst:RequestSecurityToken"/>
    </wsdl:message>
    <wsdl:message name="RST_OutputMessage">
        <wsdl:part name="RST_OperationResult" element="wst:RequestSecurityTokenResponseCollection"/>
    </wsdl:message>
    <wsdl:portType name="RST_PortType">
        <wsdl:operation name="RST_Operation">
            <wsdl:input wsaw:Action="http://schemas.xmlsoap.org/ws/2005/02/trust/RST/Issue"
                        message="tns:RST_InputMessage"/>
            <wsdl:output wsaw:Action="http://schemas.xmlsoap.org/ws/2005/02/trust/RSTR/Issue"
                         message="tns:RST_OutputMessage"/>
        </wsdl:operation>
    </wsdl:portType>
    <wsdl:binding name="UsernameBinding" type="tns:RST_PortType">
        <wsp:PolicyReference URI="#usernameAuth"/>
        <soap12:binding transport="http://schemas.xmlsoap.org/soap/http"/>
        <wsdl:operation name="RST_Operation">
            <soap12:operation soapAction="http://schemas.xmlsoap.org/ws/2005/02/trust/RST/Issue" style="document"/>
            <wsdl:input>
                <soap12:body use="literal"/>
            </wsdl:input>
            <wsdl:output>
                <soap12:body use="literal"/>
            </wsdl:output>
        </wsdl:operation>
    </wsdl:binding>
    <wsdl:service name="SecurityTokenService">
        <wsdl:port name="UsernamePort"
                   binding="tns:UsernameBinding">
            <soap12:address location="${baseUrl}/WSFederationServlet/sts/metaAlias${metaAlias}"/>
            <wsa10:EndpointReference>
                <wsa10:Address>${baseUrl}/WSFederationServlet/sts/metaAlias${metaAlias}</wsa10:Address>
            </wsa10:EndpointReference>
        </wsdl:port>
    </wsdl:service>
</wsdl:definitions>
