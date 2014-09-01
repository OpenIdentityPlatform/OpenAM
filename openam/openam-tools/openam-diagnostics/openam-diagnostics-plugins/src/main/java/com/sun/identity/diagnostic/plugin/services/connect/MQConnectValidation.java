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
 * $Id: MQConnectValidation.java,v 1.1 2008/11/22 02:41:20 ak138937 Exp $
 *
 */

/**
 * Portions copyright 2013 ForgeRock, Inc.
 */

package com.sun.identity.diagnostic.plugin.services.connect;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.services.naming.WebtopNaming;
import com.iplanet.sso.SSOToken;
import com.sun.identity.common.configuration.ServerConfiguration;
import com.sun.identity.common.configuration.SiteConfiguration;
import com.sun.identity.diagnostic.base.core.log.IToolOutput;
import com.sun.identity.diagnostic.plugin.services.common.ServiceBase;
import com.sun.identity.shared.datastruct.CollectionHelper;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;
import com.sun.identity.sm.ldap.api.CoreTokenConstants;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.TextMessage;
import javax.jms.Topic;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;
import javax.jms.TopicPublisher;
import javax.jms.TopicSession;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;


/**
 * This is a supporting class to validate the Session Failover
 * connection properties.
 */
public class MQConnectValidation extends ServiceBase implements 
    javax.jms.MessageListener , IConnectService {
    
    private IToolOutput toolOutWriter;
    private String serverList = null;
    private String userName = null;
    private String password = null;
    private TopicSession pubSession;
    private TopicPublisher publisher;
    private TopicConnection tConn;
    
    public MQConnectValidation() {
    }
    
    /**
     * Validate the configuration.
     *
     * @param path Configuration directory path location
     */
    public void testConnection(String path) {
        SSOToken ssoToken = null;
        toolOutWriter = ServerConnectionService.getToolWriter();
        if (loadConfig(path)) {
            ssoToken = getAdminSSOToken();
            if (ssoToken != null) {
                processSFO(ssoToken);
            } else {
                toolOutWriter.printError("cnt-auth-msg");
            }
        } else {
            toolOutWriter.printStatusMsg(false, "sfo-validate-cfg-prop");
        }
    }
    
    private boolean loadConfig(String path) {
        boolean loaded = false;
        try {
            if (!loadConfigFromBootfile(path).isEmpty()) {
                loaded = true;
            }
        } catch (Exception e) {
            toolOutWriter.printError("cannot-load-properties" ,
                new String[] {path});
        }
        return loaded;
    }
    
    private void processSFO(SSOToken ssoToken) {
        boolean isSessionFailoverEnabled = false;
        String sessionStoreUserName = null;
        String sessionStorePassword = null;
        int connectionMaxWaitTime = 5000; // in milli-second
        String jdbcDriverClass = null;
        String jdbcURL = null;
        int minPoolSize = 8;
        int maxPoolSize = 32;
        int maxWaitTimeForConstraint = 6000; // in milli-second
        boolean isPropertyNotificationEnabled = false;
        Map <String, String> mqParams = new HashMap<String, String>();
        toolOutWriter.printMessage("sfo-validate-cfg-prop");
        
        try {
            ServiceConfigManager scm = new ServiceConfigManager(
                amSessionService, ssoToken);
            ServiceConfig serviceConfig = scm.getGlobalConfig(null);
            
            String subCfgName = (ServerConfiguration.isLegacy(ssoToken)) ?
                getSessionServerURL() :
                SiteConfiguration.getSiteIdByURL(ssoToken, 
                    getSessionServerURL());
            ServiceConfig subConfig = serviceConfig.getSubConfig(subCfgName);
            String[] params = {getSessionServerURL()};
            if ((subConfig != null)) {
                Map sessionAttrs = subConfig.getAttributes();
                boolean sfoEnabled = Boolean.valueOf(
                    CollectionHelper.getMapAttr(
                    sessionAttrs, CoreTokenConstants.IS_SFO_ENABLED, "false")
                    ).booleanValue();
                if (sfoEnabled) {
                    isSessionFailoverEnabled = true;
                    toolOutWriter.printMessage("sfo-cfg-enabled", params);
                    sessionStoreUserName = CollectionHelper.getMapAttr(
                        sessionAttrs, SESSION_STORE_USERNAME, "amsvrusr");
                    mqParams.put(SESSION_STORE_USERNAME, sessionStoreUserName);
                    sessionStorePassword = CollectionHelper.getMapAttr(
                        sessionAttrs, SESSION_STORE_PASSWORD, "password");
                    mqParams.put(SESSION_STORE_PASSWORD, sessionStorePassword);
                    connectionMaxWaitTime = Integer.parseInt(
                        CollectionHelper.getMapAttr(
                        sessionAttrs, CONNECT_MAX_WAIT_TIME, "5000"));
                    String connectionMaxWaitTimeStr =
                        CollectionHelper.getMapAttr(
                        sessionAttrs, CONNECT_MAX_WAIT_TIME, "5000");
                    mqParams.put(CONNECT_MAX_WAIT_TIME, 
                        connectionMaxWaitTimeStr);
                    jdbcDriverClass = CollectionHelper.getMapAttr(
                        sessionAttrs, JDBC_DRIVER_CLASS, "");
                    mqParams.put(JDBC_DRIVER_CLASS, jdbcDriverClass);
                    jdbcURL = CollectionHelper.getMapAttr(
                        sessionAttrs, IPLANET_AM_SESSION_REPOSITORY_URL, "");
                    mqParams.put(IPLANET_AM_SESSION_REPOSITORY_URL, jdbcURL);
                    minPoolSize = Integer.parseInt(CollectionHelper.getMapAttr(
                        sessionAttrs, MIN_POOL_SIZE, "8"));
                    String minPoolSizeStr = CollectionHelper.getMapAttr(
                        sessionAttrs, MIN_POOL_SIZE, "8");
                    mqParams.put(MIN_POOL_SIZE, minPoolSizeStr);
                    maxPoolSize = Integer.parseInt(CollectionHelper.getMapAttr(
                        sessionAttrs, MAX_POOL_SIZE, "32"));
                    String maxPoolSizeStr = CollectionHelper.getMapAttr(
                        sessionAttrs, MAX_POOL_SIZE, "32");
                    mqParams.put(MAX_POOL_SIZE, maxPoolSizeStr);
                    toolOutWriter.printMessage("sfo-cfg-prop-details");
                    String[] params1 = {sessionStoreUserName, jdbcURL,
                    Integer.toString(connectionMaxWaitTime),
                    Integer.toString(minPoolSize),
                    Integer.toString(maxPoolSize)};
                    toolOutWriter.printMessage("sfo-cfg-params", params1);
                    mqParams.put(SESSION_STORE_USERNAME, sessionStoreUserName);
                    connectToMQ(mqParams);
                } else {
                    toolOutWriter.printMessage("sfo-cfg-not-enabled", params);
                }
            } else {
                toolOutWriter.printMessage("sfo-cfg-not-enabled", params);
            }
        } catch (Exception ex) {
            toolOutWriter.printError("sfo-cfg-prop-notfound");
            toolOutWriter.printStatusMsg(false, "sfo-validate-cfg-prop"); 
            Debug.getInstance(DEBUG_NAME).error(
                "MQConnectValidation.processSFO: " +
                "Unable to get Session Schema Information", ex);
        }
    }
    
    private boolean verifyJDBCURL(String jdbcURL){
        boolean [] valid = {false, false};
        int i = 0;
        if ((jdbcURL != null) && jdbcURL.length() > 0 ) {
            if (jdbcURL.indexOf(",") == -1) {
                int idx1 =  jdbcURL.indexOf(":");
                int idx2 =  jdbcURL.lastIndexOf(":");
                if (idx2 > idx1) {
                    toolOutWriter.printError("sfo-jdbc-url-invalid",
                        new String[] {jdbcURL});
                    return false;
                }
            }
            StringTokenizer st = new StringTokenizer(jdbcURL, ",");
            while (st.hasMoreTokens()) {
                String mqHost = st.nextToken();
                if (mqHost != null) {
                    valid[i++] = isValidSyntax(mqHost);
                }
            }
        } else {
            toolOutWriter.printError("sfo-jdbc-url-invalid",
                new String[] {"null"});
        }
        return valid[0] && valid[1];
    }
    
    private boolean isValidSyntax(String mqHost) {
      boolean valid = false;
      String[] params = {mqHost};
      if (mqHost.indexOf(":") == -1 ) {
          toolOutWriter.printError("sfo-jdbc-url-no-port", params);
      } else {
          String host = mqHost.substring(0, mqHost.indexOf(":"));
          String[] params1 = {host};
          if (!isValidHost(host)){
             toolOutWriter.printError("sfo-jdbc-url-invalid-host",
                 params1);
          } else {
              valid = true;
          }
          String port =  mqHost.substring(mqHost.indexOf(":") + 1);
          if (!isValidPort(port)){
            toolOutWriter.printError("sfo-jdbc-url-invalid-port",
                new String[] {port});
            valid = false;
         } else {
            valid &= true;
         }
      }
      return valid;
   }

    private void connectToMQ(Map mqMap){
        Topic reqTopic = null;
        Topic resTopic = null;
        boolean connected = false;
        
        String[] params =  {(String)mqMap.get(SESSION_STORE_USERNAME),
        (String)mqMap.get(IPLANET_AM_SESSION_REPOSITORY_URL)};
        String[] param1 = {(String)mqMap.get(IPLANET_AM_SESSION_REPOSITORY_URL)};
        String[] param2 = {(String)mqMap.get(SESSION_STORE_USERNAME)};
        toolOutWriter.printMessage("sfo-cfg-connect-test", params);
        try {
            if (verifyJDBCURL((String)mqMap.get(IPLANET_AM_SESSION_REPOSITORY_URL))) {
                TopicConnectionFactory tFactory =
                    new com.sun.messaging.TopicConnectionFactory();
                sunSpecificConfig(tFactory, mqMap);
                tConn = tFactory.createTopicConnection();
                tConn.start();
                close();
                connected = true;
            } else {
                toolOutWriter.printMessage("sfo-invalid-jdbc-url",
                    param1);
            }
        } catch (java.io.EOFException e) {
            // no action required.
        } catch (com.sun.messaging.jms.JMSSecurityException ex) {
            toolOutWriter.printError("sfo-connect-credential-fail",
                param2);
        } catch (Exception e) {
            toolOutWriter.printError("sfo-cfg-connect-failed", param1);
        }
        if (connected) {
            toolOutWriter.printMessage("sfo-mq-connect-status",
                new String[] {"OK"});
        } else {
            toolOutWriter.printMessage("sfo-mq-connect-status",
                new String[] {"FAILED"});
        }
    }
    
    /* Receive message from topic subscriber */
    public void onMessage(Message message) {
        try {
            TextMessage textMessage = (TextMessage) message;
            String text = textMessage.getText( );
            System.out.println("MSG RECVD : " + text);
        } catch (JMSException jmse){
            Debug.getInstance(DEBUG_NAME).error(
                "MQConnectValidation.onMessage: " +
                "Exception in receving  message", jmse);
        }
    }
    
    /* Close the JMS connection */
    private void close( ) throws JMSException {
        tConn.close( );
    }
    
    private void sunSpecificConfig(
        TopicConnectionFactory tFactory,
        Map mqMap
    ) throws Exception {
        com.sun.messaging.ConnectionFactory cf =
            (com.sun.messaging.ConnectionFactory) tFactory;
        cf.setProperty(
            com.sun.messaging.ConnectionConfiguration.imqAddressList,
            (String)mqMap.get(IPLANET_AM_SESSION_REPOSITORY_URL));
        cf.setProperty(
            com.sun.messaging.ConnectionConfiguration.imqAddressListBehavior,
            "RANDOM");
        cf.setProperty(
            com.sun.messaging.ConnectionConfiguration.imqReconnectEnabled,
            "true");
        cf.setProperty(
            com.sun.messaging.ConnectionConfiguration.imqConnectionFlowLimitEnabled,
            "true");
        cf.setProperty(
            com.sun.messaging.ConnectionConfiguration.imqDefaultUsername,
            (String)mqMap.get(SESSION_STORE_USERNAME));
        cf.setProperty(
            com.sun.messaging.ConnectionConfiguration.imqDefaultPassword,
            (String)mqMap.get(SESSION_STORE_PASSWORD));
    }
    
    private String getSessionServerURL() {
        boolean isSiteEnabled = false;
        String svrProtocol = SystemProperties.get(AM_SERVER_PROTOCOL);
        String svr = SystemProperties.get(AM_SERVER_HOST);
        String svrPort = SystemProperties.get(AM_SERVER_PORT);
        String svrURI = SystemProperties.get(AM_SERVICES_DEPLOYMENT_DESCRIPTOR);
        URL sessionServiceID = null;
        try {
            String sessionServerID = WebtopNaming.getServerID(svrProtocol,
                svr, svrPort, svrURI);
            isSiteEnabled = WebtopNaming.isSiteEnabled(svrProtocol,
                svr, svrPort, svrURI);
            if (isSiteEnabled) {
                sessionServerID = WebtopNaming.getSiteID(svrProtocol,
                    svr, svrPort, svrURI);
                sessionServiceID = new URL(WebtopNaming.getServerFromID(
                    sessionServerID));
            } else {
                sessionServiceID = new URL(WebtopNaming.getServerFromID(
                    sessionServerID));
            }
        } catch (Exception ex) {
            Debug.getInstance(DEBUG_NAME).error(
                "MQConnectValidation.getSessionServerURL: " +
                "Unable to get Session Server URL", ex);
        }
        return sessionServiceID.toString();
    }
}
