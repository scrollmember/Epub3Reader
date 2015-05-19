package com.kyushuuniv.epubreader;

import java.io.IOException;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;


public class EpubNavigator {

    private int nBooks;
    private com.kyushuuniv.epubreader.EpubManipulator[] books;
    private SplitPanel[] views;
    private boolean[] extractAudio;
    private boolean synchronizedReadingActive;
    private boolean parallelText = false;
    private MainActivity activity;
    private static Context context;

    //コンストラクタ。インスタンス化時に一度だけ実行。値の初期化など
    public EpubNavigator(int numberOfBooks, MainActivity a) {
        nBooks = numberOfBooks;
        books = new com.kyushuuniv.epubreader.EpubManipulator[nBooks];
        views = new SplitPanel[nBooks];
        extractAudio = new boolean[nBooks];
        activity = a;
        context = a.getBaseContext();
    }

    //指定の書籍を指定のパネルに開く。引数は書籍のパスと開くパネル(index=0:上、index=1:下)。成功でtrue、失敗でfalseを返す
    public boolean openBook(String path, int index) {
        try {
            if (books[index] != null)
                books[index].destroy();

            books[index] = new com.kyushuuniv.epubreader.EpubManipulator(path, index + "", context);
            changePanel(new BookView(), index);
            setBookPage(books[index].getSpineElementPath(0), index);
            return true;

        } catch (Exception e) {
            return false;
        }
    }

    //指定のパス(書籍)の指定のページのオーディオを抽出し、ビューに描画(ビューに描画は他メソッドによる)
    public void setBookPage(String page, int index) {
        if (books[index] != null) {
            books[index].goToPage(page);
            if (extractAudio[index]) {
                if (views[(index + 1) % nBooks] instanceof AudioView)
                    ((AudioView) views[(index + 1) % nBooks]).setAudioList(books[index].getAudio());
                else
                    extractAudio(index);
            }
        }
        loadPageIntoView(page, index);
    }

    //現在使用していないほうのパネル(使用しているほうのパネル番号を引数で受ける)に指定のページをセットする
    public void setNote(String page, int index) {
        loadPageIntoView(page, (index + 1) % nBooks);
    }

    //指定のパス(書籍)の指定のページをビューに描画
    public void loadPageIntoView(String pathOfPage, int index) {
        ViewStateEnum state = ViewStateEnum.notes;

        if (books[index] != null)
            if ((pathOfPage.equals(books[index].getCurrentPageURL()))
                    || (books[index].getPageIndex(pathOfPage) >= 0))
                state = ViewStateEnum.books;

        if (books[index] == null)
            state = ViewStateEnum.notes;

        if (views[index] == null || !(views[index] instanceof BookView))
            changePanel(new BookView(), index);

        ((BookView) views[index]).state = state;
        ((BookView) views[index]).loadPage(pathOfPage);
    }

    // 次の章へ移る(引数でパネル指定)
    public void goToNextChapter(int book) throws Exception {
        setBookPage(books[book].goToNextChapter(), book);

        //シンクロ中ならもう一方のパネルも変化させる
        if (synchronizedReadingActive)
            for (int i = 1; i < nBooks; i++)
                if (books[(book + i) % nBooks] != null)
                    setBookPage(books[(book + i) % nBooks].goToNextChapter(), (book + i) % nBooks);
    }

    // 前の章へ戻る(引数でパネル指定)
    public void goToPrevChapter(int book) throws Exception {
        setBookPage(books[book].goToPreviousChapter(), book);

        //シンクロ中ならもう一方のパネルも変化させる
        if (synchronizedReadingActive)
            for (int i = 1; i < nBooks; i++)
                if (books[(book + i) % nBooks] != null)
                    setBookPage(
                            books[(book + i) % nBooks].goToPreviousChapter(), (book + i) % nBooks);
    }

