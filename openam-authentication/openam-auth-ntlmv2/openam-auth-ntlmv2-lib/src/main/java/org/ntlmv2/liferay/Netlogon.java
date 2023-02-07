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

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import jcifs.dcerpc.DcerpcBinding;
import jcifs.dcerpc.DcerpcHandle;
import jcifs.dcerpc.UnicodeString;
import jcifs.smb.SmbException;

import org.ntlmv2.liferay.ntlm.msrpc.NetlogonAuthenticator;
import org.ntlmv2.liferay.ntlm.msrpc.NetlogonIdentityInfo;
import org.ntlmv2.liferay.ntlm.msrpc.NetlogonNetworkInfo;
import org.ntlmv2.liferay.ntlm.msrpc.NetlogonValidationSamInfo;
import org.ntlmv2.liferay.ntlm.msrpc.NetrLogonSamLogon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author Marcellus Tavares
 * @author Michael C. Han
 */
public class Netlogon {

	public NtlmUserAccount logon(
			String domain, String userName, String workstation,
			byte[] serverChallenge, byte[] ntResponse, byte[] lmResponse)
		throws NtlmLogonException {

		NetlogonConnection netlogonConnection = new NetlogonConnection();

		try {

			netlogonConnection.connect(
				_domainController, _domainControllerName, _ntlmServiceAccount,
				_secureRandom);

			NetlogonAuthenticator netlogonAuthenticator =
				netlogonConnection.computeNetlogonAuthenticator();

			NetlogonIdentityInfo netlogonIdentityInfo =
				new NetlogonIdentityInfo(
					domain, 0x00000820, 0, 0, userName, workstation);

			NetlogonNetworkInfo netlogonNetworkInfo = new NetlogonNetworkInfo(
				netlogonIdentityInfo, serverChallenge, ntResponse, lmResponse);

			NetrLogonSamLogon netrLogonSamLogon = new NetrLogonSamLogon(
				_domainControllerName, _ntlmServiceAccount.getComputerName(),
				netlogonAuthenticator, new NetlogonAuthenticator(), 2,
				netlogonNetworkInfo, 2, new NetlogonValidationSamInfo(), 0);

			DcerpcHandle dcerpcHandle = netlogonConnection.getDcerpcHandle();

			dcerpcHandle.sendrecv(netrLogonSamLogon);

			if (netrLogonSamLogon.getStatus() == 0) {
				NetlogonValidationSamInfo netlogonValidationSamInfo =
					netrLogonSamLogon.getNetlogonValidationSamInfo();

				UnicodeString name = new UnicodeString(
					netlogonValidationSamInfo.getEffectiveName(), false);

				return new NtlmUserAccount(name.toString());
			}
			else {
				SmbException smbe = new SmbException(
					netrLogonSamLogon.getStatus(), false);

				throw new NtlmLogonException(
					"Unable to authenticate user: " + smbe.getMessage());
			}
		}
		catch (NoSuchAlgorithmException e) {
			throw new NtlmLogonException(
				"Unable to authenticate due to invalid encryption algorithm",
				e);
		}
		catch (IOException e) {
			throw new NtlmLogonException(
				"Unable to authenticate due to communication failure with " +
					"server",
				e);
		}
		finally {
			try {
				netlogonConnection.disconnect();
			}
			catch (Exception e) {
				_log.error("Unable to disconnect Netlogon connection", e);
			}
		}
	}

	public void setConfiguration(
		String domainController, String domainControllerName,
		NtlmServiceAccount ntlmServiceAccount) {

		_domainController = domainController;
		_domainControllerName = domainControllerName;
		_ntlmServiceAccount = ntlmServiceAccount;
	}

	private static Logger _log = LoggerFactory.getLogger(Netlogon.class);

	private String _domainController;
	private String _domainControllerName;
	private NtlmServiceAccount _ntlmServiceAccount;
	private SecureRandom _secureRandom = new SecureRandom();

	static {
		DcerpcBinding.addInterface(
			"netlogon", "12345678-1234-abcd-ef00-01234567cffb:1.0");
	}

}