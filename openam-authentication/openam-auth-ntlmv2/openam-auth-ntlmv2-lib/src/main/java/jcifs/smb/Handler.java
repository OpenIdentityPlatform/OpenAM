/* jcifs smb client library in Java
 * Copyright (C) 2000  "Michael B. Allen" <jcifs at samba dot org>
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package jcifs.smb;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

public class Handler extends URLStreamHandler {

    static final URLStreamHandler SMB_HANDLER = new Handler();

    protected int getDefaultPort() {
        return SmbConstants.DEFAULT_PORT;
    }
    public URLConnection openConnection( URL u ) throws IOException {
        return new SmbFile( u );
    }
    protected void parseURL( URL u, String spec, int start, int limit ) {
        String host = u.getHost();
        String path, ref;
        int port;

        if( spec.equals( "smb://" )) {
            spec = "smb:////";
            limit += 2;
        } else if( spec.startsWith( "smb://" ) == false &&
                    host != null && host.length() == 0 ) {
            spec = "//" + spec;
            limit += 2;
        }
        super.parseURL( u, spec, start, limit );
        path = u.getPath();
        ref = u.getRef();
        if (ref != null) {
            path += '#' + ref;
        }
        port = u.getPort();
        if( port == -1 ) {
            port = getDefaultPort();
        }
        setURL( u, "smb", u.getHost(), port,
                    u.getAuthority(), u.getUserInfo(),
                    path, u.getQuery(), null );
    }
}
