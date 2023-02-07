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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;

import jcifs.dcerpc.DcerpcHandle;
import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.util.DES;
import jcifs.util.Encdec;
import jcifs.util.HMACT64;
import jcifs.util.MD4;

import org.ntlmv2.liferay.ntlm.msrpc.NetlogonAuthenticator;
import org.ntlmv2.liferay.ntlm.msrpc.NetrServerAuthenticate3;
import org.ntlmv2.liferay.ntlm.msrpc.NetrServerReqChallenge;


/*
     ntlm.auth.enabled=false
    ntlm.auth.domain.controller=127.0.0.1
    ntlm.auth.domain.controller.name=EXAMPLE
    ntlm.auth.domain=EXAMPLE
    ntlm.auth.negotiate.flags=0x600FFFFF
    ntlm.auth.service.account=LIFERAY$@EXAMPLE.COM
    ntlm.auth.service.password=test

 */

/**
 * @author Michael C. Han
 */
public class NetlogonConnection {

	public NetlogonConnection() {
		if (_negotiateFlags == 0) {
			String negotiateFlags = "0x600FFFFF";  // <<<=== TODO / msc: make configurable

			if (negotiateFlags.startsWith("0x")) {
				_negotiateFlags = Integer.valueOf(
					negotiateFlags.substring(2), 16);
			}
			else {
				_negotiateFlags = 0x600FFFFF;
			}
		}
	}

	public NetlogonConnection( int negotiateFlags ) {
		_negotiateFlags = negotiateFlags;
	}

	public NetlogonAuthenticator computeNetlogonAuthenticator() {
		int timestamp = (int)System.currentTimeMillis();

		int input = Encdec.dec_uint32le(_clientCredential, 0) + timestamp;

		Encdec.enc_uint32le(input, _clientCredential, 0);

		byte[] credential = computeNetlogonCredential(
			_clientCredential, _sessionKey);

		return new NetlogonAuthenticator(credential, timestamp);
	}

	public void connect(
			String domainController, String domainControllerName,
			NtlmServiceAccount ntlmServiceAccount, SecureRandom secureRandom)
		throws IOException, NtlmLogonException, NoSuchAlgorithmException {

		NtlmPasswordAuthentication ntlmPasswordAuthentication =
			new NtlmPasswordAuthentication(
				null, ntlmServiceAccount.getAccount(),
				ntlmServiceAccount.getPassword());

		String endpoint = "ncacn_np:" + domainController + "[\\PIPE\\NETLOGON]";

		DcerpcHandle dcerpcHandle = DcerpcHandle.getHandle(
			endpoint, ntlmPasswordAuthentication);

		setDcerpcHandle(dcerpcHandle);

		dcerpcHandle.bind();

		byte[] clientChallenge = new byte[8];

		secureRandom.nextBytes(clientChallenge);

		NetrServerReqChallenge netrServerReqChallenge =
			new NetrServerReqChallenge(
				domainControllerName, ntlmServiceAccount.getComputerName(),
				clientChallenge, new byte[8]);

		dcerpcHandle.sendrecv(netrServerReqChallenge);

		MD4 md4 = new MD4();

		md4.update(ntlmServiceAccount.getPassword().getBytes("UTF-16LE"));

		byte[] sessionKey = computeSessionKey(
			md4.digest(), clientChallenge,
			netrServerReqChallenge.getServerChallenge());

		byte[] clientCredential = computeNetlogonCredential(
			clientChallenge, sessionKey);

		NetrServerAuthenticate3 netrServerAuthenticate3 =
			new NetrServerAuthenticate3(
				domainControllerName, ntlmServiceAccount.getAccountName(), 2,
				ntlmServiceAccount.getComputerName(), clientCredential,
				new byte[8], _negotiateFlags);

		dcerpcHandle.sendrecv(netrServerAuthenticate3);

		byte[] serverCredential = computeNetlogonCredential(
			netrServerReqChallenge.getServerChallenge(), sessionKey);

		if (!Arrays.equals(
				serverCredential,
				netrServerAuthenticate3.getServerCredential())) {

			throw new NtlmLogonException("Session key negotiation failed");
		}

		_clientCredential = clientCredential;
		_sessionKey = sessionKey;
		_negotiateFlags = netrServerAuthenticate3.getNegotiatedFlags();
	}

	public void disconnect() throws IOException {
		if (_dcerpcHandle != null) {
			_dcerpcHandle.close();
		}
	}

	public byte[] getClientCredential() {
		return _clientCredential;
	}

	public DcerpcHandle getDcerpcHandle() {
		return _dcerpcHandle;
	}

	public byte[] getSessionKey() {
		return _sessionKey;
	}

	public int getNegotiateFlags() {
		return _negotiateFlags;
	}

	public void setDcerpcHandle(DcerpcHandle dcerpcHandle) {
		_dcerpcHandle = dcerpcHandle;
	}

	protected byte[] computeNetlogonCredential(
		byte[] input, byte[] sessionKey) {

		byte[] k1 = new byte[7];
		byte[] k2 = new byte[7];

		System.arraycopy(sessionKey, 0, k1, 0, 7);
		System.arraycopy(sessionKey, 7, k2, 0, 7);

		DES k3 = new DES(k1);
		DES k4 = new DES(k2);

		byte[] output1 = new byte[8];
		byte[] output2 = new byte[8];

		k3.encrypt(input, output1);
		k4.encrypt(output1, output2);

		return output2;
	}

	protected byte[] computeSessionKey(
			byte[] sharedSecret, byte[] clientChallenge, byte[] serverChallenge)
		throws NoSuchAlgorithmException {

		MessageDigest messageDigest = MessageDigest.getInstance("MD5");

		byte[] zeroes = {0, 0, 0, 0};

		messageDigest.update(zeroes, 0, 4);
		messageDigest.update(clientChallenge, 0, 8);
		messageDigest.update(serverChallenge, 0, 8);

		HMACT64 hmact64 = new HMACT64(sharedSecret);

		hmact64.update(messageDigest.digest());

		return hmact64.digest();
	}

	private int _negotiateFlags;

	private byte[] _clientCredential;
	private DcerpcHandle _dcerpcHandle;
	private byte[] _sessionKey;

}