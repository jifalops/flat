package com.essentiallocalization.connection;

import java.nio.ByteBuffer;

/**
 * Created by Jake on 9/14/13.
 */
public final class DataPacket extends Packet {

    public long hciSrcSent;
    public long hciDestReceived;
    public long hciDestSent;
    public long hciSrcReceived;

    public long javaSrcSent;
    public long javaDestReceived;
    public long javaDestSent;
    public long javaSrcReceived; 

    private static final int HEADER_SIZE = (8 * 8) + (4 * 0) + (2 * 0) + (1 * 0); // see properties above
    public static final int MAX_PAYLOAD = BUFFER_SIZE - (HEADER_SIZE + Packet.HEADER_SIZE + PREFIX.length());

    public byte[] payload;

    /*
     * It takes 3 packets to exchange transmission times when the time can only be
     * determined AFTER a packet is sent.
     *
     * 1. A sends packet to B. A will then know the send time (B will not).
     * 2. B sends ack to A. A will then know B's received time and A's received time.
     * 3. B sends ack_time to A. After B see what time the ack was sent, it can let A know.
     *
     * 'A' now has the four times needed to compute the round trip time:
     *      (aReceived - aSent) - (bSent - bReceived)
     *
     * Note that the clocks for A and B need not be synchronized for this calculation.
     */

    DataPacket() {
        type = Packet.TYPE_DATA;
    }

    DataPacket(Packet p) {
        super(p);
        type = Packet.TYPE_DATA;
    }

    DataPacket(byte[] dataPacket) {
        super(dataPacket);
        type = Packet.TYPE_DATA;

        ByteBuffer bb = ByteBuffer.wrap(dataPacket);
        bb.position(Packet.HEADER_SIZE + PREFIX.length());
        hciSrcSent = bb.getLong();
        hciDestReceived = bb.getLong();
        hciDestSent = bb.getLong();
        hciSrcReceived = bb.getLong();
        javaSrcSent = bb.getLong();
        javaDestReceived = bb.getLong();
        javaDestSent = bb.getLong();
        javaSrcReceived = bb.getLong();
        payload = new byte[bb.limit() - bb.position()];
        bb.get(payload);


    }

    @Override
    public byte[] getBytes(boolean sending) {
        return getBuffer()
                .putLong(hciSrcSent)
                .putLong(hciDestReceived)
                .putLong(hciDestSent)
                .putLong(hciSrcReceived)
                .putLong(sending ? javaSrcSent = System.nanoTime()
                                 : javaSrcSent)
                .putLong(javaDestReceived)
                .putLong(javaDestSent)
                .putLong(javaSrcReceived)
                .put(payload)
                .array();
    }

    @Override
    public int marginalHeaderSize() {
        return HEADER_SIZE;
    }

    @Override
    public int payloadSize() {
        return payload.length;
    }
}
