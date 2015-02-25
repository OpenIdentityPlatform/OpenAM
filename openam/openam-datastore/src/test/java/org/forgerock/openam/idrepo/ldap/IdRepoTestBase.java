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
 * Copyright 2013-2014 ForgeRock AS.
 */
package org.forgerock.openam.idrepo.ldap;

import com.iplanet.services.naming.WebtopNaming;
import com.sun.identity.idm.IdRepoBundle;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdRepoListener;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import org.forgerock.opendj.ldap.Connection;
import org.forgerock.opendj.ldap.ConnectionFactory;
import org.forgerock.opendj.ldap.Connections;
import org.forgerock.opendj.ldap.ErrorResultException;
import org.forgerock.opendj.ldap.FutureResult;
import org.forgerock.opendj.ldap.MemoryBackend;
import org.forgerock.opendj.ldap.RequestContext;
import org.forgerock.opendj.ldap.RequestHandler;
import org.forgerock.opendj.ldap.ResultHandler;
import org.forgerock.opendj.ldap.schema.Schema;
import org.forgerock.opendj.ldif.LDIFEntryReader;
import static org.mockito.Mockito.*;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.testng.PowerMockTestCase;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;

@PrepareForTest(value = {IdRepoListener.class, WebtopNaming.class})
public abstract class IdRepoTestBase extends PowerMockTestCase {

    protected static final String TEST1_GROUP = "test1";
    protected static final String TEST1_GROUP_DN = "cn=test1,ou=groups,dc=openam,dc=forgerock,dc=org";
    protected static final String TEST_USER1 = "testuser1";
    protected static final String DEMO = "demo";
    protected static final String USER0 = "user.0";
    protected static final String USER0_DN = "uid=user.0,ou=people,dc=openam,dc=forgerock,dc=org";
    protected static final String DEMO_DN = "uid=demo,ou=people,dc=openam,dc=forgerock,dc=org";
    protected RequestHandler<RequestContext> memoryBackend;
    protected IdRepoListener idRepoListener;
    protected DJLDAPv3Repo idrepo = new DJLDAPv3Repo() {
        @Override
        protected ConnectionFactory createConnectionFactory(String username, char[] password, int maxPoolSize) {
            return new FakeConnectionFactory();
        }

        @Override
        protected Schema getSchema() throws IdRepoException {
            return Schema.getCoreSchema().asStrictSchema();
        }
    };

    @BeforeClass
    public void setUpSuite() throws Exception {
        PowerMockito.mockStatic(WebtopNaming.class);
        idRepoListener = PowerMockito.mock(IdRepoListener.class);
        when(WebtopNaming.getAMServerID()).thenReturn("01");
        when(WebtopNaming.getSiteID(eq("01"))).thenReturn("02");
        memoryBackend = decorateBackend(new MemoryBackend(
                new LDIFEntryReader(getClass().getResourceAsStream(getLDIFPath()))));
    }

    protected RequestHandler<RequestContext> decorateBackend(MemoryBackend memoryBackend) {
        //default implementation doesn't decorate, just returns the same backend
        return memoryBackend;
    }

    @BeforeMethod
    public void resetMocks() {
        reset(idRepoListener);
    }

    @AfterClass
    public void tearDown() {
        idrepo.shutdown();
    }

    protected abstract String getLDIFPath();

    private class FakeConnectionFactory implements ConnectionFactory {

        private ConnectionFactory cf;

        public FakeConnectionFactory() {
            cf = Connections.newInternalConnectionFactory(memoryBackend);
        }

        public void close() {
            cf.close();
        }

        public FutureResult<Connection> getConnectionAsync(ResultHandler<? super Connection> handler) {
            return cf.getConnectionAsync(handler);
        }

        public Connection getConnection() throws ErrorResultException {
            return cf.getConnection();
        }
    }

    protected Callback[] getCredentials(String username, String password) {
        Callback[] credentials = new Callback[2];
        NameCallback nc = new NameCallback("dummy");
        nc.setName(username);
        PasswordCallback pc = new PasswordCallback("dummy", false);
        pc.setPassword(password.toCharArray());
        credentials[0] = nc;
        credentials[1] = pc;
        return credentials;
    }

    protected String getIdRepoExceptionMessage(String code, Object... args) {
        return new IdRepoException(IdRepoBundle.BUNDLE_NAME, code, args).getMessage();
    }
}
