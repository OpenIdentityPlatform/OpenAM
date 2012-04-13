/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011 ForgeRock AS. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 */

package org.forgerock.openam.amsessionstore.db.opendj;

import java.util.Iterator;
import org.opends.server.types.AttributeValue;
import org.forgerock.i18n.LocalizableMessage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import org.forgerock.openam.amsessionstore.common.AMRecord;
import org.forgerock.openam.amsessionstore.common.Constants;
import org.forgerock.openam.amsessionstore.common.Log;
import org.forgerock.openam.amsessionstore.db.DBStatistics;
import org.forgerock.openam.amsessionstore.db.NotFoundException;
import org.forgerock.openam.amsessionstore.db.PersistentStore;
import org.forgerock.openam.amsessionstore.db.StoreException;
import org.opends.server.core.AddOperation;
import org.opends.server.core.DeleteOperation;
import org.opends.server.core.ModifyOperation;
import org.opends.server.protocols.internal.InternalClientConnection;
import org.opends.server.protocols.internal.InternalSearchOperation;
import org.opends.server.protocols.ldap.LDAPModification;
import org.opends.server.types.Attribute;
import org.opends.server.types.DereferencePolicy;
import org.opends.server.types.DirectoryException;
import org.opends.server.types.ModificationType;
import org.opends.server.types.RawAttribute;
import org.opends.server.types.RawModification;
import org.opends.server.types.ResultCode;
import org.opends.server.types.SearchResultEntry;
import org.opends.server.types.SearchScope;
import static org.forgerock.openam.amsessionstore.i18n.AmsessionstoreMessages.*;

/**
 *
 * @author steve
 */
public class OpenDJPersistentStore implements PersistentStore, Runnable {
    private boolean shutdown = false;
    private Thread storeThread;
    private int sleepInterval = 60 * 1000;
    private final static String ID = "OpenDJPersistentStore"; 
    private static InternalClientConnection icConn;
    
    private final static String SKEY_FILTER_PRE = "(sKey=";
    private final static String SKEY_FILTER_POST = ")";
    private final static String EXPDATE_FILTER_PRE = "(expirationDate<=";
    private final static String EXPDATE_FILTER_POST = ")";
    private final static String NUM_SUB_ORD_ATTR = "numSubordinates";
    private final static String ALL_ATTRS = "(" + NUM_SUB_ORD_ATTR + "=*)";
    private static LinkedHashSet<String> returnAttrs;
    private static LinkedHashSet<String> numSubOrgAttrs;
    
    static {
        initialize();
    }
    
    private static void initialize() {
        returnAttrs = new LinkedHashSet<String>();
        returnAttrs.add("dn");
        returnAttrs.add(AMRecordDataEntry.PRI_KEY);
        returnAttrs.add(AMRecordDataEntry.SEC_KEY);
        returnAttrs.add(AMRecordDataEntry.AUX_DATA);
        returnAttrs.add(AMRecordDataEntry.DATA);
        returnAttrs.add(AMRecordDataEntry.EXP_DATE);
        returnAttrs.add(AMRecordDataEntry.EXTRA_BYTE_ATTR);
        returnAttrs.add(AMRecordDataEntry.EXTRA_STRING_ATTR);
        returnAttrs.add(AMRecordDataEntry.OPERATION);
        returnAttrs.add(AMRecordDataEntry.SERVICE);
        returnAttrs.add(AMRecordDataEntry.STATE);
        
        numSubOrgAttrs = new LinkedHashSet<String>();
        numSubOrgAttrs.add(NUM_SUB_ORD_ATTR);
    }
    
