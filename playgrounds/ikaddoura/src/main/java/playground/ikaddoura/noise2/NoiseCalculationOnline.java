/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.ikaddoura.noise2;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.StartupListener;


/**
 * @author lkroeger, ikaddoura
 *
 */

public class NoiseCalculationOnline implements BeforeMobsimListener, AfterMobsimListener , IterationEndsListener , StartupListener {
	private static final Logger log = Logger.getLogger(NoiseCalculationOnline.class);
	
	private NoiseParameters noiseParameters;
	private NoiseInitialization initialization;
	private NoiseEmissionHandler noiseEmissionHandler;
	private NoiseImmissionCalculation noiseImmission;
	private PersonActivityHandler personActivityTracker;
	private NoiseDamageCalculation noiseDamageCosts;
	
	private Map<Id<ReceiverPoint>, ReceiverPoint> receiverPoints = new HashMap<Id<ReceiverPoint>, ReceiverPoint>();
	
	public NoiseCalculationOnline(NoiseParameters noiseParameters) {
		this.noiseParameters = noiseParameters;
	}

	@Override
	public void notifyStartup(StartupEvent event) {
		
		log.info("Initialization...");
		
		this.initialization = new NoiseInitialization(event.getControler().getScenario(), noiseParameters, this.receiverPoints);
		this.initialization.initialize();
		NoiseWriter.writeReceiverPoints(this.receiverPoints, event.getControler().getConfig().controler().getOutputDirectory() + "/receiverPoints/");
		
		log.info("Initialization... Done.");

		this.noiseEmissionHandler = new NoiseEmissionHandler(event.getControler().getScenario(), noiseParameters);
		this.personActivityTracker = new PersonActivityHandler(event.getControler().getScenario(), noiseParameters, initialization, this.receiverPoints);
						
		event.getControler().getEvents().addHandler(noiseEmissionHandler);
		event.getControler().getEvents().addHandler(personActivityTracker);
	}
	
	@Override
	public void notifyBeforeMobsim(BeforeMobsimEvent event) {
		log.info("Resetting noise immissions, activity information and damages...");
		for (ReceiverPoint rp : this.receiverPoints.values()) {
			rp.getTimeInterval2actInfos().clear();
			rp.getTimeInterval2affectedAgentUnits().clear();
			rp.getTimeInterval2damageCostPerAffectedAgentUnit().clear();
			rp.getTimeInterval2damageCosts().clear();
			rp.getTimeInterval2immission().clear();
			rp.getTimeInterval2LinkId2IsolatedImmission().clear();	
		}
		log.info("Resetting noise immissions, activity information and damages... Done.");
	}
	
	@Override
	public void notifyAfterMobsim(AfterMobsimEvent event) {
				
		log.info("Calculating noise emission...");
		this.noiseEmissionHandler.calculateNoiseEmission();
		NoiseWriter.writeNoiseEmissionsStats(this.noiseEmissionHandler, this.noiseParameters, event.getControler().getConfig().controler().getOutputDirectory() + "/ITERS/it." + event.getIteration() + "/emissionStats.csv");
		NoiseWriter.writeNoiseEmissionStatsPerHour(this.noiseEmissionHandler, this.noiseParameters, event.getControler().getConfig().controler().getOutputDirectory() + "/ITERS/it." + event.getIteration() + "/emissionStatsPerHour.csv");
		log.info("Calculating noise emission... Done.");
		
		log.info("Calculating noise immission...");
		this.noiseImmission = new NoiseImmissionCalculation(this.initialization, this.noiseEmissionHandler, noiseParameters, this.receiverPoints);
		noiseImmission.calculateNoiseImmission();
		NoiseWriter.writeNoiseImmissionStats(this.receiverPoints, this.noiseParameters, event.getControler().getConfig().controler().getOutputDirectory() + "/ITERS/it." + event.getIteration() + "/immissionStats.csv");
		NoiseWriter.writeNoiseImmissionStatsPerHour(this.receiverPoints, this.noiseParameters, event.getControler().getConfig().controler().getOutputDirectory() + "/ITERS/it." + event.getIteration() + "/immissionStatsPerHour.csv");
		log.info("Calculating noise immission... Done.");
		
		log.info("Calculating each agent's activity durations...");
		this.personActivityTracker.calculateDurationsOfStay();
		log.info("Calculating each agent's activity durations... Done.");
			
		log.info("Calculating noise damage costs and throwing noise events...");
		this.noiseDamageCosts = new NoiseDamageCalculation(event.getControler().getScenario(), event.getControler().getEvents(), noiseParameters, noiseEmissionHandler, noiseImmission, this.receiverPoints);
		this.noiseDamageCosts.calculateNoiseDamageCosts();
		log.info("Calculating noise damage costs and throwing noise events... Done.");
		
	}
	
	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		log.info("Total Caused Noise Cost: " + noiseDamageCosts.getTotalCausedNoiseCost());
		log.info("Total Affected Noise Cost: " + noiseDamageCosts.getTotalAffectedNoiseCost());
	}
	
	// for testing purposes
	NoiseEmissionHandler getNoiseEmissionHandler() {
		return noiseEmissionHandler;
	}

	NoiseImmissionCalculation getNoiseImmission() {
		return noiseImmission;
	}

	NoiseDamageCalculation getNoiseDamageCosts() {
		return noiseDamageCosts;
	}

	NoiseInitialization getSpatialInfo() {
		return initialization;
	}

	Map<Id<ReceiverPoint>, ReceiverPoint> getReceiverPoints() {
		return receiverPoints;
	}
		
}
