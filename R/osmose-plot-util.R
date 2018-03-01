# Reformat the data into the ggplot2 stacked plot format.
# It returns a dataframe with specie, time and predation rate
# in the columns.
.osmose.format_data_stacked = function(data)
{
  
  ntime = dim(data)[1]
  npecie = dim(data)[2]
  time = 1:ntime  # extracts the time array
  
  # Initialize the output array
  output = data.frame()
  
  # loop over all the specie and update the output array.
  for (spec in colnames(data)) {
    temp = data.frame(specie=rep(spec, ntime), time=time, value=data[, spec])
    output = rbind(output, temp)
  }
  
  # converts specie into a factor
  output$specie = factor(output$specie)
  
  return(output)
  
}

# The most basic barplot you can do:
#barplot(my_vector)

#' Opens a figure file.
#'
#' @param figname Name of the output file. The file sufix must
#' be .png or .pdf
#' @param width  Figure width (default NULL)
#' @param height Figure height (default NULL)
#'
#' @export
osmose.openfigure = function(figname, width=NULL, height=NULL)
{
    usepng = endsWith(figname, '.png')
    usepdf = endsWith(figname, '.pdf')

    if(!usepng & !usepdf)
    {
        stop("The filename must end by .png or .pdf")
    }

    # Change the classic attribute of plots:
    if(usepng)
    {
        png(figname , width=width, height=height)
    } else {
        pdf(figname, width=width, height=height)
    }
    
}


#' Plots a barplot, with xlabels rotated with a 45degree angle
#'
#' @param x Data array
#' @param ... Additional arguments to the barplot function (color, etc.)
#'
#' @export
osmose.barplot = function(x, ...)
{
  
    # space=1 means that one blank bar is left for one drawn bar
    # xaxt="n" desactivates the defaut xlabel (True?)
    barplot(x, space=1, las=2, xaxt="n", ...)

    # last point
    end_point = 0.5 + length(x) + length(x) - 1
    
    # xlabel points
    xlab = seq(1.5, end_point, by=2)
  
    # draw the text labels.
    text(seq(1.5, end_point, by=2), par("usr")[3]-0.25, 
         srt=45, adj=1, xpd=TRUE,
         labels = paste(names(x)), cex=1)

}

#' Plot a stacked percent plot.
#'
#' @param data 
#' @param legPosX X position of legend (if NULL, no legend is plotted)
#' @param legPosY Y position of legend
#' @param ... Additional arguments to the barplot function.
#' @export
osmose.stackedpcent = function(data, legPosX="right", legPosY=NULL, col=NULL, ...)
{

  # cheks that the data frame contains labeled rows and columns
  if(length(colnames(data)) == 0)
  {
    stop("You must provide column names to your dataframe")
  }
  
  if(length(rownames(data)) == 0)
  {
    stop("You must provide row names to your dataframe")
  }

  if(is.null(col))
  {
    N = nrow(data)
    col = brewer.pal(N, "Pastel2")
  }
  
  barplot(data, col=col, ...)
  
  # to do: check that data has colnames and rownames#
  if(!is.null(legPosX))
  {
  N = nrow(data)
  legend(legPosX, legPosY, 
         legend = rownames(data), 
         ncol=1, xpd=TRUE, inset=c(-0.2, 0),
         fill=col, bg="white")
  }
}

# extract the default ggplot2 color table for fill plots
# it takes as argument the discrete level array 
.make_default_ggplot_col = function(levels)
{
  colors = scale_fill_manual(values=topo.colors(length(levels)))
  return(colors)
}



