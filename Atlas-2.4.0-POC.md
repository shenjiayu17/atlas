## 环境准备

1 ES已安装，ES index已创建并添加数据，索引结构已定义

2 Atlas环境已部署

以索引`column_catalog_1119_es`为例

```json
{
    "mappings": {
        "properties": {
            "column_id": {"type": "keyword"},
            "column_name": {"type": "keyword"},
            "table_name": {"type": "keyword"},
            "table_id": {"type": "keyword"},
            "table_description": {
                "type": "text",
                "analyzer": "standard"
            },
            "raw_column_description": {
                "type": "text",
                "analyzer": "standard"
            },
            "column_description_short": {
                "type": "text",
                "analyzer": "standard"
            },
            "column_description": {
                "type": "text",
                "analyzer": "standard"
            },
            "value_info": {"type": "object"},
            "relationship": {"type": "object"},
            "usage": {"type": "object"},
            "business_terms": {
                "type": "text",
                "analyzer": "standard"
            },
            "table_description_vector": {
                "type": "dense_vector",
                "dims": "params.vector_dim",
                "similarity": "cosine"
            },
            "column_description_short_vector": {
                "type": "dense_vector",
                "dims": "params.vector_dim",
                "similarity": "cosine"
            },
        }
    }
}
```



## 接口测试

1 新增向量检索接口，新增API文件。

POC临时测功能可行性，尽量少改动，思路如下：

- 输入query和index_name从QueryParam传入，暂时用已有索引

- EmbeddingService暂时内置，随机产生指定长度的vector

- Elasticsearch client利用官方推荐依赖 co.elastic.clients:elasticsearch-java（注意es 7和es 8依赖的客户端不同），定义请求体结构，解析响应体结构

- 检索请求体仿照finance_nl2sql项目构造，按索引`column_catalog_1119_es`结构

  ```json
  {
      "bool": {
          "should": [
          	{
                  "match": {
                      "column_name": {
                          "query": keyword,
                          "fuzziness": "AUTO"
                      }
                  }
              },
              {
                  "match": {
                      "column_description_short": {
                          "query": keyword,
                          "fuzziness": "AUTO",
                      }
                  }
              },
              {
                  "script_score": {
                      "query": {"match_all": {}},
                      "script": {
                          "source": "cosineSimilarity(params.query_vector, 'column_description_short_vector') + 1.0",
                          "params": {"query_vector": params.query_vector},
                      },
                  }
              }
          ],
          "minimum_should_match": 1
      }
  }
  ```

  

2 编译工程，定点换包 atlas-webapp（服务层入口），atlas-repository（Service实现），atlas-intg（配置参数）

3 测试后端接口



curl -X GET -H 'Accept: application/json' -u admin:admin \
 'http://8.92.9.185:21000/api/atlas/v2/search/vector/search?keyword=customer&indexName=column_catalog_1119_es'

{"keyword":"customer","resultCount":41,"results":[{"columnId":"BDSP_EDW.S00_ICBC_INT_ORG_OVERVIEW_H.INT_ORG_ID_BRANCH","columnName":"INT_ORG_ID_BRANCH","columnDesc":null,"similarity":0.52172124},{"columnId":"BDSP_EDW.S00_ICBC_INT_ORG_OVERVIEW_H.INT_ORG_ID_SUBBRANCH","columnName":"INT_ORG_ID_SUBBRANCH","columnDesc":null,"similarity":0.52091044},{"columnId":"BDSP_EDW.S00_ICBC_INT_ORG_OVERVIEW_H.INT_ORG_ID_PROBRANCH","columnName":"INT_ORG_ID_PROBRANCH","columnDesc":null,"similarity":0.51997185},{"columnId":"BDSP_EDW.S00_ICBC_INT_ORG_OVERVIEW_H.INT_ORG_ID_SUBBRANCH_NM","columnName":"INT_ORG_ID_SUBBRANCH_NM","columnDesc":null,"similarity":0.5182977},{"columnId":"BDSP_EDW.S00_ICBC_INT_ORG_OVERVIEW_H.INT_ORG_ID_NM","columnName":"INT_ORG_ID_NM","columnDesc":null,"similarity":0.51785153},{"columnId":"BDSP_EDW.S00_ICBC_INT_ORG_OVERVIEW_H.INT_ORG_ID_PROBRANCH_NM","columnName":"INT_ORG_ID_PROBRANCH_NM","columnDesc":null,"similarity":0.51725376},{"columnId":"BDSP_EDW.S00_ICBC_INT_ORG_OVERVIEW_H.INT_ORG_ID_BRANCH_NM","columnName":"INT_ORG_ID_BRANCH_NM","columnDesc":null,"similarity":0.5160554},{"columnId":"BDSP_EDW.S00_ICBC_INT_ORG_OVERVIEW_H.INT_ORG_SUBBRANCH_ADDR","columnName":"INT_ORG_SUBBRANCH_ADDR","columnDesc":null,"similarity":0.5148389},{"columnId":"BDSP_SPCP.T80_PC8_CPS_PBK.PT_DT","columnName":"PT_DT","columnDesc":null,"similarity":0.51474255},{"columnId":"BDSP_EDW.S00_ICBC_INT_ORG_OVERVIEW_H.INT_ORG_SUBBRANCH_TEL","columnName":"INT_ORG_SUBBRANCH_TEL","columnDesc":null,"similarity":0.51465225}],"limit":10,"offset":0,"indexName":"column_catalog_1119_es","executionTimeMs":590}