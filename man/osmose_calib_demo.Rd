% Generated by roxygen2: do not edit by hand
% Please edit documentation in R/osmose-main.R
\name{osmose_calib_demo}
\alias{osmose_calib_demo}
\title{Generates Osmose configuration files to run an Osmose demo.}
\usage{
osmose_calib_demo(path = NULL)
}
\arguments{
\item{path}{Path where to put the Osmose configuration file.}
}
\value{
A list containing the configuration file to use (config_file) for running the code
and the output directory to use when reading data.
}
\description{
Generates Osmose configuration files to run an Osmose demo.
}
\note{
So far, only one configuration is propose ("eec_4.3.0")
}
\examples{
\dontrun{
rm(list=ls())

library("osmose")

# Copy configuration files into the proper directory
demo = osmose_demo(path="../", config="eec_4.3.0")

# run the osmose model
run_osmose(demo$config_file, parameters=NULL, output=NULL, version="4.3.2", 
          options=NULL, verbose=TRUE, clean=TRUE)

# reads output data
data = read_osmose(demo$output_dir)

# summarize output data
summary(data)

# plot output data
plot(data)
}
}
