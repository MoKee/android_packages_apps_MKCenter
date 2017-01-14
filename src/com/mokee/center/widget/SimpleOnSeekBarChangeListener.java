package com.mokee.center.widget;

import android.widget.SeekBar;

public abstract class SimpleOnSeekBarChangeListener implements SeekBar.OnSeekBarChangeListener {

    @Override
    public abstract void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser);

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
    }

}
