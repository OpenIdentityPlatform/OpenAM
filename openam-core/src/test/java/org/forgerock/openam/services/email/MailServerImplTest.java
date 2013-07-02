/*
 * DO NOT REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 ForgeRock Inc. All rights reserved.
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
 * "Portions copyright [year] [name of copyright owner]"
 */
package org.forgerock.openam.services.email;


import com.iplanet.am.util.AMSendMail;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.modules.testng.PowerMockTestCase;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import javax.mail.MessagingException;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class MailServerImplTest extends PowerMockTestCase {

    private MailServerImpl mailServerMock;
    private AMSendMail sendMailMock;
    private static final String RECIPIENT = "to@gmail.com";
    private static final String[] recipients = {RECIPIENT};

    private static String SMTP_HOSTNAME = "forgerockEmailServiceSMTPHostName";
    private static String SMTP_HOSTPORT = "forgerockEmailServiceSMTPHostPort";
    private static String SMTP_USERNAME = "forgerockEmailServiceSMTPUserName";
    private static String SMTP_USERPASSWORD = "forgerockEmailServiceSMTPUserPassword";
    private static String SMTP_SSL_ENABLED = "forgerockEmailServiceSMTPSSLEnabled";
    private static String FROM_ADDRESS = "forgerockEmailServiceSMTPFromAddress";
    private static String SUBJECT = "forgerockEmailServiceSMTPSubject";
    private static String MESSAGE = "forgerockEmailServiceSMTPMessage";

    @Test
    public void testSendMailWithoutOptions(){
        try {
            mailServerMock.sendEmail("to@gmail.com", "Subject", "Message");
            verify(sendMailMock, times(1)).postMail(eq(recipients), anyString(), anyString(), anyString(), anyString(), anyString(),
                    anyString(), anyString(), anyString(),
                    anyBoolean());
        } catch (MessagingException e){
            assert(false);
        }

    }

    @Test
    public void testSendMailWithOptions(){
        try {
            mailServerMock.sendEmail("from@gmail.com", "to@gmail.com", "Subject", "Message", createOptionsMap());
            verify(sendMailMock, times(1)).postMail(eq(recipients), anyString(), anyString(), anyString(), anyString(), anyString(),
                    anyString(), anyString(), anyString(),
                    anyBoolean());
        } catch (MessagingException e){
            assert(false);
        }

    }

    @BeforeMethod
    public void setup(){
        ServiceConfigManager serviceConfigManagerMock = PowerMockito.mock(ServiceConfigManager.class);
        ServiceConfig serviceConfigMock = PowerMockito.mock(ServiceConfig.class);
        Debug debugMock = PowerMockito.mock(Debug.class);
        sendMailMock = PowerMockito.mock(AMSendMail.class);
        Map<String, Set<String>> options = createOptionsMap();
        try{
            Mockito.doNothing().when(sendMailMock).postMail(eq(recipients), anyString(), anyString(), anyString());
            Mockito.doNothing().when(sendMailMock).postMail(eq(recipients), anyString(), anyString(), anyString(), anyString(), anyString(),
                    anyString(), anyString(), anyString(),
                    anyBoolean());
        } catch (MessagingException e ){
            assert(false);
        }
        mailServerMock = new MailServerImpl(serviceConfigManagerMock, serviceConfigMock, debugMock, sendMailMock, options);

    }

    private Map<String, Set<String>> createOptionsMap(){
        Map<String, Set<String>> map = new HashMap<String, Set<String>>();
        Set<String> set = new HashSet<String>();
        set.add("value");
        map.put(SMTP_HOSTNAME, set);
        map.put(SMTP_HOSTPORT, set);
        map.put(SMTP_USERNAME, set);
        map.put(SMTP_USERPASSWORD, set);
        map.put(SMTP_SSL_ENABLED, set);
        map.put(FROM_ADDRESS, set);
        map.put(SUBJECT, set);
        map.put(MESSAGE, set);
        return map;
    }



}
