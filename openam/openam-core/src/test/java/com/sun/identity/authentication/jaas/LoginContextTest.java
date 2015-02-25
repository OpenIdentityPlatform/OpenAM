/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2013 ForgeRock Inc.
 */
package com.sun.identity.authentication.jaas;

import com.sun.identity.authentication.spi.InvalidPasswordException;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.AppConfigurationEntry.LoginModuleControlFlag;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Exercises the login context.
 *
 * @author andrew.forrest@forgerock.com
 */
public class LoginContextTest {

    private static final String LOGIN_MODULE = "com.sun.identity.authentication.jaas.LoginContextTest$MockModule";
    private static final String DELEGATE_MODULE = "delegate";

    private Subject subject;
    private CallbackHandler handler;
    private LoginContext context;

    private LoginModule requisiteDelegate;
    private LoginModule requiredDelegate;
    private LoginModule optionalDelegate;
    private LoginModule sufficientDelegate;

    private Map<LoginModule, Map<String, Object>> optionCache;

    /**
     * This test sets up four mock login modules, each with different control flags. The modules are created with
     * control flags in the following order: required, requisite, sufficient and optional.
     *
     * @throws LoginException
     *         Can be thrown by invocation of the authentication framework.
     */
    @BeforeMethod
    public void setUp() throws LoginException {
        optionCache = new HashMap<LoginModule, Map<String, Object>>();

        // Create required delegate login module.
        requiredDelegate = mock(LoginModule.class);
        Map<String, Object> requiredOptions = new HashMap<String, Object>();
        requiredOptions.put(DELEGATE_MODULE, requiredDelegate);
        optionCache.put(requiredDelegate, requiredOptions);
        AppConfigurationEntry requiredEntry = new AppConfigurationEntry(LOGIN_MODULE,
                LoginModuleControlFlag.REQUIRED, requiredOptions);

        // Create requisite delegate login module.
        requisiteDelegate = mock(LoginModule.class);
        Map<String, Object> requisiteOptions = new HashMap<String, Object>();
        requisiteOptions.put(DELEGATE_MODULE, requisiteDelegate);
        optionCache.put(requisiteDelegate, requisiteOptions);
        AppConfigurationEntry requisiteEntry = new AppConfigurationEntry(LOGIN_MODULE,
                LoginModuleControlFlag.REQUISITE, requisiteOptions);

        // Create sufficient delegate login module.
        sufficientDelegate = mock(LoginModule.class);
        Map<String, Object> sufficientOptions = new HashMap<String, Object>();
        sufficientOptions.put(DELEGATE_MODULE, sufficientDelegate);
        optionCache.put(sufficientDelegate, sufficientOptions);
        AppConfigurationEntry sufficientEntry = new AppConfigurationEntry(LOGIN_MODULE,
                LoginModuleControlFlag.SUFFICIENT, sufficientOptions);

        // Create optional delegate login module.
        optionalDelegate = mock(LoginModule.class);
        Map<String, Object> optionalOptions = new HashMap<String, Object>();
        optionalOptions.put(DELEGATE_MODULE, optionalDelegate);
        optionCache.put(optionalDelegate, optionalOptions);
        AppConfigurationEntry optionalEntry = new AppConfigurationEntry(LOGIN_MODULE,
                LoginModuleControlFlag.OPTIONAL, optionalOptions);

        AppConfigurationEntry[] entries =
                new AppConfigurationEntry[] {requiredEntry, requisiteEntry, sufficientEntry, optionalEntry};
        subject = new Subject();
        handler = mock(CallbackHandler.class);

        // Initialise class under test.
        context = new LoginContext(entries, subject, handler);
    }

