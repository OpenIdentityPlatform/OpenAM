/* The contents of this file are subject to the terms
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
 * $Id: HttpUtils.java,v 1.1 2007/10/04 16:55:28 hengming Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */

import java.io.*;
import java.net.*;

public class HttpUtils {

    public HttpUtils() {
    }

    public String doPost(URL url, String str) throws IOException {
	URLConnection urlConnection = url.openConnection();
	urlConnection.setDoOutput(true);
	PrintWriter writer;

	writer = new PrintWriter(urlConnection.getOutputStream());
	writer.print(str);
	writer.close();

	//Read results
	boolean readResults = true;
	String result = "";
	if(readResults) {
	    BufferedReader reader;
	    reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));

	    try {
		while(reader.ready()) {
		    result += reader.readLine();
		}
		reader.close();
	    } catch(IOException ioe) {
		throw ioe;
	    }
	}
	return result;
    }

}
