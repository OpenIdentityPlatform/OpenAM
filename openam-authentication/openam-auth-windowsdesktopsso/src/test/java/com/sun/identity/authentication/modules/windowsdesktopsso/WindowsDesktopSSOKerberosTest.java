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
 * Copyright 2026 3A Systems, LLC.
 */

package com.sun.identity.authentication.modules.windowsdesktopsso;

import com.iplanet.am.util.SystemProperties;
import com.sun.identity.authentication.service.AuthD;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.shared.debug.Debug;
import jakarta.servlet.http.HttpServletRequest;
import org.forgerock.guice.core.InjectorHolder;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.testng.PowerMockTestCase;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import javax.security.auth.Subject;
import java.io.File;
import java.io.IOException;
import java.security.PrivilegedActionException;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.expectThrows;

/**
 * Runs {@link WindowsDesktopSSO} against a real KDC (Apache Kerby) with tokens made by the JDK Kerberos client.
 * <p>
 * The module uses the krb5.conf that Kerby writes and points {@code java.security.krb5.conf} at:
 * {@link WindowsDesktopSSO#createUpdateKrb5ConfigFile()} is a no-op here, because the module splits its KDC
 * attribute on ':' and cannot express the random port Kerby listens on.
 */
@PrepareForTest({SystemProperties.class, AuthD.class, InjectorHolder.class})
// JAAS, JGSS, JCE and the JDK Krb5LoginModule must come from the system class loader: their copies in the
// PowerMock class loader fail with ClassCastException, IllegalAccessError, VerifyError or no AES cipher.
@PowerMockIgnore({"javax.security.*", "javax.crypto.*", "org.ietf.jgss.*", "com.sun.security.*",
        "jdk.internal.reflect.*"})
public class WindowsDesktopSSOKerberosTest extends PowerMockTestCase {

    private static final String REALM = "EXAMPLE.COM";
    private static final String SERVICE_PRINCIPAL = "HTTP/localhost@" + REALM;
    private static final String OTHER_SERVICE_PRINCIPAL = "HTTP/other@" + REALM;
    private static final String USER = "demo";
    private static final String USER_PRINCIPAL = USER + "@" + REALM;
    private static final int AUTH_LEVEL = 5;

    private static final String SPNEGO_MECH = "1.3.6.1.5.5.2";
    private static final String KERBEROS_MECH = "1.2.840.113554.1.2.2";
    // NTLM negotiate message, as Windows sends it when it has no Kerberos ticket.
    private static final String NTLM_TOKEN = "TlRMTVNTUAABAAAAB4IIogAAAAAAAAAAAAAAAAAAAAAKAGFKAAAADw==";

    private static final String ATTR_PREFIX = "iplanet-am-auth-windowsdesktopsso-";

    private final KerberosTestRealm realm = new KerberosTestRealm();
    private File serviceKeytab;

    @BeforeClass
    public void startKdc() throws Exception {
        realm.start(REALM, USER_PRINCIPAL, SERVICE_PRINCIPAL, OTHER_SERVICE_PRINCIPAL);
        serviceKeytab = realm.exportKeytab(SERVICE_PRINCIPAL);
    }

    @AfterClass(alwaysRun = true)
    public void stopKdc() throws Exception {
        realm.stop();
    }

    @BeforeMethod
    public void mockStatics() {
        // PowerMockTestCase clears the static mocks after every test method.
        PowerMockito.mockStatic(SystemProperties.class, InjectorHolder.class, AuthD.class);
        // Nothing configured: the krb5.conf mode of the module and the JDK Krb5LoginModule.
        PowerMockito.when(SystemProperties.get(anyString(), anyString()))
                .thenAnswer(invocation -> invocation.getArgument(1));
        PowerMockito.when(SystemProperties.getAsBoolean(anyString(), anyBoolean()))
                .thenAnswer(invocation -> invocation.getArgument(1));
    }

    @Test
    public void spnegoTokenLogsUserIn() throws Exception {
        TestModule module = newModule("spnego", negotiate(SERVICE_PRINCIPAL, SPNEGO_MECH), options());

        assertEquals(module.process(null, ISAuthConstants.LOGIN_START), ISAuthConstants.LOGIN_SUCCEED);
        assertEquals(module.getPrincipal().getName(), USER);
        assertEquals(module.moduleAuthLevel, AUTH_LEVEL);
    }

