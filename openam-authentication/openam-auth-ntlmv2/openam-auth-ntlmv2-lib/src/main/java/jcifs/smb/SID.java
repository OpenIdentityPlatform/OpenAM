/* jcifs smb client library in Java
 * Copyright (C) 2006  "Michael B. Allen" <jcifs at samba dot org>
 *                     "Eric Glass" <jcifs at samba dot org>
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

import jcifs.dcerpc.DcerpcHandle;
import jcifs.dcerpc.UnicodeString;
import jcifs.dcerpc.msrpc.*;
import jcifs.dcerpc.rpc;
import jcifs.util.Hexdump;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * A Windows SID is a numeric identifier used to represent Windows
 * accounts. SIDs are commonly represented using a textual format such as
 * <tt>S-1-5-21-1496946806-2192648263-3843101252-1029</tt> but they may
 * also be resolved to yield the name of the associated Windows account
 * such as <tt>Administrators</tt> or <tt>MYDOM\alice</tt>.
 * <p>
 * Consider the following output of <tt>examples/SidLookup.java</tt>:
 * <pre>
 *        toString: S-1-5-21-4133388617-793952518-2001621813-512
 * toDisplayString: WNET\Domain Admins
 *         getType: 2
 *     getTypeText: Domain group
 *   getDomainName: WNET
 *  getAccountName: Domain Admins
 * </pre>
 */

public class SID extends rpc.sid_t {

    public static final int SID_TYPE_USE_NONE = lsarpc.SID_NAME_USE_NONE;
    public static final int SID_TYPE_USER    = lsarpc.SID_NAME_USER;
    public static final int SID_TYPE_DOM_GRP = lsarpc.SID_NAME_DOM_GRP;
    public static final int SID_TYPE_DOMAIN  = lsarpc.SID_NAME_DOMAIN;
    public static final int SID_TYPE_ALIAS   = lsarpc.SID_NAME_ALIAS;
    public static final int SID_TYPE_WKN_GRP = lsarpc.SID_NAME_WKN_GRP;
    public static final int SID_TYPE_DELETED = lsarpc.SID_NAME_DELETED;
    public static final int SID_TYPE_INVALID = lsarpc.SID_NAME_INVALID;
    public static final int SID_TYPE_UNKNOWN = lsarpc.SID_NAME_UNKNOWN;

    static final String[] SID_TYPE_NAMES = {
        "0",
        "User",
        "Domain group",
        "Domain",
        "Local group",
        "Builtin group",
        "Deleted",
        "Invalid",
        "Unknown"
    };

    public static final int SID_FLAG_RESOLVE_SIDS = 0x0001;

    public static SID EVERYONE = null;
    public static SID CREATOR_OWNER = null;
    public static SID SYSTEM = null;

    static {
        try {
            EVERYONE = new SID("S-1-1-0");
            CREATOR_OWNER = new SID("S-1-3-0");
            SYSTEM = new SID("S-1-5-18");
        } catch (SmbException se) {
        }
    }

    static Map sid_cache = new HashMap();

    static void resolveSids(DcerpcHandle handle,
                LsaPolicyHandle policyHandle,
                SID[] sids) throws IOException {
        MsrpcLookupSids rpc = new MsrpcLookupSids(policyHandle, sids);
        handle.sendrecv(rpc);
        switch (rpc.retval) {
            case 0:
            case NtStatus.NT_STATUS_NONE_MAPPED:
            case 0x00000107: // NT_STATUS_SOME_NOT_MAPPED
                break;
            default:
                throw new SmbException(rpc.retval, false);
        }

        for (int si = 0; si < sids.length; si++) {
            sids[si].type = rpc.names.names[si].sid_type;
            sids[si].domainName = null;

            switch (sids[si].type) {
                case SID_TYPE_USER:
                case SID_TYPE_DOM_GRP:
                case SID_TYPE_DOMAIN:
                case SID_TYPE_ALIAS:
                case SID_TYPE_WKN_GRP:
                    int sid_index = rpc.names.names[si].sid_index;
                    rpc.unicode_string ustr = rpc.domains.domains[sid_index].name;
                    sids[si].domainName = (new UnicodeString(ustr, false)).toString();
                    break;
            }

            sids[si].acctName = (new UnicodeString(rpc.names.names[si].name, false)).toString();
            sids[si].origin_server = null;
            sids[si].origin_auth = null;
        }
    }
    static void resolveSids0(String authorityServerName,
                NtlmPasswordAuthentication auth,
                SID[] sids) throws IOException {
        DcerpcHandle handle = null;
        LsaPolicyHandle policyHandle = null;

synchronized (sid_cache) {
        try {
            handle = DcerpcHandle.getHandle("ncacn_np:" + authorityServerName +
                    "[\\PIPE\\lsarpc]", auth);
            String server = authorityServerName;
            int dot = server.indexOf('.');
            if (dot > 0 && Character.isDigit(server.charAt(0)) == false)
                server = server.substring(0, dot);
            policyHandle = new LsaPolicyHandle(handle, "\\\\" + server, 0x00000800);
            SID.resolveSids(handle, policyHandle, sids);
        } finally {
            if (handle != null) {
                if (policyHandle != null) {
                    policyHandle.close();
                }
                handle.close();
            }
        }
}
    }

