package com.mrikso.apkrepacker.utils.grep;

import android.annotation.SuppressLint;
import android.os.Parcel;
import android.os.Parcelable;

import com.jecelyin.common.task.JecAsyncTask;
import com.jecelyin.common.task.TaskListener;
import com.jecelyin.common.task.TaskResult;
import com.jecelyin.common.utils.DLog;
import com.jecelyin.editor.v2.io.FileEncodingDetector;


import org.apache.commons.io.FileUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * @author https://github.com/drippel/JavaGrep
 */
public class ExtGrep implements Parcelable {

    public static final Creator<ExtGrep> CREATOR = new Creator<ExtGrep>() {
        public ExtGrep createFromParcel(Parcel source) {
            return new ExtGrep(source);
        }

        public ExtGrep[] newArray(int size) {
            return new ExtGrep[size];
        }
    };
    final List<String> includeFilePatterns = new ArrayList<String>();
    final List<String> excludeDirPatterns = new ArrayList<String>();
    public List<File> filesToProcess = new ArrayList<>();
    boolean invertMatch = false;
    boolean ignoreCase = false;
    int maxCount = 0;
    boolean printFileNameOnly = false;
    boolean printByteOffset = false;
    boolean quiet = false;
    boolean printCountOnly = false;
    boolean printFilesWithoutMatch = false;
    boolean wordRegex = false;
    boolean lineRegex = false;
    boolean noMessages = false;
    boolean printFileName = false;
    boolean printMatchOnly = false;
    boolean printLineNumber = false;
    boolean recurseDirectories = false;
    boolean skipDirectories = false;
    List<String> excludeFilePatterns = new ArrayList<String>();
    boolean useInclude = false;
    boolean useExclude = false;
    int beforeContext = 0;
    int afterContext = 0;
    private Pattern grepPattern;
    private String regex;
    private boolean useRegex;
    ArrayList<String> mExtensions = new ArrayList<>();

    public ExtGrep() {
    }

    protected ExtGrep(Parcel in) {
        this.invertMatch = in.readByte() != 0;
        this.ignoreCase = in.readByte() != 0;
        this.maxCount = in.readInt();
        this.printFileNameOnly = in.readByte() != 0;
        this.printByteOffset = in.readByte() != 0;
        this.quiet = in.readByte() != 0;
        this.printCountOnly = in.readByte() != 0;
        this.printFilesWithoutMatch = in.readByte() != 0;
        this.wordRegex = in.readByte() != 0;
        this.lineRegex = in.readByte() != 0;
        this.noMessages = in.readByte() != 0;
        this.printFileName = in.readByte() != 0;
        this.printMatchOnly = in.readByte() != 0;
        this.printLineNumber = in.readByte() != 0;
        this.recurseDirectories = in.readByte() != 0;
        this.skipDirectories = in.readByte() != 0;
        this.excludeFilePatterns = in.createStringArrayList();
        this.useInclude = in.readByte() != 0;
        this.useExclude = in.readByte() != 0;
        this.beforeContext = in.readInt();
        this.afterContext = in.readInt();
        this.regex = in.readString();
        this.filesToProcess = new ArrayList<>();
        in.readList(this.filesToProcess, List.class.getClassLoader());
    }

    public static String parseReplacement(MatcherResult m, String replaceText) {
        boolean escape = false;
        boolean dollar = false;

        StringBuilder buffer = new StringBuilder();
        int length = replaceText.length();
        for (int i = 0; i < length; i++) {
            char c = replaceText.charAt(i);
            if (c == '\\' && !escape) {
                escape = true;
            } else if (c == '$' && !escape) {
                dollar = true;
            } else if (c >= '0' && c <= '9' && dollar) {
                int group = c - '0';
                if (group < m.groupCount())
                    buffer.append(m.group(group));
                dollar = false;
            } else if (c == 'r' && escape) {
                buffer.append('\r');
                escape = false;
            } else if (c == 'n' && escape) {
                buffer.append('\n');
                escape = false;
            } else if (c == 't' && escape) {
                buffer.append('\t');
                escape = false;
            } else {
                buffer.append(c);
                dollar = false;
                escape = false;
            }
        }

        // This seemingly stupid piece of code reproduces a JDK bug.
        if (escape) {
            throw new ArrayIndexOutOfBoundsException(replaceText.length());
        }
        return buffer.toString();
    }