    @Test
    public void kerberosTokenLogsUserIn() throws Exception {
        TestModule module = newModule("kerberos", negotiate(SERVICE_PRINCIPAL, KERBEROS_MECH), options());

        assertEquals(module.process(null, ISAuthConstants.LOGIN_START), ISAuthConstants.LOGIN_SUCCEED);
        assertEquals(module.getPrincipal().getName(), USER);
    }

    @Test
    public void secondRequestUsesCachedServiceLogin() throws Exception {
        TestModule first = newModule("cached", negotiate(SERVICE_PRINCIPAL, SPNEGO_MECH), options());
        assertEquals(first.process(null, ISAuthConstants.LOGIN_START), ISAuthConstants.LOGIN_SUCCEED);
        assertEquals(first.serviceLogins, 1);

        // As every request after the first one to a module instance: the service subject comes from the cache.
        TestModule second = newModule("cached", negotiate(SERVICE_PRINCIPAL, SPNEGO_MECH), options());
        assertEquals(second.process(null, ISAuthConstants.LOGIN_START), ISAuthConstants.LOGIN_SUCCEED);
        assertEquals(second.serviceLogins, 0, "the second request should not log the service in again");
        assertEquals(second.getPrincipal().getName(), USER);
    }

    @DataProvider(name = "expiredCredentials")
    public Object[][] expiredCredentials() {
        // The PrivilegedActionException that Subject.doAs throws, and a bare GSSException.
        return new Object[][] {{"wrapped", true}, {"bare", false}};
    }

    @Test(dataProvider = "expiredCredentials")
    public void retryWithRenewedServiceLoginLogsUserIn(String shape, boolean wrapped) throws Exception {
        TestModule module = newModule("retry-" + shape, negotiate(SERVICE_PRINCIPAL, SPNEGO_MECH), options());
        module.credentials = new ExpiringServiceCredentials(wrapped);

        assertEquals(processAtServerDebugLevel(module), ISAuthConstants.LOGIN_SUCCEED);
        assertEquals(module.getPrincipal().getName(), USER);
        assertEquals(module.serviceLogins, 2, "the module should log the service in again after the expiry");
    }

    @Test(dataProvider = "expiredCredentials")
    public void renewedServiceLoginIsCached(String shape, boolean wrapped) throws Exception {
        // One test method for both requests: TestNG does not order methods, and they share the expiring credentials.
        ExpiringServiceCredentials credentials = new ExpiringServiceCredentials(wrapped);
        TestModule first = newModule("cache-" + shape, negotiate(SERVICE_PRINCIPAL, SPNEGO_MECH), options());
        first.credentials = credentials;
        assertEquals(processAtServerDebugLevel(first), ISAuthConstants.LOGIN_SUCCEED);
        assertEquals(first.serviceLogins, 2);
        Subject renewed = credentials.lastUsed();

        // The next request to the module instance: the renewed service subject comes from the cache.
        TestModule second = newModule("cache-" + shape, negotiate(SERVICE_PRINCIPAL, SPNEGO_MECH), options());
        second.credentials = credentials;
        assertEquals(processAtServerDebugLevel(second), ISAuthConstants.LOGIN_SUCCEED);
        assertEquals(second.serviceLogins, 0, "the second request should use the renewed service login");
        assertSame(credentials.lastUsed(), renewed);
    }

    @Test(dataProvider = "expiredCredentials")
    public void renewedServiceLoginIsCachedWhenRetriedTokenFails(String shape, boolean wrapped) throws Exception {
        ExpiringServiceCredentials credentials = new ExpiringServiceCredentials(wrapped);
        Map<String, Object> untrusted = options();
        untrusted.put(ATTR_PREFIX + "kerberos-realms-trusted", Collections.singleton("OTHER.COM"));
        TestModule first = newModule("cache-failed-" + shape, negotiate(SERVICE_PRINCIPAL, SPNEGO_MECH), untrusted);
        first.credentials = credentials;
        AuthLoginException e = expectThrows(AuthLoginException.class, () -> processAtServerDebugLevel(first));
        assertEquals(e.getErrorCode(), "untrustedToken");
        assertEquals(first.serviceLogins, 2);
        Subject renewed = credentials.lastUsed();

        // The retried check failed on the realm, after the renewed service login: the next request must reuse it.
        TestModule second = newModule("cache-failed-" + shape, negotiate(SERVICE_PRINCIPAL, SPNEGO_MECH), options());
        second.credentials = credentials;
        assertEquals(processAtServerDebugLevel(second), ISAuthConstants.LOGIN_SUCCEED);
        assertEquals(second.serviceLogins, 0, "a failed retry should leave the renewed service login cached");
        assertSame(credentials.lastUsed(), renewed);
    }

