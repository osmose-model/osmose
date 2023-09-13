#!/bin/bash

cd osmose/_static

for f in *tex;
do
    fin=`echo $f | sed s/.tex/.pdf/`
    fout=`echo $f | sed s/.tex/.svg/`
    pdflatex $f
    pdf2svg $fin $fout
done
