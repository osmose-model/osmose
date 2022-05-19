# OSMOSE (Object-oriented Simulator of Marine Ecosystems)
# http://www.osmose-model.org
#
# Copyright (C) IRD (Institut de Recherche pour le Développement) 2009-2020
#
# Osmose is a computer program whose purpose is to simulate fish
# populations and their interactions with their biotic and abiotic environment.
# OSMOSE is a spatial, multispecies and individual-based model which assumes
# size-based opportunistic predation based on spatio-temporal co-occurrence
# and size adequacy between a predator and its prey. It represents fish
# individuals grouped into schools, which are characterized by their size,
# weight, age, taxonomy and geographical location, and which undergo major
# processes of fish life cycle (growth, explicit predation, additional and
# starvation mortalities, reproduction and migration) and fishing mortalities
# (Shin and Cury 2001, 2004).
#
# Contributor(s):
# Yunne SHIN (yunne.shin@ird.fr),
# Morgane TRAVERS (morgane.travers@ifremer.fr)
# Ricardo OLIVEROS RAMOS (ricardo.oliveros@gmail.com)
# Philippe VERLEY (philippe.verley@ird.fr)
# Laure VELEZ (laure.velez@ird.fr)
# Nicolas Barrier (nicolas.barrier@ird.fr)
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation (version 3 of the License). Full description
# is provided on the LICENSE file.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program.  If not, see <https://www.gnu.org/licenses/>.


################################################################## Test on diet matrix
# Parsing input files -----------------------------------------------------


.getVersion = function(version) {
  
  if(length(version) > 1) stop("Only one 'version' value must be provided.")
  
  # Split string by places where there's dots
  version = unlist(strsplit(x = as.character(version), split = "\\."))
  version = suppressWarnings(as.numeric(version))
  
  # Check if version has a valid form
  if(any(is.na(version)) | length(version) > 3 | any(version < 0, na.rm = TRUE)) {
    stop("Version must be of the form X, X.Y or X.Y.Z")
  }
  
  # Complete version with zeros if its length is less than 3
  version = as.integer(c(version, rep(0, 3 - length(version))))
  
  return(version)
}

# Returns +1 if version1 is greater than version2, -1 if version1
# is less than version2, 0 ib both versions are equal.
.compareVersion = function(version1, version2) {
  
  if(is.character(version1)) version1 = .getVersion(version1)
  if(is.character(version2)) version2 = .getVersion(version2)
  
  # Check numbers of version
  output = as.integer(sign(mapply("-", version1, version2)))
  
  # Return -1, 0, +1 if corresponds
  for(i in seq_along(output)) if(output[i] != 0) return(output[i])
  
  return(0)
}

# guess the type of a vector
.guessType = function(x, keep.att = FALSE){
  if(!is.character(x)) return(x)
  
  att = attributes(x)
  x = stringr::str_trim(string = strsplit(x = x, split = ",")[[1]])
  
  if(identical(tolower(x), "null")) return(NULL) 
  
  asNum = suppressWarnings(as.numeric(x))
  isNumeric = !all(is.na(asNum))
  x[x == "na"] = NA
  out = if(isNumeric) asNum else x  
  
  attributes(out) = NULL
  if(keep.att) attributes(out) = att
  
  return(out) 
}

# get a parameter from a name chain
.getPar = function(x, ..., keep.att=FALSE) {
  
  chain = unlist(list(...))
  if(is.list(x)) 
    x = do.call(.getPar, list(x = x[[chain[1]]], chain[-1]))
  
  return(.guessType(x, keep.att = keep.att))
}

# Get species names. It matches the spX regular expression.
.getSpecies = function(x)  {
  x = names(x)
  x = grep(pattern = "^sp[0-9]*$", x = x, value = TRUE)
  
  return(x)
}

.getBoolean = function(x, default = NULL){
  if(is.null(x)) return(default)
  
  if(length(x) > 1) stop("More than one value provided.")
  
  if(is.logical(x)) return(x)
  
  x = tolower(x)
  if(x == "true") return(TRUE)
  if(x == "false") return(FALSE)
  
  stop(sprintf("Invalid input, %s is not boolean.", x))
}

.readInputCsv = function(file, ...){
  sep = .guessSeparator(readLines(file, n = 1))
  out = read.csv(file, sep = sep, ...)
}

