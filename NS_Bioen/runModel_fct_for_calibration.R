### Calibration function ### 
### 31/07/2019 ###
"C:/Users/amorell/Desktop/Evo-osmose/NS_Bioen"
runModel  = function(param, names, ...) {
  setwd("C:/Users/amorell/Desktop/Evo-osmose/NS_Bioen/master")
  
  # set parameter names
  names(param) = names
  
  # writes the calibrated parameters into a CSV file
  # following Osmose format. The parameters in this file
  # will overwrite the Osmose parameter
  # write.table(param, file="calibration-parameters.csv", sep=";", 
              #col.names=FALSE, quote=FALSE)
  
  # defines the user directory
  outdir = "output"
  
  # run Osmose Model
  osmose = "C:/Users/amorell/Desktop/Evo-osmose/dist/alaia_bioen.jar"
  
  
  run_osmose(input="calib_config.csv", options=NULL,osmose = osmose, log="osmose.log")   
  
  # read Osmose outputs 
  data = read_osmose(path=outdir)
  
  # extract the growth potential, the biomass and yields variables (monthly data).
  # expectes = TRUE to average over the replicates
  osmose.growthpot = getVar(data, "growthpot", expected=TRUE)
  osmose.biomass = getVar(data, "biomass", expected=TRUE)
  osmose.landings = getVar(data, "yield", expected=TRUE)
  
  # define a year factor for yearly integration of biomass
  # from monthly biomass
  biomassDim = dim(osmose.biomass)   # dims=(time, species, replic)
  ntime = biomassDim[1]    # nyears * 12
  nspecies = biomassDim[2]
  nyears = ntime / 12
  years = factor(rep(1:nyears, each=12))
  
  # Integration of monthly values into yearly values
  # here, tapply is applied on dimension 1 (i.e. time)
  osmose.biomass = apply(osmose.biomass, 2, tapply, years, sum)
  osmose.landings = apply(osmose.landings, 2, tapply, years, sum)
  
  
  output = list( 
    # Biomass  
  AtlanticHerring.biomass = osmose.biomass[,"AtlanticHerring"],
  AtlanticMackerel.biomass = osmose.biomass[,"AtlanticMackerel"],
  Sandeel.biomass = osmose.biomass[,"Sandeel"],
  EuropeanSprat.biomass = osmose.biomass[,"EuropeanSprat"],
  NorwayPout.biomass = osmose.biomass[,"NorwayPout"],
  EuropeanPlaice.biomass = osmose.biomass[,"EuropeanPlaice"],
  CommonSole.biomass = osmose.biomass[,"CommonSole"],
  Saithe.biomass = osmose.biomass[,"Saithe"],
  AtlanticCod.biomass = osmose.biomass[,"AtlanticCod"],
  Haddock.biomass = osmose.biomass[,"Haddock"],
  HorseMackerel.biomass = osmose.biomass[,"HorseMackerel"],
  Whiting.biomass = osmose.biomass[,"Whiting"],
  CommonDab.biomass = osmose.biomass[,"CommonDab"],
  GreyGurnard.biomass = osmose.biomass[,"GreyGurnard"],
  Hake.biomass = osmose.biomass[,"Hake"],
  Shrimp.biomass = osmose.biomass[,"Shrimp"],
  # Landings
  AtlanticHerring.landings = osmose.landings[,"AtlanticHerring"],
  AtlanticMackerel.landings = osmose.landings[,"AtlanticMackerel"],
  Sandeel.landings = osmose.landings[,"Sandeel"],
  EuropeanSprat.landings = osmose.landings[,"EuropeanSprat"],
  NorwayPout.landings = osmose.landings[,"NorwayPout"],
  EuropeanPlaice.landings = osmose.landings[,"EuropeanPlaice"],
  CommonSole.landings = osmose.landings[,"CommonSole"],
  Saithe.landings = osmose.landings[,"Saithe"],
  AtlanticCod.landings = osmose.landings[,"AtlanticCod"],
  Haddock.landings = osmose.landings[,"Haddock"],
  HorseMackerel.landings = osmose.landings[,"HorseMackerel"],
  Whiting.landings = osmose.landings[,"Whiting"],
  CommonDab.landings = osmose.landings[,"CommonDab"],
  GreyGurnard.landings = osmose.landings[,"GreyGurnard"],
  Hake.landings = osmose.landings[,"Hake"],
  Shrimp.landings = osmose.landings[,"Shrimp"],
  
  #growthpot
  AtlanticHerring.growthpot = osmose.growthpot[,"AtlanticHerring"],
  AtlanticMackerel.growthpot = osmose.growthpot[,"AtlanticMackerel"],
  Sandeel.growthpot = osmose.growthpot[,"Sandeel"],
  EuropeanSprat.growthpot = osmose.growthpot[,"EuropeanSprat"],
  NorwayPout.growthpot = osmose.growthpot[,"NorwayPout"],
  EuropeanPlaice.growthpot = osmose.growthpot[,"EuropeanPlaice"],
  CommonSole.growthpot = osmose.growthpot[,"CommonSole"],
  Saithe.growthpot = osmose.growthpot[,"Saithe"],
  AtlanticCod.growthpot = osmose.growthpot[,"AtlanticCod"],
  Haddock.growthpot = osmose.growthpot[,"Haddock"],
  HorseMackerel.growthpot = osmose.growthpot[,"HorseMackerel"],
  Whiting.growthpot = osmose.growthpot[,"Whiting"],
  CommonDab.growthpot = osmose.growthpot[,"CommonDab"],
  GreyGurnard.growthpot = osmose.growthpot[,"GreyGurnard"],
  Hake.growthpot = osmose.growthpot[,"Hake"],
  Shrimp.growthpot = osmose.growthpot[,"Shrimp"])
  
  return(output)
  
  
   
}
