package com.qlktx.service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FormValidationTest {
    @Test void adminAndSignupAcceptTheSameEmailColumnLength() throws Exception {
        String email="a".repeat(60)+"@"+"b".repeat(55)+".edu";
        assertEquals(120,email.length());
        assertEquals(email,FormValidation.email(email,true));
        assertFalse(AuthService.validateRegistration(new RegistrationForm("2321160077",email,"","",
                "Strong-pass-2026","Strong-pass-2026")).containsKey("email"));
        assertThrows(BusinessException.class,()->FormValidation.email("a"+email,true));
    }

    @Test void contactNumbersAreOptionalButMustHaveNineToFourteenDigits() throws Exception {
        assertEquals("",FormValidation.phone(null));
        assertEquals("+84912345678",FormValidation.phone(" +84912345678 "));
        for(String phone:new String[]{"12345678","123456789012345","abc","+84 912345678"})
            assertThrows(BusinessException.class,()->FormValidation.phone(phone));
    }

    @Test void bothRolesRejectWeakRepeatedAndUnconfirmedPasswords() {
        assertDoesNotThrow(()->FormValidation.passwordChange("old-password","new-password","new-password"));
        assertThrows(BusinessException.class,()->FormValidation.passwordChange("same-password","same-password","same-password"));
        assertThrows(BusinessException.class,()->FormValidation.passwordChange("old-password","new-password","different"));
        for(String password:new String[]{null,"short","        ","ế".repeat(25)})
            assertThrows(BusinessException.class,()->FormValidation.passwordChange("old-password",password,password));
    }
}
