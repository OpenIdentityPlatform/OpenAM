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
 * $Id: AccountsResourceTest.java,v 1.2 2009/06/11 05:29:46 superpat7 Exp $
 */

package org.opensso.c1demoserver.service;

import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashSet;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.opensso.c1demoserver.converter.AccountsConverter;
import org.opensso.c1demoserver.converter.CallLogsConverter;
import org.opensso.c1demoserver.converter.NotificationsConverter;
import org.opensso.c1demoserver.converter.PhonesConverter;
import org.opensso.c1demoserver.converter.QuestionsConverter;
import org.opensso.c1demoserver.model.Account;
import org.opensso.c1demoserver.model.CallLog;
import org.opensso.c1demoserver.model.Notification;
import org.opensso.c1demoserver.model.OTP;
import org.opensso.c1demoserver.model.Phone;

public class AccountsResourceTest {

    Account firstAccount;
    Account firstAccountV2;
    Account firstAccountV3;
    Account secondAccount;
    Phone franksPhone;
    Phone franksPhoneV2;
    Phone franksPhoneV3;
    Phone billysPhone;
    Phone sallysPhone;
    Phone carolsPhone;

    CallLog firstLog;

    Notification firstNote;

    public AccountsResourceTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }


    @Before
    public void setUp() {

        

        // setup variables
        firstAccount = new Account();
        firstAccount.setAccountNumber("12345678901234567890");
        firstAccount.setBillToAddressLine1("123 Any St");
        //firstAccount.setBillToAddressLine2("");
        firstAccount.setBillToCity("Santa Clara");
        firstAccount.setBillToState("CA");
        firstAccount.setBillToZip("95012");
        firstAccount.setChallengeAnswer("Blue");
        firstAccount.setChallengeQuestion("Whats your favourite color?");
        firstAccount.setCreditCardNumber(1234567890123456L);
        firstAccount.setCvv((short) 1234);
        firstAccount.setPlanId(1);
        firstAccount.setPlanMinutes(1000);
        firstAccount.setPhoneCollection(Collections.EMPTY_LIST);

        secondAccount = new Account();
        secondAccount.setAccountNumber("1234567899876543210");
        secondAccount.setBillToAddressLine1("123 Any St");
        //firstAccount.setBillToAddressLine2("");
        secondAccount.setBillToCity("Santa Clara");
        secondAccount.setBillToState("CA");
        secondAccount.setBillToZip("95012");
        secondAccount.setChallengeAnswer("Blue");
        secondAccount.setChallengeQuestion("Whats your favourite color?");
        secondAccount.setCreditCardNumber(1234567890123456L);
        secondAccount.setCvv((short) 1234);
        secondAccount.setPlanId(1);
        secondAccount.setPlanMinutes(1000);
        secondAccount.setPhoneCollection(Collections.EMPTY_LIST);

        firstAccountV2 = new Account();
        firstAccountV2.setAccountNumber("12345678901234567890");
        firstAccountV2.setBillToAddressLine1("321 Any St");
        firstAccountV2.setBillToAddressLine2("Second Address");
        firstAccountV2.setBillToCity("Austin");
        firstAccountV2.setBillToState("TX");
        firstAccountV2.setBillToZip("78752");
        firstAccountV2.setChallengeAnswer("Whats your favourite color?");
        firstAccountV2.setChallengeQuestion("Blue");
        firstAccountV2.setCreditCardNumber(1234561234567890L);
        firstAccountV2.setCvv((short) 4321);
        firstAccountV2.setPlanId(23);
        firstAccountV2.setPlanMinutes(5000);
        firstAccountV2.setPhoneCollection(Collections.EMPTY_LIST);

        firstAccountV3 = new Account();
        firstAccountV3.setAccountNumber("12345678901234567890");
        firstAccountV3.setBillToAddressLine1(null);
        firstAccountV3.setBillToAddressLine2(null);
        firstAccountV3.setBillToCity(null);
        firstAccountV3.setBillToState(null);
        firstAccountV3.setBillToZip(null);
        firstAccountV3.setChallengeAnswer(null);
        firstAccountV3.setChallengeQuestion(null);
        firstAccountV3.setCreditCardNumber(1234561234567890L);
        firstAccountV3.setCvv((short) 4321);
        firstAccountV3.setPlanId(null);
        firstAccountV3.setPlanMinutes(null);
        firstAccountV3.setPhoneCollection(Collections.EMPTY_LIST);

        franksPhone = new Phone();

        franksPhone.setAccountNumber(firstAccount);
        franksPhone.setAllocatedMinutes(250);
        franksPhone.setCanDownloadMusic(true);
        franksPhone.setCanDownloadRingtones(true);
        franksPhone.setCanDownloadVideo(true);
        franksPhone.setHeadOfHousehold(true);
        franksPhone.setPhoneNumber("1234567890");
        franksPhone.setPassword("123abc");
        franksPhone.setUserName("Frank Spencer");
        franksPhone.setNotificationCollection(new HashSet<Notification>());
        franksPhone.setCallLogCollection(new HashSet<CallLog>());

        franksPhoneV2 = new Phone();
        franksPhoneV2.setAccountNumber(firstAccount);
        franksPhoneV2.setAllocatedMinutes(520);
        franksPhoneV2.setCanDownloadMusic(false);
        franksPhoneV2.setCanDownloadRingtones(false);
        franksPhoneV2.setCanDownloadVideo(false);
        franksPhoneV2.setHeadOfHousehold(true);
        franksPhoneV2.setPhoneNumber("1234567890");
        franksPhoneV2.setPassword("abc123");
        franksPhoneV2.setUserName("Franklin Spencer");
        franksPhoneV2.setNotificationCollection(new HashSet<Notification>());
        franksPhoneV2.setCallLogCollection(new HashSet<CallLog>());

        franksPhoneV3 = new Phone();
        franksPhoneV3.setAccountNumber(firstAccount);
        franksPhoneV3.setAllocatedMinutes(null);
        franksPhoneV3.setCanDownloadMusic(null);
        franksPhoneV3.setCanDownloadRingtones(null);
        franksPhoneV3.setCanDownloadVideo(null);
        franksPhoneV3.setHeadOfHousehold(null);
        franksPhoneV3.setPhoneNumber("1234567890");
        franksPhoneV3.setPassword("abc123");
        franksPhoneV3.setUserName(null);
        franksPhoneV3.setNotificationCollection(new HashSet<Notification>());
        franksPhoneV3.setCallLogCollection(new HashSet<CallLog>());

        billysPhone = new Phone();

        billysPhone.setAccountNumber(secondAccount);
        billysPhone.setAllocatedMinutes(250);
        billysPhone.setCanDownloadMusic(true);
        billysPhone.setCanDownloadRingtones(true);
        billysPhone.setCanDownloadVideo(true);
    //    billysPhone.setChallengeAnswer("Chevy Nova");
      //  billysPhone.setChallengeQuestion("What was your first car?");
        billysPhone.setHeadOfHousehold(false);
        billysPhone.setPhoneNumber("0987654321");
        billysPhone.setPassword("123abc");
        billysPhone.setUserName("Billy Spencer");
        billysPhone.setNotificationCollection(new HashSet<Notification>());
        billysPhone.setCallLogCollection(new HashSet<CallLog>());

        sallysPhone = new Phone();

        sallysPhone.setAccountNumber(firstAccount);
        sallysPhone.setAllocatedMinutes(50);
        sallysPhone.setCanDownloadMusic(true);
        sallysPhone.setCanDownloadRingtones(true);
        sallysPhone.setCanDownloadVideo(true);
        sallysPhone.setHeadOfHousehold(false);
        sallysPhone.setPhoneNumber("1111111111");
        sallysPhone.setPassword("123abc");
        sallysPhone.setUserName("Sally Spencer");
        sallysPhone.setNotificationCollection(new HashSet<Notification>());
        sallysPhone.setCallLogCollection(new HashSet<CallLog>());

        carolsPhone = new Phone();

        carolsPhone.setAccountNumber(firstAccount);
        carolsPhone.setAllocatedMinutes(250);
        carolsPhone.setCanDownloadMusic(true);
        carolsPhone.setCanDownloadRingtones(true);
        carolsPhone.setCanDownloadVideo(true);
        carolsPhone.setHeadOfHousehold(false);
        carolsPhone.setPhoneNumber("2222222222");
        carolsPhone.setPassword("123abc");
        carolsPhone.setUserName("Sally Spencer");
        carolsPhone.setNotificationCollection(new HashSet<Notification>());
        carolsPhone.setCallLogCollection(new HashSet<CallLog>());

        firstLog = new CallLog();
        firstLog.setCallDurationSecs(45);
        firstLog.setPhoneNumberFrom(franksPhone);
        firstLog.setPhoneNumberTo(sallysPhone.getPhoneNumber());

        firstNote = new Notification();
        firstNote.setMessageText("Notification Message");
        firstNote.setPhoneNumber(franksPhone);

        GiantRest gr = new GiantRest();
        
        CallLogsConverter clc = gr.getCallLogs();
        for(CallLog cl : clc.getEntities())
            gr.deleteCallLog(cl);

        NotificationsConverter nc = gr.getNotifications();
        for(Notification n : nc.getEntities())
            gr.deleteNotification(n);
        PhonesConverter pc = gr.getPhones();
        System.out.println("Phones: "+pc.getEntities().size());
        for(Phone p : pc.getEntities())
        {
            
            gr.deletePhone(p);
        }
        AccountsConverter ac = gr.getAccounts();
        for(Account a : ac.getEntities())
        {
            System.out.println("Deleting Account: "+a.getAccountNumber());
            gr.deleteAccount(a);
        }
        assertTrue(gr.getAccounts().getEntities().size()==0);


        if(gr.getAccounts().getEntities().size()!=0
                || gr.getPhones().getEntities().size()!=0
                || gr.getCallLogs().getEntities().size()!=0
                || gr.getNotifications().getEntities().size()!=0)
            throw new RuntimeException();
    }

    @After
    public void tearDown() {
    }

    private void sameAccount(Account first, Account second) {
        assertTrue(sameObj(first.getAccountNumber(), second.getAccountNumber()));
        assertTrue(sameObj(first.getBillToAddressLine1(), second.getBillToAddressLine1()));
        
        assertTrue(sameObj(first.getBillToAddressLine2(), second.getBillToAddressLine2()));
        assertTrue(sameObj(first.getBillToCity(), second.getBillToCity()));
        assertTrue(sameObj(first.getBillToState(), second.getBillToState()));
        assertTrue(sameObj(first.getBillToZip(), second.getBillToZip()));
        assertTrue(sameObj(first.getChallengeAnswer(), second.getChallengeAnswer()));
        assertTrue(first.getCreditCardNumber() == second.getCreditCardNumber());
        assertTrue(first.getCvv() == second.getCvv());
        assertTrue(sameObj(first.getPlanId(), second.getPlanId()));
        assertTrue(sameObj(first.getPlanMinutes(), second.getPlanMinutes()));
        assertTrue(sameObj(first.getBillToAddressLine1(), second.getBillToAddressLine1()));
    }

    private void samePhone(Phone first, Phone second)
    {
        assertTrue(sameObj(first.getAccountNumber(),second.getAccountNumber()));
        assertTrue(sameObj(first.getAllocatedMinutes(),second.getAllocatedMinutes()));
        assertTrue(sameObj(first.getCanDownloadMusic(),second.getCanDownloadMusic()));
        assertTrue(sameObj(first.getCanDownloadRingtones(),second.getCanDownloadRingtones()));
        assertTrue(sameObj(first.getCanDownloadVideo(),second.getCanDownloadVideo()));
        assertTrue(sameObj(first.getHeadOfHousehold(),second.getHeadOfHousehold()));
        assertTrue(sameObj(first.getOtp(),second.getOtp()));
        
        assertTrue(sameObj(first.getPhoneNumber(),second.getPhoneNumber()));
        assertTrue(sameObj(first.getUserName(),second.getUserName()));
    }

    private void sameCallLog(CallLog first, CallLog second)
    {
        assertTrue(first.getCallDurationSecs()==second.getCallDurationSecs());
        assertTrue(first.getCallId()==second.getCallId());
        assertTrue(first.getCallTime()==second.getCallTime());
        assertTrue(first.getPhoneNumberFrom().getPhoneNumber().equals(second.getPhoneNumberFrom().getPhoneNumber()));
        assertTrue(first.getPhoneNumberTo().equals(second.getPhoneNumberTo()));

    }

    public boolean sameObj(Object first, Object second) {
        if (first == null) {
            if (second == null)
                return true;
            System.out.println("Values: |"+first+"| |"+second+"|");
        }
        else if( first.equals(second))
            return true;

        System.out.println("Values: |"+first+"| |"+second+"|");
        return false;
    }

    /**
     * Test of get method, of class AccountsResource.
     */
    @Test
    public void testAccountBasics() throws URISyntaxException {
        GiantRest gr = new GiantRest();

        System.out.println("get");
        gr.createAccount(firstAccount);

        AccountsConverter ac = gr.getAccounts();
        System.out.println("Account Count(1): " + ac.getEntities().size());
        assertTrue(ac.getEntities().size() == 1);

        Account createdAccount = gr.getAccount(firstAccount.getAccountNumber());
        assertTrue(createdAccount != null);
        sameAccount(firstAccount, createdAccount);

        gr.updateAccount(firstAccountV2);

        createdAccount = gr.getAccount(firstAccount.getAccountNumber());
        assertTrue(createdAccount != null);
        sameAccount(firstAccountV2, createdAccount);

        gr.updateAccount(firstAccountV3);

        createdAccount = gr.getAccount(firstAccount.getAccountNumber());
        assertTrue(createdAccount != null);
        sameAccount(firstAccountV3, createdAccount);

        gr.deleteAccount(firstAccount);

        ac = gr.getAccounts();
        System.out.println("Account Count(0): " + ac.getEntities().size());
        assertTrue(ac.getEntities().size() == 0);
    }

    @Test
    public void testPhoneBasics() throws URISyntaxException {
        // Start phone stuff

        GiantRest gr = new GiantRest();
        gr.createAccount(firstAccount);
        gr.createPhone(franksPhone);
        //createFranksPhone();


        PhonesConverter pc = gr.getPhones();
        assertTrue(pc.getEntities().size() == 1);

        Phone phone = gr.getPhone(franksPhone.getPhoneNumber());
        samePhone(franksPhone,phone);


        gr.updatePhone(franksPhoneV2);

        phone = gr.getPhone(franksPhone.getPhoneNumber());
        samePhone(franksPhoneV2,phone);

        gr.updatePhone(franksPhoneV3);

        phone = gr.getPhone(franksPhone.getPhoneNumber());
        samePhone(franksPhoneV3,phone);

        gr.deletePhone(franksPhone);

        pc = gr.getPhones();
        assertTrue(pc.getEntities().size() == 0);

        //Delete Account
        gr.deleteAccount(firstAccount);
    }

    @Test
    public void testCallLogBasics() throws URISyntaxException
    {
        GiantRest gr = new GiantRest();
        gr.createAccount(firstAccount);
        assertTrue(gr.getPhones().getEntities().size()==0);
        gr.createPhone(franksPhone);
        gr.createPhone(sallysPhone);

        int callLogId = gr.createCallLog(firstLog);
        firstLog.setCallId(callLogId);

        assertTrue(gr.getCallLogs().getEntities().size()==1);

        CallLog callLog = gr.getCallLog(callLogId+"");
        sameCallLog(firstLog,callLog);

        gr.deleteCallLog(firstLog);

        assertTrue(gr.getCallLogs().getEntities().size()==0);

        gr.deletePhone(franksPhone);
        gr.deletePhone(sallysPhone);
        gr.deleteAccount(firstAccount);
    }

    @Test
    public void testNotificationBasics()
    {
        GiantRest gr = new GiantRest();
        gr.createAccount(firstAccount);
        gr.createPhone(franksPhone);

        Integer notificationId = gr.createNotification(firstNote);
        firstNote.setNotificationId(notificationId);

        assertTrue(gr.getNotifications().getEntities().size()==1);

        gr.deleteNotification(firstNote);

        assertTrue(gr.getNotifications().getEntities().size()==0);

        gr.deletePhone(franksPhone);
        gr.deleteAccount(firstAccount);
    }

   /* @Test
    public void testBigPush()
    {
        GiantRest gr = new GiantRest();
        Set<Phone> phones = new HashSet<Phone>();
        phones.add(franksPhone);
        phones.add(billysPhone);
        firstAccount.setPhoneCollection(phones);

        franksPhone.setCallLogCollection(Collections.singleton(firstLog));
        billysPhone.setCallLogCollection1(Collections.singleton(firstLog));

        franksPhone.setNotificationCollection(Collections.singleton(firstNote));

        gr.createAccount(firstAccount,3);
        
        //check the numbers
        assertTrue(gr.getAccounts().getEntities().size()==1);
        assertTrue(gr.getPhones().getEntities().size()==2);
        assertTrue(gr.getCallLogs().getCallLog().size()==1);
        assertTrue(gr.getNotifications().getNotification().size()==1);
    }*/

    @Test
    public void testDemoRun()
    {
        GiantRest gr = new GiantRest();
        //Initial setup - BIlly has a prepaid Cell phone
        gr.createAccount(secondAccount);        
        gr.createPhone(billysPhone);

        afterInitial();
        
        //Clerk creates account and 3 phones for frank
        gr.createAccount(firstAccount);
        gr.createPhone(franksPhone);
        gr.createPhone(sallysPhone);
        gr.createPhone(carolsPhone);

        afterClerk();

        //make a call
        gr.createCallLog(firstLog);

        afterFirstCall();

        // Franks is logging in
        QuestionsConverter qsc = gr.getPhoneQuestions(franksPhone);
        assertTrue(qsc.getEntities().size()==1); // car question

        OTP otp = gr.authPhone(franksPhone, Collections.singleton("Chevy Nova"));
        System.out.println("OTP "+otp);
        if(otp!=null)
            System.out.println("OTP value:"+otp.getText());

        afterOTP(otp);

        Phone phone = gr.getPhone(carolsPhone.getPhoneNumber());
        phone.setHeadOfHousehold(true);
        gr.updatePhone(phone);

        afterHOH();

        phone = gr.getPhone(sallysPhone.getPhoneNumber());
        phone.setAllocatedMinutes(250);
        gr.updatePhone(phone);

        afterUpdatedMinutes();

        phone = gr.getPhone(billysPhone.getPhoneNumber());
        phone.setAccountNumber(firstAccount);
        phone.setHeadOfHousehold(false);
        phone.setCanDownloadMusic(false);
        phone.setCanDownloadRingtones(false);
        phone.setCanDownloadVideo(false);
        gr.updatePhone(phone);

        afterBillyMove();

        //cleanup
//        gr.deletePhone(franksPhone);
//        gr.deletePhone(billysPhone);
//        gr.deletePhone(sallysPhone);
//        gr.deletePhone(carolsPhone);
//
//        gr.deleteAccount(firstAccount);
//        gr.deleteAccount(secondAccount);
    }

    public void afterInitial()
    {
        GiantRest gr = new GiantRest();
        assertTrue(gr.getAccounts().getEntities().size()==1);
        assertTrue(gr.getPhones().getEntities().size()==1);
    }

    public void afterClerk()
    {
        GiantRest gr = new GiantRest();
        assertTrue(gr.getAccounts().getEntities().size()==2);
        assertTrue(gr.getPhones().getEntities().size()==4);
    }

    public void afterFirstCall()
    {
        GiantRest gr = new GiantRest();
        assertTrue(gr.getCallLogs().getEntities().size()==1);
    }

    public void afterOTP(OTP otp)
    {
        GiantRest gr = new GiantRest();
        Phone phone = gr.getPhone(franksPhone.getPhoneNumber());
        assertTrue(phone.getOtp().equals(otp.getText()));
    }
    public void afterHOH()
    {
        GiantRest gr = new GiantRest();
        Phone phone = gr.getPhone(carolsPhone.getPhoneNumber());
        assertTrue(phone.getHeadOfHousehold());
    }

    public void afterUpdatedMinutes()
    {
        GiantRest gr = new GiantRest();
        Phone phone = gr.getPhone(sallysPhone.getPhoneNumber());
        assertTrue(phone.getAllocatedMinutes().intValue()==250);
    }

    public void afterBillyMove()
    {
        GiantRest gr = new GiantRest();
        Phone phone = gr.getPhone(billysPhone.getPhoneNumber());
        assertTrue(phone.getAccountNumber().getAccountNumber().equals(firstAccount.getAccountNumber()));
        assertFalse(phone.getHeadOfHousehold());
        assertFalse(phone.getCanDownloadMusic());
        assertFalse(phone.getCanDownloadVideo());
        assertFalse(phone.getCanDownloadRingtones());
    }
}