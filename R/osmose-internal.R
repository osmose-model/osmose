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
# Nicolas BARRIER (nicolas.barrier@ird.fr)
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
  if(any(is.na(version)) | length(version) > 4 | any(version < 0, na.rm = TRUE)) {
    stop("Version must be of the form X, X.Y, X.Y.Z, X.Y.Z.W")
  }

  # Complete version with zeros if its length is less than 3
  version = as.integer(c(version, rep(0, 4 - length(version))))

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
# .getPar = function(x, ..., keep.att=FALSE) {
#
#   chain = unlist(list(...))
#   if(is.list(x))
#     x = do.call(.getPar, list(x = x[[chain[1]]], chain[-1]))
#
#   return(.guessType(x, keep.att = keep.att))
# }

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
    sp  = lapply(sapply(files, FUN=.strsplit2v, sep[1],
                        USE.NAMES=FALSE)[2,], FUN=.strsplit2v, sep[2])
    ind = sapply(sp, length)!=2
    sp[ind] = NULL
    sp = do.call(cbind, sp)[2,]
    out = as.list(tapply(files[!ind], INDEX=sp, FUN=identity))
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
  out = try(read.csv(file = file, sep = sep, skip = skip,
                 row.names = row.names, na.strings = na.strings,
                 check.names = FALSE, ...), silent=TRUE)
  if(inherits(out, "try-error")) return(NULL)
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
           yieldByFisheryBySpecies = .read_osmose_ncdf(files=files, path=path, varid=varid, ...),

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
  if(length(files)==0) return(NULL)
  
  x = .readOsmoseCsv(file.path(path, files[1]), ...)
  if(is.null(x)) return(NULL)
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
  names(dimnames(output)) = c("time", "species", "replicates")
  
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
    if(is.null(x)) return(NULL)

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
      names(dimnames(y)) = c("time", "species", "replicates")
      output[[i]] = y #drop(y) no drop!
    }

    output = lapply(output, FUN = .trim_matrix)

    names(output) = slices


  } else {
    output = NULL
  }

  return(output)
}