.getFileAsVector = function(file){
  if(is.null(file)) return(NULL)
  path = attr(file, which="path")
  if(!is.null(path)) file=file.path(path, file)
  if(!file.exists(file)) stop(sprintf("File %s not found", file))
  out = .readInputCsv(file = file, row.names = 1)
  out = as.numeric(as.matrix(out))
  return(out)
}


# Parsing OSMOSE outputs --------------------------------------------------

# Groups input files for each species.
# **The name of the configuration must not contain any _ or - characters**
.bySpecies = function(files, sep=c("_", "-")) {
  out = NULL
  if(length(files)>0) {
    sp  = sapply(sapply(files, FUN=.strsplit2v, sep[1],
                        USE.NAMES=FALSE)[2,], FUN=.strsplit2v, sep[2],
                 USE.NAMES=FALSE)[2,]
    out = as.list(tapply(files, INDEX=sp, FUN=identity))
  }
  # change names for all species
  return(out)
}

.strsplit2v = function(...) {
  out = matrix(unlist(strsplit(...)), ncol=1)
  names(out) = NULL
  return(out)
}

.getModelName = function(path) {
  strsplit(dir(path=path, pattern="_biomass_")[1],"_")[[1]][1]
}

# Read Osmose CSV files
.readOsmoseCsv = function(file, sep = ",", skip = 1, row.names = 1, 
                          na.strings = c("NA", "NaN"), ...) {
  out = read.csv(file = file, sep = sep, skip = skip, 
                 row.names = row.names, na.strings = na.strings, 
                 check.names = FALSE, ...)
  
  return(out)
}

.readMortalityCsv = function(file, sep = ",", skip = 1, row.names = 1, 
                             na.strings = c("NA", "NaN"), ...) {
  
  x = readLines(file)
  .subSep = function(x) gsub(";", ",", x)
  
  x = lapply(x, .subSep)
  legend = x[[1]]
  headers = x[2:3]
  x = x[-c(1:3)]
  x = paste(x, collapse="\n")
  x = read.csv(text=x, header=FALSE, na.strings=na.strings)
  times = x[,1]
  x = as.matrix(x[, -c(1,17)])
  x = array(as.numeric(x), dim=c(nrow(x), 3, 5))
  rownames(x) = round(times, 2)
  dimnames(x)[[3]] = c("pred", "starv", "other", "fishing", "out")
  
  return(x)
}

.errorReadingOutputs = function(e) {
  warning(e)
  return(invisible(NULL))
}

.warningReadingOutputs = function(type) {
  e = sprintf("File type '%s' is not recognized by 'read_osmose'", type)
  warning(e)
  return(invisible(NULL))
}


