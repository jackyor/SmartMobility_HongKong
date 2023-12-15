import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.geom.*;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This class generates a MATSim population from the sample files in
 * examples/tutorial/population/demandGenerationFromShapefile
 * It parses two commuter statistics and places agent's work and home locations in the corresponding regions.
 * The locations within a region are chosen randomly within the shape of a region extracted from the
 * 'thrueringen-kreise.shp' shape file. To ensure agents living in populated areas it is also ensured that home and work
 * locations lie within a shape extracted from the 'landcover.shp' shapefile. The input data is taken from:
 * <p>
 * commuters-inner-regional.csv: https://statistik.arbeitsagentur.de/nn_31966/SiteGlobals/Forms/Rubrikensuche/Rubrikensuche_Form.html?view=processForm&resourceId=210368&input_=&pageLocale=de&topicId=746732&year_month=201806&year_month.GROUP=1&search=Suchen
 * commuters-inter-regional.csv: https://statistik.arbeitsagentur.de/nn_31966/SiteGlobals/Forms/Rubrikensuche/Rubrikensuche_Form.html?view=processForm&resourceId=210368&input_=&pageLocale=de&topicId=882788&year_month=201806&year_month.GROUP=1&search=Suchen
 * thueringen-kreise.shp: http://www.geodatenzentrum.de/geodaten/gdz_rahmen.gdz_div?gdz_spr=deu&gdz_akt_zeile=5&gdz_anz_zeile=1&gdz_unt_zeile=13&gdz_user_id=0
 * landcover.shp: http://www.geodatenzentrum.de/geodaten/gdz_rahmen.gdz_div?gdz_spr=deu&gdz_akt_zeile=5&gdz_anz_zeile=1&gdz_unt_zeile=22&gdz_user_id=0
 * <p>
 * The leg mode of all legs is 'car', all agents leave home at 9am and finish work at 5pm.
 */
class CreateDemand_final_drt {

	private static final Logger logger = LogManager.getLogger("CreateDemand");
	private static final int HOME_END_TIME = 8 * 60 * 60;
	private static final int WORK_END_TIME = 17 * 60 * 60 + 1800;
	private static final int WORK_START_TIME = 8 * 60 * 60;
	private static final double SCALE_FACTOR = 1;
	private static final GeometryFactory geometryFactory = new GeometryFactory();
	private static final CSVFormat csvFormat = CSVFormat.Builder.create()
			.setDelimiter(',')
			.setHeader()
			.setAllowMissingColumnNames(true)
			.build();

	private final Path innerRegionCommuterStatistic;
	private final Path interRegionCommuterStatistic;

	private final Random random = new Random();

	private Population population;
	public static Object getDc (SimpleFeature SimpleFeature, String input){
		return SimpleFeature.getAttribute(input);
	}
	public static Object getGeo (SimpleFeature SimpleFeature){return SimpleFeature.getDefaultGeometry();};
	private final Map<String, List<Geometry>> dcBuilding;
	private final Map<String, List<Geometry>> dccaBuilding;
	private final Map<String,String> dccaToDcnt;
	private final Map<String,String> carPerecentByDcNt;
	public static Map<String, String> getMapFromCSV(final String filePath) throws IOException{

		Stream<String> lines = Files.lines(Paths.get(filePath));
		Map<String, String> resultMap =
				lines.map(line -> line.split(","))
						.collect(Collectors.toMap(line -> line[0], line -> line[1]));

		lines.close();

		return resultMap;
	}

	public static Map<String, String> getMapFromCSV2(final String filePath) throws IOException{

		Stream<String> lines = Files.lines(Paths.get(filePath));
		Map<String, String> resultMap =
				lines.map(line -> line.split(","))
						.collect(Collectors.toMap(line -> line[0], line -> line[1]));

		lines.close();

		return resultMap;
	}

	AtomicInteger counter = new AtomicInteger();

	CreateDemand_final_drt() throws IOException {

		Path sampleFolder = Paths.get("input/forplan");

		this.dccaBuilding = ShapeFileReader.getAllFeatures(sampleFolder.resolve("dcNT.shp").toString()).stream()
				.collect(Collectors.groupingBy(feature -> (String) getDc(feature, "CACODE"),
						Collectors.mapping(feature -> (Geometry) getGeo(feature),Collectors.toList())));
		this.dcBuilding = ShapeFileReader.getAllFeatures(sampleFolder.resolve("dcNT.shp").toString()).stream()
				.collect(Collectors.groupingBy(feature -> (String) getDc(feature, "NTorDC"),
						Collectors.mapping(feature -> (Geometry) getGeo(feature),Collectors.toList())));

		this.innerRegionCommuterStatistic = sampleFolder.resolve("inner.csv");
		this.interRegionCommuterStatistic = sampleFolder.resolve("inter.csv");

		//for dcca map to dcnt
		this.dccaToDcnt = getMapFromCSV(String.valueOf(sampleFolder.resolve("dccaToDcnt.csv")));
		logger.info(dccaToDcnt);

		//map carPerecent to dcnt

		this.carPerecentByDcNt = getMapFromCSV2(String.valueOf(sampleFolder.resolve("carPerecent.csv")));
		logger.info(carPerecentByDcNt);


		this.population = PopulationUtils.createPopulation(ConfigUtils.createConfig());
	}

	Population getPopulation() {
		return this.population;
	}

	void create() {
		population = PopulationUtils.createPopulation(ConfigUtils.createConfig());
		createInnerRegionCommuters();
		createInterRegionCommuters();

		logger.info("Done.");
	}

