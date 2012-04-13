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
 * $Id: IToolOutput.java,v 1.1 2008/11/22 02:19:53 ak138937 Exp $
 *
 */

package com.sun.identity.diagnostic.base.core.log;

import java.util.ResourceBundle;

import com.sun.identity.diagnostic.base.core.service.ServiceResponse;
import com.sun.identity.diagnostic.base.core.ui.gui.event.MessageListener;

/**
 * Interface defines methods for writing messages from tool engine.
 */
public interface IToolOutput {

    /**
     * Initialize the output object.
     *
     * @param sResponse ServiceResponse object.
     * @param rbundle ResourceBundle object.
     */
     public void init(ServiceResponse sResponse, ResourceBundle rbundle);

     /**
     * Initialize the output object.
     *
     * @param sResponse ServiceResponse object.
     */
     public void init(ServiceResponse sResponse);

    /**
     * Prints result.
     *
     * @param str Result string.
     */
    void printResult(String str);

    /**
     * Prints result with new line.
     *
     * @param str Result string.
     */
    void printlnResult(String str);

    /**
     * Prints message.
     *
     * @param str Message string.
     */
    void printMessage(String str);
    
    /**
     * Prints message.
     *
     * @param msgArray Message string array.
     */
    void printMessage(String[] msgArray);
    
    /**
     * Prints message.
     *
     * @param str Message string.
     * @param params Message object parameters.
     */
    void printMessage(String str, Object[] params);

    /**
     * Prints message with new line.
     *
     * @param str Message string.
     */
    void printlnMessage(String str);

    /**
     * Prints error.
     *
     * @param str Error message string.
     */
    void printError(String str);
    
    /**
     * Prints error.
     *
     * @param str Error message string.
     * @param arguments Error message object arguments.
     */
    void printError(String str, Object[] arguments);

    /**
     * Prints error with new line.
     *
     * @param str Error message string.
     */
    void printlnError(String str);

    /**
     * Prints warning.
     *
     * @param str Warning message string.
     */
    void printWarning(String str);

    /**
     * Prints warning.
     *
     * @param str Warning message string.
     * @param params Warning message object parameters.
     */
    void printWarning(String str, Object[] params);

    /**
     * Prints warning with new line.
     *
     * @param str Warning message string.
     */
    void printlnWarning(String str);

    /**
     * Add a message listener. 
     *
     * @param ml Message listener to be added for a given message.
     */
    public void addMessageListener(MessageListener ml);

    /**
     * Remove a message listener. 
     *
     * @param ml Message listener to be removed for a given message.
     */
    public void removeMessageListener(MessageListener ml);
    
    /**
     * Prints a status message. 
     *
     * @param valid <code>true</code> if test succeeded false otherwise.
     * @param msg Message to be printed for test result
     */
    public void printStatusMsg(boolean valid, String msg);
}
