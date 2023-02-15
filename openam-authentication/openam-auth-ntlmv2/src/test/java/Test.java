/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright 2023 Open Identity Platform Community.
 */


import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import org.ntlmv2.liferay.NtlmLogonException;
import org.ntlmv2.liferay.NtlmManager;
import org.ntlmv2.liferay.NtlmUserAccount;

import jcifs.util.Base64;

public class Test {

	@org.testng.annotations.Test
	public void test() throws IOException, NoSuchAlgorithmException, NtlmLogonException{
//		System.setProperty("jcifs.util.loglevel", "4");
//		System.setProperty("jcifs.smb.client.connTimeout", "5000");
//		NtlmManager ntlmManager = new NtlmManager(
//				"domain",//domain, 
//				"domainController",//domainController, 
//				"domainControllerHostName",//domainControllerHostName, 
//				"serviceAccount",//serviceAccount,
//				"servicePassword"//servicePassword
//		);
//
//		byte[] serverChallenge = new byte[8];
//		ntlmManager.negotiate(Base64.decode("NTLM TlRMTVNTUAABAAAAB4IIogAAAAAAAAAAAAAAAAAAAAAFAs4OAAAADw==".substring(5)), serverChallenge);
//
//		NtlmUserAccount ntlmUserAccount = ntlmManager.authenticate(
//				Base64.decode("NTLM TlRMTVNTUAADAAAAGAAYAHYAAAC6ALoAjgAAAAoACgBIAAAAEAAQAFIAAAAUABQAYgAAAAAAAABIAQAABYKIogUCzg4AAAAPQQBEAE0AUwBLAGEAYQBzAGgAdgBlAHQANABQADAANAAtAFQARQBSAE0AMAAxAHWIM5lk6fav5NHbUqrqqo4hxtZqAO/pgJqh5mE9vPztFGIVWw6RHWEBAQAAAAAAAPPhEx8JmM4BIcbWagDv6YAAAAAAAgAKAEEARABNAFMASwABABoAUAAwADQALQBDAFIATQAtAEcAVQBJADAAMgAEABQAbQBzAGsALgBtAHQAcwAuAHIAdQADAC4AcAAwADQALQBjAHIAbQAtAGcAdQBpADAAMgAuAHAAdgAuAG0AdABzAC4AcgB1AAUADABtAHQAcwAuAHIAdQAAAAAAAAAAAA==".substring(5)),
//				Base64.decode("NTLM TlRMTVNTUAACAAAACgAKADgAAAAFgomiwqafoVTQ/aEAAAAAAAAAAIoAigBCAAAABQLODgAAAA9BAEQATQBTAEsAAgAKAEEARABNAFMASwABABoAUAAwADQALQBDAFIATQAtAEcAVQBJADAAMgAEABQAbQBzAGsALgBtAHQAcwAuAHIAdQADAC4AcAAwADQALQBjAHIAbQAtAGcAdQBpADAAMgAuAHAAdgAuAG0AdABzAC4AcgB1AAUADABtAHQAcwAuAHIAdQAAAAAA".substring(5))
//				);
	}
}