    static public void resolveSids(String authorityServerName,
                NtlmPasswordAuthentication auth,
                SID[] sids,
                int offset,
                int length) throws IOException {
        ArrayList list = new ArrayList(sids.length);
        int si;

synchronized (sid_cache) {
        for (si = 0; si < length; si++) {
            SID sid = (SID)sid_cache.get(sids[offset + si]);
            if (sid != null) {
                sids[offset + si].type = sid.type;
                sids[offset + si].domainName = sid.domainName;
                sids[offset + si].acctName = sid.acctName;
            } else {
                list.add(sids[offset + si]);
            }
        }

        if (list.size() > 0) {
            sids = (SID[])list.toArray(new SID[0]);
            SID.resolveSids0(authorityServerName, auth, sids);
            for (si = 0; si < sids.length; si++) {
                sid_cache.put(sids[si], sids[si]);
            }
        }
}
    }
    /**
     * Resolve an array of SIDs using a cache and at most one MSRPC request.
     * <p>
     * This method will attempt
     * to resolve SIDs using a cache and cache the results of any SIDs that
     * required resolving with the authority. SID cache entries are currently not
     * expired because under normal circumstances SID information never changes.
     *
     * @param authorityServerName The hostname of the server that should be queried. For maximum efficiency this should be the hostname of a domain controller however a member server will work as well and a domain controller may not return names for SIDs corresponding to local accounts for which the domain controller is not an authority.
     * @param auth The credentials that should be used to communicate with the named server. As usual, <tt>null</tt> indicates that default credentials should be used.
     * @param sids The SIDs that should be resolved. After this function is called, the names associated with the SIDs may be queried with the <tt>toDisplayString</tt>, <tt>getDomainName</tt>, and <tt>getAccountName</tt> methods.
     */
    static public void resolveSids(String authorityServerName,
                NtlmPasswordAuthentication auth,
                SID[] sids) throws IOException {
        ArrayList list = new ArrayList(sids.length);
        int si;

synchronized (sid_cache) {
        for (si = 0; si < sids.length; si++) {
            SID sid = (SID)sid_cache.get(sids[si]);
            if (sid != null) {
                sids[si].type = sid.type;
                sids[si].domainName = sid.domainName;
                sids[si].acctName = sid.acctName;
            } else {
                list.add(sids[si]);
            }
        }

        if (list.size() > 0) {
            sids = (SID[])list.toArray(new SID[0]);
            SID.resolveSids0(authorityServerName, auth, sids);
            for (si = 0; si < sids.length; si++) {
                sid_cache.put(sids[si], sids[si]);
            }
        }
}
    }
    public static SID getServerSid(String server,
                    NtlmPasswordAuthentication auth) throws IOException {
        DcerpcHandle handle = null;
        LsaPolicyHandle policyHandle = null;
        lsarpc.LsarDomainInfo info = new lsarpc.LsarDomainInfo();
        MsrpcQueryInformationPolicy rpc;

synchronized (sid_cache) {
        try {
            handle = DcerpcHandle.getHandle("ncacn_np:" + server +
                    "[\\PIPE\\lsarpc]", auth);
            // NetApp doesn't like the 'generic' access mask values
            policyHandle = new LsaPolicyHandle(handle, null, 0x00000001);
            rpc = new MsrpcQueryInformationPolicy(policyHandle,
                        (short)lsarpc.POLICY_INFO_ACCOUNT_DOMAIN,
                        info);
            handle.sendrecv(rpc);
            if (rpc.retval != 0)
                throw new SmbException(rpc.retval, false);

            return new SID(info.sid,
                        SID.SID_TYPE_DOMAIN,
                        (new UnicodeString(info.name, false)).toString(),
                        null,
                        false);
        } finally {
            if (handle != null) {
                if (policyHandle != null) {
                    policyHandle.close();
                }
                handle.close();
            }
        }
}
    }
    public static byte[] toByteArray(rpc.sid_t sid) {
        byte[] dst = new byte[1 + 1 + 6 + sid.sub_authority_count * 4];
        int di = 0;
        dst[di++] = sid.revision;
        dst[di++] = sid.sub_authority_count;
        System.arraycopy(sid.identifier_authority, 0, dst, di, 6);
        di += 6;
        for (int ii = 0; ii < sid.sub_authority_count; ii++) {
            jcifs.util.Encdec.enc_uint32le(sid.sub_authority[ii], dst, di);
            di += 4;
        }
        return dst;
    }

