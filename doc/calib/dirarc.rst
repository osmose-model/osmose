
.. _dirar:

Calibration directory architecture
+++++++++++++++++++++++++++++++++++++++++++++

To run the |os| calibration, it is assumed that the following directories and files exist:

- a directory containing observation data used in the calibration. In this example: :samp:`DATA` (see :numref:`dataprep`)
- a directory containing the configuration and forcing files that will be copied in the individual run directories of each population (in this example, :samp:`master`). 
  
In this master directory, the configuration file that will be used in the calibration process must contain the following two lines:
  
.. _table_conf_csv:

.. table:: Osmose main configuration file

    .. csv-table::
            :file: _static/calib_demo/calib_config.csv
            :delim: ;

The first entry is the name of the CSV file that will contain the calibrated parameters. This file must be generated in the calibration process (see :numref:`runmod`).

The second entry is the path of the main configuration file, which contains all the uncalibrated parameters needed by |jos|. 

.. danger::

   The main configuration file (here, :samp:`osm_all-parameters.csv`) may also contain the calibrated parameters (defined in :samp:`calibration-parameters.csv`).
   **In this case, the calibration-parameters.csv file must be included first, in order to insure that it will have precedence over the osm_all-parameters.csv file.**

