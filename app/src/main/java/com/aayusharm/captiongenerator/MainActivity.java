package com.aayusharm.captiongenerator;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {
    TextView tv;
    ImageView img;
    Bitmap bitmap;
    String[] params = new String[]{"post_url"};
    Uri imageUri;
    File storageDir, image;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        Button btn1 = findViewById(R.id.btnClick1);
        Button btn2 = findViewById(R.id.btnClick2);
        img = findViewById(R.id.imageView);
        tv = findViewById(R.id.tv);
        final Context context = this;

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.INTERNET)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.INTERNET},
                    0);

        }

        if(android.os.Build.VERSION.SDK_INT < 19 && ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED){
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        1);
        }

        btn1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                try {
                image = File.createTempFile(
                        "image",
                        ".jpg",
                        storageDir);
                } catch (IOException ex) {
                    Log.d("FILE IO EXCEPTION", "Error creating temp image file.");
                }

                if (image != null) {
                    imageUri = FileProvider.getUriForFile(context,
                            "com.aayusharm.captiongenerator.fileprovider",
                            image);
                    intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);

                    startActivityForResult(intent, 0);
                }
            }
        });

        btn2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_PICK,
                        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

                startActivityForResult(intent, 1);
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {

            if(requestCode == 0){
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    // permission was granted
                } else {
                    ActivityCompat.shouldShowRequestPermissionRationale(this,
                            Manifest.permission.INTERNET);

                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.INTERNET},
                            0);
                }
                return;
            }
            else if(requestCode == 1) {
                if (grantResults.length > 0
                        && grantResults[1] == PackageManager.PERMISSION_GRANTED){
                    // permission was granted
                } else {
                    ActivityCompat.shouldShowRequestPermissionRationale(this,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE);

                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            1);
                }
            }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            tv = findViewById(R.id.tv);
            if (requestCode == 1)
                imageUri = data.getData();
            try {
                bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
            } catch (IOException e) {
                Log.d("URI ERROR", "Error resolving image URI.");
            }

            img.setImageBitmap(bitmap);
            tv.setText("");
            SendHttpRequestTask t = new SendHttpRequestTask(this);
            t.execute(params);
        }
    }

    private class SendHttpRequestTask extends AsyncTask<String, Void, String> {

        ProgressDialog dialog;
        SendHttpRequestTask(MainActivity mainActivity) {
            dialog = new ProgressDialog(mainActivity);
            dialog.setCancelable(false);
        }

        @Override
        protected void onPreExecute() {
            dialog.setMessage("Uploading...");
            dialog.show();
        }

        @Override
        protected String doInBackground(String... params) {
            String url = params[0];
            String response = "";

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);
            try {
                HttpClient client = new HttpClient(url);
                client.connectForMultipart();
                client.addFilePart("file", "file", baos.toByteArray());
                client.finishMultipart();
                response = client.getResponse();
            } catch (Throwable t) {
                t.printStackTrace();
            }
            return response;
        }

        @Override
        protected void onPostExecute(String response) {
            tv.setText(response);
            if (dialog.isShowing()) {
                dialog.dismiss();
            }
        }

    }

}
