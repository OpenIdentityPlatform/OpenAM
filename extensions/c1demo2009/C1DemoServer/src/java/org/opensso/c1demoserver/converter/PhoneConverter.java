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
 * $Id: PhoneConverter.java,v 1.2 2009/06/11 05:29:41 superpat7 Exp $
 */

package org.opensso.c1demoserver.converter;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.opensso.c1demoserver.model.Phone;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlAttribute;
import javax.ws.rs.core.UriBuilder;
import javax.persistence.EntityManager;
import org.opensso.c1demoserver.model.Notification;
import java.util.Collection;
import org.opensso.c1demoserver.model.CallLog;
import org.opensso.c1demoserver.model.Account;

@XmlRootElement(name = "phone")
public class PhoneConverter {
    private Phone entity;
    private URI uri;
    private int expandLevel;
  
    /** Creates a new instance of PhoneConverter */
    public PhoneConverter() {
        entity = new Phone();
    }

    /**
     * Creates a new instance of PhoneConverter.
     *
     * @param entity associated entity
     * @param uri associated uri
     * @param expandLevel indicates the number of levels the entity graph should be expanded@param isUriExtendable indicates whether the uri can be extended
     */
    public PhoneConverter(Phone entity, URI uri, int expandLevel, boolean isUriExtendable) {
        this.entity = entity;
        this.uri = (isUriExtendable) ? UriBuilder.fromUri(uri).path(entity.getPhoneNumber() + "/").build() : uri;
        this.expandLevel = expandLevel;
        getAccountNumber();
        getNotificationCollection();
        getCallLogCollection();
    }

    /**
     * Creates a new instance of PhoneConverter.
     *
     * @param entity associated entity
     * @param uri associated uri
     * @param expandLevel indicates the number of levels the entity graph should be expanded
     */
    public PhoneConverter(Phone entity, URI uri, int expandLevel) {
        this(entity, uri, expandLevel, false);
    }

    /**
     * Getter for phoneNumber.
     *
     * @return value for phoneNumber
     */
    @XmlElement
    public String getPhoneNumber() {
        return (expandLevel > 0) ? entity.getPhoneNumber() : null;
    }

    /**
     * Setter for phoneNumber.
     *
     * @param value the value to set
     */
    public void setPhoneNumber(String value) {
        entity.setPhoneNumber(value);
    }

    /**
     * Getter for userName.
     *
     * @return value for userName
     */
    @XmlElement
    public String getUserName() {
        return (expandLevel > 0) ? entity.getUserName() : null;
    }

    /**
     * Setter for userName.
     *
     * @param value the value to set
     */
    public void setUserName(String value) {
        entity.setUserName(value);
    }

    /**
     * Getter for allocatedMinutes.
     *
     * @return value for allocatedMinutes
     */
    @XmlElement
    public Integer getAllocatedMinutes() {
        return (expandLevel > 0) ? entity.getAllocatedMinutes() : null;
    }

    /**
     * Setter for allocatedMinutes.
     *
     * @param value the value to set
     */
    public void setAllocatedMinutes(Integer value) {
        entity.setAllocatedMinutes(value);
    }

    /**
     * Getter for headOfHousehold.
     *
     * @return value for headOfHousehold
     */
    @XmlElement
    public Boolean getHeadOfHousehold() {
        return (expandLevel > 0) ? entity.getHeadOfHousehold() : null;
    }

    /**
     * Setter for headOfHousehold.
     *
     * @param value the value to set
     */
    public void setHeadOfHousehold(Boolean value) {
        entity.setHeadOfHousehold(value);
    }

    /**
     * Getter for canDownloadRingtones.
     *
     * @return value for canDownloadRingtones
     */
    @XmlElement
    public Boolean getCanDownloadRingtones() {
        return (expandLevel > 0) ? entity.getCanDownloadRingtones() : null;
    }

    /**
     * Setter for canDownloadRingtones.
     *
     * @param value the value to set
     */
    public void setCanDownloadRingtones(Boolean value) {
        entity.setCanDownloadRingtones(value);
    }

