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

import org.apache.atlas.exception.AtlasBaseException;

/**
 * Service interface for performing vector similarity search operations.
 * 
 * This service provides an abstraction for vector-based search capabilities,
 * allowing clients to search for semantically similar items based on vector embeddings.
 * The implementation is responsible for handling the interaction with the vector search backend
 * (e.g., Elasticsearch with vector/kNN support).
 * 
 * Key Features:
 * - Vector similarity search based on embeddings
 * - Support for pagination (limit and offset)
 * - Configurable minimum similarity score threshold
 * - Support for custom index selection
 * 
 * @author Apache Atlas
 * @version 1.0
 */
public interface VectorSearchService {

    /**
     * Perform a vector similarity search.
     * 
     * This method executes a vector-based similarity search on the specified index.
     * The search uses vector embeddings to find semantically similar items, supporting
     * operations like k-Nearest Neighbors (kNN) search.
     * 
     * The search process typically involves:
     * 1. Converting the search keyword to a vector embedding (if applicable)
     * 2. Executing a vector similarity search against the backend
     * 3. Filtering results based on the minimum similarity score threshold
     * 4. Applying pagination using limit and offset
     * 5. Normalizing scores to a [0, 1] range
     * 
     * @param request The vector search request containing:
     *                <ul>
     *                  <li><b>keyword</b> (required): The search query/keyword that will be
     *                      converted to a vector embedding or used for text-based matching.
     *                      Must not be null or empty. Maximum length is typically 4096 characters.</li>
     *                  <li><b>limit</b> (optional): Maximum number of results to return.
     *                      Default is 10. Must be greater than 0.</li>
     *                  <li><b>offset</b> (optional): Number of results to skip for pagination.
     *                      Default is 0. Must be non-negative.</li>
     *                  <li><b>indexName</b> (optional): The name of the vector search index.
     *                      Default is "columns". Must match pattern: ^[a-zA-Z0-9._-]+$</li>
     *                  <li><b>minScore</b> (optional): Minimum similarity score threshold.
     *                      Default is 0.0. Valid range is [0.0, 1.0].</li>
     *                </ul>
     * 
     * @return A VectorSearchResult object containing:
     *         <ul>
     *           <li><b>keyword</b>: The original search keyword</li>
     *           <li><b>resultCount</b>: Total number of results found (not limited by pagination)</li>
     *           <li><b>results</b>: List of search result items (limited by the 'limit' parameter,
     *               starting from the 'offset' position)</li>
     *           <li><b>limit</b>: The pagination limit value used</li>
     *           <li><b>offset</b>: The pagination offset value used</li>
     *           <li><b>indexName</b>: The index that was searched</li>
     *           <li><b>executionTimeMs</b>: Time taken to execute the search in milliseconds</li>
     *         </ul>
     *         
     *         Each result item in the results list contains:
     *         <ul>
     *           <li><b>columnId</b>: Unique identifier of the result item</li>
     *           <li><b>columnName</b>: Display name of the result item</li>
     *           <li><b>columnDesc</b>: Description of the result item</li>
     *           <li><b>similarity</b>: Similarity score in range [0.0, 1.0] where 1.0 is most similar</li>
     *         </ul>
     * 
     * @throws AtlasBaseException if:
     *         <ul>
     *           <li>The request is null or invalid</li>
     *           <li>The keyword is null, empty, or exceeds maximum length</li>
     *           <li>The limit is <= 0</li>
     *           <li>The offset is negative</li>
     *           <li>The search backend is unavailable</li>
     *           <li>The specified index does not exist</li>
     *           <li>An error occurs during the search execution</li>
     *         </ul>
     * 
     * @example
     * <pre>
     * VectorSearchRequest request = new VectorSearchRequest(
     *     "customer information",  // keyword
     *     10,                      // limit
     *     0,                       // offset
     *     "columns",               // indexName
     *     0.5f                     // minScore
     * );
     * 
     * VectorSearchResult result = vectorSearchService.vectorSearch(request);
     * 
     * System.out.println("Found " + result.getResultCount() + " results");
     * for (VectorSearchResult.VectorSearchResultItem item : result.getResults()) {
     *     System.out.println(item.getColumnName() + " (similarity: " + item.getSimilarity() + ")");
     * }
     * </pre>
     * 
     * @see VectorSearchRequest
     * @see VectorSearchResult
     * @see VectorSearchResult.VectorSearchResultItem
     * @see AtlasBaseException
     */
    VectorSearchResult vectorSearch(VectorSearchRequest request) throws AtlasBaseException;

}
