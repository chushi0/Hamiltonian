package org.cszt0.hamiltonian;

import android.opengl.GLSurfaceView;
import android.view.MotionEvent;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class NativeRenderer implements GLSurfaceView.Renderer {

    private long nativePointer;

    private int width, height;

    static {
        System.loadLibrary("renderer");
    }

    NativeRenderer() {
        nativePointer = nativeAlloc();
    }

    @Override
    protected void finalize() throws Throwable {
        nativeFree(nativePointer);
        super.finalize();
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        nativeSurfaceCreated(nativePointer);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        this.width = width;
        this.height = height;
        nativeSurfaceChange(nativePointer, width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        nativeDrawFrame(nativePointer);
    }

    private static native long nativeAlloc();

    private static native void nativeFree(long nativePointer);

    private static native void nativeSurfaceCreated(long nativePointer);

    private static native void nativeSurfaceChange(long nativePointer, int width, int height);

    private static native void nativeDrawFrame(long nativePointer);

    private static native void addParticle(long nativePointer, float x, float y);

    public void dispatchTouchEvent(MotionEvent motionEvent) {
        int pointerCount = motionEvent.getPointerCount();
        MotionEvent.PointerCoords pointerCoords = new MotionEvent.PointerCoords();
        for (int i = 0; i < pointerCount; i++) {
            motionEvent.getPointerCoords(i, pointerCoords);
            addParticle(nativePointer, pointerCoords.x * 2 / width - 1, 1 - pointerCoords.y * 2 / height);
        }
    }
}