    @Test
    public void returnRealmKeepsRealmInUserName() throws Exception {
        Map<String, Object> options = options();
        options.put(ATTR_PREFIX + "returnRealm", Collections.singleton("true"));
        TestModule module = newModule("returnRealm", negotiate(SERVICE_PRINCIPAL, SPNEGO_MECH), options);

        assertEquals(module.process(null, ISAuthConstants.LOGIN_START), ISAuthConstants.LOGIN_SUCCEED);
        assertEquals(module.getPrincipal().getName(), USER_PRINCIPAL);
    }

    @Test
    public void tokenFromTrustedRealmLogsUserIn() throws Exception {
        Map<String, Object> options = options();
        options.put(ATTR_PREFIX + "kerberos-realms-trusted", Collections.singleton(REALM));
        TestModule module = newModule("trustedRealm", negotiate(SERVICE_PRINCIPAL, SPNEGO_MECH), options);

        assertEquals(module.process(null, ISAuthConstants.LOGIN_START), ISAuthConstants.LOGIN_SUCCEED);
        assertEquals(module.getPrincipal().getName(), USER);
    }

    @Test
    public void tokenFromUntrustedRealmIsRejected() throws Exception {
        Map<String, Object> options = options();
        options.put(ATTR_PREFIX + "kerberos-realms-trusted", Collections.singleton("OTHER.COM"));
        TestModule module = newModule("untrustedRealm", negotiate(SERVICE_PRINCIPAL, SPNEGO_MECH), options);

        AuthLoginException e = expectThrows(AuthLoginException.class,
                () -> module.process(null, ISAuthConstants.LOGIN_START));
        assertEquals(e.getErrorCode(), "untrustedToken");
        // The argument is the user principal from the token, not its realm.
        assertEquals(e.getMessage(), "Kerberos token for " + USER_PRINCIPAL + " is not from a trusted realm.");
        assertNull(module.getPrincipal());
    }

    @Test
    public void tokenForKnownUserLogsUserIn() throws Exception {
        Map<String, Object> options = options();
        options.put(ATTR_PREFIX + "lookupUserInRealm", Collections.singleton("true"));
        TestModule module = newModule("knownUser", negotiate(SERVICE_PRINCIPAL, SPNEGO_MECH), options);
        module.knownUser = USER;

        assertEquals(module.process(null, ISAuthConstants.LOGIN_START), ISAuthConstants.LOGIN_SUCCEED);
        assertEquals(module.getPrincipal().getName(), USER);
    }

    @Test
    public void tokenForUnknownUserIsRejected() throws Exception {
        Map<String, Object> options = options();
        options.put(ATTR_PREFIX + "lookupUserInRealm", Collections.singleton("true"));
        TestModule module = newModule("unknownUser", negotiate(SERVICE_PRINCIPAL, SPNEGO_MECH), options);

        AuthLoginException e = expectThrows(AuthLoginException.class,
                () -> module.process(null, ISAuthConstants.LOGIN_START));
        assertEquals(e.getErrorCode(), "notfound");
        assertEquals(e.getMessageArgs(), new Object[] {USER, "/"});
        assertNull(module.getPrincipal());
    }

    @Test
    public void unexpectedFailureInTokenCheckIsRejected() throws Exception {
        TestModule module = newModule("unexpectedFailure", negotiate(SERVICE_PRINCIPAL, SPNEGO_MECH), options());
        IOException failure = new IOException("token check failed");
        module.tokenCheckFailure = failure;

        AuthLoginException e = expectThrows(AuthLoginException.class,
                () -> module.process(null, ISAuthConstants.LOGIN_START));
        assertEquals(e.getErrorCode(), "auth");
        assertSame(e.getCause(), failure);
    }

    @Test
    public void tokenForAnotherServiceIsRejected() throws Exception {
        TestModule module = newModule("otherService", negotiate(OTHER_SERVICE_PRINCIPAL, SPNEGO_MECH), options());

        AuthLoginException e = expectThrows(AuthLoginException.class,
                () -> module.process(null, ISAuthConstants.LOGIN_START));
        assertEquals(e.getErrorCode(), "auth");
        assertNull(module.getPrincipal());
    }

