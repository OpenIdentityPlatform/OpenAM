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

import jcifs.Config;
import jcifs.util.Hexdump;

import java.util.Enumeration;

abstract class SmbComTransaction extends ServerMessageBlock implements Enumeration {

    private static final int DEFAULT_MAX_DATA_COUNT =
            Config.getInt( "jcifs.smb.client.transaction_buf_size",
                    SmbComTransaction.TRANSACTION_BUF_SIZE ) - 512;

    // relative to headerStart
    private static final int PRIMARY_SETUP_OFFSET        = 61;
    private static final int SECONDARY_PARAMETER_OFFSET  = 51;

    private static final int DISCONNECT_TID      = 0x01;
    private static final int ONE_WAY_TRANSACTION = 0x02;

    private static final int PADDING_SIZE = 2;

    private int flags = 0x00;
    private int fid;
    private int pad = 0;
    private int pad1 = 0;
    private boolean hasMore = true;
    private boolean isPrimary = true;
    private int bufParameterOffset;
    private int bufDataOffset;

    static final int TRANSACTION_BUF_SIZE = 0xFFFF;

    static final byte TRANS2_FIND_FIRST2            = (byte)0x01;
    static final byte TRANS2_FIND_NEXT2             = (byte)0x02;
    static final byte TRANS2_QUERY_FS_INFORMATION   = (byte)0x03;
    static final byte TRANS2_QUERY_PATH_INFORMATION = (byte)0x05;
    static final byte TRANS2_GET_DFS_REFERRAL       = (byte)0x10;
    static final byte TRANS2_SET_FILE_INFORMATION   = (byte)0x08;

    static final int NET_SHARE_ENUM   = 0x0000;
    static final int NET_SERVER_ENUM2 = 0x0068;
    static final int NET_SERVER_ENUM3 = 0x00D7;

    static final byte TRANS_PEEK_NAMED_PIPE     = (byte)0x23;
    static final byte TRANS_WAIT_NAMED_PIPE     = (byte)0x53;
    static final byte TRANS_CALL_NAMED_PIPE     = (byte)0x54;
    static final byte TRANS_TRANSACT_NAMED_PIPE = (byte)0x26;

    protected int primarySetupOffset;
    protected int secondaryParameterOffset;
    protected int parameterCount;
    protected int parameterOffset;
    protected int parameterDisplacement;
    protected int dataCount;
    protected int dataOffset;
    protected int dataDisplacement;

    int totalParameterCount;
    int totalDataCount;
    int maxParameterCount;
    int maxDataCount = DEFAULT_MAX_DATA_COUNT;
    byte maxSetupCount;
    int timeout = 0;
    int setupCount = 1;
    byte subCommand;
    String name = "";
    int maxBufferSize; // set in SmbTransport.sendTransaction() before nextElement called

    byte[] txn_buf;

    SmbComTransaction() {
        maxParameterCount = 1024;
        primarySetupOffset = PRIMARY_SETUP_OFFSET;
        secondaryParameterOffset = SECONDARY_PARAMETER_OFFSET;
    }

