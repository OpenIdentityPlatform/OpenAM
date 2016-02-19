/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2005 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: SessionInfo.java,v 1.3 2008/06/25 05:41:31 qcheng Exp $
 *
 * Portions Copyrighted 2011-2016 ForgeRock AS.
 */
package com.iplanet.dpro.session.share;

import static java.util.concurrent.TimeUnit.*;
import static org.forgerock.openam.utils.Time.*;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sun.identity.shared.xml.XMLUtils;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.concurrent.TimeUnit;

/**
 * <code>SessionInfo</code> class holds all the information about the
 * <code>Session</code>
 */
public class SessionInfo {

    private static final char QUOTE = '\"';
    private static final char NL = '\n';

    private String sid;
    private String secret;
    private String stype;
    private String cid;
    private String cdomain;
    private long maxtime;
    private long maxidle;
    private long maxcaching;
    private long expiryTime;
    private long lastActivityTime;
    private String state;
    private Hashtable<String, String> properties = new Hashtable<String, String>();

    /**
     * Constructs <code> SessionInfo </code>
     */
    public SessionInfo() {
    }

    /**
     * translates the <code>Session</code> Information to an XML document
     * String based
     *
     * @return An XML String representing the information
     */
    public String toXMLString() {
        StringBuilder xml = new StringBuilder(200);
        xml.append("<Session sid=").append(QUOTE).append(sid).append(QUOTE)
                .append(" stype=").append(QUOTE).append(stype).append(QUOTE)
                .append(" cid=").append(QUOTE).append(
                XMLUtils.escapeSpecialCharacters(cid)).append(QUOTE)
                .append(" cdomain=").append(QUOTE).append(
                XMLUtils.escapeSpecialCharacters(cdomain))
                .append(QUOTE).append(" maxtime=").append(QUOTE)
                .append(maxtime).append(QUOTE).append(" maxidle=")
                .append(QUOTE).append(maxidle).append(QUOTE).append(
                " maxcaching=").append(QUOTE).append(maxcaching)
                .append(QUOTE).append(" timeidle=").append(QUOTE).append(
                getTimeIdle()).append(QUOTE).append(" timeleft=").append(
                QUOTE).append(getTimeLeft()).append(QUOTE).append(" state=")
                .append(QUOTE).append(state).append(QUOTE).append(">").append(
                NL);

        if (properties != null) {
            Enumeration enumerator = properties.keys();
            while (enumerator.hasMoreElements()) {
                String name = (String) enumerator.nextElement();
                String value = (String) properties.get(name);
                xml.append("<Property name=").append(QUOTE).append(
                        XMLUtils.escapeSpecialCharacters(name)).append(QUOTE)
                        .append(" value=").append(QUOTE).append(
                        XMLUtils.escapeSpecialCharacters(value))
                        .append(QUOTE).append(">").append("</Property>")
                        .append(NL);
            }
        }
        xml.append("</Session>");
        return xml.toString();
    }

    /**
     * <code>Session</code> id
     */
    public String getSessionID() {
        return sid;
    }

