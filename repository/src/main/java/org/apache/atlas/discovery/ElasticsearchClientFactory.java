/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.atlas.discovery;

import org.apache.atlas.ApplicationProperties;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.StringUtils;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

/**
 * Factory class for creating and configuring ElasticsearchClient for ES 8.x
 * 
 * This factory reads Elasticsearch configuration from atlas-application.properties
 * and initializes the ElasticsearchClient (Elasticsearch Java API Client) with proper 
 * authentication and connection settings.
 * 
 * Supports Elasticsearch 8.x versions.
 */
@Component
public class ElasticsearchClientFactory {
    private static final Logger LOG = LoggerFactory.getLogger(ElasticsearchClientFactory.class);

    private final Configuration configuration;
    private ElasticsearchClient client;
    private ElasticsearchTransport transport;
    private RestClient restClient;

    @Inject
    public ElasticsearchClientFactory(Configuration configuration) {
        this.configuration = configuration;
        initializeClient();
    }

    /**
     * Initialize and create ElasticsearchClient based on configuration
     * 
     * Creates a transport layer with RestClient and wraps it with ElasticsearchClient
     */
    private void initializeClient() {
        try {
            LOG.info("Initializing Elasticsearch 8.x client for vector search");

            // Read configuration parameters with defaults
            String hostsString = configuration.getString(
                ApplicationProperties.ELASTICSEARCH_VECTOR_SEARCH_HOSTS,
                ApplicationProperties.DEFAULT_ELASTICSEARCH_HOSTS
            );

            int port = configuration.getInt(
                ApplicationProperties.ELASTICSEARCH_VECTOR_SEARCH_PORT,
                ApplicationProperties.DEFAULT_ELASTICSEARCH_PORT
            );

            String scheme = configuration.getString(
                ApplicationProperties.ELASTICSEARCH_VECTOR_SEARCH_SCHEME,
                ApplicationProperties.DEFAULT_ELASTICSEARCH_SCHEME
            );

            String username = configuration.getString(
                ApplicationProperties.ELASTICSEARCH_VECTOR_SEARCH_USERNAME,
                null
            );

            String password = configuration.getString(
                ApplicationProperties.ELASTICSEARCH_VECTOR_SEARCH_PASSWORD,
                null
            );

            int timeoutMs = configuration.getInt(
                ApplicationProperties.ELASTICSEARCH_VECTOR_SEARCH_TIMEOUT_MS,
                ApplicationProperties.DEFAULT_ELASTICSEARCH_TIMEOUT_MS
            );

            // Parse hosts (comma-separated)
            List<HttpHost> httpHosts = parseHosts(hostsString, port, scheme);

            LOG.info("Elasticsearch 8.x client configuration: hosts={}, port={}, scheme={}, timeout={}ms",
                    hostsString, port, scheme, timeoutMs);

            // Create RestClientBuilder
            RestClientBuilder builder = RestClient.builder(httpHosts.toArray(new HttpHost[0]))
                    .setRequestConfigCallback(requestConfigBuilder ->
                            requestConfigBuilder
                                    .setConnectTimeout(timeoutMs)
                                    .setSocketTimeout(timeoutMs)
                    )
                    .setHttpClientConfigCallback(httpClientBuilder -> {
                        // 1. Add basic authentication if username and password are provided
                        if (StringUtils.isNotEmpty(username) && StringUtils.isNotEmpty(password)) {
                            LOG.info("Setting up Elasticsearch authentication with username: {}", username);
                            final BasicCredentialsProvider credentialsProvider =
                                    new BasicCredentialsProvider();
                            credentialsProvider.setCredentials(
                                    AuthScope.ANY,
                                    new UsernamePasswordCredentials(username, password)
                            );
                            httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
                        }

                        // 2. Configure SSL/TLS for HTTPS connections
                        configureSSL(httpClientBuilder);

                        return httpClientBuilder;
                    });

            // Build RestClient
            this.restClient = builder.build();

            // Create transport layer (required for ES 8.x client)
            this.transport = new RestClientTransport(
                    restClient,
                    new JacksonJsonpMapper()
            );

            // Create ElasticsearchClient
            this.client = new ElasticsearchClient(transport);

            LOG.info("Elasticsearch 8.x client initialized successfully");

        } catch (Exception e) {
            LOG.error("Failed to initialize Elasticsearch 8.x client", e);
            throw new RuntimeException("Elasticsearch 8.x client initialization failed: " + e.getMessage(), e);
        }
    }

