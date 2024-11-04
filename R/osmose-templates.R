
#' Creates OSMOSE configuration templates
#'
#' @param file File to write the template.
#' @param what Type of template desired. Currently: "initialisation".
#' @param control Additional arguments provided for each type of template. See details.
#' @param ... Extra arguments passed to each method.
#'
#' @return Called by its side effects, it creates a file with the desired template.
#' @details
#' For the template for the initialisation (\code{what='initialisation'}), control arguments
#' can be supplied as a list containing the named elements \code{'file_biomass'}, \code{'file_yield'},
#' providing the full path to the file containing the biomass and yield. If ommited, a dummy
#' value will be used in the template (that can be replaced programmatically after the file has
#' been created). Additionally, an element named \code{'path_cal'} can be used to provide a path
#' to the catch-at-length files, but each line will need to be modified to add the species-specific
#' file, as one file per species is expected. 
#' 
#' @examples 
#' \dontrun{
#' osmose_template(input=configFile, file="initialisation_template.osm")
#' osmose_template(input=configFile, file="initialisation_template.osm",
#'   control=list(file_biomass="dummy_biomass.csv", 
#'   file_yield="dummy_yield.csv", path_cal="mypath"))}
#' @export
#'
#' @inheritParams run_osmose
osmose_template = function(input, file, what="initialisation", control=list(), ...) {
  
  input = suppressWarnings(normalizePath(input, mustWork=TRUE))
  
  if(missing(file)) stop("A 'file' to create the template must be provided.")
  
  if(!is.null(attr(file, "path"))) file = file.path(attr(file, "path"), file)
  file  = suppressWarnings(normalizePath(file))
  
  if(identical(input, file)) stop("Input configuration file and template output file cannot be identical.")
  
  conf = read_osmose(input=input)
  
  out = switch(what,
               "initialisation"  = .osmose_template_init(conf, file=file, control=control),
               "initialization"  = .osmose_template_init(conf, file=file, control=control),
               stop(sprintf("what='%s' is not supported.", type))
  )  
  
  return(invisible(out))
  
}


# Internal ----------------------------------------------------------------


