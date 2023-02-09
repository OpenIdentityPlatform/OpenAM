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
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import jcifs.ntlmssp.NtlmFlags;
import jcifs.ntlmssp.Type1Message;
import jcifs.ntlmssp.Type2Message;
import jcifs.ntlmssp.Type3Message;
import jcifs.util.Encdec;

import org.ntlmv2.liferay.util.NtlmV2ArrayUtil;

/**
 * @author Marcellus Tavares
 * @author Michael C. Han
 */
public class NtlmManager {

	public NtlmManager(
		String domain, String domainController, String domainControllerHostName,
		String serviceAccount, String servicePassword) {

		setConfiguration(
			domain, domainController, domainControllerHostName, serviceAccount,
			servicePassword);
	}

	public NtlmUserAccount authenticate(byte[] material, byte[] serverChallenge)
		throws IOException, NoSuchAlgorithmException, NtlmLogonException {

		Type3Message type3Message = new Type3Message(material);

		if (type3Message.getFlag(
				_NTLMSSP_NEGOTIATE_EXTENDED_SESSION_SECURITY) &&
			(type3Message.getNTResponse().length == 24)) {

			MessageDigest messageDigest = MessageDigest.getInstance("MD5");

			byte[] bytes = new byte[16];

			System.arraycopy(serverChallenge, 0, bytes, 0, 8);
			System.arraycopy(type3Message.getLMResponse(), 0, bytes, 8, 8);

			messageDigest.update(bytes);

			serverChallenge = messageDigest.digest();
		}

		return _netlogon.logon(
			 type3Message.getDomain(), type3Message.getUser(),
			 type3Message.getWorkstation(), serverChallenge,
			 type3Message.getNTResponse(), type3Message.getLMResponse());
	}

	public String getDomain() {
		return _domain;
	}

	public String getDomainController() {
		return _domainController;
	}

	public String getDomainControllerName() {
		return _domainControllerHostName;
	}

	public String getServiceAccount() {
		return _ntlmServiceAccount.getAccount();
	}

	public String getServicePassword() {
		return _ntlmServiceAccount.getPassword();
	}

	public byte[] negotiate(byte[] material, byte[] serverChallenge)
		throws IOException {
		return negotiateType2Message(material, serverChallenge).toByteArray();
	}
	
	public Type2Message negotiateType2Message(byte[] material, byte[] serverChallenge)
		throws IOException {

		Type1Message type1Message = new Type1Message(material);

		Type2Message type2Message = new Type2Message(
			type1Message.getFlags(), serverChallenge, _domain);

		if (type2Message.getFlag(
				_NTLMSSP_NEGOTIATE_EXTENDED_SESSION_SECURITY)) {

			type2Message.setFlag(NtlmFlags.NTLMSSP_NEGOTIATE_LM_KEY, false);
			type2Message.setFlag(NtlmFlags.NTLMSSP_NEGOTIATE_TARGET_INFO, true);
			type2Message.setTargetInformation(getTargetInformation());
		}

		return type2Message;
	}

	public void setConfiguration(
		String domain, String domainController, String domainControllerName,
		String serviceAccount, String servicePassword) {

		_domain = domain;
		_domainController = domainController;
		_domainControllerHostName = domainControllerName;
		_ntlmServiceAccount = new NtlmServiceAccount(
			serviceAccount, servicePassword);

		_netlogon = new Netlogon();

		_netlogon.setConfiguration(
			domainController, domainControllerName, _ntlmServiceAccount);
	}

	protected byte[] getAVPairBytes(int avId, String value)
		throws UnsupportedEncodingException{

		byte[] valueBytes = value.getBytes("UTF-16LE");
		byte[] avPairBytes = new byte[4 + valueBytes.length];

		Encdec.enc_uint16le((short)avId, avPairBytes, 0);
		Encdec.enc_uint16le((short)valueBytes.length, avPairBytes, 2);

		System.arraycopy(valueBytes, 0, avPairBytes, 4, valueBytes.length);

		return avPairBytes;
	}

	protected byte[] getTargetInformation() throws UnsupportedEncodingException{
		byte[] computerName = getAVPairBytes(
			1, _ntlmServiceAccount.getComputerName());
		byte[] domainName = getAVPairBytes(2, _domain);

		byte[] targetInformation = NtlmV2ArrayUtil.append(computerName, domainName);

		byte[] eol = getAVPairBytes(0, " ");

		targetInformation = NtlmV2ArrayUtil.append(targetInformation, eol);

		return targetInformation;
	}

	private static final int _NTLMSSP_NEGOTIATE_EXTENDED_SESSION_SECURITY =
		0x00080000;

	private String _domain;
	private String _domainController;
	private String _domainControllerHostName;
	private Netlogon _netlogon;
	private NtlmServiceAccount _ntlmServiceAccount;

}