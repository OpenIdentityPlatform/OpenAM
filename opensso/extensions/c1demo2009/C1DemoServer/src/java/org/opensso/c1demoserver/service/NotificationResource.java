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
 * $Id: NotificationResource.java,v 1.2 2009/06/11 05:29:43 superpat7 Exp $
 */

package org.opensso.c1demoserver.service;

import javax.ws.rs.Path;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.DELETE;
import javax.ws.rs.Produces;
import javax.ws.rs.Consumes;
import javax.ws.rs.QueryParam;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import com.sun.jersey.api.core.ResourceContext;
import javax.ws.rs.WebApplicationException;
import javax.persistence.NoResultException;
import javax.persistence.EntityManager;
import org.opensso.c1demoserver.model.Notification;
import org.opensso.c1demoserver.model.Phone;
import org.opensso.c1demoserver.converter.NotificationConverter;

public class NotificationResource {
    @Context
    protected UriInfo uriInfo;
    @Context
    protected ResourceContext resourceContext;
    protected Integer id;
  
    /** Creates a new instance of NotificationResource */
    public NotificationResource() {
    }

    public void setId(Integer id) {
        this.id = id;
    }

    /**
     * Get method for retrieving an instance of Notification identified by id in XML format.
     *
     * @param id identifier for the entity
     * @return an instance of NotificationConverter
     */
    @GET
    @Produces({"application/xml", "application/json"})
    public NotificationConverter get(@QueryParam("expandLevel")
    @DefaultValue("1")
    int expandLevel) {
        PersistenceService persistenceSvc = PersistenceService.getInstance();
        try {
            persistenceSvc.beginTx();
            return new NotificationConverter(getEntity(), uriInfo.getAbsolutePath(), expandLevel);
        } finally {
            PersistenceService.getInstance().close();
        }
    }

    /**
     * Put method for updating an instance of Notification identified by id using XML as the input format.
     *
     * @param id identifier for the entity
     * @param data an NotificationConverter entity that is deserialized from a XML stream
     */
    @PUT
    @Consumes({"application/xml", "application/json"})
    public void put(NotificationConverter data) {
        PersistenceService persistenceSvc = PersistenceService.getInstance();
        try {
            persistenceSvc.beginTx();
            EntityManager em = persistenceSvc.getEntityManager();
            updateEntity(getEntity(), data.resolveEntity(em));
            persistenceSvc.commitTx();
        } finally {
            persistenceSvc.close();
        }
    }

    /**
     * Delete method for deleting an instance of Notification identified by id.
     *
     * @param id identifier for the entity
     */
    @DELETE
    public void delete() {
        PersistenceService persistenceSvc = PersistenceService.getInstance();
        try {
            persistenceSvc.beginTx();
            deleteEntity(getEntity());
            persistenceSvc.commitTx();
        } finally {
            persistenceSvc.close();
        }
    }

    /**
     * Returns an instance of Notification identified by id.
     *
     * @param id identifier for the entity
     * @return an instance of Notification
     */
    protected Notification getEntity() {
        EntityManager em = PersistenceService.getInstance().getEntityManager();
        try {
            return (Notification) em.createQuery("SELECT e FROM Notification e where e.notificationId = :notificationId").setParameter("notificationId", id).getSingleResult();
        } catch (NoResultException ex) {
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
    protected Notification updateEntity(Notification entity, Notification newEntity) {
        EntityManager em = PersistenceService.getInstance().getEntityManager();
        Phone phoneNumber = entity.getPhoneNumber();
        Phone phoneNumberNew = newEntity.getPhoneNumber();
        entity = em.merge(newEntity);
        if (phoneNumber != null && !phoneNumber.equals(phoneNumberNew)) {
            phoneNumber.getNotificationCollection().remove(entity);
        }
        if (phoneNumberNew != null && !phoneNumberNew.equals(phoneNumber)) {
            phoneNumberNew.getNotificationCollection().add(entity);
        }
        return entity;
    }

    /**
     * Deletes the entity.
     *
     * @param entity the entity to deletle
     */
    protected void deleteEntity(Notification entity) {
        EntityManager em = PersistenceService.getInstance().getEntityManager();
        Phone phoneNumber = entity.getPhoneNumber();
        if (phoneNumber != null) {
            phoneNumber.getNotificationCollection().remove(entity);
        }
        em.remove(entity);
    }

    /**
     * Returns a dynamic instance of PhoneResource used for entity navigation.
     *
     * @param id identifier for the parent entity
     * @return an instance of PhoneResource
     */
    @Path("phoneNumber/")
    public PhoneResource getPhoneNumberResource() {
        PhoneNumberResourceSub resource = resourceContext.getResource(PhoneNumberResourceSub.class);
        resource.setParent(getEntity());
        return resource;
    }

    public static class PhoneNumberResourceSub extends PhoneResource {

        private Notification parent;

        public void setParent(Notification parent) {
            this.parent = parent;
        }

        @Override
        protected Phone getEntity() {
            Phone entity = parent.getPhoneNumber();
            if (entity == null) {
                throw new WebApplicationException(new Throwable("Resource for " + uriInfo.getAbsolutePath() + " does not exist."), 404);
            }
            return entity;
        }
    }
}
