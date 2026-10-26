package com.longju.xiantu.spell;

/** Tick-based press edges: holding a key never counts as two taps. */
public final class SwordFlightControls {
 private int lastForward=-100,lastSneak=-100;
 private boolean wasForward,wasSneak,boost;
 public record Action(boolean boost,boolean dismount){}
 public Action tick(int tick,boolean forward,boolean sneak,boolean sprint,boolean open){
  if(open){reset();return new Action(false,false);}
  boolean leave=false;
  if(forward&&!wasForward){if(tick-lastForward<=7)boost=true;lastForward=tick;}
  if(sneak&&!wasSneak){leave=tick-lastSneak<=7;lastSneak=tick;}
  if(!forward)boost=false;
  wasForward=forward;wasSneak=sneak;return new Action(sprint||boost,leave);
 }
 public void reset(){lastForward=lastSneak=-100;wasForward=wasSneak=boost=false;}
}
