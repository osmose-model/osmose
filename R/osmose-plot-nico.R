
# Plots diet matrix
#
# @param x Diet matrix
# @param time.mean If TRUE, the time mean diet matrix is computed is displayed.
# @param species Species name
# @param colors GGplot2 colors (see for instance scale_fill_manual)
# @param thres Thresholds (in percentage). Time-average predation rates below this threashold 
# are binned together ("other" column).
# @param ... 
#
# @return None
# @export
# @method plot osmose.output.dietMatrix
# plot.osmose.output.dietMatrix = function(x, time.mean=FALSE, species=NULL, colors=NULL, thres=1, ...)
# {
#     
#     x = process.dietMatrix(x, species=species, time.mean=time.mean, thres=thres)
# 
#     if(time.mean)
#     {
#         # If using a time-averaged diet matrix, then 
#         # a barplot is drawn.
#         temp = as.vector(x)
#         names(temp) = names(x)
#         osmose.barplot(temp, xlab="", ylab="Predation (%)", main=species, ...)
#         return(invisible())
#     } 
# 
#     # format the diet matrix to have stacked format for ggplot2
#     data = .osmose.format_data_stacked(x)
#     
#     # Set the column names.
#     colnames(data) = c("Prey", "Time", "Predation")
# 
#     # If no color is provided, we set default colors
#     if(is.null(colors))
#     {
#         colors = .make_default_ggplot_col(rev(levels(data$Prey)))
#     }
# 
#     output = ggplot(data, aes(x=Time, y=Predation, fill=Prey)) +  geom_area(...) + colors + ggtitle(species) 
#     output = output + theme(plot.title = element_text(color="black", size=18, face="bold", hjust=0.5))
#     return(output)
# 
# }



# Plots mortality rates
#
# @param data Mortality rate
# @param species Species name
# @param time.mean If TRUE, the mortality rates for each life stade and for each mortality
# types are plotted. If FALSE, the time series for a given life stade are plotted.
# @param stade Stade ("eggs", "juveniles", "adults). Only if time.mean=FALSE
# @param norm Whether percentage instead of raw mortality rates should be plotted. 
# Only if time.mean=TRUE
# @param colors Ggplot2 color array (see for instance scale_fill_manual)
# @param ... 
#
# @export
# @method plot osmose.output.mortalityRate
# plot.osmose.output.mortalityRate = function(data, species=NULL, time.mean=TRUE, stade=NULL, norm=TRUE, colors=NULL, ...)
# {
#   
#   data = process.mortalityRate(data, species=species, time.mean=time.mean, ...)
# 
#   message = "You must provide a life stade among 'eggs', 'juveniles' or 'adults'"
#   
#   if(time.mean==FALSE)
#   {
#     if(is.null(stade)) 
#     {
#       stop(paste0("No stade provided. ", message, sep=""))
#     }
# 
#     if(!(stade %in% names(data))) 
#     {
#       stop(message)
#     }
#     
#     data = data[[stade]]
#     
#     data = osmose:::.osmose.format_data_stacked(data)
#     
#     colnames(data) = c("Type", "Time", "Rate")
#     
#     if(is.null(colors))
#     {
#     colors = osmose:::.make_default_ggplot_col(rev(levels(data$Type)))
#     }
#     
#     title = paste(species, " (", stade, ")", sep='')
#     
#     output = ggplot(data, aes(x=Time, y=Rate, fill=Type)) +  geom_area(...) + ggtitle(title)
#     output = output + theme(plot.title = element_text(color="black", size=18, face="bold", hjust=0.5))
#     return(output)
#     
#   }
#   
#   # if normalize, display mortality rates into percentage instead of absolutes.
#   if(norm)
#   {
#     # apply the normalize function to all the elements of the list.
#     data = lapply(data, norm_func)
#   }
#   
#   # convert the list into a matrix
#   data = do.call(cbind.data.frame, data)
#   data = as.matrix(data)
#   
#   xlabel = "Stage"
#   if(norm) {
#     ylabel = "Mortality rate (%)"
#   } else {
#     ylabel = "Mortality rate"
#   }
#   
#   osmose.stackedpcent(data, xlab=xlabel, main=species, ylab=ylabel, ...)
#   return(invisible())
#   
# }
# 


# Plots biomass by size class
#
# @param data Biomass distribution by size
# @param species Species name
# @param time.mean If true, time.mean biomass is plotted
# @param lwd Line width
# @param ... 
#
# @export
# @method plot osmose.output.biomassDistribBySize
# plot.osmose.output.biomassDistribBySize = function(data, species=NULL, time.mean=FALSE, lwd=2, ...)
# {
#   
#   out = plot.osmose.output.ts.generic(data, species=species, time.mean=time.mean, lwd=lwd, "Size (cm)", "Biomass", ...)
#   return(out)
# }

