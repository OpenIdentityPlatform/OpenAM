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
 * $Id: IDebugAccess.java,v 1.2 2008/06/25 05:51:36 qcheng Exp $
 *
 */

package com.sun.identity.agents.arch;

/**
 * This interface defines all the access APIs available for a given subsystem
 * to record debug messages.
 */
public interface IDebugAccess {
    
    /**
     * Convenience method to query if the <code>Debug</code> instance associated
     * with current <code>Module</code> will record <code>message</code> level
     * debug messages.
     * 
     * @return true if <code>message</code> level debug messages are allowed for
     * the <code>Debug</code> instance associated with this module, 
     * <code>false</code> otherwise.
     */    
     public boolean isLogMessageEnabled();

    /**
     * Convenience method to query if the <code>Debug</code> instance associated
     * with current <code>Module</code> will record <code>warning</code> level
     * debug messages.
     * 
     * @return true if <code>warning</code> level debug messages are allowed for
     * the <code>Debug</code> instance associated with this module, 
     * <code>false</code> otherwise.
     */    
     public boolean isLogWarningEnabled();

    /**
     * Convenience method to log a <code>message</code> level debug message in
     * the <code>Debug</code> instance associated with current
     * <code>Module</code>.
     * 
     * @param msg the message to be logged.
     */    
     public void logMessage(String msg);

    /**
     * Convenience method to log a <code>message</code> level debug message 
     * along with a given <code>Throwable</code> in the <code>Debug</code> 
     * instance associated with current <code>Module</code>.
     * 
     * @param msg the message to be logged.
     * @param th the <code>Throwable</code> to get the stack trace from.
     */   
     public void logMessage(String msg, Throwable th);

    /**
     * Convenience method to log a <code>warning</code> level debug message in
     * the <code>Debug</code> instance associated with current 
     * <code>Module</code>.
     * 
     * @param msg the message to be logged.
     */    
     public void logWarning(String msg);

    /**
     * Convenience method to log a <code>warning</code> level debug message 
     * along with a given <code>Throwable</code> in the <code>Debug</code> 
     * instance associated with current <code>Module</code>.
     * 
     * @param msg the message to be logged.
     * @param th the <code>Throwable</code> to get the stack trace from.
     */        
     public void logWarning(String msg, Throwable th);

    /**
     * Convenience method to log a <code>warning</code> level debug message in
     * the <code>Debug</code> instance associated with current 
     * <code>Module</code>.
     * 
     * @param msg the message to be logged.
     */        
     public void logError(String msg) ;

    /**
     * Convenience method to log a <code>error</code> level debug message along
     * with a given <code>Throwable</code> in the <code>Debug</code> instance
     * associated with current <code>Module</code>.
     * 
     * @param msg the message to be logged.
     * @param th the <code>Throwable</code> to get the stack trace from.
     */    
     public void logError(String msg, Throwable th);
}
