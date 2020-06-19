
# Define location of files for example ------------------------------------

folder_location = readline("Indicate a folder location where the files will be copied...: ")


# Creates basic files for a running ---------------------------------------

# Remove extra quotes on folder_location
folder_location = gsub(x = folder_location, pattern = "\"", replacement = "")

# Generates example files using 'osmose_demo' function
demoPaths = osmose_demo(path = folder_location, config = "eec_4.3.0")


# Read osmose configuration files -----------------------------------------

outputs = read_osmose(input = demoPaths$config_file)

# Check plot methods for osmose class -------------------------------------

plot(outputs, what = "reproduction", type = 1)

plot(outputs, what = "reproduction", type = 2)

plot(outputs, what = "species", type = 1)

plot(outputs, what = "predation", type = 1)
