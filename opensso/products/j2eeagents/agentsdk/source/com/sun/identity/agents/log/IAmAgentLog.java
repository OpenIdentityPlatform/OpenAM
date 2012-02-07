/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: IAmAgentLog.java,v 1.2 2008/06/25 05:51:54 qcheng Exp $
 *
 */

package com.sun.identity.agents.log;

import com.iplanet.sso.SSOToken;
import com.sun.identity.agents.arch.AgentException;
import com.sun.identity.agents.arch.LocalizedMessage;

/**
 * The interface for AmAgentLog
 */
public interface IAmAgentLog {
    public abstract void initialize() throws AgentException;

    /**
     * Logs a local/remote message for j2ee agents
     * This function internally decides whether to do local or remote logs
     * @param token user SSO Token as a String
     * @param message LocalizedMessage object
     * @return boolean success of failure while logging
     *
     * @throws AgentException
     */
    public abstract boolean log(SSOToken token, LocalizedMessage message)
            throws AgentException;

    public static final int INT_LOG_MODE_LOCAL = 0;

    public static final int INT_LOG_MODE_REMOTE = 1;

    public static final int INT_LOG_MODE_ALL = 2;

    public static final int DEFAULT_INT_LOG_MODE = INT_LOG_MODE_LOCAL;

    public static final String STR_LOG_MODE_LOCAL = "LOCAL";

    public static final String STR_LOG_MODE_REMOTE = "REMOTE";

    public static final String STR_LOG_MODE_ALL = "ALL";
}
