package com.example.facetracera;

import java.util.ArrayList;
import java.util.HashMap;

class FaceClassifier {

    static class PairV{
        String first;
        double second;

        PairV(String first, double second){
            this.first = first;
            this.second = second;
        }
    }

    private HashMap<String, ArrayList<int[]>> dataSetHistograms;

    FaceClassifier() {
        this.dataSetHistograms = new HashMap<>();
    }

    void addPerson(String label, ArrayList<int[]> histograms) {
        dataSetHistograms.put(label, histograms);
    }

    HashMap<String, Integer> kNNClassification(int[] faceFeatures, int k) {
        ArrayList<PairV> similarity = new ArrayList<>();
        for (String label : dataSetHistograms.keySet()){
            ArrayList<int[]> classHistograms = dataSetHistograms.get(label);
            assert classHistograms != null;
            for(int[] hist : classHistograms){
                double cosineSim = -getCosineSim(hist, faceFeatures);
                similarity.add(new PairV(label, cosineSim));
            }
        }

        similarity.sort((o1, o2) -> Double.compare(o1.second, o2.second));

        HashMap<String, Integer> kNN = new HashMap<>();
        for (int i = 0; i < k; i++) {
            String key = similarity.get(i).first;
            if(kNN.containsKey(key)){
                kNN.put(key, kNN.get(key) + 1);
            }
            else{
                kNN.put(key, 1);
            }
        }

        return kNN;
    }

    private double getEuclideanDist(int[] hist1, int[] hist2){
        if (hist1.length != hist2.length)
            return -1;
        double dist = 0;
        for (int j = 0; j < hist1.length; j++) { //aq
            dist += (Math.pow(hist1[j] - hist2[j], 2));
        }
        dist = Math.sqrt(dist);
        return dist;
    }

    private double getCosineSim(int[] hist1, int[] hist2){
        if (hist1.length != hist2.length)
            return -1;
        double num = 0;
        double den1 = 0;
        double den2 = 0;
        for (int i = 0; i < hist1.length; i++) { //aq
            num += hist1[i] * hist2[i];
            den1 += hist1[i] * hist1[i];
            den2 += hist2[i] * hist2[i];
        }
        den1 = Math.sqrt(den1);
        den2 = Math.sqrt(den2);
        return num / (den1 * den2);
    }

    HashMap<String, ArrayList<int[]>> getClassesFeatures(){
        return this.dataSetHistograms;
    }
}
