/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * https://opensso.dev.java.net/public/CDDLv1.0.html or
 * opensso/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at opensso/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: LogoutResponse.java,v 1.2 2008/06/25 05:47:56 qcheng Exp $
 *
 * Portions Copyrighted 2016 ForgeRock AS.
 */


package com.sun.identity.saml2.protocol;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.sun.identity.saml2.protocol.impl.LogoutResponseImpl;

/**
 * This class represents the <code>LogoutResponse</code> element in
 * SAML protocol schema.
 * The recipient of a <code>LogoutRequest</code> message MUST respond with a
 * <code>LogoutResponse</code> message, of type <code>StatusResponseType</code>,
 * with no additional content specified.
 *
 * <pre>
 * &lt;element name="LogoutResponse" 
 * type="{urn:oasis:names:tc:SAML:2.0:protocol}StatusResponseType"/>
 * </pre>
 *
 * @supported.all.api
 */

@JsonTypeInfo(include = JsonTypeInfo.As.PROPERTY, use = JsonTypeInfo.Id.CLASS,
        defaultImpl = LogoutResponseImpl.class)
public interface LogoutResponse
extends com.sun.identity.saml2.protocol.StatusResponse {   
                                                                          
}
