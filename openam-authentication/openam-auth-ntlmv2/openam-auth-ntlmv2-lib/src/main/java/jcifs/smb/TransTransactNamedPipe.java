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

class TransTransactNamedPipe extends SmbComTransaction {

    private byte[] pipeData;
    private int pipeFid, pipeDataOff, pipeDataLen;

    TransTransactNamedPipe( int fid, byte[] data, int off, int len ) {
        pipeFid = fid;
        pipeData = data;
        pipeDataOff = off;
        pipeDataLen = len;
        command = SMB_COM_TRANSACTION;
        subCommand = TRANS_TRANSACT_NAMED_PIPE;
        maxParameterCount = 0;
        maxDataCount = 0xFFFF;
        maxSetupCount = (byte)0x00;
        setupCount = 2;
        name = "\\PIPE\\";
    }

    int writeSetupWireFormat( byte[] dst, int dstIndex ) {
        dst[dstIndex++] = subCommand;
        dst[dstIndex++] = (byte)0x00;
        writeInt2( pipeFid, dst, dstIndex );
        dstIndex += 2;
        return 4;
    }
    int readSetupWireFormat( byte[] buffer, int bufferIndex, int len ) {
        return 0;
    }
    int writeParametersWireFormat( byte[] dst, int dstIndex ) {
        return 0;
    }
    int writeDataWireFormat( byte[] dst, int dstIndex ) {
        if(( dst.length - dstIndex ) < pipeDataLen ) {
            if( log.level >= 3 )
                log.println( "TransTransactNamedPipe data too long for buffer" );
            return 0;
        }
        System.arraycopy( pipeData, pipeDataOff, dst, dstIndex, pipeDataLen );
        return pipeDataLen;
    }
    int readParametersWireFormat( byte[] buffer, int bufferIndex, int len ) {
        return 0;
    }
    int readDataWireFormat( byte[] buffer, int bufferIndex, int len ) {
        return 0;
    }
    public String toString() {
        return new String( "TransTransactNamedPipe[" + super.toString() +
            ",pipeFid=" + pipeFid + "]" );
    }
}
