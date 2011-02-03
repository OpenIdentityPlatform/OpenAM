/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2005 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: AMServiceListener.java,v 1.4 2008/06/25 05:41:22 qcheng Exp $
 *
 */

package com.iplanet.am.sdk;

/**
 * The interface <code>AMServiceListener</code> needs to be implemented by
 * applications in order to receive service data change notifications. The
 * method <code>serviceChanged()</code> is invoked when a service schema data
 * or a service configuration data has been changed.
 *
 * @deprecated  As of Sun Java System Access Manager 7.1.
 * @supported.all.api
 */
public interface AMServiceListener {
    /**
     * This method will be invoked when a service's schema has been changed.
     * 
     * @param serviceName
     *            name of the service
     * @param version
     *            version of the service
     */
    public void schemaChanged(String serviceName, String version);
}
