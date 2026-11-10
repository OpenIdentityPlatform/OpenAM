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
package com.sun.identity.jaxrpc.golden;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import com.sun.identity.shared.jaxrpc.golden.GoldenCase;
import com.sun.identity.shared.jaxrpc.golden.GoldenCases;
import com.sun.identity.shared.jaxrpc.golden.GoldenValues;
import com.sun.xml.rpc.server.TieBase;
import com.sun.xml.rpc.soap.message.SOAPMessageContext;
import jakarta.xml.soap.MimeHeaders;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.TreeMap;
import java.util.stream.Stream;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Records the JAX-RPC golden files by driving the generated {@code *_Tie} classes of the
 * RPC/encoded endpoints directly. It runs only when {@code -Djaxrpc.golden.root=<repo root>}
 * is set, so a normal build never rewrites the recordings.
 */
public class GoldenRecorderTest {

    private static final String ROOT_PROPERTY = "jaxrpc.golden.root";

    private static final String CORE_RESOURCES = "openam-core/src/test/resources/";
    private static final String FEDERATION_RESOURCES =
            "openam-federation/openam-federation-library/src/test/resources/";

    private static final String[] CORE_INTERFACES = {
        "com.sun.identity.sm.jaxrpc.SMSObjectIF",
        "com.iplanet.am.sdk.remote.DirectoryManagerIF",
        "com.sun.identity.entity.EntityObjectIF",
    };
    private static final String[] FEDERATION_INTERFACES = {
        "com.sun.identity.saml.AssertionManagerIF",
        "com.sun.identity.federation.services.FSAssertionManagerIF",
        "com.sun.identity.liberty.ws.security.SecurityTokenManagerIF",
        "com.sun.liberty.jaxrpc.LibertyManagerIF",
    };

    private static final String CHECK_FOR_LOCAL_RESPONSE = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<env:Envelope xmlns:env=\"http://schemas.xmlsoap.org/soap/envelope/\""
            + " xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\""
            + " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""
            + " xmlns:enc=\"http://schemas.xmlsoap.org/soap/encoding/\""
            + " xmlns:ns0=\"http://isp.com/types\""
            + " xmlns:ns1=\"http://java.sun.com/jax-rpc-ri/internal\""
            + " env:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">"
            + "<env:Body><ans1:checkForLocalResponse xmlns:ans1=\"http://isp.com/wsdl\"/></env:Body>"
            + "</env:Envelope>";

    private Locale savedLocale;

    @BeforeClass(alwaysRun = true)
    public void pinLocale() {
        savedLocale = Locale.getDefault();
        Locale.setDefault(Locale.US);
        ResourceBundle.clearCache();
    }

    @AfterClass(alwaysRun = true)
    public void restoreLocale() {
        Locale.setDefault(savedLocale);
        ResourceBundle.clearCache();
    }

    @Test
    public void recordsTheRIResponses() throws Exception {
        String root = System.getProperty(ROOT_PROPERTY);
        if (root == null || root.isEmpty() || root.startsWith("${")) {
            throw new SkipException("set -D" + ROOT_PROPERTY + "=<repository root> to record the golden files");
        }
        Path coreDir = Paths.get(root, CORE_RESOURCES, GoldenCases.RESOURCE_ROOT);
        Path federationDir = Paths.get(root, FEDERATION_RESOURCES, GoldenCases.RESOURCE_ROOT);

        List<GoldenCase> all = new ArrayList<>();
        Map<GoldenCase, Recording> recordings = new HashMap<>();
        for (String name : CORE_INTERFACES) {
            Class<?> iface = Class.forName(name);
            List<GoldenCase> cases = CoreGoldenCases.forInterface(iface);
            recordings.putAll(record(coreDir.resolve(iface.getSimpleName()), cases));
            all.addAll(cases);
        }
        for (String name : FEDERATION_INTERFACES) {
            Class<?> iface = Class.forName(name);
            List<GoldenCase> cases = GoldenCases.forInterface(iface);
            recordings.putAll(record(federationDir.resolve(iface.getSimpleName()), cases));
            all.addAll(cases);
        }

        assertThat(new String(Files.readAllBytes(
                coreDir.resolve("SMSObjectIF").resolve("checkForLocal__ok.response.xml")), UTF_8))
                .isEqualTo(CHECK_FOR_LOCAL_RESPONSE);

        List<String> degenerated = new ArrayList<>();
        for (GoldenCase c : all) {
            Class<?> declared = declaredTypeOf(c);
            if (declared == null) {
                continue;
            }
            Recording r = recordings.get(c);
            String expected = "<faultstring>" + declared.getName() + "</faultstring>";
            if (r.status != 500 || !new String(r.response, UTF_8).contains(expected)) {
                degenerated.add(c + " (status " + r.status + ", expected " + expected + ")");
            }
        }
        assertThat(degenerated).as("declared faults that did not come back as declared faults").isEmpty();
    }