    /**
     * Parse comma-separated hosts and create HttpHost objects
     * 
     * @param hostsString comma-separated host names/IPs
     * @param port Elasticsearch port number
     * @param scheme HTTP scheme (http or https)
     * @return list of HttpHost objects
     */
    private List<HttpHost> parseHosts(String hostsString, int port, String scheme) {
        List<HttpHost> hosts = new ArrayList<>();

        if (StringUtils.isEmpty(hostsString)) {
            hostsString = ApplicationProperties.DEFAULT_ELASTICSEARCH_HOSTS;
        }

        String[] hostArray = hostsString.split(",");

        for (String host : hostArray) {
            String trimmedHost = host.trim();
            if (StringUtils.isNotEmpty(trimmedHost)) {
                hosts.add(new HttpHost(trimmedHost, port, scheme));
                LOG.debug("Added Elasticsearch host: {}://{}:{}", scheme, trimmedHost, port);
            }
        }

        if (hosts.isEmpty()) {
            // Add default host if parsing resulted in empty list
            hosts.add(new HttpHost(ApplicationProperties.DEFAULT_ELASTICSEARCH_HOSTS,
                    ApplicationProperties.DEFAULT_ELASTICSEARCH_PORT,
                    ApplicationProperties.DEFAULT_ELASTICSEARCH_SCHEME));
            LOG.warn("No valid hosts found, using default: {}://{}:{}",
                    ApplicationProperties.DEFAULT_ELASTICSEARCH_SCHEME,
                    ApplicationProperties.DEFAULT_ELASTICSEARCH_HOSTS,
                    ApplicationProperties.DEFAULT_ELASTICSEARCH_PORT);
        }

        return hosts;
    }

    /**
     * Get the initialized ElasticsearchClient
     * 
     * @return ElasticsearchClient instance for ES 8.x
     */
    public ElasticsearchClient getClient() {
        if (client == null) {
            initializeClient();
        }
        return client;
    }

    /**
     * Configure SSL/TLS for HTTPS connections (Development Environment)
     * 
     * For development/testing purposes, completely skips SSL certificate verification
     * to work with self-signed certificates or test environments without hassle.
     * 
     * WARNING: This is NOT suitable for production environments!
     * 
     * @param httpClientBuilder the HTTP client builder to configure
     */
    private void configureSSL(org.apache.http.impl.nio.client.HttpAsyncClientBuilder httpClientBuilder) {
        try {
            LOG.warn(" [DEV] Configuring Elasticsearch client to SKIP SSL certificate verification");
            LOG.warn("  WARNING: This is for development/testing only! NOT suitable for production!");
            
            // Create a TrustManager that accepts ALL certificates
            // This allows connections to servers with self-signed, expired, or invalid certificates
            TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    @Override
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }

                    @Override
                    public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {
                        // Do nothing - accept all client certificates
                    }

                    @Override
                    public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {
                        // Do nothing - accept all server certificates
                    }
                }
            };
            
            // Initialize SSLContext with TLSv1.2 (modern and secure protocol version)
            SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
            sslContext.init(null, trustAllCerts, new SecureRandom());
            
            // Set the SSL context to the HTTP client builder
            httpClientBuilder.setSSLContext(sslContext);
            
            // Also disable hostname verification to allow any hostname
            // (useful when certificate CN doesn't match the actual hostname)
            httpClientBuilder.setSSLHostnameVerifier((hostname, sslSession) -> {
                // Accept any hostname
                return true;
            });
            
            LOG.info(" [DEV] SSL verification completely disabled - Elasticsearch client ready for development");
            LOG.info("   → Certificate validation: DISABLED");
            LOG.info("   → Hostname verification: DISABLED");
            LOG.info("   → Accepts self-signed certificates: YES");
            
        } catch (Exception e) {
            LOG.error(" Failed to configure SSL context", e);
            throw new RuntimeException("SSL configuration failed: " + e.getMessage(), e);
        }
    }

    /**
     * Close the Elasticsearch client connection
     * This should be called during application shutdown
     */
    public void close() {
        try {
            if (client != null) {
                LOG.info("Closing Elasticsearch 8.x client");
                // Close transport first
                if (transport != null) {
                    transport.close();
                }
                // Then close rest client
                if (restClient != null) {
                    restClient.close();
                }
                LOG.info("Elasticsearch 8.x client closed successfully");
            }
        } catch (Exception e) {
            LOG.error("Error closing Elasticsearch 8.x client", e);
        }
    }
}
