/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2005 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: SMSFlatFileObject.java,v 1.9 2008/06/25 05:44:09 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.sm.flatfile;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;
import javax.naming.directory.ModificationItem;
import com.sun.identity.sm.SMSEntry;  // for the sunserviceID hack.
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.SchemaException;
import com.sun.identity.sm.ServiceAlreadyExistsException;
import com.sun.identity.sm.ServiceNotFoundException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOException;
import com.sun.identity.common.CaseInsensitiveHashMap;
import com.sun.identity.common.CaseInsensitiveHashSet;
import com.sun.identity.common.CaseInsensitiveProperties;
import com.sun.identity.common.CaseInsensitiveTreeSet;

/**
 * This class represents a configuration object stored in a file.
 * Each file lives in a file system under a directory of configuration 
 * objects organized in a hierarchy. Each level in the hierarchy 
 * is represented by a directory. The naming convention of 
 * a configuration object is hierarchy levels separated by a comma, 
 * for example "ou=serviceName,ou=services,dc=sun,dc=com". 
 * This object would live in the directory 
 * <config-dir>/dc=com/dc=sun/ou=services/ou=serviceName
 * The directory has a file with the object's attributes in 
 * java.util.Properties format. The file name is Attributes.properties.
 * Attributes with multi-values are seperated by a comma. 
 * A comma within a value is encoded as %2C, and a % within a value 
 * is encoded with %25.
 */
public class SMSFlatFileObject extends SMSFlatFileObjectBase {
    private Properties mNameMap = null;
    private File mNameMapHandle = null;
    static final String DEFAULT_NAMEMAP_FILENAME = "NameMap.properties";
    static final String DEFAULT_ATTRIBUTE_FILENAME = "Attributes.properties";

    /**
     * Simple class that looks for subentries with name matching the 
     * filter. Only wildcard '*' character is supported in the filter.
     */
    private class FilenameFilter implements FileFilter {
        // Pattern to match
        Pattern pattern;

        // Default constructor
        public FilenameFilter(String filter) { 
            if (filter != null && filter.length() != 0 &&
                !filter.equals("*")) {
                // Replace "*" with ".*"
                int idx = filter.indexOf('*');
                while (idx != -1) {
                    filter = filter.substring(0, idx) + ".*" +
                        filter.substring(idx + 1);
                    idx = filter.indexOf('*', idx + 2);
                }
                pattern = Pattern.compile(filter.toLowerCase());
            }
        }

        public boolean accept(File file) {
            String filename = file.getName();
            if (pattern == null) {
                // Check for all files
                return (true);
            }
            return (pattern.matcher(filename.toLowerCase()).matches());
        }
    }
    
    /**
     * Loads the name mapper, create it if it doesn't exist.
     **/
    protected void loadMapper()
        throws SMSException
    {
        StringBuffer nameMapFilename = new StringBuffer(mRootDir);
        nameMapFilename.append(File.separatorChar);
        nameMapFilename.append(DEFAULT_NAMEMAP_FILENAME);
        mNameMapHandle = new File(nameMapFilename.toString());
        if (mNameMapHandle.isFile()) {
            if (!mNameMapHandle.canRead()) {
                String errmsg = 
                    "SMSFlatFileObject.initialize: cannot read file "+
                    mNameMapHandle.getPath();
                mDebug.error(errmsg);
                throw new SMSException(errmsg);
            }
            mNameMap = loadProperties(mNameMapHandle, null);
        } else {
            try {
                mNameMapHandle.createNewFile();
            } catch (IOException e) {
                String errmsg = "SMSFlatFileObject.initialize: " +
                     "cannot create file, " + nameMapFilename +
                     ". Exception "+e.getMessage();
                mDebug.error(errmsg);
                throw new SMSException(errmsg);
            } catch (SecurityException e) {
                String errmsg = "SMSFlatFileObject.initialize: " +
                     "cannot create file " + nameMapFilename +
                     ". Exception " + e.getMessage();
                mDebug.error(errmsg);
                throw new SMSException(errmsg);
            }
            mNameMap = new CaseInsensitiveProperties();
            // create root dn if this is a new directory.
            try {
                create(null, mRootDN, new HashMap());
                if (mDebug.messageEnabled()) {
                    mDebug.message("SMSFlatFileObject.initialize: " +
                        "created SMS object for " + mRootDN);
                }
            } catch (SSOException e) {
                // not possible
            } catch (ServiceAlreadyExistsException e) {
                if (mDebug.messageEnabled()) {
                    mDebug.message("SMSFlatFileObject.initialize: " +
                        mRootDN + " already exists");
                }
            }
            
            // also create ou=services this is a new directory.
            try {
                create(null, "ou=services,"+mRootDN, new HashMap());
                if (mDebug.messageEnabled()) {
                    mDebug.message("SMSFlatFileObject.initialize: " +
                        "created SMS object for ou=services,"+
                        mRootDN);
                }
            } catch (SSOException e) {
                // not possible
            } catch (ServiceAlreadyExistsException e) {
                if (mDebug.messageEnabled()) {
                    mDebug.message("SMSFlatFileObject.initialize: "+
                        "ou=services," + mRootDN + " already exists");
                }
            }
        }
    }

