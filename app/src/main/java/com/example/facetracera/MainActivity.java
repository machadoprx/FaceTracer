package com.example.facetracera;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.os.Bundle;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.FirebaseApp;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.face.FirebaseVisionFace;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    public static int PICK_IMAGE_FOR_VIEW_1 = 0;
    public static int PICK_IMAGE_FOR_VIEW_2 = 1;

    private static int[] face1_hist;
    private static int[] face2_hist;


    public void pickImage(int view) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        startActivityForResult(intent, view);
    }

    @Override
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
                                    Rect bound = face.getBoundingBox();
                                    Bitmap originalBitmap = img.getBitmap();
                                    Bitmap boundBitmap = Bitmap.createBitmap(originalBitmap, bound.centerX() - (bound.width() / 2), bound.centerY() - (bound.height() / 2), bound.width(), bound.height());
                                    Bitmap scaledBitmap = Bitmap.createScaledBitmap(boundBitmap, bound.width(), bound.height(), true); // 70x70
                                    Bitmap croppedBitmap = FaceTransform.getCroppedBitmap(scaledBitmap);
                                    Bitmap rotateBitmap = FaceTransform.rotateBitmap(croppedBitmap, rotZ);
                                    Bitmap image = Bitmap.createScaledBitmap(rotateBitmap, 48, 48, true);

                                    ImageView view;
                                    if(reqC == PICK_IMAGE_FOR_VIEW_1){
                                        face1_hist = new FaceTracer(image).getLBPHistogram();
                                        view = findViewById(R.id.imageView);
                                    }
                                    else{
                                        face2_hist = new FaceTracer(image).getLBPHistogram();
                                        view = findViewById(R.id.imageView2);
                                    }
                                    Bitmap viewBmp = Bitmap.createScaledBitmap(image, 200, 200, true);
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
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Button setImgView = findViewById(R.id.button);
        setImgView.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                pickImage(PICK_IMAGE_FOR_VIEW_1);
            }
        });

        Button setImgView2 = findViewById(R.id.button2);
        setImgView2.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                pickImage(PICK_IMAGE_FOR_VIEW_2);
            }
        });

        Button compareViews = findViewById(R.id.button3);
        compareViews.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                if(face1_hist != null && face2_hist != null){
                    double dist = FaceClassifier.getEuclideanDist(face1_hist, face2_hist);
                    TextView res = findViewById(R.id.textView);
                    res.setText(String.format(Locale.ENGLISH,"isso aq eh parecido: %f", dist));
                }
                else{
                    Toast t = Toast.makeText(getApplicationContext(),
                            "nao selecionou as parada kraio",
                            Toast.LENGTH_SHORT);
                    t.show();
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
