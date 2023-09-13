Running the model
+++++++++++++++++++++++++++++

|jos| can be run from R by using the :samp:`run_osmose` function.

.. literalinclude:: _static/runosmose.R
    :lines: 1-10

By default, the |jos| included in the package is used. 
However, the user is free to use another build of the |jos| program as follows:

.. literalinclude:: _static/runosmose.R
    :lines: 12-15

.. warning::

    The way the |jos| core is called has changed between versions 3 and 4. As a consequence,
    the user must be sure to properly set the :samp:`version` argument.

