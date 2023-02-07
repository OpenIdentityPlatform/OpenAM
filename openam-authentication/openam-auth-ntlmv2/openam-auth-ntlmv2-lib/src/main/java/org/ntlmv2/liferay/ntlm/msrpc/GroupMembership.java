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

import jcifs.dcerpc.ndr.NdrBuffer;
import jcifs.dcerpc.ndr.NdrObject;

/**
 * @author Marcellus Tavares
 */
public class GroupMembership extends NdrObject {

	public GroupMembership() {
	}

	public GroupMembership(int relativeId, int attributes) {
		_relativeId = relativeId;
		_attributes = attributes;
	}

	@Override
	public void decode(NdrBuffer ndrBuffer) {
		ndrBuffer.align(4);

		_relativeId = ndrBuffer.dec_ndr_long();
		_attributes = ndrBuffer.dec_ndr_long();
	}

	@Override
	public void encode(NdrBuffer ndrBuffer) {
		ndrBuffer.align(4);

		ndrBuffer.enc_ndr_long(_relativeId);
		ndrBuffer.enc_ndr_long(_attributes);
	}

	public int getAttributes() {
		return _attributes;
	}

	public int getRelativeId() {
		return _relativeId;
	}

	private int _attributes;
	private int _relativeId;

}