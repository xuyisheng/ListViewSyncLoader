package com.imooc.listviewacyncloader;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.LruCache;
import android.widget.ImageView;
import android.widget.ListView;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.Set;

import libcore.io.DiskLruCache;


public class ImageLoaderWithDoubleCaches {

    private Set<ASyncDownloadImage> mTasks;
    private LruCache<String, Bitmap> mMemoryCaches;
    private DiskLruCache mDiskCaches;
    private ListView mListView;

    public ImageLoaderWithDoubleCaches(Context context, ListView listview) {
        this.mListView = listview;
        mTasks = new HashSet<>();
        int maxMemory = (int) Runtime.getRuntime().maxMemory();
        int cacheSize = maxMemory / 10;
        mMemoryCaches = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap value) {
                return value.getByteCount();
            }
        };

        File cacheDir = getFileCache(context, "disk_caches");
        if (!cacheDir.exists()) {
            cacheDir.mkdirs();
        }
        try {
            mDiskCaches = DiskLruCache.open(cacheDir, 1, 1, 10 * 1024 * 1024);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void showImage(String url, ImageView imageView) {
        Bitmap bitmap = getBitmapFromMemoryCaches(url);
        if (bitmap == null) {
            imageView.setImageResource(R.drawable.ic_launcher);
        } else {
            imageView.setImageBitmap(bitmap);
        }
    }

    public Bitmap getBitmapFromMemoryCaches(String url) {
        return mMemoryCaches.get(url);
    }

    public void addBitmapToMemoryCaches(String url, Bitmap bitmap) {
        if (getBitmapFromMemoryCaches(url) == null) {
            mMemoryCaches.put(url, bitmap);
        }
    }

    public void loadImages(int start, int end) {
        for (int i = start; i < end; i++) {
            String url = Images.IMAGE_URLS[i];
            Bitmap bitmap = getBitmapFromMemoryCaches(url);
            if (bitmap == null) {
                ASyncDownloadImage task = new ASyncDownloadImage(url);
                mTasks.add(task);
                task.execute(url);
            } else {
                ImageView imageView = (ImageView) mListView.findViewWithTag(url);
                imageView.setImageBitmap(bitmap);
            }
        }
    }

    private File getFileCache(Context context, String cacheFileName) {
        String cachePath;
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())
                || !Environment.isExternalStorageRemovable()) {
            cachePath = context.getExternalCacheDir().getPath();
        } else {
            cachePath = context.getCacheDir().getPath();
        }
        return new File(cachePath + File.separator + cacheFileName);
    }

    private static boolean getBitmapUrlToStream(String urlString, OutputStream outputStream) {
        HttpURLConnection urlConnection = null;
        BufferedOutputStream out = null;
        BufferedInputStream in = null;
        try {
            final URL url = new URL(urlString);
            urlConnection = (HttpURLConnection) url.openConnection();
            in = new BufferedInputStream(urlConnection.getInputStream(), 8 * 1024);
            out = new BufferedOutputStream(outputStream, 8 * 1024);
            int b;
            while ((b = in.read()) != -1) {
                out.write(b);
            }
            return true;
        } catch (final IOException e) {
            e.printStackTrace();
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            try {
                if (out != null) {
                    out.close();
                }
                if (in != null) {
                    in.close();
                }
            } catch (final IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public void cancelAllTasks() {
        if (mTasks != null) {
            for (ASyncDownloadImage task : mTasks) {
                task.cancel(false);
            }
        }
    }

    public String toMD5String(String key) {
        String cacheKey;
        try {
            final MessageDigest digest = MessageDigest.getInstance("MD5");
            digest.update(key.getBytes());
            cacheKey = bytesToHexString(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            cacheKey = String.valueOf(key.hashCode());
        }
        return cacheKey;
    }

    private String bytesToHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            String hex = Integer.toHexString(0xFF & bytes[i]);
            if (hex.length() == 1) {
                sb.append('0');
            }
            sb.append(hex);
        }
        return sb.toString();
    }

    public void flushCache() {
        if (mDiskCaches != null) {
            try {
                mDiskCaches.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    class ASyncDownloadImage extends AsyncTask<String, Void, Bitmap> {

        private String url;

        public ASyncDownloadImage(String url) {
            this.url = url;
        }

        @Override
        protected Bitmap doInBackground(String... params) {
            url = params[0];
            FileDescriptor fileDescriptor = null;
            FileInputStream fileInputStream = null;
            DiskLruCache.Snapshot snapShot = null;
            String key = toMD5String(url);
            try {
                snapShot = mDiskCaches.get(key);
                if (snapShot == null) {
                    DiskLruCache.Editor editor = mDiskCaches.edit(key);
                    if (editor != null) {
                        OutputStream outputStream = editor.newOutputStream(0);
                        if (getBitmapUrlToStream(url, outputStream)) {
                            editor.commit();
                        } else {
                            editor.abort();
                        }
                    }
                    snapShot = mDiskCaches.get(key);
                }
                if (snapShot != null) {
                    fileInputStream = (FileInputStream) snapShot.getInputStream(0);
                    fileDescriptor = fileInputStream.getFD();
                }
                Bitmap bitmap = null;
                if (fileDescriptor != null) {
                    bitmap = BitmapFactory.decodeFileDescriptor(fileDescriptor);
                }
                if (bitmap != null) {
                    addBitmapToMemoryCaches(params[0], bitmap);
                }
                return bitmap;
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (fileDescriptor == null && fileInputStream != null) {
                    try {
                        fileInputStream.close();
                    } catch (IOException e) {
                    }
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            super.onPostExecute(bitmap);
            ImageView imageView = (ImageView) mListView.findViewWithTag(url);
            if (imageView != null && bitmap != null) {
                imageView.setImageBitmap(bitmap);
            }
            mTasks.remove(this);
        }
    }
}
