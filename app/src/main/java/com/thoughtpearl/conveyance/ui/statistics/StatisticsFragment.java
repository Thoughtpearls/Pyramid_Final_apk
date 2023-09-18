package com.thoughtpearl.conveyance.ui.statistics;

import static android.content.Context.MODE_PRIVATE;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.thoughtpearl.conveyance.LocationApp;
import com.thoughtpearl.conveyance.R;
import com.thoughtpearl.conveyance.ui.rides.ridedetails.RideDetailsActivity;
import com.thoughtpearl.conveyance.api.ApiHandler;
import com.thoughtpearl.conveyance.api.SearchRideFilter;
import com.thoughtpearl.conveyance.api.SearchRideResponse;
import com.thoughtpearl.conveyance.api.response.Ride;
import com.thoughtpearl.conveyance.databinding.FragmentStatisticsBinding;
import com.thoughtpearl.conveyance.respository.databaseclient.DatabaseClient;
import com.thoughtpearl.conveyance.respository.executers.AppExecutors;
import com.thoughtpearl.conveyance.services.MyService;
import com.thoughtpearl.conveyance.ui.login.LoginActivity;
import com.thoughtpearl.conveyance.utility.TrackerUtility;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StatisticsFragment extends Fragment implements AdapterView.OnItemSelectedListener{

    public static final String TODAY = "Today";
    public static final String YESTERDAY = "Yesterday";
    public static final String WEEK = "Week";
    public static final String MONTH = "Month";
    public static final String YEAR = "Year";
    public static final String CUSTOM_DATE = "Custom Date";
    private FragmentStatisticsBinding binding;
    private List<String> categories;
    private List<String> rides;
    private ArrayAdapter<String> rideAdapter;
    private SearchRideResponse searchRideResponse;
    private String customStartDate;
    private String customEndDate;
    private SearchRideFilter searchRideFilter;
    public static boolean isRideListRefreshRequired = false;
    private boolean showDatePopup = false;

    Activity mActivity;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mActivity = (Activity) context;
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        StatisticsViewModel statisticsViewModel =
                new ViewModelProvider(this).get(StatisticsViewModel.class);

        binding = FragmentStatisticsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        binding.swipeRefreshLayout.setOnScrollChangeListener(new View.OnScrollChangeListener() {
            @Override
            public void onScrollChange(View v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
                if(scrollY > 5) {
                    binding.swipeRefreshLayout.setEnabled(false);
                }else{
                    binding.swipeRefreshLayout.setEnabled(true);
                }

            }
        });
        binding.swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                LocationApp.logs("TRIP", "OnRefresh called from SwipeRefreshLayout");
                searchRidesByDateRange(searchRideFilter);
            }
        });
        Date today = Calendar.getInstance().getTime();
        if (getArguments() != null && getArguments().containsKey("selectedDate")) {
            today = TrackerUtility.convertStringToDate(getArguments().getString("selectedDate"));
            customStartDate = TrackerUtility.getDateString(today);
            customEndDate = TrackerUtility.getDateString(today);
            LocationApp.logs("TRIP", customStartDate + " - " +customEndDate);
        }

        //today = TrackerUtility.convertStringToDate("2022-12-27");
        String sToday = TrackerUtility.getDateString(today);
        searchRideFilter = new SearchRideFilter(sToday, sToday);
        searchRidesByDateRange(searchRideFilter);

        final Spinner spinner = binding.spinner;
        final Spinner spinner1 = binding.listOfRides;
        final TextView durationTextView = binding.durationTextView;
        final TextView distanceTextView = binding.distanceTextView;
        final TextView reimbursementTextView = binding.reimbursementTextView;

        statisticsViewModel.getDurationText().observe(getViewLifecycleOwner(), durationTextView::setText);
        statisticsViewModel.getDistanceText().observe(getViewLifecycleOwner(), distanceTextView::setText);
        statisticsViewModel.getReimbursementText().observe(getViewLifecycleOwner(), reimbursementTextView::setText);

        // Spinner Drop down elements
        categories = new ArrayList<>();
        categories.add(TODAY);
        categories.add(YESTERDAY);
        categories.add(WEEK);
        categories.add(MONTH);
        categories.add(YEAR);
        categories.add(CUSTOM_DATE);

        // Creating adapter for spinner
        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(mActivity, R.layout.spinner_item, categories);

        // Drop down layout style - list view with radio button
        dataAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);

        // attaching data adapter to spinner
        spinner.setAdapter(dataAdapter);
        spinner.setSelection(0);
        if (getArguments() != null && getArguments().containsKey("selectedDate")) {
            spinner.setSelection(5);
            showDatePopup = true;
        }
        spinner.setOnItemSelectedListener(this);

        // Spinner Drop down elements
        rides = new ArrayList<>();

        // Creating adapter for spinner
        rideAdapter = new ArrayAdapter<String>(mActivity,  R.layout.spinner_item, rides);

        // Drop down layout style - list view with radio button
        rideAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);

        // attaching data adapter to spinner
        spinner1.setAdapter(rideAdapter);
        spinner1.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if (i > 0) {
                    Ride ride = searchRideResponse.getRideDTOList().get( i - 1);

                    Intent intent = new Intent(mActivity, RideDetailsActivity.class);
                    intent.putExtra("rideId", ride.getId());
                    intent.putExtra("isInCompleteRide", (ride.getRideEndTime() == null));
                    intent.putExtra("isFromStatisticScreen", true);
                    mActivity.startActivity(intent);
                    /*String sEndDate = ride.getRideDate() + " " + ride.getRideEndTime();
                    String sStartDate = ride.getRideDate() + " " + ride.getRideStartTime();
                    Date endDate = TrackerUtility.convertStringToDate(sEndDate);
                    Date startDate = TrackerUtility.convertStringToDate(sStartDate);

                    long duration = endDate.getTime() - startDate.getTime();
                    if (duration > 0) {
                        binding.durationTextView.setText(TrackerUtility.getDurationBreakdown(duration));
                    }
                    binding.distanceTextView.setText(TrackerUtility.roundOffDoubleToString(ride.getRideDistance()));

                    binding.reimbursementTextView.setText("Rs " + ride.getReimbursementCost());*/
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (isRideListRefreshRequired && searchRideFilter != null) {
            isRideListRefreshRequired = false;
            searchRidesByDateRange(searchRideFilter);
        }
    }

    private void searchRidesByDateRange(SearchRideFilter searchRideFilter) {
        searchRidesByDateRange(searchRideFilter, true);
    }

    private void searchRidesByDateRange(SearchRideFilter searchRideFilter, boolean showLoader) {
        if (!TrackerUtility.checkConnection(mActivity)) {
            Toast.makeText(mActivity, "Please check your network connection", Toast.LENGTH_LONG).show();
            binding.swipeRefreshLayout.setRefreshing(false);
        } else {
            Dialog dialog = null;
            if (showLoader) {
                dialog = LocationApp.showLoader(mActivity);
            }
            binding.statisticCardView.setVisibility(View.VISIBLE);
            binding.errorAnimation.setVisibility(View.GONE);
            Call<SearchRideResponse> searchRideStatistics = ApiHandler.getClient().searchRideStatistics(LocationApp.getUserName(mActivity), LocationApp.DEVICE_ID, searchRideFilter);
            Dialog finalDialog = dialog;
            searchRideStatistics.enqueue(new Callback<SearchRideResponse>() {
                @Override
                public void onResponse(Call<SearchRideResponse> call, Response<SearchRideResponse> response) {
                    if (response.code() == 200 || response.code() == 201) {
                        searchRideResponse = response.body();
                        rides = new ArrayList<>();
                        rides.add("Select Ride");
                        AtomicInteger index = new AtomicInteger(1);
                        searchRideResponse.getRideDTOList().forEach(ride -> {
                            rides.add((index.getAndIncrement()) + ".  " + ride.getRideDate() + " " + ride.getRideStartTime());
                        });

                        mActivity.runOnUiThread(() -> {
                            if (binding != null && binding.distanceTextView != null) {
                                binding.distanceTextView.setText(TrackerUtility.roundOffDoubleToString((double) searchRideResponse.getDistanceTravelled()));
                            } else {
                                TextView distanceTextView = (TextView) mActivity.findViewById(R.id.distanceTextView);
                                if (distanceTextView != null) {
                                    distanceTextView.setText(TrackerUtility.roundOffDoubleToString((double) searchRideResponse.getDistanceTravelled()));
                                }
                            }

                            if (binding != null && binding.durationTextView != null) {
                                binding.durationTextView.setText(searchRideResponse.getRideDuration());
                            } else {
                                TextView durationTextView = (TextView) mActivity.findViewById(R.id.durationTextView);
                                if (durationTextView != null) {
                                    durationTextView.setText(searchRideResponse.getRideDuration());
                                }
                            }

                            if (binding != null && binding.reimbursementTextView != null) {
                                binding.reimbursementTextView.setText("Rs " + searchRideResponse.getReimbursementAmount());
                            } else {
                                TextView reimbursementTextView = (TextView) mActivity.findViewById(R.id.reimbursementTextView);
                                if (reimbursementTextView != null) {
                                    reimbursementTextView.setText("Rs " + searchRideResponse.getReimbursementAmount());
                                }
                            }
                            rideAdapter = new ArrayAdapter<String>(mActivity, R.layout.spinner_item, rides);
                            if (binding != null && binding.listOfRides != null) {
                                binding.listOfRides.setAdapter(rideAdapter);
                            } else {
                                Spinner spinner = mActivity.findViewById(R.id.listOfRides);
                                if (spinner != null) {
                                    spinner.setAdapter(rideAdapter);
                                }
                            }
                        });

                    } else if (response.code() == 423) {
                        if (MyService.isTrackingOn != null && MyService.isTrackingOn.getValue()) {

                            MyService.isTrackingOn.observe(getActivity(), new Observer<Boolean>() {
                                @Override
                                public void onChanged(Boolean aBoolean) {
                                    if (!aBoolean) {
                                        logout();
                                    }
                                }
                            });

                            Bitmap bitmap = TrackerUtility.loadBitmapFromView(mActivity, getView());
                            File screenshot = TrackerUtility.takeScreen(mActivity, getView(), bitmap);
                            Intent intent = new Intent(mActivity, MyService.class);
                            intent.setAction(MyService.STOP_SERVICE);
                            intent.putExtra("screenshot_path", screenshot.getAbsolutePath());
                            mActivity.startService(intent);
                        } else {
                            logout();
                        }
                    } else {
                        Toast.makeText(mActivity, "No Record found", Toast.LENGTH_SHORT).show();
                    }
                    if (showLoader) {
                        finalDialog.dismiss();
                    }
                    mActivity.runOnUiThread(() -> {
                        if (binding != null && binding.swipeRefreshLayout != null) {
                            binding.swipeRefreshLayout.setRefreshing(false);
                        } else {
                            if (mActivity != null) {
                                SwipeRefreshLayout swipeRefreshLayout = mActivity.findViewById(R.id.swipeRefreshLayout);
                                if (swipeRefreshLayout != null) {
                                    swipeRefreshLayout.setRefreshing(false);
                                }
                            }
                        }
                    });
                }

                @Override
                public void onFailure(Call<SearchRideResponse> call, Throwable t) {
                    Toast.makeText(mActivity, "No Record found", Toast.LENGTH_SHORT).show();
                    if (showLoader) {
                        finalDialog.dismiss();
                    }
                    mActivity.runOnUiThread(() -> {
                        if (binding != null && binding.statisticCardView != null) {
                            binding.statisticCardView.setVisibility(View.GONE);
                        } else {
                            if (mActivity != null) {
                                View view = mActivity.findViewById(R.id.statisticCardView);
                                if (view != null) {
                                    view.setVisibility(View.GONE);
                                }
                            }
                        }

                        if (binding != null && binding.errorAnimation != null) {
                            binding.errorAnimation.setVisibility(View.VISIBLE);
                        } else {
                            if (mActivity != null) {
                                View view = mActivity.findViewById(R.id.errorAnimation);
                                if (view != null) {
                                    view.setVisibility(View.VISIBLE);
                                }
                            }
                        }

                        if (binding != null && binding.swipeRefreshLayout != null) {
                            binding.swipeRefreshLayout.setRefreshing(false);
                        } else {
                            if (mActivity != null) {
                                SwipeRefreshLayout swipeRefreshLayout = mActivity.findViewById(R.id.swipeRefreshLayout);
                                if (swipeRefreshLayout != null) {
                                    swipeRefreshLayout.setRefreshing(false);
                                }
                            }
                        }
                    });
                }
            });
        }
    }

    private void logout() {
        new Handler().post(() -> {
            SharedPreferences sharedPreferences = mActivity.getSharedPreferences(LocationApp.APP_NAME, MODE_PRIVATE);
            sharedPreferences.edit().clear().commit();
            AppExecutors.getInstance().getDiskIO().execute(()->{
                DatabaseClient.getInstance(mActivity).getTripDatabase().clearAllTables();
            });
            Intent intent = new Intent(mActivity, LoginActivity.class);
            startActivity(intent);
            mActivity.finish();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
        Date today;
        Calendar c = Calendar.getInstance();

        switch (categories.get(i)) {
            case TODAY:
                //today = TrackerUtility.convertStringToDate("2022-12-27");
                today = c.getTime();
                String sToday = TrackerUtility.getDateString(today);
                searchRideFilter = new SearchRideFilter(sToday, sToday);
                searchRidesByDateRange(searchRideFilter);
                break;
            case YESTERDAY:
                today = c.getTime();
                c = Calendar.getInstance();
                c.setTime(new Date());
                c.add(Calendar.DATE, -1);
                Date yesterday = c.getTime();
                String sYesterday = TrackerUtility.getDateString(yesterday);
                searchRideFilter = new SearchRideFilter(sYesterday, sYesterday);
                searchRidesByDateRange(searchRideFilter);
                break;
            case WEEK:
                today = Calendar.getInstance().getTime();
                c = Calendar.getInstance();
                c.setTime(new Date());
                c.add(Calendar.DATE, -7);
                Date weekRange = c.getTime();
                String sWeek = TrackerUtility.getDateString(weekRange);
                String stoday = TrackerUtility.getDateString(today);
                searchRideFilter = new SearchRideFilter(sWeek, stoday);
                searchRidesByDateRange(searchRideFilter);
                break;
            case MONTH:
                today = Calendar.getInstance().getTime();
                c = Calendar.getInstance();
                c.setTime(new Date());
                c.add(Calendar.MONTH, -1);
                Date monthRange = c.getTime();
                String sMonth = TrackerUtility.getDateString(monthRange);
                stoday = TrackerUtility.getDateString(today);
                searchRideFilter = new SearchRideFilter(sMonth, stoday);
                searchRidesByDateRange(searchRideFilter);
                break;
            case YEAR:
                today = Calendar.getInstance().getTime();
                c = Calendar.getInstance();
                c.setTime(new Date());
                c.add(Calendar.YEAR, -1);
                Date yearRange = c.getTime();

                String sYear = TrackerUtility.getDateString(yearRange);
                stoday = TrackerUtility.getDateString(today);
                searchRideFilter = new SearchRideFilter(sYear, stoday);
                searchRidesByDateRange(searchRideFilter);
                break;
            case CUSTOM_DATE:
                if (!showDatePopup) {
                    showLogoutAlertDialog();
                }
                showDatePopup = false;
                break;
            default:
                //today = TrackerUtility.convertStringToDate("2022-12-27");
                today = c.getTime();
                sToday = TrackerUtility.getDateString(today);
                searchRideFilter = new SearchRideFilter(sToday, sToday);
                searchRidesByDateRange(searchRideFilter);
        }

    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {

    }

    private void showLogoutAlertDialog() {
        customStartDate = TrackerUtility.getDateString(Calendar.getInstance().getTime());
        customEndDate = TrackerUtility.getDateString(Calendar.getInstance().getTime());

        MaterialAlertDialogBuilder alertDialogBuilder = new MaterialAlertDialogBuilder(mActivity);
        alertDialogBuilder.setCancelable(true);
        alertDialogBuilder.setView(R.layout.date_picker_layout);
        /*alertDialogBuilder.setPositiveButton("OK", (dialogInterface, i) -> {
            SearchRideFilter searchRideFilter = new SearchRideFilter(customStartDate, customEndDate);
            searchRidesByDateRange(searchRideFilter);
        });
        alertDialogBuilder.setNegativeButton("CANCEL", (dialogInterface, i) -> dialogInterface.dismiss());*/
        AlertDialog alertDialog = alertDialogBuilder.show();

        alertDialog.findViewById(R.id.okAlertButton).setOnClickListener(view-> {
            SearchRideFilter searchRideFilter = new SearchRideFilter(customStartDate, customEndDate);
            searchRidesByDateRange(searchRideFilter);
            alertDialog.dismiss();
        });

        alertDialog.findViewById(R.id.cancelAlertButton).setOnClickListener(view-> {
            alertDialog.dismiss();
        });

        DatePickerDialog.OnDateSetListener fromDateListener = (datePicker, i, i1, i2) -> {
            Calendar mCalendar = Calendar.getInstance();
            mCalendar.set(Calendar.YEAR, i);
            mCalendar.set(Calendar.MONTH, i1);
            mCalendar.set(Calendar.DAY_OF_MONTH, i2);
            customStartDate = TrackerUtility.getDateString(mCalendar.getTime());
            ((TextView)alertDialog.findViewById(R.id.fromDateTextView)).setText(customStartDate);

        };

        DatePickerDialog.OnDateSetListener toDateListener = (datePicker, i, i1, i2) -> {
            Calendar mCalendar = Calendar.getInstance();
            mCalendar.set(Calendar.YEAR, i);
            mCalendar.set(Calendar.MONTH, i1);
            mCalendar.set(Calendar.DAY_OF_MONTH, i2);

            if (!TrackerUtility.convertStringToDate(customStartDate).before(mCalendar.getTime())) {
                Toast.makeText(mActivity, "ToDate can not be less than FromDate", Toast.LENGTH_SHORT).show();
                return;
            }
            customEndDate = TrackerUtility.getDateString(mCalendar.getTime());
            ((TextView)alertDialog.findViewById(R.id.toDateTextView)).setText(customEndDate);
        };

        String sToDate = TrackerUtility.getDateString(Calendar.getInstance().getTime());
        ((TextView)alertDialog.findViewById(R.id.toDateTextView)).setText(sToDate);
        ((TextView)alertDialog.findViewById(R.id.fromDateTextView)).setText(sToDate);

        alertDialog.findViewById(R.id.fromDateTextView).setOnClickListener(view -> {
            //Toast.makeText(mActivity, "From date clicked", Toast.LENGTH_SHORT).show();
            showCalender(fromDateListener);
        });

        alertDialog.findViewById(R.id.toDateTextView).setOnClickListener(view -> {
            //Toast.makeText(mActivity, "To date clicked", Toast.LENGTH_SHORT).show();
            showCalender(toDateListener);
        });
    }

    private void showCalender(DatePickerDialog.OnDateSetListener dateListener) {
        Calendar mCalendar = Calendar.getInstance();
        int year = mCalendar.get(Calendar.YEAR);
        int month = mCalendar.get(Calendar.MONTH);
        int dayOfMonth = mCalendar.get(Calendar.DAY_OF_MONTH);
        new DatePickerDialog(mActivity, dateListener, year, month, dayOfMonth).show();
    }
}