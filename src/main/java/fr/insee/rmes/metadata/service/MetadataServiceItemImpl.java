package fr.insee.rmes.metadata.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mockito.exceptions.misusing.NullInsteadOfMockException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import fr.insee.rmes.metadata.model.ColecticaItem;
import fr.insee.rmes.metadata.model.ColecticaItemPostRef;
import fr.insee.rmes.metadata.model.ColecticaItemPostRefList;
import fr.insee.rmes.metadata.model.ColecticaItemRef;
import fr.insee.rmes.metadata.model.ColecticaItemRefList;
import fr.insee.rmes.metadata.repository.GroupRepository;
import fr.insee.rmes.metadata.repository.MetadataRepository;
import fr.insee.rmes.metadata.utils.XpathProcessor;
import fr.insee.rmes.search.model.DDIItemType;
import fr.insee.rmes.search.model.ResponseItem;
import fr.insee.rmes.webservice.rest.RMeSException;

@Service
public class MetadataServiceItemImpl implements MetadataServiceItem {

	private final static Logger logger = LogManager.getLogger(MetadataServiceItemImpl.class);

	@Autowired
	MetadataRepository metadataRepository;

	@Autowired
	GroupRepository groupRepository;

	@Autowired
	XpathProcessor xpathProcessor;

	@Override
	public ColecticaItem getItem(String id) throws Exception {
		if (StringUtils.isNotEmpty(id)) {
			return metadataRepository.findById(id);
		} else {
			return null;
		}
	}

	/**
	 * Check the type of an item and return it if this item is a sequence.
	 */
	@Override
	public ColecticaItem getSequence(String id) throws Exception {
		return getItemByType(id, DDIItemType.SEQUENCE);
	}

	/**
	 * Check the type of an item and return it if this item is a question.
	 */
	@Override
	public ColecticaItem getQuestion(String id) throws Exception {
		return getItemByType(id, DDIItemType.QUESTION);
	}

	/**
	 * Check the type of an item and return it if this item is a DDI Instance.
	 */
	@Override
	public ColecticaItem getDDIInstance(String id) throws Exception {
		return getItemByType(id, DDIItemType.DDI_INSTANCE);
	}

	/**
	 * Check the type of an item and return it if this item is a Study Unit.
	 */
	@Override
	public ColecticaItem getStudyUnit(String id) throws Exception {
		return getItemByType(id, DDIItemType.STUDY_UNIT);
	}

	@Override
	public ColecticaItem getItemByType(String id, DDIItemType type) throws Exception {
		ColecticaItem item = metadataRepository.findById(id);
		DDIItemType typeItem = item.getType();
		if (item != null && typeItem != null) {
			if (typeItem.getName().equals(type.getName())) {
				return item;
			} else {
				throw new RMeSException(404, "The type of this item isn't a " + type.getName() + " but a ",
						typeItem.getName());
			}
		} else {
			throw new RMeSException(404, "The item isn't exist", id);
		}
	}

	@Override
	public ColecticaItemRefList getChildrenRef(String id) throws Exception {
		return metadataRepository.getChildrenRef(id);
	}

	@Override
	public List<ColecticaItem> getItems(ColecticaItemRefList refs) throws Exception {
		return metadataRepository.getItems(refs);
	}

	@Override
	public Map<String, ColecticaItem> getMapItems(ColecticaItemRefList refs) throws Exception {
		Map<String, ColecticaItem> items = new HashMap<String, ColecticaItem>();
		List<ColecticaItem> itemList = getItems(refs);
		for (ColecticaItem item : itemList) {
			if (item != null && item.getIdentifier() != null) {
				items.put(item.getIdentifier(), item);
			}
		}
		return items;
	}

	@Override
	public ResponseItem getDDIRoot(String id) throws Exception {
		ResponseItem ddiRoot = new ResponseItem();
		String fragment = getItem(id).item;
		String rootExp = "//*[local-name()='DDIInstance']";
		String labelExp = "//*[local-name()='Citation']/*[local-name()='Title']/*[local-name()='String']/text()";
		Node rootNode = xpathProcessor.queryList(fragment, rootExp).item(0);
		ddiRoot.setId(id);
		ddiRoot.setLabel(xpathProcessor.queryText(rootNode, labelExp));
		ddiRoot.setResourcePackageId(getResourcePackageId(rootNode));
		ddiRoot.setChildren(getGroups(rootNode, ddiRoot));
		logger.debug("ddiRoot : " + ddiRoot.toString());
		return ddiRoot;
	}

