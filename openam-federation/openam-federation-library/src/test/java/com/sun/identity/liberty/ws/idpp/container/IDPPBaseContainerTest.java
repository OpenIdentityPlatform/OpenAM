/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2026 3A Systems, LLC.
 */
package com.sun.identity.liberty.ws.idpp.container;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.identity.liberty.ws.idpp.jaxb.DSTMonthDay;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.testng.annotations.Test;

@Test
public class IDPPBaseContainerTest {

    private final IDPPBaseContainer container = new IDPPBaseContainer() {
        @Override
        public Object getContainerObject(Map userMap) {
            return null;
        }

        @Override
        public Set getContainerAttributes() {
            return null;
        }

        @Override
        public Set getContainerAttributesForSelect(String select) {
            return null;
        }

        @Override
        public Map getDataMapForSelect(String select, List data) {
            return null;
        }

        @Override
        public boolean hasBinaryAttributes() {
            return false;
        }
    };

    public void monthDayKeepsLexicalValue() {
        DSTMonthDay monthDay = container.getDSTMonthDay("--05-17");

        assertThat(monthDay.getValue().toXMLFormat()).isEqualTo("--05-17");
    }

    /**
     * Directory values written before the JAXB 3 migration may lack the xs:gMonthDay "--" prefix.
     */
    public void monthDayWithoutPrefixIsNormalised() {
        DSTMonthDay monthDay = container.getDSTMonthDay("05-17");

        assertThat(monthDay.getValue().toXMLFormat()).isEqualTo("--05-17");
    }

    public void invalidMonthDayReturnsNull() {
        assertThat(container.getDSTMonthDay("May 17")).isNull();
    }
}