    public void setSessionID(final String sid) {
        this.sid = sid;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(final String secret) {
        this.secret = secret;
    }

    /**
     * <code>Session</code> type
     */
    public String getSessionType() {
        return stype;
    }

    public void setSessionType(final String stype) {
        this.stype = stype;
    }

    /**
     * <code>Cookie</code> id
     */
    public String getClientID() {
        return cid;
    }

    public void setClientID(final String cid) {
        this.cid = cid;
    }

    /**
     * <code> Cookie</code> domain
     */
    public String getClientDomain() {
        return cdomain;
    }

    public void setClientDomain(final String cdomain) {
        this.cdomain = cdomain;
    }

    /**
     * Max <code>Session</code> Time in minutes.
     */
    public long getMaxTime() {
        return maxtime;
    }

    public void setMaxTime(final long maxtime) {
        this.maxtime = maxtime;
    }

    /**
     * Max <code>Session</code> Idle time in minutes.
     */
    public long getMaxIdle() {
        return maxidle;
    }

    public void setMaxIdle(final long maxidle) {
        this.maxidle = maxidle;
    }

    /**
     * Max <code>Session</code> Cache time in minutes.
     */
    public long getMaxCaching() {
        return maxcaching;
    }

    public void setMaxCaching(final long maxcaching) {
        this.maxcaching = maxcaching;
    }

    /**
     * Time at which this session will expire in milliseconds from the UTC epoch.
     */
    public long getExpiryTime() {
        return expiryTime;
    }

    public void setExpiryTime(final long expiryTime) {
        this.expiryTime = expiryTime;
    }

    /**
     * Indicates that the session should never expire.
     *
     * @param neverExpiring {@code true} if the session should never expire. If {@code false} this method does nothing.
     */
    public void setNeverExpiring(final boolean neverExpiring) {
        if (neverExpiring) {
            this.expiryTime = Long.MAX_VALUE;
        }
    }

    /**
     * Whether the session will ever expire or not.
     */
    public boolean isNeverExpiring() {
        return this.expiryTime == Long.MAX_VALUE;
    }

    /**
     * Time at which there was last activity on this session as milliseconds from the UTC epoch.
     */
    public long getLastActivityTime() {
        return lastActivityTime;
    }

    public void setLastActivityTime(final long lastActivityTime) {
        this.lastActivityTime = lastActivityTime;
    }

    /**
     * <code>Session</code> idle time in seconds.
     */
    @JsonIgnore
    public long getTimeIdle() {
        if (isNeverExpiring()) {
            return 0;
        } else {
            return TimeUnit.MILLISECONDS.toSeconds(currentTimeMillis() - lastActivityTime);
        }
    }

    public void setTimeIdle(final long timeidle) {
        this.lastActivityTime = currentTimeMillis() - SECONDS.toMillis(timeidle);
    }

    /**
     * Time left for <code>Session</code> to become inactive, in seconds.
     */
    @JsonIgnore
    public long getTimeLeft() {
        if (isNeverExpiring()) {
            return Long.MAX_VALUE;
        } else {
            return TimeUnit.MILLISECONDS.toSeconds(expiryTime - currentTimeMillis());
        }
    }

    public void setTimeLeft(final long timeleft) {
        expiryTime = SECONDS.toMillis(timeleft) + currentTimeMillis();
        // Check for overflow - expiryTime should always be positive
        if (expiryTime < 0) {
            expiryTime = Long.MAX_VALUE;
        }
    }

    /**
     * <code>Session</code> state
     */
    public String getState() {
        return state;
    }

    public void setState(final String state) {
        this.state = state;
    }

    public Hashtable<String, String> getProperties() {
        return properties;
    }

    public void setProperties(final Hashtable<String, String> properties) {
        this.properties = properties;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final SessionInfo that = (SessionInfo) o;

        return maxcaching == that.maxcaching && maxidle == that.maxidle && maxtime == that.maxtime
                && lastActivityTime == that.lastActivityTime && expiryTime == that.expiryTime
                && !(cdomain != null ? !cdomain.equals(that.cdomain) : that.cdomain != null)
                && !(cid != null ? !cid.equals(that.cid) : that.cid != null)
                && !(properties != null ? !properties.equals(that.properties) : that.properties != null)
                && !(secret != null ? !secret.equals(that.secret) : that.secret != null)
                && !(sid != null ? !sid.equals(that.sid) : that.sid != null)
                && !(state != null ? !state.equals(that.state) : that.state != null)
                && !(stype != null ? !stype.equals(that.stype) : that.stype != null);

    }

    @Override
    public int hashCode() {
        int result = sid != null ? sid.hashCode() : 0;
        result = 31 * result + (secret != null ? secret.hashCode() : 0);
        result = 31 * result + (stype != null ? stype.hashCode() : 0);
        result = 31 * result + (cid != null ? cid.hashCode() : 0);
        result = 31 * result + (cdomain != null ? cdomain.hashCode() : 0);
        result = 31 * result + (int) (maxtime ^ (maxtime >>> 32));
        result = 31 * result + (int) (maxidle ^ (maxidle >>> 32));
        result = 31 * result + (int) (maxcaching ^ (maxcaching >>> 32));
        result = 31 * result + (int) (lastActivityTime ^ (lastActivityTime >>> 32));
        result = 31 * result + (int) (expiryTime ^ (expiryTime >>> 32));
        result = 31 * result + (state != null ? state.hashCode() : 0);
        result = 31 * result + (properties != null ? properties.hashCode() : 0);
        return result;
    }


    @Override
    public String toString() {
        return "SessionInfo{" +
                "sid='" + sid + '\'' +
                ", secret='" + secret + '\'' +
                ", stype='" + stype + '\'' +
                ", cid='" + cid + '\'' +
                ", cdomain='" + cdomain + '\'' +
                ", maxtime=" + maxtime +
                ", maxidle=" + maxidle +
                ", maxcaching=" + maxcaching +
                ", expiryTime=" + expiryTime +
                ", lastActivityTime=" + lastActivityTime +
                ", state='" + state + '\'' +
                ", properties=" + properties +
                '}';
    }
}
