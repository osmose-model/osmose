
# read_osmose old ---------------------------------------------------------

# Read Osmose (version 4 release 0) outputs
#
# @param path Osmose output path
# @param species.names Array of species names. If
# NULL, it is extracted from file.
#
# @return A list of list containing the output functions
#
osmose2R.v4r0 = function (path=NULL, species.names=NULL) {
  
  # Output data
  outputData = list(biomass = readOsmoseFiles(path = path, type = "biomass"),  
                    abundance = readOsmoseFiles(path = path, type = "abundance"),  
                    mortality = readOsmoseFiles(path = path, type = "mortalityRate", bySpecies = TRUE), 
                    meanTL = readOsmoseFiles(path = path, type = "meanTL"),  
                    meanTLCatch = readOsmoseFiles(path = path, type = "meanTLCatch"), 
                    biomassByTL = readOsmoseFiles(path = path, type = "biomassDistribByTL"),  
                    predatorPressure = readOsmoseFiles(path = path, type = "predatorPressure"),  
                    predPreyIni = readOsmoseFiles(path = path, type = "biomassPredPreyIni"),
                    dietMatrix = readOsmoseFiles(path = path, type = "dietMatrix"),  
                    meanSize = readOsmoseFiles(path = path, type = "meanSize"),  
                    meanSizeCatch = readOsmoseFiles(path = path, type = "meanSizeCatch"),  
                    SizeSpectrum  = readOsmoseFiles(path=path, type="SizeSpectrum")$Abundance,
                    abundanceBySize = readOsmoseFiles(path = path, type = "abundanceDistribBySize"), 
                    biomassBySize = readOsmoseFiles(path = path, type = "biomassDistribBySize"),   
                    meanTLBySize = readOsmoseFiles(path = path, type = "meanTLDistribBySize"),  
                    mortalityBySize = readOsmoseFiles(path = path, type = "mortalityRateDistribBySize", bySpecies = TRUE),  
                    dietMatrixBySize = readOsmoseFiles(path = path, type = "dietMatrixbySize", bySpecies = TRUE),  
                    predatorPressureBySize = readOsmoseFiles(path = path, type = "predatorPressureDistribBySize", bySpecies = TRUE),
                    abundanceByAge = readOsmoseFiles(path = path, type = "abundanceDistribByAge"),  
                    biomassByAge = readOsmoseFiles(path = path, type = "biomassDistribByAge"),  
                    meanSizeByAge = readOsmoseFiles(path = path, type = "meanSizeDistribByAge"), 
                    meanTLByAge = readOsmoseFiles(path = path, type = "meanTLDistribByAge"),  
                    mortalityByAge = readOsmoseFiles(path = path, type = "mortalityRateDistribByAge", bySpecies = TRUE),
                    dietMatrixByAge = readOsmoseFiles(path = path, type = "dietMatrixbyAge", bySpecies = TRUE),  
                    predatorPressureByAge = readOsmoseFiles(path = path, type = "predatorPressureDistribByAge", bySpecies = TRUE), 
                    abundanceByTL = readOsmoseFiles(path = path, type = "abundanceDistribByTL"),  
                    
                    # Fisheries outputs
                    
                    yieldByFishery = readOsmoseFiles(path = path, type = "fisheriesOutput", ext="nc"),
                    yield = readOsmoseFiles(path = path, type = "yield"), 
                    yieldN = readOsmoseFiles(path = path, type = "yieldN"), 
                    yieldBySize = readOsmoseFiles(path = path, type = "yieldDistribBySize"),  
                    yieldNBySize = readOsmoseFiles(path = path, type = "yieldNDistribBySize"),  
                    yieldByAge = readOsmoseFiles(path = path, type = "yieldDistribByAge"),  
                    yieldNByAge = readOsmoseFiles(path = path, type = "yieldNDistribByAge"),  
                    
                    
                    # bioen variables
                    ageMature = readOsmoseFiles(path = path, type = "AgeMature"),
                    growthPotential = readOsmoseFiles(path = path, type = "growthpot"),
                    ingestion = readOsmoseFiles(path = path, type = "ingestion"),
                    maintenance = readOsmoseFiles(path = path, type = "maint"),
                    sizeInf = readOsmoseFiles(path = path, type = "SizeInf"),
                    sizeMature = readOsmoseFiles(path = path, type = "SizeMature")
                    
  )
  
  # temporal
  outputData$yield = aperm(apply(outputData$yieldByFishery, 2:4, sum, na.rm=TRUE), 
                           perm = c(2,1,3))
  rownames(outputData$yield) = seq_len(nrow(outputData$yield)) - 1
  colnames(outputData$yield) = colnames(outputData$biomass)
  
  class(outputData$yield) = "osmose.yield"
  # end of temporal
  
  model = list(version = "4",
               model = .getModelName(path = path),
               simus = dim(outputData$biomass)[3],
               times = as.numeric(row.names(outputData$biomass)),
               T = nrow(outputData$biomass),
               start = as.numeric(row.names(outputData$biomass))[1],
               nsp = ncol(outputData$biomass),
               lspecies = if (!is.null(species.names)) species.names else colnames(outputData$biomass))
  
  output = c(model = list(model), species = list(colnames(outputData$biomass)),
             outputData)
  
  return(output)
}

