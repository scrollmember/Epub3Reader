package com.kyushuuniv.epubreader;


import java.io.File;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ActivityInfo;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

public class AudioView extends SplitPanel {
    //ジャグ配列「audio」を宣言。
    //1つ目の要素がどのオーディオファイルかを示し、2つ目の要素がそのファイルのどの地点かを示す。
    String[][] audio;
    ListView list;
    private MediaPlayer player;
    private Button rew;
    private Button playpause;
    private String actuallyPlaying = null;
    private SeekBar progressBar;
    private Runnable update;
    private Handler progressHandler;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View v = inflater.inflate(R.layout.activity_audio_view, container, false);
        return v;
    }

    @Override
    public void onActivityCreated(Bundle saved) {
        super.onActivityCreated(saved);
        list = (ListView) getView().findViewById(R.id.audioListView);
        rew = (Button) getView().findViewById(R.id.RewindButton);
        playpause = (Button) getView().findViewById(R.id.PlayPauseButton);
        progressBar = (SeekBar) getView().findViewById(R.id.progressBar);
        progressHandler = new Handler();

        list.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> listView, View itemView, int position, long itemId) {
                start(position);
            }
        });

        // 再生/停止ボタンの動作
        playpause.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (player.isPlaying()) {
                    //ボタンを押された時点でプレイヤーが再生中だった場合
                    player.pause();
                    playpause.setText(getString(R.string.play));
                } else {
                    //プレイヤーが停止中だった場合
                    player.start();
                    playpause.setText(getString(R.string.pause));
                    update.run();
                }
            }
        });

        //巻き戻しボタン(最初に巻き戻す)
        rew.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (player != null) {
                    player.seekTo(0);
                    player.start();
                }
            }
        });

        progressBar.setProgress(0);
        //プログレスバー
        progressBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            //プログレスバーで任意の一点をタップされた場合、そこに移動する
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && player != null)
                    player.seekTo(progress);
            }

            //
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            //
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        //オーディオを再生中は規定の時間ごとにプログレスバーを更新する
        update = new Runnable() {
            @Override
            public void run() {
                if (player != null) {
                    progressBar.setMax(player.getDuration());
                    progressBar.setProgress(player.getCurrentPosition());
                }
                //規定の時間(ms)を指定。基本は0.5秒(=500ms)
                progressHandler.postDelayed(this, 500);
            }
        };
    }

    //ボタンの状態の更新
    private void updateButtons() {
        if (player != null) {
            playpause.setEnabled(true);
            rew.setEnabled(true);

            if (player.isPlaying())
                playpause.setText(getString(R.string.pause));
            else
                playpause.setText(getString(R.string.play));
        } else {
            playpause.setEnabled(false);
            rew.setEnabled(false);
        }
    }

    //オーディオファイルのリストを取得・表示する
    public void setAudioList(String[][] audio) {
        this.audio = audio;
        if (created) {
            if (player != null) {
                player.stop();
                player.release();
                player = null;
            }
            String[] songs = new String[audio.length];
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();

            //各オーディオファイルのタイトルと長さを取得
            for (int i = 0; i < audio.length; i++) {
                retriever.setDataSource(audio[i][0].replace("file:///", ""));
                String title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
                if (title == null)
                    title = (new File(audio[i][0])).getName();
                String d = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                if (d != null)
                    d = (String) DateFormat.format("mm:ss", Integer.parseInt(d));
                else
                    d = "";
                songs[i] = (i + 1) + "\t-\t" + title + "\t" + d;
            }

            ArrayAdapter<String> songList = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1, songs);
            list.setAdapter(songList);

            //ファイル数に応じてビューの高さを変化
            if (getActivity().getResources().getConfiguration().orientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
                int height = getView().findViewById(R.id.PlayerLayout).getHeight() + 2;
                //ファイルが見つからなかった場合はビューを隠す
                if (songs.length == 0)
                    height = 0;
                View listItem;
                for (int i = 0; i < songs.length; i++) {
                    listItem = songList.getView(i, null, list);
                    listItem.measure(MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED), MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
                    height += listItem.getMeasuredHeight();
                }

                float weight = (float) height / ((MainActivity) getActivity()).getHeight();
                if (weight > 0.5f)
                    weight = 0.5f;
                navigator.changeViewsSize(1 - weight);

                //ファイルが1つだけ見つかった場合はビューではなくプレイヤーを表示してそのファイルを再生する
                if (songs.length == 1) {
                    start(0);
                    player.pause();
                }
            } else {
                //横向きなら0.5:0.5に
                navigator.changeViewsSize(0.5f);
            }
            updateButtons();
        }
    }

    public void start(int i) {
        if (i >= 0 && i < audio.length) {
            int j = 0;
            boolean err = true;
            if (player == null)
                player = new MediaPlayer();

            //指定のファイルを再生する
            while (j < audio[i].length && err)
                try {
                    player.reset();
                    player.setDataSource(audio[i][j]);
                    player.prepare();
                    player.start();
                    progressBar.setMax(player.getDuration());
                    rew.setEnabled(true);
                    playpause.setEnabled(true);
                    playpause.setText(getString(R.string.pause));
                    actuallyPlaying = audio[i][j];
                    err = false;
                } catch (Exception e) {
                    actuallyPlaying = null;
                }
            if (err) {
                playpause.setEnabled(false);
                ((MainActivity) getActivity()).errorMessage(getString(R.string.error_openaudiofile));
            }
        }
    }

    // オーディオをストップする
    public void stop() {
        if (player != null) {
            player.stop();
            player.release();
            player = null;
        }
        progressHandler.removeCallbacks(update);
    }

    //オーディオを止めてビューを閉じる
    @Override
    protected void closeView() {
        stop();
        super.closeView();
    }

    //状態の保存
    @Override
    public void saveState(Editor editor) {
        progressHandler.removeCallbacks(update);
        super.saveState(editor);

        if (player != null) {
            editor.putBoolean(index + "isPlaying", player.isPlaying());
            editor.putInt(index + "current", player.getCurrentPosition());
            editor.putString(index + "actualSong", actuallyPlaying);
            stop();
        }
    }

    //状態の読み込み
    @Override
    public void loadState(SharedPreferences preferences) {
        super.loadState(preferences);
        actuallyPlaying = preferences.getString(index + "actualSong", null);
        setAudioList(audio);

        if (actuallyPlaying != null) {
            player = new MediaPlayer();
            player.reset();
            try {
                player.setDataSource(actuallyPlaying);
                player.prepare();
                if (preferences.getBoolean(index + "isPlaying", false))
                    player.start();
                player.seekTo(preferences.getInt(index + "current", 0));
            } catch (Exception e) {
                //エラーメッセージ
            }
        }
    }
}
