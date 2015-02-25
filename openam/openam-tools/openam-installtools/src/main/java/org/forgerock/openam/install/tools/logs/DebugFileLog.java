/*
 * Copyright 2014 ForgeRock, AS.
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

package org.forgerock.openam.install.tools.logs;

import com.sun.identity.install.tools.util.Debug;

/**
 * DebugLog implementation that delegates to the legacy file-based logging framework.
 *
 * @since 12.0.0
 */
public class DebugFileLog implements DebugLog {
    public void log(String message) {
        Debug.log(message);
    }

    public void log(String message, Throwable ex) {
        Debug.log(message, ex);
    }
}
