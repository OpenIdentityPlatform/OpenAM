/**
 * Copyright (c) 2000-2012 Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package org.ntlmv2.liferay;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



/**
 * @author Marcellus Tavares
 */
public class NtlmServiceAccount {

	private static Logger log = LoggerFactory.getLogger(NtlmServiceAccount.class);
	
	public NtlmServiceAccount(String account, String password) {
		setAccount(account);
		setPassword(password);
	}

	public String getAccount() {
		return _account;
	}

	public String getAccountName() {
		return _accountName;
	}

	public String getComputerName() {
		return _computerName;
	}

	public String getPassword() {
		return _password;
	}

	public void setAccount(String account) {
		_account = account;

		_accountName = _account.substring(0, _account.indexOf("@"));
		_computerName = _account.substring(
			0, _account.indexOf("$"));
		log.info("--> account: " + _account);
		log.info("--> accountName: " + _accountName);
		log.info("--> computerName: " + _computerName);
	}

	public void setPassword(String password) {
		_password = password;
	}

	private String _account;
	private String _accountName;
	private String _computerName;
	private String _password;

}