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
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright 2024-2026 3A-Systems LLC. All rights reserved.
 */

package org.openidentityplatform.openam.authentication.modules.webauthn;

import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.shared.debug.Debug;
import com.webauthn4j.authenticator.Authenticator;
import com.webauthn4j.authenticator.AuthenticatorImpl;
import com.webauthn4j.data.attestation.authenticator.AAGUID;
import com.webauthn4j.data.attestation.authenticator.AttestedCredentialData;
import com.webauthn4j.data.attestation.authenticator.RSACOSEKey;
import org.apache.commons.lang3.SerializationUtils;
import org.mockito.Mockito;
import org.powermock.reflect.Whitebox;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.Serializable;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

public class WebAuthnAuthenticationRceTest {

    WebAuthnAuthentication webAuthnAuthentication;
    AMIdentity identity;

    static final List<String> unsafeOperations =  new ArrayList<>();



    @BeforeMethod
    public void initMocks() {

        webAuthnAuthentication = mock(WebAuthnAuthentication.class, Mockito.CALLS_REAL_METHODS);
        when(webAuthnAuthentication.getSessionId()).thenReturn("87DCE7CF5F9DB00AC98367CA8640884F");
        webAuthnAuthentication.init(null, Collections.EMPTY_MAP, Collections.EMPTY_MAP);
        Whitebox.setInternalState(webAuthnAuthentication, "debug", Debug.getInstance("test"));
        identity = mock(AMIdentity.class);

        unsafeOperations.clear();
    }

    static class MaliciousPayload implements Serializable {
        private static final long serialVersionUID = 1L;
        private final String command;
        MaliciousPayload(String command) { this.command = command; }


        private Object readResolve() throws Exception {
            unsafeOperation();
            return this;
        }

        private void unsafeOperation() {
            unsafeOperations.add(command);
        }
    }

    @Test
    void legitimateAuthenticator_deserializesCorrectly() throws Exception {

        Authenticator auth = getAuthenticator();
        String encoded = marshalToBase64(auth);

        when(identity.getAttribute(anyString())).thenReturn(Collections.singleton(encoded));

        Set<Authenticator> result = webAuthnAuthentication.loadAuthenticators(identity);

        assertEquals(1, result.size());
        Authenticator restored = result.iterator().next();
        assertThat(restored).isInstanceOf(AuthenticatorImpl.class);
        assertEquals(1, restored.getCounter());

    }


    private static AuthenticatorImpl getAuthenticator() throws NoSuchAlgorithmException {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        KeyPair kp = kpg.generateKeyPair();

        AAGUID aaguid = new AAGUID(UUID.randomUUID());
        AttestedCredentialData attestedCredentialData =  new AttestedCredentialData(aaguid, new byte[]{}, RSACOSEKey.create(kp));
        return new AuthenticatorImpl(attestedCredentialData,  null, 1);
    }


    @Test
    void vulnerableImpl_executesGadgetChain() throws Exception {

        MaliciousPayload maliciousPayload = new MaliciousPayload("rce");
        String encoded = marshalToBase64(maliciousPayload);

        when(identity.getAttribute(anyString())).thenReturn(Collections.singleton(encoded));

        try {
            webAuthnAuthentication.loadAuthenticators(identity);
        } catch (AuthLoginException e) {
            assertThat(e).isInstanceOf(AuthLoginException.class);
        }

        assertThat(unsafeOperations).doesNotContain("rce");
    }

    private static String marshalToBase64(Object obj) {
        byte[] bytes = SerializationUtils.serialize((Serializable) obj);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
