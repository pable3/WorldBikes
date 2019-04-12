package es.upv.pabgalm2.worldbikes.pojo;

import android.graphics.Bitmap;
import android.graphics.drawable.GradientDrawable;

public class StationBitmapHelper {

    private Bitmap bitmap;
    private GradientDrawable shape;
    private int color;

    public StationBitmapHelper(Bitmap bitmap, GradientDrawable shape, int color) {
        this.bitmap = bitmap;
        this.shape = shape;
        this.color = color;
    }

    public Bitmap getBitmap() {
        return bitmap;
    }

    public void setBitmap(Bitmap bitmap) {
        this.bitmap = bitmap;
    }

    public GradientDrawable getShape() {
        return shape;
    }

    public void setShape(GradientDrawable shape) {
        this.shape = shape;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }
}
