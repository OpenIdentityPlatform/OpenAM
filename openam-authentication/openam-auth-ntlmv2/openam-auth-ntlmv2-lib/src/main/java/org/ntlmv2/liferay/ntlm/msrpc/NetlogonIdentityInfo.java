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

import jcifs.dcerpc.UnicodeString;
import jcifs.dcerpc.rpc;
import jcifs.dcerpc.ndr.NdrBuffer;
import jcifs.dcerpc.ndr.NdrObject;

/**
 * @author Marcellus Tavares
 */
public class NetlogonIdentityInfo extends NdrObject {

	public NetlogonIdentityInfo(
		String logonDomainName, int parameterControl, int reservedLow,
		int reservedHigh, String userName, String workstation) {

		_logonDomainName = new UnicodeString(logonDomainName, false);
		_parameterControl= parameterControl;
		_reservedLow = reservedLow;
		_reservedHigh = reservedHigh;
		_userName = new UnicodeString(userName, false);
		_workstation = new UnicodeString(workstation, false);
	}

	@Override
	public void decode(NdrBuffer ndrBuffer) {
	}

	@Override
	public void encode(NdrBuffer ndrBuffer) {
		ndrBuffer.enc_ndr_short(_logonDomainName.length);
		ndrBuffer.enc_ndr_short(_logonDomainName.maximum_length);
		ndrBuffer.enc_ndr_referent(_logonDomainName.buffer, 1);
		ndrBuffer.enc_ndr_long(_parameterControl);
		ndrBuffer.enc_ndr_long(_reservedLow);
		ndrBuffer.enc_ndr_long(_reservedHigh);
		ndrBuffer.enc_ndr_short(_userName.length);
		ndrBuffer.enc_ndr_short(_userName.maximum_length);
		ndrBuffer.enc_ndr_referent(_userName.buffer, 1);
		ndrBuffer.enc_ndr_short(_workstation.length);
		ndrBuffer.enc_ndr_short(_workstation.maximum_length);
		ndrBuffer.enc_ndr_referent(_workstation.buffer, 1);
	}

	public void encodeLogonDomainName(NdrBuffer ndrBuffer) {
		encodeUnicodeString(ndrBuffer, _logonDomainName);
	}

	public void encodeUserName(NdrBuffer ndrBuffer) {
		encodeUnicodeString(ndrBuffer, _userName);
	}

	public void encodeWorkStationName(NdrBuffer ndrBuffer) {
		encodeUnicodeString(ndrBuffer, _workstation);
	}

	protected void encodeUnicodeString(
		NdrBuffer ndrBuffer, rpc.unicode_string string ) {

		ndrBuffer = ndrBuffer.deferred;

		int stringBufferl = string.length / 2;
		int stringBuffers = string.maximum_length / 2;

		ndrBuffer.enc_ndr_long(stringBuffers);
		ndrBuffer.enc_ndr_long(0);
		ndrBuffer.enc_ndr_long(stringBufferl);

		int stringBufferIndex = ndrBuffer.index;

		ndrBuffer.advance(2 * stringBufferl);

		ndrBuffer = ndrBuffer.derive(stringBufferIndex);

		for (int _i = 0; _i < stringBufferl; _i++) {
			ndrBuffer.enc_ndr_short(string.buffer[_i]);
		}
	}

	private rpc.unicode_string _logonDomainName;
	private int _parameterControl;
	private int _reservedHigh;
	private int _reservedLow;
	private rpc.unicode_string _userName;
	private rpc.unicode_string _workstation;

}