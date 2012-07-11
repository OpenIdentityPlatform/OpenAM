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
 * $Id: SSOTokenListener.java,v 1.3 2008/08/15 01:05:20 veiming Exp $
 *
 */

package com.iplanet.sso;

/**
 * The <code>SSOTokenListener</code> interface needs to be implemented by the
 * applications to receive SSO token events. The method
 * <code>ssoTokenChanged()</code> is invoked when an SSO token event arrives.
 * 
 * @see com.iplanet.sso.SSOTokenEvent
 * @supported.all.api
 */
public interface SSOTokenListener {
    /**
     * Handles single sign on token status changed event.
     * 
     * @param evt Single sign on token status changed event.
     */
    void ssoTokenChanged(SSOTokenEvent evt);
}
