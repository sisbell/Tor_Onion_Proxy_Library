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
package com.msopentech.thali.torsettings;

import java.io.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class TorSettingsBuilder {

    private final TorSettings settings;

    private File controlPortFile;

    private File cookieAuthFile;

    private File geoIpFile;

    private File geoIpV6File;

    private File nameserverFile;

    private final InputStream input;

    private StringBuffer buffer = new StringBuffer();

    public TorSettingsBuilder(TorSettings torSettings, InputStream input, TorConfigFiles configFiles) {
        this.settings = torSettings;
        this.input = input;
        if(configFiles != null) {
            this.controlPortFile = configFiles.getControlPortFile();
            this.cookieAuthFile = configFiles.getCookieAuthFile();
            this.geoIpFile = configFiles.getGeoIpFile();
            this.geoIpV6File = configFiles.getGeoIpV6File();
            this.nameserverFile = configFiles.getNameserverFile();
        }
    }

    /**
     * Updates the tor config for all methods annotated with SettingsConfig
     */
    public TorSettingsBuilder updateTorSettings() throws Exception {
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

    private static boolean isNullOrEmpty(Set value) {
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

    public TorSettingsBuilder automapHostsOnResolve() {
        return writeTrueProperty("AutomapHostsOnResolve");
    }

    @SettingsConfig
    public TorSettingsBuilder automapHostsOnResolveFromSettings() {
        return settings.isAutomapHostsOnResolve() ? automapHostsOnResolve() : this;
    }

    public TorSettingsBuilder bridge(String type, String config) {
        if (!isNullOrEmpty(type) && !isNullOrEmpty(config)) {
            buffer.append("Bridge ").append(type).append(' ').append(config).append('\n');
        }
        return this;
    }

    public TorSettingsBuilder configurePluggableTransports(File pluggableTransportClient,
                                                           Set<BridgeType> bridgeTypes)
            throws IOException {
        if (pluggableTransportClient == null || bridgeTypes == null || bridgeTypes.isEmpty()) {
            return this;
        }
        if (!pluggableTransportClient.exists()) {
            throw new IOException("Bridge binary does not exist: " + pluggableTransportClient
                    .getCanonicalPath());
        }

        if (!pluggableTransportClient.canExecute()) {
            throw new IOException("Bridge binary is not executable: " + pluggableTransportClient
                    .getCanonicalPath());
        }
        for (BridgeType bridgeType : bridgeTypes) {
            writeBridgeTransport(pluggableTransportClient, bridgeType);
        }
        return this;
    }

    public TorSettingsBuilder cookieAuthentication() {
        if(cookieAuthFile != null) {
            return writeTrueProperty("CookieAuthentication")
                    .writeLine("CookieAuthFile", cookieAuthFile.getAbsolutePath());
        }
        return this;
    }

    @SettingsConfig
    public TorSettingsBuilder cookieAuthenticationFromSettings() {
        return settings.hasCookieAuthentication() ? cookieAuthentication() : this;
    }

    public TorSettingsBuilder connectionPadding() {
        return writeTrueProperty("ConnectionPadding");
    }

    @SettingsConfig
    public TorSettingsBuilder connectionPaddingFromSettings() {
        return settings.hasConnectionPadding() ? connectionPadding() : this;
    }

    public TorSettingsBuilder controlPortWriteToFile(String controlPortFile) {
        return writeLine("ControlPortWriteToFile", controlPortFile).writeLine("ControlPort auto");
    }

    @SettingsConfig
    public TorSettingsBuilder controlPortWriteToFileFromConfig() {
        if(controlPortFile != null) {
            return controlPortWriteToFile(controlPortFile.getAbsolutePath());
        }
        return this;
    }

    /**
     * A custom entry looks like
     *
     * <code>
     * 69.163.45.129:443 9F090DE98CA6F67DEEB1F87EFE7C1BFD884E6E2F
     * </code>
     */
    public TorSettingsBuilder customBridges(Set<String> bridges) {
        for (String bridge : bridges) {
            if (!isNullOrEmpty(bridge)) {
                writeLine("Bridge " + bridge);
            }
        }
        return this;
    }

    @SettingsConfig
    public TorSettingsBuilder customBridgesFromSettings() {
        if (!settings.hasBridges() || !hasCustomBridges()) {
            return this;
        }
        return customBridges(settings.getCustomBridges());
    }

    public TorSettingsBuilder debugLogs() {
        writeLine("Log debug syslog");
        writeLine("Log info syslog");
        return writeFalseProperty("SafeLogging");
    }

    @SettingsConfig
    public TorSettingsBuilder debugLogsFromSettings() {
        return settings.hasDebugLogs() ? debugLogs() : this;
    }

    public TorSettingsBuilder disableNetwork() {
        return writeTrueProperty("DisableNetwork");
    }

    @SettingsConfig
    public TorSettingsBuilder disableNetworkFromSettings() {
        return settings.disableNetwork() ? disableNetwork() : this;
    }

    public TorSettingsBuilder dnsPort(String dnsHost, Integer dnsPort) {
        return writeAddress("DNSPort", dnsHost, dnsPort, null);
    }

    @SettingsConfig
    public TorSettingsBuilder dnsPortFromSettings() {
        return dnsPort(settings.getDnsHost(), settings.getDnsPort());
    }

    public TorSettingsBuilder dontUseBridges() {
        return writeFalseProperty("UseBridges");
    }

    public TorSettingsBuilder entryNodes(Set<String> entryNodes) {
        if (!isNullOrEmpty(entryNodes))
            writeLine("EntryNodes", entryNodes);
        return this;
    }

    public TorSettingsBuilder excludeNodes(Set<String> excludeNodes) {
        if (!isNullOrEmpty(excludeNodes))
            writeLine("ExcludeNodes", excludeNodes);
        return this;
    }

    public TorSettingsBuilder exitNodes(Set<String> exitNodes) {
        if (!isNullOrEmpty(exitNodes))
            writeLine("ExitNodes", exitNodes);
        return this;
    }

    public TorSettingsBuilder geoIpFile(String path) {
        if (!isNullOrEmpty(path))
            writeLine("GeoIPFile", path);
        return this;
    }

    public TorSettingsBuilder geoIpV6File(String path) {
        if (!isNullOrEmpty(path))
            writeLine("GeoIPv6File", path);
        return this;
    }

    public TorSettingsBuilder httpTunnelPort(String host, Integer port, String isolationFlags) {
        return writeAddress("HTTPTunnelPort", host, port, isolationFlags);
    }

    @SettingsConfig
    public TorSettingsBuilder httpTunnelPortFromSettings() {
        return httpTunnelPort(settings.getHttpTunnelHost(), settings.getHttpTunnelPort(),
                settings.hasIsolationAddressFlagForTunnel() ? "IsolateDestAddr" : null);
    }

    public TorSettingsBuilder makeNonExitRelay(String dnsFile, int orPort, String nickname) {
        writeLine("ServerDNSResolvConfFile", dnsFile);
        writeLine("ORPort", String.valueOf(orPort));
        writeLine("Nickname", nickname);
        return writeLine("ExitPolicy reject *:*");
    }

    /**
     * Sets the entry/exit/exclude nodes
     */
    @SettingsConfig
    public TorSettingsBuilder nodesFromSettings() {
        entryNodes(settings.getEntryNodes()).exitNodes(settings.getExitNodes())
                .excludeNodes(settings.getExcludeNodes());
        return this;
    }

    /**
     * Adds non exit relay to builder. This method uses a default google nameserver.
     */
    @SettingsConfig
    public TorSettingsBuilder nonExitRelayFromSettings() {
        if (!settings.hasReachableAddress() && !settings.hasBridges() && settings.isRelay()) {
            try {
                File resolv = nameserverFile;
                makeNonExitRelay(resolv.getCanonicalPath(), settings.getRelayPort(), settings
                        .getRelayNickname());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return this;
    }

    /**
     * Write bridges from packaged bridge list if bridges option is enabled and if user has set desired bridge types.
     * <p>
     * If the user has also defined custom bridges, these take precedence and default bridges will not be written.
     */
    @SettingsConfig
    public TorSettingsBuilder defaultBridgesFromSettings() {
        return defaultBridgesFromResources(settings.getBridgeTypes());
    }

    public TorSettingsBuilder proxyOnAllInterfaces() {
        return writeLine("SocksListenAddress 0.0.0.0");
    }

    @SettingsConfig
    public TorSettingsBuilder proxyOnAllInterfacesFromSettings() {
        return settings.hasOpenProxyOnAllInterfaces() ? proxyOnAllInterfaces() : this;
    }

    /**
     * Set socks5 proxy with no authentication. This can be set if you are using a VPN.
     */
    public TorSettingsBuilder proxySocks5(String host, Integer port) {
        buffer.append("socks5Proxy ").append(host).append(':').append(port).append('\n');
        return this;
    }

    @SettingsConfig
    public TorSettingsBuilder proxySocks5FromSettings() {
        return (settings.useSocks5() && !settings.hasBridges()) ? proxySocks5(settings
                        .getProxySocks5Host(),
                settings.getProxySocks5ServerPort()) : this;
    }

    /**
     * Sets proxyWithAuthentication information. If proxyType, proxyHost or proxyPort is empty,
     * then this method does nothing.
     */
    public TorSettingsBuilder proxyWithAuthentication(String proxyType, String proxyHost, Integer
            proxyPort, String proxyUser, String proxyPass) {
        if (!isNullOrEmpty(proxyType) && !isNullOrEmpty(proxyHost) && proxyPort != null) {
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
    public TorSettingsBuilder proxyWithAuthenticationFromSettings() {
        return (!settings.useSocks5() && !settings.hasBridges()) ? proxyWithAuthentication
                (settings.getProxyType(), settings.getProxyHost(),
                        settings.getProxyPort(), settings.getProxyUser(), settings
                                .getProxyPassword()) :
                this;
    }

    public TorSettingsBuilder reachableAddresses(Set<String> reachableAddressesPorts) {
        if (!isNullOrEmpty(reachableAddressesPorts))
            writeLine("ReachableAddresses", reachableAddressesPorts);
        return this;
    }

    @SettingsConfig
    public TorSettingsBuilder reachableAddressesFromSettings() {
        return settings.hasReachableAddress() ? reachableAddresses(settings
                .getReachableAddressPorts()) : this;

    }

    public TorSettingsBuilder reducedConnectionPadding() {
        return writeTrueProperty("ReducedConnectionPadding");
    }

    @SettingsConfig
    public TorSettingsBuilder reducedConnectionPaddingFromSettings() {
        return settings.hasReducedConnectionPadding() ? reducedConnectionPadding() : this;
    }

    public void reset() {
        buffer = new StringBuffer();
    }

    @SettingsConfig
    public TorSettingsBuilder runAsDaemonFromSettings() {
        return settings.runAsDaemon() ? runAsDaemon() : this;
    }

    public TorSettingsBuilder runAsDaemon() {
        return writeTrueProperty("RunAsDaemon");
    }

    public TorSettingsBuilder safeSocksDisable() {
        return writeFalseProperty("SafeSocks");
    }

    public TorSettingsBuilder safeSocksEnable() {
        return writeTrueProperty("SafeSocks");
    }

    @SettingsConfig
    public TorSettingsBuilder safeSocksFromSettings() {
        return settings.hasSafeSocks() ? safeSocksEnable() : this;
    }

    public TorSettingsBuilder setGeoIpFiles() throws IOException {
        if (geoIpFile != null && geoIpFile.exists()) {
            geoIpFile(geoIpFile.getCanonicalPath());
        }
        if (geoIpV6File != null && geoIpV6File.exists()) {
            geoIpV6File(geoIpV6File.getCanonicalPath());
        }
        return this;
    }

    public TorSettingsBuilder socksPort(String socksPort, String isolationFlag) {
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
    public TorSettingsBuilder socksPortFromSettings() {
        String socksPort = settings.getSocksPort();
        if (isNullOrEmpty(socksPort)) {
            return this;
        }
        if (socksPort.indexOf(':') != -1) {
            socksPort = socksPort.split(":")[1];
        }

        if (!socksPort.equalsIgnoreCase("auto") && isLocalPortOpen(Integer.parseInt(socksPort))) {
            socksPort = "auto";
        }
        return socksPort(socksPort, settings.hasIsolationAddressFlagForTunnel() ?
                "IsolateDestAddr" : null);
    }

    public TorSettingsBuilder strictNodesDisable() {
        return writeFalseProperty("StrictNodes");
    }

    public TorSettingsBuilder strictNodesEnable() {
        return writeTrueProperty("StrictNodes");
    }

    @SettingsConfig
    public TorSettingsBuilder strictNodesFromSettings() {
        return settings.hasStrictNodes() ? strictNodesEnable() : this;
    }

    public TorSettingsBuilder testSocksDisable() {
        return writeFalseProperty("TestSocks");
    }

    public TorSettingsBuilder testSocksEnable() {
        return writeTrueProperty("TestSocks");
    }

    @SettingsConfig
    public TorSettingsBuilder testSocksFromSettings() {
        return settings.hasTestSocks() ? testSocksEnable() : this;
    }

    @SettingsConfig
    public TorSettingsBuilder torrcCustomFromSettings() throws UnsupportedEncodingException {
        return settings.getCustomTorrc() != null ?
                writeLine(new String(settings.getCustomTorrc().getBytes("US-ASCII"))) : this;
    }

    public TorSettingsBuilder transparentProxyPort(String address, Integer transPort) {
        return writeAddress("TransPort", address, transPort, null);
    }

    @SettingsConfig
    public TorSettingsBuilder transPortFromSettings() {
        return transparentProxyPort(settings.getTransparentProxyAddress(), settings.getTransparentProxyPort());
    }

    public TorSettingsBuilder transportPluginMeek(String clientPath) {
        return writeLine("ClientTransportPlugin meek_lite exec", clientPath);
    }

    public TorSettingsBuilder transportPluginObfs(String clientPath) {
        return writeLine("ClientTransportPlugin obfs3 exec", clientPath)
                .writeLine("ClientTransportPlugin obfs4 exec", clientPath);
    }

    public TorSettingsBuilder useBridges() {
        writeTrueProperty("UseBridges");
        return this;
    }

    @SettingsConfig
    public TorSettingsBuilder useBridgesFromSettings() {
        if (settings.hasBridges() && (hasCustomBridges() || hasUserDefinedBridges())) {
            useBridges();
        }
        return this;
    }

    public TorSettingsBuilder virtualAddressNetwork(String address) {
        if (!isNullOrEmpty(address))
            writeLine("VirtualAddrNetwork", address);
        return this;
    }

    @SettingsConfig
    public TorSettingsBuilder virtualAddressNetworkFromSettings() {
        return virtualAddressNetwork(settings.getVirtualAddressNetwork());
    }

    /**
     * Adds bridges from a resource stream. This relies on the TorInstaller to know how to obtain this stream.
     * These entries may be type-specified like:
     *
     * <code>
     * obfs3 169.229.59.74:31493 AF9F66B7B04F8FF6F32D455F05135250A16543C9
     * </code>
     * <p>
     */
    TorSettingsBuilder defaultBridgesFromResources(Set<BridgeType> userDefinedBridgeTypes) {
        if (!settings.hasBridges() || !hasUserDefinedBridges() || hasCustomBridges()) {
            return this;
        }
        ArrayList<String> bridgeTypes = new ArrayList<>();
        for (BridgeType bridgeType : userDefinedBridgeTypes) {
            bridgeTypes.add(bridgeType.name().toLowerCase());
        }
        InputStream bridgesStream = null;
        try {
            bridgesStream = input;
            writeDefaultBridgesFromStream(bridgesStream, bridgeTypes);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (bridgesStream != null) {
                try {
                    bridgesStream.close();
                } catch (IOException e) {
                }
            }
        }
        return this;
    }

    private boolean hasCustomBridges() {
        return !settings.getCustomBridges().isEmpty();
    }

    /**
     * Returns true if user has specified bridge types and if bridges.txt file is found.
     */
    private boolean hasUserDefinedBridges() {
        return !settings.getBridgeTypes().isEmpty() && input != null;
    }

    /**
     * Reads bridges from specified <code>input</code>. If the file doesn't contain any valid bridge entries,
     * then this method returns an empty bridge list.
     */
    private static List<Bridge> readDefaultBridgesFromStream(InputStream input) {
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

    TorSettingsBuilder writeAddress(String fieldName, String address, Integer port, String flags) {
        if (isNullOrEmpty(address) && port == null) {
            return this;
        }
        buffer.append(fieldName).append(" ");
        if (!isNullOrEmpty(address)) {
            buffer.append(address).append(":");
        }
        if (port != null) {
            if (port < 0) {
                throw new IllegalArgumentException("Port value: " + fieldName + ", " + port);
            }
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

    private void writeBridgeTransport(File pluggableTransportClient, BridgeType bridgeType) throws IOException {
        switch (bridgeType) {
            case MEEK_LITE:
                transportPluginMeek(pluggableTransportClient.getCanonicalPath());
                break;
            case OBFS3:
            case OBFS4:
                transportPluginObfs(pluggableTransportClient.getCanonicalPath());
        }
    }

    /**
     * Writes bridges from bridges.txt file to the config. Only bridges of the specified <code>bridgeTypes</code> will be written.
     * <p>
     * If the input file doesn't contain any valid bridge entries, this method writes nothing to the config.
     */
    private void writeDefaultBridgesFromStream(InputStream input, List<String> userDefinedBridgeTypes) {
        if (input == null || userDefinedBridgeTypes.isEmpty()) {
            return;
        }
        List<Bridge> bridges = readDefaultBridgesFromStream(input);
        for (Bridge b : bridges) {
            if (userDefinedBridgeTypes.contains(b.type)) {
                bridge(b.type, b.config);
            }
        }
    }

    private TorSettingsBuilder writeFalseProperty(String name) {
        buffer.append(name).append(" 0").append('\n');
        return this;
    }

    public TorSettingsBuilder writeLine(String value) {
        if (!isNullOrEmpty(value)) buffer.append(value).append("\n");
        return this;
    }

    public TorSettingsBuilder writeLine(String name, Set<String> values) {
        if (!isNullOrEmpty(name) && !isNullOrEmpty(values)) {
            buffer.append(name);
            String comma = " ";
            for (String value : values) {
                buffer.append(comma).append(value);
                comma = ",";
            }
            buffer.append("\n");
        }
        return this;
    }

    public TorSettingsBuilder writeLine(String value, String value2) {
        return writeLine(value + " " + value2);
    }

    private TorSettingsBuilder writeTrueProperty(String name) {
        buffer.append(name).append(" 1").append('\n');
        return this;
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
