
# Check if pkg makes a good copy of example files -------------------------

# Define path
path <- "C:/Users/Wencheng/Downloads/Probando_Osmose/"
config <- "gog"

# Copy files
demo <- osmose_demo(path = path, config = config)

# Run osmose --------------------------------------------------------------

# Just check once, them comment it
# run_osmose(input = demo$config_file)


# Read osmose outputs -----------------------------------------------------

outputs <- read_osmose(path = demo$output_dir, input = demo$config_file)
onlyConfig <- read_osmose(input = demo$config_file)

# Check plot methods for osmose class -------------------------------------

# Check more basic forms by changing what, ts, type and (maybe) species

# Create all combinations for this 4 arguments
allTest <- expand.grid(what = c("biomass", "abundance", "yield", "yieldN"),
                       ts = c(TRUE, FALSE),
                       type = 1:4, 
                       stringsAsFactors = FALSE)
allTest <- allTest[with(allTest, order(what, ts, type)),]

# Remove non-valid forms
allTest <- allTest[-which(!allTest$ts & allTest$type == 4),]
rownames(allTest) <- seq(nrow(allTest))

# Show all lines to test
print(allTest)

# Loop over each example
for(i in seq(nrow(allTest))){
  
  # Get argument values
  what <- allTest$what[i]
  ts <- allTest$ts[i]
  type <- allTest$type[i]
  species <- if(ts & type == 4) 0 else "NULL"
  
  # Build expression
  evalExpr <- sprintf("plot(outputs, what = '%s', ts = %s, type = %s, species = %s)",
                      what, ts, type, species)
  
  # Print expression
  cat("\n", evalExpr, "\n")
  
  # Evaluate expression (make plot)
  eval(parse(text = evalExpr))
  
  # Wait for a moment
  for(j in seq(5)){
    cat("*")
    Sys.sleep(1)  
  }
}


# Check another arguments -------------------------------------------------

what <- 'biomass'

# Check species
plot(outputs, what = what, ts = TRUE, type = 2)
plot(outputs, what = what, ts = TRUE, type = 2, species = 0)
plot(outputs, what = what, ts = TRUE, type = 2, species = 0:2)
plot(outputs, what = what, ts = TRUE, type = 2, species = c(0:2, 7))

# Check speciesNames
plot(outputs, what = what, ts = TRUE, type = 2)
plot(outputs, what = what, ts = TRUE, type = 2, speciesNames = paste("mySp", 0:10))
plot(outputs, what = what, ts = TRUE, type = 2, speciesNames = paste("mySp", 0:2)) # error expected

# Check start/end
plot(outputs, what = what, ts = TRUE, type = 2)
plot(outputs, what = what, ts = TRUE, type = 2, start = 50)
plot(outputs, what = what, ts = TRUE, type = 2, end = 50)
plot(outputs, what = what, ts = TRUE, type = 2, start = 50, end = 70)

# Check initialYear
plot(outputs, what = what, ts = TRUE, type = 2)
plot(outputs, what = what, ts = TRUE, type = 2, initialYear = 2000)

# Check Combination of start-initialYear
plot(outputs, what = what, ts = TRUE, type = 2, initialYear = 2000)
plot(outputs, what = what, ts = TRUE, type = 2, initialYear = 2000, start = 12*6)

# Check replicates
plot(outputs, what = what, ts = TRUE, type = 2, replicates = TRUE)
plot(outputs, what = what, ts = TRUE, type = 2, replicates = FALSE)

# Check freq
plot(outputs, what = what, ts = TRUE, type = 2)
plot(outputs, what = what, ts = TRUE, type = 2, freq = 24)
plot(outputs, what = what, ts = TRUE, type = 2, freq = 6)

# Check horizontal
plot(outputs, what = what, ts = FALSE, type = 1)
plot(outputs, what = what, ts = FALSE, type = 1, horizontal = TRUE)
plot(outputs, what = what, ts = FALSE, type = 2)
plot(outputs, what = what, ts = FALSE, type = 2, horizontal = TRUE)
plot(outputs, what = what, ts = FALSE, type = 3)
plot(outputs, what = what, ts = FALSE, type = 3, horizontal = TRUE)

# Check conf
plot(outputs, what = what, ts = TRUE, type = 2)
plot(outputs, what = what, ts = TRUE, type = 2, conf = 0.5)
plot(outputs, what = what, ts = FALSE, type = 1)
plot(outputs, what = what, ts = FALSE, type = 1, conf = 0.5)

