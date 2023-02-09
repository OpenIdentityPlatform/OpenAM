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

package jcifs.netbios;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

class SocketOutputStream extends FilterOutputStream {

    SocketOutputStream( OutputStream out ) {
        super( out );
    }

    public synchronized void write( byte[] b, int off, int len ) throws IOException {
        if( len > 0xFFFF ) {
            throw new IOException( "write too large: " + len );
        } else if( off < 4 ) {
            throw new IOException( "NetBIOS socket output buffer requires 4 bytes available before off" );
        }

        off -= 4;

        b[off + 0] = (byte)SessionServicePacket.SESSION_MESSAGE;
        b[off + 1] = (byte)0x00; 
        b[off + 2] = (byte)(( len >> 8 ) & 0xFF ); 
        b[off + 3] = (byte)( len & 0xFF ); 

        out.write( b, off, 4 + len );
    }
}
