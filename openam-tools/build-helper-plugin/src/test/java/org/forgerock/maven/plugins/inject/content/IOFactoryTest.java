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
 * Copyright 2014 ForgeRock AS.
 */

package org.forgerock.maven.plugins.inject.content;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class IOFactoryTest {

    private IOFactory ioFactory;

    private BufferedReader reader;
    private BufferedWriter writer;

    @BeforeMethod
    public void setUp() {

        reader = mock(BufferedReader.class);
        writer = mock(BufferedWriter.class);

        ioFactory = new IOFactory() {
            @Override
            BufferedReader newReader(File file) throws FileNotFoundException {
                return reader;
            }

            @Override
            BufferedWriter newWriter(File file) throws IOException {
                return writer;
            }
        };
    }

    @Test
    public void shouldWriteContent() throws IOException {

        //Given
        BufferedWriter writer = mock(BufferedWriter.class);
        File content = mock(File.class);
        ContentConverter contentConverter = mock(ContentConverter.class);

        given(reader.readLine())
                .willReturn("LINE1")
                .willReturn("LINE2")
                .willReturn(null);

        //When
        ioFactory.writeContent(writer, content, contentConverter);

        //Then
        verify(contentConverter, times(2)).convert(anyString());
        verify(writer, times(2)).write(anyString());
        verify(writer, times(2)).newLine();

        verify(reader).close();
        verify(writer, never()).flush();
        verify(writer, never()).close();
    }

    @Test
    public void shouldMoveTempFile() throws IOException {

        //Given
        File destinationFile = mock(File.class);
        File tmpFile = mock(File.class);

        given(tmpFile.renameTo(destinationFile)).willReturn(true);

        //When
        ioFactory.moveTo(tmpFile, destinationFile);

        //Then
    }

    @Test (expectedExceptions = IOException.class)
    public void shouldFailToMoveTempFile() throws IOException {

        //Given
        File destinationFile = mock(File.class);
        File tmpFile = mock(File.class);

        given(reader.readLine())
                .willReturn("LINE1")
                .willReturn("LINE2")
                .willReturn("LINE3")
                .willReturn(null);

        given(tmpFile.renameTo(destinationFile)).willReturn(false);

        //When
        ioFactory.moveTo(tmpFile, destinationFile);
    }
}