    /**
     * Returns a path to object's attributes file.
     */
    private String getAttrFile(String objName) {
        // Check if the name is present in NameMap.properties
        String attrFile = null;
        String objKey = objName.toLowerCase();
        if ((attrFile = mNameMap.getProperty(objKey)) != null) {
            return (attrFile);
        }
        // Check immidiate parent
        int index = objName.indexOf(',');
        if (index != -1) {
            objKey = objKey.substring(index + 1);
            if ((attrFile = mNameMap.getProperty(objKey)) != null) {
                String dirName = objName.substring(0, index).trim();
                StringBuilder sb = new StringBuilder(attrFile.length() +
                    dirName.length() + 2);
                sb.append(attrFile.substring(0, attrFile.length() -
                    DEFAULT_ATTRIBUTE_FILENAME.length()));
                sb.append(FileNameEncoder.encode(dirName));
                sb.append(File.separatorChar);
                sb.append(DEFAULT_ATTRIBUTE_FILENAME);
                return (sb.toString());
            }
        }

        // Construct the file name
        StringBuilder sb =
            new StringBuilder(mRootDir.length()+objName.length()+20);
        sb.append(mRootDir);
        sb.append(File.separatorChar);
        // objName is assumed to be a dn so construct the filepath 
        // backwards from the top of directory tree.
        char[] objchars = objName.toCharArray();
        int i, j;
        for (i = j = objchars.length-1; i >= 0; i--) {
            if (objchars[i] == ',') {
                if (i == j) {
                    j--;
                } else {
                    String rdn = new String(objchars, i+1, j-i).trim();
                    // encode file name in case there are characters 
                    // unsupported in the FS such as '/' or '\' and '*' 
                    // on windows.
                    String encodedRdn = FileNameEncoder.encode(rdn);
                    sb.append(encodedRdn);
                    sb.append(File.separatorChar);
                    j = i-1;
                }
            }
        }
        if (i != j) {
            String lastRdn = new String(objchars, 0, j-i);
            String encodedLastRdn = FileNameEncoder.encode(lastRdn);
            sb.append(encodedLastRdn);
            sb.append(File.separatorChar);
        }
        sb.append(DEFAULT_ATTRIBUTE_FILENAME);
        attrFile = sb.toString();
        return attrFile;
    }

    /**
     * Creates lookup/search files with the given values under the 
     * given directory.
     */ 
    private void createLookupFiles(
        File dirHandle, 
        String attr, 
        Set sunserviceids
    ) throws SMSException {
        StringBuilder sb = new StringBuilder(dirHandle.getPath());
        sb.append(File.separatorChar);
        sb.append(attr);
        sb.append('=');
        String fileprefix = sb.toString();
        
        for (Iterator i = sunserviceids.iterator(); i.hasNext(); ) {
            String id = ((String)i.next()).toLowerCase();
            File idFile = new File(fileprefix + id);
            
            try {
                idFile.createNewFile();
            } catch (IOException e) {
                String errmsg = "SMSFlatFileObject.createLookupIdFiles: " +
                    " File, "+ idFile.getPath() + ". Exception: " +
                    e.getMessage();
                mDebug.error("SMSFlatFileObject.createLookupIdFiles",e);
                throw new SMSException(errmsg);
            }
        }
    }
    
