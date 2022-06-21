package com.sun.identity.authentication.modules.nt;

import com.iplanet.am.util.SystemProperties;
import com.sun.identity.authentication.service.AuthD;
import org.forgerock.guice.core.InjectorHolder;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.testng.PowerMockTestCase;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

@PrepareForTest({ SystemProperties.class, AuthD.class, InjectorHolder.class})
@PowerMockIgnore({"jdk.internal.reflect.*", "javax.servlet.*"})
public class NTTest extends PowerMockTestCase {

    @Test (dataProvider = "data-provider")
    public void testEncode(String string, String expected) {
        PowerMockito.mockStatic(SystemProperties.class);
        PowerMockito.mockStatic(InjectorHolder.class);
        PowerMockito.mockStatic(AuthD.class);
        NT nt = new NT();
        String encoded = nt.escapeSpecial(string);
        assertEquals(encoded, expected);
    }

    @DataProvider(name = "data-provider")
    public Object[][] dpMethod(){
        return new Object[][] {
                {"t\nт", "t\\nт"},
                {"t\\nт", "t\\nт"},
                {"тест", "тест"},
                {"test", "test"},
                {"\r\n", "\\r\\n"},
                {"\\\r\\\n", "\\\\r\\\\n"},
        };
    }
}