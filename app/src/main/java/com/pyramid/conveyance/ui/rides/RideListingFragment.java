package com.pyramid.conveyance.ui.rides;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.airbnb.lottie.LottieAnimationView;
import com.pyramid.conveyance.respository.executers.AppExecutors;
import com.pyramid.conveyance.utility.TrackerUtility;
import com.pyramid.conveyance.LocationApp;
import com.pyramid.conveyance.R;
import com.pyramid.conveyance.ui.rides.ridedetails.RideDetailsActivity;
import com.pyramid.conveyance.api.ApiHandler;
import com.pyramid.conveyance.api.SearchRideFilter;
import com.pyramid.conveyance.api.SearchRideResponse;
import com.pyramid.conveyance.api.response.Ride;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * A fragment representing a list of Items.
 */
public class RideListingFragment extends Fragment {

    // TODO: Customize parameter argument names
    private static final String ARG_COLUMN_COUNT = "column-count";
    // TODO: Customize parameters
    private int mColumnCount = 1;
    SwipeRefreshLayout swipeRefreshLayout;
    View list;
    TextView emptyRideTextview;
    LottieAnimationView errorAnimation;
    RecyclerView recyclerView;
    Context context;

    List<Ride> tripRecordList = new ArrayList<>();
    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public RideListingFragment() {
    }

    // TODO: Customize parameter initialization
    @SuppressWarnings("unused")
    public static RideListingFragment newInstance(int columnCount) {
        RideListingFragment fragment = new RideListingFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_COLUMN_COUNT, columnCount);
        fragment.setArguments(args);
        return fragment;
    }

    Activity mActivity;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mActivity = (Activity) context;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            mColumnCount = getArguments().getInt(ARG_COLUMN_COUNT);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_ride_details_list, container, false);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                LocationApp.logs("TRIP", "OnRefresh called from SwipeRefreshLayout");
                if (!TrackerUtility.checkConnection(mActivity)) {
                    Toast.makeText(mActivity, "Please check your network connection", Toast.LENGTH_LONG).show();
                    swipeRefreshLayout.setRefreshing(false);
                } else {
                    fetchTodaysRides(list, emptyRideTextview, errorAnimation, context, recyclerView);
                }
            }
        });
        list  = view.findViewById(R.id.list);
        emptyRideTextview  = view.findViewById(R.id.emptyRideTextView);
        errorAnimation = view.findViewById(R.id.errorAnimation);
        // Set the adapter
        if (list instanceof RecyclerView) {
            context = list.getContext();
            recyclerView = (RecyclerView) list;
            if (mColumnCount <= 1) {
                recyclerView.setLayoutManager(new LinearLayoutManager(context));
            } else {
                recyclerView.setLayoutManager(new GridLayoutManager(context, mColumnCount));
            }

            if (!TrackerUtility.checkConnection(mActivity)) {
                Toast.makeText(mActivity, "Please check your network connection", Toast.LENGTH_LONG).show();
                swipeRefreshLayout.setRefreshing(false);
            } else {
                fetchTodaysRides(list, emptyRideTextview, errorAnimation, context, recyclerView);
            }

            recyclerView.setAdapter(new MyItemRecyclerViewAdapter(tripRecordList, context));
            recyclerView.addOnItemTouchListener(
                    new RecyclerItemClickListener(mActivity, (view1, position) -> {
                        // TODO Handle item click
                        Log.e("@@@@@","" + position);
                        //Toast.makeText(getContext(), "Test :" + position, Toast.LENGTH_LONG).show();
                        Intent intent = new Intent(mActivity, RideDetailsActivity.class);
                        intent.putExtra("rideId", tripRecordList.get(position).getId());
                        intent.putExtra("isInCompleteRide", (tripRecordList.get(position).getRideEndTime() == null));
                        startActivity(intent);
                    })
            );
        }
        return view;
    }

    private void fetchTodaysRides(View list, TextView emptyRideTextview,LottieAnimationView lottieAnimationView, Context context, RecyclerView recyclerView) {
        fetchTodaysRides(list,emptyRideTextview, lottieAnimationView, context,recyclerView,true);
    }
    private void fetchTodaysRides(View list, TextView emptyRideTextview,LottieAnimationView animationView,  Context context, RecyclerView recyclerView, boolean showLoader) {
        Dialog dailog = null;
        if (showLoader) {
            dailog = LocationApp.showLoader(mActivity);
        }
        animationView.setVisibility(View.GONE);
        Dialog finalDailog = dailog;
        AppExecutors.getInstance().getNetworkIO().execute(() -> {
            //tripRecordList = DatabaseClient.getInstance(mActivity).getTripDatabase().tripRecordDao().getAllRides();
            LocationApp.logs("TRIP", "trip count is :" + tripRecordList.size());
            /*if (tripRecordList != null && tripRecordList.size() > 0) {
                mActivity.runOnUiThread(() -> {
                    recyclerView.setAdapter(new MyItemRecyclerViewAdapter(tripRecordList, context));
                });
            } else {*/

                Date today = Calendar.getInstance().getTime();
                //today = TrackerUtility.convertStringToDate("2022-12-27");
                SearchRideFilter filter = new SearchRideFilter(TrackerUtility.getDateString(today), TrackerUtility.getDateString(today));
                Call<SearchRideResponse> searchRideStatisticsCall = ApiHandler.getClient().searchRideStatistics(LocationApp.getUserName(mActivity), LocationApp.DEVICE_ID, filter);
                searchRideStatisticsCall.enqueue(new Callback<SearchRideResponse>() {
                    @Override
                    public void onResponse(Call<SearchRideResponse> call, Response<SearchRideResponse> response) {
                        if (response.code() == 200) {
                            tripRecordList = response.body().getRideDTOList();
                            Collections.reverse(tripRecordList);
                            if (tripRecordList.size() == 0) {
                                emptyRideTextview.setVisibility(View.VISIBLE);
                                list.setVisibility(View.GONE);
                            } else {
                                emptyRideTextview.setVisibility(View.GONE);
                                list.setVisibility(View.VISIBLE);
                            }
                            recyclerView.setAdapter(new MyItemRecyclerViewAdapter(response.body().getRideDTOList(), context));
                        } else {
                            recyclerView.setAdapter(new MyItemRecyclerViewAdapter(tripRecordList, context));
                        }
                        if(showLoader) {
                            finalDailog.dismiss();
                        }
                        swipeRefreshLayout.setRefreshing(false);
                    }
                    @Override
                    public void onFailure(Call<SearchRideResponse> call, Throwable t) {
                        Toast.makeText(context, "Failed to fetch rides at the moment.", Toast.LENGTH_LONG).show();
                        if(showLoader) {
                            finalDailog.dismiss();
                        }
                        //emptyRideTextview.setVisibility(View.VISIBLE);
                        animationView.setVisibility(View.VISIBLE);
                        list.setVisibility(View.GONE);
                        swipeRefreshLayout.setRefreshing(false);
                    }
                });
            //}
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        // Clear references to UI elements
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setOnRefreshListener(null); // Remove the refresh listener
            swipeRefreshLayout = null;
        }

        // Ensure that static or non-null references are not set to null
        // Example: mColumnCount is a static field, so don't set it to null
        // mColumnCount = 1;

        // Clear references to other objects and resources if necessary
        list = null;
        emptyRideTextview = null;
        errorAnimation = null;
        recyclerView = null;

        // Clear references to any other objects or resources as needed
        context = null;
        tripRecordList.clear(); // Clear the list, assuming it's not static

        // Note: mActivity should be allowed to be garbage collected.
    }

        }