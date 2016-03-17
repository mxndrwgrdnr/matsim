package playground.dziemke.analysis;

import java.io.File;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.utils.objectattributes.ObjectAttributes;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

import playground.dziemke.utils.ShapeReader;

/**
 * @author dziemke
 */
public class TripAnalyzerExtended {
	public static final Logger log = Logger.getLogger(TripAnalyzerExtended.class) ;

	
	/* Parameters */
	private static final String runId = "run_168a";
	private static final String usedIteration = "300"; // most frequently used value: 150
	private static final String cemdapPersonsInputFileId = "21"; // check if this number corresponds correctly to the runId
	
	private static final Integer planningAreaId = 11000000; // 11000000 = Berlin

	private static final boolean onlyCar = false; // "car"; new, should be used for runs with ChangeLegMode enabled
	private static final boolean onlyInterior = false; // "int"
	private static final boolean onlyBerlinBased = true; // "ber"; usually varied for analysis
	private static final boolean distanceFilter = true; // "dist"; usually varied for analysis
	// private static final double double minDistance = 0;
	private static final double maxDistance_km = 100; // most frequently used value: 150

	private static final boolean onlyWorkTrips = false; // "work"

	private static final boolean ageFilter = false; // "age"
	private static final Integer minAge = 80; // typically "x0"
	private static final Integer maxAge = 119; // typically "x9"; higehst number ususally chosen is 119

	private static final int maxBinDuration_min = 120;
	private static final int binWidthDuration_min = 1;

	private static final int maxBinTime_h = 23;
	private static final int binWidthTime_h = 1;

	private static final int maxBinDistance_km = 60;
	private static final int binWidthDistance_km = 1;

	private static final int maxBinSpeed_km_h = 60;
	private static final int binWidthSpeed_km_h = 1;


	/* Input and output */
	private static final String networkFile = "../../../shared-svn/studies/countries/de/berlin/counts/iv_counts/network.xml";
	private static final String eventsFile = "../../../runs-svn/cemdapMatsimCadyts/" + runId + "/ITERS/it." + usedIteration + 
			"/" + runId + "." + usedIteration + ".events.xml.gz";
	private static final String cemdapPersonsInputFile = "../../../shared-svn/projects/cemdapMatsimCadyts/scenario/cemdap_berlin/" + 
			cemdapPersonsInputFileId + "/persons1.dat";
	private static final String planningAreaShapeFile = "../../../shared-svn/projects/cemdapMatsimCadyts/scenario/shapefiles/Berlin_DHDN_GK4.shp";
	private static String outputDirectory = "../../../runs-svn/cemdapMatsimCadyts/" + runId + "/analysis";

	
	/* Variables to store objects */
	private static Network network;
	private static Geometry planningAreaGeometry;
	private static ObjectAttributes cemdapPersonAttributes;
	
	
	/* Variables to store information */
	private static int tripCounter = 0;
	private static int tripCounterSpeed = 0;
	private static int tripCounterIncomplete = 0;
	
	private static Map <Integer, Double> tripDurationMap = new TreeMap <Integer, Double>();
	private static double aggregateTripDuration = 0.;
    
	private static Map <Integer, Double> departureTimeMap = new TreeMap <Integer, Double>();
    
	private static Map <String, Double> activityTypeMap = new TreeMap <String, Double>();
    
	private static Map <Integer, Double> tripDistanceRoutedMap = new TreeMap <Integer, Double>();
	private static double aggregateTripDistanceRouted = 0.;
	
	private static Map <Integer, Double> tripDistanceBeelineMap = new TreeMap <Integer, Double>();
	private static double aggregateTripDistanceBeeline = 0.;
    
	private static Map <Integer, Double> averageTripSpeedRoutedMap = new TreeMap <Integer, Double>();
	private static double aggregateOfAverageTripSpeedsRouted = 0.;
    
	private static Map <Integer, Double> averageTripSpeedBeelineMap = new TreeMap <Integer, Double>();
	private static double aggregateOfAverageTripSpeedsBeeline = 0.;
    
	private static int numberOfTripsWithNoCalculableSpeed = 0;
    
	private static Map <Id<Trip>, Double> distanceRoutedMap = new TreeMap <Id<Trip>, Double>();
	private static Map <Id<Trip>, Double> distanceBeelineMap = new TreeMap <Id<Trip>, Double>();
    
