/* jcifs smb client library in Java
 * Copyright (C) 2003  "Michael B. Allen" <jcifs at samba dot org>
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

import jcifs.util.Encdec;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.UnknownHostException;

public class SmbRandomAccessFile implements DataOutput, DataInput {

    private static final int WRITE_OPTIONS = 0x0842;

    private SmbFile file;
    private long fp;
    private int openFlags, access = 0, readSize, writeSize, ch, options = 0;
    private byte[] tmp = new byte[8];
    private SmbComWriteAndXResponse write_andx_resp = null;

    public SmbRandomAccessFile( String url, String mode, int shareAccess )
            throws SmbException, MalformedURLException, UnknownHostException {
        this( new SmbFile( url, "", null, shareAccess ), mode );
    }
    public SmbRandomAccessFile( SmbFile file, String mode )
            throws SmbException, MalformedURLException, UnknownHostException {
        this.file = file;
        if( mode.equals( "r" )) {
            this.openFlags = SmbFile.O_CREAT | SmbFile.O_RDONLY;
        } else if( mode.equals( "rw" )) {
            this.openFlags = SmbFile.O_CREAT | SmbFile.O_RDWR | SmbFile.O_APPEND;
            write_andx_resp = new SmbComWriteAndXResponse();
            options = WRITE_OPTIONS;
            access = SmbConstants.FILE_READ_DATA | SmbConstants.FILE_WRITE_DATA;
        } else {
            throw new IllegalArgumentException( "Invalid mode" );
        }
        file.open( openFlags, access, SmbFile.ATTR_NORMAL, options );
        readSize = file.tree.session.transport.rcv_buf_size - 70;
        writeSize = file.tree.session.transport.snd_buf_size - 70;
        fp = 0L;
    }

    public int read() throws SmbException {
        if( read( tmp, 0, 1 ) == -1 ) {
            return -1;
        }
        return tmp[0] & 0xFF;
    }
    public int read( byte b[] ) throws SmbException {
        return read( b, 0, b.length );
    }
    public int read( byte b[], int off, int len ) throws SmbException {
        if( len <= 0 ) {
            return 0;
        }
        long start = fp;

        // ensure file is open
        if( file.isOpen() == false ) {
            file.open( openFlags, 0, SmbFile.ATTR_NORMAL, options );
        }

        int r, n;
        SmbComReadAndXResponse response = new SmbComReadAndXResponse( b, off );
        do {
            r = len > readSize ? readSize : len;
            file.send( new SmbComReadAndX( file.fid, fp, r, null ), response );
            if(( n = response.dataLength ) <= 0 ) {
                return (int)((fp - start) > 0L ? fp - start : -1);
            }
            fp += n;
            len -= n;
            response.off += n;
        } while( len > 0 && n == r );

        return (int)(fp - start);
    }
    public final void readFully( byte b[] ) throws SmbException {
        readFully( b, 0, b.length );
    }
    public final void readFully( byte b[], int off, int len ) throws SmbException {
        int n = 0, count;

        do {    
            count = this.read( b, off + n, len - n );
            if( count < 0 ) throw new SmbException( "EOF" );
            n += count;
            fp += count;
        } while( n < len );
    }
    public int skipBytes( int n ) throws SmbException {
        if (n > 0) {
            fp += n;
            return n;
        }
        return 0;
    }

    public void write( int b ) throws SmbException {
        tmp[0] = (byte)b;
        write( tmp, 0, 1 );
    }
    public void write( byte b[] ) throws SmbException {
        write( b, 0, b.length );
    }
    public void write( byte b[], int off, int len ) throws SmbException {
        if( len <= 0 ) {
            return;
        }

        // ensure file is open
        if( file.isOpen() == false ) {
            file.open( openFlags, 0, SmbFile.ATTR_NORMAL, options );
        }

        int w;
        do {
            w = len > writeSize ? writeSize : len;
            file.send( new SmbComWriteAndX( file.fid, fp, len - w, b, off, w, null ), write_andx_resp );
            fp += write_andx_resp.count;
            len -= write_andx_resp.count;
            off += write_andx_resp.count;
        } while( len > 0 );
    }
    public long getFilePointer() throws SmbException {
        return fp;
    }
    public void seek( long pos ) throws SmbException {
        fp = pos;
    }
    public long length() throws SmbException {
        return file.length();
    }
    public void setLength( long newLength ) throws SmbException {
        // ensure file is open
        if( file.isOpen() == false ) {
            file.open( openFlags, 0, SmbFile.ATTR_NORMAL, options );
        }
        SmbComWriteResponse rsp = new SmbComWriteResponse();
        file.send( new SmbComWrite( file.fid, (int)(newLength & 0xFFFFFFFFL), 0, tmp, 0, 0 ), rsp );
    }
    public void close() throws SmbException {
        file.close();
    }

    public final boolean readBoolean() throws SmbException {
        if((read( tmp, 0, 1 )) < 0 ) {
            throw new SmbException( "EOF" );
        }
        return tmp[0] != (byte)0x00;
    }
    public final byte readByte() throws SmbException {
        if((read( tmp, 0, 1 )) < 0 ) {
            throw new SmbException( "EOF" );
        }
        return tmp[0];
    }
    public final int readUnsignedByte() throws SmbException {
        if((read( tmp, 0, 1 )) < 0 ) {
            throw new SmbException( "EOF" );
        }
        return tmp[0] & 0xFF;
    }
    public final short readShort() throws SmbException {
        if((read( tmp, 0, 2 )) < 0 ) {
            throw new SmbException( "EOF" );
        }
        return Encdec.dec_uint16be( tmp, 0 );
    }
    public final int readUnsignedShort() throws SmbException {
        if((read( tmp, 0, 2 )) < 0 ) {
            throw new SmbException( "EOF" );
        }
        return Encdec.dec_uint16be( tmp, 0 ) & 0xFFFF;
    }
    public final char readChar() throws SmbException {
        if((read( tmp, 0, 2 )) < 0 ) {
            throw new SmbException( "EOF" );
        }
        return (char)Encdec.dec_uint16be( tmp, 0 );
    }
    public final int readInt() throws SmbException {
        if((read( tmp, 0, 4 )) < 0 ) {
            throw new SmbException( "EOF" );
        }
        return Encdec.dec_uint32be( tmp, 0 );
    }
    public final long readLong() throws SmbException {
        if((read( tmp, 0, 8 )) < 0 ) {
            throw new SmbException( "EOF" );
        }
        return Encdec.dec_uint64be( tmp, 0 );
    }
    public final float readFloat() throws SmbException {
        if((read( tmp, 0, 4 )) < 0 ) {
            throw new SmbException( "EOF" );
        }
        return Encdec.dec_floatbe( tmp, 0 );
    }
    public final double readDouble() throws SmbException {
        if((read( tmp, 0, 8 )) < 0 ) {
            throw new SmbException( "EOF" );
        }
        return Encdec.dec_doublebe( tmp, 0 );
    }
    public final String readLine() throws SmbException {
        StringBuffer input = new StringBuffer();
        int c = -1;
        boolean eol = false;

        while (!eol) {
            switch( c = read() ) {
                case -1:
                case '\n':
                    eol = true;
                    break;
                case '\r':
                    eol = true;
                    long cur = fp;
                    if( read() != '\n' ) {
                        fp = cur;
                    }
                    break;
                default:
                    input.append( (char)c );
                    break;
            }
        }

        if ((c == -1) && (input.length() == 0)) {
            return null;
        }

        return input.toString();
    }

    public final String readUTF() throws SmbException {
        int size = readUnsignedShort();
        byte[] b = new byte[size];
        read( b, 0, size );
        try {
            return Encdec.dec_utf8( b, 0, size );
        } catch( IOException ioe ) {
            throw new SmbException( "", ioe );
        }
    }
    public final void writeBoolean( boolean v ) throws SmbException {
        tmp[0] = (byte)(v ? 1 : 0);
        write( tmp, 0, 1 );
    }
    public final void writeByte( int v ) throws SmbException {
        tmp[0] = (byte)v;
        write( tmp, 0, 1 );
    }
    public final void writeShort( int v ) throws SmbException {
        Encdec.enc_uint16be( (short)v, tmp, 0 );
        write( tmp, 0, 2 );
    }
    public final void writeChar( int v ) throws SmbException {
        Encdec.enc_uint16be( (short)v, tmp, 0 );
        write( tmp, 0, 2 );
    }
    public final void writeInt( int v ) throws SmbException {
        Encdec.enc_uint32be( v, tmp, 0 );
        write( tmp, 0, 4 );
    }
    public final void writeLong( long v ) throws SmbException {
        Encdec.enc_uint64be( v, tmp, 0 );
        write( tmp, 0, 8 );
    }
    public final void writeFloat( float v ) throws SmbException {
        Encdec.enc_floatbe( v, tmp, 0 );
        write( tmp, 0, 4 );
    }
    public final void writeDouble( double v ) throws SmbException {
        Encdec.enc_doublebe( v, tmp, 0 );
        write( tmp, 0, 8 );
    }
    public final void writeBytes( String s ) throws SmbException {
        byte[] b = s.getBytes();
        write( b, 0, b.length );
    }
    public final void writeChars( String s ) throws SmbException {
        int clen = s.length();
        int blen = 2 * clen;
        byte[] b = new byte[blen];
        char[] c = new char[clen];
        s.getChars( 0, clen, c, 0 );
        for( int i = 0, j = 0; i < clen; i++ ) {
            b[j++] = (byte)(c[i] >>> 8);
            b[j++] = (byte)(c[i] >>> 0);
        }
        write( b, 0, blen );
    }
    public final void writeUTF( String str ) throws SmbException {
        int len = str.length();
        int ch, size = 0;
        byte[] dst;

        for( int i = 0; i < len; i++ ) {
            ch = str.charAt( i );
            size += ch > 0x07F ? (ch > 0x7FF ? 3 : 2) : 1;
        }
        dst = new byte[size];
        writeShort( size );
        try {
            Encdec.enc_utf8( str, dst, 0, size );
        } catch( IOException ioe ) {
            throw new SmbException( "", ioe );
        }
        write( dst, 0, size );
    }
}

