/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: IPAddressCondition.java,v 1.2 2008/06/25 05:42:37 qcheng Exp $
 *
 */
package com.sun.identity.config.pojos.condition;

/**
 * @author Victor Alfaro
 */
public class IPAddressCondition extends Condition {
    private String ipAddressFrom;
    private String ipAddressTo;
    private String dnsName;
    private Boolean dynamic;

    public String getIpAddressFrom() {
        return ipAddressFrom;
    }

    public void setIpAddressFrom(String ipAddressFrom) {
        this.ipAddressFrom = ipAddressFrom;
    }

    public String getIpAddressTo() {
        return ipAddressTo;
    }

    public void setIpAddressTo(String ipAddressTo) {
        this.ipAddressTo = ipAddressTo;
    }

    public String getDnsName() {
        return dnsName;
    }

    public void setDnsName(String dnsName) {
        this.dnsName = dnsName;
    }

    public Boolean getDynamic() {
        return dynamic;
    }

    public void setDynamic(Boolean dynamic) {
        this.dynamic = dynamic;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || !(o instanceof IPAddressCondition)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        IPAddressCondition that = (IPAddressCondition) o;
        if (dnsName != null ? !dnsName.equals(that.dnsName) : that.dnsName != null) {
            return false;
        }
        if (dynamic != null ? !dynamic.equals(that.dynamic) : that.dynamic != null) {
            return false;
        }
        if (ipAddressFrom != null ? !ipAddressFrom.equals(that.ipAddressFrom) : that.ipAddressFrom != null) {
            return false;
        }
        if (ipAddressTo != null ? !ipAddressTo.equals(that.ipAddressTo) : that.ipAddressTo != null) {
            return false;
        }
        return true;
    }
}
