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
 * Copyright 2015 ForgeRock AS.
 */

package com.sun.identity.workflow;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import org.apache.commons.io.IOUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

/*
To actually run this test, comment-out the <skipTests>true</skipTests> property in the pom.xml and run
mvn -Dtest=CreateSoapSTSDeploymentTest verify
from the command-line (other tests in the test-suite don't run successfully - hence the skipTests property.)
 */
public class CreateSoapSTSDeploymentTest {
    private static final String REALM_PARAM = "realm";
    private static final String OPENAM_URL_PARAM = "openAMUrl";
    private static final String SOAP_AGENT_NAME_PARAM = "soapAgentName";
    private static final String SOAP_AGENT_PASSWORD_PARAM = "soapAgentPassword";
    private static final String KEYSTORE_FILE_NAMES_PARAM = "keystoreFileNames";
    private static final String WSDL_FILE_NAMES_PARAM = "wsdlFileNames";

    private static final String REALM_PARAM_VALUE = "test_realm";
    private static final String OPENAM_URL_PARAM_VALUE = "https://host.com:443/om";
    private static final String SOAP_AGENT_NAME_PARAM_VALUE = "da_soap_agent";
    private static final String SOAP_AGENT_PASSWORD_PARAM_VALUE = "da_soap_agent_pw";
    private static final String KEYSTORE_FILE_NAMES_PARAM_VALUE = "keystore.jks";
    private static final String WSDL_FILE_NAMES_PARAM_VALUE = "custom.wsdl";

    private static final String SOAP_PROPERTY_FILE_AM_DEPLOYMENT_URL_KEY = "am_deployment_url";
    private static final String SOAP_PROPERTY_FILE_AM_SESSION_COOKIE_NAME_KEY = "am_session_cookie_name";
    private static final String SOAP_PROPERTY_FILE_SOAP_STS_AGENT_USERNAME_KEY = "soap_sts_agent_username";
    private static final String SOAP_PROPERTY_FILE_REALM_KEY = "am_realm";

    private static final String AM_SESSION_COOKIE_NAME = "custom_cookie_name";

    private static final String SOAP_PROPERTY_FILE_JAR_ENTRY_NAME = "WEB-INF/classes/config.properties";
    private static final String SOAP_KEYSTORE_JAR_ENTRY_NAME = "WEB-INF/classes/am_soap_sts.jks";
    private static final String MANIFEST_JAR_ENTRY_NAME = "META-INF/MANIFEST.MF";
    private static final String WEB_INF_CLASSES = "WEB-INF/classes/";

    private static final boolean WITH_KEYSTORE_FILE = true;
    private static final boolean WITH_CUSTOM_WSDL = true;
    @Rule
    public TemporaryFolder temporaryFolder;
    private final File outputWarFile;
    private final Path inputWarFilePath;
    private final Path customWsdlFilePath;
    private final Path keystoreFilePath;

    public CreateSoapSTSDeploymentTest() throws IOException {
        inputWarFilePath = Paths.get("/com", "sun", "identity", "workflow", "slim-openam-soap-sts-server.war");
        customWsdlFilePath = Paths.get("/com", "sun", "identity", "workflow", "custom.wsdl");
        keystoreFilePath = Paths.get("/com", "sun", "identity", "workflow", "keystore.jks");
        temporaryFolder = new TemporaryFolder();
        temporaryFolder.create();
        outputWarFile = temporaryFolder.newFile("test-openam-soap-sts-server.war");
    }

    @Test
    public void testCustomJarFileGeneration() throws WorkflowException, IOException {
        new MyCreateSoapSTSDeployment().execute(null, getParamMap(!WITH_KEYSTORE_FILE, !WITH_CUSTOM_WSDL));
        verifyGeneratedWarCorrectness(!WITH_KEYSTORE_FILE, !WITH_KEYSTORE_FILE);

    }

    @Test
    public void testCustomJarFileGenerationWithKeystoreAndCustomWsdl() throws WorkflowException, IOException {
        new MyCreateSoapSTSDeployment().execute(null, getParamMap(WITH_KEYSTORE_FILE, WITH_CUSTOM_WSDL));
        verifyGeneratedWarCorrectness(WITH_KEYSTORE_FILE, WITH_CUSTOM_WSDL);
    }

