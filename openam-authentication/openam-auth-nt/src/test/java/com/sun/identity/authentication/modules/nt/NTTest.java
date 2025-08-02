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
 * Copyright 2022-2025 3A Systems, LLC.
 */

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
@PowerMockIgnore({"jdk.internal.reflect.*", "jakarta.servlet.*"})
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