    int type;
    String domainName = null;
    String acctName = null;
    String origin_server = null;
    NtlmPasswordAuthentication origin_auth = null;

    /*
     * Construct a SID from it's binary representation.
     */
    public SID(byte[] src, int si) {
        revision = src[si++];
        sub_authority_count = src[si++];
        identifier_authority = new byte[6];
        System.arraycopy(src, si, identifier_authority, 0, 6);
        si += 6;
        if (sub_authority_count > 100)
            throw new RuntimeException( "Invalid SID sub_authority_count" );
        sub_authority = new int[sub_authority_count];
        for (int i = 0; i < sub_authority_count; i++) {
            sub_authority[i] = ServerMessageBlock.readInt4( src, si );
            si += 4;
        }
    }
    /**
     * Construct a SID from it's textual representation such as
     * <tt>S-1-5-21-1496946806-2192648263-3843101252-1029</tt>.
     */
    public SID(String textual) throws SmbException {
        StringTokenizer st = new StringTokenizer(textual, "-");
        if (st.countTokens() < 3 || !st.nextToken().equals("S"))
            // need S-N-M
            throw new SmbException("Bad textual SID format: " + textual);

        this.revision = Byte.parseByte(st.nextToken());
        String tmp = st.nextToken();
        long id = 0;
        if (tmp.startsWith("0x"))
            id = Long.parseLong(tmp.substring(2), 16);
        else
            id = Long.parseLong(tmp);

        this.identifier_authority = new byte[6];
        for (int i = 5; id > 0;  i--) {
            this.identifier_authority[i] = (byte) (id % 256);
            id >>= 8;
        }

        this.sub_authority_count = (byte) st.countTokens();
        if (this.sub_authority_count > 0) {
            this.sub_authority = new int[this.sub_authority_count];
            for (int i = 0; i < this.sub_authority_count; i++)
                this.sub_authority[i] = (int)(Long.parseLong(st.nextToken()) & 0xFFFFFFFFL);
        }
    }

    /**
     * Construct a SID from a domain SID and an RID
     * (relative identifier). For example, a domain SID
     * <tt>S-1-5-21-1496946806-2192648263-3843101252</tt> and RID <tt>1029</tt> would
     * yield the SID <tt>S-1-5-21-1496946806-2192648263-3843101252-1029</tt>.
     */
    public SID(SID domsid, int rid) {
        this.revision = domsid.revision;
        this.identifier_authority = domsid.identifier_authority;
        this.sub_authority_count = (byte)(domsid.sub_authority_count + 1);
        this.sub_authority = new int[this.sub_authority_count];
        int i;
        for (i = 0; i < domsid.sub_authority_count; i++) {
            this.sub_authority[i] = domsid.sub_authority[i];
        }
        this.sub_authority[i] = rid;
    }
    public SID(rpc.sid_t sid,
                    int type,
                    String domainName,
                    String acctName,
                    boolean decrementAuthority) {
        this.revision = sid.revision;
        this.sub_authority_count = sid.sub_authority_count;
        this.identifier_authority = sid.identifier_authority;
        this.sub_authority = sid.sub_authority;
        this.type = type;
        this.domainName = domainName;
        this.acctName = acctName;

        if (decrementAuthority) {
            this.sub_authority_count--;
            this.sub_authority = new int[sub_authority_count];
            for (int i = 0; i < this.sub_authority_count; i++) {
                this.sub_authority[i] = sid.sub_authority[i];
            }
        }
    }

