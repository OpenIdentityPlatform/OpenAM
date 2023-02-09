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

public abstract class SessionServicePacket { 

    // session service packet types 
    static final int SESSION_MESSAGE = 0x00; 
    static final int SESSION_REQUEST = 0x81; 
    public static final int POSITIVE_SESSION_RESPONSE = 0x82; 
    public static final int NEGATIVE_SESSION_RESPONSE = 0x83; 
    static final int SESSION_RETARGET_RESPONSE = 0x84; 
    static final int SESSION_KEEP_ALIVE = 0x85; 

    static final int MAX_MESSAGE_SIZE = 0x0001FFFF;
    static final int HEADER_LENGTH = 4;

    static void writeInt2( int val, byte[] dst, int dstIndex ) {
        dst[dstIndex++] = (byte)(( val >> 8 ) & 0xFF );
        dst[dstIndex] = (byte)( val & 0xFF );
    }
    static void writeInt4( int val, byte[] dst, int dstIndex ) {
        dst[dstIndex++] = (byte)(( val >> 24 ) & 0xFF );
        dst[dstIndex++] = (byte)(( val >> 16 ) & 0xFF );
        dst[dstIndex++] = (byte)(( val >> 8 ) & 0xFF );
        dst[dstIndex] = (byte)( val & 0xFF );
    }
    static int readInt2( byte[] src, int srcIndex ) {
        return (( src[srcIndex] & 0xFF ) << 8 ) +
                ( src[srcIndex + 1] & 0xFF );
    }
    static int readInt4( byte[] src, int srcIndex ) {
        return (( src[srcIndex] & 0xFF ) << 24 ) +
                (( src[srcIndex + 1] & 0xFF ) << 16 ) +
                (( src[srcIndex + 2] & 0xFF ) << 8 ) +
                ( src[srcIndex + 3] & 0xFF );
    }
    static int readLength( byte[] src, int srcIndex ) {
        srcIndex++;
        return (( src[srcIndex++] & 0x01 ) << 16 ) +
                (( src[srcIndex++] & 0xFF ) << 8 ) +
                ( src[srcIndex++] & 0xFF );
    }
    static int readn( InputStream in,
                byte[] b,
                int off,
                int len ) throws IOException {
        int i = 0, n;

        while (i < len) {
            n = in.read( b, off + i, len - i );
            if (n <= 0) {
                break;
            }
            i += n;
        }

        return i;
    }
    static int readPacketType( InputStream in,
                                    byte[] buffer,
                                    int bufferIndex )
                                    throws IOException {
        int n;
        if(( n = readn( in, buffer, bufferIndex, HEADER_LENGTH )) != HEADER_LENGTH ) {
            if( n == -1 ) {
                return -1;
            }
            throw new IOException( "unexpected EOF reading netbios session header" );
        }
        int t = buffer[bufferIndex] & 0xFF;
        return t;
    }

    int type, length;

    public int writeWireFormat( byte[] dst, int dstIndex ) {
        length = writeTrailerWireFormat( dst, dstIndex + HEADER_LENGTH );
        writeHeaderWireFormat( dst, dstIndex );
        return HEADER_LENGTH + length;
    }
    int readWireFormat( InputStream in, byte[] buffer, int bufferIndex ) throws IOException {
        readHeaderWireFormat( in, buffer, bufferIndex );
        return HEADER_LENGTH + readTrailerWireFormat( in, buffer, bufferIndex );
    }
    int writeHeaderWireFormat( byte[] dst, int dstIndex ) {
        dst[dstIndex++] = (byte)type;
        if( length > 0x0000FFFF ) {
            dst[dstIndex] = (byte)0x01;
        }
        dstIndex++;
        writeInt2( length, dst, dstIndex );
        return HEADER_LENGTH;
    }
    int readHeaderWireFormat( InputStream in,
                                byte[] buffer,
                                int bufferIndex )
                                throws IOException {
        type = buffer[bufferIndex++] & 0xFF;
        length = (( buffer[bufferIndex] & 0x01 ) << 16 ) + readInt2( buffer, bufferIndex + 1 );
        return HEADER_LENGTH;
    }

    abstract int writeTrailerWireFormat( byte[] dst, int dstIndex );
    abstract int readTrailerWireFormat( InputStream in,
                                byte[] buffer,
                                int bufferIndex )
                                throws IOException;
} 
