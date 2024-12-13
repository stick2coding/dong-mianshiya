package com.dong.mianshiya.es;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.UpdateQuery;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest
public class ElasticsearchRestTemplateTest {

    @Resource
    private ElasticsearchRestTemplate elasticsearchRestTemplate;

    private final String INDEX_NAME = "test_index";

    /**
     * 在索引下创建一条记录
     * @throws Exception
     */
    @Test
    public void indexDocument() throws Exception {
        Map<String, Object> document = new HashMap<>();
        document.put("title", "zhe shi biao ti");
        document.put("content", "zhe shi neirong");
        document.put("tags", "tag1,tag2");
        document.put("answer", "dong");
        document.put("userId", 1L);
        document.put("createTime", "2023-09-01 10:00:00");
        document.put("editTime", "2023-09-01 10:00:00");
        document.put("updateTime", "2023-09-01 10:00:00");
        document.put("isDelete", false);

        IndexQuery indexQuery = new IndexQuery();
        indexQuery.setId("1");
        indexQuery.setObject(document);

        String docId = elasticsearchRestTemplate.index(indexQuery, IndexCoordinates.of(INDEX_NAME));

        assertThat(docId).isNotNull();
    }


    @Test
    public void getDocument() throws Exception {
        String docId = "1";
        Map<String, Object> document = elasticsearchRestTemplate.get(docId, Map.class, IndexCoordinates.of(INDEX_NAME));
        assertThat(document).isNotNull();
        assertThat(document.get("title")).isEqualTo("zhe shi biao ti");
    }

    @Test
    public void updateDocument() {
        String docId = "1";
        Map<String, Object> updates = new HashMap<>();
        updates.put("title", "zhe shi gai bian biao ti");
        updates.put("updateTime", "2024-09-01 10:00:00");

        UpdateQuery updateQuery = UpdateQuery.builder(docId)
                .withDocument(Document.from(updates))
                .build();

        elasticsearchRestTemplate.update(updateQuery, IndexCoordinates.of(INDEX_NAME));
        Map<String, Object> document = elasticsearchRestTemplate.get(docId, Map.class, IndexCoordinates.of(INDEX_NAME));
        assertThat(document.get("title")).isEqualTo("zhe shi gai bian biao ti");
    }

    @Test
    public void deleteDocuments() {
        String docId = "1";
        String result = elasticsearchRestTemplate.delete(docId, IndexCoordinates.of(INDEX_NAME));
        assertThat(result).isNotNull();
    }

    @Test
    public void deleteIndex() {
        IndexOperations indexOperation = elasticsearchRestTemplate.indexOps(IndexCoordinates.of(INDEX_NAME));
        boolean result = indexOperation.delete();
        assertThat(result).isTrue();
    }

}
