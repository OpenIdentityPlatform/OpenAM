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
 * $Id: MechanismHandler.java,v 1.2 2008/06/25 05:47:07 qcheng Exp $
 * Portions Copyrighted 2014 ForgeRock AS.
 */


package com.sun.identity.liberty.ws.authnsvc.mechanism;

import com.sun.identity.liberty.ws.authnsvc.protocol.SASLRequest;
import com.sun.identity.liberty.ws.authnsvc.protocol.SASLResponse;
import com.sun.identity.liberty.ws.soapbinding.Message;

/**
 * The <code>MechanismHandler</code> interface needs to be implemented
 * for different SASL mechanisms to authenticate. Each SASL mechanism
 * will correspond to one handler implementation which processes incoming
 * SASL request and generates SASL response.
 * @supported.all.api
 * @deprecated since 12.0.0
 */
@Deprecated
public interface MechanismHandler {
    /**
     * Generates a SASL response according to the SASL request.
     * @param saslReq a SASL request
     * @param message a SOAP Message containing the SASL request
     * @param respMessageID messageID of SOAP Message response that will
     *                      contain returned SASL response
     * @return a SASL response
     */
    public SASLResponse processSASLRequest(SASLRequest saslReq,
                                       Message message, String respMessageID);
}
