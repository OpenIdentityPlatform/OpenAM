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
 * $Id: Audit.java,v 1.2 2008/06/25 05:51:28 qcheng Exp $
 *
 */

package com.sun.identity.install.tools.util;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Hashtable;

public class Audit {

    public void log(LocalizedMessage message) {
        getLogWriter().println(LogHelper.getDateStamp() + message);
    }

    public void logEmptyLine() {
        getLogWriter().println();
    }

    public String getName() {
        return name;
    }

    private Audit(String name) throws Exception {
        setName(name);
        setAuditLogFileName(name);
        File auditLogFile = new File(getAuditLogFileName());
        LogHelper.archiveLastLogFile(auditLogFile);
        setLogWriter(new PrintWriter(new FileWriter(auditLogFile)));
        LogHelper.addLogWriterShutdownHook(getLogWriter());
    }

    private void setName(String name) {
        this.name = name;
    }

    private PrintWriter getLogWriter() {
        return logWriter;
    }

    private void setLogWriter(PrintWriter writer) {
        logWriter = writer;
    }

    public String getAuditLogFileName() {
        return fileName;
    }

    private void setAuditLogFileName(String name) {
        fileName = STR_AUDIT_LOGS_DIR + "/" + name + STR_AUDIT_LOG_EXT;
    }

    public synchronized static Audit getInstance(String name) {
        Audit result = null;

        try {
            result = (Audit) getAuditInstances().get(name);
            if (result == null) {
                result = new Audit(name);
                getAuditInstances().put(name, result);
            }
        } catch (Exception ex) {
            Debug.log("Failed to create audit instance name: " + name, ex);
            throw new RuntimeException(LocalizedMessage.get(
                    MSG_AUDIT_INIT_FAILED).toString());
        }

        return result;
    }

    private static void initializeAudit() {
        try {
            File auditLogsDir = new File(STR_AUDIT_LOGS_DIR);
            LogHelper.initializeLogsDir(auditLogsDir);
        } catch (Exception ex) {
            Debug.log("Failed to initialize Audit", ex);
            throw new RuntimeException(LocalizedMessage.get(
                    MSG_AUDIT_INIT_FAILED).toString());
        }
    }

    private static Hashtable getAuditInstances() {
        return auditInstances;
    }

    private String name;

    private PrintWriter logWriter;

    private String fileName;

    private static Hashtable auditInstances = new Hashtable();

    public static final String MSG_AUDIT_INIT_FAILED = "audit_init_error";

    public static final String STR_AUDIT_LOGS_DIR_NAME = "audit";

    public static final String STR_AUDIT_LOG_EXT = ".log";

    public static final String STR_AUDIT_LOGS_DIR = ConfigUtil.getLogsDirPath()
            + "/" + STR_AUDIT_LOGS_DIR_NAME;

    /** Field LINE_SEP * */
    public static final String LINE_SEP = System.getProperty("line.separator");

    static {
        initializeAudit();
    }
}
