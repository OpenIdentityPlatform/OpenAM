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
 * $Id: NotificationConverter.java,v 1.2 2009/06/11 05:29:42 superpat7 Exp $
 */

package org.opensso.c1demoserver.converter;

import java.net.URI;
import java.util.Date;
import org.opensso.c1demoserver.model.Notification;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlAttribute;
import javax.ws.rs.core.UriBuilder;
import javax.persistence.EntityManager;
import org.opensso.c1demoserver.model.Phone;

@XmlRootElement(name = "notification")
public class NotificationConverter {
    private Notification entity;
    private URI uri;
    private int expandLevel;
  
    /** Creates a new instance of NotificationConverter */
    public NotificationConverter() {
        entity = new Notification();
    }

    /**
     * Creates a new instance of NotificationConverter.
     *
     * @param entity associated entity
     * @param uri associated uri
     * @param expandLevel indicates the number of levels the entity graph should be expanded@param isUriExtendable indicates whether the uri can be extended
     */
    public NotificationConverter(Notification entity, URI uri, int expandLevel, boolean isUriExtendable) {
        this.entity = entity;
        this.uri = (isUriExtendable) ? UriBuilder.fromUri(uri).path(entity.getNotificationId() + "/").build() : uri;
        this.expandLevel = expandLevel;
        getPhoneNumber();
    }

    /**
     * Creates a new instance of NotificationConverter.
     *
     * @param entity associated entity
     * @param uri associated uri
     * @param expandLevel indicates the number of levels the entity graph should be expanded
     */
    public NotificationConverter(Notification entity, URI uri, int expandLevel) {
        this(entity, uri, expandLevel, false);
    }

    /**
     * Getter for notificationTime.
     *
     * @return value for notificationTime
     */
    @XmlElement
    public Date getNotificationTime() {
        return (expandLevel > 0) ? entity.getNotificationTime() : null;
    }

    /**
     * Setter for notificationTime.
     *
     * @param value the value to set
     */
    public void setNotificationTime(Date value) {
        entity.setNotificationTime(value);
    }

    /**
     * Getter for messageText.
     *
     * @return value for messageText
     */
    @XmlElement
    public String getMessageText() {
        return (expandLevel > 0) ? entity.getMessageText() : null;
    }

    /**
     * Setter for messageText.
     *
     * @param value the value to set
     */
    public void setMessageText(String value) {
        entity.setMessageText(value);
    }

    /**
     * Getter for notificationId.
     *
     * @return value for notificationId
     */
    @XmlElement
    public Integer getNotificationId() {
        return (expandLevel > 0) ? entity.getNotificationId() : null;
    }

    /**
     * Setter for notificationId.
     *
     * @param value the value to set
     */
    public void setNotificationId(Integer value) {
        entity.setNotificationId(value);
    }

    /**
     * Getter for phoneNumber.
     *
     * @return value for phoneNumber
     */
    @XmlElement
    public PhoneConverter getPhoneNumber() {
        if (expandLevel > 0) {
            if (entity.getPhoneNumber() != null) {
                return new PhoneConverter(entity.getPhoneNumber(), uri.resolve("phoneNumber/"), expandLevel - 1, false);
            }
        }
        return null;
    }

    /**
     * Setter for phoneNumber.
     *
     * @param value the value to set
     */
    public void setPhoneNumber(PhoneConverter value) {
        entity.setPhoneNumber((value != null) ? value.getEntity() : null);
    }

    /**
     * Returns the URI associated with this converter.
     *
     * @return the uri
     */
    @XmlAttribute
    public URI getUri() {
        return uri;
    }

    /**
     * Sets the URI for this reference converter.
     *
     */
    public void setUri(URI uri) {
        this.uri = uri;
    }

    /**
     * Returns the Notification entity.
     *
     * @return an entity
     */
    @XmlTransient
    public Notification getEntity() {
        if (entity.getNotificationId() == null) {
            NotificationConverter converter = UriResolver.getInstance().resolve(NotificationConverter.class, uri);
            if (converter != null) {
                entity = converter.getEntity();
            }
        }
        return entity;
    }

    /**
     * Returns the resolved Notification entity.
     *
     * @return an resolved entity
     */
    public Notification resolveEntity(EntityManager em) {
        Phone phoneNumber = entity.getPhoneNumber();
        if (phoneNumber != null) {
            entity.setPhoneNumber(em.getReference(Phone.class, phoneNumber.getPhoneNumber()));
        }
        return entity;
    }
}
