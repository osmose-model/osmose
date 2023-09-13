
Calibration settings
+++++++++++++++++++++

The user must create a CSV file that contains informations about the data that will be used in the calibration process (see :numref:`table_calset`). This file will be read by the :samp:`calibrar` package.

.. _table_calset:
.. table:: Calibration settings
    
    .. csv-table::
        :file: _static/calib_demo/calibration_settings.csv

The table columns are:

- :samp:`variable` is the list of the variables that will be fit the model with observations.
- :samp:`type` is the likelyhood function that will be used in the fitting. It can either be a native function of the :samp:`calibrar` package or a user defined function.
- :samp:`calibrate` indicates whether the data is used in the calibration.
- :samp:`weigthts` provides the relative weights used to combine the partial objective values obtained for each variable. 
- :samp:`useData` indicates whether data are read from the disk. If :samp:`useData=FALSE`, the observed value is set to :samp:`NULL` and the likelyhood function is expected to use simulated data only. The latter option can be particularly useful to set penalties in the model outputs or parameters, where no observed data are needed. 

.. warning:: 

    If :samp:`useData = TRUE`, the name of the CSV file must be consistent with the name of the variable. For instance, if the variable is :samp:`PagellusErythrinus.thr`, the data file must be
    :samp:`PagellusErythrinus.thr.csv`


.. danger::

    The column names (i.e. the header) are important and must be as shown in :numref:`table_calset`.
