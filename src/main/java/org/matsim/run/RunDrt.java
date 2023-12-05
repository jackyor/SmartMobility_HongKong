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

import org.matsim.core.mobsim.qsim.components.QSimComponentsConfigurator;
import org.matsim.optDRT.MultiModeOptDrtConfigGroup;
import org.matsim.optDRT.OptDrt;
import org.matsim.contrib.drt.extension.fiss.FISSConfigGroup;
import org.matsim.contrib.drt.extension.fiss.FISSConfigurator;
import org.matsim.contrib.drt.run.DrtControlerCreator;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.simwrapper.SimWrapperModule;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author nagel
 *
 */
public class RunDrt {
	public static void main(String[] args) {
		String configFile = "scenarios/final_drt/car.config.xml";

		Config config = ConfigUtils.loadConfig(configFile, new MultiModeDrtConfigGroup(),
				new DvrpConfigGroup(), new OTFVisConfigGroup(),new MultiModeOptDrtConfigGroup());
		Controler controler = DrtControlerCreator.createControler(config, false);
		//fiss part
		FISSConfigGroup fissConfigGroup = ConfigUtils.addOrGetModule(config, FISSConfigGroup.class);
		//fissConfigGroup.sampleFactor = 1;
		//fissConfigGroup.sampledModes = Set.of(TransportMode.car);
		//for getting events of agents
		fissConfigGroup.switchOffFISSLastIteration = false;
		FISSConfigurator.configure(controler);
		QSimComponentsConfigurator qSimComponentsConfigurator = FISSConfigurator
				.activateModes(List.of(), MultiModeDrtConfigGroup.get(config).modes().collect(Collectors.toList()));
		controler.configureQSimComponents(qSimComponentsConfigurator);

		//opt
		//OptDrt.addAsOverridingModule(controler, ConfigUtils.addOrGetModule(config, MultiModeOptDrtConfigGroup.class));

		controler.addOverridingModule(new SimWrapperModule());

		controler.run();

	}
	
}
