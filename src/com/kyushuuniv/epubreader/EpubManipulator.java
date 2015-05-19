package com.kyushuuniv.epubreader;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import nl.siegmann.epublib.domain.Author;
import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.domain.Metadata;
import nl.siegmann.epublib.domain.Spine;
import nl.siegmann.epublib.domain.SpineReference;
import nl.siegmann.epublib.domain.TOCReference;
import nl.siegmann.epublib.domain.TableOfContents;
import nl.siegmann.epublib.epub.EpubReader;
import android.content.Context;
import android.os.Environment;
import android.util.Log;

public class EpubManipulator {
    private Book book;
    public int currentSpineElementIndex; //現在のページ番号
    private String currentPage;
    private String[] spineElementPaths;
    private int pageCount;
    private int currentLanguage;
    //利用可能な言語リスト
    private List<String> availableLanguages;
    private List<Boolean> translations;
    private String decompressedFolder;
    private String pathOPF;
    private static Context context;
    private static String location = Environment.getExternalStorageDirectory() + "/epubtemp/";
    public String fileName; //書籍名
    FileInputStream fs;
    private String actualCSS = "";
    private String[][] audio;

    //ファイル名より書籍データを作成
    public EpubManipulator(String fileName, String destFolder, Context theContext) throws Exception {

        List<String> spineElements;
        List<SpineReference> spineList;
        if (context == null) {
            context = theContext;
        }

        this.fs = new FileInputStream(fileName);
        //this.book = (new EpubReader()).readEpub(fs);
        EpubReader er = new EpubReader();
        this.book = er.readEpub(fs);


        this.fileName = fileName;
        this.decompressedFolder = destFolder;
        Spine spine = book.getSpine();
        spineList = spine.getSpineReferences();
        this.currentSpineElementIndex = 0;
        this.currentLanguage = 0;

        spineElements = new ArrayList<String>();
        pages(spineList, spineElements);
        this.pageCount = spineElements.size();

        this.spineElementPaths = new String[spineElements.size()];

        unzip(fileName, location + decompressedFolder);

        pathOPF = getPathOPF(location + decompressedFolder);

        for (int i = 0; i < spineElements.size(); ++i) {
            this.spineElementPaths[i] = "file://" + location
                    + decompressedFolder + "/" + pathOPF + "/"
                    + spineElements.get(i);
        }

        if (spineElements.size() > 0) {
            goToPage(0);
        }
        createTocFile();
    }

    //既存の非圧縮フォルダから書籍データを作成
    public EpubManipulator(String fileName, String folder, int spineIndex, int language, Context theContext) throws Exception {
        List<String> spineElements;
        List<SpineReference> spineList;

        if (context == null) {
            context = theContext;
        }

        this.fs = new FileInputStream(fileName);
        this.book = (new EpubReader()).readEpub(fs);
        this.fileName = fileName;
        this.decompressedFolder = folder;

        Spine spine = book.getSpine();
        spineList = spine.getSpineReferences();
        this.currentSpineElementIndex = spineIndex;
        this.currentLanguage = language;
        spineElements = new ArrayList<String>();
        pages(spineList, spineElements);
        this.pageCount = spineElements.size();
        this.spineElementPaths = new String[spineElements.size()];

        pathOPF = getPathOPF(location + folder);

        for (int i = 0; i < spineElements.size(); ++i) {
            this.spineElementPaths[i] = "file://" + location + folder + "/"
                    + pathOPF + "/" + spineElements.get(i);
        }
        goToPage(spineIndex);
    }

    //言語インデックス内の言語番号を受け、対応する言語を現在の言語としてセットする
    public void setLanguage(int lang) throws Exception {
        if ((lang >= 0) && (lang <= this.availableLanguages.size())) {
            this.currentLanguage = lang;
        }
        goToPage(this.currentSpineElementIndex);
    }

    //言語名を受け、その言語を現在の言語としてセットする
    public void setLanguage(String lang) throws Exception {
        int i = 0;
        while ((i < this.availableLanguages.size()) && (!(this.availableLanguages.get(i).equals(lang)))) {
            i++;
        }
        setLanguage(i);
    }

    //現在のファイルにおいて利用可能な言語リストを言語名の文字列のリストに直して返す
    public String[] getLanguages() {
        String[] lang = new String[availableLanguages.size()];
        for (int i = 0; i < availableLanguages.size(); i++) {
            lang[i] = availableLanguages.get(i);
        }
        return lang;
    }

