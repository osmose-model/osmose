library("osmose")

input.dir = file.path("eec_4.3.0", "output-PAPIER-trophic")
data = read_osmose(input.dir)

output.dir = file.path("rosmose", "_static")

test = get_var(data, "species")

png(file=file.path(output.dir, "biomass.png"))
plot(data, what="biomass")

png(file=file.path(output.dir, "abundance.png"))
plot(data, what="abundance", species=c(0, 1, 2))

png(file=file.path(output.dir, "yield.png"))
plot(data, what="yield")

png(file=file.path(output.dir, "yieldN.png"))
plot(data, what="yieldN")
