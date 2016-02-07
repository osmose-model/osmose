

# Fishing -----------------------------------------------------------------

.writeFishingFile = function(sp, fishing, species, T, ndt, output) {
  
  if(is.null(fishing$rate$method[[sp]])) return(NULL)
  
  fileCode = file.path(output, "fishing_%s_%s.csv")
  
  f           = getFishingMortality(sp, fishing, T, ndt) 
  write.osmose(f, file=sprintf(fileCode, "byDt", species$name[sp]))
  
  selectivity = getSelectivity(sp, fishing, T, ndt)
  if(!is.null(selectivity)) {
    Fs          = f %o% selectivity
    write.osmose(f, file=sprintf(fileCode, "byDt", species$name[sp]))
    
  }
  
  fishing = cbind(fishing, f) 
  write.osmose(Fs, file=file.path(output, paste0("F-", isp, ".csv")))
  
}
