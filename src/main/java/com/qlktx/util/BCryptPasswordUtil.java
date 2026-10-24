package com.qlktx.util;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import org.mindrot.jbcrypt.BCrypt;
/** BCrypt compatible with TaiKhoan.MatKhauHash (varchar(100)). */
public final class BCryptPasswordUtil {
    private BCryptPasswordUtil(){}
    public static String randomPassword(){
        byte[] bytes=new byte[18];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
    public static boolean withinLimit(String password){
        return password!=null&&password.getBytes(StandardCharsets.UTF_8).length<=72;
    }
    public static String hash(String password){
        if(!withinLimit(password))throw new IllegalArgumentException("Password exceeds BCrypt byte limit.");
        return BCrypt.hashpw(password,BCrypt.gensalt(12));
    }
    public static boolean verify(String password,String stored){
        if(!withinLimit(password)||stored==null||!stored.matches("\\$2a\\$[0-9]{2}\\$[./A-Za-z0-9]{53}"))return false;
        try{
            int cost=Integer.parseInt(stored.substring(4,6));
            if(cost<4||cost>16)return false;
            return BCrypt.checkpw(password,stored);
        }catch(IllegalArgumentException e){return false;}
    }
}
