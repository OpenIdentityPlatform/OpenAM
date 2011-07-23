/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems, Inc. All Rights Reserved.
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
 * $Id: WebContainerConfigInfoBase.java,v 1.2 2008/08/29 10:08:04 kanduls Exp $
 */

package com.sun.identity.tune.base;

import com.sun.identity.tune.common.MessageWriter;
import com.sun.identity.tune.common.AMTuneException;
import com.sun.identity.tune.common.AMTuneLogger;
import com.sun.identity.tune.constants.WebContainerConstants;
import com.sun.identity.tune.util.AMTuneUtil;
import java.io.File;

/**
 * This contains all the common properties for Web Server and Application server
 * 
 */
public abstract class WebContainerConfigInfoBase implements 
        WebContainerConstants {
    private String containerInstanceDir;
    private String webContainer;
    protected AMTuneLogger pLogger;
    protected MessageWriter mWriter;
    protected boolean isJVM64Bit;
    
    /**
     * Constructs new WebContainerConfigInfoBase object.
     * @throws com.sun.identity.tune.common.AMTuneException
     */
    public WebContainerConfigInfoBase() 
    throws AMTuneException {
        isJVM64Bit = false;
        pLogger = AMTuneLogger.getLoggerInst();
        mWriter = MessageWriter.getInstance();
        
    }
   
    /**
     * Set container instance directory.
     * @param containerInstanceDir container instance directory.
     * @throws com.sun.identity.tune.common.AMTuneException
     */
    protected void setContainerInstanceDir(String containerInstanceDir) 
    throws AMTuneException {
            if (containerInstanceDir != null &&
                containerInstanceDir.trim().length() > 0) {
            File instDir = new File(containerInstanceDir.trim());
            if (!instDir.isDirectory()) {
                AMTuneUtil.printErrorMsg(CONTAINER_INSTANCE_DIR);
                throw new AMTuneException(AMTuneUtil.getResourceBundle()
                        .getString("pt-web-inst-dir-not-found"));
            } else {
                this.containerInstanceDir = containerInstanceDir.trim();
            }
        } else {
            AMTuneUtil.printErrorMsg(CONTAINER_INSTANCE_DIR);
            throw new AMTuneException(AMTuneUtil.getResourceBundle()
                    .getString("pt-web-inst-dir-not-found"));
        }
    }
    
    /**
     * Return container instance directory.
     * @return container instance directory.
     */
    public String getContainerInstanceDir() {
        return containerInstanceDir;
    }
    
    /**
     * Set Web container type.
     * @param webContainer
     */
    protected void setWebContainer(String webContainer) {
        this.webContainer = webContainer;
    }
    
    /**
     * Return Web Container type.
     * @return webContainer type.
     */
    public String getWebContainer() {
        return webContainer;
    }
    
    /**
     * set true if jvm is 64 bit.
     * @param jvm64bitEnabled true if jvm is 64 bit.
     */
    protected void setJVM64BitEnabled(boolean jvm64bitEnabled) {
        this.isJVM64Bit = jvm64bitEnabled;
    }
    
    /**
     * Return true if jvm is 64 bit enabled.
     * @return
     */
    public boolean isJVM64Bit() {
        return isJVM64Bit;
    }
    
    /**
     * Deletes the password file.
     */
    protected abstract void deletePasswordFile();
}