	private static Map <String, Integer> otherInformationMap = new TreeMap <String, Integer>();
	
	
	private static double averageTripDuration;
	private static double averageTripDistanceRouted;
	private static double averageTripDistanceBeeline;
	private static double averageOfAverageTripSpeedsRouted;
	private static double averageOfAverageTripSpeedsBeeline;

	
	public static void main(String[] args) {
		adaptOutputDirectory();
	    
		// Create an EventsManager instance (MATSim infrastructure)
	    EventsManager eventsManager = EventsUtils.createEventsManager();
	    TripHandler handler = new TripHandler();
	    eventsManager.addHandler(handler);
	 
	    // Connect a file reader to the EventsManager and read in the event file
	    MatsimEventsReader reader = new MatsimEventsReader(eventsManager);
	    reader.readFile(eventsFile);
	    log.info("Events file read!");
	    
	       
	    /* Get network, which is needed to calculate distances */
	    network = NetworkUtils.createNetwork();
	    MatsimNetworkReader networkReader = new MatsimNetworkReader(network);
	    networkReader.readFile(networkFile);
	    
	    Map<Integer, Geometry> zoneGeometries = ShapeReader.read(planningAreaShapeFile, "NR");
		planningAreaGeometry = zoneGeometries.get(planningAreaId);	    
	    
	    if (ageFilter == true) {
	    	// TODO needs to be adapted for other analyses that are based on person-specific attributes as well
	    	CemdapPersonInputFileReader cemdapPersonInputFileReader = new CemdapPersonInputFileReader();
		 	cemdapPersonInputFileReader.parse(cemdapPersonsInputFile);
		 	cemdapPersonAttributes = cemdapPersonInputFileReader.getPersonAttributes();
	    }

	    
	    /* Do calculations */
	    for (Trip trip : handler.getTrips().values()) {
	    	if(!trip.getTripComplete()) {
	    		System.err.println("Trip is not complete!");
	    		tripCounterIncomplete++;
	    		/* The only case where incomplete trips occur is when agents are removed according to "removeStuckVehicles = true"
	    		 * Since a removed agent can at most have one incomplete trip (the one when he is removed), the number of
	    		 * incomplete trips should be equal to the number of removed agents
	    		 */
	    		continue;
	    	}	    	

	    	if (decideIfConsiderTrip(trip) == true) {
	    		analyzeTrip(trip);
	    	}
	    }
	    calculateAverages();
	    
	    otherInformationMap.put("Number of trips that have no previous activity", handler.getNoPreviousEndOfActivityCounter());
	    otherInformationMap.put("Number of trips that have no calculable speed", numberOfTripsWithNoCalculableSpeed);
	    otherInformationMap.put("Number of incomplete trips (i.e. number of removed agents)", tripCounterIncomplete);
	    otherInformationMap.put("Number of (complete) trips", tripCounter);
	    
	    writeResultFiles();
	    
	    log.info(tripCounterIncomplete + " trips are incomplete.");
	}


	private static void writeResultFiles() {
	    new File(outputDirectory).mkdir();
	    AnalysisFileWriter writer = new AnalysisFileWriter();
	    writer.writeToFileIntegerKey(tripDurationMap, outputDirectory + "/tripDuration.txt", binWidthDuration_min, tripCounter, averageTripDuration);
	    writer.writeToFileIntegerKey(departureTimeMap, outputDirectory + "/departureTime.txt", binWidthTime_h, tripCounter, averageTripDuration);
	    writer.writeToFileStringKey(activityTypeMap, outputDirectory + "/activityTypes.txt", tripCounter);
	    writer.writeToFileIntegerKey(tripDistanceRoutedMap, outputDirectory + "/tripDistanceRouted.txt", binWidthDistance_km, tripCounter, averageTripDistanceRouted);
	    writer.writeToFileIntegerKey(tripDistanceBeelineMap, outputDirectory + "/tripDistanceBeeline.txt", binWidthDistance_km, tripCounter, averageTripDistanceBeeline);
	    writer.writeToFileIntegerKey(averageTripSpeedRoutedMap, outputDirectory + "/averageTripSpeedRouted.txt", binWidthSpeed_km_h, tripCounterSpeed, averageOfAverageTripSpeedsRouted);
	    writer.writeToFileIntegerKey(averageTripSpeedBeelineMap, outputDirectory + "/averageTripSpeedBeeline.txt", binWidthSpeed_km_h, tripCounterSpeed, averageOfAverageTripSpeedsBeeline);
	    writer.writeToFileIntegerKeyCumulative(tripDurationMap, outputDirectory + "/tripDurationCumulative.txt", binWidthDuration_min, tripCounter, averageTripDuration);
	    writer.writeToFileIntegerKeyCumulative(tripDistanceBeelineMap, outputDirectory + "/tripDistanceBeelineCumulative.txt", binWidthDistance_km, tripCounter, averageTripDistanceBeeline);
	    writer.writeToFileIntegerKeyCumulative(averageTripSpeedBeelineMap, outputDirectory + "/averageTripSpeedBeelineCumulative.txt", binWidthSpeed_km_h, tripCounterSpeed, averageOfAverageTripSpeedsBeeline);
	    writer.writeToFileOther(otherInformationMap, outputDirectory + "/otherInformation.txt");
	    
	    // write a routed distance vs. beeline distance comparison file
	    writer.writeRoutedBeelineDistanceComparisonFile(distanceRoutedMap, distanceBeelineMap, outputDirectory + "/beeline.txt", tripCounter);
	}