    /**
     * Real routine to get sub entries, used by subEntries() and 
     * schemaSubEntries(). 
     *
     * @throws ServiceNotFoundException if the configuration object is 
     * not found.
     * @throws SchemaException if a sub directory name is not in the 
     * expected "ou=..." format.
     */
    protected Set getSubEntries(
        String objName,
        String filter,
        String sidFilter, 
        boolean isSubConfig,
        int numOfEntries,
        boolean sortResults, 
        boolean ascendingOrder
    )  throws SMSException {
        String objKey = objName.toLowerCase();
        Set subentries = null;
        mRWLock.readRequest();        // wait indefinitely for the read lock.
        
        try {
            // Check if object exists.
            String filepath = mNameMap.getProperty(objKey);
            if (filepath == null) {
                String errmsg = "SMSFlatFileObjectBase.getSubEntries: " +
                     objName + " : not found in objects map.";
                mDebug.warning(errmsg);
                throw new ServiceNotFoundException(errmsg);
            }

            File filehandle = new File(filepath);
            File parentDir = filehandle.getParentFile();
            if (!parentDir.isDirectory()) {  
                String errmsg = "SMSFlatFileObject.getSubEntries: "+
                    objName + " : " + filehandle.getPath() +
                    " does not exist or is not a directory.";
                mDebug.error(errmsg);
                throw new ServiceNotFoundException(errmsg);
            }

            // Create file filter for filter and sid filter.
            FilenameFilter subentFileFilter = new FilenameFilter(filter);
            FilenameFilter sidFileFilter = null;
            if (sidFilter != null && sidFilter.length() > 0) {
                // filter also needs to be encoded since the file names 
                // are encoded. 
                if (isSubConfig) {
                    sidFileFilter = new FilenameFilter(
                        SMSEntry.ATTR_SERVICE_ID + "=" +
                        sidFilter.toLowerCase());
                } else {
                    sidFileFilter = new FilenameFilter(
                        SMSEntry.ATTR_XML_KEYVAL + "=" +
                        sidFilter.toLowerCase());
                }
            }
            
            // Create set for return, use sorted set if sortResults is true.
            if (sortResults) {
                subentries = new CaseInsensitiveTreeSet(ascendingOrder);
            } else {
                subentries = new CaseInsensitiveHashSet();
            }

            // Set all entries that match filter, and that match 
            // sunserviceid/sunxmlkeyvalye if sidFilter was not null.
            File[] subentriesFound = parentDir.listFiles(subentFileFilter);
            int numEntriesAdded = 0;
            boolean done = false;
            
            for (int i = 0; (i < subentriesFound.length) && !done; i++) {
                File[] sunserviceidFiles = null;
                if (sidFileFilter == null || ((sunserviceidFiles =
                    subentriesFound[i].listFiles(sidFileFilter)) != null &&
                    sunserviceidFiles.length > 0)) {
                    String filename = subentriesFound[i].getName();
                    int equalSign = filename.indexOf('=');
                    if (equalSign < 0 || equalSign == (filename.length()-1)) {
                        String errmsg = "SMSFlatFileObject.getSubEntries: "+
                                 "Invalid sub entry name found: "+filename;
                        mDebug.error(errmsg);
                        throw new SchemaException(errmsg);
                    }
                    String subentryname = 
                        FileNameDecoder.decode(filename.substring(equalSign+1));
                    subentries.add(subentryname);
                    numEntriesAdded++;

                    // stop if number of entries requested has been reached.
                    // if sort results, need to get the whole list first.
                    done = !sortResults && (numOfEntries > 0) &&
                        (numEntriesAdded == numOfEntries);
                }
            }

            if (sortResults && (numOfEntries > 0)) {
                // remove extra entries from the bottom.
                while ((numEntriesAdded - numOfEntries) > 0) {
                    Object l = ((CaseInsensitiveTreeSet) subentries).last();
                    subentries.remove(l);
                    numEntriesAdded--;
                }
            }
        } finally {
            mRWLock.readDone();
        }

        return subentries;
    }
    
    
    /**
     * Delete sunxmlkeyvalue files under the given directory.
     */ 
    protected void deleteSunXmlKeyValFiles(File dirHandle) throws SMSException {
        // Construct the file filter and get the files
        StringBuilder sb = new StringBuilder(SMSEntry.ATTR_XML_KEYVAL);
        sb.append("=*");
        FilenameFilter filter = new FilenameFilter(sb.toString());
        File[] deleteFiles = dirHandle.listFiles(filter);
        for (int i = 0; deleteFiles != null && i < deleteFiles.length; i++) {
            File deleteFile = deleteFiles[i];
            deleteFile.delete();
        }
    }

