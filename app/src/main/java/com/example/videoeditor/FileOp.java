package com.example.videoeditor;

import android.content.Context;
import android.net.Uri;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class FileOp {

    public static void copyUriToFile(Context context, Uri uri, File dest) throws IOException
    {
        try(InputStream inputStream = context.getContentResolver().openInputStream(uri);
            OutputStream outputStream = new FileOutputStream(dest))
        {
            if(inputStream == null)
            {
                throw new IOException("Failed to Open Input Stream");
            }

            byte[] buffer = new byte[1024];
            int length;

            while((length = inputStream.read(buffer)) > 0)
            {
                outputStream.write(buffer,0,length);
            }
        }
    }
}
