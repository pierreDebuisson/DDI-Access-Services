package fr.insee.rmes.search.repository;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.databind.ObjectMapper;

import fr.insee.rmes.config.DDIItemRepositoryImplCondition;
import fr.insee.rmes.search.model.DDIItem;
import fr.insee.rmes.search.model.DDIQuery;
import fr.insee.rmes.search.model.DataCollectionContext;
import fr.insee.rmes.search.model.ResponseItem;
import fr.insee.rmes.search.model.ResponseSearchItem;

@Repository
@Conditional(value = DDIItemRepositoryImplCondition.class)
public class DDIItemRepositoryImpl implements DDIItemRepository {

	@Value("${fr.insee.rmes.elasticsearch.index.name}")
	String index;

	@Autowired
	RestHighLevelClient client;

	@Override
	public IndexResponse save(String type, ResponseItem item) throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		byte[] data = mapper.writeValueAsBytes(item);
		IndexRequest request = new IndexRequest(index, type, item.getId()).source(data, XContentType.JSON);
		return client.index(request);
	}

	@Override
	public List<DDIItem> findByLabel(String label, String... types) throws Exception {
		SearchSourceBuilder srcBuilder = new SearchSourceBuilder()
				.query(QueryBuilders.fuzzyQuery("label", label).maxExpansions(1).prefixLength(3));
		SearchRequest request = new SearchRequest().indices(index).types(types).source(srcBuilder);
		return mapResponse(client.search(request));
	}

	@Override
	public List<DDIItem> findByLabelInSubGroup(String label, String subgroupId, String... types) throws Exception {
		SearchSourceBuilder srcBuilder = new SearchSourceBuilder()
				.query(QueryBuilders.boolQuery()
						.filter(QueryBuilders.fuzzyQuery("label", label).maxExpansions(1)
								.prefixLength(label.length() - 2))
						.filter(QueryBuilders.termQuery("subGroupId.keyword", subgroupId)));
		SearchRequest request = new SearchRequest().indices(index).types(types).source(srcBuilder);
		return mapResponse(client.search(request));
	}

	@Override
	public List<DDIItem> getSubGroups() throws Exception {
		SearchRequest request = new SearchRequest().indices(index).types("sub-group");
		return mapResponse(client.search(request));
	}

	@Override
	public List<DDIItem> getStudyUnits(String subgGroupId) throws Exception {
		SearchRequest request = new SearchRequest().indices(index).types("study-unit");
		if (subgGroupId != null) {
			SearchSourceBuilder srcBuilder = new SearchSourceBuilder()
					.query(QueryBuilders.termQuery("parent", subgGroupId));
			request.source(srcBuilder);
		}
		return mapResponse(client.search(request));
	}

	@Override
	public List<DDIItem> getDataCollections(String studyUnitId) throws Exception {
		SearchSourceBuilder srcBuilder = new SearchSourceBuilder()
				.query(QueryBuilders.termQuery("parent", studyUnitId));
		SearchRequest request = new SearchRequest().indices(index).types("data-collection").source(srcBuilder);
		return mapResponse(client.search(request));
	}

	@Override
	public DeleteResponse delete(String type, String id) throws Exception {
		DeleteRequest request = new DeleteRequest(index, type, id);
		return client.delete(request);
	}

	private List<DDIItem> mapResponse(SearchResponse response) {
		List<SearchHit> esHits = Arrays.asList(response.getHits().getHits());
		return esHits.stream().map(hit -> {
			DDIItem item = new DDIItem(hit.getId(), hit.getSource().get("label").toString(),
					hit.getSource().get("parent").toString(), hit.getType());
			item.setGroupId(getHitValueOrNull(hit, "groupId"));
			item.setSubGroupId(getHitValueOrNull(hit, "subGroupId"));
			item.setStudyUnitId(getHitValueOrNull(hit, "studyUnitId"));
			item.setDataCollectionId(getHitValueOrNull(hit, "dataCollectionId"));
			item.setResourcePackageId(getHitValueOrNull(hit, "resourcePackageId"));
			return item;
		}).collect(Collectors.toList());
	}

	private String getHitValueOrNull(SearchHit hit, String field) {
		if (null == hit.getSource().get(field)) {
			return null;
		}
		return hit.getSource().get(field).toString();
	}

	@Override
	public DataCollectionContext getDataCollectionContext(String dataCollectionId) throws Exception {
		// TODO
		return null;
	}

	@Override
	public List<ResponseSearchItem> getItemsByCriteria(String subgroupId, String operationId, String dataCollectionId,
			DDIQuery criteria) throws Exception {
		// TODO
		return null;
	}

	@Override
	public void deleteAll() throws Exception {
		// TODO Auto-generated method stub
	}

	@Override
	public DDIItem getItemById(String id) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<DDIItem> getGroups() throws Exception {
		SearchRequest request = new SearchRequest().indices(index).types("group");
		return mapResponse(client.search(request));
	}
}
