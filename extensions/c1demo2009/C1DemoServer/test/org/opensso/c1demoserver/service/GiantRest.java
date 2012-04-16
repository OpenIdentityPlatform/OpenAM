/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: GiantRest.java,v 1.2 2009/06/11 05:29:46 superpat7 Exp $
 */

package org.opensso.c1demoserver.service;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.WebResource.Builder;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.Collection;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.core.MediaType;
import org.opensso.c1demoserver.converter.AccountConverter;
import org.opensso.c1demoserver.converter.AccountsConverter;
import org.opensso.c1demoserver.converter.CallLogConverter;
import org.opensso.c1demoserver.converter.CallLogsConverter;
import org.opensso.c1demoserver.converter.NotificationConverter;
import org.opensso.c1demoserver.converter.NotificationsConverter;
import org.opensso.c1demoserver.converter.PhoneConverter;
import org.opensso.c1demoserver.converter.PhonesConverter;
import org.opensso.c1demoserver.converter.QuestionsConverter;
import org.opensso.c1demoserver.model.Account;
import org.opensso.c1demoserver.model.Auth2;
import org.opensso.c1demoserver.model.CallLog;
import org.opensso.c1demoserver.model.Notification;
import org.opensso.c1demoserver.model.OTP;
import org.opensso.c1demoserver.model.Phone;

public class GiantRest {

    String base_path = "http://localhost:8080/C1DemoServer";

    private ClientConfig cc;

    private Client client;

    public GiantRest()
    {
        this.cc = new DefaultClientConfig();
        this.client = Client.create(this.cc);
    }

    public AccountsConverter getAccounts()
    {
        final WebResource wb = this.client.resource(base_path+"/resources/accounts");
        Builder builder = wb.accept(MediaType.APPLICATION_XML_TYPE);

        return builder.get(AccountsConverter.class);
    }

