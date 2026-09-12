package org.maven.repository;

import org.maven.document.ProductDoc;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface ProductRepository extends ElasticsearchRepository<ProductDoc, String> {
}
