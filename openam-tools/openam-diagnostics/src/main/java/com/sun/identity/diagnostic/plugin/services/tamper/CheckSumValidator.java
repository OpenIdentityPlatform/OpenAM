/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: CheckSumValidator.java,v 1.1 2008/11/22 02:41:22 ak138937 Exp $
 *
 */

package com.sun.identity.diagnostic.plugin.services.tamper;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Set;

import com.sun.identity.diagnostic.base.core.common.DTException;
import com.sun.identity.diagnostic.base.core.log.IToolOutput;
import com.sun.identity.diagnostic.plugin.services.common.ServiceBase;

public class CheckSumValidator extends ServiceBase implements ITamperDetector {
    
    private IToolOutput toolOutWriter;
    
    /** Creates a new instance of CheckSunValidator */
    public CheckSumValidator() {
    }
    
    public void detectTamper(
        String configPath,
        String backupPath,
        Set<String> dirFilter,
        Set<String> fileFilter
    ) throws DTException {
        try {
            toolOutWriter = TamperDetectionService.getToolWriter();
            toolOutWriter.printMessage("tamper-detect-msg");
            File configDir = new File(configPath);
            Properties checksum = TamperDetectionUtils.getChecksum(
                TamperDetectionUtils.SHA1, configDir, dirFilter, fileFilter);
            FileInputStream fin = null;
            Properties oldChecksum = new Properties();
            try {
                fin = new FileInputStream(backupPath);
                oldChecksum.load(fin);
            } finally {
                if (fin != null) {
                    try {
                        fin.close();
                    } catch (IOException ignored) {}
                }
            }
            Properties fileLost = new Properties();
            for (Enumeration e = oldChecksum.propertyNames();
                e.hasMoreElements();) {
                String key = (String) e.nextElement();
                String value = oldChecksum.getProperty(key);
                String newValue = checksum.getProperty(key);
                if (newValue != null) {
                    if (value.equals(newValue)) {
                        checksum.remove(key);
                    }
                } else {
                    fileLost.setProperty(key, value);
                }
            }
            if (!checksum.isEmpty() || !fileLost.isEmpty()) {
                toolOutWriter.printMessage(PARA_SEP_1);
                toolOutWriter.printMessage("tamper-detect-files-msg");
                toolOutWriter.printMessage(PARA_SEP_1);
                
                if (!checksum.isEmpty()) {
                    toolOutWriter.printMessage("tamper-detect-files-modified");
                    toolOutWriter.printMessage(SMALL_LINE_SEP_1);
                }
                for (Enumeration e = checksum.propertyNames();
                    e.hasMoreElements();) {
                    toolOutWriter.printResult((String) e.nextElement());
                }
                if (!fileLost.isEmpty()) {
                    toolOutWriter.printMessage("\n");
                    toolOutWriter.printMessage("tamper-detect-files-deleted");
                    toolOutWriter.printMessage(SMALL_LINE_SEP_1);
                }
                for (Enumeration e = fileLost.propertyNames(); 
                    e.hasMoreElements();) {
                    toolOutWriter.printResult((String) e.nextElement());
                }
                toolOutWriter.printStatusMsg(false, "tamper-detect-status");
            } else {
                toolOutWriter.printStatusMsg(true, "tamper-detect-status");
            }
        } catch(Exception ex) {
            throw new DTException(ex.getMessage());
        }
    }
}
