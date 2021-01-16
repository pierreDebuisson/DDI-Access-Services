package fr.insee.rmes.metadata.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.http.entity.ContentType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import fr.insee.rmes.metadata.model.ColecticaItem;
import fr.insee.rmes.metadata.model.ColecticaItemPostRef;
import fr.insee.rmes.metadata.model.ColecticaItemPostRefList;
import fr.insee.rmes.metadata.model.ColecticaItemRef;
import fr.insee.rmes.metadata.model.ColecticaItemRefList;
import fr.insee.rmes.metadata.model.Relationship;
import fr.insee.rmes.metadata.model.ObjectColecticaPost;
import fr.insee.rmes.metadata.model.Unit;

@Service
public class MetadataClientImpl implements MetadataClient {

	private static final Logger logger = LogManager.getLogger(MetadataClientImpl.class);

	@Autowired
	RestTemplate restTemplate;

	@Value("${fr.insee.rmes.api.remote.metadata.url}")
	String serviceUrl;

	@Value("${fr.insee.rmes.api.remote.metadata.agency}")
	String agency;

	@Value("${fr.insee.rmes.api.remote.metadata.key}")
	String apiKey;

	public ColecticaItem getItem(String id) throws Exception {
		String url = String.format("%s/api/v1/item/%s/%s?api_key=%s", serviceUrl, agency, id, apiKey);
		logger.info("GET Item on " + id);
		return restTemplate.getForObject(url, ColecticaItem.class);
	}

	public List<ColecticaItem> getItems(ColecticaItemRefList query) throws Exception {
		String url = String.format("%s/api/v1/item/_getList?api_key=%s", serviceUrl, apiKey);
		MultiValueMap<String, String> headers = new LinkedMultiValueMap<String, String>();
		headers.add("Content-type", ContentType.APPLICATION_JSON.getMimeType());
		HttpEntity<ColecticaItemRefList> request = new HttpEntity<>(query, headers);
		ResponseEntity<ColecticaItem[]> response = restTemplate.exchange(url, HttpMethod.POST, request,
				ColecticaItem[].class);
		logger.info("GET Items with query : " + query.toString());
		return Arrays.asList(response.getBody());
	}

	public ColecticaItemRefList getChildrenRef(String id) throws Exception {
		String url = String.format("%s/api/v1/set/%s/%s?api_key=%s", serviceUrl, agency, id, apiKey);
		ResponseEntity<ColecticaItemRef.Unformatted[]> response;
		response = restTemplate.exchange(url, HttpMethod.GET, null, ColecticaItemRef.Unformatted[].class);
		List<ColecticaItemRef> refs = Arrays.asList(response.getBody()).stream()
				.map(unformatted -> unformatted.format()).collect(Collectors.toList());
		logger.info("Get ChildrenRef for id : " + id);
		return new ColecticaItemRefList(refs);
	}

	public Integer getLastestVersionItem(String id) throws Exception {
		String url = String.format("%s/api/v1/item/%s/%s/versions/_latest?api_key=%s", serviceUrl, agency, id, apiKey);
		logger.info("GET LastestVersion for Item " + id);
		return restTemplate.getForObject(url, Integer.class);

	}

	public List<Unit> getUnits() throws Exception {

		// Fake
		List<Unit> units = new ArrayList<Unit>();
		Unit unit1 = new Unit();
		unit1.setLabel("€");
		unit1.setUri("http://id.insee.fr/unit/euro");
		units.add(unit1);
		Unit unit2 = new Unit();
		unit2.setLabel("k€");
		unit2.setUri("http://id.insee.fr/unit/keuro");
		units.add(unit2);
		Unit unit3 = new Unit();
		unit3.setLabel("%");
		unit3.setUri("http://id.insee.fr/unit/percent");
		units.add(unit3);
		return units;
	}

	@Override
	public String postItems(ColecticaItemPostRefList colecticaItemsList) throws Exception {
		String url = String.format("%s/api/v1/item?api_key=%s", serviceUrl, apiKey);

		MultiValueMap<String, String> headers = new LinkedMultiValueMap<String, String>();
		headers.add("Content-type", ContentType.APPLICATION_JSON.getMimeType());
		HttpEntity<ColecticaItemPostRefList> request = new HttpEntity<>(colecticaItemsList, headers);
		ResponseEntity<ColecticaItem[]> response = restTemplate.exchange(url, HttpMethod.POST, request,
				ColecticaItem[].class);
		return response.getStatusCode().toString();
	}

	@Override
	public String postItem(ColecticaItemPostRef ref) {

		List<ColecticaItemPostRef> items = new ArrayList<ColecticaItemPostRef>();
		ColecticaItemPostRefList colecticaItemsList = new ColecticaItemPostRefList();
		colecticaItemsList.setItems(items);
		String url = String.format("%s/api/v1/item?api_key=%s", serviceUrl, apiKey);
		MultiValueMap<String, String> headers = new LinkedMultiValueMap<String, String>();
		headers.add("Content-type", ContentType.APPLICATION_JSON.getMimeType());
		HttpEntity<ColecticaItemPostRefList> request = new HttpEntity<>(colecticaItemsList, headers);
		ResponseEntity<ColecticaItem[]> response = restTemplate.exchange(url, HttpMethod.POST, request,
				ColecticaItem[].class);
		return response.getStatusCode().toString();

	}

	@Override
	public Relationship[] getRelationship(ObjectColecticaPost objectColecticaPost) {
		String url = String.format("%s/api/v1/_query/relationship/byobject?api_key=%s", serviceUrl, apiKey);

		MultiValueMap<String, String> headers = new LinkedMultiValueMap<String, String>();
		headers.add("Content-type", ContentType.APPLICATION_JSON.getMimeType());
		HttpEntity<ObjectColecticaPost> request = new HttpEntity<>(objectColecticaPost, headers);
		ResponseEntity<Relationship[]> response = restTemplate.exchange(url, HttpMethod.POST, request,
				Relationship[].class);
		return response.getBody();
	}

	@Override
	public Relationship[] getRelationshipChildren(ObjectColecticaPost objectColecticaPost) {
		String url = String.format("%s/api/v1/_query/relationship/bysubject?api_key=%s", serviceUrl, apiKey);

		MultiValueMap<String, String> headers = new LinkedMultiValueMap<String, String>();
		headers.add("Content-type", ContentType.APPLICATION_JSON.getMimeType());
		HttpEntity<ObjectColecticaPost> request = new HttpEntity<>(objectColecticaPost, headers);
		ResponseEntity<Relationship[]> response = restTemplate.exchange(url, HttpMethod.POST, request,
				Relationship[].class);
		return response.getBody();
	}

}
