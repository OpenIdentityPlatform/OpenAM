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
 * $Id: ChallengesResource.java,v 1.2 2009/06/11 05:29:44 superpat7 Exp $
 */

package org.opensso.c1demoserver.service;

import java.util.Collection;
import javax.ws.rs.Path;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import com.sun.jersey.api.core.ResourceContext;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.persistence.EntityManager;
import org.opensso.c1demoserver.model.Notification;
import org.opensso.c1demoserver.model.CallLog;
import org.opensso.c1demoserver.model.Account;
import org.opensso.c1demoserver.converter.ChallengesConverter;
import org.opensso.c1demoserver.model.Phone;

@Path("/challenges/")
public class ChallengesResource {
    @Context
    protected UriInfo uriInfo;
    @Context
    protected ResourceContext resourceContext;
  
    /** Creates a new instance of ChallengesResource */
    public ChallengesResource() {
    }

    /**
     * Get method for retrieving a collection of Phone instance in XML format.
     *
     * @return an instance of ChallengesConverter
     */
    @GET
    @Produces({"application/xml", "application/json"})
    public ChallengesConverter get(@QueryParam("start")
    @DefaultValue("0")
    int start, @QueryParam("max")
    @DefaultValue("10")
    int max, @QueryParam("expandLevel")
    @DefaultValue("1")
    int expandLevel, @QueryParam("query")
    @DefaultValue("SELECT e FROM Phone e")
    String query) {
        PersistenceService persistenceSvc = PersistenceService.getInstance();
        try {
            persistenceSvc.beginTx();
            return new ChallengesConverter(getEntities(start, max, query), uriInfo.getAbsolutePath(), expandLevel);
        } finally {
            persistenceSvc.commitTx();
            persistenceSvc.close();
        }
    }

    /**
     * Returns a dynamic instance of ChallengeResource used for entity navigation.
     *
     * @return an instance of ChallengeResource
     */
    @Path("{phoneNumber}/")
    public ChallengeResource getChallengeResource(@PathParam("phoneNumber")
    String id) {
        ChallengeResource resource = resourceContext.getResource(ChallengeResource.class);
        resource.setId(id);
        return resource;
    }

    /**
     * Returns all the entities associated with this resource.
     *
     * @return a collection of Phone instances
     */
    protected Collection<Phone> getEntities(int start, int max, String query) {
        EntityManager em = PersistenceService.getInstance().getEntityManager();
        return em.createQuery(query).setFirstResult(start).setMaxResults(max).getResultList();
    }

    private String attrHelpString(List<Object> values)
    {
        if(values!=null && values.size()>0)
            return values.get(0).toString();
        return null;
    }
    private Integer attrHelpInteger(List<Object> values)
    {
        if(values!=null && values.size()>0)
            return Integer.parseInt(values.get(0).toString());
        return null;
    }
    private Boolean attrHelpBoolean(List<Object> values)
    {
        if(values!=null && values.size()>0)
            return Boolean.parseBoolean(values.get(0).toString());
        return null;
    }

    /**
     * Persist the given entity.
     *
     * @param entity the entity to persist
     */
    protected void createEntity(Phone entity) {
        EntityManager em = PersistenceService.getInstance().getEntityManager();
        em.persist(entity);
        Account accountNumber = entity.getAccountNumber();
        if (accountNumber != null) {
            accountNumber.getPhoneCollection().add(entity);
        }
        for (Notification value : entity.getNotificationCollection()) {
            Phone oldEntity = value.getPhoneNumber();
            value.setPhoneNumber(entity);
            if (oldEntity != null) {
                oldEntity.getNotificationCollection().remove(entity);
            }
        }
        for (CallLog value : entity.getCallLogCollection()) {
            Phone oldEntity = value.getPhoneNumberFrom();
            value.setPhoneNumberFrom(entity);
            if (oldEntity != null) {
                oldEntity.getCallLogCollection().remove(entity);
            }
        }
    }
}