	@Override
	public List<ResponseItem> getDDICodeListSchemeFromResourcePackage(String idRP) throws Exception {
		String fragment = getItem(idRP).item;
		String rootExp = "//*[local-name()='Fragment']";
		Node rootNode = xpathProcessor.queryList(fragment, rootExp).item(0);
		List<ResponseItem> clsList = new ArrayList<>();
		String childExp = ".//*[local-name()='CodeListSchemeReference']";
		NodeList children = xpathProcessor.queryList(rootNode, childExp);
		for (int i = 0; i < children.getLength(); i++) {
			String id = xpathProcessor.queryText(children.item(i), ".//*[local-name()='ID']/text()");
			fragment = getItem(id).item;
			Node child = xpathProcessor.toDocument(fragment);
			ResponseItem cls = new ResponseItem();
			cls.setId(id);
			cls.setLabel(xpathProcessor.queryText(child,
					".//*[local-name()='CodeListScheme']/*[local-name()='Label']/*[local-name()='Title']/*[local-name()='String']/text()"));
			cls.setParent(idRP);
			cls.setResourcePackageId(idRP);
			cls.setChildren(getCodeListResponseItem(child, cls));
			clsList.add(cls);
			logger.debug("CodeListScheme : " + id);
		}
		return clsList;
	}

	@Override
	public List<ResponseItem> getDDICodeListSchemeFromGroupRoot(String idGroupRoot) throws Exception {
		List<ResponseItem> clsList = new ArrayList<>();
		logger.debug("GroupRoot id : " + idGroupRoot);
		String fragment = getItem(idGroupRoot).item;
		String rootExp = "//*[local-name()='Fragment']";
		Node rootNode = xpathProcessor.queryList(fragment, rootExp).item(0);
		String childGroupExp = ".//*[local-name()='GroupReference']/*[local-name()='ID']/text()";
		String idGroup = xpathProcessor.queryString(rootNode, childGroupExp);
		logger.debug("Group id : " + idGroup);
		fragment = getItem(idGroup).item;
		Node groupNode = xpathProcessor.queryList(fragment, rootExp).item(0);
		String childSubGroupExp = ".//*[local-name()='SubGroupReference']/*[local-name()='ID']/text()";
		String idSubGroup = xpathProcessor.queryString(groupNode, childSubGroupExp);
		logger.debug("SubGroup id : " + idSubGroup);
		String childExp = ".//*[local-name()='ResourcePackageReference']";
		NodeList children = xpathProcessor.queryList(rootNode, childExp);
		for (int i = 0; i < children.getLength(); i++) {
			String idRP = xpathProcessor.queryText(children.item(i), ".//*[local-name()='ID']/text()");
			fragment = getItem(idRP).item;
			rootExp = "//*[local-name()='Fragment']";
			rootNode = xpathProcessor.queryList(fragment, rootExp).item(0);
			childExp = ".//*[local-name()='CodeListSchemeReference']";
			children = xpathProcessor.queryList(rootNode, childExp);
			for (int j = 0; j < children.getLength(); j++) {
				String id = xpathProcessor.queryString(children.item(j), ".//*[local-name()='ID']/text()");
				fragment = getItem(id).item;
				Node child = xpathProcessor.toDocument(fragment);
				ResponseItem cls = new ResponseItem();
				cls.setId(id);
				cls.setParent(idRP);
				cls.setResourcePackageId(idRP);
				cls.setSubGroupId(idSubGroup);
				cls.setChildren(getCodeListResponseItem(child, cls));
				clsList.add(cls);
				logger.debug("CodeListScheme : " + id);
			}

		}
		return clsList;
	}

	private List<ResponseItem> getCodeListResponseItem(Node node, ResponseItem cls) throws Exception {
		List<ResponseItem> clList = new ArrayList<>();
		String childExp = ".//*[local-name()='CodeListReference']";
		NodeList children = xpathProcessor.queryList(node, childExp);
		for (int i = 0; i < children.getLength(); i++) {
			String id = xpathProcessor.queryText(children.item(i), ".//*[local-name()='ID']/text()");
			String fragment = getItem(id).item;
			Node child = xpathProcessor.toDocument(fragment);
			ResponseItem cl = new ResponseItem();
			cl.setId(id);
			cl.setLabel(xpathProcessor.queryText(child, ".//*[local-name()='Label']/*[local-name()='Content']/text()"));
			cl.setParent(cls.getId());
			cl.setResourcePackageId(cls.getResourcePackageId());
			cl.setSubGroupId(cls.getSubGroupId());
			clList.add(cl);
		}
		return clList;
	}

	public String getResourcePackageId(Node rootNode) throws Exception {
		String childExp = ".//*[local-name()='ResourcePackageReference']";
		Node rpNode = xpathProcessor.queryList(rootNode, childExp).item(0);
		return xpathProcessor.queryText(rpNode, ".//*[local-name()='ID']/text()");
	}

