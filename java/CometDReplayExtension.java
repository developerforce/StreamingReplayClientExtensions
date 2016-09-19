/*
Copyright 2016 salesforce.com, inc. All rights reserved.
Use of this software is subject to the salesforce.com Developerforce Terms of Use and other applicable terms that salesforce.com may make available, as may be amended from time to time. You may not decompile, reverse engineer, disassemble, attempt to derive the source code of, decrypt, modify, or create derivative works of this software, updates thereto, or any part thereof. You may not use the software to engage in any development activity that infringes the rights of a third party, including that which interferes with, damages, or accesses in an unauthorized manner the servers, networks, or other properties or services of salesforce.com or any third party.
WITHOUT LIMITING THE GENERALITY OF THE FOREGOING, THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED. IN NO EVENT SHALL SALESFORCE.COM HAVE ANY LIABILITY FOR ANY DAMAGES, INCLUDING BUT NOT LIMITED TO, DIRECT, INDIRECT, SPECIAL, INCIDENTAL, PUNITIVE, OR CONSEQUENTIAL DAMAGES, OR DAMAGES BASED ON LOST PROFITS, DATA OR USE, IN CONNECTION WITH THE SOFTWARE, HOWEVER CAUSED AND, WHETHER IN CONTRACT, TORT OR UNDER ANY OTHER THEORY OF LIABILITY, WHETHER OR NOT YOU HAVE BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
*/
package com.salesforce.conduit.cometd.extensions;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.cometd.bayeux.Channel;
import org.cometd.bayeux.Message;
import org.cometd.bayeux.client.ClientSession;
import org.cometd.bayeux.client.ClientSession.Extension.Adapter;

/**
 * CometDReplayExtension, typical usages are the following:
 * {@code client.addExtension(new CometDReplayExtension<>(replayMap));}
 *
 * @author yzhao
 * @since 198 (Winter '16)
 */
public class CometDReplayExtension<V> extends Adapter {
    private static final String EXTENSION_NAME = "replay";
    private final ConcurrentMap<String, V> dataMap = new ConcurrentHashMap<>();
    private final AtomicBoolean supported = new AtomicBoolean();

    public CometDReplayExtension(Map<String, V> dataMap) {
        this.dataMap.putAll(dataMap);
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean rcv(ClientSession session, Message.Mutable message) {
        Object data = message.get(EXTENSION_NAME);
        if (this.supported.get() && data != null) {
            try {
                dataMap.put(message.getChannel(), (V)data);
            } catch (ClassCastException e) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean rcvMeta(ClientSession session, Message.Mutable message) {
        switch (message.getChannel()) {
        case Channel.META_HANDSHAKE:
            Map<String, Object> ext = message.getExt(false);
            this.supported.set(ext != null && Boolean.TRUE.equals(ext.get(EXTENSION_NAME)));
        }
        return true;
    }

    @Override
    public boolean sendMeta(ClientSession session, Message.Mutable message) {
        switch (message.getChannel()) {
        case Channel.META_HANDSHAKE:
            message.getExt(true).put(EXTENSION_NAME, Boolean.TRUE);
            break;
        case Channel.META_SUBSCRIBE:
            if (supported.get()) {
                message.getExt(true).put(EXTENSION_NAME, dataMap);
            }
            break;
        }
        return true;
    }
}
