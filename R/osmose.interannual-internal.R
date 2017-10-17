

# Fishing -----------------------------------------------------------------

# Writes fishing files for individual species.
#
# @param sp Species number (sp0, sp1, etc)
# @param fishing Fishing parameters (method, etc.) 
# @param species Species parameters
# @param T Number of years
# @param ndt  Time step
# @param output Output path
.writeFishingFile = function(sp, fishing, species, T, ndt, output) {
  
  if(is.null(fishing$rate$method[[sp]])) return(NULL)
  
  fileCode = file.path(output, "fishing_%s_%s.csv")
  
  f = getFishingMortality(sp, fishing, T, ndt) 
  write.osmose(f, file=sprintf(fileCode, "byDt", species$name[sp]))
  
  selectivity = getSelectivity(sp, fishing, T, ndt)
  if(!is.null(selectivity)) {
    Fs = f %o% selectivity   # outer product
    #write.osmose(f, file=sprintf(fileCode, "byDt", species$name[sp]))
    write.osmose(Fs, file=file.path(output, paste0("F-", isp, ".csv")))
    
  }
  
  # fishing = cbind(fishing, f)    ????
  # write.osmose(Fs, file=file.path(output, paste0("F-", isp, ".csv")))
  
}
