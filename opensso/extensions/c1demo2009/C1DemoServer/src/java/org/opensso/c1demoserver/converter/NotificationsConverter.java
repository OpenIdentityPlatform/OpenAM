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
 * $Id: NotificationsConverter.java,v 1.2 2009/06/11 05:29:41 superpat7 Exp $
 */

package org.opensso.c1demoserver.converter;

import java.net.URI;
import java.util.Collection;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlAttribute;
import java.util.ArrayList;
import org.opensso.c1demoserver.model.Notification;

@XmlRootElement(name = "notifications")
public class NotificationsConverter {
    private Collection<Notification> entities;
    private Collection<NotificationConverter> items;
    private URI uri;
    private int expandLevel;
  
    /** Creates a new instance of NotificationsConverter */
    public NotificationsConverter() {
    }

    /**
     * Creates a new instance of NotificationsConverter.
     *
     * @param entities associated entities
     * @param uri associated uri
     * @param expandLevel indicates the number of levels the entity graph should be expanded
     */
    public NotificationsConverter(Collection<Notification> entities, URI uri, int expandLevel) {
        this.entities = entities;
        this.uri = uri;
        this.expandLevel = expandLevel;
        getNotification();
    }

    /**
     * Returns a collection of NotificationConverter.
     *
     * @return a collection of NotificationConverter
     */
    @XmlElement
    public Collection<NotificationConverter> getNotification() {
        if (items == null) {
            items = new ArrayList<NotificationConverter>();
        }
        if (entities != null) {
            items.clear();
            for (Notification entity : entities) {
                items.add(new NotificationConverter(entity, uri, expandLevel, true));
            }
        }
        return items;
    }

    /**
     * Sets a collection of NotificationConverter.
     *
     * @param a collection of NotificationConverter to set
     */
    public void setNotification(Collection<NotificationConverter> items) {
        this.items = items;
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
     * Returns a collection Notification entities.
     *
     * @return a collection of Notification entities
     */
    @XmlTransient
    public Collection<Notification> getEntities() {
        entities = new ArrayList<Notification>();
        if (items != null) {
            for (NotificationConverter item : items) {
                entities.add(item.getEntity());
            }
        }
        return entities;
    }
}
