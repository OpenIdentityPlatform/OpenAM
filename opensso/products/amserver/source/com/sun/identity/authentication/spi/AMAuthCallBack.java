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
 * $Id: AMAuthCallBack.java,v 1.2 2008/06/25 05:42:06 qcheng Exp $
 *
 */


package com.sun.identity.authentication.spi;

import java.util.Map;

/**
 * The <code>AMAuthCallBack</code> interface should be implemented by external
 * business logic code, in order to receive callbacks from the authentication
 * framework when one of the following events happens :
 * <ul>
 *  <li>account lockout</li>
 *  <li>password change (via LDAP module)</li>
 * </ul>
 * <p>
 * The event type and related information are passed through the method call
 * {@link #authEventCallback(int, java.util.Map)}. The event parameters are
 * stored in a hash map. See information about public static fields for more 
 * details.
 * <p>
 * A plug-in class, which implements this interface, can retrieve the event
 * parameters using the get() function of the {@link Map} interface :
 * <code>eventParams.get(TIME_KEY);</code>
 * <code>eventParams.get(USER_KEY);</code>
 * <code>etc.</code>
 * <p>
 *
 * @supported.all.api
 */
public interface AMAuthCallBack {

    /**
     * The key for the value of the callback notification time (i.e. when
     * the event occured).
     */
    public static final String TIME_KEY = "timechanged";
    
    /**
     * The key for the value of the module's realm, for which the
     * callback was triggered.
     */
    public static final String REALM_KEY = "realm";
    
    /**
     * The key for the value of the user's identity for which the
     * callback was triggered.
     */
    public static final String USER_KEY = "userdn";
    
    /**
     * The constant for the PASSWORD CHANGE event type.
     */ 
    public static final int PASSWORD_CHANGE = 1;

    /**
     * The constant for the ACCOUNT LOCKOUT event type.
     */
    public static final int ACCOUNT_LOCKOUT = 2;
    
    /**
     * Receives the event notifications when the user status changes during
     * the authentication process. It includes the type of event and other
     * information pertinent to this event.
     * @param eventType the type of event for which this method is being called
     * @param eventParams a map of different parameters meaningful for this
     * event.
     * @exception AMAuthCallBackException can be thrown back
     * if necessary (currently not supported). It will be ignored by
     * authentication processing classes.
     */
    public void authEventCallback(int eventType, Map eventParams)
        throws AMAuthCallBackException; 
}
