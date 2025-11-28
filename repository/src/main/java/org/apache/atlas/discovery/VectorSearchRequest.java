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

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request model for vector similarity search on Elasticsearch
 * 
 * This class encapsulates the parameters for performing a vector search
 * using Elasticsearch's kNN functionality.
 */
public class VectorSearchRequest {
    
    /**
     * The search keyword or query string
     * This will be converted to a vector embedding for similarity search
     */
    @JsonProperty("keyword")
    private String keyword;
    
    /**
     * Maximum number of results to return (default: 10)
     */
    @JsonProperty("limit")
    private int limit = 10;
    
    /**
     * Pagination offset (default: 0)
     */
    @JsonProperty("offset")
    private int offset = 0;
    
    /**
     * The Elasticsearch index name to search in (default: "columns")
     */
    @JsonProperty("indexName")
    private String indexName = "columns";
    
    /**
     * The minimum similarity score threshold (0.0 - 1.0, default: 0.0)
     * Results with similarity score below this threshold are filtered out
     */
    @JsonProperty("minScore")
    private float minScore = 0.0f;

    // ============ Constructors ============
    
    public VectorSearchRequest() {
    }

    public VectorSearchRequest(String keyword, int limit, int offset) {
        this.keyword = keyword;
        this.limit = limit;
        this.offset = offset;
    }

    public VectorSearchRequest(String keyword, int limit, int offset, String indexName) {
        this.keyword = keyword;
        this.limit = limit;
        this.offset = offset;
        this.indexName = indexName;
    }

    public VectorSearchRequest(String keyword, int limit, int offset, String indexName, float minScore) {
        this.keyword = keyword;
        this.limit = limit;
        this.offset = offset;
        this.indexName = indexName;
        this.minScore = minScore;
    }

    // ============ Getters and Setters ============

    /**
     * Get the search keyword
     * @return the keyword
     */
    public String getKeyword() {
        return keyword;
    }

    /**
     * Set the search keyword
     * @param keyword the keyword to set
     */
    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    /**
     * Get the limit (maximum number of results)
     * @return the limit
     */
    public int getLimit() {
        return limit;
    }

    /**
     * Set the limit (maximum number of results)
     * @param limit the limit to set
     */
    public void setLimit(int limit) {
        this.limit = limit;
    }

    /**
     * Get the offset (pagination offset)
     * @return the offset
     */
    public int getOffset() {
        return offset;
    }

    /**
     * Set the offset (pagination offset)
     * @param offset the offset to set
     */
    public void setOffset(int offset) {
        this.offset = offset;
    }

    /**
     * Get the Elasticsearch index name
     * @return the index name
     */
    public String getIndexName() {
        return indexName;
    }

    /**
     * Set the Elasticsearch index name
     * @param indexName the index name to set
     */
    public void setIndexName(String indexName) {
        this.indexName = indexName;
    }

    /**
     * Get the minimum similarity score threshold
     * @return the minimum score
     */
    public float getMinScore() {
        return minScore;
    }

    /**
     * Set the minimum similarity score threshold
     * @param minScore the minimum score to set (0.0 - 1.0)
     */
    public void setMinScore(float minScore) {
        this.minScore = minScore;
    }

    // ============ toString ============
    
    @Override
    public String toString() {
        return "VectorSearchRequest{" +
                "keyword='" + keyword + '\'' +
                ", limit=" + limit +
                ", offset=" + offset +
                ", indexName='" + indexName + '\'' +
                ", minScore=" + minScore +
                '}';
    }
}
