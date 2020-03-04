## ---- echo = FALSE, message = FALSE-------------------------------------------
library(osmose)

## ---- eval=FALSE--------------------------------------------------------------
#  # From CRAN
#  install.packages("osmose")
#  
#  # From Github
#  devtools::install_github("osmose-model/osmose")

## ---- cache=TRUE, echo=FALSE--------------------------------------------------
# Define a folder to copy files
exampleFolder <- tempdir()

# Show folder
cat(exampleFolder)

# Copy files
demo <- osmose_demo(path = exampleFolder, config = "gog")


