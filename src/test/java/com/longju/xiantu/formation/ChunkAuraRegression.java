package com.longju.xiantu.formation;

public final class ChunkAuraRegression {
    private static int checks;
    private static void check(boolean pass, String name) { checks++; if (!pass) throw new AssertionError(name); }
    public static void main(String[] args) {
        check(ChunkAuraRules.rescale(600,1200,3600)==1800,"capacity adjustment preserves remaining ratio");
        check(ChunkAuraRules.rescale(0,1200,3600)==0,"capacity increase cannot refill an empty chunk");
        check(ChunkAuraRules.rescale(1800,3600,1200)==600,"round trip preserves pool ratio");
        double currentStock=3600;
        for(int second=0;second<900;second++)currentStock=ChunkAuraRules.allocate(currentStock,3600,100,4).remaining();
        check(currentStock==0,"new default supports fifteen active minutes before depletion");
        check(ChunkAuraRules.chunk(0)==0 && ChunkAuraRules.chunk(15)==0 && ChunkAuraRules.chunk(16)==1,"16 block boundaries");
        check(ChunkAuraRules.chunk(-1)==-1 && ChunkAuraRules.chunk(-16)==-1 && ChunkAuraRules.chunk(-17)==-2,"negative floor division");
        var full=ChunkAuraRules.allocate(1200,1200,100,4);
        check(full.supply()==1 && full.remaining()==1196 && full.concentration()==100,"full natural second");
        var shared=ChunkAuraRules.allocate(3,1200,100,8);
        check(shared.supply()==.375 && shared.remaining()==0,"two requests share the final three points");
        check(shared.supply()*4*2==3,"allocation conserves aura");
        var empty=ChunkAuraRules.allocate(0,1200,100,4);
        check(empty.supply()==0 && empty.remaining()==0,"empty means no progress");
        double stock=1200;
        for(int second=0;second<300;second++)stock=ChunkAuraRules.allocate(stock,1200,100,4).remaining();
        check(stock==0,"single cultivator exhausts ordinary chunk after 300 seconds");
        check(ChunkAuraRules.recover(0,1200,1,true)==0,"occupied empty chunk cannot trickle free cultivation");
        check(ChunkAuraRules.recover(0,1200,1,false)==1,"idle loaded recovery");
        check(ChunkAuraRules.recover(1199.5,1200,1,false)==1200,"recovery capacity clamp");
        for(int demand=1;demand<100;demand++) for(int available=0;available<100;available++) {
            var a=ChunkAuraRules.allocate(available,100,100,demand);
            check(Math.abs(a.remaining()+a.supply()*demand-available)<1e-9,"mass conservation");
        }
        System.out.println("Chunk aura regression PASS: "+checks);
    }
}
