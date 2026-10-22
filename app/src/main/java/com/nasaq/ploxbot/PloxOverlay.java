package com.nasaq.ploxbot;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.TextView;
final class PloxOverlay{
 final Context context;WindowManager wm;TextView view;
 PloxOverlay(Context c){context=c;}
 void show(){
  if(view!=null||!Settings.canDrawOverlays(context))return;
  wm=(WindowManager)context.getSystemService(Context.WINDOW_SERVICE);
  view=new TextView(context);view.setText("PLOX BOT - reading");view.setTextColor(-1);view.setTextSize(13);
  view.setGravity(Gravity.CENTER);view.setPadding(14,9,14,9);
  GradientDrawable d=new GradientDrawable();d.setColor(0xe8123440);d.setCornerRadius(24);view.setBackground(d);
  WindowManager.LayoutParams p=new WindowManager.LayoutParams(-2,-2,Build.VERSION.SDK_INT>=26?
   WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY:WindowManager.LayoutParams.TYPE_PHONE,
   WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,PixelFormat.TRANSLUCENT);
  p.gravity=Gravity.TOP|Gravity.CENTER_HORIZONTAL;p.y=12;p.alpha=.78f;
  try{wm.addView(view,p);}catch(Exception e){view=null;}
 }
 void update(String message){if(view!=null)view.setText(message);}
 void close(){if(view!=null&&wm!=null){try{wm.removeView(view);}catch(Exception ignored){}view=null;}}
}