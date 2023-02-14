# load libraries
suppressMessages(require("osmose"))

output.dir = file.path("rosmose", "_static")

exampleFolder <- tempdir()
demoPaths <- osmose_demo(path = exampleFolder, config = "eec_4.3.0")

# load the Osmose configuration file
osmConf = readOsmoseConfiguration(demoPaths$config_file)

# Summarize the configuration
summary(osmConf)

# extracting a particular configuration object
sim = get_var(osmConf, what="simulation")

# proper way to recover the season parameter (returns a vector of numeric)
movements = get_var(osmConf, what='movement')

# Plotting reproduction growth
png(filename = file.path(output.dir, 'species.png'))
plot(osmConf, what="species", species=0)

# Plotting reproduction seasonality
png(filename = file.path(output.dir, 'reproduction.png'))
plot(osmConf, what="reproduction", species=1)

# Plotting size range for predation
png(filename = file.path(output.dir, 'predation.png'))
plot(osmConf, what="predation", species=2)