	private void createInnerRegionCommuters() {
		logger.info("Creating InnerRegion commuters.");
		try (CSVParser parser = CSVParser.parse(innerRegionCommuterStatistic, StandardCharsets.UTF_8 , csvFormat)) {

			for (CSVRecord record : parser) {

				String homeDistrict = record.get("NTorDC");
				String homeDcca = record.get("dcca_class");

				int numberOfWork = tryParseValue(record.get("plw_same"));
				createPersons(homeDcca, homeDistrict, numberOfWork, "work");

			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void createInterRegionCommuters() {
		logger.info("Creating InterRegion commuters.");
		try (CSVParser parser = CSVParser.parse(interRegionCommuterStatistic, StandardCharsets.UTF_8, csvFormat)) {
			//take header, loop, string==3
			List<String > key = Arrays.asList("A_W", "B_W", "C_W", "D_W", "E_W",
					"F_W", "G_W", "J_W", "H_W", "M_W",
					"P_W", "Q_W", "T_W", "0_W", "1_W",
					"2_W", "3_W", "4_W", "5_W", "6_W",
					"7_W", "8_W", "9_W", "10_W", "12_W"
			);
			for (CSVRecord record : parser) {
				String homeDcca = record.get("dcca_class");
				for (int i = 0; i < key.size(); i++) {
					String toDistrict = key.get(i).substring(0,1);
					String forKey = key.get(i);
					if (Character.isDigit(forKey.charAt(1))){
						toDistrict = key.get(i).substring(0,2);
					}

					int numberOfWork = tryParseValue(record.get(forKey));
					createPersons(homeDcca, toDistrict, numberOfWork, "work");


				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void createPersons(String homeDcca, String homeDistrict, int numberOfPersons, String activity) throws IOException {

		logger.info("activity: "+activity + " Home region: " + homeDcca + " des region: " + homeDistrict + " number of commuters: " + numberOfPersons);
		//insert carPerecent
		String carPerecent = "0.01875904";
		String isCar="";

		Coord home = getCoordInGeometry(homeDcca);
		Coord work = getCoordInGeometry(homeDistrict);
		String mode = "";
		Random random = new Random();

		// create as many persons as there are commuters multiplied by the scale factor
		for (int i = 0; i < numberOfPersons * SCALE_FACTOR; i++) {
			String id = String.valueOf(counter.getAndIncrement());

			if(activity=="work" & random.nextFloat() < Float.parseFloat(carPerecent)){
					isCar="true";
					mode = "drt";
				Person person = createPerson(home, work, mode, id, activity, isCar);
				population.addPerson(person);

			}
		}
	}

	private Person createPerson(Coord home, Coord work, String mode, String id, String activity, String isCar) {

		// create a person by using the population's factory
		// The only required argument is an id
		Person person = population.getFactory().createPerson(Id.createPersonId(id));
		Plan plan = createPlan(home, work, mode, activity);
		person.addPlan(plan);

		return person;
	}

	private Plan createPlan(Coord home, Coord work, String mode, String activity) {

		// create a plan for home and work. Note, that activity -> leg -> activity -> leg -> activity have to be inserted in the right
		// order.
		Plan plan = population.getFactory().createPlan();
		//random add 2* 60*60 time
		Random rand = new Random();
		int homeEndR = rand.nextInt(2*60*60+1);

		Activity homeActivityInTheMorning = population.getFactory().createActivityFromCoord("home", home);
		homeActivityInTheMorning.setEndTime(HOME_END_TIME+homeEndR);
		plan.addActivity(homeActivityInTheMorning);

		Leg toWork = population.getFactory().createLeg(mode);
		plan.addLeg(toWork);

		int workEndR = rand.nextInt(2*60*60+1);
		//int workStartR = rand.nextInt(2*60*60+1);
		Activity workActivity = population.getFactory().createActivityFromCoord(activity, work);
		workActivity.setEndTime(WORK_END_TIME + workEndR);
		workActivity.setStartTime(HOME_END_TIME+homeEndR);
		plan.addActivity(workActivity);

		Leg toHome = population.getFactory().createLeg(mode);
		plan.addLeg(toHome);

		Activity homeActivityInTheEvening = population.getFactory().createActivityFromCoord("home", home);
		plan.addActivity(homeActivityInTheEvening);

		return plan;
	}

	private Coord getCoordInGeometry(String buildings) {

		double x, y;
		Point point;
		Map<String, List<Geometry>> currentBuildings = null;
		if (buildings.length()==3){
			if (Character.isDigit(buildings.charAt(2))){
			currentBuildings = dccaBuilding;
		}}else {
			currentBuildings = dcBuilding;
		}
		List<Geometry> selectedBuildings = currentBuildings.get(buildings);
		Random rand = new Random();
		Geometry selectedLandcover = selectedBuildings.get(rand.nextInt(selectedBuildings.size()));


		// if the landcover feature is in the correct region generate a random coordinate within the bounding box of the
		// landcover feature. Repeat until a coordinate is found which is actually within the landcover feature.
		do {
			Envelope envelope = selectedLandcover.getEnvelopeInternal();

			x = envelope.getMinX() + envelope.getWidth() * random.nextDouble();
			y = envelope.getMinY() + envelope.getHeight() * random.nextDouble();
			point = geometryFactory.createPoint(new Coordinate(x, y));
		} while (point == null || !selectedLandcover.contains(point));

		return new Coord(x, y);
	}

	private int tryParseValue(String value) {

		// first remove things excel may have put into the value
		value = value.replace(",", "");

		try {
			return Integer.parseInt(value);
		} catch (NumberFormatException e) {
			return 0;
		}
	}
}