    public SID getDomainSid() {
        return new SID(this,
                    SID_TYPE_DOMAIN,
                    this.domainName,
                    null,
                    getType() != SID_TYPE_DOMAIN);
    }
    public int getRid() {
        if (getType() == SID_TYPE_DOMAIN)
            throw new IllegalArgumentException("This SID is a domain sid");
        return sub_authority[sub_authority_count - 1];
    }

    /**
     * Returns the type of this SID indicating the state or type of account.
     * <p>
     * SID types are described in the following table.
     * <tt>
     * <table>
     * <tr><th>Type</th><th>Name</th></tr>
     * <tr><td>SID_TYPE_USE_NONE</td><td>0</td></tr>
     * <tr><td>SID_TYPE_USER</td><td>User</td></tr>
     * <tr><td>SID_TYPE_DOM_GRP</td><td>Domain group</td></tr>
     * <tr><td>SID_TYPE_DOMAIN</td><td>Domain</td></tr>
     * <tr><td>SID_TYPE_ALIAS</td><td>Local group</td></tr>
     * <tr><td>SID_TYPE_WKN_GRP</td><td>Builtin group</td></tr>
     * <tr><td>SID_TYPE_DELETED</td><td>Deleted</td></tr>
     * <tr><td>SID_TYPE_INVALID</td><td>Invalid</td></tr>
     * <tr><td>SID_TYPE_UNKNOWN</td><td>Unknown</td></tr>
     * </table>
     * </tt>
     */
    public int getType() {
        if (origin_server != null)
            resolveWeak();
        return type;
    }

    /**
     * Return text represeting the SID type suitable for display to
     * users. Text includes 'User', 'Domain group', 'Local group', etc.
     */
    public String getTypeText() {
        if (origin_server != null)
            resolveWeak();
        return SID_TYPE_NAMES[type];
    }

    /**
     * Return the domain name of this SID unless it could not be
     * resolved in which case the numeric representation is returned.
     */
    public String getDomainName() {
        if (origin_server != null)
            resolveWeak();
        if (type == SID_TYPE_UNKNOWN) {
            String full = toString();
            return full.substring(0, full.length() - getAccountName().length() - 1);
        }
        return domainName;
    }

    /**
     * Return the sAMAccountName of this SID unless it could not
     * be resolved in which case the numeric RID is returned. If this
     * SID is a domain SID, this method will return an empty String.
     */
    public String getAccountName() {
        if (origin_server != null)
            resolveWeak();
        if (type == SID_TYPE_UNKNOWN)
            return "" + sub_authority[sub_authority_count - 1];
        if (type == SID_TYPE_DOMAIN)
            return "";
        return acctName;
    }

    public int hashCode() {
        int hcode = identifier_authority[5];
        for (int i = 0; i < sub_authority_count; i++) {
            hcode += 65599 * sub_authority[i];
        }
        return hcode;
    }
    public boolean equals(Object obj) {
        if (obj instanceof SID) {
            SID sid = (SID)obj;
            if (sid == this)
                return true;
            if (sid.sub_authority_count == sub_authority_count) {
                int i = sub_authority_count;
                while (i-- > 0) {
                    if (sid.sub_authority[i] != sub_authority[i]) {
                        return false;
                    }
                }
                for (i = 0; i < 6; i++) {
                    if (sid.identifier_authority[i] != identifier_authority[i]) {
                        return false;
                    }
                }

                return sid.revision == revision;
            }
        }
        return false;
    }

