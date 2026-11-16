package dev.pixelspc;

import android.animation.*;
import android.app.*;
import android.content.Context;
import android.graphics.Rect;
import android.view.*;
import android.view.animation.AccelerateDecelerateInterpolator;

/** A focused menu window anchored exactly over the existing recessed header. */
final class HeaderMenuDialog extends Dialog {
    private final View anchor;
    private ValueAnimator motion;
    private int height;
    HeaderMenuDialog(Context context,View anchor){
        super(context);this.anchor=anchor;requestWindowFeature(Window.FEATURE_NO_TITLE);
        setCanceledOnTouchOutside(true);
        Window window=getWindow();window.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);window.setDimAmount(0);window.setWindowAnimations(0);
    }
    void expand(int target){
        cancelMotion();
        Window w=getWindow();int[] location=new int[2];anchor.getLocationOnScreen(location);
        Rect visible=new Rect();anchor.getWindowVisibleDisplayFrame(visible);
        WindowManager.LayoutParams params=w.getAttributes();params.gravity=Gravity.TOP|Gravity.LEFT;
        params.x=location[0];params.y=Math.max(0,location[1]-visible.top);params.width=anchor.getWidth()>0?anchor.getWidth():getContext().getResources().getDisplayMetrics().widthPixels-24;
        w.setAttributes(params);
        int start=height>0?height:Math.max(1,anchor.getHeight());
        if(start==target){setHeight(target);return;}
        animateHeight(start,target,false);
    }
    private void animateHeight(int start,int end,boolean close){
        if(!ValueAnimator.areAnimatorsEnabled()||!anchor.isAttachedToWindow()){setHeight(end);if(close)dismissNow();return;}
        motion=ValueAnimator.ofInt(start,end);motion.setDuration(close?220:300);motion.setInterpolator(new AccelerateDecelerateInterpolator());
        motion.addUpdateListener(a->setHeight((Integer)a.getAnimatedValue()));
        if(close)motion.addListener(new AnimatorListenerAdapter(){@Override public void onAnimationEnd(Animator a){dismissNow();}});
        setHeight(start);motion.start();
    }
    private void setHeight(int value){height=value;getWindow().setLayout(getWindow().getAttributes().width,value);}
    private void cancelMotion(){if(motion!=null){motion.removeAllListeners();motion.cancel();motion=null;}}
    @Override public void dismiss(){if(!isShowing())return;cancelMotion();animateHeight(height>0?height:anchor.getHeight(),Math.max(1,anchor.getHeight()),true);}
    void dismissNow(){cancelMotion();super.dismiss();height=0;}
}
