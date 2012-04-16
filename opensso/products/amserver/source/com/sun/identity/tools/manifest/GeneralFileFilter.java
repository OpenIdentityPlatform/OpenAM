/**
* DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
*
* Copyright (c) 2008 Sun Microsystems, Inc. All Rights Reserved.
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
* $Id: GeneralFileFilter.java,v 1.2 2008/09/04 22:26:12 kevinserwin Exp $
*/

package  com.sun.identity.tools.manifest;

import java.io.File;
import java.io.FileFilter;
import java.util.LinkedList;

public class GeneralFileFilter implements FileFilter, ManifestConstants{

    
    protected LinkedList fileNamePattern;
    protected char wildCard;

    public GeneralFileFilter(LinkedList fileNamePattern){
        this(fileNamePattern, DEFAULT_WILD_CARD);
    }

    /**
     * Constructor accepts a list of pattern and a wildcard character for the
     * pattern.
     *
     * @param fileNamePattern A list of patterns.
     * @param wildCard A character which is used as wildcard character.
     */
    
    public GeneralFileFilter(LinkedList fileNamePattern, char wildCard){
        this.fileNamePattern = fileNamePattern;
        this.wildCard = wildCard;
    }

    /**
     * Overrided method of FileFilter to check whether to accept a path.
     */
    
    public boolean accept(File path){
        if (path.exists()) {
            if (path.isDirectory()) {
                return true;
            } else{
                String fileName = path.getName();
                return Utils.isMatch(fileName, fileNamePattern, wildCard);
            }
        }
        return false;
    }

}
