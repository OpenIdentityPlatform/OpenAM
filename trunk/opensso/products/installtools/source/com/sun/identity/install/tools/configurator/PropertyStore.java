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
 * $Id: PropertyStore.java,v 1.2 2008/06/25 05:51:23 qcheng Exp $
 *
 */

package com.sun.identity.install.tools.configurator;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import com.sun.identity.install.tools.util.Debug;
import com.sun.identity.install.tools.util.LocalizedMessage;

/**
 * An abstract class which encapulates the functionality to save/load
 * properties to/from a file.
 */
abstract class PropertyStore {

    abstract protected String getFile();

    abstract protected String getFileHeader();

    abstract protected LocalizedMessage getLoadErrorMessage();

    abstract protected LocalizedMessage getSaveErrorMessage();

    protected void load() throws InstallException {
        FileInputStream fStream = null;
        Debug.log("PropertyStore : Loading the translation properties ..");
        try {
            fStream = new FileInputStream(getFile());
            getProperties().load(fStream);
        } catch (Exception e) {
            Debug.log("PropertyStore : Error loading the properties", e);
            throw new InstallException(getLoadErrorMessage(), e);
        } finally {
            if (fStream != null) {
                try {
                    fStream.close();
                } catch (IOException i) {
                    // Ignore
                }
            }
        }
    }

    protected void save() throws InstallException {
        FileOutputStream fStream = null;
        Debug.log("PropertyStore: Saving the properties into file.");
        try {
            fStream = new FileOutputStream(getFile());
            getProperties().store(fStream, getFileHeader());
        } catch (Exception e) {
            // TODO: >>> Notify Driver >>>>
            Debug.log("PropertyStore : Error saving the properties ", e);
            throw new InstallException(getSaveErrorMessage(), e);
        } finally {
            if (fStream != null) {
                try {
                    fStream.close();
                } catch (IOException i) {
                    // Ignore
                }
            }
        }
    }

    protected String get(String key) {
        return getProperties().getProperty(key);
    }

    protected Properties getProperties() {
        return properties;
    }

    protected void setProperties(Properties properties) {
        this.properties = properties;
    }

    protected void set(String key, String value) {
        getProperties().setProperty(key, value);
    }

    protected Properties properties;
}
