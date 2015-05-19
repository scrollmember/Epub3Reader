package com.kyushuuniv.epubreader;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;

//MainActivityクラスのOnOptionsItemSelectedメソッドから起動されるメニューでopenが選択された際のファイル選択のためのクラス
public class FileChooser extends ActionBarActivity {
    static List<File> epubs;
    static List<String> names;
    ArrayAdapter<String> adapter;
    static File selected;
    boolean firstTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //file_chooser_layout.xmlのレイアウトを読み込む
        super.onCreate(savedInstanceState);
        setContentView(R.layout.file_chooser_layout);

        //ePubリストが未表示の場合、端末にあるepubを探してリストにする
        if ((epubs == null) || (epubs.size() == 0)) {
            epubs = epubList(Environment.getExternalStorageDirectory());
        }

        //file_chooser.xmlのリストビュー(id:listView)を取得
        ListView list = (ListView) findViewById(R.id.listView);
        //epubリスト内の各ファイルのファイル名を取得
        names = fileNames(epubs);
        //リストをビューに渡すアダプタ
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, names);

        //リストのアイテムがクリックによって選択された場合は処理を行う.その処理の内容の記述
        list.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> listView, View itemView, int position, long itemId) {
                //選択されたファイルのパスを取得
                selected = epubs.get(position);
                Intent resultIntent = new Intent();
                //selectedに格納したパスを絶対パスに変換して、Intentの要素のひとつであるmExtrasに入れる。(「bpath=ファイルのパス」という形)
                resultIntent.putExtra("bpath", selected.getAbsolutePath());
                //resultIntentをセット
                setResult(Activity.RESULT_OK, resultIntent);
                //アクティビティを停止してresultIntentを送る
                finish();
            }
        });
        //adapterよりepubリストをリストビューにセット
        list.setAdapter(adapter);
    }

    //epubリストを受けてその名前(文字列)のリストを作成
    private List<String> fileNames(List<File> files) {
        //resという名の文字列リストを作成
        List<String> res = new ArrayList<String>();
        //見つかったファイルの数(files.size())だけresに順に項目を追加。
        for (int i = 0; i < files.size(); i++) {
            boolean notProcessedYet = true;
            //リストでの表示名は見つかったepubファイルから拡張子をなくしたもの
            String filename = files.get(i).getName().replace(".epub", "");
            //既に同名ファイルが処理されてないか確認(処理済の場合flag=falseにして再処理を行わない)
            for (int j = 0; j < res.size(); j++) {
                if (res.get(j).equals(filename)) {
                    notProcessedYet = false;
                }
            }
            //処理済みのファイルでない場合は新たにリストに追加。
            if (notProcessedYet) {
                res.add(filename);
            }
        }
        return res;
    }

    //引数で渡された場所(端末全体)からepubファイルを探してリスト化
    private List<File> epubList(File dir) {
        List<File> res = new ArrayList<File>();
        if (dir.isDirectory()) {
            File[] f = dir.listFiles();
            if (f != null) {
                for (int i = 0; i < f.length; i++) {
                    if (f[i].isDirectory()) {
                        res.addAll(epubList(f[i]));
                    } else {
                        String lowerCasedName = f[i].getName().toLowerCase();
                        //ファイル名が「.epub」で終わるファイルをリストに追加
                        if (lowerCasedName.endsWith(".epub")) {
                            res.add(f[i]);
                        }
                    }
                }
            }
        }
        return res;
    }

    //epubのリストを更新
    private void refreshList() {
        epubs = epubList(Environment.getExternalStorageDirectory());
        names.clear();
        names.addAll(fileNames(epubs));
        this.adapter.notifyDataSetChanged();
    }

    //file_chooser.xmlよりメニューの項目を取得
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.file_chooser, menu);
        return true;
    }

    //オプションメニューの項目選択時の処理
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            //updateが選択された際の処理(リストの更新)
            case R.id.update:
                refreshList();
            default:
                return super.onOptionsItemSelected(item);
        }
    }

}