	private static void calculateAverages() {
		averageTripDuration = aggregateTripDuration / tripCounter;
	    averageTripDistanceRouted = aggregateTripDistanceRouted / tripCounter;
	    averageTripDistanceBeeline = aggregateTripDistanceBeeline / tripCounter;
	    averageOfAverageTripSpeedsRouted = aggregateOfAverageTripSpeedsRouted / tripCounterSpeed;
	    averageOfAverageTripSpeedsBeeline = aggregateOfAverageTripSpeedsBeeline / tripCounterSpeed;
	}


	private static void analyzeTrip(Trip trip) {
		tripCounter++;

		// calculate travel times and store them in a map
		double tripDuration_min = trip.getCalculatedDuration_s() / 60.;
		double tripDuration_h = tripDuration_min / 60.;
		addToMapIntegerKey(tripDurationMap, tripDuration_min, binWidthDuration_min, maxBinDuration_min, 1.);
		aggregateTripDuration = aggregateTripDuration + tripDuration_min;	 

		// store departure times in a map
		double departureTime_h = trip.getDepartureTime_s() / 3600.;
		addToMapIntegerKey(departureTimeMap, departureTime_h, binWidthTime_h, maxBinTime_h, 1.);

		// store activities in a map
		String activityType = trip.getActivityStartActType();
		addToMapStringKey(activityTypeMap, activityType);

		// calculate (routed) distances and and store them in a map
		double tripDistance_m = 0.;
		for (int i = 0; i < trip.getLinks().size(); i++) {
			Id<Link> linkId = trip.getLinks().get(i);
			Link link = network.getLinks().get(linkId);
			double length_m = link.getLength();
			tripDistance_m = tripDistance_m + length_m;
		}
		// TODO here, the distances from activity to link and link to activity are missing!
		double tripDistanceRouted = tripDistance_m / 1000.;

		// store (routed) distances  in a map
		addToMapIntegerKey(tripDistanceRoutedMap, tripDistanceRouted, binWidthDistance_km, maxBinDistance_km, 1.);
		aggregateTripDistanceRouted = aggregateTripDistanceRouted + tripDistanceRouted;
		distanceRoutedMap.put(trip.getTripId(), tripDistanceRouted);

		// store (beeline) distances in a map
		double tripDistanceBeeline = trip.getBeelineDistance(network);
		addToMapIntegerKey(tripDistanceBeelineMap, tripDistanceBeeline, binWidthDistance_km, maxBinDistance_km, 1.);
		aggregateTripDistanceBeeline = aggregateTripDistanceBeeline + tripDistanceBeeline;
		distanceBeelineMap.put(trip.getTripId(), tripDistanceBeeline);

		// calculate speeds and and store them in a map
		if (tripDuration_h > 0.) {
			//System.out.println("trip distance is " + tripDistance + " and time is " + timeInHours);
			double averageTripSpeedRouted = tripDistanceRouted / tripDuration_h;
			addToMapIntegerKey(averageTripSpeedRoutedMap, averageTripSpeedRouted, binWidthSpeed_km_h, maxBinSpeed_km_h, 1.);
			aggregateOfAverageTripSpeedsRouted = aggregateOfAverageTripSpeedsRouted + averageTripSpeedRouted;

			double averageTripSpeedBeeline = tripDistanceBeeline / tripDuration_h;
			addToMapIntegerKey(averageTripSpeedBeelineMap, averageTripSpeedBeeline, binWidthSpeed_km_h, maxBinSpeed_km_h, 1.);
			aggregateOfAverageTripSpeedsBeeline = aggregateOfAverageTripSpeedsBeeline + averageTripSpeedBeeline;

			tripCounterSpeed++;
		} else {
			numberOfTripsWithNoCalculableSpeed++;
		}
	}