    //別言語での表示関連
    private void pages(List<SpineReference> spineList, List<String> pages) {
        int langIndex;
        String lang;
        String actualPage;

        this.translations = new ArrayList<Boolean>();
        this.availableLanguages = new ArrayList<String>();

        for (int i = 0; i < spineList.size(); ++i) {
            actualPage = (spineList.get(i)).getResource().getHref();
            lang = getPageLanguage(actualPage);
            if (lang != "") {
                //ほかの言語が利用可能な場合
                langIndex = languageIndexFromID(lang);
                //まだ利用可能言語インデックス(availableLanguages)に未登録の言語なら登録する
                if (langIndex == this.availableLanguages.size())
                    this.availableLanguages.add(lang);

                //言語の利用可/不可を記述するインデックスの内、対応する言語を利用可能(true)にする
                if (langIndex == 0) {
                    this.translations.add(true);
                    pages.add(actualPage);
                }
            } else {
                //ほかに利用可能な言語がなかった場合
                this.translations.add(false);
                pages.add(actualPage);
            }
        }
    }

    //言語名を引数として受け、対応する要素番号を返す
    private int languageIndexFromID(String id) {
        int i = 0;
        while ((i < availableLanguages.size()) && (!(availableLanguages.get(i).equals(id)))) {
            i++;
        }
        return i;
    }

    //OPFファイルが存在するディレクトリのパスを返す
    private static String getPathOPF(String unzipDir) throws IOException {
        String pathOPF = "";
        //OPFのパスとディレクトリをcontainer.xmlから取得。
        //OPFは形式のひとつで、ePubの構成要素やめたデータを定義する。各種仕様が書かれている
        BufferedReader br = new BufferedReader(new FileReader(unzipDir + "/META-INF/container.xml"));
        String line;
        while ((line = br.readLine()) != null) {
            if (line.indexOf(getS(R.string.full_path)) > -1) {
                int start = line.indexOf(getS(R.string.full_path));
                int start2 = line.indexOf("\"", start);
                int stop2 = line.indexOf("\"", start2 + 1);
                if (start2 > -1 && stop2 > start2) {
                    pathOPF = line.substring(start2 + 1, stop2).trim();
                    break;
                }
            }
        }
        br.close();

        //OPFファイルがルートにあった場合(パスが/を含まない場合)
        if (!pathOPF.contains("/"))
            pathOPF = "";

        //パスからOPFファイル名の部分だけなくす(OPFファイルのパスをOPFファイルがある場所のパスに直す)
        int last = pathOPF.lastIndexOf('/');
        if (last > -1) {
            pathOPF = pathOPF.substring(0, last);
        }
        return pathOPF;
    }

    //ZIP解除(改良が必要)
    public void unzip(String inputZip, String destinationDirectory) throws IOException {
        int BUFFER = 2048;
        List zipFiles = new ArrayList();
        File sourceZipFile = new File(inputZip);
        File unzipDestinationDirectory = new File(destinationDirectory);
        unzipDestinationDirectory.mkdir();

        ZipFile zipFile;
        zipFile = new ZipFile(sourceZipFile, ZipFile.OPEN_READ);
        Enumeration zipFileEntries = zipFile.entries();

        // Process each entry
        while (zipFileEntries.hasMoreElements()) {

            ZipEntry entry = (ZipEntry) zipFileEntries.nextElement();
            String currentEntry = entry.getName();
            File destFile = new File(unzipDestinationDirectory, currentEntry);

            if (currentEntry.endsWith(getS(R.string.zip))) {
                zipFiles.add(destFile.getAbsolutePath());
            }

            File destinationParent = destFile.getParentFile();
            destinationParent.mkdirs();

            if (!entry.isDirectory()) {
                BufferedInputStream is = new BufferedInputStream(
                        zipFile.getInputStream(entry));
                int currentByte;
                // buffer for writing file
                byte data[] = new byte[BUFFER];

                FileOutputStream fos = new FileOutputStream(destFile);
                BufferedOutputStream dest = new BufferedOutputStream(fos,
                        BUFFER);

                while ((currentByte = is.read(data, 0, BUFFER)) != -1) {
                    dest.write(data, 0, currentByte);
                }
                dest.flush();
                dest.close();
                is.close();

            }

        }
        zipFile.close();

        for (Iterator iter = zipFiles.iterator(); iter.hasNext(); ) {
            String zipName = (String) iter.next();
            unzip(zipName,
                    destinationDirectory
                            + File.separatorChar
                            + zipName.substring(0,
                            zipName.lastIndexOf(getS(R.string.zip))));
        }
    }

    //streamを閉じる
    public void closeStream() throws IOException {
        fs.close();
        book = null;
    }

