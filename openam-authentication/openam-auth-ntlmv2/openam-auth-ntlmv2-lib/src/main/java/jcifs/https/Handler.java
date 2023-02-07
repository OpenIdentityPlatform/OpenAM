/* jcifs smb client library in Java
 * Copyright (C) 2002  "Michael B. Allen" <jcifs at samba dot org>
 *                   "Eric Glass" <jcifs at samba dot org>
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

package jcifs.https;

/**
 * A <code>URLStreamHandler</code> used to provide NTLM authentication
 * capabilities to the default HTTPS handler.  This acts as a wrapper,
 * handling authentication and passing control to the underlying
 * stream handler.
 */
public class Handler extends jcifs.http.Handler {

    /**
     * The default HTTPS port (<code>443</code>).
     */
    public static final int DEFAULT_HTTPS_PORT = 443;

    /**
     * Returns the default HTTPS port.
     *
     * @return An <code>int</code> containing the default HTTPS port.
     */
    protected int getDefaultPort() {
        return DEFAULT_HTTPS_PORT;
    }

}
