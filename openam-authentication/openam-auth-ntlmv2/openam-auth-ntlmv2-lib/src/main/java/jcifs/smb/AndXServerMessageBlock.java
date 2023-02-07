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

import jcifs.util.Hexdump;

abstract class AndXServerMessageBlock extends ServerMessageBlock {

    private static final int ANDX_COMMAND_OFFSET  = 1;
    private static final int ANDX_RESERVED_OFFSET = 2;
    private static final int ANDX_OFFSET_OFFSET   = 3;

    private byte andxCommand        = (byte)0xFF;
    private int andxOffset          = 0;

    ServerMessageBlock andx = null;

    AndXServerMessageBlock() {
    }
    AndXServerMessageBlock( ServerMessageBlock andx ) {
        if (andx != null) {
            this.andx = andx;
            andxCommand = andx.command;
        }
    }

    int getBatchLimit( byte command ) {
        /* the default limit is 0 batched messages before this
         * one, meaning this message cannot be batched.
         */
        return 0;
    }

    /* 
     * We overload this method from ServerMessageBlock because
     * we want writeAndXWireFormat to write the parameterWords
     * and bytes. This is so we can write batched smbs because
     * all but the first smb of the chaain do not have a header
     * and therefore we do not want to writeHeaderWireFormat. We
     * just recursivly call writeAndXWireFormat.
     */ 

    int encode( byte[] dst, int dstIndex ) {
        int start = headerStart = dstIndex;

        dstIndex += writeHeaderWireFormat( dst, dstIndex );
        dstIndex += writeAndXWireFormat( dst, dstIndex );
        length = dstIndex - start;

        if( digest != null ) {
            digest.sign( dst, headerStart, length, this, response );
        }

        return length;
    }

    /*
     * We overload this because we want readAndXWireFormat to
     * read the parameter words and bytes. This is so when
     * commands are batched together we can recursivly call
     * readAndXWireFormat without reading the non-existent header.
     */