    /**
     * Constructor for SMSFlatFileObject. 
     */
    public SMSFlatFileObject() 
        throws SMSException {
        initialize();
    }


    /**
     * Reads in attributes of a configuration object.
     * 
     * @return A Map with the coniguration object's attributes or null if the 
     * configuration object does not exist or no attributes are found.
     *
     * @param token Ignored argument. Access check is assumed to have 
     * occurred before reaching this method. 
     * @param objName Name of the configuration object, expected to be a dn.
     * 
     * @throws SMSException if an IO error occurred during the read.
     * @throws SchemaException if a format error occurred while reading the 
     * attributes properties file.
     * @throws IllegalArgumentException if objName argument is null or empty.
     */
    public Map read(SSOToken token, String objName)
        throws SMSException, SSOException {

        // check args 
        if (objName == null || objName.length() == 0) {
            throw new IllegalArgumentException(
                "SMSFlatFileObject.read: object name is null or empty.");
        }

        String objKey = objName.toLowerCase();
        Map attrMap = null;
        mRWLock.readRequest();
        try {
            // check if object exists. 
            String filepath = mNameMap.getProperty(objKey);
            if (filepath == null) {
                if (mDebug.messageEnabled()) {
                    mDebug.message("SMSFlatFileObject.read: object " +
                        objName + " not found.");
                }
            } else {
                // Read in file as properties.
                File filehandle = new File(filepath);
                Properties props = null;
                try {
                    props = loadProperties(filehandle, objName);
                } catch (ServiceNotFoundException e) {
                    // props will be null if object does not exist and 
                    // this func subsequently returns null
                }

                // convert each value string to a Set.
                if (props != null) {
                    attrMap = new CaseInsensitiveHashMap();
                    Enumeration keys = props.propertyNames();
                    while (keys.hasMoreElements()) {
                        String key = (String)keys.nextElement();
                        String vals = props.getProperty(key);
                        if ((vals != null) && (vals.length() > 0)) {
                            attrMap.put(key, toValSet(key, vals)); 
                        }
                    }
                }
            }
        }
        finally {
            mRWLock.readDone();
        }

        return attrMap;
    }

