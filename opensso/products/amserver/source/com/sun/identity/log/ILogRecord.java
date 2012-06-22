/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: ILogRecord.java,v 1.1 2009/03/06 00:19:19 veiming Exp $
 */

package com.sun.identity.log;

import java.util.Map;

/**
 * Interface to get log information, subject of log by and log for.
 */
public interface ILogRecord {
    /**
     * Returns log information map.
     * 
     * @return log information map.
     */
    Map getLogInfoMap();
    
    /**
     * Adds to the log information map, the field key and its corresponding
     * value.
     *
     * @param key The key which will be used by the formatter to determine if
     *        this piece of info is supposed to be added to the log string
     *        according to the selected log fields.
     * @param value The value which may form a part of the actual log-string.
     */
    void addLogInfo(String key, Object value);

    /**
     * Returns log by subject.
     *
     * @return log by subject.
     */
    Object getLogBy();

    /**
     * Returns log for subject.
     *
     * @return log for subject.
     */
    Object getLogFor();
}