    /**
     * When a sufficient module succeeds and no preceding required or requisite modules have failed, the authentication
     * chain terminates with a successful login, thereby ignoring any modules further in the chain.
     *
     * @throws LoginException
     *         Can be thrown by invocation of the authentication framework.
     */
    @Test
    public void sufficientSuccess() throws LoginException {
        whenLoginReturnTrue(requiredDelegate, requisiteDelegate, sufficientDelegate);
        whenCommitReturnTrue(requiredDelegate, requisiteDelegate, sufficientDelegate);

        context.login();

        verifyInitialize(requiredDelegate, requisiteDelegate, sufficientDelegate);
        verifyLogin(requiredDelegate, requisiteDelegate, sufficientDelegate);
        verifyCommit(requiredDelegate, requisiteDelegate, sufficientDelegate);
        verifyNoMoreInteractions(requiredDelegate, requisiteDelegate, sufficientDelegate, optionalDelegate);
    }

    /**
     * Sufficient module failures are only noted when required or requisite modules within the chain are ignored or
     * there are no required or requisite modules in the chain and no other module has succeeded in authentication.
     *
     * @throws LoginException
     *         Can be thrown by invocation of the authentication framework.
     */
    @Test
    public void sufficientFailureIgnored() throws LoginException {
        whenLoginReturnTrue(requiredDelegate, requisiteDelegate, optionalDelegate);
        whenLoginThrowInvalidPasswordException(sufficientDelegate);
        whenCommitReturnTrue(requiredDelegate, requisiteDelegate, optionalDelegate);
        whenCommitReturnFalse(sufficientDelegate);

        context.login();

        verifyInitialize(requiredDelegate, requisiteDelegate, sufficientDelegate, optionalDelegate);
        verifyLogin(requiredDelegate, requisiteDelegate, sufficientDelegate, optionalDelegate);
        verifyCommit(requiredDelegate, requisiteDelegate, sufficientDelegate, optionalDelegate);
        verifyNoMoreInteractions(requiredDelegate, requisiteDelegate, sufficientDelegate, optionalDelegate);
    }

    /**
     * An authentication failure in a required module is thrown when the authentication chain completes.
     *
     * @throws LoginException
     *         Can be thrown by invocation of the authentication framework.
     */
    @Test(expectedExceptions = InvalidPasswordException.class)
    public void requiredFailure() throws LoginException {
        whenLoginThrowInvalidPasswordException(requiredDelegate);
        whenLoginReturnTrue(requisiteDelegate, optionalDelegate);
        // Sufficient module ignored to stop the chain completing early.
        whenLoginReturnFalse(sufficientDelegate);
        whenAbortReturnTrue(requiredDelegate, requisiteDelegate, sufficientDelegate, optionalDelegate);

        try {
            context.login();
        } finally {
            verifyInitialize(requiredDelegate, requisiteDelegate, sufficientDelegate, optionalDelegate);
            verifyLogin(requiredDelegate, requisiteDelegate, sufficientDelegate, optionalDelegate);
            verifyAbort(requiredDelegate, requisiteDelegate, sufficientDelegate, optionalDelegate);
            verifyNoMoreInteractions(requiredDelegate, requisiteDelegate, sufficientDelegate, optionalDelegate);
        }
    }

    /**
     * An authentication failure in a requisite module is thrown immediately, causing the authentication chain to
     * terminate.
     *
     * @throws LoginException
     *         Can be thrown by invocation of the authentication framework.
     */
    @Test(expectedExceptions = InvalidPasswordException.class)
    public void requisiteFailure() throws LoginException {
        whenLoginReturnTrue(requiredDelegate);
        whenLoginThrowInvalidPasswordException(requisiteDelegate);
        whenAbortReturnTrue(requiredDelegate, requisiteDelegate, sufficientDelegate, optionalDelegate);

        try {
            context.login();
        } finally {
            verifyInitialize(requiredDelegate, requisiteDelegate, sufficientDelegate, optionalDelegate);
            verifyLogin(requiredDelegate, requisiteDelegate);
            verifyAbort(requiredDelegate, requisiteDelegate, sufficientDelegate, optionalDelegate);
            verifyNoMoreInteractions(requiredDelegate, requisiteDelegate, sufficientDelegate, optionalDelegate);
        }
    }

