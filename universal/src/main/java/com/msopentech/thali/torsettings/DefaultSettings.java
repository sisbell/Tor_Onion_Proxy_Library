package com.msopentech.thali.torsettings;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Provides some reasonable default settings. Override this class or create a new implementation to
 * make changes.
 */
public class DefaultSettings implements TorSettings {
    @Override
    public boolean disableNetwork() {
        return true;
    }

    public String getDnsHost() {
        return null;
    }

    @Override
    public Integer getDnsPort() {
        return 5400;
    }

    @Override
    public Set<String> getCustomBridges() {
        return Collections.emptySet();
    }

    @Override
    public String getCustomTorrc() {
        return null;
    }

    @Override
    public Set<String> getEntryNodes() {
        return Collections.emptySet();
    }

    @Override
    public Set<String> getExcludeNodes() {
        return Collections.emptySet();
    }

    @Override
    public Set<String> getExitNodes() {
        return Collections.emptySet();
    }

    @Override
    public String getHttpTunnelHost() {
        return null;
    }

    @Override
    public Integer getHttpTunnelPort() {
        return null;
    }

    @Override
    public Set<BridgeType> getBridgeTypes() {
        return Collections.emptySet();
    }

    @Override
    public String getProxyHost() {
        return null;
    }

    @Override
    public String getProxyPassword() {
        return null;
    }

    @Override
    public Integer getProxyPort() {
        return null;
    }

    @Override
    public String getProxySocks5Host() {
        return null;
    }

    @Override
    public Integer getProxySocks5ServerPort() {
        return null;
    }

    @Override
    public String getProxyType() {
        return null;
    }

    @Override
    public String getProxyUser() {
        return null;
    }

    @Override
    public Set<String> getReachableAddressPorts() {
        Set<String> ports = new HashSet<>();
        ports.add("*:80");
        ports.add("*:443");
        return ports;
    }

    @Override
    public String getRelayNickname() {
        return null;
    }

    @Override
    public Integer getRelayPort() {
        return 9001;
    }

    @Override
    public String getSocksPort() {
        return "9050";
    }

    @Override
    public String getVirtualAddressNetwork() {
        return null;
    }

    @Override
    public boolean hasBridges() {
        return false;
    }

    @Override
    public boolean hasConnectionPadding() {
        return false;
    }

    @Override
    public boolean hasCookieAuthentication() {
        return true;
    }

    @Override
    public boolean hasDebugLogs() {
        return false;
    }

    @Override
    public boolean hasIsolationAddressFlagForTunnel() {
        return false;
    }

    @Override
    public boolean hasOpenProxyOnAllInterfaces() {
        return false;
    }

    @Override
    public boolean hasReachableAddress() {
        return false;
    }

    @Override
    public boolean hasReducedConnectionPadding() {
        return true;
    }

    @Override
    public boolean hasSafeSocks() {
        return false;
    }

    @Override
    public boolean hasStrictNodes() {
        return false;
    }

    @Override
    public boolean hasTestSocks() {
        return false;
    }

    @Override
    public boolean isAutomapHostsOnResolve() {
        return false;
    }

    @Override
    public boolean isRelay() {
        return false;
    }

    @Override
    public boolean runAsDaemon() {
        return true;
    }

    @Override
    public String getTransparentProxyAddress() {
        return null;
    }

    @Override
    public Integer getTransparentProxyPort() {
        return null;
    }

    @Override
    public boolean useSocks5() {
        return false;
    }
}
