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

import co.elastic.clients.json.JsonData;
import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.discovery.VectorSearchResult.VectorSearchResultItem;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.io.IOException;

/**
 * Implementation of vector search service using Elasticsearch 8.x
 *
 * This service performs vector similarity searches on Elasticsearch indices
 * using the kNN (k-Nearest Neighbors) query functionality.
 *
 * Compatible with Elasticsearch 8.x using the official Elasticsearch Java API Client.
 *
 * Note: This implementation assumes:
 * - Elasticsearch 8.x client is properly configured and injected
 * - The target index has proper mapping with vector field (column_desc_vector)
 * - Vector embeddings are already generated and stored in the index
 */
@Component
public class ElasticsearchVectorSearchService implements VectorSearchService {
    private static final Logger LOG = LoggerFactory.getLogger(ElasticsearchVectorSearchService.class);

    private final ElasticsearchClient elasticsearchClient;
    private final EmbeddingService embeddingService;
    private static final int DEFAULT_VECTOR_SIZE = 768;  // Common embedding size for many models
    private static final int DEFAULT_K = 10;  // Default number of nearest neighbors

    @Inject
    public ElasticsearchVectorSearchService(ElasticsearchClient elasticsearchClient) {
        this.elasticsearchClient = elasticsearchClient;
        this.embeddingService = new BasicEmbeddingService();
    }

    public ElasticsearchVectorSearchService(ElasticsearchClient elasticsearchClient, EmbeddingService embeddingService) {
        this.elasticsearchClient = elasticsearchClient;
        this.embeddingService = embeddingService != null ? embeddingService : new BasicEmbeddingService();
    }

    /**
     * Perform vector similarity search on Elasticsearch
     *
     * @param request the search request containing keyword, index name, limit, and offset
     * @return VectorSearchResult with matched items and their similarity scores
     * @throws AtlasBaseException if search fails
     */
    @Override
    public VectorSearchResult vectorSearch(VectorSearchRequest request) throws AtlasBaseException {
        long startTime = System.currentTimeMillis();

        try {
            LOG.info("Starting Elasticsearch 8.x vector search: {}", request);

            // Build and execute the search request
            SearchResponse<ObjectNode> response = buildAndExecuteSearch(request);

            // Parse the response
            VectorSearchResult result = parseSearchResponse(response, request);

            // Set additional metadata
            result.setKeyword(request.getKeyword());
            result.setLimit(request.getLimit());
            result.setOffset(request.getOffset());
            result.setIndexName(request.getIndexName());
            result.setExecutionTimeMs(System.currentTimeMillis() - startTime);

            LOG.info("Vector search completed successfully. Found {} results", result.getResultCount());
            return result;

        } catch (IOException e) {
            LOG.error("Elasticsearch 8.x vector search failed", e);
            throw new AtlasBaseException("Vector search failed: " + e.getMessage());
        }
    }

    /**
     * Build and execute the Elasticsearch search request with hybrid search for ES 8.x
     *
     * Implements hybrid search combining:
     * 1. Fuzzy text match on column_name (with AUTO fuzziness)
     * 2. Fuzzy text match on column_description_short (with AUTO fuzziness)
     * 3. Vector similarity search using script_score with cosineSimilarity
     *
     * Query structure:
     * - bool.should: Contains three OR'd clauses (text match + text match + vector similarity)
     * - script_score: Uses cosineSimilarity to compute vector similarity with +1.0 offset
     * - _source: Returns specified fields excluding vector field
     *
     * @param request the vector search request
     * @return SearchResponse from Elasticsearch
     */
    private SearchResponse<ObjectNode> buildAndExecuteSearch(VectorSearchRequest request) throws IOException {
        // Step 1: Generate vector embedding for the keyword
        float[] keywordVector = embeddingService.embed(request.getKeyword());
        LOG.debug("Generated vector embedding with dimension: {}", keywordVector.length);

        // Convert float[] to List<Double> for Elasticsearch API (required by Elasticsearch Java Client)
        java.util.List<Double> vectorList = new java.util.ArrayList<>(keywordVector.length);
        for (float f : keywordVector) {
            vectorList.add((double) f);
        }

        // Step 2: Create a search request using ES 8.x API
        SearchRequest.Builder searchBuilder = new SearchRequest.Builder()
                .index(request.getIndexName())
                .from(request.getOffset())
                .size(request.getLimit());

        // Step 3: Build hybrid search query combining text search (with fuzziness) + vector search
        // Using bool.should with three clauses:
        // 1. Fuzzy match on column_name
        // 2. Fuzzy match on column_description_short
        // 3. Vector similarity using script_score with cosineSimilarity

        searchBuilder.query(q -> q
                .bool(b -> b
                        // Clause 1: Fuzzy text search on column_name
                        .should(s -> s
                                .match(m -> m
                                        .field("column_name")
                                        .query(request.getKeyword())
                                        .fuzziness("AUTO")
                                )
                        )
                        // Clause 2: Fuzzy text search on column_description_short
                        .should(s -> s
                                .match(m -> m
                                        .field("column_description_short")
                                        .query(request.getKeyword())
                                        .fuzziness("AUTO")
                                )
                        )
                        // Clause 3: Vector similarity search using script_score
                        // Uses cosineSimilarity to compute similarity between query vector and stored vector
                        // Add 1.0 to shift scores from [-1, 1] to [0, 2] range
                        .should(s -> s
                                .scriptScore(ss -> ss
                                        // Base query: match_all to include all documents
                                        .query(mq -> mq.matchAll(ma -> ma))
                                        // Script: cosineSimilarity + 1.0 with embedded vector (no params)
                                        .script(script -> script
                                                .inline(is -> is
                                                        .source("cosineSimilarity(params.query_vector, 'column_description_short_vector') + 1.0")
                                                        .params("query_vector", JsonData.of(vectorList))
                                                )
                                        )
                                )
                        )
                )
        );

        // Step 4: Set source fields to return
        // Include: column_id, column_name, table_name, table_id, column_description, raw_column_description, column_description_short
        // Exclude: column_description_short_vector (dense_vector field)
        searchBuilder.source(sf -> sf
                .filter(f -> f
                        .includes(
                                "column_id",
                                "column_name",
                                "table_name",
                                "table_id",
                                "column_description",
                                "raw_column_description",
                                "column_description_short"
                        )
                )
        );

        // Step 5: Execute the search
        SearchResponse<ObjectNode> response = elasticsearchClient.search(searchBuilder.build(), ObjectNode.class);

        LOG.info("Hybrid vector search executed successfully. Query: {}, Hits: {}",
                request.getKeyword(),
                response.hits().hits().size());
        return response;
    }

