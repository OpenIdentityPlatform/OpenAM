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
 * $Id: ChecksumCreator.java,v 1.1 2008/11/22 02:41:22 ak138937 Exp $
 *
 */

package com.sun.identity.diagnostic.plugin.services.tamper;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Properties;
import java.util.Set;

import com.sun.identity.diagnostic.base.core.common.DTException;
import com.sun.identity.diagnostic.base.core.log.IToolOutput;
import com.sun.identity.diagnostic.plugin.services.common.ServiceBase;

public class ChecksumCreator extends ServiceBase implements ITamperDetector {
    
    private IToolOutput toolOutWriter;
    
    /** Creates a new instance of ChecksumCreator */
    public ChecksumCreator() {
    }
    
    public void detectTamper(
        String configPath, 
        String backupFile, 
        Set<String> dirFilter, 
        Set<String> fileFilter
    ) throws DTException {
        try {
            toolOutWriter = TamperDetectionService.getToolWriter();
            toolOutWriter.printMessage("tamper-checksum-creating");
            File configDir = new File(configPath);
            Properties checksum = TamperDetectionUtils.getChecksum(
                TamperDetectionUtils.SHA1, configDir, dirFilter, fileFilter);
            FileOutputStream fout = null;
            try {
                fout = new FileOutputStream(backupFile);
                checksum.store(fout, "");
                toolOutWriter.printStatusMsg(true , "tamper-checksum-created");
            } finally {
                if (fout != null) {
                    fout.close();
                }
            }
        } catch(Exception ex) {
            toolOutWriter.printStatusMsg(false , "tamper-checksum-created");
            throw new DTException(ex.getMessage());
        }
    }    
}
