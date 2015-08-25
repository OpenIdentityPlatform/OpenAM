/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: SetupListener.java,v 1.1 2010/01/20 17:01:35 veiming Exp $
 *
 * Portions Copyrighted 2015 ForgeRock AS.
 */

package com.sun.identity.setup;

/**
 * Implementors of this interface will be notified when setup is complete. Setup is complete when OpenDJ has started up
 * and the configuration is in a valid state (either upgrade or new install was successful).
 */
public interface SetupListener {

    /**
     * Called once setup is complete to indicate to the implementor that it is safe to add listeners to the SMS.
     *
     * @see com.sun.identity.sm.SMSObjectListener
     * @see com.sun.identity.sm.ServiceListener
     */
    void setupComplete();
}
