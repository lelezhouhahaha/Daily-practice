/**
 * Created by Thir on 2018/3/21.
 */
package com.meigsmart.meigrs32.model;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLSurfaceView;
import android.util.Log;


import com.meigsmart.meigrs32.R;
import com.threed.jpct.Camera;
import com.threed.jpct.FrameBuffer;
import com.threed.jpct.Light;
import com.threed.jpct.Object3D;
import com.threed.jpct.Primitives;
import com.threed.jpct.RGBColor;
import com.threed.jpct.SimpleVector;
import com.threed.jpct.Texture;
import com.threed.jpct.TextureInfo;
import com.threed.jpct.TextureManager;
import com.threed.jpct.World;
import com.threed.jpct.util.BitmapHelper;
import com.threed.jpct.util.MemoryHelper;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;


public class ThirRenderer implements GLSurfaceView.Renderer {

    private FrameBuffer m_frameBuffer;
    private World       m_world;
    private Object3D    m_cube;
    private Light       m_light;
    private Camera      m_cam;
    private int         m_cam_pos = 0;
    private int         m_cam_move_dir = 0;
    private int         m_fps;
    private long        m_time;
    private static final String TAG = "ThirRenderer";
    private Resources   m_resources;

    public ThirRenderer( Resources res ) {
        m_resources = res;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        Log.d(TAG,"onSurfaceCreated");
        gl.glEnable(GL10.GL_BLEND);
        gl.glBlendFunc(GL10.GL_ONE, GL10.GL_ONE_MINUS_SRC_ALPHA);

        Bitmap bmp1 = BitmapFactory.decodeResource(m_resources, R.mipmap.textture_1);
        Texture texture1 = new Texture(bmp1);
        TextureManager.getInstance().addTexture("texture", texture1);

        m_cube = Primitives.getPyramide(32);
        m_cube.calcTextureWrapSpherical();
        m_cube.setTexture("texture");
        m_cube.strip();
        m_cube.build();

        m_world = new World();
        m_world.setAmbientLight(192, 192, 192);
        m_world.addObject(m_cube);
        m_world.buildAllObjects();

        m_light = new Light(m_world);
        m_light.setIntensity(255, 255, 255);
        SimpleVector sv = new SimpleVector();
        sv.set(m_cube.getTransformedCenter());
        sv.y -= 32;
        sv.z -= 16;
        m_light.setPosition(sv);

        Camera cam = m_world.getCamera();
        cam.align(m_cube);
        cam.moveCamera(Camera.CAMERA_MOVEOUT, 128);
        cam.lookAt(m_cube.getCenter());
        m_cam = cam;

        MemoryHelper.compact();
    }

    public void destroy() {
        TextureManager.getInstance().removeTexture("texture");
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        Log.d(TAG,"onSurfaceChanged");
        if( m_frameBuffer != null ) {
            m_frameBuffer = null;
        }
        m_frameBuffer = new FrameBuffer(gl, width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        try {
            if (true) {
                long now = System.currentTimeMillis();

                final int CAM_MOVE_RANGE = 512;
                final float CAM_MOVE_SPEED = 0.25f;
                m_cube.rotateY(0.01f);
                if( m_cam_move_dir == 0 ) {
                    m_cam_pos++;
                    if(m_cam_pos > CAM_MOVE_RANGE) {
                        m_cam_move_dir = 1;
                    }
                    m_cam.moveCamera(Camera.CAMERA_MOVEUP, CAM_MOVE_SPEED);
                    m_cam.lookAt(m_cube.getCenter());
                } else {
                    m_cam_pos--;
                    if(m_cam_pos + CAM_MOVE_RANGE < 0) {
                        m_cam_move_dir = 0;
                    }
                    m_cam.moveCamera(Camera.CAMERA_MOVEDOWN, CAM_MOVE_SPEED);
                    m_cam.lookAt(m_cube.getCenter());
                }


                m_frameBuffer.clear(new RGBColor(64,64,64));
                m_world.renderScene(m_frameBuffer);
                m_world.draw(m_frameBuffer);
                m_frameBuffer.display();
                m_fps+=1;

                if (now - m_time > 1000) {
                    Log.v(TAG, "fps:"+m_fps);
                    m_fps = 0;
                    m_time = System.currentTimeMillis();
                }
            } else {
                if (m_frameBuffer != null) {
                    m_frameBuffer.dispose();
                    m_frameBuffer = null;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.d(TAG,"Drawing thread terminated!");
        }
    }
}
