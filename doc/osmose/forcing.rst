.. _rsc-forcing:

Resource forcing
---------------------

Osmose <= 4.0.0
+++++++++++++++++

In Osmose versions <= 4.0.0, there were several ways to read the resource forcing files, depending on the value of the :samp:`ltl.java.classname` parameter.

ECO3M
@@@@@@@@@@@@@@@@@

If the class name is set equal to :samp:`fr.ird.osmose.ltl.LTLForcingECO3M` or 
:samp:`fr.ird.osmose.ltl.LTLFastForcingECO3M`, data values were read as follows.
        
.. index:: 
    single: ltl.nstep
    single: plankton.conversion2tons.plk#
    single: ltl.netcdf.var.plankton.plk#
    single: ltl.netcdf.file.t#
    single: ltl.netcdf.var.zlevel
    single: grid.stride
    single: ltl.integration.depth

.. table:: Eco3M resource parameters
    :align: center

    .. csv-table::
        :delim: ;

        ltl.nstep ; Number of total time steps in the LTL files
        plankton.conversion2tons.plk# ; Conversion factor in tons
        ltl.netcdf.var.plankton.plk# ; Names of the Netcdf resource names
        ltl.netcdf.file.t# ; Names of the NetCDF files to read.
        ltl.netcdf.var.zlevel ; Name of the 3D depth variable
        grid.stride# ; Number of input cells that will be aggregated in an Osmose cell.
        ltl.integration.depth ; Maximum depth where to perform vertical integration

BFM
@@@@@@@@@@@@@@@@@

If the class name is set equal to :samp:`fr.ird.osmose.ltl.LTLForcingBFM` or 
:samp:`fr.ird.osmose.ltl.LTLFastForcingBFM`, data values were read as follows.
        
.. index:: 
    single: ltl.nstep
    single: plankton.conversion2tons.plk#
    single: ltl.netcdf.var.plankton.plk#
    single: ltl.netcdf.file.t#
    single: ltl.netcdf.dim.ntime
    single: grid.netcdf.file
    single: ltl.netcdf.var.zlevel
    single: ltl.netcdf.var.bathy
    single: ltl.integration.depth

.. table:: BFM resource parameters
    :align: center

    .. csv-table::
        :delim: ;

        ltl.nstep ; Number of time steps within a file.
        plankton.conversion2tons.plk# ; Conversion factor in tons
        ltl.netcdf.var.plankton.plk# ; Names of the Netcdf resource names
        ltl.netcdf.file.t# ; Names of the NetCDF files to read.
        ltl.netcdf.dim.ntime ; Number of time steps within a file
        grid.netcdf.file ; Name of the bathymetry file
        ltl.netcdf.var.zlevel ; Name of the 1D depth variable
        ltl.netcdf.var.bathy ; Name of the bathymetry variable
        ltl.integration.depth ; Maximum depth where to perform vertical integration

ROMS/PISCES
@@@@@@@@@@@@@@@

If the class name is set equal to :samp:`fr.ird.osmose.ltl.LTLForcingRomsPisces` or 
:samp:`fr.ird.osmose.ltl.LTLFastForcingRomsPisces`, data values were read as follows.

.. index:: 
    single: ltl.nstep
    single: plankton.conversion2tons.plk#
    single: ltl.netcdf.var.plankton.plk#
    single: ltl.netcdf.file.t#
    single: ltl.netcdf.grid.file
    single: ltl.netcdf.var.lon
    single: ltl.netcdf.var.lat
    single: ltl.netcdf.var.bathy
    single: ltl.netcdf.var.csr
    single: ltl.netcdf.var.hc
    single: ltl.integration.depth

.. table:: ROMS/PISCES resource parameters
    :align: center

    .. csv-table::
        :delim: ;

        ltl.nstep ; Number of time steps within a file.
        plankton.conversion2tons.plk# ; Conversion factor in tons
        ltl.netcdf.var.plankton.plk# ; Names of the Netcdf resource names
        ltl.netcdf.file.t# ; Names of the NetCDF files to read.
        ltl.netcdf.grid.file ; Name of the grid file
        ltl.netcdf.var.lon ; Name of the longitude variable
        ltl.netcdf.var.lat ; Name of the latitude variable
        ltl.netcdf.var.bathy ; Name of the bathymetry variable 
        ltl.netcdf.var.csr ; Name of the CSR variable
        ltl.netcdf.var.hc ; Name of the Hc variable
        ltl.integration.depth ; Maximum depth where to perform vertical integration