    private void verifyGeneratedWarCorrectness(boolean withKeystoreFile, boolean withCustomWsdl) throws IOException {
        try (JarInputStream jarInputStream  = new JarInputStream(getClass().getResourceAsStream(inputWarFilePath.toString()))) {
            final JarFile outputWar = new JarFile(outputWarFile);
            assertEquals(getJarInputStreamEntryNames(jarInputStream), getNonAddedOutputWarFileEntryNames(outputWar));
            assertNotNull(jarInputStream.getManifest());
            assertEquals(jarInputStream.getManifest(), outputWar.getManifest());
            verifyPropertyFileCorrectness(outputWar);
            verifyPresenceOfInternalKeyStore(outputWar);
            if (withKeystoreFile) {
                verifyKeystoreFilePresence(outputWar);
            }
            if (withCustomWsdl) {
                verifyCustomWsdlFileCorrectness(outputWar);
            }
        }
    }

    private void verifyKeystoreFilePresence(JarFile outputWar) {
        assertNotNull(outputWar.getEntry(WEB_INF_CLASSES + KEYSTORE_FILE_NAMES_PARAM_VALUE));
    }

    private void verifyPresenceOfInternalKeyStore(JarFile outputWar) {
        assertNotNull(outputWar.getEntry(SOAP_KEYSTORE_JAR_ENTRY_NAME));
    }

    private void verifyCustomWsdlFileCorrectness(JarFile outputWar) throws IOException {
        try (InputStream inputWsdlStream = getClass().getResourceAsStream(customWsdlFilePath.toString());
             InputStream fromWarInputStream = outputWar.getInputStream(outputWar.getJarEntry(WEB_INF_CLASSES + WSDL_FILE_NAMES_PARAM_VALUE))) {
            final String customWsdlInput = readStringFromInputStream(inputWsdlStream);
            final String customWsdlInWar = readStringFromInputStream(fromWarInputStream);
            assertEquals(customWsdlInput, customWsdlInWar);
        }
    }

    private String readStringFromInputStream(InputStream inputStream) throws IOException {
        final StringWriter stringWriter = new StringWriter();
        IOUtils.copy(inputStream, stringWriter);
        return stringWriter.toString();
    }

    private Set<String> getNonAddedOutputWarFileEntryNames(JarFile jarFile) {
        Set<String> entryNames = new HashSet<>();
        final Enumeration<JarEntry> entryEnumeration = jarFile.entries();
        while (entryEnumeration.hasMoreElements()) {
            String entryName = entryEnumeration.nextElement().getName();
            /*
            As we are testing for equality with the input .war, don't add the added keystore or custom wsdl files.
             */
            if (entryName.contains(WSDL_FILE_NAMES_PARAM_VALUE) ||
                    entryName.contains(KEYSTORE_FILE_NAMES_PARAM_VALUE) ||
                    entryName.contains(SOAP_KEYSTORE_JAR_ENTRY_NAME)) {
                continue;
            }
            entryNames.add(entryName);
        }
        return entryNames;
    }

    private Set<String> getJarInputStreamEntryNames(JarInputStream jarInputStream) throws IOException {
        Set<String> entryNames = new HashSet<>();
        JarEntry jarEntry = jarInputStream.getNextJarEntry();
        while (jarEntry != null) {
            entryNames.add(jarEntry.getName());
            jarEntry = jarInputStream.getNextJarEntry();
        }
        if (jarInputStream.getManifest() != null) {
            entryNames.add(MANIFEST_JAR_ENTRY_NAME);
        }
        return entryNames;
    }

