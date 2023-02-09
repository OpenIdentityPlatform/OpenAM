package jcifs.dcerpc.msrpc;

import jcifs.dcerpc.DcerpcMessage;
import jcifs.dcerpc.ndr.NdrBuffer;
import jcifs.dcerpc.ndr.NdrException;
import jcifs.dcerpc.ndr.NdrObject;
import jcifs.dcerpc.rpc;

public class samr {

    public static String getSyntax() {
        return "12345778-1234-abcd-ef00-0123456789ac:1.0";
    }

    public static final int ACB_DISABLED = 1;
    public static final int ACB_HOMDIRREQ = 2;
    public static final int ACB_PWNOTREQ = 4;
    public static final int ACB_TEMPDUP = 8;
    public static final int ACB_NORMAL = 16;
    public static final int ACB_MNS = 32;
    public static final int ACB_DOMTRUST = 64;
    public static final int ACB_WSTRUST = 128;
    public static final int ACB_SVRTRUST = 256;
    public static final int ACB_PWNOEXP = 512;
    public static final int ACB_AUTOLOCK = 1024;
    public static final int ACB_ENC_TXT_PWD_ALLOWED = 2048;
    public static final int ACB_SMARTCARD_REQUIRED = 4096;
    public static final int ACB_TRUSTED_FOR_DELEGATION = 8192;
    public static final int ACB_NOT_DELEGATED = 16384;
    public static final int ACB_USE_DES_KEY_ONLY = 32768;
    public static final int ACB_DONT_REQUIRE_PREAUTH = 65536;

    public static class SamrCloseHandle extends DcerpcMessage {

        public int getOpnum() { return 0x01; }

        public int retval;
        public rpc.policy_handle handle;

        public SamrCloseHandle(rpc.policy_handle handle) {
            this.handle = handle;
        }

        public void encode_in(NdrBuffer _dst) throws NdrException {
            handle.encode(_dst);
        }
        public void decode_out(NdrBuffer _src) throws NdrException {
            retval = (int)_src.dec_ndr_long();
        }
    }
    public static class SamrConnect2 extends DcerpcMessage {

        public int getOpnum() { return 0x39; }

        public int retval;
        public String system_name;
        public int access_mask;
        public rpc.policy_handle handle;

        public SamrConnect2(String system_name, int access_mask, rpc.policy_handle handle) {
            this.system_name = system_name;
            this.access_mask = access_mask;
            this.handle = handle;
        }

        public void encode_in(NdrBuffer _dst) throws NdrException {
            _dst.enc_ndr_referent(system_name, 1);
            if (system_name != null) {
                _dst.enc_ndr_string(system_name);

            }
            _dst.enc_ndr_long(access_mask);
        }
        public void decode_out(NdrBuffer _src) throws NdrException {
            handle.decode(_src);
            retval = (int)_src.dec_ndr_long();
        }
    }
    public static class SamrConnect4 extends DcerpcMessage {

        public int getOpnum() { return 0x3e; }

        public int retval;
        public String system_name;
        public int unknown;
        public int access_mask;
        public rpc.policy_handle handle;

        public SamrConnect4(String system_name,
                    int unknown,
                    int access_mask,
                    rpc.policy_handle handle) {
            this.system_name = system_name;
            this.unknown = unknown;
            this.access_mask = access_mask;
            this.handle = handle;
        }

        public void encode_in(NdrBuffer _dst) throws NdrException {
            _dst.enc_ndr_referent(system_name, 1);
            if (system_name != null) {
                _dst.enc_ndr_string(system_name);

            }
            _dst.enc_ndr_long(unknown);
            _dst.enc_ndr_long(access_mask);
        }
        public void decode_out(NdrBuffer _src) throws NdrException {
            handle.decode(_src);
            retval = (int)_src.dec_ndr_long();
        }
    }
    public static class SamrOpenDomain extends DcerpcMessage {

        public int getOpnum() { return 0x07; }

        public int retval;
        public rpc.policy_handle handle;
        public int access_mask;
        public rpc.sid_t sid;
        public rpc.policy_handle domain_handle;

        public SamrOpenDomain(rpc.policy_handle handle,
                    int access_mask,
                    rpc.sid_t sid,
                    rpc.policy_handle domain_handle) {
            this.handle = handle;
            this.access_mask = access_mask;
            this.sid = sid;
            this.domain_handle = domain_handle;
        }

        public void encode_in(NdrBuffer _dst) throws NdrException {
            handle.encode(_dst);
            _dst.enc_ndr_long(access_mask);
            sid.encode(_dst);
        }
        public void decode_out(NdrBuffer _src) throws NdrException {
            domain_handle.decode(_src);
            retval = (int)_src.dec_ndr_long();
        }
    }
    public static class SamrSamEntry extends NdrObject {

