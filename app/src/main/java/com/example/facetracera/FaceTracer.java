package com.example.facetracera;

import android.graphics.Bitmap;
import android.graphics.Color;

import java.util.Arrays;

class FaceTracer {
    private Bitmap CompFace;
    private int[] binPatterns = {0, 1, 2, 3, 4, 6, 7, 8, 12, 14, 15, 16, 24, 28, 30, 31, 32, 48, 56, 60, 62, 63,
            64, 96, 112, 120, 124, 126, 127, 128, 129, 131, 135, 143, 159, 191, 192, 193,
            195, 199, 207, 223, 224, 225, 227, 231, 239, 240, 241, 243, 247, 248, 249, 251,
            252, 253, 254, 255};
    private int width;
    private int height;

    FaceTracer(Bitmap realImage) {
        this.CompFace = realImage;
        this.width = realImage.getWidth();
        this.height = realImage.getHeight();
    }

    private double getGrayScalePixel(int x, int y) {
        int color = this.CompFace.getPixel(x, y);
        double blue = Color.blue(color);
        double green = Color.green(color);
        double red = Color.red(color);
        return (red * 0.299) + (green * 0.587) + (blue * 0.114); //luma encoding
    }

    private int getLBPPixel(int x, int y, int radius) {
        double threshold = getGrayScalePixel(x, y);
        int newPixel = 0;
        for (int i = 0; i < 8; i++) {
            double gxi = x + (radius * Math.cos(2 * Math.PI * i / 8));
            double gyi = y - (radius * Math.sin(2 * Math.PI * i / 8));
            int xi = (int)Math.floor(gxi);
            int yi = (int)Math.floor(gyi);

            double iPixel;
            double i1 = (xi + 1 - gxi) * getGrayScalePixel(xi, yi);
            i1 += (gxi - xi) * getGrayScalePixel(xi + 1, yi);
            double i2 = (xi + 1 - gxi) * getGrayScalePixel(xi, yi + 1);
            i2 += (gxi - xi) * getGrayScalePixel(xi + 1, yi + 1);
            iPixel = (yi + 1 - gyi) * i1;
            iPixel += (gyi - yi) * i2;

            newPixel += (iPixel >= threshold) ? Math.pow(2, i) : 0;
        }
        return newPixel;
    }

    private int binSearch(int[] arr, int key){
        int end = arr.length;
        int start = 0;
        while (start <= end){
            int mid = start + ((end - start) / 2);
            if (arr[mid] == key)
                return mid;
            else if (arr[mid] < key)
                start = mid + 1;
            else
                end = mid - 1;
        }
        return -1;
    }

    private int[] getPixelsArray(int radius){
        int[] pixelsArray = new int[this.width * this.height];
        Arrays.fill(pixelsArray, -1);
        // array[width * y + x] = value;
        for (int y = radius; y < this.height - radius - 1; y++) {
            for (int x = radius; x < this.width - radius - 1; x++) {
                pixelsArray[(this.width * y) + x] = getLBPPixel(x, y, radius);
            }
        }
        return pixelsArray;
    }

    private void fillGridHistogram(int[] pixelsArray, int startX, int startY, int endX,
                                   int endY, int grid, int offset, int[] hist){
        int gridVector = 59;
        for (int y = startY; y <= endY; y++) {
            for (int x = startX; x <= endX; x++) {
                int newPixel = pixelsArray[(this.width * y) + x];
                if(newPixel == -1)
                    continue;
                int featureIndex = binSearch(this.binPatterns, newPixel);
                hist[(grid * gridVector) + (featureIndex + 1) + offset]++;
            }
        }
    }

    private int fillHistogram(int blockWidth, int blockHeight, int radius, int offset, int[] histogram) {

        int blocksX = (int)Math.floor((2 * (double)this.width / (double)blockWidth)) - 1;
        int blocksY = (int)Math.floor((2 * (double)this.height / (double)blockHeight)) - 1;
        int startX = 0, startY = 0, grid = 0;

        int[] pixelsArray = getPixelsArray(radius);

        for (int j = 0; j < blocksY; j++) {
            int endY = startY + blockHeight;
            for (int i = 0; i < blocksX; i++) {
                int endX = startX + blockWidth;
                //Log.wtf("xama", String.format("%d %d %d %d", startX, endX, startY, endY));
                fillGridHistogram(pixelsArray, startX, startY, endX, endY, grid, offset, histogram);
                startX += (blockWidth / 2);
                grid++;
            }
            startY += (blockHeight / 2);
            startX = 0;
        }

        return blocksX * blocksY * 59;
    }

    int[] getOCLBPHistogram() {
        int featuresDimension = 0, offset = 0;
        int[] blocksDim = {10, 14, 18};

        for (int dim : blocksDim){
            int blocksX = (int)Math.floor((2 * (double)this.width / (double)dim)) - 1;
            int blocksY = (int)Math.floor((2 * (double)this.height / (double)dim)) - 1;
            featuresDimension += blocksX * blocksY;
        }
        int[] hist = new int[featuresDimension * 59];

        offset += this.fillHistogram(10, 10, 1, 0, hist);
        offset += this.fillHistogram(14, 14, 2, offset, hist);
        this.fillHistogram(18, 18, 3, offset, hist);
        //Log.wtf("xama", String.format("%d", featuresDimension * 59));
        return hist;
    }
}
