package com.nasaq.ploxbot;
import android.Manifest;
import android.app.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;

public final class MainActivity extends Activity {
 static final int REQUEST_CAPTURE=414;
 TextView status; Switch auto;
 int bg=0xff0a101c,panel=0xff152033;
 public void onCreate(Bundle b){super.onCreate(b);createUi();}
 public void onResume(){super.onResume();update();}
 int dp(float f){return (int)(f*getResources().getDisplayMetrics().density+.5f);}
 TextView text(String s,int size,int color){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setGravity(Gravity.RIGHT);return t;}
 GradientDrawable round(int c){GradientDrawable d=new GradientDrawable();d.setColor(c);d.setCornerRadius(dp(14));return d;}
 void add(LinearLayout l,View v,int top){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.topMargin=dp(top);l.addView(v,p);}
 Button button(String s,int col){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setTextSize(16);b.setTextColor(-1);b.setBackground(round(col));return b;}
 void createUi(){
  ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setBackgroundColor(bg);
  LinearLayout root=new LinearLayout(this);root.setOrientation(1);root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
  root.setPadding(dp(22),dp(35),dp(22),dp(25));scroll.addView(root);setContentView(scroll);
  add(root,text("PLOX BOT",32,0xff75f5cc),0);
  add(root,text("محرك بلوكس محلي • بدون ChatGPT أو إنترنت",15,0xffb1c4d2),5);
  TextView lead=text("يقرأ اللوحة ويحسب أفضل عمود وترتيب، مع تقييم السلاسل وخطر امتلاء اللوحة.\nالتشغيل التلقائي تجريبي ومخصص للاختبارات؛ لا يمكن ضمان الفوز.",15,-1);
  lead.setPadding(dp(15),dp(17),dp(15),dp(17));lead.setBackground(round(panel));add(root,lead,22);
  status=text("الحالة: غير نشط",14,0xffa9d9ef);add(root,status,22);
  Button overlay=button("1 - السماح بالنافذة العائمة",0xff354b65);add(root,overlay,16);
  overlay.setOnClickListener(v->startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:"+getPackageName()))));
  Button access=button("2 - تفعيل خدمة إمكانية الوصول (اختياري)",0xff354b65);add(root,access,13);
  access.setOnClickListener(v->startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
  Button start=button("3 - تشغيل قراءة الشاشة",0xff137e6a);add(root,start,13);
  start.setOnClickListener(v->{MediaProjectionManager m=(MediaProjectionManager)getSystemService(MEDIA_PROJECTION_SERVICE);startActivityForResult(m.createScreenCaptureIntent(),REQUEST_CAPTURE);});
  auto=new Switch(this);auto.setText("تفعيل التحكم التجريبي في بيئة التدريب");auto.setTextColor(-1);auto.setTextSize(15);add(root,auto,18);
  auto.setOnCheckedChangeListener((v,on)->{
   if(!on){CaptureService.autoMode=false;update();return;}
   auto.setChecked(false);
   if(!CaptureService.running||!GestureControl.enabled()){alert("شغّل الشاشة وخدمة إمكانية الوصول أولًا.");return;}
   new AlertDialog.Builder(this).setTitle("تحكم تجريبي")
    .setMessage("قد تفشل قراءة الشاشة أو حركات اللمس. لا تفعّله في مباريات تنافسية. هل تريد تشغيله لاختباره في بيئة تدريبية؟")
    .setNegativeButton("إلغاء",null).setPositiveButton("موافق",(dlg,w)->{auto.setOnCheckedChangeListener(null);auto.setChecked(true);CaptureService.autoMode=true;update();auto.setOnCheckedChangeListener((x,y)->{CaptureService.autoMode=y;update();});}).show();
  });
  Button stop=button("إيقاف كل شيء",0xffaa394c);add(root,stop,14);
  stop.setOnClickListener(v->{CaptureService.autoMode=false;stopService(new Intent(this,CaptureService.class));auto.setChecked(false);update();});
  add(root,text("التعرف مضبوط بصورة أولية لتسجيل 720×1604. إذا تغيرت واجهة اللعبة أو أبعاد الشاشة يلزم إعادة معايرة. لا توجد صلاحية شبكة.",13,0xffa1b5c6),24);
  if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=getPackageManager().PERMISSION_GRANTED)
   requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},415);
 }
 public void onActivityResult(int req,int code,Intent data){super.onActivityResult(req,code,data);
  if(req==REQUEST_CAPTURE&&code==RESULT_OK&&data!=null){Intent i=new Intent(this,CaptureService.class).setAction(CaptureService.START);
  i.putExtra(CaptureService.EXTRA_RESULT,code);i.putExtra(CaptureService.EXTRA_DATA,data);
  if(Build.VERSION.SDK_INT>=26)startForegroundService(i);else startService(i);status.setText("بدأ التقاط الشاشة، افتح اللعبة");}
 }
 void alert(String s){new AlertDialog.Builder(this).setMessage(s).setPositiveButton("حسنًا",null).show();}
 void update(){if(status!=null)status.setText("التحليل: "+(CaptureService.running?"يعمل":"متوقف")+
 " | إمكانية الوصول: "+(GestureControl.enabled()?"مفعلة":"غير مفعلة")+
 " | تحكم تجريبي: "+(CaptureService.autoMode?"نعم":"لا"));}
}