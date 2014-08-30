package org.namelessrom.devicecontrol.preferences;

import android.content.Context;
import android.graphics.Typeface;
import android.preference.CheckBoxPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import org.namelessrom.devicecontrol.R;

import butterknife.ButterKnife;

public class CustomCheckBoxPreference extends CheckBoxPreference {

    private String value;

    public CustomCheckBoxPreference(Context context) {
        super(context);
        setLayoutResource(R.layout.preference);
    }

    public CustomCheckBoxPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLayoutResource(R.layout.preference);
    }

    public CustomCheckBoxPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setLayoutResource(R.layout.preference);
    }

    public void setValue(final String value) { this.value = value; }

    public String getValue() { return this.value; }

    @Override
    public boolean isPersistent() { return false; }

    @Override
    protected boolean shouldPersist() { return false; }
}
