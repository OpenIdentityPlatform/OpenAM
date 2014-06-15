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
 * $Id: LoginContext.java,v 1.6 2008/09/22 23:19:42 manish_rustagi Exp $
 *
 */

/*
 * Portions Copyrighted 2010-2013 ForgeRock Inc.
 */

package com.sun.identity.authentication.jaas;

import com.sun.identity.authentication.spi.InvalidPasswordException;
import com.sun.identity.shared.debug.Debug;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import javax.security.auth.login.*;
import javax.security.auth.Subject;
import javax.security.auth.callback.*;

import javax.security.auth.login.AppConfigurationEntry.LoginModuleControlFlag;

/**
 * This class is pulled out from JDK1.4.
 * Removed <code>doPrivileged()</code> on <code>invoke()</code> method so that
 * Error and Runtime exceptions can be passed back to the caller. Otherwise
 * any exception is converted to <code>LoginException</code>.
 */
public class LoginContext {

    private static final String INIT_METHOD = "initialize";
    private static final String LOGIN_METHOD = "login";
    private static final String COMMIT_METHOD = "commit";
    private static final String ABORT_METHOD = "abort";
    private static final String LOGOUT_METHOD = "logout";
    private static final Class[] PARAMS = { };

    private ExceptionHolder optionalExceptionHolder;
    private ExceptionHolder requiredExceptionHolder;

    private Subject subject = null;
    private boolean subjectProvided = false;
    private boolean loginSucceeded = false;
    private CallbackHandler callbackHandler;
    private Map state = new HashMap();
    private ModuleInfo[] moduleStack;
    boolean success = false;

    private static final Debug debug = Debug.getInstance("amJAAS");

    private void init(AppConfigurationEntry[] entries) throws LoginException {
        optionalExceptionHolder = new ExceptionHolder();
        requiredExceptionHolder = new ExceptionHolder();
        moduleStack = new ModuleInfo[entries.length];
        for (int i = 0; i < entries.length; i++) {
            // clone returned array
            moduleStack[i] = new ModuleInfo(new AppConfigurationEntry(
                entries[i].getLoginModuleName(),
                entries[i].getControlFlag(),
                entries[i].getOptions()),
                null);
        }
    }

    public LoginContext(AppConfigurationEntry[] entries, 
        CallbackHandler callbackHandler) throws LoginException {
        init(entries);
        if (callbackHandler == null)
            throw new LoginException("invalid null CallbackHandler provided");
        this.callbackHandler = callbackHandler;
    }

    public LoginContext(AppConfigurationEntry[] entries, Subject subject,
                        CallbackHandler callbackHandler) throws LoginException {
        init(entries);
        if (subject == null)
            throw new LoginException("invalid null Subject provided");
        this.subject = subject;
        subjectProvided = true;
        if (callbackHandler == null)
            throw new LoginException("invalid null CallbackHandler provided");
        this.callbackHandler = callbackHandler;
    }

    public void login() throws LoginException {

        loginSucceeded = false;

        if (subject == null) {
            subject = new Subject();
        }

        try {
            // module invoked in doPrivileged
            invoke(LOGIN_METHOD);
            invoke(COMMIT_METHOD);
            loginSucceeded = true;
        } catch (LoginException le) {
            try {
                invoke(ABORT_METHOD);
            } catch (LoginException le2) {
                throw le;
            }
            throw le;
        }
    }

    public void logout() throws LoginException {
        if (subject == null) {
            throw new LoginException("null subject - logout called " +
                "before login");
        }

        // module invoked in doPrivileged
        invoke(LOGOUT_METHOD);
    }

    public Subject getSubject() {
        if (!loginSucceeded && !subjectProvided)
            return null;
        return subject;
    }

