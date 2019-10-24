package com.msopentech.thali.toronionproxy;

import java.util.List;

public interface TorSettings {
    boolean disableNetwork();

    List<String> getCustomBridges();

    String getCustomTorrc();

    String getDnsHost();

    Integer getDnsPort();

    String getEntryNodes();

    String getExcludeNodes();

    String getExitNodes();

    String getHttpTunnelHost();

    Integer getHttpTunnelPort();

    List<BridgeType> getBridgeTypes();

    String getProxyHost();

    String getProxyPassword();

    Integer getProxyPort();

    String getProxySocks5Host();

    Integer getProxySocks5ServerPort();

    String getProxyType();

    String getProxyUser();

    String getReachableAddressPorts();

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
