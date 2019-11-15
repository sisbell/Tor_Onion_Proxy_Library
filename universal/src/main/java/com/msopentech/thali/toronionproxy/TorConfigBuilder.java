/*
Copyright (c) Microsoft Open Technologies, Inc.
All Rights Reserved
Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0

THIS CODE IS PROVIDED ON AN *AS IS* BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, EITHER EXPRESS OR IMPLIED,
INCLUDING WITHOUT LIMITATION ANY IMPLIED WARRANTIES OR CONDITIONS OF TITLE, FITNESS FOR A PARTICULAR PURPOSE,
MERCHANTABLITY OR NON-INFRINGEMENT.

See the Apache 2 License for the specific language governing permissions and limitations under the License.
*/
package com.msopentech.thali.toronionproxy;

import java.io.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public final class TorConfigBuilder {

    private final TorSettings settings;
    private final OnionProxyContext context;

    private StringBuffer buffer = new StringBuffer();

    public TorConfigBuilder(OnionProxyContext context) {
        this.settings = context.getSettings();
        this.context = context;
    }

    /**
     * Updates the tor config for all methods annotated with SettingsConfig
     */
    public TorConfigBuilder updateTorConfig() throws Exception {
        for (Method method : getClass().getMethods()) {
            for (Annotation annotation : method.getAnnotations()) {
                if (annotation instanceof SettingsConfig) {
                    method.invoke(this);
                    break;
                }
            }
        }
        return this;
    }

    private static boolean isNullOrEmpty(String value) {
        return value == null || value.isEmpty();
    }

    private static boolean isLocalPortOpen(int port) {
        Socket socket = null;
        try {
            socket = new Socket();
            socket.connect(new InetSocketAddress("127.0.0.1", port), 500);
            return true;
        } catch (Exception e) {
            return false;
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (Exception ee) {
                }
            }
        }
    }

    public String asString() {
        return buffer.toString();
    }

    public TorConfigBuilder automapHostsOnResolve() {
        return writeTrueProperty("AutomapHostsOnResolve");
    }

    @SettingsConfig
    public TorConfigBuilder automapHostsOnResolveFromSettings() {
        return settings.isAutomapHostsOnResolve() ? automapHostsOnResolve() : this;
    }

    public TorConfigBuilder bridge(String type, String config) {
        if (!isNullOrEmpty(type) && !isNullOrEmpty(config)) {
            buffer.append("Bridge ").append(type).append(' ').append(config).append('\n');
        }
        return this;
    }

    public TorConfigBuilder bridgeCustom(String config) {
        if (!isNullOrEmpty(config)) {
            writeLine("Bridge", config);
        }
        return this;
    }

    public TorConfigBuilder clientTransportPlugins(List<String> supportedBridges, String clientPath) {
        if (supportedBridges == null || supportedBridges.isEmpty()) {
            return this;
        }
        String[] a = new String[supportedBridges.size() + 3];
        a[0] = "ClientTransportPlugin";
        for (int i = 1; i < a.length - 2; i++) {
            a[i] = supportedBridges.get(i - 1);
        }
        a[a.length - 2] = "exec";
        a[a.length - 1] = clientPath;
        return writeLine(a);
    }

    public TorConfigBuilder configurePluggableTransportsFromSettings(File pluggableTransportClient) throws IOException {
        List<String> supportedBridges = settings.getListOfSupportedBridges();
        if (pluggableTransportClient == null || !settings.hasBridges() || supportedBridges.size() < 1) {
            return this;
        }

        if (!pluggableTransportClient.exists() || !pluggableTransportClient.canExecute()) {
            throw new IOException("Bridge binary does not exist: " + pluggableTransportClient
                    .getCanonicalPath());
        }
        clientTransportPlugins(supportedBridges, pluggableTransportClient.getCanonicalPath());

        String type = supportedBridges.contains("meek") ? "meek_lite" : "obfs4";
        addBridgesFromResources(type, 2);
        return this;
    }

    public TorConfigBuilder cookieAuthentication() {
        return writeTrueProperty("CookieAuthentication")
                .writeLine("CookieAuthFile", context.getConfig().getCookieAuthFile().getAbsolutePath());
    }

    @SettingsConfig
    public TorConfigBuilder cookieAuthenticationFromSettings() {
        return settings.hasCookieAuthentication() ? cookieAuthentication() : this;
    }

    public TorConfigBuilder connectionPadding() {
        return writeTrueProperty("ConnectionPadding");
    }

    @SettingsConfig
    public TorConfigBuilder connectionPaddingFromSettings() {
        return settings.hasConnectionPadding() ? connectionPadding() : this;
    }

    public TorConfigBuilder controlPortWriteToFile(String controlPortFile) {
        return writeLine("ControlPortWriteToFile", controlPortFile).writeLine("ControlPort auto");
    }

    @SettingsConfig
    public TorConfigBuilder controlPortWriteToFileFromConfig() {
        return controlPortWriteToFile(context.config.getControlPortFile().getAbsolutePath());
    }

    public TorConfigBuilder debugLogs() {
        writeLine("Log debug syslog");
        writeLine("Log info syslog");
        return writeFalseProperty("SafeLogging");
    }

    @SettingsConfig
    public TorConfigBuilder debugLogsFromSettings() {
        return settings.hasDebugLogs() ? debugLogs() : this;
    }

    public TorConfigBuilder disableNetwork() {
        return writeTrueProperty("DisableNetwork");
    }

    @SettingsConfig
    public TorConfigBuilder disableNetworkFromSettings() {
        return settings.disableNetwork() ? disableNetwork() : this;
    }

    public TorConfigBuilder dnsPort(String dnsPort) {
        return writeLine("DNSPort", dnsPort);
    }

    @SettingsConfig
    public TorConfigBuilder dnsPortFromSettings() {
        return dnsPort(settings.dnsPort());
    }

    public TorConfigBuilder dontUseBridges() {
        return writeFalseProperty("UseBridges");
    }

    public TorConfigBuilder entryNodes(String entryNodes) {
        if (!isNullOrEmpty(entryNodes))
            writeLine("EntryNodes", entryNodes);
        return this;
    }

    public TorConfigBuilder excludeNodes(String excludeNodes) {
        if (!isNullOrEmpty(excludeNodes))
            writeLine("ExcludeNodes", excludeNodes);
        return this;
    }

    public TorConfigBuilder exitNodes(String exitNodes) {
        if (!isNullOrEmpty(exitNodes))
            writeLine("ExitNodes", exitNodes);
        return this;
    }

    public TorConfigBuilder geoIpFile(String path) {
        if (!isNullOrEmpty(path))
            writeLine("GeoIPFile", path);
        return this;
    }

    public TorConfigBuilder geoIpV6File(String path) {
        if (!isNullOrEmpty(path))
            writeLine("GeoIPv6File", path);
        return this;
    }

    public TorConfigBuilder httpTunnelPort(int port, String isolationFlags) {
        buffer.append("HTTPTunnelPort ").append(port);
        if (!isNullOrEmpty(isolationFlags)) {
            buffer.append(" ").append(isolationFlags);
        }
        buffer.append('\n');
        return this;
    }

    @SettingsConfig
    public TorConfigBuilder httpTunnelPortFromSettings() {
        return httpTunnelPort(settings.getHttpTunnelPort(),
                settings.hasIsolationAddressFlagForTunnel() ? "IsolateDestAddr" : null);
    }

    public TorConfigBuilder makeNonExitRelay(String dnsFile, int orPort, String nickname) {
        writeLine("ServerDNSResolvConfFile", dnsFile);
        writeAddress("ORPort", null, orPort, null);
        writeLine("Nickname", nickname);
        return writeLine("ExitPolicy reject *:*");
    }

    /**
     * Sets the entry/exit/exclude nodes
     */
    @SettingsConfig
    public TorConfigBuilder nodesFromSettings() {
        entryNodes(settings.getEntryNodes()).exitNodes(settings.getExitNodes())
                .excludeNodes(settings.getExcludeNodes());
        return this;
    }

    /**
     * Adds non exit relay to builder. This method uses a default google nameserver.
     */
    @SettingsConfig
    public TorConfigBuilder nonExitRelayFromSettings() {
        if (!settings.hasReachableAddress() && !settings.hasBridges() && settings.isRelay()) {
            try {
                File resolv = context.createGoogleNameserverFile();
                makeNonExitRelay(resolv.getCanonicalPath(), settings.getRelayPort(), settings
                        .getRelayNickname());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return this;
    }

    public TorConfigBuilder proxyOnAllInterfaces() {
        return writeLine("SocksListenAddress 0.0.0.0");
    }

    @SettingsConfig
    public TorConfigBuilder proxyOnAllInterfacesFromSettings() {
        return settings.hasOpenProxyOnAllInterfaces() ? proxyOnAllInterfaces() : this;
    }

    /**
     * Set socks5 proxy with no authentication. This can be set if yo uare using a VPN.
     */
    public TorConfigBuilder proxySocks5(String host, String port) {
        buffer.append("socks5Proxy ").append(host).append(':').append(port).append('\n');
        return this;
    }

    @SettingsConfig
    public TorConfigBuilder proxySocks5FromSettings() {
        return (settings.useSocks5() && !settings.hasBridges()) ? proxySocks5(settings
                        .getProxySocks5Host(),
                settings.getProxySocks5ServerPort()) : this;
    }

    /**
     * Sets proxyWithAuthentication information. If proxyType, proxyHost or proxyPort is empty,
     * then this method does nothing.
     */
    public TorConfigBuilder proxyWithAuthentication(String proxyType, String proxyHost, String
            proxyPort, String proxyUser, String proxyPass) {
        if (!isNullOrEmpty(proxyType) && !isNullOrEmpty(proxyHost) && !isNullOrEmpty(proxyPort)) {
            buffer.append(proxyType).append("Proxy ").append(proxyHost).append(':').append
                    (proxyPort).append('\n');

            if (proxyUser != null && proxyPass != null) {
                if (proxyType.equalsIgnoreCase("socks5")) {
                    writeLine("Socks5ProxyUsername", proxyUser);
                    writeLine("Socks5ProxyPassword", proxyPass);
                } else {
                    buffer.append(proxyType).append("ProxyAuthenticator ").append(proxyUser)
                            .append(':').append(proxyPort).append('\n');
                }
            } else if (proxyPass != null) {
                buffer.append(proxyType).append("ProxyAuthenticator ").append(proxyUser)
                        .append(':').append(proxyPort).append('\n');
            }
        }
        return this;
    }

    @SettingsConfig
    public TorConfigBuilder proxyWithAuthenticationFromSettings() {
        return (!settings.useSocks5() && !settings.hasBridges()) ? proxyWithAuthentication
                (settings.getProxyType(), settings.getProxyHost(),
                        settings.getProxyPort(), settings.getProxyUser(), settings
                                .getProxyPassword()) :
                this;
    }

    public TorConfigBuilder reachableAddressPorts(String reachableAddressesPorts) {
        if (!isNullOrEmpty(reachableAddressesPorts))
            writeLine("ReachableAddresses", reachableAddressesPorts);
        return this;
    }

    @SettingsConfig
    public TorConfigBuilder reachableAddressesFromSettings() {
        return settings.hasReachableAddress() ? reachableAddressPorts(settings
                .getReachableAddressPorts()) : this;

    }

    public TorConfigBuilder reducedConnectionPadding() {
        return writeTrueProperty("ReducedConnectionPadding");
    }

    @SettingsConfig
    public TorConfigBuilder reducedConnectionPaddingFromSettings() {
        return settings.hasReducedConnectionPadding() ? reducedConnectionPadding() : this;
    }

    public void reset() {
        buffer = new StringBuffer();
    }

    @SettingsConfig
    public TorConfigBuilder runAsDaemonFromSettings() {
        return settings.runAsDaemon() ? runAsDaemon() : this;
    }

    public TorConfigBuilder runAsDaemon() {
        return writeTrueProperty("RunAsDaemon");
    }

    public TorConfigBuilder safeSocksDisable() {
        return writeFalseProperty("SafeSocks");
    }

    public TorConfigBuilder safeSocksEnable() {
        return writeTrueProperty("SafeSocks");
    }

    @SettingsConfig
    public TorConfigBuilder safeSocksFromSettings() {
        return !settings.hasSafeSocks() ? safeSocksDisable() : safeSocksEnable();
    }

    public TorConfigBuilder setGeoIpFiles() throws IOException {
        TorConfig torConfig = context.getConfig();
        if (torConfig.getGeoIpFile().exists()) {
            geoIpFile(torConfig.getGeoIpFile().getCanonicalPath())
                    .geoIpV6File(torConfig.getGeoIpv6File().getCanonicalPath());
        }
        return this;
    }

    public TorConfigBuilder socksPort(String socksPort, String isolationFlag) {
        if (isNullOrEmpty(socksPort)) {
            return this;
        }
        buffer.append("SOCKSPort ").append(socksPort);
        if (!isNullOrEmpty(isolationFlag)) {
            buffer.append(" ").append(isolationFlag);
        }
        buffer.append(" KeepAliveIsolateSOCKSAuth");
        buffer.append(" IPv6Traffic");
        buffer.append(" PreferIPv6");

        buffer.append('\n');
        return this;
    }

    @SettingsConfig
    public TorConfigBuilder socksPortFromSettings() {
        String socksPort = settings.getSocksPort();
        if (socksPort.indexOf(':') != -1) {
            socksPort = socksPort.split(":")[1];
        }

        if (!socksPort.equalsIgnoreCase("auto") && isLocalPortOpen(Integer.parseInt(socksPort))) {
            socksPort = "auto";
        }
        return socksPort(socksPort, settings.hasIsolationAddressFlagForTunnel() ?
                "IsolateDestAddr" : null);
    }

    public TorConfigBuilder strictNodesDisable() {
        return writeFalseProperty("StrictNodes");
    }

    public TorConfigBuilder strictNodesEnable() {
        return writeTrueProperty("StrictNodes");
    }

    @SettingsConfig
    public TorConfigBuilder strictNodesFromSettings() {
        return settings.hasStrictNodes() ? strictNodesEnable() : strictNodesDisable();
    }

    public TorConfigBuilder testSocksDisable() {
        return writeFalseProperty("TestSocks");
    }

    public TorConfigBuilder testSocksEnable() {
        return writeTrueProperty("TestSocks");
    }

    @SettingsConfig
    public TorConfigBuilder testSocksFromSettings() {
        return !settings.hasTestSocks() ? testSocksDisable() : this;
    }

    @SettingsConfig
    public TorConfigBuilder torrcCustomFromSettings() throws UnsupportedEncodingException {
        return settings.getCustomTorrc() != null ?
                writeLine(new String(settings.getCustomTorrc().getBytes("US-ASCII"))) : this;
    }

    public TorConfigBuilder transPort(String transPort) {
        if (!isNullOrEmpty(transPort))
            writeLine("TransPort", transPort);
        return this;
    }

    @SettingsConfig
    public TorConfigBuilder transPortFromSettings() {
        return transPort(settings.transPort());
    }

    public TorConfigBuilder useBridges() {
        return writeTrueProperty("UseBridges");
    }

    @SettingsConfig
    public TorConfigBuilder useBridgesFromSettings() {
        return !settings.hasBridges() ? dontUseBridges() : this;
    }

    public TorConfigBuilder virtualAddressNetwork(String address) {
        if (!isNullOrEmpty(address))
            writeLine("VirtualAddrNetwork", address);
        return this;
    }

    @SettingsConfig
    public TorConfigBuilder virtualAddressNetworkFromSettings() {
        return virtualAddressNetwork(settings.getVirtualAddressNetwork());
    }

    public TorConfigBuilder writeAddress(String fieldName, String address, Integer port, String flags) {
        if (isNullOrEmpty(address) && port == null) {
            return this;
        }
        buffer.append(fieldName).append(" ");
        if (!isNullOrEmpty(address)) {
            buffer.append(address).append(":");
        }
        if (port != null && port >= 0) {
            buffer.append(port);
        } else {
            buffer.append("auto");
        }
        if (!isNullOrEmpty(flags)) {
            buffer.append(" ").append(flags);
        }
        buffer.append('\n');
        return this;
    }

    public TorConfigBuilder writeFalseProperty(String name) {
        return writeLine(name, "0");
    }

    public TorConfigBuilder writeLine(String... values) {
        StringBuilder sb = new StringBuilder();
        if (values != null) {
            for (String v : values) {
                sb.append(v).append(" ");
            }
        }
        if (sb.length() > 0) {
            buffer.append(sb.toString().trim()).append("\n");
        }
        return this;
    }

    public TorConfigBuilder writeTrueProperty(String name) {
        return writeLine(name, "1");
    }

    /**
     * Adds bridges from a resource stream. This relies on the TorInstaller to know how to obtain this stream.
     * These entries may be type-specified like:
     *
     * <code>
     * obfs3 169.229.59.74:31493 AF9F66B7B04F8FF6F32D455F05135250A16543C9
     * </code>
     * <p>
     * Or it may just be a custom entry like
     *
     * <code>
     * 69.163.45.129:443 9F090DE98CA6F67DEEB1F87EFE7C1BFD884E6E2F
     * </code>
     */
    TorConfigBuilder addBridgesFromResources(String type, int maxBridges) throws IOException {
        if (settings.hasBridges()) {
            InputStream bridgesStream = context.getInstaller().openBridgesStream();
            int formatType = bridgesStream.read();
            if (formatType == 0) {
                addBridges(bridgesStream, type, maxBridges);
            } else {
                addCustomBridges(bridgesStream);
            }
        }
        return this;
    }

    /**
     * Add bridges from bridges.txt file.
     */
    private void addBridges(InputStream input, String bridgeType, int maxBridges) {
        if (input == null || isNullOrEmpty(bridgeType) || maxBridges < 1) {
            return;
        }
        boolean hasAddedBridge = false;
        List<Bridge> bridges = readBridgesFromStream(input);
        Collections.shuffle(bridges, new Random(System.nanoTime()));
        int bridgeCount = 0;
        for (Bridge b : bridges) {
            if (b.type.equals(bridgeType)) {
                bridge(b.type, b.config);
                hasAddedBridge = true;
                if (++bridgeCount > maxBridges)
                    break;
            }
        }
        if (hasAddedBridge) useBridges();
    }

    /**
     * Add custom bridges defined by the user. These will have a bridgeType of 'custom' as the first field.
     */
    private void addCustomBridges(InputStream input) {
        if (input == null) {
            return;
        }
        boolean hasAddedBridge = false;
        List<Bridge> bridges = readCustomBridgesFromStream(input);
        for (Bridge b : bridges) {
            if (b.type.equals("custom")) {
                bridgeCustom(b.config);
                hasAddedBridge = true;
            }
        }
        if (hasAddedBridge) useBridges();
    }

    private static List<Bridge> readBridgesFromStream(InputStream input) {
        List<Bridge> bridges = new ArrayList<>();
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(input, "UTF-8"));
            for (String line = br.readLine(); line != null; line = br.readLine()) {
                String[] tokens = line.split("\\s+", 2);
                if (tokens.length != 2) {
                    continue;//bad entry
                }
                bridges.add(new Bridge(tokens[0], tokens[1]));
            }
            br.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return bridges;
    }

    private static List<Bridge> readCustomBridgesFromStream(InputStream input) {
        List<Bridge> bridges = new ArrayList<>();
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(input, "UTF-8"));
            for (String line = br.readLine(); line != null; line = br.readLine()) {
                if (line.isEmpty()) {
                    continue;
                }
                bridges.add(new Bridge("custom", line));
            }
            br.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return bridges;
    }

    private static class Bridge {
        final String type;
        final String config;

        public Bridge(String type, String config) {
            this.type = type;
            this.config = config;
        }
    }
}
