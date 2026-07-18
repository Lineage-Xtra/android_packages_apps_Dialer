/*
 * Copyright (C) 2016 The Android Open Source Project
 * Copyright (C) 2023 The LineageOS Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.incallui.incall.impl;

import android.content.Context;
import android.os.Bundle;
import android.util.ArraySet;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.android.dialer.R;
import com.android.dialer.common.Assert;
import com.android.dialer.common.FragmentUtils;
import com.android.incallui.incall.protocol.InCallButtonIds;

import java.util.List;
import java.util.Set;

/** Fragment for the in call buttons (mute, speaker, ect.). */
public class InCallButtonGridFragment extends Fragment {

  // Updated to 4 buttons in a single row
  private static final int BUTTON_COUNT = 4;
  private static final int BUTTONS_PER_ROW = 4;

  private final CheckableLabeledButton[] buttons = new CheckableLabeledButton[BUTTON_COUNT];
  private OnButtonGridCreatedListener buttonGridListener;

  private OnBackPressedCallback moreMenuBackCallback;

  public static Fragment newInstance() {
    return new InCallButtonGridFragment();
  }

  @Override
  public void onCreate(@Nullable Bundle bundle) {
    super.onCreate(bundle);
    buttonGridListener = FragmentUtils.getParent(this, OnButtonGridCreatedListener.class);
    Assert.isNotNull(buttonGridListener);

    moreMenuBackCallback = new OnBackPressedCallback(false) {
        @Override
        public void handleOnBackPressed() {
            hideMoreMenu();
        }
    };
    requireActivity().getOnBackPressedDispatcher().addCallback(this, moreMenuBackCallback);
  }

