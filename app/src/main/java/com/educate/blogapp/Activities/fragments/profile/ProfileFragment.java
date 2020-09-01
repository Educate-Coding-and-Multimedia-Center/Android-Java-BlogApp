package com.educate.blogapp.Activities.fragments.profile;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.educate.blogapp.Activities.HomeActivity;
import com.educate.blogapp.Activities.RegisterActivity;
import com.educate.blogapp.Models.Post;
import com.educate.blogapp.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.HashMap;
import java.util.Map;

import static android.app.Activity.RESULT_OK;

public class ProfileFragment extends Fragment {

    static int permissionCode = 1;
    static final int REQUESTCODE = 1;
    Uri pickedImageUri;

    FirebaseAuth firebaseAuth;
    FirebaseUser currentUser;
    FirebaseDatabase database;

    EditText profileName, profileEmail;
    ImageView profileImage;
    Button saveButton;
    ProgressBar progressBar;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_profile, container, false);

        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        firebaseAuth = FirebaseAuth.getInstance();
        currentUser = firebaseAuth.getCurrentUser();
        database = FirebaseDatabase.getInstance();

        // Init views
        profileName = view.findViewById(R.id.edtProfileName);
        profileEmail = view.findViewById(R.id.edtProfileEmail);
        profileImage = view.findViewById(R.id.imgProfileUserPhoto);
        saveButton = view.findViewById(R.id.buttonSave);
        progressBar = view.findViewById(R.id.progressBar);

        // Set current user
        profileName.setText(currentUser.getDisplayName());
        profileEmail.setText(currentUser.getEmail());
        Glide.with(this).load(currentUser.getPhotoUrl()).into(profileImage);

        profileImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Build.VERSION.SDK_INT >= 22) {
                    checkAndRequestPermission();
                }
                else {
                    openGallery();
                }
            }
        });

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveButton.setVisibility(View.INVISIBLE);
                progressBar.setVisibility(View.VISIBLE);
                final String email = profileEmail.getText().toString();
                final String name = profileName.getText().toString();

                if (email.isEmpty() || name.isEmpty() ) {
                    showMessage("Please fill all fields correctly");
                    saveButton.setVisibility(View.VISIBLE);
                    progressBar.setVisibility(View.INVISIBLE);
                }
                else {
                    updateUserProfile(email, name, pickedImageUri);
                }
            }
        });
    }

    private void updateUserProfile(final String email, final String name, Uri pickedImageUri) {
        StorageReference storage = FirebaseStorage.getInstance().getReference().child("users_photos");

        // Delete old image
        if (profileImage.getTag() == "picked"){
            final StorageReference oldImageFile = FirebaseStorage.getInstance().getReferenceFromUrl(currentUser.getPhotoUrl().toString());
            if (oldImageFile != null){
                oldImageFile.delete();
            }

            final StorageReference imageFilePath = storage.child(pickedImageUri.getLastPathSegment());
            imageFilePath.putFile(pickedImageUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                    imageFilePath.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {
                            UserProfileChangeRequest profileUpdate = new UserProfileChangeRequest.Builder()
                                    .setDisplayName(name)
                                    .setPhotoUri(uri)
                                    .build();

                            currentUser.updateProfile(profileUpdate)
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if (task.isSuccessful()) {
                                                currentUser.updateEmail(email).addOnCompleteListener(
                                                      new OnCompleteListener<Void>() {
                                                         @Override
                                                               public void onComplete(@NonNull Task<Void> task) {
                                                                    if (task.isSuccessful()) {
                                                                        showMessage("Profile updated");
                                                                        updatePostsAndComments();
                                                                    }
                                                                    else {
                                                                        showMessage(task.getException().toString());
                                                                    }
                                                                     saveButton.setVisibility(View.VISIBLE);
                                                                     progressBar.setVisibility(View.INVISIBLE);
                                                               }
                                                         }
                                                );
                                            }
                                            else {
                                                showMessage(task.getException().toString());
                                                saveButton.setVisibility(View.VISIBLE);
                                                progressBar.setVisibility(View.INVISIBLE);
                                            }
                                        }
                                    });

                        }
                    });
                }
            });
        }
        else {
            UserProfileChangeRequest profileUpdate = new UserProfileChangeRequest.Builder()
                    .setDisplayName(name)
                    .setPhotoUri(currentUser.getPhotoUrl())
                    .build();

            currentUser.updateProfile(profileUpdate)
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                currentUser.updateEmail(email).addOnCompleteListener(
                                        new OnCompleteListener<Void>() {
                                            @Override
                                            public void onComplete(@NonNull Task<Void> task) {
                                                if (task.isSuccessful()) {
                                                    showMessage("Profile updated");
                                                    updatePostsAndComments();
                                                }
                                                else {
                                                    showMessage(task.getException().toString());
                                                }
                                                saveButton.setVisibility(View.VISIBLE);
                                                progressBar.setVisibility(View.INVISIBLE);
                                            }
                                        }
                                );
                            }
                            else {
                                showMessage(task.getException().toString());
                                saveButton.setVisibility(View.VISIBLE);
                                progressBar.setVisibility(View.INVISIBLE);
                            }
                        }
                    });
        }
    }

    private void updatePostsAndComments() {

        // Update post user image for the current user update
        final DatabaseReference posts = database.getReference("Posts");
        Query currentUserPosts = posts.orderByChild("userId").equalTo(currentUser.getUid());

        currentUserPosts.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()){
                    snapshot.getRef().child("userPhoto").setValue(currentUser.getPhotoUrl().toString());
                }

                // Update all comments user image and name for all posts
                DatabaseReference comments = database.getReference().child("Comment");
                comments.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        for (DataSnapshot postSnapshot : dataSnapshot.getChildren()){
                            Query currentUserPostComments = postSnapshot.getRef().orderByChild("uid").equalTo(currentUser.getUid());
                            currentUserPostComments.addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    for (DataSnapshot commentSnapshot: dataSnapshot.getChildren()) {
                                        commentSnapshot.getRef().child("uimg").setValue(currentUser.getPhotoUrl().toString());
                                        commentSnapshot.getRef().child("uname").setValue(currentUser.getDisplayName());
                                    }
                                    updateUI();
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {

                                }
                            });
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

    private void updateUI(){
        Intent i = new Intent(getActivity(), HomeActivity.class);
        startActivity(i);
        getActivity().finish();
    }

    private void showMessage(String message) {
        Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();
    }

    private void openGallery() {
        Intent galleryIntent = new Intent(Intent.ACTION_GET_CONTENT);
        galleryIntent.setType("image/*");
        startActivityForResult(galleryIntent, REQUESTCODE);
    }

    private void checkAndRequestPermission() {
        if (ContextCompat.checkSelfPermission(
                getActivity(),
                Manifest.permission.READ_EXTERNAL_STORAGE
        ) != PackageManager.PERMISSION_GRANTED
        ) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    getActivity(),
                    Manifest.permission.READ_EXTERNAL_STORAGE
            )
            ) {
                Toast.makeText(getActivity(),
                        "Please accept for required permission",
                        Toast.LENGTH_SHORT).show();
            }
            else {
                ActivityCompat.requestPermissions(
                        getActivity(),
                        new String[]{
                                Manifest.permission.READ_EXTERNAL_STORAGE
                        },
                        permissionCode
                );
            }
        }
        else {
            openGallery();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && requestCode == REQUESTCODE && data != null) {
            pickedImageUri = data.getData();
            profileImage.setImageURI(pickedImageUri);
            profileImage.setTag("picked");
        }
    }
}