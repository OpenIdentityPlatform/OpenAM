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
 * $Id: FSNameIdentifierHelper.java,v 1.2 2008/06/25 05:47:04 qcheng Exp $
 *
 */

package com.sun.identity.federation.services.util;

import com.sun.liberty.INameIdentifier;
import com.sun.identity.federation.common.FSUtils;
import com.sun.identity.federation.common.IFSConstants;
import com.sun.identity.federation.jaxb.entityconfig.BaseConfigType;
import com.sun.identity.federation.meta.IDFFMetaUtils;


/**
 * Helper class to pick the correct <code>INameIdentifier</code> implementation
 * for a given provider.
 */
public class FSNameIdentifierHelper {
    INameIdentifier generator = null;
    
    /**
     * Constructs a <code>FSNameIdentifierHelper</code> object.
     * @param hostedConfig hosted provider's extended meta
     */
    public FSNameIdentifierHelper(BaseConfigType hostedConfig) {
        if (FSUtils.debug.messageEnabled()) {
            FSUtils.debug.message("FSNameIdentifierGeneratorHelper constructor"
                + " called");
        }
        try {
            String className = IDFFMetaUtils.getFirstAttributeValueFromConfig(
                hostedConfig, IFSConstants.NAMEID_IMPL_CLASS);
            generator = (INameIdentifier)
                (Class.forName(className).newInstance());
        } catch (ClassNotFoundException exp) {
            FSUtils.debug.error("FSNameIdentifierGeneratorHelper constructor."
                + "Not able to create instance of Generator Impl", exp);
        } catch (Exception exp) {
            FSUtils.debug.error("FSNameIdentifierGeneratorHelper constructor."
                + "Not able to create instance of Generator Impl", exp);
        }
    }

    /**
     * Creates a new name identifier string.
     * @return name identifier string; <code>null</code> if an error occurred
     *  during the creation process.
     */
    public String createNameIdentifier(){
        if (FSUtils.debug.messageEnabled()) {
            FSUtils.debug.message("FSNameIdentifierGeneratorHelper."
                + "createNameIdentifier called");
        }
        if (generator != null) {
            return generator.createNameIdentifier();
        } else {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("FSNameIdentifierGeneratorHelper.create"
                    + "NameIdentifier returning null as generator is null");
            }
            return null;
        }
    }
}
