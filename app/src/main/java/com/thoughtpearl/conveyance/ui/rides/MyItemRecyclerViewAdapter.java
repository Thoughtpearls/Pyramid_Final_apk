package com.thoughtpearl.conveyance.ui.rides;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.thoughtpearl.conveyance.api.response.Ride;
import com.thoughtpearl.conveyance.databinding.FragmentRideDetailsBinding;
import com.thoughtpearl.conveyance.ui.rides.placeholder.PlaceholderContent.PlaceholderItem;
import com.thoughtpearl.conveyance.utility.TrackerUtility;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * {@link RecyclerView.Adapter} that can display a {@link PlaceholderItem}.
 * TODO: Replace the implementation with code for your data type.
 */
public class MyItemRecyclerViewAdapter extends RecyclerView.Adapter<MyItemRecyclerViewAdapter.ViewHolder> {

    public static final String YYYY_MM_DD_HH_MM_SS = "yyyy-MM-dd HH:mm:ss";
    public static final String DD_MMM_YYYY = "dd MMM yyyy";
    //private final List<TripRecordLocationRelation> mValues;
    private final List<Ride>  mValues;
    private final Context context;

    public MyItemRecyclerViewAdapter(List<Ride>  items, Context context) {
        mValues = items;
        this.context = context;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        return new ViewHolder(FragmentRideDetailsBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));

    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        holder.mItem = mValues.get(position);
        /*holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(view.getContext(), "Test", Toast.LENGTH_LONG).show();
            }
        });*/
        Ride ride = mValues.get(position);
        String sStartDate = ride.getRideDate() + " " + ride.getRideStartTime();
        Date startDate = TrackerUtility.convertStringToDate(sStartDate, YYYY_MM_DD_HH_MM_SS);
        if (!(ride.getRideDate() == null || ride.getRideEndTime() == null || ride.getRideStartTime() == null)) {
            String sEndDate = ride.getRideDate() + " " + ride.getRideEndTime();
            Date endDate = TrackerUtility.convertStringToDate(sEndDate, YYYY_MM_DD_HH_MM_SS);
            long duration = endDate.getTime() - startDate.getTime();
            if (duration > 0) {
                //holder.duration.setCompoundDrawableTintList(ColorStateList.valueOf(context.getColor(R.color.green)));
                holder.duration.setText("" + TrackerUtility.getDurationBreakdown(duration));
            } else {
                holder.duration.setText("0sec");
            }
        } else {
            //holder.duration.setCompoundDrawableTintList(ColorStateList.valueOf(context.getColor(R.color.orange)));
            holder.duration.setText("N/A");
        }

        if (ride.getRideEndTime() == null) {
            holder.incompleteIcon.setVisibility(View.VISIBLE);
        }

        holder.distance.setText(TrackerUtility.roundOffDoubleToString(ride.getRideDistance()) + " Km");
        //Date date = new Date(tripRecord.getStartTimestamp());
        String pattern = DD_MMM_YYYY;
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
        holder.rideDate.setText(simpleDateFormat.format(startDate));
        if (ride.getReimbursementCost() != null) {
            holder.rideApprovedAmount.setVisibility(View.VISIBLE);
            holder.rideApprovedAmountIcon.setVisibility(View.GONE);
            holder.rideApprovedAmount.setText("Rs " + ride.getReimbursementCost().toString());
        } else {
            holder.rideApprovedAmount.setVisibility(View.GONE);
            holder.rideApprovedAmountIcon.setVisibility(View.VISIBLE);
            //Drawable resImg = this.context.getResources().getDrawable(R.drawable.processing);
            //holder.rideApprovedAmount.setCompoundDrawables(resImg,  null, null, null);
        }

    }

    @Override
    public int getItemCount() {
        return mValues.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public final TextView duration;
        public final TextView distance;
        public final TextView rideDate;
        public final TextView rideApprovedAmount;
        public final TextView rideApprovedAmountIcon;
        public final ImageView incompleteIcon;
        public Ride mItem;

        public ViewHolder(FragmentRideDetailsBinding binding) {
            super(binding.getRoot());
            duration = binding.totaldurationTextView;
            distance = binding.totalDistance;
            rideDate = binding.rideDate;
            rideApprovedAmount = binding.approvedAmount;
            rideApprovedAmountIcon = binding.approvedAmountIcon;
            incompleteIcon = binding.incompleteIcon;
        }

        @Override
        public String toString() {
            return super.toString() + " '" + distance.getText() + "'";
        }
    }
}