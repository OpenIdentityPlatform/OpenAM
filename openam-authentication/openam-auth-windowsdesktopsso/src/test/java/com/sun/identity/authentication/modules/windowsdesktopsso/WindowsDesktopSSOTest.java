package com.sun.identity.authentication.modules.windowsdesktopsso;

import com.iplanet.am.util.SystemProperties;
import com.sun.identity.authentication.service.AuthD;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.reflect.FieldUtils;
import org.forgerock.guice.core.InjectorHolder;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.testng.PowerMockTestCase;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.testng.Assert.*;

@PrepareForTest({ SystemProperties.class, AuthD.class, InjectorHolder.class})
@PowerMockIgnore({"jdk.internal.reflect.*", "javax.servlet.*"})
public class WindowsDesktopSSOTest extends PowerMockTestCase {

    WindowsDesktopSSO windowsDesktopSSO = null;
    @BeforeClass
    public void setup() throws Exception {
        PowerMockito.mockStatic(SystemProperties.class);
        PowerMockito.mockStatic(InjectorHolder.class);
        PowerMockito.mockStatic(AuthD.class);
        windowsDesktopSSO = new WindowsDesktopSSO();
        FieldUtils.writeField(windowsDesktopSSO, "kdcRealm", "sso.openam.org", true);
        FieldUtils.writeField(windowsDesktopSSO, "kdcServer", "sso1.openam.org:sso2.openam.org", true);
    }

    @AfterMethod
    private void tearDown() {
        File file = new File(System.getProperty("java.io.tmpdir")+File.separator+"krb5.conf");
        if(file.exists()) {
            file.delete();
        }
    }

    @Test
    public void testGetUpdatedKrb5ConfigLines() {
       List<String> newLines = windowsDesktopSSO.getUpdatedKrb5ConfigLines(new ArrayList<>());
       assertNotNull(newLines);
    }

    @Test
    public void testGetUpdatedKrb5ConfigLines_newRealm() {
        String existingConfig = "[realms]\n" +
                "old-sso.openam.org = {\n" +
                "     kdc = old-sso1.openam.org\n" +
                "}\n";

        String expected = "[realms]\n" +
                "old-sso.openam.org = {\n" +
                "     kdc = old-sso1.openam.org\n" +
                "}\n" +
                "  SSO.OPENAM.ORG={\n" +
                "    kdc=sso1.openam.org\n" +
                "    kdc=sso2.openam.org\n" +
                "}";

        List<String> existingLines = Arrays.asList(existingConfig.split("\n"));
        List<String> newLines = windowsDesktopSSO.getUpdatedKrb5ConfigLines(existingLines);
        assertNotNull(newLines);
        assertEquals(newLines, Arrays.asList(expected.split("\n")));
    }

    @Test
    public void testGetUpdatedKrb5ConfigLines_kdcChanged() {
        String existingConfig = "[realms]\n" +
                "sso.openam.org = {\n" +
                "     kdc = old-sso1.openam.org\n" +
                "}\n";

        String expected = "[realms]\n" +
                "  SSO.OPENAM.ORG={\n" +
                "    kdc=sso1.openam.org\n" +
                "    kdc=sso2.openam.org\n" +
                "}";

        List<String> existingLines = Arrays.asList(existingConfig.split("\n"));
        List<String> newLines = windowsDesktopSSO.getUpdatedKrb5ConfigLines(existingLines);
        assertEquals(newLines, Arrays.asList(expected.split("\n")));
    }

    @Test
    public void testGetUpdatedKrb5ConfigLines_notChanged() {
        String existingConfig = "[realms]\n" +
                "   sso.openam.org = {\n" +
                "     kdc = sso1.openam.org\n" +
                "     kdc = sso2.openam.org\n" +
                "}";
        List<String> existingLines = Arrays.asList(existingConfig.split("\n"));
        List<String> newLines = windowsDesktopSSO.getUpdatedKrb5ConfigLines(existingLines);
        assertNull(newLines);
    }

    @Test
    public void testCreateUpdateKrb5ConfigFile() throws IOException {
        String expected = "[realms]\n" +
                "  SSO.OPENAM.ORG={\n" +
                "    kdc=sso1.openam.org\n" +
                "    kdc=sso2.openam.org\n" +
                "}";


        File file = new File(System.getProperty("java.io.tmpdir")+File.separator+"krb5.conf");
        if(file.exists()) {
            file.delete();
        }

        windowsDesktopSSO.createUpdateKrb5ConfigFile();
        file = new File(System.getProperty("java.security.krb5.conf"));
        assertTrue(file.exists());
        List<String> content = FileUtils.readLines(file, StandardCharsets.UTF_8);
        assertEquals(content, Arrays.asList(expected.split("\n")));
    }
}