    @Test
    public void ntlmTokenIsRejected() throws Exception {
        TestModule module = newModule("ntlm", "Negotiate " + NTLM_TOKEN, options());

        AuthLoginException e = expectThrows(AuthLoginException.class,
                () -> module.process(null, ISAuthConstants.LOGIN_START));
        assertEquals(e.getErrorCode(), "token");
        assertNull(module.getPrincipal());
    }

    /** Runs process() at the server's default debug level; unit tests otherwise run at "on", which hides #1173. */
    private static int processAtServerDebugLevel(TestModule module) throws Exception {
        Debug debug = Debug.getInstance("amAuthWindowsDesktopSSO");
        int level = debug.getState();
        debug.setDebug("error");
        try {
            assertFalse(debug.messageEnabled(), "the module must run without message-level debug");
            return module.process(null, ISAuthConstants.LOGIN_START);
        } finally {
            debug.setDebug(level);
        }
    }

    /** Module settings as stored in the auth module instance; each test changes what it is about. */
    private Map<String, Object> options() {
        Map<String, Object> options = new HashMap<>();
        options.put(ATTR_PREFIX + "principal-name", Collections.singleton(SERVICE_PRINCIPAL));
        options.put(ATTR_PREFIX + "keytab-file", Collections.singleton(serviceKeytab.getAbsolutePath()));
        options.put(ATTR_PREFIX + "kerberos-realm", Collections.singleton(REALM));
        // Required by the module, but not used: the KDC address comes from Kerby's krb5.conf.
        options.put(ATTR_PREFIX + "kdc", Collections.singleton("localhost"));
        options.put(ATTR_PREFIX + "auth-level", Collections.singleton(String.valueOf(AUTH_LEVEL)));
        return options;
    }

    /**
     * Creates the module for a request with the given Authorization header. Every test uses its own instance name,
     * because the module caches the service login per instance in a static table.
     */
    private TestModule newModule(String instanceName, String authorization, Map<String, Object> options) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("Authorization")).thenReturn(authorization);
        options.put(ISAuthConstants.MODULE_INSTANCE_NAME, instanceName);
        TestModule module = new TestModule(request);
        // The module does not use the subject.
        module.init(null, new HashMap<>(), options);
        return module;
    }

    /** The Authorization header of the user's first request to the service. */
    private String negotiate(String servicePrincipal, String mech) throws Exception {
        return "Negotiate " + Base64.getEncoder().encodeToString(realm.initialToken(servicePrincipal, mech));
    }

    /** The module with the request and realm that the authentication service would provide. */
    private static final class TestModule extends WindowsDesktopSSO {
        private final HttpServletRequest request;
        private int moduleAuthLevel;
        private int serviceLogins;
        // Null: the service credentials never expire.
        private ExpiringServiceCredentials credentials;
        // Null: the token check runs; otherwise it fails as Subject.doAs reports a checked exception.
        private Exception tokenCheckFailure;
        // Null: the identity store has no users; otherwise the only user that the search finds.
        private String knownUser;

        TestModule(HttpServletRequest request) {
            this.request = request;
        }

        @Override
        public HttpServletRequest getHttpServletRequest() {
            return request;
        }

        @Override
        public String getRequestOrg() {
            return "/";
        }

        @Override
        public boolean setAuthLevel(int authLevel) {
            moduleAuthLevel = authLevel;
            return true;
        }

        /** Called once per service login of the module in krb5.conf mode, the default. */
        @Override
        protected void createUpdateKrb5ConfigFile() {
            // Keep Kerby's krb5.conf: the module would rewrite "kdc = localhost:<port>" as "kdc=localhost", port 88.
            serviceLogins++;
        }

        /** No identity store in this test: finds only the known user and, as the module does, returns the value. */
        @Override
        String searchUserAccount(String attributeValue, String organization) {
            return attributeValue.equals(knownUser) ? attributeValue : null;
        }

        @Override
        void authenticateToken(Subject subject, byte[] kerberosToken, Set<String> trustedRealms)
                throws Exception {
            if (tokenCheckFailure != null) {
                throw new PrivilegedActionException(tokenCheckFailure);
            }
            if (credentials != null) {
                credentials.check(subject);
            }
            super.authenticateToken(subject, kerberosToken, trustedRealms);
        }
    }
}
