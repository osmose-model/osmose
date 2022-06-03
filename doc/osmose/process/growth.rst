Growth
----------------------------------------

Individuals of a given school are assumed to grow in size and weight at a given time only when the amount of food they ingested fulfill maintenance requirements, i.e., only when their predation efficiency at that time is greater than the predation efficiency ensuring body maintenance of school. 

.. math:: 

    G(s, a) = M_{\Delta}(s, a) \times \frac{S_R(s, a) - C_{S_R}(s)} {1 - C_{S_R}(s)}\ if\ S_R(s, a) \ge C_{S_R}
    
    G(s, a) = 0\ if\ S_R(s, a) < C_{S_R}


with :math:`C_{S_R}` is the critical predation efficiency and :math:`S_R` the predation success rate of the school.

:math:`M_{\Delta}(s, a) = \lambda \times \Delta L(a)` is the maximum growth rate at age :math:`a` and for species :math:`s`.

:math:`\Delta L(a) = L(a + 1) - L(a)` is the mean length increase determined from a growth function (Von Bertalanffy or Gompertz growth function), while :math:`\lambda` is a factor that allows to control the maximum length at a given age.

.. index:: growth.java.classname.sp#, predation.efficiency.critical.sp#, species.delta.lmax.factor.sp#

.. table:: Growth parameters
    :align: center

    .. csv-table:: 
        :delim: ;

        growth.java.classname.sp# ; Class name of the age to length conversion
        predation.efficiency.critical.sp# ; Critical predation success (:math:`C_{S_R}`)
        species.delta.lmax.factor.sp# ; :math:`\lambda` (default = 2)
    
.. ipython:: python
    :suppress:

    import os
    import subprocess
    cwd = os.getcwd()
    fpath = "osmose/process/_static/plot_growth.py"
    with open(fpath) as f:
        with open(os.devnull, "w") as DEVNULL:
            subprocess.call(["python", fpath])



Von Bertalanffy growth 
@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@

When the :samp:`growth.java.classname.sp#` is equal to :samp:`fr.ird.osmose.process.growth.VonBertalanffyGrowth.java`, a von Bertalanffy growth function is used. **It is the defaut one.**

.. math::

    L(a) = L_{egg}\ if\ a=0
    
    L(a) = L_{egg} + (L_{thres} - L_{egg}) \times \left(\frac{a}{a_{thres}}\right)\ if\ a>0\ \&\ a<a_{thres}

    L(a) = L_{\infty} \times \left(1 - \exp^{-K\left(age - t_0\right)} \right)\ else

with 

.. math:: 
    
    L_{thres} = min\left[L_{egg}, L_{\infty} \times \left(1 - \exp^{-K\left(a_{thres} - t_0\right)}\right) \right]

A Von Bertalanffy model is used to calculate mean length increase above a threshold age :math:`a_{thres}` determined for each HTL group from the literature. Below :math:`a_{thres}`, a simple linear model is used. The rationale behind this is that Von Bertalanffy parameters are usually estimated from data excluding youngs of the year or including only very few of them. Assuming a linear growth between age 0 and :math:`a_{thres}` ensures a more realistic calculation of mean length increases for early ages of HTL groups (:cite:`Travers2009`). 

.. figure::  _static/vb.*
    :align: center

    Von Bertalanffy growth curve

.. index:: species.linf.sp#, species.k.sp#, species.t0.sp#, species.vonbertalanffy.threshold.age.sp#

.. table:: Von Bertalanffy parameters
    :align: center

    .. csv-table:: 
        :delim: ;

        species.linf.sp# ; :math:`L_{inf}` (cm)
        species.k.sp# ; :math:`K`
        species.t0.sp# ; :math:`t_0`
        species.vonbertalanffy.threshold.age.sp# ; :math:`a_{thres}` (years, default=1 year)


.. The weight of school i at time t is evaluated from the allometric relationship:

.. .. math::
.. 
..     W = C \times L^b
.. 
.. where :math:`b` and :math:`C` are allometric parameters for the HTL group to which school i belongs.

Gompertz growth 
@@@@@@@@@@@@@@@@@@@@@@@@@@@@

When the :samp:`growth.java.classname.sp#` is equal to :samp:`fr.ird.osmose.process.growth.GompertzGrowth.java`, a Gompertz growth function is used.

.. math:: 

    L(a) = L_{egg}\ if\ a=0

    L(a) = L_{start} \times exp^{K_e \times a}\ if\ a>0\ \& a<a_{exp} 

    L(a) = L_{exp} + (L_{gom} - L_{exp}) \frac{a - a_{exp}}{a_{gom} - a_{exp}}\  if\ a>a_{exp}\ \&\ a<a_{gom} 

    L(a) = L_{inf} \times exp^{-exp^{-K_g (a - t_g)}}\ else

with 

.. math:: 

    L_{exp} = L_{start} \times exp^{K_e \times a_{exp}}

    L_{gom} = L_{inf} \times exp^{-exp^{-K_g (a_{gom} - t_g)}}

.. figure::  _static/gom.*
    :align: center

    Gompertz growth curve

.. index:: 
    single: growth.exponential.lstart.sp#
    single: growth.exponential.ke.sp#
    single: growth.gompertz.linf.sp#
    single: growth.gompertz.kg.sp#
    single: growth.gompertz.tg.sp#
    single: growth.exponential.thr.age.sp#
    single: growth.gompertz.thr.age.sp#

.. table:: Gompertz parameters
    :align: center

    .. csv-table:: 
        :delim: =
    
        growth.exponential.lstart.sp# = :math:`L_{start}` (cm)
        growth.exponential.ke.sp# = :math:`K_e`
        growth.gompertz.linf.sp# = :math:`L_{inf}` (cm)
        growth.gompertz.kg.sp# =  :math:`K_g`
        growth.gompertz.tg.sp# = :math:`t_g` (years)
        growth.exponential.thr.age.sp# = :math:`a_{exp}` (years)
        growth.gompertz.thr.age.sp# =  :math:`a_{gom}` (years)


