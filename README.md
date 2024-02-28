# SmartMobility_HongKong

## Scenario
This research focuses on a scenario where daily commuting trips by taxi are replaced exclusively by ridepooling. The MATsim simulation, which is used for this study, requires inputs such as the road network, agents’ daily activities plan, vehicles, and a general configuration file. The road network is based on OpenStreetMap and the agents’ daily activities plan is derived from the Travel Characteristics Survey 2011. The simulation focuses only on commuting trips in the morning and evening. The mode choice for agents is restricted to ridepooling and walking only, with a maximum waiting time of ten minutes.

Three scenarios are considered in this study:

1. **Base case scenario**: Passengers exclusively choose private rides. The results from this scenario serve as a benchmark for comparison.
2. **Stop-based scenario**: Ridepooling routes are based on predefined stops (virtual taxi stands extracted from OpenStreetMap). There are a total of 331 predefined stops in Hong Kong.
3. **Door-to-door scenario**: Ridepooling routes are based on passengers’ requests. Passengers are picked up and dropped off at their requested locations.

The simulations are run on the Amazon Elastic Compute Cloud, with the system configuration detailed in Appendix I. The study aims to understand the impact and efficiency of ridepooling as a replacement for traditional taxi commutes.


## Run
This model was executed on an Amazon EC2 instance. The instance type was c7g.8xlarge, equipped with 32 CPU cores and 64GB of memory. The version of the Java Virtual Machine (JVM) used was 19.0.2.

To run this model, follow these steps:

1) Download the repository.
2) Open the terminal.
3) type this
```
java -jar -Xmx64g matsim-code-examples-master-SNAPSHOT.jar scenarios/opt/stop2s.config.xml
java -jar -Xmx[Max memory] matsim-code-examples-master-SNAPSHOT.jar [path to config file]
```
For your information, a full simulation consisting of 16 iterations took approximately 7 hours to complete with the aforementioned configuration.

## Output
The performance analysis data for Demand Responsive Transport (DRT) includes customer statistics, detailed vehicle statistics, and overall vehicle statistics. [findings.md](https://github.com/jackyor/matsim_HongKong/blob/main/findings.md) contains figures derived from the model’s output statistics and a comprehensive analysis of two research questions:

RQ1: How does ride-pooling outperform exclusive rides in terms of transport efficiency in Hong Kong?

RQ2: What is the ideal mix of vehicle capacity and fleet size for efficient ridepooling in Hong Kong?

A comparison of the general performance of ridepooling versus exclusive rides (e.g., taxi) in terms of Vehicle Kilometers Traveled (VKT) is also provided.

<img width="661" alt="Screenshot 2023-12-16 at 6 45 03 PM" src="https://github.com/jackyor/matsim_HongKong/assets/87265896/67b2097d-eb11-4c71-a389-8249dd0661e3">


## Input Data Preperation
### Network
1) Download the Hong Kong map file from [OpenStreetMap](https://www.openstreetmap.org/).
2) Follow the instructions in [pt2matsim](https://github.com/matsim-org/pt2matsim) to create a network.
3) If the model requires public transport, use pt2matsim to create a multimodal network and transit schedule for simulating public transport in MATSim.

