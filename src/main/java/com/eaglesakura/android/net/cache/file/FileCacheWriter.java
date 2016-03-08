package com.eaglesakura.android.net.cache.file;

import com.eaglesakura.android.net.cache.ICacheWriter;
import com.eaglesakura.util.IOUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * ファイルに直接キャッシュを書き込む
 */
public class FileCacheWriter implements ICacheWriter {
    final File mDst;

    final File mSrc;

    final FileOutputStream mStream;

    public FileCacheWriter(File file) throws IOException {
        IOUtil.mkdirs(file.getParentFile());
        this.mDst = file;
        this.mSrc = new File(file.getAbsolutePath() + "." + System.currentTimeMillis());
        this.mStream = new FileOutputStream(mSrc);
    }

    @Override
    public void write(byte[] buffer, int offset, int length) throws IOException {
        mStream.write(buffer, offset, length);
    }

    /**
     * 正常に書き込めたら、一時ファイルを正式ファイルにリネームする
     */
    @Override
    public void commit() throws IOException {
        if (IOUtil.close(mStream)) {
            if (mDst.isFile()) {
                // 既にファイルがある場合は削除する
                mDst.delete();
            }
            // 書き込んだファイルをdstにスワップする
            mDst.renameTo(mSrc);

            if (mSrc.isFile()) {
                throw new IOException("Swap Failed :: " + mSrc.getAbsolutePath() + " -> " + mDst.getAbsolutePath());
            }
        } else {
            // closeに失敗したから、書き込みが行えなかった
            throw new IOException("File Close Filed :: " + mSrc.getAbsolutePath());
        }
    }

    /**
     * 廃棄するなら、tempファイルを削除する
     */
    @Override
    public void abort() throws IOException {
        IOUtil.close(mStream);
        mSrc.delete();
    }

    @Override
    public void close() throws IOException {

    }
}
