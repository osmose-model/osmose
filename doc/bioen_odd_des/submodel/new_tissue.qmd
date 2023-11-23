(new-tissue)=

```{eval-rst}
.. ipython:: python
    :suppress:

    import os
    import subprocess
    cwd = os.getcwd()
    fpath = "bioen_odd_des/submodel/_static/plot_repfonct.py"
    subprocess.call(["python", fpath], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
```

# New tissue production: somatic and gonadic growth

The net energy available for new tissues production $E_P$ is the difference between mobilized energy
$E_M$ and maintenance $E_m$ , defined as follows:

$$
E_P(i,t) = E_M(i,t) - E_m(i, t)
$$

The net energy $E_P$  contributes to the production of new tissues with a proportion $\rho$  being allocated to the gonadic compartment $g(i,t)$  and a proportion
$1 - \rho$ allocated to the somatic one $w(i,t)$. This proportion depends on sexual maturity
status $m(i,t)$  of the schools’ individuals and their somatic mass $w(i,t)$. Before sexual
maturation, i.e., when $m(i,t) = 0$,  is equal to 0 and, after maturation, i.e., when $m(i,t) = 1$, it is defined such that the annual mean gonado-somatic
index of individuals $\dfrac{g(i,t)}{w(i,t)}$  is constant throughout their adult life-stage and equal to its genetically coded value $r(i)$  {cite}`boukal2014life, lester2004interpreting, quince2008biphasic`:

$$
\rho(i,t) = m(i,t) \dfrac{r(i)}{\eta \overline{E_P}(i)} w(i, t)
$$ (eq_rho)

where

$$
\overline{E_P}(i) = \dfrac{\Delta t}{a(i, t)} \sum_{t=0}^{t= a(i,t)/\Delta t} E_P(i, t')
$$

is the average net energy available per time step to individuals of school $i$  since their birth, with $\Delta t$  being the duration of a time step. Equation {eq}`eq_rho` differs from a deterministic continuous time version of the same model {cite}`boukal2014life, lester2004interpreting, quince2008biphasic` where the current net energy $E_P(i,t)$  would
be used instead of the average $\overline{E_P}(i)$. The averaging in a stochastic discrete time individual-based model such as EV-OSMOSE insures a smooth increase of proportion $\rho$  as individuals grow by dampening strong variations in $E_P(i,t)$  and thus in $\rho(i,t)$  due to the stochasticity of prey encounter and hence of ingested energy $I(i, t)$.

According to the definition of $\rho$, the net energy $E_P$  is thus fully allocated to somatic growth before maturation and it is shared between somatic and gonadic growth after, and the proportion $\rho$ allocated to gonads increases with mass {cite}`boukal2014life`, which limits somatic growth as individuals become bigger. However, in case mobilized energy $E_M$  cannot fully cover maintenance $E_m$, i.e. when $E_P < 0$ , new tissue production is not possible and the gonadic compartment $g(i,t)$  can be resorbed to provide energy for sustaining maintenance.

Somatic growth is then defined as follows:

$$
\dfrac{dw}{dt}(i,t) =
\begin{cases}
 (1 - \rho(i, t)) E_P(i,t) & \text{ if $E_P \geq 0$}\\
 0 & \text{ otherwise }
 \end{cases}
$$ (eq_somatic_growth)

and gonadic growth as:

$$
\dfrac{dg}{dt}(i,t) =
\begin{cases}
 \eta \rho(i, t) E_P(i,t) & \text{ if $E_P \geq 0$} \\
 \eta E_P(i,t) &  \text{ if $-g(i,t) \leq \eta E_P(i,t) < 0$}\\
 -g(i,t) & \text{ if $\eta E_P(i,t) < -g(i, t)$}\\
 \end{cases}
$$ (eq_gonad_growth)

where $\eta$ is the ratio of energy density between somatic and gonadic tissues, and the second and third expressions account for maintenance coverage by energy reserves contained in gonads. In the former case, gonads’ energy can fully cover maintenance costs but in the latter it cannot, so that individuals undergo energetic starvation and incur additional starvation mortality (see section 5 Mortality for more details).

Equation {eq}`eq_somatic_growth` mechanistically describes somatic mass increment at each time step. The length of an individual of school  at time  is then obtained from the length-mass allometric relationship:

$$
L(i,t) = k w(i,t)^{\alpha}
$$

where $k$ and $\alpha$ are allometric parameters.