# Template for initialisation
.osmose_template_init = function(conf, file, control, ...) {
  
  file_biomass = if(is.null(control$file_biomass)) "../data/path/to/biomass/file/biomass.csv" else control$file_biomass
  file_yield = if(is.null(control$file_yield)) "../data/path/to/landings/file/landings.csv" else control$file_yield
  path_cal = if(is.null(control$path_cal)) "../data/path/to/catch_at_length/file/catchatlength.csv" else control$path_cal
  ndt = if(is.null(control$ndt)) 12 else control$ndt
  
  if(length(ndt)==1) ndt = rep(ndt, 3)
  if(length(ndt)!=3) stop("Argument 'ndt' must be of length 1 or 3.")
  
  spp = sort(as.numeric(get_species(conf, type="focal", code=TRUE)))
  
  code = c("species.egg.stage.duration.sp%d", 
           "observed.biomass.guess.sp%d",
           "observed.biomass.file.sp%d",
           "observed.biomass.ndtPerYear.sp%d",
           "observed.biomass.cutoff.size.sp%d",
           "fisheries.yield.file.sp%d",
           "fisheries.yield.ndtPerYear.sp%d",
           "fisheries.catchatlength.file.sp%d",
           "fisheries.catchatlength.ndtPerYear.sp%d",
           "fisheries.selectivity.type.sp%d",
           "fisheries.selectivity.l50.sp%d",
           "fisheries.selectivity.l75.sp%d")
  
  dn = dirname(file)
  if(!dir.exists(dn)) dir.create(dn, recursive=TRUE)
  if(file.exists(file)) file.copy(from=file, to=paste(file, ".bck", sep=""))
  
  msg = sprintf("# OSMOSE initialization setup parameters (created %s)\n", date())
  cat(msg, file=file)
  
  tcat("Instructions", file=file, section=TRUE)
  
  msg = c(
    "species.egg.stage.duration         : duration of the egg stage, in days.", 
    "observed.biomass.guess             : estimated biomass at the start of the simulation, in tonnes.", 
    "observed.biomass.file              : path to a file with time series of biomass for the simulation period.",
    "                                     The name of the column matching the species name will be look for.", 
    "                                     The parameter 'observed.biomass.guess' has precedence.", 
    "observed.biomass.ndtPerYear        : Number of time steps per year in the biomass file.", 
    "observed.biomass.cutoff.size       : Cutoff size to include biomass in the computation of initial conditions.", 
    "                                     Biomass from schools with size lower than the cutoff, are not counted,.", 
    "                                     to reflect the sampling bias in the source of biomass estimates (e.g. survey selectivity).", 
    "fisheries.yield.file               : Mandatory file providing the landings for the species.", 
    "                                     The name of the column matching the species name will be look for.", 
    "                                     For non-exploited species, fill the column with zeros.", 
    "fisheries.yield.ndtPerYear         : Number of time steps per year in the yield file.", 
    "fisheries.catchatlength.file       : Optional file, providing catch-at-length information at the species level,", 
    "                                     i.e. combining all fisheries targeting the species. Optionally, catch-at-length", 
    "                                     from the main fishery can be used, if landings are dominant or minor differences", 
    "                                     in the selectivity with other fleets are expected.", 
    "fisheries.catchatlength.ndtPerYear : Number of time steps per year in the catch-at-length file.", 
    "fisheries.selectivity.type         : If no catch-at-length information is provided, a selectivity function should be assumed.", 
    "                                     0: knife-edge, 1: logistic, 2: normal, 3: log-normal", 
    "fisheries.selectivity.l50          : L50 parameter for the assumed selectivity.", 
    "fisheries.selectivity.l75          : L75 parameter for the assumed selectivity.")
  
  msg = paste("#", msg, collapse="\n")
  cat(msg, file=file, append=TRUE)
  cat("\n", file=file, append=TRUE)
  
  
  for(ispp in spp) {
    
    nm = sprintf(code, ispp)
    
    xspp = get_par(conf, sp=ispp)
    sp_name = get_par(xspp, "species.name")
    
    sb = gpar(xspp, par="seeding.biomass", as.is = FALSE, default=0)
    Linf = gpar(xspp, par="linf", as.is = FALSE, default=0)
    A = gpar(xspp, par="lifespan", as.is = FALSE, default=0)
    smat = gpar(xspp, par="maturity.size", as.is = FALSE)
    amat = unlist(gpar(xspp, par="maturity.age", as.is=TRUE, default=ifelse(A<10, 1, 2)))
    if(is.null(smat)) smat = osmose:::VB(amat, this=xspp)
    
    s1 = min(1.5*smat, 0.5*Linf)
    s2 = min(1.75*smat, 0.9*Linf)
    ss = sort(c(s1, s2))
    
    par = list(egg_stage = 2,
               biomass_guess = sb,
               biomass_file = file_biomass,
               biomass_ndt = ndt[1],
               biomass_cutoff = 5,
               yield_file = file_yield,
               yield_ndt = ndt[2],
               cal_file = path_cal,
               cal_ndt = ndt[3],
               cal_sel_type = 3,
               cal_sel_l50 = round(ss[1], 1),
               cal_sel_l75 = round(ss[2], 1))
    
    names(par) = nm
    class(par) = "osmose.configuration"
    
    tcat(sprintf("sp%d: %s", ispp, sp_name), file=file, section=TRUE)
    write_osmose(par, file=file, append = TRUE, justify=TRUE)
    
  }
  
  message(sprintf("Initialisation setup template written in '%s'.", file))
  
  return(invisible())
  
}



# Auxiliar ----------------------------------------------------------------

tcat = function(x, file, section=TRUE) {
  nx = paste(rep("-", 72 - nchar(as.character(x))), collapse="")
  if(!section) nx = ""
  cat(sprintf("\n# %s %s%s", x, nx, ifelse(section, "\n", "")), file=file, append = TRUE)
  return(invisible())
}

gpar = function(conf, par=NULL, sp=NULL, fsh=NULL, invert=FALSE, as.is=TRUE, 
                unlist=FALSE, linear=FALSE, default=NULL, sort=FALSE) {
  
  pars = get_par(conf=conf, par=par, sp=sp, fsh=fsh, invert=invert, as.is=as.is, 
                 unlist=unlist, linear=linear)
  
  if(is.null(pars) & is.null(default)) return(invisible(NULL))
  if(!invert & is.null(pars) & !is.null(default)) {
    pars = list(default)
    names(pars) = par
    class(pars) = "osmose.configuration"
  } 
  
  if(isTRUE(sort)) {
    ind = as.numeric(gsub("\\D", "", names(pars)))
    pars = pars[order(ind)]
    class(pars) = "osmose.configuration"
  }
  
  return(pars)
  
}

