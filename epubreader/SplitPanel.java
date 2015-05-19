package com.kyushuuniv.epubreader;


import android.app.Fragment;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

public class SplitPanel extends Fragment {
    private RelativeLayout generalLayout;
    protected int index;
    protected RelativeLayout layout;
    protected Button closeButton, zoomInButton, zoomOutButton;
    protected EpubNavigator navigator;
    protected int screenWidth;
    protected int screenHeight;
    protected float weight = 0.5f; //GeneralLayoutのweight((全体を1とした時)画面のどれだけを占めるか)
    protected boolean created; //fragmentが作られたか否かをTRUEorFALSEで返す

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        navigator = ((MainActivity) getActivity()).navigator;
        View v = inflater.inflate(R.layout.activity_split_panel, container, false);
        created = false;
        return v;
    }

    @Override
    public void onActivityCreated(Bundle saved) {
        created = true;
        super.onActivityCreated(saved);
        generalLayout = (RelativeLayout) getView().findViewById(R.id.GeneralLayout);
        layout = (RelativeLayout) getView().findViewById(R.id.Content);
        closeButton = (Button) getView().findViewById(R.id.CloseButton);
        zoomInButton = (Button) getView().findViewById(R.id.zoomInButton);
        zoomOutButton = (Button) getView().findViewById(R.id.zoomOutButton);

        //スクリーンサイズを取得
        DisplayMetrics metrics = this.getResources().getDisplayMetrics();
        screenWidth = metrics.widthPixels;
        screenHeight = metrics.heightPixels;
        changeWeight(weight);

        //CLOSEボタンが押された場合ビューを閉じる処理
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                closeView();
            }
        });
    }

    //ビューを閉じる
    protected void closeView() {
        navigator.closeView(index);
    }

    //weightを適用する
    //weightは1:0か0.5:0.5のどちらかで、2つめのレイアウトが作られた(created=true)ならvalue=0.5を受け
    //画面を2分割する
    public void changeWeight(float value) {
        weight = value;
        if (created) {
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT, value);
            generalLayout.setLayoutParams(params);
        }
    }

    //現在のweight値を取得する
    public float getWeight() {
        return weight;
    }

    //
    public void setKey(int value) {
        index = value;
    }

    //エラーメッセージ
    public void errorMessage(String message) {
        ((MainActivity) getActivity()).errorMessage(message);
    }

    //
    public void saveState(Editor editor) {
        editor.putFloat("weight" + index, weight);
    }

    //
    public void loadState(SharedPreferences preferences) {
        changeWeight(preferences.getFloat("weight" + index, 0.5f));
    }
}
