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

class Trans2GetDfsReferral extends SmbComTransaction {

    private int maxReferralLevel = 3;

    Trans2GetDfsReferral( String filename ) {
        path = filename;
        command = SMB_COM_TRANSACTION2;
        subCommand = TRANS2_GET_DFS_REFERRAL;
        totalDataCount = 0;
        maxParameterCount = 0;
        maxDataCount = 4096;
        maxSetupCount = (byte)0x00;
    }

    int writeSetupWireFormat( byte[] dst, int dstIndex ) {
        dst[dstIndex++] = subCommand;
        dst[dstIndex++] = (byte)0x00;
        return 2;
    }
    int writeParametersWireFormat( byte[] dst, int dstIndex ) {
        int start = dstIndex;

        writeInt2( maxReferralLevel, dst, dstIndex );
        dstIndex += 2;
        dstIndex += writeString( path, dst, dstIndex );

        return dstIndex - start;
    }
    int writeDataWireFormat( byte[] dst, int dstIndex ) {
        return 0;
    }
    int readSetupWireFormat( byte[] buffer, int bufferIndex, int len ) {
        return 0;
    }
    int readParametersWireFormat( byte[] buffer, int bufferIndex, int len ) {
        return 0;
    }
    int readDataWireFormat( byte[] buffer, int bufferIndex, int len ) {
        return 0;
    }
    public String toString() {
        return new String( "Trans2GetDfsReferral[" + super.toString() +
            ",maxReferralLevel=0x" + maxReferralLevel +
            ",filename=" + path + "]" );
    }
}
