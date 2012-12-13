/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * https://opensso.dev.java.net/public/CDDLv1.0.html or
 * opensso/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at opensso/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: ReplaceTokens.java,v 1.2 2008/06/25 05:51:30 qcheng Exp $
 *
 */

package com.sun.identity.install.tools.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * Class ReplaceTokens has the functionality to scan sourceFile extract tokens,
 * match them with any token replacements in the provided token replacement Map
 * and perform a replacement if a match is found. The entire transformation is
 * written to a specified destination file.
 * 
 */
public final class ReplaceTokens {

    /**
     * Constructs a ReplaceTokens object.
     * 
     * @param sourceFile
     *            the source file whose tokens-tags need to be swapped.
     * @param destFile
     *            the destination file which would be the transformed 
     *            sourceFile with token-tags swapped. transformed
     * @param tokens
     *            a Map containing the token name and the replacement value
     */
    public ReplaceTokens(String sourceFile, String destFile, Map tokens)
            throws Exception {
        setSourceFile(sourceFile);
        setDestinationFile(destFile);
        checkSourceAndDestFiles();
        setTokens(new HashMap());
        getTokens().putAll(tokens);
    }

    /**
     * Method to perform the token-tag swap and create a destination file with
     * the transformation.
     * 
     * @throws Exception
     *             if an error occurrs while performing the transformation.
     */
    public void tagSwapAndCopyFile() throws Exception {

        BufferedReader br = null;
        BufferedWriter bw = null;
        try {
            FileInputStream fi = new FileInputStream(getSourceFile());
            InputStreamReader ir = new InputStreamReader(fi, DEFAULT_ENCODING);
            br = new BufferedReader(ir);

            FileOutputStream fo = new FileOutputStream(getDestinationFile());
            OutputStreamWriter ow = new OutputStreamWriter(fo, 
                    DEFAULT_ENCODING);
            bw = new BufferedWriter(ow);

            // Initalize buffers and re-use to avoid creation of new buffers
            // in the method calls.
            StringBuffer tokenQueue = new StringBuffer(256);
            StringBuffer resultData = new StringBuffer();
            String lineData = null;
            while ((lineData = br.readLine()) != null) {
                String transformedLine = scanAndReplaceTokens(lineData,
                        resultData, tokenQueue);
                bw.write(transformedLine);
            } // End while
        } catch (Exception e) { // Catch the exception. Log it and throw it.
            throw e;
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException ie) {
                    Debug.log("ReplaceTokens.tagSwapAndCopy() - Exception "
                            + "occurred while closing Reader. ", ie);
                }
            }
            if (bw != null) {
                try {
                    bw.flush();
                    bw.close();
                } catch (IOException ie) {
                    Debug.log("ReplaceTokens.tagSwapAndCopy() - Exception "
                            + "occurred while closing Writer. ", ie);
                }
            }
        }
    }

    private void checkSourceAndDestFiles() throws Exception {
        File source = new File(getSourceFile());
        File destination = new File(getDestinationFile());

        if (!source.exists() || !source.canRead()) {
            String message = "Error - Source file '" + getSourceFile()
                    + "' does not exist or unable toread it.";
            Debug.log("ReplaceTokens.checkSourceAndDestFiles() " + message);
            throw new Exception(message);
        } else if (source.equals(destination)) {
            String message = "Error - Source file '" + getSourceFile()
                    + "' and destination file '" + getDestinationFile() + "' "
                    + "cannot be same.";
            Debug.log("ReplaceTokens.checkSourceAndDestFiles() " + message);
            throw new Exception(message);
        }
    }

    private String scanAndReplaceTokens(String lineData,
            StringBuffer resultData, StringBuffer tokenQueue) throws Exception 
    {        
        char[] cbuff = lineData.toCharArray();
        boolean tokenScanningInProgress = false;
        int count = cbuff.length;
        for (int index = 0; index < count; index++) {
            // Check for start or end of token
            if (cbuff[index] == TOKEN_MARKER) {
                if (index > 0 && cbuff[index - 1] == '\\') {
                    // Should ignore \@ and remove the Escape char
                    resultData.deleteCharAt(resultData.length() - 1);
                } else { // Flag the Start or End of token
                    tokenScanningInProgress = !tokenScanningInProgress;
                }
            }

            if (tokenScanningInProgress) {
                // NOTE: Begin 'TOKEN_MARKER' marker will be in the tokenQueue.
                // The End 'TOKEN_MARKER' will not be added.
                tokenQueue.append(cbuff[index]);
                if (cbuff[index] == ' ') { // Not a token Abort scanning!
                    tokenScanningInProgress = !tokenScanningInProgress;
                    resultData.append(tokenQueue);
                    tokenQueue.delete(0, tokenQueue.length());
                }
            } else if (!extractAndWriteToken(resultData, tokenQueue)) {
                // If the tokenQueue had nothing to be written, write the
                // read value form the buffer to the resultData.
                resultData.append(cbuff[index]);
            }
        }

        // We have scanned the complete line. So, the scanning should not be
        // in progress here. If it is, then file tags are not marked correctly.
        // throw an Exception!
        if (tokenScanningInProgress && tokenQueue.length() > 0) {
            // Content is in the StringBuffer which needs to be written
            // to the file.
            // TODO: Delete the destination file !!
            Debug.log("ReplaceTokens.scanAndReplaceTokens() Error: invalid "
                    + "token encountered: " + tokenQueue.toString());
            throw new Exception("Error: invalid token encountered: "
                    + tokenQueue.toString());
        }

        // Append a new line
        resultData.append(LINE_SEP);
        String resultStr = resultData.toString();

        // Clean up the resultData buffer
        resultData.delete(0, resultData.length());

        return resultStr;
    }

    /**
     * Tries to obtain a replace value for the token in tokenQueue with the
     * replace value in the tokens map. The replace value is written to the
     * resultData. If a replace value is not found then the contents of the
     * tokenQueue are written to the resultData. At the end of the operation 
     * the tokenQueue is cleared and made ready for next use.
     */
    private boolean extractAndWriteToken(StringBuffer resultData,
            StringBuffer tokenQueue) {
        // Check if the token exists in StringBuffer extract token
        // swap it and write it to file before the next char is written
        boolean written = false;
        if (tokenQueue.length() > 0) {
            // Ignore start TOKEN_MARKER already added
            String key = tokenQueue.substring(1);
            String replaceValue = (String) getTokens().get(key);
            if (replaceValue == null || replaceValue.trim().length() == 0) {
                // If replace value is not found then re-construct the
                // original token back
                replaceValue = tokenQueue.append(TOKEN_MARKER).toString();
            }
            resultData.append(replaceValue);
            tokenQueue.delete(0, tokenQueue.length()); // Clear the buffer
            written = true;
        }

        return written;
    }

    private Map getTokens() {
        return tokens;
    }

    private String getSourceFile() {
        return sourceFile;
    }

    private String getDestinationFile() {
        return destinationFile;
    }

    private void setTokens(Map tokens) {
        this.tokens = tokens;
    }

    private void setSourceFile(String sourceFile) {
        this.sourceFile = sourceFile;
    }

    private void setDestinationFile(String destFile) {
        destinationFile = destFile;
    }

    private Map tokens;

    private String sourceFile;

    private String destinationFile;

    // Default "begin/end" token character.
    private static final char TOKEN_MARKER = '@';

    private static final String DEFAULT_ENCODING = "ISO-8859-1";

    private static final String LINE_SEP = 
        System.getProperty("line.separator");

}
