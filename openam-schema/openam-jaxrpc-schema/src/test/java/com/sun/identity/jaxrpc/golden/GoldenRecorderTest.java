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

import static java.nio.charset.StandardCharsets.ISO_8859_1;
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
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Drives the generated {@code *_Tie} classes of the RPC/encoded endpoints directly with the golden
 * cases and checks the golden files against what the RI answers.
 *
 * <p>Without {@code -Djaxrpc.golden.root}, {@link #committedRecordingsAreReproduced()} compares the
 * committed files byte for byte, so a change to the case catalogue, to the client encoder or to the
 * RI that moves a recording fails the build. With {@code -Djaxrpc.golden.root=<repository root>},
 * {@link #recordsTheRIResponses()} deletes the files in the seven golden directories and writes
 * them again. To re-record, run from the repository root:
 *
 * <pre>
 * mvn -pl openam-schema/openam-jaxrpc-schema -am package -Dtest=GoldenRecorderTest \
 *   -Dsurefire.failIfNoSpecifiedTests=false -Dmaven.javadoc.skip=true -Djaxrpc.golden.root=$PWD
 * </pre>
 *
 * <p>{@code -am} builds the case catalogue ({@code GoldenCases}, {@code GoldenValues},
 * {@code CoreGoldenCases}) from the working tree. Without it, Maven takes the {@code openam-shared}
 * and {@code openam-core} test-jars from the local repository and records a stale catalogue.
 * The goal is {@code package}, not {@code test}, because the reactor then includes
 * {@code bcpkix-shaded}, whose relocated classes exist only after {@code package}.
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
        String root = rootProperty();
        if (root == null) {
            throw new SkipException("set -D" + ROOT_PROPERTY + "=<repository root> to record the golden files");
        }
        driveAll(Paths.get(root), true);
    }

    @Test
    public void committedRecordingsAreReproduced() throws Exception {
        if (rootProperty() != null) {
            throw new SkipException("recordsTheRIResponses rewrites the golden files in this run");
        }
        driveAll(repositoryRoot(), false);
    }

    private static String rootProperty() {
        String root = System.getProperty(ROOT_PROPERTY);
        return root == null || root.isEmpty() || root.startsWith("${") ? null : root;
    }

    /**
     * @return the nearest directory, from the module directory up, that holds the core test resources.
     */
    private static Path repositoryRoot() {
        Path start = Paths.get(System.getProperty("basedir", "")).toAbsolutePath();
        for (Path dir = start; dir != null; dir = dir.getParent()) {
            if (Files.isDirectory(dir.resolve(CORE_RESOURCES))) {
                return dir;
            }
        }
        throw new IllegalStateException("no " + CORE_RESOURCES + " in " + start + " or above it");
    }

    /**
     * Drives every golden case once. With write, replaces the golden files under root; otherwise
     * asserts that the committed files are exactly what the drive produced.
     */
    private static void driveAll(Path root, boolean write) throws Exception {
        Path coreDir = root.resolve(CORE_RESOURCES).resolve(GoldenCases.RESOURCE_ROOT);
        Path federationDir = root.resolve(FEDERATION_RESOURCES).resolve(GoldenCases.RESOURCE_ROOT);

        List<GoldenCase> all = new ArrayList<>();
        Map<GoldenCase, Recording> recordings = new HashMap<>();
        List<String> mismatches = new ArrayList<>();
        for (String name : CORE_INTERFACES) {
            Class<?> iface = Class.forName(name);
            List<GoldenCase> cases = CoreGoldenCases.forInterface(iface);
            recordings.putAll(driveInterface(coreDir.resolve(iface.getSimpleName()), cases, write, mismatches));
            all.addAll(cases);
        }
        for (String name : FEDERATION_INTERFACES) {
            Class<?> iface = Class.forName(name);
            List<GoldenCase> cases = GoldenCases.forInterface(iface);
            recordings.putAll(driveInterface(federationDir.resolve(iface.getSimpleName()), cases, write, mismatches));
            all.addAll(cases);
        }
        assertThat(mismatches).as("golden files that differ from what the RI answers now; re-record them"
                + " as the class Javadoc shows, or revert the change that moved them").isEmpty();

        assertThat(new String(Files.readAllBytes(
                coreDir.resolve("SMSObjectIF").resolve("checkForLocal__ok.response.xml")), UTF_8))
                .isEqualTo(CHECK_FOR_LOCAL_RESPONSE);

        List<String> degenerated = new ArrayList<>();
        for (GoldenCase c : all) {
            Recording r = recordings.get(c);
            Class<?> declared = declaredTypeOf(c);
            if (declared == null) {
                if (!c.clientObservableOnly() && r.status != 200) {
                    degenerated.add(c + " (status " + r.status + ", expected 200)");
                }
                continue;
            }
            String expected = "<faultstring>" + declared.getName() + "</faultstring>";
            if (r.status != 500 || !new String(r.response, UTF_8).contains(expected)) {
                degenerated.add(c + " (status " + r.status + ", expected " + expected + ")");
            }
        }
        assertThat(degenerated).as("cases that did not come back as the result or declared fault they were built for")
                .isEmpty();
    }

    /**
     * Drives the cases of one interface. With write, replaces the files in dir; otherwise adds to
     * mismatches every file of dir that is missing, differs or is no longer produced.
     */
    private static Map<GoldenCase, Recording> driveInterface(Path dir, List<GoldenCase> cases, boolean write,
            List<String> mismatches) throws Exception {
        Map<GoldenCase, Recording> recordings = new HashMap<>();
        Map<String, byte[]> files = new TreeMap<>();
        Map<String, Integer> statuses = new TreeMap<>();
        for (GoldenCase c : cases) {
            assertThat(statuses).as("duplicate id " + c).doesNotContainKey(c.id());
            byte[] request = c.request().getBytes(UTF_8);
            List<Object[]> received = new ArrayList<>();
            Recording r = drive(c, request, received);
            files.put(c.id() + ".request.xml", request);
            files.put(c.id() + ".response.xml", r.response);
            if (!received.isEmpty()) {
                assertThat(received).as("calls of " + c).hasSize(1);
                files.put(c.id() + ".args", GoldenValues.describe(received.get(0)).getBytes(UTF_8));
            }
            statuses.put(c.id(), r.status);
            recordings.put(c, r);
        }
        StringBuilder status = new StringBuilder();
        for (Map.Entry<String, Integer> e : statuses.entrySet()) {
            status.append(e.getKey()).append('=').append(e.getValue()).append('\n');
        }
        files.put("status.properties", status.toString().getBytes(UTF_8));
        if (write) {
            write(dir, files);
        } else {
            compare(dir, files, mismatches);
        }
        return recordings;
    }

    private static void write(Path dir, Map<String, byte[]> files) throws IOException {
        Files.createDirectories(dir);
        for (Path p : regularFiles(dir)) {
            Files.delete(p);
        }
        for (Map.Entry<String, byte[]> e : files.entrySet()) {
            Files.write(dir.resolve(e.getKey()), e.getValue());
        }
    }

    private static void compare(Path dir, Map<String, byte[]> files, List<String> mismatches) throws IOException {
        Set<String> committed = new TreeSet<>();
        if (Files.isDirectory(dir)) {
            for (Path p : regularFiles(dir)) {
                committed.add(p.getFileName().toString());
            }
        }
        String iface = dir.getFileName().toString();
        for (Map.Entry<String, byte[]> e : files.entrySet()) {
            String name = iface + "/" + e.getKey();
            if (!committed.remove(e.getKey())) {
                mismatches.add(name + ": not committed");
                continue;
            }
            int at = Arrays.mismatch(Files.readAllBytes(dir.resolve(e.getKey())), e.getValue());
            if (at >= 0) {
                mismatches.add(name + ": differs from byte " + at);
            }
        }
        for (String stale : committed) {
            mismatches.add(iface + "/" + stale + ": committed but no longer recorded");
        }
    }

    private static List<Path> regularFiles(Path dir) throws IOException {
        try (Stream<Path> listed = Files.list(dir)) {
            return listed.filter(Files::isRegularFile).collect(Collectors.toList());
        }
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
        return new Recording(withLfAfterDeclaration(out.toByteArray()), ctx.isFailure() ? 500 : 200);
    }

    /**
     * The RI ends the XML declaration with {@code System.lineSeparator()}: its
     * {@code com.sun.xml.rpc.util.xml.XmlWriter} calls {@code BufferedWriter.newLine()} after it. Writes
     * LF there instead, so that a recording made on Windows equals the committed one. The separator is
     * whitespace in the prolog, which no XML parser reports.
     */
    private static byte[] withLfAfterDeclaration(byte[] response) {
        String s = new String(response, ISO_8859_1);
        String separator = System.lineSeparator();
        int end = s.indexOf("?>") + 2;
        if (!s.startsWith("<?xml") || end < 2 || !s.startsWith(separator, end)) {
            return response;
        }
        return (s.substring(0, end) + "\n" + s.substring(end + separator.length())).getBytes(ISO_8859_1);
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
