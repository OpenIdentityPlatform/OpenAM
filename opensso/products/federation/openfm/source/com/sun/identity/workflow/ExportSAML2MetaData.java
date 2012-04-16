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
 * $Id: ExportSAML2MetaData.java,v 1.4 2009/09/21 17:27:04 exu Exp $
 *
 */

package com.sun.identity.workflow;

import com.sun.identity.saml2.jaxb.entityconfig.EntityConfigElement;
import com.sun.identity.saml2.jaxb.entityconfig.IDPSSOConfigElement;
import com.sun.identity.saml2.meta.SAML2MetaException;
import com.sun.identity.saml2.meta.SAML2MetaManager;
import com.sun.identity.saml2.meta.SAML2MetaUtils;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import javax.xml.bind.JAXBException;

/**
 * Export SAML2 Metadata.
 */
public class ExportSAML2MetaData {

    private ExportSAML2MetaData() {
    }

    public static String exportExtendedMeta(String realm, String entityID) 
        throws WorkflowException {
        try {
            String result = null;
            SAML2MetaManager metaManager = new SAML2MetaManager();
            EntityConfigElement config = metaManager.getEntityConfig(
                realm, entityID);
            if (config != null) {
                OutputStream os = new ByteArrayOutputStream();
                SAML2MetaUtils.convertJAXBToOutputStream(config, os);
                result = os.toString();
            }
            return result;
        } catch (JAXBException e) {
            throw new WorkflowException(e.getMessage());
        } catch (SAML2MetaException e) {
            throw new WorkflowException(e.getMessage());
        }
    }
    
}
