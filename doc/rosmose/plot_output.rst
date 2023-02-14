Plotting outputs
++++++++++++++++++++++++++++

Plotting Osmose outputs is achieved by calling the generic plot function on the :samp:`osmose` object, output by the :samp:`read_osmose` function. The function takes as argument the variable to be displayed (:samp:`what` argument).
The :samp:`species` argument allows to specify the indexes of the species to display (cf. the :samp:`X` in the :samp:`species.name.spX` parameters).

.. literalinclude:: _static/plot_osmose_outputs.R
    :language: R

.. hint::

    When several replicates are run, the uncertainty due to the stochastic mortality is displayed as a grey shading.

.. warning::

    At this time, only these four variables can be plotted. However, more plot functions (diet matrix, mortality, etc.) will be released soon.

.. ipython:: python
    :suppress:

    import os
    import subprocess
    cwd = os.getcwd()

    fpath = "rosmose/_static/plot_osmose_outputs.R"
    subprocess.call(["Rscript", fpath], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)


.. figure:: _static/biomass.png
    :align: center

    Biomass plot

.. figure:: _static/abundance.png
    :align: center

    Abundance plot

.. figure:: _static/yield.png
    :align: center

    Yield biomass plot

.. figure:: _static/yieldN.png
    :align: center

    Yield abundance plot
