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

package com.sun.identity.setup;

import static com.sun.identity.setup.AMSetupUtils.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

import javax.servlet.ServletContext;

import java.io.IOException;
import java.io.InputStream;

import org.forgerock.openam.utils.IOUtils;
import org.testng.annotations.Test;

public class AMSetupUtilsTest {

    @Test
    public void shouldGetResourceAsStreamUsingServletContext() {

        //Given
        ServletContext context = mock(ServletContext.class);
        String file = "AMSetupUtilsTestFile.txt";

        //When
        getResourceAsStream(context, file);

        //Then
        verify(context).getResourceAsStream(file);
    }

    @Test
    public void shouldGetResourceAsStreamUsingClassLoader() throws IOException {

        //Given
        String file = "/AMSetupUtilsTestFile.txt";

        //When
        InputStream stream = getResourceAsStream(null, file);

        //Then
        assertThat(IOUtils.readStream(stream)).isEqualTo("FILE_CONTENTS\n");
    }

    @Test(expectedExceptions = IOException.class)
    public void readFileShouldThrowIOExceptionIfFileNotFound() throws IOException {

        //Given
        ServletContext context = mock(ServletContext.class);
        String file = "INVALID_FILE_NAME";

        //When
        readFile(context, file);

        //Then
        failBecauseExceptionWasNotThrown(IOException.class);
    }

    @Test
    public void shouldReadFileUsingServletContext() throws IOException {

        //Given
        ServletContext context = mock(ServletContext.class);
        String file = "AMSetupUtilsTestFile.txt";

        given(context.getResourceAsStream(file))
                .willReturn(Thread.currentThread().getContextClassLoader().getResourceAsStream(file));

        //When
        String contents = readFile(context, file);

        //Then
        verify(context).getResourceAsStream(file);
        assertThat(contents).isEqualTo("FILE_CONTENTS\n");
    }

    @Test
    public void shouldReadFileUsingClassLoader() throws IOException {

        //Given
        String file = "/AMSetupUtilsTestFile.txt";

        //When
        String contents = readFile(null, file);

        //Then
        assertThat(contents).isEqualTo("FILE_CONTENTS\n");
    }

    @Test
    public void shouldGetRandomString() {

        //Given
        String firstRandomString = getRandomString();

        //When
        String secondRandomString = getRandomString();

        //Then
        assertThat(firstRandomString).isNotEqualTo(secondRandomString);
    }

    @Test
    public void shouldGetFirstUnusedPort() {

        //Given
        String hostname = "localhost";
        int startPort = 10;
        int interval = 10;

        //When
        int unusedPort = getFirstUnusedPort(hostname, startPort, interval);

        //Then
        assertThat(unusedPort).isBetween(10, 65535);
    }
}
