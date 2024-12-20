package com.lst.marrakechassistance.Fragment;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Environment;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.lst.marrakechassistance.Activity.MainActivity;
import com.lst.marrakechassistance.Activity.ResultRestaurantsActivity;
import com.lst.marrakechassistance.Activity.ResultsHotelsActivity;
import com.lst.marrakechassistance.Adapter.CommonQueryAdapter;
import com.lst.marrakechassistance.R;
import com.lst.marrakechassistance.utils.AppReference;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public class SearchRestaurantsFragment extends Fragment {
    public SearchRestaurantsFragment() {
        // Required empty public constructor
    }

    RecyclerView recyclerView;
    FloatingActionButton floatingActionButton;
    EditText search;
    SpeechRecognizer mSpeechRecognizer;

    // Media recorder for record an audio file in case if the user is Online
    private static final String TAG = "SearchRestaurantsFragment";
    private static final int PERMISSION_REQUEST_CODE = 1;
    private File audioFile;

    private boolean isRecording = false;
    MediaRecorder mediaRecorder;
    String ipAddress;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view =  inflater.inflate(R.layout.fragment_search_restaurants, container, false);
        mSpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(getContext());
        Intent mSpeechRecognizerIntent_EN = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        mSpeechRecognizerIntent_EN.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        mSpeechRecognizerIntent_EN.putExtra(RecognizerIntent.EXTRA_LANGUAGE,"en-US");

        mSpeechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle bundle) {

            }

            @Override
            public void onBeginningOfSpeech() {

            }

            @Override
            public void onRmsChanged(float v) {

            }

            @Override
            public void onBufferReceived(byte[] bytes) {

            }

            @Override
            public void onEndOfSpeech() {

            }

            @Override
            public void onError(int i) {

            }

            @Override
            public void onResults(Bundle bundle) {
                ArrayList<String> matches = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches.size() > 0) {
                    String query = matches.get(0);
                    search.setText(query);
                }
            }
            @Override
            public void onPartialResults(Bundle bundle) {

            }

            @Override
            public void onEvent(int i, Bundle bundle) {

            }
        });

        String[] commonQueries = getResources().getStringArray(R.array.restaurants_common_queries);
        List<String> queryList = new ArrayList<>();
        Collections.addAll(queryList, commonQueries);

        // populate the recyclerView with the data from the resources files
        recyclerView = view.findViewById(R.id.recyclerViewCommonQueries);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        DividerItemDecoration itemDecoration = new DividerItemDecoration(getContext(), LinearLayoutManager.VERTICAL);
        itemDecoration.setDrawable(ContextCompat.getDrawable(getContext(), R.drawable.divider_drawable));
        recyclerView.addItemDecoration(itemDecoration);
        CommonQueryAdapter adapter = new CommonQueryAdapter(queryList);
        recyclerView.setAdapter(adapter);

        adapter.setOnItemClickListener(query -> startResultsActivity(query));

        // add the listener to the search EditText
        search = view.findViewById(R.id.editTextQuery);
        search.setOnEditorActionListener((textView, actionId, keyEvent) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE
                    || keyEvent.getAction() == KeyEvent.ACTION_DOWN && keyEvent.getKeyCode() == KeyEvent.KEYCODE_ENTER) {

                // Retrieve the entered query
                String query = search.getText().toString().trim();

                startResultsActivity(query);
                return true;
            }
            return false;
        });

        // Check if permissions are granted, and request them if not
        String[] permissions = {Manifest.permission.RECORD_AUDIO, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        boolean allPermissionsGranted = true;
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(getActivity(), permission) != PackageManager.PERMISSION_GRANTED) {
                allPermissionsGranted = false;
                break;
            }
        }
        if (!allPermissionsGranted) {
            ActivityCompat.requestPermissions(getActivity(), permissions, PERMISSION_REQUEST_CODE);
        }

        if (isConnectedToInternet()){
            ipAddress = new AppReference(getContext()).getIpAddress();
            if (ipAddress.equals("")){
                    Intent intent = new Intent(getContext(), MainActivity.class);
                    startActivity(intent);
                    getActivity().finish();
            }
        }

        // implement the vocal search feature
        floatingActionButton = view.findViewById(R.id.restfloatingActionButton);
        floatingActionButton.setOnTouchListener((view1, motionEvent) -> {
            if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                floatingActionButton.setBackgroundTintList(ContextCompat.getColorStateList(getContext(), R.color.colorAccent));
                search.setText("");
                search.setHint("Listening ...");
                if (isConnectedToInternet() && !ipAddress.equals("")){
                    //  strart The recording
                    startRecording();
                    Toast.makeText(getContext(), "Recording audio and sending to API", Toast.LENGTH_SHORT).show();
                }else{
                    mSpeechRecognizer.startListening(mSpeechRecognizerIntent_EN);
                }
            } else if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                floatingActionButton.setBackgroundTintList(ContextCompat.getColorStateList(getContext(), R.color.colorPrimary));
                if (isConnectedToInternet()){
                    // Stop the recording
                    stopRecording();
                    Toast.makeText(getContext(), "Stopping recording", Toast.LENGTH_SHORT).show();
                } else {
                    mSpeechRecognizer.stopListening();
                }

            }
            return false;
        });

        return view;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allPermissionsGranted = true;
            for (int grantResult : grantResults) {
                if (grantResult != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false;
                    break;
                }
            }

            if (allPermissionsGranted) {
                // Permissions are granted, start audio recording
                startRecording();
            } else {
                // Permissions are denied, handle the case accordingly
                Toast.makeText(getActivity(), "Permissions denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private boolean isConnectedToInternet() {
        // Check The connectivity of The user
        ConnectivityManager connectivityManager = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }
        return false;
    }

    private void startRecording() {

        // Check if recording is already in progress
        if (isRecording) {
            return;
        }

        // Set the recording flag
        isRecording = true;

        // Generate a unique audio file name
        String audioFileName = "recording_" + UUID.randomUUID().toString() + ".wav";

        // Create the audio file using the external storage directory
        File directory = getActivity().getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        audioFile = new File(directory, audioFileName);
        // Initialize MediaRecorder and configure audio source, output format, and output file
        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.DEFAULT);
        mediaRecorder.setOutputFile(audioFile.getAbsolutePath());
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);

        try {
            // Prepare and start audio recording
            mediaRecorder.prepare();
            mediaRecorder.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void stopRecording() {
        // Check if recording is in progress
        if (!isRecording) {
            return;
        }

        // Reset the recording flag
        isRecording = false;

        if (mediaRecorder != null) {
            try {
                // Stop and release the MediaRecorder
                mediaRecorder.stop();
                mediaRecorder.release();
                uploadFile();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void uploadFile() {
        // Build the HTTP Client
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS) // Set read timeout
                .writeTimeout(30, TimeUnit.SECONDS) // Set write timeout
                .build();

        // Create a multipart request body
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", audioFile.getName(), RequestBody.create(MediaType.parse("audio/wav"), audioFile))
                .build();

        // Create the HTTP request
        Request request = new Request.Builder()
                .url("http://"+ ipAddress + ":5000/upload")
                .header("Content-Type", "application/json")
                .post(requestBody)
                .build();

        // Make the request asynchronously
        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "Request failed: " + e.getMessage());
                getActivity().runOnUiThread(() -> Toast.makeText(getActivity(), "Upload failed", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String res = response.body().string();

                getActivity().runOnUiThread(() -> startResultsActivity(res));
            }
        });
    }

    private void startResultsActivity(String query) {
        Intent intent = new Intent(getContext(), ResultRestaurantsActivity.class);
        intent.putExtra("category", "restaurants");
        intent.putExtra("query", query);
        startActivity(intent);
    }
}