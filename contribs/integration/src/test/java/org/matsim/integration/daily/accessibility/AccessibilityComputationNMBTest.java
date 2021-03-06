/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */
package org.matsim.integration.daily.accessibility;

import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.accessibility.AccessibilityConfigGroup;
import org.matsim.contrib.accessibility.AccessibilityConfigGroup.AreaOfAccesssibilityComputation;
import org.matsim.contrib.accessibility.AccessibilityModule;
import org.matsim.contrib.accessibility.FacilityTypes;
import org.matsim.contrib.accessibility.Modes4Accessibility;
import org.matsim.contrib.accessibility.utils.AccessibilityUtils;
import org.matsim.contrib.accessibility.utils.VisualizationUtils;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.testcases.MatsimTestUtils;

import com.vividsolutions.jts.geom.Envelope;

/**
 * @author dziemke
 */
public class AccessibilityComputationNMBTest {
	public static final Logger log = Logger.getLogger(AccessibilityComputationNMBTest.class);
	
	@Rule public MatsimTestUtils utils = new MatsimTestUtils();
	
//	@Test
//	public void testQuick() {
//		run(1000., false, false);
//	}
//	@Test
//	public void testLocal_beta10() {
//		run(500., false, true);
//	}
	@Test
	public void testOnServer() {
		run(1000., true, false);
	}
	
