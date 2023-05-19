Reading Osmose parameters
+++++++++++++++++++++++++++++

The loading of Osmose parameters is achieved by using the :samp:`read_osmose` function with an :samp:`input` argument providing the path of the
main configuration file.

.. literalinclude:: _static/param_loading.R
    :language: R
    :lines: 2-29

.. program-output::  Rscript rosmose/_static/param_loading.R

Furthermore, some parameters can be plotted for a given species. This is done as follows:

.. literalinclude:: _static/param_loading.R
    :language: R
    :lines: 30-


.. ipython:: python
    :suppress:

    import os
    import subprocess
    cwd = os.getcwd()

    fpath = "rosmose/_static/param_loading.R"
    subprocess.call(["Rscript", fpath], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)


.. figure:: _static/species.png
    :align: center

    Growth parameters for species 0

.. figure:: _static/reproduction.png
    :align: center

    Reproduction seasonality for species 1

.. figure:: _static/predation.png
    :align: center

    Predation size range for species 2