    /**
     * Like with sufficient, optional module failures are only noted when required or requisite modules within the chain
     * are ignored or there are no required or requisite modules in the chain and no other module has succeeded in
     * authentication.
     *
     * @throws LoginException
     *         Can be thrown by invocation of the authentication framework.
     */
    @Test
    public void optionalFailureIgnored() throws LoginException {
        whenLoginReturnTrue(requiredDelegate, requisiteDelegate);
        // Sufficient module ignored to stop the chain completing early.
        whenLoginReturnFalse(sufficientDelegate);
        whenLoginThrowInvalidPasswordException(optionalDelegate);
        whenCommitReturnTrue(requiredDelegate, requisiteDelegate);
        whenCommitReturnFalse(sufficientDelegate, optionalDelegate);

        context.login();

        verifyInitialize(requiredDelegate, requisiteDelegate, sufficientDelegate, optionalDelegate);
        verifyLogin(requiredDelegate, requisiteDelegate, sufficientDelegate, optionalDelegate);
        verifyCommit(requiredDelegate, requisiteDelegate, sufficientDelegate, optionalDelegate);
        verifyNoMoreInteractions(requiredDelegate, requisiteDelegate, sufficientDelegate, optionalDelegate);
    }

    /**
     * Optional module failures are only noted when required or requisite modules within the chain are ignored or there
     * are no required or requisite modules in the chain and no other module has succeeded in authentication.
     *
     * @throws LoginException
     *         Can be thrown by invocation of the authentication framework.
     */
    @Test(expectedExceptions = InvalidPasswordException.class)
    public void optionalFailureNoted() throws LoginException {
        whenLoginReturnFalse(requiredDelegate, requisiteDelegate, sufficientDelegate);
        whenLoginThrowInvalidPasswordException(optionalDelegate);
        whenAbortReturnTrue(requiredDelegate, requisiteDelegate, sufficientDelegate, optionalDelegate);

        try {
            context.login();
        } finally {
            verifyInitialize(requiredDelegate, requisiteDelegate, sufficientDelegate, optionalDelegate);
            verifyLogin(requiredDelegate, requisiteDelegate, sufficientDelegate, optionalDelegate);
            verifyAbort(requiredDelegate, requisiteDelegate, sufficientDelegate, optionalDelegate);
            verifyNoMoreInteractions(requiredDelegate, requisiteDelegate, sufficientDelegate, optionalDelegate);
        }
    }

    /**
     * Successful authentication in an optional module is only noted when required or requisite modules within the chain
     * are ignored or there are no required or requisite modules in the chain.
     *
     * @throws LoginException
     *         Can be thrown by invocation of the authentication framework.
     */
    @Test
    public void optionalSuccessNoted() throws LoginException {
        whenLoginReturnFalse(requiredDelegate, requisiteDelegate, sufficientDelegate);
        whenLoginReturnTrue(optionalDelegate);
        whenCommitReturnFalse(requiredDelegate, requisiteDelegate, sufficientDelegate);
        whenCommitReturnTrue(optionalDelegate);

        context.login();

        verifyInitialize(requiredDelegate, requisiteDelegate, sufficientDelegate, optionalDelegate);
        verifyLogin(requiredDelegate, requisiteDelegate, sufficientDelegate, optionalDelegate);
        verifyCommit(requiredDelegate, requisiteDelegate, sufficientDelegate, optionalDelegate);
        verifyNoMoreInteractions(requiredDelegate, requisiteDelegate, sufficientDelegate, optionalDelegate);
    }

    /**
     * Convenient method for setting login expectations.
     *
     * @param modules
     *         Modules for which the expectations are to be set.
     * @throws LoginException
     *         Can be thrown from module invocation.
     */
    private void whenLoginReturnTrue(LoginModule... modules) throws LoginException {
        for (LoginModule module : modules) {
            when(module.login()).thenReturn(true);
        }
    }

    /**
     * Convenient method for setting login expectations.
     *
     * @param modules
     *         Modules for which the expectations are to be set.
     * @throws LoginException
     *         Can be thrown from module invocation.
     */
    private void whenLoginThrowInvalidPasswordException(LoginModule... modules) throws LoginException {
        for (LoginModule module : modules) {
            when(module.login()).thenThrow(new InvalidPasswordException("test-pw-failure"));
        }
    }

