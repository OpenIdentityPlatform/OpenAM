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
 * $Id: LogMessageProvider.java,v 1.4 2008/06/25 05:43:37 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.log.messageid;

import com.sun.identity.log.LogRecord;
import java.util.List;

/**
 * This interface defines a methods for a log message provider.
 */
public interface LogMessageProvider {
    /**
     * Returns all message IDs.
     *
     * @return all message IDs.
     */
    List getAllMessageIDs();

    /**
     * Creates Log Record.
     *
     * @param messageIDName Name of Message ID.
     * @param dataInfo Array of dataInfo.
     * @param ssoToken Single sign on token which will be used to fill in
     *        details like client IP address into the log record.
     */
    LogRecord createLogRecord(
        String messageIDName,
        String[] dataInfo,
        Object ssoToken);
}
