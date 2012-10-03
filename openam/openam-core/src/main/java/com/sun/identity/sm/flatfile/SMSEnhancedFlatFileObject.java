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
 * $Id: SMSEnhancedFlatFileObject.java,v 1.8 2008/06/25 05:44:08 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.sm.flatfile;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.common.CaseInsensitiveHashMap;
import com.sun.identity.common.CaseInsensitiveHashSet;
import com.sun.identity.common.CaseInsensitiveTreeSet;
import com.sun.identity.sm.SMSEntry;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.SMSObjectListener;
import com.sun.identity.sm.SchemaException;
import com.sun.identity.sm.ServiceAlreadyExistsException;
import com.sun.identity.sm.ServiceNotFoundException;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import javax.naming.directory.ModificationItem;

/**
 * This is an enhancement of <code>SMSEnhancedFlatFileObject</code> where
 * we modified the implementation to avoid hitting the file name length
 * limitation on Windows Operating System.
 *
 * Under the base directory of the datastore, there shall be a XML file,
 * DirectoryTree.xml which contains information on the nodes ofnthe
 * directory tree. The attribute properties on these nodes are also
 * stored under the base directory. The name of this property file is
 * the hash of the distinguished name of the node.
 */
public class SMSEnhancedFlatFileObject extends SMSFlatFileObjectBase {
    private SMSFlatFileTreeNode root = null;
    static final String DIR_TREE_FILENAME = "DirectoryTree.xml";
    private FlatFileEventManager eventManager;
    
    /**
     * Constructor for SMSEnhancedFlatFileObject.
     */
    public SMSEnhancedFlatFileObject()
        throws SMSException {
        initialize();
        eventManager = new FlatFileEventManager(this);
    }
    
    /**
     * Loads the dirrectory mapper, create it if it doesn't exist.
     **/
    synchronized void loadMapper()
        throws SMSException
    {
        String fileName = mRootDir + File.separator + DIR_TREE_FILENAME;
        File fileHandle = new File(fileName);
        
        if (fileHandle.isFile()) {
            if (!fileHandle.canRead()) {
                String errmsg = 
                    "SMSEnhancedFlatFileObject.initialize: cannot read file " +
                    fileName;
                mDebug.error(errmsg);
                throw new SMSException(errmsg);
            }
            parseDirectoryTreeXML(fileName);
        } else {
            try {
                fileHandle.createNewFile();
            } catch (IOException e) {
                String errmsg = "SMSEnhancedFlatFileObject.initialize: " +
                     "cannot create file, " + fileName +
                     ". Exception " + e.getMessage();
                mDebug.error("SMSEnhancedFlatFileObject.initialize", e);
                throw new SMSException(errmsg);
            } catch (SecurityException e) {
                String errmsg = "SMSEnhancedFlatFileObject.initialize: " +
                     "cannot create file, " + fileName +
                     ". Exception " + e.getMessage();
                mDebug.error("SMSEnhancedFlatFileObject.initialize", e);
                throw new SMSException(errmsg);
            }
            
            root = new SMSFlatFileTreeNode(mRootDN);

            try {
                Map map = new HashMap(2);
                Set set = new HashSet(4);
                set.add("top");
                set.add("organizationalunit");
                map.put("objectclass", set);
                create(null, "ou=services," + mRootDN, map);
                saveDirectoryTree();
            } catch (SSOException e) {
                // not possible
            } catch (ServiceAlreadyExistsException e) {
                mDebug.message("SMSEnhancedFlatFileObject.initialize", e);
            }
        }
    }
    
