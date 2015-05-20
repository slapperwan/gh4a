package com.gh4a;

import java.util.Locale;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;

import com.larswerkman.holocolorpicker.ColorPicker;
import com.larswerkman.holocolorpicker.SaturationBar;
import com.larswerkman.holocolorpicker.ValueBar;

public class ColorPickerDialog extends AlertDialog {
    public interface OnColorChangedListener {
        /**
         * This is called after the user pressed the "OK" button of the dialog.
         *
         * @param color
         *         A string representation of the ARGB value of the selected color.
         */
        void colorChanged(String color);
    }

    private OnColorChangedListener mColorChangedListener;
    private ColorPicker mColorPicker;

    public ColorPickerDialog(Context context, String color, OnColorChangedListener listener) {
        super(context);
        mColorChangedListener = listener;

        View view = LayoutInflater.from(context).inflate(R.layout.color_picker_dialog, null);

        mColorPicker = (ColorPicker) view.findViewById(R.id.color_picker);
        mColorPicker.addSaturationBar((SaturationBar) view.findViewById(R.id.saturation));
        mColorPicker.addValueBar((ValueBar) view.findViewById(R.id.value));
        setColor(color);

        setView(view);

        setButton(BUTTON_POSITIVE, context.getString(R.string.ok), new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (mColorChangedListener != null) {
                    int colorValue = mColorPicker.getColor();
                    String color = String.format(Locale.US, "%02x%02x%02x",
                            Color.red(colorValue), Color.green(colorValue), Color.blue(colorValue));
                    mColorChangedListener.colorChanged(color);
                }
            }
        });

        setButton(BUTTON_NEGATIVE, context.getString(R.string.cancel), (OnClickListener) null);
    }

    /**
     * Set the color the color picker should highlight as selected color.
     *
     * @param color
     *         The RGB value of a color
     */
    public void setColor(String color) {
        int colorValue = Color.parseColor("#" + color);
        mColorPicker.setColor(colorValue);
        mColorPicker.setOldCenterColor(colorValue);
    }
}
