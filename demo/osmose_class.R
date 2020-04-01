
# Define location of files for example ------------------------------------

folder_location = readline("Indicate a folder location where the files will be copied...: ")


# Creates basic files for a running ---------------------------------------

# Remove extra quotes on folder_location
folder_location = gsub(x = folder_location, pattern = "\"", replacement = "")

# Generates example files using 'osmose_demo' function
demoPaths = osmose_demo(path = folder_location, config = "gog")

# Show created files 
print(list.files(path = dirname(demoPaths$config_file), recursive = TRUE))

readline("Press any key to continue...")


# Run OSMOSE --------------------------------------------------------------

# Run an example using 'run_osmose' function
run_osmose(input = demoPaths$config_file)

readline("Press any key to continue...")


# Read outputs ------------------------------------------------------------

# Read outputs using 'read_osmose' function
outputs = read_osmose(path = demoPaths$output_dir, 
                      input = demoPaths$config_file)

readline("Press any key to continue...")

# print method
print(outputs)

readline("Press any key to continue...")

# print.summary method
print(summary(outputs))


# Basic plots -------------------------------------------------------------

# Check more basic forms by changing what, ts, type and (maybe) species

# Create all combinations for this 4 arguments
allTest = expand.grid(what = c("biomass", "abundance", "yield", "yieldN"),
                      ts = c(TRUE, FALSE),
                      type = 1:4, 
                      stringsAsFactors = FALSE)
allTest = allTest[with(allTest, order(what, ts, type)),]

# Remove non-valid forms
allTest = allTest[-which(!allTest$ts & allTest$type == 4),]
rownames(allTest) = seq(nrow(allTest))

# Show all lines to test
print(allTest)

readline("^--- Data frame with combination of parameters to plot (Press any key to continue).")

# Loop over each example
for(i in seq(nrow(allTest))){
  
  # Get argument values
  what = allTest$what[i]
  ts = allTest$ts[i]
  type = allTest$type[i]
  species = if(ts & type == 4) 0 else "NULL"
  
  # Build expression
  evalExpr = sprintf("plot(outputs, what = '%s', ts = %s, type = %s, species = %s)",
                     what, ts, type, species)
  
  # Print expression
  cat("\n", evalExpr, "\n")
  
  # Evaluate expression (make plot)
  eval(parse(text = evalExpr))
}

# Check classes with categorization of Size, Age or Trophic Level (TL)

# Create all combinations for this 4 arguments
allTest = expand.grid(what = c("biomass", "abundance", "yield", "yieldN"),
                      by = c("Size", "Age", "TL"),
                      type = 1:2,
                      stringsAsFactors = FALSE)
allTest = allTest[with(allTest, order(what, by, type)),]
allTest$variable = apply(allTest[,1:2], 1, function(x) paste(append(x, "By", 1), collapse = ""))

# If the variable is empty, pass to the next variable
index = !summary(outputs)$is_empty
index = is.element(allTest$variable, rownames(index)[index[,1]])

# Loop over each example
for(i in seq(nrow(allTest))){
  
  if(!index[i]) next
  
  # Build expression
  evalExpr = sprintf("plot(outputs, what = '%s', type = %s)", 
                     allTest$variable[i], allTest$type[i])
  
  # Print expression
  cat("\n", evalExpr, "\n")
  
  # Evaluate expression (make plot)
  eval(parse(text = evalExpr))
}
