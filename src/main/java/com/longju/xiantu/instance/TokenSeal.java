package com.longju.xiantu.instance;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/** Server secret is persisted with the save, never copied to item NBT or a client packet. */
public final class TokenSeal {
    public static String sign(String secret,String dimension,String instance,long generation,String owner,String grant,int version){
        try{
            var mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(HexFormat.of().parseHex(secret),"HmacSHA256"));
            String message=String.join("\n","xiantu-cave-v1",dimension,instance,Long.toString(generation),owner,grant,Integer.toString(version));
            return HexFormat.of().formatHex(mac.doFinal(message.getBytes(StandardCharsets.UTF_8)));
        }catch(java.security.GeneralSecurityException e){throw new IllegalStateException(e);}
    }
    public static boolean matches(String expected,String actual){return actual!=null&&actual.length()==64&&MessageDigest.isEqual(expected.getBytes(StandardCharsets.US_ASCII),actual.getBytes(StandardCharsets.US_ASCII));}
    private TokenSeal(){}
}
