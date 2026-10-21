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

package org.forgerock.oauth2.restlet;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import freemarker.template.Configuration;

import org.testng.annotations.Test;

/**
 * Renders {@code CodeVerificationForm.ftl} and pins its browser-cache contract.
 *
 * <p>Kept out of {@link DeviceCodeVerificationResourceTest} on purpose: that class carries a
 * {@code @BeforeMethod} that spies a {@link DeviceCodeVerificationResource}, and running a
 * template render alongside those setups exposes cross-test state that TestNG carries between
 * classes in one JVM.
 */
public class CodeVerificationFormTest {

    private static final Pattern URL_ARGS = Pattern.compile("urlArgs\\s*:\\s*\"([^\"]*)\"");

    /**
     * The uncached user code page must ship a RequireJS urlArgs value that busts caches of the
     * previous DeviceTemplate.html (public, max-age=2592000, with no versioned URL of its own),
     * and it must also install a submit-time listener that appends csrf to any device form that
     * reaches the DOM without it. Both lines are the only defence for a browser that opened the
     * device page shortly before the upgrade and would otherwise POST with no hidden csrf field
     * for up to a month.
     *
     * <p>The {@code var require = ...} assignment must also come before the RequireJS script
     * tag; RequireJS only honours the initial global, so a later assignment is ignored.
     */
    @Test
    public void shouldCacheBustXuiBeforeRequireJs() throws Exception {
        String page = render("SECRET-TOKEN");

        int urlArgs = page.indexOf("urlArgs");
        int requireJs = page.indexOf("requirejs-2.3.7-min.js");
        assertThat(urlArgs).as("urlArgs is emitted").isGreaterThanOrEqualTo(0);
        assertThat(requireJs).as("requirejs script tag is emitted").isGreaterThanOrEqualTo(0);
        assertThat(urlArgs).as("urlArgs is set before requirejs loads").isLessThan(requireJs);
        assertThat(page).as("submit-time csrf injector is present").contains("addEventListener(\"submit\"");
    }

    /**
     * The urlArgs value must not carry the CSRF token, in any spelling: RequireJS appends it to
     * every module and text! URL it loads, so a token there would leak into access logs, shared
     * caches and any other place a request line is recorded, letting whoever reads them submit a
     * device code of their choosing on the session's behalf. The value is extracted from two
     * renders carrying different tokens: it has to be non-empty (an empty one busts nothing), free
     * of the token, and the same for every user.
     */
    @Test
    public void shouldUseOneNonSecretUrlArgsForEveryUser() throws Exception {
        String first = urlArgsFor("FIRST-TOKEN");
        assertThat(first).as("urlArgs busts the cache").isNotEmpty();
        assertThat(first).as("CSRF token must not reach urlArgs").doesNotContain("FIRST-TOKEN");
        assertThat(urlArgsFor("SECOND-TOKEN")).as("urlArgs is the same for every user").isEqualTo(first);
    }

    private static String urlArgsFor(String csrf) throws Exception {
        Matcher m = URL_ARGS.matcher(render(csrf));
        assertThat(m.find()).as("urlArgs is emitted").isTrue();
        return m.group(1);
    }

    private static String render(String csrf) throws Exception {
        Configuration cfg = new Configuration();
        cfg.setClassForTemplateLoading(CodeVerificationFormTest.class, "/templates");
        Map<String, Object> model = new HashMap<>();
        model.put("csrf", csrf);
        model.put("realm", "/");
        model.put("baseUrl", "http://am.example.com/openam");
        StringWriter out = new StringWriter();
        cfg.getTemplate("CodeVerificationForm.ftl").process(model, out);
        return out.toString();
    }
}