.readFilesList = function(files, path, type, varid=NA, ...) {
  output = tryCatch(
    switch(type,
           abundance       =  .read_1D(files=files, path=path, ...),
           biomass         =  .read_1D(files=files, path=path, ...),
           yield           =  .read_1D(files=files, path=path, ...),
           yieldN          =  .read_1D(files=files, path=path, ...),
           meanTL          =  .read_1D(files=files, path=path, ...),
           meanTLCatch     =  .read_1D(files=files, path=path, ...),
           meanSize        =  .read_1D(files=files, path=path, ...),
           meanSizeCatch   =  .read_1D(files=files, path=path, ...),
           biomassPredPreyIni =  .read_1D(files=files, path=path, ...),
           predatorPressure          = .read_2D(files=files, path=path, ...),
           dietMatrix                = .read_2D(files=files, path=path, ...),
           AgeSpectrumSpeciesB       = .read_2D(files=files, path=path, ...),
           AgeSpectrumSpeciesN       = .read_2D(files=files, path=path, ...),
           AgeSpectrumSpeciesYield   = .read_2D(files=files, path=path, ...),
           AgeSpectrumSpeciesYieldN  = .read_2D(files=files, path=path, ...),
           SizeSpectrum              = .read_2D(files=files, path=path, ...),
           SizeSpectrumSpeciesB      = .read_2D(files=files, path=path, ...),
           SizeSpectrumSpeciesN      = .read_2D(files=files, path=path, ...),
           SizeSpectrumSpeciesYield  = .read_2D(files=files, path=path, ...),
           SizeSpectrumSpeciesYieldN = .read_2D(files=files, path=path, ...),
           mortalityRate             = .read_MortStage(files=files, path=path, ...),
           
           # fisheries
           yieldByFishery = .read_osmose_ncdf(files=files, path=path, varid=varid, ...),
           
           #bioen
           sizeMature = .read_1D(files=files, path=path, ...),
           ageMature  = .read_1D(files=files, path=path, ...),
           ingestion = .read_1D(files=files, path=path, ...),
           ingestionTot  = .read_1D(files=files, path=path, ...),
           maintenance = .read_1D(files=files, path=path, ...),
           meanEnet = .read_1D(files=files, path=path, ...),
           sizeInf = .read_1D(files=files, path=path, ...),
           kappa = .read_1D(files=files, path=path, ...),
           AbundAge1 = .read_1D(files=files, path=path, ...),
           meanIngestDistribByAge = .read_2D(files=files, path=path),
           meanIngestDistribBySize = .read_2D(files=files, path=path),
           meanKappaDistribByAge = .read_2D(files=files, path=path),
           meanKappaDistribBySize = .read_2D(files=files, path=path),
           meanEnetDistribByAge = .read_2D(files=files, path=path),
           meanEnetDistribBySize = .read_2D(files=files, path=path),
           meanMaintDistribByAge = .read_2D(files=files, path=path),
           meanMaintDistribBySize = .read_2D(files=files, path=path),
           
           
           
           # osmose 3r1
           #            mortalityRateByAge = .read_MortStagebyAgeorSize(files=files, path=path, ...),
           #            mortalityRateBySize = .read_MortStagebyAgeorSize(files=files, path=path, ...),
           mortalityRateDistribByAge      = .read_2D(files=files, path=path, ...),
           mortalityRateDistribBySize     = .read_2D(files=files, path=path, ...),
           abundanceDistribBySize         = .read_2D(files=files, path=path, ...),
           biomassDistribBySize            = .read_2D(files=files, path=path, ...),
           naturalMortalityDistribBySize  = .read_2D(files=files, path=path, ...),
           naturalMortalityNDistribBySize = .read_2D(files=files, path=path, ...),
           yieldDistribBySize             = .read_2D(files=files, path=path, ...),
           yieldNDistribBySize            = .read_2D(files=files, path=path, ...),
           abundanceDistribByAge          = .read_2D(files=files, path=path, ...),
           biomassDistribByAge            = .read_2D(files=files, path=path, ...),
           meanSizeDistribByAge           = .read_2D(files=files, path=path, ...),
           naturalMortalityDistribByAge   = .read_2D(files=files, path=path, ...),
           naturalMortalityNDistribByAge  = .read_2D(files=files, path=path, ...),
           yieldDistribByAge              = .read_2D(files=files, path=path, ...),
           yieldNDistribByAge             = .read_2D(files=files, path=path, ...),
           biomassDistribByTL             = .read_2D(files=files, path=path, ...),
           #            dietMatrixbyAge                = .read_2D_ByAgeorSize(files=files, path=path, ...),
           #            dietMatrixbySize               = .read_2D_ByAgeorSize(files=files, path=path, ...),
           dietMatrixbyAge                = .read_2D(files=files, path=path, ...),
           dietMatrixbySize               = .read_2D(files=files, path=path, ...),
           meanTLDistribByAge             = .read_2D(files=files, path=path, ...),
           meanTLDistribBySize            = .read_2D(files=files, path=path, ...),
           predatorPressureDistribByAge   = .read_2D(files=files, path=path, ...),
           predatorPressureDistribBySize  = .read_2D(files=files, path=path, ...),
           abundanceDistribByTL            = .read_2D(files=files, path=path, ...),
           
           # outputs for calibration of the Osmose model.
           meanWeightDistribByAge = .read_2D(files=files, path=path, ...),
           meanWeightDistribBySize = .read_2D(files=files, path=path, ...),
           meanWeightDistribByWeight = .read_2D(files=files, path=path, ...),
      
           biomassDistribByWeight         = .read_2D(files=files, path=path, ...),     
           abundanceDistribByWeight         = .read_2D(files=files, path=path, ...),
           yieldDistribByWeight         = .read_2D(files=files, path=path, ...),
           yieldNDistribByWeight         = .read_2D(files=files, path=path, ...),
           
           .warningReadingOutputs(type)), 
    error = .errorReadingOutputs)
  
  return(output)
}

