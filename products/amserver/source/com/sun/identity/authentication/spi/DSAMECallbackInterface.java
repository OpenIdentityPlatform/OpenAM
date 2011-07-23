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
 * $Id: DSAMECallbackInterface.java,v 1.2 2008/06/25 05:42:06 qcheng Exp $
 *
 */

package com.sun.identity.authentication.spi;

import javax.security.auth.callback.Callback ;
import java.util.Map;


/**
 * The <code>DSAMECallbackInterface</code> interface needs to be implemented
 * by services and applications which want to define custom
 * Callbacks. Look at <code>javax.security.auth.callback</code>
 * javadocs for details on Callbacks.
 */
public interface DSAMECallbackInterface  extends Callback  {
    /**
     * Sets the Map which contains the values required for the callback.
     *
     * @param configMap Map contains the values required by the custom callback.
     * @throws AuthenticationException when there is an error setting the Map.
     */
    public void setConfig(Map configMap) throws AuthenticationException;

    /**
     * Returns the Map containing key/values pairs required for the
     * <code>CustomCallback</code>.
     *
     * @return Map containing values required by the callback.
     */
    public Map getConfig();
}
