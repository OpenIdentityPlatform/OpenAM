/*
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
 * $Id: WorkflowException.java,v 1.3 2008/06/25 05:50:03 qcheng Exp $
 *
 * Portions Copyrighted 2015 ForgeRock AS.
 */

package com.sun.identity.workflow;

import com.sun.identity.shared.locale.L10NMessage;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Workflow related exception.
 */
public class WorkflowException 
    extends Exception 
    implements L10NMessage {

    private String message;
    private String bundleName = "workflowMessages";
    private String errorCode;
    private Object[] args;
    private ResourceBundle bundle;

    /*
     * This constructor is used to pass the localized error message
     * At this level, the locale of the caller is not known and it is
     * not possible to throw localized error message at this level.
     * Instead this constructor provides Resource Bundle name and errorCode
     * for correctly locating the error messsage. The default getMessage()
     * will always return English messages only. This is in consistent with
     * current JRE
     * @param errorCode Key to resource bundle. You can use
     *        <code>String localizedStr = rb.getString(errorCode)</code>
     * @param args arguments to message. If it is not present pass the
     *        as null
     * be displayed according to user's preferred locale.
     */
    public WorkflowException(String errorCode, Object... args) {
        this.errorCode = errorCode;
        this.args = args;
        this.message = getL10NMessage(Locale.getDefault());
    }

    /*
     * Constructs a <code>WorkflowException</code> with a detailed message.
     *
     * @param message Exception Message.
     * Detailed message for this exception.
     */
    public WorkflowException(String message) {
        super(message);
    }

    /*
     * Returns resource bundle name associated with this error message.
     *
     * @return resource bundle name associated with this error message.
     */
    public String getResourceBundleName() {
        return bundleName;
    }

    /*
     * Returns error code associated with this error message.
     *
     * @return error code associated with this error message.
     */
    public String getErrorCode() {
        return errorCode;
    }

    /*
     * Returns arguments for formatting this error message.
     * You need to use MessageFormat class to format the message
     * It can be null.
     *
     * @return arguments for formatting this error message.
     */
     public Object[] getMessageArgs() {
        return args;
     }

     public String getMessage() {
         if (message != null) {
            // messgae is set only if l10n resource bundle is specified
            return message;
         }
         return super.getMessage();
     }

    /*
     * Returns localized error message.
     *
     * @param locale Locale of returned message.
     * @return localized error message.
     */
    public String getL10NMessage(java.util.Locale locale) {
        if (errorCode == null) {
            return getMessage();
        }
        String result = message;
        if (bundleName != null) {
            if (locale == null) {
                locale = Locale.getDefault();
            }

            bundle = ResourceBundle.getBundle(bundleName, locale);
            String mid = bundle.getString (errorCode);
            if ((args == null) || (args.length == 0)) {
                result = mid;
            } else {
                result = MessageFormat.format (mid,args);
            }
        }

        return result;
    }
}
