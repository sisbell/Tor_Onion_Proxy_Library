package com.msopentech.thali.torsettings;

import java.util.Set;

public interface TorSettings {
    boolean disableNetwork();

    Set<String> getCustomBridges();

    String getCustomTorrc();

    String getDnsHost();

    Integer getDnsPort();

    Set<String> getEntryNodes();

    Set<String> getExcludeNodes();

    Set<String> getExitNodes();

    String getHttpTunnelHost();

    Integer getHttpTunnelPort();

    Set<BridgeType> getBridgeTypes();

    String getProxyHost();

    String getProxyPassword();

    Integer getProxyPort();

    String getProxySocks5Host();

    Integer getProxySocks5ServerPort();

    String getProxyType();

    String getProxyUser();

    Set<String> getReachableAddressPorts();

    String getRelayNickname();

    Integer getRelayPort();

    String getSocksPort();

    String getTransparentProxyAddress();

    Integer getTransparentProxyPort();

    String getVirtualAddressNetwork();

    boolean hasBridges();

    boolean hasConnectionPadding();

    boolean hasCookieAuthentication();

    boolean hasDebugLogs();

    boolean hasIsolationAddressFlagForTunnel();

    boolean hasOpenProxyOnAllInterfaces();

    boolean hasReachableAddress();

    boolean hasReducedConnectionPadding();

    boolean hasSafeSocks();

    boolean hasStrictNodes();

    boolean hasTestSocks();

    boolean isAutomapHostsOnResolve();

    boolean isRelay();

    boolean runAsDaemon();

    boolean useSocks5();
}
