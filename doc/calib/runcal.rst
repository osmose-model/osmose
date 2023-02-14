.. _caldes:

Create the calibration script
+++++++++++++++++++++++++++++++++++++++++++++

The next step is to write the calibration script, which is the main script to execute to run the calibration.

The sample script used to calibrate the :cite:`Halouani2016` configuration, provide in the :samp:`osmose` package.  is 
detailed below.

.. danger:: 

    **All the files and script provided in the package are built for the calibration of the reference configuration. They should
    never be used to calibrate another configuration.**

Library and function loading
------------------------------

Load the libraries and source the :samp:`runModel.R` file (which must be written by the user, see :numref:`runmod`).

  .. literalinclude:: _static/calib_demo/calibrate.R
    :language: R
    :lines: 1-9


Define your likelyfood functions
-----------------------------------

The user has the possibility to define its own likelyhood function. For instance: 
  
  .. literalinclude:: _static/calib_demo/calibrate.R
    :language: R
    :lines: 11-16

.. warning::

   The function name must match the name of the :samp:`type` column in the calibration settings (see :numref:`table_calset`)

Read calibration information
------------------------------

The next step is to read the calibration informations (see :numref:`table_calset`). This is done 
by using the :samp:`calibrar::getCalibrationInfo` function.

  .. literalinclude:: _static/calib_demo/calibrate.R
    :language: R
    :lines: 18-21

Read observation datasets
------------------------------

The next step is to load the observation datasets by using the :samp:`calibrar::getObservedData` function.

  .. literalinclude:: _static/calib_demo/calibrate.R
    :language: R
    :lines: 23-25

Load the calibrated parameters
---------------------------------

Load the calibrated parameters (see :numref:`table_parcal`). 

In this example, the reading of the parameters is done as follows:
  
  .. literalinclude:: _static/calib_demo/calibrate.R
    :language: R
    :lines: 27-32

.. warning::

    Since the format of this file is free, the user must insure that it is properly read.

Create objective function
-----------------------------

The next step is to create the objective function, by using 
the :samp:`calibrar::createObjectiveFunction` function. It's arguments are:

- :samp:`runModel`: the function that is used to run the model (which must be created by the user, see :numref:`runmod`)
- :samp:`info`: the calibration information (output of the :samp:`calibrar::getCalibrationInfo` function)
- :samp:`observed`: the observed datasets (output of the :samp:`calibrar::getObservedData` function)
- :samp:`aggregate`: if :samp:`TRUE`, a scalar value is returned by using the :samp:`aggFn`.
- :samp:`aggFn`: the aggregation function. Default is a weighted sum (the weights being provided in :numref:`table_calset`).
- the last arguments are the additional arguments of the :samp:`runModel` function (in this case, the :samp:`names` arguments)

.. whether a scalar value is returned 
.. and eventually the aggregate function 

.. literalinclude:: _static/calib_demo/calibrate.R
    :language: R
    :lines: 34-42

Run the calibration
--------------------------

Finally, the calibration is run by using the :samp:`calibrate` function.
  
.. literalinclude:: _static/calib_demo/calibrate.R
    :language: R
    :lines: 43-

The arguments are:

- the parameter initial guess
- :samp:`fn`: the objective function
- :samp:`method`: the optimiziation method. The :samp:`default` method is the *Adaptative Hierarchical Recombination Evolutionary Strategy (AHR-ES)*
- :samp:`lower`: the lower parameter bounds 
- :samp:`upper`: the upper parameter bounds 
- :samp:`phases`: the calibration phase at which the parameters are estimated.  
- :samp:`replicates`: the number of replicates (i.e the number of times the :samp:`runModel` function will be called).
- :samp:`control`: additional arguments stored in a list (see :numref:`table_control`).
  
.. _table_control:

