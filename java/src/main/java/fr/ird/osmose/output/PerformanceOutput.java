package fr.ird.osmose.output;

import java.lang.management.ManagementFactory;

import com.sun.management.OperatingSystemMXBean;

public class PerformanceOutput extends AbstractOutput {

    private double initialTime;

    // Computation time for one time step
    private double stepComputationTime;

    // Memory usage for one time-step (in MB)
    private double stepMemoryUsage;

    // Amount of virtual memory that is guaranteed to be available to the running
    // process in bytes, or -1 if this operation is not supported.
    long committedVirtualMemorySize;

    // Amount of free physical memory in bytes.
    long freePhysicalMemorySize;

    // Amount of free swap space in bytes.
    long freeSwapSpaceSize;

    // "recent cpu usage" for the Java Virtual Machine process.
    double processCpuLoad;

    // Returns the CPU time used by the process on which the Java virtual machine is
    // running in nanoseconds.
    long processCpuTime;

    // Returns the "recent cpu usage" for the whole system.
    double systemCpuLoad;

    // Returns the total amount of physical memory in bytes.
    long totalPhysicalMemorySize;

    OperatingSystemMXBean osBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);

    private static final long MEGABYTE = 1024L * 1024L;

    PerformanceOutput(int rank, String subfolder, String name) {
        super(rank, subfolder, name);
    }

    @Override
    public void initStep() {
        initialTime = System.currentTimeMillis();
    }

    @Override
    public void reset() {
        stepMemoryUsage = 0;
        stepComputationTime = 0;

        committedVirtualMemorySize = 0;
        freePhysicalMemorySize = 0;
        freeSwapSpaceSize = 0;
        processCpuLoad = 0;
        processCpuTime = 0;
        systemCpuLoad = 0;
        totalPhysicalMemorySize = 0;
    }

    @Override
    public void update() {
        double finalTime = System.currentTimeMillis();
        stepComputationTime += (finalTime - initialTime) / 1000;

        Runtime runtime = Runtime.getRuntime();
        stepMemoryUsage  += (runtime.totalMemory() - runtime.freeMemory())/ MEGABYTE;

        committedVirtualMemorySize += osBean.getCommittedVirtualMemorySize();
        freePhysicalMemorySize += osBean.getFreePhysicalMemorySize();
        freeSwapSpaceSize += osBean.getFreeSwapSpaceSize();
        processCpuLoad += osBean.getProcessCpuLoad();
        freeSwapSpaceSize += osBean.getProcessCpuTime();
        systemCpuLoad += osBean.getSystemCpuLoad();
        totalPhysicalMemorySize += osBean.getTotalPhysicalMemorySize();


    }

    @Override
    public void write(float time) {

        double nsteps = getRecordFrequency();
        double[] output = new double[] { stepComputationTime / nsteps, stepMemoryUsage / nsteps,
                committedVirtualMemorySize / nsteps, freePhysicalMemorySize / nsteps, freeSwapSpaceSize / nsteps,
                processCpuLoad / nsteps, freeSwapSpaceSize / nsteps, systemCpuLoad / nsteps,
                totalPhysicalMemorySize / nsteps };
        writeVariable(time, output);

    }

    @Override
    String getDescription() {
        String output = "Returns the mean CPU time and memory usage for one time-step";
        return output;
    }

    @Override
    public String[] getHeaders() {

        String[] headers = new String[] { "stepComputationTime (seconds)", "stepMemoryUsage (MB)",
            "committedVirtualMemorySize", "freePhysicalMemorySize", "freeSwapSpaceSize",
            "processCpuLoad", "freeSwapSpaceSize", "systemCpuLoad",
            "totalPhysicalMemorySize" };

        return headers;
    }

}
