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
 * $Id: ISDebug.java,v 1.4 2008/06/25 05:43:40 qcheng Exp $
 *
 */


package com.sun.identity.log.spi;

import com.sun.identity.shared.debug.Debug;

/**
 * This class serves as the DSAME implementaion of DebugInterface
 * class
 */

public class ISDebug implements IDebug { 
    static Debug debugInst = null;
    static {
        debugInst = Debug.getInstance("amLog");
    }
    
    /**
     * The method which does the actual Debug. Pending work -
     * The string has to be internationalized here.
     * @param level The level of the Debug message.
     * @param msg The message string.
     * @param e The exception whose stacktrace is required.
     */
    public void debug(int level,String msg,Throwable e) {
        switch (level) {
            case 2:
                debugInst.error(msg,e);
                break;
            case 1:
                
                debugInst.warning(msg,e);
                break;
            default:
                debugInst.message(msg,e);
        }
    }
    
    /**
     * The method which does the actual Debug. Pending work -
     * The string has to be internationalized here.
     * @param level The level of the Debug message.
     * @param msg The message string.
     */
    public void debug(int level,String msg) {
        switch (level) {
            case 2:
                debugInst.error(msg);
                break;
            case 1:
                debugInst.warning(msg);
                break;
            default:
                debugInst.message(msg);
        }
    }
    
    /**
     * Return true if message mode is enabled.
     * @return true if message mode is enabled.
     */
    public boolean messageEnabled() {
        return debugInst.messageEnabled();
    }
    
    /**
     * Return true if warning mode is enabled.
     * @return true if warning mode is enabled.
     */
    public boolean warningEnabled() {
        return debugInst.warningEnabled();
    }
}