    /*
    Load the config.properties file in the generated .war file and insure that its values match those passed into the
    workflow task (which are in turn, harvested from the ViewBean)
     */
    private void verifyPropertyFileCorrectness(JarFile outputWar) throws IOException {

        try (InputStream outputWarPropertiesFileInputStream = outputWar.getInputStream(outputWar.getJarEntry(SOAP_PROPERTY_FILE_JAR_ENTRY_NAME))) {
            Properties warProperties = new Properties();
            warProperties.load(outputWarPropertiesFileInputStream);
            assertEquals(warProperties.getProperty(SOAP_PROPERTY_FILE_AM_DEPLOYMENT_URL_KEY), OPENAM_URL_PARAM_VALUE);
            assertEquals(warProperties.getProperty(SOAP_PROPERTY_FILE_AM_SESSION_COOKIE_NAME_KEY), AM_SESSION_COOKIE_NAME);
            assertEquals(warProperties.getProperty(SOAP_PROPERTY_FILE_SOAP_STS_AGENT_USERNAME_KEY), SOAP_AGENT_NAME_PARAM_VALUE);
            assertEquals(warProperties.getProperty(SOAP_PROPERTY_FILE_REALM_KEY), REALM_PARAM_VALUE);
        }
    }


    @SuppressWarnings("unchecked")
    private Map getParamMap(boolean withKeystoreFile, boolean withCustomWsdl) {
        Map<String, String> paramMap = new HashMap<>();
        paramMap.put(REALM_PARAM, REALM_PARAM_VALUE);
        paramMap.put(OPENAM_URL_PARAM, OPENAM_URL_PARAM_VALUE);
        paramMap.put(SOAP_AGENT_NAME_PARAM, SOAP_AGENT_NAME_PARAM_VALUE);
        paramMap.put(SOAP_AGENT_PASSWORD_PARAM, SOAP_AGENT_PASSWORD_PARAM_VALUE);
        if (withKeystoreFile) {
            paramMap.put(KEYSTORE_FILE_NAMES_PARAM, KEYSTORE_FILE_NAMES_PARAM_VALUE);
        }
        if (withCustomWsdl) {
            paramMap.put(WSDL_FILE_NAMES_PARAM, WSDL_FILE_NAMES_PARAM_VALUE);
        }
        return paramMap;
    }

    /*
    Subclass of the CreateSoapSTSDeployment which will provide the Jar Input/Output stream, and the getAMSessionIdCookieNameForDeployment
    values. The former is necessary to test generated war file correctness, and the latter is necessary because the test context
    does not have an initialized SystemPropertiesManager.
     */
    private class MyCreateSoapSTSDeployment extends CreateSoapSTSDeployment {
        @Override
        protected JarInputStream getJarInputStream() throws WorkflowException {
            try {
                return new JarInputStream(getClass().getResourceAsStream(inputWarFilePath.toString()));
            } catch (IOException e) {
                throw new WorkflowException("error opening test resource .war file: " + e);
            }
        }

        @Override
        protected JarOutputStream getJarOutputStream(Path outputWarFilePath, Manifest inputWarManifest) throws WorkflowException {
            try {
                return new JarOutputStream(Files.newOutputStream(outputWarFilePath, StandardOpenOption.WRITE), inputWarManifest);
            } catch (IOException e) {
                throw new WorkflowException("could not create output .war file: " + e);
            }
        }

        @Override
        protected Path getOutputJarFilePath(String realm) throws WorkflowException {
            return outputWarFile.toPath();
        }

        @Override
        protected String getCompletionMessage(Locale locale, Path outputJarPath) {
            return "bobo";
        }

        @Override
        protected String getAMSessionIdCookieNameForDeployment() {
            return AM_SESSION_COOKIE_NAME;
        }

        @Override
        protected InputStream getInputStreamForKeystoreFileOrCustomWsdlFile(String fileName) throws IOException {
            if (WSDL_FILE_NAMES_PARAM_VALUE.equals(fileName)) {
                return getClass().getResourceAsStream(customWsdlFilePath.toString());
            } else if (KEYSTORE_FILE_NAMES_PARAM_VALUE.equals(fileName)) {
                return getClass().getResourceAsStream(keystoreFilePath.toString());
            }
            throw new IllegalStateException("Unexpected fileName parameter in " +
                    "MyCreateSoapSTSDeployment#getInputStreamForKeystoreFileOrCustomWsdlFile: " + fileName);
        }
    }
}