	public List<ResponseItem> getGroups(Node node, ResponseItem ddiRoot) throws Exception {
		List<ResponseItem> groups = new ArrayList<>();
		String childExp = ".//*[local-name()='GroupReference']";
		NodeList children = xpathProcessor.queryList(node, childExp);
		for (int i = 0; i < children.getLength(); i++) {
			String id = xpathProcessor.queryText(children.item(i), ".//*[local-name()='ID']/text()");
			String fragment = getItem(id).item;
			Node child = xpathProcessor.toDocument(fragment);
			ResponseItem group = new ResponseItem();
			group.setId(id);
			group.setLabel(xpathProcessor.queryText(child,
					".//*[local-name()='Group']/*[local-name()='Citation']/*[local-name()='Title']/*[local-name()='String']/text()"));
			group.setParent(ddiRoot.getId());
			group.setResourcePackageId(ddiRoot.getResourcePackageId());
			group.setGroupId(id);
			group.setChildren(getSubGroups(child, group));
			groups.add(group);
		}
		return groups;
	}

	private List<ResponseItem> getSubGroups(Node node, ResponseItem group) throws Exception {
		List<ResponseItem> subGroups = new ArrayList<>();
		String childExp = ".//*[local-name()='SubGroupReference']";
		NodeList children = xpathProcessor.queryList(node, childExp);
		for (int i = 0; i < children.getLength(); i++) {
			String id = xpathProcessor.queryText(children.item(i), ".//*[local-name()='ID']/text()");
			String fragment = getItem(id).item;
			Node child = xpathProcessor.toDocument(fragment);
			ResponseItem subGroup = new ResponseItem();
			subGroup.setGroupId(group.getId());
			subGroup.setId(id);
			subGroup.setSubGroupId(id);
			subGroup.setResourcePackageId(group.getResourcePackageId());
			subGroup.setParent(group.getId());
			subGroup.setLabel(xpathProcessor.queryText(child,
					".//*[local-name()='SubGroup']/*[local-name()='Citation']/*[local-name()='Title']/*[local-name()='String']/text()"));
			subGroup.setChildren(getStudyUnits(child, subGroup));
			subGroups.add(subGroup);
		}
		return subGroups;
	}

	private List<ResponseItem> getStudyUnits(Node node, ResponseItem subGroup) throws Exception {
		List<ResponseItem> studyUnits = new ArrayList<>();
		String childExp = ".//*[local-name()='StudyUnitReference']";
		NodeList children = xpathProcessor.queryList(node, childExp);
		for (int i = 0; i < children.getLength(); i++) {
			String id = xpathProcessor.queryText(children.item(i), ".//*[local-name()='ID']/text()");
			String fragment = getItem(id).item;
			Node child = xpathProcessor.toDocument(fragment);
			ResponseItem studyUnit = new ResponseItem();
			studyUnit.setGroupId(subGroup.getGroupId());
			studyUnit.setSubGroupId(subGroup.getId());
			studyUnit.setId(id);
			studyUnit.setStudyUnitId(id);
			studyUnit.setResourcePackageId(subGroup.getResourcePackageId());
			studyUnit.setParent(subGroup.getId());
			studyUnit.setLabel(xpathProcessor.queryText(child,
					".//*[local-name()='StudyUnit']/*[local-name()='Citation']/*[local-name()='Title']/*[local-name()='String']/text()"));
			studyUnit.setChildren(getDataCollections(child, studyUnit));
			studyUnits.add(studyUnit);
		}
		return studyUnits;
	}

	private List<ResponseItem> getDataCollections(Node node, ResponseItem studyUnit) throws Exception {
		List<ResponseItem> dataCollections = new ArrayList<>();
		String childExp = ".//*[local-name()='DataCollectionReference']";
		NodeList children = xpathProcessor.queryList(node, childExp);
		for (int i = 0; i < children.getLength(); i++) {
			String id = xpathProcessor.queryText(children.item(i), ".//*[local-name()='ID']/text()");
			String fragment = getItem(id).item;
			Node child = xpathProcessor.toDocument(fragment);
			ResponseItem dataCollection = new ResponseItem();
			dataCollection.setId(id);
			dataCollection.setDataCollectionId(id);
			dataCollection.setGroupId(studyUnit.getGroupId());
			dataCollection.setSubGroupId(studyUnit.getSubGroupId());
			dataCollection.setStudyUnitId(studyUnit.getId());
			dataCollection.setResourcePackageId(studyUnit.getResourcePackageId());
			dataCollection.setParent(studyUnit.getId());
			dataCollection.setLabel(xpathProcessor.queryText(child,
					".//*[local-name()='DataCollection']/*[local-name()='Label']/*[local-name()='Content']/text()"));
			dataCollections.add(dataCollection);
			dataCollection.setChildren(getInstrumentSchemes(child, dataCollection));
		}
		return dataCollections;
	}

