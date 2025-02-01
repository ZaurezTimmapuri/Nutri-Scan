package com.example.nutri_scan.ui;
import android.app.Activity;
import android.app.AlertDialog;
import android.view.LayoutInflater;

import com.example.nutri_scan.R;

public class ValidatingDialog {

    private Activity activity;
    private AlertDialog dialog;

    ValidatingDialog(Activity myActivity){
        activity = myActivity;
    }

    void start_validation_Dialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);

        LayoutInflater inflater = activity.getLayoutInflater();
        builder.setView(inflater.inflate(R.layout.dialog_validating,null));
        builder.setCancelable(false);

        dialog = builder.create();
        dialog.show();

    }
    void validation_dismiss_Dialog(){
        dialog.dismiss();
    }
}