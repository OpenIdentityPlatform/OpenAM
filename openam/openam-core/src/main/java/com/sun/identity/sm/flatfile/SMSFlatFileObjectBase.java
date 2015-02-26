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
 * $Id: SMSFlatFileObjectBase.java,v 1.13 2009/10/28 04:24:26 hengming Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.sm.flatfile;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOException;
import com.sun.identity.common.CaseInsensitiveHashSet;
import com.sun.identity.common.CaseInsensitiveProperties;
import com.sun.identity.common.CaseInsensitiveTreeSet;
import com.sun.identity.common.ReaderWriterLock;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.SMSEntry;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.SMSObjectDB;
import com.sun.identity.sm.SMSObjectListener;
import com.sun.identity.sm.SchemaException;
import com.sun.identity.sm.ServiceNotFoundException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.MessageFormat;
import java.text.ParseException;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;
import javax.naming.directory.Attribute;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;

/**
 * This is the base implementation of flat file data store object.
 */
public abstract class SMSFlatFileObjectBase extends SMSObjectDB {
    protected String mRootDir = null;
    protected File mRootDirHandle = null;
    protected String mRootDN = null;
    protected boolean mInitialized = false;
    protected Debug mDebug = null;
    protected ReaderWriterLock mRWLock = new ReaderWriterLock();

    static final String SMS_FLATFILE_ROOTDIR_PROPERTY = 
        "com.sun.identity.sm.flatfile.root_dir";
    static final String DEFAULT_ROOT_DIR = "/var/opt/SUNWam/sms";

    /**
     * Gets the flat file directory from AMConfig.properties,
     * and creates the root directory if it does not exist.
     * This function must be called in a single thread.
     * @throws SMSException if any error occurs.
     */
    private void init() throws SMSException {
        getBaseDirectory();
        loadMapper();
    }
    
    private void getBaseDirectory()
        throws SMSException 
    {
        // get the flat file root dir
        mRootDir = SystemProperties.get(SMS_FLATFILE_ROOTDIR_PROPERTY,
            DEFAULT_ROOT_DIR); 
        // get the default org dn 
        mRootDN = getRootSuffix();
        // look for the object name mapper if any.
        // create the flat file directory up to the org if it doesn't exist.
        mRootDirHandle = new File(mRootDir);
        if (mRootDirHandle.isDirectory()) {
            if (!mRootDirHandle.canRead() || !mRootDirHandle.canWrite()) {
                String errmsg = "SMSFlatFileObject.initialize: " +
                     "cannot read or write to the root directory." + mRootDir;
                mDebug.error(errmsg);
                throw new SMSException(errmsg);
            } else {
                if (mDebug.messageEnabled()) {
                    mDebug.message("SMSFlatFileObject: Root Directory: " 
                        + mRootDir);
                }
            }
        } else {
            if (!mRootDirHandle.mkdirs()) {
                String errmsg = "SMSFlatFileObject.initialize: " +
                      "Cannot create the root directory." + mRootDir;
                mDebug.error(errmsg);
                throw new SMSException(errmsg);
            }
            if (mDebug.messageEnabled()) {
                mDebug.message(
                    "SMSFlatFileObject: Created root directory: "+ mRootDir);
            }
        }
    }
        
    /**
     * Loads properties from the attribute file handle. 
     * @return Properties object of the configuration object.
     * @throws ServiceNotFoundException if the attributes file is not found.
     * @throws SMSException if an IO error occurred while reading the 
     * attributes properties file.
     * @throws SchemaException if a format error occurred while reading the 
     * attributes properties file.
     */
    protected Properties loadProperties(File filehandle, String objName)
        throws SMSException {
        // read file contents into properties and 
        // form the attributes map to be returned from the properties. 
        FileInputStream fileistr = null;

        try {
            fileistr = new FileInputStream(filehandle);
            Properties props = new CaseInsensitiveProperties();
            props.load(fileistr);
            return props;
        }
        catch (FileNotFoundException e) {
            String errmsg = "SMSFlatFileObject.loadProperties: " + objName +
                " File, " + filehandle.getPath() + e.getMessage();
            mDebug.error("SMSFlatFileObject.loadProperties", e);
            throw new ServiceNotFoundException(errmsg);
        } catch (IOException e) {
            String errmsg = "SMSFlatFileObject.loadProperties: " + objName +
                " File, " + filehandle.getPath() + e.getMessage();
            mDebug.error("SMSFlatFileObject.loadProperties", e);
            throw new ServiceNotFoundException(errmsg);
        } catch (IllegalArgumentException e) {
            String errmsg = "SMSFlatFileObject.loadProperties: " + objName +
                " File, " + filehandle.getPath() + e.getMessage();
            mDebug.error("SMSFlatFileObject.loadProperties", e);
            throw new ServiceNotFoundException(errmsg);
        } finally {
            if (fileistr != null) {
                try {
                    fileistr.close();
                } catch (IOException e) {
                    // ignored
                }
            }
        }
    }