    int decode( byte[] buffer, int bufferIndex ) {
        int start = headerStart = bufferIndex;

        bufferIndex += readHeaderWireFormat( buffer, bufferIndex );
        bufferIndex += readAndXWireFormat( buffer, bufferIndex );

        length = bufferIndex - start;
        return length;
    }
    int writeAndXWireFormat( byte[] dst, int dstIndex ) {
        int start = dstIndex;

        wordCount = writeParameterWordsWireFormat( dst,
                    start + ANDX_OFFSET_OFFSET + 2 );
        wordCount += 4; // for command, reserved, and offset
        dstIndex += wordCount + 1;
        wordCount /= 2;
        dst[start] = (byte)( wordCount & 0xFF );

        byteCount = writeBytesWireFormat( dst, dstIndex + 2 );
        dst[dstIndex++] = (byte)( byteCount & 0xFF );
        dst[dstIndex++] = (byte)(( byteCount >> 8 ) & 0xFF );
        dstIndex += byteCount;

        /* Normally, without intervention everything would batch
         * with everything else. If the below clause evaluates true
         * the andx command will not be written and therefore the
         * response will not read a batched command and therefore
         * the 'received' member of the response object will not
         * be set to true indicating the send and sendTransaction
         * methods that the next part should be sent. This is a
         * very indirect and simple batching control mechanism.
         */

        if( andx == null || USE_BATCHING == false ||
                                batchLevel >= getBatchLimit( andx.command )) {
            andxCommand = (byte)0xFF;
            andx = null;

            dst[start + ANDX_COMMAND_OFFSET] = (byte)0xFF;
            dst[start + ANDX_RESERVED_OFFSET] = (byte)0x00;
//            dst[start + ANDX_OFFSET_OFFSET] = (byte)0x00;
//            dst[start + ANDX_OFFSET_OFFSET + 1] = (byte)0x00;
            dst[start + ANDX_OFFSET_OFFSET] = (byte)0xde;
            dst[start + ANDX_OFFSET_OFFSET + 1] = (byte)0xde;

            // andx not used; return
            return dstIndex - start;
        }

        /* The message provided to batch has a batchLimit that is
         * higher than the current batchLevel so we will now encode
         * that chained message. Before doing so we must increment
         * the batchLevel of the andx message in case it itself is an
         * andx message and needs to perform the same check as above.
         */

        andx.batchLevel = batchLevel + 1;


        dst[start + ANDX_COMMAND_OFFSET] = andxCommand;
        dst[start + ANDX_RESERVED_OFFSET] = (byte)0x00;
        andxOffset = dstIndex - headerStart;
        writeInt2( andxOffset, dst, start + ANDX_OFFSET_OFFSET );

        andx.useUnicode = useUnicode;
        if( andx instanceof AndXServerMessageBlock ) {

            /*
             * A word about communicating header info to andx smbs
             *
             * This is where we recursively invoke the provided andx smb
             * object to write it's parameter words and bytes to our outgoing
             * array. Incedentally when these andx smbs are created they are not
             * necessarily populated with header data because they're not writing
             * the header, only their body. But for whatever reason one might wish
             * to populate fields if the writeXxx operation needs this header data
             * for whatever reason. I copy over the uid here so it appears correct
             * in logging output. Logging of andx segments of messages inadvertantly
             * print header information because of the way toString always makes a
             * super.toString() call(see toString() at the end of all smbs classes).
             */

            andx.uid = uid;
            dstIndex += ((AndXServerMessageBlock)andx).writeAndXWireFormat( dst, dstIndex );
        } else {
            // the andx smb is not of type andx so lets just write it here and
            // were done.
            int andxStart = dstIndex;
            andx.wordCount = andx.writeParameterWordsWireFormat( dst, dstIndex );
            dstIndex += andx.wordCount + 1;
            andx.wordCount /= 2;
            dst[andxStart] = (byte)( andx.wordCount & 0xFF );
    
            andx.byteCount = andx.writeBytesWireFormat( dst, dstIndex + 2 );
            dst[dstIndex++] = (byte)( andx.byteCount & 0xFF );
            dst[dstIndex++] = (byte)(( andx.byteCount >> 8 ) & 0xFF );
            dstIndex += andx.byteCount;
        }

        return dstIndex - start;
    }
    int readAndXWireFormat( byte[] buffer, int bufferIndex ) {
        int start = bufferIndex;

        wordCount = buffer[bufferIndex++];

        if( wordCount != 0 ) {
            /*
             * these fields are common to all andx commands
             * so let's populate them here
             */

            andxCommand = buffer[bufferIndex];
            andxOffset = readInt2( buffer, bufferIndex + 2 );

            if( andxOffset == 0 ) { /* Snap server workaround */
                andxCommand = (byte)0xFF;
            }

            /*
             * no point in calling readParameterWordsWireFormat if there are no more
             * parameter words. besides, win98 doesn't return "OptionalSupport" field
             */
 
            if( wordCount > 2 ) {
                readParameterWordsWireFormat( buffer, bufferIndex + 4 );

                /* The SMB_COM_NT_CREATE_ANDX response wordCount is wrong. There's an
                 * extra 16 bytes for some "Offline Files (CSC or Client Side Caching)"
                 * junk. We need to bump up the wordCount here so that this method returns
                 * the correct number of bytes for signing purposes. Otherwise we get a
                 * signing verification failure.
                 */
                if (command == SMB_COM_NT_CREATE_ANDX && ((SmbComNTCreateAndXResponse)this).isExtended)
                    wordCount += 8;
            }

            bufferIndex = start + 1 + (wordCount * 2);
        }

        byteCount = readInt2( buffer, bufferIndex ); bufferIndex += 2;

        if (byteCount != 0) {
            int n;
            n = readBytesWireFormat( buffer, bufferIndex );
            bufferIndex += byteCount;
        }

        /*
         * if there is an andx and it itself is an andx then just recur by
         * calling this method for it. otherwise just read it's parameter words
         * and bytes as usual. Note how we can't just call andx.readWireFormat
         * because there's no header.
         */

        if( errorCode != 0 || andxCommand == (byte)0xFF ) {
            andxCommand = (byte)0xFF;
            andx = null;
        } else if( andx == null ) {
            andxCommand = (byte)0xFF;
            throw new RuntimeException( "no andx command supplied with response" );
        } else {

            /*
             * Set bufferIndex according to andxOffset
             */

            bufferIndex = headerStart + andxOffset;

            andx.headerStart = headerStart;
            andx.command = andxCommand;
            andx.errorCode = errorCode;
            andx.flags = flags;
            andx.flags2 = flags2;
            andx.tid = tid;
            andx.pid = pid;
            andx.uid = uid;
            andx.mid = mid;
            andx.useUnicode = useUnicode;

            if( andx instanceof AndXServerMessageBlock ) {
                bufferIndex += ((AndXServerMessageBlock)andx).readAndXWireFormat(
                                                buffer, bufferIndex );
            } else {

                /*
                 * Just a plain smb. Read it as normal.
                 */

                buffer[bufferIndex++] = (byte)( andx.wordCount & 0xFF );

                if( andx.wordCount != 0 ) {
                    /*
                     * no point in calling readParameterWordsWireFormat if there are no more
                     * parameter words. besides, win98 doesn't return "OptionalSupport" field
                     */

                    if( andx.wordCount > 2 ) {
                        bufferIndex += andx.readParameterWordsWireFormat( buffer, bufferIndex );
                    }
                }

                andx.byteCount = readInt2( buffer, bufferIndex );
                bufferIndex += 2;

                if( andx.byteCount != 0 ) {
                    andx.readBytesWireFormat( buffer, bufferIndex );
                    bufferIndex += andx.byteCount;
                }
            }
            andx.received = true;
        }

        return bufferIndex - start;
    }
    public String toString() {
        return new String( super.toString() +
            ",andxCommand=0x" + Hexdump.toHexString( andxCommand, 2 ) +
            ",andxOffset=" + andxOffset );
    }
}
