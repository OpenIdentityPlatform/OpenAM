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

package org.forgerock.openam.utils.file;

import org.forgerock.openam.utils.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Generate a zip from a folder
 */
public class ZipUtils {

    private List<String> fileList = new ArrayList<String>();
    private String srcFolder;

    /**
     * Generate a zip
     * @param srcFolder source folder
     * @param outputZip zip folder
     * @throws IOException
     */
    public static void generateZip(String srcFolder, String outputZip) throws IOException {
        File srcFile = new File(srcFolder);
        ZipUtils zipUtil = new ZipUtils(srcFile.getAbsolutePath());
        zipUtil.generateFileList(srcFile);
        zipUtil.zipIt(outputZip);
    }

    private ZipUtils(String srcFolder) {
        this.srcFolder = srcFolder;
    }

    /**
     * Zip it
     */
    private void zipIt(String outputZip) throws IOException {

        byte[] buffer = new byte[1024];

        FileOutputStream outputZipFileStream = new FileOutputStream(outputZip);
        ZipOutputStream outputZipStream = null;
        try {
            outputZipStream = new ZipOutputStream(outputZipFileStream);

            for (String file : this.fileList) {

                ZipEntry ze = new ZipEntry(file);
                outputZipStream.putNextEntry(ze);

                FileInputStream in = new FileInputStream(srcFolder + File.separator + file);

                int len;
                while ((len = in.read(buffer)) > 0) {
                    outputZipStream.write(buffer, 0, len);
                }

                in.close();
                outputZipStream.closeEntry();
            }
        } finally {
            IOUtils.closeIfNotNull(outputZipStream);
        }
    }

    /**
     * Traverse a directory and get all files,
     * and add the file into fileList
     * @param node file or directory
     */
    private void generateFileList(File node) throws IOException {

        //add file only
        if(node.isFile()){
            fileList.add(generateFileName(node.getAbsoluteFile().toString()));
        }

        if(node.isDirectory()){
            String[] subNote = node.list();
            if(subNote != null) {
                for (String filename : subNote) {
                    generateFileList(new File(node, filename));
                }
            }
        }
    }

    /**
     * Format the file path for zip
     * @param file file path
     * @return Formatted file path
     */
    private String generateFileName(String file){
        return file.substring(srcFolder.length() + File.separator.length(), file.length());
    }
}
