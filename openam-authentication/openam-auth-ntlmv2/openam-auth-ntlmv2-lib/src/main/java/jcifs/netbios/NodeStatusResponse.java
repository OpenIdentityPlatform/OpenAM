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

import java.io.UnsupportedEncodingException;

class NodeStatusResponse extends NameServicePacket {

    private NbtAddress queryAddress;

    private int numberOfNames;
    private byte[] macAddress;
    private byte[] stats;

    NbtAddress[] addressArray;

    /* It is a little awkward but prudent to pass the quering address
     * so that it may be included in the list of results. IOW we do
     * not want to create a new NbtAddress object for this particular
     * address from which the query is constructed, we want to populate
     * the data of the existing address that should be one of several
     * returned by the node status.
     */

    NodeStatusResponse( NbtAddress queryAddress ) {
        this.queryAddress = queryAddress;
        recordName = new Name();
        macAddress = new byte[6];
    }

    int writeBodyWireFormat( byte[] dst, int dstIndex ) {
        return 0;
    }
    int readBodyWireFormat( byte[] src, int srcIndex ) {
        return readResourceRecordWireFormat( src, srcIndex );
    }
    int writeRDataWireFormat( byte[] dst, int dstIndex ) {
        return 0;
    }
    int readRDataWireFormat( byte[] src, int srcIndex ) {
        int start = srcIndex;
        numberOfNames = src[srcIndex] & 0xFF;
        int namesLength = numberOfNames * 18;
        int statsLength = rDataLength - namesLength - 1;
        numberOfNames = src[srcIndex++] & 0xFF;
        // gotta read the mac first so we can populate addressArray with it
        System.arraycopy( src, srcIndex + namesLength, macAddress, 0, 6 );
        srcIndex += readNodeNameArray( src, srcIndex );
        stats = new byte[statsLength];
        System.arraycopy( src, srcIndex, stats, 0, statsLength );
        srcIndex += statsLength;
        return srcIndex - start;
    }
    private int readNodeNameArray( byte[] src, int srcIndex ) {
        int start = srcIndex;

        addressArray = new NbtAddress[numberOfNames];

        String n;
        int hexCode;
        String scope = queryAddress.hostName.scope;
        boolean groupName;
        int ownerNodeType;
        boolean isBeingDeleted;
        boolean isInConflict;
        boolean isActive;
        boolean isPermanent;
        int j;
        boolean addrFound = false;

        try {
            for( int i = 0; i < numberOfNames; srcIndex += 18, i++ ) {
                for( j = srcIndex + 14; src[j] == 0x20; j-- )
                    ;
                n = new String( src, srcIndex, j - srcIndex + 1, Name.OEM_ENCODING );
                hexCode          =    src[srcIndex + 15] & 0xFF;
                groupName        = (( src[srcIndex + 16] & 0x80 ) == 0x80 ) ? true : false;
                ownerNodeType    =  ( src[srcIndex + 16] & 0x60 ) >> 5;
                isBeingDeleted   = (( src[srcIndex + 16] & 0x10 ) == 0x10 ) ? true : false;
                isInConflict     = (( src[srcIndex + 16] & 0x08 ) == 0x08 ) ? true : false;
                isActive         = (( src[srcIndex + 16] & 0x04 ) == 0x04 ) ? true : false;
                isPermanent      = (( src[srcIndex + 16] & 0x02 ) == 0x02 ) ? true : false;
    
    /* The NbtAddress object used to query this node will be in the list
     * returned by the Node Status. A new NbtAddress object should not be
     * created for it because the original is potentially being actively
     * referenced by other objects. We must populate the existing object's
     * data explicitly (and carefully).
     */
                if( !addrFound && queryAddress.hostName.hexCode == hexCode &&
                        ( queryAddress.hostName == NbtAddress.UNKNOWN_NAME ||
                        queryAddress.hostName.name.equals( n ))) {
    
                    if( queryAddress.hostName == NbtAddress.UNKNOWN_NAME ) {
                        queryAddress.hostName = new Name( n, hexCode, scope );
                    }
                    queryAddress.groupName = groupName;
                    queryAddress.nodeType = ownerNodeType;
                    queryAddress.isBeingDeleted = isBeingDeleted;
                    queryAddress.isInConflict = isInConflict;
                    queryAddress.isActive = isActive;
                    queryAddress.isPermanent = isPermanent;
                    queryAddress.macAddress = macAddress;
                    queryAddress.isDataFromNodeStatus = true;
                    addrFound = true;
                    addressArray[i] = queryAddress;
                } else {
                    addressArray[i] = new NbtAddress( new Name( n, hexCode, scope ),
                                            queryAddress.address,
                                            groupName,
                                            ownerNodeType,
                                            isBeingDeleted,
                                            isInConflict,
                                            isActive,
                                            isPermanent,
                                            macAddress );
                }
            }
        } catch( UnsupportedEncodingException uee ) {
        }
        return srcIndex - start;
    }
    public String toString() {
        return new String( "NodeStatusResponse[" +
            super.toString() + "]" );
    }
}

