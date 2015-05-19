package com.kyushuuniv.epubreader;

import android.app.Activity;
import android.app.DialogFragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.LinearLayout;
import android.widget.Toast;

public class MainActivity extends ActionBarActivity {
    protected EpubNavigator navigator;
    protected int bookSelector;
    protected int panelCount;
    protected String[] settings;

    //このアプリにおいて最初の最初に処理されるメソッド
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        navigator = new EpubNavigator(2, this);
        panelCount = 0;
        settings = new String[8];

        SharedPreferences preferences = getPreferences(MODE_PRIVATE);
        loadState(preferences);
        //前回の保存状態を見て、何らかのパネルが開かれていた場合はそのパネルを展開する(前回終了した時の状態から始める)
        navigator.loadViews(preferences);
        //パネルの数が0の場合(前回パネルを全て閉じて終了した場合)は最初に自動的に書籍ファイル選択画面が開く
        if (panelCount == 0) {
            bookSelector = 0;
            Intent goToChooser = new Intent(this, FileChooser.class);
            //次の1行をコメントアウトすると自動的にファイル選択画面が開かず、起動時は必ずメイン画面になる
            //startActivityForResult(goToChooser, 0);
        }
    }

    //画面が前面に移動する際の処理
    protected void onResume() {
        super.onResume();
        if (panelCount == 0) {
            SharedPreferences preferences = getPreferences(MODE_PRIVATE);
            navigator.loadViews(preferences);
        }
    }

    //画面が背面に移動する際(ほかの画面を表示する際やアプリをいったん閉じる際)の処理。画面状態を保存
    @Override
    protected void onPause() {
        super.onPause();
        SharedPreferences preferences = getPreferences(MODE_PRIVATE);
        Editor editor = preferences.edit();
        saveState(editor);
        editor.commit();
    }

    //インテント実行結果としてこのアクティビティが展開された際の処理
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (panelCount == 0) {
            SharedPreferences preferences = getPreferences(MODE_PRIVATE);
            navigator.loadViews(preferences);
        }

        if (resultCode == Activity.RESULT_OK) {
            //mExtraのbpath="URL"から書籍のURLをpathに取得してopenBookより展開
            String path = data.getStringExtra(getString(R.string.bpath));
            navigator.openBook(path, bookSelector);
        }
    }

    //menu_main.xmlからメニュー項目を取得
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    //状態に合わせてメニューの項目を変化
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (navigator.isParallelTextOn() == false && navigator.exactlyOneBookOpen() == false) {
            menu.findItem(R.id.meta1).setVisible(true);
            menu.findItem(R.id.meta2).setVisible(true);
            menu.findItem(R.id.toc1).setVisible(true);
            menu.findItem(R.id.toc2).setVisible(true);
            menu.findItem(R.id.FirstFront).setVisible(true);
            menu.findItem(R.id.SecondFront).setVisible(true);
        }

        if (navigator.exactlyOneBookOpen() == false) {
            menu.findItem(R.id.Synchronize).setVisible(true);
            menu.findItem(R.id.Align).setVisible(true);
            //menu.findItem(R.id.SyncScroll).setVisible(true);
            menu.findItem(R.id.StyleBook1).setVisible(true);
            menu.findItem(R.id.StyleBook2).setVisible(true);
            menu.findItem(R.id.firstAudio).setVisible(true);
            menu.findItem(R.id.secondAudio).setVisible(true);
        }

        if (navigator.exactlyOneBookOpen() == true || navigator.isParallelTextOn() == true) {
            menu.findItem(R.id.meta1).setVisible(false);
            menu.findItem(R.id.meta2).setVisible(false);
            menu.findItem(R.id.toc1).setVisible(false);
            menu.findItem(R.id.toc2).setVisible(false);
            menu.findItem(R.id.FirstFront).setVisible(false);
            menu.findItem(R.id.SecondFront).setVisible(false);
        }

        if (navigator.exactlyOneBookOpen() == true) {
            menu.findItem(R.id.Synchronize).setVisible(false);
            menu.findItem(R.id.Align).setVisible(false);
            menu.findItem(R.id.SyncScroll).setVisible(false);
            menu.findItem(R.id.StyleBook1).setVisible(false);
            menu.findItem(R.id.StyleBook2).setVisible(false);
            menu.findItem(R.id.firstAudio).setVisible(false);
            menu.findItem(R.id.secondAudio).setVisible(false);
        }

        //次の機能は現在は必要ないので表示しない
        menu.findItem(R.id.Metadata).setVisible(false);
        menu.findItem(R.id.audio).setVisible(false);
        menu.findItem(R.id.Align).setVisible(false);


        //ビューがひとつしか展開されていない場合changeSizeが表示されない
        if (panelCount == 1)
            menu.findItem(R.id.changeSize).setVisible(false);
        else
            menu.findItem(R.id.changeSize).setVisible(true);

        return true;
    }

    //メニューの動作記述
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            //メニューからMake Logが選択されたとき
            case R.id.MakeLog:
                try {
                    DialogFragment newFragment = new sendData();

                    Bundle bundle = new Bundle();
                    //TODO: 登録するワードの自動入力に対応
                    bundle.putString("word","");

                    bundle.putString("book_name",navigator.getBookName(bookSelector));
                    bundle.putInt("book_page",navigator.getPageNum(bookSelector));
                    newFragment.setArguments(bundle);

                    newFragment.show(getFragmentManager(), "");
                } catch (Exception e) {
                    errorMessage("error");
                }
                return true;

            //メニューからopenが選択されたとき
            case R.id.FirstEPUB:
                //Open → Open Book 1
                bookSelector = 0;
                //インテントは他のアクティビティ(ここではFileChooser.class)を起動する。(画面遷移)
                Intent goToChooser1 = new Intent(this, FileChooser.class);
                //putExtraはIntentでアクティビティを起動する際値を渡せる。第1引数がキー、第2引数が値
                goToChooser1.putExtra(getString(R.string.second), getString(R.string.time));
                //startActivityForResult(インテント,R値)でリクエストコードをR値としてインテントを実行
                startActivityForResult(goToChooser1, 0);
                return true;

            case R.id.SecondEPUB:
                //Open → Open Book 2
                bookSelector = 1;
                Intent goToChooser2 = new Intent(this, FileChooser.class);
                goToChooser2.putExtra(getString(R.string.second), getString(R.string.time));
                startActivityForResult(goToChooser2, 0);
                return true;

            case R.id.Front:
                //Enable Parallel texts (ひとつだけ書籍を開いているか、2言語同時描画モードの場合)
                try {
                    if (navigator.exactlyOneBookOpen() == true || navigator.isParallelTextOn() == true)
                        chooseLanguage(0);
                } catch (Exception e) {

                }
                return true;

            case R.id.FirstFront:
                //Enable Parallel texts → From Book 1
                chooseLanguage(0);
                return true;

            case R.id.SecondFront:
                //Enable Parallel texts → From Book 2(書籍を二つ開いている場合のみ)
                if (navigator.exactlyOneBookOpen() == false)
                    chooseLanguage(1);
                else
                    errorMessage(getString(R.string.error_onlyOneBookOpen));
                return true;

            case R.id.PconS:
                //Reset Sync Views → Sync Book 1 with Book2(第1パネルを第2パネルにあわせる)
                try {
                    boolean yes = navigator.synchronizeView(1, 0);
                    if (!yes) {
                        errorMessage(getString(R.string.error_onlyOneBookOpen));
                    }
                } catch (Exception e) {
                    errorMessage(getString(R.string.error_cannotSynchronize));
                }
                return true;

            case R.id.SconP:
                //Reset Sync Views → Sync Book 2 with Book1(第2パネルを第1パネルにあわせる)
                try {
                    boolean ok = navigator.synchronizeView(0, 1);
                    if (!ok) {
                        errorMessage(getString(R.string.error_onlyOneBookOpen));
                    }
                } catch (Exception e) {
                    errorMessage(getString(R.string.error_cannotSynchronize));
                }
                return true;

            case R.id.Synchronize:
                //Toggle Sync View(シンクロのオン/オフ)

                boolean sync = navigator.flipSynchronizedReadingActive();
                if (!sync) {
                    errorMessage(getString(R.string.error_onlyOneBookOpen));
                }else {
                    try {
                        if (navigator.syncMode()) {
                            errorMessage(getString(R.string.syncmode));
                            //シンクロを有効にするとともにパネル2をパネル1に合わせる
                            boolean ok = navigator.synchronizeView(0, 1);
                        }else
                            errorMessage(getString(R.string.asyncmode));
                    } catch (Exception e) {
                        errorMessage(getString(R.string.error_cannotSynchronize));
                    }
                }
                return true;

            case R.id.Metadata:
                //Show Metadata (書籍がひとつだけ開いているか2言語同時描画モードの場合)
                try {
                    if (navigator.exactlyOneBookOpen() == true || navigator.isParallelTextOn() == true) {
                        navigator.displayMetadata(0);
                    } else {

                    }
                } catch (Exception e) {
                    errorMessage(getString(R.string.cannotShowMetadata));
                }
                return true;

            case R.id.meta1:
                //Show Metadata → Book 1
                if (!navigator.displayMetadata(0))
                    errorMessage(getString(R.string.error_metadataNotFound));
                return true;

            case R.id.meta2:
                //Show Metadata → Book 2
                if (!navigator.displayMetadata(1))
                    errorMessage(getString(R.string.error_metadataNotFound));
                return true;

            case R.id.tableOfContents:
                //Table of Contents (書籍が一つだけ開かれていた場合または2言語同時描画モードの場合)
                if (navigator.exactlyOneBookOpen() == true || navigator.isParallelTextOn() == true)
                    navigator.displayTOC(0);
                return true;

            case R.id.toc1:
                //Table of Contents → Book1
                if (!navigator.displayTOC(0))
                    errorMessage(getString(R.string.error_tocNotFound));
                return true;

            case R.id.toc2:
                //Table of Contents → Book2
                if (navigator.displayTOC(1))
                    errorMessage(getString(R.string.error_tocNotFound));
                return true;

            case R.id.changeSize:
                //Change Panel Size
                try {
                    DialogFragment newFragment = new SetPanelSize();
                    newFragment.show(getFragmentManager(), "");
                } catch (Exception e) {
                    errorMessage(getString(R.string.error_cannotChangeSizes));
                }
                return true;

            case R.id.Style:
                //Change Style(書籍がひとつだけ開いてる場合)
                try {
                    if (navigator.exactlyOneBookOpen() == true) {
                        DialogFragment newFragment = new ChangeCSSMenu();
                        newFragment.show(getFragmentManager(), "");
                        bookSelector = 0;
                    }
                } catch (Exception e) {
                    errorMessage(getString(R.string.error_CannotChangeStyle));
                }
                return true;

            case R.id.StyleBook1:
                //Change Style → Book 1
                try {
                    DialogFragment newFragment = new ChangeCSSMenu();
                    newFragment.show(getFragmentManager(), "");
                    bookSelector = 0;
                } catch (Exception e) {
                    errorMessage(getString(R.string.error_CannotChangeStyle));
                }
                return true;

            case R.id.StyleBook2:
                //Change Style → Book 2
                try {
                    DialogFragment newFragment = new ChangeCSSMenu();
                    newFragment.show(getFragmentManager(), "");
                    bookSelector = 1;
                } catch (Exception e) {
                    errorMessage(getString(R.string.error_CannotChangeStyle));
                }
                return true;

            case R.id.audio:
                //Extract Audio (書籍がひとつだけ開いていた場合)
                if (navigator.exactlyOneBookOpen() == true)
                    if (!navigator.extractAudio(0))
                        errorMessage(getString(R.string.no_audio));
                return true;

            case R.id.firstAudio:
                //Extract Audio → Book 1
                try {
                    if (!navigator.extractAudio(0))
                        errorMessage(getString(R.string.no_audio));
                } catch (Exception e) {

                }
                return true;
            case R.id.secondAudio:
                //Extract Audio → Book 2
                try {
                    if (!navigator.extractAudio(1))
                        errorMessage(getString(R.string.no_audio));
                } catch (Exception e) {

                }
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    //パネルを作成
    public void addPanel(SplitPanel p) {
        FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
        fragmentTransaction.add(R.id.MainLayout, p, p.getTag());
        fragmentTransaction.commit();
        panelCount++;
    }

    public void attachPanel(SplitPanel p) {
        FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
        fragmentTransaction.attach(p);
        fragmentTransaction.commit();
        panelCount++;
    }

    public void detachPanel(SplitPanel p) {
        FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
        fragmentTransaction.detach(p);
        fragmentTransaction.commit();
        panelCount--;
    }

    //パネルを裏で開いたまま非表示にする
    public void removePanelWithoutClosing(SplitPanel p) {
        FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
        fragmentTransaction.remove(p);
        fragmentTransaction.commit();
        panelCount--;
    }

    //パネルを閉じて非表示にする
    public void removePanel(SplitPanel p) {
        FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
        fragmentTransaction.remove(p);
        fragmentTransaction.commit();
        panelCount--;
        //パネルを閉じた時点でパネルがなくなった場合、アプリを閉じる。次の2行をコメントアウトで無効
        /*
        if(panelCount <= 0)
            finish();
        */
    }

    //言語選択
    public void chooseLanguage(int book) {
        String[] languages;
        languages = navigator.getLanguagesBook(book);
        if (languages.length == 2)
            refreshLanguages(book, 0, 1);
        else if (languages.length > 0) {
            Bundle bundle = new Bundle();
            bundle.putInt(getString(R.string.tome), book);
            bundle.putStringArray(getString(R.string.lang), languages);

            LanguageChooser langChooser = new LanguageChooser();
            langChooser.setArguments(bundle);
            langChooser.show(getFragmentManager(), "");
        } else {
            errorMessage(getString(R.string.error_noOtherLanguages));
        }
    }

    public void refreshLanguages(int book, int first, int second) {
        navigator.parallelText(book, first, second);
    }

    public void setCSS() {
        navigator.changeCSS(bookSelector, settings);
    }

    public void setBackColor(String my_backColor) {
        settings[1] = my_backColor;
    }

    public void setColor(String my_color) {
        settings[0] = my_color;
    }

    public void setFontType(String my_fontFamily) {
        settings[2] = my_fontFamily;
    }

    public void setFontSize(String my_fontSize) {
        settings[3] = my_fontSize;
    }

    public void setLineHeight(String my_lineHeight) {
        if (my_lineHeight != null)
            settings[4] = my_lineHeight;
    }

    public void setAlign(String my_align) {
        settings[5] = my_align;
    }

    public void setMarginLeft(String mLeft) {
        settings[6] = mLeft;
    }

    public void setMarginRight(String mRight) {
        settings[7] = mRight;
    }

    protected void changeViewsSize(float weight) {
        navigator.changeViewsSize(weight);
    }

    public int getHeight() {
        LinearLayout main = (LinearLayout) findViewById(R.id.MainLayout);
        return main.getMeasuredHeight();
    }

    protected void saveState(Editor editor) {
        navigator.saveState(editor);
    }

    protected void loadState(SharedPreferences preferences) {
        if (!navigator.loadState(preferences))
            errorMessage(getString(R.string.error_cannotLoadState));
    }

    //引数で受けた文字列をエラーメッセージとして表示する
    public void errorMessage(String message) {
        Context context = getApplicationContext();
        Toast toast = Toast.makeText(context, message, Toast.LENGTH_SHORT);
        toast.show();
    }


}
