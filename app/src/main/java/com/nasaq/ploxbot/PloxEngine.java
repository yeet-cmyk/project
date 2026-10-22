package com.nasaq.ploxbot;
public final class PloxEngine {
 public static final int WIDTH=6, HEIGHT=13;
 public static final class Decision {
  public final int column,rotations,removed,chains; public final double score;
  Decision(int c,int r,int n,int h,double s){column=c;rotations=r;removed=n;chains=h;score=s;}
 }
 static final class Outcome{
  int[][] board; int removed,chains;
  Outcome(int[][] b,int n,int c){board=b;removed=n;chains=c;}
 }
 static final int[][] DIR={{1,0},{0,1},{1,1},{1,-1}};
 public static int[][] copy(int[][] b){int[][] c=new int[13][6];for(int r=0;r<13;r++)System.arraycopy(b[r],0,c[r],0,6);return c;}
 static boolean valid(int[][] b,int col){int h=0;while(h<13&&b[h][col]!=0)h++;return h+3<=13;}
 public static Outcome simulate(int[][] board,int[] top,int col,int rotations){
  if(board==null||top==null||top.length!=3||col<0||col>=6||!valid(board,col))return null;
  int[][] b=copy(board);int h=0;while(h<13&&b[h][col]!=0)h++;
  for(int i=0;i<3;i++){int k=top[(2-i+rotations)%3];if(k<1||k>5)return null;b[h+i][col]=k;}
  int removed=0,chains=0;
  while(chains<24){
   boolean[][] mark=new boolean[13][6];
   for(int y=0;y<13;y++)for(int x=0;x<6;x++){
    int color=b[y][x];if(color==0)continue;
    for(int[] d:DIR){
     int py=y-d[0],px=x-d[1];
     if(py>=0&&py<13&&px>=0&&px<6&&b[py][px]==color)continue;
     int len=0,ny=y,nx=x;
     while(ny>=0&&ny<13&&nx>=0&&nx<6&&b[ny][nx]==color){len++;ny+=d[0];nx+=d[1];}
     if(len>=3){ny=y;nx=x;for(int j=0;j<len;j++){mark[ny][nx]=true;ny+=d[0];nx+=d[1];}}
    }
   }
   int count=0;for(int y=0;y<13;y++)for(int x=0;x<6;x++)if(mark[y][x]){b[y][x]=0;count++;}
   if(count==0)break;removed+=count;chains++;
   for(int x=0;x<6;x++){int w=0;for(int y=0;y<13;y++)if(b[y][x]!=0)b[w++][x]=b[y][x];while(w<13)b[w++][x]=0;}
  }
  return new Outcome(b,removed,chains);
 }
 static double assess(int[][] b,int n,int chains){
  double score=n*90+Math.max(0,chains-1)*250;
  int max=0,cells=0,legal=0;double sq=0;
  for(int x=0;x<6;x++){int h=0;while(h<13&&b[h][x]!=0)h++;max=Math.max(max,h);cells+=h;sq+=h*h;
   if(h>=10)score-=(h-9)*(h-9)*115;
   if(valid(b,x))legal++;
  }
  score-=sq*1.8+Math.pow(max,3)*.65+cells*3;
  if(max>=12)score-=900;if(max>=13)score-=3000;
  if(legal==0)score-=20000;else if(legal<3)score-=(3-legal)*500;
  for(int y=0;y<13;y++)for(int x=0;x<6;x++){
   int color=b[y][x];if(color==0)continue;
   if(x+1<6&&b[y][x+1]==color)score+=7.5;
   if(y+1<13&&b[y+1][x]==color)score+=7.5;
   if(y+1<13&&x+1<6&&b[y+1][x+1]==color)score+=7.5;
   if(y+1<13&&x>0&&b[y+1][x-1]==color)score+=7.5;
  }
  return score;
 }
 public static Decision decide(int[][] board,int[] current,int[] next){
  if(board==null||board.length!=13||current==null||current.length!=3)return null;
  for(int[] row:board)if(row==null||row.length!=6)return null;
  Decision best=null;double bs=Double.NEGATIVE_INFINITY;
  for(int col=0;col<6;col++)for(int rot=0;rot<3;rot++){
   Outcome one=simulate(board,current,col,rot);if(one==null)continue;
   double val=assess(one.board,one.removed,one.chains);
   if(next!=null&&next.length==3&&next[0]>0&&next[1]>0&&next[2]>0){
    double follow=Double.NEGATIVE_INFINITY;
    for(int col2=0;col2<6;col2++)for(int rot2=0;rot2<3;rot2++){
     Outcome two=simulate(one.board,next,col2,rot2);if(two!=null)follow=Math.max(follow,assess(two.board,two.removed,two.chains));
    }
    val=.68*val+.32*follow;
   }
   val-=Math.abs(col-2.5)*.025;
   if(val>bs){bs=val;best=new Decision(col,rot,one.removed,one.chains,val);}
  }
  return best;
 }
}