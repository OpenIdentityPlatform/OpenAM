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
 * $Id: CallLog.java,v 1.2 2009/06/11 05:29:42 superpat7 Exp $
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
@Table(name = "call_log")
@NamedQueries({@NamedQuery(name = "CallLog.findAll", query = "SELECT c FROM CallLog c"), @NamedQuery(name = "CallLog.findByPhoneNumberTo", query = "SELECT c FROM CallLog c WHERE c.phoneNumberTo = :phoneNumberTo"), @NamedQuery(name = "CallLog.findByCallTime", query = "SELECT c FROM CallLog c WHERE c.callTime = :callTime"), @NamedQuery(name = "CallLog.findByCallDurationSecs", query = "SELECT c FROM CallLog c WHERE c.callDurationSecs = :callDurationSecs"), @NamedQuery(name = "CallLog.findByCallId", query = "SELECT c FROM CallLog c WHERE c.callId = :callId")})
public class CallLog implements Serializable {
    private static final long serialVersionUID = 1L;
    @Basic(optional = false)
    @Column(name = "phone_number_to")
    private String phoneNumberTo;
    @Basic(optional = false)
    @Column(name = "call_time")
    @Temporal(TemporalType.TIMESTAMP)
    private Date callTime;
    @Basic(optional = false)
    @Column(name = "call_duration_secs")
    private int callDurationSecs;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "call_id")
    private Integer callId;
    @JoinColumn(name = "phone_number_from", referencedColumnName = "phone_number")
    @ManyToOne(optional = false)
    private Phone phoneNumberFrom;

    public CallLog() {
    }

    public CallLog(Integer callId) {
        this.callId = callId;
    }

    public CallLog(Integer callId, String phoneNumberTo, Date callTime, int callDurationSecs) {
        this.callId = callId;
        this.phoneNumberTo = phoneNumberTo;
        this.callTime = callTime;
        this.callDurationSecs = callDurationSecs;
    }

    public Date getCallTime() {
        return callTime;
    }

    public void setCallTime(Date callTime) {
        this.callTime = callTime;
    }

    public int getCallDurationSecs() {
        return callDurationSecs;
    }

    public void setCallDurationSecs(int callDurationSecs) {
        this.callDurationSecs = callDurationSecs;
    }

    public Integer getCallId() {
        return callId;
    }

    public void setCallId(Integer callId) {
        this.callId = callId;
    }

    public Phone getPhoneNumberFrom() {
        return phoneNumberFrom;
    }

    public void setPhoneNumberFrom(Phone phoneNumberFrom) {
        this.phoneNumberFrom = phoneNumberFrom;
    }

    public String getPhoneNumberTo() {
        return phoneNumberTo;
    }

    public void setPhoneNumberTo(String phoneNumberTo) {
        this.phoneNumberTo = phoneNumberTo;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (callId != null ? callId.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof CallLog)) {
            return false;
        }
        CallLog other = (CallLog) object;
        if ((this.callId == null && other.callId != null) || (this.callId != null && !this.callId.equals(other.callId))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "org.opensso.c1demoserver.model.CallLog[callId=" + callId + "]";
    }

}
