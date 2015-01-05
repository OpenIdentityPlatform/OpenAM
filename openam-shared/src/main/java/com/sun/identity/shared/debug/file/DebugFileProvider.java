/**
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
 *
 * Copyright 2014 ForgeRock AS.
 */
package com.sun.identity.shared.debug.file;


/**
 * Provide the debug file instance associated with a debug name
 * It maintains the integrity constraint than a log file is controlled by only one debug file instance
 */
public interface DebugFileProvider {

    /**
     * Returns an instance of <code>IDebugFile</code>.
     *
     * @param debugName name of the debug instance which will be returned.
     * @return an instance of <code>IDebugFile</code> type known by the given
     * <code>debugName</code> value.
     */
    DebugFile getInstance(String debugName);

    /**
     * Returns an instance of <code>IDebugFile</code> linked with stdout.
     *
     * @return an instance of <code>IDebugFile</code> for stdout
     */
    DebugFile getStdOutDebugFile();

}
