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
 * Copyright 2013-2015 ForgeRock AS.
 */
package org.forgerock.openam.cts.utils.blob.strategies;

import com.sun.identity.shared.debug.Debug;
import org.forgerock.openam.cts.utils.blob.TokenStrategyFailedException;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Arrays;

import static org.mockito.BDDMockito.mock;

import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.fail;

public class EncryptionStrategyTest {

    private EncryptionStrategy strategy;

    @BeforeMethod
    public void setup() {
        strategy = new EncryptionStrategy(mock(Debug.class));
    }

    @Test (expectedExceptions = IllegalArgumentException.class)
    public void shouldPreventNullBlobOnPerform() throws TokenStrategyFailedException {
        strategy.perform(null);
    }

    @Test (expectedExceptions = IllegalArgumentException.class)
    public void shouldPreventNullBlobOnReverse() throws TokenStrategyFailedException {
        strategy.reverse(null);
    }

    @Test
    public void shouldBeThreadSafe() throws InterruptedException, TokenStrategyFailedException {

        final EncryptionStrategy encryptionStrategy = new EncryptionStrategy(mock(Debug.class));

        // We create two tokens and we encrypt them, in a non concurrent context.
        final String tokenID1 = "cc402b5b-d532-4b70-8ea8-90ca668c6f8d";
        final byte[] data1 = ("{\"expireTime\":[\"1441282506944\"],\"tokenName\":[\"access_token\"]," +
                "\"scope\":[\"openid\"," +
                "\"profile\"],\"grant_type\":[\"authorization_code\"],\"clientID\":[\"myOAuth2Client\"]," +
                "\"parent\":[\"453d68bb-6799-49e9-93be-5796698f0f21\"]," +
                "\"refreshToken\":[\"0377c0df-e5ee-418d-ad0c-b0a69115949e\"]," +
                "\"id\":[\"" + tokenID1 + "\"],\"tokenType\":[\"Bearer\"],\"realm\":[\"/\"]," +
                "\"redirectURI\":[\"http://openam.example.com:28080/openid/cb-basic.html\"],\"nonce\":[]," +
                "\"userName\":[\"amadmin\"]}").getBytes();
        final byte[] data1Encrypted = encryptionStrategy.perform(data1);


        final String tokenID2 = "b487f234-dac4-41a8-9782-f1f4bb30f02b";
        final byte[] data2 = ("{\"expireTime\":[\"1441282506944\"],\"tokenName\":[\"access_token\"]," +
                "\"scope\":[\"openid\"," +
                "\"profile\"],\"grant_type\":[\"authorization_code\"],\"clientID\":[\"myOAuth2Client\"]," +
                "\"parent\":[\"453d68bb-6799-49e9-93be-5796698f0f21\"]," +
                "\"refreshToken\":[\"0377c0df-e5ee-418d-ad0c-b0a69115949e\"]," +
                "\"id\":[\"" + tokenID2 + "\"],\"tokenType\":[\"Bearer\"],\"realm\":[\"/\"]," +
                "\"redirectURI\":[\"http://openam.example.com:28080/openid/cb-basic.html\"],\"nonce\":[]," +
                "\"userName\":[\"amadmin\"]}").getBytes();
        final byte[] data2Encrypted = encryptionStrategy.perform(data2);

        // This test is just a safeguard: should never failed.
        assertNotEquals(data1Encrypted, data2Encrypted, "Data 1 and Data2 encryption are equal");

        final IssueDetected issueDetected = new IssueDetected();

        Thread thread1 = new Thread(new CheckEncryptionStrategy(encryptionStrategy, issueDetected, data1Encrypted, data1));
        Thread thread2 = new Thread(new CheckEncryptionStrategy(encryptionStrategy, issueDetected, data2Encrypted, data2));

        thread1.setName("Data 1 Thread");
        thread2.setName("Data 2 Thread");

        thread1.start();
        thread2.start();

        thread1.join();
        thread2.join();

        if (issueDetected.error) {
            fail(issueDetected.message);
        }
    }

    public static class CheckEncryptionStrategy implements Runnable {

        private IssueDetected issueDetected;
        private EncryptionStrategy encryptionStrategy;
        private byte[] dataEncrypted;
        private byte[] data;

        public CheckEncryptionStrategy(EncryptionStrategy encryptionStrategy, IssueDetected issueDetected, byte[]
                dataEncrypted, byte[] data) {

            this.encryptionStrategy = encryptionStrategy;
            this.issueDetected = issueDetected;
            this.dataEncrypted = dataEncrypted;
            this.data = data;
        }

        @Override
        public void run() {
            try {
                for (int i = 0; i < 100 && !issueDetected.error; i++) {
                    //test encryption
                    byte[] dataEncryptedResult = encryptionStrategy.perform(data);
                    if (!Arrays.equals(dataEncrypted, dataEncryptedResult)) {
                        issueDetected.setError(Thread.currentThread().getName()
                                + " ->Iteration '" + i + "': Encryption failed");
                    }

                    //test decryption
                    byte[] dataDecryptedResult = encryptionStrategy.reverse(dataEncrypted);
                    if (!Arrays.equals(data, dataDecryptedResult)) {
                        issueDetected.setError(Thread.currentThread().getName()
                                + " ->Iteration '" + i + "': Decryption failed");
                    }
                }
            } catch (TokenStrategyFailedException e) {
                issueDetected.setError(e.getMessage());
                e.printStackTrace();
            }
        }
    }

    public static class IssueDetected {

        public boolean error = false;
        public String message;

        public synchronized void setError(String message) {
            this.message = message;
            this.error = true;
        }
    }
}