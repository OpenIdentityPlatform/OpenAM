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
 * $Id: ChallengeResource.java,v 1.2 2009/06/11 05:29:43 superpat7 Exp $
 */

package org.opensso.c1demoserver.service;

import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import com.sun.jersey.api.core.ResourceContext;
import java.io.StringReader;
import java.net.URLDecoder;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import javax.ws.rs.WebApplicationException;
import javax.persistence.NoResultException;
import javax.persistence.EntityManager;
import org.opensso.c1demoserver.model.Notification;
import java.util.Collection;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import org.opensso.c1demoserver.converter.ChallengeConverter;
import org.opensso.c1demoserver.model.CallLog;
import org.opensso.c1demoserver.model.Account;
import org.opensso.c1demoserver.model.Auth2;
import org.opensso.c1demoserver.model.OTP;
import org.opensso.c1demoserver.model.Phone;
import org.opensso.c1demoserver.model.SetPassword;
import org.opensso.c1demoserver.model.SetQuestion;

public class ChallengeResource {
    @Context
    protected UriInfo uriInfo;
    @Context
    protected ResourceContext resourceContext;
    protected String id;

    /** Creates a new instance of ChallengeResource */
    public ChallengeResource() {
    }

    public void setId(String id) {
        this.id = id;
    }

    /**
     * Get method for retrieving an instance of Challenge identified by id in XML format.
     *
     * @param id identifier for the entity
     * @return an instance of ChallengeConverter
     */
    @GET
    @Produces({"application/xml", "application/json"})
    public ChallengeConverter get(@QueryParam("expandLevel")
    @DefaultValue("1")
    int expandLevel) {
        Phone phone = getPhone(expandLevel);
        if(phone!=null) {
            return new ChallengeConverter(phone, uriInfo.getAbsolutePath(), expandLevel);
        }
        return null;
    }

    /**
     * Post method for doing stuff to the phone
     *
     * @param form form content
     * @return response for the client
     */
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response post(MultivaluedMap<String, String> form,
    @Context
    HttpServletRequest request) {
        String action = form.get("action").get(0);
        String content = form.get("content").get(0);

        try {
            if ( action.equals("auth2")) {
                return auth2(content);
            } else if ( action.equals("setQuestion")) {
                return setQuestion(content);
            } else if ( action.equals("setPassword")) {
                return setPassword(content);
            }

            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        } catch (Exception e) {
            e.printStackTrace();
            throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
        }

    }

    private Response auth2(String content) throws JAXBException {
        JAXBContext ctx = JAXBContext.newInstance(new Class[]{Auth2.class});
        Unmarshaller um = ctx.createUnmarshaller();
        Auth2 auth2 = (Auth2) um.unmarshal(new StringReader(content));
        Phone phone = getEntity();
        Object[] answers = auth2.getAnswers().toArray();
        if ( phone.getAccountNumber().getChallengeQuestion() == null ) {
            // Check card digits and CVV
            if ( auth2.getAnswers().size() != 2 )
            {
                throw new WebApplicationException(Response.Status.BAD_REQUEST);
            }

            if ( phone.getAccountNumber().getCreditCardNumber() % 10000 != Integer.parseInt((String)answers[0]) ) {
                throw new WebApplicationException(Response.Status.BAD_REQUEST);
            }
            if ( phone.getAccountNumber().getCvv() != Integer.parseInt((String)answers[1]) ) {
                throw new WebApplicationException(Response.Status.BAD_REQUEST);
            }
        } else {
            // Compare challenge answer
            if ( auth2.getAnswers().size() != 1 )
            {
                throw new WebApplicationException(Response.Status.BAD_REQUEST);
            }

            if ( ! phone.getAccountNumber().getChallengeAnswer().equals(answers[0])) {
                throw new WebApplicationException(Response.Status.BAD_REQUEST);
            }
        }

        // Everything is good - generate and return an OTP
        String otpString = getRandomOtp();

        // Write otpString to phone table to check later
        phone.setOtp(otpString);

        PersistenceService persistenceSvc = PersistenceService.getInstance();
        try {
            persistenceSvc.beginTx();
            EntityManager em = persistenceSvc.getEntityManager();
            updateEntity(getEntity(), phone);
            persistenceSvc.commitTx();
        } finally {
            persistenceSvc.close();
        }

        OTP otp = new OTP();
        otp.setText(otpString);
        return Response.ok(otp).build();
    }

