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


/**
 * @author Michael C. Han
 */
public class NtlmLogonException extends Exception {

	public NtlmLogonException() {
		super();
	}

	public NtlmLogonException(String msg) {
		super(msg);
	}

	public NtlmLogonException(String msg, Throwable cause) {
		super(msg, cause);
	}

	public NtlmLogonException(Throwable cause) {
		super(cause);
	}

}