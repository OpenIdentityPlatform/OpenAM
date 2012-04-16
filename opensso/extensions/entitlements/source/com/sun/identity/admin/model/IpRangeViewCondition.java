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
 * $Id: IpRangeViewCondition.java,v 1.4 2009/08/13 16:55:04 farble1670 Exp $
 */
package com.sun.identity.admin.model;

import com.sun.identity.entitlement.EntitlementCondition;
import com.sun.identity.entitlement.IPCondition;
import java.io.Serializable;

public class IpRangeViewCondition
        extends ViewCondition
        implements Serializable {

    public IpAddress getStartIp() {
        return startIp;
    }

    public void setStartIp(IpAddress startIp) {
        this.startIp = startIp;
    }

    public IpAddress getEndIp() {
        return endIp;
    }

    public void setEndIp(IpAddress endIp) {
        this.endIp = endIp;
    }

    public static class IpAddress {

        private Octet[] octets;

        public static class Octet {

            private int value;

            public Octet(int value) {
                this.value = value;
            }

            public int getValue() {
                return value;
            }

            public void setValue(int value) {
                this.value = value;
            }

            @Override
            public String toString() {
                return Integer.toString(value);
            }
        }

        public IpAddress() {
            Octet[] os = {new Octet(0), new Octet(0), new Octet(0), new Octet(0)};
            octets = os;
        }

        public IpAddress(int o1, int o2, int o3, int o4) {
            Octet[] os = {new Octet(o1), new Octet(o2), new Octet(o3), new Octet(o4)};
            octets = os;
        }

        public IpAddress(String ipString) {
            String[] ips = ipString.split("\\.");
            assert (ips.length == 4);

            Octet[] os = {new Octet(0), new Octet(0), new Octet(0), new Octet(0)};
            octets = os;
            for (int i = 0; i < 4; i++) {
                octets[i].setValue(Integer.valueOf(ips[i]));
            }
        }

        public Octet[] getOctets() {
            return octets;
        }

        public void setOctets(Octet[] octets) {
            this.octets = octets;
        }

        @Override
        public String toString() {
            String s = octets[0] + "." + octets[1] + "." + octets[2] + "." + octets[3];
            return s;
        }
    }
    private IpAddress startIp = new IpAddress(0, 0, 0, 0);
    private IpAddress endIp = new IpAddress(255, 255, 255, 255);

    public EntitlementCondition getEntitlementCondition() {
        IPCondition ipc = new IPCondition();
        ipc.setDisplayType(getConditionType().getName());

        ipc.setStartIp(getStartIp().toString());
        ipc.setEndIp(getEndIp().toString());

        return ipc;
    }

    @Override
    public String toString() {
        return getTitle() + ":{" + getStartIp().toString() + ">" + getEndIp().toString() + "}";
    }
}
