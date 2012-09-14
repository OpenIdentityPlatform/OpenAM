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
 * $Id: ToolGUIWriter.java,v 1.1 2008/11/22 02:19:54 ak138937 Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.diagnostic.base.core.log.impl;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashSet;
import java.util.ResourceBundle;
import java.util.Set;

import com.sun.identity.diagnostic.base.core.log.IToolOutput;
import com.sun.identity.diagnostic.base.core.service.ServiceResponse;
import com.sun.identity.diagnostic.base.core.ui.gui.event.MessageEvent;
import com.sun.identity.diagnostic.base.core.ui.gui.event.MessageListener;


/**
 * ToolGUIWriter gets the messages from services modules and
 * writes them to GUI objects to be displayed.
 */
public class ToolGUIWriter implements IToolOutput {
    
    private ServiceResponse sResponse = null;
    private ResourceBundle rb = null;
    private static Set<MessageListener> listeners =
        Collections.synchronizedSet(new HashSet<MessageListener>());
    
    ToolGUIWriter() {
        System.out.println("Invoking GUI");
    }
    
    /**
     * Initialize the output object.
     *
     * @param sResponse ServiceResponse object.
     * @param rbundle ResourceBundle object.
     */
    public void init(
        ServiceResponse sResponse,
        ResourceBundle rbundle
    ) {
        this.sResponse = sResponse;
        rb = rbundle;
    }
    
    /**
     * Initialize the output object.
     *
     * @param sResponse ServiceResponse object.
     */
    public void init(ServiceResponse sResponse) {
        this.sResponse = sResponse;
    }
    
    /**
     * Prints result.
     *
     * @param str Result string.
     */
    public void printResult(String str) {
        try {
            String message = ((rb == null) ? str : rb.getString(str));
            fireMessageEvent(new MessageEvent(
                MessageEvent.RESULT_MESSAGE, message + "\n"));
            sResponse.setResult(message);
        } catch(Exception ex) {
            fireMessageEvent(new MessageEvent(
                MessageEvent.RESULT_MESSAGE, str + "\n"));
            sResponse.setResult(str);
        }
    }
    
    /**
     * Prints result with new line.
     *
     * @param str Result string.
     */
    public void printlnResult(String str) {
        try {
            String message = ((rb == null) ? str : rb.getString(str)) + "\n";
            fireMessageEvent(new MessageEvent(
                MessageEvent.RESULT_MESSAGE, message + "\n"));
            sResponse.setResult(message);
        } catch (Exception ex) {
            fireMessageEvent(new MessageEvent(
                MessageEvent.RESULT_MESSAGE, str + "\n"));
            sResponse.setResult(str);
        }
    }
    
    /**
     * Prints Message.
     *
     * @param str Message string.
     */
    public void printMessage(String str) {
        try {
            String message = ((rb == null) ? str : rb.getString(str));
            fireMessageEvent(new MessageEvent(
                MessageEvent.INFO_MESSAGE, message + "\n"));
            sResponse.setMessage(message);
        } catch (Exception ex) {
            fireMessageEvent(new MessageEvent(
                MessageEvent.INFO_MESSAGE, str + "\n"));
            sResponse.setMessage(str);
        }
    }
    
    /**
     * Prints message.
     *
     * @param msgArray Message string array.
     */
    public void printMessage(String[] msgArray) {
        try {
            StringBuilder buff = new StringBuilder();
            buff.append(((rb == null) ?
                msgArray[0] : rb.getString(msgArray[0])))
                .append("................: ")
                .append(((rb == null) ?
                    msgArray[1] : rb.getString(msgArray[1])));
            fireMessageEvent(new MessageEvent(
                MessageEvent.INFO_MESSAGE, buff.toString() + "\n"));
            sResponse.setMessage(buff.toString());
        } catch (Exception ex) {
            StringBuilder buff = new StringBuilder();
            buff.append(msgArray[0])
                .append("................: ")
                .append(msgArray[1]);
            fireMessageEvent(new MessageEvent(
                MessageEvent.INFO_MESSAGE, buff.toString() + "\n"));
            sResponse.setMessage(buff.toString());
        }
    }
    
    /**
     * Prints message.
     *
     * @param str Message string.
     * @param params Message object parameters.
     */
    public void printMessage(String str, Object[] params) {
        try {
            String message = ((rb == null) ? str : rb.getString(str));
            fireMessageEvent(new MessageEvent(
                MessageEvent.INFO_MESSAGE, MessageFormat.format(
                    message, (Object[])params) + "\n"));
            sResponse.setMessage(MessageFormat.format(
                message, (Object[])params));
        } catch (Exception ex) {
            fireMessageEvent(new MessageEvent(
                MessageEvent.INFO_MESSAGE, MessageFormat.format(
                    str, (Object[])params) + "\n"));
            sResponse.setMessage(MessageFormat.format(
                str, (Object[])params));
        }
    }
    