    //ビューを閉じる
    public void closeView(int index) {
        //閉じるのがAudioViewの場合はオーディオを止める
        if (views[index] instanceof AudioView) {
            ((AudioView) views[index]).stop();
            extractAudio[index > 0 ? index - 1 : nBooks - 1] = false;
        }
        //閉じようとしているパネルにオーディオが含まれ、もう一方のパネルがAudioViewの場合
        if (extractAudio[index] && views[(index + 1) % nBooks] instanceof AudioView) {
            closeView((index + 1) % nBooks);
            extractAudio[index] = false;
        }

        // 閉じようとするパネルが空でない書籍ファイルを扱っており、なおかつ閉じようとするビューがBookViewでないかビューが扱っているのが書籍でない場合
        if (books[index] != null && (!(views[index] instanceof BookView) || (((BookView) views[index]).state != ViewStateEnum.books))) {
            BookView v = new BookView();
            changePanel(v, index);
            v.loadPage(books[index].getCurrentPageURL());
        } else {
            if (books[index] != null)
                try {
                    books[index].destroy();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            activity.removePanel(views[index]);

            while (index < nBooks - 1) {
                books[index] = books[index + 1]; // shift left all books
                if (books[index] != null) // updating their folder
                    books[index].changeDirName(index + ""); // according to the

                views[index] = views[index + 1]; // shift left every panel
                if (views[index] != null) {
                    views[index].setKey(index); // update the panel key
                    if (views[index] instanceof BookView
                            && ((BookView) views[index]).state == ViewStateEnum.books)
                        ((BookView) views[index]).loadPage(books[index]
                                .getCurrentPageURL()); // reload the book page
                }
                index++;
            }
            books[nBooks - 1] = null; // last book and last view
            views[nBooks - 1] = null; // don't exist anymore
        }
    }

    //指定のインデックスの書籍の言語を取得する
    public String[] getLanguagesBook(int index) {
        return books[index].getLanguages();
    }

    //二つの言語による同時表示
    public boolean parallelText(int book, int firstLanguage, int secondLanguage) {
        boolean ok = true;

        if (firstLanguage != -1) {
            try {
                if (secondLanguage != -1) {
                    //パネル1の言語とパネル2の言語が決まっている場合
                    openBook(books[book].getFileName(), (book + 1) % 2);
                    books[(book + 1) % 2].goToPage(books[book].getCurrentSpineElementIndex());
                    books[(book + 1) % 2].setLanguage(secondLanguage);
                    setBookPage(books[(book + 1) % 2].getCurrentPageURL(), (book + 1) % 2);
                }
                books[book].setLanguage(firstLanguage);
                setBookPage(books[book].getCurrentPageURL(), book);
            } catch (Exception e) {
                ok = false;
            }

            //2つの言語が指定されている場合シンクロを有効に
            if (ok && firstLanguage != -1 && secondLanguage != -1)
                setSynchronizedReadingActive(true);

            parallelText = true;
        }
        return ok;
    }

    //シンクロの有効/無効を切り返る。引数でどちらか指定する
    public void setSynchronizedReadingActive(boolean value) {
        synchronizedReadingActive = value;
    }

    //シンクロのオン/オフを切り替える
    public boolean flipSynchronizedReadingActive() {
        if (exactlyOneBookOpen())
            return false;
        synchronizedReadingActive = !synchronizedReadingActive;
        return true;
    }

    //シンクロのオン/オフを返す
    public boolean syncMode() {
        return synchronizedReadingActive;
    }

    public boolean synchronizeView(int from, int to) throws Exception {
        if (!exactlyOneBookOpen()) {
            setBookPage(books[to].goToPage(books[from]
                    .getCurrentSpineElementIndex()), to);
            return true;
        } else
            return false;
    }

    // display book metadata
    // returns true if metadata are available, false otherwise
    public boolean displayMetadata(int book) {
        boolean res = true;
        if (books[book] != null) {
            DataView dv = new DataView();
            dv.loadData(books[book].metadata());
            changePanel(dv, book);
        } else
            res = false;

        return res;
    }

    // return true if TOC is available, false otherwise
    public boolean displayTOC(int book) {
        boolean res = true;

        if (books[book] != null)
            setBookPage(books[book].tableOfContents(), book);
        else
            res = false;
        return res;
    }

    public void changeCSS(int book, String[] settings) {
        books[book].addCSS(settings);
        loadPageIntoView(books[book].getCurrentPageURL(), book);
    }

    public boolean extractAudio(int book) {
        if (books[book].getAudio().length > 0) {
            extractAudio[book] = true;
            AudioView a = new AudioView();
            a.setAudioList(books[book].getAudio());
            changePanel(a, (book + 1) % nBooks);
            return true;
        }
        return false;
    }

    public void changeViewsSize(float weight) {
        if (views[0] != null && views[1] != null) {
            views[0].changeWeight(1 - weight);
            views[1].changeWeight(weight);
        }
    }

    public boolean isParallelTextOn() {
        return parallelText;
    }

    public boolean isSynchronized() {
        return synchronizedReadingActive;
    }

    public boolean atLeastOneBookOpen() {
        for (int i = 0; i < nBooks; i++)
            if (books[i] != null)
                return true;
        return false;
    }

    //書籍が1つだけ開いていた場合trueを返す(0や2以上ならfalse)
    public boolean exactlyOneBookOpen() {
        int i = 0;
        //空でない書籍を探す(iは空の書籍の数になる)
        while (i < nBooks && books[i] == null)
            i++;

        //すべての書籍が空の場合、falseを返す
        if (i == nBooks)
            return false;

        i++;

        //さらに空でない書籍を探す
        while (i < nBooks && books[i] == null)
            i++;

        if (i == nBooks) {
            //ほかに空でない書籍が見つからなかった場合trueを返す
            return true;
        } else
            //ほかにも空でない書籍が見つかった場合falseを返す
            return false;
    }




    //indexの位置(index=0:上、index=1:下)にあるパネルを新しいパネルpに変える
    public void changePanel(SplitPanel p, int index) {
        if (views[index] != null) {
            activity.removePanelWithoutClosing(views[index]);
            p.changeWeight(views[index].getWeight());
        }

        /*
        if(p.isAdded())
        	activity.removePanelWithoutClosing(p);
         */

        views[index] = p;
        activity.addPanel(p);
        p.setKey(index);

        for (int i = index + 1; i < views.length; i++)
            if (views[i] != null) {
                activity.detachPanel(views[i]);
                activity.attachPanel(views[i]);
            }
    }

    //クラス名から新たにパネルを生成
    private SplitPanel newPanelByClassName(String className) {
        if (className.equals(BookView.class.getName()))
            return new BookView();
        if (className.equals(DataView.class.getName()))
            return new DataView();
        if (className.equals(AudioView.class.getName()))
            return new AudioView();
        return null;
    }

    //状態保存
    public void saveState(Editor editor) {

        editor.putBoolean(getS(R.string.sync), synchronizedReadingActive);
        editor.putBoolean(getS(R.string.parallelTextBool), parallelText);

        //書籍保存
        for (int i = 0; i < nBooks; i++)
            if (books[i] != null) {
                editor.putInt(getS(R.string.CurrentPageBook) + i,
                        books[i].getCurrentSpineElementIndex());
                editor.putInt(getS(R.string.LanguageBook) + i,
                        books[i].getCurrentLanguage());
                editor.putString(getS(R.string.nameEpub) + i,
                        books[i].getDecompressedFolder());
                editor.putString(getS(R.string.pathBook) + i,
                        books[i].getFileName());
                editor.putBoolean(getS(R.string.exAudio) + i, extractAudio[i]);
                try {
                    books[i].closeStream();
                } catch (IOException e) {
                    Log.e(getS(R.string.error_CannotCloseStream),
                            getS(R.string.Book_Stream) + (i + 1));
                    e.printStackTrace();
                }
            } else {
                editor.putInt(getS(R.string.CurrentPageBook) + i, 0);
                editor.putInt(getS(R.string.LanguageBook) + i, 0);
                editor.putString(getS(R.string.nameEpub) + i, null);
                editor.putString(getS(R.string.pathBook) + i, null);
            }

        //ビューの保存
        for (int i = 0; i < nBooks; i++)
            if (views[i] != null) {
                editor.putString(getS(R.string.ViewType) + i, views[i]
                        .getClass().getName());
                views[i].saveState(editor);
                activity.removePanelWithoutClosing(views[i]);
            } else
                editor.putString(getS(R.string.ViewType) + i, "");
    }

    //状態の読み込み
    public boolean loadState(SharedPreferences preferences) {
        boolean ok = true;
        synchronizedReadingActive = preferences.getBoolean(getS(R.string.sync), false);
        parallelText = preferences.getBoolean(getS(R.string.parallelTextBool), false);

        int current, lang;
        String name, path;
        for (int i = 0; i < nBooks; i++) {
            current = preferences.getInt(getS(R.string.CurrentPageBook) + i, 0);
            lang = preferences.getInt(getS(R.string.LanguageBook) + i, 0);
            name = preferences.getString(getS(R.string.nameEpub) + i, null);
            path = preferences.getString(getS(R.string.pathBook) + i, null);
            extractAudio[i] = preferences.getBoolean(getS(R.string.exAudio) + i, false);
            //抽出された書籍の読み込み
            if (path != null) {
                try {
                    books[i] = new com.kyushuuniv.epubreader.EpubManipulator(path, name, current, lang, context);
                    books[i].goToPage(current);
                } catch (Exception e1) {

                    // exception: retry this way
                    try {
                        books[i] = new com.kyushuuniv.epubreader.EpubManipulator(path, i + "", context);
                        books[i].goToPage(current);
                    } catch (Exception e2) {
                        ok = false;
                    } catch (Error e3) {
                        ok = false;
                    }
                } catch (Error e) {
                    // error: retry this way
                    try {
                        books[i] = new com.kyushuuniv.epubreader.EpubManipulator(path, i + "", context);
                        books[i].goToPage(current);
                    } catch (Exception e2) {
                        ok = false;
                    } catch (Error e3) {
                        ok = false;
                    }
                }
            } else
                books[i] = null;
        }
        return ok;
    }

    //保存状態を受けて最も適したビューを読み込みパネルを追加(views[i]にはBookViewやAudioViewなどが入る)
    public void loadViews(SharedPreferences preferences) {
        for (int i = 0; i < nBooks; i++) {
            views[i] = newPanelByClassName(preferences.getString(getS(R.string.ViewType) + i, ""));
            if (views[i] != null) {
                activity.addPanel(views[i]);
                views[i].setKey(i);
                if (views[i] instanceof AudioView)
                    ((AudioView) views[i]).setAudioList(books[i > 0 ? i - 1 : nBooks - 1].getAudio());
                views[i].loadState(preferences);
            }
        }
    }

    //
    public String getS(int id) {
        return context.getResources().getString(id);
    }

    //指定のパネル(0:上、1:下)の現在のページを返す
    public int getPageNum(int index){
        return books[index].currentSpineElementIndex+1;
    }

    //指定のパネルに開いている書籍ファイルのファイル名を返す
    public String getBookName(int index){
        String name = books[index].fileName;
        //"/"が含まれる場合はファイルパスと判断してファイル名を得る
        while(true){
            int i = name.indexOf('/') + 1;
            if(i == 0)
                break;
            else{
                name = name.substring(i);
            }
        }
        //"."が含まれる場合は拡張子と判断して拡張子を削除
        int i = name.indexOf('.') + 1;
        if(i != 0){
            name = name.substring(0, i - 1);
        }
        //アンダーバーによる区切りを半角スペースによる区切りに変える
        name = name.replace('_',' ');
        return name;
    }

}
