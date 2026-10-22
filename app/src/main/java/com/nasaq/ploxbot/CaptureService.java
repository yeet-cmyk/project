package com.nasaq.ploxbot;
import android.app.*;
import android.content.*;
import android.graphics.PixelFormat;
import android.hardware.display.*;
import android.media.*;
import android.media.projection.*;
import android.os.*;
import android.util.DisplayMetrics;
import android.view.WindowManager;

/** Foreground capture engine: no network, no cloud. */
public final class CaptureService extends Service {
 public static final String START="com.nasaq.ploxbot.START", STOP="com.nasaq.ploxbot.STOP";
 public static final String EXTRA_RESULT="capture_result",EXTRA_DATA="capture_intent";
 public static volatile boolean running=false,autoMode=false;
 static final String CHANNEL="plox_scan";
 final Handler main=new Handler(Looper.getMainLooper());
 HandlerThread thread;Handler worker;MediaProjection projection;VirtualDisplay display;ImageReader reader;PloxOverlay overlay;
 int screenW,screenH;long lastFrame=0,lastAction=0;int stable=0;String seen="";boolean armed=true;
 public void onCreate(){super.onCreate();
  NotificationManager nm=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
  if(Build.VERSION.SDK_INT>=26)nm.createNotificationChannel(new NotificationChannel(CHANNEL,"Plox Bot scan",NotificationManager.IMPORTANCE_LOW));
 }
 public int onStartCommand(Intent intent,int flags,int id){
  if(intent==null)return START_NOT_STICKY;
  if(STOP.equals(intent.getAction())){autoMode=false;stopSelf();return START_NOT_STICKY;}
  if(START.equals(intent.getAction())){
   if(running)return START_NOT_STICKY;
   int result=intent.getIntExtra(EXTRA_RESULT,0);
   Intent data=Build.VERSION.SDK_INT>=33?intent.getParcelableExtra(EXTRA_DATA,Intent.class):intent.getParcelableExtra(EXTRA_DATA);
   if(data==null){stopSelf();return START_NOT_STICKY;}
   try{
    if(Build.VERSION.SDK_INT>=29)startForeground(1,notice(),android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION);
    else startForeground(1,notice());
    begin(result,data);
   }catch(Exception e){
    autoMode=false;running=false;android.util.Log.e("PloxBot","Screen capture failed",e);stopSelf();
   }
  }
  return START_NOT_STICKY;
 }
 Notification notice(){
  Intent stop=new Intent(this,CaptureService.class).setAction(STOP);
  PendingIntent pi=PendingIntent.getService(this,88,stop,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
  Notification.Builder b=new Notification.Builder(this,CHANNEL);
  return b.setSmallIcon(android.R.drawable.ic_menu_view).setContentTitle("Plox Bot - analyzing")
   .setContentText("Local screen reader - tap STOP to end").setOngoing(true)
   .addAction(android.R.drawable.ic_menu_close_clear_cancel,"STOP",pi).build();
 }
 void begin(int code,Intent data){
  DisplayMetrics dm=new DisplayMetrics();
  ((WindowManager)getSystemService(WINDOW_SERVICE)).getDefaultDisplay().getRealMetrics(dm);
  screenW=dm.widthPixels;screenH=dm.heightPixels;
  thread=new HandlerThread("plox-vision");thread.start();worker=new Handler(thread.getLooper());
  reader=ImageReader.newInstance(screenW,screenH,PixelFormat.RGBA_8888,2);
  MediaProjectionManager mgr=(MediaProjectionManager)getSystemService(MEDIA_PROJECTION_SERVICE);
  projection=mgr.getMediaProjection(code,data);
  if(projection==null)throw new IllegalStateException("No MediaProjection");
  projection.registerCallback(new MediaProjection.Callback(){public void onStop(){main.post(()->stopSelf());}},main);
  display=projection.createVirtualDisplay("Plox Screen",screenW,screenH,dm.densityDpi,
   DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,reader.getSurface(),null,worker);
  overlay=new PloxOverlay(this);overlay.show();running=true;autoMode=false;
  reader.setOnImageAvailableListener(r->{
   Image im=null;
   try{
    im=r.acquireLatestImage();if(im==null)return;
    long now=SystemClock.elapsedRealtime();
    if(now-lastFrame<160)return;
    lastFrame=now;inspect(PloxVision.read(im),now);
   }catch(Exception e){android.util.Log.w("PloxBot","Frame decode error",e);}
   finally{if(im!=null)im.close();}
  },worker);
 }
 void inspect(PloxVision.Snapshot frame,long now){
  if(!frame.ready()){
   stable=0;if(now-lastAction>750)armed=true;
   message(frame.problem==null?"Waiting for Plox board":frame.problem);return;
  }
  String key=frame.activeColumn+":"+frame.active[0]+frame.active[1]+frame.active[2];
  if(key.equals(seen))stable++;else{seen=key;stable=1;}
  if(stable<2){message("Reading next piece");return;}
  PloxEngine.Decision d=PloxEngine.decide(frame.board,frame.active,frame.next);
  if(d==null){message("No safe move");return;}
  message("Best column "+(d.column+1)+" | taps "+d.rotations+" | chains "+d.chains+
   (autoMode?" | AUTO TEST":" | ASSIST"));
  if(autoMode&&armed&&now-lastAction>1400&&GestureControl.enabled()){
   armed=false;lastAction=now;GestureControl.execute(frame.activeColumn,d,screenW,screenH);
  }
 }
 void message(String text){main.post(()->{if(overlay!=null)overlay.update("PLOX BOT - "+text);});}
 public void onDestroy(){
  running=false;autoMode=false;
  if(reader!=null){reader.setOnImageAvailableListener(null,null);reader.close();reader=null;}
  if(display!=null){display.release();display=null;}
  if(projection!=null){try{projection.stop();}catch(Exception ignored){}projection=null;}
  if(thread!=null){thread.quitSafely();thread=null;}
  if(overlay!=null){overlay.close();overlay=null;}
  super.onDestroy();
 }
 public IBinder onBind(Intent intent){return null;}
}