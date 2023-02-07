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

class TransPeekNamedPipeResponse extends SmbComTransactionResponse {

    private SmbNamedPipe pipe;
    private int head;

    static final int STATUS_DISCONNECTED = 1;
    static final int STATUS_LISTENING = 2;
    static final int STATUS_CONNECTION_OK = 3;
    static final int STATUS_SERVER_END_CLOSED = 4;

    int status, available;

    TransPeekNamedPipeResponse( SmbNamedPipe pipe ) {
        this.pipe = pipe;
    }

    int writeSetupWireFormat( byte[] dst, int dstIndex ) {
        return 0;
    }
    int writeParametersWireFormat( byte[] dst, int dstIndex ) {
        return 0;
    }
    int writeDataWireFormat( byte[] dst, int dstIndex ) {
        return 0;
    }
    int readSetupWireFormat( byte[] buffer, int bufferIndex, int len ) {
        return 0;
    }
    int readParametersWireFormat( byte[] buffer, int bufferIndex, int len ) {
        available = readInt2( buffer, bufferIndex ); bufferIndex += 2;
        head = readInt2( buffer, bufferIndex ); bufferIndex += 2;
        status = readInt2( buffer, bufferIndex );
        return 6;
    }
    int readDataWireFormat( byte[] buffer, int bufferIndex, int len ) {
        return 0;
    }
    public String toString() {
        return new String( "TransPeekNamedPipeResponse[" + super.toString() + "]" );
    }
}
