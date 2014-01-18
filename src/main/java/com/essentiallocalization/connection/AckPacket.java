package com.essentiallocalization.connection;

import java.nio.ByteBuffer;

/**
 * Created by Jake on 9/14/13.
 */
public final class AckPacket extends Packet {

    public final long received;
    public long resent;

    @Override
    public int size() {
        return Packet.HEADER_SIZE + 16;
    }

    public AckPacket(int packetIndex, int msgIndex, byte msgAttempt, byte msgPart, byte msgParts, long received) {
        super(Packet.TYPE_ACK, packetIndex, msgIndex, msgAttempt, msgPart, msgParts);
        this.received = received;
    }

    public AckPacket(byte[] fromAckPacket) {
        super(fromAckPacket);
        received = ByteBuffer.wrap(fromAckPacket).getLong(Packet.HEADER_SIZE);
        resent = ByteBuffer.wrap(fromAckPacket).getLong(Packet.HEADER_SIZE + 8);
    }

    public AckPacket(DataPacket packet) {
        super(Packet.TYPE_ACK, packet.packetIndex, packet.msgIndex, packet.msgAttempt, packet.msgPart, packet.msgParts);
        this.received = System.nanoTime();
    }

    @Override
    public byte[] onSend() {
        return getBuffer()
                .putLong(received)
                .putLong(resent = System.nanoTime())
                .array();
    }
}
