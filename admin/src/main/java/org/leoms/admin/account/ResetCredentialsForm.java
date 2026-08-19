package org.leoms.admin.account;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class ResetCredentialsForm {
    @Size(min = 0, max = 72)
    private String password = "";
    @Pattern(regexp = "^$|\\d{4}", message = "PIN must be blank or exactly 4 digits")
    private String pin = "";
    @Pattern(regexp = "^$|\\d{6}", message = "PIC must be blank or exactly 6 digits")
    private String pic = "";

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getPin() { return pin; }
    public void setPin(String pin) { this.pin = pin; }
    public String getPic() { return pic; }
    public void setPic(String pic) { this.pic = pic; }
}