# Generic function to read 1D output files (abundance, biomass, etc).
# If no file is found, it returns NULL.
#
# @param files List of input files
# @param path  Data directory
# @param ... Additional arguments of the \code{\link{.readOsmoseCsv}} function.
#
# @return A 3D array (time, species, replicates) or NULL if no file is found.
.read_1D = function(files, path, ...) {
  # TO_DO: change for the unified approach! species as list
  if(length(files)!=0) {
    x = .readOsmoseCsv(file.path(path, files[1]), ...)
    species = names(x)
    times   = rownames(x)
    
    output = array(dim=c(dim(x),length(files)))
    output[,,1] = as.matrix(x)
    if(length(files)>1) {
      for(i in seq_along(files[-1])) {
        x = .readOsmoseCsv(file.path(path, files[i+1]), ...)
        output[,,i+1]= as.matrix(x)
      }
    }
    rownames(output) = times
    colnames(output) = species
  } else {
    output = NULL
  }
  
  return(output)
}

#' Generic function to read 2D output files (dietMatrix, sizeSpectrum, etc).
#' If no file is found, it returns NULL.
#'
#' @param files List of input files
#' @param path  Data directory
#' @param ... Extra arguments passed to the method.
#'
#' @return A 3D array (time, species, replicates) or NULL if no file is found.
.read_2D = function(files, path, ...) {
  
  if(length(files)!=0) {
    
    x = .readOsmoseCsv(file.path(path, files[1]), row.names=NULL, ...)
    
    rows    = unique(x[,1])
    cols    = unique(x[,2])
    slices  = names(x)[-(1:2)]
    
    x = .reshapeOsmoseTable(x)
    
    out = array(dim = c(dim(x), length(files)))
    
    out[, , , 1] = x
    
    if(length(files)>1) {
      for(i in seq_along(files[-1])) {
        x = .readOsmoseCsv(file.path(path, files[i+1]), row.names=NULL, ...)
        x = .reshapeOsmoseTable(x)
        out[, , , i+1]= x
      }
    }
    
    out = aperm(out, c(1,2,4,3))
    
    rownames(out) = rows
    colnames(out) = cols
    
    nsp = dim(out)[4]
    
    output=list()
    
    for(i in seq_len(nsp)) {
      y = out[, , , i, drop=FALSE]
      dnames = dimnames(y)[1:3]
      dim(y) = dim(y)[-length(dim(y))]
      dimnames(y) = dnames
      output[[i]] = drop(y)
    }
    
    names(output) = slices
    
  } else {
    output = NULL
  }
  
  return(output)
}

.read_MortStage = function(files, path, ...) {
  
  if(length(files)!=0) {
    
    x = .readMortalityCsv(file.path(path, files[1]), row.names=NULL, ...)
    
    rows = row.names(x)
    cols = c("pred", "starv", "other", "fishing", "out")
    
    out = array(dim = c(dim(x), length(files)))
    
    out[, , , 1] = x
    
    if(length(files)>1) {
      for(i in seq_along(files[-1])) {
        x = .readMortalityCsv(file.path(path, files[i+1]), row.names=NULL, ...)
        out[, , , i+1]= x
      }
    }
    
    rownames(out) = rows
    dimnames(out)[[3]] = cols
    
    output=list()
    
    # remove the dropping of dimensions
    # warning, the dimension will be ntime, 1, 5, replicate.
    output$eggs      = out[, 1, , ]
    output$juveniles = out[, 2, , ]
    output$adults    = out[, 3, , ]
    
  } else {
    output = NULL
  }
  
  return(output)
}


#' Function to read osmose netcdf files
#'
#' @param files String of name of the file that will be read.
#' @param path String of path of the file that will be read
#' @param ... Extra arguments 
#'
.read_osmose_ncdf = function(files, path, varid, ...) {
  
  species_names = NULL
  
  if(length(files)!=0) {
    
    nc = nc_open(file.path(path, files[1]))
    x = ncvar_get(nc, varid=varid) # assumes only one variable in the file
    att = ncatt_get(nc, varid, attname="species_names")
    if(att$hasatt) {
      species_names = att$value
      species_names = unlist(strsplit(species_names, split=", "))
    } else species_names = NULL
    
    nc_close(nc)
    
    output = array(dim = c(dim(x), length(files)))
    
    output[, , , 1] = x
    
    if(length(files)>1) {
      for(i in seq_along(files[-1])) {
        nc = nc_open(file.path(path, files[i+1]))
        x = ncvar_get(nc, varid=varid) # assumes only one variable in the file
        nc_close(nc)
        output[, , , i+1]= x
      }
    }
    
  } else {
    output = NULL
  }

  if(!is.null(species_names)) attr(output, which="species_names") = species_names
  
  return(output)
}


