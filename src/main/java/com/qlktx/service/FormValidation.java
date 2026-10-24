package com.qlktx.service;

import com.qlktx.util.BCryptPasswordUtil;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

public final class FormValidation {
    private FormValidation() { }
    public static String text(String value,String label,int max,boolean required)throws BusinessException{
        String result=value==null?"":value.trim();
        if((required&&result.isEmpty())||result.length()>max)throw new BusinessException(label+" không hợp lệ (tối đa "+max+" ký tự).");
        return result;
    }
    public static String id(String value,String label)throws BusinessException{
        String result=text(value,label,20,true);
        if(!result.matches("[A-Za-z0-9_.-]+"))throw new BusinessException(label+" chỉ gồm chữ, số, dấu chấm, gạch ngang hoặc gạch dưới.");
        return result;
    }
    public static String email(String value,boolean required)throws BusinessException{
        String result=text(value,"Email",120,required);
        if(!result.isEmpty()&&!result.matches("^[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+@[A-Za-z0-9-]+(?:\\.[A-Za-z0-9-]+)+$"))throw new BusinessException("Email không đúng định dạng.");
        return result;
    }
    public static LocalDate date(String value,String label,boolean required)throws BusinessException{
        if(value==null||value.isBlank()){if(required)throw new BusinessException("Vui lòng nhập "+label+".");return null;}
        try{return LocalDate.parse(value);}catch(DateTimeParseException e){throw new BusinessException(label+" không hợp lệ.");}
    }
    public static int number(String value,String label,int min,int max)throws BusinessException{
        try{int result=Integer.parseInt(value);if(result<min||result>max)throw new NumberFormatException();return result;}
        catch(NumberFormatException e){throw new BusinessException(label+" phải từ "+min+" đến "+max+".");}
    }
    public static String phone(String value)throws BusinessException{
        String result=text(value,"Số điện thoại",15,false);
        if(!result.isEmpty()&&!result.matches("\\+?[0-9]{9,14}"))
            throw new BusinessException("Số điện thoại gồm 9–14 chữ số, có thể bắt đầu bằng +.");
        return result;
    }
    public static void passwordChange(String current,String next,String confirmation)throws BusinessException{
        if(next==null||next.isBlank()||next.length()<8||!BCryptPasswordUtil.withinLimit(next))
            throw new BusinessException("Mật khẩu mới ít nhất 8 ký tự, tối đa 72 byte UTF-8.");
        if(!next.equals(confirmation))throw new BusinessException("Xác nhận mật khẩu mới chưa khớp.");
        if(next.equals(current))throw new BusinessException("Mật khẩu mới phải khác mật khẩu hiện tại.");
    }
}
