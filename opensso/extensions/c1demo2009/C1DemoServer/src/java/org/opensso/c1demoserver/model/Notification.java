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
 * $Id: Notification.java,v 1.2 2009/06/11 05:29:42 superpat7 Exp $
 */

package org.opensso.c1demoserver.model;

import java.io.Serializable;
import java.util.Date;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Entity
@Table(name = "notification")
@NamedQueries({@NamedQuery(name = "Notification.findAll", query = "SELECT n FROM Notification n"), @NamedQuery(name = "Notification.findByNotificationTime", query = "SELECT n FROM Notification n WHERE n.notificationTime = :notificationTime"), @NamedQuery(name = "Notification.findByMessageText", query = "SELECT n FROM Notification n WHERE n.messageText = :messageText"), @NamedQuery(name = "Notification.findByNotificationId", query = "SELECT n FROM Notification n WHERE n.notificationId = :notificationId")})
public class Notification implements Serializable {
    private static final long serialVersionUID = 1L;
    @Basic(optional = false)
    @Column(name = "notification_time")
    @Temporal(TemporalType.TIMESTAMP)
    private Date notificationTime;
    @Basic(optional = false)
    @Column(name = "message_text")
    private String messageText;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "notification_id")
    private Integer notificationId;
    @JoinColumn(name = "phone_number", referencedColumnName = "phone_number")
    @ManyToOne(optional = false)
    private Phone phoneNumber;

    public Notification() {
    }

    public Notification(Integer notificationId) {
        this.notificationId = notificationId;
    }

    public Notification(Integer notificationId, Date notificationTime, String messageText) {
        this.notificationId = notificationId;
        this.notificationTime = notificationTime;
        this.messageText = messageText;
    }

    public Date getNotificationTime() {
        return notificationTime;
    }

    public void setNotificationTime(Date notificationTime) {
        this.notificationTime = notificationTime;
    }

    public String getMessageText() {
        return messageText;
    }

    public void setMessageText(String messageText) {
        this.messageText = messageText;
    }

    public Integer getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(Integer notificationId) {
        this.notificationId = notificationId;
    }

    public Phone getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(Phone phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (notificationId != null ? notificationId.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof Notification)) {
            return false;
        }
        Notification other = (Notification) object;
        if ((this.notificationId == null && other.notificationId != null) || (this.notificationId != null && !this.notificationId.equals(other.notificationId))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "org.opensso.c1demoserver.model.Notification[notificationId=" + notificationId + "]";
    }

}
