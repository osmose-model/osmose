#!/bin/bash
#SBATCH --job-name=osmose_calibration
#SBATCH --partition=standard
#SBATCH -c 90 
#SBATCH --mem=256G



# change to work directory
cd $SLURM_SUBMIT_DIR

export PBS_NODEFILE=$(mktemp)  # Create a temporary file for the node list
scontrol show hostnames $SLURM_JOB_NODELIST > "$PBS_NODEFILE"

# export R_LIBS with the desired version of osmose and calibrar
export R_LIBS=/home/$USER/R/x86_64-pc-linux-gnu-library/4.4

# Load modules (if available, choose versions working in parallel)
#module load R
#module load java
#module load nco

# Load conda environment (if available)
#conda activate r4

# Run R script in parallel mode
time Rscript .calibration.R --ncores=90 --omp --nht &>> mycalibration.log