    /**
     * Return the numeric representation of this sid such as
     * <tt>S-1-5-21-1496946806-2192648263-3843101252-1029</tt>.
     */
    public String toString() {
        String ret = "S-" + (revision & 0xFF) + "-";

        if (identifier_authority[0] != (byte)0 || identifier_authority[1] != (byte)0) {
            ret += "0x";
            ret += Hexdump.toHexString(identifier_authority, 0, 6);
        } else {
            long shift = 0;
            long id = 0;
            for (int i = 5; i > 1; i--) {
                id += (identifier_authority[i] & 0xFFL) << shift;
                shift += 8;
            }
            ret += id;
        }

        for (int i = 0; i < sub_authority_count ; i++)
            ret += "-" + (sub_authority[i] & 0xFFFFFFFFL);

        return ret;
    }

    /**
     * Return a String representing this SID ideal for display to
     * users. This method should return the same text that the ACL
     * editor in Windows would display.
     * <p>
     * Specifically, if the SID has
     * been resolved and it is not a domain SID or builtin account,
     * the full DOMAIN\name form of the account will be
     * returned (e.g. MYDOM\alice or MYDOM\Domain Users).
     * If the SID has been resolved but it is is a domain SID,
     * only the domain name will be returned (e.g. MYDOM).
     * If the SID has been resolved but it is a builtin account,
     * only the name component will be returned (e.g. SYSTEM).
     * If the sid cannot be resolved the numeric representation from
     * toString() is returned.
     */
    public String toDisplayString() {
        if (origin_server != null)
            resolveWeak();
        if (domainName != null) {
            String str;

            if (type == SID_TYPE_DOMAIN) {
                str = domainName;
            } else if (type == SID_TYPE_WKN_GRP ||
                        domainName.equals("BUILTIN")) {
                if (type == SID_TYPE_UNKNOWN) {
                    str = toString();
                } else {
                    str = acctName;
                }
            } else {
                str = domainName + "\\" + acctName;
            }

            return str;
        }
        return toString();
    }

    /**
     * Manually resolve this SID. Normally SIDs are automatically
     * resolved. However, if a SID is constructed explicitly using a SID
     * constructor, JCIFS will have no knowledge of the server that created the
     * SID and therefore cannot possibly resolve it automatically. In this case,
     * this method will be necessary.
     *  
     * @param authorityServerName The FQDN of the server that is an authority for the SID.
     * @param auth Credentials suitable for accessing the SID's information.
     */
    public void resolve(String authorityServerName,
                    NtlmPasswordAuthentication auth) throws IOException {
        SID[] sids = new SID[1];
        sids[0] = this;
        SID.resolveSids(authorityServerName, auth, sids);
    }

    void resolveWeak() {
        if (origin_server != null) {
            try {
                resolve(origin_server, origin_auth);
            } catch(IOException ioe) {
            } finally {
                origin_server = null;
                origin_auth = null;
            }
        }
    }

    static SID[] getGroupMemberSids0(DcerpcHandle handle,
                    SamrDomainHandle domainHandle,
                    SID domsid,
                    int rid,
                    int flags) throws IOException {
        SamrAliasHandle aliasHandle = null;
        lsarpc.LsarSidArray sidarray = new lsarpc.LsarSidArray();
        MsrpcGetMembersInAlias rpc = null;

        try {
            aliasHandle = new SamrAliasHandle(handle, domainHandle, 0x0002000c, rid);
            rpc = new MsrpcGetMembersInAlias(aliasHandle, sidarray);
            handle.sendrecv(rpc);
            if (rpc.retval != 0)
                throw new SmbException(rpc.retval, false);
            SID[] sids = new SID[rpc.sids.num_sids];

            String origin_server = handle.getServer();
            NtlmPasswordAuthentication origin_auth =
                        (NtlmPasswordAuthentication)handle.getPrincipal();

            for (int i = 0; i < sids.length; i++) {
                sids[i] = new SID(rpc.sids.sids[i].sid,
                            0,
                            null,
                            null,
                            false);
                sids[i].origin_server = origin_server;
                sids[i].origin_auth = origin_auth;
            }
            if (sids.length > 0 && (flags & SID_FLAG_RESOLVE_SIDS) != 0) {
                SID.resolveSids(origin_server, origin_auth, sids);
            }
            return sids;
        } finally {
            if (aliasHandle != null) {
                aliasHandle.close();
            }
        }
    }

