package jcifs.dcerpc;

import jcifs.dcerpc.ndr.NdrBuffer;
import jcifs.dcerpc.ndr.NdrException;
import jcifs.dcerpc.ndr.NdrObject;

public class rpc {

    public static class uuid_t extends NdrObject {

        public int time_low;
        public short time_mid;
        public short time_hi_and_version;
        public byte clock_seq_hi_and_reserved;
        public byte clock_seq_low;
        public byte[] node;

        public void encode(NdrBuffer _dst) throws NdrException {
            _dst.align(4);
            _dst.enc_ndr_long(time_low);
            _dst.enc_ndr_short(time_mid);
            _dst.enc_ndr_short(time_hi_and_version);
            _dst.enc_ndr_small(clock_seq_hi_and_reserved);
            _dst.enc_ndr_small(clock_seq_low);
            int _nodes = 6;
            int _nodei = _dst.index;
            _dst.advance(1 * _nodes);

            _dst = _dst.derive(_nodei);
            for (int _i = 0; _i < _nodes; _i++) {
                _dst.enc_ndr_small(node[_i]);
            }
        }
        public void decode(NdrBuffer _src) throws NdrException {
            _src.align(4);
            time_low = (int)_src.dec_ndr_long();
            time_mid = (short)_src.dec_ndr_short();
            time_hi_and_version = (short)_src.dec_ndr_short();
            clock_seq_hi_and_reserved = (byte)_src.dec_ndr_small();
            clock_seq_low = (byte)_src.dec_ndr_small();
            int _nodes = 6;
            int _nodei = _src.index;
            _src.advance(1 * _nodes);

            if (node == null) {
                if (_nodes < 0 || _nodes > 0xFFFF) throw new NdrException( NdrException.INVALID_CONFORMANCE );
                node = new byte[_nodes];
            }
            _src = _src.derive(_nodei);
            for (int _i = 0; _i < _nodes; _i++) {
                node[_i] = (byte)_src.dec_ndr_small();
            }
        }
    }
    public static class policy_handle extends NdrObject {

        public int type;
        public uuid_t uuid;

        public void encode(NdrBuffer _dst) throws NdrException {
            _dst.align(4);
            _dst.enc_ndr_long(type);
            _dst.enc_ndr_long(uuid.time_low);
            _dst.enc_ndr_short(uuid.time_mid);
            _dst.enc_ndr_short(uuid.time_hi_and_version);
            _dst.enc_ndr_small(uuid.clock_seq_hi_and_reserved);
            _dst.enc_ndr_small(uuid.clock_seq_low);
            int _uuid_nodes = 6;
            int _uuid_nodei = _dst.index;
            _dst.advance(1 * _uuid_nodes);

            _dst = _dst.derive(_uuid_nodei);
            for (int _i = 0; _i < _uuid_nodes; _i++) {
                _dst.enc_ndr_small(uuid.node[_i]);
            }
        }
        public void decode(NdrBuffer _src) throws NdrException {
            _src.align(4);
            type = (int)_src.dec_ndr_long();
            _src.align(4);
            if (uuid == null) {
                uuid = new uuid_t();
            }
            uuid.time_low = (int)_src.dec_ndr_long();
            uuid.time_mid = (short)_src.dec_ndr_short();
            uuid.time_hi_and_version = (short)_src.dec_ndr_short();
            uuid.clock_seq_hi_and_reserved = (byte)_src.dec_ndr_small();
            uuid.clock_seq_low = (byte)_src.dec_ndr_small();
            int _uuid_nodes = 6;
            int _uuid_nodei = _src.index;
            _src.advance(1 * _uuid_nodes);

            if (uuid.node == null) {
                if (_uuid_nodes < 0 || _uuid_nodes > 0xFFFF) throw new NdrException( NdrException.INVALID_CONFORMANCE );
                uuid.node = new byte[_uuid_nodes];
            }
            _src = _src.derive(_uuid_nodei);
            for (int _i = 0; _i < _uuid_nodes; _i++) {
                uuid.node[_i] = (byte)_src.dec_ndr_small();
            }
        }
    }
    public static class unicode_string extends NdrObject {

        public short length;
        public short maximum_length;
        public short[] buffer;

