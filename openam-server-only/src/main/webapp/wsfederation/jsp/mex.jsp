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
             import="org.forgerock.openam.utils.StringUtils"
%><s:Envelope xmlns:s="http://www.w3.org/2003/05/soap-envelope" xmlns:a="http://www.w3.org/2005/08/addressing">
    <s:Header>
        <a:Action s:mustUnderstand="1">http://schemas.xmlsoap.org/ws/2004/09/transfer/GetResponse</a:Action>
        <% if (StringUtils.isNotEmpty((String) request.getAttribute("inResponseTo"))) { %>
            <a:RelatesTo>${inResponseTo}</a:RelatesTo>
        <% } %>
    </s:Header>
    <s:Body>
        <Metadata xmlns="http://schemas.xmlsoap.org/ws/2004/09/mex" xmlns:xsd="http://www.w3.org/2001/XMLSchema"
                  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                  xmlns:wsx="http://schemas.xmlsoap.org/ws/2004/09/mex">
            <wsx:MetadataSection Dialect="http://schemas.xmlsoap.org/wsdl/"
                                 Identifier="http://forgerock.org/ws-fed/securitytokenservice"
                                 xmlns="">
                <jsp:include page="wsdl.jsp" />
            </wsx:MetadataSection>
        </Metadata>
    </s:Body>
</s:Envelope>