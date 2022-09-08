
rm(list=ls())
getwd()

library(osmose)

demo = osmose_demo(path = "./", config="eec_4.3.0")

print(demo$config_file)
print(demo$extra_args)

# run the osmose java core
run_osmose(demo$config_file, parameters=demo$extra_args, force=TRUE)

# read the osmose outputs
data = read_osmose(demo$output_dir)

# plot the outputs
png(file="astart/_static/osmose_ref_conf.png")
plot(data)
