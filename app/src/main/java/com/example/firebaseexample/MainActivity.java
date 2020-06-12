package com.example.firebaseexample;
/*
Things to consider:

1`.  Create a camera option for saving the image.

2.  Add the remaining fields for storage of each item:  Value, Artist, Category = all mandatory fields.

Value should be int, artist should be String, Category should be drop down drop down spinner.

3.  Introduce user validation.
Each user should have their data stored in their own db.


 */

import android.Manifest;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    /*constants for:
    Image Capture from Gallery,
    Image Capture from camera
    TAG for logs
    source: for the recycler view and firebase
    https://www.youtube.com/watch?v=gqIWrNitbbk&list=PLrnPJCHvNZuB_7nB5QD-4bNg6tpdEUImQ&index=2
     */

    private static final int REQUEST_IMAGE_CAPTURE = 101;
    private static final int PICK_IMAGE_REQUEST = 1;
    private static final String TAG = "SHIT";
    private FirebaseAnalytics mFirebaseAnalytics;
    //initiate FirebaseAuth for authorisation
    FirebaseAuth mAuth;

    //declarations for views
    private Button btnChooseImage, btnTakePicture, btnShowUploads, btnDetails, btnGridView;
    private Button btnUpload, btnSignOut;
    private TextView tvUserVerified;
    private EditText etFileName, etArtist, etValue;
    private ImageView imageView;
    private ProgressBar progressBar;
    private Uri mImageUri;
    private Spinner spCategory;
    private EditText etDetails, etDDetails;


    private String deets = "";
    //declarations for FirebaseStorage and firebase Database

    private StorageReference mStorageRef;
    private DatabaseReference mDatabaseRef;
    private FirebaseUser fbUser;//declared as private  - not sure this is okay....
    //declaration for listview adapter
    public ArrayAdapter<String> dataAdapter;

    //private Storage Task mUploadTask = can it be deleted = I think so, unused...
    private StorageTask mUploadTask;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

        //assign user to mAuth;
        mAuth = FirebaseAuth.getInstance();

        //check what is passed to
        Log.d(TAG, mAuth.toString());
        String user = FirebaseAuth.getInstance().getUid();
        fbUser = mAuth.getCurrentUser();

        // Toast.makeText(this, "FBUserId:" + fbUser, Toast.LENGTH_LONG).show();

        btnDetails = findViewById(R.id.btnDetails);
        btnSignOut = findViewById(R.id.btnSignOut);
        spCategory = findViewById(R.id.spCategory);
        btnTakePicture = findViewById(R.id.btnTakePicture);
        btnChooseImage = findViewById(R.id.btnChooseImage);
        btnUpload = findViewById(R.id.btnUpload);
        btnShowUploads = findViewById(R.id.btnShowUploads);
        etFileName = findViewById(R.id.etFileName);
        etArtist = findViewById(R.id.etArtist);
        etValue = findViewById(R.id.etValue);
        imageView = findViewById(R.id.imageView);
        progressBar = findViewById(R.id.progressBar);
        tvUserVerified = findViewById(R.id.tvUserVerified);
        etDDetails = findViewById(R.id.etDDetails);
        btnGridView = findViewById(R.id.btnGridView);
        checkUser();


        mStorageRef = FirebaseStorage.getInstance().getReference(user + "/uploads");
        mDatabaseRef = FirebaseDatabase.getInstance().getReference(user + "/uploads");

        List<String> categories = new ArrayList<>();

        categories.add("Painting");
        categories.add("Porcelain");
        categories.add("Statue");
        categories.add("Bronze");
        categories.add("Furniture");
        categories.add("Books");
        categories.add("Glass");
        categories.add("Other");

        dataAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, categories);

        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        spCategory.setAdapter(dataAdapter);

        btnChooseImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openFileChooser();

            }
        });


        btnGridView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this,GridViewActivity.class);
                intent.putExtra("Screen",1);
                startActivity(intent);
            }
        });


        btnDetails.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);

                etDetails = new EditText((MainActivity.this));
                etDetails.setText(etDDetails.getText().toString());
                //    final EditText editText = new EditText(MainActivity.this);
                builder.setView(etDetails);

                builder.setMessage(("Enter Details"))
                        .setPositiveButton("Enter", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                etDDetails.setText(etDetails.getText().toString().trim());
                                deets = "" + etDetails.getText().toString().trim();

                                Log.d("Deets", "" + deets);
                            }

                        }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });


                AlertDialog dialog = builder.create();
                dialog.show();


            }


        });

        btnSignOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signOut();
            }
        });

        btnTakePicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                takePicture();
            }
        });

        btnUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (etValue.getText().toString().equals("") || etArtist.getText().toString().equals("") || etFileName.getText().toString().equals("")) {
                    Toast.makeText(MainActivity.this, "Please ensure all fields are populated", Toast.LENGTH_SHORT).show();
                    return;
                }


                if (mUploadTask != null && mUploadTask.isInProgress()) {
                    Toast.makeText(MainActivity.this, "Upload in Progress", Toast.LENGTH_SHORT).show();

                } else if (!(etValue.getText() == null || etArtist.getText() == null || etFileName.getText() == null)) {
                    uploadFile();
                }


            }
        });


        btnShowUploads.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openImagesActivity();

            }
        });
    }

    private void openFileChooser() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Choose Image from Gallery:"), PICK_IMAGE_REQUEST);

    }

    private void takePicture() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, REQUEST_IMAGE_CAPTURE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            mImageUri = data.getData();

            Picasso.get().load(mImageUri).into(imageView);

            //old way - imageView.setImageURI(mImageUri);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.System.canWrite(this)) {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE}, 2909);

            }
        }
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {


            Bundle extras = data.getExtras();
            final Bitmap imageBitmap = (Bitmap) extras.get("data");
            mImageUri = getImageUri(MainActivity.this, imageBitmap);

            Picasso.get().load(mImageUri).into(imageView);
            //imageView.setImageBitmap(imageBitmap);


            //Picasso.get().load(mImageUri).into(imageView);
        }
    }


    private String getFileExtension(Uri uri) {
        ContentResolver cR = getContentResolver();
        MimeTypeMap mime = MimeTypeMap.getSingleton();
        return mime.getExtensionFromMimeType(cR.getType(uri));
    }

    private void uploadFile() {
        if (mImageUri != null || etValue.getText().equals(null) || etArtist.getText().equals(null) || etFileName.getText().equals(null)) {


            progressBar.setVisibility(View.VISIBLE);

            final StorageReference fileReference = mStorageRef.child(System.currentTimeMillis()
                    + "." + getFileExtension(mImageUri));

            fileReference.putFile(mImageUri).continueWithTask(
                    new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                        @Override
                        public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                            if (!task.isSuccessful()) {
                                throw task.getException();
                            }

                            return fileReference.getDownloadUrl();
                        }
                    })

                    .addOnCompleteListener(new OnCompleteListener<Uri>() {
                        @Override
                        public void onComplete(@NonNull Task<Uri> task) {
                            if (task.isSuccessful()) {
                                progressBar.setVisibility(View.GONE);
                                Uri downloadUri = task.getResult();
                                String etFile = etFileName.getText().toString().trim();
                                String etArt = etArtist.getText().toString().trim();
                                String etVal = etValue.getText().toString().trim();
                                String etCat = spCategory.getSelectedItem().toString();
                                String etDetailss = etDDetails.getText().toString().trim();
                                Log.d("etCat", "" + etCat);

                                Upload upload = new Upload(etFile, downloadUri.toString(), etArt, Integer.parseInt(etVal), etCat, etDetailss);
                                mDatabaseRef.push().setValue(upload);
                                Toast.makeText(MainActivity.this, "Upload Successful", Toast.LENGTH_SHORT).show();

                                etFileName.setText("");
                                etArtist.setText("");
                                etValue.setText("");
                                imageView.setImageResource(0);
                                etFileName.requestFocus();
                                deets = "";
                                etDDetails.setText("");
                            } else {
                                Toast.makeText(MainActivity.this, "Failed!", Toast.LENGTH_SHORT).show();
                            }
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                        }

                    });
        } else {
            Toast.makeText(this, "Make sure all fields are populated, including image!", Toast.LENGTH_SHORT).show();
        }

    }

    private void openImagesActivity() {
        Intent intent = new Intent(this, ImagesActivity.class);
        startActivity(intent);//this doesn't work going through the vid = 4.59 part 4
    }//https://www.youtube.com/watch?v=3LnMk0-k8bw&list=PLrnPJCHvNZuB_7nB5QD-4bNg6tpdEUImQ&index=4

    //taken from stack overflow - convert bitmap to Uri, pass it back to onActivityResult
    //https://stackoverflow.com/questions/8295773/how-can-i-transform-a-bitmap-into-a-uri/16168087#16168087
    public Uri getImageUri(Context context, Bitmap bitmapImage) {

        ByteArrayOutputStream byes = new ByteArrayOutputStream();
        bitmapImage.compress(Bitmap.CompressFormat.PNG, 100, byes);
        String mills = "" + System.currentTimeMillis();
        String path = MediaStore.Images.Media.insertImage(context.getContentResolver(), bitmapImage, "Title" + mills, null);

        return Uri.parse(path);
    }

    @Override

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case 2909: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                } else {
                    Toast.makeText(this, "Permission Denied!", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    /*
        @Override
        public boolean onCreateOptionsMenu(Menu menu) {
            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.example_menu, menu);

            MenuItem searchItem = menu.findItem(R.id.action_search);
            SearchView searchView = (SearchView) searchItem.getActionView();
            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    return false;
                }

                @Override
                public boolean onQueryTextChange(String newText) {
                    dataAdapter.getFilter().filter(newText);
                    return false;
                }
            });
            return true;
        }
    */
    private void checkUser() {
        final FirebaseUser user = mAuth.getCurrentUser();
        if (user.isEmailVerified()) {
            tvUserVerified.setVisibility(View.GONE);

        } else {
            tvUserVerified.setText("Email not Verfified - Click to Verify");
            btnUpload.setVisibility(View.GONE);
            tvUserVerified.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    user.sendEmailVerification().addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            Toast.makeText(MainActivity.this, "Verification Email sent", Toast.LENGTH_SHORT).show();

                        }
                    });
                }
            });
        }
    }

    //sign out user and go back to Loggy Main screen.
    private void signOut() {
        FirebaseAuth.getInstance().signOut();
        startActivity(new Intent(this, Loggy.class));
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("Name", etFileName.getText().toString());
        //    outState.putString("Detail", etDDetails.getText().toString());
        outState.putString("Artist", etArtist.getText().toString());
        outState.putString("Value", etValue.getText().toString());
        outState.putString("Details", etDDetails.getText().toString());


        Log.d("DeetsinSavedInstanceStatemethod", "" + etDDetails.getText().toString());
//        outState.putString("Detail",""+builder.setMessage(etDDetails.getText().toString()));

    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        etFileName.setText(savedInstanceState.getString("Name"));
        etArtist.setText(savedInstanceState.getString("Artist"));
        etValue.setText(savedInstanceState.getString("Value"));
        etDDetails.setText(savedInstanceState.getString("Details"));


    }
}












