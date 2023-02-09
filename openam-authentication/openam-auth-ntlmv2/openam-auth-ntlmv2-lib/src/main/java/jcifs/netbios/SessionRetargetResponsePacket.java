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

import java.io.IOException;
import java.io.InputStream;

class SessionRetargetResponsePacket extends SessionServicePacket {

    private NbtAddress retargetAddress;
    private int retargetPort;

    SessionRetargetResponsePacket() {
        type = SESSION_RETARGET_RESPONSE;
        length = 6;
    }

    int writeTrailerWireFormat( byte[] dst, int dstIndex ) {
        return 0;
    }
    int readTrailerWireFormat( InputStream in,
                            byte[] buffer,
                            int bufferIndex )
                            throws IOException {
        if( in.read( buffer, bufferIndex, length ) != length ) {
            throw new IOException( "unexpected EOF reading netbios retarget session response" );
        }
        int addr = readInt4( buffer, bufferIndex );
        bufferIndex += 4;
        retargetAddress = new NbtAddress( null, addr, false, NbtAddress.B_NODE );
        retargetPort = readInt2( buffer, bufferIndex );
        return length;
    }
}
