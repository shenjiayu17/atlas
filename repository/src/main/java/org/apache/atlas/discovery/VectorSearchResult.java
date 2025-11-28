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
import java.util.ArrayList;
import java.util.List;

/**
 * Result model for vector similarity search on Elasticsearch
 * 
 * This class encapsulates the results of a vector search operation,
 * including the retrieved column information and similarity scores.
 */
public class VectorSearchResult {
    
    /**
     * The search keyword that was used
     */
    @JsonProperty("keyword")
    private String keyword;
    
    /**
     * Total number of results found
     */
    @JsonProperty("resultCount")
    private int resultCount;
    
    /**
     * List of search result items
     */
    @JsonProperty("results")
    private List<VectorSearchResultItem> results;
    
    /**
     * The limit (maximum results per page)
     */
    @JsonProperty("limit")
    private int limit;
    
    /**
     * The offset used in the search
     */
    @JsonProperty("offset")
    private int offset;
    
    /**
     * The index name that was searched
     */
    @JsonProperty("indexName")
    private String indexName;
    
    /**
     * Search execution time in milliseconds
     */
    @JsonProperty("executionTimeMs")
    private long executionTimeMs;

    // ============ Constructors ============
    
    public VectorSearchResult() {
        this.results = new ArrayList<>();
    }

    public VectorSearchResult(String keyword, int limit, int offset) {
        this.keyword = keyword;
        this.limit = limit;
        this.offset = offset;
        this.results = new ArrayList<>();
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
     * Get the total result count
     * @return the result count
     */
    public int getResultCount() {
        return resultCount;
    }

    /**
     * Set the total result count
     * @param resultCount the result count to set
     */
    public void setResultCount(int resultCount) {
        this.resultCount = resultCount;
    }

    /**
     * Get the search results
     * @return list of result items
     */
    public List<VectorSearchResultItem> getResults() {
        return results;
    }

    /**
     * Set the search results
     * @param results the results to set
     */
    public void setResults(List<VectorSearchResultItem> results) {
        this.results = results;
    }

    /**
     * Add a single result item
     * @param item the result item to add
     */
    public void addResult(VectorSearchResultItem item) {
        if (this.results == null) {
            this.results = new ArrayList<>();
        }
        this.results.add(item);
    }

    /**
     * Get the limit
     * @return the limit
     */
    public int getLimit() {
        return limit;
    }

    /**
     * Set the limit
     * @param limit the limit to set
     */
    public void setLimit(int limit) {
        this.limit = limit;
    }

    /**
     * Get the offset
     * @return the offset
     */
    public int getOffset() {
        return offset;
    }

    /**
     * Set the offset
     * @param offset the offset to set
     */
    public void setOffset(int offset) {
        this.offset = offset;
    }

    /**
     * Get the index name
     * @return the index name
     */
    public String getIndexName() {
        return indexName;
    }

    /**
     * Set the index name
     * @param indexName the index name to set
     */
    public void setIndexName(String indexName) {
        this.indexName = indexName;
    }

    /**
     * Get the execution time
     * @return the execution time in milliseconds
     */
    public long getExecutionTimeMs() {
        return executionTimeMs;
    }

    /**
     * Set the execution time
     * @param executionTimeMs the execution time in milliseconds
     */
    public void setExecutionTimeMs(long executionTimeMs) {
        this.executionTimeMs = executionTimeMs;
    }

    // ============ toString ============
    
    @Override
    public String toString() {
        return "VectorSearchResult{" +
                "keyword='" + keyword + '\'' +
                ", resultCount=" + resultCount +
                ", results=" + results +
                ", limit=" + limit +
                ", offset=" + offset +
                ", indexName='" + indexName + '\'' +
                ", executionTimeMs=" + executionTimeMs +
                '}';
    }

    /**
     * Inner class representing a single search result item
     */
    public static class VectorSearchResultItem {
        
        /**
         * Column ID
         */
        @JsonProperty("columnId")
        private String columnId;
        
        /**
         * Column name
         */
        @JsonProperty("columnName")
        private String columnName;
        
        /**
         * Column description
         */
        @JsonProperty("columnDesc")
        private String columnDesc;
        
        /**
         * Similarity score (0.0 - 1.0, higher is more similar)
         */
        @JsonProperty("similarity")
        private float similarity;

        // ============ Constructors ============
        
        public VectorSearchResultItem() {
        }

        public VectorSearchResultItem(String columnId, String columnName, String columnDesc, float similarity) {
            this.columnId = columnId;
            this.columnName = columnName;
            this.columnDesc = columnDesc;
            this.similarity = similarity;
        }

        // ============ Getters and Setters ============

        /**
         * Get the column ID
         * @return the column ID
         */
        public String getColumnId() {
            return columnId;
        }

        /**
         * Set the column ID
         * @param columnId the column ID to set
         */
        public void setColumnId(String columnId) {
            this.columnId = columnId;
        }

        /**
         * Get the column name
         * @return the column name
         */
        public String getColumnName() {
            return columnName;
        }

        /**
         * Set the column name
         * @param columnName the column name to set
         */
        public void setColumnName(String columnName) {
            this.columnName = columnName;
        }

        /**
         * Get the column description
         * @return the column description
         */
        public String getColumnDesc() {
            return columnDesc;
        }

        /**
         * Set the column description
         * @param columnDesc the column description to set
         */
        public void setColumnDesc(String columnDesc) {
            this.columnDesc = columnDesc;
        }

        /**
         * Get the similarity score
         * @return the similarity score (0.0 - 1.0)
         */
        public float getSimilarity() {
            return similarity;
        }

        /**
         * Set the similarity score
         * @param similarity the similarity score to set (0.0 - 1.0)
         */
        public void setSimilarity(float similarity) {
            this.similarity = similarity;
        }

        // ============ toString ============
        
        @Override
        public String toString() {
            return "VectorSearchResultItem{" +
                    "columnId='" + columnId + '\'' +
                    ", columnName='" + columnName + '\'' +
                    ", columnDesc='" + columnDesc + '\'' +
                    ", similarity=" + similarity +
                    '}';
        }
    }
}
