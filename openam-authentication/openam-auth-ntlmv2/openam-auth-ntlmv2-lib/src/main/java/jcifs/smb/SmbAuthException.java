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

/**
 * The <code>SmbAuthException</code> encapsulates the variety of
 * authentication related error codes returned by an SMB server.
 * <p>
 * See <a href="../../../authhandler.html">jCIFS Exceptions and NtlmAuthenticator</a> for more information about <code>SmbAuthException</code>.
 */

public class SmbAuthException extends SmbException {

    SmbAuthException( int errcode ) {
        super( errcode, null );
    }
}
