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
 * $Id: PhoneResource.java,v 1.3 2009/06/11 05:58:51 superpat7 Exp $
 */

package org.opensso.c1demoserver.service;

import java.security.NoSuchAlgorithmException;
import javax.ws.rs.Path;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.Consumes;
import javax.ws.rs.QueryParam;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import com.sun.jersey.api.core.ResourceContext;
import java.io.StringReader;
import java.net.URLDecoder;
import java.security.SecureRandom;
import javax.ws.rs.WebApplicationException;
import javax.persistence.NoResultException;
import javax.persistence.EntityManager;
import javax.xml.bind.JAXBException;
import org.opensso.c1demoserver.model.Notification;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.POST;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import org.opensso.c1demoserver.model.CallLog;
import org.opensso.c1demoserver.model.Account;
import org.opensso.c1demoserver.converter.PhoneConverter;
import org.opensso.c1demoserver.model.Auth2;
import org.opensso.c1demoserver.model.OTP;
import org.opensso.c1demoserver.model.Phone;
import org.opensso.c1demoserver.model.SetPassword;
import org.opensso.c1demoserver.model.SetQuestion;

public class PhoneResource {
    @Context
    protected UriInfo uriInfo;
    @Context
    protected ResourceContext resourceContext;
    protected String id;
  
    /** Creates a new instance of PhoneResource */
    public PhoneResource() {
    }

    public void setId(String id) {
        this.id = id;
    }

    /**
     * Get method for retrieving an instance of Phone identified by id in XML format.
     *
     * @param id identifier for the entity
     * @return an instance of PhoneConverter
     */
    @GET
    @Produces({"application/xml", "application/json"})
    public PhoneConverter get(@QueryParam("expandLevel")
    @DefaultValue("1")
    int expandLevel) {
        Phone phone = getPhone(expandLevel);
        if(phone!=null) {
            return new PhoneConverter(phone, uriInfo.getAbsolutePath(), expandLevel);
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
    public Response post(
    @QueryParam("action")
    String action,
    @QueryParam("content")
    String content,
    @Context
    HttpServletRequest request) {
        try {
            if ( action.equals("patch")) {
                return patch(content, request);
            }

            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        } catch (Exception e) {
            e.printStackTrace();
            throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
        }

    }

    private Response patch(String content, HttpServletRequest request) throws JAXBException {
        JAXBContext ctx = JAXBContext.newInstance(new Class[]{PhoneConverter.class});
        Unmarshaller um = ctx.createUnmarshaller();
        PhoneConverter phoneConverter = (PhoneConverter) um.unmarshal(new StringReader(content));

        Phone phonePatch = phoneConverter.getEntity();

        // Get phone for this URI
        Phone phone = this.getEntity();

        // Update phone resource and write it back
        if ( phonePatch.getAllocatedMinutes() != null ) {
            phone.setAllocatedMinutes(phonePatch.getAllocatedMinutes());
        }
        if ( phonePatch.getCanDownloadRingtones() != null ) {
            phone.setCanDownloadRingtones(phonePatch.getCanDownloadRingtones());
        }
        if ( phonePatch.getCanDownloadMusic() != null ) {
            phone.setCanDownloadMusic(phonePatch.getCanDownloadMusic());
        }
        if ( phonePatch.getCanDownloadVideo() != null ) {
            phone.setCanDownloadVideo(phonePatch.getCanDownloadVideo());
        }
        if ( phonePatch.getHeadOfHousehold() != null ) {
            phone.setHeadOfHousehold(phonePatch.getHeadOfHousehold());
        }

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

    /**
     * Returns a dynamic instance of AccountResource used for entity navigation.
     *
     * @param id identifier for the parent entity
     * @return an instance of AccountResource
     */
    @Path("accountNumber/")
    public AccountResource getAccountNumberResource() {
        AccountNumberResourceSub resource = resourceContext.getResource(AccountNumberResourceSub.class);
        resource.setParent(getEntity());
        return resource;
    }

    /**
     * Returns a dynamic instance of NotificationsResource used for entity navigation.
     *
     * @param id identifier for the parent entity
     * @return an instance of NotificationsResource
     */
    @Path("notificationCollection/")
    public NotificationsResource getNotificationCollectionResource() {
        NotificationCollectionResourceSub resource = resourceContext.getResource(NotificationCollectionResourceSub.class);
        resource.setParent(getEntity());
        return resource;
    }

    /**
     * Returns a dynamic instance of CallLogsResource used for entity navigation.
     *
     * @param id identifier for the parent entity
     * @return an instance of CallLogsResource
     */
    @Path("callLogCollection/")
    public CallLogsResource getCallLogCollectionResource() {
        CallLogCollectionResourceSub resource = resourceContext.getResource(CallLogCollectionResourceSub.class);
        resource.setParent(getEntity());
        return resource;
    }

    public static class AccountNumberResourceSub extends AccountResource {

        private Phone parent;

        public void setParent(Phone parent) {
            this.parent = parent;
        }

        @Override
        protected Account getEntity() {
            Account entity = parent.getAccountNumber();
            if (entity == null) {
                throw new WebApplicationException(new Throwable("Resource for " + uriInfo.getAbsolutePath() + " does not exist."), 404);
            }
            return entity;
        }
    }

    public static class NotificationCollectionResourceSub extends NotificationsResource {

        private Phone parent;

        public void setParent(Phone parent) {
            this.parent = parent;
        }

        @Override
        protected Collection<Notification> getEntities(int start, int max, String query) {
            Collection<Notification> result = new java.util.ArrayList<Notification>();
            int index = 0;
            for (Notification e : parent.getNotificationCollection()) {
                if (index >= start && (index - start) < max) {
                    result.add(e);
                }
                index++;
            }
            return result;
        }
    }

    public static class CallLogCollectionResourceSub extends CallLogsResource {

        private Phone parent;

        public void setParent(Phone parent) {
            this.parent = parent;
        }

        @Override
        protected Collection<CallLog> getEntities(int start, int max, String query) {
            Collection<CallLog> result = new java.util.ArrayList<CallLog>();
            int index = 0;
            for (CallLog e : parent.getCallLogCollection()) {
                if (index >= start && (index - start) < max) {
                    result.add(e);
                }
                index++;
            }
            return result;
        }
    }
}
