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
 * $Id: SAML2MetaServiceListener.java,v 1.5 2009/08/28 23:42:14 exu Exp $
 *
 */


package com.sun.identity.saml2.meta;

import com.sun.identity.shared.debug.Debug;
import com.sun.identity.saml2.key.KeyUtil;
import com.sun.identity.saml2.profile.IDPCache;
import com.sun.identity.saml2.profile.SPCache;
import com.sun.identity.plugin.configuration.ConfigurationListener;
import com.sun.identity.plugin.configuration.ConfigurationActionEvent;

/**
 * The <code>SAML2MetaServiceListener</code> implements
 * <code>ConfigurationListener</code> interface and is
 * used for maintaining the metadata cache.
 */
class SAML2MetaServiceListener implements ConfigurationListener
{
    private static Debug debug = SAML2MetaUtils.debug;

    SAML2MetaServiceListener() {
    }

    /**
     * This method will be invoked when a service's organization
     * configuation data has been changed.
     *
     * @param e the configuaration action event
     */
    public void configChanged(ConfigurationActionEvent e) {
        if (debug.messageEnabled()) {
            debug.message("SAML2MetaServiceListener.configChanged: config=" + 
                e.getConfigurationName() + ", component=" + 
                e.getComponentName());
        }
        SAML2MetaCache.clear();
        String realm = e.getRealm();
        SPCache.clear(realm);
        IDPCache.clear(realm);
        KeyUtil.clear();
    }
}
