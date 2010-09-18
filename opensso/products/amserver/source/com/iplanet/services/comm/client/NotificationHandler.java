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
 * $Id: NotificationHandler.java,v 1.2 2008/06/25 05:41:33 qcheng Exp $
 *
 */

package com.iplanet.services.comm.client;

import java.util.Vector;

/**
 * The <code>NotificationHandler</code> interface needs to be implemented by
 * high level services and applications in order to be able to receive
 * notifications from the Platform Low Level API. The handler registration is
 * done through the PLLClient class.
 * 
 * @see com.iplanet.services.comm.share.Notification
 * @see com.iplanet.services.comm.client.PLLClient
 */

public interface NotificationHandler {
    /**
     * This interface must be implemented by the high level applications and
     * applications in order to receive notifications from the Platform Low
     * Level API.
     * 
     * @param notifications
     *            A Vector of Notification objects.
     */
    public void process(Vector notifications);
}
