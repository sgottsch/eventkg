import de.l3s.eventkg.integration.DataSets;
import de.l3s.eventkg.integration.DataStoreWriter;
import de.l3s.eventkg.integration.model.relation.DataSet;
import de.l3s.eventkg.meta.Language;
import de.l3s.eventkg.meta.Source;
import de.l3s.eventkg.pipeline.Config;

public class Test {
	//
	// public static void main(String[] args) {
	// // TODO Auto-generated method stub
	//
	// String object = "\\m/";
	// object = object.replace("\\", "\\\\");
	// System.out.println(object);
	// }

	public static void main(String[] args) {
		
		String property="<playsFor>";
		if (property.startsWith("<"))
			property = property.substring(1, property.length() - 1);
System.out.println(property);

		String rel ="owl#test";
	System.out.println(rel.substring(rel.indexOf("#")+1));

		
		String id = "<graphs/bla>";
		System.out.println(id.replace("graphs/", ""));

		//
		// Config.init("config_eventkb_local.txt");
		// DataStoreWriter dsw=new DataStoreWriter();
		//
		// DataSets.getInstance().addDataSet(Language.DE, Source.DBPEDIA,
		// "http://de.dbpedia.org/");
		// DataSets.getInstance().addDataSet(Language.FR, Source.DBPEDIA,
		// "http://fr.dbpedia.org/");
		// DataSets.getInstance().addDataSet(Language.RU, Source.DBPEDIA,
		// "http://ru.dbpedia.org/");
		// DataSets.getInstance().addDataSet(Language.PT, Source.DBPEDIA,
		// "http://pt.dbpedia.org/");
		// DataSets.getInstance().addDataSet(Language.EN, Source.DBPEDIA,
		// "http://dbpedia.org/");
		// DataSets.getInstance().addDataSet(Language.DE, Source.WIKIPEDIA,
		// "https://dumps.wikimedia.org/dewiki/");
		// DataSets.getInstance().addDataSet(Language.FR, Source.WIKIPEDIA,
		// "https://dumps.wikimedia.org/frwiki/");
		// DataSets.getInstance().addDataSet(Language.RU, Source.WIKIPEDIA,
		// "https://dumps.wikimedia.org/ruwiki/");
		// DataSets.getInstance().addDataSet(Language.PT, Source.WIKIPEDIA,
		// "https://dumps.wikimedia.org/ptwiki/");
		// DataSets.getInstance().addDataSet(Language.EN, Source.WIKIPEDIA,
		// "https://dumps.wikimedia.org/enwiki/");
		// DataSets.getInstance().addDataSetWithoutLanguage(Source.WIKIDATA,
		// "https://dumps.wikimedia.org/wikidatawiki/entities/");
		// DataSets.getInstance().addDataSetWithoutLanguage(Source.YAGO,
		// "https://www.mpi-inf.mpg.de/de/departments/databases-and-information-systems/research/yago-naga/yago/downloads/");
		// DataSets.getInstance().addDataSet(Language.EN, Source.WCE,
		// "http://wikitimes.l3s.de/Resource.jsp");
		//
		// dsw.write();
	}

}
