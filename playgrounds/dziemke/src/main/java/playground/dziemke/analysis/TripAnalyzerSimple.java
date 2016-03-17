package playground.dziemke.analysis;

import java.io.File;
import java.util.Map;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkUtils;
import org.matsim.utils.objectattributes.ObjectAttributes;

/**
 * @author dziemke
 */
public class TripAnalyzerSimple {
	
	/* Parameters */
	private static final String runId = "run_168a";
	private static final String usedIteration = "300"; // most frequently used value: 150

	private static final int maxBinDuration = 120;
	private static final int binWidthDuration = 1;

	private static final int maxBinTime = 23;
	private static final int binWidthTime = 1;

	private static final int maxBinDistance = 60;
	private static final int binWidthDistance = 1;

	private static final int maxBinSpeed = 60;
	private static final int binWidthSpeed = 1;


	/* Input and output */
	private static final String networkFile = "../../../shared-svn/studies/countries/de/berlin/counts/iv_counts/network.xml";
	private static final String eventsFile = "../../../runs-svn/cemdapMatsimCadyts/" + runId + "/ITERS/it." + usedIteration + "/" + runId + "." + usedIteration + ".events.xml.gz";
	private static String outputDirectory = "../../../runs-svn/cemdapMatsimCadyts/" + runId + "/analysis";

	
	/* Variables to store objects */
	private static Network network;
	
	
	@SuppressWarnings("unused")
	public static void main(String[] args) {
		// Create an EventsManager instance (MATSim infrastructure)
	    EventsManager eventsManager = EventsUtils.createEventsManager();
	    TripHandler handler = new TripHandler();
	    eventsManager.addHandler(handler);
	 
	    // Connect a file reader to the EventsManager and read in the event file
	    MatsimEventsReader reader = new MatsimEventsReader(eventsManager);
	    reader.readFile(eventsFile);
	    System.out.println("Events file read!");
	    
	    // check if all trips have been completed; if so, result will be zero
	    int numberOfIncompleteTrips = 0;
	    for (Trip trip : handler.getTrips().values()) {
	    	if(!trip.getTripComplete()) { numberOfIncompleteTrips++; }
	    }
	    System.out.println(numberOfIncompleteTrips + " trips are incomplete.");

	    
	    /* Get network, which is needed to calculate distances */
	    network = NetworkUtils.createNetwork();
	    MatsimNetworkReader networkReader = new MatsimNetworkReader(network);
	    networkReader.readFile(networkFile);
    	
    	// create objects
    	int tripCounter = 0;
    	int tripCounterSpeed = 0;
    	int tripCounterIncomplete = 0;
    	
    	Map <Integer, Double> tripDurationMap = new TreeMap <Integer, Double>();
	    double aggregateTripDuration = 0.;
	    
	    Map <Integer, Double> departureTimeMap = new TreeMap <Integer, Double>();
	    
	    Map <String, Double> activityTypeMap = new TreeMap <String, Double>();
	    
	    Map <Integer, Double> tripDistanceRoutedMap = new TreeMap <Integer, Double>();
		double aggregateTripDistanceRouted = 0.;
		
		Map <Integer, Double> tripDistanceBeelineMap = new TreeMap <Integer, Double>();
		double aggregateTripDistanceBeeline = 0.;
	    
		Map <Integer, Double> averageTripSpeedRoutedMap = new TreeMap <Integer, Double>();
	    double aggregateOfAverageTripSpeedsRouted = 0.;
	    
	    Map <Integer, Double> averageTripSpeedBeelineMap = new TreeMap <Integer, Double>();
	    double aggregateOfAverageTripSpeedsBeeline = 0.;
	    

	    int numberOfTripsWithNoCalculableSpeed = 0;
	    
	    Map <Id<Trip>, Double> distanceRoutedMap = new TreeMap <Id<Trip>, Double>();
	    Map <Id<Trip>, Double> distanceBeelineMap = new TreeMap <Id<Trip>, Double>();
	    
	    Map <String, Integer> otherInformationMap = new TreeMap <String, Integer>();
	    
	    // --------------------------------------------------------------------------------------------------
	    ObjectAttributes personActivityAttributes = new ObjectAttributes();
	    // --------------------------------------------------------------------------------------------------
	    
	    
	    // do calculations
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
	    	

//	    	if (decideIfConsiderTrip(trip) == true) {
	    		tripCounter++;

	    		// calculate travel times and store them in a map
	    		// trip.getArrivalTime() / trip.getDepartureTime() yields values in seconds!
	    		double departureTime_s = trip.getDepartureTime_s();
	    		double arrivalTime_s = trip.getArrivalTime_s();
	    		double tripDuration_s = arrivalTime_s - departureTime_s;
	    		double tripDuration_min = tripDuration_s / 60.;
	    		double tripDuration_h = tripDuration_min / 60.;
	    		addToMapIntegerKey(tripDurationMap, tripDuration_min, binWidthDuration, maxBinDuration, 1.);
	    		aggregateTripDuration = aggregateTripDuration + tripDuration_min;	 

	    		// store departure times in a map
	    		double departureTime_h = departureTime_s / 3600.;
	    		addToMapIntegerKey(departureTimeMap, departureTime_h, binWidthTime, maxBinTime, 1.);

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
	    		addToMapIntegerKey(tripDistanceRoutedMap, tripDistanceRouted, binWidthDistance, maxBinDistance, 1.);
	    		aggregateTripDistanceRouted = aggregateTripDistanceRouted + tripDistanceRouted;
	    		distanceRoutedMap.put(trip.getTripId(), tripDistanceRouted);

	    		// store (beeline) distances in a map
	    		double tripDistanceBeeline = trip.getBeelineDistance(network);
	    		addToMapIntegerKey(tripDistanceBeelineMap, tripDistanceBeeline, binWidthDistance, maxBinDistance, 1.);
	    		aggregateTripDistanceBeeline = aggregateTripDistanceBeeline + tripDistanceBeeline;
	    		distanceBeelineMap.put(trip.getTripId(), tripDistanceBeeline);

	    		// calculate speeds and and store them in a map
	    		if (tripDuration_h > 0.) {
	    			//System.out.println("trip distance is " + tripDistance + " and time is " + timeInHours);
	    			double averageTripSpeedRouted = tripDistanceRouted / tripDuration_h;
	    			addToMapIntegerKey(averageTripSpeedRoutedMap, averageTripSpeedRouted, binWidthSpeed, maxBinSpeed, 1.);
	    			aggregateOfAverageTripSpeedsRouted = aggregateOfAverageTripSpeedsRouted + averageTripSpeedRouted;

	    			double averageTripSpeedBeeline = tripDistanceBeeline / tripDuration_h;
	    			addToMapIntegerKey(averageTripSpeedBeelineMap, averageTripSpeedBeeline, binWidthSpeed, maxBinSpeed, 1.);
	    			aggregateOfAverageTripSpeedsBeeline = aggregateOfAverageTripSpeedsBeeline + averageTripSpeedBeeline;

	    			tripCounterSpeed++;
	    		} else {
	    			numberOfTripsWithNoCalculableSpeed++;
	    		}
//	    	}
	    }	    
	    
