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
 * Copyright 2026 3A Systems LLC.
 */
package com.sun.identity.config.wizard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import com.sun.identity.config.SessionAttributeNames;
import org.openidentityplatform.openam.config.servlet.ConfiguratorContext;
import org.testng.SkipException;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Byte-exact response and session side-effect coverage for the migrated Step2 page, matching
 * the old Click AjaxPage/ProtectedPage/Step2 behavior these handlers were ported from.
 */
public class Step2Test {

    private HttpServletRequest request;
    private HttpServletResponse response;
    private StringWriter responseBody;
    private Map<String, Object> sessionAttributes;
    private Step2 step2;
    private Path tempDir;

    @BeforeMethod
    public void setup() throws Exception {
        sessionAttributes = new HashMap<>();

        HttpSession session = mock(HttpSession.class);
        doAnswer(inv -> sessionAttributes.put(inv.getArgument(0), inv.getArgument(1)))
                .when(session).setAttribute(anyString(), any());
        when(session.getAttribute(anyString())).thenAnswer(inv -> sessionAttributes.get(inv.getArgument(0)));

        request = mock(HttpServletRequest.class);
        when(request.getSession()).thenReturn(session);
        when(request.getSession(false)).thenReturn(session);
        when(request.getParameter("locale")).thenReturn("en");

        responseBody = new StringWriter();
        response = mock(HttpServletResponse.class);
        when(response.getWriter()).thenReturn(new PrintWriter(responseBody));

        step2 = new Step2();
        step2.setContext(new ConfiguratorContext(request, response));
    }

    @AfterMethod
    public void cleanup() throws IOException {
        if (tempDir != null && Files.exists(tempDir)) {
            try (var stream = Files.walk(tempDir)) {
                stream.sorted((a, b) -> b.compareTo(a)).forEach(p -> p.toFile().delete());
            }
        }
    }

    private void param(String name, String value) {
        when(request.getParameter(name)).thenReturn(value);
    }

    @Test
    public void validateConfigDirRejectsMissingDir() {
        step2.validateConfigDir();

        assertThat(responseBody.toString()).isEqualTo("Missing Required Field");
        assertThat(sessionAttributes).isEmpty();
    }

    @Test
    public void validateConfigDirRejectsNoWritePermission() throws IOException {
        tempDir = Files.createTempDirectory("step2-nowrite");
        tempDir.toFile().setWritable(false);
        if (tempDir.toFile().canWrite()) {
            // File.setWritable(false) on a directory isn't enforced by every environment this
            // suite runs in: Windows doesn't honor the read-only attribute for writes into a
            // directory, and a privileged (e.g. root) test runner on POSIX bypasses permission
            // checks entirely. Same "not reliably reproducible across environments" category as
            // the other environment-dependent branches.
            tempDir.toFile().setWritable(true);
            throw new SkipException(
                    "Cannot make " + tempDir + " non-writable in this environment (Windows or privileged test runner)");
        }
        param("dir", tempDir.toString());

        step2.validateConfigDir();

        assertThat(responseBody.toString()).isEqualTo("Do not have write permission to this directory.");
        assertThat(sessionAttributes).isEmpty();

        tempDir.toFile().setWritable(true);
    }

    @Test
    public void validateConfigDirRejectsNonEmptyDir() throws IOException {
        tempDir = Files.createTempDirectory("step2-content");
        Files.createFile(tempDir.resolve("existing.txt"));
        param("dir", tempDir.toString());

        step2.validateConfigDir();

        assertThat(responseBody.toString()).isEqualTo("Directory is not empty");
        assertThat(sessionAttributes).isEmpty();
    }

    @Test
    public void validateConfigDirAcceptsWritableEmptyDir() throws IOException {
        tempDir = Files.createTempDirectory("step2-ok");
        param("dir", tempDir.toString());

        step2.validateConfigDir();

        assertThat(responseBody.toString()).isEqualTo("true");
        assertThat(sessionAttributes.get(SessionAttributeNames.CONFIG_DIR)).isEqualTo(tempDir.toString());
    }

    // validateCookieDomain() is not covered here: its first line unconditionally calls
    // ServicesDefaultValues.isCookieDomainValid(...), whose class has an eager singleton static
    // initializer that loads serviceDefaultValues.properties - a resource that lives only in
    // openam-server-only/src/main/resources/config/, never on openam-core's classpath (test or
    // main) in any build configuration. That makes this method untestable from an openam-core
    // unit test regardless of environment.
}
