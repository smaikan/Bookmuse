import android.os.SystemClock;
import android.view.InputDevice;
import android.view.InputEvent;
import android.view.MotionEvent;
import java.lang.reflect.Method;

/** Shell-only emulator test driver. Never packaged with the reader or granted to it. */
public final class KitapTouch {
  private static Object manager;
  private static Method inject;
  private static long down;
  private static void event(int action, int count, float[] xy) throws Exception {
    MotionEvent.PointerProperties[] properties=new MotionEvent.PointerProperties[count];
    MotionEvent.PointerCoords[] coords=new MotionEvent.PointerCoords[count];
    for(int i=0;i<count;i++) {
      properties[i]=new MotionEvent.PointerProperties();properties[i].id=i;properties[i].toolType=MotionEvent.TOOL_TYPE_FINGER;
      coords[i]=new MotionEvent.PointerCoords();coords[i].x=xy[i*2];coords[i].y=xy[i*2+1];coords[i].pressure=1;coords[i].size=.05f;
    }
    MotionEvent e=MotionEvent.obtain(down,SystemClock.uptimeMillis(),action,count,properties,coords,0,0,1,1,0,0,InputDevice.SOURCE_TOUCHSCREEN,0);
    try { if(!Boolean.TRUE.equals(inject.invoke(manager,e,2))) throw new IllegalStateException("Touch injection rejected"); }
    finally {e.recycle();}
  }
  public static void main(String[] args) throws Exception {
    Class<?> type;
    try {type=Class.forName("android.hardware.input.InputManagerGlobal");}
    catch(ClassNotFoundException old){type=Class.forName("android.hardware.input.InputManager");}
    manager=type.getMethod("getInstance").invoke(null);inject=type.getMethod("injectInputEvent",InputEvent.class,int.class);
    if(args.length==3&&args[0].equals("double")) {
      float[] xy={Float.parseFloat(args[1]),Float.parseFloat(args[2]),0,0};
      for(int tap=0;tap<2;tap++){down=SystemClock.uptimeMillis();event(MotionEvent.ACTION_DOWN,1,xy);event(MotionEvent.ACTION_UP,1,xy);if(tap==0)SystemClock.sleep(100);}
      System.out.println("Injected double tap");return;
    }
    if(args.length>=6&&args[0].equals("drag")) {
      float x=Float.parseFloat(args[1]),y=Float.parseFloat(args[2]),endX=Float.parseFloat(args[3]),endY=Float.parseFloat(args[4]);
      int duration=Integer.parseInt(args[5]);if(duration<100||duration>5000)throw new IllegalArgumentException("Duration out of range");
      float[] xy={x,y,0,0};down=SystemClock.uptimeMillis();event(MotionEvent.ACTION_DOWN,1,xy);
      int steps=Math.max(12,duration/16);
      for(int step=1;step<=steps;step++){xy[0]=x+(endX-x)*step/steps;xy[1]=y+(endY-y)*step/steps;event(MotionEvent.ACTION_MOVE,1,xy);SystemClock.sleep(duration/steps);}
      System.out.println("Drag hold started");System.out.flush();
      if(args.length>6)SystemClock.sleep(Math.min(5000,Math.max(0,Integer.parseInt(args[6]))));
      event(args.length>7&&args[7].equals("cancel")?MotionEvent.ACTION_CANCEL:MotionEvent.ACTION_UP,1,xy);
      System.out.println("Injected complete single-touch stream");return;
    }
    if(args.length<9)throw new IllegalArgumentException("x1 y1 x2 y2 endX1 endY1 endX2 endY2 durationMs [cancel|remaining]");
    float[] from=new float[4],to=new float[4],xy=new float[4];
    for(int i=0;i<4;i++){from[i]=Float.parseFloat(args[i]);to[i]=Float.parseFloat(args[i+4]);xy[i]=from[i];}
    int duration=Integer.parseInt(args[8]);if(duration<100||duration>5000)throw new IllegalArgumentException("Duration out of range");
    down=SystemClock.uptimeMillis();event(MotionEvent.ACTION_DOWN,1,xy);SystemClock.sleep(30);
    event(MotionEvent.ACTION_POINTER_DOWN|(1<<MotionEvent.ACTION_POINTER_INDEX_SHIFT),2,xy);
    int steps=Math.max(12,duration/16);
    for(int step=1;step<=steps;step++){for(int i=0;i<4;i++)xy[i]=from[i]+(to[i]-from[i])*step/steps;event(MotionEvent.ACTION_MOVE,2,xy);SystemClock.sleep(duration/steps);}
    if(args.length>9&&args[9].equals("cancel")){event(MotionEvent.ACTION_CANCEL,2,xy);return;}
    event(MotionEvent.ACTION_POINTER_UP|(1<<MotionEvent.ACTION_POINTER_INDEX_SHIFT),2,xy);
    if(args.length>9&&args[9].equals("remaining")){xy[0]=Math.max(60,xy[0]-240);SystemClock.sleep(20);event(MotionEvent.ACTION_MOVE,1,xy);}
    event(MotionEvent.ACTION_UP,1,xy);
    System.out.println("Injected complete multi-touch stream");
  }
}
