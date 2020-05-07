package com.semihoksuzoglu.instagramclonefirebase;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.IOException;
import java.util.HashMap;
import java.util.UUID;

public class uploadActivity extends AppCompatActivity {

    EditText postCommentText;
    ImageView postImage;
    FirebaseDatabase firebaseDatabase;
    DatabaseReference myRef;
    private StorageReference mStorageRef;
    private FirebaseAuth mAuth;
    Uri selectedImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload);
        postCommentText = findViewById(R.id.postCommentText);
        postImage = findViewById(R.id.postImageView);

        firebaseDatabase = FirebaseDatabase.getInstance();
        myRef = firebaseDatabase.getReference();

        mAuth = FirebaseAuth.getInstance();

        mStorageRef = FirebaseStorage.getInstance().getReference();
    }

    public void upload(View view) {

        UUID uuid = UUID.randomUUID();
        final String imageName = "images/" + uuid + ".jpg";

        StorageReference storageReference = mStorageRef.child(imageName);
        storageReference.putFile(selectedImage).addOnSuccessListener
                (this, new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        //download url
                        StorageReference newReference = FirebaseStorage.getInstance().getReference(imageName);
                        newReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(Uri uri) {
                                String downloadURL = uri.toString();
                                FirebaseUser user = mAuth.getCurrentUser();
                                String userEmail = user.getEmail();
                                String userComment = postCommentText.getText().toString();

                                UUID uuid1 = UUID.randomUUID();
                                String uuidString = uuid1.toString();

                                HashMap<String, Object> postData = new HashMap<>();
                                postData.put("useremail", userEmail);
                                postData.put("comment", userComment);
                                postData.put("downloadurl", downloadURL);
                                postData.put("data", FieldValue.serverTimestamp());

                                FirebaseFirestore.getInstance().collection("Posts")
                                        .add(postData)
                                        .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                                            @Override
                                            public void onSuccess(DocumentReference documentReference) {
                                                Intent intent = new Intent(getApplicationContext(),feedActivity.class);
                                                startActivity(intent);

                                            }
                                        }).addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Toast.makeText(uploadActivity.this, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                                    }
                                });


                                /*
                                FIREBASE REALTIME DATABASE
                                myRef.child("Posts").child(uuidString).child("useremail").setValue(userEmail);
                                myRef.child("Posts").child(uuidString).child("comment").setValue(userComment);
                                myRef.child("Posts").child(uuidString).child("dowloadurl").setValue(downloadURL);

                                Toast.makeText(uploadActivity.this, "Post Shared", Toast.LENGTH_SHORT).show();

                                Intent intent= new Intent(getApplicationContext(),feedActivity.class);
                                finish();
                                startActivity(intent);
                                */
                            }
                        });

                        //username,comment,
                    }
                }).addOnFailureListener(this, new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(uploadActivity.this, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    public void selectImage(View view) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        } else {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, 2);
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(intent, 2);
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == 2 && resultCode == RESULT_OK && data != null) {
            selectedImage = data.getData();
            try {
                if (Build.VERSION.SDK_INT >= 28) {
                    ImageDecoder.Source source = ImageDecoder.createSource(this.getContentResolver(), selectedImage);
                    Bitmap bitmap = ImageDecoder.decodeBitmap(source);
                    postImage.setImageBitmap(bitmap);
                } else {
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), selectedImage);
                    postImage.setImageBitmap(bitmap);
                }

            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        super.onActivityResult(requestCode, resultCode, data);
    }
}