# Plots biomass by age class
#
# @param data Biomass distribution by age
# @param species Species name
# @param time.mean If true, time.mean biomass is plotted
# @param lwd Line width
# @param ... 
#
# @export
# @method plot osmose.output.biomassDistribByAge
# plot.osmose.output.biomassDistribByAge = function(data, species=NULL, time.mean=FALSE, lwd=2, ...)
# {
#   
#   out = plot.osmose.output.ts.generic(data, species=species, time.mean=time.mean, lwd=lwd, "Age", "Biomass", ...)
#   return(out)
# }



# Plots the biomass distribution by trophic level
#
# @param data Biomass by trophic level data
# @param species  Species name
# @param time.mean If TRUE, the time-mean biomass is plotted for each TL class
# @param lwd Line width
# @param thres  Threshold below which data are not plotted. If NULL, all data are plotted. Only if time.mean=TRUE
# @param colors GGplot2 colors
# @param ... 
#
# @export
# @method plot osmose.output.biomassDistribByTL
# plot.osmose.output.biomassDistribByTL = function(data, species=NULL, time.mean=TRUE, lwd=2, thres=1, colors=NULL, ...) {
#   
#   .check_species(data, species)
#   
#   data = data[[species]]
# 
#   # computes the replicate mean
#   data = apply(data, c(1, 2), mean)
#   
#   if(time.mean)
#   {
#     # computes temporal mean
#     data = apply(data, 2, mean)
#     
#     # if threshold, removes all the time means below 
#     # threashold
#     if(!is.null(thres)) data = data[data>thres]
#   
#     # keeps only two significative digits to the TL label
#     names(data) = signif(as.numeric(names(data)), 2)
#     osmose:::osmose.barplot(data, ylab="Biomass", xlab="Trophic Level", main=species)
#     return(invisible())
#   }
#   
#   # removes the TL class for which biomass is 0
#   temp = (data != 0)   
#   temp = apply(temp, 2, sum)
#   data = data[, temp>0]
#   
#   colnames(data) = signif(as.numeric(colnames(data)), 2)
#   data = osmose:::.osmose.format_data_stacked(data)
#   
#   colnames(data) = c("TL", "Time", "Biomass")
#     
#   if(is.null(colors))
#   {
#     colors = osmose:::.make_default_ggplot_col(rev(levels(data$Type)))
#   }
#   
#   output = ggplot(data, aes(x=Time, y=Biomass, fill=TL)) +  geom_area(...) + ggtitle(species)
#   output = output + theme(plot.title = element_text(color="black", size=18, face="bold", hjust=0.5))
#   return(output)
#   
# }
# 


# Plots predator pressure.
#
# @param data Predator pressure.
# @param time.mean If TRUE, plots the time-average predator pressure 
# sorted in decreasing order 
# @param species Species names
# @param colors  GGplot2 colors
# @param nmax Maximum number of values to draw. If NULL, all data are plotted.
# @param ... 
#
# @export
# @method plot osmose.output.predatorPressure
# plot.osmose.output.predatorPressure = function(data, time.mean=FALSE, species=NULL, colors=NULL, nmax=NULL, ...)
# {
# 
#   .check_species(data, species)
#   data = data[[species]]
#   
#   # computes replicate mean
#   data = apply(data, c(1, 2), mean, na.rm=TRUE)
#   
#   if(time.mean)
#   {
#     # If using a time-averaged diet matrix, then 
#     # a barplot is draw.
#     
#     # time.mean
#     data = apply(data, 2, mean, na.rm=TRUE)
#     data = sort(data, decreasing=TRUE)
#     if(!is.null(nmax)) data = data[1:nmax]
#     
#     temp = as.vector(data)
#     names(temp) = names(data)
#     osmose.barplot(temp, xlab="", ylab="Predation pressure", main=species, ...)
#     return(invisible())
#   } 
#   
#   # computes the temporal mean and extracts the indexes from higher to lower
#   temp = apply(data, 2, mean, na.rm=TRUE)
#   temp = sort(temp, decreasing=TRUE, index.return=TRUE)
#   index = temp$ix
#   data = data[, index]
#   if(!is.null(nmax))
#   {
#     data = data[, 1:nmax]
#   }
#   
#   # format the diet matrix to have stacked format for ggplot2
#   data = .osmose.format_data_stacked(data)
#   
#   # Set the column names.
#   colnames(data) = c("Prey", "Time", "Predation.pressure")
#   
#   # If no color is provided, we set default colors
#   if(is.null(colors))
#   {
#     colors = .make_default_ggplot_col(rev(levels(data$Prey)))
#   }
#   
#   output = ggplot(data, aes(x=Time, y=Predation.pressure, fill=Prey)) +  geom_area(...) + colors + ggtitle(species) 
#   output = output + theme(plot.title = element_text(color="black", size=18, face="bold", hjust=0.5))
#   return(output)
#   
# }

