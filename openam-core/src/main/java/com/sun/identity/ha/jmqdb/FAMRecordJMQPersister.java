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
 * $Id: FAMRecordJMQPersister.java,v 1.7 2009/11/19 22:34:55 weisun2 Exp $
 *
 */

/*
 * Portions Copyrighted 2010-2012 ForgeRock Inc
 */

package com.sun.identity.ha.jmqdb;

import java.util.Hashtable;
import java.util.HashMap;
import java.util.Random;
import java.util.Vector;
import javax.jms.BytesMessage;
import javax.jms.DeliveryMode;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.Session;
import javax.jms.Topic;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;
import javax.jms.TopicPublisher;
import javax.jms.TopicSession;
import javax.jms.TopicSubscriber;
import java.util.Iterator;
import com.iplanet.dpro.session.service.SessionService;
import com.sun.identity.sm.model.FAMRecord;
import com.sun.identity.ha.FAMRecordPersister;
import com.sun.identity.ha.FAMRecordUtils;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.configuration.SystemPropertiesManager;
import com.iplanet.services.naming.WebtopNaming;
import com.sun.identity.common.ShutdownListener;
import com.sun.identity.common.ShutdownManager;

/**
 * This class <code>FAMRecordJMQPersister</code> implements 
 * </code> MessageListener</code> which is to receive 
 * asynchronously delivered messages. It also sends FAMRecord,  
 * processes return message and reformat it to FAMRecord. 
 */