	@SuppressWarnings("all")
	private static void adaptOutputDirectory() {
		outputDirectory = outputDirectory + "_" + usedIteration;
	    if (onlyCar == true) {
			outputDirectory = outputDirectory + "_car";
		}
	    if (onlyInterior == true) {
			outputDirectory = outputDirectory + "_int";
	    }
		if (onlyBerlinBased == true) {
			outputDirectory = outputDirectory + "_ber";
		}
		if (distanceFilter == true) {
			outputDirectory = outputDirectory + "_dist";
		}
		if (onlyWorkTrips == true) {
			outputDirectory = outputDirectory + "_work";
		}
		if (ageFilter == true) {
			outputDirectory = outputDirectory + "_age_" + minAge.toString();
			outputDirectory = outputDirectory + "_" + maxAge.toString();
		}
		outputDirectory = outputDirectory + "_2"; // in case used for double-check
	}
	
	
	@SuppressWarnings("unused")
	private static boolean decideIfConsiderTrip(Trip trip){
    	boolean considerTrip = true;

    	// get coordinates of links
    	Id<Link> departureLinkId = trip.getDepartureLinkId();
    	Id<Link> arrivalLinkId = trip.getArrivalLinkId();
//
    	Link departureLink = network.getLinks().get(departureLinkId);
    	Link arrivalLink = network.getLinks().get(arrivalLinkId);

    	// TODO use coords of toNode instead of center coord of link
    	double arrivalCoordX = arrivalLink.getCoord().getX();
    	double arrivalCoordY = arrivalLink.getCoord().getY();
    	double departureCoordX = departureLink.getCoord().getX();
    	double departureCoordY = departureLink.getCoord().getY();

    	// create points
    	Point arrivalLocation = MGC.xy2Point(arrivalCoordX, arrivalCoordY);
    	Point departureLocation = MGC.xy2Point(departureCoordX, departureCoordY);

    	// choose if trip will be considered
    	if (onlyBerlinBased == true) {
    		if (!planningAreaGeometry.contains(arrivalLocation) && !planningAreaGeometry.contains(departureLocation)) {
    			considerTrip = false;
    		}
    	}
    	if (onlyInterior == true) {
    		if (!planningAreaGeometry.contains(arrivalLocation) || !planningAreaGeometry.contains(departureLocation)) {
    			considerTrip = false;
    		}
    	}
//		if (!trip.getMode().equals("car") && !trip.getMode().equals("pt")) {
//			throw new RuntimeException("In current implementation leg mode must either be car or pt");
//		}
    	if (onlyCar == true) {
    		if (!trip.getMode().equals("car")) {
    			considerTrip = false;
    		}
    	}
    	if (distanceFilter == true && trip.getBeelineDistance(network) >= maxDistance_km) {
    		considerTrip = false;
    	}
//    	if (distanceFilter == true && trip.getBeelineDistance(network) <= minDistance) {
//    		considerTrip = false;
//    	}
    	if (onlyWorkTrips == true) {
    		if (trip.getActivityEndActType().equals("work")) {
    			considerTrip = false;
    		}
    	}
    	
    	// TODO The plan was to calculate activity-chain frequencies here. Needs to be done somewhere else
    	// write person activity attributes
//    	if (trip.getActivityEndActType().equals("work")) {
//    		personActivityAttributes.putAttribute(trip.getDriverId(), "hasWorkActivity", true);
//    	}

    	/* Person-specific attributes */
    	if (ageFilter == true) {
    		// TODO needs to be adapted for other analyses that are based on person-specific attributes as well
    		// so far age is the only one
    		String personId = trip.getPersonId().toString();
    		int age = (int) cemdapPersonAttributes.getAttribute(personId, "age");

    		if (age < minAge) {
    			considerTrip = false;
    		}
    		if (age > maxAge) {
    			considerTrip = false;
    		}
    	}

    	return considerTrip;
	}

	
	private static void addToMapIntegerKey(Map <Integer, Double> map, double inputValue, int binWidth, int limitOfLastBin, double weight) {
		double inputValueBin = inputValue / binWidth;
		int ceilOfLastBin = limitOfLastBin / binWidth;		
		// Math.ceil returns the higher integer number (but as a double value)
		int ceilOfValue = (int)Math.ceil(inputValueBin);
		if (ceilOfValue < 0) {
			throw new RuntimeException("Lower end of bin may not be smaller than zero!");
		}
				
		if (ceilOfValue >= ceilOfLastBin) {
			ceilOfValue = ceilOfLastBin;
		}
						
		if (!map.containsKey(ceilOfValue)) {
			map.put(ceilOfValue, weight);
		} else {
			double value = map.get(ceilOfValue);
			value = value + weight;
			map.put(ceilOfValue, value);
		}			
	}
	
	
	private static void addToMapStringKey(Map <String, Double> map, String caption) {
		if (!map.containsKey(caption)) {
			map.put(caption, 1.);
		} else {
			double value = map.get(caption);
			value++;
			map.put(caption, value);
		}
	}	
}