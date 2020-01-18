package com.example.facetracera;

import android.content.ClipData;
import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import com.google.firebase.FirebaseApp;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.face.FirebaseVisionFace;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.os.ParcelFileDescriptor;
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
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    public static int PICK_MULTIPLE_IMAGES = 0;
    public static int PICK_PREDICT_IMAGE = 1;
    public static int dataAvailable = 0;
    private static boolean personAddBusy = false;
    private static int[] predictHistBuffer;
    private FaceClassifier faceClassifier;

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

        assert parcelFileDescriptor != null;
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
            EditText personNameRes = findViewById(R.id.editText);
            String personName = personNameRes.getText().toString();
            final ArrayList<Uri> uris = new ArrayList<>();
            final ArrayList<int[]> personHists = new ArrayList<>();
            try {
                FirebaseVisionFaceDetectorOptions options =
                        new FirebaseVisionFaceDetectorOptions.Builder()
                                .setClassificationMode(FirebaseVisionFaceDetectorOptions.ACCURATE)
                                .setLandmarkMode(FirebaseVisionFaceDetectorOptions.NO_LANDMARKS)
                                .setClassificationMode(FirebaseVisionFaceDetectorOptions.NO_CLASSIFICATIONS)
                                .setMinFaceSize(0.15f)
                                .build();

                if (clipData == null) {
                    uris.add(data.getData());
                }
                else {
                    for (int i = 0; i < clipData.getItemCount(); i++)
                        uris.add(clipData.getItemAt(i).getUri());
                }
                for (int i = 0; i < uris.size(); i++) {
                    Bitmap image = getBitmapFromUri(getContentResolver(), uris.get(i));
                    final int index = i;
                    final FirebaseVisionImage img = FirebaseVisionImage.fromBitmap(image);
                    FirebaseVision.getInstance().getVisionFaceDetector(options).detectInImage(img)
                            .addOnSuccessListener(
                                    faces -> {
                                        if (faces.size() == 0)
                                            return;
                                        FirebaseVisionFace face = faces.get(0);
                                        float rotZ = face.getHeadEulerAngleZ();
                                        Rect faceBound = face.getBoundingBox();
                                        Bitmap faceBitmap = FaceTransform.processImage(img.getBitmap(), faceBound, 96, 96, rotZ);
                                        personHists.add(new FaceTracer(faceBitmap).getOCLBPHistogram());
                                        if(requestCode == PICK_PREDICT_IMAGE){
                                            predictHistBuffer = personHists.get(0);
                                            ImageView view = findViewById(R.id.imageView);
                                            Bitmap viewBmp = Bitmap.createScaledBitmap(faceBitmap, 350, 350, false);
                                            view.setImageBitmap(viewBmp);
                                        }
                                        else{
                                            dataAvailable++;
                                            Toast.makeText(getApplicationContext(), String.format(Locale.ENGLISH, "%d %s done", index + 1, personName), Toast.LENGTH_SHORT).show();
                                        }
                                    });
                }
            }
            catch (Exception ignore){}
            if(requestCode == PICK_MULTIPLE_IMAGES)
                faceClassifier.addPerson(personName, personHists);
            personAddBusy = false;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        faceClassifier = new FaceClassifier();

        Button addPerson = findViewById(R.id.button4);
        addPerson.setOnClickListener(view -> {
            if(!personAddBusy){
                EditText personNameRes = findViewById(R.id.editText);
                String personName = personNameRes.getText().toString();
                if(faceClassifier.getClassesFeatures().containsKey(personName)){
                    Toast.makeText(getApplicationContext(), "Person already exists", Toast.LENGTH_LONG).show();
                    return;
                }
                selectMultipleImages(PICK_MULTIPLE_IMAGES);
            }
            else{
                Toast.makeText(getApplicationContext(), "calma ae", Toast.LENGTH_LONG).show();
            }
        });

        Button addPredictFace = findViewById(R.id.button);
        addPredictFace.setOnClickListener(view -> {
            if(!personAddBusy){
                selectMultipleImages(PICK_PREDICT_IMAGE);
            }
            else{
                Toast.makeText(getApplicationContext(), "calma ae", Toast.LENGTH_LONG).show();
            }
        });

        final Button predictFace = findViewById(R.id.button3);
        predictFace.setOnClickListener(view -> {
            if(predictHistBuffer != null && !personAddBusy){
                EditText kRes = findViewById(R.id.editText2);
                int k = Integer.parseInt(kRes.getText().toString());
                if(dataAvailable < k){
                    Toast.makeText(getApplicationContext(), "There is not enough data", Toast.LENGTH_LONG).show();
                    return;
                }
                HashMap<String, Integer> kNN = faceClassifier.kNNClassification(predictHistBuffer, k);
                TextView text = findViewById(R.id.textView);
                StringBuilder results = new StringBuilder();
                for(String key : kNN.keySet()){
                    results.append(key).append(": ").append(kNN.get(key)).append(" ");
                }
                text.setText(results.toString());
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
