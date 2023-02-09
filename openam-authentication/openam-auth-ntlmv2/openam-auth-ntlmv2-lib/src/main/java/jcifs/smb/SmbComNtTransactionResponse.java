/* jcifs smb client library in Java
 * Copyright (C) 2005  "Michael B. Allen" <jcifs at samba dot org>
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

abstract class SmbComNtTransactionResponse extends SmbComTransactionResponse {

    SmbComNtTransactionResponse() {
        super();
    }

    int readParameterWordsWireFormat( byte[] buffer, int bufferIndex ) {
        int start = bufferIndex;

        buffer[bufferIndex++] = (byte)0x00;        // Reserved
        buffer[bufferIndex++] = (byte)0x00;        // Reserved
        buffer[bufferIndex++] = (byte)0x00;        // Reserved

        totalParameterCount = readInt4( buffer, bufferIndex );
        if( bufDataStart == 0 ) {
            bufDataStart = totalParameterCount;
        }
        bufferIndex += 4;
        totalDataCount = readInt4( buffer, bufferIndex );
        bufferIndex += 4;
        parameterCount = readInt4( buffer, bufferIndex );
        bufferIndex += 4;
        parameterOffset = readInt4( buffer, bufferIndex );
        bufferIndex += 4;
        parameterDisplacement = readInt4( buffer, bufferIndex );
        bufferIndex += 4;
        dataCount = readInt4( buffer, bufferIndex );
        bufferIndex += 4;
        dataOffset = readInt4( buffer, bufferIndex );
        bufferIndex += 4;
        dataDisplacement = readInt4( buffer, bufferIndex );
        bufferIndex += 4;
        setupCount = buffer[bufferIndex] & 0xFF;
        bufferIndex += 2;
        if( setupCount != 0 ) {
            if( log.level >= 3 )
                log.println( "setupCount is not zero: " + setupCount );
        }

        return bufferIndex - start;
    }
}