.read_MortStage = function(files, path, ...) {

  if(length(files)!=0) {

    x = .readMortalityCsv(file.path(path, files[1]), row.names=NULL, ...)
    if(is.null(x)) return(NULL)

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
.read_osmose_ncdf = function(files, path, varid, calendar=360, ...) {

  species_names = NULL

  if(length(files)!=0) {

    nc = try(nc_open(file.path(path, files[1])))
    if(inherits(nc, "try-error")) return(NULL)

    time = ncvar_get(nc, varid="time")/calendar
    x = ncvar_get(nc, varid=varid) 
    sp_att = ncatt_get(nc, varid, attname="species_names")
    fsh_att = ncatt_get(nc, varid, attname="fisheries_names")
    if(sp_att$hasatt) {
      species_names = unlist(strsplit(sp_att$value, split=", "))
    } else species_names = NULL
    if(fsh_att$hasatt) {
      fishery_names = unlist(strsplit(fsh_att$value, split=", "))
    } else fishery_names = NULL
    nc_close(nc)

    x = aperm(x, 3:1)
    output = array(dim = c(dim(x), length(files)))

    output[, , , 1] = x

    if(length(files)>1) {
      for(i in seq_along(files[-1])) {
        nc = nc_open(file.path(path, files[i+1]))
        x = ncvar_get(nc, varid=varid) # assumes only one variable in the file
        nc_close(nc)
        output[, , , i+1] = aperm(x, 3:1)
      }
    }

    dimnames(output) = list(time=time,
                            species=species_names,
                            fishery=fishery_names,
                            replicates=seq_along(files))
  } else {
    output = NULL
  }

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


.trim_matrix = function(x) {
  ind = apply(x, 2, FUN=function(x) !all(is.na(x)))
  if(all(ind)) return(x)       # all TRUE
  if(all(!ind)) return(NULL)   # all FALSE
  if(tail(ind, 1)) return(x)   # ends in TRUE
  xind = rle(ind)
  xind$values[-length(xind$values)] = TRUE
  ind = inverse.rle(xind)
  x = x[,ind,, drop=FALSE]
  return(x)
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
                    yield = readOsmoseFiles(path = path, type = "yield"),
                    yieldN = readOsmoseFiles(path = path, type = "yieldN"),
                    yieldBySize = readOsmoseFiles(path = path, type = "yieldDistribBySize"),
                    yieldNBySize = readOsmoseFiles(path = path, type = "yieldNDistribBySize"),
                    yieldByAge = readOsmoseFiles(path = path, type = "yieldDistribByAge"),
                    yieldNByAge = readOsmoseFiles(path = path, type = "yieldNDistribByAge"),
                    landingsArray = readOsmoseFiles(path = path, type = "yieldByFisheryBySpecies", varid="landings", ext="nc"),
                    # discardsArray = readOsmoseFiles(path = path, type = "yieldByFisheryBySpecies", varid="discards", ext="nc"),
                    # accessibleBiomass = readOsmoseFiles(path = path, type = "yieldByFisheryBySpecies", varid="accessible_biomass", ext="nc"),

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

  if(!is.null(outputData$landingsArray)) {
    ### LANDINGS
    outputData$landingsBySpeciesByFishery = .reshapeFishery(outputData$landingsArray, rf=rf, by="species")
    outputData$landingsByFisheryBySpecies = .reshapeFishery(outputData$landingsArray, rf=rf, by="fishery")
    outputData$landingsBySpecies =  .reshapeFishery(outputData$landingsArray, rf=rf, by="species", aggregate=TRUE)
    outputData$landingsByFishery =  .reshapeFishery(outputData$landingsArray, rf=rf, by="fishery", aggregate=TRUE)
    outputData$landingsBySpeciesGroups = .add_fisheries_groups(outputData$landingsBySpecies, conf, type="species", merge=FALSE) 
    outputData$landingsByFisheryGroups = .add_fisheries_groups(outputData$landingsByFishery, conf, type="fisheries", merge=FALSE)
    outputData$observed.landings = c(.aggregate_catch_bytime(outputData, conf, type="landingsBySpecies"),
                                     .aggregate_catch_bytime(outputData, conf, type="landingsBySpeciesGroups"),
                                     .aggregate_catch_bytime(outputData, conf, type="landingsByFishery"),
                                     .aggregate_catch_bytime(outputData, conf, type="landingsByFisheryGroups"))
    ### DISCARDS
    
    ### ACCESSIBLE BIOMASS
    # temporal
    # dmn = dimnames(outputData$biomass)
    # rf = .getPar(conf, "output.recordfrequency.ndt")
    # # ybf = aperm(apply(outputData$yieldArray, c(1, 3, 4), sum), c(2,1,3))
    # ybf = apply(outputData$yieldArray, c(1, 3, 4), sum)
    # ybf = rowsum(ybf, group=rep(seq_len(nrow(ybf)), each=rf, length.out=nrow(ybf)))
    # rownames(ybf) = dmn[[1]]
    # colnames(ybf) = get_fisheries(conf)
    # outputData$yieldByFishery = ybf
    # outputData$yieldByFishery = .add_fisheries_groups(x=outputData$yieldByFishery, conf=conf)
    # 
    # # yieldByFisheryBySpecies
    # outputData$yieldByFisheryBySpecies = .reshapeFishery(outputData$yieldArray, rf=rf)
    # names(outputData$yieldByFisheryBySpecies) = get_fisheries(conf)
    # xfg = .get_functional_groups(conf, "fisheries")
    # outputData$yieldByFisheryBySpecies = c(outputData$yieldByFisheryBySpecies, 
    #                                        lapply(xfg, FUN=.my_list_sum, x=outputData$yieldByFisheryBySpecies))
    # # yieldBySpeciesByFishery
    # dmn[[2]] = get_fisheries(conf)
    # outputData$yieldBySpeciesByFishery = .reshapeFishery(outputData$yieldArray, rf=rf, by = "species")
    # names(outputData$yieldBySpeciesByFishery) = get_species(conf, type="focal")
    # xfg = .get_functional_groups(conf, "species")
    # outputData$yieldBySpeciesByFishery = c(outputData$yieldBySpeciesByFishery, 
    #                                        lapply(xfg, FUN=.my_list_sum, x=outputData$yieldBySpeciesByFishery))
    # end of temporal
  } else {
    ### LANDINGS
    outputData$landingsBySpeciesByFishery = NULL
    outputData$landingsByFisheryBySpecies = NULL
    outputData$landingsBySpecies = NULL  
    outputData$landingsByFishery = NULL
    outputData$landingsBySpeciesGroups = NULL 
    outputData$landingsByFisheryGroups = NULL
    outputData$observed.landings = NULL
    ### DISCARDS
    ### ACCESSIBLE BIOMASS
  }

  # surveys as independent outputs
  outputData = .add_surveys(x=outputData$surveyBiomass, out=outputData, type="biomass", conf=conf)
  outputData = .add_surveys(x=outputData$surveyAbundance, out=outputData, type="abundance", conf=conf)
  # outputData = .add_surveys(x=outputData$yieldByFisheryBySpecies, out=outputData, type="yield", conf=conf)

  if(!is.null(conf)) {

    outputData = .calculate_size_residuals_byage(outputData, conf)
    outputData = .calculate_mort_residuals_byage(outputData, conf)

    # reshaping of catch-at-length/age
    outputData = .aggregate_catch_byclass(outputData, conf, "size", "abundance")
    outputData = .aggregate_catch_byclass(outputData, conf, "size", "biomass")
    outputData = .aggregate_catch_byclass(outputData, conf, "age", "abundance")
    outputData = .aggregate_catch_byclass(outputData, conf, "age", "biomass")

    # yieldByYear (include species groups)
    outputData = .aggregate_catch_byyear(outputData, conf, type="yield")
    
    start = get_par(conf, "simulation.time.start")
    if(is.null(start)) start = 0
    ndt   = get_par(conf, "simulation.time.ndtPerYear")/get_par(conf, "output.recordfrequency.ndt")
    nyear = get_par(conf, "simulation.time.nyear") - get_par(conf, "output.start.year")
    step0 = get_par(conf, "output.step0.include")
    if(is.null(step0)) step0 = 0
    times = start + seq(from=1-step0, to=nyear*ndt)/ndt

    spp = c(get_species(conf, type="focal"), get_species(conf, type="background"))

    model = get_par(conf, "output.file.prefix")
    simus = get_par(conf, "simulation.nsimulation")
    nsp = length(spp)
    T = nyear*ndt

  } else {
    model = NULL
    simus = NULL
    times = NULL
    T     = NULL
    start = NULL
    spp   = NULL
    nsp   = NULL
  }

  model = list(version = "4", model = model, simus = simus, times = times,
               T = T, start = start, nsp = nsp, lspecies = spp)

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



.my_list_sum = function(ind, x) {
  ii = x[ind]  
  out = 0
  for(i in seq_along(ii)) out = out + ii[[i]]
  return(out)
}


.add_fisheries_groups = function(x, conf, type="fisheries", merge=TRUE) {
  
  if(is.null(x)) return(NULL)
  if(all(sapply(x, is.null))) return(NULL)
  if(length(x) == 0) return(NULL)
  
  # fg = .get_functional_groups(conf, type=type)
  fgd = get_fg_data(conf, x=x, type=type)
  if(is.null(fgd)) return(x)
  rownames(fgd) = rownames(x)
  dimnames(fgd)[[3]] = dimnames(x)[[3]]
  
  if(isTRUE(merge)) fgd = .cbind(x, fgd)
  
  class(fgd) = c("osmose.yield", class(x))
  
  return(fgd)
  
}

.cbind = function(x, y) {
  if(is.null(y)) return(x)
  if(length(dim(x))==2) dim(x) = c(dim(x), 1)
  xout = array(dim = dim(x) + c(0, dim(y)[2], 0))
  xout[, seq_len(ncol(x)),] = x
  xout[, ncol(x) + seq_len(ncol(y)), ] = y
  colnames(xout) = c(colnames(x), colnames(y))
  rownames(xout) = rownames(x)
  return(xout)
}

.add_surveys = function(x, out, type, conf) {

  if(is.null(x)) return(out)
  if(all(sapply(x, is.null))) return(out)
  if(length(x) == 0) return(out)
  for(i in seq_along(x)) {
    if(type!="yield") {
      src = get_surveys(conf, sr=names(x)[i])
      spc = get_par(conf, par=sprintf("surveys.targetspecies.sr%d", src))
      
      fgd = get_fg_data(conf, x=x[[i]], type="species")
      
      if(!is.null(spc)) {
        spx = get_species(conf, nm=sprintf("sp%d", spc))
        x[[i]] = x[[i]][, spx, , drop=FALSE]
      }
      
    } else {

      fgd = get_fg_data(conf, x=x[[i]], type="species")
      
    } # end of !yield
    
    if(!is.null(fgd)) x[[i]] = .cbind(x[[i]], fgd)

    class(x[[i]]) = c(sprintf("osmose.%s", type), class(x[[i]]))
    
  }
  names(x) = paste(type, names(x), sep=".")
  out = c(out, x)
  return(out)
  
}

get_fg_data = function(conf, x, type="species") {
  fg = .get_functional_groups(conf, type=type)
  if(is.null(fg)) return(NULL)
  ppp = lapply(fg, FUN=.fg_col_sum, x=x)
  out = array(dim=c(dim(ppp[[1]])[1], length(ppp), dim(ppp[[1]])[2]))
  for(i in seq_along(ppp)) out[, i, ] = ppp[[i]]
  colnames(out) = names(fg)
  return(out)
}

.fg_col_sum = function(ind, x) apply(x[, ind, , drop=FALSE], MARGIN = c(1,3), FUN=sum, na.rm=TRUE)


.get_functional_groups = function(conf, type="species") {
  
  code = sprintf("output.%s.group.%s", type, ifelse(type=="species", "sp", "fsh"))
  gp = get_par(conf, code, unlist=TRUE)
  if(is.null(gp)) return(NULL)
  
  .find_species_in_group = function(y, gp, conf, type) {
    nmg = gsub(names(gp[which(gp==y)]), pattern=code, replacement="")
    xout = NULL
    if(type=="species") xout = get_species(conf, nm=sprintf("sp%s", nmg))
    if(type=="fisheries") xout = get_fisheries(conf, nm=sprintf("fsh%s", nmg))
    return(xout)
  }
  
  out = sapply(na.omit(unique(gp)), FUN=.find_species_in_group, gp=gp, conf=conf, type=type)
  
  return(out)
  
}

.reshapeFishery = function(x, rf, by="fishery", aggregate=FALSE) {

  by = match.arg(by, c("fishery", "species"))
  
  if(is.null(x)) return(x)

  nm = dimnames(x)
  ind = rep(seq_len(nrow(x)), each=rf, length.out=nrow(x))
  tt = tapply(as.numeric(nm[[1]]), INDEX=ind, FUN=max)
  
  .addRep = function(x) {
    if(length(dim(x))==2) dim(x) = c(dim(x), 1)
    return(x)
  }
  
  if(isTRUE(aggregate)) {
    
    if(by=="fishery")  out = apply(x, c(1,3,4), FUN=sum, na.rm=TRUE)
    if(by=="species")  out = apply(x, c(1,2,4), FUN=sum, na.rm=TRUE)
    out = rowsum(out, group=ind)
    dimnames(out)[[1]] = tt
    names(dimnames(out))[1] = "time"
    
  } else {
    
    if(by=="fishery") out = lapply(seq_len(dim(x)[3]), FUN = function(i, x) x[,,i,], x=x)
    if(by=="species") out = lapply(seq_len(dim(x)[2]), FUN = function(i, x) x[,i,,], x=x)

    xp = if(by=="fishery") 3 else 2
    gp = nm[[xp]]
    nm = nm[-xp]
    nm[[1]] = tt
    
    out = lapply(out, FUN=.addRep)
    out = lapply(out, FUN=rowsum, group=ind)
    
    for(i in seq_along(out)) {
      dimnames(out[[i]]) = nm
    }
    names(out) = gp
  }
  
  return(out)
  
}
