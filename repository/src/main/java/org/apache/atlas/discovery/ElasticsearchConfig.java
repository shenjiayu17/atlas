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
import org.apache.atlas.AtlasException;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration for vector search service (Elasticsearch 8.x compatible)
 * 
 * This configuration class sets up the necessary beans for vector search functionality,
 * including the Elasticsearch 8.x client and the VectorSearchService implementation.
 * 
 * Supports Elasticsearch 8.x versions using the official Elasticsearch Java API Client.
 */
@Configuration
public class ElasticsearchConfig {

    /**
     * Create the ElasticsearchClientFactory bean
     * This bean is responsible for initializing and managing the Elasticsearch 8.x client
     * with configuration from atlas-application.properties
     *
     * @return configured ElasticsearchClientFactory instance
     */
    @Bean
    public ElasticsearchClientFactory elasticsearchClientFactory() throws AtlasException {
        return new ElasticsearchClientFactory(ApplicationProperties.get());
    }

    /**
     * Create the ElasticsearchClient bean for ES 8.x
     * This client is used for all Elasticsearch operations with the official Java API Client
     *
     * @return ElasticsearchClient from the factory for ES 8.x operations
     */
    @Bean
    public ElasticsearchClient elasticsearchClient(ElasticsearchClientFactory clientFactory) {
        return clientFactory.getClient();
    }

    /**
     * Create and configure the VectorSearchService bean
     * 
     * @param elasticsearchClient the Elasticsearch 8.x client
     * @return configured VectorSearchService instance
     */
    @Bean
    public VectorSearchService vectorSearchService(ElasticsearchClient elasticsearchClient) {
        return new ElasticsearchVectorSearchService(elasticsearchClient);
    }

}
