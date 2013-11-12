/*
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
 */
/*
 * Portions Copyrighted 2013 Syntegrity.
 * Portions Copyrighted 2013 ForgeRock Inc.
 */

package org.forgerock.openam.authentication.modules.deviceprint.model;

import java.util.Date;

/**
 * Representation of the user profile stored in the user record.
 * 
 * @author ljaromin
 */
public class UserProfile {

	/** Unique id of profile. */
	private String uuid;

	/** Profile creation timestamp. */
	private Date createDate;

	/** Timestamp of last positive selection of this profile. */
	private Date lastSelectedDate;

	/** Quantity of this profile selections. */
	private Long selectionCounter;

	/** Device print attributes. */
	private DevicePrint devicePrint = new DevicePrint();

    /**
     * For Jackson Mapper to construct an UserProfile instance.
     */
    public UserProfile() {
    }

	/**
	 * Create profile using common attributes.
	 * 
	 * @param createDate The creation date.
	 * @param lastSelectedDate The last selected date.
	 * @param selectionCounter The number of times this profile has been selected.
	 */
	public UserProfile(Date createDate, Date lastSelectedDate,
			Long selectionCounter) {
		super();
		this.createDate = createDate;
		this.lastSelectedDate = lastSelectedDate;
		this.selectionCounter = selectionCounter;
	}

    /**
     * Gets the User Profile's last selected date.
     *
     * @return The last selected date.
     */
	public Date getLastSelectedDate() {
		return lastSelectedDate;
	}

    /**
     * Sets the User Profile's last selected date.
     *
     * @param lastSelectedDate The last selected date.
     */
	public void setLastSelectedDate(Date lastSelectedDate) {
		this.lastSelectedDate = lastSelectedDate;
	}

    /**
     * Gets the User Profile's selection counter.
     *
     * @return The selection counter.
     */
	public Long getSelectionCounter() {
		return selectionCounter;
	}

    /**
     * Sets the User Profile's selection counter.
     *
     * @param selectionCounter The selection counter.
     */
	public void setSelectionCounter(Long selectionCounter) {
		this.selectionCounter = selectionCounter;
	}

    /**
     * Gets the User Profile's Device Print information.
     *
     * @return The Device Print information.
     */
	public DevicePrint getDevicePrint() {
		return devicePrint;
	}

    /**
     * Sets the User Profile's Device Print information.
     *
     * @param devicePrint The Device Print information.
     */
	public void setDevicePrint(DevicePrint devicePrint) {
		this.devicePrint = devicePrint;
	}

    /**
     * Gets the User Profile's unique id.
     *
     * @return The unique id.
     */
	public String getUuid() {
		return uuid;
	}

    /**
     * Sets the User Profile's unique id.
     *
     * @param uuid The unique id.
     */
	public void setUuid(String uuid) {
		this.uuid = uuid;
	}
}
