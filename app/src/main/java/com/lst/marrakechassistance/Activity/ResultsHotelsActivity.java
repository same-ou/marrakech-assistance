package com.lst.marrakechassistance.Activity;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.facebook.shimmer.ShimmerFrameLayout;
import com.lst.marrakechassistance.Adapter.HotelAdapter;
import com.lst.marrakechassistance.Adapter.HotelResultAdapter;
import com.lst.marrakechassistance.Model.Hotel;
import com.lst.marrakechassistance.R;
import com.lst.marrakechassistance.utils.AppReference;
import com.lst.marrakechassistance.utils.HotelUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ResultsHotelsActivity extends AppCompatActivity {
    private ShimmerFrameLayout mShimmerViewContainer;
    RecyclerView recyclerView;
    ArrayList<Hotel> hotels;
    HotelResultAdapter adapter;
    String ipAddress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_results);
        Intent intent = getIntent();
        String selectedCategory = intent.getStringExtra("category");
        String query = intent.getStringExtra("query");
        mShimmerViewContainer = findViewById(R.id.shimmerLayout);

        recyclerView = findViewById(R.id.hotelsRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        new ResultsHotelsActivity.HotelDataLoadingTask().execute(query);
    }
    private class HotelDataLoadingTask extends AsyncTask<String, Void, ArrayList<Hotel>> {
        @Override
        protected ArrayList<Hotel> doInBackground(String... params) {
            String query = params[0];
            return (ArrayList<Hotel>) new HotelUtil(ResultsHotelsActivity.this).getHotels(query);
        }
        @Override
        protected void onPostExecute(ArrayList<Hotel> result) {
            // Hide the shimmer animation
            mShimmerViewContainer.stopShimmer();
            mShimmerViewContainer.setVisibility(View.GONE);

            // Update the RecyclerView with the retrieved hotels
            hotels = result;
            adapter = new HotelResultAdapter(hotels);
            adapter.setOnItemClickListener(new HotelAdapter.OnItemClickListener() {

                @Override
                public void onItemClick(View view, int position) {
                    Hotel selectedHotel = hotels.get(position);

                    Intent intent = new Intent(ResultsHotelsActivity.this, HotelDetailActivity.class);
                    intent.putExtra("selectedHotel", selectedHotel);
                    startActivity(intent);
                }
            });
            recyclerView.setAdapter(adapter);
        }
    }
}