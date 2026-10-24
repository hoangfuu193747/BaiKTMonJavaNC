package com.qlktx.service;
import com.qlktx.dao.PortalDAO;
import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PortalRulesTest {
    @Test void paymentsAreWholeDongAndFitTheSqlColumn() throws Exception {
        assertEquals(new BigDecimal("700000"),PortalService.amount("700000",false));
        assertEquals(BigDecimal.ZERO,PortalService.amount("0",true));
        for(String value:new String[]{"0","-1","0.1","1e6","1000000000000",""," 100"})
            assertThrows(BusinessException.class,()->PortalService.amount(value,false));
    }
    @Test void registrationEligibilityAccountsForGenderCapacityAndExistingBookings() {
        var room=Map.<String,Object>of("trangThai","HOAT_DONG","khuHoatDong",true,"gioiTinh","Nam","conCho",1);
        assertTrue(PortalService.roomReasons(room,"Nam",false,false).isEmpty());
        assertFalse(PortalService.roomReasons(room,"Nữ",false,false).isEmpty());
        assertEquals(2,PortalService.roomReasons(room,"Nam",true,true).size());
        var unavailable=Map.<String,Object>of("trangThai","BAO_TRI","khuHoatDong",false,"gioiTinh","Nam","conCho",0);
        assertEquals(2,PortalService.roomReasons(unavailable,"Nam",false,false).size());
        assertFalse(PortalService.roomReasons(room,null,false,false).isEmpty());
    }
    @Test void repairAndNotificationContentCannotBeEmptyOversizedOrContainNull() throws Exception {
        assertEquals("Hỏng vòi nước",PortalService.content("  Hỏng vòi nước  ","Nội dung",1000));
        for(String value:new String[]{""," ","x".repeat(1001),"a\0b"})
            assertThrows(BusinessException.class,()->PortalService.content(value,"Nội dung",1000));
    }
}