    /**
     * Records the cases of one interface into dir, replacing what was recorded there before.
     */
    private static Map<GoldenCase, Recording> record(Path dir, List<GoldenCase> cases) throws Exception {
        Files.createDirectories(dir);
        try (Stream<Path> old = Files.list(dir)) {
            for (Path p : (Iterable<Path>) old::iterator) {
                if (Files.isRegularFile(p)) {
                    Files.delete(p);
                }
            }
        }
        Map<GoldenCase, Recording> recordings = new HashMap<>();
        Map<String, Integer> statuses = new TreeMap<>();
        for (GoldenCase c : cases) {
            assertThat(statuses).as("duplicate id " + c).doesNotContainKey(c.id());
            byte[] request = c.request().getBytes(UTF_8);
            List<Object[]> received = new ArrayList<>();
            Recording r = drive(c, request, received);
            Files.write(dir.resolve(c.id() + ".request.xml"), request);
            Files.write(dir.resolve(c.id() + ".response.xml"), r.response);
            if (!received.isEmpty()) {
                assertThat(received).as("calls of " + c).hasSize(1);
                Files.write(dir.resolve(c.id() + ".args"), GoldenValues.describe(received.get(0)).getBytes(UTF_8));
            }
            statuses.put(c.id(), r.status);
            recordings.put(c, r);
        }
        StringBuilder status = new StringBuilder();
        for (Map.Entry<String, Integer> e : statuses.entrySet()) {
            status.append(e.getKey()).append('=').append(e.getValue()).append('\n');
        }
        Files.write(dir.resolve("status.properties"), status.toString().getBytes(UTF_8));
        return recordings;
    }

    private static Recording drive(GoldenCase c, byte[] request, List<Object[]> received) throws Exception {
        SOAPMessageContext ctx = new SOAPMessageContext();
        MimeHeaders headers = new MimeHeaders();
        headers.addHeader("Content-Type", "text/xml; charset=\"utf-8\"");
        ctx.setMessage(ctx.createMessage(headers, new ByteArrayInputStream(request)));
        TieBase tie = (TieBase) Class.forName(c.iface().getName() + "_Tie").getConstructor().newInstance();
        tie.setTarget((Remote) GoldenCases.target(c, received));
        tie.handle(ctx);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ctx.getMessage().writeTo(out);
        return new Recording(out.toByteArray(), ctx.isFailure() ? 500 : 200);
    }

    /**
     * @return the first type the method of c declares, other than RemoteException, that c.thrown()
     *         is an instance of; null when there is none.
     */
    private static Class<?> declaredTypeOf(GoldenCase c) {
        if (c.thrown() == null) {
            return null;
        }
        for (Method m : c.iface().getMethods()) {
            if (!m.getName().equals(c.operation())) {
                continue;
            }
            for (Class<?> type : m.getExceptionTypes()) {
                if (type != RemoteException.class && type.isInstance(c.thrown())) {
                    return type;
                }
            }
        }
        return null;
    }

    private static final class Recording {
        private final byte[] response;
        private final int status;

        private Recording(byte[] response, int status) {
            this.response = response;
            this.status = status;
        }
    }
}