# Read Osmose (version 3 release 2) outputs
#
# @param path Osmose output path
# @param species.names Array of species names. If
# NULL, it is extracted from file.
#
# @return A list of list containing the output functions
#
osmose2R.v3r2 = function (path=NULL, species.names=NULL) {
  
  # Output data
  outputData = list(biomass = readOsmoseFiles(path = path, type = "biomass"),  
                    abundance = readOsmoseFiles(path = path, type = "abundance"),  
                    yield = readOsmoseFiles(path = path, type = "yield"), 
                    yieldN = readOsmoseFiles(path = path, type = "yieldN"), 
                    mortality = readOsmoseFiles(path = path, type = "mortalityRate", bySpecies = TRUE), 
                    meanTL = readOsmoseFiles(path = path, type = "meanTL"),  
                    meanTLCatch = readOsmoseFiles(path = path, type = "meanTLCatch"), 
                    biomassByTL = readOsmoseFiles(path = path, type = "biomassDistribByTL"),  
                    predatorPressure = readOsmoseFiles(path = path, type = "predatorPressure"),  
                    predPreyIni = readOsmoseFiles(path = path, type = "biomassPredPreyIni"),
                    dietMatrix = readOsmoseFiles(path = path, type = "dietMatrix"),  
                    meanSize = readOsmoseFiles(path = path, type = "meanSize"),  
                    meanSizeCatch = readOsmoseFiles(path = path, type = "meanSizeCatch"),  
                    SizeSpectrum  = readOsmoseFiles(path=path, type="SizeSpectrum")$Abundance,
                    abundanceBySize = readOsmoseFiles(path = path, type = "abundanceDistribBySize"), 
                    biomassBySize = readOsmoseFiles(path = path, type = "biomassDistribBySize"),   
                    yieldBySize = readOsmoseFiles(path = path, type = "yieldDistribBySize"),  
                    yieldNBySize = readOsmoseFiles(path = path, type = "yieldNDistribBySize"),  
                    meanTLBySize = readOsmoseFiles(path = path, type = "meanTLDistribBySize"),  
                    mortalityBySize = readOsmoseFiles(path = path, type = "mortalityRateDistribBySize", bySpecies = TRUE),  
                    dietMatrixBySize = readOsmoseFiles(path = path, type = "dietMatrixbySize", bySpecies = TRUE),  
                    predatorPressureBySize = readOsmoseFiles(path = path, type = "predatorPressureDistribBySize", bySpecies = TRUE),
                    abundanceByAge = readOsmoseFiles(path = path, type = "abundanceDistribByAge"),  
                    biomassByAge = readOsmoseFiles(path = path, type = "biomassDistribByAge"),  
                    yieldByAge = readOsmoseFiles(path = path, type = "yieldDistribByAge"),  
                    yieldNByAge = readOsmoseFiles(path = path, type = "yieldNDistribByAge"),  
                    meanSizeByAge = readOsmoseFiles(path = path, type = "meanSizeDistribByAge"), 
                    meanTLByAge = readOsmoseFiles(path = path, type = "meanTLDistribByAge"),  
                    mortalityByAge = readOsmoseFiles(path = path, type = "mortalityRateDistribByAge", bySpecies = TRUE),
                    dietMatrixByAge = readOsmoseFiles(path = path, type = "dietMatrixbyAge", bySpecies = TRUE),  
                    predatorPressureByAge = readOsmoseFiles(path = path, type = "predatorPressureDistribByAge", bySpecies = TRUE), 
                    abundanceByTL = readOsmoseFiles(path = path, type = "abundanceDistribByTL"),  
                    
                    # bioen variables
                    ageMature = readOsmoseFiles(path = path, type = "AgeMature"),
                    growthPotential = readOsmoseFiles(path = path, type = "growthpot"),
                    ingestion = readOsmoseFiles(path = path, type = "ingestion"),
                    maintenance = readOsmoseFiles(path = path, type = "maint"),
                    sizeInf = readOsmoseFiles(path = path, type = "SizeInf"),
                    sizeMature = readOsmoseFiles(path = path, type = "SizeMature")
                    
  )
  
  model = list(version = "3u2",
               model = .getModelName(path = path),
               simus = dim(outputData$biomass)[3],
               times = as.numeric(row.names(outputData$biomass)),
               T = nrow(outputData$biomass),
               start = as.numeric(row.names(outputData$biomass))[1],
               nsp = ncol(outputData$biomass),
               lspecies = if (!is.null(species.names)) species.names else colnames(outputData$biomass))
  
  output = c(model = list(model), species = list(colnames(outputData$biomass)),
             outputData)
  
  # barrier.n: remove NULL elements fom the list
  cond = sapply(output, is.null)
  output = output[!cond]
  
  return(output)
}


