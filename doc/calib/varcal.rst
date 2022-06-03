
List of variables to calibrate
++++++++++++++++++++++++++++++++

The next step is to create a table containing the list of the Osmose parameters to calibrate (see :numref:`table_parcal`). 

.. _table_parcal:
.. table:: Parameters to calibrate
    
    .. csv-table::
        :file: _static/calib_demo/parameters_to_calib.csv

This table is as follows:

- the first column contains the parameters to calibrate.
- :samp:`paropt` is the parameter initial guess. If :samp:`NA`, the values are initialized as the mean of :samp:`parmin` and :samp:`parmax` (if both are defined). Else, initialized as 0.
- :samp:`parmin` is the minimum possible value of the parameter
- :samp:`parmax` is the maximum possible value of the parameter
- :samp:`parphase` is the calibration phase in which the parameter will be calibrated. If :samp:`NA`, the parameter is not calibrated.

.. hint::

    Here, the table format do not matter since this table will be read by the user prior to run the calibration. **However, it is recommended to keep this format.**

In this example, all the parameters except the :samp:`q.spX` ones, are |os| Java parameters. The :samp:`q.spX` parameters can be viewed as "capturability indexes". Since observation datasets may not be on the same spatial scale as model outputs (survey zone, etc.), this index is used to convert Osmose outputs into observational dataset.

.. danger::
    
    The names of the |jos| calibrated parameters must be properly set, in order to be insure that they will be used by the model.
