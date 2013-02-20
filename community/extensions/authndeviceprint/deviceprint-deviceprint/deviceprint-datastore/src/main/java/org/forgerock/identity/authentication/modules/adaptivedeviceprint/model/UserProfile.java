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

package org.forgerock.identity.authentication.modules.adaptivedeviceprint.model;

import java.util.Date;

/**
 * Representation of the user profile stored in the user record
 * 
 * @author ljaromin
 * 
 */
public class UserProfile {

	/**
	 * Unique id of profile.
	 */
	private String uuid;

	/**
	 * Human readable name of profile
	 */
	private String name;

	/**
	 * Profile creation timestamp
	 */
	private Date createDate;

	/**
	 * Timestamp of last positive selection of this profile.
	 */
	private Date lastSelectedDate;

	/**
	 * Timestamp of last persistent cookie update;
	 */
	private Date lastPersistentCookieUpdateDate;

	/**
	 * Quantity of this profile selections
	 */
	private Long selectionCounter;

	/**
	 * Device print attributes
	 */
	private DevicePrint devicePrint = new DevicePrint();

	/**
	 * Create profile using common attributes
	 * 
	 * @param createDate
	 * @param lastSelectedDate
	 * @param selectionCounter
	 */
	public UserProfile(Date createDate, Date lastSelectedDate,
			Long selectionCounter) {
		super();
		this.createDate = createDate;
		this.lastSelectedDate = lastSelectedDate;
		this.selectionCounter = selectionCounter;
	}

	/**
	 * Generic constructor
	 */
	public UserProfile() {
		super();
	}

	public Date getCreateDate() {
		return createDate;
	}

	public void setCreateDate(Date createDate) {
		this.createDate = createDate;
	}

	public Date getLastSelectedDate() {
		return lastSelectedDate;
	}

	public void setLastSelectedDate(Date lastSelectedDate) {
		this.lastSelectedDate = lastSelectedDate;
	}

	public Long getSelectionCounter() {
		return selectionCounter;
	}

	public void setSelectionCounter(Long selectionCounter) {
		this.selectionCounter = selectionCounter;
	}

	public DevicePrint getDevicePrint() {
		return devicePrint;
	}

	public void setDevicePrint(DevicePrint devicePrint) {
		this.devicePrint = devicePrint;
	}

	public String getUuid() {
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Date getLastPersistentCookieUpdateDate() {
		return lastPersistentCookieUpdateDate;
	}

	public void setLastPersistentCookieUpdateDate(
			Date lastPersistentCookieUpdateDate) {
		this.lastPersistentCookieUpdateDate = lastPersistentCookieUpdateDate;
	}

}