    private Response setQuestion(String content) throws JAXBException {
        JAXBContext ctx = JAXBContext.newInstance(new Class[]{SetQuestion.class});
        Unmarshaller um = ctx.createUnmarshaller();
        SetQuestion setQuestion = (SetQuestion) um.unmarshal(new StringReader(content));

        // Check OTP
        Phone phone = this.getEntity();
        if ( ! setQuestion.getOtp().getText().equals( phone.getOtp() ) )
        {
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }

        // Update account resource and write it back
        Account account = phone.getAccountNumber();
        account.setChallengeQuestion(URLDecoder.decode(setQuestion.getQuestion()));
        account.setChallengeAnswer(URLDecoder.decode(setQuestion.getAnswer()));

        PersistenceService persistenceSvc = PersistenceService.getInstance();
        try {
            persistenceSvc.beginTx();
            EntityManager em = persistenceSvc.getEntityManager();
            account = em.merge(account);
            persistenceSvc.commitTx();
        } finally {
            persistenceSvc.close();
        }

        // Everything is good - generate and return a new OTP
        String otpString = getRandomOtp();

        // Write otpString to phone table to check later
        phone.setOtp(otpString);

        try {
            persistenceSvc.beginTx();
            EntityManager em = persistenceSvc.getEntityManager();
            updateEntity(getEntity(), phone);
            persistenceSvc.commitTx();
        } finally {
            persistenceSvc.close();
        }

        OTP otp = new OTP();
        otp.setText(otpString);
        return Response.ok(otp).build();
    }

    private Response setPassword(String content) throws JAXBException {
        JAXBContext ctx = JAXBContext.newInstance(new Class[]{SetPassword.class});
        Unmarshaller um = ctx.createUnmarshaller();
        SetPassword setPassword = (SetPassword) um.unmarshal(new StringReader(content));

        // Check OTP
        Phone phone = this.getEntity();
        if ( ! setPassword.getOtp().getText().equals( phone.getOtp() ) )
        {
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }

        // Update phone resource and write it back
        phone.setPassword(setPassword.getPassword().getText());
        phone.setOtp(null);

        PersistenceService persistenceSvc = PersistenceService.getInstance();
        try {
            persistenceSvc.beginTx();
            EntityManager em = persistenceSvc.getEntityManager();
            updateEntity(getEntity(), phone);
            persistenceSvc.commitTx();
        } finally {
            persistenceSvc.close();
        }

        return Response.noContent().build();
    }

    private String getRandomOtp()
    {
        SecureRandom random;
        try {
            random = SecureRandom.getInstance("SHA1PRNG");
        } catch (NoSuchAlgorithmException ex) {
            throw new WebApplicationException(ex, Response.Status.INTERNAL_SERVER_ERROR);
        }

        byte[] otpBytes = new byte[64];
        random.nextBytes(otpBytes);
        sun.misc.BASE64Encoder encoder =
                    new sun.misc.BASE64Encoder();

        String otpString = encoder.encodeBuffer(otpBytes);

        // Base 64 is not safe to pass around (URL encoding issues) so let's sanitize it
        // replace '+' with '-', '/' with '_', remove newlines and trailing '=';
        StringBuffer sanitized = new StringBuffer();
        for ( int i = 0; i < otpString.length(); i++)
        {
            char c = otpString.charAt(i);
            if ( c == '+') {
                c = '-';
            } else if ( c == '/' ) {
                c = '_';
            } else if ( c == '\n') {
                continue;
            } else if ( c == '=') {
                break;
            }
            sanitized.append(c);
        }

        return sanitized.toString();
    }