# Check factor
plot(outputs, what = what, ts = TRUE, type = 2)
plot(outputs, what = what, ts = TRUE, type = 2, factor = 1e-2)
plot(outputs, what = what, ts = FALSE, type = 1)
plot(outputs, what = what, ts = FALSE, type = 1, factor = 1e-2)

# Check xlim/ylim
plot(outputs, what = what, ts = TRUE, type = 2)
plot(outputs, what = what, ts = TRUE, type = 2, xlim = c(4, 6), ylim = c(0, 100))
plot(outputs, what = what, ts = FALSE, type = 1)
plot(outputs, what = what, ts = FALSE, type = 1, ylim = c(0, 350))

# Check col
plot(outputs, what = what, ts = TRUE, type = 2)
plot(outputs, what = what, ts = TRUE, type = 2, col = "blue")
plot(outputs, what = what, ts = TRUE, type = 2, col = c("blue", "green"))
plot(outputs, what = what, ts = TRUE, type = 2, col = terrain.colors(11))
plot(outputs, what = what, ts = FALSE, type = 2)
plot(outputs, what = what, ts = FALSE, type = 2, col = "yellow")

# Check alpha
plot(outputs, what = what, ts = TRUE, type = 2)
plot(outputs, what = what, ts = TRUE, type = 2, alpha = 1)
plot(outputs, what = what, ts = TRUE, type = 3)
plot(outputs, what = what, ts = TRUE, type = 3, alpha = 0.3)

# Check border
plot(outputs, what = what, ts = TRUE, type = 2)
plot(outputs, what = what, ts = TRUE, type = 2, border = TRUE)
plot(outputs, what = what, ts = TRUE, type = 2, border = "blue")
plot(outputs, what = what, ts = FALSE, type = 1)
plot(outputs, what = what, ts = FALSE, type = 1, border = NA)
plot(outputs, what = what, ts = FALSE, type = 1, border = "blue")

# Check lty
plot(outputs, what = what, ts = TRUE, type = 2)
plot(outputs, what = what, ts = TRUE, type = 2, lty = 2)
plot(outputs, what = what, ts = TRUE, type = 2, lty = "dotted")
plot(outputs, what = what, ts = TRUE, type = 2, lty = 1:3)

# Check lwd
plot(outputs, what = what, ts = TRUE, type = 2)
plot(outputs, what = what, ts = TRUE, type = 2, lwd = 3)
plot(outputs, what = what, ts = TRUE, type = 2, lwd = 1:3)

# Check axes
plot(outputs, what = what, ts = TRUE, type = 1)
plot(outputs, what = what, ts = TRUE, type = 1, axes = FALSE)
plot(outputs, what = what, ts = FALSE, type = 2)
plot(outputs, what = what, ts = FALSE, type = 2, axes = FALSE)

# Check legend
plot(outputs, what = what, ts = TRUE, type = 2)
plot(outputs, what = what, ts = TRUE, type = 2, legend = FALSE)
plot(outputs, what = what, ts = TRUE, type = 3)
plot(outputs, what = what, ts = TRUE, type = 3, legend = FALSE)

# Check units
plot(outputs, what = 'biomass', ts = TRUE, type = 2)
plot(outputs, what = 'abundance', ts = TRUE, type = 2)
plot(outputs, what = 'biomass', ts = TRUE, type = 2)
plot(outputs, what = 'biomass', ts = TRUE, type = 2, units = "some strange unit")

# Check EXTRA graphic arguments 
# Check las
plot(outputs, what = what, ts = TRUE, type = 2)
plot(outputs, what = what, ts = TRUE, type = 2, las = 2)
plot(outputs, what = what, ts = TRUE, type = 2, las = 3)

# Check cex
plot(outputs, what = what, ts = TRUE, type = 2)
plot(outputs, what = what, ts = TRUE, type = 2, cex = 1.2)
plot(outputs, what = what, ts = TRUE, type = 2, cex = 0.5)

# Check cex.axis
plot(outputs, what = what, ts = TRUE, type = 2)
plot(outputs, what = what, ts = TRUE, type = 2, cex.axis = 1.5)
plot(outputs, what = what, ts = FALSE, type = 2)
plot(outputs, what = what, ts = FALSE, type = 2, cex.axis = 0.5)

# Check xaxs/yaxs
plot(outputs, what = what, ts = TRUE, type = 2)
plot(outputs, what = what, ts = TRUE, type = 2, xaxs = "i", yaxs = "i")
plot(outputs, what = what, ts = FALSE, type = 2)
plot(outputs, what = what, ts = FALSE, type = 2, yaxs = "i")
