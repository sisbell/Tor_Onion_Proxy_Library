package com.msopentech.thali.torsettings;

import java.io.File;

public final class TorConfigFiles {

    private final File controlPortFile;

    private final File cookieAuthFile;

    private final File geoIpFile;

    private final File geoIpV6File;

    private final File nameserverFile;

    public TorConfigFiles(File controlPortFile, File cookieAuthFile,
                          File geoIpFile, File geoIpV6File, File nameserverFile) {
        this.controlPortFile = controlPortFile;
        this.cookieAuthFile = cookieAuthFile;
        this.geoIpFile = geoIpFile;
        this.geoIpV6File = geoIpV6File;
        this.nameserverFile = nameserverFile;
    }

    public File getControlPortFile() {
        return controlPortFile;
    }

    public File getCookieAuthFile() {
        return cookieAuthFile;
    }

    public File getGeoIpFile() {
        return geoIpFile;
    }

    public File getGeoIpV6File() {
        return geoIpV6File;
    }

    public File getNameserverFile() {
        return nameserverFile;
    }
}
