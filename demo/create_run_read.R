rm(list = ls()); gc(reset = TRUE)


# Define location of files for example ------------------------------------

folder_location <- readline("Indicate a folder location where the files will be copied...: ")


# Creates basic files for a running ---------------------------------------

# Remove extra quotes on folder_location
folder_location <- gsub(x = folder_location, pattern = "\"", replacement = "")

# Generates example files using 'osmose_demo' function
demoPaths <- osmose_demo(path = folder_location, config = "gog")

# Show created files 
print(list.files(path = dirname(demoPaths$config_file), recursive = TRUE))

readline("Press any key to continue...")

# Run OSMOSE --------------------------------------------------------------

# Run an example using 'run_osmose' function
run_osmose(input = demoPaths$config_file)

readline("Press any key to continue...")

# Read outputs ------------------------------------------------------------

# Read outputs using 'read_osmose' function
outputs <- read_osmose(path = demoPaths$output_dir, 
                       input = demoPaths$config_file)

readline("Press any key to continue...")

# print method
print(outputs)

readline("Press any key to continue...")

# print.summary method
print(summary(outputs))
