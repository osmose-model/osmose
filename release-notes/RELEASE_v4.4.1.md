# 🌊 OSMOSE 4.4.1 — Java Release Notes

## Bug fixes

- `cacheManager.R`: correct the wrong URLs for the jar files
- `ResourceForcing.java`: correct the parameter names in the inialization of resource forcings (`species.biomass.constant.spX` instead of `species.constant.biomass.sp` and `species.biomass.file.sp` instead of `species.file.sp`)

## Debug features

- Adding a parameter `simulation.kill.if.no.school.enabled` to kill a simulation when a species collapses after the seeding period
- Adding a parameter `species.is.enabled.spX` to deactivate a species in the system.
