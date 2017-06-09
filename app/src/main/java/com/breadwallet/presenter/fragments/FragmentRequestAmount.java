package com.breadwallet.presenter.fragments;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ArgbEvaluator;
import android.animation.LayoutTransition;
import android.animation.ValueAnimator;
import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.OvershootInterpolator;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.breadwallet.R;
import com.breadwallet.presenter.customviews.BRKeyboard;
import com.breadwallet.presenter.customviews.BRToast;
import com.breadwallet.tools.adapter.CurAdapter;
import com.breadwallet.tools.animation.BRAnimator;
import com.breadwallet.tools.animation.SlideDetector;
import com.breadwallet.tools.animation.SpringAnimator;
import com.breadwallet.tools.listeners.RecyclerItemClickListener;
import com.breadwallet.tools.manager.BRClipboardManager;
import com.breadwallet.tools.manager.SharedPreferencesManager;
import com.breadwallet.tools.qrcode.QRUtils;
import com.breadwallet.tools.sqlite.CurrencyDataSource;
import com.breadwallet.tools.util.BRExchange;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.tools.util.BRCurrency;
import com.breadwallet.tools.util.Utils;
import com.breadwallet.wallet.BRWalletManager;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static com.breadwallet.R.id.iso;


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

public class FragmentRequestAmount extends Fragment {
    private static final String TAG = FragmentRequestAmount.class.getName();
    private BRKeyboard keyboard;
    private StringBuilder amountBuilder;
    private TextView isoText;
    private EditText amountEdit;
    public TextView mTitle;
    public TextView mAddress;
    public ImageView mQrImage;
    public ScrollView backgroundLayout;
    public LinearLayout signalLayout;
    private String receiveAddress;
    private View shareSeparator;
    private View separator;
    private View keyboardSeparator;
    private Button shareButton;
    private Button shareEmail;
    private Button shareTextMessage;
    private LinearLayout shareButtonsLayout;
    private boolean shareButtonsShown = true;
    private int keyboardPosition = 0;
    private String selectedIso;
    private CurAdapter curAdapter;
    private Button isoButton;
    private RecyclerView currencyRecycler;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_request, container, false);
        backgroundLayout = (ScrollView) rootView.findViewById(R.id.background_layout);
        signalLayout = (LinearLayout) rootView.findViewById(R.id.signal_layout);
        keyboard = (BRKeyboard) rootView.findViewById(R.id.keyboard);
        keyboard.setBRButtonBackgroundResId(R.drawable.keyboard_white_button);
        keyboard.setBRKeyboardColor(R.color.white);
        isoText = (TextView) rootView.findViewById(R.id.iso_text);
        amountEdit = (EditText) rootView.findViewById(R.id.amount_edit);
        amountBuilder = new StringBuilder(0);
        currencyRecycler = (RecyclerView) rootView.findViewById(R.id.cur_spinner);
        isoButton = (Button) rootView.findViewById(R.id.iso_button);
        mTitle = (TextView) rootView.findViewById(R.id.title);
        mAddress = (TextView) rootView.findViewById(R.id.address_text);
        mQrImage = (ImageView) rootView.findViewById(R.id.qr_image);
        shareButton = (Button) rootView.findViewById(R.id.share_button);
        shareEmail = (Button) rootView.findViewById(R.id.share_email);
        shareTextMessage = (Button) rootView.findViewById(R.id.share_text);
        shareButtonsLayout = (LinearLayout) rootView.findViewById(R.id.share_buttons_layout);
        shareSeparator = rootView.findViewById(R.id.share_separator);
        separator = rootView.findViewById(R.id.separator);
        keyboardSeparator = rootView.findViewById(R.id.view2);
        LayoutTransition layoutTransition = signalLayout.getLayoutTransition();
        layoutTransition.enableTransitionType(LayoutTransition.CHANGING);
        setListeners();

        final List<String> curList = new ArrayList<>();
        curList.add("BTC");
        curList.addAll(CurrencyDataSource.getInstance(getActivity()).getAllISOs());
        curAdapter = new CurAdapter(getContext(), curList);
        currencyRecycler.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        currencyRecycler.setAdapter(curAdapter);
        selectedIso = curAdapter.getItemAtPos(0);

        signalLayout.removeView(currencyRecycler);

        signalLayout.setOnTouchListener(new SlideDetector(getContext(), signalLayout));
        signalLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                removeCurrencySelector();
            }
        });
        keyboardPosition = signalLayout.indexOfChild(keyboard);
        updateText();
        signalLayout.setOnTouchListener(new SlideDetector(getContext(), signalLayout));

        return rootView;
    }

    private void setListeners() {

        amountEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                removeCurrencySelector();
                showKeyboard(true);
            }
        });

        currencyRecycler.addOnItemTouchListener(new RecyclerItemClickListener(getContext(),
                currencyRecycler, new RecyclerItemClickListener.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position, float x, float y) {
                Log.e(TAG, "onItemClick: " + position);
//                BRAnimator.showTransactionPager(BreadActivity.this, adapter.getItems(), position);
                SpringAnimator.springView(view);
                selectedIso = curAdapter.getItemAtPos(position);

                Log.e(TAG, "onItemSelected: " + selectedIso);
//                isoText.setText(BRCurrency.getSymbolByIso(getActivity(), selectedIso));
                SpringAnimator.springView(isoText);
                updateText();
                boolean generated = generateQrImage(receiveAddress, amountEdit.getText().toString(), selectedIso);
                if (!generated)
                    throw new RuntimeException("failed to generate qr image for address");
                removeCurrencySelector();
            }

            @Override
            public void onLongItemClick(View view, int position) {

            }
        }));

        mQrImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                removeCurrencySelector();
                showKeyboard(false);
            }
        });

        keyboard.addOnInsertListener(new BRKeyboard.OnInsertListener() {
            @Override
            public void onClick(String key) {
                removeCurrencySelector();
                handleClick(key);
            }
        });


        shareEmail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                removeCurrencySelector();
                if (!BRAnimator.isClickAllowed()) return;
                SpringAnimator.springView(v);
                showKeyboard(false);
                String iso = selectedIso;
                String strAmount = amountEdit.getText().toString();
                BigDecimal bigAmount = new BigDecimal((Utils.isNullOrEmpty(strAmount) || strAmount.equalsIgnoreCase(".")) ? "0" : strAmount);
                long amount = BRExchange.getSatoshisFromAmount(getActivity(), iso, bigAmount).longValue();
                String bitcoinUri = Utils.createBitcoinUrl(receiveAddress, amount, null, null, null);
                QRUtils.share("mailto:", getActivity(), bitcoinUri);

            }
        });
        shareTextMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                removeCurrencySelector();
                if (!BRAnimator.isClickAllowed()) return;
                SpringAnimator.springView(v);
                showKeyboard(false);
                String iso = selectedIso;
                String strAmount = amountEdit.getText().toString();
                BigDecimal bigAmount = new BigDecimal((Utils.isNullOrEmpty(strAmount) || strAmount.equalsIgnoreCase(".")) ? "0" : strAmount);
                long amount = BRExchange.getSatoshisFromAmount(getActivity(), iso, bigAmount).longValue();
                String bitcoinUri = Utils.createBitcoinUrl(receiveAddress, amount, null, null, null);
                QRUtils.share("sms:", getActivity(), bitcoinUri);
            }
        });
        shareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                removeCurrencySelector();
                if (!BRAnimator.isClickAllowed()) return;
                SpringAnimator.springView(v);
                toggleShareButtonsVisibility();
                showKeyboard(false);
            }
        });
        mAddress.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                removeCurrencySelector();
                if (!BRAnimator.isClickAllowed()) return;
                BRClipboardManager.putClipboard(getContext(), mAddress.getText().toString());
                BRToast.showCustomToast(getActivity(), "Copied to Clipboard.", (int) mAddress.getY(), Toast.LENGTH_SHORT, R.drawable.toast_layout_blue);
                showKeyboard(false);
            }
        });

        backgroundLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                removeCurrencySelector();
                if (!BRAnimator.isClickAllowed()) return;
                getActivity().onBackPressed();
            }
        });

        isoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SpringAnimator.springView(v);
                try {
                    signalLayout.addView(currencyRecycler, 3);
                } catch (IllegalStateException ex) {
                    signalLayout.removeView(currencyRecycler);
                }
            }
        });

    }

    private void toggleShareButtonsVisibility() {

        if (shareButtonsShown) {
            signalLayout.removeView(shareButtonsLayout);
            signalLayout.removeView(shareSeparator);
            shareButtonsShown = false;
        } else {
            signalLayout.addView(shareButtonsLayout, signalLayout.getChildCount());
            signalLayout.addView(shareSeparator, signalLayout.getChildCount());
            shareButtonsShown = true;
        }

    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final ViewTreeObserver observer = signalLayout.getViewTreeObserver();
        observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                observer.removeGlobalOnLayoutListener(this);
                BRAnimator.animateBackgroundDim(backgroundLayout, false);
                BRAnimator.animateSignalSlide(signalLayout, false, null);
                toggleShareButtonsVisibility();
            }
        });

        new Thread(new Runnable() {
            @Override
            public void run() {
                boolean success = BRWalletManager.refreshAddress(getActivity());
                if (!success) throw new RuntimeException("failed to retrieve address");

                receiveAddress = SharedPreferencesManager.getReceiveAddress(getActivity());

                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mAddress.setText(receiveAddress);
                        boolean generated = generateQrImage(receiveAddress, "0", "BTC");
                        if (!generated)
                            throw new RuntimeException("failed to generate qr image for address");
                    }
                });
            }
        }).start();

    }

    @Override
    public void onStop() {
        super.onStop();
        BRAnimator.animateBackgroundDim(backgroundLayout, true);
        BRAnimator.animateSignalSlide(signalLayout, true, new BRAnimator.OnSlideAnimationEnd() {
            @Override
            public void onAnimationEnd() {
                if (getActivity() != null) {
                    try {
                        getActivity().getFragmentManager().popBackStack();
                    } catch (Exception ignored) {

                    }
                }
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }


    private void handleClick(String key) {
        if (key == null) {
            Log.e(TAG, "handleClick: key is null! ");
            return;
        }

        if (key.isEmpty()) {
            handleDeleteClick();
        } else if (Character.isDigit(key.charAt(0))) {
            handleDigitClick(Integer.parseInt(key.substring(0, 1)));
        } else if (key.charAt(0) == '.') {
            handleSeparatorClick();
        }

        boolean generated = generateQrImage(receiveAddress, amountEdit.getText().toString(), selectedIso);
        if (!generated) throw new RuntimeException("failed to generate qr image for address");
    }

    private void handleDigitClick(Integer dig) {
        String currAmount = amountBuilder.toString();
        String iso = selectedIso;
        if (new BigDecimal(currAmount.concat(String.valueOf(dig))).doubleValue()
                <= BRExchange.getMaxAmount(getActivity(), iso).doubleValue()) {
            //do not insert 0 if the balance is 0 now
            if (currAmount.equalsIgnoreCase("0")) amountBuilder = new StringBuilder("");
            if ((currAmount.contains(".") && (currAmount.length() - currAmount.indexOf(".") > BRCurrency.getMaxDecimalPlaces(iso))))
                return;
            amountBuilder.append(dig);
            updateText();
        }
    }

    private void handleSeparatorClick() {
        String currAmount = amountBuilder.toString();
        if (currAmount.contains(".") || BRCurrency.getMaxDecimalPlaces(selectedIso) == 0)
            return;
        amountBuilder.append(".");
        updateText();
    }

    private void handleDeleteClick() {
        String currAmount = amountBuilder.toString();
        if (currAmount.length() > 0) {
            amountBuilder.deleteCharAt(currAmount.length() - 1);
            updateText();
        }

    }

    private void updateText() {
        if (getActivity() == null) return;
        String tmpAmount = amountBuilder.toString();
        amountEdit.setText(tmpAmount);
        isoText.setText(BRCurrency.getSymbolByIso(getActivity(), selectedIso));
        isoButton.setText(selectedIso);

    }

    private void showKeyboard(boolean b) {
        try {
            if (b) {
                signalLayout.addView(keyboard, keyboardPosition);
                signalLayout.addView(keyboardSeparator, keyboardPosition + 1);
            } else {
                signalLayout.removeView(keyboard);
                signalLayout.removeView(keyboardSeparator);
            }
        } catch (Exception ignored) {

        }
    }

    private boolean generateQrImage(String address, String strAmount, String iso) {
        String amountArg = "";
        if (strAmount != null && !strAmount.isEmpty()) {
            BigDecimal bigAmount = new BigDecimal((Utils.isNullOrEmpty(strAmount) || strAmount.equalsIgnoreCase(".")) ? "0" : strAmount);
            long amount = BRExchange.getSatoshisFromAmount(getActivity(), iso, bigAmount).longValue();
            String am = new BigDecimal(amount).divide(new BigDecimal(100000000), 8, BRConstants.ROUNDING_MODE).toPlainString();
            amountArg = "?amount=" + am;
        }
        return BRWalletManager.getInstance().generateQR(getActivity(), "bitcoin:" + address + amountArg, mQrImage);
    }


    private void removeCurrencySelector() {
        try {
            signalLayout.removeView(currencyRecycler);
        } catch (IllegalStateException ignored) {

        }
    }


}