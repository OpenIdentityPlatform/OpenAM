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

package org.forgerock.openam.amsessionrepository.client;

import com.iplanet.dpro.session.service.SessionService;
import com.sun.identity.ha.FAMRecord;
import com.sun.identity.ha.FAMRecordPersister;
import com.sun.identity.ha.FAMRecordUtils;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.encode.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import org.forgerock.openam.amsessionstore.common.AMRecord;
import org.restlet.Client;
import org.restlet.Context;
import org.restlet.data.Protocol;

/**
 * This is an implementation of the FAMRecordPersister interface. It uses REST
 * calls to send/receive messages to the AMSessionStore server. 
 *
 */
public class AMSessionDBRecordPersister implements FAMRecordPersister {
    private String resourceURL = null;
    private String userName = null;
    private String password = null;
    private int readTimeOut = 5000;
    
    private final static String BLOBS = "blobs";
    private final static Debug debug = FAMRecordUtils.debug;
    
    private ExecutorService threadPool = null;
    private Client client = null;
            
    public AMSessionDBRecordPersister() {
        resourceURL = SessionService.getJdbcURL();
        userName = SessionService.getSessionStoreUserName();
        password = SessionService.getSessionStorePassword();
        readTimeOut = SessionService.getConnectionMaxWaitTime();
        
        threadPool = Executors.newCachedThreadPool();
        
        if (debug.messageEnabled()) {
            debug.message("AMSessionDBRecordPersister created: URL: " +
                    resourceURL + " : username: " + userName + " : password: " +
                    password + " readTimeOut: " + readTimeOut);
        }
        
        client = new Client(new Context(), Protocol.HTTP);
        
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                shutdown();
            }
        });
    }
    
    /**
     * This method is called by OpenAM to send the message to the amsessiondb
     * server. This class makes use of a thread pool to decouple the sending of
     * the messages from the authentication process.
     * 
     * @param famRecord The record to persist
     * @return Some operations return their results in a FAMRecord
     * @throws Exception If something goes wrong
     */
    @Override
    public FAMRecord send(FAMRecord famRecord)
    throws Exception {
        String op =  famRecord.getOperation();

        // Process the operation
        if (op.equals(FAMRecord.DELETE)) {
            if (debug.messageEnabled()) {
                debug.message("AMSessionDBRecordPersister: " + FAMRecord.DELETE);
            }
            
            String recordToDelete = famRecord.getPrimaryKey();
            
            if (recordToDelete == null) {
                debug.error("Unable to delete without a primary key");
                throw new Exception("Unable to delete without a primary key");
            }
            
            Callable<AMRecord> deleteTask = 
                    new DeleteTask(client, resourceURL, userName, password, recordToDelete);
            threadPool.submit(deleteTask);
            
            if (debug.messageEnabled()) {
                debug.message("AMSessionDBRecordPersister: DeleteTasks queued");
            }
        } else if (op.equals(FAMRecord.DELETEBYDATE)) {
            if (debug.messageEnabled()) {
                debug.message("AMSessionDBRecordPersister: " + FAMRecord.DELETEBYDATE);
            }

            long expTime = famRecord.getExpDate(); 

            if (expTime < 0) {
                throw new IllegalArgumentException("Invalid expiration time" + expTime);
            }

            Callable<AMRecord> deleteByDateTask = 
                    new DeleteByDateTask(client, resourceURL, userName, password, expTime);
            threadPool.submit(deleteByDateTask);
            
            if (debug.messageEnabled()) {
                debug.message("AMSessionDBRecordPersister: DeleteByDateTasks queued");
            }
        } else if (op.equals(FAMRecord.WRITE)) {
            if (debug.messageEnabled()) {
                debug.message("AMSessionDBRecordPersister: " + FAMRecord.WRITE);
            }
            
            AMRecord record = new AMRecord();

            record.setOperation(op);
            record.setService(famRecord.getService());

            // Write Primary key   
            String pKey = famRecord.getPrimaryKey(); 

            if (pKey == null || pKey.length() <= 0 || pKey.length() > 256) {
                debug.error("Primary key length is not valid: " + pKey);
                return null;
            }

            if (pKey != null && !pKey.isEmpty()) {
                record.setPrimaryKey(pKey);
            }

            //Write expiration date 
            long expirationTime = famRecord.getExpDate(); 
            if (expirationTime > 0) {
                record.setExpDate(expirationTime);
            }

            // Write Secondary Key such as UUID
            String sKey = famRecord.getSecondarykey(); 

            if (sKey != null && !sKey.isEmpty()) {
                record.setSecondaryKey(sKey);
            } 

            // Write AuxData such as Master ID 
            String auxData = famRecord.getAuxData();
            if (auxData != null && !auxData.isEmpty()) {
                record.setAuxdata(auxData);
            }

            int state = famRecord.getState(); 
            if (state > 0) {
                record.setState(state);
            }

            byte[] blob = famRecord.getBlob(); 
            if (blob != null) {
                String data = Base64.encode(blob);
                record.setData(data);
            }
               
            // Write extra bytes 
            Map<String, byte[]> extraByteAttrs = (Map<String, byte[]>) famRecord.getExtraByteAttributes();

            if (extraByteAttrs != null) {
               for (Map.Entry<String, byte[]> entry : extraByteAttrs.entrySet()) {
                   String data = Base64.encode(entry.getValue());
                   record.setExtraByteAttrs(entry.getKey(), data);
               }
            }

            // Write extra String 
            record.setExtraStringAttrs(famRecord.getExtraStringAttributes());
            
            Callable<AMRecord> writeTask = 
                    new WriteTask(client, resourceURL, userName, password, record);
            threadPool.submit(writeTask);
            
            if (debug.messageEnabled()) {
                debug.message("AMSessionDBRecordPersister: WriteTask queued");
            }
        } else if (op.equals(FAMRecord.SHUTDOWN)) {
            if (debug.messageEnabled()) {
                debug.message("AMSessionDBRecordPersister: " + FAMRecord.SHUTDOWN);
            }
            
            Callable<AMRecord> shutdownTask = 
                    new ShutdownTask(client, resourceURL, userName, password);
            threadPool.submit(shutdownTask);
            
            if (debug.messageEnabled()) {
                debug.message("AMSessionDBRecordPersister: ShutdownTask queued");
            }
        } else if (op.equals(FAMRecord.READ)) {
            if (debug.messageEnabled()) {
                debug.message("AMSessionDBRecordPersister: " + FAMRecord.READ);
            }
            
            String recordToRead = famRecord.getPrimaryKey();
            
            if (recordToRead == null) {
                debug.error("Unable to delete without a primary key");
                throw new IllegalArgumentException("Unable to delete without a primary key");
            }
            
            Callable<AMRecord> readTask = 
                    new ReadTask(client, resourceURL, userName, password, recordToRead);
            FutureTask<AMRecord> result = new FutureTask<AMRecord>(readTask);
            threadPool.execute(result);
            
            if (debug.messageEnabled()) {
                debug.message("AMSessionDBRecordPersister: ReadTask queued");
            }
            
            AMRecord record = result.get(readTimeOut, TimeUnit.MILLISECONDS);
            
            if (debug.messageEnabled()) {
                debug.message("AMSessionDBRecordPersister: ReadTask received: " + record);
            }
            
            return toFAMRecord(record);
        } else if (op.equals(FAMRecord.GET_RECORD_COUNT)) {
            if (debug.messageEnabled()) {
                debug.message("AMSessionDBRecordPersister: " + FAMRecord.GET_RECORD_COUNT);
            }
            
            String pKey = famRecord.getPrimaryKey();
            String sKey = famRecord.getSecondarykey();
            Callable<AMRecord> getRecordCountTask = 
                    new GetRecordCountTask(client, resourceURL, userName, password, pKey, sKey);
            FutureTask<AMRecord> result = new FutureTask<AMRecord>(getRecordCountTask);
            threadPool.execute(result);
            
            if (debug.messageEnabled()) {
                debug.message("AMSessionDBRecordPersister: GetRecordCountTask queued");
            }
                       
            AMRecord record = result.get(readTimeOut, TimeUnit.MILLISECONDS);
            
            if (debug.messageEnabled()) {
                debug.message("AMSessionDBRecordPersister: GetRecordCountTask received: " + record);
            }
            
            return toFAMRecord(record);
        } else if (op.equals(FAMRecord.READ_WITH_SEC_KEY)) {
            if (debug.messageEnabled()) {
                debug.message("AMSessionDBRecordPersister: " + FAMRecord.READ_WITH_SEC_KEY);
            }
            
            String pKey = famRecord.getPrimaryKey();
            String sKey = famRecord.getSecondarykey();
            Callable<AMRecord> readWithSecKeyTask = 
                    new ReadWithSecKeyTask(client, resourceURL, userName, password, pKey, sKey);
            FutureTask<AMRecord> result = new FutureTask<AMRecord>(readWithSecKeyTask);
            threadPool.execute(result);
            
            if (debug.messageEnabled()) {
                debug.message("AMSessionDBRecordPersister: ReadWithSecKeyTask queued");
            }
            
            AMRecord record = result.get(readTimeOut, TimeUnit.MILLISECONDS);
            
            if (debug.messageEnabled()) {
                debug.message("AMSessionDBRecordPersister: ReadWithSecKeyTask received: " + record);
            }
            
            return toFAMRecord(record);
        } 
        
        return null;
    }

    /**
     * No implementation required with this implementation of FAMRecordPersister
     * 
     * @throws Exception 
     */
    public void close()
    throws Exception {
        // no implementation required
    }
    
    /**
     * Called by the JVM shutdown hook, shuts down the thread pool
     */
    protected void shutdown() {
        threadPool.shutdown();
    }
    
    //Cumbersome code, read TODO on AMRecord    
    /**
     * For a set of operations turns an AMRecord into a FAMRecord
     * 
     * @param record The AMRecord to convert
     * @param blobs The Vector of sessions
     * @return The FAMRecord object
     * @throws Exception If the incoming record is invalid
     */
    @SuppressWarnings("UseOfObsoleteCollectionType")
    protected FAMRecord toFAMRecord(AMRecord record)
    throws Exception {
        FAMRecord result = null;
        
        String service = record.getService();
        
        if (service == null || service.isEmpty()) {
            throw new Exception("Service cannot be null");
        }
        
        String operation = record.getOperation();
        
        if (operation == null || operation.isEmpty()) {
            throw new Exception("Operation cannot be null");
        }
        
        String pKey = record.getPrimaryKey();

        if (pKey == null || pKey.isEmpty()) {
            throw new Exception("Primary key cannot be null");
        }
       
        if (operation.equals(FAMRecord.READ)) {
            String data = record.getData();
            byte[] blob = null;
            
            if (data != null) {
                blob = Base64.decode(data);
            } else {
                debug.error("Data is null during READ");
            }
            
            result = new FAMRecord(service, operation, pKey, 0, null, 0, null, blob);
        } else if (operation.equals(FAMRecord.GET_RECORD_COUNT)) {
            result = new FAMRecord(service, operation, pKey, 0, null, 0, null, null);
            result.setStringAttrs(new HashMap(record.getExtraStringAttributes()));
        } else if (operation.equals(FAMRecord.READ_WITH_SEC_KEY)) {
            Vector<String> sessions = record.getRecords();
            
            if (sessions == null) {
                throw new Exception("blobs cannot be null");
            }
            
            result = new FAMRecord(service, operation, pKey, 0, null, 0, null, null);
            HashMap<String, Vector<String>> blobs = new HashMap<String, Vector<String>>();
            blobs.put(BLOBS, sessions);
            result.setStringAttrs(blobs);
        } else {
            throw new Exception("Unsupported operation " + record.getOperation());
        }
        
        return result;
    }
}
