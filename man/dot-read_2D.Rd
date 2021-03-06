% Generated by roxygen2: do not edit by hand
% Please edit documentation in R/osmose-internal.R
\name{.read_2D}
\alias{.read_2D}
\title{Generic function to read 2D output files (dietMatrix, sizeSpectrum, etc).
If no file is found, it returns NULL.}
\usage{
.read_2D(files, path, ...)
}
\arguments{
\item{files}{List of input files}

\item{path}{Data directory}

\item{...}{Extra arguments passed to the method.}
}
\value{
A 3D array (time, species, replicates) or NULL if no file is found.
}
\description{
Generic function to read 2D output files (dietMatrix, sizeSpectrum, etc).
If no file is found, it returns NULL.
}
