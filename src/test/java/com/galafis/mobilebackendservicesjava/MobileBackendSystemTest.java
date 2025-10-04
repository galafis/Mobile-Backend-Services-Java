package com.galafis.mobilebackendservicesjava;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class MobileBackendSystemTest {

    private MobileBackendSystem system;

    @Before
    public void setUp() {
        system = new MobileBackendSystem();
    }

    @After
    public void tearDown() {
        system.shutdown();
    }

    @Test
    public void testInitialization() throws ExecutionException, InterruptedException {
        system.initialize().get();
        // Assuming initialize generates at least some records
        assertTrue(system.dataRecords.size() > 0);
    }

    @Test
    public void testDataRecordCreation() {
        LocalDateTime now = LocalDateTime.now();
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("key", "value");
        MobileBackendSystem.DataRecord record = new MobileBackendSystem.DataRecord("1", now, 100.0, metadata);

        assertEquals("1", record.getId());
        assertEquals(now, record.getTimestamp());
        assertEquals(100.0, record.getValue(), 0.001);
        assertEquals("value", record.getMetadata().get("key"));
    }

    @Test
    public void testProcessData() throws ExecutionException, InterruptedException {
        system.initialize().get();
        MobileBackendSystem.AnalysisResult result = system.processData().get();

        assertNotNull(result);
        assertNotNull(result.getSummary());
        assertNotNull(result.getInsights());
        assertNotNull(result.getRecommendations());
        assertTrue(result.getProcessingTimeMs() >= 0);

        // Basic check for summary content
        assertTrue(result.getSummary().containsKey("totalRecords"));
        assertTrue(result.getSummary().get("totalRecords") > 0);
    }

    @Test
    public void testCalculateSummary() throws ExecutionException, InterruptedException {
        // Add specific data for predictable summary
        system.dataRecords.add(new MobileBackendSystem.DataRecord("rec1", LocalDateTime.now(), 10.0, new HashMap<>()));
        system.dataRecords.add(new MobileBackendSystem.DataRecord("rec2", LocalDateTime.now(), 20.0, new HashMap<>()));
        system.dataRecords.add(new MobileBackendSystem.DataRecord("rec3", LocalDateTime.now(), 30.0, new HashMap<>()));

        Map<String, Double> summary = system.calculateSummary();

        assertEquals(3.0, summary.get("totalRecords"), 0.001);
        assertEquals(20.0, summary.get("averageValue"), 0.001);
        assertEquals(30.0, summary.get("maxValue"), 0.001);
        assertEquals(10.0, summary.get("minValue"), 0.001);
    }

    @Test
    public void testGenerateInsights() throws ExecutionException, InterruptedException {
        // Add specific data for predictable insights
        Map<String, Object> metaA = new HashMap<>();
        metaA.put("category", "A");
        Map<String, Object> metaB = new HashMap<>();
        metaB.put("category", "B");

        system.dataRecords.add(new MobileBackendSystem.DataRecord("rec1", LocalDateTime.now(), 10.0, metaA));
        system.dataRecords.add(new MobileBackendSystem.DataRecord("rec2", LocalDateTime.now(), 100.0, metaA));
        system.dataRecords.add(new MobileBackendSystem.DataRecord("rec3", LocalDateTime.now(), 10.0, metaB));

        List<String> insights = system.generateInsights();
        assertFalse(insights.isEmpty());
        assertTrue(insights.stream().anyMatch(s -> s.contains("Category 'A' represents")));
    }

    @Test
    public void testGenerateRecommendations() throws ExecutionException, InterruptedException {
        // Test case for low data collection
        system = new MobileBackendSystem(); // Reset system to have few records
        system.dataRecords.add(new MobileBackendSystem.DataRecord("rec1", LocalDateTime.now(), 10.0, new HashMap<>()));
        List<String> recommendations = system.generateRecommendations();
        assertTrue(recommendations.stream().anyMatch(s -> s.contains("Consider increasing data collection")));

        // Test case for outdated data (assuming no recent data added)
        system = new MobileBackendSystem(); // Reset system
        for (int i = 0; i < 200; i++) {
            system.dataRecords.add(new MobileBackendSystem.DataRecord("old_rec" + i, LocalDateTime.now().minusDays(30), 50.0, new HashMap<>()));
        }
        recommendations = system.generateRecommendations();
        assertTrue(recommendations.stream().anyMatch(s -> s.contains("Data appears outdated")));
    }

    @Test
    public void testExportData() throws ExecutionException, InterruptedException {
        system.initialize().get();
        Map<String, Object> exportedData = system.exportData();

        assertNotNull(exportedData);
        assertTrue(exportedData.containsKey("data"));
        assertTrue(exportedData.containsKey("exportTime"));
        assertTrue(exportedData.containsKey("recordCount"));
        assertTrue(exportedData.containsKey("systemVersion"));
        assertTrue(((List<?>) exportedData.get("data")).size() == (Integer) exportedData.get("recordCount"));
    }

    @Test
    public void testShutdown() {
        try {
            system.initialize().get();
        } catch (InterruptedException | ExecutionException e) {
            fail("Initialization failed in testShutdown: " + e.getMessage());
        }
        system.shutdown();
        assertTrue(system.executorService.isShutdown());
        assertTrue(system.executorService.isTerminated());
    }
}

