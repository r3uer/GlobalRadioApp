package com.example.globalradioapp.dialog;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.NumberPicker;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.example.globalradioapp.R;

public class SleepTimerDialog extends DialogFragment {

    public interface OnTimerSetListener {
        void onTimerSet(int minutes);
    }

    private OnTimerSetListener listener;

    public void setOnTimerSetListener(OnTimerSetListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_sleep_timer, null);

        NumberPicker numberPicker = view.findViewById(R.id.numberPickerMinutes);
        numberPicker.setMinValue(1);
        numberPicker.setMaxValue(120);
        numberPicker.setValue(30);

        return new AlertDialog.Builder(requireContext())
                .setTitle("Sleep Timer")
                .setView(view)
                .setPositiveButton("Set Timer", (dialog, which) -> {
                    if (listener != null) {
                        listener.onTimerSet(numberPicker.getValue());
                    }
                })
                .setNegativeButton("Cancel", null)
                .create();
    }
}
