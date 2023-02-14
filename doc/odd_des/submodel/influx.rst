Incoming flux
++++++++++++++++++++++++++++++++++

Some species might not do the full life cycle within the simulated domain (reproduce outside the domain for example). For such species, one way
to take them into account is to include a flux of schools with user-defined age or length at specific time of the year. This is done by setting either the
:samp:`flux.incoming.byDt.byAge.file.sp#` or :samp:`flux.incoming.byDt.bySize.file.sp#` parameters,
which are the paths of the CSV files containing the input flux.

It provides the biomass (in tons) for the given size or age classes and must be as follows:

.. _table_paros_influx:
.. table:: Example of input flux by time-step and by age class

     .. csv-table::
        :delim: ;
        :header-rows: 1

        Time step / Age;0;2;3;4
        0;0;500;800;0
        1;0;500;800;0
        2;0;400;700;0
        3;0;400;700;0

The values of the class intervals (first row) are automatically scanned by Osmose. If :samp:`flux.incoming.byDt.byAge.file.sp#` is provided,
the corresponding length will be computed from age classes using the growth ``ageToLength`` method.
Conversely, if :samp:`flux.incoming.byDt.bySize.file.sp#` is provided, the corresponding age will be computed using the ``lengthToAge``
method (cf. :numref:`mermaid_init_influx`).

.. _mermaid_init_influx:

.. mermaid:: _static/mermaid/init_influx.md
   :align: center
   :caption: Initialization of the incoming flux process

In the above example, there are 4 age classes: :samp:`[0 2[`, :samp:`[2 3[`, :samp:`[3 4[` and :samp:`[4 lifespan[`. Osmose will compute the
incoming age at the middle of the interval (i.e. 1 year, 2.5 year, 3.5 year, etc). For the first time step, Osmose will
therefore input 500 tons of 2.5 year-old school and 800 tons of 3.5 year school.

The values of the time step (leftmost column) does not matter. Osmose assumes there is one line per time step. The number of time
steps in the CSV file must be a multiple of the number of time steps per year. If the time series is shorter than the duration
of the simulation, Osmose will loop over it.
If the time series is longer than the duration of the simulation, Osmose will ignore the exceeding steps.

The state variable that is updated is the ``Simulation.schoolSet`` variable, to which is added these newly created schools.

The number of schools created for each species and each size-class is controlled by
the ``simulation.nschool.sp#`` (also used in the reproduction processes). If the abundance is less than the number
of schools, one school of abundance :math:`A` is created. Else, :math:`N_{school}` of
abundance :math:`\dfrac{A}{N_{school}}` are created (cf. :numref:`mermaid_influx`).

.. _mermaid_influx:

.. mermaid:: _static/mermaid/influx.md
   :align: center
   :caption: Incomging flux process

Size-classes are handled the same way than age classes, except that the last size-class covers the :samp:`[4, Linf[` interval.

.. danger::

   The incoming biomass should be calibrated.