package com.example.nutri_scan.utils;

import android.content.Context;
import android.os.Handler;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class Typewriter extends androidx.appcompat.widget.AppCompatTextView {

    private CharSequence mText;
    private int mIndex;
    private long mDelay = 20; //Default 150ms delay

    public Typewriter(@NonNull Context context) {
        super(context);
    }

    public Typewriter(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    private final Handler myhandler = new Handler();
    private final Runnable characterAdder = new Runnable() {
        @Override
        public void run() {
            setText(mText.subSequence(0,mIndex++));
            if(mIndex <= mText.length()){
                myhandler.postDelayed(characterAdder, mDelay);
            }
        }
    };

    public void animateText(CharSequence text) {
        mText = text;
        mIndex = 0;
        setText("");
        myhandler.removeCallbacks(characterAdder);
        myhandler.postDelayed(characterAdder, mDelay);
    }

    public void setCharacterDelay(long millis) {
        mDelay = millis;
    }
}
