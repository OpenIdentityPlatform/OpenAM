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

class Trans2QueryFSInformationResponse extends SmbComTransactionResponse {

    // information levels
    static final int SMB_INFO_ALLOCATION = 1;
    static final int SMB_QUERY_FS_SIZE_INFO = 0x103;
    static final int SMB_FS_FULL_SIZE_INFORMATION = 1007;

    class SmbInfoAllocation implements AllocInfo {
        long alloc;                  // Also handles SmbQueryFSSizeInfo
        long free;
        int sectPerAlloc;
        int bytesPerSect;

        public long getCapacity() {
            return alloc * sectPerAlloc * bytesPerSect;
        }
        public long getFree() {
            return free * sectPerAlloc * bytesPerSect;
        }
        public String toString() {
            return new String( "SmbInfoAllocation[" +
                "alloc=" + alloc + ",free=" + free +
                ",sectPerAlloc=" + sectPerAlloc +
                ",bytesPerSect=" + bytesPerSect + "]" );
        }
    }

    private int informationLevel;

    AllocInfo info;

    Trans2QueryFSInformationResponse( int informationLevel ) {
        this.informationLevel = informationLevel;
        command = SMB_COM_TRANSACTION2;
        subCommand = SmbComTransaction.TRANS2_QUERY_FS_INFORMATION;
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
        return 0;
    }
    int readDataWireFormat( byte[] buffer, int bufferIndex, int len ) {
        switch( informationLevel ) {
            case SMB_INFO_ALLOCATION:
                return readSmbInfoAllocationWireFormat( buffer, bufferIndex );
            case SMB_QUERY_FS_SIZE_INFO:
                return readSmbQueryFSSizeInfoWireFormat( buffer, bufferIndex );
            case SMB_FS_FULL_SIZE_INFORMATION:
                return readFsFullSizeInformationWireFormat( buffer, bufferIndex );
            default:
                return 0;
        }
    }

    int readSmbInfoAllocationWireFormat( byte[] buffer, int bufferIndex ) {
        int start = bufferIndex;

        SmbInfoAllocation info = new SmbInfoAllocation();

        bufferIndex += 4; // skip idFileSystem

        info.sectPerAlloc = readInt4( buffer, bufferIndex );
        bufferIndex += 4;

        info.alloc = readInt4( buffer, bufferIndex );
        bufferIndex += 4;

        info.free = readInt4( buffer, bufferIndex );
        bufferIndex += 4;

        info.bytesPerSect = readInt2( buffer, bufferIndex );
        bufferIndex += 4;

        this.info = info;

        return bufferIndex - start;
    }
    int readSmbQueryFSSizeInfoWireFormat( byte[] buffer, int bufferIndex ) {
        int start = bufferIndex;

        SmbInfoAllocation info = new SmbInfoAllocation();

        info.alloc = readInt8( buffer, bufferIndex );
        bufferIndex += 8;

        info.free = readInt8( buffer, bufferIndex );
        bufferIndex += 8;

        info.sectPerAlloc = readInt4( buffer, bufferIndex );
        bufferIndex += 4;

        info.bytesPerSect = readInt4( buffer, bufferIndex );
        bufferIndex += 4;

        this.info = info;

        return bufferIndex - start;
    }
    int readFsFullSizeInformationWireFormat( byte[] buffer, int bufferIndex )
    {
        int start = bufferIndex;
        
        SmbInfoAllocation info = new SmbInfoAllocation();
        
        // Read total allocation units.
        info.alloc = readInt8( buffer, bufferIndex );
        bufferIndex += 8;
        
        // read caller available allocation units 
        info.free = readInt8( buffer, bufferIndex );
        bufferIndex += 8;

        // skip actual free units
        bufferIndex += 8;
        
        info.sectPerAlloc = readInt4( buffer, bufferIndex );
        bufferIndex += 4;
        
        info.bytesPerSect = readInt4( buffer, bufferIndex );
        bufferIndex += 4;

        this.info = info;

        return bufferIndex - start;
    }

    public String toString() {
        return new String( "Trans2QueryFSInformationResponse[" +
            super.toString() + "]" );
    }
}
