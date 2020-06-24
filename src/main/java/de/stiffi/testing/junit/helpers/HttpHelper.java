package de.stiffi.testing.junit.helpers;

import org.apache.http.HttpHeaders;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.*;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.net.URI;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

public class HttpHelper {

    private static final int TIMEOUT_MS = 60000;

    private static CloseableHttpClient getClient(URI uri, UsernamePasswordCredentials basicAuth, int timeoutMs, boolean ignoreSSLErrors) {
        HttpClientBuilder httpClientBuilder = HttpClients.custom();
        if (basicAuth != null) {
            CredentialsProvider credsProvider = new BasicCredentialsProvider();
            credsProvider.setCredentials(
                    new AuthScope(uri.getHost(), uri.getPort()),
                    basicAuth);
            httpClientBuilder.setDefaultCredentialsProvider(credsProvider);
        }
        httpClientBuilder.setSSLHostnameVerifier((s, sslSession) -> true);
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(timeoutMs)
                .setSocketTimeout(timeoutMs)
                .setConnectionRequestTimeout(timeoutMs)
                .build();
        httpClientBuilder.setDefaultRequestConfig(requestConfig);

        if (ignoreSSLErrors) {
            try {
                // use the TrustSelfSignedStrategy to allow Self Signed Certificates
                SSLContext sslContext = SSLContextBuilder
                        .create()
                        .loadTrustMaterial(new TrustSelfSignedStrategy())
                        .build();
                HostnameVerifier allowAllHosts = new NoopHostnameVerifier();
                SSLConnectionSocketFactory connectionFactory = new SSLConnectionSocketFactory(sslContext, allowAllHosts);
                httpClientBuilder.setSSLSocketFactory(connectionFactory);
            } catch (NoSuchAlgorithmException | KeyStoreException |KeyManagementException  e) {
                throw new IllegalStateException(e.getMessage(), e);
            }
        }

        CloseableHttpClient httpClient = httpClientBuilder.build();
        return httpClient;
    }

    public static CloseableHttpResponse executePost(String url, String payload, String payloadContentType, String accept, UsernamePasswordCredentials basicAuth) throws IOException {
        return executePost(url, payload != null ? payload.getBytes() : null, payloadContentType, accept, basicAuth, null);
    }

    /**
     * Calls POST /reservations on cs-reservation-service-rest
     */
    public static CloseableHttpResponse executePost(String url, byte[] payload, String payloadContentType, String accept, UsernamePasswordCredentials basicAuth, Map<String, String> headers) throws IOException {
        URI uri = URI.create(url);
        HttpPost post = new HttpPost(uri);

        if (payload != null) {
            post.setEntity(new ByteArrayEntity(payload));
        }
        post.setHeader(HttpHeaders.CONTENT_TYPE, payloadContentType);
        if (accept != null) {
            post.setHeader(HttpHeaders.ACCEPT, accept);
        }
        if (headers != null) {
            for (Map.Entry<String, String> e : headers.entrySet()) {
                post.setHeader(e.getKey(), e.getValue());
            }
        }
        Dumper.sout("HTTP POST: " + url + ": " + (payload != null ? new String(payload) : "<NULL>" ));
        CloseableHttpClient httpclient = getClient(uri, basicAuth, 60000, true);
        CloseableHttpResponse response = httpclient.execute(post);
        dumpResponse(response);
        return response;
    }

    public static CloseableHttpResponse executeDelete(String url) throws IOException {
        URI uri = URI.create(url);
        HttpDelete delete = new HttpDelete(uri);
        Dumper.sout("HTTP DELETE: " + url);
        CloseableHttpClient httpclient = getClient(uri, null, TIMEOUT_MS, true);
        CloseableHttpResponse response = httpclient.execute(delete);
        dumpResponse(response);
        return response;
    }

    private static void dumpResponse(CloseableHttpResponse response) {
        Dumper.sout("HTTP Response: " + response.getStatusLine().getStatusCode() + " - " + response.getStatusLine().getReasonPhrase());
    }

    public static CloseableHttpResponse executeJsonPost(String url, String payload, UsernamePasswordCredentials basicAuth) throws IOException {
        return executePost(url, payload, "application/json", "application/json", basicAuth);
    }


    public static CloseableHttpResponse executeGetForJson(String url) throws IOException {
        return executeGet(url, "application/json");

    }

    public static CloseableHttpResponse executeGet(String url, String... acceptedMediaTypes) throws IOException {
        return executeGet(url, acceptedMediaTypes, null);
    }

    public static CloseableHttpResponse executeGet(String url, String[] acceptedMediaTypes, Map<String, String> headers) throws IOException {
        URI uri = URI.create(url);
        CloseableHttpClient httpClient = getClient(uri, null, 10000, true);
        HttpGet get = new HttpGet(uri);
        if (acceptedMediaTypes != null) {
            for (String accept : acceptedMediaTypes) {
                get.addHeader(HttpHeaders.ACCEPT, accept);
            }
        }
        if (headers != null) {
            headers.entrySet().forEach(
                    entry -> get.addHeader(entry.getKey(), entry.getValue())
            );
        }

        System.out.println("HTTP GET: " + url);
        CloseableHttpResponse response = httpClient.execute(get);
        System.out.println("--> "+ response.getStatusLine().getStatusCode() + " - " + response.getStatusLine().getReasonPhrase());
        return response;
    }

    public static CloseableHttpResponse executeOptions(String url) throws IOException {
        URI uri = URI.create(url);
        CloseableHttpClient httpClient = getClient(uri, null, 10000, true);
        HttpOptions options = new HttpOptions(uri);
        System.out.println("HTTP OPTIONS: " + uri);
        CloseableHttpResponse response = httpClient.execute(options);
        return response;
    }

    public static String readResponseBody(CloseableHttpResponse response) throws IOException {
        return readResponseBody(response, true);
    }

    public static String readResponseBody(CloseableHttpResponse response, boolean trace) throws IOException {
        String s = StreamReader.read(response.getEntity().getContent());
        if (trace) {
            System.out.println("Response: " + response.getStatusLine().getStatusCode() + " - " + response.getStatusLine().getReasonPhrase() + ": \n" + s);
        }
        return s;
    }


}
