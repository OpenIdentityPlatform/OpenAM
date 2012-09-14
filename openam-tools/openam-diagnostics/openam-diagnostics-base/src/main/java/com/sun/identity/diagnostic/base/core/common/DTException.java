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
 * $Id: DTException.java,v 1.1 2008/11/22 02:19:53 ak138937 Exp $
 *
 */

package com.sun.identity.diagnostic.base.core.common;

import java.text.MessageFormat;
import java.util.ResourceBundle;

import com.sun.identity.shared.locale.L10NMessage;

public class DTException extends RuntimeException implements L10NMessage {
    
    private String message;
    private String bundleName = ToolConstants.DEBUG_NAME;
    private String errorCode  ;
    private Object[] args	;
    private ResourceBundle bundle;
    
    /**
     * Exception for the tool.
     *
     * @param errorCode	 Key to resource bundle. 
     * @param args  arguments to message. If it is not present pass the
     *              as null
     * @param locale - User's preferred locale. 
     */
    public DTException(String errorCode, Object[] args,
        java.util.Locale locale) {   
        this.errorCode	= errorCode;
        this.args	= args;
        this.message	= getL10NMessage(locale);
    }
    
    /*
     * Constructs a <code>DTException</code> with a detailed message.
     *
     * @param message
     * Detailed message for this exception.
     */
    public DTException(String message) {
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
     * @param locale
     * @return localized error message.
     */
    public String getL10NMessage(java.util.Locale locale) {
        if (errorCode == null) {
            return getMessage();
        }
        String result = message;
        if (bundleName != null && locale != null) {
            bundle = ResourceBundle.getBundle(bundleName, locale);
            String mId	= bundle.getString(errorCode);
            if (args == null || args.length == 0) {
                result = mId;
            } else {
                result = MessageFormat.format(mId, args);
            }
        }
        return result;
    }
}
