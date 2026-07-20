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
 * Portions copyright 2026 3A Systems, LLC.
 */

package com.sun.identity.authentication.share;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.security.Principal;
import java.util.Map;

import javax.security.auth.Subject;

import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.sun.identity.authentication.spi.DSAMECallbackInterface;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.xml.XMLUtils;

/**
 * Security regression tests for {@link AuthXMLUtils} covering GHSA-wg5r-wc3x-39vc:
 * <ul>
 *   <li>unauthenticated RCE via {@code Class.forName} in {@code createCustomCallback()};</li>
 *   <li>unrestricted Java deserialization in {@code getDeSerializedSubject()}.</li>
 * </ul>
 */
public class AuthXMLUtilsSecurityTest {

    private static final Debug DEBUG = Debug.getInstance("test");

    // ---- createCustomCallback() : arbitrary class instantiation (RCE) -------

    @Test
    public void createCustomCallbackRejectsNonCallbackClassWithoutInstantiating() {
        NonCallbackProbe.instantiated = false;
        Node node = customCallbackNode(NonCallbackProbe.class.getName());

        DSAMECallbackInterface result = AuthXMLUtils.createCustomCallback(node, null);

        assertThat(result).as("a class that is not a DSAMECallbackInterface must be rejected")
                .isNull();
        assertThat(NonCallbackProbe.instantiated)
                .as("the attacker-named class must never be instantiated").isFalse();
    }

    @Test
    public void createCustomCallbackInstantiatesLegitimateCallback() {
        Node node = customCallbackNode(LegitCallback.class.getName());

        DSAMECallbackInterface result = AuthXMLUtils.createCustomCallback(node, null);

        assertThat(result).isInstanceOf(LegitCallback.class);
    }

    private static Node customCallbackNode(String className) {
        Document doc = XMLUtils.toDOMDocument(
                "<CustomCallback className=\"" + className + "\"/>", DEBUG);
        return doc.getDocumentElement();
    }

    // ---- getDeSerializedSubject() : Subject deserialization allowlist -------

    @Test
    public void subjectFilterAllowsSubjectGraphWithPrincipal() throws Exception {
        Subject subject = new Subject();
        subject.getPrincipals().add(new TestPrincipal("alice"));

        Object restored = deserializeWithSubjectFilter(serialize(subject));

        assertThat(restored).isInstanceOf(Subject.class);
        assertThat(((Subject) restored).getPrincipals())
                .extracting(Principal::getName).contains("alice");
    }

    @Test
    public void subjectFilterRejectsNonAllowlistedClass() throws Exception {
        // EvilPayload is Serializable but is neither a Principal nor a JDK
        // collection/scalar, so the allowlist must reject it before readObject runs.
        EvilPayload.deserialized = false;

        Throwable thrown = catchThrowable(
                () -> deserializeWithSubjectFilter(serialize(new EvilPayload())));

        assertThat(thrown).isInstanceOf(InvalidClassException.class);
        assertThat(EvilPayload.deserialized)
                .as("a rejected class must not complete deserialization").isFalse();
    }

    private static byte[] serialize(Serializable object) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(object);
        }
        return bos.toByteArray();
    }

    private static Object deserializeWithSubjectFilter(byte[] bytes) throws Exception {
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
            ois.setObjectInputFilter(subjectDeserialisationFilter());
            return ois.readObject();
        }
    }

    /** Reaches the production filter so the test exercises the real allowlist. */
    private static ObjectInputFilter subjectDeserialisationFilter() throws Exception {
        Field field = AuthXMLUtils.class.getDeclaredField("SUBJECT_DESERIALISATION_FILTER");
        field.setAccessible(true);
        return (ObjectInputFilter) field.get(null);
    }

    // ---- fixtures -----------------------------------------------------------

    /** Stand-in for an attacker-chosen gadget class; not a DSAMECallbackInterface. */
    public static class NonCallbackProbe {
        static volatile boolean instantiated = false;

        public NonCallbackProbe() {
            instantiated = true;
        }
    }

    /** A legitimate custom callback, which must keep working. */
    public static class LegitCallback implements DSAMECallbackInterface {
        private Map config;

        @Override
        public void setConfig(Map configMap) {
            this.config = configMap;
        }

        @Override
        public Map getConfig() {
            return config;
        }
    }

    /** Custom Principal, as an auth module would contribute to the Subject. */
    public static class TestPrincipal implements Principal, Serializable {
        private static final long serialVersionUID = 1L;
        private final String name;

        public TestPrincipal(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }
    }

    /** Serializable non-Principal payload that must be denied by the filter. */
    public static class EvilPayload implements Serializable {
        private static final long serialVersionUID = 1L;
        static volatile boolean deserialized = false;

        private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
            in.defaultReadObject();
            deserialized = true;
        }
    }
}