    private static String escapeRegexChar(String pattern) {
        final String metachar = ".^$[]*+?|()\\{}";

        StringBuilder newpat = new StringBuilder();

        int len = pattern.length();

        for (int i = 0; i < len; i++) {
            char c = pattern.charAt(i);
            if (metachar.indexOf(c) >= 0) {
                newpat.append('\\');
            }
            newpat.append(c);
        }
        return newpat.toString();
    }

    private void printMessage(final String msg) {

//        if (!quiet) {
//            System.out.println(msg);
//        }
    }

    private List<Result> grepFiles() {
        ArrayList<Result> results = new ArrayList<>();
        // at this point the list was expanded into file names
        for (final File f : filesToProcess) {
            if (!f.exists() || !f.canRead())
                continue;
            results.addAll(Objects.requireNonNull(grepFile(f)));
        }
        return results;
    }

    void readExcludeFrom(final String vals[]) {

        for (String val : vals) {
            try {
                BufferedReader br = new BufferedReader(new FileReader(new File(val)));
                for (String line = br.readLine(); line != null; line = br.readLine()) {
                    excludeFilePatterns.add(line);
                }

            } catch (IOException e) {
                DLog.e(e);
            }
        }

    }

    void readIncludesFrom(final String vals[]) {

        for (String val : vals) {
            try {
                BufferedReader br = new BufferedReader(new FileReader(new File(val)));
                for (String line = br.readLine(); line != null; line = br.readLine()) {
                    includeFilePatterns.add(line);
                }

            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

    }

    public void printErrorMessage(final String msg) {

        if (!noMessages) {
            System.out.println(msg);
        }
    }

    private Result printMatch(final File file, final String line, final int lineNumber,
                              final int startOffset, final int endOffset, final int lineStartOffset, final int matchStart, final int matchEnd, final List<String> beforeContextLines,
                              final List<String> afterContextLines) {
        //int maxText = 20;
        //int start = Math.max(lineStartOffset - maxText, 0);
       // int end = Math.min(lineStartOffset + maxText, line.length());
        Result result = new Result();
        result.file = file;
        //не обзезать найденую строку
         result.line = line;//.substring((int) start, (int) end);
      //  result.line = line.substring((int) start, (int) end);//строка будет обрезаться
        result.lineNumber = lineNumber;
        result.startOffset = startOffset;
        result.endOffset = endOffset;
        result.lineStartOffset = lineStartOffset;
        result.matchStart = matchStart;
        result.matchEnd = matchEnd;
       // if (result.matchEnd > end - start) {
        //    result.matchEnd = end - start;
      //  }
        return result;
    }

    public void verifyFileList() {

        List<File> list = new ArrayList<>();

        for (final File f : filesToProcess) {

            if (f.exists()) {
                if (f.isFile()) {
                    if (includeFile(f)) {
                        if (excludeFile(f)) {
                            list.add(f);
                        }
                    }
                } else if (f.isDirectory()) {
                    String[] suffixs = mExtensions.toArray(new String[mExtensions.size()]);
                    Collection<File> children = FileUtils.listFiles(f,suffixs , recurseDirectories);
                    list.addAll(children);
//                    if (recurseDirectories) {
//                        list.addAll(recurseDir(f));
//                    }
                }
            }
        }

        filesToProcess = list;
    }

    private List<File> recurseDir(final File dir) {

        List<File> files = new ArrayList<File>();

        for (File f : dir.listFiles()) {

            if (f.isFile()) {
                if (includeFile(f)) {
                    if (excludeFile(f)) {
                        files.add(f);
                    }
                }
            } else if (f.isDirectory()) {
                if (!excludeDir(f)) {
                    files.addAll(recurseDir(f));
                }
            }

        }

        return files;
    }

    void readRegexFromFile(final String... fnames) {

    }

    void readLongRegexFromFile(final String[] fnames) {


    }

    private void reset() {

        grepPattern = null;
        regex = null;
        filesToProcess = null;
        excludeFilePatterns.clear();

    }

    public boolean includeFile(final File f) {

        if (!useInclude) {
            return true;
        }

//        if( CollectionUtils.exists( includeFilePatterns, wildcardMatcher( f ) ) ) {
//            return true;
//        }

        return false;
    }

    public boolean excludeFile(final File f) {

        /*
        if( !useExclude ) {
           return false;
       }


         */
        FileFilterCriteria d = new FileFilterCriteria();
        return d.accept(f);
        //if( ///CollectionUtils.exists( excludeFilePatterns, wildcardMatcher( f ) ) ) {
        //   return true;
        //   }

        // return IOUtils.isBinaryFile(f); //对中文有误杀
        // return false;
    }

    private boolean excludeDir(final File f) {

      //  String dir = "original";
        // return DirectoryFileFilter.DIRECTORY.accept(f,dir );
        //   if( CollectionUtils.exists( excludeDirPatterns, wildcardMatcher( f ) ) ) {
              // return true;
       //  }
        return false;
    }

    private boolean printFileNamesOnly() {

        return printFilesWithoutMatch || printFileNameOnly;
    }

    private Matcher matchAny(final CharSequence line) {
        Matcher m = grepPattern.matcher(line);
        if (m.find()) {
            return m;
        }

        return null;
    }

    public String replaceAll(String text, String replaceText) {
        compilePattern();
        Matcher m = grepPattern.matcher(text);
        return m.replaceAll(replaceText);
        /*ArrayList<Integer> array = new ArrayList<>();
        // 从头开始搜索获取所有位置
        while (m.find()) {
            array.add(m.start());
            array.add(m.end());
        }
        int size = array.size();
        if (size == 0) {
           // UIUtils.toast(text.getContext(), text.getContext().getResources().getQuantityString(R.plurals.x_text_replaced, 0));
            return;
        }
        int count = 0;
        for (int i = size - 2; i >= 0; i -= 2) {
            count++;
            text.(array.get(i), array.get(i + 1), replaceText);
        }*/
      //  UIUtils.toast(text.getContext(), text.getContext().getResources().getQuantityString(R.plurals.x_text_replaced, count, count));
    }

    private List<Result> grepFile(final File file) {
        int lineNumber = 0;
        int count = 0;
        int byteOffset = 0;
        List<String> beforeContextLines = new LinkedList<String>();
        List<String> afterContextLines = new LinkedList<String>();
        ArrayList<Result> results = new ArrayList<>();
        BufferedReader bfr = null;
        try {
            String encoding = FileEncodingDetector.detectEncoding(file);
            bfr = new BufferedReader(new InputStreamReader(new FileInputStream(file), encoding), 16000);
            for (String line = bfr.readLine(); line != null; line = bfr.readLine()) {

                lineNumber++;
                Matcher m = matchAny(line);

                if (((m != null) && !invertMatch)) {
                    count++;
                    if (afterContext > 0) {
                        afterContextLines.clear();
                        bfr.mark(4000);
                        for (int i = 0; i < afterContext; i++) {
                            String s = bfr.readLine();
                            if (s != null) {
                                afterContextLines.add(s);
                            } else {
                                break;
                            }
                        }
                        bfr.reset();
                    }
                    String match = line;
                    if (printMatchOnly) {
                        match = m.group();
                    }
//                    printMatch(file, match, lineNumber, count, (byteOffset + m.start()), beforeContextLines,
                    results.add(printMatch(file, match, lineNumber, byteOffset + m.start(), byteOffset+ m.end(), m.start(), m.start(), m.end(), beforeContextLines,
                            afterContextLines));
                    if (printFileNameOnly) {
                        return null;
                    }
                    //
                } else if ((m == null) && invertMatch) {
                    count++;
                    results.add(printMatch(file, line, lineNumber, byteOffset, byteOffset, byteOffset,byteOffset, byteOffset, beforeContextLines,
                            afterContextLines));
                    // TODO: this has a slightly different meaning
                    if (printFileNameOnly) {
                        return null;
                    }
                }

                if ((maxCount != 0) && (count >= maxCount)) {
                    break;
                }

                byteOffset += line.length();
                byteOffset += 1; // TODO: end of line char - what about DOS/Win32?

                beforeContextLines.add(line);
                if (beforeContextLines.size() > beforeContext) {
                    beforeContextLines.remove(0);
                }
            }
        } catch (Exception ioe) {
            DLog.e(ioe);
        } finally {
            if (bfr != null)
                try {
                    bfr.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }

        if (count == 0) {
            // no matches in file
            if (printFilesWithoutMatch) {
                printMessage(file.getName());
            }
        }

        if (printCountOnly) {
            printMessage((file.getName() + ":" + count));
        }

        return results;
    }

    @SuppressLint("StaticFieldLeak")
    public void grepText(final GrepDirect direct, final CharSequence line, int start, TaskListener<MatcherResult> listener) {
        new JecAsyncTask<Integer, Void, MatcherResult>() {

            @Override
            protected void onRun(TaskResult<MatcherResult> taskResult, Integer... params) throws Exception {
                compilePattern();
                MatcherResult results = null;
                int index = params[0];
                Matcher m = grepPattern.matcher(line);
                if (direct == GrepDirect.NEXT) {
                    for (; true; ) {
                        if (m.find(index)) {
                            results = new MatcherResult(m);
                            break;
                        } else if (index > 0) {
                            index = 0;
                        } else {
                            break;
                        }
                    }
                } else {
                    if (index <= 0)
                        index = line.length();

                    // 从头开始搜索获取所有位置
                    while (m.find()) {
                        if (m.end() >= index) {
                            break;
                        }
                        results = new MatcherResult(m);
                    }
                }
                taskResult.setResult(results);
            }
        }.setTaskListener(listener).execute(start);
    }

    public void setRegex(final String r, boolean useRegex) {
        regex = !useRegex ? escapeRegexChar(r) : r;
        this.useRegex = useRegex;
    }

    public boolean isUseRegex() {
        return useRegex;
    }

    public String getRegex() {
        return regex;
    }

    private void compilePattern() {

//        int flags = Pattern.COMMENTS;
        int flags = 0;

        String pattern = regex;

        if (ignoreCase) {
            flags |= Pattern.CASE_INSENSITIVE;
        }

        // we are ignoring line-regex if both are supplied
        if (wordRegex) {
            // poor mans way to do it
            pattern = "\\b" + pattern + "\\b";
        } else {
            if (lineRegex) {

                // poor mans way to do it
                pattern = "^" + pattern + "$";
            }
        }

        grepPattern = Pattern.compile(pattern, flags);

    }

    public void addFile(final String name) {

        filesToProcess.add(new File(name));
    }

    @SuppressLint("StaticFieldLeak")
    public void execute(TaskListener<List<Result>> listener) {
        new JecAsyncTask<Void, Void, List<Result>>() {

            @Override
            protected void onRun(TaskResult<List<Result>> taskResult, Void... params) throws Exception {
                compilePattern();
                verifyFileList();
                List<Result> results = grepFiles();
                taskResult.setResult(results);
            }
        }.setTaskListener(listener).execute();
    }

    public  List<Result> execute(){
        compilePattern();
        verifyFileList();
        return grepFiles();
    }
    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByte(invertMatch ? (byte) 1 : (byte) 0);
        dest.writeByte(ignoreCase ? (byte) 1 : (byte) 0);
        dest.writeInt(this.maxCount);
        dest.writeByte(printFileNameOnly ? (byte) 1 : (byte) 0);
        dest.writeByte(printByteOffset ? (byte) 1 : (byte) 0);
        dest.writeByte(quiet ? (byte) 1 : (byte) 0);
        dest.writeByte(printCountOnly ? (byte) 1 : (byte) 0);
        dest.writeByte(printFilesWithoutMatch ? (byte) 1 : (byte) 0);
        dest.writeByte(wordRegex ? (byte) 1 : (byte) 0);
        dest.writeByte(lineRegex ? (byte) 1 : (byte) 0);
        dest.writeByte(noMessages ? (byte) 1 : (byte) 0);
        dest.writeByte(printFileName ? (byte) 1 : (byte) 0);
        dest.writeByte(printMatchOnly ? (byte) 1 : (byte) 0);
        dest.writeByte(printLineNumber ? (byte) 1 : (byte) 0);
        dest.writeByte(recurseDirectories ? (byte) 1 : (byte) 0);
        dest.writeByte(skipDirectories ? (byte) 1 : (byte) 0);
        dest.writeStringList(this.excludeFilePatterns);
        dest.writeByte(useInclude ? (byte) 1 : (byte) 0);
        dest.writeByte(useExclude ? (byte) 1 : (byte) 0);
        dest.writeInt(this.beforeContext);
        dest.writeInt(this.afterContext);
        dest.writeString(this.regex);
        dest.writeList(this.filesToProcess);
    }

    public enum GrepDirect {
        PREV,
        NEXT,
    }

    public static interface OnSearchFinishListener {
        public void onFinish(List<Result> results);
    }

    public static class Result {
        public File file;
        public String line;
        public int lineNumber;
        public int lineStartOffset;
        public int startOffset;
        public int endOffset;
        public int matchStart;
        public int matchEnd;

        private Result() {
        }
    }

    class FileFilterCriteria implements FileFilter {

   //     private final String[] fileExtension = new String[]{".xml", ".txt", ".json", ".smali"};

        public boolean accept(File file) {
            // System.out.println("File from directory:- " + file.getName());
            for (String extension : mExtensions) {
                if (file.getName().toLowerCase().endsWith(extension)) {
                    return true;
                }
            }
            return false;
        }
    }
}
