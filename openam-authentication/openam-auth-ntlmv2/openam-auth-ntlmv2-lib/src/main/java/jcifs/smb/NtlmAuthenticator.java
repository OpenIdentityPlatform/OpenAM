/* jcifs smb client library in Java
 * Copyright (C) 2002  "Michael B. Allen" <jcifs at samba dot org>
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
This class can be extended by applications that wish to trap authentication related exceptions and automatically retry the exceptional operation with different credentials. Read <a href="../../../authhandler.html">jCIFS Exceptions and NtlmAuthenticator</a> for complete details.
 */

public abstract class NtlmAuthenticator {

    private static NtlmAuthenticator auth;

    private String url;
    private SmbAuthException sae;

    private void reset() {
        url = null;
        sae = null;
    }

/**
Set the default <tt>NtlmAuthenticator</tt>. Once the default authenticator is set it cannot be changed. Calling this metho again will have no effect.
 */

    public synchronized static void setDefault( NtlmAuthenticator a ) {
        if( auth != null ) {
            return;
        }
        auth = a;
    }

    protected final String getRequestingURL() {
        return url;
    }
    protected final SmbAuthException getRequestingException() {
        return sae;
    }

/**
Used internally by jCIFS when an <tt>SmbAuthException</tt> is trapped to retrieve new user credentials.
 */

    public static NtlmPasswordAuthentication
                requestNtlmPasswordAuthentication( String url, SmbAuthException sae ) {
        if( auth == null ) {
            return null;
        }
        synchronized( auth ) {
            auth.url = url;
            auth.sae = sae;
            return auth.getNtlmPasswordAuthentication();
        }
    }
/**
An application extending this class must provide an implementation for this method that returns new user credentials try try when accessing SMB resources described by the <tt>getRequestingURL</tt> and <tt>getRequestingException</tt> methods.
If this method returns <tt>null</tt> the <tt>SmbAuthException</tt> that triggered the authenticator check will simply be rethrown. The default implementation returns <tt>null</tt>.
*/
    protected NtlmPasswordAuthentication getNtlmPasswordAuthentication() {
        return null;
    }
}