# Plots the prey biomass before the mortality rate is applied
#
# @param  Data array
# @param x osmose.output.object
# @param start First simulation year
# @param conf  Confidence interval
# @param factor Multiplication factor
# @param replicates Draws the time-series for each replicate
# @param freq Output frequency (used to convert from time-step into years)
# @param alpha Transparency for confidence interval
# @param col  Line color
# @param xlim  Xaxis limit
# @param ylim  Yaxis limit
# @param nrep  Maximum number of replicate to draw. If NULL, all the replicates
# are drawn. Only used if replicates=TRUE
# @param time.mean If FALSE, the mean biomass is drawn for each specie as a barplot.
# @param nmax Maximum number of species to draw (only used if time.mean=TRUE)
# @param species Name of the species to draw (only if time.mean=FALSE)
# @param ... 
#
# @return Nothing
# @export
# @method plot osmose.output.biomassPredPreyIni
# plot.osmose.output.biomassPredPreyIni = function(x, start=NULL, conf=0.95, factor=1e-6, replicates=FALSE,
#                                       freq=12, alpha=0.5, col="black", xlim=NULL, ylim=NULL, nrep=NULL,
#                                       time.mean=FALSE, nmax=NULL, species=NULL, ...) {
#   
#   opar = par(no.readonly = TRUE)
#   on.exit(par(opar))
#   
#   if(isTRUE(time.mean)) {
#     .plotAverageBiomass(x, col=col, nmax=nmax, factor=factor, ylab="Biomass before mort.", title="Mean biomass before mortality", ...)
#     return(invisible())
#   }
#   
#   if(!species %in% colnames(x) | is.null(species))
#   {
#     stop("You should proper a species name")
#   }
#   
#   start   = if(is.null(start)) as.numeric(rownames(x)[1]) else start
#   
#   .plotBiomass(x=x, sp=species, start=start, conf=conf, factor=factor, 
#                replicates=replicates, nrep=nrep, freq=freq, col=col, alpha=alpha, 
#                xlim=xlim, ylim=xlim) 
#   
#   title(xlab="Time (years)", ylab="Biomass before pred.", main=species)
#   
#   return(invisible())
# }
# 
# Plots mortality rates by size class.
#
# @param mort Mortality object
# @param species Species name
# @param time.mean If time-average should be plotted. If TRUE, mortality rates by
# for each age class and for each mortality types are plotted. Else, the time series for
# a specific type of mortality is plotted. 
# @param norm Wheter mortality should be normalized (only if time.mean=TRUE)
# @param mtype Mortality type ("Mtot", Mpred", "Mstar", "Mnat", "F" or "Z")
# @param ... Additional arguments of the function.
#
# @return If time.mean=FALSE, returns a ggplot2 object
# @export
# @method plot osmose.output.mortalityRateDistribBySize
# plot.osmose.output.mortalityRateDistribBySize = function(mort, species=NULL, time.mean=TRUE, norm=TRUE, mtype="Mtot", ...)
# {
#   
#   mort = process.mortalityRate(mort, species=species, time.mean=time.mean, norm=norm)
#   
#   if(time.mean) 
#   {
#     plot.mort.byclass.tmean(mort, species, norm, class="size")
#     return(invisible())
#   }
#   
#   mort = extract_mort(mort, mtype)
# 
#   # computes the sum over time (removes NaNs)
#   temp = apply(mort, 2, sum, na.rm=TRUE)
#   
#   # extract size class for which sum is greater than 0
#   mort = mort[, temp>0]
#   
#   # removes the time-step for which there is a NaN value
#   df = as.data.frame(mort)
#   dfna = na.omit(df)
#   mort = as.matrix(dfna)
#   
#   # extracts the time
#   time = rownames(mort)
#   time = as.numeric(time)
#   
#   data = osmose:::.osmose.format_data_stacked(mort, time=time)
#   colnames(data) = c("Size", "Time", "Mortality")
#   
#   colors = NULL
#   # If no color is provided, we set default colors
#   if(is.null(colors))
#   {
#     colors = osmose:::.make_default_ggplot_col(rev(levels(data$Size)))
#   }
#   
#   title = paste(species, " (", mtype, ")", sep="")
#   output = ggplot(data, aes(x=Time, y=Mortality, fill=Size)) +  geom_area(...) + colors + ggtitle(title) 
#   output = output + theme(plot.title = element_text(color="black", size=18, face="bold", hjust=0.5))
#   return(output)
#   
# }