@Deprecated
public class FAMRecordJMQPersister implements FAMRecordPersister,
    MessageListener { 
     
     /** Represents not found */
    static public final String NOT_FOUND = "notfound";

    /** Represents Operation status */
    static public final String OP_STATUS = "opstatus";
    /* JMQ Queue/Topic names */
    static public final String DBREQUEST = "AM_DBREQUEST";

    static public final String DBRESPONSE = "AM_DRESPONSE";

    /* JMQ Properties */
    static public final String ID = "ID";

    static public int TIMEOUT = 1000;
    
    // Private data members
    private String _id;

    TopicConnectionFactory tFactory = null;

    TopicConnection tConn = null;

    TopicSession tSession = null;

    Topic reqTopic = null;

    Topic resTopic = null;

    TopicPublisher reqPub = null;

    TopicSubscriber resSub = null;

    Hashtable processedMsgs = new Hashtable();

    Random rdmGen = new Random();

    /* Config data */
    int msgcount = 0;

   /**
    *
    * Constructs new FAMRecordJMQPersister
    * @param id SessionId
    *
    */
   public FAMRecordJMQPersister(String id) throws Exception {
        _id = id;
        // Initialize all message queues/topics
        ConnectionFactoryProvider provider = ConnectionFactoryProviderFactory
                .getProvider();
        tFactory = provider.newTopicConnectionFactory();
        int flag = Session.DUPS_OK_ACKNOWLEDGE;

        ShutdownManager shutdownMan = ShutdownManager.getInstance();
        if (shutdownMan.acquireValidLock()) {
            try {
                tConn = tFactory.createTopicConnection();
                tSession = tConn.createTopicSession(false, flag);
                shutdownMan.addShutdownListener(
                    new ShutdownListener() {
                        public void shutdown() {
                            try {
                                tSession.close();
                                tConn.close();
                            } catch (JMSException ex) {
                                FAMRecordUtils.debug.error("Error closing JMS connection", ex);
                            }
                        }
                    }
                );
            } finally {
                shutdownMan.releaseLockAndNotify();
            }
        }
        reqTopic = tSession.createTopic(DBREQUEST);
        resTopic = tSession.createTopic(DBRESPONSE);

        reqPub = tSession.createPublisher(reqTopic);
        String selector = "ID = '" + _id + "'";
        resSub = tSession.createSubscriber(resTopic, selector, true);
        resSub.setMessageListener(this);
        tConn.start();
    }

    private String serverList = null;

    private String userName = null;

    private String password = null;

    // The read timout for retrieving the session (in SFO) needs
    // to be as small as possible since in the case where there
    // is an existing session cookie in client's browser and
    // there is no corresponding session entry in the repository
    // (e.g. timeout), client is forced to wait until this timeout
    // to be able to be redirected back to the login page.
    private int readTimeOut = 5 * 1000; /* 5 sec in millisec */

    // The read timout for getting the session count (for session
    // constraint) is different from the SFO case because the
    // master BDB node will send the response message to the
    // client even though the session count is 0.
    private int readTimeOutForConstraint = 6 * 1000;

   /**
    * Constructs new FAMRecordJMQPersister
    */
    public FAMRecordJMQPersister() throws Exception {
        String thisServerProtocol = SystemPropertiesManager
                .get(Constants.AM_SERVER_PROTOCOL);
        String thisServer = SystemPropertiesManager
                .get(Constants.AM_SERVER_HOST);
        String thisServerPortAsString = SystemPropertiesManager
                .get(Constants.AM_SERVER_PORT);
        String thisURI = SystemPropertiesManager
                .get(Constants.AM_SERVICES_DEPLOYMENT_DESCRIPTOR);

        if (thisServerProtocol == null
                || thisServerPortAsString == null
                || thisServer == null) {
            //TODO: 
            throw new Exception(FAMRecordUtils.bundle.getString(
                "propertyMustBeSet")); 
        }

        _id = WebtopNaming.getServerID(thisServerProtocol,
                thisServer, thisServerPortAsString,
                thisURI);
                   
        // Initialize all message queues/topics
        serverList = SessionService.getSessionExternalRepositoryURL();
        userName =   SessionService.getSessionStoreUserName();
        password =   SessionService.getSessionStorePassword();
        readTimeOut = SessionService.getConnectionMaxWaitTime();
        readTimeOutForConstraint = 
            SessionService.getMaxWaitTimeForConstraint();
        ConnectionFactoryProvider provider = ConnectionFactoryProviderFactory
                .getProvider();
        tFactory = provider
        .newTopicConnectionFactory(serverList,true, true, userName, password);
        int flag = Session.DUPS_OK_ACKNOWLEDGE;

        ShutdownManager shutdownMan = ShutdownManager.getInstance();
        if (shutdownMan.acquireValidLock()) {
            try {
                tConn = tFactory.createTopicConnection();
                tSession = tConn.createTopicSession(false, flag);
                shutdownMan.addShutdownListener(
                    new ShutdownListener() {
                        public void shutdown() {
                            try {
                                tSession.close();
                                tConn.close();
                            } catch (JMSException ex) {
                                FAMRecordUtils.debug.error("Error closing JMS connection", ex);
                            }
                        }
                    }
                );
            } finally {
                shutdownMan.releaseLockAndNotify();
            }
        }
        reqTopic = tSession.createTopic(DBREQUEST);
        resTopic = tSession.createTopic(DBRESPONSE);

        reqPub = tSession.createPublisher(reqTopic);
        reqPub.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
        String selector = "ID = '" + _id + "'";
        resSub = tSession.createSubscriber(resTopic, selector, true);
        resSub.setMessageListener(this); 
        tConn.start();
    }
   
   /** 
    * Passes a message to the listener.
    *
    * @param message the message passed to the listener
    */
   public void onMessage(Message message) {
        try {
            BytesMessage msg = (BytesMessage) message;
            long rndnum = msg.readLong();
            Long random = new Long(rndnum);

            // Determine if we have a read thread waiting...
            Object rnd = processedMsgs.get(random);
            if (rnd != null) {
                processedMsgs.put(rnd, message);
                synchronized (rnd) {
                    rnd.notify();
                }
            }
        } catch (Exception ex) {
            // Since we dont know the thread, not much we can do here -
            // we will just let the thread timeout.
            // TODO Debug.error.
        }
    }
   
   public FAMRecord send(FAMRecord famRecord) throws Exception {
       BytesMessage msg =(BytesMessage) tSession.createBytesMessage();
           
       msg.setStringProperty(ID, _id);
       String op =  famRecord.getOperation();
       msg.setStringProperty("op", op);
       String service = famRecord.getService();
       msg.setStringProperty("service", service);
       
       // Write Primary key   
       String pKey = famRecord.getPrimaryKey(); 
       if (op.equals(FAMRecord.READ)) {
           if (pKey == null || pKey.length() <= 0 || pKey.length() > 256) {
               return null;
           }
       }
       if (pKey != null && (!pKey.equals(""))) {
           msg.writeLong(pKey.length());
           msg.writeBytes(pKey.getBytes());
       }
       //Write expiration date 
       long expirationTime = famRecord.getExpDate(); 
       if (expirationTime > 0) {
           msg.writeLong(expirationTime);
       }
       // Write Secondary Key such as UUID
       String tmp = famRecord.getSecondaryKey();
       if (tmp != null && (!tmp.equals(""))) {
           msg.writeLong(tmp.length());
           msg.writeBytes(tmp.getBytes());
       } else {
           if (op.equals(FAMRecord.WRITE)){
               msg.writeLong(0); 
           }
       }
       // Write AuxData such as Master ID 
       tmp = famRecord.getAuxData();
       if (tmp != null && (!tmp.equals(""))) {
           msg.writeLong(tmp.length());
           msg.writeBytes(tmp.getBytes());
       } else {
           if (op.equals(FAMRecord.WRITE)) {
               msg.writeLong(0);
           }
       }
       int state = famRecord.getState(); 
       if (state > 0) {
           msg.writeInt(state);
       } else {
           if (op.equals(FAMRecord.WRITE)) {
               msg.writeInt(0);
           }
       } 
       
       
       byte[] blob = famRecord.getSerializedInternalSessionBlob();
       if (blob != null) {
           msg.writeLong(blob.length);
           msg.writeBytes(blob);
       }
       // Write extra bytes 
       HashMap mm = famRecord.getExtraByteAttributes();
       Iterator it; 
       if (mm != null) {
           it = mm.keySet().iterator(); 
           while (it.hasNext()) {
           byte[] bt = famRecord.getBytes((String) it.next()); 
           msg.writeLong(bt.length);
           msg.writeBytes(bt);
       }
       }
       // Write extra String 
       mm = famRecord.getExtraStringAttributes(); 
       if (mm != null) {
            it = mm.keySet().iterator();
            String key = null; 
            while (it.hasNext()) {
           key = (String) it.next(); 
           tmp = famRecord.getString(key); 
           msg.setStringProperty(key, tmp);
       }
       }
       // Call for action 
   
       if (op.equals(FAMRecord.DELETE) || op.equals(FAMRecord.DELETEBYDATE)||
           op.equals(FAMRecord.WRITE) || op.equals(FAMRecord.SHUTDOWN)) {
           reqPub.publish(msg);
           return null; 
       } else if (op.equals(FAMRecord.READ)) {
           // Allocate a random string for onMessage to find us.
           Long random = new Long(rdmGen.nextLong());
           processedMsgs.put(random, random);
           msg.writeLong(random.longValue());
           // onMessage thread will wake us up when data is ready
           synchronized (random) {
               reqPub.publish(msg);
               random.wait(readTimeOut);
          }
          // TODO : process timeout
          Object object = processedMsgs.remove(random);
          if(object instanceof Long) {
               //timeout
               return null;
          }
          else {
          	BytesMessage message1 = (BytesMessage) object;
          	String opStatus = message1.getStringProperty(OP_STATUS);
          	if (opStatus != null && opStatus.equals(NOT_FOUND)) {
              		throw new Exception(FAMRecordUtils.bundle.getString(
                  		"notFoundSession"));
          	}
          
          	// Fill in the return value in FAMRecord 
          	// Data is in blob field 
          	long len = message1.readLong();
          	byte[] bytes = new byte[(int) len];
          	message1.readBytes(bytes);
          	FAMRecord ret = new FAMRecord(service,
              		op, pKey, 0, null, 0, null, bytes); 
          	return ret; 
         }
       } else if (op.equals(FAMRecord.GET_RECORD_COUNT)){
           // Allocate a random string for onMessage to find us
           Long random = new Long(rdmGen.nextLong());
           processedMsgs.put(random, random);
           msg.writeLong(random.longValue());
           // onMessage thread will wake us up when data is ready
           synchronized (random) {
               reqPub.publish(msg);
               random.wait(readTimeOutForConstraint);
           }
           Object retMsg = processedMsgs.remove(random);
           BytesMessage message1; 
           if (retMsg instanceof Long) {
               // timeout
               return null;
           } else {
                message1 = (BytesMessage) retMsg;
           }
           //Fill in the return value in FAMRecord 
           int retCount = 0; 
           HashMap aMap = new HashMap(); 
           if (message1 != null) {
               retCount = message1.readInt();
               String hKey = null;
               for (int i = 0; i < retCount; i++) {
                   int len = message1.readInt();
                   byte[] bytes = new byte[len];
                   message1.readBytes(bytes);
                   hKey = new String(bytes);
                   Long expireTime = new Long(message1.readLong());
                   aMap.put(hKey, expireTime);
               }
            }
            FAMRecord ret = new FAMRecord(service,
                op, pKey, 0, null, 0, null, null);
            ret.setStringAttrs(aMap);
            return ret; 
           
       } else if (op.equals(FAMRecord.READ_WITH_SEC_KEY)){
           // Allocate a random string for onMessage to find us
           Long random = new Long(rdmGen.nextLong());
           processedMsgs.put(random, random);
           msg.writeLong(random.longValue());
           // onMessage thread will wake us up when data is ready
           synchronized (random) {
               reqPub.publish(msg);
               random.wait(readTimeOutForConstraint);
           }
           Object retMsg = processedMsgs.remove(random);
           BytesMessage message1; 
           if (retMsg instanceof Long) {
               // timeout
               return null;
           } else {
                message1 = (BytesMessage) retMsg;
           }
           //Fill in the return value in FAMRecord 
          Vector blobs = new Vector();
           if (message1 != null) {
               int retCount = message1.readInt();
               for (int i = 0; i < retCount; i++) {
                   int len = message1.readInt();
                   byte[] bytes = new byte[len];
                   message1.readBytes(bytes);
                   blobs.add(bytes);
               }
            }
            HashMap aMap = new HashMap(); 
            aMap.put("blobs", blobs);
            FAMRecord ret = new FAMRecord(service,
                op, pKey, 0, null, 0, null, null);
            ret.setStringAttrs(aMap);
            return ret; 
       }  
       return null;   
   }

   public void close()
   throws Exception {
        try {
            tSession.close();
            tConn.close();
        } catch (JMSException ex) {
            FAMRecordUtils.debug.error("Error closing JMS connection", ex);
            throw ex;
        }
   }
}
