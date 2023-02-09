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

class SmbComEcho extends ServerMessageBlock {

    int echoCount;

    SmbComEcho( int echoCount ) {
        command = SMB_COM_ECHO;
        this.echoCount = echoCount;
    }

    int writeParameterWordsWireFormat( byte[] dst, int dstIndex ) {
        writeInt2( echoCount, dst, dstIndex );
        return 2;
    }
    int writeBytesWireFormat( byte[] dst, int dstIndex ) {
        dst[dstIndex] = (byte)'M';
        return 1;
    }
    int readParameterWordsWireFormat( byte[] buffer, int bufferIndex ) {
        return 0;
    }
    int readBytesWireFormat( byte[] buffer, int bufferIndex ) {
        return 0;
    }
    public String toString() {
        return new String( "SmbComEcho[" +
            super.toString() +
            ",echoCount=" + echoCount + "]" );
    }
}
