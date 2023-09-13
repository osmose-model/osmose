.. _runmod:

Creation of a runModel function
++++++++++++++++++++++++++++++++++

The next step is to create a :samp:`runModel` function, the purpose of which is to preprocess parameter files, run |jos| Java and postprocess the outputs. 

The :samp:`runModel` function used to calibrate the :cite:`Halouani2016` configuration is detailed below.

.. danger::

    **This runModel.R script is built for the reference configuration and must not be used to run other configurations. It must be adapted by the
    user.**
    

Function arguments
-------------------------

This function should be adapted by the user. It must take at least two arguments:

- the array of calibrated parameters (:samp:`param` arguments)
- the names of the calibrated parameters (:samp:`names` arguments)

.. literalinclude:: _static/calib_demo/runModel.R
    :language: R
    :lines:  10                

Writes the calibrated parameters in the CSV file
----------------------------------------------------

The first step is to write the calibrated parameters in the :samp:`calibration-parameters.csv` file (that will be located in the :samp:`master` directory):

.. literalinclude:: _static/calib_demo/runModel.R
    :language: R
    :lines:  12-19  

.. danger:: 

    The file format must be consistent with the expectations of |jos|.

Run |jos|
----------------------------------------------------

The next step is to run the |jos| program. This is done by using the :samp:`osmose::run_osmose` function:

.. literalinclude:: _static/calib_demo/runModel.R
    :language: R
    :lines:  21-27

Here, the :samp:`input` argument is the calibration configuration file used by |os| Java. In this case, it is :samp:`calib_config.csv`, which 
includes the :samp:`calibration-parameters.csv` and the :samp:`osm_all-parameters.csv` (see :numref:`table_conf_csv`).

Process |jos| outputs
-----------------------

The last step is to process |jos| outputs, so that they fit the observation time series (see :numref:`dataprep`). 

The reading of |jos| outputs is achieved by using the :samp:`osmose::read_osmose` function.

.. literalinclude:: _static/calib_demo/runModel.R
    :language: R
    :lines:  29-30

Since biomass and yields obervations are on yearly time-scales, the monthly simulated biomass and yields must be yearly integrated:

.. literalinclude:: _static/calib_demo/runModel.R
    :language: R
    :lines:  32-49

Finally, the output must be converted into a list, **whose elements' names must be consistent with the observation names** (:samp:`variable` column in :numref:`table_calset`).

.. literalinclude:: _static/calib_demo/runModel.R
    :language: R
    :lines:  51-89