### Plan
The MATSim plan file uses a synthetic population based on the [Travel Characteristics Survey 2011](https://www.td.gov.hk/en/publications_and_press_releases/publications/free_publications/travel_characteristics_survey_2011_final_report/index.html) and [Census 2021](https://www.census2021.gov.hk/en/index.html). This population, created from general parameters, focuses on daily taxi commuting trips. The generation process involves several steps.

1) Get inner and inter commuting data
The first step in preparing the data involves obtaining the [Statistics by District Council Constituency Area](https://www.census2021.gov.hk/doc/DCCA_21C.xlsx) from the 2021 Census. This provides information on the **Working population by place of work** including Work in the same district; Work in another district on Hong Kong Island; Work in another district in Kowloon; Work in another district in new towns; and Work in another district in other areas in the New Territories. The statistics are grouped by District Council Constituency Area.

2) Mark buildings to their District Council and District Council Constituency Areas
The buildings in Hong Kong, obtained from the [Lands Department](https://data.gov.hk/en-data/dataset/hk-landsd-openmap-b50k-topographic-map-of-hong-kong), are marked according to their District Council and District Council Constituency Areas using GIS. This data is used to randomly select home and work coordinates. For more precision, residential buildings and others can be separated during the selection process.

3) Generate inter and inner district commuting work trips
Create `inner.csv` and `inter.csv` files from the commuting data, similar to the CSV files in the "forplan" folder. This is done using [Table A.2 Daily Person Trip Productions and Attractions by Broad District and Trip Purpose](https://www.td.gov.hk/filemanager/en/content_4652/tcs2011app_eng.pdf) from the Travel Characteristics Survey 2011. 

Next, set the home end time and work end time. Initially, these times are the same for every plan. However, they are redistributed according to the Figure 3.3 Hourly Profiles of Mechanised Trips from the Travel Characteristics Survey 2011 final report. This redistribution is done using a Python script, `main.py`.

<img width="651" alt="Figure 3.3 Hourly Profiles of Mechanised Trips in Travel Characteristics Survey 2011 final report" src="https://github.com/jackyor/matsim_HongKong/assets/87265896/a587955f-3b49-4996-abf9-5d1870cbd569">

Next, commuting work trips within and between districts were generated. According to Table A.3 from the Travel Characteristics Survey 2011, taxi trips accounted for about 3.72% (230,000 out of 6,179,000) of all home-based work trips. Therefore, only 3.72% of home-based work trips were generated for this model. The home coordinates were randomly selected from a pool of buildings within the designated District Council Constituency Areas, as listed in the inner or inter CSV file. Similarly, the work coordinates were randomly selected from a pool of buildings within the designated District Council or corresponding New Towns. In this model, all plans follow a home-work-home activity structure.

### Drt Vehicle
Utilize the CreateFleetVehicles.java file to generate vehicle files with a specified number of vehicles and operating hours. In this model, the Demand Responsive Transport (DRT) vehicles operate 24 hours a day.

### Drt Stops
In the stop-based ridepooling scenario, virtual stops for DRT vehicles are defined in the `drtStops.xml` file. These stops, representing points for pick-up and drop-off, are extracted from OpenStreetMap as Taxi Stand locations in Hong Kong.

## MATSim
For more information about MATSim, visit the official [MATSim.org](https://www.matsim.org/) website. The configuration files for this model are located in the `scenarios/opt` folder and have the extension `.config.xml`. The `door2s` configuration refers to a door-to-door ridepooling scenario with a vehicle capacity of 2, while `stop2s` refers to a stop-based ridepooling scenario also with a vehicle capacity of 2. Each simulation runs for a total of 16 iterations.

The model uses the DRT, DVRP, and drt-extension modules for simulating ridepooling (these can be found in the `pom.xml` file). These modules are available in the [matsim-libs repository](https://github.com/matsim-org/matsim-libs/tree/master/contribs). The model also uses the [FISS (Flow-inflated Selective Sampling)](https://github.com/matsim-org/matsim-libs/tree/master/contribs/drt-extensions/src/main/java/org/matsim/contrib/drt/extension/fiss) method from the drt-extension to halve the computation time.

The `simwrapper` extension is used for analyzing output in dashboards. More details can be found on the [simwrapper website](https://simwrapper.github.io/). 

[optDRT](https://github.com/matsim-vsp/opt-drt) is also utilized in the model, but not as an extension. The optDRT Java classes can be found in this repository. It is used to increase the fleet number of DRT vehicles if the 90th percentile of waiting time exceeds 360 seconds or 6 minutes after an iteration.