    /**
     * Convenient method for setting login expectations.
     *
     * @param modules
     *         Modules for which the expectations are to be set.
     * @throws LoginException
     *         Can be thrown from module invocation.
     */
    private void whenLoginReturnFalse(LoginModule... modules) throws LoginException {
        for (LoginModule module : modules) {
            when(module.login()).thenReturn(false);
        }
    }

    /**
     * Convenient method for setting commit expectations.
     *
     * @param modules
     *         Modules for which the expectations are to be set.
     * @throws LoginException
     *         Can be thrown from module invocation.
     */
    private void whenCommitReturnTrue(LoginModule... modules) throws LoginException {
        for (LoginModule module : modules) {
            when(module.commit()).thenReturn(true);
        }
    }

    /**
     * Convenient method for setting commit expectations.
     *
     * @param modules
     *         Modules for which the expectations are to be set.
     * @throws LoginException
     *         Can be thrown from module invocation.
     */
    private void whenCommitReturnFalse(LoginModule... modules) throws LoginException {
        for (LoginModule module : modules) {
            when(module.commit()).thenReturn(false);
        }
    }

    /**
     * Convenient method for setting abort expectations.
     *
     * @param modules
     *         Modules for which the expectations are to be set.
     * @throws LoginException
     *         Can be thrown from module invocation.
     */
    private void whenAbortReturnTrue(LoginModule... modules) throws LoginException {
        for (LoginModule module : modules) {
            when(module.abort()).thenReturn(true);
        }
    }

    /**
     * Convenient method verifying invocation of the initialize method against the passed modules.
     *
     * @param modules
     *         Modules for which method invocations are to be verified.
     * @throws LoginException
     *         Can be thrown from module invocation.
     */
    private void verifyInitialize(LoginModule... modules) {
        for (LoginModule module : modules) {
            // Options use eq() as opposed to same() because the map is wrapped by the authn framework.
            verify(module).initialize(same(subject), same(handler), anyMap(), eq(optionCache.get(module)));
        }
    }

    /**
     * Convenient method verifying invocation of the login method against the passed modules.
     *
     * @param modules
     *         Modules for which method invocations are to be verified.
     * @throws LoginException
     *         Can be thrown from module invocation.
     */
    private void verifyLogin(LoginModule... modules) throws LoginException {
        for (LoginModule module : modules) {
            verify(module).login();
        }
    }

    /**
     * Convenient method verifying invocation of the commit method against the passed modules.
     *
     * @param modules
     *         Modules for which method invocations are to be verified.
     * @throws LoginException
     *         Can be thrown from module invocation.
     */
    private void verifyCommit(LoginModule... modules) throws LoginException {
        for (LoginModule module : modules) {
            verify(module).commit();
        }
    }

    /**
     * Convenient method verifying invocation of the abort method against the passed modules.
     *
     * @param modules
     *         Modules for which method invocations are to be verified.
     * @throws LoginException
     *         Can be thrown from module invocation.
     */
    private void verifyAbort(LoginModule... modules) throws LoginException {
        for (LoginModule module : modules) {
            verify(module).abort();
        }
    }

    /**
     * As the authn framework initialises login modules via reflection, this class allows for method calls to be
     * push out to a delegate, whereby the delegate is a mocked object that can have condition checking.
     */
    private static class MockModule implements LoginModule {

        private LoginModule delegate;

        public MockModule() {
            // No-arg constructor.
        }

        @Override
        public void initialize(Subject subject, CallbackHandler callbackHandler,
                               Map<String, ?> sharedState, Map<String, ?> options) {
            // Taking advantage of the options map to pass in the delegate module.
            delegate = (LoginModule)options.get(DELEGATE_MODULE);
            delegate.initialize(subject, callbackHandler, sharedState, options);
        }

        @Override
        public boolean login() throws LoginException {
            return delegate.login();
        }

        @Override
        public boolean commit() throws LoginException {
            return delegate.commit();
        }

        @Override
        public boolean abort() throws LoginException {
            return delegate.abort();
        }

        @Override
        public boolean logout() throws LoginException {
            return delegate.logout();
        }

    }

}
