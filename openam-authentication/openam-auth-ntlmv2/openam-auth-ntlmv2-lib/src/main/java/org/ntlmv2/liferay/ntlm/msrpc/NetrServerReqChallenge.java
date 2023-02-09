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

package org.ntlmv2.liferay.ntlm.msrpc;

import jcifs.dcerpc.DcerpcMessage;
import jcifs.dcerpc.ndr.NdrBuffer;

/**
 * @author Marcellus Tavares
 */
public class NetrServerReqChallenge extends DcerpcMessage {

	public NetrServerReqChallenge(
		String primaryName, String computerName, byte[] clientChallenge,
		byte[] serverChallenge) {

		_primaryName = primaryName;
		_computerName = computerName;
		_clientChallenge = clientChallenge;
		_serverChallenge = serverChallenge;

		 ptype = 0;
		 flags = DCERPC_FIRST_FRAG | DCERPC_LAST_FRAG;
	}

	@Override
	public void decode_out(NdrBuffer ndrBuffer) {
		int index = ndrBuffer.index;

		ndrBuffer.advance(8);

		ndrBuffer = ndrBuffer.derive(index);

		for (int i = 0; i < 8; i++) {
			_serverChallenge[i] = (byte) ndrBuffer.dec_ndr_small();
		}

		_status = ndrBuffer.dec_ndr_long();
	}

	@Override
	public void encode_in(NdrBuffer ndrBuffer) {
		ndrBuffer.enc_ndr_referent(_primaryName, 1);
		ndrBuffer.enc_ndr_string(_primaryName);
		ndrBuffer.enc_ndr_string(_computerName);

		int index = ndrBuffer.index;

		ndrBuffer.advance(8);

		ndrBuffer = ndrBuffer.derive(index);

		for (int i = 0; i < 8; i++) {
			ndrBuffer.enc_ndr_small(_clientChallenge[i]);
		}
	}

	@Override
	public int getOpnum() {
		return 4;
	}

	public byte[] getServerChallenge() {
		return _serverChallenge;
	}

	public int getStatus() {
		return _status;
	}

	private byte[] _clientChallenge;
	private String _computerName;
	private String _primaryName;
	private byte[] _serverChallenge;
	private int _status;

}