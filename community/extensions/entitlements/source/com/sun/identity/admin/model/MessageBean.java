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
 * $Id: MessageBean.java,v 1.3 2009/06/04 11:49:15 veiming Exp $
 */

package com.sun.identity.admin.model;

import java.io.Serializable;
import javax.faces.application.FacesMessage;

public class MessageBean implements Serializable {
    private String summary;
    private String detail;
    private FacesMessage.Severity severity;

    public MessageBean() {
        // nothing
    }
    public MessageBean(FacesMessage fm) {
        this.summary = fm.getSummary();
        if (fm.getDetail() != null && !fm.getDetail().equals(fm.getSummary())) {
            this.detail = fm.getDetail();
        }
        this.severity = fm.getSeverity();
    }

    public String getSummary() {
        return summary;
    }

    public String getDetail() {
        return detail;
    }

    public boolean isError() {
        return getSeverity().equals(FacesMessage.SEVERITY_ERROR);
    }

    public boolean isWarning() {
        return getSeverity().equals(FacesMessage.SEVERITY_WARN);
    }

    public boolean isInfo() {
        return getSeverity().equals(FacesMessage.SEVERITY_INFO);
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public FacesMessage.Severity getSeverity() {
        return severity;
    }

    public void setSeverity(FacesMessage.Severity severity) {
        this.severity = severity;
    }

    public FacesMessage toFacesMessage() {
        FacesMessage fm = new FacesMessage();
        fm.setDetail(detail);
        fm.setSummary(summary);
        fm.setSeverity(severity);

        return fm;
    }
}
