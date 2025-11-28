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
package org.apache.atlas.web.rest;

import org.apache.atlas.annotation.Timed;
import org.apache.atlas.discovery.VectorSearchService;
import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.discovery.VectorSearchRequest;
import org.apache.atlas.discovery.VectorSearchResult;
import org.apache.atlas.repository.Constants;
import org.apache.atlas.utils.AtlasPerfTracer;
import org.apache.atlas.web.util.Servlets;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

/**
 * REST interface for vector similarity search using Elasticsearch
 * 
 * This REST controller provides vector search capabilities leveraging Elasticsearch's
 * vector search functionality (kNN search). It allows searching for semantically similar
 * items based on vector embeddings.
 */
@Path("v2/search/vector")
@Singleton
@Service
@Consumes({Servlets.JSON_MEDIA_TYPE, MediaType.APPLICATION_JSON})
@Produces({Servlets.JSON_MEDIA_TYPE, MediaType.APPLICATION_JSON})
public class VectorSearchREST {
    private static final Logger PERF_LOG = AtlasPerfTracer.getPerfLogger("rest.VectorSearchREST");

    @Context
    private HttpServletRequest httpServletRequest;

    private final int maxVectorQueryLength;
    private final VectorSearchService vectorSearchService;

    @Inject
    public VectorSearchREST(VectorSearchService vectorSearchService, Configuration configuration) {
        this.vectorSearchService = vectorSearchService;
        this.maxVectorQueryLength = configuration.getInt(Constants.MAX_FULLTEXT_QUERY_STR_LENGTH, 4096);
    }

    /**
     * Perform a vector similarity search on Elasticsearch index
     * 
     * This endpoint searches for semantically similar columns based on vector embeddings.
     * The search uses Elasticsearch's kNN (k-Nearest Neighbors) functionality to find
     * the most relevant results based on vector distance.
     *
     * @param keyword The search keyword/query (required)
     * @param limit Maximum number of results to return (optional, default: 10)
     * @param offset Pagination offset (optional, default: 0)
     * @param indexName The Elasticsearch index name (optional, default: "columns")
     * @param minScore Minimum similarity score threshold (optional, default: 0.0)
     * @return VectorSearchResult containing the search results with column information
     * @throws AtlasBaseException if validation fails or search encounters an error
     * 
     * @HTTP 200 Successful vector search with results
     * @HTTP 400 Invalid request parameters or search keyword
     * @HTTP 500 Search service error
     * 
     * Example Request:
     * GET /api/atlas/v2/search/vector/search?keyword=customer&limit=10&offset=0&indexName=columns
     * 
     * Example Response:
     * {
     *   "keyword": "customer",
     *   "resultCount": 5,
     *   "results": [
     *     {
     *       "columnId": "col_123",
     *       "columnName": "customer_id",
     *       "columnDesc": "Unique customer identifier",
     *       "similarity": 0.95
     *     },
     *     ...
     *   ],
     *   "limit": 10,
     *   "offset": 0,
     *   "executionTimeMs": 45
     * }
     */
    @GET
    @Path("search")
    @Timed
    public VectorSearchResult vectorSearch(
            @QueryParam("keyword") String keyword,
            @QueryParam("limit") @DefaultValue("10") Integer limit,
            @QueryParam("offset") @DefaultValue("0") Integer offset,
            @QueryParam("indexName") @DefaultValue("columns") String indexName,
            @QueryParam("minScore") @DefaultValue("0.0") Float minScore) throws AtlasBaseException {
        
        // Create request object from query parameters
        VectorSearchRequest request = new VectorSearchRequest(keyword, limit, offset, indexName, minScore);
        validateVectorSearchRequest(request);

        AtlasPerfTracer perf = null;
        try {
            if (AtlasPerfTracer.isPerfTraceEnabled(PERF_LOG)) {
                perf = AtlasPerfTracer.getPerfTracer(PERF_LOG, 
                    "VectorSearchREST.vectorSearch(keyword=" + keyword + 
                    ", limit=" + limit + ", offset=" + offset + ")");
            }

            return vectorSearchService.vectorSearch(request);
        } finally {
            AtlasPerfTracer.log(perf);
        }
    }

    /**
     * Validate vector search request parameters
     *
     * @param request The vector search request to validate
     * @throws AtlasBaseException if validation fails
     */
    private void validateVectorSearchRequest(VectorSearchRequest request) throws AtlasBaseException {
        // Validate keyword
        if (request == null) {
            throw new AtlasBaseException("Vector search request cannot be null");
        }

        if (StringUtils.isEmpty(request.getKeyword())) {
            throw new AtlasBaseException("Vector search keyword cannot be empty");
        }

        // Validate keyword length
        if (request.getKeyword().length() > maxVectorQueryLength) {
            throw new AtlasBaseException(
                "Vector search keyword length exceeds maximum allowed length: " + maxVectorQueryLength);
        }

        // Validate limit (should be positive)
        if (request.getLimit() <= 0) {
            throw new AtlasBaseException("Vector search limit must be greater than 0");
        }

        // Validate offset (should be non-negative)
        if (request.getOffset() < 0) {
            throw new AtlasBaseException("Vector search offset cannot be negative");
        }

        // Validate index name if provided
        if (StringUtils.isNotEmpty(request.getIndexName()) && 
            !request.getIndexName().matches("^[a-zA-Z0-9._-]+$")) {
            throw new AtlasBaseException("Invalid Elasticsearch index name format");
        }
    }
}