    public void createAccount(Account ac)
    {
        createAccount(ac,1);
    }
    public void createAccount(Account ac,int expandLevel)
    {
        final WebResource wb = this.client.resource(base_path+"/resources/accounts/");
        Builder builder = wb.accept(MediaType.APPLICATION_XML_TYPE);
        builder.type(MediaType.APPLICATION_XML_TYPE);
        try {
            builder.post(new AccountConverter(ac, new URI(base_path+"/resources/accounts/"+ac.getAccountNumber()+"/"), expandLevel, true));
        } catch (URISyntaxException ex) {
            Logger.getLogger(GiantRest.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public Account getAccount(String accountNumber)
    {
        final WebResource wb = this.client.resource(base_path+"/resources/accounts/"+accountNumber);
        Builder builder = wb.accept(MediaType.APPLICATION_XML_TYPE);
        builder.type(MediaType.APPLICATION_XML_TYPE);
        return builder.get(AccountConverter.class).getEntity();
    }

    public void updateAccount(Account ac)
    {
        final WebResource wb = this.client.resource(base_path+"/resources/accounts/"+ac.getAccountNumber());
        Builder builder = wb.accept(MediaType.APPLICATION_XML_TYPE);
        builder.type(MediaType.APPLICATION_XML_TYPE);
        try {
            builder.put(new AccountConverter(ac, new URI(base_path+"/resources/accounts/"+ac.getAccountNumber()+"/"),1, false));
        } catch (URISyntaxException ex) {
            Logger.getLogger(GiantRest.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void deleteAccount(Account account)
    {
        final WebResource wb = this.client.resource(base_path+"/resources/accounts/"+account.getAccountNumber());
        Builder builder = wb.accept(MediaType.APPLICATION_XML_TYPE);
        builder.type(MediaType.APPLICATION_XML_TYPE);
        builder.delete();
    }

    //Phone Section

    public PhonesConverter getPhones()
    {
        final WebResource wb = this.client.resource(base_path+"/resources/phones");
        Builder builder = wb.accept(MediaType.APPLICATION_XML_TYPE);

        return builder.get(PhonesConverter.class);
    }

    public void createPhone(Phone phone)
    {
        
        final WebResource wb = this.client.resource(base_path+"/resources/phones/");
        Builder builder = wb.accept(MediaType.APPLICATION_XML_TYPE);
        builder.type(MediaType.APPLICATION_XML_TYPE);
        System.out.println(base_path+"/resources/phones/");
        try {
            builder.post(new PhoneConverter(phone, new URI(base_path+"/resources/phones/"+phone.getPhoneNumber()+"/"),1, false));
        } catch (URISyntaxException ex) {
            Logger.getLogger(GiantRest.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public Phone getPhone(String phoneNumber)
    {
        System.out.println("CCPhoneGet: "+base_path+"/resources/phones/"+phoneNumber);
        final WebResource wb = this.client.resource(base_path+"/resources/phones/"+phoneNumber+"/");
        Builder builder = wb.accept(MediaType.APPLICATION_XML_TYPE);
        builder.type(MediaType.APPLICATION_XML_TYPE);
        return builder.get(PhoneConverter.class).getEntity();
    }

    public void deletePhone(Phone phone)
    {
        final WebResource wb = this.client.resource(base_path+"/resources/phones/"+phone.getPhoneNumber());
        Builder builder = wb.accept(MediaType.APPLICATION_XML_TYPE);
        builder.type(MediaType.APPLICATION_XML_TYPE);
        builder.delete();
    }

    public void updatePhone(Phone phone)
    {
        final WebResource wb = this.client.resource(base_path+"/resources/phones/"+phone.getPhoneNumber());
        Builder builder = wb.accept(MediaType.APPLICATION_XML_TYPE);
        builder.type(MediaType.APPLICATION_XML_TYPE);
        try {
            builder.put(new PhoneConverter(phone, new URI(base_path+"/resources/phones/"+phone.getPhoneNumber()+"/"), 1, false));
        } catch (URISyntaxException ex) {
            Logger.getLogger(GiantRest.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public QuestionsConverter getPhoneQuestions(Phone phone)
    {

        final WebResource wb = this.client.resource(base_path+"/resources/phones/"+phone.getPhoneNumber()+"/questionCollection/");
        Builder builder = wb.accept(MediaType.APPLICATION_XML_TYPE);

        return builder.get(QuestionsConverter.class);
    }

    public OTP authPhone(Phone phone, Collection<String> answers)
    {
        String url = base_path+"/resources/phones/"+phone.getPhoneNumber()+"/";
        String content = "<auth2>";
        for(String answer : answers)
            content+="<answer>"+answer+"</answer>";
        content+="</auth2>";
        final WebResource wb = this.client.resource(url);
        Builder builder = wb.accept(MediaType.APPLICATION_XML_TYPE);
        builder.type(MediaType.APPLICATION_FORM_URLENCODED_TYPE);
        System.out.println(base_path+"/resources/phones/");
        //try {
            Auth2 a2 = new Auth2();
            a2.setAnswers(answers);
            ClientResponse response = builder.entity("action=auth2&content="+URLEncoder.encode(content)).post(ClientResponse.class);
            OTP otp = response.getEntity(OTP.class);
            if(otp!=null)
                return otp;
    //    } catch (URISyntaxException ex) {
       //     Logger.getLogger(GiantRest.class.getName()).log(Level.SEVERE, null, ex);
      //  }
        return null;
    }

    // Call log
    public CallLogsConverter getCallLogs()
    {
        final WebResource wb = this.client.resource(base_path+"/resources/callLogs");
        Builder builder = wb.accept(MediaType.APPLICATION_XML_TYPE);

        return builder.get(CallLogsConverter.class);
    }

    public CallLog getCallLog(String callLog)
    {
        System.out.println("CCPhoneGet: "+base_path+"/resources/callLogs/"+callLog);
        final WebResource wb = this.client.resource(base_path+"/resources/callLogs/"+callLog+"/");
        Builder builder = wb.accept(MediaType.APPLICATION_XML_TYPE);
        builder.type(MediaType.APPLICATION_XML_TYPE);
        return builder.get(CallLogConverter.class).getEntity();
    }

    public Integer createCallLog(CallLog callLog)
    {
        final WebResource wb = this.client.resource(base_path+"/resources/callLogs/");
        System.out.println(base_path+"/resources/callLogs/");
        Builder builder = wb.accept(MediaType.APPLICATION_XML_TYPE);
        builder.type(MediaType.APPLICATION_XML_TYPE);

        try {
            ClientResponse response = builder.entity(new CallLogConverter(callLog,
                new URI(base_path+"/resources/callLogs/"), 1, false)).post(ClientResponse.class);
            String loc = response.getLocation().toString();
            System.out.println("loc: "+loc);
            if(loc.endsWith("/"))
                loc=loc.substring(0,loc.length()-1);
            loc = loc.substring(loc.lastIndexOf("/")+1);
            System.out.println("Loc2: "+loc);
            return Integer.parseInt(loc);
        } catch (URISyntaxException ex) {
            Logger.getLogger(GiantRest.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    public void deleteCallLog(CallLog callLog)
    {
        final WebResource wb = this.client.resource(base_path+"/resources/callLogs/"+callLog.getCallId());
        Builder builder = wb.accept(MediaType.APPLICATION_XML_TYPE);
        builder.type(MediaType.APPLICATION_XML_TYPE);
        builder.delete();
    }

    // Notification
    public NotificationsConverter getNotifications()
    {
        final WebResource wb = this.client.resource(base_path+"/resources/notifications");
        Builder builder = wb.accept(MediaType.APPLICATION_XML_TYPE);

        return builder.get(NotificationsConverter.class);
    }
    public Integer createNotification(Notification notification)
    {
        final WebResource wb = this.client.resource(base_path+"/resources/notifications/");
        System.out.println(base_path+"/resources/notifications/");
        Builder builder = wb.accept(MediaType.APPLICATION_XML_TYPE);
        builder.type(MediaType.APPLICATION_XML_TYPE);

        try {
            ClientResponse response = builder.entity(new NotificationConverter(notification,
                new URI(base_path+"/resources/notifications/"), 1, false)).post(ClientResponse.class);
            System.out.println("resonse: "+response.getStatus());
            System.out.println("Location: "+response.getLocation());
            String loc = response.getLocation().toString();
            System.out.println("loc: "+loc);
            if(loc.endsWith("/"))
                loc=loc.substring(0,loc.length()-1);
            loc = loc.substring(loc.lastIndexOf("/")+1);
            System.out.println("Loc2: "+loc);
            return Integer.parseInt(loc);
        } catch (URISyntaxException ex) {
            Logger.getLogger(GiantRest.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    public void deleteNotification(Notification notification)
    {
        final WebResource wb = this.client.resource(base_path+"/resources/notifications/"+notification.getNotificationId());
        Builder builder = wb.accept(MediaType.APPLICATION_XML_TYPE);
        builder.type(MediaType.APPLICATION_XML_TYPE);
        builder.delete();
    }

}
