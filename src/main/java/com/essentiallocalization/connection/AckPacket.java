package com.essentiallocalization.connection;

import java.nio.ByteBuffer;

/**
 * Created by Jake on 9/14/13.
 */
public final class AckPacket extends Packet {

    public final long received;

    public AckPacket(int packetIndex, int msgIndex, byte msgAttempt, byte msgPart, byte msgParts) {
        super(Packet.TYPE_ACK, packetIndex, msgIndex, msgAttempt, msgPart, msgParts);
        received = System.nanoTime();
    }



    public AckPacket(byte[] fromAckPacket) {
        super(fromAckPacket);
        received = ByteBuffer.wrap(fromAckPacket).getLong(Packet.HEADER_SIZE);
    }

    public AckPacket(DataPacket packet) {
        this(packet.packetIndex, packet.msgIndex, packet.msgAttempt, packet.msgPart, packet.msgParts);
    }

    @Override
    public byte[] getBytes() {
        return getBuffer()
                .putLong(received)
                .array();
    }

    @Override
    public int size() {
        return Packet.HEADER_SIZE + 8;
    }
}
