Grid
--------------

The grid defines the geographical extent of the simulated domain and the spatial discretization.

Osmose <= 4.1.0
+++++++++++++++++++++++++++++

In Osmose versions prior to 4.1.0, there were several ways to define the Osmose grid, depending on the input format of resource variables.
This was controlled by using the :samp:`grid.java.classname` parameter.

fr.ird.osmose.grid.NcGrid.java
@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@

The easiest way to define an Osmose grid is via a NetCDF file, containing the geographical coordinates and a land/sea mask. It is parameterized as follows:

.. index:: grid.var.lat, grid.var.lon, grid.var.mask, grid.netcdf.file

.. _ncgrid:
.. table:: Parameters for the :samp:`NcGrid.java` class
    :align: center

    .. csv-table:: 
        :delim: =

        grid.netcdf.file = Name of the NetCDF file
        grid.var.lat = Name of the latitude variable
        grid.var.lon = Name of the longitude variable
        grid.var.mask = Name of the mask variable


Points are considered as masked when the :samp:`mask` values is less equal than 0.

fr.ird.osmose.grid.ECO3MGrid.java
@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@

The :samp:`ECO3MGrid.java` class is very similar to :samp:`NcGrid.java`, excepts it has an additional parameter:

.. index:: grid.stride

.. _eco3mgrid:
.. table:: Parameters for the :samp:`ECO3MGrid.java` class
    :align: center

    .. csv-table:: 
        :delim: =

        grid.stride = Number of aggregation points

This parameter defines the number of input cells that will be aggregated together to make one osmose cell. For instance, a value of 4 implies that
16 cells of Eco3M grid will be used to make one cell of the Osmose model.

Note that the input values are expected to be :samp:`double`.

fr.ird.osmose.grid.BFMGrid.java
@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@

It is the same as `ECO3MGrid.java`, except that the input values of longitude, latitude and mask are expected to be :samp:`float`.


fr.ird.osmose.grid.OriginalGrid
@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@

Historically OSMOSE allows to define a regular grid, given a few parameters. Even though it is preferable to use the NetCDF grid definition, here is how a regular grid can be defined:

.. index::
    single: grid.ncolumn
    single: grid.nline
    single: grid.lowright.lat
    single: grid.lowright.lon
    single: grid.upleft.lat
    single: grid.upleft.lon
    single: grid.mask.file


.. _originalgrid:
.. table:: Parameters for the :samp:`ECO3MGrid.java` class
    :align: center

    .. csv-table:: 
        :delim: =

        grid.ncolumn = Number of longitudes
        grid.nline = Number of latitudes
        grid.lowright.lat = Lower right latitude
        grid.lowright.lon = Lower right longitude
        grid.upleft.lat = Upper left latitude
        grid.upleft.lon = Upper right latitude
        grid.mask.file = CSV containing the land-sea mask

The mask file is a CSV with :samp:`ncolumn` and :samp:`nline`. Land takes the value :samp:`-99`,  and ocean cell are defined by a :samp:`0`. Here is an example of a CSV mask file for a 4x4 grid:

.. note:: 

    The :samp:`grid.ncolumn` and :samp:`grid.nline` have been renamed to :samp:`grid.nlon` and :samp:`grid.nlat` 
    in version 3.3.3

.. _table_paros_grid_ex:
.. table:: Example of a 4x4 grid file
    :align: center

    .. csv-table::
        :delim: ;

        0;0;0;0
        0;0;0;-99
        0;0;-99;-99
        0;0;-99;-99

Osmose >= 4.2.0
+++++++++++++++++++++++++++++

From Osmose 4.2.0, only the :samp:`NcGrid.java` class has been kept.

.. danger:: 

    Therefore, old configurations will need pre-processing in order to run with the most recent Osmose versions

Osmose >= 4.3.0
++++++++++++++++++++

In Osmose >= 4.3, :samp:`NaN` will be considered as land points. Therefore, resource files with filled values
over land can be used as a mask file.

Furthermore, an additional optional parameter has been added, :samp:`grid.var.surf`, which provides the name of the cell surface variable (which must be defined in :math:`m^2`).
I not provided, cell surfaces will be reconstructed from longitude and latitude coordinates.
