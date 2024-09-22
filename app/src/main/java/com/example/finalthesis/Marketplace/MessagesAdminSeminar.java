package com.example.finalthesis.Marketplace;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finalthesis.Menu.Seminar_Report_Module;
import com.example.finalthesis.R;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class MessagesAdminSeminar extends AppCompatActivity {

    Toolbar toolbar;

    private static final int MAX_IMAGE_COUNT = 5;
    private List<Uri> selectedImages;
    private LinearLayout selectedImagesLayout;

    private ActivityResultLauncher<Intent> someActivityResultLauncher;
    FirebaseFirestore fStore;
    FirebaseAuth fAuth;
    String threadId;
    String currentUserEmail;
    String adminEmail;

    String currentUserName;

    String adminName;

    RecyclerView messagesRecyclerView;
    MessagesAdapter messagesAdapter;
    List<MessageModel> messagesList;

    EditText messageInput;
    ImageButton sendMessageButton;

    ImageButton addImageButton;

    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final int LENGTH = 12;
    private static final Random RANDOM = new SecureRandom();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationIcon(R.drawable.arrowback);
        int titleTextColor = getResources().getColor(android.R.color.white);
        toolbar.setTitleTextColor(titleTextColor);

        fStore = FirebaseFirestore.getInstance();
        fAuth = FirebaseAuth.getInstance();

        threadId = getIntent().getStringExtra("THREAD_ID");
        currentUserEmail = getIntent().getStringExtra("CURRENT_USER_EMAIL");
        adminEmail = getIntent().getStringExtra("ADMIN_EMAIL");
        adminName = getIntent().getStringExtra("ADMIN_NAME");
        currentUserName = getIntent().getStringExtra("CURRENT_USER_NAME");
        getSupportActionBar().setTitle("Message for Seminar");

        messagesRecyclerView = findViewById(R.id.messagesRecyclerView);
        messageInput = findViewById(R.id.messageInput);
        sendMessageButton = findViewById(R.id.sendMessageButton);
        addImageButton = findViewById(R.id.addImageButton);
        selectedImagesLayout = findViewById(R.id.selectedImagesLayout);

        messagesList = new ArrayList<>();
        messagesAdapter = new MessagesAdapter(messagesList, currentUserEmail, this);
        messagesRecyclerView.setAdapter(messagesAdapter);
        messagesRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        selectedImages = new ArrayList<>();

        sendMessageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage();
            }
        });

        addImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (selectedImages.size() < MAX_IMAGE_COUNT) {
                    Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                    someActivityResultLauncher.launch(intent);
                } else {
                    // Show message: Max 5 images
                }
            }
        });

        someActivityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        if (result.getData().getClipData() != null) {
                            int count = result.getData().getClipData().getItemCount();
                            for (int i = 0; i < count; i++) {
                                if (selectedImages.size() < MAX_IMAGE_COUNT) {
                                    Uri imageUri = result.getData().getClipData().getItemAt(i).getUri();
                                    selectedImages.add(imageUri);
                                    displaySelectedImages();
                                }
                            }
                        } else if (result.getData().getData() != null) {
                            Uri imageUri = result.getData().getData();
                            selectedImages.add(imageUri);
                            displaySelectedImages();
                        }
                    }
                }
        );

        fetchMessages();

        // Check for PDF path and send as the first message
        String pdfPath = getIntent().getStringExtra("PDF_PATH");
        if (pdfPath != null) {
            sendPdfAsFirstMessage(pdfPath);
        }
    }

    private void fetchMessages() {
        fStore.collection("Seminar").document(threadId).collection("Chats")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot snapshots, @Nullable FirebaseFirestoreException e) {
                        if (e != null) {
                            return;
                        }

                        assert snapshots != null;
                        for (DocumentChange dc : snapshots.getDocumentChanges()) {
                            if (dc.getType() == DocumentChange.Type.ADDED) {
                                messagesList.add(dc.getDocument().toObject(MessageModel.class));
                                messagesAdapter.notifyItemInserted(messagesList.size() - 1);
                                messagesRecyclerView.scrollToPosition(messagesList.size() - 1);
                            }
                        }
                    }
                });
    }

    private void sendMessage() {
        String messageText = messageInput.getText().toString().trim();
        if (!messageText.isEmpty()) {
            Map<String, Object> message = new HashMap<>();
            message.put("sender", currentUserEmail);
            message.put("receiver", adminEmail);
            message.put("senderName", currentUserName);
            message.put("receiverName", adminName);
            message.put("message", messageText);
            message.put("timestamp", System.currentTimeMillis());

            DocumentReference threadRef = fStore.collection("Seminar").document(threadId);

            if (!selectedImages.isEmpty()) {
                // Upload images to Firebase Storage
                uploadImagesToFirebase(message, threadRef);
            } else {
                // Send message without images
                sendMessageToFirestore(message, threadRef);
            }
        }
    }

    private void sendPdfAsFirstMessage(String pdfPath) {
        File pdfFile = new File(pdfPath);
        Uri pdfUri = Uri.fromFile(pdfFile);

        // Generate a unique file name for the PDF
        String uniqueFileName = "seminarrequest" + System.currentTimeMillis() + ".pdf";

        // Reference to Firebase Storage
        StorageReference storageRef = FirebaseStorage.getInstance().getReference().child("seminar_requests/" + uniqueFileName);

        // Upload PDF file
        storageRef.putFile(pdfUri).addOnSuccessListener(taskSnapshot -> {
            // Once uploaded, get the download URL
            storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                // Prepare message data
                Map<String, Object> message = new HashMap<>();
                message.put("sender", currentUserEmail);
                message.put("receiver", adminEmail);
                message.put("senderName", currentUserName);
                message.put("receiverName", adminName);
                message.put("message", "Request for Seminar - Pending");
                message.put("pdfUrl", uri.toString());
                message.put("timestamp", System.currentTimeMillis());
                message.put("dateAccepted", "");
                message.put("dateDeclined", "");
                message.put("dateRejected", "");
                message.put("declineReason", "");
                message.put("seminarRequestId", "");
                message.put("requestStatus", "Pending");

                // Generate a unique seminarRequestId
                generateUniqueSeminarChatId(new OnSuccessListener<String>() {
                    @Override
                    public void onSuccess(String seminarRequestId) {
                        message.put("seminarRequestId", seminarRequestId);

                        // Reference to the thread in Firestore
                        DocumentReference threadRef = fStore.collection("Seminar").document(threadId);

                        // Save message and update request status to Pending
                        sendMessageToFirestore(message, threadRef);
                        Map<String, Object> requestStatusUpdate = new HashMap<>();
                        requestStatusUpdate.put("requestStatus", "Pending");
                        threadRef.update(requestStatusUpdate);
                    }
                });
            });
        });
    }



    private void generateUniqueSeminarChatId(OnSuccessListener<String> onSuccessListener) {
        String seminarRequestId = generateRandomId();
        fStore.collection("Seminar")
                .whereEqualTo("seminarRequestId", seminarRequestId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null && task.getResult().isEmpty()) {
                        onSuccessListener.onSuccess(seminarRequestId);
                    } else {
                        // If the generated reportId already exists, generate a new one
                        generateUniqueSeminarChatId(onSuccessListener);
                    }
                });
    }

    private void uploadImagesToFirebase(Map<String, Object> message, DocumentReference threadRef) {
        List<String> imageUrls = new ArrayList<>();
        for (Uri imageUri : selectedImages) {
            // Generate a unique file name for each image
            String fileName = System.currentTimeMillis() + ".jpg";
            StorageReference storageRef = FirebaseStorage.getInstance().getReference().child("chat_images_seminar/" + threadId + "/" + fileName);

            storageRef.putFile(imageUri).addOnSuccessListener(taskSnapshot -> storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                imageUrls.add(uri.toString());
                if (imageUrls.size() == selectedImages.size()) {
                    // All images uploaded
                    message.put("images", imageUrls);
                    sendMessageToFirestore(message, threadRef);
                }
            }));
        }
    }

    private String generateRandomId() {
        StringBuilder sb = new StringBuilder(LENGTH);
        for (int i = 0; i < LENGTH; i++) {
            sb.append(CHARACTERS.charAt(RANDOM.nextInt(CHARACTERS.length())));
        }
        return sb.toString();
    }

    private void sendMessageToFirestore(Map<String, Object> message, DocumentReference threadRef) {
        threadRef.collection("Chats").add(message);
        threadRef.update("participants", Arrays.asList(currentUserEmail, adminEmail))
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        messageInput.setText("");
                        selectedImages.clear();
                        selectedImagesLayout.removeAllViews();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        generateUniqueReportId(id -> {
                            Map<String, Object> threadData = new HashMap<>();

                            threadData.put("participants", Arrays.asList(currentUserEmail, adminEmail));
                            threadData.put("requestStatus", "Pending");
                            threadData.put("dateAccepted", "");
                            threadData.put("dateDeclined", "");
                            threadData.put("seminarRequestId", id);
                            threadData.put("sentTimestamp", System.currentTimeMillis());
                            threadRef.set(threadData)
                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            messageInput.setText("");
                                            selectedImages.clear();
                                            selectedImagesLayout.removeAllViews();
                                        }
                                    });
                        });
                    }
                });
    }

    private void generateUniqueReportId(OnSuccessListener<String> onSuccessListener) {
        String seminarRequestId = generateRandomId();
        fStore.collection("Seminar")
                .whereEqualTo("seminarRequestId", seminarRequestId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null && task.getResult().isEmpty()) {
                        onSuccessListener.onSuccess(seminarRequestId);
                    } else {
                        // If the generated reportId already exists, generate a new one
                        generateUniqueReportId(onSuccessListener);
                    }
                });
    }

    private void displaySelectedImages() {
        selectedImagesLayout.removeAllViews();
        for (Uri imageUri : selectedImages) {
            ImageView imageView = new ImageView(this);
            imageView.setLayoutParams(new LinearLayout.LayoutParams(200, 200));
            imageView.setImageURI(imageUri);
            selectedImagesLayout.addView(imageView);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent intent = new Intent(MessagesAdminSeminar.this, Seminar_Report_Module.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();

    }
}
