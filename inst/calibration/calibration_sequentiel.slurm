#!/bin/bash
#SBATCH --job-name=osmose_calibration
#SBATCH --partition=standard
#SBATCH -c 1 
#SBATCH --mem=4G



# change to work directory
cd $SLURM_SUBMIT_DIR




# export R_LIBS with the desired version of osmose and calibrar
export R_LIBS=/home/$USER/R/x86_64-pc-linux-gnu-library/4.4

# Load modules (if available, choose versions working in parallel)
#module load R
#module load java
#module load nco

# Load conda environment (if available)
#conda activate r4

# Run R script in parallel mode
time Rscript .calibration.R &>> mycalibration.log
