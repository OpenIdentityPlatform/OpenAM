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

abstract class SmbComNtTransaction extends SmbComTransaction {

    // relative to headerStart
    private static final int NTT_PRIMARY_SETUP_OFFSET       = 69;
    private static final int NTT_SECONDARY_PARAMETER_OFFSET  = 51;

    static final int NT_TRANSACT_QUERY_SECURITY_DESC = 6;

    int function;

    SmbComNtTransaction() {
        super();
        primarySetupOffset = NTT_PRIMARY_SETUP_OFFSET;
        secondaryParameterOffset = NTT_SECONDARY_PARAMETER_OFFSET;
    }

    int writeParameterWordsWireFormat( byte[] dst, int dstIndex ) {
        int start = dstIndex;

        if (command != SMB_COM_NT_TRANSACT_SECONDARY) {
            dst[dstIndex++] = maxSetupCount;
        } else {
            dst[dstIndex++] = (byte)0x00;          // Reserved
        }
        dst[dstIndex++] = (byte)0x00;          // Reserved
        dst[dstIndex++] = (byte)0x00;          // Reserved
        writeInt4( totalParameterCount, dst, dstIndex );
        dstIndex += 4;
        writeInt4( totalDataCount, dst, dstIndex );
        dstIndex += 4;
        if (command != SMB_COM_NT_TRANSACT_SECONDARY) {
            writeInt4( maxParameterCount, dst, dstIndex );
            dstIndex += 4;
            writeInt4( maxDataCount, dst, dstIndex );
            dstIndex += 4;
        }
        writeInt4( parameterCount, dst, dstIndex );
        dstIndex += 4;
        writeInt4(( parameterCount == 0 ? 0 : parameterOffset ), dst, dstIndex );
        dstIndex += 4;
        if (command == SMB_COM_NT_TRANSACT_SECONDARY) {
            writeInt4( parameterDisplacement, dst, dstIndex );
            dstIndex += 4;
        }
        writeInt4( dataCount, dst, dstIndex );
        dstIndex += 4;
        writeInt4(( dataCount == 0 ? 0 : dataOffset ), dst, dstIndex );
        dstIndex += 4;
        if (command == SMB_COM_NT_TRANSACT_SECONDARY) {
            writeInt4( dataDisplacement, dst, dstIndex );
            dstIndex += 4;
            dst[dstIndex++] = (byte)0x00;      // Reserved1
        } else {
            dst[dstIndex++] = (byte)setupCount;
            writeInt2( function, dst, dstIndex );
            dstIndex += 2;
            dstIndex += writeSetupWireFormat( dst, dstIndex );
        }

        return dstIndex - start;
    }
}
