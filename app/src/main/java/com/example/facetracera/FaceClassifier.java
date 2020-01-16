package com.example.facetracera;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Objects;

public class FaceClassifier {

    static class PairV{
        String first;
        double second;

        PairV(String first, double second){
            this.first = first;
            this.second = second;
        }
    }

    private String dataSetPath;
    private String[] dataSetClasses;
    private int dataSetWidth;
    private int dataSetHeight;
    private HashMap<String, ArrayList<int[]>> dataSetHistograms;

    FaceClassifier(String folder, int width, int height) {
        File f = new File(folder);
        this.dataSetClasses = f.list();
        this.dataSetPath = folder;
        this.dataSetWidth = width;
        this.dataSetHeight = height;
    }

    FaceClassifier(HashMap<String, ArrayList<int[]>> hists, int width, int height) {
        this.dataSetHistograms = hists;
        this.dataSetWidth = width;
        this.dataSetHeight = height;
    }

    public void trainDataSet() {
        this.dataSetHistograms = new HashMap<>();
        for (String aClass : this.dataSetClasses) {

            final String dirClassString = this.dataSetPath + '\\' + aClass;
            final File dirClass = new File(dirClassString);

            if (dirClass.isDirectory()){
                ArrayList<int[]> faceClassHistograms = new ArrayList<>();
                for (File imgFile: Objects.requireNonNull(dirClass.listFiles())){
                    final String imgPath = imgFile.getPath();
                    final Bitmap img = BitmapFactory.decodeFile(imgPath);
                    if (img.getWidth() != this.dataSetWidth || img.getHeight() != this.dataSetHeight){
                        throw new AssertionError();
                    }
                    int[] aFaceFeaturesHist = new FaceTracer(img).getLBPHistogram();
                    faceClassHistograms.add(aFaceFeaturesHist);
                }
                this.dataSetHistograms.put(aClass, faceClassHistograms);
                System.out.println(aClass + " computation completed");
            }
            else{
                System.err.println(dirClass + " not a folder");
            }
        }
    }

    static double getEuclideanDist(int[] hist1, int[] hist2){
        double dist = 0;
        for (int j = 0; j < hist1.length; j++) { //aq
            dist += (Math.pow(hist1[j] - hist2[j], 2));
        }
        dist = Math.sqrt(dist);
        return dist;
    }

    public HashMap<String, Integer> kNNClassification(String facePath, int k) {

        int[] faceFeatures;
        Bitmap faceImage;

        faceImage = BitmapFactory.decodeFile(facePath);
        if (faceImage.getWidth() != this.dataSetWidth || faceImage.getHeight() != this.dataSetHeight)
            throw new AssertionError("image size should be (WxH) : " + this.dataSetWidth + "x" + this.dataSetHeight);

        FaceTracer faceFeaturesObj = new FaceTracer(faceImage);
        faceFeatures = faceFeaturesObj.getLBPHistogram();

        ArrayList<PairV> allEucDistances = new ArrayList<>();
        for (String c : this.dataSetHistograms.keySet()){
            ArrayList<int[]> classHistograms = this.dataSetHistograms.get(c);
            for(int[] hist : classHistograms){
                double eucDist = getEuclideanDist(hist, faceFeatures);
                allEucDistances.add(new PairV(c, eucDist));
            }
        }

        allEucDistances.sort(new Comparator<PairV>() {
            public int compare(PairV o1, PairV o2) {
                return Double.compare(o1.second, o2.second);
            }
        });

        HashMap<String, Integer> kNN = new HashMap<>();

        for (int i = 0; i < k; i++) {
            String key = allEucDistances.get(i).first;
            if(kNN.containsKey(key)){
                kNN.put(key, kNN.get(key) + 1);
            }
            else{
                kNN.put(key, 1);
            }
        }

        return kNN;
    }

    public HashMap<String, ArrayList<int[]>> getClassesFeatures(){
        return this.dataSetHistograms;
    }
}