    /**
     * Getter for canDownloadMusic.
     *
     * @return value for canDownloadMusic
     */
    @XmlElement
    public Boolean getCanDownloadMusic() {
        return (expandLevel > 0) ? entity.getCanDownloadMusic() : null;
    }

    /**
     * Setter for canDownloadMusic.
     *
     * @param value the value to set
     */
    public void setCanDownloadMusic(Boolean value) {
        entity.setCanDownloadMusic(value);
    }

    /**
     * Getter for canDownloadVideo.
     *
     * @return value for canDownloadVideo
     */
    @XmlElement
    public Boolean getCanDownloadVideo() {
        return (expandLevel > 0) ? entity.getCanDownloadVideo() : null;
    }

    /**
     * Setter for canDownloadVideo.
     *
     * @param value the value to set
     */
    public void setCanDownloadVideo(Boolean value) {
        entity.setCanDownloadVideo(value);
    }

    /**
     * Getter for accountNumber.
     *
     * @return value for accountNumber
     */
    @XmlElement
    public AccountConverter getAccountNumber() {
        if (expandLevel > 0) {
            if (entity.getAccountNumber() != null) {
                return new AccountConverter(entity.getAccountNumber(), uri.resolve("accountNumber/"), expandLevel - 1, false);
            }
        }
        return null;
    }

    /**
     * Setter for accountNumber.
     *
     * @param value the value to set
     */
    public void setAccountNumber(AccountConverter value) {
        entity.setAccountNumber((value != null) ? value.getEntity() : null);
    }

    /**
     * Getter for notificationCollection.
     *
     * @return value for notificationCollection
     */
    @XmlElement
    public NotificationsConverter getNotificationCollection() {
        if (expandLevel > 0) {
            if (entity.getNotificationCollection() != null) {
                return new NotificationsConverter(entity.getNotificationCollection(), uri.resolve("notificationCollection/"), expandLevel - 1);
            }
        }
        return null;
    }

    /**
     * Setter for notificationCollection.
     *
     * @param value the value to set
     */
    public void setNotificationCollection(NotificationsConverter value) {
        entity.setNotificationCollection((value != null) ? value.getEntities() : null);
    }

    /**
     * Getter for callLogCollection.
     *
     * @return value for callLogCollection
     */
    @XmlElement
    public CallLogsConverter getCallLogCollection() {
        if (expandLevel > 0) {
            if (entity.getCallLogCollection() != null) {
                return new CallLogsConverter(entity.getCallLogCollection(), uri.resolve("callLogCollection/"), expandLevel - 1);
            }
        }
        return null;
    }

    /**
     * Setter for callLogCollection.
     *
     * @param value the value to set
     */
    public void setCallLogCollection(CallLogsConverter value) {
        entity.setCallLogCollection((value != null) ? value.getEntities() : null);
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
     * Returns the Phone entity.
     *
     * @return an entity
     */
    @XmlTransient
    public Phone getEntity() {
        if (entity.getPhoneNumber() == null) {
            PhoneConverter converter = UriResolver.getInstance().resolve(PhoneConverter.class, uri);
            if (converter != null) {
                entity = converter.getEntity();
            }
        }
        return entity;
    }

    /**
     * Returns the resolved Phone entity.
     *
     * @return an resolved entity
     */
    public Phone resolveEntity(EntityManager em) {
        Account accountNumber = entity.getAccountNumber();
        if (accountNumber != null) {
            entity.setAccountNumber(em.getReference(Account.class, accountNumber.getAccountNumber()));
        }
        Collection<Notification> notificationCollection = entity.getNotificationCollection();
        Collection<Notification> newnotificationCollection = new java.util.ArrayList<Notification>();
        if(notificationCollection!=null)
            for (Notification item : notificationCollection) {
                newnotificationCollection.add(em.getReference(Notification.class, item.getNotificationId()));
            }
        entity.setNotificationCollection(newnotificationCollection);
        Collection<CallLog> callLogCollection = entity.getCallLogCollection();
        Collection<CallLog> newcallLogCollection = new java.util.ArrayList<CallLog>();
        for (CallLog item : callLogCollection) {
            newcallLogCollection.add(em.getReference(CallLog.class, item.getCallId()));
        }
        entity.setCallLogCollection(newcallLogCollection);
        return entity;
    }
}
