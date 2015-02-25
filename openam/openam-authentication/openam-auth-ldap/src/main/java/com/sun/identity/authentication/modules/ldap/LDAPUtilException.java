/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2005 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: LDAPUtilException.java,v 1.4 2009/01/28 05:34:53 ww203982 Exp $
 *
 */

/*
 * Portions Copyrighted 2011 ForgeRock AS
 */

package com.sun.identity.authentication.modules.ldap;

import com.sun.identity.shared.locale.AMResourceBundleCache;
import com.sun.identity.shared.locale.L10NMessage;
import java.text.MessageFormat;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutionException;
import org.forgerock.opendj.ldap.ResultCode;

/**
 * Exception that is thrown when the user  
 * fail the LDAP  authentication.
 */
public class LDAPUtilException extends ExecutionException implements L10NMessage {
    private String msgID;
    private String bundleName;
    private String message;
    private Object[] args;
    private ResultCode resultCode;

    private static String defaultBundleName = "amAuthLDAP";
    AMResourceBundleCache amCache = AMResourceBundleCache.getInstance();

    /** 
     * Constructor
     */ 
    public LDAPUtilException() {
    }

    /**
     * Constructor 
     * @param message Message of the exception
     */
    public LDAPUtilException(String message) {
        super(message);
    }

    /**
     * Constructor.
     *
     * @param message Message of the exception
     * @param errorCode The error code of LDAPException
     */
    protected LDAPUtilException(String message, ResultCode errorCode) {
        super(message);
        this.resultCode = errorCode;
    }

    /**
     * Constructor.
     *
     * @param msgID The key of the error message in message resource bundle
     * @param args arguments to the error message
     */
    public LDAPUtilException(String msgID, Object[] args) {
        this.msgID = msgID;
        this.args = args;
        this.bundleName = defaultBundleName;
        this.message = getL10NMessage(java.util.Locale.ENGLISH);
    }

    /**
     * Constructor.
     *
     * @param msgID The key of the error message in message resource bundle
     * @param errorCode The error code of LDAPException
     * @param args arguments to the error message identified by msgID
     */
    public LDAPUtilException(String msgID, ResultCode errorCode, Object[] args) {
        super(msgID);
        this.resultCode = errorCode;
        this.msgID  = msgID;
        this.args   = args;
        this.bundleName = defaultBundleName;
        this.message = getL10NMessage(java.util.Locale.ENGLISH);
    }

    /**
     * Constructor.
     *
     * @param rbName Resource bundle name of the error message
     * @param msgID Key of the error message in resource bundle
     * @param errorCode Error code of LDAPException
     * @param args Arguments to the error message  
     */
    public LDAPUtilException(String rbName, String msgID, ResultCode errorCode, 
        Object[] args) {
        super(msgID);
        this.resultCode = errorCode;
        this.msgID  = msgID;
        this.args   = args;
        this.bundleName = rbName;
        this.message = getL10NMessage(java.util.Locale.ENGLISH);
    }

    /**
     * Constructor.
     *
     * @param ex Root cause of this exception
     */
    public LDAPUtilException(Throwable ex) {
        this.message = ex.getMessage();
        if (ex instanceof L10NMessage) {
            L10NMessage lex = (L10NMessage) ex;        
            this.bundleName = lex.getResourceBundleName();
                  this.msgID = lex.getErrorCode();
            this.args = lex.getMessageArgs();
        }
    } 

    /**
     * Returns the localized message for specified locale of this exception.
     *
     * @param locale Locale in which the message is represented.
     * @return Localized message.
     */
    public String getL10NMessage(java.util.Locale locale) {
        if (msgID == null) {
            return getMessage();
         }

        String result = msgID;
        if (bundleName != null && locale != null) {
            ResourceBundle bundle = amCache.getResBundle (bundleName, locale);
            result = bundle.getString (msgID);
            if (args != null && args.length != 0) {
                result = MessageFormat.format(result,args);
            }
        }

        return result;
    }

    /**
     * Returns the English message for this exception.
     *
     * @return the English message for this exception.
     */
    @Override
    public String getMessage() {
        if (message != null) {
            return message;
        }
        
        return super.getMessage();
    }

    /**
     * Returns the arguments for the message string.
     *
     * @return the arguments for the message string.
     */
    public Object[] getMessageArgs() {
        return args;
    }

    /**
     * Returns the error code in string.
     *
     * @return the error code in string.
     */
    public String getErrorCode() {
        return msgID;
    }
 
    /**
     * Returns resource bundle name in string.
     * @return Resource bundle name in string.
     */
    public String getResourceBundleName() {
        return bundleName;
    }
    
    public ResultCode getResultCode () {
        return resultCode;
    }
}
 
