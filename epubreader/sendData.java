package com.kyushuuniv.epubreader;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class sendData extends DialogFragment {
    private SpannableStringBuilder sb;
    protected AlertDialog.Builder builder;
    protected String log;
    protected String book_name;
    protected int page_number;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.log_layout, null);
        log = getArguments().getString("word");
        book_name = getArguments().getString("book_name");
        page_number = getArguments().getInt("book_page");


        //ここでデフォルトのテキストをセット可能


        EditText word_etext = (EditText) view.findViewById(R.id.word_etext);
        word_etext.setText(log, TextView.BufferType.EDITABLE);

        final EditText book_etext = (EditText) view.findViewById(R.id.book_etext);
        book_etext.setText(book_name, TextView.BufferType.EDITABLE);

        final EditText page_etext = (EditText) view.findViewById(R.id.page_etext);
        page_etext.setText(String.valueOf(page_number), TextView.BufferType.EDITABLE);

        builder.setTitle(getString(R.string.makeLog));
        builder.setView(view);
        //パネルサイズ変更画面でOKが押された場合の処理(画面サイズ変更を適用)
        builder.setPositiveButton("Send", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                //TODO: ここに送信処理を書く。
                errorMessage(getString(R.string.sent));
            }
        });
        //ログ用editTextが書き換えられた場合
        word_etext.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                //s:editTextの文字, start:変更されたテキストが何文字目からか(左端の文字を0とする)
                //count:修正後の文字列の長さ after:修正したことにより新たにできた文字列の長さ

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                //s:変更後のeditTextの文字列,　start:修正により新たにできた文字列の開始点(左端を0とする)
                //before:修正により消えた文字列の長さ,　after:修正したことにより新たにできた文字列の長さ
                log = s.toString();
            }

            @Override
            public void afterTextChanged(Editable s) {

            }

        });
        //書籍用editTextが書き換えられた場合
        book_etext.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                book_name = s.toString();
            }

            @Override
            public void afterTextChanged(Editable s) {

            }

        });
        //ページ用editTextが書き換えられた場合
        page_etext.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                try {
                    String str = s.toString();
                    page_number = Integer.valueOf(str);
                }catch(Exception e){
                    errorMessage(getString(R.string.unavailable_input));
                    page_etext.setText(String.valueOf(page_number));
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }

        });

        //パネルサイズ変更画面でCancelが押された場合の処理
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                //Cancelの際になにか処理をする場合はここに書く
            }
        });

        return builder.create();
    }

    public void errorMessage(String message) {
        MainActivity a = (MainActivity) getActivity();
        Context context = a.getBaseContext();
        Toast toast = Toast.makeText(context, message, Toast.LENGTH_SHORT);
        toast.show();
    }
}