    private void parseDirectoryTreeXML(String filename)
        throws SMSException
    {
        FileReader in = null;
        BufferedReader buff = null;
        try {
            in = new FileReader(filename);
            buff = new BufferedReader(in);
            StringBuilder sb = new StringBuilder();
            String line = buff.readLine();
            while (line != null) {
                sb.append(line);
                line = buff.readLine();
            }
            root = SMSFlatFileTreeNode.createTree(sb.toString(), mDebug);
        } catch (IOException e) {
            throw new SMSException(
                "SMSEnhancedFlatFileObject.parseDirectoryTreeXML, Exception" +
                e.getMessage());
        } catch (Exception e) {
            throw new SMSException(
                "SMSEnhancedFlatFileObject.parseDirectoryTreeXML, Exception" +
                e.getMessage());
        } finally {
            if (buff != null) {
                try {
                    buff.close();
                } catch (IOException e) {
                    //ignored
                }
            }
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    //ignored
                }
            }
        }
    }

    private void saveDirectoryTree()
        throws SMSException
    {
        String fileName = mRootDir + File.separator + DIR_TREE_FILENAME;
        FileOutputStream fout = null;
        OutputStreamWriter writer = null;
        try {
            fout = new FileOutputStream(fileName, false);
            writer = new OutputStreamWriter(fout);
            writer.write(root.toXML());
            writer.flush();
        } catch (FileNotFoundException ex) {
            throw new SMSException(
                "SMSEnhancedFlatFileObject.saveDirectoryTree, Exception" +
                ex.getMessage());
        } catch (IOException ex) {
            throw new SMSException(
                "SMSEnhancedFlatFileObject.saveDirectoryTree, Exception" +
                ex.getMessage());
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException ex) {
                    //ignored
                }
            }
            if (fout != null) {
                try {
                    fout.close();
                } catch (IOException ex) {
                    //ignored
                }
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
            SMSFlatFileTreeNode node = root.getChild(objKey);
            if (node == null) {
                String errmsg = "SMSEnhancedFlatFileObject.getSubEntries: " +
                     objName + " : not found in objects map.";
                mDebug.warning(errmsg);
                throw new ServiceNotFoundException(errmsg);
            }

            // Create file filter for filter and sid filter.
            NodeNameFilter subEntNodeFilter = new NodeNameFilter(filter);
            NodeNameFilter sidNameFilter = getSidNodeFilter(
                sidFilter, isSubConfig);
            
            // Create set for return, use sorted set if sortResults is true.
            if (sortResults) {
                subentries = new CaseInsensitiveTreeSet(ascendingOrder);
            } else {
                subentries = new CaseInsensitiveHashSet();
            }

            // Set all entries that match filter, and that match 
            // sunserviceid/sunxmlkeyvalye if sidFilter was not null.
            Set subEntries = node.searchChildren(subEntNodeFilter, false);
            int numEntriesAdded = 0;
            int sz = subEntries.size();
            boolean done = false;
            
            
            for (Iterator i = subEntries.iterator(); i.hasNext() && !done;) {
                SMSFlatFileTreeNode n = (SMSFlatFileTreeNode)i.next();
                String nodeDN = n.getName();
                boolean accept = (sidNameFilter == null);
                
                if (!accept) {
                    Set sids = n.searchChildren(sidNameFilter, false);
                    accept = (sids != null) && !sids.isEmpty();
                }
                
                if (accept) {
                    int idx = nodeDN.indexOf('=');
                    if ((idx == -1) || (idx == (nodeDN.length()-1))) {
                        String errmsg = 
                            "SMSEnhancedFlatFileObject.getSubEntries: "+
                             "Invalid sub entry name found: " + nodeDN;
                        mDebug.error(errmsg);
                        throw new SchemaException(errmsg);
                    }

                    String subentryname = FileNameDecoder.decode(
                        nodeDN.substring(idx +1));
                    subentries.add(subentryname);
                    numEntriesAdded++;

                    // stop if number of entries requested has been reached.
                    // if sort results, need to get the whole list first.
                    done = !sortResults && (numOfEntries > 0) &&
                        (numEntriesAdded == numOfEntries);
                }
            }
            
            if (sortResults && (numOfEntries > 0)) {
                while ((numEntriesAdded - numOfEntries) > 0) {
                    Object l = ((CaseInsensitiveTreeSet)subentries).last();
                    subentries.remove(l);
                    numEntriesAdded--;
                }
            }
        } finally {
            mRWLock.readDone();
        }

        return subentries;
    }
    
    private NodeNameFilter getSidNodeFilter(
        String sidFilter,
        boolean isSubConfig
    ) {
        NodeNameFilter sidNodeFilter = null;
        if ((sidFilter != null) && (sidFilter.length() > 0)) {
            // filter also needs to be encoded since the file names
            // are encoded.
            if (isSubConfig) {
                sidNodeFilter = new NodeNameFilter(SMSEntry.ATTR_SERVICE_ID + 
                    "=" + sidFilter.toLowerCase());
            } else {
                sidNodeFilter = new NodeNameFilter(SMSEntry.ATTR_XML_KEYVAL + 
                    "=" + sidFilter.toLowerCase());
            }
        }
        return sidNodeFilter;
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
                "SMSEnhancedFlatFileObject.read: object name is null or empty."
                );
        }

        Map attrMap = null;
        mRWLock.readRequest();
        try {
            // check if object exists. 
            String filepath = root.getAttributeFilename(objName, mRootDir);
            if (filepath == null) {
                if (mDebug.messageEnabled()) {
                    mDebug.message("SMSEnhancedFlatFileObject.read: object " +
                        objName + " not found.");
                }
            } else {
                attrMap = Collections.EMPTY_MAP;
                // Read in file as properties.
                File filehandle = new File(filepath);
                Properties props = null;

                if (filehandle.exists()) {
                    try {
                        props = loadProperties(filehandle, objName);
                    } catch (ServiceNotFoundException e) {
                        // props will be null if object does not exist and 
                        // this func subsequently returns null
                    }
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
        if ((objName == null) || (objName.length() == 0) || (attrs == null)) {
            throw new IllegalArgumentException(
                "SMSEnhancedFlatFileObject.create: " +
                "One or more arguments is null or empty");
        }

        String attributeFileName = null;
        mRWLock.readRequest();
        
        try {
            if (root.isExists(mRootDir, objName)) {
                String errmsg = "SMSEnhancedFlatFileObject.create: object " + 
                    objName;
                mDebug.error(errmsg);
                throw new ServiceAlreadyExistsException(errmsg);
            }
        } finally {
            mRWLock.readDone();
        }

        // Now Create the object.
        mRWLock.writeRequest();
        if (root.isExists(mRootDir, objName)) {
            String errmsg = "SMSEnhancedFlatFileObject.create: object " +
                objName;
            mDebug.error(errmsg);
            throw new ServiceAlreadyExistsException(errmsg);
        }

        SMSFlatFileTreeNode node = new SMSFlatFileTreeNode(objName);
        String filepath = node.getAttributeFilename(mRootDir);

        try {
            /*
             * Put attrs into in properties format, replacing any percent's 
             * with %25 and commas with %2C in the values. 
             */
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

            try {
                File attrFile = new File(filepath);
                
                try {
                    if (!attrFile.createNewFile()) {
                        String errmsg = 
                            "SMSEnhancedFlatFileObject.create: object " + 
                            objName + ": Could not create file " + filepath;
                        mDebug.error(errmsg);
                        throw new SMSException(errmsg);
                    }
                } catch (IOException e) {
                    String errmsg = 
                        "SMSEnhancedFlatFileObject.create: object " + 
                        objName + " IOException encountered when creating file "
                         + filepath + ". Exception: " + e.getMessage();
                    mDebug.error("SMSEnhancedFlatFileObject.create", e);
                    throw new SMSException(errmsg);
                }
                // write the attributes properties file.
                saveProperties(props, attrFile, objName);
                createSunServiceIdFiles(node, sunserviceids);
                createSunXmlKeyValFiles(node, sunxmlkeyvals);
                if (!root.addChild(node)) {
                    throw new SMSException(
                        "parent not found for node name=" + objName);
                }
                saveDirectoryTree();
            } catch (SMSException e) {
                File attrFile = new File(filepath);
                try {
                    attrFile.delete();
                } catch (SecurityException se) {
                    //ignored
                }
                throw e;
            }
        } finally {
            mRWLock.writeDone();
        }
    }

    /**
     * Modifies the attributes for the given configuration object.
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
                "SMSEnhancedFlatFileObject.modify: "+
                "One or more arguments is null or empty");
        }

        mRWLock.readRequest();
        
        try {
            if (!root.isExists(mRootDir, objName)) { 
                String errmsg = "SMSEnhancedFlatFileObject.modify: object " +
                     objName + " not found.";
                mDebug.error(errmsg);
                throw new ServiceNotFoundException(errmsg);
            }
        } finally {
            mRWLock.readDone();
        }

        mRWLock.writeRequest();
        try {
            SMSFlatFileTreeNode node = root.getChild(objName);
            if (node == null) {
                String errmsg = "SMSEnhancedFlatFileObject.modify: object " +
                     objName + " not found.";
                mDebug.error(errmsg);
                throw new ServiceNotFoundException(errmsg);
            }
            String filepath = node.getAttributeFilename(mRootDir);
            
            if (filepath == null) {
                String errmsg = "SMSEnhancedFlatFileObject.modify: object " +
                    objName + " not found.";
                mDebug.error(errmsg);
                throw new ServiceNotFoundException(errmsg);
            }
            
            File filehandle = new File(filepath);
            
            if (!filehandle.isFile()) {
                String errmsg = 
                    "SMSEnhancedFlatFileObject.modify: Attributes file for " +
                    "object " + objName + " not found.";
                mDebug.error(errmsg);
                throw new ServiceNotFoundException(errmsg);
            }
            
            // Read in attributes in existing file first. 
            Properties props = loadProperties(filehandle, objName);
            boolean hasSunXmlKeyValue = props.getProperty(
                SMSEntry.ATTR_XML_KEYVAL) != null;

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
            
            String newSunXMLKeyValue = props.getProperty(
                SMSEntry.ATTR_XML_KEYVAL);
            
            if (newSunXMLKeyValue != null) {
                Set xmlKeyVals = toValSet(SMSEntry.ATTR_XML_KEYVAL,
                    newSunXMLKeyValue);
                if (!hasSunXmlKeyValue) {
                    deleteSunXmlKeyValFiles(node);
                }
                createSunXmlKeyValFiles(node, xmlKeyVals);
                saveDirectoryTree();
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
            "SMSEnhancedFlatFileObject.delete: object name is null or empty.");
        }

        mRWLock.writeRequest();
        try {
            SMSFlatFileTreeNode node = root.getChild(objName);
            if (node != null) {
                node.getParentNode().removeChild(node, mRootDir);
                saveDirectoryTree();
            }
        } finally {
            mRWLock.writeDone();
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
                "SMSEnhancedFlatFileObject.entryExists: "+
                "One or more arguments is null or empty.");
        }
        mRWLock.readRequest();

        try {
            exists = root.isExists(mRootDir, objName);
        } finally {
            mRWLock.readDone();
        }
        
        return exists;
    }

    /**
     * @return a String representing the name of this class.
     */
    public String toString() {
        return ("SMSEnhancedFlatFileObject");
    }
    
    /**
     * Delete sunxmlkeyvalue files under the given node.
     */ 
    protected void deleteSunXmlKeyValFiles(SMSFlatFileTreeNode node)
        throws SMSException {
        NodeNameFilter filter = new NodeNameFilter(
            SMSEntry.ATTR_XML_KEYVAL + "=*");
        Set toDelete = node.searchChildren(filter, false);
        
        for (Iterator i = toDelete.iterator(); i.hasNext(); ) {
            SMSFlatFileTreeNode c = (SMSFlatFileTreeNode)i.next();
            node.removeChild(c, mRootDir);
        }
    }
    
    /**
     * Creates sunxmlkeyvalue files with the given values under the
     * given directory. sunxmlkeyvalue files are created so searching
     * for realms does not have to read in every attribute properrties
     * file and look for the sunxmlkeyvalue attribute. we just need to look
     * for sub directories with the sunxmlkeyvalue file.
     */
    private void createSunXmlKeyValFiles(
         SMSFlatFileTreeNode node, 
         Set sunxmlkeyvals
     ) throws SMSException {
        if ((sunxmlkeyvals != null) && !sunxmlkeyvals.isEmpty()) {
            createLookupFiles(node, SMSEntry.ATTR_XML_KEYVAL,
                sunxmlkeyvals);
        }
    }

    /**
     * Creates sunserviceid files with the given values under the 
     * given directory. sunserviceid files are created so getting 
     * schemasubentries does not have to read in every attribute properrties 
     * file and look for the serviceid attribute. we just need to look 
     * for sub directories with the sunserviceid file. 
     */ 
     private void createSunServiceIdFiles(
         SMSFlatFileTreeNode node, 
         Set sunserviceids
     ) throws SMSException {
         if ((sunserviceids != null) && !sunserviceids.isEmpty()) {
             createLookupFiles(node, SMSEntry.ATTR_SERVICE_ID,
                sunserviceids);
         }
     }
     
    private void createLookupFiles(
        SMSFlatFileTreeNode node, 
        String attr, 
        Set sunserviceids
    ) throws SMSException {
        for (Iterator i = sunserviceids.iterator(); i.hasNext(); ) {
            String id = ((String)i.next()).toLowerCase();
            SMSFlatFileTreeNode child = new SMSFlatFileTreeNode(
                attr + "=" + id + "," + node.getDN());
            node.addChild(child);        
        }
    }

    /**
     * Register a listener.
     */
    public String registerCallbackHandler(
        SSOToken token,
        SMSObjectListener changeListener)
        throws SMSException, SSOException {
        return eventManager.addObjectChangeListener(changeListener);
    }

    /**
     * De-Register a listener.
     */
    public void deregisterCallbackHandler(String id) {
        eventManager.removeObjectChangeListener(id);
    }
}
