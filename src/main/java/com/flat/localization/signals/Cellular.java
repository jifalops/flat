package com.flat.localization.signals;

import android.content.Context;
import android.telephony.CellInfo;
import android.telephony.CellInfoCdma;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoWcdma;
import android.telephony.CellLocation;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.telephony.cdma.CdmaCellLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * Reminder: Android's NETWORK_PROVIDER for the LocationManager incorporates both cellular and wifi signals into its algorithm.
 *
 * This class is used mainly for getting available signal strengths from surrounding cellular towers.
 * However, CDMA phones have more potential here because CDMA base stations have latitude and longitude available.
 *
 * Created by Jacob Phillips (09/2014)
 */
public final class Cellular extends AbstractSignal {

    public static final int EVENT_CELL_INFOS = 1;
    public static final int EVENT_CELL_LOCATION = 2;
    public static final int EVENT_CELL_STRENGTH = 3;

    private boolean enabled;

    private int phoneType;

    /*
     * Simple Singleton
     */
    private Cellular() { super("Cellular"); }
    private static final Cellular instance = new Cellular();
    public static Cellular getInstance() { return instance; }


    @Override
    public void enable(Context ctx) {
        TelephonyManager manager = (TelephonyManager) ctx.getSystemService(Context.TELEPHONY_SERVICE);
        phoneType = manager.getPhoneType();
        manager.listen(cellListener,
                PhoneStateListener.LISTEN_CELL_INFO |
                PhoneStateListener.LISTEN_CELL_LOCATION |
                PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
        enabled = true;
    }

    @Override
    public void disable(Context ctx) {
        TelephonyManager manager = (TelephonyManager) ctx.getSystemService(Context.TELEPHONY_SERVICE);
        manager.listen(cellListener, PhoneStateListener.LISTEN_NONE);
        enabled = false;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    private final PhoneStateListener cellListener = new PhoneStateListener() {
        @Override
        public void onCellInfoChanged(List<CellInfo> cellInfo) {
            CellLocationInfo.Params p;
            List<CellLocationInfo> cells = new ArrayList<CellLocationInfo>(cellInfo.size());
            for (CellInfo info : cellInfo) {
                p = new CellLocationInfo.Params();
                p.time = info.getTimeStamp();
                if (info instanceof CellInfoCdma) {
                    CellInfoCdma cdma = (CellInfoCdma) info;
                    p.id = cdma.getCellIdentity().getBasestationId();
                    p.lat = cdma.getCellIdentity().getLatitude();
                    p.lon = cdma.getCellIdentity().getLongitude();
                    p.dbm = cdma.getCellSignalStrength().getDbm(); // there's two other dbm methods
                    p.asu = cdma.getCellSignalStrength().getAsuLevel();
                } else if (info instanceof CellInfoGsm) {
                    CellInfoGsm gsm = (CellInfoGsm) info;
                    p.id = gsm.getCellIdentity().getCid();
                    p.dbm = gsm.getCellSignalStrength().getDbm();
                    p.asu = gsm.getCellSignalStrength().getAsuLevel();
                } else if (info instanceof CellInfoLte) {
                    CellInfoLte lte = (CellInfoLte) info;
                    p.id = lte.getCellIdentity().getCi();
                    p.dbm = lte.getCellSignalStrength().getDbm();
                    p.asu = lte.getCellSignalStrength().getAsuLevel();
                } else if (info instanceof CellInfoWcdma) {
                    CellInfoWcdma wcdma = (CellInfoWcdma) info;
                    p.id = wcdma.getCellIdentity().getCid();
                    p.dbm = wcdma.getCellSignalStrength().getDbm();
                    p.asu = wcdma.getCellSignalStrength().getAsuLevel();
                }
                cells.add(new CellLocationInfo(p));
            }
            cellInfos = cells.toArray(new CellLocationInfo[cells.size()]);
            notifyListeners(EVENT_CELL_INFOS);
        }

        @Override
        public void onCellLocationChanged(CellLocation location) {
            if (phoneType == TelephonyManager.PHONE_TYPE_CDMA) {
                CdmaCellLocation cdma = (CdmaCellLocation) location;
                CellLocationInfo.Params p = new CellLocationInfo.Params(baseStation);
                p.id = cdma.getBaseStationId();
                p.lat = cdma.getBaseStationLatitude();
                p.lon = cdma.getBaseStationLongitude();
                baseStation = new CellLocationInfo(p);
                notifyListeners(EVENT_CELL_LOCATION);
            }
        }

        @Override
        public void onSignalStrengthsChanged(SignalStrength signalStrength) {
            CellLocationInfo.Params p = new CellLocationInfo.Params(baseStation);
            p.time = System.nanoTime();
            if (signalStrength.isGsm()) {
                p.asu = signalStrength.getGsmSignalStrength();
            } else {
                p.dbm = signalStrength.getCdmaDbm();
            }
            baseStation = new CellLocationInfo(p);
            notifyListeners(EVENT_CELL_STRENGTH);
        }
    };

    public static final class CellLocationInfo {
        private static final class Params {
            int id = -1;
            int lat = Integer.MIN_VALUE;
            int lon = Integer.MIN_VALUE;
            int dbm = Integer.MIN_VALUE;
            int asu = Integer.MIN_VALUE;
            long time = Long.MIN_VALUE;
            Params() {}
            Params(CellLocationInfo src) {
                if (src != null) {
                    id = src.id;
                    lat = src.lat;
                    lon = src.lon;
                    dbm = src.dbm;
                    asu = src.asu;
                    time = src.time;
                }
            }
        }
        public final int id;
        /** Only works for CDMA phones */
        public final int lat;
        /** Only works for CDMA phones */
        public final int lon;
        public final long time;
        public final int dbm;
        public final int asu;
        public CellLocationInfo(Params p) {
            id = p.id;
            lat = p.lat;
            lon = p.lon;
            dbm = p.dbm;
            asu = p.asu;
            time = p.time;
        }
    }

    private CellLocationInfo baseStation;
    private CellLocationInfo[] cellInfos;

    public CellLocationInfo getBaseStationInfo() { return baseStation; }
    public CellLocationInfo[] getCellLocationInfo() { return cellInfos; }

}