    /**
     * Creates the configuration object. Creates the directory for the 
     * object and the attributes properties file with the given attributes.
     * 
     * @param token Ignored argument. Access check is assumed to have 
     * occurred before reaching this method. 
     * @param objName Name of the configuration object to create. Name is 
     * expected to be a dn.
     * @param attrs Map of attributes for the object.
     *
     * @throws IllegalArgumentException if the objName or attrs argument is 
     * null or empty.
     * @throws ServiceAlreadyExistsException if the configuration object 
     * already exists.
     * @throws SMSException if an IO error occurred while creating the 
     * configuration object.
     */
    public void create(SSOToken token, String objName, Map attrs)
        throws SMSException, SSOException {
        if (objName == null || objName.length() == 0 || attrs == null) {
            throw new IllegalArgumentException(
                "SMSFlatFileObject.create: "+
                "One or more arguments is null or empty");
        }

        String objKey = objName.toLowerCase();
        String filepath = null;
        mRWLock.readRequest();
        
        try {
            // Check if object already exists.
            filepath = mNameMap.getProperty(objKey);
            if (filepath != null) {
                String errmsg = "SMSFlatFileObject.create: object " + objName +
                         " already exists in " + filepath;
                mDebug.error(errmsg);
                throw new ServiceAlreadyExistsException(errmsg);
            }
        } finally {
            mRWLock.readDone();
        }

        // Now Create the object.
        mRWLock.writeRequest();
        try {
            filepath = mNameMap.getProperty(objKey); // recheck
            if (filepath != null) {
                String errmsg = "SMSFlatFileObject.create: object " + objName +
                    " already exists in " + filepath;
                mDebug.error(errmsg);
                throw new ServiceAlreadyExistsException(errmsg);
            }
            
            filepath = getAttrFile(objName);
            File filehandle = new File(filepath);
            File parentDir = filehandle.getParentFile();
            if (parentDir.isDirectory()) {
                String errmsg = "SMSFlatFileObject.create: object " + objName +
                    " directory " + parentDir.getPath() +
                    " exists before create!";
                mDebug.error(errmsg);
                throw new ServiceAlreadyExistsException(errmsg);
            }
            
            // Put attrs into in properties format, 
            // replacing any percent's with %25 and commas with %2C
            // in the values. 
            Set sunserviceids = null;
            Set sunxmlkeyvals = null;
            // there's no need for case insensitive properties here since 
            // we are not reading from it. 
            Properties props = new Properties(); 
            Set keys = attrs.keySet();
            
            if (keys != null) {
                for (Iterator i = keys.iterator(); i.hasNext(); ) {
                    String key = (String)i.next();
                    Set vals = (Set)attrs.get(key);
                    if (key.equalsIgnoreCase(SMSEntry.ATTR_SERVICE_ID)) {
                        sunserviceids = vals;  
                    } else if (key.equalsIgnoreCase(SMSEntry.ATTR_XML_KEYVAL)) {
                        sunxmlkeyvals = vals;  
                    }
                    props.put(key, toValString(vals));
                }
            }

            // Create directory, property file, etc. 
            try {
                // create directory
                if (!parentDir.mkdirs()) {
                    String errmsg = "SMSFlatFileObject.create: object " + 
                        objName + ": Could not create directory " +
                        parentDir.getPath();
                    mDebug.error(errmsg);
                    throw new SMSException(errmsg);
                }

                // create the attributes properties file.
                try {
                    if (!filehandle.createNewFile()) {
                        String errmsg = "SMSFlatFileObject.create: object " + 
                            objName + ": Could not create file " + filepath;
                        mDebug.error(errmsg);
                        throw new SMSException(errmsg);
                    }
                }catch (IOException e) {
                    String errmsg = "SMSFlatFileObject.create: object " + 
                        objName + " IOException encountered when creating file "
                        + filehandle.getPath() + ". Exception: " + 
                        e.getMessage();
                    mDebug.error("SMSFlatFileObject.create", e);
                    throw new SMSException(errmsg);
                }
                // write the attributes properties file.
                saveProperties(props, filehandle, objName);
                // create sunserviceid files for faster return in 
                // schemaSubEntries method. 
                if (sunserviceids != null && !sunserviceids.isEmpty()) {
                    createSunServiceIdFiles(parentDir, sunserviceids);
                }
                // create sunxmlkeyvalue files for faster search
                if (sunxmlkeyvals != null && !sunxmlkeyvals.isEmpty()) {
                    createSunXmlKeyValFiles(parentDir, sunxmlkeyvals);
                }
                // add the name in the name map and save.
                mNameMap.setProperty(objKey, filepath);
                saveProperties(mNameMap, mNameMapHandle, null);
            } catch (SMSException e) {
                // If any error occurred, clean up - remove the directory 
                // and files created.
                deleteDir(parentDir); 
                mNameMap.remove(objKey);
                throw e;
            }
        } finally {
            mRWLock.writeDone();
        }
    }

