/*
 * SPDX-FileCopyrightText: 2026 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.incallui.incall.impl;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.dialer.R;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class MoreOptionsBottomSheet extends BottomSheetDialogFragment {

    // Define the interface to communicate with InCallButtonGridFragment
    public interface Listener {
        void onAddCallClicked();
        void onHoldClicked();
        void onVideoCallClicked();
    }

    private Listener listener;

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.more_options_bottom_sheet, container, false);

        view.findViewById(R.id.bottom_sheet_close).setOnClickListener(v -> dismiss());

        view.findViewById(R.id.option_add_call).setOnClickListener(v -> {
            if (listener != null) listener.onAddCallClicked();
            dismiss();
        });

        view.findViewById(R.id.option_hold).setOnClickListener(v -> {
            if (listener != null) listener.onHoldClicked();
            dismiss();
        });

        view.findViewById(R.id.option_video_call).setOnClickListener(v -> {
            if (listener != null) listener.onVideoCallClicked();
            dismiss();
        });

        return view;
    }
}