  @Nullable
  @Override
  public View onCreateView(
      LayoutInflater inflater, @Nullable ViewGroup parent, @Nullable Bundle bundle) {
    View view = inflater.inflate(R.layout.incall_button_grid, parent, false);

    buttons[0] = ((CheckableLabeledButton) view.findViewById(R.id.incall_first_button));
    buttons[1] = ((CheckableLabeledButton) view.findViewById(R.id.incall_second_button));
    buttons[2] = ((CheckableLabeledButton) view.findViewById(R.id.incall_third_button));
    buttons[3] = ((CheckableLabeledButton) view.findViewById(R.id.incall_fourth_button));

    buttons[3].setVisibility(View.INVISIBLE);

    try {
        for (int i = 0; i < buttons[3].getChildCount(); i++) {
            View child = buttons[3].getChildAt(i);
            if (child instanceof android.widget.ImageView) {
                ((android.widget.ImageView) child).setImageResource(R.drawable.quantum_ic_more_vert_vd_theme_24);
                break;
            }
        }
    } catch (Exception e) {
        e.printStackTrace();
    }

    // Hardcode the 4th button to trigger the custom animated menu
    buttons[3].setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            showMoreMenu();
        }
    });

    return view;
  }

  private void showMoreMenu() {
      FrameLayout container = getActivity().findViewById(R.id.more_options_container);
      if (container == null) return;

      if (container.getChildCount() == 0) {

          Context themeContext = new ContextThemeWrapper(getContext(), android.R.style.Theme_DeviceDefault_DayNight);
          View menu = LayoutInflater.from(themeContext).inflate(R.layout.more_options_menu, container, false);

          menu.findViewById(R.id.menu_close).setOnClickListener(v -> hideMoreMenu());

          menu.findViewById(R.id.option_add_call).setOnClickListener(v -> {
              triggerButtonController(InCallButtonIds.BUTTON_ADD_CALL);
              hideMoreMenu();
          });

          menu.findViewById(R.id.option_hold).setOnClickListener(v -> {
              triggerButtonController(InCallButtonIds.BUTTON_HOLD);
              hideMoreMenu();
          });

          menu.findViewById(R.id.option_record).setOnClickListener(v -> {
              triggerButtonController(InCallButtonIds.BUTTON_RECORD_CALL);
              hideMoreMenu();
          });

          menu.findViewById(R.id.option_video_call).setOnClickListener(v -> {
              triggerButtonController(InCallButtonIds.BUTTON_UPGRADE_TO_VIDEO);
              hideMoreMenu();
          });

          container.addView(menu);
      }

      updateMoreMenuStates();

      container.setVisibility(View.VISIBLE);
      container.setTranslationY(800f);
      container.animate().translationY(0f).setDuration(250).start();

      moreMenuBackCallback.setEnabled(true);
  }

  private void hideMoreMenu() {
      FrameLayout container = getActivity().findViewById(R.id.more_options_container);
      if (container != null && container.getVisibility() == View.VISIBLE) {
          container.animate().translationY(container.getHeight()).setDuration(250).withEndAction(() -> {
              container.setVisibility(View.GONE);
              moreMenuBackCallback.setEnabled(false);
          }).start();
      }
  }

  private void updateMoreMenuStates() {
      if (getActivity() == null) return;
      FrameLayout container = getActivity().findViewById(R.id.more_options_container);

      if (container != null && container.getChildCount() > 0) {
          View menu = container.getChildAt(0);
          updateMenuOptionState(menu.findViewById(R.id.option_add_call), InCallButtonIds.BUTTON_ADD_CALL);
          updateMenuOptionState(menu.findViewById(R.id.option_hold), InCallButtonIds.BUTTON_HOLD);
          updateMenuOptionState(menu.findViewById(R.id.option_record), InCallButtonIds.BUTTON_RECORD_CALL);
          updateMenuOptionState(menu.findViewById(R.id.option_video_call), InCallButtonIds.BUTTON_UPGRADE_TO_VIDEO);
      }
  }

  private void updateMenuOptionState(View optionView, @InCallButtonIds int buttonId) {
      if (optionView == null || buttonGridListener == null) return;

      ButtonController controller = buttonGridListener.getButtonController(buttonId);
      if (controller != null) {
          boolean isEnabled = controller.isEnabled() && controller.isAllowed();
          optionView.setEnabled(isEnabled);
          optionView.setAlpha(isEnabled ? 1.0f : 0.4f);
      } else {
          optionView.setEnabled(false);
          optionView.setAlpha(0.4f);
      }
  }

  /**
   * Safe helper method to grab an existing AOSP dynamic button controller,
   * cast it, and programmatically simulate a physical button press action.
   */
  private void triggerButtonController(@InCallButtonIds int buttonId) {
    if (buttonGridListener != null) {
      ButtonController controller = buttonGridListener.getButtonController(buttonId);
      if (controller != null) {
          // Create a dummy button, let the controller attach its listener to it,
          // perform the click to trigger the native dialer action, and unbind.
          CheckableLabeledButton dummyButton = new CheckableLabeledButton(getContext());
          controller.setButton(dummyButton);
          dummyButton.performClick();

          // Clean up to prevent memory leaks or ghost states
          controller.setButton(null);
      }
    }
  }

  @Override
  public void onViewCreated(View view, @Nullable Bundle bundle) {
    super.onViewCreated(view, bundle);
    buttonGridListener.onButtonGridCreated(this);
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    buttonGridListener.onButtonGridDestroyed();
  }

  public void onInCallScreenDialpadVisibilityChange(boolean isShowing) {
    for (CheckableLabeledButton button : buttons) {
      button.setImportantForAccessibility(
          isShowing
              ? View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
              : View.IMPORTANT_FOR_ACCESSIBILITY_AUTO);
    }
  }

  public int updateButtonStates(
      List<ButtonController> buttonControllers,
      @Nullable ButtonChooser buttonChooser,
      int voiceNetworkType,
      int phoneType) {
    Set<Integer> allowedButtons = new ArraySet<>();
    Set<Integer> disabledButtons = new ArraySet<>();
    for (ButtonController controller : buttonControllers) {
      if (controller.isAllowed()) {
        allowedButtons.add(controller.getInCallButtonId());
        if (!controller.isEnabled()) {
          disabledButtons.add(controller.getInCallButtonId());
        }
      }
    }

    for (ButtonController controller : buttonControllers) {
      controller.setButton(null);
    }

    if (buttonChooser == null) {
      buttonChooser =
          ButtonChooserFactory.newButtonChooser(voiceNetworkType, false, phoneType);
    }

    int numVisibleButtons = getResources().getInteger(R.integer.incall_num_rows) * BUTTONS_PER_ROW;
    List<Integer> buttonsToPlace =
        buttonChooser.getButtonPlacement(numVisibleButtons, allowedButtons, disabledButtons);

    // Limit dynamic placement to the first 3 buttons. 4th is reserved for "More".
    for (int i = 0; i < 3; ++i) {
      if (i >= buttonsToPlace.size()) {
        buttons[i].setVisibility(View.INVISIBLE);
        continue;
      }
      @InCallButtonIds int button = buttonsToPlace.get(i);
      buttonGridListener.getButtonController(button).setButton(buttons[i]);
    }

    if (buttonsToPlace.size() > 0) {
        buttons[3].setVisibility(View.VISIBLE);
    } else {
        buttons[3].setVisibility(View.INVISIBLE);
    }

    updateMoreMenuStates();

    return numVisibleButtons;
  }

  /** Interface to let the listener know the status of the button grid. */
  public interface OnButtonGridCreatedListener {
    void onButtonGridCreated(InCallButtonGridFragment inCallButtonGridFragment);
    void onButtonGridDestroyed();

    ButtonController getButtonController(@InCallButtonIds int id);
  }
}
