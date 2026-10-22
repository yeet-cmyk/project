package com.nasaq.ploxbot;
import android.accessibilityservice.*;
import android.graphics.Path;
import android.os.*;
import android.view.accessibility.AccessibilityEvent;
/** Gestures only with explicit Android Accessibility consent. */
public final class GestureControl extends AccessibilityService {
 static volatile GestureControl instance;
 final Handler handler=new Handler(Looper.getMainLooper());
 public void onAccessibilityEvent(AccessibilityEvent event){}
 public void onInterrupt(){}
 protected void onServiceConnected(){super.onServiceConnected();instance=this;}
 public void onDestroy(){if(instance==this)instance=null;super.onDestroy();}
 static boolean enabled(){return instance!=null;}
 static float bx(int col){return 192+56*(col+.5f);}
 public static void execute(int originCol,PloxEngine.Decision d,int w,int h){
  GestureControl service=instance;if(service==null||d==null)return;
  float sx=w/720f,sy=h/1604f;
  for(int i=0;i<d.rotations;i++){
   int delay=75+i*155;
   service.handler.postDelayed(()->{if(instance==service&&CaptureService.running&&CaptureService.autoMode)
    service.gesture(360*sx,530*sy,360*sx,530*sy,55);},delay);
  }
  long delay=75+d.rotations*155L;
  service.handler.postDelayed(()->{if(instance!=service||!CaptureService.running||!CaptureService.autoMode)return;
   float x0=bx(originCol)*sx,x1=bx(d.column)*sx;
   if(Math.abs(x0-x1)>5)service.gesture(x0,490*sy,x1,490*sy,175);
  },delay);
  service.handler.postDelayed(()->{if(instance!=service||!CaptureService.running||!CaptureService.autoMode)return;
   float x=bx(d.column)*sx;service.gesture(x,490*sy,x,915*sy,170);
  },delay+290);
 }
 void gesture(float x0,float y0,float x1,float y1,long ms){
  Path p=new Path();p.moveTo(x0,y0);if(Math.abs(x0-x1)>.5f||Math.abs(y0-y1)>.5f)p.lineTo(x1,y1);
  GestureDescription g=new GestureDescription.Builder().addStroke(new GestureDescription.StrokeDescription(p,0,ms)).build();
  dispatchGesture(g,null,null);
 }
}