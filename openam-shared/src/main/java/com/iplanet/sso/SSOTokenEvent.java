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
 * $Id: SSOTokenEvent.java,v 1.4 2008/08/15 01:05:20 veiming Exp $
 *
 */

package com.iplanet.sso;

/**
 * The <code>SSOTokenEvent</code> is an interface that represents an SSO token
 * event.The single sign on token event represents a change in 
 * <code>SSOToken</code>.
 * </p>
 * The following are possible SSO token event types:
 * <ul>
 * <li><code>SSO_TOKEN_IDLE_TIMEOUT</code>,
 * <li><code>SSO_TOKEN_MAX_TIMEOUT</code> and
 * <li><code>SSO_TOKEN_DESTROY</code>
 * <li><code>SSO_TOKEN_PROPERTY_CHANGED</code>
 * </ul>
 *
 * @supported.all.api
 */
public interface SSOTokenEvent {
    /**
     * SSO Token idle timeout event.
     */
    int SSO_TOKEN_IDLE_TIMEOUT = 1;

    /**
     * SSO Token maximum time out event.
     */
    int SSO_TOKEN_MAX_TIMEOUT = 2;

    /**
     * SSO Token destroy event.
     */
    int SSO_TOKEN_DESTROY = 3;

    /**
     * SSO Token property changed event.
     */
    int SSO_TOKEN_PROPERTY_CHANGED = 4;

    /**
     * Returns the <code>SSOToken</code> associated with the SSO Token event.
     * 
     * @return <code>SSOToken</code>
     */
    SSOToken getToken();

    /**
     * Returns the time of this event.
     * 
     * @return The event time as <code>UTC</code> milliseconds from the epoch.
     */
    long getTime();

    /**
     * Returns the type of this event.
     * 
     * @return The type of this event. Possible types are :
     *         <ul>
     *         <li><code>SSO_TOKEN_IDLE_TIMEOUT</code></li>
     *         <li><code>SSO_TOKEN_MAX_TIMEOUT</code></li>
     *         <li><code>SSO_TOKEN_DESTROY</code></li> and
     *         <li><code>SSO_TOKEN_PROPERTY_CHANGED</code></li>
     *         </ul>
     * @throws SSOException if the <code>SSOTokenEvent</code> type is
     *         not one of the above.
     */
    int getType() throws SSOException;
}
