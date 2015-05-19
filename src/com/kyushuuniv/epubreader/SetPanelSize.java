package com.kyushuuniv.epubreader;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;


//オプションのChange panel sizeから開く
public class SetPanelSize extends DialogFragment {
    protected SeekBar seekbar;
    protected float value = (float) 0.2;
    protected int sBv = 50;
    protected Context context;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.set_panel_size, null);
        final SharedPreferences preferences = ((MainActivity) getActivity()).getPreferences(Context.MODE_PRIVATE);
        //sBv=seekBarValue=パネルサイズ変更用スライダーの値(取得失敗時は50とする)
        sBv = preferences.getInt("seekBarValue", 50);
        seekbar = (SeekBar) view.findViewById(R.id.progressBar);
        seekbar.setProgress(sBv);
        Button defaultButton = (Button) view.findViewById(R.id.defaultButton);

        defaultButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                ((MainActivity) getActivity()).changeViewsSize((float) 0.5);
                SharedPreferences.Editor editor = preferences.edit();
                editor.putInt("seekBarValue", 50);
                editor.commit();
                dismiss();
            }
        });

        builder.setTitle(getString(R.string.SetSizeTitle));
        builder.setView(view);

        //パネルサイズ変更画面でOKが押された場合の処理(画面サイズ変更を適用)
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                float actual = (float) seekbar.getProgress();
                value = actual / (float) seekbar.getMax();
                if (value <= 0.1)
                    value = (float) 0.1;
                if (value >= 0.9)
                    value = (float) 0.9;
                ((MainActivity) getActivity()).changeViewsSize(value);
                SharedPreferences.Editor editor = preferences.edit();
                sBv = seekbar.getProgress();
                editor.putInt("seekBarValue", sBv);
                editor.commit();
            }
        });

        //パネルサイズ変更画面でCancelが押された場合の処理
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
            }
        });
        return builder.create();
    }
}