.reshapeOsmoseTable = function(x) {
  
  rows    = unique(x[,1])
  cols    = unique(x[,2])
  slices  = names(x)[-(1:2)]
  
  x       = as.matrix(x[,-(1:2)])
  dim(x)  = c(length(cols), length(rows), length(slices))
  x       = aperm(x, c(2,1,3))
  
  dimnames(x) = list(rows, cols, slices)
  
  return(x)
}

.rewriteOutputs = function(path) {
  dir.create(file.path(path, "osmose2R"))
  # not finished
}

.countOnes = function(files, ...) {
  
  out = numeric(length(files))
  
  for(i in seq_along(files)) {
    
    x = read.csv(files[i], header=FALSE, ...)
    out[i] = sum(x>0, na.rm=TRUE)
    
  }
  
  out = c(min=min(out), mean=mean(out), 
          median=median(out), max=max(out))
  return(out)
  
}


.removeZeros = function(object) {
  remove = apply(object, 2, function(x) all(x==0))
  object = object[ , !remove, ]
  return(object)
}


.niceSeq = function(x, zero=FALSE) {
  ncode = floor(log10(x)) + 1 
  out = sprintf(paste0("%0", ncode, "d"), seq_len(x) - zero)
  return(out)
}

# read_osmose old ---------------------------------------------------------

