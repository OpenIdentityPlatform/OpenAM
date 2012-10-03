/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: CreateRemoteIDP.java,v 1.4 2008/06/25 05:50:01 qcheng Exp $
 *
 */

package com.sun.identity.workflow;

import com.sun.identity.cot.COTException;
import java.util.Locale;
import java.util.Map;

/**
 * Creates Remote Identity Provider.
 */
public class CreateRemoteIDP
    extends Task
{
    public CreateRemoteIDP() {
    }

    /**
     * Creates remote identity provider.
     *
     * @param locale Locale of the request.
     * @param params Map of creation parameters.
     */
    public String execute(Locale locale, Map params)
        throws WorkflowException {
        validateParameters(params);
        String realm = getString(params, ParameterKeys.P_REALM);
        String metadataFile = getString(params, ParameterKeys.P_META_DATA);
        String metadata = getContent(metadataFile, locale);
        String[] results = ImportSAML2MetaData.importData(
            realm, metadata, null);

        String cot = getString(params, ParameterKeys.P_COT);
        if ((cot != null) && (cot.length() > 0)) {
            try {
                String entityId = results[1];
                AddProviderToCOT.addToCOT(realm, cot, entityId);
            } catch (COTException e) {
                throw new WorkflowException(e.getMessage());
            }
        }
        return getMessage("idp.configured", locale);
    }
    
    private void validateParameters(Map params)
        throws WorkflowException {
        String metadata = getString(params, ParameterKeys.P_META_DATA);
        if ((metadata == null) || (metadata.trim().length() == 0)) {
            throw new WorkflowException("meta-data-required", null);
        }
        String realm = getString(params, ParameterKeys.P_REALM);
        if ((realm == null) || (realm.trim().length() == 0)) {
            throw new WorkflowException("missing-realm", null);
        }
    }
}
