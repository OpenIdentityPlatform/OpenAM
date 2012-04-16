/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: ConnectionFactoryProvider.java,v 1.2 2008/06/25 05:43:27 qcheng Exp $
 *
 */

package com.sun.identity.ha.jmqdb;

import javax.jms.TopicConnectionFactory;

/**
 * Allows the session service implementation to create a new instance of a
 * <code>TopicConnectionFactory</code>. This interface is used to provide
 * custom construction of the <code>TopicConnectionFactory</code> which allows
 * the implementation to use vendor specific APIs if necessary. An
 * implementation of this interface is located at runtime via the factory method
 * in <code>ConnectionFactoryProviderFactory</code>.
 */
public interface ConnectionFactoryProvider {

    /**
     * Creates a new <code>TopicConnectionFactory</code> instance.
     * 
     * @return a newly created <code>TopicConnectionFactory</code>.
     */
    public TopicConnectionFactory newTopicConnectionFactory();

    /**
     * Creates a new <code>TopicConnectionFactory</code> instance. The
     * supplied parameters are optionally used to configure the newly created
     * instance using vendor APIs if applicable.
     * 
     * @param brokerAddressList
     *            the list of addresses of the brokers
     * @param reconnectEnabled
     *            a flag indicating if reconnect will be enabled
     * @param flowLimitEnabled
     *            a flag indicating if flow limit will be enabled
     * @param defaultUsername
     *            the default username for establishing connections
     * @param defaultPassword
     *            the default password for establishing connections
     * @return a newly created <code>TopicConnectionFactory</code>.
     * @throws Exception
     *             if an exception occurs during creation or configuration of
     *             the <code>TopicConnectionFactory</code>.
     */
    public TopicConnectionFactory newTopicConnectionFactory(
            String brokerAddressList, boolean reconnectEnabled,
            boolean flowLimitEnabled, String defaultUsername,
            String defaultPassword) throws Exception;
}
