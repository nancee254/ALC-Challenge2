package com.nancee254.travelmantics;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.widget.Toolbar;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.nancee254.travelmantics.Utils.FirebaseUtil;
import com.nancee254.travelmantics.abstracts.BaseActivity;
import com.nancee254.travelmantics.model.TravelDeal;
import com.squareup.picasso.Picasso;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.util.Objects;

public class CreateNewDeal extends BaseActivity {

    // TODO: 8/7/2019   Declare
    private FirebaseDatabase firebaseDatabase;
    private DatabaseReference databaseReference;
    private StorageReference storageReference;
    private ProgressDialog progressDialog;
    private Uri imageURI;
    private TravelDeal travelDeal;
    private String downloadUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_new_deal);

        // TODO: 8/7/2019  Initialize
        initViews();
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        initialize();
    }

    private Toolbar toolbar;
    private EditText txtTitle;
    private EditText txtPrice;
    private EditText txtDescription;
    private TextView dealImage;
    private ImageView myDealImage;

    public void initViews() {
        toolbar = findViewById(R.id.toolbar);
        txtTitle = findViewById(R.id.txtTitle);
        txtPrice = findViewById(R.id.txtPrice);
        txtDescription = findViewById(R.id.txtDescription);
        dealImage = findViewById(R.id.dealImage);
        myDealImage = findViewById(R.id.myDealImage);
    }

    @Override
    public void initialize() {
        firebaseDatabase = FirebaseUtil.firebaseDatabase;
        databaseReference = FirebaseUtil.databaseReference;
        storageReference = FirebaseUtil.storageReference;
        progressDialog = new ProgressDialog(this);
        dealImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CropImage.activity()
                        .setGuidelines(CropImageView.Guidelines.ON)
                        .start(CreateNewDeal.this);
            }
        });
        Intent intent = getIntent();
        if (intent.hasExtra("TravelDeal")) {
            TravelDeal travelDeal = (TravelDeal) intent.getSerializableExtra("TravelDeal");
            if (travelDeal == null) {
                travelDeal = new TravelDeal();
            }
            this.travelDeal = travelDeal;
            txtTitle.setText(travelDeal.getTitle());
            txtDescription.setText(travelDeal.getDescription());
            txtPrice.setText(travelDeal.getPrice());
            Picasso.get()
                    .load(travelDeal.getImageUrl())
                    .into(myDealImage);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        CropImage.ActivityResult result = CropImage.getActivityResult(data);

        if (resultCode == RESULT_OK && result != null) {
            imageURI = result.getUri();
        } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE && result != null) {
            Exception error = result.getError();
            if (error != null)
                Log.d("ImageError", String.valueOf(error.getMessage()));
        }
        myDealImage.setImageURI(imageURI);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.create_deal_menu, menu);
//        if (FirebaseUtil.isAdmin) {
        menu.findItem(R.id.action_delete).setVisible(true);
        menu.findItem(R.id.save_action).setVisible(true);
        enableEditTexts(true);
//        } else {
//            menu.findItem(R.id.action_delete).setVisible(false);
//            menu.findItem(R.id.save_action).setVisible(false);
//            enableEditTexts(false);
//        }
        return true;
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.save_action:
                saveDeal();
                return true;

            case R.id.action_delete:
                deleteDeal();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void clean() {
        txtTitle.setText("");
        txtDescription.setText("");
        txtPrice.setText("");
//        myDealImage.setImageDrawable(new ColorDrawable(getResources().getColor(R.color.colorAccent)));
        txtTitle.requestFocus();
    }

    private void backToTravelListActivity() {
        startActivity(new Intent(getApplicationContext(), MainActivity.class));
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void saveDeal() {
        progressDialog.setMessage("Saving deal...");
        progressDialog.show();

        if (travelDeal != null) {
            travelDeal.setImageUrl(travelDeal.getImageUrl());
            travelDeal.setTitle(txtTitle.getText().toString());
            travelDeal.setDescription(txtDescription.getText().toString());
            travelDeal.setPrice(txtPrice.getText().toString());

            databaseReference.child(travelDeal.getId()).setValue(travelDeal).addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    Toast.makeText(CreateNewDeal.this, "Deal Saved", Toast.LENGTH_LONG).show();
                    clean();
                    backToTravelListActivity();
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.d("PushError", String.valueOf(String.valueOf(e.getMessage())));
                }
            });
        } else {
            this.travelDeal = new TravelDeal();
            final StorageReference filePath = storageReference.child("TravelDeal_Images")
                    .child((Objects.requireNonNull(imageURI.getLastPathSegment())));
            filePath.putFile(imageURI).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onComplete(@NonNull final Task<UploadTask.TaskSnapshot> task) {
                    if (task.getResult() == null) {
                        Toast.makeText(CreateNewDeal.this, "Image upload failed", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    String imageName = task.getResult().getStorage().getPath();
                    Log.d("imageName", imageName);
                    travelDeal.setImageName(imageName);
                    if (task.isSuccessful()) {
                        filePath.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(Uri uri) {
                                downloadUrl = uri.toString();
                                travelDeal.setImageUrl(downloadUrl);
                                travelDeal.setTitle(txtTitle.getText().toString());
                                travelDeal.setDescription(txtDescription.getText().toString());
                                travelDeal.setPrice(txtPrice.getText().toString());

                                if (travelDeal.getId() == null) {
                                    Log.d("checkID", travelDeal.getImageUrl() + "\n\n" + travelDeal.getTitle() + "\n\n" + travelDeal.getDescription());
                                    databaseReference.push().setValue(travelDeal).addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            Toast.makeText(CreateNewDeal.this, "Deal Saved", Toast.LENGTH_LONG).show();
                                            clean();
                                            backToTravelListActivity();
                                        }
                                    }).addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            Log.d("PushError", String.valueOf(e.getMessage()));
                                        }
                                    });
                                }
                            }
                        });
                    }
                }
            });

        }
    }

    private void deleteDeal() {
        if (travelDeal.getId() == null) {
            Toast.makeText(getApplicationContext(), "Save the deal before deleting", Toast.LENGTH_SHORT).show();
        } else {
            final AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
            alertDialog.setMessage("Are you sure you want to delete this deal?");
            alertDialog.setCancelable(true);
            alertDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(final DialogInterface dialogInterface, int i) {
                    databaseReference.child(travelDeal.getId()).removeValue();
                    StorageReference picRef = FirebaseUtil.firebaseStorage.getReference().child(travelDeal.getImageName());
                    picRef.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Log.d("Delete Image", "Successful deletion");
                            Toast.makeText(CreateNewDeal.this, "Deal Deleted!", Toast.LENGTH_SHORT).show();
                            backToTravelListActivity();
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.d("Delete Image", String.valueOf(e.getMessage()));
                        }
                    });
                }
            });

            alertDialog.setNegativeButton("No", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.dismiss();
                }
            });
            alertDialog.show();

        }

    }

    private void enableEditTexts(boolean state) {
        txtTitle.setEnabled(state);
        txtDescription.setEnabled(state);
        txtPrice.setEnabled(state);
        dealImage.setEnabled(state);
    }

}
