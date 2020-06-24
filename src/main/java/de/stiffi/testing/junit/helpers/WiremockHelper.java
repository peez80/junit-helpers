package de.stiffi.testing.junit.helpers;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.utils.URIBuilder;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URISyntaxException;

/**
 * Helper class to manage a currently running wiremock instance. Tested with Docker image rodolpheche/wiremock.
 */
public class WiremockHelper {

    private String wiremockUrl;

    public WiremockHelper(String wiremockUrl) {
        verifyWiremockBaseUrl(wiremockUrl);
        this.wiremockUrl = wiremockUrl;
    }

    private void verifyWiremockBaseUrl(String wiremockUrl) {
        try {
            String url = new URIBuilder(wiremockUrl).setPath("/__admin").toString();
            CloseableHttpResponse response = HttpHelper.executeGetForJson(url);
            verifyResponse(response, 200);
        } catch (URISyntaxException | IOException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }

    }

    public void createMapping(String mappingJson) throws IOException {
        CloseableHttpResponse response = HttpHelper.executePost(getMappingsUrl(), mappingJson, "application/json", "application/json", null);
        verifyResponse(response, 201);
    }

    public void createMapping(String method, String urlPattern, String response, String responseContentType, int responseStatus) throws IOException {

        String sanitizedResponse = sanitizeJson(response);
        String sanitizedUrlPattern = sanitizeRegex(urlPattern);

        String mappingJson = new TemplateHelper()
                .with("{{method}}", method)
                .with("{{urlPattern}}", urlPattern)
                .with("{{responseBody}}", sanitizedResponse)
                .with("{{responseContentType}}", responseContentType)
                .with("{{responseStatus}}", "" + responseStatus)
                .readTemplateFile("Wiremock-Request-Template.json", WiremockHelper.class);

        createMapping(mappingJson);
    }

    protected String sanitizeRegex(String regex) {
        return regex.replace("\\", "\\\\");
    }

    protected String sanitizeJson(String unescapedJson) {
        return unescapedJson
                .replace("\\", "\\\\")
                .replace("\r", "")
                .replace("\n", "\\n")
                .replace("\"", "\\\"");
    }

    public void deleteAllMappings() throws IOException {
        CloseableHttpResponse response = HttpHelper.executeDelete(getMappingsUrl());
    }

    public void deleteAllRequests() throws IOException {
        CloseableHttpResponse response = HttpHelper.executeDelete(getRequestsUrl());
    }

    /**
     * Return all requests, as a wiremock json output. AS it's a quite complex object,
     * users have to either do some "contains" logic or e.g. parse it with {@link JSONObject}. An
     * example how to use this can be found in {@link #getRequestCount()}
     *
     * @return
     * @throws IOException
     */
    public String getAllRequests() throws IOException {
        CloseableHttpResponse response = HttpHelper.executeGetForJson(getRequestsUrl());
        return HttpHelper.readResponseBody(response);
    }

    public int getRequestCount() throws IOException {
        String requestsJson = getAllRequests();
        JSONObject json = new JSONObject(requestsJson);
        JSONArray requests = json.getJSONArray("requests");
        return requests.length();
    }

    private void verifyResponse(CloseableHttpResponse response, int expectedStatusCode) throws IOException {
        if (response.getStatusLine().getStatusCode() != expectedStatusCode) {
            String responseString = HttpHelper.readResponseBody(response);
            throw new IllegalStateException("Could not create Mapping. StatusCode: " + response.getStatusLine().getStatusCode() + ", Reason: " + response.getStatusLine().getReasonPhrase() + ", ResponseBody: " + responseString);
        }
    }

    private String getMappingsUrl() {
        try {
            return new URIBuilder(wiremockUrl).setPath("/__admin/mappings").toString();
        }catch(URISyntaxException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    private String getRequestsUrl() {
        try {
            return new URIBuilder(wiremockUrl).setPath("/__admin/requests").toString();
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }
}
