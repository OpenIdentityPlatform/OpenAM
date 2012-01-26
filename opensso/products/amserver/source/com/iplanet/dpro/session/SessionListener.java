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
 * $Id: SessionListener.java,v 1.2 2008/06/25 05:41:29 qcheng Exp $
 *
 */

package com.iplanet.dpro.session;

/**
 * The <code>SessionListener</code> interface needs to be implemented by
 * applications in order to receive session events. The method
 * <code>sessionChanged()</code> is invoked when a session event arrives.
 * 
 * @see com.iplanet.dpro.session.SessionEvent
 */

public interface SessionListener {
    /**
     * This method will be invoked when a session event arrives.
     * 
     * @param evt The session event object.
     */
    public void sessionChanged(SessionEvent evt);
}
