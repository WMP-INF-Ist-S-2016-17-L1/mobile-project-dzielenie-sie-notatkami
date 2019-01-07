/*
 * Copyright 2017 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.adrian.mob.activities;;

import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.EditText;
import android.widget.Toast;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnPausedListener;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;
import com.google.firebase.storage.UploadTask;
import com.example.adrian.mob.model.UploadInfo;
import com.example.adrian.mob.viewholder.ImgViewHolder;
import com.squareup.picasso.Picasso;
import com.example.adrian.mob.R;


public class StorageActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "StorageActivity";
    //track Choosing Image Intent
    private static final int CHOOSING_IMAGE_REQUEST = 1234;

    private EditText edtFileName;

    private Uri fileUri;

    private DatabaseReference mDataReference;
    private StorageReference imageReference;
    private StorageReference fileRef;

    private RecyclerView rcvListImg;
    private FirebaseRecyclerAdapter<UploadInfo, ImgViewHolder> mAdapter;

    ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_storage);

        edtFileName = (EditText) findViewById(R.id.edt_file_name);
        rcvListImg = (RecyclerView) findViewById(R.id.rcv_list_img);

        mDataReference = FirebaseDatabase.getInstance().getReference("images");
        imageReference = FirebaseStorage.getInstance().getReference().child("images");
        fileRef = null;
        progressDialog = new ProgressDialog(this);

        findViewById(R.id.btn_choose_file).setOnClickListener(this);
        findViewById(R.id.btn_upload_file).setOnClickListener(this);
        findViewById(R.id.btn_back).setOnClickListener(this);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setReverseLayout(false);
        rcvListImg.setHasFixedSize(false);
        rcvListImg.setLayoutManager(layoutManager);

        Query query = mDataReference.limitToLast(15);

        mAdapter = new FirebaseRecyclerAdapter<UploadInfo, ImgViewHolder>(
                UploadInfo.class, R.layout.item_image, ImgViewHolder.class, query) {
            @Override
            protected void populateViewHolder(ImgViewHolder viewHolder, UploadInfo model, int position) {
                viewHolder.nameView.setText(model.name);
                Picasso.with(StorageActivity.this)
                        .load(model.url)
                        .error(R.drawable.common_google_signin_btn_icon_dark)
                        .into(viewHolder.imageView);
            }

        };

        rcvListImg.setAdapter(mAdapter);
    }

    private void uploadFile() {
        if (fileUri != null) {
            final String fileName = edtFileName.getText().toString();

            if (!validateInputFileName(fileName)) {
                return;
            }

            progressDialog.setTitle("Dodawanie...");
            progressDialog.show();

            fileRef = imageReference.child(fileName + "." + getFileExtension(fileUri));
             StorageTask<UploadTask.TaskSnapshot> uploadTask = fileRef.putFile(fileUri);
            Task<Uri> urlTask = uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                @Override
                public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }

                    return fileRef.getDownloadUrl();

                }
            }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                @Override
                public void onComplete(@NonNull Task<Uri> task) {
                    if (task.isSuccessful()) {
                        progressDialog.dismiss();
                        Uri downloadUri = task.getResult();
                        String name = fileName;
                        Log.e(TAG, "Uri: " + downloadUri.toString());
                        Log.e(TAG, "Name: " + name);

                        writeNewImageInfoToDB(name, downloadUri.toString());

                        Toast.makeText(StorageActivity.this, "Plik dodany ", Toast.LENGTH_LONG).show();
                    } else {
                    }
                }
            });
            progressDialog.dismiss();
        } else {
            Toast.makeText(StorageActivity.this, "Brak pliku!", Toast.LENGTH_LONG).show();
        }
    }

    private void writeNewImageInfoToDB(String name, String url) {
        UploadInfo info = new UploadInfo(name, url);

        String key = mDataReference.push().getKey();
        mDataReference.child(key).setValue(info);
    }

    private void showChoosingFile() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Wybierz obraz"), CHOOSING_IMAGE_REQUEST);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        mAdapter.cleanup();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CHOOSING_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            fileUri = data.getData();
        }
    }

    @Override
    public void onClick(View view) {
        int i = view.getId();

        if (i == R.id.btn_choose_file) {
            showChoosingFile();
        } else if (i == R.id.btn_upload_file) {
            uploadFile();
        } else if (i == R.id.btn_back) {
            finish();
        }
    }

    private String getFileExtension(Uri uri) {
        ContentResolver contentResolver = getContentResolver();
        MimeTypeMap mime = MimeTypeMap.getSingleton();

        return mime.getExtensionFromMimeType(contentResolver.getType(uri));
    }
/*
    private void downloadToLocalFile(StorageReference fileRef) {
        if (fileRef != null) {
            progressDialog.setTitle("Downloading...");
            progressDialog.setMessage(null);
            progressDialog.show();

            try {
                final File localFile = File.createTempFile("images", "jpg");

                fileRef.getFile(localFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                        Bitmap bmp = BitmapFactory.decodeFile(localFile.getAbsolutePath());
                        imageView.setImageBitmap(bmp);
                        progressDialog.dismiss();
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        progressDialog.dismiss();
                        Toast.makeText(StorageActivity.this, exception.getMessage(), Toast.LENGTH_LONG).show();
                    }
                }).addOnProgressListener(new OnProgressListener<FileDownloadTask.TaskSnapshot>() {
                    @Override
                    public void onProgress(FileDownloadTask.TaskSnapshot taskSnapshot) {
                        // progress percentage
                        double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();

                        // percentage in progress dialog
                        progressDialog.setMessage("Downloaded " + ((int) progress) + "%...");
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            Toast.makeText(StorageActivity.this, "Upload file before downloading", Toast.LENGTH_LONG).show();
        }
    }
    */

    private boolean validateInputFileName(String fileName) {

        if (TextUtils.isEmpty(fileName)) {
            Toast.makeText(StorageActivity.this, "Wprowadz nazwę", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }
}