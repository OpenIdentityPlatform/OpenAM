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

import org.apache.maven.plugin.MojoExecutionException;
import org.mockito.ArgumentCaptor;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class InjectContentTest {

    private InjectContent injectContent;

    private IOFactory ioFactory;
    private List<Inject> injects;

    @BeforeMethod
    public void setUp() {

        ioFactory = mock(IOFactory.class);
        injects = new ArrayList<Inject>();

        injectContent = new InjectContent(ioFactory, injects);
    }

    private Inject setUpInject(String destinationFilePath, File tempFile, BufferedReader reader, BufferedWriter writer,
            Content... content) throws IOException {

        List<Content> contents = new ArrayList<Content>();
        contents.addAll(Arrays.asList(content));

        File destinationFile = mock(File.class);
        given(destinationFile.getAbsolutePath()).willReturn(destinationFilePath);

        Inject inject = new Inject();
        inject.setContents(contents);
        inject.setDestinationFile(destinationFile);

        given(ioFactory.newReader(destinationFile)).willReturn(reader);
        given(ioFactory.newWriter(tempFile)).willReturn(writer);

        return inject;
    }

    private Content setUpContent(String contentId, File contentFile) {

        Content content = new Content();
        content.setId(contentId);
        content.setFile(contentFile);

        return content;
    }

    @Test
    public void shouldWriteContentToDestinationFile() throws MojoExecutionException, IOException {

        //Given
        File contentFile = mock(File.class);
        BufferedReader destinationFileReader = mock(BufferedReader.class);
        BufferedWriter tmpFileWriter = mock(BufferedWriter.class);
        File temporaryFile = mock(File.class);

        Inject inject = setUpInject("DEST_FILE_ABS_PATH", temporaryFile, destinationFileReader, tmpFileWriter,
                setUpContent("CONTENT_ID", contentFile));

        injects.add(inject);

        given(ioFactory.createTemporaryFile(inject.getDestinationFile())).willReturn(temporaryFile);

        given(destinationFileReader.readLine())
                .willReturn("ANYTHING")
                .willReturn("BLAH BLAH ${inject.content.CONTENT_ID} BLAH BLAH")
                .willReturn("SOMETHING_ELSE")
                .willReturn(null);

        //When
        injectContent.execute();

        //Then
        ArgumentCaptor<ContentConverter> contentConverterCaptor = ArgumentCaptor.forClass(ContentConverter.class);
        verify(ioFactory).writeContent(eq(tmpFileWriter), eq(contentFile), contentConverterCaptor.capture());
        assertThat(contentConverterCaptor.getValue()).isInstanceOf(NoOpContentConverter.class);
        verify(ioFactory).writeLine(tmpFileWriter, "ANYTHING");
        verify(ioFactory).writeLine(tmpFileWriter, "SOMETHING_ELSE");
        verify(ioFactory).moveTo(temporaryFile, inject.getDestinationFile());

        verify(destinationFileReader).close();
        verify(tmpFileWriter).flush();
        verify(tmpFileWriter).close();
    }

    @Test
    public void shouldWriteTwoContentFilesToDestinationFile() throws MojoExecutionException, IOException {

        //Given
        File contentFile = mock(File.class);
        File contentFile2 = mock(File.class);
        BufferedReader destinationFileReader = mock(BufferedReader.class);
        BufferedWriter tmpFileWriter = mock(BufferedWriter.class);
        File temporaryFile = mock(File.class);

        Inject inject = setUpInject("DEST_FILE_ABS_PATH", temporaryFile, destinationFileReader, tmpFileWriter,
                setUpContent("CONTENT_ID", contentFile), setUpContent("CONTENT_ID2", contentFile2));
        inject.setContentConverter("xml");

        injects.add(inject);

        given(ioFactory.createTemporaryFile(inject.getDestinationFile())).willReturn(temporaryFile);

        given(destinationFileReader.readLine())
                .willReturn("ANYTHING")
                .willReturn("BLAH BLAH ${inject.content.CONTENT_ID} BLAH BLAH")
                .willReturn("SOMETHING_ELSE")
                .willReturn("${inject.content.CONTENT_ID2}")
                .willReturn(null);

        //When
        injectContent.execute();

        //Then
        ArgumentCaptor<ContentConverter> contentConverterCaptor = ArgumentCaptor.forClass(ContentConverter.class);
        verify(ioFactory).writeContent(eq(tmpFileWriter), eq(contentFile), contentConverterCaptor.capture());
        assertThat(contentConverterCaptor.getValue()).isInstanceOf(XmlContentConverter.class);

        contentConverterCaptor = ArgumentCaptor.forClass(ContentConverter.class);
        verify(ioFactory).writeContent(eq(tmpFileWriter), eq(contentFile2), contentConverterCaptor.capture());
        assertThat(contentConverterCaptor.getValue()).isInstanceOf(XmlContentConverter.class);

        verify(ioFactory).writeLine(tmpFileWriter, "ANYTHING");
        verify(ioFactory).writeLine(tmpFileWriter, "SOMETHING_ELSE");
        verify(ioFactory).moveTo(temporaryFile, inject.getDestinationFile());

        verify(destinationFileReader).close();
        verify(tmpFileWriter).flush();
        verify(tmpFileWriter).close();
    }

    @Test (expectedExceptions = MojoExecutionException.class)
    public void shouldThrowMojoExecutionExceptionWithUnknownContentConverter() throws MojoExecutionException,
            IOException {

        //Given
        File contentFile = mock(File.class);
        BufferedReader destinationFileReader = mock(BufferedReader.class);
        BufferedWriter tmpFileWriter = mock(BufferedWriter.class);
        File temporaryFile = mock(File.class);

        Inject inject = setUpInject("DEST_FILE_ABS_PATH", temporaryFile, destinationFileReader, tmpFileWriter,
                setUpContent("CONTENT_ID", contentFile));
        inject.setContentConverter("unknown");

        injects.add(inject);

        given(ioFactory.createTemporaryFile(inject.getDestinationFile())).willReturn(temporaryFile);

        given(destinationFileReader.readLine())
                .willReturn("${inject.content.CONTENT_ID}")
                .willReturn(null);

        //When
        try {
            injectContent.execute();
        } catch (MojoExecutionException e) {
            //Then
            verify(destinationFileReader).close();
            verify(tmpFileWriter).flush();
            verify(tmpFileWriter).close();

            throw e;
        }
    }

    @Test
    public void shouldWriteToTwoDestinationFiles() throws MojoExecutionException, IOException {

        //Given
        File contentFileOne = mock(File.class);
        BufferedReader destinationFileReaderOne = mock(BufferedReader.class);
        BufferedWriter tmpFileWriterOne = mock(BufferedWriter.class);
        File contentFileTwo = mock(File.class);
        BufferedReader destinationFileReaderTwo = mock(BufferedReader.class);
        BufferedWriter tmpFileWriterTwo = mock(BufferedWriter.class);
        File temporaryFileOne = mock(File.class);
        File temporaryFileTwo = mock(File.class);

        Inject injectOne = setUpInject("DEST_FILE_ABS_PATH_1", temporaryFileOne, destinationFileReaderOne,
                tmpFileWriterOne, setUpContent("CONTENT_ID", contentFileOne));
        Inject injectTwo = setUpInject("DEST_FILE_ABS_PATH_2", temporaryFileTwo, destinationFileReaderTwo,
                tmpFileWriterTwo, setUpContent("CONTENT_ID", contentFileTwo));
        injectTwo.setContentConverter("xml");

        injects.add(injectOne);
        injects.add(injectTwo);

        given(ioFactory.createTemporaryFile(injectOne.getDestinationFile())).willReturn(temporaryFileOne);
        given(ioFactory.createTemporaryFile(injectTwo.getDestinationFile())).willReturn(temporaryFileTwo);

        given(destinationFileReaderOne.readLine())
                .willReturn("ANYTHING")
                .willReturn("BLAH BLAH ${inject.content.CONTENT_ID} BLAH BLAH")
                .willReturn("SOMETHING_ELSE")
                .willReturn(null);

        given(destinationFileReaderTwo.readLine())
                .willReturn("ANYTHING")
                .willReturn("BLAH BLAH ${inject.content.CONTENT_ID} BLAH BLAH")
                .willReturn("SOMETHING_ELSE")
                .willReturn(null);

        //When
        injectContent.execute();

        //Then
        ArgumentCaptor<ContentConverter> contentConverterCaptor = ArgumentCaptor.forClass(ContentConverter.class);
        verify(ioFactory).writeContent(eq(tmpFileWriterOne), eq(contentFileOne), contentConverterCaptor.capture());
        assertThat(contentConverterCaptor.getValue()).isInstanceOf(NoOpContentConverter.class);
        verify(ioFactory).writeLine(tmpFileWriterOne, "ANYTHING");
        verify(ioFactory).writeLine(tmpFileWriterOne, "SOMETHING_ELSE");
        verify(ioFactory).moveTo(temporaryFileOne, injectOne.getDestinationFile());

        verify(destinationFileReaderOne).close();
        verify(tmpFileWriterOne).flush();
        verify(tmpFileWriterOne).close();

        contentConverterCaptor = ArgumentCaptor.forClass(ContentConverter.class);
        verify(ioFactory).writeContent(eq(tmpFileWriterTwo), eq(contentFileTwo), contentConverterCaptor.capture());
        assertThat(contentConverterCaptor.getValue()).isInstanceOf(XmlContentConverter.class);
        verify(ioFactory).writeLine(tmpFileWriterTwo, "ANYTHING");
        verify(ioFactory).writeLine(tmpFileWriterTwo, "SOMETHING_ELSE");
        verify(ioFactory).moveTo(temporaryFileTwo, injectTwo.getDestinationFile());

        verify(destinationFileReaderTwo).close();
        verify(tmpFileWriterTwo).flush();
        verify(tmpFileWriterTwo).close();
    }
}
