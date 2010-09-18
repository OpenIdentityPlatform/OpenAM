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
 * $Id: LogReadHandler.java,v 1.4 2008/06/25 05:43:36 qcheng Exp $
 *
 */



package com.sun.identity.log.handlers;

import java.io.IOException;
import java.util.Set;

/**LogReadHandler interface provides methods that must be present
 * in each log read handler to read a log file. The LogReader knows
 * only these methods and will call one of these as required.
 **/
public interface LogReadHandler {
    /**
     * LogReader calls this method method. It collects header, records,
     * applies query (if any), sorts (if asked) the records on field, checks
     * the max records to return, collects all the recods and returns.
     *
     * @param fileName is complete filename with path
     * @param qry is user specified qury chriteria with sorting requirement
     * @param sourceData it specifies whether return data should be original
     *        data received by logger (source) or formatted data as in file.
     * @return all the matched records with query
     * @throws IOException if it fails to read log records.
     * @throws NoSuchFieldException if it fails to retrieve the name of field.
     * @throws IllegalArgumentException if query has wrong value.
     * @throws RuntimeException if it fails to retrieve log record.
     * @throws Exception if it fails any of operation.
     */
    public String [][] logRecRead(
        String fileName,
        com.sun.identity.log.LogQuery qry,
        boolean sourceData
    ) throws IOException, NoSuchFieldException, IllegalArgumentException,
        RuntimeException, Exception;

    /**
     * LogReader calls this method method. It collects header, records,
     * applies query (if any), sorts (if asked) the records on field, checks
     * the max records to return, collects all the recods and returns.
     *
     * @param fileNames is a Set of filenames complete with path
     * @param qry is user specified qury chriteria with sorting requirement
     * @param sourceData it specifies whether return data should be original
     *        data received by logger (source) or formatted data as in file.
     * @return all the matched records with query
     * @throws IOException if it fails to read log records.
     * @throws NoSuchFieldException if it fails to retrieve the name of field.
     * @throws IllegalArgumentException if query has wrong value.
     * @throws RuntimeException if it fails to retrieve log record.
     * @throws Exception if it fails any of operation.
     */
    public String [][] logRecRead(
        Set fileNames,
        com.sun.identity.log.LogQuery qry,
        boolean sourceData
    ) throws IOException, NoSuchFieldException, IllegalArgumentException,
        RuntimeException, Exception;
}