	public void run(Double cellSize, boolean push2Geoserver, boolean createQGisOutput) {

		final Config config = ConfigUtils.createConfig(new AccessibilityConfigGroup());
		
		// Network file
		String folderStructure = "../../";
		String networkFile = "matsimExamples/countries/za/nmb/network/NMBM_Network_CleanV7.xml.gz";
		// Adapt folder structure that may be different on different machines, in particular on server
		folderStructure = PathUtils.tryANumberOfFolderStructures(folderStructure, networkFile);
		config.network().setInputFile(folderStructure + networkFile);
		
		// ---------- Change Events
//		String changeEventsInputFile = "/Users/Dominik/Accessibility/Data/nmbm_change/changeevents200.xml.gz";
//		config.network().setTimeVariantNetwork(true);
//		config.network().setChangeEventsInputFile(changeEventsInputFile);
		// ----------
		
		config.facilities().setInputFile(folderStructure + "matsimExamples/countries/za/nmb/facilities/20121010/facilities.xml.gz");
		
		config.controler().setOutputDirectory(utils.getOutputDirectory());
		
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setLastIteration(0);
		config.controler().setRunId("za_nmb_" + AccessibilityUtils.getDate() + "_" + cellSize.toString().split("\\.")[0]);
		
		// Use other beta
//		config.planCalcScore().setBrainExpBeta(10);
		//
		
//		final String outputDirectory = "../../../shared-svn/projects/maxess/data/nmb/output/46/";
//		final String travelTimeMatrixFile = folderStructure + "matsimExamples/countries/za/nmb/regular-pt/travelTimeMatrix_space.csv";
//		final String travelDistanceMatrixFile = folderStructure + "matsimExamples/countries/za/nmb/regular-pt/travelDistanceMatrix_space.csv";
//		final String ptStopsFile = folderStructure + "matsimExamples/countries/za/nmb/regular-pt/ptStops.csv";
		
		AccessibilityConfigGroup acg = ConfigUtils.addOrGetModule(config, AccessibilityConfigGroup.class);
		acg.setCellSizeCellBasedAccessibility(cellSize.intValue());
		acg.setComputingAccessibilityForMode(Modes4Accessibility.walk, true);
		acg.setComputingAccessibilityForMode(Modes4Accessibility.freespeed, true);
		acg.setComputingAccessibilityForMode(Modes4Accessibility.car, true);
		acg.setComputingAccessibilityForMode(Modes4Accessibility.bike, true);
		acg.setOutputCrs(TransformationFactory.WGS84_SA_Albers);
		
		// ---------- Change Events
//		acg.setTimeOfDay(16.*60.*60.);
		// ----------
		
		acg.setAreaOfAccessibilityComputation(AreaOfAccesssibilityComputation.fromNetwork.toString());
		// Network bounds to determine envelope
//		BoundingBox networkBounds = BoundingBox.createBoundingBox(scenario.getNetwork());
//		Envelope networkEnvelope = new Envelope(networkBounds.getXMin(), networkBounds.getXMax(), networkBounds.getYMin(), networkBounds.getYMax());
		
		ConfigUtils.setVspDefaults(config);
		
		final Scenario scenario = ScenarioUtils.loadScenario(config);
		
		// ---------- Change speeds
//		Network network = scenario.getNetwork();
//		double maxSpeed = 35./3.6;
//		for (Link link : network.getLinks().values()) {
//			System.out.println("----- link = " + link.getId() + " -- speed before = " + link.getFreespeed());
//			link.setFreespeed(Math.min(maxSpeed, link.getFreespeed()));
//			System.out.println("----- link = " + link.getId() + " -- speed after = " + link.getFreespeed());
//		}
		// ----------
		
		// Matrix-based pt
//		MatrixBasedPtRouterConfigGroup mbpcg = ConfigUtils.addOrGetModule(config, MatrixBasedPtRouterConfigGroup.GROUP_NAME, MatrixBasedPtRouterConfigGroup.class);
//		mbpcg.setPtStopsInputFile(ptStopsFile);
//		mbpcg.setUsingTravelTimesAndDistances(true);
//		mbpcg.setPtTravelDistancesInputFile(travelDistanceMatrixFile);
//		mbpcg.setPtTravelTimesInputFile(travelTimeMatrixFile);
		
		// Activity types
		final List<String> activityTypes = Arrays.asList(new String[]{FacilityTypes.SHOPPING, FacilityTypes.LEISURE, FacilityTypes.OTHER, FacilityTypes.EDUCATION});
//		final List<String> activityTypes = Arrays.asList(new String[]{FacilityTypes.EDUCATION});
		log.info("Using activity types: " + activityTypes);
		
		// Combine certain activity options into one combined computation
//		String combinedType = "w-eq";
//		final List<String> activityTypes = Arrays.asList(new String[]{combinedType}); // Only include the combined type into this list!
//		final List<String> activityOptionsToBeIncluded = Arrays.asList(new String[]{FacilityTypes.SHOPPING, FacilityTypes.LEISURE, FacilityTypes.OTHER, FacilityTypes.EDUCATION});
//		AccessibilityUtils.combineDifferentActivityOptionTypes(scenario, combinedType, activityOptionsToBeIncluded);
		
		// Collect homes for density layer
		String activityFacilityType = FacilityTypes.HOME;
		ActivityFacilities densityFacilities = AccessibilityUtils.collectActivityFacilitiesWithOptionOfType(scenario, activityFacilityType);
		// Network density points (as proxy for population density)
//		final ActivityFacilities densityFacilities = AccessibilityUtils.createFacilityForEachLink(scenario.getNetwork()); // will be aggregated in downstream code!
		
		final Controler controler = new Controler(scenario);
		
		for (String activityType : activityTypes) {
			AccessibilityModule module = new AccessibilityModule();
			module.setConsideredActivityType(activityType);
			module.addAdditionalFacilityData(densityFacilities);
			module.setPushing2Geoserver(push2Geoserver);
			controler.addOverridingModule(module);
		}
		
		controler.run();
		
		// QGis
		if (createQGisOutput) {
			final boolean includeDensityLayer = true;
			final Integer range = 9; // In the current implementation, this must always be 9
			final Double lowerBound = 0.5; // (upperBound - lowerBound) ideally nicely divisible by (range - 2)
			final Double upperBound = 4.0;
			final int populationThreshold = (int) (50 / (1000/cellSize * 1000/cellSize));
			
			String osName = System.getProperty("os.name");
			String workingDirectory = config.controler().getOutputDirectory();
			for (String actType : activityTypes) {
				String actSpecificWorkingDirectory = workingDirectory + actType + "/";
				for (Modes4Accessibility mode : acg.getIsComputingMode()) {
					// TODO maybe use envelope and crs from above
//					VisualizationUtils.createQGisOutput(actType, mode.toString(), new Envelope(115000,161000,-3718000,-3679000), workingDirectory, TransformationFactory.WGS84_SA_Albers, includeDensityLayer,
					VisualizationUtils.createQGisOutput(actType, mode.toString(), new Envelope(100000,180000,-3720000,-3675000), workingDirectory, TransformationFactory.WGS84_SA_Albers, includeDensityLayer,
							lowerBound, upperBound, range, cellSize.intValue(), populationThreshold);
					VisualizationUtils.createSnapshot(actSpecificWorkingDirectory, mode.toString(), osName);
				}
			}  
		}
	}
}