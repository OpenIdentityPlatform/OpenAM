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

package jcifs.netbios;

import jcifs.Config;
import jcifs.util.LogStream;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;

/**
Do not use this class. Writing to the OutputStream of this type of socket
requires leaving a 4 byte prefix for the NBT header. IOW you must call
write( buf, 4, len ). Calling write( buf, 0, len ) will generate an error.
 */

public class NbtSocket extends Socket {

    private static final int SSN_SRVC_PORT = 139;
    private static final int BUFFER_SIZE = 512;
    private static final int DEFAULT_SO_TIMEOUT = 5000;

    private static LogStream log = LogStream.getInstance();

    private NbtAddress address;
    private Name calledName;
    private int soTimeout;

    public NbtSocket() {
        super();
    }
    public NbtSocket( NbtAddress address, int port ) throws IOException {
        this( address, port, null, 0 );
    }
    public NbtSocket( NbtAddress address, int port,
                                InetAddress localAddr, int localPort ) throws IOException {
        this( address, null, port, localAddr, localPort );
    }
    public NbtSocket( NbtAddress address, String calledName, int port,
                                InetAddress localAddr, int localPort ) throws IOException {
        super( address.getInetAddress(), ( port == 0 ? SSN_SRVC_PORT : port ),
                                localAddr, localPort );
        this.address = address;
        if( calledName == null ) {
            this.calledName = address.hostName;
        } else {
            this.calledName = new Name( calledName, 0x20, null );
        }
        soTimeout = Config.getInt( "jcifs.netbios.soTimeout", DEFAULT_SO_TIMEOUT );
        connect();
    }

    public NbtAddress getNbtAddress() {
        return address;
    }
    public InputStream getInputStream() throws IOException {
        return new SocketInputStream( super.getInputStream() );
    }
    public OutputStream getOutputStream() throws IOException {
        return new SocketOutputStream( super.getOutputStream() );
    }
    public int getPort() {
        return super.getPort();
    }
    public InetAddress getLocalAddress() {
        return super.getLocalAddress();
    }
    public int getLocalPort() {
        return super.getLocalPort();
    }
    public String toString() {
        return "NbtSocket[addr=" + address +
                ",port=" + super.getPort() +
                ",localport=" + super.getLocalPort() + "]";
    }
    private void connect() throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        int type;
        InputStream in;

        try {
            in = super.getInputStream();
            OutputStream out = super.getOutputStream();

            SessionServicePacket ssp0 = new SessionRequestPacket( calledName, NbtAddress.localhost.hostName );
            out.write( buffer, 0, ssp0.writeWireFormat( buffer, 0 ));

            setSoTimeout( soTimeout );
            type = ssp0.readPacketType( in, buffer, 0 );
        } catch( IOException ioe ) {
            close();
            throw ioe;
        }

        switch( type ) {
            case SessionServicePacket.POSITIVE_SESSION_RESPONSE:
                if( log.level > 2 )
                    log.println( "session established ok with " + address );
                return;
            case SessionServicePacket.NEGATIVE_SESSION_RESPONSE:
                int errorCode = (int)( in.read() & 0xFF );
                close();
                throw new NbtException( NbtException.ERR_SSN_SRVC, errorCode );
            case -1:
                throw new NbtException( NbtException.ERR_SSN_SRVC, NbtException.CONNECTION_REFUSED );
            default:
                close();
                throw new NbtException( NbtException.ERR_SSN_SRVC, 0 );
        }
    }
    public void close() throws IOException {
        if( log.level > 3 )
            log.println( "close: " + this );
        super.close();
    }
}
