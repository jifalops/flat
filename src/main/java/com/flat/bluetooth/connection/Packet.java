package com.flat.bluetooth.connection;

import java.nio.ByteBuffer;

/**
 * Created by Jake on 9/14/13.
 */
public abstract class Packet {
    public static final int BUFFER_SIZE = 1024;

    public static final byte TYPE_DATA = 1;
    public static final byte TYPE_ACK = 2;
    public static final byte TYPE_ACK_TIME = 3;

    public static final String PREPEND = "EssLocPr";
    public static final String APPEND = "EssLocAp";

    // Perspective is from the source device (i.e. this device's pktIndex)
    /** this DOES include the size of the prefix string. */
    public short size;
    public byte type;
    public byte src;
    public byte dest;
    public int pktIndex;    // 0 based
    public int msgIndex;    // 0 based
    public byte msgPart;    // 0 based
    public byte msgParts;   // 1 based
    public byte attempt;    // 1 based

    public long javaSrcSent;

    /** does not include the PREPEND string. */
    protected static final int HEADER_SIZE = (8 * 1) + (4 * 2) + (2 * 1) + (1 * 6); // see properties above

    public static byte getType(byte[] packet) {
        return packet[PREPEND.length() + 2];
    }

    public static short getSize(byte[] packet) {
        ByteBuffer bb = ByteBuffer.wrap(packet);
        bb.position(PREPEND.length());
        return bb.getShort();
    }

    /** If sending, the java based sent timestamps will be updated. */
    public abstract byte[] getBytes(boolean sending);

    public final byte[] getBytes() { return getBytes(false); }

    Packet(byte[] bytes) {
        ByteBuffer bb = ByteBuffer.wrap(bytes);
        bb.position(PREPEND.length());
        size = bb.getShort();
        type = bb.get();
        src = bb.get();
        dest = bb.get();
        pktIndex = bb.getInt();
        msgIndex = bb.getInt();
        msgPart = bb.get();
        msgParts = bb.get();
        attempt = bb.get();
        javaSrcSent = bb.getLong();
    }

    public abstract int marginalHeaderSize();
    public abstract int payloadSize();

    /** without the PREPEND string */
    public final int headerSize() {
        return HEADER_SIZE + marginalHeaderSize();
    }

    /** This size includes the PREPEND string. It also has the side effect of
     * updating the size attribute of the packet header. */
    public final int size() {
        size = (short) (PREPEND.length() + headerSize() + payloadSize());
        return size;
    }

    protected final ByteBuffer getBuffer(boolean updateJavaSrcSent) {
        return ByteBuffer.allocate(size())
                .put(PREPEND.getBytes())
                .putShort(size)
                .put(type)
                .put(src)
                .put(dest)
                .putInt(pktIndex)
                .putInt(msgIndex)
                .put(msgPart)
                .put(msgParts)
                .put(attempt)
                .putLong(updateJavaSrcSent
                        ? javaSrcSent = System.nanoTime()
                        : javaSrcSent);
    }

    Packet() {}

    Packet(Packet p) {
        size = p.size;
        type = p.type;
        src = p.src;
        dest = p.dest;
        pktIndex = p.pktIndex;
        msgIndex = p.msgIndex;
        msgPart = p.msgPart;
        msgParts = p.msgParts;
        attempt = p.attempt;
        javaSrcSent = p.javaSrcSent;
    }

//    @Override
//    public final String toString() {
//        int i = 0;
//        byte[] bytes = getBytes(false);
//        for (byte b : bytes) {
//            if (b == 0) {
//                break;
//            }
//            i++;
//        }
//        return new String(Arrays.copyOfRange(bytes, 0, i));
//    }
}
