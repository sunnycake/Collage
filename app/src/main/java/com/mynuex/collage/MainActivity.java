package com.mynuex.collage;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "MAIN_ACTIVITY";

    private ImageButton mImageButton1, mImageButton2, mImageButton3, mImageButton4;

    private List<ImageButton> mImageButtons;
    private ArrayList<String> mImageFilePaths;

    private String mCurrentImagePath;

    private final static String BUNDLE_KEY_IMAGE_FILE_PATHS = "bundle key image file paths";
    private final static String BUNDLE_KEY_MOST_RECENT_FILE_PATH = "bundle key most recent file path";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mImageButton1 = findViewById(R.id.imageButton1);
        mImageButton2 = findViewById(R.id.imageButton2);
        mImageButton3 = findViewById(R.id.imageButton3);
        mImageButton4 = findViewById(R.id.imageButton4);

        mImageButtons = new ArrayList<>(Arrays.asList(mImageButton1, mImageButton2, mImageButton3, mImageButton4));

        for (ImageButton button : mImageButtons) {
            button.setOnClickListener(this);
        }

        if (savedInstanceState != null) {
            mImageFilePaths = savedInstanceState.getStringArrayList(BUNDLE_KEY_IMAGE_FILE_PATHS);
            mCurrentImagePath = savedInstanceState.getString(BUNDLE_KEY_MOST_RECENT_FILE_PATH);
        }

        if (mCurrentImagePath == null) {
            mCurrentImagePath = "";
        }

        if (mImageFilePaths == null) {
            mImageFilePaths = new ArrayList<>(Arrays.asList("", "", "", ""));
        }

    }
    // Save image to file path
    @Override
    public void onSaveInstanceState(Bundle outBundle) {
        super.onSaveInstanceState(outBundle);
        outBundle.putString(BUNDLE_KEY_MOST_RECENT_FILE_PATH, mCurrentImagePath);
        outBundle.putStringArrayList(BUNDLE_KEY_IMAGE_FILE_PATHS, mImageFilePaths);
    }

//        for (int index = 0; index < mImageFilePaths.size() ; index++) {
//            String imageFilePath = mImageFilePaths.get(index);
//            if (imageFilePath != null) {
//                loadImage(index);
//            }
//        }
//    }


    @Override
    public void onClick(View view) {

        // Use the position in the ArrayList as the request code
        // When the image capture Activity returns,
        // can read the request code and work out which ImageButton was clicked.
        int requestCodeButtonIndex = mImageButtons.indexOf(view);

        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            try {
                File imageFile = createImageFile();
                if (imageFile != null) {
                    Uri imageURI = FileProvider.getUriForFile(this, "com.mynuex.collage.fileprovider", imageFile);
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageURI);
                    startActivityForResult(takePictureIntent, requestCodeButtonIndex);
                } else {
                    Log.e(TAG, "Image file is null");
                }
            } catch (IOException e) {
                Log.e(TAG, "Error creating image file " + e);
            }
        }
    }

    private File createImageFile() throws IOException {
        // Create unique filename with timestamp
        String imageFilename = "COLLAGE_" + new Date().getTime();
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File imageFile = File.createTempFile(
                imageFilename,
                ".jpg",
                storageDir
        );

        // Save the file path globally, when the take picture intent returns
        // this location will be where the image is saved
        mCurrentImagePath = imageFile.getAbsolutePath();
        return imageFile;
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            Log.d(TAG, "onActivityResult for code " + requestCode + " and current path " + mCurrentImagePath);
            mImageFilePaths.set(requestCode, mCurrentImagePath);    // Save the path in mImageFilePaths
            requestSaveImageToMediaStore();
        }

        else if (resultCode == RESULT_CANCELED) {
            mCurrentImagePath = "";

        }
    }


    @Override
    public void onWindowFocusChanged(boolean hasFocus) {

        // The view hasn't loaded in onActivityResult if the device
        // is rotated when the picture is taken
        // onWindowFocusedChanged is called after onActivityResult
        // and the view has loaded, so override this
        // method to display the images.

        Log.d(TAG, "focus changed " + hasFocus);
        if (hasFocus) {
            for (int index = 0; index < mImageButtons.size() ; index++) {
                loadImage(index);
            }
        }
    }


    private void loadImage(int index) {

        ImageButton imageButton = mImageButtons.get(index);
        // Pass index used to fetch path
        String path = mImageFilePaths.get(index);

        if (path != null && !path.isEmpty()) {
            Picasso.get()
                    .load(new File(path))
                    .error(android.R.drawable.stat_notify_error) // built-in error icon
                    .fit()
                    .centerCrop()
                    .into(imageButton, new Callback() {
                        @Override
                        public void onSuccess() {
                            Log.d(TAG, "Image loaded");
                        }

                        // Troubleshoot: if images are not loading
                        // add callback to into() method and check logs
                        @Override
                        public void onError(Exception e) {
                            Log.e(TAG, "error loading image", e);
                        }
                    });
        }
    }

    // Methods to request permission from user before inserting
    private void requestSaveImageToMediaStore() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                PackageManager.PERMISSION_GRANTED) {
            saveImage();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 100);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults){
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            saveImage();
        } else {
            Toast.makeText(this, "Images will NOT be saved to media store", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveImage() {
        try {
            MediaStore.Images.Media.insertImage(getContentResolver(), mCurrentImagePath, "Collage", "Collage");
        } catch (IOException e) {
            Log.e(TAG, "Image file not found", e);
        }
    }
}