        public void encode(NdrBuffer _dst) throws NdrException {
            _dst.align(4);
            _dst.enc_ndr_short(length);
            _dst.enc_ndr_short(maximum_length);
            _dst.enc_ndr_referent(buffer, 1);

            if (buffer != null) {
                _dst = _dst.deferred;
                int _bufferl = length / 2;
                int _buffers = maximum_length / 2;
                _dst.enc_ndr_long(_buffers);
                _dst.enc_ndr_long(0);
                _dst.enc_ndr_long(_bufferl);
                int _bufferi = _dst.index;
                _dst.advance(2 * _bufferl);

                _dst = _dst.derive(_bufferi);
                for (int _i = 0; _i < _bufferl; _i++) {
                    _dst.enc_ndr_short(buffer[_i]);
                }
            }
        }
        public void decode(NdrBuffer _src) throws NdrException {
            _src.align(4);
            length = (short)_src.dec_ndr_short();
            maximum_length = (short)_src.dec_ndr_short();
            int _bufferp = _src.dec_ndr_long();

            if (_bufferp != 0) {
                _src = _src.deferred;
                int _buffers = _src.dec_ndr_long();
                _src.dec_ndr_long();
                int _bufferl = _src.dec_ndr_long();
                int _bufferi = _src.index;
                _src.advance(2 * _bufferl);

                if (buffer == null) {
                    if (_buffers < 0 || _buffers > 0xFFFF) throw new NdrException( NdrException.INVALID_CONFORMANCE );
                    buffer = new short[_buffers];
                }
                _src = _src.derive(_bufferi);
                for (int _i = 0; _i < _bufferl; _i++) {
                    buffer[_i] = (short)_src.dec_ndr_short();
                }
            }
        }
    }
    public static class sid_t extends NdrObject {

        public byte revision;
        public byte sub_authority_count;
        public byte[] identifier_authority;
        public int[] sub_authority;

        public void encode(NdrBuffer _dst) throws NdrException {
            _dst.align(4);
            int _sub_authoritys = sub_authority_count;
            _dst.enc_ndr_long(_sub_authoritys);
            _dst.enc_ndr_small(revision);
            _dst.enc_ndr_small(sub_authority_count);
            int _identifier_authoritys = 6;
            int _identifier_authorityi = _dst.index;
            _dst.advance(1 * _identifier_authoritys);
            int _sub_authorityi = _dst.index;
            _dst.advance(4 * _sub_authoritys);

            _dst = _dst.derive(_identifier_authorityi);
            for (int _i = 0; _i < _identifier_authoritys; _i++) {
                _dst.enc_ndr_small(identifier_authority[_i]);
            }
            _dst = _dst.derive(_sub_authorityi);
            for (int _i = 0; _i < _sub_authoritys; _i++) {
                _dst.enc_ndr_long(sub_authority[_i]);
            }
        }
        public void decode(NdrBuffer _src) throws NdrException {
            _src.align(4);
            int _sub_authoritys = _src.dec_ndr_long();
            revision = (byte)_src.dec_ndr_small();
            sub_authority_count = (byte)_src.dec_ndr_small();
            int _identifier_authoritys = 6;
            int _identifier_authorityi = _src.index;
            _src.advance(1 * _identifier_authoritys);
            int _sub_authorityi = _src.index;
            _src.advance(4 * _sub_authoritys);

            if (identifier_authority == null) {
                if (_identifier_authoritys < 0 || _identifier_authoritys > 0xFFFF) throw new NdrException( NdrException.INVALID_CONFORMANCE );
                identifier_authority = new byte[_identifier_authoritys];
            }
            _src = _src.derive(_identifier_authorityi);
            for (int _i = 0; _i < _identifier_authoritys; _i++) {
                identifier_authority[_i] = (byte)_src.dec_ndr_small();
            }
            if (sub_authority == null) {
                if (_sub_authoritys < 0 || _sub_authoritys > 0xFFFF) throw new NdrException( NdrException.INVALID_CONFORMANCE );
                sub_authority = new int[_sub_authoritys];
            }
            _src = _src.derive(_sub_authorityi);
            for (int _i = 0; _i < _sub_authoritys; _i++) {
                sub_authority[_i] = (int)_src.dec_ndr_long();
            }
        }
    }
}