    /**
     * Prints message with new line.
     *
     * @param str Message string.
     */
    public void printlnMessage(String str) {
        try {
            String message = ((rb == null) ? str : rb.getString(str)) + "\n";
            fireMessageEvent(new MessageEvent(
                MessageEvent.INFO_MESSAGE, message + "\n"));
            sResponse.setMessage(message);
        } catch (Exception ex) {
            fireMessageEvent(new MessageEvent(
                MessageEvent.INFO_MESSAGE, str + "\n"));
            sResponse.setMessage(str);
        }
    }
    
    /**
     * Prints error.
     *
     * @param str Error message string.
     * @param params Error message object params.
     */
    public void printError(String str, Object[] params) {
        try {
            String message = ((rb == null) ? str : rb.getString(str));
            fireMessageEvent(new MessageEvent(
                MessageEvent.ERROR_MESSAGE, MessageFormat.format(
                    message, (Object[])params) + "\n"));
            sResponse.setError(MessageFormat.format(
                message, (Object[])params));
        } catch (Exception ex) {
            fireMessageEvent(new MessageEvent(
                MessageEvent.ERROR_MESSAGE, MessageFormat.format(
                    str, (Object[])params) + "\n"));
            sResponse.setError(MessageFormat.format(
                str, (Object[])params));
        }
    }
    
    /**
     * Prints error.
     *
     * @param str Error message string.
     */
    public void printError(String str) {
        try {
            String message = ((rb == null) ? str : rb.getString(str));
            fireMessageEvent(new MessageEvent(
                MessageEvent.ERROR_MESSAGE, message + "\n"));
            sResponse.setError(message);
        } catch (Exception ex) {
            fireMessageEvent(new MessageEvent(
                MessageEvent.ERROR_MESSAGE, str + "\n"));
            sResponse.setError(str);
        }
    }
    
    /**
     * Prints error with new line.
     *
     * @param str Error message string.
     */
    public void printlnError(String str) {
        try {
            String message = ((rb == null) ? str : rb.getString(str)) + "\n";
            fireMessageEvent(new MessageEvent(
                MessageEvent.ERROR_MESSAGE, message + "\n"));
            sResponse.setError(message);
        } catch (Exception ex) {
            fireMessageEvent(new MessageEvent(
                MessageEvent.ERROR_MESSAGE, str + "\n"));
            sResponse.setError(str);
        }
    }
    
    /**
     * Prints Warning.
     *
     * @param str Warning message string.
     */
    public void printWarning(String str) {
        try {
            String message = ((rb == null) ? str : rb.getString(str));
            fireMessageEvent(new MessageEvent(
                MessageEvent.WARNING_MESSAGE, message + "\n"));
            sResponse.setWarning(message);
        } catch (Exception ex) {
            fireMessageEvent(new MessageEvent(
                MessageEvent.WARNING_MESSAGE, str + "\n"));
            sResponse.setWarning(str);
        }
    }
    
    /**
     * Prints warning.
     *
     * @param str Warning message string.
     * @param params Warning message object parameters.
     */
    public void printWarning(String str, Object[] params) {
        try {
            String message = ((rb == null) ? str : rb.getString(str));
            fireMessageEvent(new MessageEvent(
                MessageEvent.WARNING_MESSAGE, MessageFormat.format(
                    message, (Object[])params) + "\n"));
            sResponse.setWarning(MessageFormat.format(
                message, (Object[])params));
        } catch (Exception ex) {
            fireMessageEvent(new MessageEvent(
                MessageEvent.WARNING_MESSAGE, MessageFormat.format(
                    str, (Object[])params) + "\n"));
            sResponse.setWarning(MessageFormat.format(
                str, (Object[])params));
        }
    }
    
    /**
     * Prints warning with new line.
     *
     * @param str Warning message string.
     */
    public void printlnWarning(String str) {
        try {
            String message = ((rb == null) ? str : rb.getString(str)) + "\n";
            fireMessageEvent(new MessageEvent(
                MessageEvent.WARNING_MESSAGE, message + "\n"));
            sResponse.setWarning(message);
        } catch (Exception ex) {
            fireMessageEvent(new MessageEvent(
                MessageEvent.WARNING_MESSAGE, str + "\n"));
            sResponse.setWarning(str);
        }
    }
    
    /**
     * Add a message listener.
     *
     * @param ml Message listener to be added for a given message.
     */
    public void addMessageListener(MessageListener ml) {
        listeners.add(ml);
    }
    
    /**
     * Remove a message listener.
     *
     * @param ml Message listener to be removed for a given message.
     */
    public void removeMessageListener(MessageListener ml) {
        listeners.remove(ml);
    }
    
    private void fireMessageEvent(MessageEvent e) {
        synchronized (listeners) {
            for (MessageListener l : listeners) {
                l.messagePublished(e);
            }
        }
    }
    
    /**
     * Prints a status message.
     *
     * @param valid <code>true</code> if test succeeded false otherwise.
     * @param msg Message to be printed for test result
     */
    public void printStatusMsg(boolean valid, String msg) {
        if (valid) {
            this.printMessage(new String[] {msg, "msg-ok"});
        } else {
            this.printMessage(new String[] {msg, "msg-fail"});
        }
    }
}