        public int idx;
        public rpc.unicode_string name;

        public void encode(NdrBuffer _dst) throws NdrException {
            _dst.align(4);
            _dst.enc_ndr_long(idx);
            _dst.enc_ndr_short(name.length);
            _dst.enc_ndr_short(name.maximum_length);
            _dst.enc_ndr_referent(name.buffer, 1);

            if (name.buffer != null) {
                _dst = _dst.deferred;
                int _name_bufferl = name.length / 2;
                int _name_buffers = name.maximum_length / 2;
                _dst.enc_ndr_long(_name_buffers);
                _dst.enc_ndr_long(0);
                _dst.enc_ndr_long(_name_bufferl);
                int _name_bufferi = _dst.index;
                _dst.advance(2 * _name_bufferl);

                _dst = _dst.derive(_name_bufferi);
                for (int _i = 0; _i < _name_bufferl; _i++) {
                    _dst.enc_ndr_short(name.buffer[_i]);
                }
            }
        }
        public void decode(NdrBuffer _src) throws NdrException {
            _src.align(4);
            idx = (int)_src.dec_ndr_long();
            _src.align(4);
            if (name == null) {
                name = new rpc.unicode_string();
            }
            name.length = (short)_src.dec_ndr_short();
            name.maximum_length = (short)_src.dec_ndr_short();
            int _name_bufferp = _src.dec_ndr_long();

            if (_name_bufferp != 0) {
                _src = _src.deferred;
                int _name_buffers = _src.dec_ndr_long();
                _src.dec_ndr_long();
                int _name_bufferl = _src.dec_ndr_long();
                int _name_bufferi = _src.index;
                _src.advance(2 * _name_bufferl);

                if (name.buffer == null) {
                    if (_name_buffers < 0 || _name_buffers > 0xFFFF) throw new NdrException( NdrException.INVALID_CONFORMANCE );
                    name.buffer = new short[_name_buffers];
                }
                _src = _src.derive(_name_bufferi);
                for (int _i = 0; _i < _name_bufferl; _i++) {
                    name.buffer[_i] = (short)_src.dec_ndr_short();
                }
            }
        }
    }
    public static class SamrSamArray extends NdrObject {

        public int count;
        public SamrSamEntry[] entries;

        public void encode(NdrBuffer _dst) throws NdrException {
            _dst.align(4);
            _dst.enc_ndr_long(count);
            _dst.enc_ndr_referent(entries, 1);

            if (entries != null) {
                _dst = _dst.deferred;
                int _entriess = count;
                _dst.enc_ndr_long(_entriess);
                int _entriesi = _dst.index;
                _dst.advance(12 * _entriess);

                _dst = _dst.derive(_entriesi);
                for (int _i = 0; _i < _entriess; _i++) {
                    entries[_i].encode(_dst);
                }
            }
        }
        public void decode(NdrBuffer _src) throws NdrException {
            _src.align(4);
            count = (int)_src.dec_ndr_long();
            int _entriesp = _src.dec_ndr_long();

            if (_entriesp != 0) {
                _src = _src.deferred;
                int _entriess = _src.dec_ndr_long();
                int _entriesi = _src.index;
                _src.advance(12 * _entriess);

                if (entries == null) {
                    if (_entriess < 0 || _entriess > 0xFFFF) throw new NdrException( NdrException.INVALID_CONFORMANCE );
                    entries = new SamrSamEntry[_entriess];
                }
                _src = _src.derive(_entriesi);
                for (int _i = 0; _i < _entriess; _i++) {
                    if (entries[_i] == null) {
                        entries[_i] = new SamrSamEntry();
                    }
                    entries[_i].decode(_src);
                }
            }
        }
    }
    public static class SamrEnumerateAliasesInDomain extends DcerpcMessage {

        public int getOpnum() { return 0x0f; }

        public int retval;
        public rpc.policy_handle domain_handle;
        public int resume_handle;
        public int acct_flags;
        public SamrSamArray sam;
        public int num_entries;

        public SamrEnumerateAliasesInDomain(rpc.policy_handle domain_handle,
                    int resume_handle,
                    int acct_flags,
                    SamrSamArray sam,
                    int num_entries) {
            this.domain_handle = domain_handle;
            this.resume_handle = resume_handle;
            this.acct_flags = acct_flags;
            this.sam = sam;
            this.num_entries = num_entries;
        }

