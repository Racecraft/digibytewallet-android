package io.digibyte.presenter.fragments;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.LayoutTransition;
import android.animation.ObjectAnimator;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;

import io.digibyte.DigiByte;
import io.digibyte.R;
import io.digibyte.databinding.FragmentBreadPinBinding;
import io.digibyte.presenter.fragments.interfaces.OnBackPressListener;
import io.digibyte.presenter.fragments.interfaces.PinFragmentCallback;
import io.digibyte.presenter.fragments.models.PinFragmentViewModel;
import io.digibyte.presenter.interfaces.BRAuthCompletion;
import io.digibyte.tools.animation.BRAnimator;
import io.digibyte.tools.animation.SpringAnimator;
import io.digibyte.tools.security.AuthManager;
import io.digibyte.tools.security.BRKeyStore;

/**
 * BreadWallet
 * <p>
 * Created by Mihail Gutan <mihail@breadwallet.com> on 6/29/15.
 * Copyright (c) 2016 breadwallet LLC
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

public class FragmentPin extends Fragment implements OnBackPressListener {
    private static final String TAG = FragmentPin.class.getName();

    private BRAuthCompletion completion;
    private FragmentBreadPinBinding binding;
    private final StringBuilder pin = new StringBuilder();

    private PinFragmentCallback mPinFragmentCallback = new PinFragmentCallback() {
        @Override
        public void onClick(String key) {
            handleClick(key);
        }

        @Override
        public void onCancelClick() {
            fadeOutRemove(false);
        }
    };


    public static void show(AppCompatActivity activity, String title, String message, BRAuthCompletion completion) {
        FragmentPin fragmentPin = new FragmentPin();
        fragmentPin.setCompletion(completion);
        Bundle args = new Bundle();
        args.putString("title", title);
        args.putString("message", message);
        fragmentPin.setArguments(args);
        FragmentTransaction transaction = activity.getSupportFragmentManager().beginTransaction();
        transaction.setCustomAnimations(R.animator.from_bottom, R.animator.to_bottom, R.animator.from_bottom, R.animator.to_bottom);
        transaction.add(android.R.id.content, fragmentPin, FragmentPin.class.getName());
        transaction.addToBackStack(FragmentPin.class.getName());
        transaction.commitAllowingStateLoss();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        binding = FragmentBreadPinBinding.inflate(inflater);
        binding.setCallback(mPinFragmentCallback);
        binding.mainLayout.getLayoutTransition().enableTransitionType(LayoutTransition.CHANGING);
        Bundle bundle = getArguments();
        PinFragmentViewModel viewModel = new PinFragmentViewModel();
        viewModel.setTitle(bundle.getString("title"));
        viewModel.setMessage(bundle.getString("message"));
        binding.setData(viewModel);
        binding.executePendingBindings();
        return binding.getRoot();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        ObjectAnimator colorFade = BRAnimator.animateBackgroundDim(binding.mainLayout, false, null);
        colorFade.setStartDelay(350);
        colorFade.setDuration(500);
        colorFade.start();
        Animator upFromBottom = AnimatorInflater.loadAnimator(getContext(), R.animator.from_bottom);
        upFromBottom.setTarget(binding.brkeyboard);
        upFromBottom.setDuration(1000);
        upFromBottom.setInterpolator(new DecelerateInterpolator());
        upFromBottom.start();
    }

    @Override
    public void onResume() {
        super.onResume();
        updateDots();
    }


    private void handleClick(@Nullable final String key) {
        if (key == null) {
            Log.e(TAG, "handleClick: key is null! ");
            return;
        }
        if (key.isEmpty()) {
            handleDeleteClick();
        } else if (Character.isDigit(key.charAt(0))) {
            handleDigitClick(Integer.parseInt(key.substring(0, 1)));
        } else {
            Log.e(TAG, "handleClick: oops: " + key);
        }
    }

    private void handleDigitClick(@NonNull final Integer dig) {
        if (pin.length() < 6) {
            pin.append(dig);
        }
        updateDots();
    }

    private void handleDeleteClick() {
        if (pin.length() > 0) {
            pin.deleteCharAt(pin.length() - 1);
        }
        updateDots();
    }

    private void updateDots() {
        AuthManager.getInstance().updateDots(pin.toString(), binding.dot1,
                binding.dot2, binding.dot3, binding.dot4, binding.dot5, binding.dot6, () -> {
                    if (AuthManager.getInstance().checkAuth(pin.toString(), getContext())) {
                        fadeOutRemove(true);
                    } else {
                        SpringAnimator.failShakeAnimation(getActivity(), binding.pinLayout);
                        new Handler().postDelayed(() -> updateDots(), 500);
                        AuthManager.getInstance().authFail(getActivity());
                    }
                    pin.delete(0, pin.length());
                });
    }

    public void setCompletion(BRAuthCompletion completion) {
        this.completion = completion;
    }

    private void fadeOutRemove(boolean authenticated) {
        ObjectAnimator colorFade = BRAnimator.animateBackgroundDim(binding.mainLayout, true, null);
        colorFade.setDuration(500);
        Animator downToBottom = AnimatorInflater.loadAnimator(DigiByte.getContext(), R.animator.to_bottom);
        downToBottom.setTarget(binding.brkeyboard);
        downToBottom.setDuration(500);
        downToBottom.setInterpolator(new DecelerateInterpolator());
        AnimatorSet set = new AnimatorSet();
        set.playSequentially(colorFade, downToBottom);
        set.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                remove();
                if (authenticated) {
                    Handler handler = new Handler(Looper.myLooper());
                    handler.postDelayed(() -> {
                        if (completion != null) completion.onComplete();
                    }, 350);
                }
            }
        });
        set.start();
    }

    private void remove() {
        if (getFragmentManager() == null) {
            return;
        }
        getFragmentManager().popBackStack();
    }

    @Override
    public void onBackPressed() {
        fadeOutRemove(false);
    }
}