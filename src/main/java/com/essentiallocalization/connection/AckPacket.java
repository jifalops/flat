package com.essentiallocalization.connection;

import java.nio.ByteBuffer;

/**
 * Created by Jake on 9/14/13.
 */
public class AckPacket implements Packet {
    public final byte type;
    public final int packetIndex;
    public final int msgIndex;
    public final byte msgAttempt;
    public final byte msgPart;
    public final byte msgParts;
    public final long received;

    public AckPacket(int packetIndex, int msgIndex, byte msgAttempt, byte msgPart, byte msgParts) {
        type = Packet.TYPE_ACK;
        this.packetIndex = packetIndex;
        this.msgIndex = msgIndex;
        this.msgAttempt = msgAttempt;
        this.msgPart = msgPart;
        this.msgParts = msgParts;
        received = System.nanoTime();
    }

    public AckPacket(byte[] fromAckPacket) {
        ByteBuffer bb = ByteBuffer.wrap(fromAckPacket);
        type = bb.get();
        packetIndex = bb.getInt();
        msgIndex = bb.getInt();
        msgAttempt = bb.get();
        msgPart = bb.get();
        msgParts = bb.get();
        received = bb.getLong();
    }

    public AckPacket(DataPacket packet) {
        this(packet.packetIndex, packet.msgIndex, packet.msgAttempt, packet.msgPart, packet.msgParts);
    }

    @Override
    public byte[] getBytes() {
        return ByteBuffer.allocate(13)
                .put(type)
                .putInt(packetIndex)
                .putInt(msgIndex)
                .put(msgAttempt)
                .put(msgPart)
                .put(msgParts)
                .putLong(received)
                .array();
    }
}
