package com.essentiallocalization.connection;

/**
 * Created by Jake on 2/5/14.
 */
public interface PacketList {
    DataPacket findPacket(byte src, byte dest, int pktIndex, long javaSrcSent);
}
