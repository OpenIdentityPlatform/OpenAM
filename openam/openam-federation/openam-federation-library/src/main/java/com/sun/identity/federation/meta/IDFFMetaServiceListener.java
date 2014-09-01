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
 * $Id: IDFFMetaServiceListener.java,v 1.3 2008/06/25 05:46:49 qcheng Exp $
 *
 */


package com.sun.identity.federation.meta;

import com.sun.identity.federation.meta.IDFFMetaCache;
import com.sun.identity.federation.meta.IDFFMetaUtils;
import com.sun.identity.shared.debug.Debug;

import com.sun.identity.plugin.configuration.ConfigurationListener;
import com.sun.identity.plugin.configuration.ConfigurationActionEvent;
import com.sun.identity.federation.key.KeyUtil;
/**
 * The <code>IDFFMetaServiceListener</code> implements
 * <code>ConfigurationListener</code> interface . This class listens
 * for federation metadata configuration changes and updates the
 * metadata cache.
 */
public class IDFFMetaServiceListener implements ConfigurationListener {
    private static Debug debug = IDFFMetaUtils.debug;

    public IDFFMetaServiceListener() {
    }

    /**
     * Updates the federation metadata cache .
     *
     * @param event the configuaration action event
     */
    public void configChanged(ConfigurationActionEvent event) {
        String componentName = event.getComponentName();

        if (debug.messageEnabled()) {
            debug.message("IDFFMetaServiceListener.configChanged: name=" 
                + componentName + ", config=" + event.getConfigurationName()
                + ", realm=" + event.getRealm() + ", type=" + event.getType());
        } 
        if (componentName == null || 
                componentName.equals(IDFFMetaUtils.IDFF_META_SERVICE)) {
  
            if (debug.messageEnabled()) {
                debug.message("IDFFMetaListener.configChanged: update cache"); 
            } 
            IDFFMetaCache.clearCache();
            KeyUtil.encHash.clear();
        }
    }
}
