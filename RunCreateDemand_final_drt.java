import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;

import java.io.IOException;
import java.nio.file.Paths;

public class RunCreateDemand_final_drt {

	public static void main(String[] argsf) throws IOException {

		CreateDemand_final_drt createDemand = new CreateDemand_final_drt();
		createDemand.create();
		Population result = createDemand.getPopulation();

		new PopulationWriter(result).write(Paths.get("prepare/plan_drt3.0.xml").toString());
	}
}
