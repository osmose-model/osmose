library("osmose")

# recover the reference configuration file
filename = system.file(package="osmose", "extdata", "master", "osm_all-parameters.csv")

# setting output directory
outdir = 'output'

# default run mode (java file in inst/java/)
run_osmose(input=fileName, output=outdir)

# Running Osmose by using another version of the .jar file.
jarfile = "/home/nbarrier/Modeles/osmose/svn-osmose/trunk/dist/osmose-trunk.jar"
run_osmose(osmose=jarfile, input=fileName, 
          output=outdir, version="4")
