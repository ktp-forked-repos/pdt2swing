package prlg.demo;

import java.io.File;

import pdt.gui.PrologGui;
import pdt.gui.data.PrologAdapter;
import pdt.gui.data.PrologGuiBundle;
import pdt.gui.data.PrologTableData;
import pdt.gui.datapanels.handler.PrologFactHandler;
import pdt.gui.datapanels.handler.PrologRelationHandler;
import pdt.gui.datapanels.handler.PrologTextFileHandler;
import pdt.prolog.elements.PrologArgument;
import pdt.prolog.elements.PrologGoal;

public class PrologDemoGui {

	public  PrologDemoGui() {
		File dataDir = new File("src/data");
		File loadFile = new File(dataDir, "load.pl");
	
		// Open Prolog connection with load file
		PrologAdapter con = new PrologAdapter(loadFile);

		PrologGuiBundle bundle1 = createSerienBundle(dataDir, con);
		PrologGuiBundle bundle2 = createKategorieBundle(dataDir, con);
		
		new PrologGui(con, new DemoBundleProvider(con, bundle1, bundle2));

	}

private PrologGuiBundle createSerienBundle(File dataDir, PrologAdapter con) {
		
		File seriesFile = new File(dataDir, "fsdb_serie.pl");
		File ratingFile = new File(dataDir, "fsdb_serie_rating.pl");
		File tagFile = new File(dataDir, "fsdb_tags.pl");
		
		PrologGoal tableGoal = new PrologGoal("fsdb_serie_display",
				PrologArgument.createId(),
				PrologArgument.createAtom("Name"),
				PrologArgument.createAtom("Herkunftsland"),
				PrologArgument.createNumber("Staffeln"),
				PrologArgument.createNumber("Episoden"),
				PrologArgument.createNumber("Rating")
		);
		
		PrologGoal seriesGoal = new PrologGoal("fsdb_serie",
				PrologArgument.createId(),
				PrologArgument.createAtom("Name"),
				PrologArgument.createFixedAtom("Herkunftsland", "USA", "Deutschland", "Großbritannien"),
				PrologArgument.createNumber("Staffeln"),
				PrologArgument.createNumber("Episoden"),
				PrologArgument.createBoolean("Epic")
		);

		PrologGoal ratingGoal = new PrologGoal("fsdb_serie_rating", PrologArgument.createId(), PrologArgument.createLimitedNumber("Rating", 0, 10, true));
		PrologGoal tagGoal = new PrologGoal("fsdb_tags", PrologArgument.createId(), PrologArgument.createAtom("Tag"));
		
		PrologTableData tableData = new PrologTableData(con, tableGoal);
		final PrologFactHandler seriesData = new PrologFactHandler(con, "Data", seriesFile, true, seriesGoal);
		final PrologTextFileHandler textData = seriesData.createTextFileHandler("Zusammenfassung", null, false);
		final PrologFactHandler ratingData = new PrologFactHandler(con, "Rating", ratingFile, false, ratingGoal);
		final PrologRelationHandler tagData = new PrologRelationHandler(con, "Tags", tagFile, tagGoal, true);
		
		PrologGuiBundle bundle = new PrologGuiBundle(tableData, seriesData, textData, ratingData, tagData);
		return bundle;
	}
	
private PrologGuiBundle createKategorieBundle(File dataDir, PrologAdapter con) {
	
	File catFile = new File(dataDir, "fsdb_category.pl");
	
	PrologGoal tableGoal = new PrologGoal("fsdb_category",
			PrologArgument.createId(),
			PrologArgument.createAtom("Name")
	);
	
	PrologTableData tableData = new PrologTableData(con, tableGoal);
	final PrologFactHandler categoryData = new PrologFactHandler(con, "Data", catFile, true, tableGoal);
	
	PrologGuiBundle bundle = new PrologGuiBundle(tableData, categoryData);
	return bundle;
}


	public static void main(String[] args) {
        // Schedule a job for the event-dispatching thread:
        // creating and showing this application's GUI.
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
            	new PrologDemoGui();
            }
        });
    }
	
}