    public SID[] getGroupMemberSids(String authorityServerName,
                    NtlmPasswordAuthentication auth,
                    int flags) throws IOException {
        if (type != SID_TYPE_DOM_GRP && type != SID_TYPE_ALIAS)
            return new SID[0];

        DcerpcHandle handle = null;
        SamrPolicyHandle policyHandle = null;
        SamrDomainHandle domainHandle = null;
        SID domsid = getDomainSid();

synchronized (sid_cache) {
        try {
            handle = DcerpcHandle.getHandle("ncacn_np:" + authorityServerName +
                    "[\\PIPE\\samr]", auth);
            policyHandle = new SamrPolicyHandle(handle, authorityServerName, 0x00000030);
            domainHandle = new SamrDomainHandle(handle, policyHandle, 0x00000200, domsid);
            return SID.getGroupMemberSids0(handle,
                        domainHandle,
                        domsid,
                        getRid(),
                        flags);
        } finally {
            if (handle != null) {
                if (policyHandle != null) {
                    if (domainHandle != null) {
                        domainHandle.close();
                    }
                    policyHandle.close();
                }
                handle.close();
            }
        }
}
    }

    /**
     * This specialized method returns a Map of users and local groups for the
     * target server where keys are SIDs representing an account and each value
     * is an ArrayList of SIDs represents the local groups that the account is
     * a member of.
     * <p/>
     * This method is designed to assist with computing access control for a
     * given user when the target object's ACL has local groups. Local groups
     * are not listed in a user's group membership (e.g. as represented by the
     * tokenGroups constructed attribute retrived via LDAP).
     * <p/>
     * Domain groups nested inside a local group are currently not expanded. In
     * this case the key (SID) type will be SID_TYPE_DOM_GRP rather than
     * SID_TYPE_USER.
     * 
     * @param authorityServerName The server from which the local groups will be queried.
     * @param auth The credentials required to query groups and group members.
     * @param flags Flags that control the behavior of the operation. When all
     * name associated with SIDs will be required, the SID_FLAG_RESOLVE_SIDS
     * flag should be used which causes all group member SIDs to be resolved
     * together in a single more efficient operation.
     */
    static Map getLocalGroupsMap(String authorityServerName,
                    NtlmPasswordAuthentication auth,
                    int flags) throws IOException {
        SID domsid = SID.getServerSid(authorityServerName, auth);
        DcerpcHandle handle = null;
        SamrPolicyHandle policyHandle = null;
        SamrDomainHandle domainHandle = null;
        samr.SamrSamArray sam = new samr.SamrSamArray();
        MsrpcEnumerateAliasesInDomain rpc;

synchronized (sid_cache) {
        try {
            handle = DcerpcHandle.getHandle("ncacn_np:" + authorityServerName +
                    "[\\PIPE\\samr]", auth);
            policyHandle = new SamrPolicyHandle(handle, authorityServerName, 0x02000000);
            domainHandle = new SamrDomainHandle(handle, policyHandle, 0x02000000, domsid);
            rpc = new MsrpcEnumerateAliasesInDomain(domainHandle, 0xFFFF, sam);
            handle.sendrecv(rpc);
            if (rpc.retval != 0)
                throw new SmbException(rpc.retval, false);

            Map map = new HashMap();

            for (int ei = 0; ei < rpc.sam.count; ei++) {
                samr.SamrSamEntry entry = rpc.sam.entries[ei];

                SID[] mems = SID.getGroupMemberSids0(handle,
                            domainHandle,
                            domsid,
                            entry.idx,
                            flags);
                SID groupSid = new SID(domsid, entry.idx);
                groupSid.type = SID_TYPE_ALIAS;
                groupSid.domainName = domsid.getDomainName();
                groupSid.acctName = (new UnicodeString(entry.name, false)).toString();

                for (int mi = 0; mi < mems.length; mi++) {
                    ArrayList groups = (ArrayList)map.get(mems[mi]);
                    if (groups == null) {
                        groups = new ArrayList();
                        map.put(mems[mi], groups);
                    }
                    if (!groups.contains(groupSid))
                        groups.add(groupSid);
                }
            }

            return map;
        } finally {
            if (handle != null) {
                if (policyHandle != null) {
                    if (domainHandle != null) {
                        domainHandle.close();
                    }
                    policyHandle.close();
                }
                handle.close();
            }
        }
}
    }
}