    /**
     * Attempts to invoke the method described by methodName against each module within the stack.
     *
     * @param methodName
     *         String method name to be invoked on each module.
     * @throws LoginException
     *         Throw in the case of some login failure.
     */
    private void invoke(String methodName) throws LoginException {

        for (int i = 0; i < moduleStack.length; i++) {

            ModuleInfo info = moduleStack[i];
            LoginModuleControlFlag controlFlag = info.entry.getControlFlag();

            try {

                int mIndex = 0;
                Method[] methods = null;

                if (info.module != null) {
                    methods = info.module.getClass().getMethods();
                } else {

                    // instantiate the LoginModule
                    Class c = Class.forName(info.entry.getLoginModuleName(), true,
                            Thread.currentThread().getContextClassLoader());

                    Constructor constructor = c.getConstructor(PARAMS);
                    Object[] args = {};

                    // allow any object to be a LoginModule
                    // as long as it conforms to the interface
                    info.module = constructor.newInstance(args);

                    methods = info.module.getClass().getMethods();

                    // call the LoginModule's initialize method
                    for (mIndex = 0; mIndex < methods.length; mIndex++) {
                        if (methods[mIndex].getName().equals(INIT_METHOD))
                            break;
                    }

                    // Invoke the LoginModule initialize method
                    Object[] initArgs = {subject, callbackHandler, state, info.entry.getOptions()};
                    methods[mIndex].invoke(info.module, initArgs);
                }

                // find the requested method in the LoginModule
                for (mIndex = 0; mIndex < methods.length; mIndex++) {
                    if (methods[mIndex].getName().equals(methodName))
                        break;
                }

                // set up the arguments to be passed to the LoginModule method
                Object[] args = {};

                // invoke the LoginModule method
                boolean status = (Boolean)methods[mIndex].invoke(info.module, args);

                if (status) {

                    // if SUFFICIENT, return if no prior REQUIRED errors
                    if (!requiredExceptionHolder.hasException() && controlFlag == LoginModuleControlFlag.SUFFICIENT &&
                            (methodName.equals(LOGIN_METHOD) || methodName.equals(COMMIT_METHOD))) {

                        if (debug.messageEnabled()) {
                            debug.message(methodName + " SUFFICIENT success");
                        }

                        return;
                    }

                    if (debug.messageEnabled()) {
                        debug.message(methodName + " success");
                    }

                    success = true;
                } else {
                    if (debug.messageEnabled()) {
                        debug.message(methodName + " ignored");
                    }
                }

            } catch (NoSuchMethodException nsme) {
                throw new LoginException("unable to instantiate LoginModule, module, because it does " +
                        "not provide a no-argument constructor:" + info.entry.getLoginModuleName());
            } catch (InstantiationException ie) {
                throw new LoginException("unable to instantiate LoginModule: " + ie.getMessage());
            } catch (ClassNotFoundException cnfe) {
                throw new LoginException("unable to find LoginModule class: " + cnfe.getMessage());
            } catch (IllegalAccessException iae) {
                throw new LoginException("unable to access LoginModule: " + iae.getMessage());
            } catch (InvocationTargetException ite) {

                if (ite.getTargetException() instanceof Error) {
                    if (debug.messageEnabled()) {
                        debug.message("LoginContext.invoke(): Handling expected java.lang.Error");
                    }
                    throw (Error)ite.getTargetException();
                }

                // failure cases
                LoginException le = null;

                if (ite.getTargetException() instanceof LoginException) {
                    le = (LoginException)ite.getTargetException();
                } else if (ite.getTargetException() instanceof SecurityException) {
                    // do not want privacy leak
                    // (e.g., sensitive file path in exception msg)
                    le = new LoginException("Security Exception");
                    // le.initCause(new SecurityException());
                    if (debug.messageEnabled()) {
                        debug.message("original security exception with detail msg " +
                                "replaced by new exception with empty detail msg");
                        debug.message("original security exception: " + ite.getTargetException().toString());
                    }
                } else {
                    // capture an unexpected LoginModule exception
                    StringWriter sw = new StringWriter();
                    ite.getTargetException().printStackTrace(new PrintWriter(sw));
                    sw.flush();
                    le = new LoginException(sw.toString());
                }

                if (debug.messageEnabled()) {
                    debug.message(String.format("Method %s %s failure.", methodName, controlFlag));
                }

                if (controlFlag == LoginModuleControlFlag.OPTIONAL || controlFlag == LoginModuleControlFlag.SUFFICIENT) {
                    // mark down that an OPTIONAL module failed
                    optionalExceptionHolder.setException(le);
                } else {
                    requiredExceptionHolder.setException(le);

                    if (controlFlag == LoginModuleControlFlag.REQUISITE &&
                            (methodName.equals(LOGIN_METHOD) || methodName.equals(COMMIT_METHOD))) {
                        // if REQUISITE, then immediately throw an exception
                        throw requiredExceptionHolder.getException();
                    }
                }
            }
        }

        if (requiredExceptionHolder.hasException()) {
            // a REQUIRED module failed -- return the error
            throw requiredExceptionHolder.getException();
        } else if (success == false && optionalExceptionHolder.hasException()) {
            // no module succeeded -- return the first optional error
            throw optionalExceptionHolder.getException();
        } else if (success == false) {
            // no module succeeded -- all modules were IGNORED
            throw new LoginException("Login Failure: all modules ignored");
        }
    }

    // Exception holder class. Prompts InvalidPasswordExceptions above other LoginException types.
    private static class ExceptionHolder {

        private LoginException exception;

        /**
         * The captured exception.
         *
         * @param exception
         *         Captured exception.
         */
        public void setException(LoginException exception) {
            if (this.exception == null ||
                    (!(this.exception instanceof InvalidPasswordException) &&
                            exception instanceof InvalidPasswordException)) {
                this.exception = exception;
            }
        }

        /**
         * @return The captured exception.
         */
        public LoginException getException() {
            return exception;
        }

        /**
         * @return Whether a valid exception has been captured.
         */
        public boolean hasException() {
            return exception != null;
        }

    }

    /**
     * LoginModule information -
     *      incapsulates Configuration info and actual module instances.
     */
    private static class ModuleInfo {
        AppConfigurationEntry entry;
        Object module;

        ModuleInfo(AppConfigurationEntry newEntry, Object newModule) {
            this.entry = newEntry;
            this.module = newModule;
        }
    }
}
