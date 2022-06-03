Reading outputs
++++++++++++++++++++++++++++

The reading of outputs is achieved by using the :samp:`read_osmose` function:


.. literalinclude:: _static/read_osmose_outputs.R
    :language: R
    :lines: 1-3

The content of the data object can be obtained as follows:

.. code-block:: R

    names(data)

.. program-output:: Rscript rosmose/_static/getVar2.R

Variables are accessed by using the :samp:`getVar` function, 
which allows to apply some operations prior to extraction (average over the replicates, conversion
into a list or a matrix):

.. literalinclude:: _static/getVar.R
    :language: R

.. program-output:: Rscript rosmose/_static/getVar.R

The first argument is the data object obtained by using the :samp:`read_osmose` function, while the second argument is
the name of the variable to extract.





Reading NetCDF outputs
^^^^^^^^^^^^^^^^^^^^^^^^^

In the new Osmose version, the user has the possibility to save outputs as NetCDF (.nc) instead of
CSV (.csv) files. **However, no function has been provided to automatically read all the Osmose NetCDF files.** Therefore, scripts must be written by the user, following the example shown below:

.. literalinclude:: _static/read_nc.R
    :language: R

.. program-output:: Rscript rosmose/_static/read_nc.R











.. Processing diet matrix
.. ^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. 
.. Diet matrix is processed as follows:
.. 
.. .. literalinclude:: rosmose/_static/dietMatrix.R
..     :language: R
..     :lines: 9-21
.. 
.. .. program-output:: Rscript rosmose/_static/dietMatrix.R
.. 
.. By default, the :samp:`summary` and :samp:`plot` functions for the :samp:`dietMatrix` aggreagates the prey that account for less than 1% of the predator diet into the :samp:`other` variable.
.. 
.. The :samp:`time.mean` argument specifies whether data are time-averaged (default).
.. 
.. .. figure:: rosmose/_static/dietMatrix_mean.png
..     :align: center
.. 
..     Time-average diet matrix for a single species
.. 
.. 
.. .. figure:: rosmose/_static/dietMatrix_ts.png
..     :align: center
..     
..     Time-series of the diet matrix for a single species
.. 
.. Processing mortality
.. ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. 
.. Mortality data is processed as follows:
.. 
.. .. literalinclude:: rosmose/_static/mortality.R
..     :language: R
..     :lines: 9-
.. 
.. .. program-output:: Rscript rosmose/_static/mortality.R
.. 
.. By default, plotting of time-averaged mortality is normalised, i.e. the units are %. This is overriden by setting the :samp:`norm` argument to :samp:`FALSE`.
.. 
.. The plotting of time-varying mortality matrix requires to set the :samp:`stade` arguments. 
.. 
.. .. figure:: rosmose/_static/mortality_mean.png
..     :align: center
.. 
..     Time-average mortality for a single species
.. 
.. 
.. .. figure:: rosmose/_static/mortality_mean_norm.png
..     :align: center
..     
..     Normalised (i.e. in percentage) time-average mortality for a single species
.. 
.. .. figure:: rosmose/_static/mortality_ts.png
..     :align: center
..     
..     Time-average mortality for the adults of a single species 
