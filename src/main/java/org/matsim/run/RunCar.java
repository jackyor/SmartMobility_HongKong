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
package org.matsim.run;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.drt.extension.fiss.FISSConfigGroup;
import org.matsim.contrib.drt.extension.fiss.FISSConfigurator;
import org.matsim.contrib.drt.run.DrtControlerCreator;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.mobsim.qsim.components.QSimComponentsConfigurator;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.optDRT.MultiModeOptDrtConfigGroup;
import org.matsim.simwrapper.SimWrapperModule;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author nagel
 *
 */
public class RunCar {
	public static void main(String[] args) {
		Config config = ConfigUtils.loadConfig( "scenarios/final_car/car.config.xml" ) ;

		// possibly modify config here

		// ---

		Scenario scenario = ScenarioUtils.loadScenario(config) ;

		// possibly modify scenario here

		// ---

		Controler controler = new Controler( scenario ) ;

		// possibly modify controler here

//		controler.addOverridingModule( new OTFVisLiveModule() ) ;

		// ---

		controler.run();

	}
	
}
