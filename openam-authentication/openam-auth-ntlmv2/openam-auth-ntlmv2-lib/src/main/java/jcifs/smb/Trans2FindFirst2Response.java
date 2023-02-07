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

import java.io.UnsupportedEncodingException;
import java.util.Date;

class Trans2FindFirst2Response extends SmbComTransactionResponse {

    // information levels

    static final int SMB_INFO_STANDARD                 = 1;
    static final int SMB_INFO_QUERY_EA_SIZE            = 2;
    static final int SMB_INFO_QUERY_EAS_FROM_LIST      = 3;
    static final int SMB_FIND_FILE_DIRECTORY_INFO      = 0x101;
    static final int SMB_FIND_FILE_FULL_DIRECTORY_INFO = 0x102;
    static final int SMB_FILE_NAMES_INFO               = 0x103;
    static final int SMB_FILE_BOTH_DIRECTORY_INFO      = 0x104;

    class SmbFindFileBothDirectoryInfo implements FileEntry {
        int nextEntryOffset;
        int fileIndex;
        long creationTime;
        long lastAccessTime;
        long lastWriteTime;
        long changeTime;
        long endOfFile;
        long allocationSize;
        int extFileAttributes;
        int fileNameLength;
        int eaSize;
        int shortNameLength;
        String shortName;
        String filename;

        public String getName() {
            return filename;
        }
        public int getType() {
            return SmbFile.TYPE_FILESYSTEM;
        }
        public int getAttributes() {
            return extFileAttributes;
        }
        public long createTime() {
            return creationTime;
        }
        public long lastModified() {
            return lastWriteTime;
        }
        public long length() {
            return endOfFile;
        }

        public String toString() {
            return new String( "SmbFindFileBothDirectoryInfo[" +
                "nextEntryOffset=" + nextEntryOffset +
                ",fileIndex=" + fileIndex +
                ",creationTime=" + new Date( creationTime ) +
                ",lastAccessTime=" + new Date( lastAccessTime ) +
                ",lastWriteTime=" + new Date( lastWriteTime ) +
                ",changeTime=" + new Date( changeTime ) +
                ",endOfFile=" + endOfFile +
                ",allocationSize=" + allocationSize +
                ",extFileAttributes=" + extFileAttributes +
                ",fileNameLength=" + fileNameLength +
                ",eaSize=" + eaSize +
                ",shortNameLength=" + shortNameLength +
                ",shortName=" + shortName +
                ",filename=" + filename + "]" );
        }
    }

    int sid;
    boolean isEndOfSearch;
    int eaErrorOffset;
    int lastNameOffset, lastNameBufferIndex;
    String lastName;
    int resumeKey;


    Trans2FindFirst2Response() {
        command = SMB_COM_TRANSACTION2;
        subCommand = SmbComTransaction.TRANS2_FIND_FIRST2;
    }

    String readString( byte[] src, int srcIndex, int len ) {
        String str = null;
        try {
            if( useUnicode ) {
                // should Unicode alignment be corrected for here?
                str = new String( src, srcIndex, len, UNI_ENCODING );
            } else {
    
                /* On NT without Unicode the fileNameLength
                 * includes the '\0' whereas on win98 it doesn't. I
                 * guess most clients only support non-unicode so
                 * they don't run into this.
                 */
    
    /* UPDATE: Maybe not! Could this be a Unicode alignment issue. I hope
     * so. We cannot just comment out this method and use readString of
     * ServerMessageBlock.java because the arguments are different, however
     * one might be able to reduce this.
     */
    
                if( len > 0 && src[srcIndex + len - 1] == '\0' ) {
                    len--;
                }
                str = new String( src, srcIndex, len, ServerMessageBlock.OEM_ENCODING );
            }
        } catch( UnsupportedEncodingException uee ) {
            if( log.level > 1 )
                uee.printStackTrace( log );
        }
        return str;
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
        int start = bufferIndex;

        if( subCommand == SmbComTransaction.TRANS2_FIND_FIRST2 ) {
            sid = readInt2( buffer, bufferIndex );
            bufferIndex += 2;
        }
        numEntries = readInt2( buffer, bufferIndex );
        bufferIndex += 2;
        isEndOfSearch = ( buffer[bufferIndex] & 0x01 ) == 0x01 ? true : false;
        bufferIndex += 2;
        eaErrorOffset = readInt2( buffer, bufferIndex );
        bufferIndex += 2;
        lastNameOffset = readInt2( buffer, bufferIndex );
        bufferIndex += 2;

        return bufferIndex - start;
    }
    int readDataWireFormat( byte[] buffer, int bufferIndex, int len ) {
        int start = bufferIndex;
        SmbFindFileBothDirectoryInfo e;

        lastNameBufferIndex = bufferIndex + lastNameOffset;

        results = new SmbFindFileBothDirectoryInfo[numEntries];
        for( int i = 0; i < numEntries; i++ ) {
            results[i] = e = new SmbFindFileBothDirectoryInfo();

            e.nextEntryOffset = readInt4( buffer, bufferIndex );
            e.fileIndex = readInt4( buffer, bufferIndex + 4 );
            e.creationTime = readTime( buffer, bufferIndex + 8 );
    //      e.lastAccessTime = readTime( buffer, bufferIndex + 16 );
            e.lastWriteTime = readTime( buffer, bufferIndex + 24 );
    //      e.changeTime = readTime( buffer, bufferIndex + 32 );
            e.endOfFile = readInt8( buffer, bufferIndex + 40 );
    //      e.allocationSize = readInt8( buffer, bufferIndex + 48 );
            e.extFileAttributes = readInt4( buffer, bufferIndex + 56 );
            e.fileNameLength = readInt4( buffer, bufferIndex + 60 );
    //      e.eaSize = readInt4( buffer, bufferIndex + 64 );
    //      e.shortNameLength = buffer[bufferIndex + 68] & 0xFF;

            /* With NT, the shortName is in Unicode regardless of what is negotiated.
             */

    //      e.shortName = readString( buffer, bufferIndex + 70, e.shortNameLength );
            e.filename = readString( buffer, bufferIndex + 94, e.fileNameLength );

            /* lastNameOffset ends up pointing to either to
             * the exact location of the filename(e.g. Win98)
             * or to the start of the entry containing the
             * filename(e.g. NT). Ahhrg! In either case the
             * lastNameOffset falls between the start of the
             * entry and the next entry.
             */

            if( lastNameBufferIndex >= bufferIndex && ( e.nextEntryOffset == 0 ||
                        lastNameBufferIndex < ( bufferIndex + e.nextEntryOffset ))) {
                lastName = e.filename;
                resumeKey = e.fileIndex;
            }

            bufferIndex += e.nextEntryOffset;
        }

        /* last nextEntryOffset for NT 4(but not 98) is 0 so we must
         * use dataCount or our accounting will report an error for NT :~(
         */

        //return bufferIndex - start;

        return dataCount;
    }
    public String toString() {
        String c;
        if( subCommand == SmbComTransaction.TRANS2_FIND_FIRST2 ) {
            c = "Trans2FindFirst2Response[";
        } else {
            c = "Trans2FindNext2Response[";
        }
        return new String( c + super.toString() +
            ",sid=" + sid +
            ",searchCount=" + numEntries +
            ",isEndOfSearch=" + isEndOfSearch +
            ",eaErrorOffset=" + eaErrorOffset +
            ",lastNameOffset=" + lastNameOffset +
            ",lastName=" + lastName + "]" );
    }
}