    @SuppressWarnings("CallToThreadStartDuringObjectConstruction")
    public OpenDJPersistentStore() 
    throws StoreException {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                internalShutdown();
            }
        });
        
        storeThread = new Thread(this);        
        storeThread.setName(ID);
        storeThread.start();
        initializeOpenDJ();        
        icConn = InternalClientConnection.getRootConnection();
        Log.logger.log(Level.FINE, DB_DJ_STR_OK.get().toString());
    }
    
    private boolean replicationEnabled() {
        boolean multipleServers = false;
        
        try {
            multipleServers = EmbeddedOpenDJ.getServers().size() > 1;
        } catch (StoreException se) {
            Log.logger.log(Level.SEVERE, DB_DJ_SVR_CNT.get().toString(), se);
        }
        
        return multipleServers;
    }
    
    private void initializeOpenDJ() 
    throws StoreException {
        try {
            EmbeddedOpenDJ.startServer(OpenDJConfig.getOdjRoot());
        } catch (Exception ex) {
            Log.logger.log(Level.SEVERE, DB_DJ_NO_START.get().toString(), ex);
            throw new StoreException(DB_DJ_NO_START.get().toString(), ex);
        }
    }
    
    @SuppressWarnings("SleepWhileInLoop")
    @Override
    public void run() {
        while (!shutdown) {
            try {
                Thread.sleep(sleepInterval);
                long curTime = System.currentTimeMillis() / 1000;
                deleteExpired(curTime);
            } catch (InterruptedException ie) {
                Log.logger.log(Level.WARNING, DB_THD_INT.get().toString(), ie);
            } catch (StoreException se) {
                Log.logger.log(Level.WARNING, DB_STR_EX.get().toString(), se);
            }   
        }
    }
    
    @Override
    public void write(AMRecord record) 
    throws StoreException {
        boolean found = false;
        StringBuilder baseDN = new StringBuilder();
        baseDN.append(Constants.AMRECORD_NAMING_ATTR).append(Constants.EQUALS);
        baseDN.append(record.getPrimaryKey()).append(Constants.COMMA);
        baseDN.append(Constants.BASE_DN).append(Constants.COMMA).append(OpenDJConfig.getSessionDBSuffix());
        
        try {
            InternalSearchOperation iso = icConn.processSearch(baseDN.toString(),
                SearchScope.SINGLE_LEVEL, DereferencePolicy.NEVER_DEREF_ALIASES,
                0, 0, false, Constants.FAMRECORD_FILTER , returnAttrs);
            ResultCode resultCode = iso.getResultCode();

            if (resultCode == ResultCode.SUCCESS) {
                final LocalizableMessage message = DB_ENT_P.get(baseDN);
                Log.logger.log(Level.FINE, message.toString());
                found = true;
            } else if (resultCode == ResultCode.NO_SUCH_OBJECT) {
                final LocalizableMessage message = DB_ENT_NOT_P.get(baseDN);
                Log.logger.log(Level.FINE, message.toString());
            } else {
                final LocalizableMessage message = DB_ENT_ACC_FAIL.get(baseDN, resultCode.toString());
                Log.logger.log(Level.WARNING, message.toString());
                throw new StoreException(message.toString());
            }
        } catch (DirectoryException dex) {
            final LocalizableMessage message = DB_ENT_ACC_FAIL2.get(baseDN);
            Log.logger.log(Level.WARNING, message.toString(), dex);
            throw new StoreException(message.toString(), dex);
        }
        
        if (found) {
            update(record);
        } else {
            store(record);
        }
    }
   
    public void store(AMRecord record) 
    throws StoreException {
        AMRecordDataEntry entry = new AMRecordDataEntry(record);
        List<RawAttribute> attrList = entry.getAttrList();
        StringBuilder dn = new StringBuilder();
        dn.append(AMRecordDataEntry.PRI_KEY).append(Constants.EQUALS).append(record.getPrimaryKey());
        dn.append(Constants.COMMA).append(Constants.BASE_DN);
        dn.append(Constants.COMMA).append(OpenDJConfig.getSessionDBSuffix());
        attrList.addAll(AMRecordDataEntry.getObjectClasses());
        AddOperation ao = icConn.processAdd(dn.toString(), attrList);
        ResultCode resultCode = ao.getResultCode();
        
        if (resultCode == ResultCode.SUCCESS) {
            final LocalizableMessage message = DB_SVR_CREATE.get(dn);
            Log.logger.log(Level.FINE, message.toString());
        } else if (resultCode == ResultCode.ENTRY_ALREADY_EXISTS) {
            final LocalizableMessage message = DB_SVR_CRE_FAIL.get(dn);
            Log.logger.log(Level.WARNING, message.toString());
        } else {
            final LocalizableMessage message = DB_SVR_CRE_FAIL2.get(dn, resultCode.toString());
            Log.logger.log(Level.WARNING, message.toString());
            throw new StoreException(message.toString());
        }
    }
    
    protected void update(AMRecord record)
    throws StoreException {
        List<RawModification> modList = createModificationList(record);
        StringBuilder dn = new StringBuilder();
        dn.append(AMRecordDataEntry.PRI_KEY).append(Constants.EQUALS).append(record.getPrimaryKey());
        dn.append(Constants.COMMA).append(Constants.BASE_DN);
        dn.append(Constants.COMMA).append(OpenDJConfig.getSessionDBSuffix());
        
        ModifyOperation mo = icConn.processModify(dn.toString(), modList);
        ResultCode resultCode = mo.getResultCode();
        
        if (resultCode == ResultCode.SUCCESS) {
            final LocalizableMessage message = DB_SVR_MOD.get(dn);
            Log.logger.log(Level.FINE, message.toString());
        } else {
            final LocalizableMessage message = DB_SVR_MOD_FAIL.get(dn, resultCode.toString());
            Log.logger.log(Level.WARNING, message.toString());
            throw new StoreException(message.toString());
        }
    }
    
    private List<RawModification> createModificationList(AMRecord record)
    throws StoreException {
        List<RawModification> mods = new ArrayList<RawModification>();
        AMRecordDataEntry entry = new AMRecordDataEntry(record);
        List<RawAttribute> attrList = entry.getAttrList();
        
        for (RawAttribute attr : attrList) {
            RawModification mod = new LDAPModification(ModificationType.REPLACE, attr);
            mods.add(mod);
        }
                
        return mods;
    }
    
    @Override
    public AMRecord read(String id) 
    throws NotFoundException, StoreException { 
        StringBuilder baseDN = new StringBuilder();
        
        try {
            baseDN.append(Constants.AMRECORD_NAMING_ATTR).append(Constants.EQUALS);
            baseDN.append(id).append(Constants.COMMA).append(Constants.BASE_DN);
            baseDN.append(Constants.COMMA).append(OpenDJConfig.getSessionDBSuffix());
            InternalSearchOperation iso = icConn.processSearch(baseDN.toString(),
                SearchScope.BASE_OBJECT, DereferencePolicy.NEVER_DEREF_ALIASES,
                0, 0, false, Constants.FAMRECORD_FILTER , returnAttrs);
            ResultCode resultCode = iso.getResultCode();

            if (resultCode == ResultCode.SUCCESS) {
                LinkedList searchResult = iso.getSearchEntries();
                
                if (!searchResult.isEmpty()) {
                    SearchResultEntry entry =
                        (SearchResultEntry) searchResult.get(0);
                    List<Attribute> attributes = entry.getAttributes();

                    Map<String, Set<String>> results = 
                            EmbeddedSearchResultIterator.convertLDAPAttributeSetToMap(attributes);
                    AMRecordDataEntry dataEntry = new AMRecordDataEntry("pkey=" + id + "," + baseDN, AMRecord.READ, results);
                    return dataEntry.getAMRecord();
                } else {
                    return null;
                }
            } else if (resultCode == ResultCode.NO_SUCH_OBJECT) {
                final LocalizableMessage message = DB_ENT_NOT_P.get(baseDN);
                Log.logger.log(Level.FINE, message.toString());
                
                return null;
            } else {
                Object[] params = { baseDN, resultCode };
                final LocalizableMessage message = DB_ENT_ACC_FAIL.get(baseDN, resultCode.toString());
                Log.logger.log(Level.WARNING, message.toString());
                throw new StoreException(message.toString());
            }
        } catch (DirectoryException dex) {
            final LocalizableMessage message = DB_ENT_ACC_FAIL2.get(baseDN);
            Log.logger.log(Level.WARNING, message.toString(), dex);
            throw new StoreException(message.toString(), dex);
        }
    }
    
    @Override
    public Set<String> readWithSecKey(String id) 
    throws StoreException, NotFoundException {
        try {
            StringBuilder baseDN = new StringBuilder();
            StringBuilder filter = new StringBuilder();
            filter.append(SKEY_FILTER_PRE).append(id).append(SKEY_FILTER_POST);
            baseDN.append(Constants.BASE_DN).append(Constants.COMMA).append(OpenDJConfig.getSessionDBSuffix());
            InternalSearchOperation iso = icConn.processSearch(baseDN.toString(),
                SearchScope.SINGLE_LEVEL, DereferencePolicy.NEVER_DEREF_ALIASES,
                0, 0, false, filter.toString() , returnAttrs);
            ResultCode resultCode = iso.getResultCode();

            if (resultCode == ResultCode.SUCCESS) {
                LinkedList<SearchResultEntry> searchResult = iso.getSearchEntries();
                
                if (!searchResult.isEmpty()) {
                    Set<String> result = new HashSet<String>();
                    
                    for (SearchResultEntry entry : searchResult) {
                        List<Attribute> attributes = entry.getAttributes();
                        Map<String, Set<String>> results = 
                            EmbeddedSearchResultIterator.convertLDAPAttributeSetToMap(attributes);
                        
                        Set<String> value = results.get(AMRecordDataEntry.DATA);
                        
                        if (value != null && !value.isEmpty()) {
                            for (String v : value) {
                                result.add(v);
                            }
                        }   
                    }

                    final LocalizableMessage message = DB_R_SEC_KEY_OK.get(id, Integer.toString(result.size()));
                    Log.logger.log(Level.FINE, message.toString());
                    
                    return result;
                } else {
                    return null;
                }
            } else if (resultCode == ResultCode.NO_SUCH_OBJECT) {
                final LocalizableMessage message = DB_ENT_NOT_P.get(OpenDJConfig.getSessionDBSuffix());
                Log.logger.log(Level.FINE, message.toString());
                
                return null;
            } else {
                final LocalizableMessage message = DB_ENT_ACC_FAIL.get(OpenDJConfig.getSessionDBSuffix(), resultCode.toString());
                Log.logger.log(Level.WARNING, message.toString());
                throw new StoreException(message.toString());
            }
        } catch (DirectoryException dex) {
            final LocalizableMessage message = DB_ENT_ACC_FAIL2.get(OpenDJConfig.getSessionDBSuffix());
            Log.logger.log(Level.WARNING, message.toString(), dex);
            throw new StoreException(message.toString(), dex);
        }
    }
    
    @Override
    public void delete(String id)
    throws StoreException, NotFoundException {
        StringBuilder dn = new StringBuilder();
        dn.append(AMRecordDataEntry.PRI_KEY).append(Constants.EQUALS).append(id);
        dn.append(Constants.COMMA).append(Constants.BASE_DN);
        dn.append(Constants.COMMA).append(OpenDJConfig.getSessionDBSuffix());
        DeleteOperation dop = icConn.processDelete(dn.toString());
        ResultCode resultCode = dop.getResultCode();
        
        if (resultCode != ResultCode.SUCCESS) {
            final LocalizableMessage message = DB_ENT_DEL_FAIL.get(dn);
            Log.logger.log(Level.WARNING, message.toString());
        }
    }
    
    @Override
    public void deleteExpired(long expDate)
    throws StoreException {
        try {
            StringBuilder baseDN = new StringBuilder();
            StringBuilder filter = new StringBuilder();
            filter.append(EXPDATE_FILTER_PRE).append(expDate).append(EXPDATE_FILTER_POST);
            baseDN.append(Constants.BASE_DN).append(Constants.COMMA).append(OpenDJConfig.getSessionDBSuffix());
            InternalSearchOperation iso = icConn.processSearch(baseDN.toString(),
                SearchScope.SINGLE_LEVEL, DereferencePolicy.NEVER_DEREF_ALIASES,
                0, 0, false, filter.toString() , returnAttrs);
            ResultCode resultCode = iso.getResultCode();

            if (resultCode == ResultCode.SUCCESS) {
                LinkedList<SearchResultEntry> searchResult = iso.getSearchEntries();
                
                if (!searchResult.isEmpty()) {
                    for (SearchResultEntry entry : searchResult) {
                        List<Attribute> attributes = entry.getAttributes();

                        Map<String, Set<String>> results = 
                            EmbeddedSearchResultIterator.convertLDAPAttributeSetToMap(attributes);
                    
                        Set<String> value = results.get(AMRecordDataEntry.PRI_KEY);
                    
                        if (value != null && !value.isEmpty()) {
                            for (String v : value) {
                                try {
                                    delete(v);
                                } catch (NotFoundException nfe) {
                                    final LocalizableMessage message = DB_ENT_NOT_FOUND.get(v);
                                    Log.logger.log(Level.WARNING, message.toString());
                                }
                            }
                        }
                    }
                }
            } else if (resultCode == ResultCode.NO_SUCH_OBJECT) {
                final LocalizableMessage message = DB_ENT_NOT_P.get(OpenDJConfig.getSessionDBSuffix());
                Log.logger.log(Level.FINE, message.toString());
            } else { 
                final LocalizableMessage message = DB_ENT_ACC_FAIL.get(OpenDJConfig.getSessionDBSuffix(), resultCode.toString());
                Log.logger.log(Level.WARNING, message.toString());
                throw new StoreException(message.toString());
            }
        } catch (DirectoryException dex) {
            final LocalizableMessage message = DB_ENT_ACC_FAIL2.get(OpenDJConfig.getSessionDBSuffix());
            Log.logger.log(Level.WARNING, message.toString(), dex);
            throw new StoreException(message.toString(), dex);
        } catch (Exception ex) {
            if (!shutdown) {
                Log.logger.log(Level.WARNING, DB_ENT_EXP_FAIL.get().toString(), ex);
            } else {
                Log.logger.log(Level.FINEST, DB_ENT_EXP_FAIL.get().toString(), ex);
            }          
        }        
    }
    
    @Override
    public Map<String, Long> getRecordCount(String id) 
    throws StoreException {
        try {
            StringBuilder baseDN = new StringBuilder();
            StringBuilder filter = new StringBuilder();
            filter.append(SKEY_FILTER_PRE).append(id).append(SKEY_FILTER_POST);
            baseDN.append(Constants.BASE_DN).append(Constants.COMMA).append(OpenDJConfig.getSessionDBSuffix());
            InternalSearchOperation iso = icConn.processSearch(baseDN.toString(),
                SearchScope.SINGLE_LEVEL, DereferencePolicy.NEVER_DEREF_ALIASES,
                0, 0, false, filter.toString() , returnAttrs);
            ResultCode resultCode = iso.getResultCode();

            if (resultCode == ResultCode.SUCCESS) {
                LinkedList<SearchResultEntry> searchResult = iso.getSearchEntries();
                
                if (!searchResult.isEmpty()) {
                    Map<String, Long> result = new HashMap<String, Long>();
                    
                    for (SearchResultEntry entry : searchResult) {
                        List<Attribute> attributes = entry.getAttributes();
                        Map<String, Set<String>> results = 
                            EmbeddedSearchResultIterator.convertLDAPAttributeSetToMap(attributes);
                        
                        String key = "";
                        Long expDate = new Long(0);
                        
                        Set<String> value = results.get(AMRecordDataEntry.AUX_DATA);
                        
                        if (value != null && !value.isEmpty()) {
                            for (String v : value) {
                                key = v;
                            }
                        } 
                        
                        value = results.get(AMRecordDataEntry.EXP_DATE);
                        
                        if (value != null && !value.isEmpty()) {
                            for (String v : value) {
                                expDate = AMRecordDataEntry.toAMDateFormat(v);
                            }
                        }  
                        
                        result.put(key, expDate);
                    }
                    
                    final LocalizableMessage message = DB_GET_REC_CNT_OK.get(id, Integer.toString(result.size()));
                    Log.logger.log(Level.FINE, message.toString());
                    
                    return result;
                } else {
                    return null;
                }
            } else if (resultCode == ResultCode.NO_SUCH_OBJECT) {
                final LocalizableMessage message = DB_ENT_NOT_P.get(OpenDJConfig.getSessionDBSuffix());
                Log.logger.log(Level.FINE, message.toString());
                
                return null;
            } else {
                final LocalizableMessage message = DB_ENT_ACC_FAIL.get(OpenDJConfig.getSessionDBSuffix(), resultCode.toString());
                Log.logger.log(Level.WARNING, message.toString());
                throw new StoreException(message.toString());
            }
        } catch (DirectoryException dex) {
            final LocalizableMessage message = DB_ENT_ACC_FAIL2.get(OpenDJConfig.getSessionDBSuffix());
            Log.logger.log(Level.WARNING, message.toString(), dex);
            throw new StoreException(message.toString(), dex);
        }
    }
    
    @Override
    public void shutdown() {
        internalShutdown();
        Log.logger.log(Level.FINE, DB_AM_SHUT.get().toString());
    }
    
    @Override
    public DBStatistics getDBStatistics() {
        DBStatistics stats = DBStatistics.getInstance();
        
        try {
            stats.setNumRecords(getNumSubordinates());
        } catch (StoreException se) {
            final LocalizableMessage message = DB_STATS_FAIL.get(se.getMessage());
            Log.logger.log(Level.WARNING, message.toString());
            stats.setNumRecords(-1);
        }
        
        return stats;
    }
    
    protected void internalShutdown() {
        shutdown = true;    
        Log.logger.log(Level.FINE, DB_AM_INT_SHUT.get().toString());
        
        try {
            EmbeddedOpenDJ.shutdownServer(); 
        } catch (Exception ex) {
            Log.logger.log(Level.WARNING, DB_AM_SHUT_FAIL.get().toString(), ex);
        }
    }
    
    protected int getNumSubordinates() 
    throws StoreException {
        int recordCount = -1;
        StringBuilder baseDN = new StringBuilder();
        baseDN.append(Constants.BASE_DN).append(Constants.COMMA).append(OpenDJConfig.getSessionDBSuffix());
        
        try {
            InternalSearchOperation iso = icConn.processSearch(baseDN.toString(),
                SearchScope.BASE_OBJECT, DereferencePolicy.NEVER_DEREF_ALIASES,
                0, 0, false, ALL_ATTRS , numSubOrgAttrs);
            ResultCode resultCode = iso.getResultCode();
            
            if (resultCode == ResultCode.SUCCESS) {
                LinkedList<SearchResultEntry> searchResult = iso.getSearchEntries();
                
                if (!searchResult.isEmpty()) {                    
                    for (SearchResultEntry entry : searchResult) {
                        List<Attribute> attributes = entry.getAttributes();

                        for (Attribute attr : attributes) {
                            if (attr.isVirtual() && attr.getName().equals(NUM_SUB_ORD_ATTR)) {
                                Iterator<AttributeValue> values = attr.iterator();
                                
                                while (values.hasNext()) {
                                    AttributeValue value = values.next();
                                    
                                    try {
                                        recordCount = Integer.parseInt(value.toString());
                                    } catch (NumberFormatException nfe) {
                                        final LocalizableMessage message = DB_STATS_NFS.get(nfe.getMessage());
                                        Log.logger.log(Level.INFO, message.toString());
                                        throw new StoreException(message.toString());
                                    }
                                }
                            }
                        }
                    }
                }
            } else if (resultCode == ResultCode.NO_SUCH_OBJECT) {
                final LocalizableMessage message = DB_ENT_NOT_P.get(OpenDJConfig.getSessionDBSuffix());
                Log.logger.log(Level.FINE, message.toString());
                throw new StoreException(message.toString());
            } else {
                final LocalizableMessage message = DB_ENT_ACC_FAIL.get(OpenDJConfig.getSessionDBSuffix(), resultCode.toString());
                Log.logger.log(Level.WARNING, message.toString());
                throw new StoreException(message.toString());
            }
        } catch (DirectoryException dex) {
            final LocalizableMessage message = DB_ENT_ACC_FAIL2.get(OpenDJConfig.getSessionDBSuffix());
            Log.logger.log(Level.WARNING, message.toString(), dex);
            throw new StoreException(message.toString(), dex);
        }
        
        return recordCount;
    }
}