    /**
     * Saves properties to the attributes file handle, with given objName 
     * in the file header.
     */
    protected void saveProperties(
        Properties props, 
        File filehandle,
        String header
    ) throws SMSException {
        FileOutputStream fileostr = null;
        try {
            fileostr = new FileOutputStream(filehandle);
            props.store(fileostr, header);
        } catch (FileNotFoundException e) {
            String errmsg = "SMSFlatFileObjectBase.saveProperties: "+
                (header==null ? "" : header + ": ") + " File, " +
                 filehandle.getPath() + ". Exception: "+e.getMessage();
            mDebug.error("SMSFlatFileObjectBase.saveProperties", e);
            throw new ServiceNotFoundException(errmsg);
        } catch (IOException e) {
            String errmsg = "SMSFlatFileObjectBase.saveProperties: "+
                (header==null ? "" : header + ": ") + " File, " +
                 filehandle.getPath() + ". Exception: "+e.getMessage();
            mDebug.error("SMSFlatFileObjectBase.saveProperties", e);
            throw new ServiceNotFoundException(errmsg);
        } finally {
            try {
                fileostr.close();
            } catch (IOException e) {
                // ignored
            }
        }
    }

    /**
     * Converts a Set of values for an attribute into a string, 
     * encoding special characters in the values as necessary.
     */
    protected String toValString(Set vals) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        
        for (Iterator i = vals.iterator(); i.hasNext(); ) {
            String val = (String)i.next();
            
            /*
             * encode any comma's and percent's in case there's more than one 
             * value.
             */
            val = encodeVal(val);

            if (first) {
                first = false;
            } else {
                sb.append(',');
            }
            sb.append(val);
        }
        return sb.toString();
    }

    protected void modifyValues(
        String objName,
        ModificationItem modItem,
        Properties props
    ) {
        Attribute attr = modItem.getAttribute(); // will not be null
        String key = attr.getID(); // will not be null
        try {
            int op = modItem.getModificationOp();
            switch (op) {
            case DirContext.ADD_ATTRIBUTE:
                Set values = toValSet(key, (String)props.get(key));
                for (NamingEnumeration e = attr.getAll(); 
                    e.hasMoreElements();) {
                    values.add(e.nextElement());
                }
                props.put(key, toValString(values));
                break;
            case DirContext.REMOVE_ATTRIBUTE:
                Set val = toValSet(key, (String)props.get(key));
                for (NamingEnumeration e = attr.getAll(); 
                    e.hasMoreElements();) {
                    val.remove(e.nextElement());
                }
                props.put(key, toValString(val));
                break;
            case DirContext.REPLACE_ATTRIBUTE:
                props.put(key, toValString(attr.getAll()));
                break;
            }
        } catch (NamingException e) {
            mDebug.error("SMSFlatFileObjectBase.modifyValues", e);
            throw new IllegalArgumentException(
                "SMSFlatFileObjectBase.modifyValues: " + objName +
                    ": Error modifying attributes: " + e.getMessage());
        }
    }

    /**
     * Converts an enumeration of values for an attribute into a string,
     * encoding special characters in the values as necessary.
     * This is used by the modify() method where the values needs to be 
     * in a particular order.
     */
    protected String toValString(Enumeration en) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        while (en.hasMoreElements()) {
            String val = (String)en.nextElement();
            
            // now append the encoded value to the string of values.
            val = encodeVal(val);
            
            if (first) {
                first = false;
            } else {
                sb.append(',');
            }
            sb.append(val);
        }
        return sb.toString();
    }

    /**
     * Encodes special characters in a value. 
     * percent to %25 and comma to %2C.
     */
    protected String encodeVal(String v) {
        char[] chars = v.toCharArray();
        StringBuilder sb = new StringBuilder(chars.length+20);
        int i = 0, lastIdx = 0;
        for (i = 0; i < chars.length; i++) {
            if (chars[i] == '%') {
                if (lastIdx != i) {
                    sb.append(chars, lastIdx, i-lastIdx);
                }
                sb.append("%25");
                lastIdx = i+1;
            }
            else if (chars[i] == ',') {
                if (lastIdx != i) {
                    sb.append(chars, lastIdx, i-lastIdx);
                }
                sb.append("%2C");
                lastIdx = i+1;
            }
        }
        if (lastIdx != i) {
            sb.append(chars, lastIdx, i-lastIdx);
        }
        return sb.toString();
    }

    /**
     * Converts a string of values from the attributes properties file 
     * to a Set, decoding special characters in each value.
     */
    protected Set toValSet(String attrName, String vals) {
        Set valset = (SMSEntry.isAttributeCaseSensitive(attrName)) ?
            new HashSet() : new CaseInsensitiveHashSet();
        if ((vals != null) && (vals.length() > 0)) { 
            char[] valchars = vals.toCharArray();
            int i, j;

            for (i = 0, j = 0; j < valchars.length; j++) {
                char c = valchars[j];
                if (c == ',') {
                    if (i == j) {
                        i = j +1;
                    } else { // separator found
                        String val = new String(valchars, i, j-i).trim();
                        if (val.length() > 0) {
                            val = decodeVal(val);
                        }
                        valset.add(val);
                        i = j +1;
                    }
                }
            }
            if (j == valchars.length && i < j) {
                String val = new String(valchars, i, j-i).trim();
                if (val.length() > 0) {
                    val = decodeVal(val);
                }
                valset.add(val);
            }
        }
        return valset;
    }

    /** 
     * Decodes a value, %2C to comma and %25 to percent.
     */
    protected String decodeVal(String v) {
        char[] chars = v.toCharArray();
        StringBuilder sb = new StringBuilder(chars.length);
        int i = 0, lastIdx = 0;
        for (i = 0; i < chars.length; i++) {
            if (chars[i] == '%' && i+2 < chars.length && chars[i+1] == '2') {
                if (lastIdx != i) {
                    sb.append(chars, lastIdx, i-lastIdx);
                }
                if (chars[i+2] == 'C') {
                    sb.append(',');
                }
                else if (chars[i+2] == '5') {
                    sb.append('%');
                }
                else {
                    sb.append(chars, i, 3);
                }
                i += 2;
                lastIdx = i+1;
            }
        }
        if (lastIdx != i) {
            sb.append(chars, lastIdx, i-lastIdx);
        }
        return sb.toString();
    }

    /**
     * Returns sub-configuration names
     */
    private Set getSubEntries(
        String objName, 
        String filter,
        String sidFilter, 
        int numOfEntries,
        boolean sortResults,
        boolean ascendingOrder
    )  throws SMSException {
        return (getSubEntries(objName, "ou=" + filter, sidFilter, true,
            numOfEntries, sortResults, ascendingOrder));
    }
    

    /**
     * Constructor for SMSFlatFileObject. 
     */
    protected SMSFlatFileObjectBase() 
        throws SMSException {
    }

    /**
     * Initializes the SMSFlatFileObject: 
     * Gets the flat file directory and default organization DN from 
     * AMConfig.properties, creates the root directory if it does not exist.
     */
    protected synchronized void initialize() 
        throws SMSException
    {
        if (!mInitialized) {
            mDebug = Debug.getInstance("amSMSFlatFiles");
            init(); 
            mInitialized = true;
        }
    }

    /**
     * Returns a Set of sub-entry names that match the given filter. 
     * 
     * @return Set of sub entry names that match the given filter, or an 
     * empty Set if the objName is not found or if no sub entries are found 
     * with the given filter.
     * 
     * @param token Ignored argument. Access check is assumed to have 
     * occurred before reaching this method. 
     * @param objName Name of the configuration object to get sub entries for.
     * Name is expected to be a dn.
     * @param filter Filter of sub entry names to get. Only the wildcard 
     * character '*' is currently supported. 
     * @param numOfEntries Number of entries to return, or 0 to return all 
     * entries. 
     * @param sortResults Whether to sort results. If true will return 
     * a Set that will return entries in a sorted order.
     * @param ascendingOrder Whether the sorted results should be in 
     * alphabetically ascending or decending order. This argument is ignored 
     * if sortResults is false.
     * 
     * @throws IllegalArgumentException if objName or filter is null or empty, 
     * or if numOfEntries is less than 0. 
     * @throws SchemaException if a sub directory name is not in the expected 
     * "ou=..." format.
     */
    public Set subEntries(
        SSOToken token,
        String objName,
        String filter,
        int numOfEntries,
        boolean sortResults,
        boolean ascendingOrder
    ) throws SMSException, SSOException {
        if ((objName == null) || (objName.length() == 0) || 
            (filter == null) || (filter.length() == 0) || (numOfEntries < 0)
        ) {
            throw new IllegalArgumentException(
                "SMSFlatFileObject.subEntries(): "+
                "One or more arguments is null or empty: "+
                "objName ["+objName==null?"null":objName+
                "] filter ]"+filter==null?"null":filter+"]");
        }

        Set subentries = null;
        try {
            subentries = getSubEntries(objName, filter, null, numOfEntries,
                sortResults, ascendingOrder);
        } catch (ServiceNotFoundException e) {
            // return empty set if object does not exist. 
            subentries = new CaseInsensitiveHashSet();
        }

        if (mDebug.messageEnabled()) {
            mDebug.message("SMSFlatFileObject: "+
                "SubEntries search "+ filter + " for " + objName +
                " returned " + subentries.size() + " items");
        }
        return (subentries);
    }

    /**
     * Returns a Set of sub entry names that match the given filter and 
     * the given sun service id filter.  
     * 
     * @return Set of sub entry names that match the given filter and 
     * sun service id filter, or an empty Set if the objName is not found 
     * or if no sub entries are found with the given filters.
     * 
     * @param token Ignored argument. Access check is assumed to have 
     * occurred before reaching this method. 
     * @param objName Name of the configuration object to get sub entries for.
     * Name is expected to be a dn.
     * @param filter Filter of sub entry names to get. Only the wildcard 
     * character '*' is currently supported. 
     * @param sidFilter Filter of Sun Service ID for the sub entries. 
     * @param numOfEntries Number of entries to return, or 0 to return all 
     * entries. 
     * @param sortResults Whether to sort results. If true will return 
     * a Set that will return entries in a sorted order.
     * @param ascendingOrder Whether the sorted results should be in 
     * alphabetically ascending or decending order. This argument is ignored 
     * if sortResults is false.
     * 
     * @throws IllegalArgumentException if objName or filter is null or empty, 
     * or if numOfEntries is less than 0. 
     * @throws SchemaException if a sub directory name is not in the expected 
     * "ou=..." format.
     */
    public Set schemaSubEntries(SSOToken token, String objName, String filter, 
        String sidFilter, int numOfEntries, boolean sortResults, 
        boolean ascendingOrder)
        throws SMSException, SSOException {

        // Check args
        if (objName == null || objName.length() == 0 || 
            filter == null || filter.length() == 0 || 
            sidFilter == null || sidFilter.length() == 0) {
            throw new IllegalArgumentException(
                "SMSFlatFileObject.schemaSubEntries: "+
                "One or more arguments is null or empty.");
        }

        Set subentries = null;
        try {
            subentries = getSubEntries(objName, filter, sidFilter,
                numOfEntries, sortResults, ascendingOrder);
        } catch (ServiceNotFoundException e) {
            // return empty set if service does not exist.
            subentries = new CaseInsensitiveHashSet();
        }

        if (mDebug.messageEnabled()) {
            mDebug.message("SMSFlatFileObject: "+
                          "SchemaSubEntries search "+filter+" for "+objName+
                          " returned "+subentries.size()+" items");
        }

        return (subentries);
    }

    /**
     * Search for a config object with the given filter.
     * Do some cheating here - callers of this method only pass 
     * service name and version in the filter in a ldap filter format. 
     * So return entries matching service name and version in the filter.
     * 
     * @param token Ignored argument. Access check is assumed to have 
     * occurred before reaching this method. 
     * @param objName Name of the configuration object to begin search. Name is 
     * expected to be a dn.
     * @param filter Filter of service name and version. Expected to be in 
     * SMSEntry.FILTER_PATTERN_SERVICE format.
     * @param numOfEntries number of max entries, 0 means unlimited
     * @param timeLimit maximum number of seconds for the search to spend, 0
     * means unlimited
     * @param sortResults <code>true</code> to have result sorted.
     * @param ascendingOrder <code>true</code> to have result sorted in
     * ascending order.
     * @param excludes Set of DNs to excluded.
     * @return a Map of entries (dn's) that match the given filter.
     *
     * @throws IllegalArgumentException if objName or filter is null or empty, 
     * or if filter is not in the expected format.
     */
    public Iterator search(SSOToken token, String objName, String filter,
        int numOfEntries, int timeLimit, boolean sortResults,
        boolean ascendingOrder, Set excludes)
        throws SSOException, SMSException {
        return null;
    }


    /**
     * Search for a config object with the given filter.
     * Do some cheating here - callers of this method only pass 
     * service name and version in the filter in a ldap filter format. 
     * So return entries matching service name and version in the filter.
     * 
     * @param token Ignored argument. Access check is assumed to have 
     * occurred before reaching this method. 
     * @param objName Name of the configuration object to begin search. Name is 
     * expected to be a dn.
     * @param filter Filter of service name and version. Expected to be in 
     * SMSEntry.FILTER_PATTERN_SERVICE format.
     * @param numOfEntries number of max entries, 0 means unlimited
     * @param timeLimit maximum number of seconds for the search to spend, 0
     * means unlimited
     * @param sortResults <code>true</code> to have result sorted.
     * @param ascendingOrder <code>true</code> to have result sorted in
     * ascending order.
     * @return a Set of entries (dn's) that match the given filter.
     *
     * @throws IllegalArgumentException if objName or filter is null or empty, 
     * or if filter is not in the expected format.
     */
    public Set search(SSOToken token, String objName, String filter,
        int numOfEntries, int timeLimit, boolean sortResults,
        boolean ascendingOrder) throws SSOException, SMSException {

        if (objName == null || objName.length() == 0 ||
            filter == null || filter.length() == 0) {
            throw new IllegalArgumentException(
                "SMSFlatFileObject.search: "+
                "One or more arguments is null or empty.");
        }

        try {
            String filterPattern = SMSEntry.getFilterPatternService();
            MessageFormat format = new MessageFormat(filterPattern);
            Object[] args = format.parse(filter);
            if (args.length != 2 || 
                !(args[0] instanceof String) || !(args[1] instanceof String)) {
                throw new IllegalArgumentException(
                        "SMSFlatFile.search: Error parsing filter pattern "+
                        filter);
            }
            String serviceName = (String)args[0];
            String sunservice = (String)args[1];
            String theObjName = "ou="+serviceName+",ou=services,"+mRootDN;
            Set subentries = null;
            subentries = getSubEntries(theObjName, "*", "ou="+sunservice, 
                                        0, false, false);
            return subentries;
        } catch (ParseException e) {
            throw new IllegalArgumentException(
                    "SMSFlatFileObject.search: Unexpected filter pattern "+
                    filter);
        }
    }

    /**
     * Returns the suborganization names. Returns a set of SMSEntry objects
     * that are suborganization names. The paramter <code>numOfEntries</code>
     * identifies the number of entries to return, if <code>0</code>
     * returns all the entries.
     */
    public Set searchSubOrgNames(SSOToken token, String objName, 
        String filter, int numOfEntries, boolean sortResults, 
        boolean ascendingOrder, boolean recursive)
        throws SMSException, SSOException {
        return (searchOrgs(token, objName, filter, numOfEntries,
            sortResults, ascendingOrder, recursive, null, null, null));
    }

    /**
     * Returns the organization names. Returns a set of SMSEntry objects
     * that are organization names. The paramter <code>numOfEntries</code>
     * identifies the number of entries to return, if <code>0</code>
     * returns all the entries.
     */
    public Set searchOrganizationNames(SSOToken token, String objName,
        int numOfEntries, boolean sortResults, boolean ascendingOrder, 
        String serviceName, String attrName, Set values)
        throws SMSException, SSOException {
        // Search for organization names should include the current
        // organization and hence reset the objName to its parent
        int index = objName.indexOf(',');
        if (index != -1) {
            objName = objName.substring(index + 1);
        }
        return (searchOrgs(token, objName, "*", numOfEntries, sortResults,
            ascendingOrder, true, serviceName, attrName, values));
    }

    private Set searchOrgs(
        SSOToken token,
        String objName,
        String filter,
        int numOfEntries,
        boolean sortResults,
        boolean ascendingOrder,
        boolean recursive, 
        String serviceName, 
        String attrName,
        Set values
    ) throws SMSException, SSOException {
        // Check the args
        if ((objName == null) || (objName.length() == 0) ||
            (filter == null) || (filter.length() == 0) || (numOfEntries < 0)
        ) {
            throw new IllegalArgumentException(
                "SMSFlatFileObject.searchOrganizationNames(): "+
                "One or more arguments is null or empty: "+
                "objName ["+objName==null?"null":objName+
                "] filter ]"+filter==null?"null":filter+"]");
        }

        // For org search the filter prefix would be "o="
        // However for root realm it would be "ou=" when search is performed
        String fPrefix = "o=";
        String sidFilter = null;
        
        // If serviceName, attrName and values are not null
        // construct the filename filter
        if ((serviceName != null) && (attrName != null) && (values != null)
            && !values.isEmpty()
        ) {
            sidFilter = serviceName + "-" + attrName + "=" +
                values.iterator().next();
            if (objName.equalsIgnoreCase(mRootDN)) {
                fPrefix = "ou=";
            }
        }

        Set subentries = null;
        if (sortResults) {
            subentries = new CaseInsensitiveTreeSet(ascendingOrder);
        } else {
            subentries = new CaseInsensitiveHashSet();
        }

        try {
            Set entries = getSubEntries(objName, fPrefix + filter, sidFilter,
                false, numOfEntries, sortResults, ascendingOrder);
            // Prefix suborg names with "ou=" and suffix it with ",$objName"
            // to make it a full DN
            for (Iterator i = entries.iterator(); i.hasNext();) {
                String suborg = (String) i.next();
                subentries.add(fPrefix + suborg + "," + objName);
            }
            
            if (recursive) {
                // Get the list if sub-orgs and search
                Set subOrgs = new HashSet();
                if (!filter.equals("*") || (sidFilter != null)) {
                    Set ssubOrgs = getSubEntries(objName, fPrefix + "*",
                        null, false, 0, sortResults, ascendingOrder);
                    for (Iterator i = ssubOrgs.iterator(); i.hasNext();) {
                        String suborg = (String) i.next();
                        subOrgs.add(fPrefix + suborg + "," + objName);
                    }
                } else {
                    subOrgs.addAll(subentries);
                }
                for (Iterator i = subOrgs.iterator(); i.hasNext();) {
                    String subOrgName = (String)i.next();
                    int reqEntries = (numOfEntries == 0) ? numOfEntries :
                        numOfEntries - subentries.size();
                    if (numOfEntries < 0) {
                        break;
                    }
                    Set subsubentries = searchOrgs(token, subOrgName,
                        filter, reqEntries, sortResults, ascendingOrder,
                        recursive, serviceName, attrName, values);
                    subentries.addAll(subsubentries);
                }
            }
        } catch (ServiceNotFoundException e) {
            // return empty set if object does not exist. 
            subentries = new CaseInsensitiveHashSet();
        }

        if (mDebug.messageEnabled()) {
            mDebug.message("SMSFlatFileObject:searchOrgs "+
                "search " + filter + " for " + objName +
                " returned " + subentries.size() + " items");
        }

        return (subentries);
    }

    /**
     * Register a listener.
     * Not yet implemented. 
     */
    public String registerCallbackHandler(SSOToken token,
        SMSObjectListener changeListener) 
        throws SMSException, SSOException {
        // not yet implemented
        return null;
    }

    /**
     * De-Register a listener.
     * Not yet implemented
     */
    public void deregisterCallbackHandler(String id) {
        // not yet implemented
    }

    /**
     * Loads the name mapper, create it if it doesn't exist.
     **/
    abstract void loadMapper()
        throws SMSException;

    /**
     * Real routine to get sub entries, used by subEntries() and 
     * schemaSubEntries(). 
     *
     * @throws ServiceNotFoundException if the configuration object is 
     * not found.
     * @throws SchemaException if a sub directory name is not in the 
     * expected "ou=..." format.
     */
    abstract protected Set getSubEntries(
        String objName,
        String filter,
        String sidFilter, 
        boolean isSubConfig,
        int numOfEntries,
        boolean sortResults, 
        boolean ascendingOrder
    )  throws SMSException;
}
 
