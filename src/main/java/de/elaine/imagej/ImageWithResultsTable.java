package de.elaine.imagej;

import ij.ImagePlus;
import ij.measure.ResultsTable;

/*
Used to return a ImagePlus with a Result Table, see analyzeParticles()
 */

public class ImageWithResultsTable {
    private ImagePlus image;
    private ResultsTable resultsTable;

    public ImageWithResultsTable(ImagePlus image, ResultsTable resultsTable) {
        this.image = image;
        this.resultsTable = resultsTable;
    }

    public ImageWithResultsTable() {
    }

    public ImagePlus getImage() {
        return image;
    }

    public void setImage(ImagePlus image) {
        this.image = image;
    }

    public ResultsTable getResultsTable() {
        return resultsTable;
    }

    public void setResultsTable(ResultsTable resultsTable) {
        this.resultsTable = resultsTable;
    }
}
