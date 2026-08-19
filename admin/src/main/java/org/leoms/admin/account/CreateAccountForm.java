package org.leoms.admin.account;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class CreateAccountForm {
    @NotBlank @Pattern(regexp = "[A-Za-z0-9]{4,13}", message = "Use 4-13 letters or digits")
    private String username = "";
    @NotBlank @Size(min = 12, max = 72)
    private String password = "";
    @NotBlank @Pattern(regexp = "\\d{4}", message = "PIN must be exactly 4 digits")
    private String pin = "";
    @NotBlank @Pattern(regexp = "\\d{6}", message = "PIC must be exactly 6 digits")
    private String pic = "";

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getPin() { return pin; }
    public void setPin(String pin) { this.pin = pin; }
    public String getPic() { return pic; }
    public void setPic(String pic) { this.pic = pic; }
}