.. table:: Elements that can be put in the :samp:`control` argument list

    +-----------------+-----------------------------------------------------------------------------------------------------------------------------+
    |    Option       |   Description                                                                                                               | 
    +=================+=============================================================================================================================+
    |    maxit        |   Maximum number of executions of the objective function.                                                                   |
    +-----------------+-----------------------------------------------------------------------------------------------------------------------------+
    |    maxgen       |   Maximum number of generations                                                                                             |
    +-----------------+-----------------------------------------------------------------------------------------------------------------------------+
    |    popsize      |   Population size                                                                                                           |
    +-----------------+-----------------------------------------------------------------------------------------------------------------------------+
    |    parallel     |   Boolean, :samp:`TRUE` in order to activate the parallel execution of the optimization.                                    |
    +-----------------+-----------------------------------------------------------------------------------------------------------------------------+
    |    ncores       |   The number of cores available in the parallel cluster for the active session.                                             |
    |                 |   If :samp:`parallel=TRUE`, the default is to get the number of cores of the system.                                        |
    +-----------------+-----------------------------------------------------------------------------------------------------------------------------+
    |    run          |   An optional folder path to create all the temporary folders needed to run the simulations                                 |
    |                 |   for each parameter combination tested by the optimization algorithm. The folders are recycled every generation            |
    +-----------------+-----------------------------------------------------------------------------------------------------------------------------+
    |    master       |   An optional folder path. All the contents of the designated folder will be copied to each temporary folder.               |
    +-----------------+-----------------------------------------------------------------------------------------------------------------------------+
    |    REPORT       |   Number of iterations after saving a new restart object, which contains all the information necessary to restart           |
    |                 |   the calibration at that point. The default is NULL, and no restart files are created.                                     |
    +-----------------+-----------------------------------------------------------------------------------------------------------------------------+
    |    restart.file |   Filename for the restart file to be created.                                                                              |
    +-----------------+-----------------------------------------------------------------------------------------------------------------------------+


In the above example, calibration for each population will be performed in a :samp:`RUN` directory. For each population, a :samp:`RUN/iX` will be created, which will be the working directory in which the :samp:`runModel` function will be called. 

The :samp:`master` directory (containing the :samp:`calibration-parameters.csv` and :samp:`config.csv` configuration files) will be copied in all the :samp:`RUN/iX` directories.

.. hint::

    For |os| calibration, you can control the number of replicates either through the :samp:`replicates` argument, or by setting the |jos| parameter :samp:`simulation.nsimulation`. 

.. note:: 

    Either :samp:`maxit` or :samp:`maxgen` must be provided. If  :samp:`maxit` is provided, :samp:`maxgen` is computed as 
    
    .. math::

        \frac{maxit}{replicates \times popsize}

.. warning:: 

    Since the |os| model is stochastic, gradient base methods are innappropriate. The only method compatible with |os| are :samp:`AHR-ES`.


.. _para_cuncal:

Running calibration in parallel
-----------------------------------

Because the calibration process is time and resource consuming, it is highly advised to run the calibration in parallel.
This is achieved by setting the :samp:`control$parallel = TRUE` and by initializing the cluster priori the call to the :samp:`calibrate` function.

Furthermore, the content that is used in the calibration (calibration data, likelyhood functions, packages) must be exported to the cluster. This is done by using the
:samp:`clusterExport` and :samp:`clusterEvalQ` functions. The :samp:`calibrate.R` file must be edited as follows:

.. code-block:: R

    control$parallel = TRUE
    control$nCores = 2   #  default value is the available number of cores

    cl = makeCluster(control$nCores)
    registerDoParallel(cl)

    # send the variables and loaded libraries defined in the above to the nodes
    clusterExport(cl, c("objfn", "calibData", "calInfo", "observed", "minmaxt"))
    clusterEvalQ(cl, library("osmose"))
    clusterEvalQ(cl, library("calibrar"))

    cal1 = calibrate(calibData['paropt'], fn=objfn, method='default',
                     lower=calibData['parmin'], upper=calibData['parmax'], 
                     phases=calibData['parphase'], control=control, replicates=2)

    stopCluster(cl)

.. danger:: 

    The way to run R in parallel depends on the calculation platform used!

.. caution:: 

    The cluster must be closed after the :samp:`calibrate` function.
