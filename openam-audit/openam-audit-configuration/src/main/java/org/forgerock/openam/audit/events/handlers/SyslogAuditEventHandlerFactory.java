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
* Copyright 2015 ForgeRock AS.
*/
package org.forgerock.openam.audit.events.handlers;

import static com.sun.identity.shared.datastruct.CollectionHelper.*;

import com.iplanet.am.util.SystemProperties;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.audit.AuditException;
import org.forgerock.audit.events.handlers.AuditEventHandler;
import org.forgerock.audit.handlers.syslog.Facility;
import org.forgerock.audit.handlers.syslog.SyslogAuditEventHandler;
import org.forgerock.audit.handlers.syslog.SyslogAuditEventHandlerConfiguration;
import org.forgerock.audit.handlers.syslog.SyslogAuditEventHandlerConfiguration.EventBufferingConfiguration;
import org.forgerock.audit.handlers.syslog.TransportProtocol;
import org.forgerock.audit.providers.LocalHostNameProvider;
import org.forgerock.audit.providers.ProductInfoProvider;
import org.forgerock.openam.audit.AuditEventHandlerFactory;
import org.forgerock.openam.audit.configuration.AuditEventHandlerConfiguration;
import org.forgerock.openam.utils.StringUtils;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.Set;

/**
 * This factory is responsible for creating an instance of the {@link SyslogAuditEventHandler}.
 *
 * @since 13.0.0
 */
public class SyslogAuditEventHandlerFactory implements AuditEventHandlerFactory {

    private static final Debug DEBUG = Debug.getInstance("amAudit");

    @Override
    public AuditEventHandler create(AuditEventHandlerConfiguration configuration) throws AuditException {
        Map<String, Set<String>> attributes = configuration.getAttributes();

        SyslogAuditEventHandlerConfiguration syslogHandlerConfiguration = new SyslogAuditEventHandlerConfiguration();
        syslogHandlerConfiguration.setEnabled(getBooleanMapAttr(attributes, "enabled", true));
        syslogHandlerConfiguration.setName(configuration.getHandlerName());
        syslogHandlerConfiguration.setTopics(attributes.get("topics"));
        syslogHandlerConfiguration.setHost(getMapAttr(attributes, "host"));
        setPort(syslogHandlerConfiguration, attributes);

        String transportProtocol = getMapAttr(attributes, "transportProtocol");
        try {
            syslogHandlerConfiguration.setProtocol(TransportProtocol.valueOf(transportProtocol));
        } catch (IllegalArgumentException iae) {
            throw new AuditException("Attribute 'transportProtocol' is invalid: " + transportProtocol);
        }
        setConnectTimeout(syslogHandlerConfiguration, attributes);
        EventBufferingConfiguration eventBufferingConfiguration = new EventBufferingConfiguration();
        eventBufferingConfiguration.setEnabled(getBooleanMapAttr(attributes, "bufferingEnabled", true));
        syslogHandlerConfiguration.setBufferingConfiguration(eventBufferingConfiguration);

        String facility = getMapAttr(attributes, "facility");
        try {
            syslogHandlerConfiguration.setFacility(Facility.valueOf(facility));
        } catch (IllegalArgumentException iae) {
            throw new AuditException("Attribute 'facility' is invalid: " + facility);
        }
        //Severity Field Mappings intentionally not set.

        return new SyslogAuditEventHandler(syslogHandlerConfiguration, configuration.getEventTopicsMetaData(),
                new ProductInfoProviderImpl(), new SyslogLocalHostNameProvider());
    }

    private void setPort(SyslogAuditEventHandlerConfiguration syslogHandlerConfiguration,
                         Map<String, Set<String>> attributes) {
        Integer port = parseIntegerFromAttribute("port", attributes);
        if (port != null) {
            syslogHandlerConfiguration.setPort(port);
        }
    }

    private void setConnectTimeout(SyslogAuditEventHandlerConfiguration syslogHandlerConfiguration,
                                   Map<String, Set<String>> attributes) {
        Integer connectTimeout = parseIntegerFromAttribute("connectTimeout", attributes);
        if (connectTimeout != null) {
            syslogHandlerConfiguration.setConnectTimeout(connectTimeout);
        }
    }

    private Integer parseIntegerFromAttribute(String attributeName, Map<String, Set<String>> attributes) {
        String attributeValue = getMapAttr(attributes, attributeName);

        if (StringUtils.isNotEmpty(attributeValue)) {
            try {
                return Integer.parseInt(attributeValue);
            } catch (NumberFormatException nfe) {
                DEBUG.warning("Unable to parse Integer from attribute {}", attributeValue);
            }
        }

        return null;
    }

    /**
     * Implementation of the strategy for obtaining the server's local hostname.
     */
    private final static class SyslogLocalHostNameProvider implements LocalHostNameProvider {

        @Override
        public String getLocalHostName() {
            try {
                return InetAddress.getLocalHost().getHostName();
            } catch (UnknownHostException uhe) {
                DEBUG.error("Cannot resolve name of localhost server", uhe);
                return SystemProperties.get(Constants.AM_SERVER_HOST);
            }
        }
    }

    /**
     * Implementation of the strategy for obtaining the information relating to the product in which the AuditService is
     * deployed.
     */
    private final static class ProductInfoProviderImpl implements ProductInfoProvider {

        @Override
        public String getProductName() {
            return "OpenAM";
        }
    }
}
