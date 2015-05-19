package com.kyushuuniv.epubreader;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.support.v4.view.MotionEventCompat;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;


public class BookView extends SplitPanel {
    public ViewStateEnum state = ViewStateEnum.books;
    protected String viewedPage;
    protected WebView view;
    protected float swipeOriginX, swipeOriginY;
    protected Button zoomInButton, zoomOutButton;
    protected int RoM = 0; //Rate of Magnification(拡大率)

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View v = inflater.inflate(R.layout.activity_book_view, container, false);
        return v;
    }

    @Override
    public void onActivityCreated(Bundle saved) {
        super.onActivityCreated(saved);
        view = (WebView) getView().findViewById(R.id.Viewport);
        zoomInButton = (Button) getView().findViewById(R.id.zoomInButton);
        zoomOutButton = (Button) getView().findViewById(R.id.zoomOutButton);

        //Viewport(ウェブビュー)におけるJavaScriptの有効化
        view.getSettings().setJavaScriptEnabled(true);

        //書籍閲覧ページがスワイプされた際の処理(処理の詳細はswipePageメソッドに記述)
        view.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                if (state == ViewStateEnum.books)
                    swipePage(v, event, 0);
                WebView view = (WebView) v;
                return view.onTouchEvent(event);
            }
        });

        //書籍閲覧ページ上で長押しされた際の処理
        view.setOnLongClickListener(new OnLongClickListener() {

            @Override
            public boolean onLongClick(View v) {
                /*
                Message msg = new Message();
                msg.setTarget(new android.os.Handler() {
                    @Override
                    public void handleMessage(Message msg) {
                        super.handleMessage(msg);
                        String url = msg.getData().getString(getString(R.string.url));
                        if (url != null)
                            navigator.setNote(url, index);
                    }
                });
                view.requestFocusNodeHref(msg);
                //falseだと続けてデフォルトの長押し処理をする。trueだとしない
                */

                return true;
            }
        });

        //
        view.setWebViewClient(new WebViewClient() {
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                try {
                    navigator.setBookPage(url, index);
                } catch (Exception e) {
                    errorMessage(getString(R.string.error_LoadPage));
                }
                return true;
            }
        });
        loadPage(viewedPage);

        //ズームインボタン
        zoomInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(RoM < 5) {
                    view.zoomIn();
                    RoM++;
                }
            }
        });

        //ズームアウトボタン
        zoomOutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(RoM > -5){
                    view.zoomOut();
                    RoM--;
                }
            }
        });
    }

    //書籍の表示された画面をスワイプした時の処理(スワイプの方向別に処理を記述)
    protected void swipePage(View v, MotionEvent event, int book) {
        int action = MotionEventCompat.getActionMasked(event);
        switch (action) {
            //指が置かれた場所のX,Y座標を取得
            case (MotionEvent.ACTION_DOWN):
                swipeOriginX = event.getX();
                swipeOriginY = event.getY();
                break;

            //指が上げられた場所のX,Y座標を取得してその値を元に画面に変化を与える
            case (MotionEvent.ACTION_UP):
                int quarterWidth = (int) (screenWidth * 0.25);
                float diffX = swipeOriginX - event.getX();
                float diffY = swipeOriginY - event.getY();
                float absDiffX = Math.abs(diffX);
                float absDiffY = Math.abs(diffY);

                //横方向にスワイプされたと判断される場合分岐
                if ((diffX > quarterWidth) && (absDiffX > absDiffY)) {
                    //拡大縮小率をリセット
                    RoM = 0;
                    //左スワイプは次の章に移動。次がない場合はメッセージ
                    try {
                        navigator.goToNextChapter(index);
                    } catch (Exception e) {
                        errorMessage(getString(R.string.error_cannotTurnPage));
                    }
                } else if ((diffX < -quarterWidth) && (absDiffX > absDiffY)) {
                    //拡大縮小率をリセット
                    RoM = 0;
                    //右スワイプは前の章に移動。前がない場合はメッセージ
                    try {
                        navigator.goToPrevChapter(index);
                    } catch (Exception e) {
                        errorMessage(getString(R.string.error_cannotTurnPage));
                    }
                }
                break;
        }
    }

    //状態保存
    @Override
    public void saveState(Editor editor) {
        super.saveState(editor);
        editor.putString("state" + index, state.name());
        editor.putString("page" + index, viewedPage);
    }

    //状態読み込み
    @Override
    public void loadState(SharedPreferences preferences) {
        super.loadState(preferences);
        loadPage(preferences.getString("page" + index, ""));
        state = ViewStateEnum.valueOf(preferences.getString("state" + index, ViewStateEnum.books.name()));
    }

    //引数として受けたパスのページを表示する
    public void loadPage(String path) {
        viewedPage = path;
        if (created)
            view.loadUrl(path);
    }
}
