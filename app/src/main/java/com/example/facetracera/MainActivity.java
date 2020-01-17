package com.example.facetracera;

import android.content.ClipData;
import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.FirebaseApp;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.face.FirebaseVisionFace;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.os.ParcelFileDescriptor;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.FileDescriptor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    public static int PICK_MULTIPLE_IMAGES = 0;
    public static int PICK_PREDICT_IMAGE = 1;
    private static boolean personAddBusy = false;
    private static int[] predictHistBuffer;
    private static FaceClassifier faceClassifier;

    public void selectMultipleImages(int requestCode) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.setType("image/*");
        startActivityForResult(intent, requestCode);
    }

    private Bitmap getBitmapFromUri(ContentResolver contentResolver, Uri uri) throws Exception{
        ParcelFileDescriptor parcelFileDescriptor =
                contentResolver.openFileDescriptor(uri, "r");
        FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
        Bitmap bitmap = BitmapFactory.decodeFileDescriptor(fileDescriptor);
        parcelFileDescriptor.close();

        return bitmap;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            personAddBusy = true;
            FirebaseApp.getApps(getApplicationContext());
            ClipData clipData = data.getClipData();
            final ArrayList<int[]> personHists = new ArrayList<>();
            EditText personNameRes = findViewById(R.id.editText);
            String personName = personNameRes.getText().toString();

            try {
                FirebaseVisionFaceDetectorOptions options =
                        new FirebaseVisionFaceDetectorOptions.Builder()
                                .setClassificationMode(FirebaseVisionFaceDetectorOptions.ACCURATE)
                                .setLandmarkMode(FirebaseVisionFaceDetectorOptions.NO_LANDMARKS)
                                .setClassificationMode(FirebaseVisionFaceDetectorOptions.NO_CLASSIFICATIONS)
                                .setMinFaceSize(0.15f)
                                .build();

                ArrayList<Uri> uris = new ArrayList<>();

                if (clipData == null) {
                    if(requestCode == PICK_PREDICT_IMAGE){
                        Bitmap image = getBitmapFromUri(getContentResolver(), data.getData());
                        final FirebaseVisionImage img = FirebaseVisionImage.fromBitmap(image);
                        FirebaseVision.getInstance().getVisionFaceDetector(options).detectInImage(img)
                            .addOnSuccessListener(
                                new OnSuccessListener<List<FirebaseVisionFace>>() {
                                    @Override
                                    public void onSuccess(List<FirebaseVisionFace> faces) {
                                        if (faces.size() == 0)
                                            return;
                                        FirebaseVisionFace face = faces.get(0);
                                        float rotZ = face.getHeadEulerAngleZ();
                                        Rect faceBound = face.getBoundingBox();
                                        Bitmap faceBitmap = FaceTransform.processImage(img.getBitmap(), faceBound, 96, 96, rotZ);
                                        predictHistBuffer = new FaceTracer(faceBitmap).getOCLBPHistogram();
                                        ImageView view = findViewById(R.id.imageView);
                                        Bitmap viewBmp = Bitmap.createScaledBitmap(faceBitmap, 350, 350, false);
                                        view.setImageBitmap(viewBmp);
                                    }
                                });
                        personAddBusy = false;
                        return;
                    }
                    uris.add(data.getData());
                }
                else {
                    for (int i = 0; i < clipData.getItemCount(); i++)
                        uris.add(clipData.getItemAt(i).getUri());
                }
                int index = 1;
                for (Uri faceUri : uris) {
                    Bitmap image = getBitmapFromUri(getContentResolver(), faceUri);
                    final FirebaseVisionImage img = FirebaseVisionImage.fromBitmap(image);
                    FirebaseVision.getInstance().getVisionFaceDetector(options).detectInImage(img)
                        .addOnSuccessListener(
                            new OnSuccessListener<List<FirebaseVisionFace>>() {
                                @Override
                                public void onSuccess(List<FirebaseVisionFace> faces) {
                                    if (faces.size() == 0)
                                        return;
                                    FirebaseVisionFace face = faces.get(0);
                                    float rotZ = face.getHeadEulerAngleZ();
                                    Rect faceBound = face.getBoundingBox();
                                    Bitmap faceBitmap = FaceTransform.processImage(img.getBitmap(), faceBound, 96, 96, rotZ);
                                    personHists.add(new FaceTracer(faceBitmap).getOCLBPHistogram());
                                }
                            });

                    Toast.makeText(getApplicationContext(), String.format(Locale.ENGLISH, "%d %s face added", index, personName), Toast.LENGTH_LONG).show();
                    index++;
                }
                faceClassifier.addPerson(personName, personHists);
            }
            catch (Exception ignore){}
            personAddBusy = false;
        }
    }

    /*@Override
    public void onActivityResult(int requestCode, int resultCode, Intent data){
        if(resultCode == RESULT_OK) {
            if ((requestCode == PICK_IMAGE_FOR_VIEW_1 || requestCode == PICK_IMAGE_FOR_VIEW_2)) {
                if (data == null || data.getData() == null) {
                    Toast.makeText(getApplicationContext(),
                            "Essa imagem n presta",
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                try {
                    FirebaseApp.getApps(getApplicationContext());
                    InputStream imgStream = getApplicationContext().getContentResolver().openInputStream(data.getData());
                    Bitmap temp = BitmapFactory.decodeStream(imgStream);
                    final FirebaseVisionImage img = FirebaseVisionImage.fromBitmap(temp);

                    FirebaseVisionFaceDetectorOptions options =
                            new FirebaseVisionFaceDetectorOptions.Builder()
                                    .setClassificationMode(FirebaseVisionFaceDetectorOptions.ACCURATE)
                                    .setLandmarkMode(FirebaseVisionFaceDetectorOptions.NO_LANDMARKS)
                                    .setClassificationMode(FirebaseVisionFaceDetectorOptions.NO_CLASSIFICATIONS)
                                    .setMinFaceSize(0.15f)
                                    .build();
                    final int reqC = requestCode;
                    FirebaseVision.getInstance().getVisionFaceDetector(options).detectInImage(img)
                        .addOnSuccessListener(
                            new OnSuccessListener<List<FirebaseVisionFace>>() {
                                @Override
                                public void onSuccess(List<FirebaseVisionFace> faces) {
                                    if(faces.size() == 0)
                                        return;
                                    FirebaseVisionFace face = faces.get(0);
                                    float rotZ = face.getHeadEulerAngleZ();
                                    Bitmap originalBitmap = img.getBitmap();
                                    Rect faceBound = face.getBoundingBox();
                                    Bitmap boundBitmap = Bitmap.createBitmap(originalBitmap,
                                            faceBound.centerX() - (faceBound.width() / 2),
                                            faceBound.centerY() - (faceBound.height() / 2),
                                            faceBound.width(), faceBound.height());

                                    Bitmap scaledBitmap = Bitmap.createScaledBitmap(boundBitmap, faceBound.width(), faceBound.height(), false); // 70x70
                                    Bitmap croppedBitmap = FaceTransform.getCroppedBitmap(scaledBitmap);
                                    Bitmap rotatedBitmap = FaceTransform.rotateBitmap(croppedBitmap, rotZ);
                                    Bitmap image = Bitmap.createScaledBitmap(rotatedBitmap, 96, 96, true);

                                    ImageView view;
                                    if(reqC == PICK_IMAGE_FOR_VIEW_1){
                                        face1_hist = new FaceTracer(image).getOCLBPHistogram();
                                        view = findViewById(R.id.imageView);
                                    }
                                    else{
                                        face2_hist = new FaceTracer(image).getOCLBPHistogram();
                                        view = findViewById(R.id.imageView2);
                                    }
                                    Bitmap viewBmp = Bitmap.createScaledBitmap(rotatedBitmap, 350, 350, false);
                                    view.setImageBitmap(viewBmp);
                                }
                            })
                        .addOnFailureListener(
                            new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {

                                }
                            });
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }*/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        faceClassifier = new FaceClassifier();

        Button addPerson = findViewById(R.id.button4);
        addPerson.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                if(!personAddBusy){
                    selectMultipleImages(PICK_MULTIPLE_IMAGES);
                }
                else{
                    Toast.makeText(getApplicationContext(), "calma ae", Toast.LENGTH_LONG).show();
                }
            }
        });

        Button addPredictFace = findViewById(R.id.button);
        addPredictFace.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                if(!personAddBusy){
                    selectMultipleImages(PICK_PREDICT_IMAGE);
                }
                else{
                    Toast.makeText(getApplicationContext(), "calma ae", Toast.LENGTH_LONG).show();
                }
            }
        });

        final Button predictFace = findViewById(R.id.button3);
        predictFace.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                if(predictHistBuffer != null && !personAddBusy){
                    HashMap<String, Integer> kNN = faceClassifier.kNNClassification(predictHistBuffer, 3);
                    TextView text = findViewById(R.id.textView);
                    String results = "";
                    for (String key : kNN.keySet()){
                        results += key + ": " + kNN.get(key) + " | ";
                    }
                    text.setText(results);
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
