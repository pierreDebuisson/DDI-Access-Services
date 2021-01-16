package fr.insee.rmes.metadata.repository;

import java.util.List;
import java.util.Map;

import fr.insee.rmes.metadata.model.ColecticaItem;
import fr.insee.rmes.metadata.model.ColecticaItemPostRef;
import fr.insee.rmes.metadata.model.ColecticaItemPostRefList;
import fr.insee.rmes.metadata.model.ColecticaItemRefList;
import fr.insee.rmes.metadata.model.Relationship;
import fr.insee.rmes.metadata.model.ObjectColecticaPost;
import fr.insee.rmes.metadata.model.Unit;
import fr.insee.rmes.search.model.DDIItemType;

public interface MetadataRepository {

	ColecticaItem findById(String id) throws Exception;

	ColecticaItemRefList getChildrenRef(String id) throws Exception;

	List<ColecticaItem> getItems(ColecticaItemRefList refs) throws Exception;

	String postItems(ColecticaItemPostRefList refs) throws Exception;

	String postItem(ColecticaItemPostRef ref) throws Exception;

	Integer getLastestVersionItem(String id) throws Exception;

	Relationship[] getRelationship(ObjectColecticaPost relationshipPost) throws Exception;

	List<Unit> getUnits() throws Exception;

	Relationship[] getRelationshipChildren(ObjectColecticaPost relationshipPost);
}
