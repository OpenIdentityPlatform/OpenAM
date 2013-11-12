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
 * $Id: IPConditionTest.java,v 1.1 2009/08/19 05:41:00 veiming Exp $
 */
package com.sun.identity.entitlement;


import org.testng.annotations.Test;

/**
 *
 * @author dillidorai
 */
public class IPConditionTest {

    @Test
    public void testConstruction() throws Exception {
        String startIp = "100.100.100.100";
        String endIp = "200.200.200.200";

        IPCondition ipc = new IPCondition(startIp, endIp);
        ipc.setPConditionName("ip1");

        String readIp = ipc.getStartIp();
        if (!startIp.equals(readIp)) {
            throw new Exception("IPConditionTest.testConstruction():" +
                " read startIp did not equal startIp set in constructor");
        }

        readIp = ipc.getEndIp();
        if (!endIp.equals(readIp)) {
            throw new Exception("IPConditionTest.testConstruction():" +
                " read endIp did not equal endIp set in constructor");
        }

        startIp = "120.120.120.120";
        endIp = "220.220.220.220";
        ipc.setStartIp(startIp);
        ipc.setEndIp(endIp);

        readIp = ipc.getStartIp();
        if (!startIp.equals(readIp)) {
            throw new Exception("IPConditionTest.testConstruction():" +
                " read startIp did no tequal startIp set");
        }

        readIp = ipc.getEndIp();
        if (!endIp.equals(readIp)) {
            throw new Exception("IPConditionTest.testConstruction():" +
                " read endIp did not tequal endIp set");
        }

        String dnsName = "*.sun.com";
        DNSNameCondition dnsc = new DNSNameCondition(dnsName);
        dnsc.setPConditionName("ip2");

        String rdnsName = dnsc.getDomainNameMask();
        if (!dnsName.equals(rdnsName)) {
            throw new Exception("IPConditionTest.testConstruction():" +
                " read dnsName did not equal dnsName set in constructor");
        }

        dnsName = "*.iplanet.com";
        dnsc.setDomainNameMask(dnsName);
        rdnsName = dnsc.getDomainNameMask();
        if (!dnsName.equals(rdnsName)) {
            throw new Exception("IPConditionTest.testConstruction():" +
                " read dnsName did not equal dnsName set");
        }

        IPCondition ipc1 = new IPCondition();
        ipc1.setState(ipc.getState());
        DNSNameCondition dnsc1 = new DNSNameCondition();
        dnsc1.setState(dnsc.getState());

        if (!ipc1.equals(ipc)) {
            throw new Exception("IPConditionTest.testConstruction():" +
                " ipc1 not equal ipc");
        }
        if (!dnsc1.equals(dnsc)) {
            throw new Exception("IPConditionTest.testConstruction():" +
                " dnsc1 not equal dnsc");
        }

    }

}
