package com.nasaq.ploxbot;
import android.media.Image;
import java.nio.ByteBuffer;
public final class PloxVision {
 public static final class Snapshot{
  public final int[][] board;public final int[] active,next;public final int activeColumn;public final float confidence;public final String problem;
  Snapshot(int[][] b,int[] a,int[] n,int col,float conf,String err){board=b;active=a;next=n;activeColumn=col;confidence=conf;problem=err;}
  boolean ready(){return confidence>=.85f&&active!=null&&activeColumn>=0;}
 }
 static final float W=720f,H=1604f,LEFT=192f,TOP=259f,TILE=56f;
 public static Snapshot read(Image frame){
  int width=frame.getWidth(),height=frame.getHeight();int[][] b=new int[13][6];
  if(width<400||height<800||Math.abs(width/(float)height-W/H)>.045f)return new Snapshot(b,null,null,-1,0,"Screen ratio unsupported");
  Image.Plane plane=frame.getPlanes()[0];ByteBuffer data=plane.getBuffer();
  int stride=plane.getRowStride(),step=plane.getPixelStride();
  if(step<4)return new Snapshot(b,null,null,-1,0,"Unsupported pixel layout");
  float sx=width/W,sy=height/H;
  for(int row=0;row<13;row++)for(int col=0;col<6;col++)
   b[row][col]=sample(data,stride,step,width,height,(LEFT+TILE*(col+.5f))*sx,(TOP+TILE*(12-row+.5f))*sy);
  int[] active=null;int activeCol=-1,count=0;
  for(int col=0;col<6;col++){
   float x=(LEFT+TILE*(col+.5f))*sx;
   int a=sample(data,stride,step,width,height,x,(TOP-TILE*.5f)*sy);
   int c=sample(data,stride,step,width,height,x,(TOP+TILE*.5f)*sy);
   int d=sample(data,stride,step,width,height,x,(TOP+TILE*1.5f)*sy);
   if(a>0&&c>0&&d>0){active=new int[]{a,c,d};activeCol=col;count++;}
  }
  if(count!=1)return new Snapshot(b,null,null,-1,0,"Waiting for new triplet");
  b[12][activeCol]=0;b[11][activeCol]=0;
  int[] next={sample(data,stride,step,width,height,172*sx,389*sy),sample(data,stride,step,width,height,172*sx,405*sy),sample(data,stride,step,width,height,172*sx,421*sy)};
  if(next[0]==0||next[1]==0||next[2]==0)next=null;
  return new Snapshot(b,active,next,activeCol,.94f,null);
 }
 static int sample(ByteBuffer buf,int stride,int step,int w,int h,float fx,float fy){
  int x=Math.max(0,Math.min(w-1,Math.round(fx))),y=Math.max(0,Math.min(h-1,Math.round(fy)));
  int[] votes=new int[6];int n=0;int size=Math.max(2,Math.round(w/240f));
  for(int dy=-size;dy<=size;dy+=size)for(int dx=-size;dx<=size;dx+=size){
   int xx=Math.max(0,Math.min(w-1,x+dx)),yy=Math.max(0,Math.min(h-1,y+dy)),p=yy*stride+xx*step;
   if(p+2>=buf.limit())continue;
   votes[classify(buf.get(p)&255,buf.get(p+1)&255,buf.get(p+2)&255)]++;n++;
  }
  int color=0,m=0;for(int c=1;c<=5;c++)if(votes[c]>m){m=votes[c];color=c;}
  return m>=Math.max(4,n*.6f)?color:0;
 }
 public static int classify(int r,int g,int b){
  int max=Math.max(r,Math.max(g,b)),min=Math.min(r,Math.min(g,b)),diff=max-min;
  if(max<170||diff<80||diff/(float)Math.max(1,max)<.48f)return 0;
  float hue;if(max==r)hue=(g-b)/(float)diff;else if(max==g)hue=2+(b-r)/(float)diff;else hue=4+(r-g)/(float)diff;
  hue*=60;if(hue<0)hue+=360;
  if(hue>=345||hue<13)return 1;
  if(hue>=38&&hue<=69)return 2;
  if(hue>=98&&hue<=153)return 3;
  if(hue>=175&&hue<=215)return 4;
  if(hue>=286&&hue<=333)return 5;
  return 0;
 }
}