    public Phone getPhone(int expandLevel)
    {
        PersistenceService persistenceSvc = PersistenceService.getInstance();
        try {
            return getEntity();
        } finally {
            PersistenceService.getInstance().close();
        }
    }

    /**
     * Returns an instance of Phone identified by id.
     *
     * @param id identifier for the entity
     * @return an instance of Phone
     */
    protected Phone getEntity() {
        EntityManager em = PersistenceService.getInstance().getEntityManager();
        try {
            return (Phone) em.createQuery("SELECT e FROM Phone e where e.phoneNumber = :phoneNumber").setParameter("phoneNumber", id).getSingleResult();
        } catch (NoResultException ex) {
            System.out.println("ex"+ex);
            ex.printStackTrace();
            throw new WebApplicationException(new Throwable("Resource for " + uriInfo.getAbsolutePath() + " does not exist."), 404);
        }
    }

    /**
     * Updates entity using data from newEntity.
     *
     * @param entity the entity to update
     * @param newEntity the entity containing the new data
     * @return the updated entity
     */
    protected Phone updateEntity(Phone entity, Phone newEntity) {
        EntityManager em = PersistenceService.getInstance().getEntityManager();
        Account accountNumber = entity.getAccountNumber();
        Account accountNumberNew = newEntity.getAccountNumber();
        Collection<Notification> notificationCollection = entity.getNotificationCollection();
        Collection<Notification> notificationCollectionNew = newEntity.getNotificationCollection();
        Collection<CallLog> callLogCollection = entity.getCallLogCollection();
        Collection<CallLog> callLogCollectionNew = newEntity.getCallLogCollection();
        entity = em.merge(newEntity);
        if (accountNumber != null && !accountNumber.equals(accountNumberNew)) {
            accountNumber.getPhoneCollection().remove(entity);
        }
        if (accountNumberNew != null && !accountNumberNew.equals(accountNumber)) {
            accountNumberNew.getPhoneCollection().add(entity);
        }
        if(notificationCollection!=null)
            for (Notification value : notificationCollection) {
                if (!notificationCollectionNew.contains(value)) {
                    throw new WebApplicationException(new Throwable("Cannot remove items from notificationCollection"));
                }
            }
        for (Notification value : notificationCollectionNew) {
            if (!notificationCollection.contains(value)) {
                Phone oldEntity = value.getPhoneNumber();
                value.setPhoneNumber(entity);
                if (oldEntity != null && !oldEntity.equals(entity)) {
                    oldEntity.getNotificationCollection().remove(value);
                }
            }
        }
        for (CallLog value : callLogCollection) {
            if (!callLogCollectionNew.contains(value)) {
                throw new WebApplicationException(new Throwable("Cannot remove items from callLogCollection"));
            }
        }
        for (CallLog value : callLogCollectionNew) {
            if (!callLogCollection.contains(value)) {
                Phone oldEntity = value.getPhoneNumberFrom();
                value.setPhoneNumberFrom(entity);
                if (oldEntity != null && !oldEntity.equals(entity)) {
                    oldEntity.getCallLogCollection().remove(value);
                }
            }
        }
        return entity;
    }

    /**
     * Deletes the entity.
     *
     * @param entity the entity to deletle
     */
    protected void deleteEntity(Phone entity) {
        EntityManager em = PersistenceService.getInstance().getEntityManager();
        Account accountNumber = entity.getAccountNumber();
        if (accountNumber != null) {
            accountNumber.getPhoneCollection().remove(entity);
        }
        if (!entity.getNotificationCollection().isEmpty()) {
            throw new WebApplicationException(new Throwable("Cannot delete entity because notificationCollection is not empty."));
        }
        if (!entity.getCallLogCollection().isEmpty()) {
            throw new WebApplicationException(new Throwable("Cannot delete entity because callLogCollection is not empty."));
        }
        em.remove(entity);
    }
}