# Read Osmose (version 4 release 0) outputs
#
# @param path Osmose output path
# @param species.names Array of species names. If
# NULL, it is extracted from file.
#
# @return A list of list containing the output functions
#
osmose2R.v4r0 = function (path=NULL, species.names=NULL, conf=NULL, ...) {
  
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
                    dietMatrixBySize = readOsmoseFiles(path = path, type = "dietMatrixDistribBySize", bySpecies = TRUE),  
                    predatorPressureBySize = readOsmoseFiles(path = path, type = "predatorPressureDistribBySize", bySpecies = TRUE),
                    abundanceByAge = readOsmoseFiles(path = path, type = "abundanceDistribByAge"),  
                    biomassByAge = readOsmoseFiles(path = path, type = "biomassDistribByAge"),  
                    meanSizeByAge = readOsmoseFiles(path = path, type = "meanSizeDistribByAge"), 
                    meanTLByAge = readOsmoseFiles(path = path, type = "meanTLDistribByAge"),  
                    mortalityByAge = readOsmoseFiles(path = path, type = "mortalityRateDistribByAge", bySpecies = TRUE),
                    dietMatrixByAge = readOsmoseFiles(path = path, type = "dietMatrixDistribByAge", bySpecies = TRUE),  
                    predatorPressureByAge = readOsmoseFiles(path = path, type = "predatorPressureDistribByAge", bySpecies = TRUE), 
                    abundanceByTL = readOsmoseFiles(path = path, type = "abundanceDistribByTL"),
                    
                    # Fisheries outputs
                    yieldByFishery = readOsmoseFiles(path = path, type = "yieldByFishery", varid="landings", ext="nc"),
                    yield = readOsmoseFiles(path = path, type = "yield"), 
                    yieldN = readOsmoseFiles(path = path, type = "yieldN"), 
                    yieldBySize = readOsmoseFiles(path = path, type = "yieldDistribBySize"),  
                    yieldNBySize = readOsmoseFiles(path = path, type = "yieldNDistribBySize"),  
                    yieldByAge = readOsmoseFiles(path = path, type = "yieldDistribByAge"),  
                    yieldNByAge = readOsmoseFiles(path = path, type = "yieldNDistribByAge"),  
                    discards = readOsmoseFiles(path = path, type = "yieldByFishery", varid="discards", ext="nc"),
                    
                    # survey outputs
                    surveyBiomass = readOsmoseFiles(path = path, type = "biomass", bySpecies = TRUE),  
                    surveyAbundance = readOsmoseFiles(path = path, type = "abundance", bySpecies = TRUE),  
                    surveyYield = readOsmoseFiles(path = path, type = "yield", bySpecies = TRUE),
                    
                    # bioen variables
                    sizeMature = readOsmoseFiles(path = path, type = "sizeMature"),
                    ageMature = readOsmoseFiles(path = path, type = "ageMature"),
                    ingestion = readOsmoseFiles(path = path, type = "ingestion"),
                    ingestionTot = readOsmoseFiles(path = path, type = "ingestionTot"),
                    maintenance = readOsmoseFiles(path = path, type = "maintenance"),
                    meanEnet = readOsmoseFiles(path=path, type="meanEnet"),
                    sizeInf = readOsmoseFiles(path = path, type = "sizeInf"),
                    kappa = readOsmoseFiles(path = path, type = "kappa"),
                    abundAge1 = readOsmoseFiles(path=path, type="AbundAge1"),
                    ingestByAge = readOsmoseFiles(path=path, type="meanIngestDistribByAge"),
                    ingestBySize = readOsmoseFiles(path=path, type="meanIngestDistribBySize"),
                    kappaByAge = readOsmoseFiles(path=path, type="meanKappaDistribByAge"),
                    kappaBySize = readOsmoseFiles(path=path, type="meanKappaDistribBySize"),
                    enetByAge = readOsmoseFiles(path=path, type="meanEnetDistribByAge"),
                    enetBySize = readOsmoseFiles(path=path, type="meanEnetDistribBySize"),
                    maintenanceByAge = readOsmoseFiles(path=path, type="meanMaintDistribByAge"),
                    maintenanceBySize = readOsmoseFiles(path=path, type="meanMaintDistribBySize"),
                    
                    # outputs used for the calibration of the economic model
                    meanWeightByAge = readOsmoseFiles(path=path, type="meanWeightDistribByAge"),
                    meanWeightBySize = readOsmoseFiles(path=path, type="meanWeightDistribBySize"),
                    meanWeightByWeight = readOsmoseFiles(path=path, type="meanWeightDistribByWeight"),
                    
                    yieldByWeight = readOsmoseFiles(path = path, type = "yieldDistribByWeight"),  
                    yieldNByWeight = readOsmoseFiles(path = path, type = "yieldNDistribByWeight"),  
                    abundanceByWeight = readOsmoseFiles(path = path, type = "abundanceDistribByWeight"),
                    biomassByWeight = readOsmoseFiles(path = path, type = "biomassDistribByWeight")
                    
)
  
  if(!is.null(outputData$yieldByFishery)) {
    # temporal
    dmn = dimnames(outputData$biomass)
    rf = .getPar(conf, "output.recordfrequency.ndt")
    outputData$yieldByFishery = .reshapeFishery(outputData$yieldByFishery, nm=dmn, rf=rf)
    nm = sprintf("fishery%d", seq_along(outputData$yieldByFishery) - 1)
    if(!is.null(conf)) nm = unlist(.getPar(conf, "fisheries.name"))
    names(outputData$yieldByFishery) = nm
    # end of temporal
  }

  outputData = .add_surveys(x=outputData$surveyBiomass, out=outputData, type="biomass")
  outputData = .add_surveys(x=outputData$surveyAbundance, out=outputData, type="abundance")
  outputData = .add_surveys(x=outputData$surveyYield, out=outputData, type="yield")
  outputData = .add_surveys(x=outputData$yieldByFishery, out=outputData, type="yield")
  
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
                    SizeSpectrumN = readOsmoseFiles(path=path, type="SizeSpectrumSpeciesN"),
                    SizeSpectrumB = readOsmoseFiles(path=path, type="SizeSpectrumSpeciesB"),
                    SizeSpectrumC = readOsmoseFiles(path=path, type="SizeSpectrumSpeciesYield"),
                    SizeSpectrumY = readOsmoseFiles(path=path, type="SizeSpectrumSpeciesYieldN"),
                    abundanceBySize = readOsmoseFiles(path = path, type = "abundanceDistribBySize"), 
                    biomassBySize = readOsmoseFiles(path = path, type = "biomassDistribBySize"),   
                    yieldBySize = readOsmoseFiles(path = path, type = "yieldDistribBySize"),  
                    yieldNBySize = readOsmoseFiles(path = path, type = "yieldNDistribBySize"),  
                    meanTLBySize = readOsmoseFiles(path = path, type = "meanTLDistribBySize"),  
                    mortalityBySize = readOsmoseFiles(path = path, type = "mortalityRateDistribBySize", bySpecies = TRUE),  
                    dietMatrixBySize = readOsmoseFiles(path = path, type = "dietMatrixDistribBySize", bySpecies = TRUE),  
                    predatorPressureBySize = readOsmoseFiles(path = path, type = "predatorPressureDistribBySize", bySpecies = TRUE),
                    abundanceByAge = readOsmoseFiles(path = path, type = "abundanceDistribByAge"),  
                    biomassByAge = readOsmoseFiles(path = path, type = "biomassDistribByAge"),  
                    yieldByAge = readOsmoseFiles(path = path, type = "yieldDistribByAge"),  
                    yieldNByAge = readOsmoseFiles(path = path, type = "yieldNDistribByAge"),  
                    meanSizeByAge = readOsmoseFiles(path = path, type = "meanSizeDistribByAge"), 
                    meanTLByAge = readOsmoseFiles(path = path, type = "meanTLDistribByAge"),  
                    mortalityByAge = readOsmoseFiles(path = path, type = "mortalityRateDistribByAge", bySpecies = TRUE),
                    dietMatrixByAge = readOsmoseFiles(path = path, type = "dietMatrixDistribByAge", bySpecies = TRUE),  
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
  
  # remove NULL elements fom the list
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
                    TL   = readOsmoseFiles(path=path, type="TL"),
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


