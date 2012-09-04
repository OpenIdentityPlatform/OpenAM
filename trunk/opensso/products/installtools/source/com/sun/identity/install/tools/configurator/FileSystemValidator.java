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
 * $Id: FileSystemValidator.java,v 1.2 2008/06/25 05:51:18 qcheng Exp $
 *
 */

package com.sun.identity.install.tools.configurator;

import java.io.File;
import java.util.Map;

import com.sun.identity.install.tools.util.Debug;
import com.sun.identity.install.tools.util.LocalizedMessage;

/*
 * Class to validate files, directories etc.
 * 
 */
public class FileSystemValidator extends ValidatorBase {

    public FileSystemValidator() throws Exception {
        super();
    }

    /**
     * 
     * Method isDirectoryValid
     *
     *
     * @param dirname Directory name
     * @param props Map for name value pairs
     * @param state IStateAccess 
     *
     * @return ValidationResult
     *
     */
    public ValidationResult isDirectoryValid(String dirname, Map props,
            IStateAccess state) {

        ValidationResultStatus validRes = ValidationResultStatus.STATUS_FAILED;
        LocalizedMessage returnMessage = null;

        if (isStringValid(dirname)) {
            File dir = new File(dirname);

            if (dir.exists() && dir.isDirectory()) {
                returnMessage = LocalizedMessage.get(LOC_VA_MSG_VAL_DIRECTORY,
                        new Object[] { dirname });
                validRes = ValidationResultStatus.STATUS_SUCCESS;
            }
        }

        if (validRes.getIntValue() == ValidationResultStatus.INT_STATUS_FAILED)
        {
            returnMessage = LocalizedMessage.get(LOC_VA_WRN_IN_VAL_DIRECTORY,
                    new Object[] { dirname });
        }

        Debug.log("FileSystemValidator : Is directory : " + dirname
                + " valid ? " + validRes.isSuccessful());
        return new ValidationResult(validRes, null, returnMessage);
    }

    /**
     * Method isDirectoryReadWrite
     *
     *
     * @param dirname Directory name
     * @param props Map for name value pairs
     * @param state IStateAccess 
     *
     *
     * @return IStateAccess
     *
     */
    public ValidationResult isDirectoryReadWrite(String dirname, Map props,
            IStateAccess state) {

        ValidationResultStatus validRes = ValidationResultStatus.STATUS_FAILED;
        LocalizedMessage returnMessage = null;

        if (isStringValid(dirname)) {
            File dir = new File(dirname);
            if (dir.exists() && dir.isDirectory() && (dir.canRead())
                    && (dir.canWrite())) {
                returnMessage = LocalizedMessage.get(LOC_VA_MSG_DIR_READ_WRITE,
                        new Object[] { dirname });
                validRes = ValidationResultStatus.STATUS_SUCCESS;
            }
        }

        if (validRes.getIntValue() == ValidationResultStatus.INT_STATUS_FAILED)
        {
            returnMessage = LocalizedMessage.get(LOC_VA_WRN_DIR_NOT_READ_WRITE,
                    new Object[] { dirname });
        }

        Debug.log("FileSystemValidator : Is directory : " + dirname
                + " read/writable ? " + validRes.isSuccessful());
        return new ValidationResult(validRes, null, returnMessage);

    }

    /**
     * Method isFileValid
     *
     *
     * @param filename File path
     * @param props Map for name value pairs
     * @param IStateAccess 
     *
     * @return ValidationResult
     *
     */
    public ValidationResult isFileValid(String filename, Map props,
            IStateAccess state) {

        ValidationResultStatus validRes = ValidationResultStatus.STATUS_FAILED;
        LocalizedMessage returnMessage = null;
        if (isStringValid(filename)) {
            File file = new File(filename);
            if (file.exists() && file.isFile()) {
                returnMessage = LocalizedMessage.get(LOC_VA_MSG_VAL_FILE,
                        new Object[] { filename });
                validRes = ValidationResultStatus.STATUS_SUCCESS;
            }
        }

        if (validRes.getIntValue() == ValidationResultStatus.INT_STATUS_FAILED)
        {
            returnMessage = LocalizedMessage.get(LOC_VA_WRN_IN_VAL_FILE,
                    new Object[] { filename });
        }

        Debug.log("FileSystemValidator : Is File : " + filename + " valid ? "
                + validRes.isSuccessful());
        return new ValidationResult(validRes, null, returnMessage);
    }

