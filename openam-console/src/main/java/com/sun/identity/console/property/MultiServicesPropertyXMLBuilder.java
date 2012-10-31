/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: MultiServicesPropertyXMLBuilder.java,v 1.2 2008/06/25 05:43:09 qcheng Exp $
 *
 */

package com.sun.identity.console.property;

import com.iplanet.sso.SSOException;
import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.sm.AttributeSchema;
import com.sun.identity.sm.ServiceSchema;
import com.sun.identity.sm.SMSException;

public class MultiServicesPropertyXMLBuilder
    extends PropertyXMLBuilder
{
    private String svcName;

    /**
     * Returns a XML for displaying attribute in property sheet.
     *
     * @param serviceSchema Service schemas.
     * @param model Model for getting localized string and user locale.
     * @throws SMSException if attribute schema cannot obtained.
     * @throws SSOException if single sign on token is invalid.
     */
    public MultiServicesPropertyXMLBuilder(
        ServiceSchema serviceSchema,
        AMModel model
    ) throws SMSException, SSOException {
        super(serviceSchema, model);
        svcName = serviceSchema.getServiceName();
    }

    protected String getAttributeNameForPropertyXML(AttributeSchema as) {
        return svcName + '_' +  as.getName();
    }
}
