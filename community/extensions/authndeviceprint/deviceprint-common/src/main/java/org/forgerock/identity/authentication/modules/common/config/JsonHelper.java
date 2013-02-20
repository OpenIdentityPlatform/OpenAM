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

package org.forgerock.identity.authentication.modules.common.config;

import java.io.IOException;
import java.io.InputStream;

public class JsonHelper {
	public static <T> T readJSONFromFile(String filePath, Class<T> clazz) {
		InputStream resourceAsStream = null;
		try {
			resourceAsStream = JsonHelper.class.getResourceAsStream(filePath);
			ReadableDateMapper mapper = new ReadableDateMapper();
			return mapper.readValue(resourceAsStream, clazz);
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			if(resourceAsStream != null) {
				try {
					resourceAsStream.close();
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		}
	}
}
