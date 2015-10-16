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
package org.forgerock.openam.radius.server;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.forgerock.guava.common.base.Strings;
import org.forgerock.openam.radius.common.Attribute;
import org.forgerock.openam.radius.common.AttributeSet;
import org.forgerock.openam.radius.common.Packet;
import org.forgerock.openam.radius.common.UserNameAttribute;
import org.forgerock.openam.radius.server.config.RadiusServerConstants;
import org.joda.time.DateTime;

import com.sun.identity.shared.debug.Debug;

/**
 * Encapsulates a radius request. Contains the received packet, along with other extracted information such as the user
 * making the request, the decrypted credentials and the packet attributes.
 */
public class RadiusRequest {

    /**
     * Debug logging object.
     */
    private static final Debug LOG = Debug.getInstance(RadiusServerConstants.RADIUS_SERVER_LOGGER);

    /**
     * Map of attributes from the request packet.
     */
    private Map<Class, Attribute> attributeMap = new HashMap<Class, Attribute>();

    /**
     * The username of the authenticating party.
     */
    private volatile String userName;

    /**
     * A unique ID associated with the request. Formed from a UUID with -rid-<id> appended, where <id> is the requestId
     * from the radius packet.
     */
    private volatile String requestId = null;

    /**
     * The time at which the request started.
     */
    private volatile DateTime requestStartTime = DateTime.now();

    /**
     * The radius packet received by the radius server (the request)
     */
    private final Packet requestPacket;

    /**
     * This is the key used to store the ContextHolder entry in the ContextHolderCache. The cache is used to hold the
     * OpenAM AuthContext for the request process between calls. i.e. if the initial RADIUS Access-Request call results
     * in an Access-Challenge, this is the key that will be in the Access-Challenge request and provided in the
     * StateAttribute an subsequently used to obtain the cached AuthContext associated with the initial request. It is
     * used in Audit Logging to tie up the two requests.
     */
    private String contextHolderKey;


    public RadiusRequest(Packet requestPacket) {
        this.requestPacket = requestPacket;

        loadAttsMap();
    }

    /**
     * @return the requestPacket
     */
    public Packet getRequestPacket() {
        return requestPacket;
    }

    /**
     * Get an attribute that was sent in the requestPacket, or return null if no attribute of the requested type was in
     * the packet.
     * 
     * @param attributeType
     * @return the Attribute of the requested type, or NULL if non exists.
     */
    public Attribute getAttribute(Class attributeType) {
        return attributeMap.get(attributeType);
    }

    /**
     * Loads the attributes into a map. warning: this is lossy for atts that support duplicates like proxyState. but we
     * aren't using those for authentication but only need State, UserName, and UserPassword. So we are good.
     */
    private void loadAttsMap() {
        final AttributeSet atts = requestPacket.getAttributeSet();

        for (int i = 0; i < atts.size(); i++) {
            final Attribute att = atts.getAttributeAt(i);
            // warning: this is lossy for atts that support duplicates like proxyState. but we aren't using those
            // for authentication but only need State, UserName, and UserPassword. So we are good.
            attributeMap.put(att.getClass(), att);
        }
    }

    public String getUsername() {
        if (Strings.isNullOrEmpty(userName)) {
            UserNameAttribute userNameAttribute = (UserNameAttribute) attributeMap.get(UserNameAttribute.class);
            if (userNameAttribute != null) {
                this.userName = userNameAttribute.getName();
            }
        }
        return userName;
    }

    /**
     * @return The ID of the request.
     */
    public synchronized String getRequestId() {
        if (this.requestId == null) {
            StringBuilder sb = new StringBuilder(UUID.randomUUID().toString());
            if (this.requestPacket != null) {
                short reqId = requestPacket.getIdentifier();
                sb.append("-rid-").append(Short.toString(reqId));
            }
            requestId = sb.toString();
        }
        return requestId;
    }

    /**
     * Get the number of milliseconds since the Unix epoch at which the request was made.
     * 
     * @return the number of milliseconds since the Unix epoch at which the request was made.
     */
    public long getStartTimestampInMillis() {
        return this.requestStartTime.getMillis();
    }

    /**
     * Set the key used to store the ContextHolder entry in the ContextHolderCache. The cache is used to hold the OpenAM
     * AuthContext for the request process between calls. i.e. if the initial RADIUS Access-Request call results in an
     * Access-Challenge, this is the key that will be in the Access-Challenge request and provided in the StateAttribute
     * an subsequently used to obtain the cached AuthContext associated with the initial request. It is used in Audit
     * Logging to tie up the two requests.
     * 
     * @param cacheKey
     *            the key used to store the ContextHolder in the ContextHolder cache.
     */
    public void setContextHolderKey(String cacheKey) {
        this.contextHolderKey = cacheKey;
    }

    /**
     * Get the key used to store the ContextHolder entry in the ContextHolderCache. The cache is used to hold the OpenAM
     * AuthContext for the request process between calls. i.e. if the initial RADIUS Access-Request call results in an
     * Access-Challenge, this is the key that will be in the Access-Challenge request and provided in the StateAttribute
     * an subsequently used to obtain the cached AuthContext associated with the initial request. It is used in Audit
     * Logging to tie up the two requests.
     * 
     * @return
     */
    public String getContextHolderKey() {
        return contextHolderKey;
    }
}
