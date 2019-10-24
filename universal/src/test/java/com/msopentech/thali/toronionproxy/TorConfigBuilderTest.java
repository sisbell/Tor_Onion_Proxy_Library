package com.msopentech.thali.toronionproxy;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class TorConfigBuilderTest {

    /**
     * Bridges are added in random order
     */
    @Test
    public void testAddCustomBridges() throws Exception {
        TorSettings torSettings = mock(TorSettings.class);
        when(torSettings.hasBridges()).thenReturn(true);
        when(torSettings.getCustomBridges()).thenReturn(Arrays.asList("b1", "b2"));

        OnionProxyContext context = mock(OnionProxyContext.class);
        when(context.getSettings()).thenReturn(torSettings);

        TorConfigBuilder builder = new TorConfigBuilder(context);
        builder.customBridgesFromSettings();
        String result = builder.asString();
        assertTrue("Bridge b2\nBridge b1\n".equals(result) || "Bridge b1\nBridge b2\n".equals(result));
    }

    @Test
    public void testNoCustomBridgesIfBridgesAreDisabled() throws Exception {
        TorSettings torSettings = mock(TorSettings.class);
        when(torSettings.hasBridges()).thenReturn(false);
        when(torSettings.getCustomBridges()).thenReturn(Arrays.asList("b1", "b2"));

        OnionProxyContext context = mock(OnionProxyContext.class);
        when(context.getSettings()).thenReturn(torSettings);

        TorConfigBuilder builder = new TorConfigBuilder(context);
        builder.customBridgesFromSettings();
        String result = builder.asString();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testNoCustomBridges() throws Exception {
        TorSettings torSettings = mock(TorSettings.class);
        when(torSettings.hasBridges()).thenReturn(true);
        when(torSettings.getCustomBridges()).thenReturn(Collections.emptyList());

        OnionProxyContext context = mock(OnionProxyContext.class);
        when(context.getSettings()).thenReturn(torSettings);

        TorConfigBuilder builder = new TorConfigBuilder(context);
        builder.customBridgesFromSettings();
        String result = builder.asString();
        assertTrue(result.isEmpty());
    }

    /**
     * SHould be empty
     */
    @Test
    public void testUseBridgesFromSettingsFalse() {
        TorSettings torSettings = mock(TorSettings.class);
        when(torSettings.hasBridges()).thenReturn(false);
        OnionProxyContext context = mock(OnionProxyContext.class);
        when(context.getSettings()).thenReturn(torSettings);
        TorConfigBuilder builder = new TorConfigBuilder(context);

        builder.useBridgesFromSettings();
        String result = builder.asString();
        assertEquals("", result);
    }

    @Test
    public void testUseBridgesFromSettingsNoBridgesAvailable() {
        TorSettings torSettings = mock(TorSettings.class);
        when(torSettings.hasBridges()).thenReturn(true);
        OnionProxyContext context = mock(OnionProxyContext.class);
        when(context.getSettings()).thenReturn(torSettings);
        TorConfigBuilder builder = new TorConfigBuilder(context);

        builder.useBridgesFromSettings();
        String result = builder.asString();
        assertEquals("", result);
    }

    @Test
    public void testUseBridgesFromSettingsWithCustomBridges() {
        TorSettings torSettings = mock(TorSettings.class);
        when(torSettings.hasBridges()).thenReturn(true);
        when(torSettings.getCustomBridges()).thenReturn(Arrays.asList("b1", "b2"));
        OnionProxyContext context = mock(OnionProxyContext.class);
        when(context.getSettings()).thenReturn(torSettings);
        TorConfigBuilder builder = new TorConfigBuilder(context);

        builder.useBridgesFromSettings();
        String result = builder.asString();
        assertEquals("UseBridges 1\n", result);
    }

    @Test
    public void testUseBridgesFromSettingsWithPredefinedBridges() {
        TorSettings torSettings = mock(TorSettings.class);
        when(torSettings.hasBridges()).thenReturn(true);
        when(torSettings.getBridgeTypes()).thenReturn(Collections.singletonList(BridgeType.OBFS4));
        OnionProxyContext context = mock(OnionProxyContext.class);
        when(context.getSettings()).thenReturn(torSettings);
        TorConfigBuilder builder = new TorConfigBuilder(context);

        builder.useBridgesFromSettings();
        String result = builder.asString();
        assertEquals("UseBridges 1\n", result);
    }

    @Test
    public void testPredefinedBridgesFromSettingsFilterOneType() throws IOException {
        String bridgeList = "obfs4 192\nobfs3 190\nobfs4 189\n,meek 170\n";
        InputStream bridgeStream = new ByteArrayInputStream(bridgeList.getBytes());

        TorSettings torSettings = mock(TorSettings.class);
        when(torSettings.hasBridges()).thenReturn(true);
        when(torSettings.getBridgeTypes()).thenReturn(Collections.singletonList(BridgeType.OBFS4));
        TorInstaller torInstaller = mock(TorInstaller.class);
        when(torInstaller.openBridgesStream()).thenReturn(bridgeStream);//implement
        OnionProxyContext context = mock(OnionProxyContext.class);
        when(context.getSettings()).thenReturn(torSettings);
        when(context.getInstaller()).thenReturn(torInstaller);
        TorConfigBuilder builder = new TorConfigBuilder(context);

        builder.predefinedBridgesFromSettings();
        String result = builder.asString();
        assertTrue("Bridge obfs4 192\nBridge obfs4 189\n".equals(result)
                || "Bridge obfs4 189\nBridge obfs4 192\n".equals(result));

    }

    @Test
    public void testPredefinedBridgesFromSettingsFilterTwoTypes() throws IOException {
        String bridgeList = "obfs4 192\nobfs3 190\nobfs3 189\nmeek_lite 170\n";
        InputStream bridgeStream = new ByteArrayInputStream(bridgeList.getBytes());

        TorSettings torSettings = mock(TorSettings.class);
        when(torSettings.hasBridges()).thenReturn(true);
        when(torSettings.getBridgeTypes()).thenReturn(Arrays.asList(BridgeType.OBFS4, BridgeType.MEEK_LITE));
        TorInstaller torInstaller = mock(TorInstaller.class);
        when(torInstaller.openBridgesStream()).thenReturn(bridgeStream);//implement
        OnionProxyContext context = mock(OnionProxyContext.class);
        when(context.getSettings()).thenReturn(torSettings);
        when(context.getInstaller()).thenReturn(torInstaller);
        TorConfigBuilder builder = new TorConfigBuilder(context);

        builder.predefinedBridgesFromSettings();
        String result = builder.asString();
        assertTrue("Bridge obfs4 192\nBridge meek_lite 170\n".equals(result)
                || "Bridge meek_lite 170\nBridge obfs4 192\n".equals(result));

    }

    @Test
    public void testConfigureTransportsAddedWhenNoBridges() throws IOException {
        TorSettings torSettings = mock(TorSettings.class);
        when(torSettings.hasBridges()).thenReturn(false);
        OnionProxyContext context = mock(OnionProxyContext.class);
        when(context.getSettings()).thenReturn(torSettings);
        TorConfigBuilder builder = new TorConfigBuilder(context);
        File transport = File.createTempFile("transport", ".exe");
        transport.setExecutable(true);

        builder.configurePluggableTransports(transport, Arrays.asList(BridgeType.OBFS4));
        String result = builder.asString();
        assertTrue(result.contains("ClientTransportPlugin obfs3 exec"));
        assertTrue(result.contains("ClientTransportPlugin obfs4 exec"));

    }

    @Test
    public void testConfigureTransportsMeek() throws IOException {
        TorSettings torSettings = mock(TorSettings.class);
        when(torSettings.hasBridges()).thenReturn(false);
        OnionProxyContext context = mock(OnionProxyContext.class);
        when(context.getSettings()).thenReturn(torSettings);
        TorConfigBuilder builder = new TorConfigBuilder(context);
        File transport = File.createTempFile("transport", ".exe");
        transport.setExecutable(true);

        builder.configurePluggableTransports(transport, Arrays.asList(BridgeType.MEEK_LITE));
        String result = builder.asString();
        assertTrue(result.contains("ClientTransportPlugin meek_lite exec"));
        assertFalse(result.contains("ClientTransportPlugin obfs4 exec"));
    }

    @Test
    public void testHttpTunnelPort() throws IOException {
        TorSettings torSettings = mock(TorSettings.class);
        when(torSettings.getHttpTunnelHost()).thenReturn("192.1.1.1");
        when(torSettings.getHttpTunnelPort()).thenReturn(8080);
        when(torSettings.hasIsolationAddressFlagForTunnel()).thenReturn(true);

        OnionProxyContext context = mock(OnionProxyContext.class);
        when(context.getSettings()).thenReturn(torSettings);
        TorConfigBuilder builder = new TorConfigBuilder(context);
        builder.httpTunnelPortFromSettings();
        String result = builder.asString();
        assertEquals("HTTPTunnelPort 192.1.1.1:8080 IsolateDestAddr\n", result);
    }

    @Test
    public void testTransparentProxy() throws IOException {
        TorSettings torSettings = mock(TorSettings.class);
        when(torSettings.getTransparentProxyAddress()).thenReturn("192.1.1.1");
        when(torSettings.getTransparentProxyPort()).thenReturn(8080);
        OnionProxyContext context = mock(OnionProxyContext.class);
        when(context.getSettings()).thenReturn(torSettings);
        TorConfigBuilder builder = new TorConfigBuilder(context);
        builder.transPortFromSettings();
        String result = builder.asString();
        assertEquals("TransPort 192.1.1.1:8080\n", result);
    }

    @Test
    public void testDnsPort() throws IOException {
        TorSettings torSettings = mock(TorSettings.class);
        when(torSettings.getDnsPort()).thenReturn(5111);
        OnionProxyContext context = mock(OnionProxyContext.class);
        when(context.getSettings()).thenReturn(torSettings);
        TorConfigBuilder builder = new TorConfigBuilder(context);
        builder.dnsPortFromSettings();
        String result = builder.asString();
        assertEquals("DNSPort 5111\n", result);
    }

    @Test
    public void testAddAddress() throws IOException {
        OnionProxyContext context = mock(OnionProxyContext.class);
        TorConfigBuilder builder = new TorConfigBuilder(context);
        builder.addAddress("fieldName", "192.1.1.0", 8080, null);
        String result = builder.asString();
        assertEquals("fieldName 192.1.1.0:8080\n", result);
    }

    @Test
    public void testAddAddressNoAddress() throws IOException {
        OnionProxyContext context = mock(OnionProxyContext.class);
        TorConfigBuilder builder = new TorConfigBuilder(context);
        builder.addAddress("fieldName", null, 8080, null);
        String result = builder.asString();
        assertEquals("fieldName 8080\n", result);
    }

    @Test
    public void testAddAddressNoPort() throws IOException {
        OnionProxyContext context = mock(OnionProxyContext.class);
        TorConfigBuilder builder = new TorConfigBuilder(context);
        builder.addAddress("fieldName", "192.1.1.0", null, null);
        String result = builder.asString();
        assertEquals("fieldName 192.1.1.0:auto\n", result);
    }

    @Test
    public void testAddAddressIllegalPort() throws IOException {
        OnionProxyContext context = mock(OnionProxyContext.class);
        TorConfigBuilder builder = new TorConfigBuilder(context);
        builder.addAddress("fieldName", "192.1.1.0", 0, null);
        String result = builder.asString();
        assertEquals("fieldName 192.1.1.0:auto\n", result);
    }

    @Test
    public void testAddAddressNull() throws IOException {
        OnionProxyContext context = mock(OnionProxyContext.class);
        TorConfigBuilder builder = new TorConfigBuilder(context);
        builder.addAddress("fieldName", null, null, null);
        String result = builder.asString();
        assertEquals("", result);
    }
}