osmose2R.v3r1 = function(path=NULL, species.names=NULL, ...) {
  
  # Output data
  outputData = list(biomass    = readOsmoseFiles(path=path, type="biomass"),
                    abundance  = readOsmoseFiles(path=path, type="abundance"),
                    yield      = readOsmoseFiles(path=path, type="yield"),
                    catch      = readOsmoseFiles(path=path, type="yieldN"),
                    mortality  = readOsmoseFiles(path=path, type="mortalityRate", bySpecies=TRUE),
                    dietMatrix  = readOsmoseFiles(path=path, type="dietMatrix"),
                    meanTL      = readOsmoseFiles(path=path, type="meanTL"),
                    meanTLCatch = readOsmoseFiles(path=path, type="meanTLCatch"),
                    predatorPressure = readOsmoseFiles(path=path, type="predatorPressure"),
                    predPreyIni = readOsmoseFiles(path=path, type="biomassPredPreyIni"),
                    TLDistrib   = readOsmoseFiles(path=path, type="TLDistrib"),
                    meanSize      = readOsmoseFiles(path=path, type="meanSize"),
                    meanSizeCatch = readOsmoseFiles(path=path, type="meanSizeCatch"),
                    SizeSpectrum  = readOsmoseFiles(path=path, type="SizeSpectrum")$Abundance,
                    SizeSpectrumN = readOsmoseFiles(path=path, type="SizeSpectrumSpeciesN"),
                    SizeSpectrumB = readOsmoseFiles(path=path, type="SizeSpectrumSpeciesB"),
                    SizeSpectrumC = readOsmoseFiles(path=path, type="SizeSpectrumSpeciesYield"),
                    SizeSpectrumY = readOsmoseFiles(path=path, type="SizeSpectrumSpeciesYieldN"),
                    AgeSpectrumN = readOsmoseFiles(path=path, type="AgeSpectrumSpeciesN"),
                    AgeSpectrumB = readOsmoseFiles(path=path, type="AgeSpectrumSpeciesB"),
                    AgeSpectrumC = readOsmoseFiles(path=path, type="AgeSpectrumSpeciesYield"),
                    AgeSpectrumY = readOsmoseFiles(path=path, type="AgeSpectrumSpeciesYieldN"))
  
  model = list(version  = "3u1",
               model    = .getModelName(path=path),
               simus    = dim(outputData$biomass)[3],
               times    = as.numeric(row.names(outputData$biomass)),
               T        = nrow(outputData$biomass),
               start    = as.numeric(row.names(outputData$biomass))[1],
               nsp      = ncol(outputData$biomass),
               lspecies = if(!is.null(species.names)) species.names else colnames(outputData$biomass))
  
  output = c(model = list(model), species = list(colnames(outputData$biomass)), 
             outputData)
  
  return(output)
  
}


