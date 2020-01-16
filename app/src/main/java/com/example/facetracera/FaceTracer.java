package com.example.facetracera;

import android.graphics.Bitmap;
import android.graphics.Color;
import java.util.Arrays;

class FaceTracer {
    private Bitmap CompFace;
    private static int[] binPatterns = {0, 1, 2, 3, 4, 6, 7, 8, 12, 14, 15, 16, 24, 28, 30, 31, 32, 48, 56, 60, 62, 63,
            64, 96, 112, 120, 124, 126, 127, 128, 129, 131, 135, 143, 159, 191, 192, 193,
            195, 199, 207, 223, 224, 225, 227, 231, 239, 240, 241, 243, 247, 248, 249, 251,
            252, 253, 254, 255};
    private static int gridVector = 59;

    FaceTracer(Bitmap realImage) {
        this.CompFace = realImage;
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
            if (gxi % 1 != 0 || gyi % 1 != 0){
                double i1 = (xi + 1 - gxi) * getGrayScalePixel(xi, yi);
                i1 += (gxi - xi) * getGrayScalePixel(xi + 1, yi);
                double i2 = (xi + 1 - gxi) * getGrayScalePixel(xi, yi + 1);
                i2 += (gxi - xi) * getGrayScalePixel(xi + 1, yi + 1);
                iPixel = (yi + 1 - gyi) * i1;
                iPixel += (gyi - yi) * i2;
            }
            else {
                iPixel = getGrayScalePixel(xi, yi);
            }
            newPixel += (iPixel >= threshold) ? Math.pow(2, i) : 0;
        }
        return newPixel;
    }

    private void fillGridHistogram(int startX, int startY, int endX, int endY, int radius,
                                   int grid, int offset, int[] hist){
        for (int y = startY; y <= endY; y++) {
            for (int x = startX; x <= endX; x++) {
                if (x + radius + 1 >= this.CompFace.getWidth()
                    || y + radius + 1 >= this.CompFace.getHeight()
                    || x - radius < 0
                    || y - radius < 0)
                    continue;

                int newPixel = getLBPPixel(x, y, radius);
                int featureIndex = Arrays.binarySearch(FaceTracer.binPatterns, newPixel);

                if (featureIndex < 0)
                    hist[(grid * FaceTracer.gridVector) + offset]++;
                else
                    hist[(grid * FaceTracer.gridVector) + (featureIndex + 1) + offset]++;
            }
        }
    }

    private void fillHistogram(int blocksX, int blocksY, int radius, int offset, int[] histogram) {

        int imageWidth = this.CompFace.getWidth();
        int imageHeight = this.CompFace.getHeight();
        int blockWidth = (2 * imageWidth) / (blocksX + 1);
        int blockHeight = (2 * imageHeight) / (blocksY + 1);
        int startX = 0, startY = 0;

        for (int j = 0; j < blocksY; j++) {
            for (int i = 0; i < blocksX; i++) {
                fillGridHistogram(startX, startY, startX + blockWidth,
                        startY + blockHeight, radius,i + j, offset, histogram);
                startX += (blockWidth / 2);
            }
            startY += (blockHeight / 2);
            startX = 0;
        }
    }

    int[] getLBPHistogram() {

        int[] LBPH = new int[36580];

        this.fillHistogram(10, 10, 1, 0, LBPH);
        this.fillHistogram(14, 14, 2, 5900, LBPH);
        this.fillHistogram(18, 18, 3, 17464, LBPH);

        return LBPH;
    }
}
