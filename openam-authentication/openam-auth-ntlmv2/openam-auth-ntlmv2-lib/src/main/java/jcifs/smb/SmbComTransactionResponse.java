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

import java.util.Enumeration;

abstract class SmbComTransactionResponse extends ServerMessageBlock implements Enumeration {

    // relative to headerStart
    private static final int SETUP_OFFSET        = 61;

    private static final int DISCONNECT_TID      = 0x01;
    private static final int ONE_WAY_TRANSACTION = 0x02;

    private int pad;
    private int pad1;
    private boolean parametersDone, dataDone;

    protected int totalParameterCount;
    protected int totalDataCount;
    protected int parameterCount;
    protected int parameterOffset;
    protected int parameterDisplacement;
    protected int dataOffset;
    protected int dataDisplacement;
    protected int setupCount;
    protected int bufParameterStart;
    protected int bufDataStart;

    int dataCount;
    byte subCommand;
    boolean hasMore = true;
    boolean isPrimary = true;
    byte[] txn_buf;

    /* for doNetEnum and doFindFirstNext */
    int status;
    int numEntries;
    FileEntry[] results;

    SmbComTransactionResponse() {
        txn_buf = null;
    }

    void reset() {
        super.reset();
        bufDataStart = 0;
        isPrimary = hasMore = true; 
        parametersDone = dataDone = false;
    }
    public boolean hasMoreElements() {
        return errorCode == 0 && hasMore;
    }
    public Object nextElement() {
        if( isPrimary ) {
            isPrimary = false;
        }
        return this;
    }
    int writeParameterWordsWireFormat( byte[] dst, int dstIndex ) {
        return 0;
    }
    int writeBytesWireFormat( byte[] dst, int dstIndex ) {
        return 0;
    }
    int readParameterWordsWireFormat( byte[] buffer, int bufferIndex ) {
        int start = bufferIndex;

        totalParameterCount = readInt2( buffer, bufferIndex );
        if( bufDataStart == 0 ) {
            bufDataStart = totalParameterCount;
        }
        bufferIndex += 2;
        totalDataCount = readInt2( buffer, bufferIndex );
        bufferIndex += 4; // Reserved
        parameterCount = readInt2( buffer, bufferIndex );
        bufferIndex += 2;
        parameterOffset = readInt2( buffer, bufferIndex );
        bufferIndex += 2;
        parameterDisplacement = readInt2( buffer, bufferIndex );
        bufferIndex += 2;
        dataCount = readInt2( buffer, bufferIndex );
        bufferIndex += 2;
        dataOffset = readInt2( buffer, bufferIndex );
        bufferIndex += 2;
        dataDisplacement = readInt2( buffer, bufferIndex );
        bufferIndex += 2;
        setupCount = buffer[bufferIndex] & 0xFF;
        bufferIndex += 2;
        if( setupCount != 0 ) {
            if( log.level > 2 )
                log.println( "setupCount is not zero: " + setupCount );
        }

        return bufferIndex - start;
    }
    int readBytesWireFormat( byte[] buffer, int bufferIndex ) {
        pad = pad1 = 0;
        int n;

        if( parameterCount > 0 ) {
            bufferIndex += pad = parameterOffset - ( bufferIndex - headerStart );
            System.arraycopy( buffer, bufferIndex, txn_buf,
                            bufParameterStart + parameterDisplacement, parameterCount );
            bufferIndex += parameterCount;
        }
        if( dataCount > 0 ) {
            bufferIndex += pad1 = dataOffset - ( bufferIndex - headerStart );
            System.arraycopy( buffer, bufferIndex, txn_buf,
                            bufDataStart + dataDisplacement, dataCount );
            bufferIndex += dataCount;
        }

        /* Check to see if the entire transaction has been
         * read. If so call the read methods.
         */

        if( !parametersDone &&
                ( parameterDisplacement + parameterCount ) == totalParameterCount) {
            parametersDone = true;
        }

        if( !dataDone && ( dataDisplacement + dataCount ) == totalDataCount) {
            dataDone = true;
        }

        if( parametersDone && dataDone ) {
            hasMore = false;
            readParametersWireFormat( txn_buf, bufParameterStart, totalParameterCount );
            readDataWireFormat( txn_buf, bufDataStart, totalDataCount );
        }

        return pad + parameterCount + pad1 + dataCount;
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