# Plots mortality rates by age class.
#
# @param mort Mortality object
# @param species Species name
# @param time.mean If time-average should be plotted. If TRUE, mortality rates by
# for each age class and for each mortality types are plotted. Else, the time series for
# a specific type of mortality is plotted. 
# @param norm Wheter mortality should be normalized (only if time.mean=TRUE)
# @param mtype Mortality type ("Mtot", Mpred", "Mstar", "Mnat", "F" or "Z")
# @param ... Additional arguments of the function.
#
# @return If time.mean=FALSE, returns a ggplot2 object
# @export
# @method plot osmose.output.mortalityRateDistribByAge
# plot.osmose.output.mortalityRateDistribByAge = function(mort, species=NULL, time.mean=TRUE, norm=TRUE, mtype="Mtot", ...)
# {
#   
#   mort = process.mortalityRate(mort, species=species, time.mean=time.mean, norm=norm)
#   
#   if(time.mean) 
#   {
#     plot.mort.byclass.tmean(mort, species, norm, class="age")
#     return(invisible())
#   }
#   
#   mort = extract_mort(mort, mtype)
#   
#   # computes the sum over time (removes NaNs)
#   temp = apply(mort, 2, sum, na.rm=TRUE)
#   
#   # extract size class for which sum is greater than 0
#   mort = mort[, temp>0]
#   
#   # removes the time-step for which there is a NaN value
#   df = as.data.frame(mort)
#   dfna = na.omit(df)
#   mort = as.matrix(dfna)
#   
#   # extracts the time
#   time = rownames(mort)
#   time = as.numeric(time)
#   
#   data = osmose:::.osmose.format_data_stacked(mort, time=time)
#   colnames(data) = c("Age", "Time", "Mortality")
#   
#   colors = NULL
#   # If no color is provided, we set default colors
#   if(is.null(colors))
#   {
#     colors = osmose:::.make_default_ggplot_col(rev(levels(data$Age)))
#   }
#   
#   title = paste(species, " (", mtype, ")", sep="")
#   output = ggplot(data, aes(x=Time, y=Mortality, fill=Age)) +  geom_area(...) + colors + ggtitle(title) 
#   output = output + theme(plot.title = element_text(color="black", size=18, face="bold", hjust=0.5))
#   return(output)
#   
# }


# Geric function to plot mortality time mean as a function of class (size or age)
# plot.mort.byclass.tmean = function(mort, species, norm, class="size")
# {
#   
#   # converts into matrix
#   mort = do.call(cbind.data.frame, mort)
#   mort = as.matrix(mort)
#   
#   # Counts the total mortality rate for each size class
#   tot = apply(mort, 1, sum)
#   
#   # Extracts the size class with mortality > 0
#   mort = mort[tot>0, ]
#   tot = tot[tot>0]
#   
#   # If data should be normalized, then it is divided by the total 
#   # mortality
#   if(norm) mort = (mort / tot) * 100
#   
#   # Transpose the dataframe
#   mort = t(mort)
#   
#   if(norm) {
#     ylab = "Mortality (%)"
#   } else {
#     ylab = "Mortality"
#   }
#   
#   if(class=="size")
#   {
#     osmose.stackedpcent(mort, xlab="Size (cm)", main=species, ylab=ylab)
#   } else {
#     osmose.stackedpcent(mort, xlab="Age", main=species, ylab=ylab)
#   }
#   return(invisible())
# }
# 

# Extract the mortality array providing the 
# mortality type.
# extract_mort = function(mort, mtype){
#   
#   if(mtype == "Mtot")
#   {
#     mort = mort[["Mpred"]] + mort[["Mstar"]] + mort[["Mnat"]] + mort[["F"]] + mort[["Z"]]
#   } else {
#     
#     if(!(mtype %in% names(mort)))
#     {
#       stop('Mortality type should be "Mtot", Mpred", "Mstar", "Mnat", "F" or "Z"')
#     }
#     
#     # extracts the mortality type
#     mort = mort[[mtype]]
#     
#   }
#   
#   return(mort)
# 
# }
# 
# normalize function. 
# returns percentage instead of raw values
# norm_func = function(data)
# {
#   output = 100 * data / sum(data)
#   dimnames(output) = dimnames(data)
#   return(output)
# }



# 
# plot.osmose.output.ts.generic = function(data, species=NULL, time.mean=FALSE, lwd=2, legtitle, ylab, ...)
# {
#   
#   .check_species(data, species)
#   
#   y = data[[species]]
#   
#   # computes the replicate mean
#   y = apply(y, c(1, 2), mean)
#   
#   if(time.mean == FALSE)
#   {
#     .osmose.plot_ts(y, xlab='Time', ylab=ylab, title=species, lwd=lwd, legtitle=legtitle, ...)
#     return(invisible())
#   }
#   
#   if(time.mean){
#     
#     # Computes the time-mean
#     y = apply(y, 2, mean)
#     temp = as.vector(y)
#     names(temp) = names(y)
#     osmose.barplot(temp, xlab=legtitle, ylab=ylab, main=species, ...)
#     return(invisible())
#   }
#   
# }
# 