osmose2R.v3r0 = function(path=NULL, species.names=NULL, ...) {
  
  # Output data
  outputData = list(biomass    = readOsmoseFiles(path=path, type="biomass"),
                    abundance  = readOsmoseFiles(path=path, type="abundance"),
                    yield      = readOsmoseFiles(path=path, type="yield"),
                    catch      = readOsmoseFiles(path=path, type="yieldN"),
                    mortality  = readOsmoseFiles(path=path, type="mortalityRate", bySpecies=TRUE),
                    meanTL      = readOsmoseFiles(path=path, type="meanTL"),
                    meanTLCatch = readOsmoseFiles(path=path, type="meanTLCatch"),
                    predatorPressure = readOsmoseFiles(path=path, type="predatorPressure"),
                    predPreyIni = readOsmoseFiles(path=path, type="biomassPredPreyIni"),
                    meanSize      = readOsmoseFiles(path=path, type="meanSize"),
                    meanSizeCatch = readOsmoseFiles(path=path, type="meanSizeCatch"),
                    SizeSpectrum  = readOsmoseFiles(path=path, type="SizeSpectrum")$Abundance,
                    SizeSpectrumN = readOsmoseFiles(path=path, type="SizeSpectrumSpeciesN"),
                    SizeSpectrumB = readOsmoseFiles(path=path, type="SizeSpectrumSpeciesB"),
                    SizeSpectrumC = readOsmoseFiles(path=path, type="SizeSpectrumSpeciesYield"),
                    SizeSpectrumY = readOsmoseFiles(path=path, type="SizeSpectrumSpeciesYieldN"),
                    AgeSpectrumN = readOsmoseFiles(path=path, type="AgeSpectrumSpeciesN"),
                    AgeSpectrumB = readOsmoseFiles(path=path, type="AgeSpectrumSpeciesB"),
                    AgeSpectrumC = readOsmoseFiles(path=path, type="AgeSpectrumSpeciesYield"),
                    AgeSpectrumY = readOsmoseFiles(path=path, type="AgeSpectrumSpeciesYieldN"))
  
  model = list(version  = "3.0b",
               model    = .getModelName(path=path),
               simus    = dim(outputData$biomass)[3],
               times    = as.numeric(row.names(outputData$biomass)),
               T        = nrow(outputData$biomass),
               start    = as.numeric(row.names(outputData$biomass))[1],
               nsp      = ncol(outputData$biomass),
               lspecies = if(!is.null(species.names)) species.names else colnames(outputData$biomass))
  
  output = c(model = list(model), species = list(colnames(outputData$biomass)),
             outputData)
  
  return(output)
  
}
