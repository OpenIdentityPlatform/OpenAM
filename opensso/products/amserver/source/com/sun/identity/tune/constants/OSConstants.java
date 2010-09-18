/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems, Inc. All Rights Reserved.
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * https://opensso.dev.java.net/public/CDDLv1.0.html or
 * opensso/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at opensso/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: OSConstants.java,v 1.3 2009/12/09 00:33:00 ykwon Exp $
 */

package com.sun.identity.tune.constants;


public interface OSConstants {
    static String RLIM_FD_MAX = "rlim_fd_max";
    static String RLIM_FD_CUR = "rlim_fd_cur";
    static String TCP_CON_HASH_SIZE = "tcp:tcp_conn_hash_size";
    static String SOL_TCP_TIME_WAIT_INTERVAL = "tcp_time_wait_interval";
    static String SOL_TCP_FIN_WAIT_2_FLUSH_INTERVAL = 
            "tcp_fin_wait_2_flush_interval";
    static String SOL_TCP_CONN_REQ_MAX_Q = "tcp_conn_req_max_q";
    static String SOL_TCP_CONN_REQ_MAX_Q0 = "tcp_conn_req_max_q0";    
    static String SOL_TCP_KEEPALIVE_INTERVAL = "tcp_keepalive_interval";
    static String SOL_TCP_SMALLEST_ANON_PORT = "tcp_smallest_anon_port";
    static String SOL_TCP_SLOW_START_INTITIAL = "tcp_slow_start_initial";
    static String SOL_TCP_MAX_BUF = "tcp_max_buf";
    static String SOL_TCP_CWND_MAX = "tcp_cwnd_max";
    static String SOL_TCP_XMIT_HIWAT = "tcp_xmit_hiwat";
    static String SOL_TCP_RECV_HIWAT = "tcp_recv_hiwat";
    static String SOL_TCP_IP_ABORT_CINTERVAL = "tcp_ip_abort_cinterval";
    static String SOL_TCP_DEFERRED_ACK_INTERVAL = "tcp_deferred_ack_interval";
    static String SOL_TCP_STRONG_ISS = "tcp_strong_iss";
    static String SOL_TCP_IP_ABORT_INTERVAL = "tcp_ip_abort_interval";
    static String SOL_TCP_REXMIT_INTERVAL_MAX = "tcp_rexmit_interval_max";
    static String SOL_TCP_REXMIT_INTERVAL_MIN = "tcp_rexmit_interval_min";
    static String SOL_TCP_REXMIT_INTERVAL_INITIAL = 
            "tcp_rexmit_interval_initial";
    static String FLUSH_INTERVAL_VAL = "67500";
    static String KEEP_ALIVE_INTERVAL_VAL = "90000";
    static String ANON_PORT_VAL = "1024";
    static String SLOW_START_INITIAL_VAL = "2";
    static String MAX_BUF_CWND_VAL = "2097152";
    static String XMIT_RECV_HIWAT_VAL = "400000";
    static String ABORT_CINTERVAL_VAL = "10000";
    static String ACK_INTERVAL_VAL = "5";
    static String STRONG_ISS_VAL = "2";
    static String LNX_FILE_MAX = "fs.file-max";
    static String LNX_IPV4_LOCAL_PORT_RANGE = "net.ipv4.ip_local_port_range";
    static String LNX_CORE_RMEM_MAX = "net.core.rmem_max";
    static String LNX_CORE_RMEM_DEFAULT = "net.core.rmem_default";
    static String LNX_IPV4_TCP_RMEM = "net.ipv4.tcp_rmem";
    static String LNX_IPV4_TCP_WMEM = "net.ipv4.tcp_wmem";
    static String LNX_IPV4_TCP_SACK = "net.ipv4.tcp_sack";
    static String LNX_IPV4_TCP_TIMESTAMPS = "net.ipv4.tcp_timestamps";
    static String LNX_IPV4_TCP_WINDOW_SCALING = "net.ipv4.tcp_window_scaling";
    static String LNX_IPV4_TCP_KEEPALIVE_TIME = "net.ipv4.tcp_keepalive_time";
    static String LNX_IPV4_TCP_KEEPALIVE_INTVL = "net.ipv4.tcp_keepalive_intvl";
    static String LNX_IPV4_TCP_FIN_TIMEOUT = "net.ipv4.tcp_fin_timeout";
    static String LNX_TCP_FIN_TIMEOUT_NAME =
            "/proc/sys/net/ipv4/tcp_fin_timeout";
    static String LNX_TCP_KEEPALIVE_TIME_NAME = 
            "/proc/sys/net/ipv4/tcp_keepalive_time";
    static String LNX_TCP_KEEPALIVE_INTVL_NAME =
            "/proc/sys/net/ipv4/tcp_keepalive_intvl";
    static String LNX_TCP_WINDOW_SCALING_NAME =
            "/proc/sys/net/ipv4/tcp_window_scaling";
    static String LNX_LOAD_SYSCTL_CMD =
            "sysctl -p /etc/sysctl.conf";
}
