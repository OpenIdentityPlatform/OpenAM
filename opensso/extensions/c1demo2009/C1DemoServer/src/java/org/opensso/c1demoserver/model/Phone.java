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
 * $Id: Phone.java,v 1.2 2009/06/11 05:29:42 superpat7 Exp $
 */

package org.opensso.c1demoserver.model;

import java.io.Serializable;
import java.util.Collection;
import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;

@Entity
@Table(name = "phone")
@NamedQueries({@NamedQuery(name = "Phone.findAll", query = "SELECT p FROM Phone p"), @NamedQuery(name = "Phone.findByPhoneNumber", query = "SELECT p FROM Phone p WHERE p.phoneNumber = :phoneNumber"), @NamedQuery(name = "Phone.findByUserName", query = "SELECT p FROM Phone p WHERE p.userName = :userName"), @NamedQuery(name = "Phone.findByPassword", query = "SELECT p FROM Phone p WHERE p.password = :password"), @NamedQuery(name = "Phone.findByAllocatedMinutes", query = "SELECT p FROM Phone p WHERE p.allocatedMinutes = :allocatedMinutes"), @NamedQuery(name = "Phone.findByHeadOfHousehold", query = "SELECT p FROM Phone p WHERE p.headOfHousehold = :headOfHousehold"), @NamedQuery(name = "Phone.findByCanDownloadRingtones", query = "SELECT p FROM Phone p WHERE p.canDownloadRingtones = :canDownloadRingtones"), @NamedQuery(name = "Phone.findByCanDownloadMusic", query = "SELECT p FROM Phone p WHERE p.canDownloadMusic = :canDownloadMusic"), @NamedQuery(name = "Phone.findByCanDownloadVideo", query = "SELECT p FROM Phone p WHERE p.canDownloadVideo = :canDownloadVideo"), @NamedQuery(name = "Phone.findByOtp", query = "SELECT p FROM Phone p WHERE p.otp = :otp")})
public class Phone implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @Basic(optional = false)
    @Column(name = "phone_number")
    private String phoneNumber;
    @Column(name = "user_name")
    private String userName;
    @Column(name = "password")
    private String password;
    @Column(name = "allocated_minutes")
    private Integer allocatedMinutes;
    @Column(name = "head_of_household")
    private Boolean headOfHousehold;
    @Column(name = "can_download_ringtones")
    private Boolean canDownloadRingtones;
    @Column(name = "can_download_music")
    private Boolean canDownloadMusic;
    @Column(name = "can_download_video")
    private Boolean canDownloadVideo;
    @Column(name = "otp")
    private String otp;
    @JoinColumn(name = "account_number", referencedColumnName = "account_number")
    @ManyToOne(optional = false)
    private Account accountNumber;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "phoneNumber")
    private Collection<Notification> notificationCollection;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "phoneNumberFrom")
    private Collection<CallLog> callLogCollection;

    public Phone() {
    }

    public Phone(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
    
    public Phone(Account account, String phoneNumber,  String userName, Integer allocatedMinutes,
            Boolean headOfHousehold, Boolean canDownloadRingtones, Boolean canDownloadMusic,
            Boolean canDownloadVideo)
    {
        this.accountNumber = account;
        this.phoneNumber = phoneNumber;
        this.userName = userName;
        this.allocatedMinutes = allocatedMinutes;
        this.headOfHousehold = headOfHousehold;
        this.canDownloadRingtones = canDownloadRingtones;
        this.canDownloadMusic = canDownloadMusic;
        this.canDownloadVideo = canDownloadVideo;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Integer getAllocatedMinutes() {
        return allocatedMinutes;
    }

    public void setAllocatedMinutes(Integer allocatedMinutes) {
        this.allocatedMinutes = allocatedMinutes;
    }

    public Boolean getHeadOfHousehold() {
        return headOfHousehold;
    }

    public void setHeadOfHousehold(Boolean headOfHousehold) {
        this.headOfHousehold = headOfHousehold;
    }

    public Boolean getCanDownloadRingtones() {
        return canDownloadRingtones;
    }

    public void setCanDownloadRingtones(Boolean canDownloadRingtones) {
        this.canDownloadRingtones = canDownloadRingtones;
    }

    public Boolean getCanDownloadMusic() {
        return canDownloadMusic;
    }

    public void setCanDownloadMusic(Boolean canDownloadMusic) {
        this.canDownloadMusic = canDownloadMusic;
    }

    public Boolean getCanDownloadVideo() {
        return canDownloadVideo;
    }

    public void setCanDownloadVideo(Boolean canDownloadVideo) {
        this.canDownloadVideo = canDownloadVideo;
    }

    public String getOtp() {
        return otp;
    }

    public void setOtp(String otp) {
        this.otp = otp;
    }

    public Account getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(Account accountNumber) {
        this.accountNumber = accountNumber;
    }

    public Collection<Notification> getNotificationCollection() {
        return notificationCollection;
    }

    public void setNotificationCollection(Collection<Notification> notificationCollection) {
        this.notificationCollection = notificationCollection;
    }

    public Collection<CallLog> getCallLogCollection() {
        return callLogCollection;
    }

    public void setCallLogCollection(Collection<CallLog> callLogCollection) {
         
        this.callLogCollection = callLogCollection;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (phoneNumber != null ? phoneNumber.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof Phone)) {
            return false;
        }
        Phone other = (Phone) object;
        if ((this.phoneNumber == null && other.phoneNumber != null) || (this.phoneNumber != null && !this.phoneNumber.equals(other.phoneNumber))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "org.opensso.c1demoserver.model.Phone[phoneNumber=" + phoneNumber + "]";
    }

}