    /**
     * Modify the attributes for the given configuration object.
     * 
     * @param token Ignored argument. Access check is assumed to have 
     * occurred before reaching this method. 
     * @param objName Name of the configuration object to modify. Name is 
     * expected to be a dn.
     * @param mods Array of attributes to modify. 
     * 
     * @throws IllegalArgumentException if objName or mods argument is null or 
     * empty, or if an error was encountered getting attributes from the 
     * mods argument.
     * @throws ServiceNotFoundException if the attributes properties file 
     * for the configuration object is not found.
     * @throws SchemaException if a format error occurred while reading in the 
     * existing attributes properties file.
     * @throws SMSException if an IO error occurred while reading or writing 
     * to the attributes properties file.
     */
    public void modify(SSOToken token, String objName, ModificationItem[] mods) 
        throws SMSException, SSOException {

        if ((objName == null) || (objName.length() == 0) || 
            (mods == null) || (mods.length == 0)
        ) {
            throw new IllegalArgumentException(
                "SMSFlatFileObject.modify: "+
                "One or more arguments is null or empty");
        }

        String objKey = objName.toLowerCase();
        String filepath = null;
        mRWLock.readRequest();
        try {
            // Check if object exists. 
            filepath = mNameMap.getProperty(objKey);
            if (filepath == null) {
                String errmsg = "SMSFlatFileObject.modify: object " +
                     objName + " not found.";
                mDebug.error(errmsg);
                throw new ServiceNotFoundException(errmsg);
            }
        } finally {
            mRWLock.readDone();
        }

        // Now do the modification.
        mRWLock.writeRequest();
        try {
            filepath = mNameMap.getProperty(objKey); // recheck
            if (filepath == null) {
                String errmsg = "SMSFlatFileObject.modify: object " +
                    objName + " not found.";
                mDebug.error(errmsg);
                throw new ServiceNotFoundException(errmsg);
            }
            
            File filehandle = new File(filepath);
            
            if (!filehandle.isFile()) {
                String errmsg = 
                    "SMSFlatFileObject.modify: Attributes file for object "
                    + objName + " not found.";
                mDebug.error(errmsg);
                throw new ServiceNotFoundException(errmsg);
            }
            
            // Read in attributes in existing file first. 
            Properties props = loadProperties(filehandle, objName);
            boolean hasSunXmlKeyValue = (props.getProperty(
                SMSEntry.ATTR_XML_KEYVAL) == null) ? false: true;

            // Replace modification items in attributes properties 
            for (int i = 0; i < mods.length; i++) {
                modifyValues(objName, mods[i], props);
            }

            /*
             * save attributes properties file 
             * sunserviceid's are never modified so don't worry about 
             * renaming them in modify().
             **/
            saveProperties(props, filehandle, objName);

            // Check for sunxmlkeyvalues
            if (!hasSunXmlKeyValue) {
                hasSunXmlKeyValue = (props.getProperty(
                    SMSEntry.ATTR_XML_KEYVAL) == null) ? false: true;
            }
            if (hasSunXmlKeyValue) {
                // Delete the lookup files and recreate them
                deleteSunXmlKeyValFiles(filehandle.getParentFile());
                Set xmlKeyVals = toValSet(SMSEntry.ATTR_XML_KEYVAL,
                    props.getProperty(SMSEntry.ATTR_XML_KEYVAL));
                createSunXmlKeyValFiles(filehandle.getParentFile(), xmlKeyVals);
            }
        } finally {
            mRWLock.writeDone();
        }
    }

