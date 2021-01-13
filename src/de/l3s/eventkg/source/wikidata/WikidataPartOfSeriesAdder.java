package de.l3s.eventkg.source.wikidata;

public class WikidataPartOfSeriesAdder {

//	private Map<String, Set<String>> subEvents = new HashMap<String, Set<String>>();
//
//	public static void main(String[] args) {
//
//		Config.init(args[0]);
//
//		WikidataPartOfSeriesAdder wpsa = new WikidataPartOfSeriesAdder();
//
//		wpsa.loadExistingRelations();
//		wpsa.loadMissingRelations();
//	}
//
//	public void loadExistingRelations() {
//
//		System.out.println("loadExistingRelations");
//
//		System.out.println(FileLoader.getPath(FileName.ALL_TTL_EVENTS_BASE_RELATIONS));
//
//		int lineNo = -1;
//		int existLineNo = 0;
//		LineIterator it3 = null;
//		try {
//			it3 = FileLoader.getLineIterator(FileName.ALL_TTL_EVENTS_BASE_RELATIONS);
//			while (it3.hasNext()) {
//				String line = it3.nextLine();
//
//				lineNo += 1;
//
//				if (lineNo % 10000 == 0)
//					System.out.println(lineNo + " -> " + subEvents.keySet().size());
//
//				String[] parts = line.split(" ");
//
//				if (parts[1].equals("<http://semanticweb.cs.vu.nl/2009/11/sem/hasSubEvent>")) {
//
//					String parentId = parts[0];
//					String childId = parts[2];
//
//					parentId = parentId.substring(parentId.lastIndexOf("/") + 1, parentId.length() - 1);
//					childId = childId.substring(childId.lastIndexOf("/") + 1, childId.length() - 1);
//
//					if (!subEvents.containsKey(parentId))
//						subEvents.put(parentId, new HashSet<String>());
//
//					subEvents.get(parentId).add(childId);
//
//					if (existLineNo < 50) {
//						System.out.println("exists: " + parentId + " -> " + childId);
//					}
//					existLineNo += 1;
//				}
//
//			}
//		} catch (IOException e) {
//			e.printStackTrace();
//		} finally {
//			try {
//				it3.close();
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
//		}
//
//	}
//
//	public void loadMissingRelations() {
//
//		System.out.println("loadMissingRelations");
//
//		PrefixList.getInstance().init(null);
//
//		EntityIdGenerator generator = new EntityIdGenerator(false);
//		DataStoreWriter dataStoreWriter = new DataStoreWriter(null, null,
//				DataStoreWriterMode.USE_IDS_OF_CURRENT_EVENTKG_VERSION);
//		dataStoreWriter.init();
//		dataStoreWriter.initPrefixes();
//
//		DataSet dataSet = DataSets.getInstance().getDataSetWithoutLanguage(Source.WIKIDATA);
//		int lineNo = -1;
//		int writtenLineNo = 0;
//
//		PrintWriter writer = null;
//		PrintWriter writerPreview = null;
//		try {
//			writer = FileLoader.getWriter(FileName.ALL_TTL_ADDITIONAL_SUB_EVENTS);
//			writerPreview = FileLoader.getWriter(FileName.ALL_TTL_ADDITIONAL_SUB_EVENTS_PREVIEW);
//
//			LineIterator it3 = null;
//			try {
//				it3 = FileLoader.getLineIterator(FileName.WIKIDATA_PART_OF_SERIES);
//				while (it3.hasNext()) {
//					String line = it3.nextLine();
//
//					lineNo += 1;
//
//					if (lineNo % 10000 == 0)
//						System.out.println(lineNo + " -> " + writtenLineNo);
//
//					String[] parts = line.split(Config.TAB);
//
//					String childWikidataId = parts[0];
//					String parentWikidataId = parts[2];
//
//					if (lineNo < 50) {
//						System.out.println(parentWikidataId + " -> " + childWikidataId);
//					}
//
//					String childEventKGId = generator.getEventKGEventIDByWikidataId(childWikidataId);
//					if (childEventKGId == null)
//						continue;
//
//					String parentEventKGId = generator.getEventKGEventIDByWikidataId(parentWikidataId);
//					if (parentEventKGId == null)
//						continue;
//
//					if (subEvents.containsKey(parentEventKGId)
//							&& subEvents.get(parentEventKGId).contains(childEventKGId))
//						continue;
//
//					if (lineNo < 50) {
//						System.out.println(" " + parentEventKGId + " -> " + childEventKGId);
//					}
//
//					dataStoreWriter.writeSubEvent(parentEventKGId, childEventKGId, writtenLineNo, dataSet, writer,
//							writerPreview);
//					writtenLineNo += 1;
//				}
//			} catch (IOException e) {
//				e.printStackTrace();
//			} finally {
//				try {
//					it3.close();
//				} catch (IOException e) {
//					e.printStackTrace();
//				}
//			}
//
//		} catch (FileNotFoundException e) {
//			e.printStackTrace();
//		} finally {
//			writer.close();
//			writerPreview.close();
//		}
//
//	}

}