    //streamとextraction folderを閉じる
    public void destroy() throws IOException {
        closeStream();
        File c = new File(location + decompressedFolder);
        deleteDir(c);
    }

    //ディレクトリの再帰的除去
    private void deleteDir(File f) {
        if (f.isDirectory())
            for (File child : f.listFiles())
                deleteDir(child);
        f.delete();
    }

    //解凍されたフォルダ名を引数で渡された文字列に変更
    public void changeDirName(String newName) {
        File dir = new File(location + decompressedFolder);
        File newDir = new File(location + newName);
        dir.renameTo(newDir);

        for (int i = 0; i < spineElementPaths.length; ++i)
            spineElementPaths[i] = spineElementPaths[i].replace("file://" + location + decompressedFolder, "file://" + location + newName);
        decompressedFolder = newName;
        try {
            goToPage(currentSpineElementIndex);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //現在の言語でページを取得
    public String goToPage(int page) throws Exception {
        return goToPage(page, this.currentLanguage);
    }

    //引数で渡された言語でページを取得
    public String goToPage(int page, int lang) throws Exception {
        String spineElement;
        String extension;
        if (page < 0) {
            page = 0;
        }
        if (page >= this.pageCount) {
            page = this.pageCount - 1;
        }
        this.currentSpineElementIndex = page;

        spineElement = this.spineElementPaths[currentSpineElementIndex];

        if (this.translations.get(page)) {
            extension = spineElement.substring(spineElement.lastIndexOf("."));
            spineElement = spineElement.substring(0,
                    spineElement.lastIndexOf(this.availableLanguages.get(0)));

            spineElement = spineElement + this.availableLanguages.get(lang)
                    + extension;
        }

        this.currentPage = spineElement;

        audioExtractor(currentPage);

        return spineElement;
    }

    //章を次の章へ移す
    public String goToNextChapter() throws Exception {
        return goToPage(this.currentSpineElementIndex + 1);
    }

    //章を前章へ移す
    public String goToPreviousChapter() throws Exception {
        return goToPage(this.currentSpineElementIndex - 1);
    }

    // 書籍のメタデータからHTMLページを作成して返す
    public String metadata() {
        List<String> tmp;
        Metadata metadata = book.getMetadata();
        String html = getS(R.string.htmlBodyTableOpen);

        //書籍のタイトル
        tmp = metadata.getTitles();
        if (tmp.size() > 0) {
            html += getS(R.string.titlesMeta);
            html += "<td>" + tmp.get(0) + "</td></tr>";
            for (int i = 1; i < tmp.size(); i++)
                html += "<tr><td></td><td>" + tmp.get(i) + "</td></tr>";
        }

        //書籍の著者
        List<Author> authors = metadata.getAuthors();
        if (authors.size() > 0) {
            html += getS(R.string.authorsMeta);
            html += "<td>" + authors.get(0).getFirstname() + " "
                    + authors.get(0).getLastname() + "</td></tr>";
            for (int i = 1; i < authors.size(); i++)
                html += "<tr><td></td><td>" + authors.get(i).getFirstname()
                        + " " + authors.get(i).getLastname() + "</td></tr>";
        }

        //書籍の投稿者
        authors = metadata.getContributors();
        if (authors.size() > 0) {
            html += getS(R.string.contributorsMeta);
            html += "<td>" + authors.get(0).getFirstname() + " "
                    + authors.get(0).getLastname() + "</td></tr>";
            for (int i = 1; i < authors.size(); i++) {
                html += "<tr><td></td><td>" + authors.get(i).getFirstname()
                        + " " + authors.get(i).getLastname() + "</td></tr>";
            }
        }

        //言語
        html += getS(R.string.languageMeta) + metadata.getLanguage()
                + "</td></tr>";

        //書籍の出版社
        tmp = metadata.getPublishers();
        if (tmp.size() > 0) {
            html += getS(R.string.publishersMeta);
            html += "<td>" + tmp.get(0) + "</td></tr>";
            for (int i = 1; i < tmp.size(); i++)
                html += "<tr><td></td><td>" + tmp.get(i) + "</td></tr>";
        }

        //書籍のタイプ
        tmp = metadata.getTypes();
        if (tmp.size() > 0) {
            html += getS(R.string.typesMeta);
            html += "<td>" + tmp.get(0) + "</td></tr>";
            for (int i = 1; i < tmp.size(); i++)
                html += "<tr><td></td><td>" + tmp.get(i) + "</td></tr>";
        }

        //書籍の説明
        tmp = metadata.getDescriptions();
        if (tmp.size() > 0) {
            html += getS(R.string.descriptionsMeta);
            html += "<td>" + tmp.get(0) + "</td></tr>";
            for (int i = 1; i < tmp.size(); i++)
                html += "<tr><td></td><td>" + tmp.get(i) + "</td></tr>";
        }

        //書籍の権利
        tmp = metadata.getRights();
        if (tmp.size() > 0) {
            html += getS(R.string.rightsMeta);
            html += "<td>" + tmp.get(0) + "</td></tr>";
            for (int i = 1; i < tmp.size(); i++)
                html += "<tr><td></td><td>" + tmp.get(i) + "</td></tr>";
        }

        html += getS(R.string.tablebodyhtmlClose);
        return html;
    }

    //目次TOC(Table of Contents)情報を持つファイルからHTMLを作成して返す
    public String r_createTocFile(TOCReference e) {

        String childrenPath = "file://" + location + decompressedFolder + "/"
                + pathOPF + "/" + e.getCompleteHref();

        String html = "<ul><li>" + "<a href=\"" + childrenPath + "\">"
                + e.getTitle() + "</a>" + "</li></ul>";

        List<TOCReference> children = e.getChildren();

        for (int j = 0; j < children.size(); j++)
            html += r_createTocFile(children.get(j));

        return html;
    }

    //TOC情報を持ったHTMLファイルを作成
    public void createTocFile() {
        List<TOCReference> tmp;
        TableOfContents toc = book.getTableOfContents();
        String html = "<html><body><ul>";

        tmp = toc.getTocReferences();

        if (tmp.size() > 0) {
            html += getS(R.string.tocReference);
            for (int i = 0; i < tmp.size(); i++) {
                String path = "file://" + location + decompressedFolder + "/"
                        + pathOPF + "/" + tmp.get(i).getCompleteHref();

                html += "<li>" + "<a href=\"" + path + "\">"
                        + tmp.get(i).getTitle() + "</a>" + "</li>";

                List<TOCReference> children = tmp.get(i).getChildren();

                for (int j = 0; j < children.size(); j++)
                    html += r_createTocFile(children.get(j));
            }
        }

        html += getS(R.string.tablebodyhtmlClose);

        //HTMLファイルに書き出す
        String filePath = location + decompressedFolder + "/Toc.html";
        try {
            File file = new File(filePath);
            FileWriter fw = new FileWriter(file);
            fw.write(html);
            fw.flush();
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //table of contentsの書かれたHTMLファイルのパスを返す
    public String tableOfContents() {
        return "File://" + location + decompressedFolder + "/Toc.html";
    }

    //求められたページがブックを持つ場合はそのページ番号を返す。持たない場合は-1を返す
    public int getPageIndex(String page) {
        int result = -1;
        String lang;

        lang = getPageLanguage(page);
        if ((this.availableLanguages.size() > 0) && (lang != "")) {
            page = page.substring(0, page.lastIndexOf(lang))
                    + this.availableLanguages.get(0)
                    + page.substring(page.lastIndexOf("."));
        }
        for (int i = 0; i < this.spineElementPaths.length && result == -1; i++) {
            if (page.equals(this.spineElementPaths[i])) {
                result = i;
            }
        }

        return result;
    }

    //現在のページと言語をセットする。成功したらtrue、失敗したらfalseを返す
    public boolean goToPage(String page) {
        int index = getPageIndex(page);
        boolean res = false;
        if (index >= 0) {
            String newLang = getPageLanguage(page);
            try {
                goToPage(index);
                if (newLang != "") {
                    setLanguage(newLang);
                }
                res = true;
            } catch (Exception e) {
                res = false;
                Log.e(getS(R.string.error_goToPage), e.getMessage());
            }
        }
        return res;
    }

    //ISO 639-1の命名規則に基づきそのページの言語を返す
    //言語が見つからない場合は空文字を返す
    public String getPageLanguage(String page) {
        String[] tmp = page.split("\\.");
        // Language XY is present if the string format is "pagename.XY.xhtml",
        // where XY are 2 non-numeric characters that identify the language
        if (tmp.length > 2) {
            String secondFromLastItem = tmp[tmp.length - 2];
            if (secondFromLastItem.matches("[a-z][a-z]")) {
                return secondFromLastItem;
            }
        }
        return "";
    }

    public void addCSS(String[] settings) {
        //CSSファイル
        String css = "<style type=\"text/css\">\n";

        if (!settings[0].isEmpty()) {
            css = css + "body{color:" + settings[0] + ";}";
            css = css + "a:link{color:" + settings[0] + ";}";
        }

        if (!settings[1].isEmpty())
            css = css + "body {background-color:" + settings[1] + ";}";

        if (!settings[2].isEmpty())
            css = css + "p{font-family:" + settings[2] + ";}";

        if (!settings[3].isEmpty())
            css = css + "p{\n\tfont-size:" + settings[3] + "%\n}\n";

        if (!settings[4].isEmpty())
            css = css + "p{line-height:" + settings[4] + "em;}";

        if (!settings[5].isEmpty())
            css = css + "p{text-align:" + settings[5] + ";}";

        if (!settings[6].isEmpty())
            css = css + "body{margin-left:" + settings[6] + "%;}";

        if (!settings[7].isEmpty())
            css = css + "body{margin-right:" + settings[7] + "%;}";

        css = css + "</style>";

        for (int i = 0; i < spineElementPaths.length; i++) {
            String path = spineElementPaths[i].replace("file:///", "");
            String source = readPage(path);

            source = source.replace(actualCSS + "</head>", css + "</head>");

            writePage(path, source);
        }
        actualCSS = css;

    }

    //オーディオファイルの相対パスを絶対パスに直す
    private void adjustAudioLinks() {
        for (int i = 0; i < audio.length; i++)
            for (int j = 0; j < audio[i].length; j++) {
                if (audio[i][j].startsWith("./"))
                    audio[i][j] = currentPage.substring(0,
                            currentPage.lastIndexOf("/"))
                            + audio[i][j].substring(1);

                if (audio[i][j].startsWith("../")) {
                    String temp = currentPage.substring(0,
                            currentPage.lastIndexOf("/"));
                    audio[i][j] = temp.substring(0, temp.lastIndexOf("/"))
                            + audio[i][j].substring(2);
                }
            }
    }

    //すべてのオーディオタグのソースフィールドを抽出
    private ArrayList<String> getAudioSources(String audioTag) {
        ArrayList<String> srcs = new ArrayList<String>();
        Pattern p = Pattern.compile("src=\"[^\"]*\"");
        Matcher m = p.matcher(audioTag);
        while (m.find())
            srcs.add(m.group().replace("src=\"", "").replace("\"", ""));

        return srcs;
    }

    //xhtmlページからすべてのオーディオタグを抽出
    private ArrayList<String> getAudioTags(String page) {
        ArrayList<String> res = new ArrayList<String>();

        String source = readPage(page);

        Pattern p = Pattern.compile("<audio(?s).*?</audio>|<audio(?s).*?/>");
        Matcher m = p.matcher(source);
        while (m.find())
            res.add(m.group(0));

        return res;
    }

    //オーディオファイル抽出
    private void audioExtractor(String page) {
        ArrayList<String> tags = getAudioTags(page.replace("file:///", ""));
        ArrayList<String> srcs;
        audio = new String[tags.size()][];

        for (int i = 0; i < tags.size(); i++) {
            srcs = getAudioSources(tags.get(i));
            audio[i] = new String[srcs.size()];
            for (int j = 0; j < srcs.size(); j++)
                audio[i][j] = srcs.get(j);
        }
        adjustAudioLinks();
    }

    //オーディオファイルを返す
    public String[][] getAudio() {
        return audio;
    }

    //ページのパスを受け、そのページの内容をstringにして返す。取得できなかった場合は空文字列を返す
    private String readPage(String path) {
        try {
            FileInputStream input = new FileInputStream(path);
            byte[] fileData = new byte[input.available()];

            input.read(fileData);
            input.close();

            String xhtml = new String(fileData);
            return xhtml;
        } catch (IOException e) {
            return "";
        }
    }

    //pathで指定したページにxhtmlを書き足す。成功したらtrue、失敗したらfalseを返す
    private boolean writePage(String path, String xhtml) {
        try {
            File file = new File(path);
            FileWriter fw = new FileWriter(file);
            fw.write(xhtml);
            fw.flush();
            fw.close();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    //現在のSpineインデックスの要素番号を返す
    public int getCurrentSpineElementIndex() {
        return currentSpineElementIndex;
    }

    //Spineの要素番号を受け、そのパス(文字列)を返す
    public String getSpineElementPath(int elementIndex) {
        return spineElementPaths[elementIndex];
    }

    //現在のページのURLを返す
    public String getCurrentPageURL() {
        return currentPage;
    }

    //現在の言語を返す
    public int getCurrentLanguage() {
        return currentLanguage;
    }

    //ファイル名を返す
    public String getFileName() {
        return fileName;
    }

    //圧縮が解除されたフォルダを返す
    public String getDecompressedFolder() {
        return decompressedFolder;
    }

    //idを受けて対応する文字列リソースのcontextを返す
    public static String getS(int id) {
        return context.getResources().getString(id);
    }
}
