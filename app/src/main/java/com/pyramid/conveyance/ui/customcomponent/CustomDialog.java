package com.pyramid.conveyance.ui.customcomponent;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import com.pyramid.conveyance.api.response.RideReason;
import com.pyramid.conveyance.R;

import java.util.ArrayList;
import java.util.List;

public class CustomDialog {
    int selectedPosition = -1;
    Context context;

//    boolean useGps = true;
    boolean isCancelled = false;
    ArrayList<RideReason> reasonArrayList;
    public CustomDialog(Context context, ArrayList<RideReason> reasonArrayList) {
        this.context = context;
        this.reasonArrayList = reasonArrayList;
    }
    public void show(DialogInterface.OnDismissListener listener) {
        Dialog dialog = new Dialog(context, R.style.Theme_Dialog);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(false);
        dialog.setContentView(R.layout.alert_layout);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        List<String> spinnerArray =  new ArrayList<String>();
       /* spinnerArray.add("Ride Reason 1 to different location than normal visit Ride Reason 1 to different location than normal visit");
        spinnerArray.add("Ride Reason 2 to different location than normal visit");
        spinnerArray.add("Ride Reason 3");*/

        reasonArrayList.stream().forEach(reason -> spinnerArray.add(reason.getPurpose()));

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                context, R.layout.spinner_layout, spinnerArray);

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        Spinner sItems = dialog.findViewById(R.id.mySpinner);
//        RadioButton useGpsRadioBtn = dialog.findViewById(R.id.gpslocation);
//        RadioButton useFusedRadioBtn = dialog.findViewById(R.id.fusedlocation);
//        if (useGpsRadioBtn!= null) {
//            useGpsRadioBtn.setOnClickListener(v -> useGps = useGpsRadioBtn.isChecked());
//        }
//        if (useFusedRadioBtn!= null) {
//            useFusedRadioBtn.setOnClickListener(v -> useGps = !useFusedRadioBtn.isChecked());
//        }

        sItems.setAdapter(adapter);

        sItems.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1,
                                       int position, long arg3) {
                //String imc_met= sItems.getSelectedItem().toString();
                selectedPosition = position;
                //Toast.makeText(context,position + "position : " +arg3 + " "+ imc_met, Toast.LENGTH_LONG).show();
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
                // TODO Auto-generated method stub

            }
        });

        Button cancelBtn = dialog.findViewById(R.id.cancelAlertButton);
        cancelBtn.setOnClickListener(view -> {
            isCancelled = true;
            dialog.dismiss();
            listener.onDismiss(dialog);
        });

        Button okBtn = dialog.findViewById(R.id.okAlertButton);
        okBtn.setOnClickListener(view -> {
            isCancelled = false;
            dialog.dismiss();
            listener.onDismiss(dialog);
        });

        dialog.show();
    }

    public int getSelectedPosition() {
        return selectedPosition;
    }

    public boolean isCancelled() {
        return isCancelled;
    }

//    public boolean isUseGps() {
//        return useGps;
//    }
//
//    public boolean isCancelled() {
//        return useGps;
//    }
}

