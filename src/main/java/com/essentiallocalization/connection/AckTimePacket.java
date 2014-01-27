package com.essentiallocalization.connection;

import java.nio.ByteBuffer;

/**
 * Created by Jake on 9/14/13.
 */
public final class AckTimePacket extends Packet {

    public long hciDestSent;
    private static final int HEADER_SIZE = (8 * 1) + (4 * 0) + (2 * 0) + (1 * 0); // see properties above

    AckTimePacket(byte[] ackTimePacket) {
        super(ackTimePacket);
        type = Packet.TYPE_ACK_TIME;

        ByteBuffer bb = ByteBuffer.wrap(ackTimePacket);
        bb.position(Packet.HEADER_SIZE + PREFIX.length());
        hciDestSent = bb.getLong(Packet.HEADER_SIZE);
    }

    AckTimePacket() {
        type = Packet.TYPE_ACK_TIME;
    }

    AckTimePacket(Packet p) {
        super(p);
        type = Packet.TYPE_ACK_TIME;
    }

    @Override
    public byte[] getBytes(boolean sending) {
        return getBuffer()
                .putLong(hciDestSent)
                .array();
    }

    @Override
    public int marginalHeaderSize() {
        return HEADER_SIZE;
    }

    @Override
    public int payloadSize() {
        return 0;
    }
}
