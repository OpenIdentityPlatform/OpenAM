/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: GeneralFileFilter.java,v 1.2 2008/06/25 05:44:11 qcheng Exp $
 *
 */

package com.sun.identity.tools.bundles;

import java.io.File;
import java.io.FileFilter;

public class GeneralFileFilter implements FileFilter, SetupConstants{

    protected String fileNamePattern;
    protected char wildCard;

    /**
     * File names filter.
     *
     * @param fileNamePattern The file name pattern with default wildcard.
     */
    
    public GeneralFileFilter(String fileNamePattern) {
        this(fileNamePattern, DEFAULT_WILD_CARD);
    }

    /**
     * File names filter.
     *
     * @param fileNamePattern The file name pattern with customized wildcard.
     * @param wildCard The wildcard character.
     */
    
    public GeneralFileFilter(String fileNamePattern, char wildCard) {
        this.fileNamePattern = fileNamePattern;
        this.wildCard = wildCard;
    }
    
    public boolean accept(File path) {
        if (path.exists()) {
            if (path.isDirectory()) {
                return true;
            } else {
                String tempPattern = fileNamePattern.trim();
                String fileName = path.getName();
                int fileNameOffset = 0;
                boolean matched = true;
                StringBuffer buffer = new StringBuffer();
                for (int i = 0; i < tempPattern.length(); i++) {
                    if (tempPattern.charAt(i) != wildCard) {
                        buffer.append(tempPattern.charAt(i));
                    }
                    if ((i == (tempPattern.length() - 1)) ||
                        (tempPattern.charAt(i) == wildCard)) {
                        if (buffer.length() > 0) {
                            int matchedIndex = fileName.indexOf(
                                buffer.toString(), fileNameOffset);
                        if (matchedIndex >= fileNameOffset) {
                            if (i != (tempPattern.length() - 1)) {
                                fileNameOffset = matchedIndex + buffer.length();
                            } else {
                                if (tempPattern.charAt(i) != wildCard) {
                                    if (fileName.substring(matchedIndex).
                                        length() != buffer.length()) {
                                            matched = false;
                                            break;
                                        }
                                    }
                                }
                            } else {
                                matched = false;
                                break;
                            }
                            buffer = new StringBuffer();
                        }
                    }
                }
                return matched;
            }
        }
        return false;
    }
}
