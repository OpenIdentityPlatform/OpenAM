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
 * Copyright 2026 3A Systems LLC.
 */

package org.openidentityplatform.openam.test.integration;

import org.codehaus.cargo.container.ContainerType;
import org.codehaus.cargo.container.InstalledLocalContainer;
import org.codehaus.cargo.container.configuration.ConfigurationType;
import org.codehaus.cargo.container.configuration.LocalConfiguration;
import org.codehaus.cargo.container.deployable.WAR;
import org.codehaus.cargo.container.installer.ZipURLInstaller;
import org.codehaus.cargo.container.property.GeneralPropertySet;
import org.codehaus.cargo.container.property.ServletPropertySet;
import org.codehaus.cargo.generic.DefaultContainerFactory;
import org.codehaus.cargo.generic.configuration.DefaultConfigurationFactory;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

import java.io.File;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

/**
 * Base class for integration tests that need a running OpenAM. Each test method runs against its own
 * freshly installed OpenAM instance: a Tomcat container is started in {@code @BeforeMethod} and stopped
 * in {@code @AfterMethod}, so the instances run sequentially (one at a time) rather than all three being
 * deployed into a single shared container as before.
 *
 * <p>The web app context to deploy is taken from the {@link OpenAMContext} annotation on the test method.
 * The Tomcat distribution, the OpenAM WAR location, the container id and the JVM arguments are provided by
 * the build through system properties (see {@code openam-server/pom.xml}).</p>
 */
public abstract class CargoBaseTest extends BaseTest {

    static final int SERVLET_PORT = 8207;
    static final int RMI_PORT = 8206;

    private InstalledLocalContainer container;
    private String context;

    @BeforeMethod(alwaysRun = true)
    public void startOpenAM(Method method) throws Exception {
        final OpenAMContext annotation = method.getAnnotation(OpenAMContext.class);
        if (annotation == null) {
            throw new IllegalStateException(
                    "test method " + method.getName() + " must be annotated with @OpenAMContext");
        }
        context = annotation.value();

        // ensure the next OpenAM boots unconfigured
        cleanConfig();

        final String containerId = System.getProperty("cargo.containerId");
        final File installs = new File(System.getProperty("cargo.install.dir"));
        installs.mkdirs();

        // download + unpack Tomcat once; subsequent calls reuse the cached install
        final ZipURLInstaller installer = new ZipURLInstaller(
                new URI(System.getProperty("cargo.tomcat.zip.url")).toURL(),
                installs.getAbsolutePath(), installs.getAbsolutePath());
        installer.install();

        final WAR war = new WAR(System.getProperty("openam.war"));
        war.setContext(context);

        final File configHome = new File(System.getProperty("cargo.config.dir"), context);

        final LocalConfiguration configuration = (LocalConfiguration) new DefaultConfigurationFactory()
                .createConfiguration(containerId, ContainerType.INSTALLED,
                        ConfigurationType.STANDALONE, configHome.getAbsolutePath());
        configuration.setProperty(ServletPropertySet.PORT, String.valueOf(SERVLET_PORT));
        configuration.setProperty(GeneralPropertySet.RMI_PORT, String.valueOf(RMI_PORT));
        configuration.setProperty(GeneralPropertySet.JVMARGS, jvmArgs());
        configuration.addDeployable(war);

        container = (InstalledLocalContainer) new DefaultContainerFactory()
                .createContainer(containerId, ContainerType.INSTALLED, configuration);
        container.setHome(installer.getHome());
        container.setTimeout(180_000L);

        System.out.println("starting OpenAM instance, context=/" + context);
        container.start();
        waitForContext();
        System.out.println("OpenAM instance started, context=/" + context);
    }

    /**
     * Wait until the deployed web app answers on its context root. {@code container.start()} only blocks
     * until Tomcat itself is up; the heavy OpenAM web app may still be initialising, so poll the context
     * (this mirrors the per-deployable {@code pingURL} the cargo-maven plugin used previously).
     */
    private void waitForContext() throws Exception {
        final URL url = new URI("http://localhost:" + SERVLET_PORT + "/" + context).toURL();
        final long deadline = System.nanoTime() + 180_000L * 1_000_000L;
        Exception last = null;
        while (System.nanoTime() < deadline) {
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) url.openConnection();
                connection.setInstanceFollowRedirects(false);
                connection.setConnectTimeout(5_000);
                connection.setReadTimeout(10_000);
                final int code = connection.getResponseCode();
                if (code > 0 && code != 404 && code < 500) {
                    return;
                }
            } catch (Exception e) {
                last = e;
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
            Thread.sleep(2_000);
        }
        throw new IllegalStateException("OpenAM context /" + context + " did not become available", last);
    }

    @AfterMethod(alwaysRun = true)
    public void stopOpenAM() {
        if (container != null) {
            System.out.println("stopping OpenAM instance, context=/" + context);
            try {
                container.stop();
            } finally {
                container = null;
            }
        }
    }

    /** JVM arguments for the forked Tomcat process running this OpenAM instance. */
    private String jvmArgs() {
        final String configPath = System.getProperty("test.config.path");
        // the surefire options carry the JPMS --add-opens/--add-exports OpenAM needs on JDK 17+
        final String surefire = System.getProperty("cargo.surefire.options", "").replaceAll("\\s+", " ").trim();
        return surefire + " -Xmx2g"
                + " -Dfile.encoding=UTF-8"
                + " -Dcom.sun.xml.ws.transport.http.HttpAdapter.dump=true"
                + " -Dcom.iplanet.services.configpath=" + configPath
                + " -Dcom.sun.identity.configuration.directory=" + configPath
                + " -Dlogback.configurationFile=" + System.getProperty("cargo.logback.config")
                + " -Dssoadm.disabled=false"
                + " -Dcom.iplanet.services.debug.level=warning"
                + " -Dcom.sun.services.debug.mergeall=on"
                + " -DXUI.enable=false";
    }
}
