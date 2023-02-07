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

class SocketInputStream extends InputStream {

    private static final int TMP_BUFFER_SIZE = 256;

    private InputStream in;
    private SessionServicePacket ssp;
    private int tot, bip, n;
    private byte[] header, tmp;

    SocketInputStream( InputStream in ) {
        this.in = in;
        header = new byte[4];
        tmp = new byte[TMP_BUFFER_SIZE];
    }

    public synchronized int read() throws IOException {
        if( read( tmp, 0, 1 ) < 0 ) {
            return -1;
        }
        return tmp[0] & 0xFF;
    }
    public synchronized int read( byte[] b ) throws IOException {
        return read( b, 0, b.length );
    }

    /* This method will not return until len bytes have been read
     * or the stream has been closed.
     */

    public synchronized int read( byte[] b, int off, int len ) throws IOException {
        if( len == 0 ) {
            return 0;
        }
        tot = 0;

        while( true ) {
            while( bip > 0 ) {
                n = in.read( b, off, Math.min( len, bip ));
                if( n == -1 ) {
                    return tot > 0 ? tot : -1;
                }
                tot += n;
                off += n;
                len -= n;
                bip -= n;
                if( len == 0 ) {
                    return tot;
                }
            }

            switch( SessionServicePacket.readPacketType( in, header, 0 )) {
                case SessionServicePacket.SESSION_KEEP_ALIVE:
                    break;
                case SessionServicePacket.SESSION_MESSAGE:
                    bip = SessionServicePacket.readLength( header, 0 );
                    break;
                case -1:
                    if( tot > 0 ) {
                        return tot;
                    }
                    return -1;
            }
        }
    }
    public synchronized long skip( long numbytes ) throws IOException {
        if( numbytes <= 0 ) {
            return 0;
        }
        long n = numbytes;
        while( n > 0 ) {
            int r = read( tmp, 0, (int)Math.min( (long)TMP_BUFFER_SIZE, n ));
            if (r < 0) {
                break;
            }
            n -= r;
        }
        return numbytes - n;
    }
    public int available() throws IOException {
        if( bip > 0 ) {
            return bip;
        }
        return in.available();
    }
    public void close() throws IOException {
        in.close();
    }
}

