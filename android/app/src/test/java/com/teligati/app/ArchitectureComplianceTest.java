package com.teligati.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.File;
import java.nio.file.Files;

public class ArchitectureComplianceTest {

    @Test
    public void testExplicitIntentConstants() {
        assertEquals("EXTRA_ID", OpportunityDetailActivity.EXTRA_ID);
        assertEquals("EXTRA_TITLE", OpportunityDetailActivity.EXTRA_TITLE);
        assertEquals("EXTRA_ORG", OpportunityDetailActivity.EXTRA_ORG);
        assertEquals("EXTRA_COMPENSATION", OpportunityDetailActivity.EXTRA_COMPENSATION);
        assertEquals("EXTRA_SKILLS", OpportunityDetailActivity.EXTRA_SKILLS);
    }

    @Test
    public void testActivityClassesIntegrity() {
        assertNotNull(OpportunityDetailActivity.class);
        assertNotNull(com.teligati.app.auth.AuthGatewayActivity.class);
        assertNotNull(MainActivity.class);
        assertNotNull(com.teligati.app.firebase.FirebaseDemoActivity.class);
    }

    @Test
    public void testManifestContainsDeclaredActivities() throws Exception {
        File manifest = new File("src/main/AndroidManifest.xml");
        if (!manifest.exists()) {
            manifest = new File("app/src/main/AndroidManifest.xml");
        }
        if (!manifest.exists()) {
            manifest = new File("android/app/src/main/AndroidManifest.xml");
        }

        assertTrue("AndroidManifest.xml must exist", manifest.exists());
        String xml = new String(Files.readAllBytes(manifest.toPath()));

        assertTrue("Manifest must declare AuthGatewayActivity", xml.contains("AuthGatewayActivity"));
        assertTrue("Manifest must declare MainActivity", xml.contains("MainActivity"));
        assertTrue("Manifest must declare OpportunityDetailActivity", xml.contains("OpportunityDetailActivity"));
        assertTrue("Manifest must declare FirebaseDemoActivity", xml.contains("FirebaseDemoActivity"));
        assertTrue("Manifest must declare SystemEventReceiver", xml.contains("SystemEventReceiver"));
        assertTrue("Manifest must declare StudySessionService", xml.contains("StudySessionService"));
    }

    @Test
    public void testNetworkConfigurationEndpoints() throws Exception {
        assertEquals("http://10.0.2.2:8080/", BuildConfig.EMULATOR_BASE_URL);
        assertEquals("http://192.168.0.64:8080/", BuildConfig.PHYSICAL_BASE_URL);

        File netSecConfig = new File("src/main/res/xml/network_security_config.xml");
        if (!netSecConfig.exists()) {
            netSecConfig = new File("app/src/main/res/xml/network_security_config.xml");
        }
        if (!netSecConfig.exists()) {
            netSecConfig = new File("android/app/src/main/res/xml/network_security_config.xml");
        }
        assertTrue("network_security_config.xml must exist", netSecConfig.exists());
        String xml = new String(Files.readAllBytes(netSecConfig.toPath()));
        assertTrue("network_security_config must permit 10.0.2.2", xml.contains("10.0.2.2"));
        assertTrue("network_security_config must permit 192.168.0.64", xml.contains("192.168.0.64"));
    }
}
