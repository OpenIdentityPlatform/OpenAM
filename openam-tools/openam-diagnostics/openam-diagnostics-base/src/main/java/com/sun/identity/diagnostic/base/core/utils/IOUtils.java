/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: IOUtils.java,v 1.1 2008/11/22 02:19:58 ak138937 Exp $
 *
 */

package com.sun.identity.diagnostic.base.core.utils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.MalformedURLException;

/**
 * This class contains general purpose utility methods for the
 * diagnostic tool application.
 *
 */
public class IOUtils {
    
    public IOUtils() {
    }
    
    /**
     * Converts the given File to a valid URL.
     *
     * @param f File object that needs to be converted to URL.
     * @return URL representing the given file.
     */
    public static URL convertFileToURL(final File f)
    throws MalformedURLException, IOException {
        try {
            return f.getCanonicalFile().toURI().toURL();
        } catch (MalformedURLException mae) {
            throw mae;
        } catch (IOException ioe){
            throw ioe;
        }
    }
}