    /**
     * Parse the Elasticsearch search response for ES 8.x
     *
     * @param response the search response from Elasticsearch
     * @param request the original search request
     * @return VectorSearchResult parsed from the response
     */
    private VectorSearchResult parseSearchResponse(SearchResponse<ObjectNode> response, VectorSearchRequest request) {
        VectorSearchResult result = new VectorSearchResult();

        // Get total hits count
        if (response.hits().total() != null) {
            result.setResultCount((int) response.hits().total().value());
        }

        // Parse each hit
        for (Hit<ObjectNode> hit : response.hits().hits()) {
            ObjectNode source = hit.source();
            if (source == null) {
                continue;
            }

            // Extract field values
            String columnId = source.has("column_id") ? source.get("column_id").asText(null) : null;
            String columnName = source.has("column_name") ? source.get("column_name").asText(null) : null;
            String columnDesc = source.has("column_desc") ? source.get("column_desc").asText(null) : null;

            // Get score from hit
            double hitScore = hit.score() != null ? hit.score() : 0.0;
            float similarity = normalizeScore((float) hitScore);

            // Create result item
            VectorSearchResultItem item = new VectorSearchResultItem();
            item.setColumnId(columnId);
            item.setColumnName(columnName);
            item.setColumnDesc(columnDesc);
            item.setSimilarity(similarity);

            result.addResult(item);

            LOG.debug("Added search result: columnId={}, columnName={}, similarity={}",
                    columnId, columnName, similarity);
        }

        return result;
    }

    /**
     * Normalize Elasticsearch score to 0-1 range
     *
     * Elasticsearch scores are typically in the range of 0-several hundred depending on the query.
     * This method converts them to a 0-1 scale for consistency.
     *
     * @param score the raw Elasticsearch score
     * @return normalized score in 0-1 range
     */
    private float normalizeScore(float score) {
        // Use sigmoid function to normalize scores
        // This converts any value to a range between 0 and 1
        if (score <= 0) {
            return 0.0f;
        }

        // Simple normalization: divide by (score + 1)
        // This approach gives higher scores more weight while staying in 0-1 range
        return score / (score + 1);
    }

    /**
     * Basic interface for embedding service
     * Provides a contract for converting text to vector embeddings
     */
    public interface EmbeddingService {
        /**
         * Convert text to a vector embedding
         *
         * @param text the text to embed
         * @return array of floats representing the embedding vector
         */
        float[] embed(String text);

        /**
         * Get the dimension of the embedding vector
         *
         * @return the embedding dimension
         */
        int getEmbeddingDimension();
    }

    /**
     * Basic implementation of EmbeddingService
     *
     * This is a simple implementation that generates random vectors as placeholders.
     * In production, this should be replaced with a real embedding service
     * (e.g., OpenAI, Hugging Face, or a local embedding model).
     */
    public static class BasicEmbeddingService implements EmbeddingService {
        private static final Logger LOG = LoggerFactory.getLogger(BasicEmbeddingService.class);
        private final int embeddingDimension = 768;  // Default dimension for most embedding models

        @Override
        public float[] embed(String text) {
            if (text == null || text.isEmpty()) {
                LOG.warn("Empty text provided for embedding, returning zero vector");
                return new float[embeddingDimension];
            }

            // Basic implementation: Use text hash to generate deterministic but simple embeddings
            // In production, replace this with a real embedding service call
            LOG.info("Generating embedding for text: {}", text.substring(0, Math.min(50, text.length())));

            float[] embedding = new float[embeddingDimension];

            // Create a simple hash-based embedding
            int hash = text.hashCode();
            java.util.Random rand = new java.util.Random(hash);

            for (int i = 0; i < embeddingDimension; i++) {
                // Generate random values between -1 and 1
                embedding[i] = (rand.nextFloat() * 2.0f) - 1.0f;
            }

            // Normalize the vector to unit length
            normalizeVector(embedding);

            return embedding;
        }

        @Override
        public int getEmbeddingDimension() {
            return embeddingDimension;
        }

        /**
         * Normalize a vector to unit length
         *
         * @param vector the vector to normalize
         */
        private void normalizeVector(float[] vector) {
            float norm = 0.0f;
            for (float v : vector) {
                norm += v * v;
            }
            norm = (float) Math.sqrt(norm);

            if (norm > 0.0f) {
                for (int i = 0; i < vector.length; i++) {
                    vector[i] /= norm;
                }
            }
        }
    }
}