        public void encode_in(NdrBuffer _dst) throws NdrException {
            domain_handle.encode(_dst);
            _dst.enc_ndr_long(resume_handle);
            _dst.enc_ndr_long(acct_flags);
        }
        public void decode_out(NdrBuffer _src) throws NdrException {
            resume_handle = (int)_src.dec_ndr_long();
            int _samp = _src.dec_ndr_long();
            if (_samp != 0) {
                if (sam == null) { /* YOYOYO */
                    sam = new SamrSamArray();
                }
                sam.decode(_src);

            }
            num_entries = (int)_src.dec_ndr_long();
            retval = (int)_src.dec_ndr_long();
        }
    }
    public static class SamrOpenAlias extends DcerpcMessage {

        public int getOpnum() { return 0x1b; }

        public int retval;
        public rpc.policy_handle domain_handle;
        public int access_mask;
        public int rid;
        public rpc.policy_handle alias_handle;

        public SamrOpenAlias(rpc.policy_handle domain_handle,
                    int access_mask,
                    int rid,
                    rpc.policy_handle alias_handle) {
            this.domain_handle = domain_handle;
            this.access_mask = access_mask;
            this.rid = rid;
            this.alias_handle = alias_handle;
        }

        public void encode_in(NdrBuffer _dst) throws NdrException {
            domain_handle.encode(_dst);
            _dst.enc_ndr_long(access_mask);
            _dst.enc_ndr_long(rid);
        }
        public void decode_out(NdrBuffer _src) throws NdrException {
            alias_handle.decode(_src);
            retval = (int)_src.dec_ndr_long();
        }
    }
    public static class SamrGetMembersInAlias extends DcerpcMessage {

        public int getOpnum() { return 0x21; }

        public int retval;
        public rpc.policy_handle alias_handle;
        public lsarpc.LsarSidArray sids;

        public SamrGetMembersInAlias(rpc.policy_handle alias_handle, lsarpc.LsarSidArray sids) {
            this.alias_handle = alias_handle;
            this.sids = sids;
        }

        public void encode_in(NdrBuffer _dst) throws NdrException {
            alias_handle.encode(_dst);
        }
        public void decode_out(NdrBuffer _src) throws NdrException {
            sids.decode(_src);
            retval = (int)_src.dec_ndr_long();
        }
    }
    public static final int SE_GROUP_MANDATORY = 1;
    public static final int SE_GROUP_ENABLED_BY_DEFAULT = 2;
    public static final int SE_GROUP_ENABLED = 4;
    public static final int SE_GROUP_OWNER = 8;
    public static final int SE_GROUP_USE_FOR_DENY_ONLY = 16;
    public static final int SE_GROUP_RESOURCE = 536870912;
    public static final int SE_GROUP_LOGON_ID = -1073741824;

    public static class SamrRidWithAttribute extends NdrObject {

        public int rid;
        public int attributes;

        public void encode(NdrBuffer _dst) throws NdrException {
            _dst.align(4);
            _dst.enc_ndr_long(rid);
            _dst.enc_ndr_long(attributes);

        }
        public void decode(NdrBuffer _src) throws NdrException {
            _src.align(4);
            rid = (int)_src.dec_ndr_long();
            attributes = (int)_src.dec_ndr_long();

        }
    }
    public static class SamrRidWithAttributeArray extends NdrObject {

        public int count;
        public SamrRidWithAttribute[] rids;

        public void encode(NdrBuffer _dst) throws NdrException {
            _dst.align(4);
            _dst.enc_ndr_long(count);
            _dst.enc_ndr_referent(rids, 1);

            if (rids != null) {
                _dst = _dst.deferred;
                int _ridss = count;
                _dst.enc_ndr_long(_ridss);
                int _ridsi = _dst.index;
                _dst.advance(8 * _ridss);

                _dst = _dst.derive(_ridsi);
                for (int _i = 0; _i < _ridss; _i++) {
                    rids[_i].encode(_dst);
                }
            }
        }
        public void decode(NdrBuffer _src) throws NdrException {
            _src.align(4);
            count = (int)_src.dec_ndr_long();
            int _ridsp = _src.dec_ndr_long();

            if (_ridsp != 0) {
                _src = _src.deferred;
                int _ridss = _src.dec_ndr_long();
                int _ridsi = _src.index;
                _src.advance(8 * _ridss);

                if (rids == null) {
                    if (_ridss < 0 || _ridss > 0xFFFF) throw new NdrException( NdrException.INVALID_CONFORMANCE );
                    rids = new SamrRidWithAttribute[_ridss];
                }
                _src = _src.derive(_ridsi);
                for (int _i = 0; _i < _ridss; _i++) {
                    if (rids[_i] == null) {
                        rids[_i] = new SamrRidWithAttribute();
                    }
                    rids[_i].decode(_src);
                }
            }
        }
    }
}