#' Reads Osmose configuration files.
#' 
#' @param file Main configuration file
#' @param config Configuration object to which file parameters are appended
#' @param absolute Whether the path is absolute (TRUE) or relative (FALSE)
#' @return A list tree.
readOsmoseConfiguration = function(file, config=NULL, absolute=TRUE) {
  
  L0 = .readOsmoseConfiguration(input=file, absolute=absolute)
  
  if(!is.null(config)) {
    config = .getConfig(config)
    L0 = c(config, L0)
    L0 = L0[!duplicated(names(L0))]
  }
  
  L1 = .createParameterList(L0)
  class(L1) = c("osmose.config", class(L1))
  
  return(L1)
}



#' Reads calibration parameters from an osmose.config list.
#' 
#' The configuration argument must contain a "calibration" entry to work.
#'
#' @param L1 osmose.config object (see \code{\link{readOsmoseConfiguration}})
#'
#' @return A list of parameters to calibrate ("guess", "max", "min", "phase")
configureCalibration = function(L1) {
  
  nameCal  = names(unlist(L1$calibration))
  valueCal = unname(unlist(L1$calibration))
  
  #guess List
  guessList = .createCalibrationList(nameCal, valueCal, "\\.max|\\.min|\\.phase", TRUE)
  
  #max List
  maxList   = .createCalibrationList(nameCal, valueCal, "\\.max", FALSE)
  
  #phase List
  phaseList = .createCalibrationList(nameCal, valueCal, "\\.phase", FALSE)
  
  #min List
  minList   = .createCalibrationList(nameCal, valueCal, "\\.min", FALSE)
  
  L2 = list(guess=guessList, max=maxList, min=minList, phase=phaseList)
  
  return(L2)
  
}

.add_surveys = function(x, out, type) {
  if(is.null(x)) return(out)
  if(length(x) == 0) return(out)
  names(x) = paste(type, names(x), sep=".")
  for(i in seq_along(x)) class(x[[i]]) = c(sprintf("osmose.%s", type), class(x[[i]]))
  out = c(out, x)
  return(out)
}

.reshapeFishery = function(x, nm, rf) {
  
  if(is.null(x)) return(x)

  nm = attr(x, "species_names")
  
  .agg = function(x, rf) {
    ind = rep(seq_len(nrow(x)), each=rf, length.out=nrow(x))
    xy = apply(x, 2:3, FUN=rowsum, group=ind)
    return(xy)
  }
  
  out = lapply(1:dim(x)[1], FUN = function(i, x) x[i,,,], x=x)
  .addRep = function(x) {
    if(length(dim(x))==2) dim(x) = c(dim(x), 1)
    return(x)
  }
  out = lapply(out, FUN=.addRep)
  out = lapply(out, FUN=aperm, perm=c(2,1,3))
  out = lapply(out, FUN=.agg, rf=rf)
  
  # if(!is.null(species_names)) {
  #   for(i in seq_along(out)) dimnames(out[[i]][[2]]) = nm
  # }
  
  return(out)
}