    void reset() {
        super.reset();
        isPrimary = hasMore = true; 
    }
    void reset( int key, String lastName ) {
        reset();
    }
    public boolean hasMoreElements() {
        return hasMore;
    }
    public Object nextElement() {
        if( isPrimary ) {
            isPrimary = false;

            parameterOffset = primarySetupOffset + ( setupCount * 2 ) + 2;
            if (command != SMB_COM_NT_TRANSACT) {
                if( command == SMB_COM_TRANSACTION && isResponse() == false ) {
                    parameterOffset += stringWireLength( name, parameterOffset );
                }
            } else if (command == SMB_COM_NT_TRANSACT) {
                parameterOffset += 2;
            }
            pad = parameterOffset % PADDING_SIZE;
            pad = pad == 0 ? 0 : PADDING_SIZE - pad;
            parameterOffset += pad;

            totalParameterCount = writeParametersWireFormat( txn_buf, bufParameterOffset );
            bufDataOffset = totalParameterCount; // data comes right after data

            int available = maxBufferSize - parameterOffset;
            parameterCount = Math.min( totalParameterCount, available );
            available -= parameterCount;

            dataOffset = parameterOffset + parameterCount;
            pad1 = dataOffset % PADDING_SIZE;
            pad1 = pad1 == 0 ? 0 : PADDING_SIZE - pad1;
            dataOffset += pad1;

            totalDataCount = writeDataWireFormat( txn_buf, bufDataOffset );

            dataCount = Math.min( totalDataCount, available );
        } else {
            if (command != SMB_COM_NT_TRANSACT) {
                command = SMB_COM_TRANSACTION_SECONDARY;
            } else {
                command = SMB_COM_NT_TRANSACT_SECONDARY;
            }
            // totalParameterCount and totalDataCount are set ok from primary

            parameterOffset = SECONDARY_PARAMETER_OFFSET;
            if(( totalParameterCount - parameterDisplacement ) > 0 ) {
                pad = parameterOffset % PADDING_SIZE;
                pad = pad == 0 ? 0 : PADDING_SIZE - pad;
                parameterOffset += pad;
            }

            // caclulate parameterDisplacement before calculating new parameterCount
            parameterDisplacement += parameterCount;

            int available = maxBufferSize - parameterOffset - pad;
            parameterCount = Math.min( totalParameterCount - parameterDisplacement, available);
            available -= parameterCount;

            dataOffset = parameterOffset + parameterCount;
            pad1 = dataOffset % PADDING_SIZE;
            pad1 = pad1 == 0 ? 0 : PADDING_SIZE - pad1;
            dataOffset += pad1;

            dataDisplacement += dataCount;

            available -= pad1;
            dataCount = Math.min( totalDataCount - dataDisplacement, available );
        }
        if(( parameterDisplacement + parameterCount ) >= totalParameterCount &&
                    ( dataDisplacement + dataCount ) >= totalDataCount ) {
            hasMore = false;
        }
        return this;
    }
    int writeParameterWordsWireFormat( byte[] dst, int dstIndex ) {
        int start = dstIndex;

        writeInt2( totalParameterCount, dst, dstIndex );
        dstIndex += 2;
        writeInt2( totalDataCount, dst, dstIndex );
        dstIndex += 2;
        if( command != SMB_COM_TRANSACTION_SECONDARY ) {
            writeInt2( maxParameterCount, dst, dstIndex );
            dstIndex += 2;
            writeInt2( maxDataCount, dst, dstIndex );
            dstIndex += 2;
            dst[dstIndex++] = maxSetupCount;
            dst[dstIndex++] = (byte)0x00;           // Reserved1
            writeInt2( flags, dst, dstIndex );
            dstIndex += 2;
            writeInt4( timeout, dst, dstIndex );
            dstIndex += 4;
            dst[dstIndex++] = (byte)0x00;           // Reserved2
            dst[dstIndex++] = (byte)0x00;
        }
        writeInt2( parameterCount, dst, dstIndex );
        dstIndex += 2;
//        writeInt2(( parameterCount == 0 ? 0 : parameterOffset ), dst, dstIndex );
        writeInt2(parameterOffset, dst, dstIndex );
        dstIndex += 2;
        if( command == SMB_COM_TRANSACTION_SECONDARY ) {
            writeInt2( parameterDisplacement, dst, dstIndex );
            dstIndex += 2;
        }
        writeInt2( dataCount, dst, dstIndex );
        dstIndex += 2;
        writeInt2(( dataCount == 0 ? 0 : dataOffset ), dst, dstIndex );
        dstIndex += 2;
        if( command == SMB_COM_TRANSACTION_SECONDARY ) {
            writeInt2( dataDisplacement, dst, dstIndex );
            dstIndex += 2;
        } else {
            dst[dstIndex++] = (byte)setupCount;
            dst[dstIndex++] = (byte)0x00;           // Reserved3
            dstIndex += writeSetupWireFormat( dst, dstIndex );
        }

        return dstIndex - start;
    }
    int writeBytesWireFormat( byte[] dst, int dstIndex ) {
        int start = dstIndex;
        int p = pad;

        if( command == SMB_COM_TRANSACTION && isResponse() == false ) {
            dstIndex += writeString( name, dst, dstIndex );
        }

        if( parameterCount > 0 ) {
            while( p-- > 0 ) {
                dst[dstIndex++] = (byte)0x00;       // Pad
            }

            System.arraycopy( txn_buf, bufParameterOffset, dst, dstIndex, parameterCount );
            dstIndex += parameterCount;
        }

        if( dataCount > 0 ) {
            p = pad1;
            while( p-- > 0 ) {
                dst[dstIndex++] = (byte)0x00;       // Pad1
            }
            System.arraycopy( txn_buf, bufDataOffset, dst, dstIndex, dataCount );
            bufDataOffset += dataCount;
            dstIndex += dataCount;
        }

        return dstIndex - start;
    }
    int readParameterWordsWireFormat( byte[] buffer, int bufferIndex ) {
        return 0;
    }
    int readBytesWireFormat( byte[] buffer, int bufferIndex ) {
        return 0;
    }

    abstract int writeSetupWireFormat( byte[] dst, int dstIndex );
    abstract int writeParametersWireFormat( byte[] dst, int dstIndex );
    abstract int writeDataWireFormat( byte[] dst, int dstIndex );
    abstract int readSetupWireFormat( byte[] buffer, int bufferIndex, int len );
    abstract int readParametersWireFormat( byte[] buffer, int bufferIndex, int len );
    abstract int readDataWireFormat( byte[] buffer, int bufferIndex, int len );

    public String toString() {
        return new String( super.toString() +
            ",totalParameterCount=" + totalParameterCount +
            ",totalDataCount=" + totalDataCount +
            ",maxParameterCount=" + maxParameterCount +
            ",maxDataCount=" + maxDataCount +
            ",maxSetupCount=" + (int)maxSetupCount +
            ",flags=0x" + Hexdump.toHexString( flags, 2 ) +
            ",timeout=" + timeout +
            ",parameterCount=" + parameterCount +
            ",parameterOffset=" + parameterOffset +
            ",parameterDisplacement=" + parameterDisplacement +
            ",dataCount=" + dataCount +
            ",dataOffset=" + dataOffset +
            ",dataDisplacement=" + dataDisplacement +
            ",setupCount=" + setupCount +
            ",pad=" + pad +
            ",pad1=" + pad1 );
    }
}
