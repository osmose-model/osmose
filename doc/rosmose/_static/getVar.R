library("osmose")

getwd()
input.dir = file.path("eec_4.3.0", "output-PAPIER-trophic")
data = read_osmose(input.dir)

biom = get_var(data, "biomass")
class(biom)
dim(biom)
cat("\n")

# expected = TRUE if mean over replicate should be returned
# only works for data that inherits from array
biom = get_var(data, "biomass", expected=TRUE)
class(biom)
dim(biom)
cat("\n")

biom = get_var(data, "biomass", how="list")
class(biom)
names(biom)
cat(biom$OctopusVulgaris)
cat("\n")

biom = get_var(data, "dietMatrixByAge")
class(biom)
cat("\n")

biom = get_var(data, "dietMatrixByAge", how="list")
class(biom)

