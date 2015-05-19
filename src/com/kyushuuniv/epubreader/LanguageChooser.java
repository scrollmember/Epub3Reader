package com.kyushuuniv.epubreader;

import java.util.ArrayList;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

//Enable Parallel Textsから呼ばれる
public class LanguageChooser extends DialogFragment {
    String[] languages;
    int book;
    boolean[] selected;
    int number_selected_elements;
    ArrayList<Integer> mSelectedItems = new ArrayList<Integer>();

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle b = this.getArguments();
        languages = b.getStringArray(getString(R.string.lang));
        book = b.getInt(getString(R.string.tome));
        selected = new boolean[languages.length];
        number_selected_elements = 0;

        //書籍を上下に並列して表示する際に使用する言語(上と下の2つ)を選ばせるダイアログ
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(getString(R.string.LanguageChooserTitle));
        builder.setMultiChoiceItems(languages, selected, new DialogInterface.OnMultiChoiceClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                if (isChecked) {
                    //新たにチェックが入った場合
                    if (number_selected_elements == 2) {
                        //チェックを入れる前の時点で既に2つ言語が選ばれていた場合
                        //そのチェックは無効になる(ただしチェックボックスにチェックは入ったまま)
                        selected[which] = false;
                    } else {
                        //チェックを入れる前の時点で選ばれていた言語が1つ以下だった場合
                        //利用する言語リストに追加
                        mSelectedItems.add(which);
                        number_selected_elements++;
                    }
                } else if (mSelectedItems.contains(which)) {
                    //チェックが外れた場合
                    mSelectedItems.remove(Integer.valueOf(which));
                    number_selected_elements--;
                }
            }
        });
        builder.setPositiveButton(getString(R.string.OK), new DialogInterface.OnClickListener() {
            //OKボタンが押された場合の処理
            @Override
            public void onClick(DialogInterface dialog, int id) {
                int first = -1;
                int second = -1;

                for (int i = 0; i < selected.length; i++) {
                    if (selected[i]) {
                        if (first == -1) {
                            //1つめの言語が選択されていた場合、画面1の言語として設定
                            first = i;
                        } else if (second == -1) {
                            //2つめの言語が選択されていた場合、画面2の言語として設定
                            second = i;
                        }
                    }
                }
                //選択された言語が2つより多かった場合の処理および選択された言語が1つのみだった場合
                if (number_selected_elements >= 2)
                    ((MainActivity) getActivity()).refreshLanguages(book, first, second);
                else if (number_selected_elements == 1)
                    ((MainActivity) getActivity()).refreshLanguages(book, first, -1);
            }
        });

        //Cancelが選択された場合の処理
        builder.setNegativeButton(getString(R.string.Cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {

            }
        });
        return builder.create();
    }
}
