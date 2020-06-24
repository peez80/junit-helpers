package de.stiffi.testing.junit.helpers;

import de.stiffi.testing.junit.rules.docker.DockerContainerRule;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.utils.URIBuilder;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;

public class WiremockHelperTest {

    @ClassRule
    public static DockerContainerRule wiremockContainer = DockerContainerRule.newDockerContainerRule("rodolpheche/wiremock")
            .withPortForward(8080)
           // .withFreeLocalPorts(30000, 30001, 30002, 30003, 30004)
            .withWaitAfterStartup(1000);


    @Before
    public void before() throws IOException {
        WiremockHelper underTest = new WiremockHelper(getWiremockUrl());
        underTest.deleteAllMappings();
        underTest.deleteAllRequests();
    }



    @Test
    public void testInstantiateSuccessfully() throws InterruptedException, URISyntaxException {
        //When
        WiremockHelper underTest = new WiremockHelper(getWiremockUrl());

        //Then
        //If no exception is thrown - everything is fine
    }

    @Test(expected = IllegalStateException.class)
    public void testInstantiateFail() throws URISyntaxException {
        // Given
        String uri = new URIBuilder(getWiremockUrl()).setPort(wiremockContainer.getMappedHostPort(8080) + 10).toString();
        //When
        //Instantiate wiremockhelper with wrong url
        WiremockHelper underTest = new WiremockHelper(uri);
    }

    @Test
    public void testCreateMapping() throws IOException {
        // Given
        WiremockHelper underTest = new WiremockHelper(getWiremockUrl());

        // When
        createDummyMapping(underTest);

        //Then
        assertElementsExisting(underTest, 1);
    }

    @Test
    public void testCreateMappingProgrammatically() throws IOException {
        // Given
        WiremockHelper underTest = new WiremockHelper(getWiremockUrl());
        String expectedResponse = "DUMMY_RESPONSE\nSecond\"Line\"";

        //When
        underTest.createMapping("GET", "/.*", expectedResponse, "application/dummyresponse", 201);

        //Then
        assertElementsExisting(underTest, 1);
    }

    @Test
    public void testDeleteMapping() throws IOException {
        //Given
        WiremockHelper underTest = new WiremockHelper(getWiremockUrl());
        createDummyMapping(underTest);

        //When
        underTest.deleteAllMappings();

        //Then
        assertElementsExisting(underTest, 0);
    }

    @Test
    public void testGetAllRequestsEmpty() throws IOException {
        //Given
        WiremockHelper underTest = new WiremockHelper(getWiremockUrl());

        //When
        String result = underTest.getAllRequests();

        //Then
        Assert.assertTrue("Didn't receive 0 elements", result.contains("\"total\" : 0"));
    }

    @Test
    public void testGetAllRequests() throws IOException {
        //Given
        WiremockHelper underTest = new WiremockHelper(getWiremockUrl());
        underTest.createMapping("GET", "/test", "OK", "text/plain", 200);

        CloseableHttpResponse matchedResponse = HttpHelper.executeGet(getWiremockUrl() + "/test", "text/plain");
        CloseableHttpResponse unmatchedResponse = HttpHelper.executeGet(getWiremockUrl()+"/some/thing", "text/plain");

        //When
        String requestsJson = underTest.getAllRequests();


        //Then
        Assert.assertEquals("Wrong response from matched request", 200, matchedResponse.getStatusLine().getStatusCode());
        Assert.assertEquals("Wrong response from matched request", 200, matchedResponse.getStatusLine().getStatusCode());
        Assert.assertTrue("Didn't receive 2 requests", requestsJson.contains("\"total\" : 2"));
    }

    @Test
    public void testDeleteAllRequests() throws IOException {
        //Given
        WiremockHelper underTest = new WiremockHelper(getWiremockUrl());
        underTest.createMapping("GET", "/test", "OK", "text/plain", 200);

        CloseableHttpResponse matchedResponse = HttpHelper.executeGet(getWiremockUrl() + "/test", "text/plain");
        CloseableHttpResponse unmatchedResponse = HttpHelper.executeGet(getWiremockUrl()+"/some/thing", "text/plain");

        //When
        String requestsBefore = underTest.getAllRequests();
        underTest.deleteAllRequests();
        String requestsAfter = underTest.getAllRequests();

        //Then
        Assert.assertTrue("RequetsBefore didn't contain 2 requests", requestsBefore.contains("\"total\" : 2"));
        Assert.assertTrue("RequestsAfter wasn't 2", requestsAfter.contains("\"total\" : 0"));

    }

    @Test
    public void testGetRequestCount() throws IOException {
        //Given
        WiremockHelper underTest = new WiremockHelper(getWiremockUrl());
        underTest.createMapping("GET", "/test", "OK", "text/plain", 200);

        CloseableHttpResponse matchedResponse = HttpHelper.executeGet(getWiremockUrl() + "/test", "text/plain");
        CloseableHttpResponse unmatchedResponse = HttpHelper.executeGet(getWiremockUrl()+"/some/thing", "text/plain");

        //When - Then 1
        int requestCountBefore = underTest.getRequestCount();
        Assert.assertEquals("Wrong request count before received", 2, requestCountBefore);

        // When - Then 2
        underTest.deleteAllRequests();
        int requestCountAfter = underTest.getRequestCount();
        Assert.assertEquals("Wrong request count after received", 0, requestCountAfter);
    }

    @Test
    public void testSanitizeJsonUrlPattern() {
        //Given
        WiremockHelper underTest = new WiremockHelper(getWiremockUrl());
        String unsanitizedUrlPattern = "\\/some\\/dir?test=a\\b";

        //When
        String sanitizedUrlPattern = underTest.sanitizeRegex(unsanitizedUrlPattern);

        System.out.println(unsanitizedUrlPattern);
        System.out.println(sanitizedUrlPattern);
    }




    private void assertElementsExisting(WiremockHelper wiremockHelper, int expectedMappingCount) throws IOException {
        CloseableHttpResponse response = HttpHelper.executeGet(getWiremockUrl() + "/__admin/mappings");
        String responseString = HttpHelper.readResponseBody(response);
        Assert.assertTrue("Not exactly " + expectedMappingCount + " mapping existing: " + responseString, responseString.contains("\"total\" : " + expectedMappingCount));
    }

    private String getWiremockUrl() {
        return "http://localhost:" + wiremockContainer.getMappedHostPort(8080);
    }

    private static void createDummyMapping(WiremockHelper wiremockHelper) throws IOException {
        String mapping = "{\n" +
                "  \"request\": {\n" +
                "    \"method\": \"GET\",\n" +
                "    \"url\": \"/some/thing\"\n" +
                "  },\n" +
                "  \"response\": {\n" +
                "    \"body\": \"Hello world!\",\n" +
                "    \"headers\": {\n" +
                "      \"Content-Type\": \"text/plain\"\n" +
                "    },\n" +
                "    \"status\": 200\n" +
                "  }\n" +
                "}";
        wiremockHelper.createMapping(mapping);
    }

}