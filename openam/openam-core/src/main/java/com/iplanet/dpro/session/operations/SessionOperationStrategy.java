/**
 * Copyright 2014 ForgeRock AS.
 *
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 */
package com.iplanet.dpro.session.operations;

import com.iplanet.dpro.session.Session;
import com.iplanet.dpro.session.SessionException;

/**
 * Responsible for providing an appropriate implementation of the SessionOperationStrategy
 * based on system dependent factors.
 *
 * Use Case: The ClientSDK shares code with Session, therefore it is inappropriate to
 * try and make one implementation of this strategy work in multiple cases.
 */
public interface SessionOperationStrategy {
    /**
     * Based on the Session, determine the appropriate SessionOperations strategy to select.
     *
     * @param session Non null Session to use.
     * @return A non null SessionOperations implementation to use.
     */
    public SessionOperations getOperation(Session session) throws SessionException;
}