    /**
     * Method isFileReadable
     *
     *
     * @param filename File path
     * @param props Map for name value pairs
     * @param IStateAccess 
     *
     * @return ValidationResult
     *
     */
    public ValidationResult isFileReadable(String filename, Map props,
            IStateAccess state) {

        ValidationResultStatus validRes = ValidationResultStatus.STATUS_FAILED;
        LocalizedMessage returnMessage = null;
        if (isStringValid(filename)) {
            File file = new File(filename);
            if (file.exists() && file.isFile() && file.canRead()) {
                returnMessage = LocalizedMessage.get(LOC_VA_MSG_FILE_READ,
                        new Object[] { filename });
                validRes = ValidationResultStatus.STATUS_SUCCESS;
            }
        }

        if (validRes.getIntValue() == ValidationResultStatus.INT_STATUS_FAILED)
        {
            returnMessage = LocalizedMessage.get(LOC_VA_WRN_FILE_NOT_READ,
                    new Object[] { filename });
        }

        Debug.log("FileSystemValidator : Is File : " + filename
                + " readable ? " + validRes.isSuccessful());
        return new ValidationResult(validRes, null, returnMessage);
    }

    /**
     * Method isFileWritable
     *
     *
     * @param filename File path
     * @param props Map for name value pairs
     * @param state IStateAccess 
     *
     * @return ValidationResult
     *
     */
    public ValidationResult isFileWritable(String filename, Map props,
            IStateAccess state) {

        ValidationResultStatus validRes = ValidationResultStatus.STATUS_FAILED;
        LocalizedMessage returnMessage = null;

        if (isStringValid(filename)) {
            File file = new File(filename);
            if (file.exists() && file.isFile() && file.canWrite()) {
                returnMessage = LocalizedMessage.get(
                        LOC_VA_MSG_FILE_READ_WRITE, new Object[] { filename });
                validRes = ValidationResultStatus.STATUS_SUCCESS;
            }
        }

        if (validRes.getIntValue() == ValidationResultStatus.INT_STATUS_FAILED)
        {
            returnMessage = LocalizedMessage.get(
                    LOC_VA_WRN_FILE_NOT_READ_WRITE, new Object[] { filename });
        }

        Debug.log("FileSystemValidator : Is file : " + filename
                + " writable ? " + validRes.isSuccessful());
        return new ValidationResult(validRes, null, returnMessage);
    }

    /*
     * Helper function
     */
    private boolean isStringValid(String string) {

        boolean result = false;
        if ((string == null) || (string.trim().length() == 0)) {
            result = false;
        } else {
            result = true;
        }
        return result;
    }

    public void initializeValidatorMap() throws InstallException {

        Class[] paramObjs = { String.class, Map.class, IStateAccess.class };

        try {
            getValidatorMap().put("DIR_EXISTS", 
                    this.getClass().getMethod("isDirectoryValid", paramObjs));

            getValidatorMap().put("DIR_READ_WRITE", 
                    this.getClass().getMethod("isDirectoryReadWrite", 
                            paramObjs));

            getValidatorMap().put("FILE_VALID",
                    this.getClass().getMethod("isFileValid", paramObjs));

            getValidatorMap().put("FILE_WRITABLE",
                    this.getClass().getMethod("isFileWritable", paramObjs));

            getValidatorMap().put("FILE_READABLE",
                    this.getClass().getMethod("isFileReadable", paramObjs));

        } catch (NoSuchMethodException nsme) {
            Debug.log("FileSystemValidator: "
                    + "NoSuchMethodException thrown while loading method :",
                    nsme);
            throw new InstallException(LocalizedMessage
                    .get(LOC_VA_ERR_VAL_METHOD_NOT_FOUND), nsme);
        } catch (SecurityException se) {
            Debug.log("FileSystemValidator: "
                    + "SecurityException thrown while loading method :", se);
            throw new InstallException(LocalizedMessage
                    .get(LOC_VA_ERR_VAL_METHOD_NOT_FOUND), se);
        } catch (Exception ex) {
            Debug.log("FileSystemValidator: "
                    + "Exception thrown while loading method :", ex);
            throw new InstallException(LocalizedMessage
                    .get(LOC_VA_ERR_VAL_METHOD_NOT_FOUND), ex);
        }
    }

    /*
     *  Localized messages
     */
    public static String LOC_VA_MSG_VAL_DIRECTORY = "VA_MSG_VAL_DIRECTORY";

    public static String LOC_VA_WRN_IN_VAL_DIRECTORY = 
        "VA_WRN_IN_VAL_DIRECTORY";

    public static String LOC_VA_MSG_DIR_READ_WRITE = "VA_MSG_DIR_READ_WRITE";

    public static String LOC_VA_WRN_DIR_NOT_READ_WRITE = 
        "VA_WRN_DIR_NOT_READ_WRITE";

    public static String LOC_VA_MSG_VAL_FILE = "VA_MSG_VAL_FILE";

    public static String LOC_VA_WRN_IN_VAL_FILE = "VA_WRN_IN_VAL_FILE";

    public static String LOC_VA_MSG_FILE_READ_WRITE = "VA_MSG_FILE_READ_WRITE";

    public static String LOC_VA_WRN_FILE_NOT_READ_WRITE = 
        "VA_WRN_FILE_NOT_READ_WRITE";

    public static String LOC_VA_MSG_FILE_READ = "VA_MSG_FILE_READ";

    public static String LOC_VA_WRN_FILE_NOT_READ = "VA_WRN_FILE_NOT_READ";
}
