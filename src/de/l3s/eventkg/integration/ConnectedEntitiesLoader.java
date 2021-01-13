package de.l3s.eventkg.integration;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.LineIterator;

import de.l3s.eventkg.integration.model.Entity;
import de.l3s.eventkg.util.FileLoader;
import de.l3s.eventkg.util.FileName;

public class ConnectedEntitiesLoader {

	public static Map<Entity, Set<Entity>> loadConnectedEntities(WikidataIdMappings wikidataIdMappings) {

		System.out.println("Load connected entities.");

		Map<Entity, Set<Entity>> connectedEntities = new HashMap<Entity, Set<Entity>>();

		LineIterator it = null;
		try {
			it = FileLoader.getLineIterator(FileName.CONNECTED_ENTITIES);
			while (it.hasNext()) {
				String line = it.nextLine();
				String[] parts = line.split(",");

				Entity entity1 = wikidataIdMappings.getEntityByWikidataId(parts[0]);
				Entity entity2 = wikidataIdMappings.getEntityByWikidataId(parts[1]);

				if (!connectedEntities.containsKey(entity1))
					connectedEntities.put(entity1, new HashSet<Entity>());

				if (!connectedEntities.containsKey(entity2))
					connectedEntities.put(entity2, new HashSet<Entity>());

				connectedEntities.get(entity1).add(entity2);
				connectedEntities.get(entity2).add(entity1);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				it.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		System.out.println("# connected entities: " + connectedEntities.keySet().size());

		return connectedEntities;
	}

}