Fast forcing
@@@@@@@@@@@@@@ 

In the above configurations, Osmose processes the file to convert them into the right format. However, there is also the possibility to use a file already in the right format using
the :samp:`fr.ird.osmose.ltl.LTLFastForcing` class.
        
.. index:: 
    single: ltl.nstep 
    single: ltl.netcdf.file
    single: plankton.biomass.total.plk#
    single: plankton.multiplier.plk#

.. table:: Fast forcing resource parameters
    :align: center

    .. csv-table::
        :delim: ;

        ltl.nstep ; Number of time steps within a file.
        ltl.netcdf.file ; Name of the resource NetCDF file
        plankton.biomass.total.plk# ; Total biomass within the domain. If not found, reads value from NetCDF
        plankton.multiplier.plk# ; Multiplier of input biomass (default 1, used to run sensitivity experiments).

The netcdf file must contain a 4d variable `ltl_biomass`, whose dimensions are :samp:`(time, ltl, lat, lon)`

.. danger:: 

    The order along the :samp:`ltl` dimension must be consistent with the order of definition of
    the plankton parameters.

.. danger:: 

    **With the fast forcing, the** :samp:`plankton.conversion2tons.plk#` **parameter is not used since data must be provided 
    in tons!**


Osmose 4.1.0 - 4.2.0
+++++++++++++++++++++++++++++

In versions 4.1.0 and 4.2.0, only the :samp:`FastForcing` method has been kept. However, it has been slightly improved. Now
the model can take one NetCDF file per resource variable, and the variable in the NetCDF must match the :samp:`plankton.name.plk#` 
parameter. In this way, there is no more dependency on the order.
        
.. index:: 
    single: plankton.biomass.total.plk#
    single: ltl.nstep
    single: ltl.netcdf.file.plk#
    single: plankton.multiplier.plk#

.. table:: Fast forcing resource parameters
    :align: center

    .. csv-table::
        :delim: ;

        plankton.biomass.total.plk# ; Total biomass within the domain. If not found, reads value from NetCDF
        ltl.nstep ; Number of time steps within a file.
        ltl.netcdf.file.plk# ; Name of the resource NetCDF file (one for each resource)
        plankton.multiplier.plk# ; Multiplier of input biomass (default 1, used to run sensitivity experiments).

.. danger::

    Resource species for which :samp:`plankton.biomass.total.plk#` is used should be defined last, i.e.
    their index :samp:`#` should be greater than those of the other species.

.. danger:: 

    **The resource files must be provided in tons! **The** :samp:`plankton.conversion2tons.plk#` **parameter is not used.**


Osmose >= 4.3.0
+++++++++++++++++++++++++++++++

In the 4.3.0 version, the resource forcing remains similar to versions 4.1-4.2, with some changes in parameters.

.. index:: 
      single:  species.biomass.total.sp#
      single:  species.file.sp#
      single:  species.file.caching.sp#

.. table:: Fast forcing resource parameters
    :align: center

    .. csv-table:: 
        :delim: ;

        species.biomass.total.sp# ; Total biomass for the given ressource (will be distributed over the whole domain)
        species.file.sp# ; Regular expression defining the input files. Can be a file name. 
        species.file.caching.sp# ; Resource caching method. Must be :samp:`none`, :samp:`incremental` or :samp:`all` (default).

If the :samp:`species.biomass.total.sp#` is not found, then the value will be read from the NetCDF file.

The :samp:`species.file.sp#` parameter can now take as an input regular expressions, which will allow to loop over all the files. The regular expressions must be defined in Java mode
(see for instance XXX).

The :samp:`species.file.caching.sp#` defines how the input data will be read:

- In :samp:`all` mode, all the dataset is read at the first time step and stored into memory. *Should be used for climatological forcings for instance.*
- In :samp:`incremental` mode, each time a new time-step is read from file, it is stored in memory. Previous time-steps are kept in memory.
- In :samp:`none` mode, the data is read from file at each time-step. This mode is costly in term of runtime but light in memory since only one time-step is stored.

.. note:: 

    In version 4.3.0, the resource forcing parameter also applies to background species.
