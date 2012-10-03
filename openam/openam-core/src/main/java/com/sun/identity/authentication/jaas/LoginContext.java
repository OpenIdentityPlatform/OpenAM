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
 * Portions Copyrighted [2010-2011] [ForgeRock AS]
 */

package com.sun.identity.authentication.jaas;

import com.sun.identity.shared.debug.Debug;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import javax.security.auth.login.*;
import javax.security.auth.Subject;
import javax.security.auth.callback.*;

import com.sun.identity.authentication.spi.*;

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
    private static final String OTHER = "other";
    private static final String DEFAULT_HANDLER =
        "auth.login.defaultCallbackHandler";
    private Subject subject = null;
    private boolean subjectProvided = false;
    private boolean loginSucceeded = false;
    private CallbackHandler callbackHandler;
    private Map state = new HashMap();

    private Configuration config;
    private boolean configProvided = false;
    private ModuleInfo[] moduleStack;
    private static final Class[] PARAMS = { };

    LoginException passwordError = null;    // OPENAM-46

    LoginException firstError = null;
    LoginException firstRequiredError = null;
    boolean success = false;

    private static final Debug debug = Debug.getInstance("amJAAS");

    private void init(AppConfigurationEntry[] entries) throws LoginException {
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

    private void throwException(LoginException originalError, LoginException le)
        throws LoginException {
        throw ((originalError != null) ? originalError : le);
    }

    private void invoke(String methodName) throws LoginException {

        for (int i = 0; i < moduleStack.length; i++) {
            try {

                int mIndex = 0;
                Method[] methods = null;

                if (moduleStack[i].module != null) {
                    methods = moduleStack[i].module.getClass().getMethods();
                } else {

                    // instantiate the LoginModule
                    Class c = Class.forName(
                        moduleStack[i].entry.getLoginModuleName(), true,
                        Thread.currentThread().getContextClassLoader());

                    Constructor constructor = c.getConstructor(PARAMS);
                    Object[] args = { };

                    // allow any object to be a LoginModule
                    // as long as it conforms to the interface
                    moduleStack[i].module = constructor.newInstance(args);

                    methods = moduleStack[i].module.getClass().getMethods();

                    // call the LoginModule's initialize method
                    for (mIndex = 0; mIndex < methods.length; mIndex++) {
                        if (methods[mIndex].getName().equals(INIT_METHOD))
                            break;
                    }

                    Object[] initArgs = {subject,
                                        callbackHandler,
                                        state,
                                        moduleStack[i].entry.getOptions() };
                    // invoke the LoginModule initialize method
                    methods[mIndex].invoke(moduleStack[i].module, initArgs);
                }

                // find the requested method in the LoginModule
                for (mIndex = 0; mIndex < methods.length; mIndex++) {
                    if (methods[mIndex].getName().equals(methodName))
                        break;
                }

                // set up the arguments to be passed to the LoginModule method
                Object[] args = { };

                // invoke the LoginModule method
                boolean status = ((Boolean)methods[mIndex].invoke
                                (moduleStack[i].module, args)).booleanValue();

                if (status == true) {

                    // if SUFFICIENT, return if no prior REQUIRED errors
                    if (!methodName.equals(ABORT_METHOD) &&
                        !methodName.equals(LOGOUT_METHOD) &&
                        moduleStack[i].entry.getControlFlag() ==
                    AppConfigurationEntry.LoginModuleControlFlag.SUFFICIENT &&
                        firstRequiredError == null) {

                        if (debug.messageEnabled())
                            debug.message(methodName + " SUFFICIENT success");
                        return;
                    }

                    if (debug.messageEnabled())
                        debug.message(methodName + " success");
                    success = true;
                } else {
                    if (debug.messageEnabled())
                        debug.message(methodName + " ignored");
                }

            } catch (NoSuchMethodException nsme) {
                throw new LoginException(
                        "unable to instantiate LoginModule, module, because " +
                        "it does not provide a no-argument constructor:" +
                        moduleStack[i].entry.getLoginModuleName());
            } catch (InstantiationException ie) {
                throw new LoginException("unable to instantiate " +
                    "LoginModule: " +ie.getMessage());
            } catch (ClassNotFoundException cnfe) {
                throw new LoginException(
                        "unable to find LoginModule class: " +
                        cnfe.getMessage());
            } catch (IllegalAccessException iae) {
                throw new LoginException(
                        "unable to access LoginModule: " +
                        iae.getMessage());
            } catch (InvocationTargetException ite) {
                if (ite.getTargetException() instanceof java.lang.Error) {
                    if (debug.messageEnabled()){	
                        debug.message("LoginContext.invoke():" +
                        "Handling expected java.lang.Error");
                    }
                    throw (java.lang.Error)ite.getTargetException();
                }
                // failure cases
                LoginException le;

                if (ite.getTargetException() instanceof LoginException) {
                    le = (LoginException)ite.getTargetException();
                } else if (ite.getTargetException() instanceof
                    SecurityException) {
                    // do not want privacy leak
                    // (e.g., sensitive file path in exception msg)
                    le = new LoginException("Security Exception");
                    // le.initCause(new SecurityException());
                    if (debug.messageEnabled()) {
                        debug.message
                            ("original security exception with detail msg " +
                            "replaced by new exception with empty detail msg");
                        debug.message("original security exception: " +
                                ite.getTargetException().toString());
                    }
                } else {
                    // capture an unexpected LoginModule exception
                    java.io.StringWriter sw = new java.io.StringWriter();
                    ite.getTargetException().printStackTrace
                                                (new java.io.PrintWriter(sw));
                    sw.flush();
                    le = new LoginException(sw.toString());
                }

                // If we have a InvalidPasswordException save the exception,
                // so we can rethrow it for account lockout OPENAM-46
                if (le instanceof InvalidPasswordException) {
                    if (passwordError == null)
                        passwordError = le;
                }

                if (moduleStack[i].entry.getControlFlag() ==
                    AppConfigurationEntry.LoginModuleControlFlag.REQUISITE) {

                    if (debug.messageEnabled()) 
                        debug.message(methodName + " REQUISITE failure");

                    // if REQUISITE, then immediately throw an exception
                    if (methodName.equals(ABORT_METHOD) ||
                        methodName.equals(LOGOUT_METHOD)) {
                        if (firstRequiredError == null)
                            firstRequiredError = le;
                    } else if (passwordError != null) {
                        throwException(passwordError, le);
                    } else {
                        throwException(firstRequiredError, le);
                    }

                } else if (moduleStack[i].entry.getControlFlag() ==
                    AppConfigurationEntry.LoginModuleControlFlag.REQUIRED) {

                    if (debug.messageEnabled())  {
                        debug.message(methodName + " REQUIRED failure");
                        debug.message("Exception: " + le);
                    }

                    // mark down that a REQUIRED module failed
                    if (firstRequiredError == null) {
                        firstRequiredError = le;
                        debug.message("Set firstRequiredError to " + le);
                    }

                } else {

                    if (debug.messageEnabled()) 
                        debug.message(methodName + " OPTIONAL failure");

                    // mark down that an OPTIONAL module failed
                    if (firstError == null)
                        firstError = le;
                }
            }
        }

        // we went thru all the LoginModules.
        // If there was a password error,  throw as exception so account lockout
        // works as expected.  OPENAM-46
        
        if (passwordError != null) {
            throwException(passwordError, null);
        } else if (firstRequiredError != null) {
            // a REQUIRED module failed -- return the error
            throwException(firstRequiredError, null);
        } else if (success == false && firstError != null) {
            // no module succeeded -- return the first error
            throwException(firstError, null);
        } else if (success == false) {
            // no module succeeded -- all modules were IGNORED
            throwException(new LoginException(
                "Login Failure: all modules ignored"),
                null);
        } else {
            // success
            return;
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