	    double averageTripDuration = aggregateTripDuration / tripCounter;
	    double averageTripDistanceRouted = aggregateTripDistanceRouted / tripCounter;
	    double averageTripDistanceBeeline = aggregateTripDistanceBeeline / tripCounter;
	    double averageOfAverageTripSpeedsRouted = aggregateOfAverageTripSpeedsRouted / tripCounterSpeed;
	    double averageOfAverageTripSpeedsBeeline = aggregateOfAverageTripSpeedsBeeline / tripCounterSpeed;
	    
	    
	    otherInformationMap.put("Number of trips that have no previous activity", handler.getNoPreviousEndOfActivityCounter());
	    otherInformationMap.put("Number of trips that have no calculable speed", numberOfTripsWithNoCalculableSpeed);
	    otherInformationMap.put("Number of incomplete trips (i.e. number of removed agents)", tripCounterIncomplete);
	    otherInformationMap.put("Number of (complete) trips", tripCounter);
	 
	    
	    // write results to files
	    new File(outputDirectory).mkdir();
	    AnalysisFileWriter writer = new AnalysisFileWriter();
	    writer.writeToFileIntegerKey(tripDurationMap, outputDirectory + "/tripDuration.txt", binWidthDuration, tripCounter, averageTripDuration);
	    writer.writeToFileIntegerKey(departureTimeMap, outputDirectory + "/departureTime.txt", binWidthTime, tripCounter, averageTripDuration);
	    writer.writeToFileStringKey(activityTypeMap, outputDirectory + "/activityTypes.txt", tripCounter);
	    writer.writeToFileIntegerKey(tripDistanceRoutedMap, outputDirectory + "/tripDistanceRouted.txt", binWidthDistance, tripCounter, averageTripDistanceRouted);
	    writer.writeToFileIntegerKey(tripDistanceBeelineMap, outputDirectory + "/tripDistanceBeeline.txt", binWidthDistance, tripCounter, averageTripDistanceBeeline);
	    writer.writeToFileIntegerKey(averageTripSpeedRoutedMap, outputDirectory + "/averageTripSpeedRouted.txt", binWidthSpeed, tripCounterSpeed, averageOfAverageTripSpeedsRouted);
	    writer.writeToFileIntegerKey(averageTripSpeedBeelineMap, outputDirectory + "/averageTripSpeedBeeline.txt", binWidthSpeed, tripCounterSpeed, averageOfAverageTripSpeedsBeeline);
	    writer.writeToFileIntegerKeyCumulative(tripDurationMap, outputDirectory + "/tripDurationCumulative.txt", binWidthDuration, tripCounter, averageTripDuration);
	    writer.writeToFileIntegerKeyCumulative(tripDistanceBeelineMap, outputDirectory + "/tripDistanceBeelineCumulative.txt", binWidthDistance, tripCounter, averageTripDistanceBeeline);
	    writer.writeToFileIntegerKeyCumulative(averageTripSpeedBeelineMap, outputDirectory + "/averageTripSpeedBeelineCumulative.txt", binWidthSpeed, tripCounterSpeed, averageOfAverageTripSpeedsBeeline);
	    writer.writeToFileOther(otherInformationMap, outputDirectory + "/otherInformation.txt");
	    
	    // write a routed distance vs. beeline distance comparison file
	    writer.writeRoutedBeelineDistanceComparisonFile(distanceRoutedMap, distanceBeelineMap, outputDirectory + "/beeline.txt", tripCounter);
	}


	private static void addToMapIntegerKey(Map <Integer, Double> map, double inputValue, int binWidth, int limitOfLastBin, double weight) {
		double inputValueBin = inputValue / binWidth;
		int ceilOfLastBin = limitOfLastBin / binWidth;		
		// Math.ceil returns the higher integer number (but as a double value)
		int ceilOfValue = (int)Math.ceil(inputValueBin);
		if (ceilOfValue < 0) {
			new RuntimeException("Lower end of bin may not be smaller than zero!");
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