package com.example.vedeshkumar.myapplicationopencart;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private LinearLayout mMainView;
    private int mViewPositionTag = 0;

    private static final int RC_SAVE_PROFILE_PICTURE = 1;
    private static final int REQUEST_CAMERA = 4;
    int mClickedButtonTag;
    private final String twoHyphens = "--";
    private final String lineEnd = "\r\n";
    private final String boundary = "apiclient-" + System.currentTimeMillis();
    private final String mimeType = "multipart/form-data;boundary=" + boundary;
    Uri mUri;
    private byte[] multipartBody;
    private Bitmap bitmapImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bindView();
        initFirstLayoutAlwaysStays();
    }

    private void initFirstLayoutAlwaysStays() {
        View eachOptionLayout = LayoutInflater.from(MainActivity.this).inflate(R.layout.each_option_layout, mMainView, false);
        eachOptionLayout.setTag(mViewPositionTag);
        eachOptionLayout.findViewById(R.id.each_option_delete_option).setTag(mViewPositionTag);
        eachOptionLayout.findViewById(R.id.each_option_gallery_button).setTag(mViewPositionTag);
        eachOptionLayout.findViewById(R.id.each_option_camera_button).setTag(mViewPositionTag);

        mMainView.addView(eachOptionLayout);
        mViewPositionTag++;
    }

    private void bindView() {
        mMainView = (LinearLayout) findViewById(R.id.all_options);
    }

    public void onCLickOpenGallery(View view) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            mClickedButtonTag = (int) view.getTag();
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.setType("image/*");
            startActivityForResult(Intent.createChooser(intent, "Select File"), RC_SAVE_PROFILE_PICTURE);
        } else {
            mClickedButtonTag = (int) view.getTag();
            String[] permissions = {"android.permission.WRITE_EXTERNAL_STORAGE"};
            requestPermissions(permissions, RC_SAVE_PROFILE_PICTURE);
        }
    }

    public void onCLickOpenCamera(View view) {

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            mClickedButtonTag = (int) view.getTag();
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            mUri = Uri.fromFile(new File(Environment.getExternalStorageDirectory(), "IMG_" + String.valueOf(System.currentTimeMillis()) + ".jpg"));
            intent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, mUri);
            startActivityForResult(intent, REQUEST_CAMERA);
        } else {
            mClickedButtonTag = (int) view.getTag();
            String[] permissions = {"android.permission.WRITE_EXTERNAL_STORAGE"};
            requestPermissions(permissions, REQUEST_CAMERA);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                mUri = Uri.fromFile(new File(Environment.getExternalStorageDirectory(), "IMG_" + String.valueOf(System.currentTimeMillis()) + ".jpg"));
                intent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, mUri);
                startActivityForResult(intent, REQUEST_CAMERA);
            }
        } else if (requestCode == RC_SAVE_PROFILE_PICTURE) {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            mUri = Uri.fromFile(new File(Environment.getExternalStorageDirectory(), "imgnm_" + String.valueOf(System.currentTimeMillis()) + ".jpg"));
            intent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, mUri);
            intent.setType("image/*");
            startActivityForResult(Intent.createChooser(intent, "Select File"), RC_SAVE_PROFILE_PICTURE);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SAVE_PROFILE_PICTURE) {
            if (resultCode == Activity.RESULT_OK) {

                Uri uri = data.getData();
                String uriString = uri.toString();
                File myFile = new File(uriString);

                String displayName = null;

                if (uriString.startsWith("content://")) {
                    Cursor cursor = null;
                    try {
                        cursor = this.getContentResolver().query(uri, null, null, null, null);
                        if (cursor != null && cursor.moveToFirst()) {
                            displayName = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                        }
                    } finally {
                        cursor.close();
                    }
                } else if (uriString.startsWith("file://")) {
                    displayName = myFile.getName();
                }

                try {
                    Bitmap pickedImage = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                    setImageAndName(pickedImage, displayName);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else if (requestCode == REQUEST_CAMERA) {
            if (resultCode == Activity.RESULT_OK) {
                Bitmap clickedImage;
                String filename = "";
                String path = mUri.getPath();
                if (path.length() > 0) {
                    filename = path.substring(path.lastIndexOf("/") + 1, path.length());
                    clickedImage = BitmapFactory.decodeFile(path);
                    setImageAndName(clickedImage, filename);
                }
            }
        } else {
            Log.d("TAG", "Came here its null !");
            Toast.makeText(getApplicationContext(), "failed to get Image!", Toast.LENGTH_SHORT).show();
        }
    }

    public void setImageAndName(Bitmap clickedImage, String displayName) {
        ImageView eachOptionImage = (ImageView) mMainView.findViewById(R.id.all_options).findViewWithTag(mClickedButtonTag).findViewById(R.id.each_option_image_preview);
        eachOptionImage.setImageBitmap(clickedImage);
        bitmapImage = clickedImage;
        TextView eachOptionName = (TextView) mMainView.findViewById(R.id.all_options).findViewWithTag(mClickedButtonTag).findViewById(R.id.each_option_name_tv);
        eachOptionName.setText(displayName);
    }

    public void onCLickResetImageView(View view) {
        ImageView imageView = (ImageView) mMainView.findViewById(R.id.all_options).findViewWithTag(mClickedButtonTag).findViewById(R.id.each_option_image_preview);
        imageView.setImageBitmap(null);
        bitmapImage = null;
        TextView eachOptionName = (TextView) mMainView.findViewById(R.id.all_options).findViewWithTag(mClickedButtonTag).findViewById(R.id.each_option_name_tv);
        eachOptionName.setText("No File Selected");
    }

    public void UploadImages(View v){
        uploadImage(bitmapImage, "MY_IMG");
    }

    private void uploadImage(Bitmap thumbnailBitmap, String fileName) {
        if(thumbnailBitmap != null) {
            try {
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                if (thumbnailBitmap != null) {
                    thumbnailBitmap.compress(Bitmap.CompressFormat.JPEG, 90, byteArrayOutputStream);
                }

                byte[] fileByteArray = byteArrayOutputStream.toByteArray();
                ByteArrayOutputStream byteArrayOutputStream2 = new ByteArrayOutputStream();
                DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream2);
                try {
                    buildPart(dataOutputStream, fileByteArray, fileName, true);
                    dataOutputStream.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);
                    multipartBody = byteArrayOutputStream2.toByteArray();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                String url = "PUT_YOUR_IMAGE_SERVER_URL";
                Log.d("url", url + "");
                MultipartRequest multipartRequest = new MultipartRequest(Request.Method.POST, url, null, mimeType, multipartBody, new Response.Listener<NetworkResponse>() {
                    @Override
                    public void onResponse(NetworkResponse response) {
                        Log.d("Response", response + "");
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(getApplicationContext(), "Try Again", Toast.LENGTH_SHORT).show();
                    }
                });
                MySingleton.getInstance(this).addToRequestQueue(multipartRequest);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        else
            Toast.makeText(this, "Please select your image", Toast.LENGTH_SHORT).show();
    }

    private void buildPart(DataOutputStream dataOutputStream, byte[] fileData, String fileName, boolean mainImage) throws IOException {
        dataOutputStream.writeBytes(twoHyphens + boundary + lineEnd);
        if (mainImage)
            dataOutputStream.writeBytes("Content-Disposition: form-data; name=\"file_product_main_image_detailed[]\"; filename=\""
                    + fileName + "\"" + lineEnd);
        else
            dataOutputStream.writeBytes("Content-Disposition: form-data; name=\"images[]\"; filename=\""
                    + fileName + "\"" + lineEnd);

        dataOutputStream.writeBytes(lineEnd);

        ByteArrayInputStream fileInputStream = new ByteArrayInputStream(fileData);
        int bytesAvailable = fileInputStream.available();

        int maxBufferSize = 2024 * 2024;
        int bufferSize = Math.min(bytesAvailable, maxBufferSize);
        byte[] buffer = new byte[bufferSize];

        // read file and write it into form...
        int bytesRead = fileInputStream.read(buffer, 0, bufferSize);

        while (bytesRead > 0) {
            dataOutputStream.write(buffer, 0, bufferSize);
            bytesAvailable = fileInputStream.available();
            bufferSize = Math.min(bytesAvailable, maxBufferSize);
            bytesRead = fileInputStream.read(buffer, 0, bufferSize);
        }
        dataOutputStream.writeBytes(lineEnd);
    }

    private void buildTextPart(DataOutputStream dataOutputStream, String parameterName, String parameterValue) throws IOException {
        dataOutputStream.writeBytes(twoHyphens + boundary + lineEnd);
        dataOutputStream.writeBytes("Content-Disposition: form-data; name=\"" + parameterName + "\"" + lineEnd);
        dataOutputStream.writeBytes("Content-Type: text/plain; charset=UTF-8" + lineEnd);
        dataOutputStream.writeBytes(lineEnd);
        Log.d("parameterValue", parameterValue + "");
        dataOutputStream.writeBytes(parameterValue + lineEnd);
    }
}