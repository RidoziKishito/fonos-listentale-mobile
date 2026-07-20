package com.example.fonoss.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import android.content.res.AssetFileDescriptor;

public class ImageModerationHelper {
    private Interpreter tflite;

    public ImageModerationHelper(Context context) {
        try {
            MappedByteBuffer tfliteModel = loadModelFile(context, "nsfw.tflite");
            Interpreter.Options options = new Interpreter.Options();
            options.setNumThreads(2);
            tflite = new Interpreter(tfliteModel, options);
        } catch (Exception e) {
            e.printStackTrace();
            tflite = null;
        }
    }

    private MappedByteBuffer loadModelFile(Context context, String modelPath) throws IOException {
        AssetFileDescriptor fileDescriptor = context.getAssets().openFd(modelPath);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    /**
     * Checks if the image is appropriate (not NSFW).
     * @param bitmap the image to check
     * @return true if safe, false if NSFW
     */
    public boolean isImageAppropriate(Bitmap bitmap) {
        if (tflite == null) return true; // Fallback to allow if model fails to load

        try {
            int INPUT_WIDTH = 224;
            int INPUT_HEIGHT = 224;
            float[] VGG_MEAN = {103.939f, 116.779f, 123.68f};

            Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, 256, 256, true);

            ByteBuffer inputBuffer = ByteBuffer.allocateDirect(1 * INPUT_WIDTH * INPUT_HEIGHT * 3 * 4);
            inputBuffer.order(ByteOrder.LITTLE_ENDIAN);
            
            int[] intValues = new int[INPUT_WIDTH * INPUT_HEIGHT];
            int startX = Math.max((scaledBitmap.getWidth() - INPUT_WIDTH) / 2, 0);
            int startY = Math.max((scaledBitmap.getHeight() - INPUT_HEIGHT) / 2, 0);
            
            scaledBitmap.getPixels(intValues, 0, INPUT_WIDTH, startX, startY, INPUT_WIDTH, INPUT_HEIGHT);
            
            for (int color : intValues) {
                inputBuffer.putFloat((color & 0xFF) - VGG_MEAN[0]); // Blue
                inputBuffer.putFloat(((color >> 8) & 0xFF) - VGG_MEAN[1]); // Green
                inputBuffer.putFloat(((color >> 16) & 0xFF) - VGG_MEAN[2]); // Red
            }

            // Output is usually [1][2] for open_nsfw
            float[][] outputBuffer = new float[1][2];

            tflite.run(inputBuffer, outputBuffer);

            float[] results = outputBuffer[0];
            
            // For open_nsfw: index 0 is SFW, index 1 is NSFW
            if (results.length >= 2) {
                float nsfwScore = results[1];
                if (nsfwScore > 0.7f) {
                    return false;
                }
            }
            
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            // Fallback just returns true to not crash
            try {
                return true;
            } catch (Exception ex) {
                ex.printStackTrace();
                return true; 
            }
        }
    }
    
    public void close() {
        if (tflite != null) {
            tflite.close();
            tflite = null;
        }
    }
}
