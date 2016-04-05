package com.eaglesakura.android.net.parser;

import com.eaglesakura.android.net.Result;
import com.eaglesakura.util.IOUtil;
import com.eaglesakura.util.RandomUtil;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class FileParser implements RequestParser<File> {
    @NonNull
    final File mLocalFile;

    /**
     * バッファサイズ
     */
    int mBufferSize = 1024 * 128;

    @Nullable
    ProgressListener mListener;

    public FileParser(@NonNull File localFile) {
        mLocalFile = localFile;
    }

    /**
     * ダウンロード時のキャッシュサイズを指定する
     */
    public void setBufferSize(int bufferSize) {
        mBufferSize = bufferSize;
    }

    /**
     * 経過取得用のListenerを取得する
     */
    public void setListener(@NonNull ProgressListener listener) {
        mListener = listener;
    }

    @Override
    public File parse(Result<File> sender, InputStream data) throws Exception {

        final File TEMP_FILE = new File(mLocalFile.getAbsolutePath() + "." + RandomUtil.randShortString() + ".bin");
        TEMP_FILE.getParentFile().mkdirs();
        FileOutputStream os = new FileOutputStream(TEMP_FILE);
        boolean completed = false;
        long downloaded = 0;
        try {

            byte[] buffer = new byte[mBufferSize];
            int readed = 0;

            while ((readed = data.read(buffer)) > 0) {
                os.write(buffer, 0, readed);
                downloaded += readed;
                if (mListener != null) {
                    mListener.onDownload(sender, downloaded);
                }

            }

            // 正常にコピーできたら、tempをターゲットへ変更する
            TEMP_FILE.renameTo(mLocalFile);

            completed = true;
        } finally {
            IOUtil.close(os);

            // 一時ファイルが残っている場合、削除する
            if (!completed) {
                TEMP_FILE.delete();
            }
        }

        return mLocalFile;
    }

    public interface ProgressListener {
        void onDownload(Result<File> sender, long completedSize);
    }
}