    /**
     * Deletes the configuration object and all objects below it.
     * 
     * @param token Ignored argument. Access check is assumed to have 
     * occurred before reaching this method. 
     * @param objName Name of the configuration object to delete. Name is 
     * expected to be a dn.
     * 
     * @throws IllegalArgumentException if objName argument is null or empty.
     * @throws SMSException if any files for or under the configuration object 
     * could not be removed. 
     */
    public void delete(SSOToken token, String objName)
        throws SMSException, SSOException {

        if ((objName == null) || (objName.length() == 0)) {
            throw new IllegalArgumentException(
                "SMSFlatFileObject.delete: object name is null or empty.");
        }

        String objKey = objName.toLowerCase();
        String filepath = null;
        mRWLock.readRequest();
        try {
            // Check if object exists
            filepath = mNameMap.getProperty(objKey);
            if ((filepath == null) && mDebug.messageEnabled()) {
                mDebug.message("SMSFlatFileObject.delete: " + objName +
                    ": object not found.");
            }
        } finally {
            mRWLock.readDone();
        }

        if (filepath != null) {
            mRWLock.writeRequest();
            try {
                filepath = mNameMap.getProperty(objKey); // recheck. 
                if (filepath == null) {
                    if (mDebug.messageEnabled()) {
                        mDebug.message("SMSFlatFileObject.delete: " + objName +
                            ": object not found.");
                    }
                } else {
                    File filehandle = new File(filepath);
                    File parentDir = filehandle.getParentFile();

                    // delete everything from the file dir on. 
                    deleteDir(parentDir);

                    // remove all names from name map under the objname.
                    objName = objName.toLowerCase();
                    Enumeration keysEnum = mNameMap.keys();
                    while (keysEnum.hasMoreElements()) {
                        String key = (String)keysEnum.nextElement();
                        if (key.endsWith(objName)) {
                            mNameMap.remove(key);
                        }
                    }
                    saveProperties(mNameMap, mNameMapHandle, null);
                }
            } finally {
                mRWLock.writeDone();
            }
        }
    }


    /**
     * Returns <code>ture</code> if the configuration object exists. 
     * 
     * @param token Ignored argument. Access check is assumed to have 
     *        occurred before reaching this method. 
     * @param objName Name of the configuration object to check.
     * @return <code>true>/code> if the configuration object exists.
     * @throws IllegalArgumentException if objName is null or empty.
     */
    public boolean entryExists(SSOToken token, String objName) {
        boolean exists = false;

        if (objName == null || objName.length() == 0) {
            throw new IllegalArgumentException(
                "SMSFlatFileObject.entryExists: "+
                "One or more arguments is null or empty.");
        }
        mRWLock.readRequest();

        try {
            String filepath = mNameMap.getProperty(objName.toLowerCase());
            if (filepath != null) {
                exists = true;
            }
        } finally {
            mRWLock.readDone();
        }
        
        return exists;
    }

    /**
     * @return a String representing the name of this class.
     */
    public String toString() {
        return ("SMSFlatFileObject");
    }

    /**
     * Recursively deletes the given directory and all 
     * files and directories underneath.
     */
    private void deleteDir(File dirhandle) 
        throws SMSException {
        File[] files = dirhandle.listFiles();
        // remove all files in each sub-dir
        if (files != null && files.length > 0) {
            for (int i = 0; i < files.length; i++) {
                if (files[i].isDirectory()) {
                    deleteDir(files[i]);
                } else if (!files[i].delete()) {
                    String errmsg = "SMSFlatFileObject.delete: File "+
                                    files[i].getPath()+
                                    " could not be removed!";
                    mDebug.error(errmsg);
                    throw new SMSException(errmsg);
                }
            }
        }
        dirhandle.delete();
    }
    
    
    /**
     * Creates sunxmlkeyvalue files with the given values under the
     * given directory. sunxmlkeyvalue files are created so searching
     * for realms does not have to read in every attribute properrties
     * file and look for the sunxmlkeyvalue attribute. we just need to look
     * for sub directories with the sunxmlkeyvalue file.
     */
    private void createSunXmlKeyValFiles(File dirHandle, Set sunxmlkeyvals)
        throws SMSException {
        createLookupFiles(dirHandle, SMSEntry.ATTR_XML_KEYVAL,
            sunxmlkeyvals);
    }

    /**
     * Creates sunserviceid files with the given values under the 
     * given directory. sunserviceid files are created so getting 
     * schemasubentries does not have to read in every attribute properrties 
     * file and look for the serviceid attribute. we just need to look 
     * for sub directories with the sunserviceid file. 
     */ 
     private void createSunServiceIdFiles(File dirHandle, Set sunserviceids)  
         throws SMSException {
         createLookupFiles(dirHandle, SMSEntry.ATTR_SERVICE_ID,
            sunserviceids);
     }
}
