package com.example.videoeditor;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
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

    public void copyFile(File source, File dest)
    {
        if(!source.exists())
        {
            Log.e("FileCopy","Source file does not exist: "+source.getAbsoluteFile());
            return;
        }

        try(InputStream in = new FileInputStream(source);
        OutputStream out = new FileOutputStream(dest))
        {
            byte[] buffer = new byte[1024];
            int length;

            while((length = in.read(buffer)) > 0)
            {
                out.write(buffer,0,length);
            }

            Log.d("FileCopy", "File copied successfully: " + dest.getAbsolutePath());
        }
        catch (IOException e)
        {
            Log.e("FileCopy", "Error copying file: " + e.getMessage());
        }
    }

    public void saveFile(Context context,File cachedVideoFile,File outputFile)
    {
        try {
            FileInputStream fis = new FileInputStream(cachedVideoFile);
            FileOutputStream fos = new FileOutputStream(outputFile);

            byte[] buffer = new byte[1024];
            int length;

            while((length = fis.read(buffer)) > 0)
            {
                fos.write(buffer, 0, length);
            }

            Toast.makeText(context, "Saved in Movies Folder", Toast.LENGTH_SHORT).show();
            fis.close();
            fos.close();

        }catch (IOException e)
        {
            e.printStackTrace();
            Toast.makeText(context, "Error in Saving", Toast.LENGTH_SHORT).show();
        }
    }


}