	private List<ResponseItem> getInstrumentSchemes(Node node, ResponseItem dataCollection) throws Exception {
		List<ResponseItem> instrumentSchemes = new ArrayList<>();
		String childExp = ".//*[local-name()='InstrumentSchemeReference']";
		NodeList children = xpathProcessor.queryList(node, childExp);
		for (int i = 0; i < children.getLength(); i++) {
			String id = xpathProcessor.queryText(children.item(i), ".//*[local-name()='ID']/text()");
			String fragment = getItem(id).item;
			Node child = xpathProcessor.toDocument(fragment);
			ResponseItem instrumentScheme = new ResponseItem();
			instrumentScheme.setId(id);
			instrumentScheme.setParent(dataCollection.getId());
			instrumentScheme.setGroupId(dataCollection.getGroupId());
			instrumentScheme.setSubGroupId(dataCollection.getSubGroupId());
			instrumentScheme.setStudyUnitId(dataCollection.getStudyUnitId());
			instrumentScheme.setDataCollectionId(dataCollection.getId());
			instrumentScheme.setResourcePackageId(dataCollection.getResourcePackageId());
			instrumentSchemes.add(instrumentScheme);
			instrumentScheme.setChildren(getInstruments(child, instrumentScheme));
		}
		return instrumentSchemes;
	}

	private List<ResponseItem> getInstruments(Node node, ResponseItem instrumentScheme) throws Exception {
		List<ResponseItem> instruments = new ArrayList<>();
		String childExp = ".//*[local-name()='InstrumentReference']";
		NodeList children = xpathProcessor.queryList(node, childExp);
		for (int i = 0; i < children.getLength(); i++) {
			String id = xpathProcessor.queryText(children.item(i), ".//*[local-name()='ID']/text()");
			String fragment = getItem(id).item;
			Node child = xpathProcessor.toDocument(fragment);
			ResponseItem instrument = new ResponseItem();
			instrument.setId(id);
			instrument.setParent(instrumentScheme.getId());
			instrument.setDataCollectionId(instrumentScheme.getDataCollectionId());
			instrument.setStudyUnitId(instrumentScheme.getStudyUnitId());
			instrument.setSubGroupId(instrumentScheme.getSubGroupId());
			instrument.setGroupId(instrumentScheme.getGroupId());
			instrument.setResourcePackageId(instrumentScheme.getResourcePackageId());
			instrument.setLabel(xpathProcessor.queryText(child,
					".//*[local-name()='Instrument']/*[local-name()='Label']/*[local-name()='Content']/text()"));
			instrument.setName(
					xpathProcessor.queryText(child, ".//*[local-name()='Instrument']/*[local-name()='UserID']/text()"));
			instruments.add(instrument);
		}
		return instruments;
	}

	@Override
	public String postItems(ColecticaItemPostRefList refs) throws Exception {
		for (ColecticaItemPostRef ref : refs.getItems()) {
			checks(ref);
		}

		return metadataRepository.postItems(refs);
	}

	@Override
	public String postItem(ColecticaItemPostRef ref) throws Exception {
		checks(ref);
		return metadataRepository.postItem(ref);

	}

	/**
	 * Method which groups all the checks before posting an item.
	 * 
	 * @param ref : target ColecticaItemPostRef object
	 */
	public void checks(ColecticaItemPostRef ref) {
		checkVersionItem(ref);
		checkUUIDItem(ref);
	}

	/**
	 * Check the version of a specific item (the version is incremented)
	 * 
	 * @param ref
	 *            : reference of the DDI Item.
	 */
	public void checkVersionItem(ColecticaItemPostRef ref) {
		if (ref != null) {
			try {
				ColecticaItem item = this.getItem(ref.identifier);
				int version = Integer.valueOf(item.version) + 1;
				ref.version = String.valueOf(version);
			} catch (Exception e) {
				ref.version = "1";
			}
		} else {
			throw new NullPointerException();
		}
	}

	/**
	 * Check if the reference of the DDI item has an identifier. If no identifier is
	 * found, a new UUID will be assigned.
	 * 
	 * @param ref
	 *            : reference of the DDI Item.
	 */
	public void checkUUIDItem(ColecticaItemPostRef ref) {
		if (ref != null) {
			if (ref.identifier == null || ref.identifier.equals("")) {
				ref.identifier = UUID.randomUUID().toString();
			}
		} else {
			throw new NullPointerException();
		}
	}

}
