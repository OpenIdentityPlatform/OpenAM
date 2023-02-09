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

import java.io.IOException;

class NtTransQuerySecurityDescResponse extends SmbComNtTransactionResponse {

    SecurityDescriptor securityDescriptor;

    NtTransQuerySecurityDescResponse() {
        super();
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
        length = readInt4( buffer, bufferIndex );
        return 4;
    }
    int readDataWireFormat( byte[] buffer, int bufferIndex, int len ) {
        int start = bufferIndex;

        if (errorCode != 0)
            return 4;

        try {
            securityDescriptor = new SecurityDescriptor();
            bufferIndex += securityDescriptor.decode(buffer, bufferIndex, len);
        } catch (IOException ioe) {
            throw new RuntimeException(ioe.getMessage());
        }

        return bufferIndex - start;
    }
    public String toString() {
        return new String( "NtTransQuerySecurityResponse[" +
            super.toString() + "]" );
    }
}
