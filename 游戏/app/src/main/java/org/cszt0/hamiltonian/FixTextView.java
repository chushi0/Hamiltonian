package org.cszt0.hamiltonian;

import android.content.Context;
import android.util.AttributeSet;

public class FixTextView extends androidx.appcompat.widget.AppCompatButton {
    public FixTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        //noinspection SuspiciousNameCombination
        super.onMeasure(widthMeasureSpec, widthMeasureSpec);
    }
}
