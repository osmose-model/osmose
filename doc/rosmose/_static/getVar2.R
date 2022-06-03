library("osmose")

input.dir = file.path("eec_4.3.0", "output-PAPIER-trophic")

data = read_osmose(input.dir)

names(data)
