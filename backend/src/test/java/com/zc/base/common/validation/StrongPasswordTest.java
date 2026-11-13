package com.zc.base.common.validation;

import com.zc.base.system.dto.ChangePasswordDTO;
import com.zc.base.system.dto.ProfileUpdateDTO;
import com.zc.base.system.dto.ResetPasswordDTO;
import com.zc.base.system.dto.SysUserDTO;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StrongPasswordTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void boundaries() {
        assertFalse("abc1234");      // 7 位
        assertFalse("12345678");     // 8 位纯数字
        assertFalse("abcdefgh");     // 8 位纯字母
        assertFalse("abcd!@#$");     // 没有数字
        assertTrue("abcd1234");      // 8 位字母加数字
        assertTrue("Abcdefg1");
        assertTrue("a-very-long-password-with-1-digit");
    }

    @Test
    void appliesToResetPassword() {
        ResetPasswordDTO dto = new ResetPasswordDTO();
        dto.setNewPassword("12345678");
        assertEquals(1, validator.validate(dto).size());
        dto.setNewPassword("abc12345");
        assertEquals(0, validator.validate(dto).size());
    }

    @Test
    void appliesToChangePassword() {
        ChangePasswordDTO dto = new ChangePasswordDTO();
        dto.setOldPassword("x");
        dto.setNewPassword("short1");
        assertEquals(1, validator.validate(dto).size());
        dto.setNewPassword("longer123");
        assertEquals(0, validator.validate(dto).size());
    }

    @Test
    void createRequiresPasswordButUpdateDoesNot() {
        SysUserDTO dto = new SysUserDTO();
        dto.setUsername("u");

        assertEquals(1, validator.validate(dto, SysUserDTO.Create.class).size(), "新增必须填密码");
        assertEquals(0, validator.validate(dto).size(), "修改可以不填");
        dto.setPassword("");
        assertEquals(0, validator.validate(dto).size(), "修改时空串表示不改");

        dto.setPassword("12345678");
        assertEquals(1, validator.validate(dto, SysUserDTO.Create.class).size());
        assertEquals(1, validator.validate(dto).size(), "修改时填了就要符合强度");

        dto.setPassword("abc12345");
        assertEquals(0, validator.validate(dto, SysUserDTO.Create.class).size());
    }

    @Test
    void createStillRequiresUsername() {
        SysUserDTO dto = new SysUserDTO();
        dto.setPassword("abc12345");
        assertEquals(1, validator.validate(dto, SysUserDTO.Create.class).size());
    }

    @Test
    void profileEmailAndPhoneAllowEmptyButCheckFormat() {
        ProfileUpdateDTO dto = new ProfileUpdateDTO();
        assertEquals(0, validator.validate(dto).size());
        dto.setEmail("");
        dto.setPhone("");
        assertEquals(0, validator.validate(dto).size());

        dto.setEmail("a@b.com");
        dto.setPhone("13812345678");
        assertEquals(0, validator.validate(dto).size());

        dto.setEmail("not-an-email");
        dto.setPhone("12345");
        assertEquals(2, validator.validate(dto).size());
        dto.setEmail("a@b");
        assertEquals(2, validator.validate(dto).size(), "没有域名后缀的邮箱不通过");
    }

    private void assertFalse(String password) {
        ResetPasswordDTO dto = new ResetPasswordDTO();
        dto.setNewPassword(password);
        assertEquals(1, validator.validate(dto).size(), password);
    }

    private void assertTrue(String password) {
        ResetPasswordDTO dto = new ResetPasswordDTO();
        dto.setNewPassword(password);
        assertEquals(0, validator.validate(dto).size(), password);
    }
}
